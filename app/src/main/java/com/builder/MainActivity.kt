package com.builder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.LifecycleCameraController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import com.builder.screens.CameraScreen

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

            MaterialTheme {
                Surface {
                    CameraScreen(
                        controller = controller,
                        currentLoc = null,
                        onOpenGallery = { 
                            Toast.makeText(context, "Membuka Galeri...", Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToSettings = { 
                            Toast.makeText(context, "Ke Settings...", Toast.LENGTH_SHORT).show()
                        },
                        onCapturePhoto = {
                            Toast.makeText(context, "Jepret dengan Vivid Rust!", Toast.LENGTH_SHORT).show()
                            // Nanti panggil fungsi JNI Rust di sini
                        }
                    )
                }
            }
        }
    }
}
