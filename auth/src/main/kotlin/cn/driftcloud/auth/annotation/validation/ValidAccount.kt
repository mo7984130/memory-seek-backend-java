package cn.driftcloud.auth.annotation.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [AccountValidator::class])
@NotBlank(message = "账号不能为空")
annotation class ValidAccount(
    val message: String = "账号格式不正确",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
