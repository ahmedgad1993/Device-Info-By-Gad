package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import android.location.LocationManager
import android.location.LocationListener
import android.location.Location
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.log10

@Composable
fun ProximityTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var isNear by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val proxSensor = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                    isNear = event.values[0] < (proxSensor?.maximumRange ?: 5f)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, proxSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    val bgColor = if (isNear) Color.Red else MaterialTheme.colorScheme.background

    Column(modifier = Modifier.fillMaxSize().background(bgColor).padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Proximity Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if(isNear) Color.White else MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(16.dp))
        Text(if (isNear) "HAND DETECTED" else "HOVER HAND OVER TOP OF SCREEN", fontSize = 18.sp, color = if(isNear) Color.White else MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun FlashlightSosTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0]
            val sPattern = listOf(200L, 200L, 200L, 200L, 200L, 200L) 
            val oPattern = listOf(600L, 200L, 600L, 200L, 600L, 200L)
            
            suspend fun flashPattern(times: List<Long>) {
                for (i in times.indices) {
                    if (!isRunning) return
                    val on = i % 2 == 0
                    cameraManager.setTorchMode(cameraId, on)
                    delay(times[i])
                }
            }
            
            while (isRunning) {
                flashPattern(sPattern)
                delay(400)
                flashPattern(oPattern)
                delay(400)
                flashPattern(sPattern)
                delay(1000)
            }
            cameraManager.setTorchMode(cameraId, false)
        } catch (e: Exception) {
            // Flashlight error
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isRunning = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
            drawCircle(color = Color.Yellow)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Transmitting S-O-S Signal...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { isRunning = false; onClose() }) { Text("Stop & Close") }
    }
}

@Composable
fun LocationGpsTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var locationData by remember { mutableStateOf("Waiting for GPS lock...") }
    var satellites by remember { mutableStateOf("0") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }

    DisposableEffect(hasPermission) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var listener: LocationListener? = null
        if (hasPermission) {
            listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    locationData = "Lat: ${loc.latitude}\nLon: ${loc.longitude}\nAccuracy: ${loc.accuracy} meters\nProvider: ${loc.provider}"
                    satellites = loc.extras?.getInt("satellites")?.toString() ?: "Unknown"
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            try {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener!!)
            } catch (e:SecurityException) {}
        } else {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        onDispose {
            listener?.let { lm.removeUpdates(it) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("GPS & Location Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        Text(locationData, fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text("Satellites: $satellites", fontSize = 16.sp, modifier = Modifier.padding(bottom = 32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun VolumeKeysTest(onClose: () -> Unit) {
    var volumeUp by remember { mutableStateOf(false) }
    var volumeDown by remember { mutableStateOf(false) }
    
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).focusRequester(focusRequester).focusable().onKeyEvent {
        if (it.key == Key.VolumeUp) {
            volumeUp = true
            return@onKeyEvent true
        }
        if (it.key == Key.VolumeDown) {
            volumeDown = true
            return@onKeyEvent true
        }
        false
    }, verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Volume Keys Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        Text("Press Volume Up and Volume Down", fontSize = 16.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        Row {
            Card(modifier = Modifier.size(100.dp), colors = CardDefaults.cardColors(containerColor = if (volumeUp) Color.Green else Color.Gray)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("UP", color = Color.White) }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Card(modifier = Modifier.size(100.dp), colors = CardDefaults.cardColors(containerColor = if (volumeDown) Color.Green else Color.Gray)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("DOWN", color = Color.White) }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}
@Composable
fun MicTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    var amplitude by remember { mutableStateOf(0) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }
    
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            try {
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                recorder.setOutputFile("/dev/null")
                recorder.prepare()
                recorder.start()
                
                while(true) {
                    delay(100)
                    amplitude = recorder.maxAmplitude
                }
            } catch(e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val db = if (amplitude > 0) (20 * log10(amplitude.toDouble())).toInt() else 0

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Microphone Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        if (!hasPermission) {
            Text("Requires RECORD_AUDIO permission")
        } else {
            Text("$db dB", fontSize = 64.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(progress = { (db / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(16.dp), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun SpeakerTest(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    
    val playTone = { rightVol: Float, leftVol: Float ->
        scope.launch {
            val sampleRate = 44100
            val numSamples = sampleRate * 1 
            val sample = ShortArray(numSamples)
            val freq = 440.0
            
            for (i in 0 until numSamples) {
                val value = Math.sin(2 * Math.PI * i / (sampleRate / freq)) * Short.MAX_VALUE
                sample[i] = value.toInt().toShort()
            }
            
            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                sample.size * 2,
                AudioTrack.MODE_STATIC
            )
            
            track.write(sample, 0, sample.size)
            track.setStereoVolume(leftVol, rightVol)
            track.play()
            delay(1000)
            track.release()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Speaker Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { playTone(0f, 1f) }, modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Left Channel")
            }
            Button(onClick = { playTone(1f, 0f) }, modifier = Modifier.weight(1f).padding(8.dp)) {
                Text("Right Channel")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun BiometricsTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var biometryStatus by remember { mutableStateOf("Ready") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Biometrics Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        Text(biometryStatus, fontSize = 18.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        Button(onClick = {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                BiometricManager.BIOMETRIC_SUCCESS -> {
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        val executor = ContextCompat.getMainExecutor(context)
                        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                biometryStatus = "Error: $errString"
                            }
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                biometryStatus = "Success! Authentication passed."
                            }
                            override fun onAuthenticationFailed() {
                                biometryStatus = "Failed to authenticate."
                            }
                        })
                        
                        val promptInfo = BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Biometric Test")
                            .setSubtitle("Confirm your identity")
                            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build()
                            
                        prompt.authenticate(promptInfo)
                    } else {
                        biometryStatus = "Error: Not a FragmentActivity"
                    }
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> biometryStatus = "No hardware available."
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> biometryStatus = "Hardware unavailable currently."
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> biometryStatus = "No biometrics enrolled."
                else -> biometryStatus = "Unsupported."
            }
        }) {
            Text("Authenticate")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun HapticsTest(onClose: () -> Unit) {
    val context = LocalContext.current
    
    val doVibrate = { timings: LongArray, amplitudes: IntArray ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(timings, -1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Advanced Haptics Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        
        Button(onClick = { doVibrate(longArrayOf(0, 50), intArrayOf(0, 255)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(stringResource(R.string.haptic_short))
        }
        Button(onClick = { doVibrate(longArrayOf(0, 500), intArrayOf(0, 255)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(stringResource(R.string.haptic_long))
        }
        Button(onClick = { doVibrate(longArrayOf(0, 60, 200, 60), intArrayOf(0, 255, 0, 255)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text(stringResource(R.string.haptic_heartbeat))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun EarSpeakerTest(onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    
    val playEarTone = { 
        isPlaying = true
        scope.launch {
            val sampleRate = 44100
            val numSamples = sampleRate * 3 // 3 seconds
            val sample = ShortArray(numSamples)
            val freq = 440.0 // A4 tone
            
            for (i in 0 until numSamples) {
                val value = Math.sin(2 * Math.PI * i / (sampleRate / freq)) * (Short.MAX_VALUE / 2) // Lower volume
                sample[i] = value.toInt().toShort()
            }
            
            // STREAM_VOICE_CALL routes strictly to earpiece
            val track = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                sample.size * 2,
                AudioTrack.MODE_STATIC
            )
            
            track.write(sample, 0, sample.size)
            track.play()
            delay(3000)
            track.release()
            isPlaying = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Ear Speaker Test", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))
        Text("Put the phone to your ear and listen for the tone.", modifier = Modifier.padding(bottom = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        
        Button(onClick = { playEarTone() }, enabled = !isPlaying) {
            Text(if (isPlaying) "Playing..." else "Play Tone")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose, enabled = !isPlaying) { Text("Close") }
    }
}

@Composable
fun GyroscopeTest(onClose: () -> Unit) {
    val context = LocalContext.current
    var rotX by remember { mutableStateOf(0f) }
    var rotY by remember { mutableStateOf(0f) }
    
    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroSensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_GYROSCOPE) {
                    rotX += event.values[0] * 10f
                    rotY += event.values[1] * 10f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sm.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }

    // Keep ball inside bounds approx
    val clampedX = rotX.coerceIn(-150f, 150f)
    val clampedY = rotY.coerceIn(-250f, 250f)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Gyroscope Test", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Tilt your phone to move the ball.")
        }
        
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = androidx.compose.ui.graphics.Color(0xFF42A5F5),
                radius = 30.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(center.x + clampedY.dp.toPx(), center.y + clampedX.dp.toPx())
            )
        }
        
        Button(onClick = onClose, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Text("Close")
        }
    }
}

@Composable
fun MultiPaintTest(onClose: () -> Unit) {
    var pointers by remember { mutableStateOf(mapOf<Long, androidx.compose.ui.geometry.Offset>()) }
    val colors = listOf(Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.Magenta, Color.Cyan, Color.White, Color.LightGray, Color.DarkGray, Color.Black)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val newPointers = mutableMapOf<Long, androidx.compose.ui.geometry.Offset>()
                for (change in event.changes) {
                    if (change.pressed) {
                        newPointers[change.id.value] = change.position
                    }
                }
                pointers = newPointers
            }
        }
    }) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            var i = 0
            for ((_, offset) in pointers) {
                drawCircle(
                    color = colors[i % colors.size],
                    radius = 40.dp.toPx(),
                    center = offset
                )
                i++
            }
        }
        
        Column(modifier = Modifier.align(Alignment.TopCenter).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Multi-Finger Paint", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Touches Count: ${pointers.size}", color = Color.White, fontSize = 18.sp)
        }
        
        Button(onClick = onClose, modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Text("Close")
        }
    }
}
