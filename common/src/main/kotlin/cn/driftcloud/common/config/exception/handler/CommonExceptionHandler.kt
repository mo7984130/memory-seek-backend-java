package cn.driftcloud.common.config

import cn.driftcloud.common.exception.RateLimitException
import cn.driftcloud.common.exception.auth.UnauthorizedException
import cn.driftcloud.common.response.R
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.sql.SQLException

/**
 * 通用异常处理器：处理与 Web 容器无关的异常
 * @author driftcloud
 */
@RestControllerAdvice
class CommonExceptionHandler {
    private val log = LoggerFactory.getLogger(CommonExceptionHandler::class.java)

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(ex: UnauthorizedException): ResponseEntity<R<Nothing?>> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(R.error(401, ex.message ?: "认证失败"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<R<Nothing?>> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.error(400, ex.message ?: "参数错误"))
    }

    @ExceptionHandler(value = [SQLException::class, DataAccessException::class])
    fun handleSqlException(e: Exception): R<Nothing?> {
        log.error("数据库操作异常: ", e)
        return R.error(500, "系统数据访问异常")
    }

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(ex: RateLimitException): ResponseEntity<R<Nothing?>> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(R.error(429, ex.message ?: "请求过于频繁"))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<R<Nothing?>> {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(R.error(405, ex.message ?: "请求方法不允许"))
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<R<Nothing?>> {
        log.error("运行时异常: ", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.error(500, "系统异常: ${ex.message}"))
    }
}