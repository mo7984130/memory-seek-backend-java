package cn.driftcloud.auth.entity

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/20
 */
object UserTable : LongIdTable("auth_user") {
    val username             = varchar(   "username",                           50).uniqueIndex()
    val email                = varchar(   "email",                              100).nullable()
    val password             = varchar(   "password",                           100).nullable()
    val oauthId              = varchar(   "oauth_id",                           100).nullable()
    val oauthType            = varchar(   "oauth_type",                         20).nullable()
    val nickname             = varchar(   "nickname",                           50)
    val avatarUrl            = varchar(   "avatar_url",                         255).nullable()
    val inviter              = long(      "inviter").nullable()
    val refreshToken         = varchar(   "refresh_token",                      255).nullable()
    val refreshTokenExpireAt = timestamp( "refresh_token_expire_at").nullable()
    val createdAt            = timestamp( "created_at").default(Instant.now())
    val updatedAt            = timestamp( "updated_at").default(Instant.now())
}