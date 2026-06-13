package cn.driftcloud.photo.entity

import cn.driftcloud.photo.config.typehandler.JsonbTypeHandler
import cn.driftcloud.photo.config.typehandler.VectorTypeHandler
import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 面部特征
 * @Date 2026/2/13
 */
@TableName("photo_face_feature", autoResultMap = true)
data class FaceFeature(
    /** ID */
    @TableId(type = IdType.AUTO)
    val id: Long? = null,
    /** 图片ID */
    val photoId: Long,
    /** 人物ID */
    var personId: Long?,
    /** 特征向量 */
    @TableField(typeHandler = VectorTypeHandler::class)
    val embedding: FloatArray,
    /** 边框 */
    @TableField(typeHandler = JsonbTypeHandler::class)
    val bbox: FaceBBox,
    /** 置信度 */
    val score: Float,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    val createdAt: Instant? = null,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val updatedAt: Instant? = null
)

data class FaceBBox(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var w: Float = 0.0f,
    var h: Float = 0.0f
)