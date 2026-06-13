package cn.driftcloud.auth.dto

import cn.driftcloud.auth.annotation.validation.ValidAccount
import cn.driftcloud.auth.annotation.validation.ValidPassword

/**
 * @author DriftCloud
 * @Description 登录DTO
 * @param account 账号
 * @param password 密码
 * @Date 2026/1/13
 */
data class LoginDTO(
    @field:ValidAccount
    val account: String,

    @field:ValidPassword
    val password: String,
)