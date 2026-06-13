package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 照片评论
 * @Date 2026/2/11
 */
@TableName("photo_comment")
data class PhotoComment(
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    /** 照片ID */
    val photoId: Long,
    /** 评论者用户ID */
    val userId: Long,
    /** 评论内容 */
    val content: String,
    /** 点赞总数 */
    val likeCount: Int,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    var createdAt: Instant? = null,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: Instant? = null
)
