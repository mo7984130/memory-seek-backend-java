package cn.driftcloud.common.util

import arrow.core.Either
import org.slf4j.LoggerFactory
import org.springframework.util.unit.DataSize
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.util.Locale.getDefault
import javax.imageio.ImageIO

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
object FileValidator {

    /** 图片元数据 */
    data class ImageMetaData(
        /** 图片格式 jpg... */
        val format: String,
        /** 图片宽度 单位：像素 */
        val width: Int,
        /** 图片高度 单位：像素 */
        val height: Int,
        /** 文件大小 单位：字节 */
        val size: Long,
        /** 文件名 */
        val name: String,
        /** 文件MIME类型 */
        val mineType: String,
    )

    private val log = LoggerFactory.getLogger(FileValidator::class.java)

    private val ALLOW_IMAGE_TYPE = mapOf(
        "jpg" to "FFD8FF",
        "jpeg" to "FFD8FF",
        "png" to "89504E47",
        "gif" to "47494638",
        "bmp" to "424D"
    )

    private const val ALLOW_IMAGE_MAX_SIZE: Long = 20 * 1024 * 1024 // 10MB

    /**
     * 验证文件是否为图片
     * @param file 上传的文件
     * @return Either 失败则是错误信息, 成功是fileType
     */
    fun validateImage(file: MultipartFile): Either<String, ImageMetaData> {
        // 1. 检查文件是否为空
        if (file.isEmpty) return Either.Left("上传文件不能为空")
        // 2. 检查文件大小
        if (file.size > ALLOW_IMAGE_MAX_SIZE) return Either.Left("上传文件大小不能超过${DataSize.ofBytes(ALLOW_IMAGE_MAX_SIZE)}")
        // 3. 检查文件名称是否为空
        if (file.originalFilename === null) return Either.Left("上传文件名称不能为空")
        // 4. 检查文件格式是否支持
        val fileType = file.originalFilename!!.substringAfterLast(".").lowercase(getDefault())
        if (fileType !in ALLOW_IMAGE_TYPE.keys) return Either.Left("不支持的文件格式: $fileType，仅支持 ${ALLOW_IMAGE_TYPE.keys}")

        try {

            // 5. 检查文件头是否匹配
            file.inputStream.use { inputStream ->
                val headerBytes = ByteArray(4)
                if (inputStream.read(headerBytes) == -1) {
                    return Either.Left("无法读取文件头")
                }
                val fileHeader = bytesToHex(headerBytes)
                val expectedHeader = ALLOW_IMAGE_TYPE[fileType]

                if (!fileHeader.startsWith(expectedHeader!!)) {
                    return Either.Left("文件头不匹配，禁止伪造文件格式！(检测为: $fileHeader)")
                }
            }

            // 6. 获取宽高信息 (Metadata Extraction)
            // 使用 ImageReader 而不是 ImageIO.read()，避免将整个图片加载进内存，节省 RAM
            var width = 0
            var height = 0
            file.inputStream.use { inputStream ->
                // ImageIO 创建流
                ImageIO.createImageInputStream(inputStream).use { imageStream ->
                    if (imageStream == null) return Either.Left("图片数据流异常")

                    val readers = ImageIO.getImageReaders(imageStream)
                    if (!readers.hasNext()) {
                        return Either.Left("无法识别的图片内容，可能文件已损坏")
                    }
                    val reader = readers.next()
                    try {
                        reader.input = imageStream
                        width = reader.getWidth(0)
                        height = reader.getHeight(0)
                    } finally {
                        reader.dispose()
                    }
                }
            }
            // 7. 返回图片元数据
            return Either.Right(
                ImageMetaData(
                    format = fileType,
                    width = width,
                    height = height,
                    size = file.size,
                    name = file.originalFilename!!,
                    mineType = file.contentType!!,
                )
            )

        } catch (e: IOException) {
            log.error(e.message)
            return Either.Left("文件读取失败")
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val hex = Integer.toHexString(b.toInt() and 0xFF)
            if (hex.length < 2) {
                sb.append("0")
            }
            sb.append(hex)
        }
        return sb.toString().uppercase()
    }



}