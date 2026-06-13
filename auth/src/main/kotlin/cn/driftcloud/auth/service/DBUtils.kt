package cn.driftcloud.auth.service

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/21
 */
@Component
class DBUtils(
    @param:Qualifier("writeTransactionTemplate") private val writeTemplate: TransactionTemplate,
    @param:Qualifier("readTransactionTemplate") private val readTemplate: TransactionTemplate
) {
    fun <T> read(block: () -> T): T? = readTemplate.execute { block() }
    fun <T> write(block: () -> T): T = writeTemplate.execute { block() }!!
}