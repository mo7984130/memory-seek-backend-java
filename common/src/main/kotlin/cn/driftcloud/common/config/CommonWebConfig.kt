package cn.driftcloud.common.config

import cn.driftcloud.common.resolver.UserIdHeaderResolver
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * @author DriftCloud
 * @Description 公共Web配置
 * @Date 2026/1/12
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
// 注意：不要在类头部直接 import 具体的 WebFlux 类
class CommonMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(UserIdHeaderResolver())
    }
}