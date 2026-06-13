package cn.driftcloud.photo.pojo

import jakarta.validation.constraints.Size

/**
 * @author DriftCloud
 * @Description 收藏夹编辑DTO
 * @Date 2026/2/7
 */
data class CollectionEditDTO(
    @get:Size(max = 20, message = "收藏夹名称不能超过20个字符")
    val name: String? = null,

    @get:Size(max = 1000, message = "收藏夹描述不能超过1000个字符")
    val description: String? = null
)
