package cn.driftcloud.photo.controller

import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.pojo.vo.CursorPageVO
import cn.driftcloud.common.response.R
import cn.driftcloud.common.response.toRE
import cn.driftcloud.photo.entity.vo.FaceFeatureVO
import cn.driftcloud.photo.entity.vo.FacePersonSimpleVO
import cn.driftcloud.photo.entity.vo.FacePersonVO
import cn.driftcloud.photo.pojo.PhotoVO
import cn.driftcloud.photo.service.FaceEngineService
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

// 重命名人物请求参数
data class RenamePersonRequest(
    // 新名称
    @field:NotBlank(message = "新名称不能为空")
    @field:Size(min = 1, max = 20, message = "新名称长度在1-20个字符之间")
    val newName: String
)

// 合并人物请求参数
data class MergePersonRequest(
    // 源人物ID, 被合并的人物
    @field:NotBlank(message = "目标人物ID不能为空")
    val sourcePersonId: String,
    // 目标人物ID, 合并到的人物
    @field:NotBlank(message = "源人物ID不能为空")
    val targetPersonId: String,
)

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
@RestController
@RequestMapping("/face")
class FaceController(
    private val faceEngineService: FaceEngineService
) {

    /**
     * 查询人物分页
     */
    @GetMapping("/person")
    fun getPersonPage(
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) cursor: String?
    )
    : ResponseEntity<R<out CursorPageVO<FacePersonVO, String>?>>
    = faceEngineService.getPersonPage(cursor?.toLong(), size).map {
        CursorPageVO(it.records, it.nextCursor.toString(), it.hasMore)
    }.toRE()

    /**
     * 重命名人物
     */
    @PostMapping("/person/{personId}/name")
    fun renamePerson(
        @PathVariable personId: String,
        @RequestBody request: RenamePersonRequest
    ): ResponseEntity<R<out FacePersonVO?>>
    = faceEngineService.renamePerson(personId.toLong(), request.newName).toRE()

    /**
     * 合并人物
     */
    @PostMapping("/person/merge")
    fun mergePerson(
        @RequestBody request: MergePersonRequest
    ): ResponseEntity<R<out FacePersonVO?>>
    = faceEngineService.mergePerson(request.sourcePersonId.toLong(), request.targetPersonId.toLong()).toRE()

    /**
     * 删除人物
     */
    @DeleteMapping("/person/{personId}")
    fun deletePerson(
        @PathVariable personId: String
    ): ResponseEntity<R<out Boolean?>>
    = faceEngineService.deletePerson(personId.toLong()).toRE()

    /**
     * 查询照片人脸
     */
    @GetMapping("/feature/{photoId}")
    fun getPhotoFeature(
        @PathVariable photoId: String
    ): ResponseEntity<R<out List<FaceFeatureVO>?>>
    = faceEngineService.getPhotoFeature(photoId.toLong()).toRE()

    /**
     * 获取人物的照片
     */
    @GetMapping("/person/{personId}/photo")
    fun getPersonPhoto(
        @CurrentUserId userId: Long,
        @PathVariable personId: String,
        @RequestParam(required = false, defaultValue = "20") size: Int,
        @RequestParam(required = false) cursor: String?
    ): ResponseEntity<R<out CursorPageVO<PhotoVO, String>?>>
    = faceEngineService.getPersonPhoto(userId, personId.toLong(), cursor?.toLong(), size).map {
        CursorPageVO(it.records, it.nextCursor.toString(), it.hasMore)
    }.toRE()

    /**
     * 修改人脸归属
     */
    @PostMapping("/feature/{featureId}/belonging/{personId}")
    fun changeFaceBelonging(
        @PathVariable featureId: String,
        @PathVariable personId: String
    ): ResponseEntity<R<out Unit?>>
    = faceEngineService.changeFaceBelonging(featureId.toLong(), personId.toLong()).toRE()

    /**
     * 获取人物信息
     */
    @GetMapping("/person/{personId}")
    fun getPersonInfo(
        @CurrentUserId userId: Long,
        @PathVariable personId: String
    ): ResponseEntity<R<out FacePersonVO?>>
    = faceEngineService.getPersonInfo(userId, personId.toLong()).toRE()

    /**
     * 获取所有人物信息
     */
    @GetMapping("/person/all")
    fun getAllPerson(): ResponseEntity<R<out List<FacePersonSimpleVO>?>>
    =    faceEngineService.getAllPersonInfo().toRE()
}