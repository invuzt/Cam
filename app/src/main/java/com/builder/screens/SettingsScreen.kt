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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.builder.utils.WatermarkOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("camru_prefs", Context.MODE_PRIVATE) }

    var isHighQuality by remember { mutableStateOf(prefs.getBoolean("hq", true)) }
    var isPremium by remember { mutableStateOf(prefs.getBoolean("is_premium", false)) }
    var useRustCompress by remember { mutableStateOf(prefs.getBoolean("use_rust_compress", false)) }

    var options by remember {
        mutableStateOf(
            WatermarkOptions(
                showTime = prefs.getBoolean("w_time", true),
                showDate = prefs.getBoolean("w_date", true),
                showCoords = prefs.getBoolean("w_coords", true),
                showAddress = prefs.getBoolean("w_addr", true),
                customText = prefs.getString("w_custom", "") ?: "",
                removeBrand = prefs.getBoolean("w_remove_brand", false)
            )
        )
    }

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
        containerColor = Color(0xFF121212),
        contentColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Engine Optimization", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Cyan)
            Spacer(Modifier.height(12.dp))

            // FITUR BARU: Toggle Khusus untuk mengaktifkan kompresi Rust
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Photo Compress (Rust Engine)", fontWeight = FontWeight.Bold, color = if(useRustCompress) Color.Cyan else Color.White)
                    Text("Kompresi gambar ultra cepat via biner native Rust + Rayon", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(useRustCompress, { active ->
                    useRustCompress = active
                    prefs.edit().putBoolean("use_rust_compress", active).apply()
                }, Modifier.scale(0.8f), colors = SwitchDefaults.colors(checkedThumbColor = Color.Cyan, checkedTrackColor = Color.Cyan.copy(0.4f)))
            }

            Spacer(Modifier.height(16.dp))
            Text("Watermark Config", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Yellow)
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("High Quality Photo (Slow Save)")
                Switch(isHighQuality, { active ->
                    isHighQuality = active
                    prefs.edit().putBoolean("hq", active).apply()
                }, Modifier.scale(0.8f))
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Aktivasi Lisensi Pro (Premium)", color = if(isPremium) Color.Yellow else Color.White)
                Switch(isPremium, { active ->
                    isPremium = active
                    prefs.edit().putBoolean("is_premium", active).apply()
                    if (!active) {
                        options = options.copy(removeBrand = false)
                        prefs.edit().putBoolean("w_remove_brand", false).apply()
                    }
                }, Modifier.scale(0.8f))
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.DarkGray)

            SettingToggle("Tampilkan Jam", options.showTime) {
                options = options.copy(showTime = it)
                prefs.edit().putBoolean("w_time", it).apply()
            }
            SettingToggle("Tampilkan Hari & Tanggal", options.showDate) {
                options = options.copy(showDate = it)
                prefs.edit().putBoolean("w_date", it).apply()
            }
            SettingToggle("Tampilkan Koordinat", options.showCoords) {
                options = options.copy(showCoords = it)
                prefs.edit().putBoolean("w_coords", it).apply()
            }
            SettingToggle("Tampilkan Alamat", options.showAddress) {
                options = options.copy(showAddress = it)
                prefs.edit().putBoolean("w_addr", it).apply()
            }

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    text = "Hilangkan Watermark 'Shot by CakRu'",
                    fontSize = 14.sp,
                    color = if (isPremium) Color.White else Color.Gray
                )
                Switch(
                    checked = options.removeBrand,
                    onCheckedChange = { active ->
                        if (isPremium) {
                            options = options.copy(removeBrand = active)
                            prefs.edit().putBoolean("w_remove_brand", active).apply()
                        }
                    },
                    enabled = isPremium,
                    modifier = Modifier.scale(0.7f)
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = options.customText,
                onValueChange = { text ->
                    options = options.copy(customText = text)
                    prefs.edit().putString("w_custom", text).apply()
                },
                label = { Text("Teks Kustom", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Yellow,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.Yellow
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))
            Text("About App", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Developer: CakRu", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("License: Open Source (MIT)", fontSize = 13.sp, color = Color.LightGray)
                    Spacer(Modifier.height(12.dp))
                    Text("Engine Dependencies:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• Jetpack Compose (Modern UI Ecosystem)\n• CameraX (Core Camera Subsystem)\n• Google Play Services (High-Precision GPS)\n• Kotlin Coroutines (Asynchronous Concurrency)\n• Rust Engine (Parallel Native Image Encoding)", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingToggle(label: String, value: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp)
        Switch(value, onCheckedChange, Modifier.scale(0.7f))
    }
}
