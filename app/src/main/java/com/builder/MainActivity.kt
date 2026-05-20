package com.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.LifecycleCameraController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Bundle?(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val controller = LifecycleCameraController(applicationContext)
        
        setContent {
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()
            var showBottomSheet by remember { mutableStateOf(false) }

            // UI Utama: Camera Screen selalu di latar belakang
            Scaffold { padding ->
                CameraScreen(
                    controller = controller,
                    currentLoc = null, // Location logic bisa ditambahkan nanti
                    onOpenGallery = { /* Aksi galeri */ },
                    onNavigateToSettings = { showBottomSheet = true }
                )

                // Bottom Sheet sebagai pengganti navigasi pindah layar (Sesuai saran Video)
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showBottomSheet = false },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ) {
                        // Isi SettingsScreen ada di dalam sheet
                        SettingsScreen(onBack = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) showBottomSheet = false
                            }
                        })
                    }
                }
            }
        }
    }
}
