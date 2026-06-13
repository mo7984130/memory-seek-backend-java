package cn.driftcloud.common.configuration

import cn.driftcloud.common.aspect.RateLimitAspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
@Configuration
class RateLimitAutoConfiguration {

    // 情况 A：有 Redis，正常初始化
    @Configuration
    @ConditionalOnClass(StringRedisTemplate::class)
    class RateLimitEnableConfiguration {
        @Bean
        fun rateLimitAspect(redisTemplate: StringRedisTemplate): RateLimitAspect {
            return RateLimitAspect(redisTemplate)
        }
    }

    // 情况 B：没有 Redis，抛出警告或异常
    @Configuration
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    class RateLimitFailureConfiguration {

        @Bean
        fun rateLimitFailureReport(): Any {
            throw IllegalStateException("检测到使用了限流注解，但项目中未配置 Redis 依赖！\n" +
                    "请检查是否导入了 spring-boot-starter-data-redis。\n")
        }
    }
}