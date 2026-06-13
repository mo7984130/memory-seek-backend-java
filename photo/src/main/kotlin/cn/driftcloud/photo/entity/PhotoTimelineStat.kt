package cn.driftcloud.photo.entity

import com.baomidou.mybatisplus.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 照片时间线统计表实体类
 * @Date 2026/1/27
 */
@TableName("\"photo_timeline_stat\"")
data class PhotoTimelineStat(
    /** 时间线数据字符串 */
    @TableId(type = IdType.INPUT)
    val dateStr: String,
    /** 照片数量 */
    val count: Int,
    /** 最新一张照片时间锚点 */
    val anchorTime: Instant,
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    val updatedAt: Instant? = null,
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    val createdAt: Instant? = null,
)
