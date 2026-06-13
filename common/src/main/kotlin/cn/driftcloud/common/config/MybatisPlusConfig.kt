package cn.driftcloud.common.config

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import org.apache.ibatis.reflection.MetaObject
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/19
 */
@Component
@ConditionalOnClass(MetaObjectHandler::class)
class MybatisPlusConfig: MetaObjectHandler {
    override fun insertFill(metaObject: MetaObject?) {
        this.strictInsertFill(metaObject, "createdAt", Instant::class.java, Instant.now())
        this.strictInsertFill(metaObject, "updatedAt", Instant::class.java, Instant.now())
    }

    override fun updateFill(metaObject: MetaObject?) {
        this.strictUpdateFill(metaObject, "updatedAt", Instant::class.java, Instant.now())
    }
}