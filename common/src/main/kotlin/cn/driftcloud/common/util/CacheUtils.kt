package cn.driftcloud.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/12
 */
@Component
class CacheUtils(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {

    private val logger = org.slf4j.LoggerFactory.getLogger(CacheUtils::class.java)

    fun <T> getOrLoad(
        keyProvider: () -> String,
        type: Class<T>,
        loader: () -> T
    ): T? {
        val key = keyProvider()

        val cachedJson = redisTemplate.opsForValue().get(key)
        if (!cachedJson.isNullOrBlank()) {
            return try {
                objectMapper.readValue(cachedJson, type)
            } catch (e: Exception) {
                logger.error("在缓存中获取数据时出错", e)
                null
            }
        }

        val newValue = loader()
        if (newValue != null) {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(newValue))
        }
        return newValue
    }

    fun <K, V> getOrLoadBatch(
        params: List<K>,
        keyProvider: (K) -> String,
        type: Class<V>,
        loader: (List<K>) -> List<V>,
        resultMapper: (V) -> K
    ): List<V> {
        if (params.isEmpty()) return emptyList()

        // 参数去重
        val distinctParams = params.distinct()
        val paramToKeyMap = distinctParams.associateWith { keyProvider(it) }
        val keys = distinctParams.map { paramToKeyMap[it] }

        // 批量从 REDIS MGET
        val cachedJsons = redisTemplate.opsForValue().multiGet(keys) ?: emptyList()

        val hitMap = mutableMapOf<K, V>()
        val missMap = mutableListOf<K>()

        // 解析缓存
        distinctParams.forEachIndexed { index, param ->
            val json = cachedJsons.getOrNull(index)
            if (json.isNullOrBlank()) {
                missMap.add(param)
            } else {
                try {
                    hitMap[param] = objectMapper.readValue(json, type)
                } catch (e: Exception) {
                    logger.error("在缓存中获取数据时出错", e)
                    missMap.add(param)
                }
            }
        }

        // 加载缺失的数据
        if (missMap.isNotEmpty()) {
            val data = loader(missMap)

            // 使用Pipeline异步回写
            if (data.isNotEmpty()) {
                redisTemplate.executePipelined {
                    data.forEach { item ->
                        val param = resultMapper(item)
                        val key = paramToKeyMap[param]!!
                        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(item))
                        hitMap[param] = item
                    }
                    null
                }
            }
        }

        // 按照原始 params 的顺序和数量返回结果
        return params.mapNotNull { hitMap[it] }
    }

    /**
     * 删除缓存
     * @param key 缓存的 key
     */
    fun delete(key: String) {
        redisTemplate.delete(key)
    }

}