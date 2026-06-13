package cn.driftcloud.auth.controller

import cn.driftcloud.auth.dto.ChangeNicknameDTO
import cn.driftcloud.auth.dto.ChangePasswordDTO
import cn.driftcloud.auth.dto.UserDTO
import cn.driftcloud.auth.dto.toDto
import cn.driftcloud.auth.entity.User
import cn.driftcloud.auth.service.MinioService
import cn.driftcloud.auth.service.UserService
import cn.driftcloud.common.annotation.CurrentUserId
import cn.driftcloud.common.annotation.RateLimit
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.response.R
import cn.driftcloud.common.response.toRE
import cn.driftcloud.common.util.CacheUtils
import cn.driftcloud.auth.service.DBUtils
import cn.driftcloud.common.util.RandomUtil
import jakarta.validation.constraints.NotEmpty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Duration
import java.time.Instant

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/11
 */
@RestController
@RequestMapping("/user")
class UserController(
    private val redisTemplate: StringRedisTemplate,
    private val minioService: MinioService,
    private val userService: UserService,
    private val cacheUtils: CacheUtils,
    private val db: DBUtils
) {

    /**
     * 注销登录
     */
    @GetMapping("/logout")
    fun logout(
        @CurrentUserId userId: Long
    ): R<Nothing?> {
        userService.logout(userId)
        return R.ok(null, "注销登录成功")
    }

    /**
     * 获取用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    fun info(
        @CurrentUserId userId: Long
    ): ResponseEntity<R<out UserDTO?>>
    = userService.getUserInfo(userId).toRE()


    /**
     * 生成邀请码
     * 邀请码有效期为10分钟
     * @return 邀请码
     */
    @GetMapping("/inviter-code")
    @RateLimit(key = "#userId", message = "邀请码生成过于频繁，请稍后重试", time = 10)
    fun generateInviterCode(
        @CurrentUserId userId: Long
    ): R<out Map<String, Any>> {
        val code = generateSequence { RandomUtil.generateCode(6) }
            .first { code ->
                val key = RedisKeys.AUTH_INVITER_CODE_PREFIX.format(code)
                redisTemplate.opsForValue().setIfAbsent(
                    key,
                    userId.toString(),
                    Duration.ofMinutes(10)
                ) == true
            }
        return R.ok(mapOf(
            "inviterCode" to code,
            "expireAt" to Instant.now().plus(Duration.ofMinutes(10))
        ))
    }

    @PostMapping("/avatar")
    fun uploadAvatar(
        @CurrentUserId userId: Long,
        @RequestParam("file") file: MultipartFile
    ) = userService.updateAvatar(userId, file).toRE()

    @PostMapping("/nickname")
    fun changeNickname(
        @CurrentUserId userId: Long,
        @RequestBody @Validated nicknameDTO: ChangeNicknameDTO
    ) = userService.updateNickname(userId, nicknameDTO.nickname).toRE()

    @PostMapping("/password")
    fun changePassword(
        @CurrentUserId userId: Long,
        @RequestBody @Validated passwordDTO: ChangePasswordDTO
    ) = userService.changePassword(userId, passwordDTO.oldPassword, passwordDTO.newPassword).toRE()

    data class UserInfoBatchDTO(
        @field:NotEmpty(message = "ID列表不能为空")
        val userIds: List<Long>
    )
    /**
     * 根据id批量获取用户信息
     */
    @PostMapping("/info/batch")
    fun batchInfo(
        @RequestBody dto: UserInfoBatchDTO
    ) = userService.getUserInfoBatch(dto.userIds).toRE()
}