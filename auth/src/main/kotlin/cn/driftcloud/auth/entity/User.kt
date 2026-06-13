package cn.driftcloud.auth.entity

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/20
 */
class User(id: EntityID<Long>) : LongEntity(id) {

    companion object : LongEntityClass<User>(UserTable)

    var username by UserTable.username
    var email by UserTable.email
    var password by UserTable.password
    var inviter by UserTable.inviter
    var nickname by UserTable.nickname
    var avatarUrl by UserTable.avatarUrl
    var refreshToken by UserTable.refreshToken
    var refreshTokenExpireAt by UserTable.refreshTokenExpireAt
    var createdAt by UserTable.createdAt
    var updatedAt by UserTable.updatedAt
}
