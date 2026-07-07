package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.ui.res.stringResource

data class PartitionInfo(val name: String, val path: String, val totalBytes: Long, val freeBytes: Long, val type: String = "Unknown")

@Composable
fun RomTab(viewModel: CpuHardwareViewModel) {
    DisposableEffect(Unit) {
        viewModel.startStorageMonitoring()
        onDispose { viewModel.stopStorageMonitoring() }
    }

    val tStorage by viewModel.totalStorage.collectAsStateWithLifecycle()
    val fStorage by viewModel.freeStorage.collectAsStateWithLifecycle()
    val extTotal by viewModel.externalStorageTotal.collectAsStateWithLifecycle()
    val extFree by viewModel.externalStorageFree.collectAsStateWithLifecycle()
    
    val isStorageReady by viewModel.isStorageReady.collectAsStateWithLifecycle()
    val isInit = !isStorageReady
    
    val context = LocalContext.current
    var partitions by remember { mutableStateOf<List<PartitionInfo>>(emptyList()) }
    var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
        val list = mutableListOf<PartitionInfo>()
        val paths = listOf(
            Pair("System (Root)", Environment.getRootDirectory()),
            Pair("Cache", Environment.getDownloadCacheDirectory()),
            Pair("External Media", Environment.getExternalStorageDirectory())
        )
        
        for ((name, file) in paths) {
            try {
                if (file.exists() && file.canRead()) {
                    val stat = StatFs(file.path)
                    val total = stat.totalBytes
                    val free = stat.availableBytes
                    if (total > 0) {
                        list.add(PartitionInfo(name, file.path, total, free))
                    }
                }
            } catch (e: Exception) {}
        }
        partitions = list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
        ) {
            com.deviceinfo.gad.ui.components.GlassCard {
                val storageUsed = if (tStorage > 0) tStorage - fStorage else 0
                val storagePct = if (tStorage > 0) storageUsed.toFloat() / tStorage.toFloat() else 0f
                
                Text(stringResource(R.string.sys_internal_storage), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatSize(fStorage)} Free", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("${formatSize(storageUsed)} / ${formatSize(tStorage)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val animStorage by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = storagePct,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "storageBar"
                )
                
                LinearProgressIndicator(
                    progress = { animStorage },
                    modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        AnimatedVisibility(
            visible = visible && extTotal > 0L,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
        ) {
            com.deviceinfo.gad.ui.components.GlassCard {
                val extUsed = extTotal - extFree
                val extPct = if (extTotal > 0) extUsed.toFloat() / extTotal.toFloat() else 0f
                
                Text(stringResource(R.string.sys_sd_card), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatSize(extFree)} Free", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("${formatSize(extUsed)} / ${formatSize(extTotal)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val animExtStorage by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = extPct,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "extStorageBar"
                )
                
                LinearProgressIndicator(
                    progress = { animExtStorage },
                    modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        partitions.forEachIndexed { index, p ->
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { 50 * (index + 1) }) + fadeIn(animationSpec = tween(500, delayMillis = 100 * index))
            ) {
                val used = p.totalBytes - p.freeBytes
                val pct = if (p.totalBytes > 0) used.toFloat() / p.totalBytes.toFloat() else 0f
                com.deviceinfo.gad.ui.components.GlassCard {
                    Text("${p.name.uppercase()} ${stringResource(R.string.sys_partition)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                    Text(p.path, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
                    
                    val animPct by animateFloatAsState(
                        targetValue = pct,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                        ),
                        label = "pctAnim"
                    )
                    LinearProgressIndicator(
                        progress = { animPct },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatSize(used) + " " + stringResource(R.string.sys_used), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        Text(formatSize(p.freeBytes) + " " + stringResource(R.string.sys_free), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CategoryLegend(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.2f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
