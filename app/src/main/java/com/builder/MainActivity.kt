package com.builder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.view.LifecycleCameraController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val controller = LifecycleCameraController(applicationContext)
        
        setContent {
            val navController = rememberNavController()
            
            // Menggunakan NavHost agar halaman Settings terpisah dan ringan
            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    CameraScreen(
                        controller = controller,
                        currentLoc = null,
                        onOpenGallery = { /* Aksi galeri */ },
                        onNavigateToSettings = { 
                            navController.navigate("settings") 
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(onBack = { 
                        navController.popBackStack() 
                    })
                }
            }
        }
    }
}
