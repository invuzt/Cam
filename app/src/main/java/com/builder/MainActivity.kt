package com.builder

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen
import com.builder.utils.NativeLib
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Permissions
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 101)
        }

        setContent {
            val context = applicationContext
            val prefs = remember { context.getSharedPreferences("camru_prefs", Context.MODE_PRIVATE) }
            var currentScreen by remember { mutableStateOf("camera") }
            
            val controller = remember {
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.VIDEO_CAPTURE)
                }
            }

            MaterialTheme {
                Surface {
                    when (currentScreen) {
                        "camera" -> CameraScreen(
                            controller = controller,
                            currentLoc = null,
                            onOpenGallery = { /* Logic Galeri */ },
                            onNavigateToSettings = { currentScreen = "settings" },
                            onCapturePhoto = {
                                val useRust = prefs.getBoolean("use_rust_compress", true)
                                capturePhoto(controller, useRust)
                            }
                        )
                        "settings" -> SettingsScreen(onBack = { currentScreen = "camera" })
                    }
                }
            }
        }
    }

    private fun capturePhoto(controller: LifecycleCameraController, useRust: Boolean) {
        controller.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    image.close()

                    // Proses Rust jika aktif
                    val finalBytes = if (useRust) {
                        NativeLib.processVividEnhance(bytes)
                    } else {
                        bytes
                    }

                    saveImageToGallery(finalBytes)
                    runOnUiThread { Toast.makeText(applicationContext, "Vivid Photo Saved!", Toast.LENGTH_SHORT).show() }
                }

                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread { Toast.makeText(applicationContext, "Error: ${exception.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    private fun saveImageToGallery(bytes: ByteArray) {
        val name = "CamRU_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CamRU")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(bytes)
            }
        }
    }
}
