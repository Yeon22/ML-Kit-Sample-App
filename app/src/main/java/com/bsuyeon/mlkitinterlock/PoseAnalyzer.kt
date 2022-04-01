package com.bsuyeon.mlkitinterlock

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark

@ExperimentalGetImage
class PoseAnalyzer(
    private val poseDetector: PoseDetector
) : ImageAnalysis.Analyzer {
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    Log.d(
                        TAG,
                        "Nose Landmark ${pose.getPoseLandmark(PoseLandmark.NOSE).hashCode()}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "error $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    mediaImage.close()
                }
        }
    }

    companion object {
        private const val TAG = "ML Kit Sample App"
    }
}