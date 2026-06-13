package cn.driftcloud.common.constant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
object RedisKeys {
    // %s为email, 值为emailCode
    const val AUTH_EMAIL_CODE_PREFIX = "auth:verify:email:%s"
    // %s为邀请码, 值为userId
    const val AUTH_INVITER_CODE_PREFIX = "auth:inviter:code:%s"
    // %d为userId, 值为accessToken
    const val AUTH_USER_ACCESS_TOKEN = "auth:user:accessToken:%d"

    // %d为userId, 值为用户信息缓存
    const val AUTH_USER_INFO_CACHE = "auth:user:info:cache:%d"

    // %d为userId, 值为用户喜欢收藏夹的id
    const val PHOTO_USER_FAVORITE_COLLECTION_ID = "photo:user:favorite:collection:id:{%d}"

    // %d为personId, 值为名字
    const val PHOTO_FACE_PERSON_NAME_CACHE = "photo:face:feature:person:name:cache:%d"
}