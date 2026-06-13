package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
@TableName("\"photo_photo\"")
data class Photo(
    /** 主键 */
    @TableId(type = IdType.AUTO)
    val id: Long? = null,
    /** 用户ID */
    val userId: Long,
    /** 文件名 */
    val name: String? = null,
    /** 文件大小 单位：字节 */
    val size: Long? = null,
    /** 图片宽度 单位：像素 */
    val width: Int? = null,
    /** 图片高度 单位：像素 */
    val height: Int? = null,
    /** 文件MIME类型 */
    val mimeType: String? = null,
    /** 文件MD5值 */
    val md5: String,
    /** 文件ID */
    val fileId: String,

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val updatedAt: Instant? = null,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val createdAt: Instant? = null,
)
