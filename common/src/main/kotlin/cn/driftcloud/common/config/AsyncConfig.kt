package cn.driftcloud.common.config

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
@Configuration
@EnableAsync
class AsyncConfig {

    @Bean
    fun asyncExecutor(): Executor {
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val poolSize = cpuCores * 2
        val queueCap = 500
        val threadNamePrefix = "Async-"

        return ThreadPoolTaskExecutor().apply {
            corePoolSize = poolSize
            maxPoolSize = poolSize
            queueCapacity = queueCap
            setThreadNamePrefix(threadNamePrefix)
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(60)
            initialize()
        }
    }

}