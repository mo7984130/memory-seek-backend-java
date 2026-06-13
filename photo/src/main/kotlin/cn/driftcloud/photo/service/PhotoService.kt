package cn.driftcloud.photo.service

import arrow.core.Either
import cn.driftcloud.common.config.MinioConfig
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.common.pojo.vo.CursorPageVO.Companion.emptyCursorPageVO
import cn.driftcloud.common.util.FileValidator
import cn.driftcloud.photo.entity.Photo
import cn.driftcloud.photo.event.PhotoUploadedEvent
import cn.driftcloud.photo.mapper.CollectionMapper
import cn.driftcloud.photo.mapper.PhotoMapper
import cn.driftcloud.photo.pojo.PhotoVO
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
@Service
class PhotoService(
    private val photoMapper: PhotoMapper,
    private val collectionMapper: CollectionMapper,
    private val collectionService: CollectionService,
    private val imgProxyService: ImgProxyService,
    private val minioClient: MinioClient,
    private val minioConfig: MinioConfig,
    private val eventPublisher: ApplicationEventPublisher,
): ServiceImpl<PhotoMapper, Photo>() {

    private val logger = LoggerFactory.getLogger(PhotoService::class.java)

    /**
     * 检查MD5是否存在
     * @param md5 MD5值
     * @return 是否存在
     */
    fun md5Exist(md5: String): Boolean {
        return photoMapper.selectCount(
            KtQueryWrapper(Photo::class.java)
                .eq(Photo::md5, md5)
        ) > 0
    }

    fun getPhotoCursorPage(
        userId: Long,
        cursor: Instant?,
        size: Int,
        direction: String = "next",
        defaultCollectionId: Long?
    ): CursorPageVO<PhotoVO, Instant> {
        logger.info("获取照片分页列表请求: 用户ID={}, 游标={}, 大小={}, 方向={}, 默认收藏夹ID={}", userId, cursor, size, direction, defaultCollectionId)
        try {
            // 多取一条判断是否有更多
            val queryWrapper = KtQueryWrapper(Photo::class.java).last("LIMIT ${size + 1}")

            if (direction == "next") {
                queryWrapper.orderByDesc(Photo::createdAt)
            } else {
                queryWrapper.orderByAsc(Photo::createdAt)
            }

            if (cursor != null) {
                if (direction == "next") {
                    queryWrapper.lt(Photo::createdAt, cursor)
                } else {
                    queryWrapper.gt(Photo::createdAt, cursor)
                }
            }

            // 执行查询
            val list = baseMapper.selectList(queryWrapper)
            logger.debug("查询到照片数量: {}", list.size)
            
            // 如果多一条的话, 去除
            val hasMore = list.size > size
            val resultList = if (hasMore) list.dropLast(1) else list
            val sortedPhotos = if (direction == "prev") resultList.reversed() else resultList
            val photoIds = sortedPhotos.map { it.id!! }

            if (sortedPhotos.isEmpty()) {
                logger.info("获取照片分页列表成功: 无数据, 用户ID={}", userId)
                return emptyCursorPageVO()
            }

            // 查询是否喜欢
            // 获取用户喜欢收藏夹的id
            val favoriteCollectionId = collectionService.getFavoriteCollectionId(userId)
            val collectedFolderPhotoIds = collectionMapper.findCollectedIds(userId, favoriteCollectionId, photoIds).toSet()
            logger.debug("用户喜欢的照片数量: {}", collectedFolderPhotoIds.size)

            // 查询图片是否在默认收藏夹中
            var collectedInDefaultCollectionPhotoIds = emptySet<Long>()
            if (defaultCollectionId != null) {
                collectedInDefaultCollectionPhotoIds = collectionMapper.findCollectedIds(userId, defaultCollectionId, photoIds).toSet()
                logger.debug("用户默认收藏夹中的照片数量: {}", collectedInDefaultCollectionPhotoIds.size)
            }

            val voList = sortedPhotos.map { photo ->
                val photoVO = imgProxyService.photoToVO(photo)
                photoVO.isFavorited = collectedFolderPhotoIds.contains(photo.id)
                photoVO.isCollected = collectedInDefaultCollectionPhotoIds.contains(photo.id)
                photoVO
            }

            val result = CursorPageVO(
                records = voList,
                nextCursor = if (hasMore) voList.lastOrNull()?.createdAt else null,
                hasMore = hasMore
            )
            
            logger.info("获取照片分页列表成功: 返回数量={}, 是否有更多={}, 用户ID={}", voList.size, hasMore, userId)
            return result
        } catch (e: Exception) {
            logger.error("获取照片分页列表异常: 用户ID={}", userId, e)
            return emptyCursorPageVO()
        }
    }

    /**
     * 上传照片
     * @param file 照片文件
     * @param meta 照片元数据
     * @param userId 用户ID
     * @return 照片VO
     */
    fun uploadPhoto(file: MultipartFile, meta: FileValidator.ImageMetaData, userId: Long, createdAt: Instant? = null): Either<String, PhotoVO> {
        logger.info("照片上传请求: 文件名={}, 大小={}, 用户ID={}", meta.name, file.size, userId)
        try {
            val md5 = DigestUtils.md5DigestAsHex(file.inputStream)
            logger.debug("照片MD5: {}", md5)

            // 2. 校验MD5是否存在
            if (md5Exist(md5)) {
                logger.warn("照片上传失败: 图片已存在, MD5={}, 用户ID={}", md5, userId)
                return Either.Left("图片已存在")
            }

            val uuid = UUID.randomUUID().toString().replace("-", "")
            val datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
            val filePath = "photos/$datePath/$uuid.${meta.format}"
            logger.debug("照片存储路径: {}", filePath)

            // 上传图片
            file.inputStream.use {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(minioConfig.bucketName)
                        .`object`(filePath)
                        .stream(it, file.size, -1)
                        .contentType(meta.mineType)
                        .build()
                )
            }
            logger.info("照片上传到MinIO成功: 路径={}, 桶={}", filePath, minioConfig.bucketName)

            // 更新数据库
            val photo = Photo(
                userId = userId,
                name = meta.name,
                size = file.size,
                width = meta.width,
                height = meta.height,
                mimeType = meta.mineType,
                md5 = md5,
                fileId = filePath,
                createdAt = createdAt
            )
            photoMapper.insert(photo)
            logger.info("照片信息保存到数据库成功: 照片ID={}, 文件名={}", photo.id, meta.name)

            //  发布照片上传事件
            try {
                eventPublisher.publishEvent(PhotoUploadedEvent(photo.id!!, file.bytes))
                logger.info("发布照片上传事件成功: 照片ID={}", photo.id)
            } catch (e: Exception) {
                logger.error("发布照片上传事件失败: 照片ID={}", photo.id, e)
            }

            val photoVO = imgProxyService.photoToVO(photo)
            logger.info("照片上传完成: 照片ID={}, 用户ID={}", photo.id, userId)
            return Either.Right(photoVO)
        } catch (e: Exception) {
            logger.error("照片上传异常: 文件名={}, 用户ID={}", meta.name, userId, e)
            return Either.Left("照片上传失败，请稍后重试")
        }
    }

    /**
     * 获取照片详情
     * @param id 照片ID
     * @return 照片详情
     */
    fun getPhoto(id: String): Either<String, PhotoVO> {
        logger.info("获取照片详情请求: 照片ID={}", id)
        try {
            val photo = photoMapper.selectById(id) ?: run {
                logger.warn("获取照片详情失败: 照片不存在, 照片ID={}", id)
                return Either.Left("照片不存在")
            }
            val photoVO = imgProxyService.photoToVO(photo)
            logger.info("获取照片详情成功: 照片ID={}, 文件名={}", id, photo.name)
            return Either.Right(photoVO)
        } catch (e: Exception) {
            logger.error("获取照片详情异常: 照片ID={}", id, e)
            return Either.Left("获取照片详情失败，请稍后重试")
        }
    }

    /**
     * 根据照片ID列表获取照片VO列表
     * @param photoIds 照片ID列表
     * @return 照片VO列表
     */
     fun getPhotoVOs(userId: Long, photoIds: List<Long>): List<PhotoVO> {
        if (photoIds.isEmpty()) return emptyList()
        val photos = photoMapper.selectList(KtQueryWrapper(Photo::class.java).`in`(Photo::id, photoIds))

        val favoriteCollectionId = collectionService.getFavoriteCollectionId(userId)
        val collectedFolderPhotoIds = collectionMapper.findCollectedIds(userId, favoriteCollectionId, photoIds).toSet()

        return photos.map { imgProxyService.photoToVO(it).apply {
            isFavorited = collectedFolderPhotoIds.contains(it.id)
        } }
    }

}