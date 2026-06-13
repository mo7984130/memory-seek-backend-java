package cn.driftcloud.photo.service

import arrow.core.Either
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.photo.entity.Collection
import cn.driftcloud.photo.entity.CollectionPhoto
import cn.driftcloud.photo.mapper.CollectionMapper
import cn.driftcloud.photo.mapper.CollectionPhotoMapper
import cn.driftcloud.photo.mapper.PhotoMapper
import cn.driftcloud.photo.pojo.CollectionPhotoVO
import cn.driftcloud.photo.pojo.CollectionVO
import cn.driftcloud.photo.pojo.toVO
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/4
 */
@Service
class CollectionService(
    private val collectionMapper: CollectionMapper,
    private val collectionPhotoMapper: CollectionPhotoMapper,
    private val photoMapper: PhotoMapper,
    private val imgProxyService: ImgProxyService,
    private val redisTemplate: StringRedisTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 获取用户的所有收藏夹
     * @param userId 用户ID
     * @return Either<String, List<CollectionVO>>
     */
    fun getCollectionList(userId: Long): Either<String, List<CollectionVO>> {
        // 获取所有收藏夹记录
        val collections = collectionMapper.selectByUserId(userId)
        // 如果没有默认收藏夹, 创建一个
        if (collections.isEmpty()) {
            createFavoriteCollection(userId)
            return getCollectionList(userId)
        }

        // 收藏夹封面照片id (collectionId -> photoId)
        val photoIdMap = mutableMapOf<Long, Long>()

        // 获取设置了封面的收藏夹的封面id
        collections.filter { it.coverImageId != null }.forEach {
            photoIdMap[it.id!!] = it.coverImageId!!
        }

        // 没有设置封面的收藏夹, 默认使用最新一张照片
        val noCoverCollectionIds = collections.filter { it.coverImageId == null }.map { it.id!! }
        if (noCoverCollectionIds.isNotEmpty()) {
            val lastestPairs = collectionPhotoMapper.findLatestPhotoIdBatch(noCoverCollectionIds)
            for (pair in lastestPairs) {
               photoIdMap[pair.collectionId] = pair.photoId
            }
        }

        // 批量查询涉及到的照片
        val allPhotoIds = photoIdMap.values.distinct()
        val photoMap = if (allPhotoIds.isNotEmpty()) {
            photoMapper.selectByIds(allPhotoIds).associateBy { it.id }
        } else emptyMap()

        // 构建结果
        val result = collections.map { collection ->
            val coverImageUrl = if (photoIdMap.containsKey(collection.id!!)) {
                photoMap[photoIdMap[collection.id!!]]?.let { imgProxyService.photoToVO(it).thumbnailUrl }
            } else null

            CollectionVO(
                id = collection.id!!.toString(),
                name = collection.name,
                description = collection.description,
                photoCount = collection.photoCount,
                coverImageUrl = coverImageUrl,
                createdAt = collection.createdAt!!
            )
        }

        return Either.Right(result)
    }

    /**
     * 编辑收藏夹信息
     * @param userId 用户ID
     * @param collectionId 收藏夹ID
     * @param name 收藏夹名称
     * @param description 收藏夹描述
     * @return Either<String, CollectionVO>
     */
    fun editCollectionInfo(userId: Long, collectionId: Long, name: String?, description: String?): Either<String, CollectionVO> {
        val collection = collectionMapper.selectById(collectionId) ?: return Either.Left("未找到该收藏夹")
        if (collection.userId != userId) return Either.Left("无权限")
        if (name != null && name.isNotBlank()) collection.name = name
        if (description != null && description.isNotBlank()) collection.description = description

        val updated = collectionMapper.updateById(collection)
        return if (updated > 0) {
            Either.Right(collection.toVO(null))
        } else {
            Either.Left("更新失败")
        }
    }


    /**
     * 获取收藏夹中的照片
     * @param userId 用户ID
     * @param collectionId 收藏夹ID
     * @param cursor 分页游标
     * @param size 分页大小
     * @return Either<String, PageResponse<FolderPhotoVO, Instant>>
     */
    fun getPhotoFromCollection(userId: Long, collectionId: Long, cursor: Instant?, size: Int): Either<String, CursorPageVO<CollectionPhotoVO, Instant>> {
        // 鉴权, 确定该收藏夹属于该用户
        val collection = collectionMapper.selectById(collectionId) ?: return Either.Left("未找到该收藏夹")
        if (collection.userId != userId) return Either.Left("无权限")

        // 查询
        val collectionPhotos = collectionPhotoMapper.findPagePhotosByCollectionId(collectionId, cursor, size + 1)
        if (collectionPhotos.isEmpty()) return Either.Right(CursorPageVO(emptyList(), null, false))

        val hasMore = collectionPhotos.size > size
        val resultCollectionPhotos = if (hasMore) collectionPhotos.take(size) else collectionPhotos
        val resultCollectionPhotoIds = resultCollectionPhotos.map { it.photoId }

        // 查询是否喜欢
        val favoriteCollectionId = getFavoriteCollectionId(userId)
        val favoritePhotoId = collectionPhotoMapper.findCollectedPhotoIds(userId, collectionId, resultCollectionPhotoIds).toSet()

        // 获取照片详情
        val photos = photoMapper.selectByIds(resultCollectionPhotoIds)
        val photoMap = photos.associateBy { it.id }

        val result = resultCollectionPhotos.mapNotNull { collectionPhoto ->
            val photo = photoMap[collectionPhoto.photoId]
            if (photo != null) CollectionPhotoVO(
                photo = imgProxyService.photoToVO(photo).apply { isFavorited = favoritePhotoId.contains(photo.id) },
                collectedAt = collectionPhoto.createdAt!!
            ) else {
                logger.warn("收藏夹Id为 {} 照片Id为 {} 的照片在数据库中不存在", collectionId, collectionPhoto.photoId)
                null
            }
        }
        val nextCursor = if (hasMore) resultCollectionPhotos.last().createdAt else null
        return Either.Right(CursorPageVO(result, nextCursor, hasMore))
    }

    /**
     * 将照片添加到收藏夹
     * @param userId 用户ID
     * @param collectionId 收藏夹ID
     * @param photoId 照片ID
     * @return Either<String, Unit>
     */
    @Transactional
    fun addPhotoToCollection(userId: Long, collectionId: Long, photoId: Long): Either<String, Unit> {
        // 鉴权, 保证 userId 和 folderId 相匹配
        val collection = collectionMapper.selectById(collectionId) ?: return Either.Left("未找到该收藏夹")
        if (collection.userId != userId) return Either.Left("无权限")

        // 插入记录
        val relation = CollectionPhoto(
            collectionId = collectionId,
            photoId = photoId,
            userId = userId
        )
        try {
            collectionPhotoMapper.insert(relation)

            // 更新收藏夹照片计数
            collectionMapper.incrementPhotoCount(collectionId)
        } catch (_: DuplicateKeyException) {
            logger.warn("用户Id为 {} 的用户尝试将照片Id为 {} 的照片添加到收藏夹Id为 {} 的收藏夹中，但该照片已存在", userId, photoId, collectionId)
            return Either.Left("已存在")
        }

        return Either.Right(Unit)
    }

    /**
     * 从收藏夹中移除照片
     * @param userId 用户ID
     * @param collectionId 收藏夹ID
     * @param photoId 照片ID
     * @return Either<String, Unit>
     */
    @Transactional
    fun removePhotoFromCollection(userId: Long, collectionId: Long, photoId: Long): Either<String, Unit> {
        // 1. 删除关联记录
        val deletedRows = collectionPhotoMapper.deletePhotoByLogic(collectionId, photoId, userId)

        return if (deletedRows > 0) {
            // 2. 只有删除成功时，才减少计数器
            collectionMapper.decrementPhotoCount(collectionId)
            Either.Right(Unit)
        } else {
            logger.warn("用户Id为 {} 的用户尝试从收藏夹Id为 {} 的收藏夹中移除照片Id为 {} 的照片，但该照片不存在", userId, collectionId, photoId)
            Either.Left("未找到该收藏关系或无权限")
        }
    }

    /**
     * 创建收藏夹
     * @param userId 用户ID
     * @param name 收藏夹名称
     * @param description 收藏夹描述
     * @return Either<String, FolderVO>
     */
    @Transactional
    fun createCollection(userId: Long, name: String, description: String? = null): Either<String, CollectionVO> {
        val collection = Collection(
            userId = userId,
            name = name,
            description = description,
            photoCount = 0,
            coverImageId = null
        )
        val row = collectionMapper.insert(collection)
        return if (row > 0) {
            Either.Right(collection.toVO(null))
        } else {
            logger.warn("用户Id为 {} 的用户尝试创建收藏夹名称为 {} 的收藏夹，但创建失败", userId, name)
            Either.Left("创建失败")
        }
    }

    /**
     * 删除收藏夹
     * @param userId 用户ID
     * @param collectionId 收藏夹ID
     * @return Either<String, Unit>
     */
    @Transactional
    fun deleteCollection(userId: Long, collectionId: Long): Either<String, Unit> {
        // 鉴权
        val collection = collectionMapper.selectById(collectionId) ?: return Either.Left("未找到该收藏夹")
        if (collection.userId != userId) return Either.Left("无权限")

        // 我喜欢不可删除
        if (collection.isFavorite) return Either.Left("我喜欢不可删除")

        // 清理照片记录
        collectionMapper.deleteByIdAndUserId(collectionId, userId)

        // 删除收藏夹记录
        val deleted = collectionMapper.deleteByIdAndUserId(collectionId, userId)

        return if (deleted > 0) {
            Either.Right(Unit)
        } else {
            logger.warn("用户Id为 {} 的用户尝试删除收藏夹Id为 {} 的收藏夹，但删除失败", userId, collectionId)
            Either.Left("删除失败")
        }
    }

    /**
     * 根据照片ID查找收藏夹ID
     * @param userId 用户ID
     * @param photoId 照片ID
     * @return Either<String, List<Long>>
     */
    fun findCollectionIdsByPhotoId(userId: Long, photoId: Long): Either<String, List<String>> {
        // 1. 使用 selectList 获取所有关联记录
        val ids = collectionPhotoMapper.selectObjs<Number>(
            KtQueryWrapper(CollectionPhoto::class.java)
                .eq(CollectionPhoto::userId, userId)
                .eq(CollectionPhoto::photoId, photoId)
                .select(CollectionPhoto::collectionId)
        ).map { it.toLong().toString() }

        return Either.Right(ids)
    }

    /**
     * 创建我喜欢
     * @param userId 用户ID
     * @return Folder
     */
    @Transactional
    fun createFavoriteCollection(userId: Long): Collection {
        val existing = collectionMapper.findFavorite(userId)
        if (existing != null) return existing

        val favorite = Collection(
            userId = userId,
            name = "我喜欢",
            description = "喜欢收藏夹",
            photoCount = 0,
            coverImageId = null,
            isFavorite = true
        )
        collectionMapper.insert(favorite)
        return favorite
    }

    /**
     * 获取我喜欢收藏夹
     * @param userId 用户ID
     * @return Collection
     */
    fun getFavoriteCollection(userId: Long): Collection {
        val favorite = collectionMapper.findFavorite(userId)
        if (favorite == null) {
            logger.warn("用户Id为 {} 的用户尝试获取喜欢收藏夹，但未找到", userId)
            throw RuntimeException("未找到该用户的喜欢收藏夹")
        }
        return favorite
    }

    /**
     * 获取我喜欢收藏夹的ID
     * @param userId 用户ID
     * @return Long
     */
    fun getFavoriteCollectionId(userId: Long): Long {
        val cachedId = redisTemplate.opsForValue().get(RedisKeys.PHOTO_USER_FAVORITE_COLLECTION_ID.format(userId))?.toLong()
        return if (cachedId != null) cachedId
        else {
            val id = getFavoriteCollection(userId).id!!
            redisTemplate.opsForValue().set(RedisKeys.PHOTO_USER_FAVORITE_COLLECTION_ID.format(userId), id.toString())
            id
        }
    }
}