package cn.driftcloud.common.pojo.vo

/**
 * @author DriftCloud
 * @Description 分页响应
 * @Date 2026/2/4
 */
data class CursorPageVO<T, C>(
    val records: List<T>,
    val nextCursor: C?,
    val hasMore: Boolean,
) {
    companion object {
        fun <T, C> emptyCursorPageVO(): CursorPageVO<T, C> {
            return CursorPageVO<T, C>(emptyList(), null, false)
        }
    }
}

data class CursorPageRequest<C>(
    val cursor: C?,
    val size: Int = 20
)