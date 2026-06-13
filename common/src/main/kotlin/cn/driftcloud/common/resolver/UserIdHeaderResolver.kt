package cn.driftcloud.common.resolver

import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.constant.Headers
import cn.driftcloud.common.exception.auth.UnauthorizedException
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * @author DriftCloud
 * @Description 当前用户ID参数解析器
 * @Date 2026/1/12
 */
class UserIdHeaderResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(CurrentUserId::class.java)
    }

    override fun resolveArgument(parameter: MethodParameter,
                                 mavContainer: ModelAndViewContainer?,
                                 webRequest: NativeWebRequest,
                                 binderFactory: WebDataBinderFactory?
    ): Long {
        val userIdStr = webRequest.getHeader(Headers.USERID)
        if (userIdStr.isNullOrBlank()) throw UnauthorizedException("未登录")
        return userIdStr.toLong()
    }
}