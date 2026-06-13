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
@NotBlank(message = "密码不能为空")
@Length(min = 8, max = 64, message = "密码长度需在 8 到 64 位之间")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S+$",
    message = "需包含字母和数字(包含特殊字符)"
)
annotation class ValidPassword(
    val message: String = "密码格式错误",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)