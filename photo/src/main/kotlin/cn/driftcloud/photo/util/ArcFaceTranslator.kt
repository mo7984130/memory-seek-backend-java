package cn.driftcloud.photo.util

import ai.djl.modality.cv.Image
import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.types.Shape
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext

/**
 * ArcFace 人脸特征提取转换器
 * 输入：112x112 对齐后的人脸图片
 * 输出：512 维特征向量
 */
class ArcFaceTranslator : Translator<Image, FloatArray> {

    override fun processInput(ctx: TranslatorContext, input: Image): NDList {
        // 2. 转换为 NDArray (uint8) 并立即转为 Float32 以便导出
        val u8Array = input.toNDArray(ctx.ndManager)
        val hwcPixels = u8Array.toType(DataType.FLOAT32, false).toFloatArray()

        // 3. 手动执行 HWC -> CHW 和 归一化 (x - 127.5) / 128.0
        val area = 112 * 112
        val chwPixels = FloatArray(3 * area)

        for (i in 0 until area) {
            val r = hwcPixels[i * 3]
            val g = hwcPixels[i * 3 + 1]
            val b = hwcPixels[i * 3 + 2]

            chwPixels[i] = (b - 127.5f) / 128.0f           // B
            chwPixels[i + area] = (g - 127.5f) / 128.0f    // G
            chwPixels[i + 2 * area] = (r - 127.5f) / 128.0f // R
        }

        // 4. 直接创建带 Batch 维度的 NDArray [1, 3, 112, 112]
        // 避开 expandDims 算子
        val nchwArray = ctx.ndManager.create(chwPixels, Shape(1, 3, 112, 112))

        return NDList(nchwArray)
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): FloatArray {
        // 直接导出第一个输出节点的 float 数组
        return list[0].toFloatArray()
    }

    // --- 适配 OnnxRuntime 的关键重写 ---

    override fun getBatchifier(): Batchifier? {
        // 禁用默认的 StackBatchifier，避开不支持的 stack 算子
        return null
    }

    override fun batchProcessInput(ctx: TranslatorContext, inputs: List<Image>): NDList {
        // 即使是批量调用，也手动处理第一张（或者循环处理）
        return processInput(ctx, inputs[0])
    }

    override fun batchProcessOutput(ctx: TranslatorContext, list: NDList): List<FloatArray> {
        return listOf(processOutput(ctx, list))
    }
}