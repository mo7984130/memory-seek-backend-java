package cn.driftcloud.auth.annotation.validation

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
object UsernameValidConfig{
    const val minLength: Int = 4
    const val maxLength: Int = 20
    const val lenErrorMsg: String = "账号长度在 $minLength 到 $maxLength 个字符"
    const val charErrorMsg: String = "用户名只能包含字母、数字、下划线和短横线"
    const val pattern: String = "^[a-zA-Z0-9_-]+$"
}