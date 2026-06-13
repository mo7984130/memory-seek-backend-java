package cn.driftcloud.common.config

import cn.driftcloud.common.response.R
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class GlobalExceptionReactiveHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionReactiveHandler::class.java)

    @ExceptionHandler(org.springframework.web.reactive.resource.NoResourceFoundException::class)
    fun handleNoResourceFound(ex: Exception): ResponseEntity<R<Nothing?>> {
        logger.warn("响应式资源不存在异常", ex)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(R.error(404, "接口路径不存在"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception): ResponseEntity<R<Nothing?>> {
        logger.error("响应式全局异常", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.error(500, "服务器内部错误"))
    }
}