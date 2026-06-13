package cn.driftcloud.common.exception.auth

/**
 * @author DriftCloud
 * @Description 未授权异常
 * @Date 2026/1/12
 */
class UnauthorizedException(message: String) : RuntimeException(message)