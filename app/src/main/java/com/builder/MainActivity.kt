package com.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.remember
import com.builder.screens.CameraScreen
import com.builder.ui.theme.CamRUTheme
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = applicationContext
            val controller = remember { 
                LifecycleCameraController(context).apply {
                    setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE or LifecycleCameraController.VIDEO_CAPTURE)
                }
            }

            CamRUTheme {
                CameraScreen(
                    controller = controller,
                    currentLoc = null,
                    onOpenGallery = { /* Logic Galeri */ },
                    onNavigateToSettings = { /* Logic Settings */ },
                    onCapturePhoto = {
                        // Logika jepret langsung di sini agar pasti work
                        Toast.makeText(context, "Capturing with Vivid Rust...", Toast.LENGTH_SHORT).show()
                        // Di sini panggil fungsi Rust JNI kamu nantinya
                    }
                )
            }
        }
    }
}
