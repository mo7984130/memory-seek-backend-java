package cn.driftcloud.auth.dto

import cn.driftcloud.auth.annotation.validation.ValidEmail
import cn.driftcloud.auth.annotation.validation.ValidNickname
import cn.driftcloud.auth.annotation.validation.ValidPassword
import cn.driftcloud.auth.annotation.validation.ValidUsername
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

/**
 * @author DriftCloud
 * @Description 注册DTO
 * @Date 2026/1/13
 */
data class RegisterDTO(

    @field:ValidUsername
    val username: String,

    @field:ValidEmail
    val email: String,

    @field:ValidPassword
    val password: String,

    @field:ValidNickname
    val nickname: String,

    @field:NotBlank(message = "邀请码不能为空")
    @field:Length(min = 6, max = 6, message = "邀请码长度为 6 个字符")
    val inviterCode: String,

    @field:NotBlank(message = "邮箱验证码不能为空")
    @field:Length(min = 6, max = 6, message = "邮箱验证码长度为 6 个字符")
    val emailVerifyCode: String
)