package com.builder.screens

import android.location.Location
import android.widget.Toast
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.builder.utils.VideoRecorder
import androidx.core.content.ContextCompat

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    currentLoc: Location?,
    onOpenGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCapturePhoto: () -> Unit // Kita hubungkan ke fungsi jepret Rust
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoRecorder = remember { VideoRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    this.controller = controller
                    controller.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 40.dp, start = 24.dp, end = 24.dp, top = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) {
                    Icon(Icons.Default.Settings, null, tint = Color.White)
                }
                if (isRecording) {
                    Surface(color = Color.Red, shape = CircleShape) {
                        Text("REC", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White)
                    }
                }
            }

            // Bottom Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenGallery, modifier = Modifier.size(56.dp).background(Color.Black.copy(0.2f), CircleShape)) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Tombol FOTO (Besar Putih)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(BorderStroke(4.dp, Color.White), CircleShape)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = onCapturePhoto,
                        modifier = Modifier.fillMaxSize().background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.Default.Camera, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                }

                // Tombol VIDEO (Merah)
                IconButton(
                    onClick = {
                        if (isRecording) {
                            videoRecorder.stopRecording()
                            isRecording = false
                        } else {
                            videoRecorder.startRecording { 
                                isRecording = false
                                Toast.makeText(context, "Video saved to Gallery", Toast.LENGTH_SHORT).show()
                            }
                            isRecording = true
                        }
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .background(if (isRecording) Color.White else Color.Red, CircleShape)
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.RadioButtonChecked,
                        null,
                        tint = if (isRecording) Color.Red else Color.White,
                        modifier = Modifier.size(35.dp)
                    )
                }
            }
        }
    }
}
