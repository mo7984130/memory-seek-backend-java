package cn.driftcloud.common.constant

/**
 * @author DriftCloud
 * @Description 消息队列常量
 * @Date 2026/2/6
 */
object MqConstants {
    /** 用户事件交换机 */
    const val USER_EVENT_EXCHANGE = "user.event.exchange"
    /** 用户注册路由键 */
    const val USER_REGISTER_ROUTING_KEY = "user.register.key"

    /** 照片收藏夹初始化队列 */
    const val PHOTO_FOLDER_INIT_QUEUE = "photo.folder.init.queue"
}

