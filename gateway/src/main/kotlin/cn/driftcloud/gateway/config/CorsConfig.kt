package cn.driftcloud.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    open fun corsWebFilter(): CorsWebFilter {
        val config = CorsConfiguration()
        // 允许的跨域域名
        config.addAllowedOriginPattern("*")
        // 允许的请求头
        config.addAllowedHeader("*")
        // 允许的请求方法
        config.addAllowedMethod("*")
        // 是否允许携带cookie
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return CorsWebFilter(source)
    }
}
