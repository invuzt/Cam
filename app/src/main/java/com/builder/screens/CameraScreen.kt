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
import androidx.compose.material3.* import androidx.compose.runtime.* import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color             
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext      
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.builder.utils.*

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
    val useRustEnhance = prefs.getBoolean("use_rust_compress", false)

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
                            Toast.makeText(context, "Applying Rust Vivid Effect...", Toast.LENGTH_SHORT).show()
                            takePhotoVivid(context, controller, options, currentLoc, currentAddress) { preview = it }
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

// Alur Baru: 1 Jepret (Anti-Blur) & Kirim ke Rust untuk Vivid Color
private fun takePhotoVivid(
    context: Context,
    c: LifecycleCameraController,
    opt: WatermarkOptions,
    currentLoc: android.location.Location?,
    currentAddr: String,
    onRes: (Bitmap) -> Unit
) {
    // 1. Ambil hanya SATU foto (Cepat & Anti-Blur)
    c.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(img: ImageProxy) {
            val rotationDegrees = img.imageInfo.rotationDegrees

            // 2. Ubah langsung ke ByteArray di RAM Kotlin
            val buffer = img.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            img.close()

            // 3. Lempar ke Rust Engine untuk Color Grading masif (Rayon Multi-core)
            try {
                val vividResultBytes = NativeLib.processVividEnhance(bytes)

                // 4. Decode hasil Vivid Rust kembali ke Bitmap
                val baseBitmap = BitmapFactory.decodeByteArray(vividResultBytes, 0, vividResultBytes.size)

                // 5. Perbaiki rotasi
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotatedBitmap = Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)

                // 6. Tempelkan watermark dari Kotlin di atas hasil matang Rust
                val finalBitmap = WatermarkManager.apply(rotatedBitmap, currentLoc, currentAddr, opt)

                // 7. Simpan hasil akhir ke galeri
                FileManager.saveImageToGallery(context, finalBitmap, true)
                
                // Tampilkan ke UI Preview
                onRes(finalBitmap)
            } catch (e: Exception) {
                Log.e("Err", "Rust Vivid Enhancement Failed: $e")
                // Jika gagal, tampilkan toast dan gunakan foto biasa tanpa enhance
                Toast.makeText(context, "Enhance failed, saving normal photo", Toast.LENGTH_SHORT).show()
                val normalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                onRes(normalBitmap)
            }
        }

        override fun onError(e: ImageCaptureException) {
            Log.e("Err", "Vivid Capture failed: $e")
        }
    })
}
