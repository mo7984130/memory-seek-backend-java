package cn.driftcloud.auth.service

import arrow.core.Either
import arrow.core.flatMap
import cn.driftcloud.auth.dto.UserDTO
import cn.driftcloud.auth.dto.UserInfoDTO
import cn.driftcloud.auth.dto.toDto
import cn.driftcloud.auth.entity.User
import cn.driftcloud.auth.entity.UserTable
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.util.CacheUtils
import cn.driftcloud.auth.service.DBUtils
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/12
 */
@Service
class UserService(
    private val cacheUtils: CacheUtils,
    private val db: DBUtils,
    private val minioService: MinioService
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * 根据用户ID获取用户信息
     */
    fun getUserInfo(userId: Long): Either<String, UserDTO> {
        return db.read {
            User.findById(userId)?.toDto()
        }?.let { Either.Right(it) } ?: Either.Left("用户不存在")
    }

    /**
     * 更新用户昵称
     */
    fun updateNickname(userId: Long, newNickname: String): Either<String, String> {
        return try {
            db.write {
                val user = User.findById(userId) ?: return@write null
                user.nickname = newNickname
                newNickname
            }?.let {
                cacheUtils.delete(RedisKeys.AUTH_USER_INFO_CACHE.format(userId))
                Either.Right(it)
            } ?: Either.Left("用户不存在")
        } catch (e: Exception) {
            Either.Left("昵称更新失败")
        }
    }

    /**
     * 更新用户头像
     */
    fun updateAvatar(userId: Long, file: MultipartFile): Either<String, String> {
        // 1. 调用 MinioService 上传文件并获取新 URL
        return minioService.uploadAvatarFile(userId, file).flatMap { newUrl ->
            try {
                // 2. 开启事务更新数据库
                db.write {
                    val user = User.findById(userId) ?: return@write null
                    val oldUrl = user.avatarUrl

                    // 更新新头像
                    user.avatarUrl = newUrl

                    // 3. 数据库成功后，异步或随后清理 MinIO 旧文件（不影响主流程）
                    oldUrl?.let { minioService.deleteFileByUrl(it) }

                    newUrl
                }?.let {
                    cacheUtils.delete(RedisKeys.AUTH_USER_INFO_CACHE.format(userId))
                    Either.Right(it)
                } ?: Either.Left("用户不存在")
            } catch (e: Exception) {
                Either.Left("头像地址保存失败")
            }
        }
    }

    /**
     * 修改密码
     */
    fun changePassword(userId: Long, oldPassword: String, newPassword: String): Either<String, Unit> {
        // 第一段：读取旧密码
        val storedPassword = db.read {
            User.findById(userId)?.password
        } ?: return Either.Left("用户不存在")

        // 非事务区：验证旧密码 (耗时)
        if (!BCrypt.checkpw(oldPassword, storedPassword)) {
            return Either.Left("原密码错误")
        }

        // 非事务区：计算新密码哈希 (耗时)
        val newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        // 第二段：写入更新并让所有 Token 失效
        return try {
            db.write {
                val user = User.findById(userId)!!
                user.password = newHashedPassword
                user.refreshToken = null
                user.refreshTokenExpireAt = null

                // 清理 AccessToken 缓存
                cacheUtils.delete(RedisKeys.AUTH_USER_ACCESS_TOKEN.format(userId))
            }
            Either.Right(Unit)
        } catch (e: Exception) {
            Either.Left("密码更新失败")
        }
    }

    /**
     * 登出
     */
    fun logout(userId: Long) {
        db.write {
            val user = User.findById(userId)
            user?.apply {
                refreshToken = null
                refreshTokenExpireAt = null
            }
            cacheUtils.delete(RedisKeys.AUTH_USER_ACCESS_TOKEN.format(userId))
        }
    }

    /**
     * 根据用户ID列表获取用户信息列表
     */
    fun getUserInfoBatch(userIds: List<Long>): Either<String, List<UserInfoDTO>> {
        logger.info("批量获取用户信息请求: 用户ID数量={}", userIds.size)
        try {
            val data = cacheUtils.getOrLoadBatch(
                params = userIds,
                keyProvider = { RedisKeys.AUTH_USER_INFO_CACHE.format(it) },
                type = UserInfoDTO::class.java,
                loader = { missingIds ->
                    logger.debug("从数据库加载用户信息: 缺失ID数量={}", missingIds.size)
                    db.read {
                        UserTable
                            .select(UserTable.id, UserTable.nickname, UserTable.avatarUrl)
                            .where { UserTable.id inList missingIds }
                            .map {
                                // 这里的 it 是 ResultRow，直接转换为 DTO
                                UserInfoDTO(
                                    id = it[UserTable.id].value.toString(),
                                    nickname = it[UserTable.nickname],
                                    avatarUrl = it[UserTable.avatarUrl]
                                )
                            }
                    } ?: emptyList()
                },
                resultMapper = { dto -> dto.id.toLong() }
            )
            logger.info("批量获取用户信息成功: 返回数量={}", data.size)
            return Either.Right(data)
        } catch (e: Exception) {
            logger.error("批量获取用户信息异常: 用户ID列表={}", userIds, e)
            return Either.Left("获取用户信息失败")
        }
    }

}