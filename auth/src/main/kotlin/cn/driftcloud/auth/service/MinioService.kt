package cn.driftcloud.auth.service

import arrow.core.Either
import arrow.core.flatMap
import cn.driftcloud.common.config.MinioConfig
import cn.driftcloud.common.util.FileValidator
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Service
class MinioService(
    private val minioClient: MinioClient,
    private val minioConfig: MinioConfig
) {

    private val logger = LoggerFactory.getLogger(MinioService::class.java)

    /**
     * 上传文件
     * @param file 文件
     * @return 文件URL
     */
    fun uploadFile(file: MultipartFile, filename: String): Either<String, String> {
        if (file.isEmpty) {
            return Either.Left("上传文件不能为空")
        }

        try {
            val inputStream = file.inputStream

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.bucketName) // 从配置类读取
                    .`object`(filename)
                    .stream(inputStream, file.size, -1)
                    .contentType(file.contentType)
                    .build()
            )

            inputStream.close()

            val baseUrl = (minioConfig.externalUrl ?: "").trimEnd('/')
            return Either.Right("${baseUrl}/${minioConfig.bucketName}/$filename")
        } catch (e: Exception) {
            logger.error("上传文件到MinIO失败: ${e.message}", e)
            return Either.Left("上传文件到MinIO失败")
        }
    }

    /**
     * 上传头像
     * @param userId 用户ID
     * @param file 头像文件
     * @return 错误原因 | 头像url
     */
    fun uploadAvatarFile(userId: Long, file: MultipartFile): Either<String, String> {
        return FileValidator.validateImage(file).flatMap { metaData ->
            val objectName = "${minioConfig.pathPrefix}/${userId}_${System.currentTimeMillis()}.${metaData.format}"
            uploadFile(file, objectName)
        }
    }

    /**
     * 根据 URL 清理物理文件
     */
    fun deleteFileByUrl(url: String) {
        if (url.isBlank()) return
        try {
            val objectName = extractObjectName(url) ?: return

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.bucketName)
                    .`object`(objectName)
                    .build()
            )
            logger.debug("已清理 MinIO 垃圾文件: {}", objectName)
        } catch (e: Exception) {
            // 清理失败仅记录日志，不抛出异常，防止干扰业务主流程
            logger.error("MinIO 物理文件删除失败: {}", url, e)
        }
    }

    private fun extractObjectName(url: String): String? {
        if (url.isBlank()) return null
        return try {
            // 方案 A：使用新的标准方式代替 URL(string)
            val uri = java.net.URI(url)
            val path = uri.path // 获取路径部分，例如 /bucketName/path/prefix/avatar.jpg

            val bucketFlag = "/${minioConfig.bucketName}/"
            if (path.contains(bucketFlag)) {
                // 截取 bucket 名称之后的部分
                path.substringAfter(bucketFlag)
            } else {
                // 如果 URL 比较特殊，尝试移除开头的斜杠
                path.removePrefix("/")
            }
        } catch (_: Exception) {
            // 如果解析失败，回退到最简单的字符串截取（作为保底）
            url.substringAfterLast("${minioConfig.bucketName}/", "")
                .takeIf { it.isNotEmpty() }
        }
    }
}