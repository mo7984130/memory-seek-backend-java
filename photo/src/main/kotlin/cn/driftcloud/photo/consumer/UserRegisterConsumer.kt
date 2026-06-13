package cn.driftcloud.photo.consumer

import cn.driftcloud.common.constant.MqConstants
import cn.driftcloud.common.pojo.vo.mq.UserRegisterEvent
import cn.driftcloud.photo.service.CollectionService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

/**
 * @author DriftCloud
 * @Description 监听用户注册消息，创建默认收藏夹
 * @Date 2026/2/6
 */
@Component
class UserRegisterConsumer(
    private val folderService: CollectionService
) {

    private val logger = LoggerFactory.getLogger(UserRegisterConsumer::class.java)

    /**
     * 监听用户注册消息，创建默认收藏夹
     */
    @RabbitListener(queues = [MqConstants.PHOTO_FOLDER_INIT_QUEUE])
    fun onUserRegister(event: UserRegisterEvent) {
        try {
            folderService.createFavoriteCollection(event.userId)
        } catch (e: Exception) {
            logger.warn("创建默认收藏夹失败，用户ID：{}", event.userId, e)
            throw e
        }
    }
}