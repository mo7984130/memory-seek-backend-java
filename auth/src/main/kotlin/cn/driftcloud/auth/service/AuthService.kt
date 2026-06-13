package cn.driftcloud.auth.service

import arrow.core.Either
import cn.driftcloud.auth.dto.LoginDTO
import cn.driftcloud.auth.dto.RegisterDTO
import cn.driftcloud.auth.dto.UserDTO
import cn.driftcloud.auth.dto.toDto
import cn.driftcloud.auth.entity.User
import cn.driftcloud.auth.entity.UserTable
import cn.driftcloud.common.constant.MqConstants
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.constant.auth.TokenExpiration
import cn.driftcloud.common.pojo.vo.mq.UserRegisterEvent
import cn.driftcloud.auth.service.DBUtils
import cn.driftcloud.common.util.EmailSender
import cn.driftcloud.common.util.RandomUtil
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/**
 * @author DriftCloud
 * @Description 登录服务
 * @Date 2026/2/5
 */
@Service
class AuthService(
    private val redisTemplate: StringRedisTemplate,
    private val emailSender: EmailSender,
    private val rabbitTemplate: RabbitTemplate,
    private val db: DBUtils
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /**
     * 登录
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @Transactional
    fun login(loginDTO: LoginDTO): Either<String, UserDTO> {
        logger.info("用户登录请求: 账号={}", loginDTO.account)
        try {
            // 验证用户名或邮箱是否存在
            val user = db.read {
                User.find {
                    (UserTable.username eq loginDTO.account) or (UserTable.email eq loginDTO.account)
                }.singleOrNull()?.let { it.id.value to it.password }
            } ?: return Either.Left("用户不存在")

            // 验证密码
            if (!BCrypt.checkpw(loginDTO.password, user.second)) {
                logger.warn("登录失败: 密码错误, 账号={}", loginDTO.account)
                return Either.Left("密码错误")
            }

            // 生成token
            val userDto = db.write {
                // 重新获取 entity 确保它绑定在当前“事务 2”的上下文中
                val entity = User.findById(user.first)!!

                val token = processTokensAndGetAccess(entity)

                // 返回 DTO
                entity.toDto(token, Instant.now().plus(TokenExpiration.ACCESS_TOKEN_EXPIRATION))
            }

            logger.info("登录成功: 用户ID={}, 用户名={}", userDto.id, userDto.username)
            return Either.Right(userDto)
        } catch (e: Exception) {
            logger.error("登录过程异常: 账号={}", loginDTO.account, e)
            return Either.Left("登录失败，请稍后重试")
        }
    }

    /**
     * 注册
     * @param dto 注册信息
     * @return 注册结果
     */
    @Transactional
    fun register(dto: RegisterDTO): Either<String, UserDTO> {
        logger.info("用户注册请求: 用户名={}, 邮箱={}, 昵称={}", dto.username, dto.email, dto.nickname)
        try {
            // 校验验证码
            if (!verifyEmailCode(dto.email, dto.emailVerifyCode)) {
                return Either.Left("邮箱验证码错误")
            }

            // 验证邀请码
            val inviterId = if (dto.inviterCode == "DriftC") 1L else verifyInviterCode(dto.inviterCode)
            if (inviterId == null) return Either.Left("邀请码无效")

            // 检查用户名/邮箱重复 (DSL exists 查询比 DAO 查全量更高效)
            val isDuplicate = UserTable.selectAll().where {
                (UserTable.username eq dto.username) or (UserTable.email eq dto.email)
            }.any()
            if (isDuplicate) return Either.Left("用户名或邮箱已存在")

            // 密码加密
            val hashedPw = BCrypt.hashpw(dto.password, BCrypt.gensalt())

            // 创建事务
            val userDto = db.write {
                val user = User.new {
                    this.username = dto.username
                    this.email = dto.email
                    this.password = hashedPw
                    this.nickname = dto.nickname
                    this.inviter = inviterId
                }
                user.toDto()
            }
            logger.info("用户注册成功: 用户ID={}, 用户名={}", userDto.id, dto.username)
            sendRegisterMqEvent(userDto.id.toLong())

            return Either.Right(userDto)
        } catch (e: ExposedSQLException) {
            logger.error("注册过程异常: 用户名={}, 邮箱={}", dto.username, dto.email, e)
            return Either.Left("注册失败，请稍后重试")
        }
    }

    /**
     * 发送用户注册成功事件
     * @param userId 用户ID
     */
    private fun sendRegisterMqEvent(userId: Long) {
        try {
            val event = UserRegisterEvent(userId = userId)
            rabbitTemplate.convertAndSend(
                MqConstants.USER_EVENT_EXCHANGE,
                MqConstants.USER_REGISTER_ROUTING_KEY,
                event
            )
            logger.info("发送用户注册事件成功: 用户ID={}", userId)
        } catch (e: Exception) {
            logger.error("异步发送注册事件失败: 用户ID={}", userId, e)
        }
    }

    /**
     * 发送邮箱验证码
     * @param email 邮箱
     * @return 是否发送成功
     */
    fun sendEmailCode(email: String): Either<String, String> {
        try {
            val code = RandomUtil.generateCode(6)
            val key = RedisKeys.AUTH_EMAIL_CODE_PREFIX.format(email)
            redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(10))
            emailSender.sendHtmlMail(
                from = "no-reply@driftcloud.ink",
                nickname = "寻忆",
                to = email,
                subject = "寻忆邮箱验证码",
                content = """
                <p>您的验证码为: <strong>$code</strong></p>
                <p>该验证码有效期为10分钟。</p>
            """.trimIndent()
            )
        } catch (e: Exception) {
            logger.warn("发送验证码到 $email 失败", e)
            return Either.Left("验证码发送失败")
        }
        return Either.Right("验证码发送成功, 有效期为10分钟")
    }

    /**
     * 生成accessToken
     * @param userId 用户id
     * @param refreshToken refreshToken
     * @return accessToken, accessTokenExpireAt
     */
    fun generateAccessToken(userId: Long, refreshToken: String): Either<String, Map<String, Any>> {
        logger.info("生成访问令牌请求: 用户ID={}", userId)
        try {
            // 验证refreshToken
            val storedRefreshToken = db.read {
                User.findById(userId)?.refreshToken
            }
            if (storedRefreshToken != refreshToken) {
                logger.warn("生成访问令牌失败: 刷新令牌无效, 用户ID={}", userId)
                return Either.Left("刷新令牌无效")
            }

            val newAccessToken = RandomUtil.generateCode(32)
            // 存储新的accessToken
            val key = RedisKeys.AUTH_USER_ACCESS_TOKEN.format(userId)
            redisTemplate.opsForValue().set(
                key,
                newAccessToken,
                TokenExpiration.ACCESS_TOKEN_EXPIRATION
            )
            val accessTokenExpireAt = Instant.now().plus(TokenExpiration.ACCESS_TOKEN_EXPIRATION)

            logger.info("生成访问令牌成功: 用户ID={}", userId)
            return Either.Right(mapOf(
                "accessToken" to newAccessToken,
                "accessTokenExpireAt" to accessTokenExpireAt
            ))
        } catch (e: Exception) {
            logger.error("生成访问令牌异常: 用户ID={}", userId, e)
            return Either.Left("生成访问令牌失败，请稍后重试")
        }
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱
     * @param emailCode 邮箱验证码
     * @return 是否验证成功
     */
    private fun verifyEmailCode(
        email: String,
        emailCode: String
    ): Boolean {
        val key = RedisKeys.AUTH_EMAIL_CODE_PREFIX.format(email)
        val storedCode = redisTemplate.opsForValue().get(key)
        return storedCode == emailCode
    }

    /**
     * 验证邀请码
     * @param inviterCode 邀请码
     * @return 邀请人的id, 为null表示邀请码无效
     */
    private fun verifyInviterCode(
        inviterCode: String
    ): Long? {
        val key = RedisKeys.AUTH_INVITER_CODE_PREFIX.format(inviterCode)
        return redisTemplate.opsForValue().get(key)?.toLongOrNull()
    }


    /**
     * 处理令牌并获取访问令牌
     * @param user 用户
     * @return 访问令牌
     */
    private fun processTokensAndGetAccess(user: User): String {
        user.refreshToken = RandomUtil.generateCode(32)
        user.refreshTokenExpireAt = Instant.now().plus(TokenExpiration.REFRESH_TOKEN_EXPIRATION)

        // B. 处理 Redis
        val accessToken = RandomUtil.generateCode(32)
        val key = RedisKeys.AUTH_USER_ACCESS_TOKEN.format(user.id.value)
        redisTemplate.opsForValue().set(
            key,
            accessToken,
            TokenExpiration.ACCESS_TOKEN_EXPIRATION
        )

        return accessToken
    }
}