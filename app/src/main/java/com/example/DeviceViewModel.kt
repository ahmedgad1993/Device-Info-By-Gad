package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.RandomAccessFile
import java.util.Locale
import java.net.Inet4Address
import java.net.NetworkInterface

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // CPU Stream
    val cpuUsage = MutableStateFlow("0%")
    val cpuUsageFloat = MutableStateFlow(0f)
    val cpuPerCore = MutableStateFlow<List<String>>(emptyList())
    val cpuThermal = MutableStateFlow("N/A")
    val cpuFreq = MutableStateFlow("N/A")
    val cpuArch = Build.SUPPORTED_ABIS.joinToString(", ")
    val cpuBoard = Build.BOARD

    // RAM & Storage
    val totalRam = MutableStateFlow(0L)
    val availRam = MutableStateFlow(0L)
    val totalStorage = MutableStateFlow(0L)
    val freeStorage = MutableStateFlow(0L)

    val externalStorageTotal = MutableStateFlow(0L)
    val externalStorageFree = MutableStateFlow(0L)

    // Battery
    val batteryPct = MutableStateFlow(0f)
    val batteryHealth = MutableStateFlow("")
    val batteryTech = MutableStateFlow("")
    val batteryTemp = MutableStateFlow(0f)
    val batteryVolt = MutableStateFlow(0)
    val batteryStatus = MutableStateFlow("")
    val batteryCurrent = MutableStateFlow("0 mA")
    val batteryEstTime = MutableStateFlow("Unknown")
    val batteryCycleCount = MutableStateFlow("Unknown")

    // Network
    val wifiSsid = MutableStateFlow("Disconnected")
    val linkSpeed = MutableStateFlow("0 Mbps")
    val networkType = MutableStateFlow("None")
    val ipAddress = MutableStateFlow("0.0.0.0")
    val pingResult = MutableStateFlow("-")
    val dnsServers = MutableStateFlow("Not Supported")
    val defaultGateway = MutableStateFlow("Not Supported")
    val networkInterfaces = MutableStateFlow("Not Supported")

    // Display
    val screenSize = MutableStateFlow("Not Supported")
    val screenRes = MutableStateFlow("Not Supported")
    val refreshRate = MutableStateFlow("Not Supported")
    val screenDpi = MutableStateFlow("Not Supported")
    val physicalDimensions = MutableStateFlow("Not Supported")
    val hdrCapabilities = MutableStateFlow("Not Supported")

    // Previous CPU stats for real-time calculation
    private var prevCpuTotal = 0L
    private var prevCpuIdle = 0L
    private val prevCoreTiks = mutableMapOf<String, Pair<Long, Long>>()

    // OS
    val osVersion = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT.toString()
    val buildId = Build.DISPLAY

    // Sensors
    val compassAzimuth = MutableStateFlow(0f)
    val bubblePitch = MutableStateFlow(0f)
    val bubbleRoll = MutableStateFlow(0f)
    
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                // Compass & Bubble level Tracking
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = it.values.clone()
                    // Extract pitch/roll for bubble level (approx)
                    bubblePitch.value = it.values[1] // Y-axis
                    bubbleRoll.value = it.values[0]  // X-axis
                }
                if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = it.values.clone()
                }
                
                if (gravity.isNotEmpty() && geomagnetic.isNotEmpty()) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        val azimutD = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        compassAzimuth.value = (azimutD + 360) % 360
                    }
                }

                val updatedSensors = _sensorsFlow.value.toMutableMap()
                val valuesStr = it.values.joinToString(", ") { v -> String.format(Locale.US, "%.2f", v) }
                updatedSensors[it.sensor.name] = valuesStr
                _sensorsFlow.value = updatedSensors
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    private val _sensorsFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val sensorsFlow: StateFlow<Map<String, String>> = _sensorsFlow

    init {
        startHardwareMonitoring()
        startNetworkMonitoring()
        fetchDisplayAndOsInfo()
        registerSensors()
    }

    private fun startHardwareMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                // RAM
                val mi = ActivityManager.MemoryInfo()
                am.getMemoryInfo(mi)
                totalRam.value = mi.totalMem
                availRam.value = mi.availMem

                // Storage
                val statFs = StatFs(Environment.getDataDirectory().path)
                totalStorage.value = statFs.totalBytes
                freeStorage.value = statFs.availableBytes
                
                try {
                    val externalDirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
                    if (externalDirs.size > 1 && externalDirs[1] != null && Environment.isExternalStorageRemovable(externalDirs[1])) {
                        val extStatFs = StatFs(externalDirs[1].path)
                        externalStorageTotal.value = extStatFs.totalBytes
                        externalStorageFree.value = extStatFs.availableBytes
                    } else {
                        externalStorageTotal.value = 0L
                        externalStorageFree.value = 0L
                    }
                } catch(e:Exception){
                    externalStorageTotal.value = 0L
                    externalStorageFree.value = 0L
                }

                // Battery
                val batteryStatusIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                batteryStatusIntent?.let { intent ->
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    batteryPct.value = level * 100 / scale.toFloat()
                    val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                    batteryTemp.value = temp
                    batteryVolt.value = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    batteryTech.value = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

                    val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
                    batteryHealth.value = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                        else -> "Not Supported"
                    }

                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    batteryStatus.value = if (status == BatteryManager.BATTERY_STATUS_CHARGING) "Charging" else "Discharging/Full"
                    
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    if (currentNow != Int.MIN_VALUE && currentNow != 0) {
                        val milliAmps = if (Math.abs(currentNow) > 100000) Math.abs(currentNow) / 1000 else Math.abs(currentNow)
                        batteryCurrent.value = "$milliAmps mA"
                    } else {
                        batteryCurrent.value = "Not Supported"
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val timeRemaining = bm.computeChargeTimeRemaining()
                        if (timeRemaining > 0) {
                            val hours = timeRemaining / (1000 * 60 * 60)
                            val minutes = (timeRemaining / (1000 * 60)) % 60
                            batteryEstTime.value = "${hours}h ${minutes}m"
                        } else {
                            batteryEstTime.value = "Not Supported"
                        }
                    } else {
                        batteryEstTime.value = "Not Supported"
                    }
                    
                    // Attempt to read cycle count
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val cc = intent.getIntExtra("android.os.extra.CYCLE_COUNT", -1)
                        batteryCycleCount.value = if (cc > 0) cc.toString() else "Not Supported"
                    } else {
                        batteryCycleCount.value = "Not Supported"
                    }

                }

                // CPU Cores & Thermal
                val coresList = mutableListOf<String>()
                try {
                    val reader = RandomAccessFile("/proc/stat", "r")
                    var line = reader.readLine()
                    while(line != null) {
                        if (line.startsWith("cpu") && line.substring(3).firstOrNull()?.isDigit() == true) {
                            val toks = line.split(" ".toRegex()).filter { it.isNotBlank() }
                            if (toks.size >= 5) {
                                val user = toks[1].toLong(); val nice = toks[2].toLong()
                                val system = toks[3].toLong(); val idle = toks[4].toLong()
                                val total = user + nice + system + idle
                                val active = total - idle
                                
                                val prev = prevCoreTiks[toks[0]]
                                val prevActive = prev?.first ?: 0L
                                val prevTotal = prev?.second ?: 0L
                                
                                val diffActive = active - prevActive
                                val diffTotal = total - prevTotal
                                
                                val pct = if (diffTotal > 0) (diffActive.toFloat() / diffTotal.toFloat()) * 100f else 0f
                                coresList.add(String.format(Locale.US, "${toks[0]}: %.1f%%", Math.min(100f, pct)))
                                prevCoreTiks[toks[0]] = Pair(active, total)
                            }
                        }
                        line = reader.readLine()
                    }
                    reader.close()
                    
                    // Main CPU avg
                    val readerMain = RandomAccessFile("/proc/stat", "r")
                    val loadMain = readerMain.readLine()
                    val toksMain = loadMain.split(" ".toRegex()).filter { it.isNotBlank() }.toTypedArray()
                    val mainIdle = toksMain[4].toLong()
                    val mainTotal = toksMain[1].toLong() + toksMain[2].toLong() + toksMain[3].toLong() + mainIdle
                    val mainActive = mainTotal - mainIdle
                    val diffMainActive = mainActive - prevCpuTotal
                    val diffMainTotal = mainTotal - prevCpuIdle
                    readerMain.close()
                    
                    if (diffMainTotal > 0) {
                        val res = (diffMainActive.toFloat() / diffMainTotal.toFloat())
                        cpuUsageFloat.value = Math.min(1f, res)
                        cpuUsage.value = String.format(Locale.US, "%.1f%%", Math.min(100f, res * 100f))
                    }
                    prevCpuTotal = mainActive
                    prevCpuIdle = mainTotal
                    
                } catch(e:Exception){}
                cpuPerCore.value = coresList
                
                try {
                    val tempReader = RandomAccessFile("/sys/class/thermal/thermal_zone0/temp", "r")
                    val tempStr = tempReader.readLine()
                    tempReader.close()
                    val tempInt = tempStr.toIntOrNull()
                    if (tempInt != null) {
                        cpuThermal.value = "${tempInt / 1000.0} °C"
                    } else {
                        cpuThermal.value = "Not Supported"
                    }
                } catch(e:Exception){
                    cpuThermal.value = "Not Supported"
                }

                delay(1000)
            }
        }
    }

    private fun readCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            val toks = load.split(" ".toRegex()).filter { it.isNotBlank() }.toTypedArray()
            val idle1 = toks[4].toLong()
            val cpu1 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong()
            reader.close()
            
            // Simple generic calculation without delay locking
            (cpu1 - idle1).toFloat() / cpu1
        } catch (ex: Exception) {
            0f
        }
    }

    private fun startNetworkMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                // Determine connection type
                val network = cm.activeNetwork
                val capabilities = cm.getNetworkCapabilities(network)
                val linkProperties = cm.getLinkProperties(network)
                
                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            networkType.value = "Wi-Fi"
                            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                            val info = wifiManager.connectionInfo
                            wifiSsid.value = info.ssid ?: "Unknown"
                            linkSpeed.value = "${info.linkSpeed} Mbps"
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            networkType.value = "Cellular"
                            wifiSsid.value = "N/A"
                            linkSpeed.value = "N/A"
                        }
                        else -> {
                            networkType.value = "Other"
                            wifiSsid.value = "N/A"
                            linkSpeed.value = "N/A"
                        }
                    }
                } else {
                    networkType.value = "Disconnected"
                    wifiSsid.value = "N/A"
                    linkSpeed.value = "N/A"
                }
                
                // Advanced Link Properties
                if (linkProperties != null) {
                    dnsServers.value = linkProperties.dnsServers.joinToString(", ") { it.hostAddress ?: "" }.takeIf { it.isNotBlank() } ?: "None"
                    
                    val gateway = linkProperties.routes.firstOrNull { it.isDefaultRoute }?.gateway
                    defaultGateway.value = gateway?.hostAddress ?: "None"
                } else {
                    dnsServers.value = "None"
                    defaultGateway.value = "None"
                }
                
                // IP Address & Interfaces
                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    var found = false
                    val interfaceNames = mutableListOf<String>()
                    while (interfaces.hasMoreElements()) {
                        val networkInterface = interfaces.nextElement()
                        if (networkInterface.isUp) {
                            interfaceNames.add(networkInterface.name)
                        }
                        
                        if (!found) {
                            val addresses = networkInterface.inetAddresses
                            while (addresses.hasMoreElements()) {
                                val address = addresses.nextElement()
                                if (!address.isLoopbackAddress && address is Inet4Address) {
                                    ipAddress.value = address.hostAddress ?: ""
                                    found = true
                                    break
                                }
                            }
                        }
                    }
                    networkInterfaces.value = interfaceNames.joinToString(", ").takeIf { it.isNotBlank() } ?: "None"
                } catch (e: Exception) {
                    ipAddress.value = "Unknown"
                    networkInterfaces.value = "Unknown"
                }

                delay(3000)
            }
        }
    }
    
    fun performPingTest() {
        viewModelScope.launch(Dispatchers.IO) {
            pingResult.value = "Pinging..."
            try {
                val process = Runtime.getRuntime().exec("ping -c 3 google.com")
                val exitVal = process.waitFor()
                if (exitVal == 0) {
                    val reader = process.inputStream.bufferedReader()
                    val output = reader.readText()
                    // Extract avg time if possible
                    val pingLine = output.split("\n").find { it.contains("avg") }
                    if (pingLine != null) {
                        val avg = pingLine.split("/")[4]
                        pingResult.value = "$avg ms"
                    } else {
                        pingResult.value = "Success"
                    }
                } else {
                    pingResult.value = "Failed"
                }
            } catch (e: Exception) {
                pingResult.value = "Error"
            }
        }
    }

    private fun fetchDisplayAndOsInfo() {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(metrics)

        screenRes.value = "${metrics.widthPixels} x ${metrics.heightPixels}"
        screenDpi.value = "${metrics.densityDpi} dpi"
        refreshRate.value = "${windowManager.defaultDisplay.refreshRate} Hz"
        
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        screenSize.value = String.format(Locale.US, "%.1f inches", diagonalInches)
        
        physicalDimensions.value = String.format(Locale.US, "%.1f\" x %.1f\"", widthInches, heightInches)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val hdrCaps = windowManager.defaultDisplay.hdrCapabilities.supportedHdrTypes
            val hdrStrings = hdrCaps.map { 
                when(it) {
                    android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                    android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                    android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                    2 -> "HDR10" // some hardcoded maps if constant not accessible
                    3 -> "HLG"
                    4 -> "HDR10_PLUS"
                    else -> "Unknown ($it)"
                }
            }
            hdrCapabilities.value = if(hdrStrings.isEmpty()) "Not Supported" else hdrStrings.joinToString(", ")
        } else {
            hdrCapabilities.value = "Not Supported"
        }
    }

    private fun registerSensors() {
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        sensors.forEach { sensor ->
            sm.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            val updated = _sensorsFlow.value.toMutableMap()
            updated[sensor.name] = "Waiting for data..."
            _sensorsFlow.value = updated
        }
    }

    override fun onCleared() {
        super.onCleared()
        sm.unregisterListener(sensorEventListener)
    }

    fun generateExportReport(): String {
        return buildString {
            appendLine("--- Device Info By Gad ---")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Board: $cpuBoard")
            appendLine("CPU Arch: $cpuArch")
            appendLine("OS Version: $osVersion (API $apiLevel)")
            appendLine("Build ID: $buildId")
            
            appendLine("\n--- Battery ---")
            appendLine("Level: ${batteryPct.value.toInt()}%")
            appendLine("Health: ${batteryHealth.value}")
            appendLine("Temperature: ${batteryTemp.value} C")
            
            appendLine("\n--- Network ---")
            appendLine("Type: ${networkType.value}")
            appendLine("IP Address: ${ipAddress.value}")
            appendLine("Link Speed: ${linkSpeed.value}")
            
            appendLine("\n--- Display ---")
            appendLine("Size: ${screenSize.value}")
            appendLine("Resolution: ${screenRes.value}")
            appendLine("Refresh Rate: ${refreshRate.value}")
        }
    }
}
