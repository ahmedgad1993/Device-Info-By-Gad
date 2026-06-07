package com.example

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private lateinit var appPreferences: AppPreferences
    private val deviceViewModel: DeviceViewModel by viewModels()

    private fun exportReport() {
        val report = deviceViewModel.generateExportReport()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Device Info Report")
            putExtra(android.content.Intent.EXTRA_TEXT, report)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.export_report)))
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
        appPreferences = AppPreferences(this)
        enableEdgeToEdge()
        setContent {
            val currentTheme by appPreferences.themeMode.collectAsStateWithLifecycle()
            val isDark = when(currentTheme) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            MyApplicationTheme(darkTheme = isDark) {
                AppMainScreen(
                    viewModel = deviceViewModel,
                    appPreferences = appPreferences,
                    onLanguageChanged = { lang ->
                        appPreferences.setLanguage(lang)
                        recreate()
                    },
                    onExportClicked = { exportReport() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(
    viewModel: DeviceViewModel, 
    appPreferences: AppPreferences,
    onLanguageChanged: (String) -> Unit,
    onExportClicked: () -> Unit
) {
    val tabTitles = listOf(
        R.string.tab_dashboard,
        R.string.tab_cpu,
        R.string.tab_ram,
        R.string.tab_battery,
        R.string.tab_network,
        R.string.tab_display,
        R.string.tab_camera,
        R.string.tab_sensors,
        R.string.tab_apps,
        R.string.tab_tests,
        R.string.tab_security,
        R.string.tab_settings
    )
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabTitles.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info By Gad", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = onExportClicked) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Report")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp) },
                indicator = { tabPositions ->
                    if (pagerState.currentPage < tabPositions.size) {
                       TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary)
                    }
                }
            ) {
                tabTitles.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = { Text(stringResource(id = titleRes), fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                    )
                }
            }

            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> DashboardTab(viewModel)
                    1 -> CpuTab(viewModel)
                    2 -> RamTab(viewModel)
                    3 -> BatteryTab(viewModel)
                    4 -> NetworkTab(viewModel)
                    5 -> DisplayTab(viewModel)
                    6 -> CameraTab()
                    7 -> SensorsTab(viewModel)
                    8 -> AppsTab()
                    9 -> TestsTab(viewModel)
                    10 -> SecurityTab()
                    11 -> SettingsTab(appPreferences, onLanguageChanged)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(viewModel: DeviceViewModel) {
    val totalRam by viewModel.totalRam.collectAsStateWithLifecycle()
    val availRam by viewModel.availRam.collectAsStateWithLifecycle()
    val batteryPct by viewModel.batteryPct.collectAsStateWithLifecycle()
    val totalStorage by viewModel.totalStorage.collectAsStateWithLifecycle()
    val freeStorage by viewModel.freeStorage.collectAsStateWithLifecycle()
    
    val extTotal by viewModel.externalStorageTotal.collectAsStateWithLifecycle()
    val extFree by viewModel.externalStorageFree.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val ramPct = if (totalRam > 0) ((totalRam - availRam).toFloat() / totalRam.toFloat()) else 0f
            CircularStatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.quick_ram), progress = ramPct, color = Color(0xFFFFA726))
            
            val storagePct = if (totalStorage > 0) ((totalStorage - freeStorage).toFloat() / totalStorage.toFloat()) else 0f
            CircularStatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.quick_internal_storage), progress = storagePct, color = Color(0xFF42A5F5))
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val extPct = if (extTotal > 0) ((extTotal - extFree).toFloat() / extTotal.toFloat()) else 0f
            if (extTotal > 0) {
                CircularStatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.quick_external_storage), progress = extPct, color = Color(0xFF26C6DA))
                CircularStatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.quick_battery), progress = batteryPct / 100f, color = Color(0xFF66BB6A))
            } else {
                CircularStatCard(modifier = Modifier.weight(1f), title = stringResource(R.string.quick_battery), progress = batteryPct / 100f, color = Color(0xFF66BB6A))
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CircularStatCard(modifier: Modifier = Modifier, title: String, progress: Float, color: Color) {
    Card(
        modifier = modifier.aspectRatio(1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = color, strokeWidth = 8.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun CpuTab(viewModel: DeviceViewModel) {
    val usage by viewModel.cpuUsage.collectAsStateWithLifecycle()
    val thermal by viewModel.cpuThermal.collectAsStateWithLifecycle()
    val cores by viewModel.cpuPerCore.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_cpu).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                InfoRow(stringResource(R.string.cpu_architecture), viewModel.cpuArch)
                InfoRow(stringResource(R.string.cpu_board), viewModel.cpuBoard)
                InfoRow(stringResource(R.string.cpu_cores), usage)
                InfoRow(stringResource(R.string.cpu_thermal), thermal)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.cpu_per_core).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp))
                if (cores.isEmpty()) {
                    Text("N/A", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
                } else {
                    cores.forEach { core ->
                        val parts = core.split(":")
                        if (parts.size == 2) {
                            InfoRow(parts[0].trim(), parts[1].trim())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RamTab(viewModel: DeviceViewModel) {
    val total by viewModel.totalRam.collectAsStateWithLifecycle()
    val avail by viewModel.availRam.collectAsStateWithLifecycle()
    val tStorage by viewModel.totalStorage.collectAsStateWithLifecycle()
    val fStorage by viewModel.freeStorage.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.tab_ram), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp), color = MaterialTheme.colorScheme.primary)
                InfoRow(stringResource(R.string.ram_total), formatBytes(total))
                InfoRow(stringResource(R.string.ram_available), formatBytes(avail))
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.quick_internal_storage), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp), color = MaterialTheme.colorScheme.primary)
                InfoRow(stringResource(R.string.storage_total), formatBytes(tStorage))
                InfoRow(stringResource(R.string.storage_free), formatBytes(fStorage))
                InfoRow(stringResource(R.string.storage_used), formatBytes(tStorage - fStorage))
            }
        }
    }
}

@Composable
fun BatteryTab(viewModel: DeviceViewModel) {
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.battery_level), "${level.toInt()}%")
                InfoRow(stringResource(R.string.battery_status), status)
                InfoRow(stringResource(R.string.battery_current), current)
                InfoRow(stringResource(R.string.battery_health), health)
                InfoRow(stringResource(R.string.battery_temp), "${temp} °C")
                InfoRow(stringResource(R.string.battery_voltage), "${volt} mV")
                InfoRow(stringResource(R.string.battery_tech), tech)
                InfoRow(stringResource(R.string.battery_est_time), estTime)
                InfoRow(stringResource(R.string.battery_cycle), cycleCount)
            }
        }
    }
}

@Composable
fun NetworkTab(viewModel: DeviceViewModel) {
    val ssid by viewModel.wifiSsid.collectAsStateWithLifecycle()
    val speed by viewModel.linkSpeed.collectAsStateWithLifecycle()
    val type by viewModel.networkType.collectAsStateWithLifecycle()
    val ip by viewModel.ipAddress.collectAsStateWithLifecycle()
    val dns by viewModel.dnsServers.collectAsStateWithLifecycle()
    val gateway by viewModel.defaultGateway.collectAsStateWithLifecycle()
    val interfaces by viewModel.networkInterfaces.collectAsStateWithLifecycle()
    val ping by viewModel.pingResult.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.net_type), type)
                InfoRow(stringResource(R.string.net_wifi), ssid)
                InfoRow(stringResource(R.string.net_speed), speed)
                InfoRow(stringResource(R.string.net_ip), ip)
                InfoRow(stringResource(R.string.net_gateway), gateway)
                InfoRow(stringResource(R.string.net_dns), dns)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.net_interfaces).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                InfoRow("Interfaces", interfaces)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.net_ping).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                Text("Result: $ping", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = { viewModel.performPingTest() }) {
                    Text("Start Ping")
                }
            }
        }
    }
}

@Composable
fun DisplayTab(viewModel: DeviceViewModel) {
    val size by viewModel.screenSize.collectAsStateWithLifecycle()
    val res by viewModel.screenRes.collectAsStateWithLifecycle()
    val dpi by viewModel.screenDpi.collectAsStateWithLifecycle()
    val fps by viewModel.refreshRate.collectAsStateWithLifecycle()
    val dimensions by viewModel.physicalDimensions.collectAsStateWithLifecycle()
    val hdr by viewModel.hdrCapabilities.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.disp_size), size)
                InfoRow(stringResource(R.string.disp_res), res)
                InfoRow(stringResource(R.string.disp_physical), dimensions)
                InfoRow(stringResource(R.string.disp_dpi), dpi)
                InfoRow(stringResource(R.string.disp_refresh), fps)
                InfoRow(stringResource(R.string.disp_hdr), hdr)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(stringResource(R.string.os_version), viewModel.osVersion)
                InfoRow(stringResource(R.string.os_api), viewModel.apiLevel)
                InfoRow(stringResource(R.string.os_build), viewModel.buildId)
            }
        }
    }
}

@Composable
fun SensorsTab(viewModel: DeviceViewModel) {
    val sensors by viewModel.sensorsFlow.collectAsStateWithLifecycle()
    val azimuth by viewModel.compassAzimuth.collectAsStateWithLifecycle()
    val pitch by viewModel.bubblePitch.collectAsStateWithLifecycle()
    val roll by viewModel.bubbleRoll.collectAsStateWithLifecycle()
    
    val sensorList = sensors.entries.toList()
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Compass Card
                Card(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.sensor_compass), fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = androidx.compose.ui.graphics.Color.Gray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                rotate(-azimuth) {
                                    drawLine(
                                        color = androidx.compose.ui.graphics.Color.Red,
                                        start = center,
                                        end = androidx.compose.ui.geometry.Offset(center.x, center.y - 40.dp.toPx()),
                                        strokeWidth = 8f
                                    )
                                }
                            }
                        }
                        Text("${azimuth.toInt()}°", modifier = Modifier.padding(top = 8.dp))
                    }
                }
                
                // Bubble Level Card
                Card(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                        Text(stringResource(R.string.sensor_bubble), fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val radius = 35.dp.toPx()
                                drawCircle(color = androidx.compose.ui.graphics.Color.LightGray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
                                // Assuming 9.8 is max. Normalize to radius.
                                val xOffset = (roll / 9.8f) * radius
                                val yOffset = (pitch / 9.8f) * radius
                                drawCircle(
                                    color = androidx.compose.ui.graphics.Color.Blue,
                                    radius = 10.dp.toPx(),
                                    center = androidx.compose.ui.geometry.Offset(center.x + xOffset, center.y + yOffset)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        items(sensorList.size) { index ->
            val sensor = sensorList[index]
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(sensor.key, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(sensor.value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsTab(appPreferences: AppPreferences, onLanguageChanged: (String) -> Unit) {
    val theme by appPreferences.themeMode.collectAsStateWithLifecycle()
    val lang by appPreferences.language.collectAsStateWithLifecycle()

    Column(modifier = Modifier.padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.set_lang).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                Row {
                    RadioButton(selected = lang == "en", onClick = { onLanguageChanged("en") })
                    Text("English", modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = lang == "ar", onClick = { onLanguageChanged("ar") })
                    Text("العربية", modifier = Modifier.align(Alignment.CenterVertically))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.set_theme).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == "light", onClick = { appPreferences.setThemePreference("light") })
                        Text(stringResource(R.string.light_theme))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == "dark", onClick = { appPreferences.setThemePreference("dark") })
                        Text(stringResource(R.string.dark_theme))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = theme == "system", onClick = { appPreferences.setThemePreference("system") })
                        Text(stringResource(R.string.system_theme))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(R.string.contact_us), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp, start = 8.dp), color = MaterialTheme.colorScheme.primary)
        
        val context = LocalContext.current
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f).clickable {
                    try {
                        val uri = android.net.Uri.parse("https://www.facebook.com/ahmedgad1993")
                        var intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        // Try to open with FB app explicitly if installed, fallback to browser
                        try {
                            context.packageManager.getPackageInfo("com.facebook.katana", 0)
                            intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/ahmedgad1993"))
                        } catch (e: Exception) {
                            // FB app not found
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.app_not_installed), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = "Facebook", tint = Color(0xFF1877F2), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Facebook", fontWeight = FontWeight.Bold)
                }
            }
            
            Card(
                modifier = Modifier.weight(1f).clickable {
                    try {
                        val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=+201013749174")
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, context.getString(R.string.app_not_installed), android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Phone, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("WhatsApp", fontWeight = FontWeight.Bold)
                }
            }
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
fun InfoRow(title: String, value: String) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
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
