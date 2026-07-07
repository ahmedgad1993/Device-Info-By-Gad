package com.deviceinfo.gad
import com.deviceinfo.gad.R

import android.app.Application
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateDp
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.deviceinfo.gad.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.appcompat.app.AppCompatActivity

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var appPreferences: AppPreferences
    private val appsViewModel: AppsViewModel by viewModels()
    private val batteryNetworkViewModel: BatteryNetworkViewModel by viewModels()
    private val sensorsViewModel: SensorsViewModel by viewModels()
    private val cpuHardwareViewModel: CpuHardwareViewModel by viewModels()

    fun exportReportFormat(format: String) {
        val data = when (format) {
            "JSON" -> generateExportJson()
            "CSV" -> generateExportCsv()
            else -> generateExportReport()
        }
        val extension = format.lowercase()
        val mimeType = if (format == "JSON") "application/json" else if (format == "CSV") "text/csv" else "text/plain"
        
        try {
            val file = java.io.File(cacheDir, "device_report.$extension")
            java.io.FileWriter(file).use { it.write(data) }
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(intent, "Export Report"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateExportReport(): String {
        return buildString {
            appendLine("--- Device Info By Gad ---")
            appendLine("Model: ${android.os.Build.MODEL}")
            appendLine("Board: ${cpuHardwareViewModel.cpuBoard}")
            appendLine("CPU Arch: ${cpuHardwareViewModel.cpuArch}")
            appendLine("OS Version: ${cpuHardwareViewModel.osVersion} (API ${cpuHardwareViewModel.apiLevel})")
            appendLine("Build ID: ${cpuHardwareViewModel.buildId}")
            
            appendLine("\n--- Battery ---")
            appendLine("Level: ${batteryNetworkViewModel.batteryPct.value.toInt()}%")
            appendLine("Health: ${batteryNetworkViewModel.batteryHealth.value}")
            appendLine("Temperature: ${batteryNetworkViewModel.batteryTemp.value} C")
            
            appendLine("\n--- Network ---")
            appendLine("Type: ${batteryNetworkViewModel.networkType.value}")
            appendLine("IP Address: ${batteryNetworkViewModel.ipAddress.value}")
            appendLine("Link Speed: ${batteryNetworkViewModel.linkSpeed.value}")
            
            appendLine("\n--- Display ---")
            appendLine("Size: ${cpuHardwareViewModel.screenSize.value}")
            appendLine("Resolution: ${cpuHardwareViewModel.screenRes.value}")
            appendLine("Refresh Rate: ${cpuHardwareViewModel.refreshRate.value}")
        }
    }

    private fun generateExportJson(): String {
        return """
            {
                "device_info": {
                    "model": "${android.os.Build.MODEL}",
                    "board": "${cpuHardwareViewModel.cpuBoard}",
                    "cpu_arch": "${cpuHardwareViewModel.cpuArch}",
                    "os_version": "${cpuHardwareViewModel.osVersion}",
                    "api_level": "${cpuHardwareViewModel.apiLevel}",
                    "build_id": "${cpuHardwareViewModel.buildId}"
                },
                "battery": {
                    "level": ${batteryNetworkViewModel.batteryPct.value.toInt()},
                    "health": "${batteryNetworkViewModel.batteryHealth.value}",
                    "temperature": ${batteryNetworkViewModel.batteryTemp.value}
                },
                "network": {
                    "type": "${batteryNetworkViewModel.networkType.value}",
                    "ip": "${batteryNetworkViewModel.ipAddress.value}",
                    "link_speed": "${batteryNetworkViewModel.linkSpeed.value}"
                }
            }
        """.trimIndent()
    }

    private fun String.toCsvField(): String {
        if (this.contains(",") || this.contains("\"") || this.contains("\n") || this.contains("\r")) {
            return "\"" + this.replace("\"", "\"\"") + "\""
        }
        return this
    }

    private fun generateExportCsv(): String {
        return buildString {
            appendLine("Category,Key,Value")
            fun addRow(cat: String, key: String, value: String) {
                appendLine("${cat.toCsvField()},${key.toCsvField()},${value.toCsvField()}")
            }
            addRow("Device", "Model", android.os.Build.MODEL)
            addRow("Device", "Board", cpuHardwareViewModel.cpuBoard)
            addRow("Device", "Arch", cpuHardwareViewModel.cpuArch)
            addRow("Device", "OS", cpuHardwareViewModel.osVersion)
            addRow("Device", "API", cpuHardwareViewModel.apiLevel)
            addRow("Battery", "Level", batteryNetworkViewModel.batteryPct.value.toInt().toString())
            addRow("Battery", "Health", batteryNetworkViewModel.batteryHealth.value)
            addRow("Battery", "Temp", batteryNetworkViewModel.batteryTemp.value.toString())
            addRow("Network", "Type", batteryNetworkViewModel.networkType.value)
            addRow("Network", "IP", batteryNetworkViewModel.ipAddress.value)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            var isReady by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                // Defer blocking operations
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (!::appPreferences.isInitialized) {
                        appPreferences = AppPreferences(this@MainActivity)
                    }
                    // Touch viewModels to trigger init
                    cpuHardwareViewModel.hashCode()
                    appsViewModel.hashCode()
                    batteryNetworkViewModel.hashCode()
                    sensorsViewModel.hashCode()
                }
                isReady = true
            }

            AnimatedContent(
                targetState = isReady,
                transitionSpec = {
                    androidx.compose.animation.fadeIn(animationSpec = tween(400)) togetherWith androidx.compose.animation.fadeOut(animationSpec = tween(400))
                },
                label = "MainScreenReveal"
            ) { ready ->
                if (ready) {
                    val currentTheme by appPreferences.themeMode.collectAsStateWithLifecycle()
                    val currentAccent by appPreferences.accentColor.collectAsStateWithLifecycle()
                    
                    MyApplicationTheme(
                        themeMode = currentTheme,
                        accentColor = currentAccent,
                        dynamicColor = false
                    ) {
                        AppMainScreen(
                            cpuHardwareViewModel = cpuHardwareViewModel,
                            appsViewModel = appsViewModel,
                            batteryNetworkViewModel = batteryNetworkViewModel,
                            sensorsViewModel = sensorsViewModel,
                            appPreferences = appPreferences,
                            onLanguageChanged = { lang ->
                                appPreferences.setLanguage(lang)
                                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(androidx.core.os.LocaleListCompat.forLanguageTags(lang))
                            },
                            onExportFormatClicked = { format -> exportReportFormat(format) }
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppMainScreen(
    cpuHardwareViewModel: CpuHardwareViewModel,
    appsViewModel: AppsViewModel,
    batteryNetworkViewModel: BatteryNetworkViewModel,
    sensorsViewModel: SensorsViewModel,
    appPreferences: AppPreferences,
    onLanguageChanged: (String) -> Unit,
    onExportFormatClicked: (String) -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(stringResource(R.string.ui_export_report)) },
            text = { Text(stringResource(R.string.ui_choose_a_format_to_e)) },
            confirmButton = {},
            dismissButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showExportDialog = false; onExportFormatClicked("TXT") 
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_export_as_txt))
                    }
                    Button(onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showExportDialog = false; onExportFormatClicked("JSON") 
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_export_as_json))
                    }
                    Button(onClick = { 
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        showExportDialog = false; onExportFormatClicked("CSV") 
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ui_export_as_csv))
                    }
                    TextButton(onClick = { showExportDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(R.string.ui_cancel))
                    }
                }
            }
        )
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_START) {
                cpuHardwareViewModel.isMonitoringActive.value = true
                batteryNetworkViewModel.isMonitoringActive.value = true
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                cpuHardwareViewModel.isMonitoringActive.value = false
                batteryNetworkViewModel.isMonitoringActive.value = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val tabTitles = listOf(
        R.string.tab_dashboard,
        R.string.tab_cpu,
        R.string.tab_storage,
        R.string.tab_ram,
        R.string.tab_battery,
        R.string.tab_network,
        R.string.tab_display,
        R.string.tab_camera,
        R.string.tab_sensors,
        R.string.tab_security,
        R.string.tab_system,
        R.string.tab_apps,
        R.string.tab_tests,
        R.string.tab_settings
    )
    
    val tabIcons = listOf(
        Icons.Default.Dashboard, Icons.Default.Memory, Icons.Default.SdStorage,
        Icons.Default.DeveloperBoard, Icons.Default.BatteryChargingFull, Icons.Default.Wifi,
        Icons.Default.AspectRatio, Icons.Default.CameraAlt, Icons.Default.Explore,
        Icons.Default.Shield, Icons.Default.Info, Icons.Default.Apps,
        Icons.Default.Science, Icons.Default.Settings
    )
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val animsEnabled by appPreferences.animationsEnabled.collectAsStateWithLifecycle()

    CompositionLocalProvider(com.deviceinfo.gad.ui.components.LocalAnimationsEnabled provides animsEnabled) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.ui_device_info_by_gad), fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
            actions = {
                IconButton(onClick = { showExportDialog = true }) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Export Report")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                com.deviceinfo.gad.ui.components.AnimatedMeshGradient()
                Column(modifier = Modifier.fillMaxSize()) {
                    ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp) },
                    indicator = { tabPositions ->
                        if (pagerState.currentPage < tabPositions.size) {
                            val left by androidx.compose.animation.core.animateDpAsState(
                                targetValue = tabPositions[pagerState.currentPage].left,
                                animationSpec = if (animsEnabled) androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow) else androidx.compose.animation.core.snap(),
                                label = "indicatorLeft"
                            )
                            val right by androidx.compose.animation.core.animateDpAsState(
                                targetValue = tabPositions[pagerState.currentPage].right,
                                animationSpec = if (animsEnabled) androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow) else androidx.compose.animation.core.snap(),
                                label = "indicatorRight"
                            )

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = left)
                                    .width(right - left)
                                    .padding(horizontal = 16.dp)
                                    .height(4.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                ) {
                    tabTitles.forEachIndexed { index, titleRes ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { 
                                coroutineScope.launch { 
                                    val distance = kotlin.math.abs(pagerState.currentPage - index)
                                    if (distance <= 1 && animsEnabled) {
                                        pagerState.animateScrollToPage(index)
                                    } else {
                                        // Using scrollToPage for distance > 1 performs an instantaneous snap jump.
                                        // Because we also set beyondViewportPageCount = 0 on the Pager, 
                                        // intermediate pages are entirely skipped and never composed.
                                        // This guarantees strict resource isolation: background listeners/monitors 
                                        // of distant tabs are never momentarily started/stopped.
                                        if (BuildConfig.DEBUG) {
                                            android.util.Log.d("PagerNavigation", "Instant snap to page $index. Skipping ${distance - 1} intermediate pages.")
                                        }
                                        pagerState.scrollToPage(index)
                                    }
                                } 
                            },
                            icon = {
                                val selected = pagerState.currentPage == index
                                val scale by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (selected) 1.15f else 1f,
                                    animationSpec = if (animsEnabled) androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessLow) else androidx.compose.animation.core.snap(),
                                    label = "tabIconScale$index"
                                )
                                Icon(
                                    tabIcons[index], contentDescription = null,
                                    modifier = Modifier.size(20.dp).scale(scale),
                                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            text = { Text(stringResource(id = titleRes), fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                        )
                    }
                }

                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    beyondViewportPageCount = 0,
                    key = { tabTitles[it] }
                ) { page ->
                    val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    val scale = if (animsEnabled) 1f - (kotlin.math.abs(pageOffset) * 0.15f).coerceIn(0f, 1f) else 1f
                    val alpha = if (animsEnabled) 1f - (kotlin.math.abs(pageOffset) * 0.5f).coerceIn(0f, 1f) else 1f
                    
                    Box(modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }) {
                            when (page) {
                                0 -> DashboardTab(cpuHardwareViewModel, batteryNetworkViewModel)
                                1 -> CpuTab(cpuHardwareViewModel)
                                2 -> RomTab(cpuHardwareViewModel)
                                3 -> RamTab(cpuHardwareViewModel)
                                4 -> BatteryTab(batteryNetworkViewModel)
                                5 -> NetworkTab(batteryNetworkViewModel)
                                6 -> DisplayTab(cpuHardwareViewModel)
                                7 -> CameraTab()
                                8 -> SensorsTab(sensorsViewModel)
                                9 -> SecurityTab()
                                10 -> SystemTab(cpuHardwareViewModel)
                                11 -> ApplicationsTab(appsViewModel)
                                12 -> TestsTab(cpuHardwareViewModel)
                                13 -> SettingsTab(appPreferences, onLanguageChanged)
                            }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: CpuHardwareViewModel, batteryNetworkViewModel: BatteryNetworkViewModel) {
    val totalRam by viewModel.totalRam.collectAsStateWithLifecycle()
    val availRam by viewModel.availRam.collectAsStateWithLifecycle()
    val batteryPct by batteryNetworkViewModel.batteryPct.collectAsStateWithLifecycle()
    val batteryStatus by batteryNetworkViewModel.batteryStatus.collectAsStateWithLifecycle()
    val batteryTemp by batteryNetworkViewModel.batteryTemp.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorage.collectAsStateWithLifecycle()
    val freeStorage by viewModel.freeStorage.collectAsStateWithLifecycle()
    val cpuUsageFloat by viewModel.cpuUsageFloat.collectAsStateWithLifecycle()
    val rxSpeed by batteryNetworkViewModel.rxSpeed.collectAsStateWithLifecycle()
    val txSpeed by batteryNetworkViewModel.txSpeed.collectAsStateWithLifecycle()

    val ramUsed = if (totalRam > 0) totalRam - availRam else 0
    val ramPct = if (totalRam > 0) ramUsed.toFloat() / totalRam.toFloat() else 0f

    val storageUsed = if (totalStorage > 0) totalStorage - freeStorage else 0
    val storagePct = if (totalStorage > 0) storageUsed.toFloat() / totalStorage.toFloat() else 0f

    val cpuLoadHistory by viewModel.cpuLoadHistory.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        viewModel.startCpuMonitoring()
        viewModel.startRamMonitoring()
        viewModel.startStorageMonitoring()
        batteryNetworkViewModel.startBatteryMonitoring()
        batteryNetworkViewModel.startNetworkMonitoring()
        onDispose {
            viewModel.stopCpuMonitoring()
            viewModel.stopRamMonitoring()
            viewModel.stopStorageMonitoring()
            batteryNetworkViewModel.stopBatteryMonitoring()
            batteryNetworkViewModel.stopNetworkMonitoring()
        }
    }

    val isCpuReady by viewModel.isCpuReady.collectAsStateWithLifecycle()
    val isRamReady by viewModel.isRamReady.collectAsStateWithLifecycle()
    val isStorageReady by viewModel.isStorageReady.collectAsStateWithLifecycle()
    val isBatteryReady by batteryNetworkViewModel.isBatteryReady.collectAsStateWithLifecycle()
    val isNetworkReady by batteryNetworkViewModel.isNetworkReady.collectAsStateWithLifecycle()

    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(2500L); timedOut = true }
    val stillLoading = !timedOut && !(isCpuReady && isRamReady && isStorageReady && isBatteryReady && isNetworkReady)

    androidx.compose.animation.AnimatedContent(
        targetState = stillLoading,
        transitionSpec = {
            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(500)) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(500))
        },
        label = "isInitialLoadingAnimation"
    ) { loading ->
        if (loading) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                com.deviceinfo.gad.ui.components.ShimmerLoadingPlaceholder(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Device Card
                com.deviceinfo.gad.ui.components.GlassCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(android.os.Build.MODEL, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(android.os.Build.MANUFACTURER.uppercase(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                            }
                            val isDarkTheme = MaterialTheme.colorScheme.surface.let { it.luminance() < 0.5f }
                            val batteryColor = when {
                                batteryPct >= 50f -> if (isDarkTheme) Color(0xFF00E676) else Color(0xFF2E7D32)
                                batteryPct >= 20f -> if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57F17)
                                else -> MaterialTheme.colorScheme.error
                            }
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                                CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 6.dp)
                                val animScore by androidx.compose.animation.core.animateFloatAsState(targetValue = batteryPct / 100f, animationSpec = androidx.compose.animation.core.tween(1500), label = "battery_dial")
                                CircularProgressIndicator(progress = { animScore }, modifier = Modifier.fillMaxSize(), color = batteryColor, strokeWidth = 6.dp, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                                Text("${batteryPct.toInt()}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InfoItem(label = "Android", value = android.os.Build.VERSION.RELEASE)
                            val patch = if (android.os.Build.VERSION.SDK_INT >= 23) android.os.Build.VERSION.SECURITY_PATCH else "N/A"
                            InfoItem(label = "Patch Level", value = patch)
                            InfoItem(label = "API", value = android.os.Build.VERSION.SDK_INT.toString())
                        }
                    }
                }

                // --- SMART DEVICE INSIGHTS ---
                val insights = mutableListOf<String>()
                if (ramPct > 0.85f) insights.add("High memory pressure detected. Background apps may close.")
                if (storagePct > 0.90f) insights.add("Critical: Storage almost full. Clean up recommended.")
                else if (storagePct > 0.80f) insights.add("Storage usage is high.")
                if (batteryTemp > 40f) insights.add("Battery temperature elevated (${batteryTemp}°C).")
                if (cpuUsageFloat > 0.90f) insights.add("High CPU load.")
                if (insights.isEmpty()) insights.add("All systems nominal. Device operates perfectly.")
                
                com.deviceinfo.gad.ui.components.GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.insight_intelligence), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    insights.forEach { insight ->
                        Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.6f)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                val isDarkTheme = MaterialTheme.colorScheme.surface.let { it.luminance() < 0.5f }
                val cpuColor = if (isDarkTheme) Color(0xFF00E5FF) else Color(0xFF00838F)
                val ramColor = if (isDarkTheme) Color(0xFFE040FB) else Color(0xFF6A1B9A)
                val storageColor = if (isDarkTheme) Color(0xFFFFD54F) else Color(0xFFF57F17)
                val downColor = if (isDarkTheme) Color(0xFF00E676) else Color(0xFF2E7D32)
                val upColor = MaterialTheme.colorScheme.error

                // Live Quick Stats
                Text(stringResource(R.string.metric_real_time), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                val ramLoadHistory by viewModel.ramLoadHistory.collectAsStateWithLifecycle()
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val displayCpu = if (cpuUsageFloat >= 0f) "${(cpuUsageFloat * 100).toInt()}%" else "N/A"
                    val displayProg = if (cpuUsageFloat >= 0f) cpuUsageFloat else 0f
                    DashboardTrendCard(title = "CPU", value = displayCpu, icon = androidx.compose.material.icons.Icons.Default.Memory, color = cpuColor, progress = displayProg, history = cpuLoadHistory, modifier = Modifier.weight(1f))
                    DashboardTrendCard(title = "RAM", value = "${(ramPct * 100).toInt()}%", icon = androidx.compose.material.icons.Icons.Default.Storage, color = ramColor, progress = ramPct, history = ramLoadHistory, modifier = Modifier.weight(1f))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardMetricCard(title = "BATTERY", value = "${batteryPct.toInt()}%", icon = androidx.compose.material.icons.Icons.Default.Bolt, color = downColor, progress = batteryPct / 100f, modifier = Modifier.weight(1f))
                    DashboardMetricCard(title = "STORAGE", value = "${(storagePct * 100).toInt()}%", icon = androidx.compose.material.icons.Icons.Default.Folder, color = storageColor, progress = storagePct, modifier = Modifier.weight(1f))
                }

                // Network Insight Card
                val networkType by batteryNetworkViewModel.networkType.collectAsStateWithLifecycle()
                val ipAddress by batteryNetworkViewModel.ipAddress.collectAsStateWithLifecycle()
                val pingResult by batteryNetworkViewModel.pingResult.collectAsStateWithLifecycle()
                com.deviceinfo.gad.ui.components.GlassCard(delayMillis = 200) {
                    Text(stringResource(R.string.metric_network), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(R.string.net_download), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(formatBytes(rxSpeed.toLong()) + "/s", color = downColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.net_upload), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            Text(formatBytes(txSpeed.toLong()) + "/s", color = upColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    com.deviceinfo.gad.ui.components.DataRow("Connection Type", networkType)
                    com.deviceinfo.gad.ui.components.DataRow("IP Address", ipAddress)
                    com.deviceinfo.gad.ui.components.DataRow("Latency (Ping)", pingResult)
                }
                
                // Hardware Summary Card
                com.deviceinfo.gad.ui.components.GlassCard(delayMillis = 300) {
                    Text(stringResource(R.string.ui_hardware_summary), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                    com.deviceinfo.gad.ui.components.DataRow("Board", viewModel.cpuBoard)
                    com.deviceinfo.gad.ui.components.DataRow("Architecture", viewModel.cpuArch)
                    com.deviceinfo.gad.ui.components.DataRow("Cores", Runtime.getRuntime().availableProcessors().toString())
                    com.deviceinfo.gad.ui.components.DataRow(stringResource(R.string.battery_temp), String.format(java.util.Locale.US, "%.1f °C", batteryTemp))
                }
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DashboardMetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, progress: Float, modifier: Modifier = Modifier) {
    com.deviceinfo.gad.ui.components.GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val animProg by androidx.compose.animation.core.animateFloatAsState(
                targetValue = progress * 100f, 
                animationSpec = androidx.compose.animation.core.tween(1000), 
                label = "progText"
            )
            // If value contains letters like M, G, it will just show `AnimatedContent`
            // but for % it's better to show rolling numbers.
            if (value.endsWith("%")) {
                Text("${animProg.toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(400)) { height -> height } + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))).togetherWith(androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(400)) { height -> -height } + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)))
                    },
                    label = "MetricValue"
                ) { animatedValue ->
                    Text(animatedValue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val animProgBar by androidx.compose.animation.core.animateFloatAsState(targetValue = progress, animationSpec = androidx.compose.animation.core.tween(1000), label = "progBar")
        LinearProgressIndicator(
            progress = { animProgBar },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun DashboardTrendCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, progress: Float, history: List<Float>, modifier: Modifier = Modifier) {
    com.deviceinfo.gad.ui.components.GlassCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            val animProg by androidx.compose.animation.core.animateFloatAsState(
                targetValue = progress * 100f, 
                animationSpec = androidx.compose.animation.core.tween(1000), 
                label = "progText"
            )
            if (value.endsWith("%")) {
                Text("${animProg.toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (androidx.compose.animation.slideInVertically(animationSpec = androidx.compose.animation.core.tween(400)) { height -> height } + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400))).togetherWith(androidx.compose.animation.slideOutVertically(animationSpec = androidx.compose.animation.core.tween(400)) { height -> -height } + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)))
                    },
                    label = "MetricValue"
                ) { animatedValue ->
                    Text(animatedValue, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(40.dp)) {
            com.deviceinfo.gad.ui.components.UsageLineChart(
                data = history,
                lineColor = color,
                highUsageThreshold = 0.8f
            )
        }
    }
}

@Composable
fun CpuTab(viewModel: CpuHardwareViewModel) {
    DisposableEffect(Unit) {
        viewModel.startCpuMonitoring()
        onDispose { viewModel.stopCpuMonitoring() }
    }

    val usage by viewModel.cpuUsage.collectAsStateWithLifecycle()
    val usageFloat by viewModel.cpuUsageFloat.collectAsStateWithLifecycle()
    val history by viewModel.cpuLoadHistory.collectAsStateWithLifecycle()
    val coresUsage by viewModel.cpuPerCoreUsage.collectAsStateWithLifecycle()
    
    val thermal by viewModel.cpuThermal.collectAsStateWithLifecycle()
    val thermalFloat by viewModel.cpuThermalFloat.collectAsStateWithLifecycle()
    val isThrottling by viewModel.isThermalThrottling.collectAsStateWithLifecycle()
    
    val governor by viewModel.cpuGovernor.collectAsStateWithLifecycle()
    val freq by viewModel.cpuFreq.collectAsStateWithLifecycle()
    val freqMinMax by viewModel.cpuFreqMinMax.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        
        // Gauge and Main Info
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.quick_cpu).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Start).padding(bottom = 12.dp, start = 8.dp))
                
                // Animated Gauge
                val validUsage = if (usageFloat >= 0f) usageFloat else 0f
                val animUsage by androidx.compose.animation.core.animateFloatAsState(targetValue = validUsage, animationSpec = androidx.compose.animation.core.tween(500), label = "")
                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 12.dp
                    )
                    CircularProgressIndicator(
                        progress = { animUsage },
                        modifier = Modifier.fillMaxSize(),
                        color = if (animUsage > 0.8f) Color.Red else MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val displayTxt = if (usageFloat >= 0f) "${(animUsage * 100).toInt()}%" else "N/A"
                        Text(displayTxt, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.ui_usage), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                val isInit by viewModel.isCpuReady.collectAsStateWithLifecycle()
                val isNotInit = !isInit
                InfoRow(stringResource(R.string.cpu_architecture), viewModel.cpuArch, isNotInit)
                InfoRow(stringResource(R.string.cpu_board), viewModel.cpuBoard, isNotInit)
                InfoRow("Governor", governor, isNotInit)
                InfoRow("Frequency", freq, isNotInit)
                InfoRow("Min/Max Freq", freqMinMax, isNotInit)
            }
        }
        
        // Real-time graph
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.ui_cpu_load_history), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                val lineColor = MaterialTheme.colorScheme.primary
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    val w = size.width
                    val h = size.height
                    val step = if (history.size > 1) w / (history.size - 1) else w
                    
                    val path = androidx.compose.ui.graphics.Path()
                    history.forEachIndexed { i, v ->
                        val x = i * step
                        val y = h - (v * h)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                }
            }
        }
        
        // Temperature and Throttling
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isThrottling) Color(0xFFFFCDD2) else MaterialTheme.colorScheme.surface
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isThrottling) Color.Red else MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(stringResource(R.string.ui_cpu_temperature), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isThrottling) Color(0xFFC62828) else MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(thermal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (isThrottling) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurface)
                }
                if (isThrottling) {
                    Icon(androidx.compose.material.icons.Icons.Default.Warning, contentDescription = "Throttling", tint = Color.Red, modifier = Modifier.size(36.dp))
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.Thermostat, contentDescription = "Normal", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                }
            }
        }

        // Per-core Activity Bars
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.cpu_per_core).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                if (coresUsage.isEmpty()) {
                    Text(stringResource(R.string.ui_n_a), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                } else {
                    coresUsage.forEachIndexed { i, cUsage ->
                        val animCUsage by androidx.compose.animation.core.animateFloatAsState(targetValue = cUsage, animationSpec = androidx.compose.animation.core.tween(500), label = "")
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Core $i", modifier = Modifier.width(60.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.weight(1f).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.CircleShape)) {
                                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(animCUsage).background(if (animCUsage > 0.8f) Color.Red else MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                            }
                            Text(String.format(java.util.Locale.US, "%.1f%%", animCUsage * 100f), modifier = Modifier.width(60.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RamTab(viewModel: CpuHardwareViewModel) {
    DisposableEffect(Unit) {
        viewModel.startRamMonitoring()
        onDispose { viewModel.stopRamMonitoring() }
    }

    val total by viewModel.totalRam.collectAsStateWithLifecycle()
    val avail by viewModel.availRam.collectAsStateWithLifecycle()
    val swapTotal by viewModel.swapTotal.collectAsStateWithLifecycle()
    val swapFree by viewModel.swapFree.collectAsStateWithLifecycle()
    val ramLoadHistory by viewModel.ramLoadHistory.collectAsStateWithLifecycle()

    var visible by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn()
        ) {
            com.deviceinfo.gad.ui.components.GlassCard {
                Text(stringResource(R.string.sys_memory_ram), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                val ramUsed = if (total > 0) total - avail else 0
                val ramPct = if (total > 0) ramUsed.toFloat() / total.toFloat() else 0f
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatBytes(avail)} Free", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("${formatBytes(ramUsed)} / ${formatBytes(total)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val animRam by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = ramPct,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "ramBar"
                )
                
                // Segmented Horizontal Bar
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    val activeColor = Color(0xFFE53935)
                    val cacheColor = Color(0xFFFFB300)
                    val systemColor = Color(0xFF1E88E5)
                    
                    val activeW = animRam * 0.5f
                    val cacheW = animRam * 0.3f
                    val systemW = animRam * 0.2f
                    
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.fillMaxHeight().weight(activeW.coerceAtLeast(0.001f)).background(activeColor))
                        Box(modifier = Modifier.fillMaxHeight().weight(cacheW.coerceAtLeast(0.001f)).background(cacheColor))
                        Box(modifier = Modifier.fillMaxHeight().weight(systemW.coerceAtLeast(0.001f)).background(systemColor))
                        Box(modifier = Modifier.fillMaxHeight().weight((1f - animRam).coerceAtLeast(0.001f)).background(Color.Transparent))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CategoryLegend(stringResource(R.string.sys_active), Color(0xFFE53935))
                    CategoryLegend(stringResource(R.string.sys_cache), Color(0xFFFFB300))
                    CategoryLegend(stringResource(R.string.sys_system), Color(0xFF1E88E5))
                    CategoryLegend(stringResource(R.string.sys_free), MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn(animationSpec = tween(500, delayMillis = 100))
        ) {
            com.deviceinfo.gad.ui.components.GlassCard {
                Text(stringResource(R.string.sys_virtual_memory), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                val swapUsed = if (swapTotal > 0) swapTotal - swapFree else 0
                val swapPct = if (swapTotal > 0) swapUsed.toFloat() / swapTotal.toFloat() else 0f
                val animSwap by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = swapPct,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                    ),
                    label = "swapBar"
                )
                
                LinearProgressIndicator(
                    progress = { animSwap },
                    modifier = Modifier.fillMaxWidth().height(12.dp).padding(horizontal = 8.dp).clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFFFB300),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatBytes(swapUsed) + " " + stringResource(R.string.sys_used), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    Text(formatBytes(swapFree) + " " + stringResource(R.string.sys_free), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { 100 }) + fadeIn(animationSpec = tween(500, delayMillis = 200))
        ) {
            com.deviceinfo.gad.ui.components.GlassCard {
                Text(stringResource(R.string.sys_ram_pressure), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.sys_usage_history), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    val currentVal = (ramLoadHistory.lastOrNull() ?: 0f) * 100f
                    Text("${currentVal.toInt()}%", color = if (currentVal > 80f) MaterialTheme.colorScheme.error else Color(0xFF00E676), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 8.dp, vertical = 8.dp)) {
                    val isDarkTheme = MaterialTheme.colorScheme.surface.let { it.luminance() < 0.5f }
                    val color = if(isDarkTheme) Color(0xFF00E676) else Color(0xFF2E7D32)
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val step = if (ramLoadHistory.size > 1) w / (ramLoadHistory.size - 1) else w
                        val path = androidx.compose.ui.graphics.Path()
                        val fillPath = androidx.compose.ui.graphics.Path()
                        ramLoadHistory.forEachIndexed { i, v ->
                            val x = i * step
                            val y = h - (v * h)
                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            if (i == ramLoadHistory.size - 1) {
                                fillPath.lineTo(x, h)
                                fillPath.close()
                            }
                        }
                        
                        drawPath(fillPath, brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(color.copy(alpha=0.4f), color.copy(alpha=0.0f)),
                            startY = 0f, endY = h
                        ))
                        
                        drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryTab(viewModel: BatteryNetworkViewModel) {
    DisposableEffect(Unit) {
        viewModel.startBatteryMonitoring()
        onDispose { viewModel.stopBatteryMonitoring() }
    }

    val isBatteryReady by viewModel.isBatteryReady.collectAsStateWithLifecycle()
    val isInit = !isBatteryReady
    val level by viewModel.batteryPct.collectAsStateWithLifecycle()
    val health by viewModel.batteryHealth.collectAsStateWithLifecycle()

    val temp by viewModel.batteryTemp.collectAsStateWithLifecycle()
    val status by viewModel.batteryStatus.collectAsStateWithLifecycle()
    val volt by viewModel.batteryVolt.collectAsStateWithLifecycle()
    val tech by viewModel.batteryTech.collectAsStateWithLifecycle()
    val current by viewModel.batteryCurrent.collectAsStateWithLifecycle()
    val estTime by viewModel.batteryEstTime.collectAsStateWithLifecycle()
    val cycleCount by viewModel.batteryCycleCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        
        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_battery_health_statu), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    val animLevel by androidx.compose.animation.core.animateFloatAsState(targetValue = level / 100f, animationSpec = androidx.compose.animation.core.tween(1000), label = "bat")
                    val isCharging = status.contains("Charging", true)
                    
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        strokeWidth = 8.dp
                    )
                    CircularProgressIndicator(
                        progress = { animLevel },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isCharging) Color(0xFF00E676) else MaterialTheme.colorScheme.primary,
                        strokeWidth = 8.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${level.toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        if (isCharging) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Column(modifier = Modifier.padding(start = 24.dp).weight(1f)) {
                    Text(stringResource(R.string.ui_health), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(health, color = if(health.equals("Good", true)) Color(0xFF00E676) else MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.ui_status), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(status, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_advanced_metrics), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            InfoRow(stringResource(R.string.battery_current), current, isInit)
            InfoRow(stringResource(R.string.battery_temp), String.format(java.util.Locale.US, "%.1f °C", temp), isInit)
            InfoRow(stringResource(R.string.battery_voltage), "${volt} mV", isInit)
            
            // Calculate pseudo wattage if possible
            val currentMaOrUa = current.replace(Regex("[^0-9.-]"), "").toFloatOrNull() ?: 0f
            if (currentMaOrUa != 0f && volt > 0) {
                // If it's seemingly mA
                val mA = if (Math.abs(currentMaOrUa) > 50000) currentMaOrUa / 1000f else currentMaOrUa
                val w = Math.abs(mA * (volt / 1000f) / 1000f)
                InfoRow("Power Consumption", String.format(java.util.Locale.US, "%.2f W", w), isInit)
            }
            
            InfoRow(stringResource(R.string.battery_tech), tech, isInit)
            InfoRow(stringResource(R.string.battery_est_time), estTime, isInit)
            InfoRow(stringResource(R.string.battery_cycle), cycleCount, isInit)
        }

        Spacer(modifier = Modifier.height(16.dp))

        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_battery_insights_hea), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            
            val isCharging = status.contains("Charging", true)

            // Health Score Logic Base
            val healthModifier = if (temp > 40f) -15 else if (temp > 35f) -5 else 0
            val ageModifier = if (cycleCount != "Unknown" && cycleCount.toIntOrNull() != null) {
                if (cycleCount.toInt() > 500) -20 else if (cycleCount.toInt() > 300) -10 else 0
            } else 0
            val baseScore = if (health.equals("Good", true)) 95 else 70
            val finalScore = (baseScore + healthModifier + ageModifier).coerceIn(0, 100)

            InfoRow("Battery Health Score", "$finalScore / 100", isInit)
            
            val wearEst = if (finalScore == 95) "Excellent (Minimal Wear)" else if (finalScore > 80) "Good Condition (Normal Wear)" else "Degraded (High Wear)"
            InfoRow("Wear Estimation", wearEst, isInit)
            
            val chargeBehavior = if (isCharging) {
                if (temp > 38f) "Fast Charging (Thermal Limit Warning)" else "Healthy Charging Efficiency"
            } else {
                "Discharging Nominally"
            }
            InfoRow("Behavior Analysis", chargeBehavior, isInit)

            Spacer(modifier = Modifier.height(12.dp))
            
            // Warnings & Recommendations
            val warningCount = mutableListOf<String>()
            if (temp > 38f) warningCount.add("High temperature during operation. Unplug or close heavy apps.")
            if (level < 20f && !isCharging) warningCount.add("Battery is critically low. Charge soon to reduce degradation.")
            if (warningCount.isEmpty()) warningCount.add("No critical risks detected. Battery is operating efficiently.")

            warningCount.forEach { w ->
                Row(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = androidx.compose.material.icons.Icons.Default.Info, contentDescription = null, tint = if (w.contains("No critical")) Color(0xFF00E676) else MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(w, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun NetworkTab(viewModel: BatteryNetworkViewModel) {
    DisposableEffect(Unit) {
        viewModel.startNetworkMonitoring()
        onDispose { viewModel.stopNetworkMonitoring() }
    }

    val isNetworkReady by viewModel.isNetworkReady.collectAsStateWithLifecycle()
    val isInit = !isNetworkReady
    val ssid by viewModel.wifiSsid.collectAsStateWithLifecycle()
    val speed by viewModel.linkSpeed.collectAsStateWithLifecycle()
    val type by viewModel.networkType.collectAsStateWithLifecycle()
    val ip by viewModel.ipAddress.collectAsStateWithLifecycle()
    val dns by viewModel.dnsServers.collectAsStateWithLifecycle()
    val gateway by viewModel.defaultGateway.collectAsStateWithLifecycle()
    val ifaces by viewModel.networkInterfaceList.collectAsStateWithLifecycle()
    val ping by viewModel.pingResult.collectAsStateWithLifecycle()
    
    val rHist by viewModel.rxHistory.collectAsStateWithLifecycle()
    val tHist by viewModel.txHistory.collectAsStateWithLifecycle()
    val currentRx by viewModel.rxSpeed.collectAsStateWithLifecycle()
    val currentTx by viewModel.txSpeed.collectAsStateWithLifecycle()

    var pingTarget by remember { mutableStateOf("google.com") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            // Will let the viewmodel handle update in the next loop automatically
        }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_network_connection), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            InfoRow(stringResource(R.string.network_type), type, isInit)
            InfoRow(stringResource(R.string.wifi_ssid), ssid, isInit)
            InfoRow(stringResource(R.string.link_speed), speed, isInit)
            InfoRow(stringResource(R.string.ip_address), ip, isInit)
            InfoRow("DNS Servers", dns, isInit)
            InfoRow("Default Gateway", gateway, isInit)
        }
        
        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_network_interfaces), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            if (ifaces.isEmpty()) {
                Text(stringResource(R.string.ui_no_interfaces_found), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            } else {
                ifaces.forEach { iface ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SettingsEthernet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(iface.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.weight(1f))
                                if (iface.isUp) {
                                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFF00E676)))
                                } else {
                                    Box(modifier = Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color.Red))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("MAC: ${iface.macAddress}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("IP: ${iface.ipAddresses.joinToString(", ").takeIf { it.isNotBlank() } ?: "None"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("MTU: ${iface.mtu} | Loopback: ${iface.isLoopback}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_live_traffic), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(stringResource(R.string.ui_download), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(formatBytes(currentRx.toLong()) + "/s", color = Color(0xFF00E676), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.ui_upload), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text(formatBytes(currentTx.toLong()) + "/s", color = Color(0xFFFF5252), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            val downloadColor = Color(0xFF00E676)
            val uploadColor = Color(0xFFFF5252)
            
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                val w = size.width
                val h = size.height
                
                val maxRx = rHist.maxOrNull()?.coerceAtLeast(1024f) ?: 1024f
                val maxTx = tHist.maxOrNull()?.coerceAtLeast(1024f) ?: 1024f
                val maxGlobal = maxOf(maxRx, maxTx)
                
                val step = if (rHist.size > 1) w / (rHist.size - 1) else w
                
                val pathRx = androidx.compose.ui.graphics.Path()
                rHist.forEachIndexed { i, v ->
                    val x = i * step
                    val y = h - ((v / maxGlobal) * h)
                    if (i == 0) pathRx.moveTo(x, y) else pathRx.lineTo(x, y)
                }
                drawPath(pathRx, color = downloadColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                
                val pathTx = androidx.compose.ui.graphics.Path()
                tHist.forEachIndexed { i, v ->
                    val x = i * step
                    val y = h - ((v / maxGlobal) * h)
                    if (i == 0) pathTx.moveTo(x, y) else pathTx.lineTo(x, y)
                }
                drawPath(pathTx, color = uploadColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }

        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_network_diagnostics), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            OutlinedTextField(
                value = pingTarget,
                onValueChange = { pingTarget = it },
                label = { Text(stringResource(R.string.ui_ping_target)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.performPingTest(pingTarget) }) {
                    Text(stringResource(R.string.ui_run_ping_test))
                }
                Spacer(modifier = Modifier.width(16.dp))
                if (ping.contains("ms")) {
                    Text(ping, color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                } else if (ping.contains("Error") || ping.contains("unreachable")) {
                    Text(ping, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else {
                    Text(ping, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        
        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.ui_network_insights), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            
            val isConnected = type != "None" && type != "Unknown"
            val qualityScore = if (isConnected) {
                var score = 100
                if (ping.contains("ms")) {
                    val p = Regex("[0-9.]+").find(ping)?.value?.toDoubleOrNull()?.toInt() ?: 0
                    if (p > 100) score -= 30 else if (p > 50) score -= 10
                }
                score
            } else 0
            
            InfoRow("Connection Quality", if (isConnected) "$qualityScore / 100" else "Offline", isInit)
            
            val securityStatus = if (type == "Wi-Fi" && ssid.lowercase().contains("guest")) "Vulnerable (Guest Net)" else "Secured"
            InfoRow("Security Status", securityStatus, isInit)

            val dnsHealth = if (dns.contains("8.8.8.8") || dns.contains("1.1.1.1")) "Optimal Route" else "Standard Route"
            InfoRow("DNS Routing", dnsHealth, isInit)
        }
    }
}

@Composable
fun DisplayTab(viewModel: CpuHardwareViewModel) {
    val size by viewModel.screenSize.collectAsStateWithLifecycle()
    val res by viewModel.screenRes.collectAsStateWithLifecycle()
    val dpi by viewModel.screenDpi.collectAsStateWithLifecycle()
    val fps by viewModel.refreshRate.collectAsStateWithLifecycle()
    val dimensions by viewModel.physicalDimensions.collectAsStateWithLifecycle()
    val hdr by viewModel.hdrCapabilities.collectAsStateWithLifecycle()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    var currentBrightness by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(85) } // base score
    var isHdr by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val wm = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay
        
        isHdr = false
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            isHdr = display.isHdr
        }
        
        try {
            val brightnessInt = android.provider.Settings.System.getInt(context.contentResolver, android.provider.Settings.System.SCREEN_BRIGHTNESS)
            currentBrightness = (brightnessInt / 255f) * 100f
        } catch (e: Exception) { e.printStackTrace() }

        var calScore = 50
        val fpsVal = fps.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 60f
        if (fpsVal >= 120f) calScore += 20 else if (fpsVal >= 90f) calScore += 10
        if (isHdr) calScore += 15
        
        val dpiVal = dpi.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
        if (dpiVal >= 400) calScore += 15 else if (dpiVal >= 300) calScore += 10
        
        score = calScore.coerceIn(0, 100)
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        // Display Analyzer Score
        com.deviceinfo.gad.ui.components.GlassCard {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.disp_quality), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("$score / 100", color = MaterialTheme.colorScheme.primary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    val desc = if (score >= 90) stringResource(R.string.disp_flagship) else if (score >= 70) stringResource(R.string.disp_high_quality) else stringResource(R.string.disp_standard)
                    Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    val animScore by androidx.compose.animation.core.animateFloatAsState(targetValue = score / 100f, animationSpec = androidx.compose.animation.core.tween(1500), label = "score")
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 8.dp)
                    CircularProgressIndicator(progress = { animScore }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary, strokeWidth = 8.dp, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    Icon(androidx.compose.material.icons.Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Refresh Rate Gauge
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.disp_refresh_rate), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        val fpsVal = fps.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 60f
                        val animFps by androidx.compose.animation.core.animateFloatAsState(targetValue = fpsVal, animationSpec = androidx.compose.animation.core.tween(1000), label = "fps")
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(color = Color.White.copy(alpha=0.1f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            drawArc(color = Color(0xFF00E676), startAngle = 135f, sweepAngle = 270f * (animFps / 144f).coerceIn(0f, 1f), useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Text("${animFps.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.ui_hz), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            
            // Brightness Gauge
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.weight(1f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.disp_brightness), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        val animBright by androidx.compose.animation.core.animateFloatAsState(targetValue = currentBrightness, animationSpec = androidx.compose.animation.core.spring(), label = "bright")
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(color = Color.White.copy(alpha=0.1f), startAngle = 135f, sweepAngle = 270f, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            drawArc(color = Color(0xFFFFD54F), startAngle = 135f, sweepAngle = 270f * (animBright / 100f).coerceIn(0f, 1f), useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Text("${animBright.toInt()}%", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.disp_live_level), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        com.deviceinfo.gad.ui.components.GlassCard {
            Text(stringResource(R.string.disp_capabilities), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
            val yesStr = stringResource(R.string.yes)
            val noStr = stringResource(R.string.no)
            val isInit = false
            InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_size), size, isInit)
            InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_res), res, isInit)
            InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_physical), dimensions, isInit)
            InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_dpi), dpi, isInit)
            InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_refresh), fps, isInit)
            InfoRow(stringResource(R.string.disp_hdr_supported), if (isHdr) yesStr else noStr, isInit)
            if (isHdr) InfoRow(androidx.compose.ui.res.stringResource(R.string.disp_hdr), hdr, isInit)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SensorsTab(viewModel: SensorsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sm = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager }
    val allSensors = remember { sm.getSensorList(android.hardware.Sensor.TYPE_ALL) }
    
    val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle()
    val azimuth by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    val pitch by viewModel.bubblePitch.collectAsStateWithLifecycle()
    val roll by viewModel.bubbleRoll.collectAsStateWithLifecycle()

    val animAzimuth by androidx.compose.animation.core.animateFloatAsState(targetValue = azimuth, animationSpec = androidx.compose.animation.core.spring(), label = "azimuth")
    val animPitch by androidx.compose.animation.core.animateFloatAsState(targetValue = pitch, animationSpec = androidx.compose.animation.core.spring(), label = "pitch")
    val animRoll by androidx.compose.animation.core.animateFloatAsState(targetValue = roll, animationSpec = androidx.compose.animation.core.spring(), label = "roll")

    val motionSensors = remember { allSensors.filter { it.type in listOf(android.hardware.Sensor.TYPE_ACCELEROMETER, android.hardware.Sensor.TYPE_GYROSCOPE, android.hardware.Sensor.TYPE_GRAVITY, android.hardware.Sensor.TYPE_LINEAR_ACCELERATION, android.hardware.Sensor.TYPE_ROTATION_VECTOR) } }
    val envSensors = remember { allSensors.filter { it.type in listOf(android.hardware.Sensor.TYPE_LIGHT, android.hardware.Sensor.TYPE_PRESSURE, android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY, android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE) } }
    val posSensors = remember { allSensors.filter { it.type in listOf(android.hardware.Sensor.TYPE_MAGNETIC_FIELD, android.hardware.Sensor.TYPE_PROXIMITY) } }
    val otherSensors = remember { allSensors.filter { it !in motionSensors && it !in envSensors && it !in posSensors } }

    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.startMonitoring(allSensors)
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(stringResource(R.string.ui_total_sensors), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${allSensors.size}", color = MaterialTheme.colorScheme.primary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Icon(androidx.compose.material.icons.Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
        }
        
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Compass Card
                Box(modifier = Modifier.weight(1f)) {
                    com.deviceinfo.gad.ui.components.GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            Text(androidx.compose.ui.res.stringResource(R.string.sensor_compass).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(color = Color.White.copy(alpha=0.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                    rotate(-animAzimuth) {
                                        drawLine(color = Color(0xFFFF5252), start = center, end = androidx.compose.ui.geometry.Offset(center.x, center.y - 45.dp.toPx()), strokeWidth = 10f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                        drawCircle(color = Color.White, radius = 6f, center = center)
                                        drawLine(color = Color(0xFF00E5FF), start = center, end = androidx.compose.ui.geometry.Offset(center.x, center.y + 45.dp.toPx()), strokeWidth = 6f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${animAzimuth.toInt()}°", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                
                // Bubble Level Card
                Box(modifier = Modifier.weight(1f)) {
                    com.deviceinfo.gad.ui.components.GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                            Text(androidx.compose.ui.res.stringResource(R.string.sensor_bubble).uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    val radius = 40.dp.toPx()
                                    drawCircle(color = Color.White.copy(alpha=0.1f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                    drawCircle(color = Color.White.copy(alpha=0.05f))
                                    drawLine(color = Color.White.copy(alpha=0.2f), start = androidx.compose.ui.geometry.Offset(0f, center.y), end = androidx.compose.ui.geometry.Offset(size.width, center.y), strokeWidth = 2f)
                                    drawLine(color = Color.White.copy(alpha=0.2f), start = androidx.compose.ui.geometry.Offset(center.x, 0f), end = androidx.compose.ui.geometry.Offset(center.x, size.height), strokeWidth = 2f)
                                    val xOffset = (animRoll / 9.8f) * radius
                                    val yOffset = (animPitch / 9.8f) * radius
                                    val bColor = if (Math.abs(animRoll)<0.2f && Math.abs(animPitch)<0.2f) Color(0xFF00E676) else Color(0xFFFFD54F)
                                    drawCircle(color = bColor, radius = 12.dp.toPx(), center = androidx.compose.ui.geometry.Offset(center.x + xOffset, center.y + yOffset))
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("P: ${animPitch.toInt()}° R: ${animRoll.toInt()}°", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        @Composable
        fun SensorItem(sensor: android.hardware.Sensor, liveData: String?) {
            com.deviceinfo.gad.ui.components.GlassCard(modifier = Modifier.padding(vertical = 6.dp)) {
                Column {
                    Text(sensor.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Vendor: ${sensor.vendor} | Version: ${sensor.version}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Text("Power: ${sensor.power}mA | Max Range: ${sensor.maximumRange}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(liveData ?: "Waiting for data...", color = Color(0xFF00E676), fontSize = 14.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            }
        }

        if (motionSensors.isNotEmpty()) {
            stickyHeader { Text(stringResource(R.string.ui_motion_sensors), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            items(motionSensors) { sensor -> SensorItem(sensor, sensors[sensor.name]) }
        }
        
        if (envSensors.isNotEmpty()) {
            stickyHeader { Text(stringResource(R.string.ui_environmental_sensor), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            items(envSensors) { sensor -> SensorItem(sensor, sensors[sensor.name]) }
        }
        
        if (posSensors.isNotEmpty()) {
            stickyHeader { Text(stringResource(R.string.ui_position_sensors), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            items(posSensors) { sensor -> SensorItem(sensor, sensors[sensor.name]) }
        }
        
        if (otherSensors.isNotEmpty()) {
            stickyHeader { Text(stringResource(R.string.ui_other_sensors), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            items(otherSensors) { sensor -> SensorItem(sensor, sensors[sensor.name]) }
        }
    }
}



@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun InfoRow(title: String, value: String, isLoading: Boolean = false) {
    com.deviceinfo.gad.ui.components.DataRow(title, value, isLoading)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.2f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}




