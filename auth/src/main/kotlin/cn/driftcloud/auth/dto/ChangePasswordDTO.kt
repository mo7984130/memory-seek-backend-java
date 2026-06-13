package cn.driftcloud.auth.dto

import cn.driftcloud.auth.annotation.validation.ValidPassword

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
data class ChangePasswordDTO(
    /**
     * 旧密码
     */
    @field:ValidPassword
    val oldPassword: String,

    /**
     * 新密码
     */
    @field:ValidPassword
    val newPassword: String
)
