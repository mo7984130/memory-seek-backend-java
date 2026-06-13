package cn.driftcloud.common.aspect

import cn.driftcloud.common.annotation.RateLimit
import cn.driftcloud.common.exception.RateLimitException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ServerWebExchange
import java.lang.reflect.Method
import java.util.*

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
@Aspect
class RateLimitAspect(
    private val redisTemplate: StringRedisTemplate
) {

    private val log = LoggerFactory.getLogger(RateLimitAspect::class.java)
    private val parser = SpelExpressionParser()
    private val nameDiscoverer = DefaultParameterNameDiscoverer()
    private val limitScript: RedisScript<Long>

    init {
        // Lua 脚本逻辑:
        // KEYS[1]: 限流 Key
        // ARGV[1]: 限制次数 (limit)
        // ARGV[2]: 过期时间 (秒)
        val scriptText = """
            local key = KEYS[1]
            local limit = tonumber(ARGV[1])
            local expireTime = tonumber(ARGV[2])
            
            -- 1. 获取当前值
            local current = redis.call('get', key)
            
            -- 2. 如果存在且已超过限制，返回 0 (限流)
            if current and tonumber(current) >= limit then
                return 0
            end
            
            -- 3. 自增
            current = redis.call('incr', key)
            
            -- 4. 如果是第一次自增 (值为1)，设置过期时间
            -- 这样保证了 INCR 和 EXPIRE 的原子性，不会出现永不过期的 Key
            if tonumber(current) == 1 then
                redis.call('expire', key, expireTime)
            end
            
            -- 5. 返回 1 (通过)
            return 1
        """.trimIndent()

        // 指定返回值类型为 Long
        limitScript = DefaultRedisScript(scriptText, Long::class.java)
    }

    @Around("@annotation(cn.driftcloud.common.annotation.RateLimit) || @annotation(cn.driftcloud.common.annotation.RateLimit.Container)")
    fun around(point: ProceedingJoinPoint): Any? {
        val signature = point.signature as MethodSignature
        val method = signature.method

        val rateLimits = method.getAnnotationsByType(RateLimit::class.java)

        for (rateLimit in rateLimits) {
            val key = generateKey(point, rateLimit)

            // 计算过期时间 (秒)
            val timeInSeconds = rateLimit.unit.toSeconds(rateLimit.time)
            // 兜底：Redis expire 至少需要 1 秒
            val finalTime = if (timeInSeconds < 1) 1 else timeInSeconds

            // 执行 Lua 脚本
            val result = redisTemplate.execute(
                limitScript,
                Collections.singletonList(key), // KEYS[1]
                rateLimit.limit.toString(),     // ARGV[1]
                finalTime.toString()            // ARGV[2]
            )

            // Lua 返回 0 表示被限流
            if (result == 0L) {
                log.warn("Rate limit triggered for key: $key, limit: ${rateLimit.limit}")
                throw RateLimitException(rateLimit.message)
            }
        }

        return point.proceed()
    }

    /**
     * 生成 Redis Key
     * 1. 有 Key: rate_limit:类名.方法名:SpEL值
     * 2. 无 Key: rate_limit:类名.方法名:IP地址
     */
    private fun generateKey(point: ProceedingJoinPoint, rateLimit: RateLimit): String {
        val signature = point.signature as MethodSignature
        val method = signature.method

        // 基础前缀
        val prefix = "rate_limit:${method.declaringClass.simpleName}.${method.name}"

        // 情况 A: 注解配置了 key，使用 SpEL 解析
        if (rateLimit.key.isNotBlank()) {
            return try {
                val suffix = parseSpel(method, point.args, rateLimit.key)
                "$prefix:$suffix"
            } catch (e: Exception) {
                log.error("SpEL parsing failed for key: ${rateLimit.key}", e)
                "$prefix:error"
            }
        }

        // 情况 B: 注解未配置 key，使用 IP 限流
        val ip = getIpAddress(point)
        return "$prefix:$ip"
    }

    /**
     * 获取客户端 IP 地址
     * 兼容 WebFlux (需参数包含 ServerWebExchange) 和 Spring MVC
     */
    private fun getIpAddress(point: ProceedingJoinPoint): String {
        // 1. 尝试 WebFlux 方式：遍历参数寻找 ServerWebExchange
        point.args.forEach { arg ->
            if (arg is ServerWebExchange) {
                return getIpFromExchange(arg)
            }
        }

        // 2. 尝试 Spring MVC 方式 (依赖 RequestContextHolder)
        try {
            val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            if (attributes != null) {
                val request = attributes.request
                return getIpFromServlet(request)
            }
        } catch (e: Exception) {
            // 忽略 MVC 上下文获取失败 (例如在非 Web 线程或纯 WebFlux 环境中)
        }

        return "unknown-ip"
    }

    // WebFlux 从 Exchange 获取 IP
    private fun getIpFromExchange(exchange: ServerWebExchange): String {
        val request = exchange.request
        val headers = request.headers

        // 优先获取 X-Forwarded-For (经过 Nginx/Gateway 代理的情况)
        val xForwardedFor = headers.getFirst("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank() && !"unknown".equals(xForwardedFor, ignoreCase = true)) {
            return xForwardedFor.split(",")[0].trim()
        }

        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }

    // Spring MVC 从 HttpServletRequest 获取 IP
    private fun getIpFromServlet(request: jakarta.servlet.http.HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank() && !"unknown".equals(xForwardedFor, ignoreCase = true)) {
            return xForwardedFor.split(",")[0].trim()
        }
        return request.remoteAddr ?: "unknown"
    }

    /**
     * 解析 SpEL 表达式
     */
    private fun parseSpel(method: Method, args: Array<Any>, keyExpression: String): String {
        val paramNames = nameDiscoverer.getParameterNames(method) ?: return ""
        val context = StandardEvaluationContext()

        // 将参数名和参数值放入上下文
        for (i in paramNames.indices) {
            // 安全检查，防止参数越界
            if (i < args.size) {
                context.setVariable(paramNames[i], args[i])
            }
        }

        // 解析表达式并转为 String (处理 Long, Int 等类型)
        val expression = parser.parseExpression(keyExpression)
        val value = expression.getValue(context)

        return value?.toString() ?: ""
    }

}