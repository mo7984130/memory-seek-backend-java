package cn.driftcloud.photo.entity.vo

import cn.driftcloud.photo.entity.FaceBBox
import cn.driftcloud.photo.entity.FaceFeature

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
data class FaceFeatureVO(
    val id: String,
    val personId: String,
    val personName: String,
    val score: Float,
    val bbox: FaceBBox
)

fun FaceFeature.toVO(
    personName: String
): FaceFeatureVO {
    return FaceFeatureVO(
        id = id.toString(),
        personId = personId.toString(),
        personName = personName,
        score = score,
        bbox = bbox
    )
}