package com.builder

import android.Manifest
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.builder.screens.CameraScreen
import com.builder.screens.SettingsScreen
import com.google.android.gms.location.*

class MainActivity : ComponentActivity() {
    private var currentLocation by mutableStateOf<Location?>(null)
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                requestLocationUpdates()
            }
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
                        controller = controller,
                        currentLoc = currentLocation,
                        onOpenGallery = {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
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

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                currentLocation = result.lastLocation
            }
        }

        try {
            // Ambil lokasi terakhir dulu buat pancingan agar tidak "Searching" terus
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) currentLocation = loc
            }
            // Minta update terus menerus tiap 5 detik
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
