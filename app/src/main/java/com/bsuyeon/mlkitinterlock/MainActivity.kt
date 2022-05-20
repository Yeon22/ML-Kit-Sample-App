package com.bsuyeon.mlkitinterlock

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bsuyeon.mlkitinterlock.databinding.ActivityMainBinding
import com.google.android.filament.*
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.nio.ByteBuffer
import java.util.concurrent.Executors


@ExperimentalGetImage
class MainActivity : Activity() {
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector: PoseDetector = PoseDetection.getClient(options)
    private val lifeCycle: CustomLifeCycle = CustomLifeCycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setPoseAnalyzer()
        setSurfaceView()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            loadGlb("xbot")
            modelViewer.scene.skybox =
                Skybox.Builder().color(1f, 1f, 1f, 1f).build(modelViewer.engine)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(currentTime: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(currentTime)
        }
    }

    private fun loadGlb(name: String) {
        val buffer = readAsset("${name}.glb")
        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
    }

    private fun setSurfaceView() {
        surfaceView = findViewById(R.id.srfView)
        choreographer = Choreographer.getInstance()
        modelViewer = ModelViewer(surfaceView)
        surfaceView.setOnTouchListener(modelViewer)
    }

    private fun setPoseAnalyzer() {
        imageAnalysis = createImageAnalysis()
        imageAnalysis.setAnalyzer(cameraExecutor, PoseAnalyzer(poseDetector))
    }

    private fun createImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder().build()
    }

    private fun startCamera() {
        Log.i("MainActivity", "startCamera")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = createPreview()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifeCycle, cameraSelector, preview, imageAnalysis
                )

            } catch (exc: Exception) {
                println("Use case binding failed $exc")
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun createPreview(): Preview {
        return Preview.Builder().build().also {
            it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        choreographer.removeFrameCallback(frameCallback)
        cameraExecutor.shutdown()
        imageAnalysis.clearAnalyzer()

        // Stop the animation and any pending frame
        choreographer.removeFrameCallback(frameCallback)
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).toTypedArray()

        init {
            Utils.init()
        }
    }
}

