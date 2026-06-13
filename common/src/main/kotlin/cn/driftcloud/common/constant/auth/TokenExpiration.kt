package cn.driftcloud.common.constant.auth

import java.time.Duration

/**
 * @author DriftCloud
 * @Description 令牌过期时间
 * @Date 2026/1/13
 */
object TokenExpiration {
    /**
     * 访问令牌过期时间
     */
    val ACCESS_TOKEN_EXPIRATION = Duration.ofHours(1)

    /**
     * 刷新令牌过期时间
     */
    val REFRESH_TOKEN_EXPIRATION = Duration.ofDays(30)
}