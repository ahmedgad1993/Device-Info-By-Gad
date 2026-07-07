package com.deviceinfo.gad

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

data class NetworkInterfaceInfo(val name: String, val isUp: Boolean, val macAddress: String, val ipAddresses: List<String>, val mtu: Int, val isLoopback: Boolean)

class BatteryNetworkViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

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
    val networkInterfaceList = MutableStateFlow<List<NetworkInterfaceInfo>>(emptyList())
    val rxSpeed = MutableStateFlow(0f)
    val txSpeed = MutableStateFlow(0f)
    val rxHistory = MutableStateFlow<List<Float>>(List(60){0f})
    val txHistory = MutableStateFlow<List<Float>>(List(60){0f})

    val isMonitoringActive = MutableStateFlow(true)
    val isBatteryReady = MutableStateFlow(false)
    val isNetworkReady = MutableStateFlow(false)
    private val prefs = AppPreferences(application.applicationContext)
    private var batteryReceiver: android.content.BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var batteryRefCount = 0
    fun startBatteryMonitoring() {
        if (batteryRefCount++ == 0) registerBatteryReceiver()
    }
    fun stopBatteryMonitoring() {
        if (--batteryRefCount <= 0) {
            batteryRefCount = 0
            batteryReceiver?.let {
                try { context.unregisterReceiver(it) } catch(e: Exception) {}
            }
            batteryReceiver = null
        }
    }

    private val networkMonitor = RefCountedMonitor(viewModelScope, 
        onStart = {
            registerNetworkCallback()
            launch(Dispatchers.IO) { networkMonitorLoop() }
        },
        onStop = {
            networkCallback?.let { cm.unregisterNetworkCallback(it) }
            networkCallback = null
        }
    )

    fun startNetworkMonitoring() = networkMonitor.acquire()
    fun stopNetworkMonitoring() = networkMonitor.release()

    init {
        // Init block no longer unconditionally starts monitoring
    }

    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent?.let { handleBatteryIntent(it) }
            }
        }
        val stickyIntent = context.registerReceiver(batteryReceiver, filter)
        stickyIntent?.let { handleBatteryIntent(it) }
    }

    private fun handleBatteryIntent(it: Intent) {
        viewModelScope.launch(Dispatchers.IO) {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    batteryPct.value = level * 100 / scale.toFloat()
                    val temp = it.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                    batteryTemp.value = temp
                    batteryVolt.value = it.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                    batteryTech.value = it.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

                    val health = it.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
                    batteryHealth.value = when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                        else -> "Not Supported"
                    }

                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    batteryStatus.value = if (status == BatteryManager.BATTERY_STATUS_CHARGING) "Charging" else "Discharging/Full"
                    
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
                    if (currentNow != Int.MIN_VALUE && currentNow != 0) {
                        val milliAmps = if (Math.abs(currentNow) > 100000) Math.abs(currentNow) / 1000 else Math.abs(currentNow)
                        batteryCurrent.value = "$milliAmps mA"
                    } else {
                        batteryCurrent.value = "Not Supported"
                    }
                    
                    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    if (isCharging) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val timeRemaining = bm.computeChargeTimeRemaining()
                            batteryEstTime.value = if (timeRemaining > 0) {
                                val hours = timeRemaining / (1000 * 60 * 60)
                                val minutes = (timeRemaining / (1000 * 60)) % 60
                                "Full in ${hours}h ${minutes}m"
                            } else "Calculating..."
                        } else {
                            batteryEstTime.value = "Charging"
                        }
                    } else {
                        try {
                            val chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                            if (chargeCounter > 0 && currentNow != Int.MIN_VALUE && currentNow != 0) {
                                val currentMicroAmps = Math.abs(currentNow).toFloat()
                                val chargeRemainingMicroAh = chargeCounter.toFloat()
                                val hoursRemaining = chargeRemainingMicroAh / currentMicroAmps
                                val totalMinutes = (hoursRemaining * 60).toInt()
                                val hours = totalMinutes / 60
                                val minutes = totalMinutes % 60
                                batteryEstTime.value = if (hours > 0) "${hours}h ${minutes}m remaining" else "${minutes}m remaining"
                            } else {
                                val pct = level * 100 / scale.toFloat()
                                batteryEstTime.value = "~${(pct * 0.5f).toInt()}h (estimated)"
                            }
                        } catch (e: Exception) {
                            batteryEstTime.value = "Not Available"
                        }
                    }
                    
                    var cycleFound = false
                    if (Build.VERSION.SDK_INT >= 34) {
                        val cc = it.getIntExtra("android.os.extra.CYCLE_COUNT", -1)
                        if (cc > 0) {
                            batteryCycleCount.value = cc.toString()
                            cycleFound = true
                        }
                    }
                    if (!cycleFound) {
                        val cyclePaths = listOf(
                            "/sys/class/power_supply/battery/cycle_count",
                            "/sys/class/power_supply/bms/cycle_count",
                            "/sys/class/power_supply/Battery/cycle_count",
                            "/sys/class/power_supply/battery/charge_cycle_count"
                        )
                        for (path in cyclePaths) {
                            try {
                                val value = java.io.RandomAccessFile(path, "r").use { f -> f.readLine() }
                                val count = value?.trim()?.toIntOrNull()
                                if (count != null && count > 0) {
                                    batteryCycleCount.value = count.toString()
                                    cycleFound = true
                                    break
                                }
                            } catch (e: Exception) { continue }
                        }
                    }
                    if (!cycleFound) batteryCycleCount.value = "Not Supported"
                    
                    if (!isBatteryReady.value) isBatteryReady.value = true
        }
    }

    private suspend fun networkMonitorLoop() {
        var lastRx = android.net.TrafficStats.getTotalRxBytes()
        var lastTx = android.net.TrafficStats.getTotalTxBytes()
        var lastTime = System.currentTimeMillis()
        
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!isMonitoringActive.value) {
                delay(1000)
                if (!kotlinx.coroutines.currentCoroutineContext().isActive) return
                lastTime = System.currentTimeMillis()
                lastRx = android.net.TrafficStats.getTotalRxBytes()
                lastTx = android.net.TrafficStats.getTotalTxBytes()
                continue
            }
            val nowTime = System.currentTimeMillis()
            val nowRx = android.net.TrafficStats.getTotalRxBytes()
            val nowTx = android.net.TrafficStats.getTotalTxBytes()
            
            val timeDiff = (nowTime - lastTime) / 1000f
            if (timeDiff > 0) {
                val rSpeed = (nowRx - lastRx) / timeDiff
                val tSpeed = (nowTx - lastTx) / timeDiff
                rxSpeed.value = rSpeed
                txSpeed.value = tSpeed
                
                val rHist = ArrayDeque(rxHistory.value)
                if (rHist.size >= 60) rHist.removeFirst()
                rHist.addLast(rSpeed)
                rxHistory.value = rHist.toList()
                
                val tHist = ArrayDeque(txHistory.value)
                if (tHist.size >= 60) tHist.removeFirst()
                tHist.addLast(tSpeed)
                txHistory.value = tHist.toList()
            }
            
            lastRx = nowRx
            lastTx = nowTx
            lastTime = nowTime
            
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            val linkProperties = cm.getLinkProperties(network)
            
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        networkType.value = "Wi-Fi"
                        try {
                            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                            @Suppress("DEPRECATION")
                            val info = wifiManager.connectionInfo
                            @Suppress("DEPRECATION")
                            wifiSsid.value = info.ssid ?: "Unknown"
                            @Suppress("DEPRECATION")
                            linkSpeed.value = "${info.linkSpeed} Mbps"
                        } catch (e: Exception) {
                            wifiSsid.value = "Unknown"
                            linkSpeed.value = "Unknown"
                        }
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        networkType.value = "Cellular"
                        wifiSsid.value = "N/A"
                        val downMbps = capabilities.linkDownstreamBandwidthKbps / 1000f
                        linkSpeed.value = if (downMbps > 0) String.format(Locale.US, "%.1f Mbps", downMbps) else "N/A"
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
            
            if (linkProperties != null) {
                val dns = linkProperties.dnsServers.joinToString(", ") { it.hostAddress ?: "" }
                dnsServers.value = dns.takeIf { it.isNotBlank() } ?: "None"
                
                val gateway = linkProperties.routes.firstOrNull { it.isDefaultRoute }?.gateway
                defaultGateway.value = gateway?.hostAddress ?: "None"
            } else {
                dnsServers.value = "None"
                defaultGateway.value = "None"
            }
            
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                var found = false
                val ifacesList = mutableListOf<NetworkInterfaceInfo>()
                while (interfaces.hasMoreElements()) {
                    val networkInterface = interfaces.nextElement()
                    val ips = networkInterface.inetAddresses.toList()
                        .filter { !it.isLoopbackAddress && it is Inet4Address }
                        .map { it.hostAddress ?: "" }
                        .filter { it.isNotBlank() }
                    val mac = try {
                        networkInterface.hardwareAddress?.joinToString(":") { "%02X".format(it) } ?: "N/A"
                    } catch (e: Exception) { "N/A" }
                    ifacesList.add(NetworkInterfaceInfo(
                        name = networkInterface.name,
                        ipAddresses = ips,
                        macAddress = mac,
                        isUp = networkInterface.isUp,
                        isLoopback = networkInterface.isLoopback,
                        mtu = networkInterface.mtu
                    ))
                    
                    if (!found && networkInterface.isUp && ips.isNotEmpty()) {
                        ipAddress.value = ips.first()
                        found = true
                    }
                }
                networkInterfaceList.value = ifacesList
            } catch (e: Exception) {
                ipAddress.value = "Unknown"
                networkInterfaceList.value = emptyList()
            }

            if (!isNetworkReady.value) isNetworkReady.value = true
            delay(prefs.refreshInterval.value.toLong().coerceIn(250L, 5000L))
        }
    }

    fun performPingTest(target: String = "google.com") {
        viewModelScope.launch(Dispatchers.IO) {
            val sanitizedTarget = target.trim()
            if (!sanitizedTarget.matches(Regex("^[a-zA-Z0-9.-]+$"))) {
                pingResult.value = "Invalid hostname"
                return@launch
            }
            
            pingResult.value = "Pinging $sanitizedTarget..."
            try {
                val process = ProcessBuilder("ping", "-c", "4", "-W", "3", sanitizedTarget).start()
                val exitVal = process.waitFor()
                if (exitVal == 0) {
                    val output = process.inputStream.bufferedReader().readText()
                    val avgLine = output.lines().find { it.contains("avg") || it.contains("rtt") }
                    val avg = avgLine?.split("/")?.getOrNull(4)?.trim()
                    pingResult.value = if (avg != null) "${avg} ms (${target})" else "Reachable"
                } else {
                    pingResult.value = "Host unreachable: $target"
                }
            } catch (e: Exception) {
                pingResult.value = "Error: ${e.message?.take(30)}"
            }
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        
        networkCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("NewApi")
            object : ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                    val ssid = wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Unknown"
                    wifiSsid.value = ssid
                    linkSpeed.value = "${wifiInfo?.linkSpeed ?: 0} Mbps"
                }
                override fun onLost(network: Network) {
                    wifiSsid.value = "Disconnected"
                    linkSpeed.value = "N/A"
                }
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val wifiInfo = networkCapabilities.transportInfo as? WifiInfo
                        val ssid = wifiInfo?.ssid?.removePrefix("\"")?.removeSuffix("\"")?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: "Unknown"
                        wifiSsid.value = ssid
                        linkSpeed.value = "${wifiInfo?.linkSpeed ?: 0} Mbps"
                    }
                }
                override fun onLost(network: Network) {
                    wifiSsid.value = "Disconnected"
                    linkSpeed.value = "N/A"
                }
            }
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                cm.registerNetworkCallback(request, networkCallback!!)
            }
        } catch (e: Exception) {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val info = wm.connectionInfo
            @Suppress("DEPRECATION")
            val ssid = info.ssid?.removePrefix("\"")?.removeSuffix("\"")
                ?.takeIf { it != "<unknown ssid>" } ?: "Unknown"
            wifiSsid.value = ssid
        }
    }

    override fun onCleared() {
        super.onCleared()
        batteryReceiver?.let { context.unregisterReceiver(it) }
        networkCallback?.let { cm.unregisterNetworkCallback(it) }
    }
}
