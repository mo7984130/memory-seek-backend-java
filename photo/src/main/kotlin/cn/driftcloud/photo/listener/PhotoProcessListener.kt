package cn.driftcloud.photo.listener

import cn.driftcloud.photo.event.PhotoUploadedEvent
import cn.driftcloud.photo.service.FaceEngineService
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/14
 */
@Component
class PhotoProcessListener(
    private val faceEngineService: FaceEngineService
) {

    private val logger = org.slf4j.LoggerFactory.getLogger(PhotoProcessListener::class.java)

    @Async
    @EventListener
    fun handlePhotoUploaded(event: PhotoUploadedEvent) {
        // 调用你写好的 runPipeline
        faceEngineService.detectFaceAndRecognize(event.photoId, event.imageByte)
            .onLeft { logger.error("照片 ${event.photoId} 识别失败: $it") }
            .onRight { logger.info("照片 ${event.photoId} 处理完成") }
    }

}