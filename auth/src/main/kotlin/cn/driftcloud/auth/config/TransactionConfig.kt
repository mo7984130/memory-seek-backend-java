package cn.driftcloud.auth.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/21
 */
@Configuration
class TransactionConfig {

    @Bean("writeTransactionTemplate")
    fun writeTransactionTemplate(manager: PlatformTransactionManager): TransactionTemplate {
        return TransactionTemplate(manager)
    }

    @Bean("readTransactionTemplate")
    fun readTransactionTemplate(manager: PlatformTransactionManager): TransactionTemplate {
        return TransactionTemplate(manager).apply {
            isReadOnly = true
            propagationBehavior = TransactionDefinition.PROPAGATION_SUPPORTS
        }
    }
}