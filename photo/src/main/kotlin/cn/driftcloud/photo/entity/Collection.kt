package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 收藏夹实体
 * @Date 2026/2/4
 */
@TableName("\"photo_collection\"")
data class Collection(
    /** 收藏夹ID */
    @TableId(type = IdType.AUTO)
    var id: Long? = null,
    /** 用户ID */
    var userId: Long,
    /** 收藏夹名称 */
    var name: String,
    /** 收藏夹详细描述 */
    var description: String? = null,
    /** 收藏夹下的图片总数 */
    var photoCount: Long = 0,
    /** 收藏夹封面图ID */
    var coverImageId: Long? = null,
    /** 是否为我喜欢 */
    var isFavorite: Boolean = false,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    var createdAt: Instant? = null,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: Instant? = null
)
