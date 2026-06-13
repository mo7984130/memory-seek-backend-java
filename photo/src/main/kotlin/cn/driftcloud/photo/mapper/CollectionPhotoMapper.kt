package cn.driftcloud.photo.mapper

import cn.driftcloud.photo.entity.CollectionPhoto
import cn.driftcloud.photo.entity.dto.CollectionPhotoIdPair
import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/8
 */
@Mapper
interface CollectionPhotoMapper: BaseMapper<CollectionPhoto> {

    /**
     * 删除收藏关系
     * 必须传入 userId 确保只能删除自己的收藏
     */
    @Delete("""
        DELETE FROM photo_collection_photo 
        WHERE collection_id = #{collectionId} 
          AND photo_id = #{photoId} 
          AND user_id = #{userId}
    """)
    fun deletePhotoByLogic(@Param("collectionId") collectionId: Long, @Param("photoId") photoId: Long, @Param("userId") userId: Long): Int

    /**
     * 分页获取收藏夹中的照片ID
     * @param collectionId 收藏夹ID
     * @param cursor 分页游标
     * @param limit 分页限制
     * @return List<CollectionPhoto>
     */
    @Select("""
        <script>
            SELECT *
            FROM photo_collection_photo 
            WHERE collection_id = #{collectionId}
            <if test="cursor != null">
                <![CDATA[ AND created_at < #{cursor} ]]>
            </if>
            ORDER BY created_at DESC
            LIMIT #{limit}
        </script>
    """)
    fun findPagePhotosByCollectionId(@Param("collectionId") collectionId: Long, @Param("cursor") cursor: Instant?, @Param("limit") limit: Int): List<CollectionPhoto>

    /**
     * 批量查询多个文件夹各自最新的一张照片 ID
     * 使用 PostgreSQL 特有的 DISTINCT ON 提高性能
     */
    @Select("""
        <script>
            SELECT DISTINCT ON (collection_id) collection_id, photo_id
            FROM photo_collection_photo
            WHERE collection_id IN 
            <foreach item='id' collection='collectionIds' open='(' separator=',' close=')'>
                #{id}
            </foreach>
            ORDER BY collection_id, created_at DESC
        </script>
    """)
    fun findLatestPhotoIdBatch(@Param("collectionIds") collectionIds: List<Long>): List<CollectionPhotoIdPair>

    /**
     * 根据photoIds, 获取是否被收藏
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
    fun findCollectedPhotoIds(
        @Param("userId") userId: Long,
        @Param("collectionId") collectionId: Long,
        @Param("photoIds") photoIds: List<Long>
    ): List<Long>
}