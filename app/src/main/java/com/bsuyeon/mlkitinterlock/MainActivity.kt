package com.bsuyeon.mlkitinterlock

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.loader.ResourcesLoader
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceView
import androidx.camera.core.*
import androidx.camera.core.CameraSelector.LENS_FACING_FRONT
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bsuyeon.mlkitinterlock.databinding.ActivityMainBinding
import com.google.android.filament.*
import com.google.android.filament.gltfio.FilamentInstance
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.utils.*
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.nio.ByteBuffer
import java.util.concurrent.Executors


@ExperimentalGetImage
class MainActivity : Activity() {
    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var previewView: PreviewView
    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val options = PoseDetectorOptions.Builder()
        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
        .build()
    private val poseDetector: PoseDetector = PoseDetection.getClient(options)


    @SuppressLint("Range")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        previewView = findViewById(R.id.previewView)
        setPoseAnalyzer()
        setSurfaceView()

        // Request camera permissions
        if (allPermissionsGranted()) {
//            startCamera()
            loadGlb("t-pose")
            modelViewer.scene.skybox =
                Skybox.Builder().color(1f, 1f, 1f, 1f).build(modelViewer.engine)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
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

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(LENS_FACING_FRONT).build()
            val lifecycle = CustomLifeCycle()
            lifecycle.doOnResume()
            lifecycle.doOnStart()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycle, cameraSelector, preview, imageAnalysis
            )
            preview.setSurfaceProvider(
                previewView.surfaceProvider
            )
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * If you only have fbx format file, then need to convert to glb(gltf)
     * You can use blender(import fbx, export gltf)
     * Just use blender, if you use free convert website may lost joint and skin data.
     */
    private fun loadGlb(name: String) {
        val buffer = readAsset("models/${name}.glb")
        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    private fun readAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(currentTime: Long) {
            val seconds = (currentTime - startTime).toDouble() / 1_000_000_000
            choreographer.postFrameCallback(this)
            modelViewer.asset?.apply {
                modelViewer.transformToUnitCube()
                var rightHand: Int = getFirstEntityByName("mixamorig:RightHand")
                val rightHandTransform = getTransform(rightHand)
                val degrees = 20f * seconds.toFloat()
                val axis = Float3(0f, 0f, 1f)
                setTransform(rightHand, rightHandTransform * rotation(axis, degrees))
//                root transform works but...
//                val rootTransform = getTransform(root)
//                setTransform(root, rootTransform * rotation(axis, degrees))
            }
            modelViewer.render(currentTime)
        }
    }

    private fun getTransform(entity: Int): Mat4 {
        val tm = modelViewer.engine.transformManager
        return Mat4.of(*tm.getTransform(tm.getInstance(entity), FloatArray(16)))
    }

    private fun setTransform(entity: Int, mat4: Mat4) {
        val tm = modelViewer.engine.transformManager
        tm.setTransform(tm.getInstance(entity), mat4.toFloatArray())
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        imageAnalysis.clearAnalyzer()
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

