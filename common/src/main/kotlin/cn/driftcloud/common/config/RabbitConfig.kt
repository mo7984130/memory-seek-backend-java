package cn.driftcloud.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/5
 */
@Configuration
@ConditionalOnClass(MessageConverter::class)
class RabbitConfig {

    /**
     * 配置消息转换器
     */
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }

}