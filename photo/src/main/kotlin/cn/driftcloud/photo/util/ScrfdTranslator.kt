package cn.driftcloud.photo.util

import ai.djl.modality.cv.Image
import ai.djl.modality.cv.output.BoundingBox
import ai.djl.modality.cv.output.Rectangle
import ai.djl.ndarray.NDList
import ai.djl.ndarray.types.DataType
import ai.djl.ndarray.types.Shape
import ai.djl.translate.Batchifier
import ai.djl.translate.Translator
import ai.djl.translate.TranslatorContext

/**
 * 人脸检测结果封装
 */
data class FaceDetection(
    val boundingBox: BoundingBox,
    val score: Float,
    val landmarks: FloatArray
)

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
class ScrfdTranslator(
    private val threshold: Float = 0.5f,
    private val nmsThreshold: Float = 0.45f
) : Translator<Image, List<FaceDetection>> {

    private val strides = intArrayOf(8, 16, 32)
    private val inputSize = 640

    override fun processInput(ctx: TranslatorContext, input: Image): NDList {
        val resizedImg = input.resize(inputSize, inputSize, true)
        val hwcPixels = resizedImg.toNDArray(ctx.ndManager).toType(DataType.FLOAT32, false).toFloatArray()
        val chwPixels = FloatArray(3 * inputSize * inputSize)
        val area = inputSize * inputSize
        for (i in 0 until area) {
            chwPixels[i] = (hwcPixels[i * 3] - 127.5f) / 128.0f
            chwPixels[i + area] = (hwcPixels[i * 3 + 1] - 127.5f) / 128.0f
            chwPixels[i + 2 * area] = (hwcPixels[i * 3 + 2] - 127.5f) / 128.0f
        }
        return NDList(ctx.ndManager.create(chwPixels, Shape(1, 3, 640, 640)))
    }

    override fun processOutput(ctx: TranslatorContext, list: NDList): List<FaceDetection> {
        val results = mutableListOf<FaceDetection>()
        for (i in strides.indices) {
            val stride = strides[i]
            val featSize = inputSize / stride
            val scores = list[i].toFloatArray()
            val bboxes = list[i + 3].toFloatArray()
            val kps = list[i + 6].toFloatArray()
            val numAnchors = scores.size / (featSize * featSize)

            for (idx in scores.indices) {
                if (scores[idx] < threshold) continue

                val anchorIdx = idx / numAnchors
                val gridY = (anchorIdx / featSize).toFloat()
                val gridX = (anchorIdx % featSize).toFloat()

                // 核心修复：坐标解码公式
                val bIdx = idx * 4
                val x1 = (gridX - bboxes[bIdx]) * stride
                val y1 = (gridY - bboxes[bIdx + 1]) * stride
                val x2 = (gridX + bboxes[bIdx + 2]) * stride
                val y2 = (gridY + bboxes[bIdx + 3]) * stride

                val rect = Rectangle(
                    (x1 / inputSize).toDouble().coerceIn(0.0, 1.0),
                    (y1 / inputSize).toDouble().coerceIn(0.0, 1.0),
                    ((x2 - x1) / inputSize).toDouble().coerceIn(0.0, 1.0),
                    ((y2 - y1) / inputSize).toDouble().coerceIn(0.0, 1.0)
                )

                val lms = FloatArray(10)
                val kIdx = idx * 10
                for (k in 0 until 5) {
                    // 将预测的偏移值还原到 640x640 空间
                    lms[k * 2] = (gridX + kps[kIdx + k * 2]) * stride
                    lms[k * 2 + 1] = (gridY + kps[kIdx + k * 2 + 1]) * stride
                }
                results.add(FaceDetection(rect, scores[idx], lms))
            }
        }
        return applyNMS(results)
    }

    private fun applyNMS(detections: List<FaceDetection>): List<FaceDetection> {
        if (detections.isEmpty()) return emptyList()
        val sorted = detections.sortedByDescending { it.score }.toMutableList()
        val selected = mutableListOf<FaceDetection>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            val it = sorted.iterator()
            while (it.hasNext()) {
                if (best.boundingBox.getIoU(it.next().boundingBox) > nmsThreshold) it.remove()
            }
        }
        return selected
    }

    override fun getBatchifier(): Batchifier? = null
}