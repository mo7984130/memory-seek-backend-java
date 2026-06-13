package cn.driftcloud.photo.config

import cn.driftcloud.common.constant.MqConstants
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/6
 */
@Configuration
class PhotoRabbitConfig {

    /**
     * 文件夹初始化队列
     */
    @Bean
    fun folderInitQueue(): Queue = Queue(MqConstants.PHOTO_FOLDER_INIT_QUEUE, true)

    /**
     * 用户事件交换机
     * 消费者也声明 Exchange 防止 Photo 服务先于 Auth 服务启动时报错
     */
    @Bean
    fun userEventExchange(): TopicExchange = TopicExchange(MqConstants.USER_EVENT_EXCHANGE)

    /**
     * 文件夹初始化队列绑定用户注册路由键
     */
    @Bean
    fun folderInitBinding(folderInitQueue: Queue, userEventExchange: TopicExchange): Binding {
        return BindingBuilder
            .bind(folderInitQueue)
            .to(userEventExchange)
            .with(MqConstants.USER_REGISTER_ROUTING_KEY)
    }

}