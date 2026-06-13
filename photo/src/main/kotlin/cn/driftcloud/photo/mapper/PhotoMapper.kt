package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.entity.Photo
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/21
 */
@Mapper
interface PhotoMapper : BaseMapper<Photo> {

    // 自定义一个简单的 ID 过滤查询
    @Select("SELECT * FROM photo_photo WHERE id > #{lastId} ORDER BY id ASC LIMIT #{limit}")
    fun findPhotosForProcessing(@Param("lastId") lastId: Long, @Param("limit") limit: Int): List<Photo>
}