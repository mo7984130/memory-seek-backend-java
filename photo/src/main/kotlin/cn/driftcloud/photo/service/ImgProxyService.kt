package cn.driftcloud.photo.service

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/22
 */
import cn.driftcloud.common.config.ImgProxyConfig
import cn.driftcloud.common.config.MinioConfig
import cn.driftcloud.photo.entity.Photo
import cn.driftcloud.photo.pojo.PhotoVO
import cn.driftcloud.photo.pojo.toVO
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/22
 */
@Service
class ImgProxyService(
    private val imgProxyConfig: ImgProxyConfig,
    private val minioConfig: MinioConfig
) {

    private lateinit var keySpec: SecretKeySpec
    private lateinit var saltBytes: ByteArray
    private val base64Encoder = Base64.getUrlEncoder().withoutPadding()

    @PostConstruct
    fun init() {
        val keyBytes = hexToBytes(imgProxyConfig.key)
        this.saltBytes = hexToBytes(imgProxyConfig.salt)

        this.keySpec = SecretKeySpec(keyBytes, "HmacSHA256")
    }

    fun generateImgProxyUrl(fileId: String, options: String, extension: String = "webp"): String {
        val sourceUrl = "s3://${minioConfig.bucketName}/$fileId"
        val encodedSourceUrl = base64Encoder.encodeToString(sourceUrl.toByteArray())

        val path = "/$options/$encodedSourceUrl.$extension"
        val signature = calculateSignature(path)

        return "${imgProxyConfig.baseUrl ?: ""}/$signature$path"
    }

    private fun calculateSignature(path: String): String {
        val hmac = Mac.getInstance("HmacSHA256")
        hmac.init(keySpec)

        hmac.update(saltBytes)
        val hash = hmac.doFinal(path.toByteArray())
        return base64Encoder.encodeToString(hash)
    }

    private fun hexToBytes(hex: String): ByteArray {
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    /**
     * 照片实体转换为VO
     * @param photo 照片实体
     * @return 照片VO
     */
    fun photoToVO(photo: Photo): PhotoVO {
        val extension = photo.fileId.substringAfterLast(".", "jpg")
        return photo.toVO(
            thumbnailUrl = generateImgProxyUrl(photo.fileId, "rs:fill:300:300/q:75", "webp"),
            previewUrl = generateImgProxyUrl(photo.fileId, "rs:fit:1200:0/q:85", "webp"),
            originalUrl = generateImgProxyUrl(photo.fileId, "rs:fit:0:0", extension)
        )
    }
}