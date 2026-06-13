package cn.driftcloud.photo.entity.dto

/**
 * @author DriftCloud
 * @Description 数据库表 photo_collection_photo 的实体类, 表示收藏夹与图片的关联关系
 * @Date 2026/2/4
 */
data class CollectionPhotoIdPair(
    /**
     * 收藏夹ID
     */
    val collectionId: Long,
    /**
     * 图片ID
     */
    val photoId: Long
)