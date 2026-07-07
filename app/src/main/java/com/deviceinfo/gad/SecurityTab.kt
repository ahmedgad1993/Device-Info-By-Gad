package com.deviceinfo.gad
import com.deviceinfo.gad.R
import androidx.compose.ui.res.stringResource

import android.app.KeyguardManager
import android.content.Context
import android.media.MediaDrm
import android.media.UnsupportedSchemeException
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SecurityCheckItem(
    val name: String,
    val statusText: String,
    val status: SecurityStatus,
    val icon: ImageVector,
    val recommendation: String? = null
)

enum class SecurityStatus {
    SECURE, WARNING, RISK
}

@Composable
fun SecurityTab() {
    val context = LocalContext.current
    var checks by remember { mutableStateOf<List<SecurityCheckItem>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var isScanning by remember { mutableStateOf(true) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val list = mutableListOf<SecurityCheckItem>()
            var totalScore = 100

            // 1. Root Check
            val isRooted = checkRoot(context)
            if (isRooted) {
                list.add(SecurityCheckItem("Root Access", "Root detected", SecurityStatus.RISK, Icons.Default.Warning, "Rooting removes system restrictions and decreases security."))
                totalScore -= 40
            } else {
                list.add(SecurityCheckItem("Root Access", "Not Rooted", SecurityStatus.SECURE, Icons.Default.VerifiedUser))
            }
            checks = list.toList(); score = totalScore

            // 2. Magisk Check
            val isMagisk = checkMagisk()
            if (isMagisk) {
                list.add(SecurityCheckItem("Magisk Detection", "Magisk present", SecurityStatus.RISK, Icons.Default.GppBad, "Magisk alters systemless partitions."))
                totalScore -= 20
            } else {
                list.add(SecurityCheckItem("Magisk Detection", "Passed", SecurityStatus.SECURE, Icons.Default.Verified))
            }
            checks = list.toList(); score = totalScore

            // 3. BusyBox Check
            val busyBoxPaths = arrayOf("/system/xbin/busybox", "/system/bin/busybox", "/data/local/xbin/busybox", "/data/local/bin/busybox", "/data/local/busybox", "/sbin/busybox")
            val isBusyBox = busyBoxPaths.any { File(it).exists() }
            if (isBusyBox) {
                list.add(SecurityCheckItem("BusyBox", "Detected", SecurityStatus.WARNING, Icons.Default.Terminal, "Busybox binaries expose multiple util functions that may pose risk."))
                totalScore -= 10
            } else {
                list.add(SecurityCheckItem("BusyBox", "Not found", SecurityStatus.SECURE, Icons.Default.CheckCircle))
            }
            checks = list.toList(); score = totalScore

            // 4. USB Debugging
            val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
            if (adbEnabled) {
                list.add(SecurityCheckItem("USB Debugging", "Enabled", SecurityStatus.WARNING, Icons.Default.Usb, "Turn off USB debugging if not developing applications."))
                totalScore -= 10
            } else {
                list.add(SecurityCheckItem("USB Debugging", "Disabled", SecurityStatus.SECURE, Icons.Default.UsbOff))
            }
            checks = list.toList(); score = totalScore

            // 5. Developer Options
            val devOptionsEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
            if (devOptionsEnabled) {
                list.add(SecurityCheckItem("Developer Options", "Enabled", SecurityStatus.WARNING, Icons.Default.BugReport, "Disable to prevent unauthorized side-loaded changes."))
                totalScore -= 5
            } else {
                list.add(SecurityCheckItem("Developer Options", "Disabled", SecurityStatus.SECURE, Icons.Default.BugReport))
            }
            checks = list.toList(); score = totalScore

            // 6. SELinux Check
            val selinuxStatus = checkSELinux()
            if (selinuxStatus == "Enforcing") {
                list.add(SecurityCheckItem("SELinux Status", "Enforcing", SecurityStatus.SECURE, Icons.Default.Shield))
            } else if (selinuxStatus == "Permissive") {
                list.add(SecurityCheckItem("SELinux Status", "Permissive", SecurityStatus.RISK, Icons.Default.Warning, "Permissive mode weakens device security policies."))
                totalScore -= 20
            } else {
                list.add(SecurityCheckItem("SELinux Status", selinuxStatus, SecurityStatus.WARNING, Icons.AutoMirrored.Filled.HelpOutline, "Unable to verify SELinux state."))
            }
            checks = list.toList(); score = totalScore

            // 7. Secure Lock Screen
            try {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (km?.isDeviceSecure == true) {
                    list.add(SecurityCheckItem("Screen Lock", "Secured", SecurityStatus.SECURE, Icons.Default.Lock))
                } else {
                    list.add(SecurityCheckItem("Screen Lock", "Insecure / None", SecurityStatus.RISK, Icons.Default.LockOpen, "Set a Pattern, PIN, or Password immediately!"))
                    totalScore -= 30
                }
            } catch (e: Exception) {
                list.add(SecurityCheckItem("Screen Lock", "Unknown", SecurityStatus.WARNING, Icons.Default.LockOpen))
            }
            checks = list.toList(); score = totalScore

            // 8. Device Encryption
            var encrypted = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? android.app.admin.DevicePolicyManager
                    if (dpm != null) {
                        val status = dpm.storageEncryptionStatus
                        encrypted = status == android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE || status == android.app.admin.DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
                    }
                }
            } catch (e: Exception) {
                // Ignore SecurityException, default to true or false depending on heuristics
                // Modern Android is encrypted by default
                encrypted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            }
            if (encrypted) {
                list.add(SecurityCheckItem("Device Encryption", "Encrypted", SecurityStatus.SECURE, Icons.Default.VpnKey))
            } else {
                list.add(SecurityCheckItem("Device Encryption", "Not Encrypted", SecurityStatus.RISK, Icons.Default.LockOpen, "Encrypt device in settings to protect local data."))
                totalScore -= 20
            }
            checks = list.toList(); score = totalScore

            // 9. Play Protect / Unknown Sources
            var unknownSources = false
            try {
                unknownSources = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.packageManager.canRequestPackageInstalls()
                } else {
                    @Suppress("DEPRECATION")
                    Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            if (unknownSources) {
                list.add(SecurityCheckItem("Unknown Sources", "Allowed", SecurityStatus.WARNING, Icons.Default.Android, "Be cautious installing unknown APKs."))
                totalScore -= 10
            } else {
                list.add(SecurityCheckItem("Unknown Sources", "Blocked", SecurityStatus.SECURE, Icons.Default.Shop))
            }
            
            checks = list.toList()
            score = totalScore.coerceIn(0, 100)
            isScanning = false
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    AnimatedSecurityShield(score = score, isScanning = isScanning)
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isScanning) {
                        Text(androidx.compose.ui.res.stringResource(R.string.sec_analyzing), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val color = when {
                            score >= 90 -> Color(0xFF00E676)
                            score >= 70 -> Color(0xFFFFD54F)
                            else -> Color(0xFFFF5252)
                        }
                        val statusText = when {
                            score >= 90 -> androidx.compose.ui.res.stringResource(R.string.sec_secure)
                            score >= 70 -> androidx.compose.ui.res.stringResource(R.string.sec_moderate)
                            else -> androidx.compose.ui.res.stringResource(R.string.sec_high)
                        }
                        Text("$score", fontSize = 48.sp, fontWeight = FontWeight.Black, color = color)
                        Text(statusText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            androidx.compose.ui.res.stringResource(R.string.sec_disclaimer),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        if (isScanning) {
            items(10) {
                com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.size(36.dp).background(Color(0xFF2C2F36), androidx.compose.foundation.shape.CircleShape))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).background(Color(0xFF2C2F36)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.3f).height(10.dp).background(Color(0xFF2C2F36)))
                        }
                    }
                }
            }
        } else {
            items(checks.size) { index ->
                val check = checks[index]
                SecurityCheckRow(check)
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun SecurityCheckRow(item: SecurityCheckItem) {
    val color = when (item.status) {
        SecurityStatus.SECURE -> Color(0xFF00E676)
        SecurityStatus.WARNING -> Color(0xFFFFD54F)
        SecurityStatus.RISK -> Color(0xFFFF5252)
    }

    com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.padding(vertical = 6.dp)) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(color.copy(alpha=0.1f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                        Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(item.statusText, fontSize = 14.sp, color = color)
                    }
                }
                if (item.status != SecurityStatus.SECURE) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = color)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, tint = color)
                }
            }
            if (!item.recommendation.isNullOrEmpty() && item.status != SecurityStatus.SECURE) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.recommendation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.background(Color(0x11FFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(8.dp))
            }
        }
    }
}

@Composable
fun AnimatedSecurityShield(score: Int, isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse"
    )

    val color = if (isScanning) Color(0xFF00E5FF) else when {
        score >= 90 -> Color(0xFF00E676)
        score >= 70 -> Color(0xFFFFD54F)
        else -> Color(0xFFFF5252)
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (isScanning) {
                drawCircle(color = color.copy(alpha=0.1f), radius = size.width/2 * scale)
                drawCircle(color = color.copy(alpha=0.3f), radius = size.width/2.5f)
            } else {
                drawCircle(color = color.copy(alpha=0.2f), radius = size.width/2)
            }
        }
        Icon(Icons.Default.Security, contentDescription = null, tint = color.copy(alpha=if (isScanning) 0.5f else 1f), modifier = Modifier.size(if (isScanning) (64 * scale).dp else 80.dp))
    }
}

private fun checkRoot(context: Context): Boolean {
    val rootPaths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su")
    var isRooted = false
    try {
        for (path in rootPaths) {
            if (File(path).exists()) isRooted = true
        }
    } catch (e: Exception) {}

    val buildTags = Build.TAGS
    if (buildTags != null && buildTags.contains("test-keys")) isRooted = true

    try {
        val p = ProcessBuilder("getprop", "ro.secure").start()
        val reader = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
        if (reader.readLine()?.trim() == "0") isRooted = true
        p.waitFor()
    } catch (e: Exception) {}

    val packages = arrayOf("com.topjohnwu.magisk", "eu.chainfire.supersu", "com.noshufou.android.su", "com.koushikdutta.superuser", "com.thirdparty.superuser")
    try {
        val pm = context.packageManager
        for (pkg in packages) {
            try {
                pm.getPackageInfo(pkg, 0)
                isRooted = true
            } catch (e: Exception) {}
        }
    } catch (e: Exception) {}
    
    // Check for su binary execution
    try {
        val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
        val buffer = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        if (buffer.readLine() != null) isRooted = true
    } catch (e: Exception) {}

    return isRooted
}

private fun checkMagisk(): Boolean {
    val magiskPaths = arrayOf("/sbin/magisk", "/data/adb/magisk", "/magisk", "/data/magisk", "/cache/magisk.log", "/data/adb/magisk.db", "/init.magisk.rc")
    for (path in magiskPaths) {
        try {
            if (File(path).exists()) return true
        } catch(e: Exception){}
    }
    
    // Deeper Magisk hide check
    try {
        val p = ProcessBuilder("getprop", "ro.magisk.disable").start()
        val reader = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
        val output = reader.readLine()
        p.waitFor()
        if (output?.trim() == "1" || output?.trim() == "0") return true
    } catch(e: Exception) {}
    
    return false
}

private fun checkSELinux(): String {
    var status = "Not accessible without root on this device."
    try {
        val p = ProcessBuilder("getenforce").start()
        val reader = java.io.BufferedReader(java.io.InputStreamReader(p.inputStream))
        val output = reader.readLine()
        p.waitFor()
        if (output != null) {
            status = output.trim()
        }
    } catch (e: Exception) {
        try {
            val rootPaths = arrayOf("/sys/fs/selinux/enforce")
            for (path in rootPaths) {
                if (File(path).exists()) {
                    val content = File(path).readText().trim()
                    if (content == "1") status = "Enforcing"
                    else if (content == "0") status = "Permissive"
                }
            }
        } catch (e: Exception) {}
    }
    return status
}
