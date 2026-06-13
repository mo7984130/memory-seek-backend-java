package cn.driftcloud.photo.event

/**
 * @author DriftCloud
 * @Description 照片上传成功事件
 * @Date 2026/2/14
 */
data class PhotoUploadedEvent(
    val photoId: Long,
    val imageByte: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhotoUploadedEvent

        if (photoId != other.photoId) return false
        if (!imageByte.contentEquals(other.imageByte)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = photoId.hashCode()
        result = 31 * result + imageByte.contentHashCode()
        return result
    }
}