package cn.driftcloud.photo.controller

import cn.driftcloud.common.response.R
import cn.driftcloud.photo.pojo.PhotoTimelineStatVO
import cn.driftcloud.photo.service.PhotoTimeStatService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/31
 */
@RestController
@RequestMapping("/photo-timeline-stat")
class PhotoTimelineStatController(
    private val service: PhotoTimeStatService
) {

    /**
     * 获取照片时间线统计
     */
    @GetMapping
    fun getPhotoTimelineStat(): R<List<PhotoTimelineStatVO>> {
        val states = service.getPhotoTimeStat()
        return R.ok(states)
    }

}