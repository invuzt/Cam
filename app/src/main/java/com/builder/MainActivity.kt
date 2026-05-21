package com.builder

import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {
    private var currentLocation by mutableStateOf<Location?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _ -> 
            // Langsung tarik lokasi terakhir (simpel & stabil)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) currentLocation = loc
                }
            } catch (e: SecurityException) {}
        }
        
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        val controller = LifecycleCameraController(applicationContext)
        
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "camera") {
                composable("camera") {
                    CameraScreen(
                    onCapturePhoto = { takePhoto() },
                        controller = controller,
                        currentLoc = currentLocation,
                        onOpenGallery = {
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
