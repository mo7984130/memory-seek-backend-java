package cn.driftcloud.photo.config.typehandler

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedJdbcTypes
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
@MappedTypes(FloatArray::class)
@MappedJdbcTypes(JdbcType.OTHER)
class VectorTypeHandler: BaseTypeHandler<FloatArray>() {
    override fun setNonNullParameter(
        ps: PreparedStatement,
        i: Int,
        parameter: FloatArray,
        jdbcType: JdbcType?
    ) {
        val vectorStr = parameter.joinToString(prefix = "[", postfix = "]", separator = ",")
        ps.setObject(i, vectorStr, Types.OTHER)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String?): FloatArray? {
        val str = rs.getString(columnName) ?: return null
        return str.removeSurrounding("[", "]").split(",").map { it.toFloat() }.toFloatArray()
    }

    override fun getNullableResult(rs: ResultSet?, columnIndex: Int): FloatArray? = null

    override fun getNullableResult(cs: CallableStatement?, columnIndex: Int): FloatArray? = null
}