package cn.driftcloud.auth.annotation.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import kotlin.reflect.KClass

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@NotBlank(message = "邮箱不能为空")
@Email(message = "邮箱格式不正确")
annotation class ValidEmail (
    val message: String = "邮箱格式错误",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)