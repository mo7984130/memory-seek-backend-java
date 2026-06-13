package cn.driftcloud.photo.util

import org.opencv.calib3d.Calib3d
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


/**
 * @author DriftCloud
 * @Description 人脸对齐工具
 * @Date 2026/2/13
 */
object FaceAligner {

    // InsightFace 标准 112x112 参考坐标
    private val DST_POINTS = MatOfPoint2f(
        Point(38.2946, 51.6963), // 左眼
        Point(73.5318, 51.5014), // 右眼
        Point(56.0252, 71.7366), // 鼻子
        Point(41.5493, 92.3655), // 左嘴角
        Point(70.7299, 92.2041)  // 右嘴角
    )

    fun alignFace(srcImg: Mat, landmarks: FloatArray): Mat {
        val srcPoints = MatOfPoint2f(
            Point(landmarks[0].toDouble(), landmarks[1].toDouble()),
            Point(landmarks[2].toDouble(), landmarks[3].toDouble()),
            Point(landmarks[4].toDouble(), landmarks[5].toDouble()),
            Point(landmarks[6].toDouble(), landmarks[7].toDouble()),
            Point(landmarks[8].toDouble(), landmarks[9].toDouble())
        )

        val alignedFace = Mat()
        // 关键：estimateAffinePartial2D 专门用于相似变换，严禁非等比拉伸
        val warpMat = Calib3d.estimateAffinePartial2D(srcPoints, DST_POINTS)

        try {
            if (warpMat.empty()) {
                // 如果计算失败（极少见），回退到非对齐裁剪或返回原图的一块，防止崩溃
                return Mat()
            }

            // 执行仿射变换
            Imgproc.warpAffine(
                srcImg,
                alignedFace,
                warpMat,
                Size(112.0, 112.0),
                Imgproc.INTER_CUBIC
            )
        } finally {
            srcPoints.release()
            warpMat.release()
        }

        return alignedFace
    }
}