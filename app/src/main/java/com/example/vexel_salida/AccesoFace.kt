package com.example.vexel_salida

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File

class AccesoFace : AppCompatActivity() {
    private lateinit var cancelBtn : Button
    private lateinit var previewView: PreviewView
    private lateinit var faceDetector: FaceDetector
    private lateinit var imageCapture: ImageCapture
    private var caraDetectada = false

    private val solicitudPermisos = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            iniciarCamara()
        } else {
            Toast.makeText(this, "Se necesita aceptar los permisos de camara", Toast.LENGTH_LONG).show()
        }
    }

    //Permiso de camara
    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acceso_face)

        previewView = findViewById(R.id.previewView)
        cancelBtn = findViewById(R.id.cancel_button)

        cancelBtn.setOnClickListener {
            faceDetector.close()
            finish()
        }

        // Configurar el detector de rostros con clasificación activada
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(options)

        // Solicitar permisos de cámara
        if (verificarPermisos()) {
            iniciarCamara()
        } else {
            solicitudPermisos.launch(arrayOf(Manifest.permission.CAMERA))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetector.close()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configurar la vista previa
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Definir resolución preferida (1920x1080)
            val resolucionPreferida = android.util.Size(1920, 1080)

            // Crear un ResolutionSelector con fotos de 1080p
            val resolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(resolucionPreferida, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
                )
                .build()
            imageCapture = ImageCapture.Builder()
                .setResolutionSelector(resolutionSelector)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Configurar el analizador de imágenes
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        if (caraDetectada) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                            faceDetector.process(image).addOnSuccessListener { faces ->
                                if (faces.isNotEmpty()) {
                                    val cara = faces[0]
                                    val probSonrisa = cara.smilingProbability

                                    if (probSonrisa != null && probSonrisa > 0.5f) {
                                        caraDetectada = true
                                        runOnUiThread {
                                            tomarFoto()
                                        }
                                    }
                                }
                            }
                                .addOnFailureListener { e ->
                                    Log.e("MainActivity", "Error en la detección de rostros", e)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            // Seleccionar la cámara frontal
            val tipoCamara = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, tipoCamara, preview, imageCapture,imageAnalysis)
            } catch (exc: Exception) {
                Log.e("MainActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun tomarFoto() {
        val imageCapture = this.imageCapture ?: return

        val archivoFoto = File(externalCacheDir, "foto_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(archivoFoto).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val mp = MediaPlayer.create(this@AccesoFace,R.raw.beep)
                    mp.start()
                    val intent = Intent(this@AccesoFace, Respuesta::class.java)
                    intent.putExtra("modo", "foto")
                    intent.putExtra("rutaFoto", archivoFoto.absolutePath)
                    startActivity(intent)
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Error al guardar imagen: ${exc.message}", exc)
                }
            }
        )
    }

    private fun verificarPermisos() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}