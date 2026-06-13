package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 收藏关系实体
 * @Date 2026/2/4
 */
@TableName("\"photo_collection_photo\"")
data class CollectionPhoto(
    /** 收藏关系ID */
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    /** 收藏夹ID */
    var collectionId: Long,
    /** 图片ID */
    var photoId: Long,
    /** 用户ID */
    var userId: Long,

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: Instant? = null,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    var createdAt: Instant? = null
)
