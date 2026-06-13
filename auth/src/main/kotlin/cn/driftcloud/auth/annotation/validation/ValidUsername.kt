package cn.driftcloud.auth.annotation.validation

import com.alibaba.nacos.api.grpc.auto.Payload
import jakarta.validation.Constraint
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import kotlin.reflect.KClass

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@NotBlank(message = "用户名不能为空")
@Length(min = UsernameValidConfig.minLength, max = UsernameValidConfig.maxLength, message = UsernameValidConfig.lenErrorMsg)
@Pattern(regexp = UsernameValidConfig.pattern, message = UsernameValidConfig.charErrorMsg)
annotation class ValidUsername(
    val message: String = "用户名格式错误",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
