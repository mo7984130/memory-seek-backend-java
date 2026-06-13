package cn.driftcloud.photo.service

import arrow.core.Either
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.photo.entity.PhotoComment
import cn.driftcloud.photo.entity.PhotoCommentLike
import cn.driftcloud.photo.mapper.PhotoCommentLikeMapper
import cn.driftcloud.photo.mapper.PhotoCommentMapper
import cn.driftcloud.photo.pojo.PhotoCommentVO
import cn.driftcloud.photo.pojo.toVO
import com.baomidou.mybatisplus.extension.kotlin.KtQueryWrapper
import com.baomidou.mybatisplus.extension.kotlin.KtUpdateWrapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/11
 */
@Service
class PhotoCommentService(
    private val commentMapper: PhotoCommentMapper,
    private val commentLikeMapper: PhotoCommentLikeMapper
) {

    /**
     * 获取评论分页
     * @param photoId 照片ID
     * @param userId 用户ID
     * @param cursor 时间游标
     * @param limit 每页数量
     */
    fun getCommentPage(photoId: Long, userId: Long, cursor: Instant?, limit: Int): Either<String, CursorPageVO<PhotoCommentVO, Instant>> {
        // 第一页的话, 前三条为热评, 大于5个点赞才显示
        val hotComments = if (cursor == null) {
            commentMapper.selectList(
                KtQueryWrapper(PhotoComment::class.java)
                    .eq(PhotoComment::photoId, photoId)
                    .gt(PhotoComment::likeCount, 5)
                    .orderByDesc(PhotoComment::likeCount)
                    .last("LIMIT 3")
            )
        } else {
            emptyList()
        }

        // 获取时间线评论
        val hotIds = hotComments.map { it.id }
        val timeQuery = KtQueryWrapper(PhotoComment::class.java)
            .eq(PhotoComment::photoId, photoId)
            .notIn(hotIds.isNotEmpty() ,PhotoComment::id, hotIds)
        if (cursor != null) timeQuery.lt(PhotoComment::createdAt, cursor)
        timeQuery.orderByDesc(PhotoComment::createdAt).last("LIMIT ${limit + 1}")

        val originalTimeComments = commentMapper.selectList(timeQuery)
        val hasMore = originalTimeComments.size > limit
        val timeComments = originalTimeComments.take(limit)

        // 聚合数据
        val comments = hotComments + timeComments
        val favoritedSet = if (comments.isNotEmpty()) {
            commentLikeMapper.selectList(
                KtQueryWrapper(PhotoCommentLike::class.java)
                    .eq(PhotoCommentLike::userId, userId)
                    .`in`(PhotoCommentLike::commentId, comments.map { it.id })
            ).map { it.commentId }.toSet()
        } else {
            emptySet()
        }

        // 组装VO
        val records = comments.map {
            it.toVO(favoritedSet.contains(it.id))
        }
        return Either.Right(
            CursorPageVO(
                records = records,
                nextCursor = if (hasMore) originalTimeComments.last().createdAt else null,
                hasMore = hasMore
            )
        )
    }

    /**
     * 发表评论
     * @param photoId 照片ID
     * @param userId 用户ID
     * @param content 评论内容
     */
    @Transactional
    fun publishComment(photoId: Long, userId: Long, content: String): Either<String, PhotoCommentVO> {
        val comment = PhotoComment(
            photoId = photoId,
            userId = userId,
            content = content,
            likeCount = 0
        )
        val row = commentMapper.insert(comment)
        return if (row > 0) Either.Right(comment.toVO(false)) else Either.Left("发布失败")
    }

    /**
     * 删除评论
     * @param userId 用户ID
     * @param commentId 评论ID
     */
    @Transactional
    fun deleteComment(userId: Long, commentId: Long): Either<String, Unit> {
        // 查询评论
        val comment = commentMapper.selectById(commentId) ?: return Either.Left("评论不存在")
        if (comment.userId != userId) return Either.Left("无权限删除")

        // 删除评论
        val rows = commentMapper.deleteById(commentId)
        return if (rows > 0) {
            // 删除评论点赞
            commentLikeMapper.delete(
                KtQueryWrapper(PhotoCommentLike::class.java)
                    .eq(PhotoCommentLike::commentId, commentId)
            )
            Either.Right(Unit)
        } else {
            Either.Left("删除失败")
        }
    }

    /**
     * 点赞/取消点赞切换
     */
    @Transactional
    fun toggleLike(userId: Long, commentId: Long): Either<String, Boolean> {
        val query = KtQueryWrapper(PhotoCommentLike::class.java)
            .eq(PhotoCommentLike::userId, userId)
            .eq(PhotoCommentLike::commentId, commentId)
        val existingLike = commentLikeMapper.selectOne(query)

        return Either.Right(
            if (existingLike != null) {
                // 取消点赞
                commentLikeMapper.deleteById(existingLike.id)
                updateLikeCount(commentId, -1)
                false
            } else {
                val newLike = PhotoCommentLike(
                    commentId = commentId,
                    userId = userId
                )
                commentLikeMapper.insert(newLike)
                updateLikeCount(commentId, 1)
                true
            }
        )
    }

    /**
     * 更新点赞数
     * @param commentId 评论ID
     * @param delta 点赞数变化量
     */
    private fun updateLikeCount(commentId: Long, delta: Int) {
        commentMapper.update(null,
            KtUpdateWrapper(PhotoComment::class.java)
                .eq(PhotoComment::id, commentId)
                .setSql("like_count = like_count + $delta")
        )
    }

}