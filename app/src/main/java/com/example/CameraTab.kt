package com.example

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class CameraInfoData(
    val id: String,
    val facing: String,
    val megapixels: String,
    val aperture: String,
    val hasOis: Boolean,
    val maxRes: String,
    val rawSupport: Boolean,
    val isoRange: String,
    val focalLengths: String
)

@Composable
fun CameraTab() {
    val context = LocalContext.current
    var cameras by remember { mutableStateOf<List<CameraInfoData>>(emptyList()) }

    LaunchedEffect(Unit) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val list = mutableListOf<CameraInfoData>()
        try {
            for (cameraId in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(cameraId)
                
                // Facing
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> context.getString(R.string.camera_front)
                    CameraCharacteristics.LENS_FACING_BACK -> context.getString(R.string.camera_back)
                    else -> "External/Other"
                }
                
                // Megapixels
                val size = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
                val mp = if (size != null) {
                    val mPixels = (size.width * size.height) / 1_000_000.0
                    String.format(Locale.US, "%.1f MP", mPixels)
                } else "Unknown"
                
                // Aperture
                val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
                val apStr = if (apertures != null && apertures.isNotEmpty()) {
                    "f/${apertures.minOrNull() ?: apertures[0]}"
                } else "Unknown"
                
                // OIS
                val ois = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                val hasOis = ois != null && ois.isNotEmpty() && ois.any { it != CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_OFF }
                
                // Resolution (Simple assumption using sensor size)
                val maxRes = if (size != null) "${size.width}x${size.height}" else "Unknown"
                
                // RAW Support
                val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val hasRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true
                
                // ISO Range
                val isoRangeVal = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
                val isoStr = if (isoRangeVal != null) {
                    "${isoRangeVal.lower} - ${isoRangeVal.upper}"
                } else {
                    "Unknown"
                }
                
                // Focal Lengths
                val focals = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalsStr = if (focals != null && focals.isNotEmpty()) {
                    focals.joinToString(", ") { "$it mm" }
                } else {
                    "Unknown"
                }
                
                list.add(CameraInfoData(cameraId, facingStr, mp, apStr, hasOis, maxRes, hasRaw, isoStr, focalsStr))
            }
            cameras = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(cameras) { cam ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("${cam.facing} (ID: ${cam.id})", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp))
                    InfoRow(stringResource(R.string.camera_megapixels), cam.megapixels)
                    InfoRow(stringResource(R.string.camera_aperture), cam.aperture)
                    InfoRow(stringResource(R.string.camera_ois), if (cam.hasOis) stringResource(R.string.camera_support_yes) else stringResource(R.string.camera_support_no))
                    InfoRow(stringResource(R.string.camera_resolution), cam.maxRes)
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    InfoRow(stringResource(R.string.camera_raw), if (cam.rawSupport) stringResource(R.string.camera_support_yes) else stringResource(R.string.camera_support_no))
                    InfoRow(stringResource(R.string.camera_iso), cam.isoRange)
                    InfoRow(stringResource(R.string.camera_focal), cam.focalLengths)
                }
            }
        }
    }
}
