package cn.driftcloud.photo.service

import ai.djl.inference.Predictor
import ai.djl.modality.cv.Image
import ai.djl.ndarray.NDManager
import ai.djl.opencv.OpenCVImageFactory
import ai.djl.repository.zoo.ZooModel
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.common.util.CacheUtils
import cn.driftcloud.common.util.VectorUtils
import cn.driftcloud.photo.entity.FaceBBox
import cn.driftcloud.photo.entity.FaceFeature
import cn.driftcloud.photo.entity.FacePerson
import cn.driftcloud.photo.entity.vo.FaceFeatureVO
import cn.driftcloud.photo.entity.vo.FacePersonSimpleVO
import cn.driftcloud.photo.entity.vo.FacePersonVO
import cn.driftcloud.photo.entity.vo.toVO
import cn.driftcloud.photo.mapper.*
import cn.driftcloud.photo.pojo.PhotoVO
import cn.driftcloud.photo.util.FaceAligner
import cn.driftcloud.photo.util.FaceDetection
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 人脸处理服务
 * @Date 2026/2/13
 */
@Service
class FaceEngineService(
    private val detModel: ZooModel<Image, List<FaceDetection>>,
    @param:Qualifier("faceRecognitionModel") private val recModel: ZooModel<Image, FloatArray>,
    private val faceFeatureMapper: FaceFeatureMapper,
    private val facePersonMapper: FacePersonMapper,
    private val cacheUtils: CacheUtils,
    private val photoService: PhotoService,
    private val imgProxyService: ImgProxyService,
    private val photoMapper: PhotoMapper
) {
    val logger = LoggerFactory.getLogger(FaceEngineService::class.java)

    private val CENTROID_EFFECT_DISTANCE = 0.7f

    // 定义一个泛型函数，生成对应类型的配置
    private fun <T> createPoolConfig(): GenericObjectPoolConfig<T> {
        val config = GenericObjectPoolConfig<T>()
        val cores = Runtime.getRuntime().availableProcessors()

        config.apply {
            maxTotal = (cores * 1.2).toInt().coerceAtLeast(4)
            blockWhenExhausted = true
            setMaxWait(Duration.ofSeconds(10))
            minIdle = 2
        }
        return config
    }

    //  初始化检测池
    private val detectorPool = GenericObjectPool(
        object : BasePooledObjectFactory<Predictor<Image, List<FaceDetection>>>() {
            override fun create() = detModel.newPredictor()
            override fun wrap(p: Predictor<Image, List<FaceDetection>>) = DefaultPooledObject(p)
        },
        createPoolConfig<Predictor<Image, List<FaceDetection>>>()
    )
    // 初始化识别池
    private val recognizerPool = GenericObjectPool(
        object : BasePooledObjectFactory<Predictor<Image, FloatArray>>() {
            override fun create() = recModel.newPredictor()
            override fun wrap(p: Predictor<Image, FloatArray>) = DefaultPooledObject(p)
        },
        createPoolConfig<Predictor<Image, FloatArray>>()
    )

    /**
     * 检测图片中的人脸并进行识别
     * @param photoId 图片ID
     * @param imageByte 图片字节数组
     * @return Either<错误原因, Unit>
     */
    @Transactional
    fun detectFaceAndRecognize(photoId: Long, imageByte: ByteArray): Either<String, Unit> {
        logger.info("开始检测photoId: ${photoId}的图片")

        // OpenCV堆外解码
        val mob = MatOfByte(*imageByte)
        val mat = try {
            Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR)
        } finally {
            mob.release()
        }

        val detector = detectorPool.borrowObject()
        val recognizer = recognizerPool.borrowObject()
        val factory = OpenCVImageFactory.getInstance()

        NDManager.newBaseManager().use { _ ->
            try {
                val detections = detector.predict(factory.fromImage(mat))
                logger.info("在photoId: ${photoId}的图片中检测到${detections.size}张人脸")

                detections.forEach { detection ->
                    val matW = mat.width().toFloat()
                    val matH = mat.height().toFloat()
                    val rawBbox = detection.boundingBox.bounds

                    // 质量初筛
                    val pxArea = (rawBbox.width * matW) * (rawBbox.height * matH)
                    if (detection.score < 0.65f || pxArea < 160 * 160 || rawBbox.width < 0.05f || rawBbox.height < 0.05f) return@forEach

                    val scrfdLms = detection.landmarks
                    val inputSize = 640f // SCRFD 内部 resize 的尺寸

                    val pointsForAlign = FloatArray(10)
                    for (i in 0 until 5) {
                        // 逻辑：(坐标 / 640) * 原图对应维度
                        pointsForAlign[i * 2] = (scrfdLms[i * 2] / inputSize) * matW
                        pointsForAlign[i * 2 + 1] = (scrfdLms[i * 2 + 1] / inputSize) * matH
                    }
                    val alignedMat = FaceAligner.alignFace(mat, pointsForAlign)
                    try {
                        // 1. 用于识别：转成 RGB
                        val rgbMat = Mat()
                        Imgproc.cvtColor(alignedMat, rgbMat, Imgproc.COLOR_BGR2RGB)

                        // 提取特征向量 (w600k_r50)
                        val embedding = try {
                            VectorUtils.l2Normalize(recognizer.predict(factory.fromImage(rgbMat)))
                        } finally {
                            rgbMat.release()
                        }
                        // 构造特征对象 (person_id 设为 null，待凌晨聚类)
                        val feature = FaceFeature(
                            photoId = photoId,
                            embedding = embedding,
                            bbox = FaceBBox(
                                x = rawBbox.x.toFloat(),
                                y = rawBbox.y.toFloat(),
                                w = rawBbox.width.toFloat(),
                                h = rawBbox.height.toFloat()
                            ),
                            score = detection.score,
                            personId = null
                        )
                        faceFeatureMapper.insert(feature)
                    } finally {
                        alignedMat.release()
                    }
                }
            } catch (e: Exception) {
                logger.warn("处理photoId: ${photoId}的图片时出错", e)
                return Either.Left("处理图片时出错")
            } finally {
                mat.release()
                detectorPool.returnObject(detector)
                recognizerPool.returnObject(recognizer)
            }
        }
        return Either.Right(Unit)
    }
    /**
     * 将对齐后的人脸 Mat 保存为本地图片以供调试
     */
    fun debugSaveAlignedFace(alignedMat: Mat, prefix: String = "debug_face") {
        try {
            // 创建调试文件夹
            val debugFolder = File("C:\\Users\\mo\\Downloads\\debug_output")
            if (!debugFolder.exists()) debugFolder.mkdirs()

            // 生成唯一文件名，例如：debug_face_1707835200.jpg
            val timestamp = Instant.now().epochSecond
            val fileName = "${debugFolder.absolutePath}/${prefix}_$timestamp.jpg"

            // 使用 OpenCV 保存 Mat
            val success = Imgcodecs.imwrite(fileName, alignedMat)

            if (success) {
                println("✅ 调试图片已保存: $fileName")
            } else {
                println("❌ 调试图片保存失败，请检查 OpenCV 库是否正确加载")
            }
        } catch (e: Exception) {
            println("⚠️ 保存调试图片时发生错误: ${e.message}")
        }
    }

    data class PersonCentroid(
        val id: Long,
        var vector: FloatArray,
        val memberNodes: MutableList<FeatureNode>,
        var totalWeight: Float
    )
    /**
     * 执行全局聚类
     * @param radius 邻域半径 (Eps)。对于 ArcFace 512维归一化向量，通常在 0.6-0.7 之间（欧氏距离）
     * @param minPoints 形成一个人物所需的最少照片数
     */
    @Transactional
    fun performGlobalClustering(seedRadius: Double = 0.70, minPoints: Int = 2) {
        // 1. 加载所有特征数据
        val nodes = faceFeatureMapper.selectAllEmbeddingsWithPersonId()
        if (nodes.size < minPoints) return
        logger.info("开始聚类: 样本总数 ${nodes.size}")

        // --- Phase 1: 寻找种子 (Union-Find) ---
        // 通过并查集找到相似度极高的小簇，作为人物的初始“根基”
        val seedIndices = runUnionFind(nodes, seedRadius)
        val seeds = mutableListOf<PersonCentroid>()

        seedIndices.forEach { (_, indices) ->
            if (indices.size < minPoints) return@forEach
            val group = indices.map { nodes[it] }

            // 物理隔离校验：同一个种子簇内不允许出现同图照片
            if ( group.groupingBy { it.photoId }.eachCount().values.any{ it > 1} ) return@forEach

            // 计算初始重心并持久化 Person 记录
            val centroid = calculateWeightedCentroid(group)
            val best = group.maxBy { it.score }
            val weight = group.sumOf { it.score.toDouble() }.toFloat()

            val personId = createOrUpdatePerson(group, centroid, weight, best)

            // 更新内存状态
            group.forEach { it.personId = personId }
            seeds.add(PersonCentroid(personId, centroid, group.toMutableList(), weight))
        }

        // --- Phase 2: 双层吸附增长 (Grow Stage) ---
        // 第一轮：0.75 严格吸附 (允许重心微调，适合捕获同一个人不同角度的清晰照)
        growStage(nodes, seeds, growRadius = 0.75, updateCentroid = true)
        // 第二轮：0.85 宽松吸附 (禁止更新重心，防止雪崩，用于打捞模糊或远景照片)
        growStage(nodes, seeds, growRadius = 0.85, updateCentroid = false)

        // --- Phase 3: 最终同步 (批量持久化) ---
        syncResultsToDb(seeds)

        logger.info("聚类任务执行完毕，当前人物总数: ${seeds.size}")
    }
    private fun growStage(nodes: List<FeatureNode>, seeds: List<PersonCentroid>, growRadius: Double, updateCentroid: Boolean) {
        val unassigned = nodes.filter { it.personId == null }.sortedByDescending { it.score }

        unassigned.forEach { node ->
            // 找到最近的种子重心
            val nearest = seeds.asSequence()
                .map { it to calculateDistance(node.embedding, it.vector) }
                .filter { it.second < growRadius }
                .minByOrNull { it.second } ?: return@forEach

            val (seed, dist) = nearest

            // 必须满足同图唯一性
            if (seed.memberNodes.none { it.photoId == node.photoId }) {
                node.personId = seed.id
                seed.memberNodes.add(node)

                // 只有高置信度且允许更新时才漂移重心
                if (updateCentroid && dist < CENTROID_EFFECT_DISTANCE) {
                    seed.vector = calculateWeightedCentroid(seed.memberNodes)
                    seed.totalWeight = seed.memberNodes.sumOf { it.score.toDouble() }.toFloat()
                }
            }
        }
    }
    private fun createOrUpdatePerson(
        group: List<FeatureNode>,
        centroid: FloatArray,
        totalWeight: Float,
        bestFeature: FeatureNode
    ): Long {
        // 1. 继承身份逻辑（保持不变）
        group.firstNotNullOfOrNull { it.personId }?.let { existingId ->
            facePersonMapper.manualUpdateStats(
                id = existingId,
                centroid = centroid,
                weight = totalWeight,
                count = group.size,
                maxScore = bestFeature.score,
                maxScoreId = bestFeature.id
            )
            return existingId
        }

        // 2. 创建全新身份
        // 使用 for 循环可以更清晰地表达“最后一次失败直接抛出”的逻辑
        for (attempt in 1..3) {
            try {
                val datePart = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd").format(java.time.LocalDate.now())
                val randomPart = java.util.UUID.randomUUID().toString().substring(0, 4)
                val uniqueName = "人物_${datePart}_$randomPart"

                val newPerson = FacePerson(
                    name = uniqueName,
                    maxScoreFeatureId = bestFeature.id,
                    maxScore = bestFeature.score,
                    totalPhotoCount = group.size.toLong(),
                    totalWeightCount = totalWeight,
                    centroidEmbedding = centroid
                )

                facePersonMapper.insert(newPerson)
                return newPerson.id!!
            } catch (e: DuplicateKeyException) {
                if (attempt == 3) throw e // 最后一次直接抛出，不再捕获
                logger.warn("⚠️ 第 $attempt 次命名冲突 [${bestFeature.id}]，正在重试...")
            }
        }
        throw IllegalStateException("无法在多次尝试后创建唯一的人物记录")
    }
    /**
     * 辅助函数：计算加权重心
     */
    private fun calculateWeightedCentroid(nodes: List<FeatureNode>): FloatArray {
        val dimension = 512
        val totalWeight = nodes.sumOf { it.score.toDouble() }.toFloat()
        val weightedSum = FloatArray(dimension)

        nodes.forEach { node ->
            val w = node.score
            val emb = node.embedding
            for (i in 0 until dimension) {
                weightedSum[i] += emb[i] * w
            }
        }
        return FloatArray(dimension) { weightedSum[it] / totalWeight }
    }
    private fun runUnionFind(nodes: List<FeatureNode>, radius: Double): Map<Int, List<Int>> {
        val n = nodes.size
        val parent = IntArray(n) { it }

        fun find(i: Int): Int {
            var c = i
            while (parent[c] != c) {
                parent[c] = parent[parent[c]] // 路径压缩
                c = parent[c]
            }
            return c
        }

        fun union(i: Int, j: Int) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) parent[rootI] = rootJ
        }

        // 空间距离遍历
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                // 同一张照片里的脸绝不能合并（物理常识）
                if (nodes[i].photoId == nodes[j].photoId) continue

                if (calculateDistance(nodes[i].embedding, nodes[j].embedding) < radius) {
                    union(i, j)
                }
            }
        }

        // 返回：根节点索引 -> 属于该簇的所有节点索引列表
        return nodes.indices.groupBy { find(it) }
    }
    private fun syncResultsToDb(seeds: List<PersonCentroid>) {
        seeds.forEach { seed ->
            val featureIds = seed.memberNodes.map { it.id }
            if (featureIds.isEmpty()) return@forEach

            // 1. 批量同步 Feature 所属的 PersonID
            faceFeatureMapper.batchUpdatePersonId(seed.id, featureIds)

            // 2. 校准 Person 表统计信息
            val best = seed.memberNodes.maxBy { it.score }
            facePersonMapper.manualUpdateStats(
                seed.id, seed.vector, seed.totalWeight,
                seed.memberNodes.size, best.score, best.id
            )
        }
    }
    private fun calculateDistance(a: FloatArray, b: FloatArray): Double {
        var sum = 0.0
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return Math.sqrt(sum)
    }

    /**
     * 查询人物分页
     * @param cursor 分页游标, 即人物的总照片数
     * @param size 每页大小
     * @return Either<错误原因, CursorPageVO<FacePersonVO, Long>>
     */
    fun getPersonPage(
        cursor: Long?,
        size: Int
    ): Either<String, CursorPageVO<FacePersonVO, Long>> {
        // 查询人物
        val query = KtQueryWrapper(FacePerson::class.java)
            .orderByDesc(FacePerson::totalPhotoCount)
        if (cursor != null) query.lt(FacePerson::totalPhotoCount, cursor)
        query.last("LIMIT ${size + 1}")

        // 组装Page参数
        val originalList = facePersonMapper.selectList(query)

        if (originalList.isEmpty()) return CursorPageVO.emptyCursorPageVO<FacePersonVO, Long>().right()

        val hasMore = originalList.size > size
        val nextCursor = if (hasMore) originalList.last().totalPhotoCount else null
        val list = originalList.take(size)

        // 获取封面图片, bbox裁剪
        // 获取封面feature
        val featureIds = list.map { it.maxScoreFeatureId }
        val simpleFaceFeatures = faceFeatureMapper.selectSimpleFaceFeatureByFeatureIds(featureIds)
        val featureMap = simpleFaceFeatures.associateBy { it.id }
        // 获取图片
        val photoIds = simpleFaceFeatures.map { it.photoId }.distinct()
        val photos = photoMapper.selectByIds(photoIds)
        val photoMap = photos.associateBy { it.id }

        val records = list.mapNotNull { person ->
            featureMap[person.maxScoreFeatureId]?.let { feature ->
                photoMap[feature.photoId]?.let { photo ->
                    // 构造裁剪字符串

                    val (x, y, w, h) = feature.bbox
                    val width = photo.width!!
                    val height = photo.height!!

                    val cw = (w * width).toInt()
                    val ch = (h * height).toInt()
                    val cx = (x * width).coerceAtLeast(0.0f).toInt()
                    val cy = (y * height).coerceAtLeast(0.0f).toInt()

                    // 组装imgproxy参数
                    val imgOption = "crop:$cw:$ch:nowe:$cx:$cy/rs:fill:200:200"

                    person.toVO(imgProxyService.generateImgProxyUrl(photo.fileId, imgOption))
                }
            }
        }
        return Either.Right(CursorPageVO(records, nextCursor, hasMore))
    }

    /**
     * 重命名人物
     * @param personId 人物ID
     * @param newName 新名称
     * @return Either<错误原因, FacePersonVO>
     */
    @Transactional
    fun renamePerson(
        personId: Long,
        newName: String
    ): Either<String, FacePersonVO> {
        //  获取人物
        val person = facePersonMapper.selectById(personId) ?: return Either.Left("人物不存在")
        // 检查名字是否存在
        val query = KtQueryWrapper(FacePerson::class.java).eq(FacePerson::name, newName)
        val exists = facePersonMapper.selectOne(query) != null
        if (exists) return Either.Left("人物名称已存在")
        // 更新人物名称
        person.name = newName
        val row = facePersonMapper.updateById(person)
        return if (row < 1) Either.Left("更新失败") else {
            cacheUtils.delete(RedisKeys.PHOTO_FACE_PERSON_NAME_CACHE.format(personId))
            Either.Right(person.toVO())
        }
    }

    /**
     * 合并人物
     * @param sourcePersonId 源人物ID 被删除的那个
     * @param targetPersonId 目标人物ID
     * @return Either<错误原因, FacePersonVO>
     */
    @Transactional
    fun mergePerson(sourcePersonId: Long, targetPersonId: Long): Either<String, FacePersonVO> {
        if (sourcePersonId == targetPersonId) return Either.Left("源人物和目标人物相同")

        // 获取人物
        val sourcePerson = facePersonMapper.selectByPersonId(sourcePersonId) ?: return Either.Left("源人物不存在")
        val targetPerson = facePersonMapper.selectByPersonId(targetPersonId) ?: return Either.Left("目标人物不存在")

        // 计算质心
        val w1 = sourcePerson.totalWeightCount
        val w2 = targetPerson.totalWeightCount
        val newTotalWeight = w1 + w2
        val c1 = sourcePerson.centroidEmbedding
        val c2 = targetPerson.centroidEmbedding
        val mergedCentroid = FloatArray(512)

        for (i in 0 until 512) {
            mergedCentroid[i] = (c1[i].times(w1) + c2[i].times(w2)) / newTotalWeight
        }
        targetPerson.centroidEmbedding = VectorUtils.l2Normalize(mergedCentroid)
        targetPerson.totalWeightCount = newTotalWeight

        // 更新照片总数
        targetPerson.totalPhotoCount += sourcePerson.totalPhotoCount
        facePersonMapper.updateById(targetPerson)

        // 更新特征表
        val updateWrapper = KtUpdateWrapper(FaceFeature::class.java)
            .eq(FaceFeature::personId, sourcePersonId)
            .set(FaceFeature::personId, targetPersonId)
        faceFeatureMapper.update(null, updateWrapper)

        // 删除目标人物
        facePersonMapper.deleteById(sourcePerson)
        cacheUtils.delete(RedisKeys.PHOTO_FACE_PERSON_NAME_CACHE.format(sourcePersonId))

        return Either.Right(targetPerson.toVO())
    }

    /**
     * 更改特征所属人物
     * @param featureId 特征ID
     * @param targetPersonId 目标人物ID
     * @return Either<错误原因, Unit>
     */
    @Transactional
    fun changeFaceBelonging(featureId: Long, targetPersonId: Long): Either<String, Unit> {
        // 1. 获取特征并检查
        val feature = faceFeatureMapper.selectById(featureId) ?: return "特征不存在".left()
        val sourcePersonId = feature.personId // 这里不再强转非空
        if (sourcePersonId == targetPersonId) return "源人物和目标人物相同".left()

        // 2. 准备锁定逻辑
        // 只有 sourcePersonId 存在时才参与锁定和减法逻辑
        val idsToLock = mutableListOf(targetPersonId)
        sourcePersonId?.let { idsToLock.add(it) }

        val personMap = idsToLock.sorted().associateWith { facePersonMapper.selectForUpdate(it)!! }
        val targetPerson = personMap[targetPersonId]!!
        val sourcePerson = sourcePersonId?.let { personMap[it] }

        // 3. 物理变更提前
        feature.personId = targetPersonId
        faceFeatureMapper.updateById(feature)

        val fWeight = feature.score
        val fEmb = feature.embedding
        val DIM = 512

        // --- 4. 如果存在源人物，执行“减法”和“封面校准” ---
        sourcePerson?.let { person ->
            person.totalPhotoCount -= 1
            if (person.totalPhotoCount <= 0) {
                facePersonMapper.deleteById(person.id)
            } else {
                val sOldW = person.totalWeightCount
                val sourceDistance = calculateDistance(fEmb, person.centroidEmbedding)

                // 增量减去重心权重
                if (sourceDistance < CENTROID_EFFECT_DISTANCE && sOldW > fWeight) {
                    val sNewW = (sOldW - fWeight).coerceAtLeast(0.01f) // 保证不为0
                    person.centroidEmbedding = FloatArray(DIM) { i ->
                        (person.centroidEmbedding[i] * sOldW - fEmb[i] * fWeight) / sNewW
                    }
                    person.totalWeightCount = sNewW
                }

                // 封面图重选
                if (person.maxScoreFeatureId == featureId) {
                    val nextBest = faceFeatureMapper.selectMaxScoreFeatureByPersonId(person.id!!)
                    person.maxScore = nextBest?.score ?: 0f
                    person.maxScoreFeatureId = nextBest?.id!! // 这里移除 !! 以防万一
                }
                facePersonMapper.updateById(person)
            }
        }

        // --- 5. 更新目标人物 (移入逻辑保持不变) ---
        val tOldW = targetPerson.totalWeightCount
        val targetDistance = calculateDistance(fEmb, targetPerson.centroidEmbedding)

        if (targetDistance < CENTROID_EFFECT_DISTANCE) {
            val tNewW = tOldW + fWeight
            targetPerson.centroidEmbedding = FloatArray(DIM) { i ->
                (targetPerson.centroidEmbedding[i] * tOldW + fEmb[i] * fWeight) / tNewW
            }
            targetPerson.totalWeightCount = tNewW
        }

        targetPerson.totalPhotoCount += 1
        if (fWeight > targetPerson.maxScore) {
            targetPerson.maxScore = fWeight
            targetPerson.maxScoreFeatureId = featureId
        }
        facePersonMapper.updateById(targetPerson)

        return Either.Right(Unit)
    }

    /**
     * 获取所有人物的信息
     */
    fun getAllPersonInfo(): Either<String, List<FacePersonSimpleVO>> {
        val persons = facePersonMapper.selectListIdName()
        return Either.Right(persons.map { FacePersonSimpleVO(it.id.toString(), it.name) })
    }

    /**
     * 删除人物
     * @param personId 人物ID
     * @return Either<错误原因, 是否删除成功>
     */
    @Transactional
    fun deletePerson(personId: Long): Either<String, Boolean> {
        // 获取人物
        facePersonMapper.selectById(personId) ?: return "人物不存在".left()

        // 删除特征表的关联记录
        val query = KtQueryWrapper(FaceFeature::class.java).eq(FaceFeature::personId, personId)
        var deleteRow = faceFeatureMapper.delete(query)
        if (deleteRow < 1) return "删除特征表记录失败".left()

        // 删除人物
        deleteRow = facePersonMapper.deleteById(personId)
        if (deleteRow < 1) return "删除人物失败".left()

        cacheUtils.delete(RedisKeys.PHOTO_FACE_PERSON_NAME_CACHE.format(personId))

        return Either.Right(true)
    }

    /**
     * 获取照片中的所有人脸
     * @param photoId 照片ID
     * @return Either<错误原因, List<FaceFeatureVO>>
     */
    fun getPhotoFeature(photoId: Long): Either<String, List<FaceFeatureVO>> {
        // 获取特征
        val query = KtQueryWrapper(FaceFeature::class.java).eq(FaceFeature::photoId, photoId)
        val features = faceFeatureMapper.selectList(query)
        if (features.isEmpty()) return emptyList<FaceFeatureVO>().right()

        val personIds = features.mapNotNull { it.personId }.distinct()

        // 获取人物名字
        val personNameMap = if (personIds.isNotEmpty()) {
            cacheUtils.getOrLoadBatch(
                personIds,
                { RedisKeys.PHOTO_FACE_PERSON_NAME_CACHE.format(it) },
                FacePersonIdNamePair::class.java,
                { missingIds ->
                    facePersonMapper.selectIdNameByIds(missingIds)
                },
                { it.id }
            ).associate { it.id to it.name }
        } else {
            emptyMap()
        }

        return features.map { feature ->
            val personName = feature.personId?.let { personNameMap[it] } ?: "未知人物"
            feature.toVO(personName)
        }.right()
    }

    /**
     * 获取人物信息
     * @param personId 人物ID
     * @return Either<错误原因, FacePersonVO>
     */
    fun getPersonInfo(userId: Long, personId: Long): Either<String, FacePersonVO> {
        val person = facePersonMapper.selectByPersonId(personId) ?: return "人物不存在".left()
        val feature = faceFeatureMapper.selectById(person.maxScoreFeatureId) ?: return "特征不存在".left()
        val photoVOs = photoService.getPhotoVOs(userId, listOf(feature.photoId))
        if (photoVOs.isEmpty()) return "照片不存在".left()
        val photoVO = photoVOs[0]
        val coverPhotoUrl = photoVO.thumbnailUrl
        return Either.Right(person.toVO(coverPhotoUrl))
    }

    /**
     * 获取人物的所有照片
     * @param personId 人物ID
     * @param cursor 特征id
     * @return Either<错误原因, List<PhotoVO>>
     */
    fun getPersonPhoto(userId:Long, personId: Long, cursor: Long?, size: Int): Either<String, CursorPageVO<PhotoVO, Long>> {
        // 获取features
        val query = KtQueryWrapper(FaceFeature::class.java)
            .eq(FaceFeature::personId, personId)
            .orderByDesc(FaceFeature::id)
        if(cursor != null) query.lt(FaceFeature::id, cursor)
        query.last("LIMIT ${size + 1}")
        val originalFeatures = faceFeatureMapper.selectList(query)
        val hasMore = originalFeatures.size > size
        val displayFeatures = originalFeatures.take(size)
        val recordPhotoIds = displayFeatures.map { it.photoId }.distinct()
        val nextCursor = if (hasMore) displayFeatures.last().id else null

        // 获取照片
        return Either.Right(CursorPageVO(photoService.getPhotoVOs(userId, recordPhotoIds), nextCursor, hasMore))
    }
}