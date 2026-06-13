package cn.driftcloud.common.annotation

import java.lang.annotation.Inherited
import java.util.concurrent.TimeUnit

/**
 * @author DriftCloud
 * @Description 接口限流注解
 * 采用 Redis setIfAbsent (SETNX) 实现，适用于“一定时间内只允许访问一次”的场景
 * @Date 2026/1/12
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
@Inherited
annotation class RateLimit(
    /**
     * 限流的 Key 后缀，支持 SpEL 表达式
     * 例如: "#userId", "#dto.mobile", "#request.remoteAddr"
     */
    val key: String = "",

    /**
     * 限制时间 (默认 60)
     */
    val time: Long = 60,

    /**
     * 时间单位 (默认秒)
     */
    val unit: TimeUnit = TimeUnit.SECONDS,

     /**
     * 限制次数 (默认 1)
     */
    val limit: Long = 1,

    /**
     * 被限流时的提示信息
     */
    val message: String = "请求过于频繁，请稍后重试"
)
