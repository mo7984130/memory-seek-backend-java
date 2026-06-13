package cn.driftcloud.photo.service

import cn.driftcloud.photo.entity.PhotoTimelineStat
import cn.driftcloud.photo.mapper.PhotoTimelineStatMapper
import cn.driftcloud.photo.pojo.PhotoTimelineStatVO
import cn.driftcloud.photo.pojo.PhotoVO
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/27
 */
@Service
class PhotoTimeStatService()
    : ServiceImpl<PhotoTimelineStatMapper, PhotoTimelineStat>()
{

    private val dateFormater = DateTimeFormatter.ofPattern("YYYY-MM").withZone(ZoneId.of("Asia/Shanghai"))
    private val logger = LoggerFactory.getLogger(PhotoTimeStatService::class.java)

    /**
     * 增加照片时间统计
     * @param photoVO 照片VO
     */
    fun addPhotoTimeStat(
        photoVO: PhotoVO
    ) {
        if (photoVO.createdAt == null) {
            logger.warn("Photo createdAt is null, photoVO: {}", photoVO)
            return
        }

        val dateStr = dateFormater.format(photoVO.createdAt)
        baseMapper.incrCount(dateStr, photoVO.createdAt)
    }

    /**
     * 获取照片时间线统计
     */
    fun getPhotoTimeStat(): List<PhotoTimelineStatVO> {
        val entities = baseMapper.selectList(KtQueryWrapper(PhotoTimelineStat::class.java).orderByDesc(PhotoTimelineStat::dateStr))
        return entities.map {
            PhotoTimelineStatVO(
                dateStr = it.dateStr,
                count = it.count,
                anchorTime = it.anchorTime
            )
        }
    }

}