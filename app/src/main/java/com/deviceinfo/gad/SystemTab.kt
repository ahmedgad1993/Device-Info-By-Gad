package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.deviceinfo.gad.ui.components.DataRow
import com.deviceinfo.gad.ui.components.SectionCard
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

@Composable
fun SystemTab(viewModel: CpuHardwareViewModel) {
    var uptime by remember { mutableStateOf(formatUptimeS(SystemClock.elapsedRealtime())) }

    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            uptime = formatUptimeS(SystemClock.elapsedRealtime())
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionCard(title = stringResource(R.string.sys_os), isExpandable = true) {
            DataRow(stringResource(R.string.sys_android_ver), Build.VERSION.RELEASE ?: "Unknown")
            DataRow(stringResource(R.string.sys_api_level), Build.VERSION.SDK_INT.toString())
            if (Build.VERSION.SDK_INT >= 23) {
                DataRow(stringResource(R.string.sys_sec_patch), Build.VERSION.SECURITY_PATCH ?: "Unknown")
            }
            DataRow(stringResource(R.string.sys_code_name), Build.VERSION.CODENAME ?: "Unknown")
            DataRow(stringResource(R.string.sys_base_os), if (Build.VERSION.SDK_INT >= 23) (Build.VERSION.BASE_OS ?: "Unknown") else "Unknown")
        }

        SectionCard(title = stringResource(R.string.sys_arch), isExpandable = true) {
            DataRow(stringResource(R.string.sys_kernel_arch), System.getProperty("os.arch") ?: "Unknown")
            DataRow(stringResource(R.string.sys_kernel_ver), System.getProperty("os.version") ?: "Unknown")
            DataRow(stringResource(R.string.sys_java_vm), System.getProperty("java.vm.name") ?: "Unknown")
            DataRow(stringResource(R.string.sys_java_ver), System.getProperty("java.specification.version") ?: "Unknown")
            DataRow(stringResource(R.string.sys_supported_abis), Build.SUPPORTED_ABIS?.joinToString(", ") ?: "Unknown")
        }

        SectionCard(title = stringResource(R.string.sys_identity), isExpandable = true) {
            DataRow(stringResource(R.string.sys_bootloader), Build.BOOTLOADER ?: "Unknown")
            DataRow(stringResource(R.string.sys_build_num), Build.DISPLAY ?: "Unknown")
            DataRow(stringResource(R.string.sys_build_id), Build.ID ?: "Unknown")
            DataRow(stringResource(R.string.sys_build_tags), Build.TAGS ?: "Unknown")
            DataRow(stringResource(R.string.sys_build_type), Build.TYPE ?: "Unknown")
            DataRow(stringResource(R.string.sys_uptime), uptime)
        }
    }
}

private fun formatUptimeS(uptimeMillis: Long): String {
    val seconds = (uptimeMillis / 1000) % 60
    val minutes = (uptimeMillis / (1000 * 60)) % 60
    val hours = (uptimeMillis / (1000 * 60 * 60)) % 24
    val days = uptimeMillis / (1000 * 60 * 60 * 24)
    return if (days > 0) {
        "${days}d ${hours}h ${minutes}m ${seconds}s"
    } else {
        "${hours}h ${minutes}m ${seconds}s"
    }
}
