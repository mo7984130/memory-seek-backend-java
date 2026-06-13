package cn.driftcloud.auth.config

import cn.driftcloud.common.constant.MqConstants
import org.springframework.amqp.core.TopicExchange
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/5
 */
@Configuration
class UserRabbitConfig {
    @Bean
    fun userRegisterExchange(): TopicExchange = TopicExchange(MqConstants.USER_EVENT_EXCHANGE)
}