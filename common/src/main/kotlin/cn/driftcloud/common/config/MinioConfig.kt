package cn.driftcloud.common.config

import io.minio.MinioClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@ConditionalOnProperty(prefix = "minio", name = ["endpoint"])
class MinioConfig {

    lateinit var endpoint: String
    lateinit var accessKey: String
    lateinit var secretKey: String
    lateinit var bucketName: String
    var externalUrl: String? = null
    lateinit var pathPrefix: String

    @Bean
    fun minioClient(): MinioClient {
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }
}