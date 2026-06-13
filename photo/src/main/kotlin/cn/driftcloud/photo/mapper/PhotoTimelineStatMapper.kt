package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.entity.PhotoTimelineStat
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/27
 */
@Mapper
interface PhotoTimelineStatMapper : BaseMapper<PhotoTimelineStat> {

    /**
     * 增加指定日期的照片数量
     * @param dateStr 日期字符串，格式为yyyy-MM
     * @param anchorTime 照片时间, 用于确定照片的时间范围
     */
    @Insert("""
        INSERT INTO photo_timeline_stat (date_str, count, anchor_time, created_at, updated_at)
        VALUES (#{dateStr}, 1, #{anchorTime}, NOW(), NOW())
        ON CONFLICT (date_str) 
        DO UPDATE SET
            count = photo_timeline_stat.count + 1,
            anchor_time = GREATEST(photo_timeline_stat.anchor_time, EXCLUDED.anchor_time),
            updated_at = NOW()
    """)
    fun incrCount(@Param("dateStr") dateStr: String, @Param("anchorTime") anchorTime: Instant)

}