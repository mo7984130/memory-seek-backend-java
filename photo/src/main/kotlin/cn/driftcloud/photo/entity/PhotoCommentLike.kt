package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 照片评论点赞
 * @Date 2026/2/11
 */
@TableName("photo_comment_like")
data class PhotoCommentLike(
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    val id: Long? = null,
    /** 照片评论ID */
    val commentId: Long,
    /** 用户ID */
    val userId: Long,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    val createdAt: Instant? = null,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val updatedAt: Instant? = null
)