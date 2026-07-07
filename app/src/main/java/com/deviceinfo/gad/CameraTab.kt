package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Size
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class CameraInfoData(
    val id: String,
    val facing: String,
    val type: String,
    val megapixels: String,
    val aperture: String,
    val hasOis: Boolean,
    val hasEis: Boolean,
    val hasFlash: Boolean,
    val maxRes: String,
    val rawSupport: Boolean,
    val hdrSupport: Boolean,
    val isoRange: String,
    val focalLengths: String,
    val sensorSize: String,
    val score: Int
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CameraTab() {
    val context = LocalContext.current
    var cameras by remember { mutableStateOf<List<CameraInfoData>>(emptyList()) }
    var overallScore by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val list = mutableListOf<CameraInfoData>()
            var totalScore = 0
            if (manager != null) {
                for (cameraId in manager.cameraIdList) {
                    val chars = manager.getCameraCharacteristics(cameraId)
                
                var camScore = 50

                // Facing
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front Camera"
                    CameraCharacteristics.LENS_FACING_BACK -> "Rear Camera"
                    else -> "External"
                }
                
                // Focal Length Array
                val focals = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalsStr = if (focals != null && focals.isNotEmpty()) focals.joinToString(", ") { "$it mm" } else "Unknown"

                // Type estimation
                val type = when {
                    focals != null && focals.isNotEmpty() && focals[0] < 2f -> "Ultra-Wide"
                    focals != null && focals.isNotEmpty() && focals[0] > 5f -> "Telephoto"
                    else -> facingStr
                }
                if (type == "Ultra-Wide" || type == "Telephoto") camScore += 10
                
                // Megapixels
                val size = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val mp = if (size != null) {
                    val mPixels = (size.width * size.height) / 1_000_000.0
                    if (mPixels > 12) camScore += 10
                    String.format(Locale.US, "%.1f MP", mPixels)
                } else "Unknown"
                
                // Aperture
                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val apStr = if (apertures != null && apertures.isNotEmpty()) {
                    val minAp = apertures.minOrNull() ?: apertures[0]
                    if (minAp < 2.0f) camScore += 5
                    "f/$minAp"
                } else "Unknown"
                
                // OIS & EIS
                val ois = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = ois != null && ois.isNotEmpty() && ois.any { it != CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_OFF }
                if (hasOis) camScore += 10

                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val hasRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
                if (hasRaw) camScore += 10

                // EIS is often tracked by control modes
                val modes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                val hasEis = modes != null && modes.any { it != CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_OFF }
                if (hasEis) camScore += 5

                // Flash
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                // HDR
                val sceneModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
                val hdrSupport = sceneModes?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) == true || 
                                 (capabilities?.contains(18) == true) // DYNAMIC_RANGE_TEN_BIT
                if (hdrSupport) camScore += 5

                // Sensor Size
                val physicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val sensorSizeStr = if (physicalSize != null) "${String.format(Locale.US, "%.2f", physicalSize.width)} x ${String.format(Locale.US, "%.2f", physicalSize.height)} mm" else "Unknown"
                
                // Resolution
                val maxRes = if (size != null) "${size.width} x ${size.height}" else "Unknown"
                
                // ISO Range
                val isoRangeVal = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val isoStr = if (isoRangeVal != null) "${isoRangeVal.lower} - ${isoRangeVal.upper}" else "Unknown"
                
                list.add(CameraInfoData(cameraId, facingStr, type, mp, apStr, hasOis, hasEis, hasFlash, maxRes, hasRaw, hdrSupport, isoStr, focalsStr, sensorSizeStr, camScore))
                totalScore += camScore
                }
            }
            cameras = list
            if (list.isNotEmpty()) {
                overallScore = (totalScore / list.size).coerceIn(0, 100)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.ui_camera_capability), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("$overallScore / 100", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        Text(stringResource(R.string.ui_lens_array_analyzed), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    AnimatedLensEffect(score = overallScore)
                }
            }
        }

        items(cameras) { cam ->
            ExpandableCameraCard(cam)
        }
        
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun ExpandableCameraCard(cam: CameraInfoData) {
    var expanded by remember { mutableStateOf(false) }
    
    com.deviceinfo.gad.ui.components.GlassCard(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = if (cam.facing.contains("Front")) Icons.Default.CameraFront else Icons.Default.CameraRear
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp).padding(end = 8.dp))
                    Column {
                        Text(cam.type.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text(cam.megapixels, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FeatureChip("RAW", cam.rawSupport)
                        FeatureChip("OIS", cam.hasOis)
                        FeatureChip("EIS", cam.hasEis)
                        FeatureChip("HDR", cam.hdrSupport)
                        FeatureChip("FLASH", cam.hasFlash)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                    com.deviceinfo.gad.InfoRow("Camera ID", cam.id)
                    com.deviceinfo.gad.InfoRow("Max Resolution", cam.maxRes)
                    com.deviceinfo.gad.InfoRow("Aperture", cam.aperture)
                    com.deviceinfo.gad.InfoRow("Focal Length", cam.focalLengths)
                    com.deviceinfo.gad.InfoRow("Sensor Size", cam.sensorSize)
                    com.deviceinfo.gad.InfoRow("ISO Range", cam.isoRange)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Score: ${cam.score}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureChip(label: String, supported: Boolean) {
    val bgColor = if (supported) Color(0xFF00E676).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (supported) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bgColor, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AnimatedLensEffect(score: Int) {
    val animationsEnabled = com.deviceinfo.gad.ui.components.LocalAnimationsEnabled.current
    val infiniteTransition = rememberInfiniteTransition(label = "lens")
    val pulse by if (animationsEnabled) infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse"
    ) else remember { mutableStateOf(1f) }
    
    val rotation by if (animationsEnabled) infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotation"
    ) else remember { mutableStateOf(0f) }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            
            // Outer Ring
            drawCircle(
                color = Color(0xFF1E1E24),
                radius = size.width / 2,
                center = center
            )
            
            // Aperture Blades
            rotate(rotation) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = (size.width / 2) * pulse,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
                for (i in 0..5) {
                    rotate(i * 60f) {
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha=0.5f),
                            start = center,
                            end = androidx.compose.ui.geometry.Offset(center.x, center.y - (size.width/2.5f)),
                            strokeWidth = 2f
                        )
                    }
                }
            }
            
            // Inner Lens
            drawCircle(
                color = Color(0xFF000000),
                radius = size.width / 3.5f,
                center = center
            )
            // Lens Reflection
            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                radius = size.width / 8f,
                center = androidx.compose.ui.geometry.Offset(center.x - 10f, center.y - 10f)
            )
        }
    }
}
