package com.builder.screens

import android.location.Location
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.builder.utils.VideoRecorder
import kotlinx.coroutines.launch

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    currentLoc: Location?,
    onOpenGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val videoRecorder = remember { VideoRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Preview Kamera
        AndroidView(
            factory = { controller },
            modifier = Modifier.fillMaxSize()
        )

        // UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Baris Atas: Settings & Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }

                if (isRecording) {
                    Text(
                        "● REC",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .background(Color.Black.copy(0.5f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Baris Bawah: Galeri, Foto, Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Galeri
                IconButton(
                    onClick = onOpenGallery,
                    modifier = Modifier.size(56.dp).background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }

                // Tombol FOTO (Vivid Rust)
                Button(
                    onClick = { /* Fungsi Foto Rust Kamu */ },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("IMG", color = Color.Black)
                }

                // Tombol VIDEO (Kotlin Native)
                Button(
                    onClick = {
                        if (isRecording) {
                            videoRecorder.stopRecording()
                            isRecording = false
                        } else {
                            videoRecorder.startRecording { /* Handle save */ }
                            isRecording = true
                        }
                    },
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.DarkGray else Color.Red
                    )
                ) {
                    Text(if (isRecording) "STOP" else "REC", color = Color.White)
                }
            }
        }
    }
}
