package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deviceinfo.gad.ui.components.GlassCard
import com.deviceinfo.gad.InfoRow

@Composable
fun SettingsTab(prefs: AppPreferences, onLanguageChanged: (String) -> Unit) {
    val context = LocalContext.current
    val theme by prefs.themeMode.collectAsStateWithLifecycle()
    val accent by prefs.accentColor.collectAsStateWithLifecycle()
    val lang by prefs.language.collectAsStateWithLifecycle()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val animEnabled by prefs.animationsEnabled.collectAsStateWithLifecycle()
    var visible by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = if (animEnabled) androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 } else androidx.compose.animation.EnterTransition.None
        ) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.set_lang).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                LanguageRadioRow("🇺🇸 " + stringResource(R.string.lang_english), "en", lang) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    prefs.setLanguage("en")
                    onLanguageChanged("en")
                }
                LanguageRadioRow("🇸🇦 " + stringResource(R.string.lang_arabic), "ar", lang) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    prefs.setLanguage("ar")
                    onLanguageChanged("ar")
                }
                LanguageRadioRow("🇫🇷 " + stringResource(R.string.lang_french), "fr", lang) {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    prefs.setLanguage("fr")
                    onLanguageChanged("fr")
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = if (animEnabled) androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 } else androidx.compose.animation.EnterTransition.None
        ) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.set_theme).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                ThemeRadioRow(stringResource(R.string.system_theme), "system", theme) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setThemePreference("system") }
                ThemeRadioRow(stringResource(R.string.light_theme), "light", theme) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setThemePreference("light") }
                ThemeRadioRow(stringResource(R.string.dark_theme), "dark", theme) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setThemePreference("dark") }
                ThemeRadioRow("Black (AMOLED)", "black", theme) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setThemePreference("black") }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.set_accent_color).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                
                val colors = listOf(
                    "blue" to Color(0xFF2196F3),
                    "purple" to Color(0xFF9C27B0),
                    "green" to Color(0xFF4CAF50),
                    "orange" to Color(0xFFFF9800),
                    "red" to Color(0xFFF44336),
                    "teal" to Color(0xFF009688),
                    "pink" to Color(0xFFE91E63),
                    "indigo" to Color(0xFF3F51B5)
                )
                
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), 
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    colors.forEach { (key, color) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setAccentColor(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accent == key) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = if (animEnabled) androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 } else androidx.compose.animation.EnterTransition.None
        ) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_performance), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.ui_ui_animations), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.ui_enable_smooth_transi), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = animEnabled, onCheckedChange = { prefs.setAnimationsEnabled(it) })
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                val refreshMs by prefs.refreshInterval.collectAsStateWithLifecycle()
                Text(stringResource(R.string.ui_refresh_interval), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val intervals = listOf(500, 1000, 2000)
                    intervals.forEach { ms ->
                        val isSelected = refreshMs == ms
                        FilterChip(
                            selected = isSelected,
                            onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); prefs.setRefreshInterval(ms) },
                            label = { Text("${ms}ms", fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = if (animEnabled) androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 4 } else androidx.compose.animation.EnterTransition.None
        ) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.set_developer).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                com.deviceinfo.gad.ui.components.DeveloperSupportCard()
                
                val crashFile = java.io.File(context.filesDir, "last_crash.txt")
                if (crashFile.exists()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    var showCrashDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showCrashDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ui_view_last_crash_log))
                    }
                    
                    if (showCrashDialog) {
                        val crashLog = try { crashFile.readText() } catch (e: Exception) { "Could not read crash log." }
                        AlertDialog(
                            onDismissRequest = { showCrashDialog = false },
                            title = { Text(stringResource(R.string.ui_last_crash_log)) },
                            text = { 
                                Column(modifier = Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                                    Text(crashLog, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText(context.getString(R.string.ui_crash_log), crashLog)
                                    clipboard.setPrimaryClip(clip)
                                    android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                }) { Text(stringResource(R.string.ui_copy)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { 
                                    crashFile.delete()
                                    showCrashDialog = false 
                                }) { Text(stringResource(R.string.ui_clear)) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeRadioRow(label: String, value: String, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = value == currentValue,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun LanguageRadioRow(label: String, value: String, currentValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = value == currentValue,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
