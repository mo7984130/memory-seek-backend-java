package cn.driftcloud.auth.dto

import cn.driftcloud.auth.entity.User
import cn.driftcloud.common.constant.auth.TokenExpiration
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
data class UserDTO(
    val id: String,          // 数据库 ID
    val username: String?,    // 登录名
    val nickname: String?, // 显示昵称
    val email: String?,       // 邮箱
    val avatarUrl: String?,     // 头像Url
    val createdAt: Instant?, // 创建时间
    val refreshToken: String?, // 刷新令牌
    val refreshTokenExpireAt: Instant?, // 刷新令牌过期时间
    val accessToken: String?, // 访问令牌
    val accessTokenExpireAt: Instant?, // 访问令牌过期时间
)

fun User.toDto(accessToken: String? = null, accessTokenExpireAt: Instant? = null): UserDTO {
    return UserDTO(
        id = this.id.toString(),
        username = this.username,
        nickname = this.nickname,
        email = this.email,
        avatarUrl = this.avatarUrl,
        createdAt = this.createdAt,
        refreshToken = this.refreshToken,
        accessToken = accessToken,
        refreshTokenExpireAt = this.refreshTokenExpireAt,
        accessTokenExpireAt = run {
            if (accessToken == null) null
            else accessTokenExpireAt ?: Instant.now().plus(TokenExpiration.ACCESS_TOKEN_EXPIRATION)
        },
    )
}