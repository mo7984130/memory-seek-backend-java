package cn.driftcloud.common.response

import arrow.core.Either
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
data class R<T>(
    val data: T,
    val code: Int?,
    val msg: String?
) {
    companion object {
        fun <T> success(data: T): R<T> {
            return R(data, 200, null)
        }

        fun <T> ok(data: T): R<T> {
            return R(data, 200, null)
        }

        fun <T> ok(data: T, msg: String): R<T> {
            return R(data, 200, msg)
        }

        fun error(code: Int, msg: String?): R<Nothing?> {
            return R(null, code, msg)
        }

        fun error(code: HttpStatus, msg: String?): R<Nothing?> {
            return R(null, code.value(), msg)
        }

        fun error(msg: String?): R<Nothing?> {
            return R(null, 500, msg)
        }
    }
}

fun <T> Either<String, T>.toRE(status: HttpStatus = HttpStatus.BAD_REQUEST): ResponseEntity<R<out T?>> {
    return this.fold(
        { errorMsg ->
            // 业务失败：返回 400 状态码，并在 Body 中携带错误信息
            ResponseEntity.status(status).body(R.error(status.value(), errorMsg))
        },
        { data ->
            ResponseEntity.ok(R.ok(data))
        }
    )
}
