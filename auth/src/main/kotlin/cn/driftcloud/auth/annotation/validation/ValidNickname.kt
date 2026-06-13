package cn.driftcloud.auth.annotation.validation

import cn.driftcloud.common.annotation.validation.ValidNormalChar
import com.alibaba.nacos.api.grpc.auto.Payload
import jakarta.validation.Constraint
import jakarta.validation.constraints.NotBlank
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
@NotBlank(message = "昵称不能为空")
@Length(max = 20, message = "昵称长度在 1 到 20 个字符")
@ValidNormalChar
annotation class ValidNickname(
    val message: String = "昵称格式错误",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
