package cn.driftcloud.photo.entity.vo

import cn.driftcloud.photo.entity.FacePerson

/**
 * @author DriftCloud
 * @Description 人物信息
 * @Date 2026/2/13
 */
data class FacePersonVO (
    val id: String,
    val name: String,
    val totalPhotoCount: Long,
    val coverPhotoUrl: String?
)

fun FacePerson.toVO(
    coverPhotoUrl: String? = null
): FacePersonVO {
    return FacePersonVO(
        id = id.toString(),
        name = name,
        totalPhotoCount = totalPhotoCount,
        coverPhotoUrl = coverPhotoUrl
    )
}