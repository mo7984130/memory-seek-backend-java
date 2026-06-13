package cn.driftcloud.photo.pojo

import java.time.Instant

/**
 * @author DriftCloud
 * @Description 收藏夹照片VO
 * @Date 2026/2/4
 */
data class CollectionPhotoVO(
    val photo: PhotoVO,
    val collectedAt: Instant
)
