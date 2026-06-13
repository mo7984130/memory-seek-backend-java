package cn.driftcloud.photo.pojo

import cn.driftcloud.photo.entity.Collection
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 收藏夹信息VO
 * @Date 2026/2/5
 */
data class CollectionVO(
    /** 收藏夹ID */
    val id: String,
    /** 收藏夹名称 */
    val name: String,
    /** 收藏夹描述 */
    val description: String? = null,
    /** 收藏夹照片数量 */
    val photoCount: Long = 0,
    /** 收藏夹封面图片URL */
    val coverImageUrl: String? = null,
    /** 收藏夹创建时间 */
    val createdAt: Instant?
)

fun Collection.toVO(
    coverImageUrl: String?
): CollectionVO {
    return CollectionVO(
        id = id.toString(),
        name = name,
        description = description,
        photoCount = photoCount,
        coverImageUrl = coverImageUrl,
        createdAt = createdAt
    )
}
