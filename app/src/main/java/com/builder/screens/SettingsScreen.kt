package com.builder.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("camru_prefs", Context.MODE_PRIVATE) }
    
    // State lengkap termasuk Vivid Rust
    var useRust by remember { mutableStateOf(prefs.getBoolean("use_rust_compress", true)) }
    var isHq by remember { mutableStateOf(prefs.getBoolean("hq", true)) }
    var isPro by remember { mutableStateOf(prefs.getBoolean("is_pro", false)) }
    var showTime by remember { mutableStateOf(prefs.getBoolean("w_time", true)) }
    var showDate by remember { mutableStateOf(prefs.getBoolean("w_date", true)) }
    var showCoords by remember { mutableStateOf(prefs.getBoolean("w_coords", true)) }
    var showAddr by remember { mutableStateOf(prefs.getBoolean("w_addr", true)) }
    var removeBrand by remember { mutableStateOf(prefs.getBoolean("w_remove_brand", false)) }
    var customText by remember { mutableStateOf(prefs.getString("w_custom", "") ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Engine Core", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Toggle Utama yang tadi sempat hilang
            SettingRow("Vivid Rust Enhance", useRust) { useRust = it; prefs.edit().putBoolean("use_rust_compress", it).apply() }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Watermark Config", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))

            SettingRow("High Quality Photo (Slow Save)", isHq) { isHq = it; prefs.edit().putBoolean("hq", it).apply() }
            SettingRow("Aktivasi Lisensi Pro (Premium)", isPro) { isPro = it; prefs.edit().putBoolean("is_pro", it).apply() }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            SettingRow("Tampilkan Jam", showTime) { showTime = it; prefs.edit().putBoolean("w_time", it).apply() }
            SettingRow("Tampilkan Hari & Tanggal", showDate) { showDate = it; prefs.edit().putBoolean("w_date", it).apply() }
            SettingRow("Tampilkan Koordinat", showCoords) { showCoords = it; prefs.edit().putBoolean("w_coords", it).apply() }
            SettingRow("Tampilkan Alamat", showAddr) { showAddr = it; prefs.edit().putBoolean("w_addr", it).apply() }
            SettingRow("Hilangkan Watermark 'Shot by CakRu'", removeBrand) { removeBrand = it; prefs.edit().putBoolean("w_remove_brand", it).apply() }

            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it; prefs.edit().putString("w_custom", it).apply() },
                label = { Text("Teks Kustom", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White)
            )

            Text("About App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Developer: CakRu", color = Color.Yellow, fontWeight = FontWeight.Bold)
                    Text("License: Open Source (MIT)", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Engine Dependencies:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    listOf("Jetpack Compose", "CameraX", "Google Play Services", "Kotlin Coroutines").forEach { 
                        Text("• $it", color = Color.Gray, fontSize = 12.sp) 
                    }
                }
            }
        }
    }
}

@Composable
fun SettingRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
