package cn.driftcloud.photo.controller

import arrow.core.left
import arrow.core.right
import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.response.R
import cn.driftcloud.common.response.toRE
import cn.driftcloud.photo.mapper.PhotoMapper
import cn.driftcloud.photo.service.FaceEngineService
import cn.driftcloud.photo.service.MinioService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/15
 */
@RestController
@RequestMapping("/admin")
class AdminController(
    private val photoMapper: PhotoMapper,
    private val minioService: MinioService,
    private val faceService: FaceEngineService
) {

    private val logger = LoggerFactory.getLogger(AdminController::class.java)

    /**
     * 触发存量照片特征提取
     */
    @GetMapping("/rebuild-features")
    fun rebuildFeatures(
        @CurrentUserId userId: Long
    ): ResponseEntity<out R<out String?>> {
        if (userId != 1L) return "无权限".left().toRE()

        // 创建一个固定 8 线程的线程池
        val executor = Executors.newFixedThreadPool(8)
        // 信号量：控制同时处理的照片数，防止 100 张照片同时进内存压垮 JVM
        val semaphore = Semaphore(8)

        CompletableFuture.runAsync {
            var currentId = 9270L
            val batchSize = 100
            var totalProcessed = 0
            val limit = Int.MAX_VALUE// 调高上限以便测试性能

            logger.info("🚀 [并行重建启动] 使用 8 线程处理，开始提取...")

            searchLoop@ while (true) {
                val photos = try {
                    photoMapper.findPhotosForProcessing(currentId, batchSize)
                } catch (e: Exception) {
                    logger.error("❌ 获取列表失败: ${e.message}")
                    break
                }

                if (photos.isEmpty()) break

                // 将一批任务提交给线程池
                val futures = photos.map { photo ->
                    CompletableFuture.runAsync({
                        semaphore.acquire() // 获取许可
                        try {
                            minioService.getObjectStream(photo.fileId) { inputStream ->
                                val bytes = inputStream.readAllBytes()
                                faceService.detectFaceAndRecognize(photo.id!!, bytes)
                            }
                        } catch (e: Exception) {
                            logger.error("❌ ID: ${photo.id} 处理异常: ${e.message}")
                        } finally {
                            semaphore.release() // 释放许可
                        }
                    }, executor)
                }

                // 等待这一批（100张）全部处理完，再进行下一批，防止游标跳跃
                CompletableFuture.allOf(*futures.toTypedArray()).join()

                totalProcessed += photos.size
                currentId = photos.last().id!!

                logger.info("⏳ 进度: 已处理 $totalProcessed 张，当前 ID 游标: $currentId")

                // 内存保护与退出逻辑
                if (totalProcessed % 200 == 0) {
                    System.gc()
                }

                if (totalProcessed >= limit) {
                    logger.info("🛑 达到上限 $limit，停止任务")
                    break@searchLoop
                }
            }

            executor.shutdown() // 记得关闭线程池
            logger.info("✅ [全量重建完成] 总计处理: $totalProcessed")
        }

        return "8 线程后台任务已启动，请观察日志。".right().toRE()
    }

    /**
     * 触发人脸聚类
     */
    @GetMapping("/cluster-faces")
    fun clusterFaces(
        @CurrentUserId userId: Long
    ): ResponseEntity<out R<out String?>> {
        if (userId != 1L) return "无权限".left().toRE()
        faceService.performGlobalClustering()
        return "后台任务已启动，正在处理人脸聚类，请查看日志关注进度。".right().toRE()
    }

}