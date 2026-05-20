package com.builder.screens                           
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector            
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException     
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController 
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image              
import androidx.compose.foundation.background         
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons          
import androidx.compose.material.icons.filled.Cameraswitch                                                  
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*                   
import androidx.compose.runtime.*                     
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color             
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext      
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.builder.utils.*
import java.io.ByteArrayOutputStream

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    currentLoc: android.location.Location?,
    onOpenGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("camru_prefs", Context.MODE_PRIVATE) }
    val isHighQuality = prefs.getBoolean("hq", true)
    val useRustEnhance = prefs.getBoolean("use_rust_compress", false) // Membaca toggle Rust

    val options = WatermarkOptions(                           
        showTime = prefs.getBoolean("w_time", true),
        showDate = prefs.getBoolean("w_date", true),          
        showCoords = prefs.getBoolean("w_coords", true),
        showAddress = prefs.getBoolean("w_addr", true),
        customText = prefs.getString("w_custom", "") ?: "",
        removeBrand = prefs.getBoolean("w_remove_brand", false)
    )                                                 
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var showFlash by remember { mutableStateOf(false) }

    val currentAddress by produceState(initialValue = "", currentLoc) {
        value = if (options.showAddress) LocationHelper.getAddress(context, currentLoc) else ""
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (preview != null) {
            Image(bitmap = preview!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            IconButton(onClick = { preview = null }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        } else {
            CameraPreview(controller, Modifier.fillMaxSize())

            val alpha by animateFloatAsState(targetValue = if (showFlash) 1f else 0f, animationSpec = tween(100), finishedListener = { showFlash = false })
            Box(Modifier.fillMaxSize().alpha(alpha).background(Color.White))
            
            SmallFloatingActionButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                containerColor = Color.Black.copy(0.5f),                                                                    
                contentColor = Color.White
            ) { Icon(Icons.Default.Settings, null) }
            
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    controller.cameraSelector = if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                        CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                }) { Icon(Icons.Default.Cameraswitch, null, tint = Color.White) }
                
                IconButton(
                    onClick = {
                        showFlash = true                                      
                        if (useRustEnhance) {
                            Toast.makeText(context, "Processing Rust Enhance HDR...", Toast.LENGTH_SHORT).show()
                            takePhotoHDR(context, controller, options, currentLoc, currentAddress) { preview = it }
                        } else {
                            takePhoto(context, controller, isHighQuality, options, currentLoc, currentAddress) { preview = it }
                        }
                    },
                    modifier = Modifier.size(80.dp)
                ) { Icon(Icons.Default.Circle, null, tint = Color.White, modifier = Modifier.size(80.dp)) }
                
                IconButton(onOpenGallery) { Icon(Icons.Default.PhotoLibrary, null, tint = Color.White) }
            }
        }
    }
}

// Alur Lama: Single Shot & Simpan Biasa via Kotlin
private fun takePhoto(
    context: Context,
    c: LifecycleCameraController,
    hq: Boolean,
    opt: WatermarkOptions,
    currentLoc: android.location.Location?,
    currentAddr: String,
    onRes: (Bitmap) -> Unit
) {
    c.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(img: ImageProxy) {
            val b = img.toBitmap().let { src ->
                val matrix = Matrix().apply { postRotate(img.imageInfo.rotationDegrees.toFloat()) }
                Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
            }

            val wm = WatermarkManager.apply(b, currentLoc, currentAddr, opt)                                            
            FileManager.saveImageToGallery(context, wm, hq)
            onRes(wm)
            img.close()
        }
        override fun onError(e: ImageCaptureException) { Log.e("Err", "$e") }
    })
}

// Alur Baru: 3x Burst Shot via Exposure Bracketing untuk disetor ke Rust
private fun takePhotoHDR(
    context: Context,
    c: LifecycleCameraController,
    opt: WatermarkOptions,
    currentLoc: android.location.Location?,
    currentAddr: String,
    onRes: (Bitmap) -> Unit
) {
    val cameraControl = c.cameraControl
    if (cameraControl == null) {
        Log.e("Err", "Camera control not available")
        return
    }

    val exposures = listOf(-2, 0, 2) // Gelap, Normal, Terang
    val capturedBytesList = mutableListOf<ByteArray>()
    var rotationDegrees = 0

    // Fungsi rekursif internal untuk menjepret antrean sekuensial agar eksposur tidak tumpang tindih
    fun captureStep(index: Int) {
        if (index >= exposures.size) {
            // Semua 3 foto berhasil diambil! Kirim ke Rust untuk Stacking & Compress
            try {
                val rustResultBytes = NativeLib.processHDRAndCompress(
                    capturedBytesList[0],
                    capturedBytesList[1],
                    capturedBytesList[2]
                )

                // Decode hasil matang dari Rust kembali ke Bitmap
                val baseBitmap = BitmapFactory.decodeByteArray(rustResultBytes, 0, rustResultBytes.size)

                // Perbaiki rotasi orientasi gambar berdasarkan info sensor kamera
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)

                // Tempelkan watermark dari Kotlin di atas hasil matang Rust
                val finalBitmap = WatermarkManager.apply(rotatedBitmap, currentLoc, currentAddr, opt)

                // Simpan hasil akhir ke galeri
                FileManager.saveImageToGallery(context, finalBitmap, true)
                
                // Tampilkan ke UI Preview
                onRes(finalBitmap)
            } catch (e: Exception) {
                Log.e("Err", "Rust Processing Failed: $e")
            } finally {
                // Kembalikan setelan eksposur kamera ke normal (0)
                cameraControl.setExposureCompensationIndex(0)
            }
            return
        }

        // Setel tingkat kecerahan sensor untuk jepretan saat ini
        cameraControl.setExposureCompensationIndex(exposures[index])

        // Trigger jepretan CameraX
        c.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(img: ImageProxy) {
                rotationDegrees = img.imageInfo.rotationDegrees
                
                // Ubah ImageProxy langsung ke format kompresi ByteArray di RAM tanpa alokasi Bitmap Java
                val buffer = img.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                
                capturedBytesList.add(bytes)
                img.close()

                // Lanjut ke jepretan berikutnya
                captureStep(index + 1)
            }

            override fun onError(e: ImageCaptureException) {
                Log.e("Err", "Capture step $index failed: $e")
                cameraControl.setExposureCompensationIndex(0)
            }
        })
    }

    // Mulai rentetan jepretan pertama
    captureStep(0)
}
