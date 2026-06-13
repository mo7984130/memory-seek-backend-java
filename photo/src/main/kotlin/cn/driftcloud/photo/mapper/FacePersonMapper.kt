package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.config.typehandler.VectorTypeHandler
import cn.driftcloud.photo.entity.FacePerson
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.*

/**
 * 结果集映射：用于混合检索返回 ID 和 最小距离
 */
data class PersonDistanceResult(
    val id: Long,
    val distance: Float
)

data class FacePersonIdNamePair(
    val id: Long,
    val name: String
)

@Mapper
interface FacePersonMapper : BaseMapper<FacePerson> {

    /**
     * 1. 双轨制混合检索
     */
    @Select("""
        SELECT 
            p.id, 
            LEAST(
                (p.centroid_embedding <=> #{embedding, typeHandler=cn.driftcloud.photo.config.typehandler.VectorTypeHandler}),
                (f.embedding <=> #{embedding, typeHandler=cn.driftcloud.photo.config.typehandler.VectorTypeHandler})
            ) as distance
        FROM photo_face_person p
        LEFT JOIN photo_face_feature f ON p.max_score_feature_id = f.id
        WHERE p.total_weight_count > 0
        ORDER BY distance ASC
        LIMIT 1
    """)
    fun findNearestPersonHybrid(@Param("embedding") embedding: FloatArray): PersonDistanceResult?

    /**
     * 2. 原子更新重心 (重写了 unnest 逻辑，解决 ORDER BY 语法错误)
     * 使用 ARRAY(SELECT ... FROM unnest(...) ORDER BY ...) 的方式确保维度顺序正确
     */
    @Update("""
        UPDATE photo_face_person 
        SET 
            centroid_embedding = CASE 
                WHEN total_weight_count = 0 THEN #{embedding}::vector
                ELSE (
                    SELECT ARRAY(
                        SELECT (v_old * total_weight_count + v_new * #{score}) / (total_weight_count + #{score})
                        FROM unnest(centroid_embedding::float4[], #{embedding}::float4[]) WITH ORDINALITY AS t(v_old, v_new, ord)
                        ORDER BY ord
                    )::vector
                )
            END,
            total_photo_count = total_photo_count + 1,
            total_weight_count = total_weight_count + #{score},
            max_score_feature_id = CASE 
                WHEN #{score} > max_score THEN #{featureId} 
                ELSE max_score_feature_id 
            END,
            max_score = GREATEST(max_score, #{score}),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
    """)
    fun atomicUpdatePerson(
        @Param("id") id: Long,
        @Param("embedding") embedding: FloatArray,
        @Param("score") score: Float,
        @Param("featureId") featureId: Long
    ): Int

    /**
     * 3. 仅增加照片计数 (不污染重心)
     */
    @Update("""
        UPDATE photo_face_person 
        SET 
            total_photo_count = total_photo_count + 1, 
            updated_at = CURRENT_TIMESTAMP 
        WHERE id = #{id}
    """)
    fun incrementPhotoCount(@Param("id") id: Long): Int

    /**
     * 4. 明星脸独立更新检查
     */
    @Update("""
        UPDATE photo_face_person 
        SET 
            max_score_feature_id = #{featureId},
            max_score = #{score},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND #{score} > max_score
    """)
    fun updateMaxScoreFeatureIfBetter(
        @Param("id") id: Long,
        @Param("score") score: Float,
        @Param("featureId") featureId: Long
    ): Int

    /**
     * 5. 标准查询
     */
    @Select("SELECT * FROM photo_face_person WHERE id = #{id}")
    @Results(
        Result(column = "centroid_embedding", property = "centroidEmbedding",
            typeHandler = VectorTypeHandler::class)
    )
    fun selectByPersonId(@Param("id") id: Long): FacePerson?

    @Select("""
        <script>
        SELECT id, name FROM photo_face_person 
        WHERE id IN 
        <foreach item='id' collection='ids' open='(' separator=',' close=')'>
            #{id}
        </foreach>
        </script>
    """)
    fun selectIdNameByIds(@Param("ids") ids: List<Long>): List<FacePersonIdNamePair>

    @Select("SELECT id, name FROM photo_face_person")
    fun selectListIdName(): List<FacePersonIdNamePair>

    @Update("""
    UPDATE photo_face_person 
    SET 
        centroid_embedding = #{centroid, typeHandler=cn.driftcloud.photo.config.typehandler.VectorTypeHandler},
        total_weight_count = #{weight},
        total_photo_count = #{count},
        max_score = #{maxScore},
        max_score_feature_id = #{maxScoreId},
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{id}
""")
    fun manualUpdateStats(
        @Param("id") id: Long,
        @Param("centroid") centroid: FloatArray,
        @Param("weight") weight: Float,
        @Param("count") count: Int,
        @Param("maxScore") maxScore: Float,
        @Param("maxScoreId") maxScoreId: Long
    )

    @Select("SELECT * FROM photo_face_person WHERE id = #{id} FOR UPDATE")
    @Results(
        Result(column = "centroid_embedding", property = "centroidEmbedding",
            typeHandler = VectorTypeHandler::class)
    )
    fun selectForUpdate(@Param("id") id: Long): FacePerson?

    @Insert("""
    <script>
    INSERT INTO photo_face_person (
        name, 
        max_score_feature_id, 
        centroid_embedding, 
        total_photo_count,
        total_weight_count
    )
    VALUES 
    <foreach collection="list" item="item" separator=",">
        (
            #{item.name}, 
            #{item.maxScoreFeatureId}, 
            #{item.centroidEmbedding, typeHandler=cn.driftcloud.photo.config.typehandler.VectorTypeHandler},
            #{item.totalPhotoCount},
            #{item.totalWeightCount}
        )
    </foreach>
    </script>
""")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insertBatch(@Param("list") list: List<FacePerson>): Int
}