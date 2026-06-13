package cn.driftcloud.photo.pojo

import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/31
 */
data class PhotoTimelineStatVO(
    val dateStr: String,
    val count: Int,
    val anchorTime: Instant
)