package cn.driftcloud.photo.pojo

import cn.driftcloud.photo.entity.PhotoComment

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/11
 */
data class PhotoCommentVO (
    val id: String,
    val content: String,
    val userId: String,
    val isLike: Boolean,
    val createdAt: String
)

fun PhotoComment.toVO(
    isLike: Boolean
): PhotoCommentVO {
    return PhotoCommentVO(
        id = id.toString(),
        content = content,
        userId = userId.toString(),
        createdAt = createdAt.toString(),
        isLike = isLike
    )
}