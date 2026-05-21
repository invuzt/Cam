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
import kotlinx.coroutines.delay

@Composable
fun CameraScreen(
    controller: LifecycleCameraController,
    currentLoc: Location?,
    onOpenGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onCapturePhoto: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoRecorder = remember { VideoRecorder(context) }
    var isRecording by remember { mutableStateOf(false) }
    var secondsRecorded by remember { mutableStateOf(0) }

    // Timer Logic
    LaunchedEffect(isRecording) {
        if (isRecording) {
            secondsRecorded = 0
            while (isRecording) {
                delay(1000)
                secondsRecorded++
            }
        }
    }

    val timeString = remember(secondsRecorded) {
        val mins = secondsRecorded / 60
        val secs = secondsRecorded % 60
        String.format("%02d:%02d", mins, secs)
    }

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
            // Top Bar dengan Timer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateToSettings, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) {
                    Icon(Icons.Default.Settings, null, tint = Color.White)
                }
                
                if (isRecording) {
                    Row(
                        modifier = Modifier.background(Color.Black.copy(0.6f), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color.Red, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                        Spacer(Modifier.width(8.dp))
                        Text(timeString, color = Color.White, style = MaterialTheme.typography.bodyMedium)
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

                // Tombol FOTO
                Box(modifier = Modifier.size(80.dp).border(BorderStroke(4.dp, Color.White), CircleShape).padding(4.dp)) {
                    IconButton(onClick = onCapturePhoto, modifier = Modifier.fillMaxSize().background(Color.White, CircleShape)) {
                        Icon(Icons.Default.Camera, null, tint = Color.Black, modifier = Modifier.size(40.dp))
                    }
                }

                // Tombol VIDEO
                IconButton(
                    onClick = {
                        if (isRecording) {
                            videoRecorder.stopRecording()
                            isRecording = false
                        } else {
                            videoRecorder.startRecording { isRecording = false }
                            isRecording = true
                        }
                    },
                    modifier = Modifier.size(60.dp).background(if (isRecording) Color.White else Color.Red, CircleShape)
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
