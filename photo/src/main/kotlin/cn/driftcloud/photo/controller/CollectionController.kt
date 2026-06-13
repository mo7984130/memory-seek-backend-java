package cn.driftcloud.photo.controller

import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.response.toRE
import cn.driftcloud.photo.pojo.CollectionCreateDTO
import cn.driftcloud.photo.pojo.CollectionEditDTO
import cn.driftcloud.photo.service.CollectionService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/4
 */
@RestController
@RequestMapping("/collection")
class CollectionController(
    private val folderService: CollectionService
) {

    /**
     * 获取用户收藏夹列表
     */
    @GetMapping("")
    fun getCollectionList(@CurrentUserId userId: Long)
    = folderService.getCollectionList(userId).toRE()

    /**
     * 获取收藏夹照片
     */
    @GetMapping("/{id}/photos")
    fun getCollectionPhotos(
        @CurrentUserId userId: Long,
        @PathVariable("id") collectionId: String,
        @RequestParam(required = false) cursor: Instant? = null,
        @RequestParam(required = false, defaultValue = "20") size: Int
    )
    = folderService.getPhotoFromCollection(userId, collectionId.toLong(), cursor, size).toRE()

    /**
     * 添加照片到收藏夹
     */
    @PostMapping("/{collectionId}/photos/{photoId}")
    fun addPhoto(
        @CurrentUserId userId: Long,
        @PathVariable collectionId: String,
        @PathVariable photoId: String
    )
    = folderService.addPhotoToCollection(userId, collectionId.toLong(), photoId.toLong()).toRE()


    /**
     * 从收藏夹移除照片
     */
    @DeleteMapping("/{collectionId}/photos/{photoId}")
    fun removePhoto(
        @CurrentUserId userId: Long,
        @PathVariable collectionId: String,
        @PathVariable photoId: String
    )
    = folderService.removePhotoFromCollection(userId, collectionId.toLong(), photoId.toLong()).toRE()

    /**
     * 创建收藏夹
     */
    @PostMapping("")
    fun createFolder(
        @CurrentUserId userId: Long,
        @RequestBody @Valid collectionCreateDTO: CollectionCreateDTO
    )
    = folderService.createCollection(userId, collectionCreateDTO.name, collectionCreateDTO.description).toRE()

    /**
     * 编辑收藏夹信息
     */
    @PatchMapping("/{collectionId}")
    fun editFolderInfo(
        @CurrentUserId userId: Long,
        @PathVariable collectionId: String,
        @RequestBody @Valid collectionEditDTO: CollectionEditDTO
    )
    = folderService.editCollectionInfo(userId, collectionId.toLong(), collectionEditDTO.name, collectionEditDTO.description).toRE()

    /**
     * 删除收藏夹
     */
    @DeleteMapping("/{collectionId}")
    fun deleteFolder(
        @CurrentUserId userId: Long,
        @PathVariable collectionId: String
    )
    = folderService.deleteCollection(userId, collectionId.toLong()).toRE()

    @GetMapping("/photo/{photoId}")
    fun getCollectionByPhotoId(
        @CurrentUserId userId: Long,
        @PathVariable photoId: String
    )
    = folderService.findCollectionIdsByPhotoId(userId, photoId.toLong()).toRE()
}