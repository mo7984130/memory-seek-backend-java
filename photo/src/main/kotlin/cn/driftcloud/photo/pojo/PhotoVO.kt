package cn.driftcloud.photo.pojo

import cn.driftcloud.photo.entity.Photo
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/22
 */
data class PhotoVO(
    val id: String,
    val name: String,
    val thumbnailUrl: String,
    val previewUrl: String,
    val originalUrl: String,
    val createdAt: Instant?,
    val width: Int?,
    val height: Int?,
    var isFavorited: Boolean = false,
    var isCollected: Boolean = false
)

fun Photo.toVO(
    thumbnailUrl: String,
    previewUrl: String,
    originalUrl: String
): PhotoVO {
    return PhotoVO(
        id = this.id.toString(),
        name = this.name ?: "未知",
        thumbnailUrl = thumbnailUrl,
        previewUrl = previewUrl,
        originalUrl = originalUrl,
        createdAt = this.createdAt,
        width = this.width,
        height = this.height
    )
}
