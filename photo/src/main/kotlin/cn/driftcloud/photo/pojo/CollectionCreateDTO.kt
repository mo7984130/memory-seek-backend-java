package cn.driftcloud.photo.pojo

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * @author DriftCloud
 * @Description 创建收藏夹DTO
 * @Date 2026/2/6
 */
data class CollectionCreateDTO(
    @get:NotBlank(message = "收藏夹名称不能为空")
    @get:Size(max = 20, message = "收藏夹名称不能超过20个字符")
    val name: String,

    @get:Size(max = 1000, message = "收藏夹描述不能超过1000个字符")
    val description: String? = null
)