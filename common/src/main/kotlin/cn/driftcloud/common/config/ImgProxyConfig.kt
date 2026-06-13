package cn.driftcloud.common.config

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/22
 */
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/22
 */
@Configuration
@ConfigurationProperties(prefix = "img-proxy")
class ImgProxyConfig{
    var baseUrl: String? = null
        set(value) {
            field = value?.trim()?.removeSuffix("/")
        }
    lateinit var key: String
    lateinit var salt: String
}
