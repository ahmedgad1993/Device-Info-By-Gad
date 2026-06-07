package com.example

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

@Composable
fun TestsTab(viewModel: DeviceViewModel) {
    val context = LocalContext.current
    var activeTest by remember { mutableStateOf("") }
    
    // Vibration Logic
    val triggerVibration = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)
        }
    }
    
    // Flashlight Logic
    var isFlashlightOn by remember { mutableStateOf(false) }
    val toggleFlashlight = {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            isFlashlightOn = !isFlashlightOn
            cameraManager.setTorchMode(cameraId, isFlashlightOn)
        } catch (e: Exception) {
            // Flashlight error
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
    } else if (activeTest == "multi_paint") {
        MultiPaintTest { activeTest = "" }
    } else {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.tab_tests).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                    Button(onClick = { activeTest = "touch" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_touch))
                    }
                    Button(onClick = { activeTest = "multitouch" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_multitouch))
                    }
                    Button(onClick = { activeTest = "colors" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_colors))
                    }
                    Button(onClick = { triggerVibration() }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_vibration))
                    }
                    Button(onClick = { toggleFlashlight() }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(if(isFlashlightOn) "Turn Off Flashlight" else stringResource(R.string.test_flashlight))
                    }
                }
            }
            
            // Advanced Tests Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ADVANCED TESTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                    Button(onClick = { activeTest = "mic" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_mic))
                    }
                    Button(onClick = { activeTest = "speaker" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text("Speakers (L/R)")
                    }
                    Button(onClick = { activeTest = "biometrics" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_biometrics))
                    }
                    Button(onClick = { activeTest = "haptics" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_haptics))
                    }
                    Button(onClick = { activeTest = "volume" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_volume))
                    }
                    Button(onClick = { activeTest = "proximity" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_proximity))
                    }
                    Button(onClick = { activeTest = "flash_sos" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_flashlight_sos))
                    }
                    Button(onClick = { activeTest = "gps" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_gps))
                    }
                    Button(onClick = { activeTest = "ear_speaker" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_ear_speaker))
                    }
                    Button(onClick = { activeTest = "gyro" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_gyroscope))
                    }
                    Button(onClick = { activeTest = "multi_paint" }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(stringResource(R.string.test_multi_paint))
                    }
                }
            }
        }
    }
}

@Composable
fun TouchGridTest(onClose: () -> Unit) {
    var coloredTiles by remember { mutableStateOf(setOf<Int>()) }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Button(onClick = onClose, modifier = Modifier.padding(16.dp)) {
            Text("Close Test")
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
            Text("fingers detected", color = Color.LightGray, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onClose) {
                Text("Close Test")
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
            Text("Tap to change color. Tap through all to exit.", color = Color.White, modifier = Modifier.align(Alignment.Center).padding(16.dp), style = MaterialTheme.typography.titleMedium)
        }
    }
}
