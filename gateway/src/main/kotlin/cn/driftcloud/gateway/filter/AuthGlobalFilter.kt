package cn.driftcloud.gateway.filter

import cn.driftcloud.common.constant.Headers
import cn.driftcloud.common.constant.RedisKeys
import cn.driftcloud.common.exception.auth.UnauthorizedException
import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/1/12
 */
@Component
class AuthGlobalFilter(
    private val redisTemplate: StringRedisTemplate
): GlobalFilter, Ordered {

    private val logger = LoggerFactory.getLogger(AuthGlobalFilter::class.java)
    private val antPathMatcher = AntPathMatcher()

    private val whiteList = listOf(
        "/auth/auth/**"
    )

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain?
    ): Mono<Void?>? {

        val request = exchange.request
        val path = request.uri.path
        val method = request.method
        
        logger.info("网关请求: 方法={}, 路径={}", method, path)

        // 白名单放行
        if (whiteList.any { antPathMatcher.match(it, path) }) {
            logger.info("白名单放行: 路径={}", path)
            return chain?.filter(exchange)
        }

        try {
            // 获取Id, AccessToken
            val authorization = request.headers.getFirst(Headers.AUTHORIZATION)
            if (authorization.isNullOrBlank()) {
                logger.warn("认证失败: 认证头缺失, 路径={}", path)
                return Mono.error(UnauthorizedException("认证头缺失或格式错误"))
            }
            
            // 解析Id, AccessToken
            val auth = authorization.split(" ")
            if (auth.size != 2) {
                logger.warn("认证失败: 认证头格式错误, 路径={}, 认证头={}", path, authorization)
                return Mono.error(UnauthorizedException("认证头缺失或格式错误"))
            }
            
            val (userId, accessToken) = auth
            logger.debug("解析认证信息: 用户ID={}, 路径={}", userId, path)
            
            // 校验AccessToken
            val redisAccessToken = redisTemplate.opsForValue().get(RedisKeys.AUTH_USER_ACCESS_TOKEN.format(userId.toLong()))
            if (redisAccessToken != accessToken) {
                logger.warn("认证失败: 访问令牌无效, 用户ID={}, 路径={}", userId, path)
                return Mono.error(UnauthorizedException("认证失败"))
            }
            
            logger.info("认证成功: 用户ID={}, 路径={}", userId, path)
            
            // 加入请求头
            val newRequest = exchange.request.mutate()
                .header(Headers.USERID, userId)
                .build()

            // 2. 构建新的 Exchange (将新的 Request 塞进去)
            // 注意：Exchange 也是不可变的，必须 mutate 产生一个新的
            val newExchange = exchange.mutate()
                .request(newRequest)
                .build()

            // 3. 将【新的 Exchange】传递给下游
            return chain?.filter(newExchange)
        } catch (e: Exception) {
            logger.error("网关认证异常: 路径={}", path, e)
            return Mono.error(UnauthorizedException("认证失败"))
        }
    }

    override fun getOrder(): Int {
        return -1
    }


}