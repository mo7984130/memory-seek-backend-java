package cn.driftcloud.photo.entity

import cn.driftcloud.photo.config.typehandler.VectorTypeHandler
import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 人脸信息
 * @Date 2026/2/13
 */
@TableName("photo_face_person")
data class FacePerson(
    /** 人物ID */
    @TableId(type = IdType.AUTO)
    val id: Long? = null,
    /** 人物名称 */
    var name: String,
    /** 最大置信度特征ID */
    var maxScoreFeatureId: Long = -1,
    /** 最大置信度 */
    var maxScore: Float = 0.0f,
    /** 总图片数 */
    var totalPhotoCount: Long = 0,
    /** 人物描述 */
    var totalWeightCount: Float = 0.0f,
    /** 中心特征向量 */
    @TableField(typeHandler = VectorTypeHandler::class)
    var centroidEmbedding: FloatArray = FloatArray(512),
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    val createdAt: Instant? = null,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val updatedAt: Instant? = null
)
