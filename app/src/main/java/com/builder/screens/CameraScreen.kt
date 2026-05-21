package com.builder.screens

import android.location.Location
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.builder.utils.VideoRecorder

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    currentLoc: Location?,
    onOpenGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoRecorder = remember { VideoRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }

                if (isRecording) {
                    Text("● REC", color = Color.Red, modifier = Modifier.background(Color.Black.copy(0.5f)).padding(8.dp))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenGallery) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                }

                // Tombol Foto
                Button(onClick = { /* Foto Logic */ }, modifier = Modifier.size(80.dp), shape = CircleShape) {
                    Text("IMG")
                }

                // Tombol Video
                Button(
                    onClick = {
                        if (isRecording) {
                            videoRecorder.stopRecording()
                            isRecording = false
                        } else {
                            videoRecorder.startRecording { isRecording = false }
                            isRecording = true
                        }
                    },
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color.Gray else Color.Red)
                ) {
                    Text(if (isRecording) "STOP" else "REC")
                }
            }
        }
    }
}
