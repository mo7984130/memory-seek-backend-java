package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.entity.Collection
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/4
 */
@Mapper
interface CollectionMapper : BaseMapper<Collection> {

    /**
     * 根据用户ID查询收藏夹列表
     * @param userId 用户ID
     * @return 收藏夹列表
     */
    @Select("SELECT * FROM photo_collection WHERE user_id = #{userId}")
    fun selectByUserId(@Param("userId") userId: Long): List<Collection>

    /**
     * 原子增加计数器，并同步更新 updated_at
     */
    @Update("""
        UPDATE photo_collection 
        SET photo_count = photo_count + 1
        WHERE id = #{collectionId}
    """)
    fun incrementPhotoCount(@Param("collectionId") collectionId: Long): Int

    /**
     * 原子减少计数器（需保证不小于0），并同步更新 updated_at
     */
    @Update("""
        UPDATE photo_collection 
        SET photo_count = CASE WHEN photo_count > 0 THEN photo_count - 1 ELSE 0 END
        WHERE id = #{collectionId}
    """)
    fun decrementPhotoCount(@Param("collectionId") collectionId: Long): Int

    /**
     * 更新封面图
     */
    @Update("UPDATE photo_collection SET cover_image_id = #{photoId} WHERE id = #{collectionId}")
    fun updateCover(@Param("collectionId") collectionId: Long, @Param("photoId") photoId: Long): Int

    /**
     * 根据ID和用户ID删除收藏夹
     */
    @Update("DELETE FROM photo_collection WHERE id = #{collectionId} AND user_id = #{userId}")
    fun deleteByIdAndUserId(@Param("collectionId") collectionId: Long, @Param("userId") userId: Long): Int

    /**
     * 根据用户ID查询喜欢收藏夹
     * @param userId 用户ID
     * @return 喜欢收藏夹
     */
    @Select("SELECT * FROM photo_collection WHERE user_id = #{userId} AND is_favorite = true LIMIT 1")
    fun findFavorite(@Param("userId") userId: Long): Collection?

    /**
     * 根据用户ID和图片ID列表查询喜欢的图片ID
     * @param userId 用户ID
     * @param photoIds 图片ID列表
     * @return 喜欢的图片ID列表
     */
    @Select("""
        <script>
            SELECT photo_id
            FROM photo_collection_photo
            WHERE user_id = #{userId}
            AND collection_id = #{collectionId}
            AND photo_id IN
            <foreach collection="photoIds" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
        </script>
    """)
    fun findCollectedIds(
        @Param("userId") userId: Long,
        @Param("collectionId") collectionId: Long,
        @Param("photoIds") photoIds: kotlin.collections.Collection<Long>
    ): List<Long>
}