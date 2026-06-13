package cn.driftcloud.photo.config.typehandler

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.PreparedStatement
import java.sql.Types

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/14
 */
@MappedJdbcTypes(JdbcType.VARCHAR, JdbcType.OTHER)
@MappedTypes(Any::class)
class JsonbTypeHandler(type: Class<*>) : JacksonTypeHandler(type) {

    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: Any, jdbcType: JdbcType?) {
        val json = toJson(parameter)
        ps.setObject(i, json, Types.OTHER)
    }
}