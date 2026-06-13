package cn.driftcloud.common.annotation.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.ReportAsSingleViolation
import jakarta.validation.constraints.Pattern
import kotlin.reflect.KClass

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/20
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
@Pattern(regexp = """^[^<>/\\"'&]+$""")
@ReportAsSingleViolation
annotation class ValidNormalChar (
    val message: String = "不能包含 < > / \\ \" ' & 等特殊符号",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)