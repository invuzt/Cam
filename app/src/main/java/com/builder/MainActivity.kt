package com.builder

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Auto-request permission saat pertama kali buka
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> }
        
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))

        val controller = LifecycleCameraController(applicationContext)
        
        setContent {
            val navController = rememberNavController()
            
            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    CameraScreen(
                        controller = controller,
                        currentLoc = null,
                        onOpenGallery = {
                            // Fungsi Galeri: Membuka folder foto kamera
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.type = "image/*"
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        },
                        onNavigateToSettings = { navController.navigate("settings") }
                    )
                }
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
