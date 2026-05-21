package com.builder

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaScannerConnection
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
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Minta semua izin di awal
        val permissions = arrayOf(
            Manifest.permission.CAMERA, 
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
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
                            onOpenGallery = { /* Tambahkan intent galeri jika perlu */ },
                            onNavigateToSettings = { currentScreen = "settings" },
                            onCapturePhoto = {
                                capturePhotoWithRustAndWatermark(controller, prefs)
                            }
                        )
                        "settings" -> SettingsScreen(onBack = { currentScreen = "camera" })
                    }
                }
            }
        }
    }

    private fun capturePhotoWithRustAndWatermark(controller: LifecycleCameraController, prefs: android.content.SharedPreferences) {
        controller.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining()).apply { buffer.get(this) }
                    image.close()

                    // 1. PROSES RUST VIVID (Jika diaktifkan)
                    val useRust = prefs.getBoolean("use_rust_compress", true)
                    var processedBytes = if (useRust) {
                        NativeLib.processVividEnhance(bytes)
                    } else {
                        bytes
                    }

                    // 2. PROSES WATERMARK (Jika diaktifkan)
                    if (!prefs.getBoolean("w_remove_brand", false)) {
                        val customText = prefs.getString("w_custom", "Shot by CamRU") ?: "Shot by CamRU"
                        processedBytes = applyWatermark(processedBytes, customText)
                    }

                    // 3. SIMPAN KE GALERI
                    saveImageToGallery(processedBytes)
                    
                    runOnUiThread { Toast.makeText(applicationContext, "Vivid Photo Saved!", Toast.LENGTH_SHORT).show() }
                }

                override fun onError(exc: ImageCaptureException) {
                    runOnUiThread { Toast.makeText(applicationContext, "Error: ${exc.message}", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    private fun applyWatermark(data: ByteArray, text: String): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = result.width / 25f // Ukuran font proporsional
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
        }
        canvas.drawText(text, 50f, result.height - 100f, paint)
        
        val stream = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        return stream.toByteArray()
    }

    private fun saveImageToGallery(bytes: ByteArray) {
        val name = "CamRU_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/CamRU")
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) }
            MediaScannerConnection.scanFile(this, arrayOf(it.toString()), null, null)
        }
    }
}
