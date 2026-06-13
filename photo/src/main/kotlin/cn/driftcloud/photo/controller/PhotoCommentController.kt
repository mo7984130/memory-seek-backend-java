package cn.driftcloud.photo.controller

import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.response.toRE
import cn.driftcloud.photo.service.PhotoCommentService
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/11
 */
@Controller
@RequestMapping("/photo/comment")
class PhotoCommentController(
    private val photoCommentService: PhotoCommentService
) {

    data class PublishCommentDTO(
        val content: String
    )
    /**
     * 发布评论
     */
    @PostMapping("/{photoId}")
    fun publishComment(
        @PathVariable photoId: Long,
        @CurrentUserId userId: Long,
        @RequestBody dto: PublishCommentDTO
    )
    = photoCommentService.publishComment(photoId, userId, dto.content).toRE()

    /**
     * 删除评论
     */
    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @PathVariable commentId: Long,
        @CurrentUserId userId: Long
    )
    = photoCommentService.deleteComment(userId, commentId).toRE()

    /**
     * 获取评论列表
     */
    @GetMapping("/{photoId}")
    fun getCommentList(
        @PathVariable photoId: Long,
        @CurrentUserId userId: Long,
        @RequestParam cursor: Instant?,
        @RequestParam(required = false) limit: Int = 20
    )
    = photoCommentService.getCommentPage(photoId, userId, cursor, limit).toRE()

    /**
     * 点赞评论
     */
    @PostMapping("/{commentId}/like/toggle")
    fun likeComment(
        @PathVariable commentId: Long,
        @CurrentUserId userId: Long
    )
    = photoCommentService.toggleLike(userId, commentId).toRE()
}