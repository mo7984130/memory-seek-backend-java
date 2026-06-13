package cn.driftcloud.auth.annotation.validation

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
class AccountValidator : ConstraintValidator<ValidAccount, String> {

    private val emailRegex = Regex(
        "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$"
    )

    // 用户名正则
    private val usernameRegex = Regex(UsernameValidConfig.pattern)

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        // 1. 如果为空，交给 @NotBlank 处理，这里直接返回 true
        if (value.isNullOrBlank()) {
            return true
        }

        // 2. 分支逻辑
        if (value.contains("@")) {
            // --- 邮箱校验 ---
            if (!emailRegex.matches(value)) {
                disableDefaultAndBuildMessage(context, "请输入正确的邮箱地址")
                return false
            }
        } else {
            if (!usernameRegex.matches(value)) {
                disableDefaultAndBuildMessage(context, UsernameValidConfig.charErrorMsg)
                return false
            }
            if (value.length !in UsernameValidConfig.minLength..UsernameValidConfig.maxLength) {
                disableDefaultAndBuildMessage(context, UsernameValidConfig.lenErrorMsg)
                return false
            }
        }
        return true
    }

    // 辅助函数：替换默认的报错信息
    private fun disableDefaultAndBuildMessage(context: ConstraintValidatorContext, message: String) {
        context.disableDefaultConstraintViolation()
        context.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation()
    }
}