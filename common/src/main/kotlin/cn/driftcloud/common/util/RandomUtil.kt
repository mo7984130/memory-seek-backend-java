package cn.driftcloud.common.util

import java.security.SecureRandom

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
object RandomUtil {
    private const val CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 去掉了容易混淆的 I, 1, O, 0
    private val random = SecureRandom()

    // 生成指定长度的随机码
    fun generateCode(length: Int = 6): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(CHARACTERS[random.nextInt(CHARACTERS.length)])
        }
        return sb.toString()
    }
}