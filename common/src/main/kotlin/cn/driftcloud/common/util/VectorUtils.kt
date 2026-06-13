package cn.driftcloud.common.util

import kotlin.math.sqrt

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/15
 */
object VectorUtils {
    /**
     * L2 归一化：将任意长度的向量映射到单位球面上
     */
    fun l2Normalize(embedding: FloatArray): FloatArray {
        var squareSum = 0.0f
        for (v in embedding) {
            squareSum += v * v
        }

        val norm = sqrt(squareSum)

        // 如果模长接近 0，说明是无效向量，原样返回
        if (norm < 1e-10) return embedding

        return FloatArray(embedding.size) { i -> embedding[i] / norm }
    }

    /**
     * 计算余弦距离 (1 - CosineSimilarity)
     * 归一化后，余弦距离公式简化为：1 - (A dot B)
     */
    fun cosineDistance(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0.0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
        }
        return 1.0f - dotProduct
    }
}