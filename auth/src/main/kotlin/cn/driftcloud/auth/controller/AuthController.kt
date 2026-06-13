package cn.driftcloud.auth.controller

import cn.driftcloud.auth.dto.LoginDTO
import cn.driftcloud.auth.dto.RegisterDTO
import cn.driftcloud.auth.service.AuthService
import cn.driftcloud.common.annotation.RateLimit
import cn.driftcloud.common.response.R
import cn.driftcloud.common.response.toRE
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/17
 */
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    /**
     * 登录
     */
    @PostMapping("/login")
    fun login(
        @RequestBody @Valid loginDTO: LoginDTO
    )
    = authService.login(loginDTO).toRE()


    /**
     * 注册用户
     * @return 是否注册成功
     */
    @PostMapping("/register")
    fun register(
        @RequestBody @Valid registerDTO: RegisterDTO
    )
    = authService.register(registerDTO).toRE()

    /**
     * 发送邮箱验证码
     * @param email 邮箱
     * @return 验证码
     */
    @GetMapping("/email-verify-code")
    @RateLimit(key = "#email", message = "邮箱验证码发送过于频繁，请稍后重试")
    fun sendEmailCode(
        @RequestParam("email") email: String
    )
    = authService.sendEmailCode(email).toRE()

    /**
     * 生成accessToken
     * @return accessToken
     */
    @GetMapping("/access-token")
    fun generateAccessToken(
        request: HttpServletRequest
    ): Any {
        val userIdHeader = request.getHeader("X-User-Id") ?: return R.error("未提供用户id")
        val userId = userIdHeader.toLongOrNull() ?: return R.error("用户id格式错误")
        val refreshToken = request.getHeader("X-Refresh-Token") ?: return R.error("未提供刷新令牌")

        return authService.generateAccessToken(userId, refreshToken).toRE()
    }

}