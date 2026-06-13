package cn.driftcloud.photo.config

import ai.djl.modality.cv.Image
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import cn.driftcloud.photo.util.ArcFaceTranslator
import cn.driftcloud.photo.util.FaceDetection
import cn.driftcloud.photo.util.ScrfdTranslator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.File

/**
 * @author DriftCloud
 * @Description
 * @Date 2026/2/13
 */
@Configuration
class FaceAiConfig {
    @Value($$"${driftcloud.ai.model.det.path}")
    lateinit var detModalPath: String
    @Value($$"${driftcloud.ai.model.rec.path}")
    lateinit var recModalPath: String

    init {
        // 加载OpenCV库
        nu.pattern.OpenCV.loadShared()
    }

    // 加载 SCRFD 检测模型
    @Bean
    fun faceDetectionModel(): ZooModel<Image, List<FaceDetection>> {
        val criteria = Criteria.builder()
            .setTypes(Image::class.java, List::class.java as Class<List<FaceDetection>>)
            .optModelUrls(File(detModalPath).toURI().toString())
            .optEngine("OnnxRuntime")
            // 这里需要自定义 Translator 来解析 SCRFD 的多层输出 (scores, bboxes, kps)
            .optTranslator(ScrfdTranslator())
            .build()
        return criteria.loadModel().also { it.warmUp(640, 640) }
    }

    // 加载 ArcFace 识别模型
    @Bean
    fun faceRecognitionModel(): ZooModel<Image, FloatArray> {
        val criteria = Criteria.builder()
            .setTypes(Image::class.java, FloatArray::class.java)
            .optModelUrls(File(recModalPath).toURI().toString())
            .optEngine("OnnxRuntime")
            // 标准 ArcFace Translator: 输入 112x112, 归一化 (x-127.5)/128
            .optTranslator(ArcFaceTranslator())
            .build()
        return criteria.loadModel().also { it.warmUp(112, 112) }
    }
}

/**
 * 预热模型
 */
private fun <I, O> ZooModel<I, O>.warmUp(width: Int, height: Int) {
    // 既然指定了 OpenCV 图像工厂，就必须用 Mat
    val mat = org.opencv.core.Mat.zeros(height, width, org.opencv.core.CvType.CV_8UC3)
    try {
        // 使用 OpenCVImageFactory 将 Mat 包装成 Image
        val factory = ai.djl.opencv.OpenCVImageFactory()
        val img = factory.fromImage(mat)

        this.newPredictor().use { it.predict(img as I) }
    } finally {
        // 记得手动释放堆外内存
        mat.release()
    }
}