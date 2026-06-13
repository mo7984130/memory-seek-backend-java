package cn.driftcloud.photo.config.typehandler

import org.apache.ibatis.type.BaseTypeHandler
import org.apache.ibatis.type.JdbcType
import org.apache.ibatis.type.MappedTypes
import java.sql.CallableStatement
import java.sql.PreparedStatement
import java.sql.ResultSet

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/16
 */
@MappedTypes(List::class)
class ListTypeHandler : BaseTypeHandler<List<Long>>() {

    override fun setNonNullParameter(ps: PreparedStatement, i: Int, parameter: List<Long>, jdbcType: JdbcType?) {
        // 核心：使用连接对象创建一个 SQL Array
        val array = ps.connection.createArrayOf("bigint", parameter.toTypedArray())
        ps.setArray(i, array)
    }

    override fun getNullableResult(rs: ResultSet, columnName: String): List<Long>? {
        return extractList(rs.getArray(columnName))
    }

    override fun getNullableResult(rs: ResultSet, columnIndex: Int): List<Long>? {
        return extractList(rs.getArray(columnIndex))
    }

    override fun getNullableResult(cs: CallableStatement, columnIndex: Int): List<Long>? {
        return extractList(cs.getArray(columnIndex))
    }

    private fun extractList(sqlArray: java.sql.Array?): List<Long>? {
        if (sqlArray == null) return null
        val array = sqlArray.array as Array<*>
        return array.map { (it as Number).toLong() }
    }
}