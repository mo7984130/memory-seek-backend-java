package cn.driftcloud.auth.dto

import cn.driftcloud.auth.annotation.validation.ValidNickname

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
data class ChangeNicknameDTO(
    @field:ValidNickname
    val nickname: String,
)