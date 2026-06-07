package com.example

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.media.MediaDrm
import android.media.UnsupportedSchemeException
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID

@Composable
fun SecurityTab() {
    val context = LocalContext.current
    
    // Root check
    val isRooted = remember {
        val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
        paths.any { File(it).exists() }
    }
    
    // Widevine DRM
    val widevineLevel = remember {
        try {
            val widevineUuid = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)
            val drm = MediaDrm(widevineUuid)
            val level = drm.getPropertyString("securityLevel")
            drm.release()
            level
        } catch (e: UnsupportedSchemeException) {
            "Not Supported"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Encryption
    val encryptionStatus = remember {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val status = dpm.storageEncryptionStatus
        if (status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE || status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY || status == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER) {
            true
        } else {
            false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.tab_security).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                
                InfoRow(stringResource(R.string.sec_root), if (isRooted) stringResource(R.string.sec_root_yes) else stringResource(R.string.sec_root_no))
                InfoRow(stringResource(R.string.sec_drm), widevineLevel)
                InfoRow(stringResource(R.string.sec_encryption), if (encryptionStatus) stringResource(R.string.sec_encrypted) else stringResource(R.string.sec_unencrypted))
            }
        }
    }
}
