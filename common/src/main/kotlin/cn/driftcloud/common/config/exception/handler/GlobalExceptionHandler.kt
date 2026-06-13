package cn.driftcloud.common.config

import cn.driftcloud.common.response.R
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class GlobalExceptionServletHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionServletHandler::class.java)

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException::class)
    fun handleNoResourceFound(ex: Exception): ResponseEntity<R<Nothing?>> {
        logger.warn("资源不存在异常", ex)
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(R.error(404, "资源不存在"))
    }

    @ExceptionHandler(BindException::class, MethodArgumentNotValidException::class)
    fun handleValidationException(ex: Exception): ResponseEntity<R<Nothing?>> {
        val message = when(ex) {
            is BindException -> ex.bindingResult.allErrors.firstOrNull()?.defaultMessage
            is MethodArgumentNotValidException -> ex.bindingResult.allErrors.firstOrNull()?.defaultMessage
            else -> "参数校验失败"
        }
        logger.warn("参数校验异常: {}", message, ex)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(R.error(400, message ?: "参数错误"))
    }

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(ex: Exception): ResponseEntity<R<Nothing?>> {
        logger.error("全局异常", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(R.error(500, "服务器内部错误"))
    }
}