package com.bsuyeon.mlkitinterlock

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark

/**
 * Ml Kit Post Analyzer
 * Get image from camera(when stream_mode) and analyze pose
 */
@ExperimentalGetImage
class PoseAnalyzer(
    private val poseDetector: PoseDetector
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        println("analyze")
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    println(
                        "Nose 3D Position ${pose.getPoseLandmark(PoseLandmark.NOSE)?.position3D}"
                    )
                }
                .addOnFailureListener { e ->
                    println("error $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    mediaImage.close()
                }
        }
    }
}