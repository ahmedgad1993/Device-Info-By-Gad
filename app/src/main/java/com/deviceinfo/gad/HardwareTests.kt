package com.deviceinfo.gad
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import com.deviceinfo.gad.R

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@androidx.compose.foundation.layout.ExperimentalLayoutApi
@Composable
fun TestsTab(viewModel: CpuHardwareViewModel) {
    val context = LocalContext.current
    val pm = context.packageManager
    var activeTest by remember { mutableStateOf("") }
    val hasFlash = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)
    val hasBiometrics = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT) || pm.hasSystemFeature("android.hardware.biometrics.face") || pm.hasSystemFeature("android.hardware.biometrics.iris")
    val hasMic = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_MICROPHONE)
    val hasGps = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LOCATION_GPS)
    val hasProximity = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_PROXIMITY)
    val hasGyro = pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_GYROSCOPE)
    
    // Vibration Logic
    val triggerVibration = {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(500)
            }
        } catch (e: Exception) {
            android.util.Log.e("HardwareTests", "Error", e)
        }
    }
    
    // Flashlight Logic
    var isFlashlightOn by remember { mutableStateOf(false) }
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraManager != null && cameraManager.cameraIdList.isNotEmpty()) {
                    cameraManager.setTorchMode(cameraManager.cameraIdList[0], false)
                }
            } catch (e: Exception) {}
        }
    }
    
    val toggleFlashlight = {
        try {
            if (cameraManager != null) {
                val idList = cameraManager.cameraIdList
                if (idList.isNotEmpty()) {
                    val cameraId = idList[0]
                    isFlashlightOn = !isFlashlightOn
                    cameraManager.setTorchMode(cameraId, isFlashlightOn)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HardwareTests", "Error", e)
            isFlashlightOn = false
        }
    }

    if (activeTest == "touch") {
        TouchGridTest { activeTest = "" }
    } else if (activeTest == "multitouch") {
        MultiTouchTest { activeTest = "" }
    } else if (activeTest == "colors") {
        DeadPixelTest { activeTest = "" }
    } else if (activeTest == "mic") {
        MicTest { activeTest = "" }
    } else if (activeTest == "speaker") {
        SpeakerTest { activeTest = "" }
    } else if (activeTest == "biometrics") {
        BiometricsTest { activeTest = "" }
    } else if (activeTest == "haptics") {
        HapticsTest { activeTest = "" }
    } else if (activeTest == "volume") {
        VolumeKeysTest { activeTest = "" }
    } else if (activeTest == "proximity") {
        ProximityTest { activeTest = "" }
    } else if (activeTest == "flash_sos") {
        FlashlightSosTest { activeTest = "" }
    } else if (activeTest == "gps") {
        LocationGpsTest { activeTest = "" }
    } else if (activeTest == "ear_speaker") {
        EarSpeakerTest { activeTest = "" }
    } else if (activeTest == "gyro") {
        GyroscopeTest { activeTest = "" }
    } else if (activeTest == "refresh_rate") {
        RefreshRateTest { activeTest = "" }
    } else if (activeTest == "burn_in") {
        BurnInTest { activeTest = "" }
    } else if (activeTest == "multi_paint") {
        MultiPaintTest { activeTest = "" }
    } else {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            
            // Diagnostics Summary
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.ui_device_diagnostics), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.ui_hardware_tests), color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        Icon(androidx.compose.material.icons.Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Text(stringResource(R.string.ui_display_touch), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Touch Grid", icon = androidx.compose.material.icons.Icons.Default.TouchApp, modifier = Modifier.weight(1f)) { activeTest = "touch" }
                    DiagnosticTestCard(title = "Multi-Touch", icon = androidx.compose.material.icons.Icons.Default.PanTool, modifier = Modifier.weight(1f)) { activeTest = "multitouch" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Dead Pixels", icon = androidx.compose.material.icons.Icons.Default.Image, modifier = Modifier.weight(1f)) { activeTest = "colors" }
                    DiagnosticTestCard(title = "Multi Paint", icon = androidx.compose.material.icons.Icons.Default.FormatPaint, modifier = Modifier.weight(1f)) { activeTest = "multi_paint" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Refresh Rate", icon = androidx.compose.material.icons.Icons.Default.Speed, modifier = Modifier.weight(1f)) { activeTest = "refresh_rate" }
                    DiagnosticTestCard(title = "Burn-in Fixer", icon = androidx.compose.material.icons.Icons.Default.FlipToBack, modifier = Modifier.weight(1f)) { activeTest = "burn_in" }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(stringResource(R.string.ui_audio_haptics), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Vibration", icon = androidx.compose.material.icons.Icons.Default.Vibration, modifier = Modifier.weight(1f)) { triggerVibration() }
                    DiagnosticTestCard(title = "Haptics", icon = androidx.compose.material.icons.Icons.Default.WavingHand, modifier = Modifier.weight(1f)) { activeTest = "haptics" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Microphone", icon = androidx.compose.material.icons.Icons.Default.Mic, modifier = Modifier.weight(1f), enabled = hasMic) { activeTest = "mic" }
                    DiagnosticTestCard(title = "Speakers", icon = androidx.compose.material.icons.Icons.Default.Speaker, modifier = Modifier.weight(1f)) { activeTest = "speaker" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Ear Speaker", icon = androidx.compose.material.icons.Icons.Default.Hearing, modifier = Modifier.weight(1f)) { activeTest = "ear_speaker" }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(stringResource(R.string.ui_sensors_hardware), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = if(isFlashlightOn) "Flash: ON" else "Flashlight", icon = androidx.compose.material.icons.Icons.Default.FlashlightOn, modifier = Modifier.weight(1f), enabled = hasFlash) { toggleFlashlight() }
                    DiagnosticTestCard(title = "Biometrics", icon = androidx.compose.material.icons.Icons.Default.Fingerprint, modifier = Modifier.weight(1f), enabled = hasBiometrics) { activeTest = "biometrics" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "Volume Keys", icon = Icons.AutoMirrored.Filled.VolumeUp, modifier = Modifier.weight(1f)) { activeTest = "volume" }
                    DiagnosticTestCard(title = "Proximity", icon = androidx.compose.material.icons.Icons.Default.Visibility, modifier = Modifier.weight(1f), enabled = hasProximity) { activeTest = "proximity" }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiagnosticTestCard(title = "GPS Satellites", icon = androidx.compose.material.icons.Icons.Default.GpsFixed, modifier = Modifier.weight(1f), enabled = hasGps) { activeTest = "gps" }
                    DiagnosticTestCard(title = "Gyroscope", icon = androidx.compose.material.icons.Icons.Default.Sync, modifier = Modifier.weight(1f), enabled = hasGyro) { activeTest = "gyro" }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DiagnosticTestCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    com.deviceinfo.gad.ui.components.GlassCard(
        modifier = modifier.clickable(enabled = enabled) { onClick() }
    ) {
        val alpha = if (enabled) 1f else 0.4f
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary.copy(alpha=alpha), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (enabled) title else "N/A", color = MaterialTheme.colorScheme.onSurface.copy(alpha=alpha), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TouchGridTest(onClose: () -> Unit) {
    var coloredTiles by remember { mutableStateOf(setOf<Int>()) }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Button(onClick = onClose, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.ui_close_test))
        }
        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxSize()) {
            items(50) { index ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(if (coloredTiles.contains(index)) Color.Green else Color.DarkGray)
                        .padding(2.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    coloredTiles = coloredTiles.toMutableSet().apply { add(index) }
                                }
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun MultiTouchTest(onClose: () -> Unit) {
    var pointers by remember { mutableStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        pointers = event.changes.size
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$pointers", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_fingers_detected), color = Color.LightGray, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onClose) {
                Text(stringResource(R.string.ui_close_test))
            }
        }
    }
}

@Composable
fun DeadPixelTest(onClose: () -> Unit) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black, Color.Gray)
    var currentIndex by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors[currentIndex])
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (currentIndex < colors.size - 1) {
                    currentIndex++
                } else {
                    onClose()
                }
            }
    ) {
        if (currentIndex == 0) {
            Text(stringResource(R.string.ui_tap_to_change_color_), color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RefreshRateTest(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }
    val refreshRate = display?.refreshRate ?: 60f

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween((1000f * (120f / refreshRate)).toInt(), easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "xOffset"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Display Refresh Rate", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Text("Reported Rate: ${refreshRate.toInt()} Hz", fontSize = 18.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .graphicsLayer { translationX = xOffset }
                .size(50.dp)
                .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun BurnInTest(onClose: () -> Unit) {
    var colorIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            colorIndex = (colorIndex + 1) % colors.size
        }
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(colors[colorIndex])
        .pointerInput(Unit) {
            detectTapGestures {
                colorIndex = (colorIndex + 1) % colors.size
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onClose, modifier = Modifier.padding(32.dp)) {
            Text("End Burn-in Fixer")
        }
    }
}
