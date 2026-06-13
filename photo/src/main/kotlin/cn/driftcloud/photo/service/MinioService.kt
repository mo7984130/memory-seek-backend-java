package cn.driftcloud.photo.service

import cn.driftcloud.common.config.MinioConfig
import io.minio.GetObjectArgs
import io.minio.MinioClient
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class MinioService(
    private val minioClient: MinioClient,
    private val minioConfig: MinioConfig
) {

    /**
     * 流式获取 MinIO 对象，利用 block 回调确保 InputStream 及时关闭
     */
    fun <T> getObjectStream(objectName: String, block: (InputStream) -> T): T {
        return minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioConfig.bucketName)
                .`object`(objectName)
                .build()
        ).use { block(it) }
    }

    /**
     * 获取字节数组（用于需要频繁随机访问或多次读取的场景）
     */
    fun getObjectBytes(objectName: String): ByteArray {
        return getObjectStream(objectName) { it.readAllBytes() }
    }
}