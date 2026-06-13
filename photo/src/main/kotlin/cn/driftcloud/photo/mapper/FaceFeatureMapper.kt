package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.config.typehandler.JsonbTypeHandler
import cn.driftcloud.photo.config.typehandler.VectorTypeHandler
import cn.driftcloud.photo.entity.FaceBBox
import cn.driftcloud.photo.entity.FaceFeature
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update


// 用于聚类计算的轻量载体
data class FeatureNode(
    val id: Long = -1,
    var personId: Long? = null,
    // 如果你的数据库 embedding 是 jsonb/vector 类型，且你有 TypeHandler
    @TableField(typeHandler = VectorTypeHandler::class)
    val embedding: FloatArray = floatArrayOf(),
    val score: Float = 0.0f,
    val photoId: Long = -1
)

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
@Mapper
interface FaceFeatureMapper : BaseMapper<FaceFeature> {
    @Update("""
        UPDATE photo_face_feature 
        SET person_id = #{personId}, updated_at = CURRENT_TIMESTAMP
        WHERE id = ANY(#{featureIds, typeHandler=cn.driftcloud.photo.config.typehandler.ListTypeHandler})
    """)
    fun batchUpdatePersonId(
        @Param("personId") personId: Long,
        @Param("featureIds") featureIds: List<Long>
    ): Int

    fun selectSimpleFaceFeatureByFeatureIds(@Param("ids") ids: List<Long>): List<SimpleFaceFeature>

    fun selectAllEmbeddingsWithPersonId(): List<FeatureNode>

    /**
     * 使用 PostgreSQL 的 UPDATE FROM VALUES 语法进行批量关联更新
     * 这种方式比循环更新快几个数量级
     */
    @Update("""
        <script>
        UPDATE photo_face_feature AS f
        SET 
            person_id = v.new_p_id
        FROM (VALUES 
            <foreach collection="list" item="item" separator=",">
                (#{item.first}::bigint, #{item.second}::bigint)
            </foreach>
        ) AS v(f_id, new_p_id)
        WHERE f.id = v.f_id
        </script>
    """)
    fun batchUpdatePersonIdByPair(@Param("list") list: List<Pair<Long, Long>>): Int

    /**
     * 获取指定人物下分数最高的一条人脸特征
     * 用于在手动移出人脸后，重新校准人物的封面图（MaxScore）
     */
    @Select("""
        SELECT id, score 
        FROM photo_face_feature 
        WHERE person_id = #{personId} 
        ORDER BY score DESC 
        LIMIT 1
    """)
    fun selectMaxScoreFeatureByPersonId(personId: Long): FaceFeature?
}

data class SimpleFaceFeature(
    val id: Long = -1,
    val photoId: Long = -1,
    @TableField(typeHandler = JsonbTypeHandler::class)
    val bbox: FaceBBox = FaceBBox()
)