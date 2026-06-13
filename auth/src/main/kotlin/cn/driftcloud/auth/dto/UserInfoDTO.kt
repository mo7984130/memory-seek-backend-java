package cn.driftcloud.auth.dto

import cn.driftcloud.auth.entity.User

/**
 * @author DriftCloud
 * @Description 用户信息数据
 * @Date 2026/2/12
 */
data class UserInfoDTO(
    val id: String,
    val nickname: String,
    val avatarUrl: String?
)

fun User.toInfoDTO(): UserInfoDTO {
    return UserInfoDTO(
        id = id.toString(),
        nickname = nickname ?: "该用户没有设置昵称",
        avatarUrl = avatarUrl
    )
}