package com.deviceinfo.gad

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class CpuHardwareViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // CPU Stream
    val cpuUsage = MutableStateFlow("0%")
    val cpuUsageFloat = MutableStateFlow(0f)
    val cpuPerCoreUsage = MutableStateFlow<List<Float>>(List(Runtime.getRuntime().availableProcessors()) { 0f })
    val cpuThermal = MutableStateFlow("N/A")
    val cpuFreq = MutableStateFlow("N/A")
    val cpuArch = Build.SUPPORTED_ABIS.joinToString(", ")
    val cpuBoard = Build.BOARD
    
    val cpuGovernor = MutableStateFlow("Unknown")
    val cpuFreqMinMax = MutableStateFlow("...")
    val cpuThermalFloat = MutableStateFlow(0f)
    val isThermalThrottling = MutableStateFlow(false)
    val cpuLoadHistory = MutableStateFlow<List<Float>>(List(60){0f})
    val ramLoadHistory = MutableStateFlow<List<Float>>(List(60){0f})

    // RAM & Storage
    val totalRam = MutableStateFlow(0L)
    val availRam = MutableStateFlow(0L)
    val swapTotal = MutableStateFlow(0L)
    val swapFree = MutableStateFlow(0L)
    val totalStorage = MutableStateFlow(0L)
    val freeStorage = MutableStateFlow(0L)
    val externalStorageTotal = MutableStateFlow(0L)
    val externalStorageFree = MutableStateFlow(0L)

    // OS & Display
    val screenSize = MutableStateFlow("Not Supported")
    val screenRes = MutableStateFlow("Not Supported")
    val refreshRate = MutableStateFlow("Not Supported")
    val screenDpi = MutableStateFlow("Not Supported")
    val physicalDimensions = MutableStateFlow("Not Supported")
    val hdrCapabilities = MutableStateFlow("Not Supported")
    
    val osVersion = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT.toString()
    val buildId = Build.DISPLAY

    private val prevCoreTiks = ConcurrentHashMap<String, Pair<Long, Long>>()
    val isMonitoringActive = MutableStateFlow(true)
    val isCpuReady = MutableStateFlow(false)
    val isRamReady = MutableStateFlow(false)
    val isStorageReady = MutableStateFlow(false)

    private val prefs = AppPreferences(application.applicationContext)

    private val cpuMonitor = RefCountedMonitor(viewModelScope, onStart = { launch(Dispatchers.IO) { cpuMonitorLoop() } })
    private val ramMonitor = RefCountedMonitor(viewModelScope, onStart = { launch(Dispatchers.IO) { ramMonitorLoop() } })
    private val storageMonitor = RefCountedMonitor(viewModelScope, onStart = { launch(Dispatchers.IO) { storageMonitorLoop() } })

    fun startCpuMonitoring() = cpuMonitor.acquire()
    fun stopCpuMonitoring() = cpuMonitor.release()
    fun startRamMonitoring() = ramMonitor.acquire()
    fun stopRamMonitoring() = ramMonitor.release()
    fun startStorageMonitoring() = storageMonitor.acquire()
    fun stopStorageMonitoring() = storageMonitor.release()

    init {
        // Do the one-time /proc/stat warm-up read
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RandomAccessFile("/proc/stat", "r").use { reader ->
                    var line = reader.readLine()
                    while(line != null) {
                        if (line.startsWith("cpu")) {
                            val toks = line.split(" ".toRegex()).filter { it.isNotBlank() }
                            if (toks.size >= 5) {
                                val cpuId = toks[0]
                                val user = toks[1].toLong(); val nice = toks[2].toLong()
                                val system = toks[3].toLong(); val idle = toks[4].toLong()
                                val iowait = if (toks.size > 5) toks[5].toLong() else 0L
                                val irq = if (toks.size > 6) toks[6].toLong() else 0L
                                val softirq = if (toks.size > 7) toks[7].toLong() else 0L
                                val steal = if (toks.size > 8) toks[8].toLong() else 0L
                                val totalIdle = idle + iowait
                                val nonIdle = user + nice + system + irq + softirq + steal
                                val total = totalIdle + nonIdle
                                prevCoreTiks[cpuId] = Pair(total, totalIdle)
                            }
                        }
                        line = reader.readLine()
                    }
                }
            } catch(e: Exception) {
                isProcStatFailed = true
            }
        }
        fetchDisplayAndOsInfo()
    }

    private var isProcStatFailed = false

    private suspend fun ramMonitorLoop() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!isMonitoringActive.value) { kotlinx.coroutines.delay(1000); continue }
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            totalRam.value = mi.totalMem
            availRam.value = mi.availMem
            if (mi.totalMem > 0) {
                val ramPct = (mi.totalMem - mi.availMem).toFloat() / mi.totalMem.toFloat()
                val history = ArrayDeque(ramLoadHistory.value)
                if (history.size >= 60) history.removeFirst()
                history.addLast(ramPct)
                ramLoadHistory.value = history.toList()
            }
            try {
                RandomAccessFile("/proc/meminfo", "r").use { mReader ->
                    var mLine = mReader.readLine()
                    var sTotal = 0L
                    var sFree = 0L
                    while (mLine != null) {
                        if (mLine.startsWith("SwapTotal:")) {
                            val toks = mLine.split("\\s+".toRegex())
                            if (toks.size > 1) sTotal = (toks[1].toLongOrNull() ?: 0L) * 1024L
                        }
                        if (mLine.startsWith("SwapFree:")) {
                            val toks = mLine.split("\\s+".toRegex())
                            if (toks.size > 1) sFree = (toks[1].toLongOrNull() ?: 0L) * 1024L
                        }
                        mLine = mReader.readLine()
                    }
                    swapTotal.value = sTotal
                    swapFree.value = sFree
                }
            } catch(e: Exception){}
            if (!isRamReady.value) isRamReady.value = true
            kotlinx.coroutines.delay(prefs.refreshInterval.value.toLong().coerceIn(250L, 5000L))
        }
    }

    private suspend fun storageMonitorLoop() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!isMonitoringActive.value) { kotlinx.coroutines.delay(1000); continue }
            try {
                val statFs = StatFs(Environment.getDataDirectory().path)
                totalStorage.value = statFs.totalBytes
                freeStorage.value = statFs.availableBytes
            } catch (e: Exception) {}
            
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
            if (!isStorageReady.value) isStorageReady.value = true
            kotlinx.coroutines.delay(30_000)
        }
    }

    private suspend fun cpuMonitorLoop() {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            if (!isMonitoringActive.value) { kotlinx.coroutines.delay(1000); continue }
            try {
                val numCores = Runtime.getRuntime().availableProcessors()
                val coresUsageList = mutableListOf<Float>()
                var totalCpuUsage = 0f
                var procStatSuccess = false

                try {
                    val isFirstTick = prevCoreTiks.isEmpty()
                    RandomAccessFile("/proc/stat", "r").use { reader ->
                        var line = reader.readLine()
                        while(line != null) {
                            if (line.startsWith("cpu")) {
                                val toks = line.split(" ".toRegex()).filter { it.isNotBlank() }
                                if (toks.size >= 5) {
                                    val cpuId = toks[0]
                                    val user = toks[1].toLong()
                                    val nice = toks[2].toLong()
                                    val system = toks[3].toLong()
                                    val idle = toks[4].toLong()
                                    val iowait = if (toks.size > 5) toks[5].toLong() else 0L
                                    val irq = if (toks.size > 6) toks[6].toLong() else 0L
                                    val softirq = if (toks.size > 7) toks[7].toLong() else 0L
                                    val steal = if (toks.size > 8) toks[8].toLong() else 0L
                                    
                                    val totalIdle = idle + iowait
                                    val nonIdle = user + nice + system + irq + softirq + steal
                                    val total = totalIdle + nonIdle
                                    
                                    val prev = prevCoreTiks[cpuId] ?: Pair(total, totalIdle)
                                    val prevTotal = prev.first
                                    val prevIdle = prev.second
                                    
                                    val diffTotal = total - prevTotal
                                    val diffIdle = totalIdle - prevIdle
                                    
                                    val usage = if (diffTotal > 0) {
                                        (diffTotal - diffIdle).toFloat() / diffTotal.toFloat()
                                    } else 0f
                                    
                                    if (cpuId == "cpu") {
                                        totalCpuUsage = usage
                                        procStatSuccess = true
                                    } else {
                                        coresUsageList.add(usage)
                                    }
                                    
                                    prevCoreTiks[cpuId] = Pair(total, totalIdle)
                                }
                            }
                            line = reader.readLine()
                        }
                    }
                    
                    if (procStatSuccess && !isFirstTick) {
                        cpuPerCoreUsage.value = coresUsageList
                        cpuUsageFloat.value = totalCpuUsage
                        cpuUsage.value = String.format(Locale.US, "%.0f%%", totalCpuUsage * 100f)
                        
                        val history = ArrayDeque(cpuLoadHistory.value)
                        if (history.size >= 60) history.removeFirst()
                        history.addLast(totalCpuUsage)
                        cpuLoadHistory.value = history.toList()
                    }
                } catch (e: Exception) {
                    procStatSuccess = false
                }

                if (!procStatSuccess) {
                    var freqSum = 0f
                    var maxFreqSum = 0f
                    var allFailed = true
                    var readFailures = 0
                    coresUsageList.clear()
                    for (i in 0 until numCores) {
                        try {
                            val f = RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq", "r").use { it.readLine() }.toLongOrNull()
                            val m = RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq", "r").use { it.readLine() }.toLongOrNull()
                            
                            if (f != null && m != null && m > 0) {
                                val used = (f.toFloat() / m.toFloat()).coerceIn(0f, 1f)
                                coresUsageList.add(used)
                                allFailed = false
                                freqSum += f
                                maxFreqSum += m
                            } else {
                                coresUsageList.add(-1f)
                                readFailures++
                            }
                        } catch(e: Exception) {
                            coresUsageList.add(-1f)
                            readFailures++
                        }
                    }
                    
                    if (readFailures == numCores) {
                        totalCpuUsage = -1f
                    } else {
                        totalCpuUsage = if (maxFreqSum > 0) freqSum / maxFreqSum else -1f
                    }
                    
                    // Fallback to app's own CPU time if global fails
                    if (totalCpuUsage < 0) {
                        try {
                            val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
                            val procCpuTime = android.os.Process.getElapsedCpuTime()
                            val diffReal = elapsedRealtime - (prevCoreTiks["app_real"]?.first ?: elapsedRealtime)
                            val diffCpu = procCpuTime - (prevCoreTiks["app_cpu"]?.first ?: procCpuTime)
                            
                            if (diffReal > 0 && prevCoreTiks.containsKey("app_real")) {
                                totalCpuUsage = (diffCpu.toFloat() / diffReal.toFloat()).coerceIn(0f, 1f)
                            } else if (prevCoreTiks.isEmpty()) {
                                totalCpuUsage = 0f // Initialize on first tick
                            }
                            
                            prevCoreTiks["app_real"] = Pair(elapsedRealtime, 0L)
                            prevCoreTiks["app_cpu"] = Pair(procCpuTime, 0L)
                        } catch (e: Exception) {
                            totalCpuUsage = -1f
                        }
                    }
                    
                    val listToUse = if (allFailed) emptyList() else coresUsageList.filter { it >= 0f }
                    cpuPerCoreUsage.value = listToUse
                    cpuUsageFloat.value = totalCpuUsage
                    cpuUsage.value = if (totalCpuUsage >= 0) String.format(Locale.US, "%.0f%%", totalCpuUsage * 100f) else "Unavailable"
                    
                    val history = ArrayDeque(cpuLoadHistory.value)
                    if (history.size >= 60) history.removeFirst()
                    history.addLast(if (totalCpuUsage >= 0) totalCpuUsage else 0f)
                    cpuLoadHistory.value = history.toList()
                }
            } catch(e: Exception){
            }
            
            try {
                val gov = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "r").use { it.readLine() }
                cpuGovernor.value = gov ?: "Unknown"
            } catch(e: Exception) {
                cpuGovernor.value = "Unknown"
            }
            
            try {
                val maxFreq = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq", "r").use { it.readLine() }.toLongOrNull() ?: 0L
                val minFreq = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq", "r").use { it.readLine() }.toLongOrNull() ?: 0L
                if (maxFreq > 0) {
                    cpuFreqMinMax.value = "${minFreq / 1000} MHz - ${maxFreq / 1000} MHz"
                } else {
                    cpuFreqMinMax.value = "Not Supported"
                }
            } catch(e: Exception) {
                cpuFreqMinMax.value = "Not Supported"
            }
            
            try {
                val curFreq = RandomAccessFile("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r").use { it.readLine() }.toLongOrNull() ?: 0L
                if (curFreq > 0) {
                    cpuFreq.value = "${curFreq / 1000} MHz"
                } else {
                    cpuFreq.value = "Not Supported"
                }
            } catch(e: Exception) {
                cpuFreq.value = "Not Supported"
            }
            
            try {
                val (tempFloat, tempStr) = readCpuTemperature()
                cpuThermalFloat.value = tempFloat
                cpuThermal.value = tempStr
                isThermalThrottling.value = tempFloat >= 75f
            } catch(e: Exception){
                cpuThermalFloat.value = 0f
                cpuThermal.value = "Not Supported"
            }
            
            if (!isCpuReady.value) isCpuReady.value = true
            kotlinx.coroutines.delay(prefs.refreshInterval.value.toLong().coerceIn(250L, 5000L))
        }
    }
    
    private fun readCpuTemperature(): Pair<Float, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val headroom = powerManager.getThermalHeadroom(1)
                if (headroom.isFinite() && headroom > 0f) {
                    val approxTemp = 35f + (headroom * 55f)
                    return Pair(approxTemp, String.format(Locale.US, "~%.0f °C", approxTemp))
                }
            } catch (e: Exception) { }
        }
        
        val thermalPaths = buildList {
            for (i in 0..15) add("/sys/class/thermal/thermal_zone$i/temp")
            add("/sys/devices/virtual/thermal/thermal_zone5/temp")
            add("/sys/devices/virtual/thermal/thermal_zone7/temp")
            add("/sys/devices/platform/soc/soc:qcom,msm-thermal/qcom,msm-thermal/temp")
        }
        val cpuKeywords = listOf("cpu", "tsens", "core", "cluster", "big", "little", "ap", "soc")
        
        val candidates = mutableListOf<Float>()
        
        for (path in thermalPaths) {
            try {
                val typeFile = path.replace("/temp", "/type")
                val zoneType = try {
                    RandomAccessFile(typeFile, "r").use { it.readLine()?.lowercase() ?: "" }
                } catch (e: Exception) { "" }
                
                val tempStr = RandomAccessFile(path, "r").use { it.readLine() } ?: continue
                val tempLong = tempStr.trim().toLongOrNull() ?: continue
                if (tempLong <= 0) continue
                
                val tempC = if (tempLong > 1000L) tempLong / 1000.0 else tempLong.toDouble()
                if (tempC < 20.0 || tempC > 100.0) continue
                
                if (cpuKeywords.any { zoneType.contains(it) }) {
                    return Pair(tempC.toFloat(), String.format(Locale.US, "%.1f °C", tempC))
                }
                candidates.add(tempC.toFloat())
            } catch (e: Exception) { continue }
        }
        
        if (candidates.isNotEmpty()) {
            val best = candidates.max()
            return Pair(best, String.format(Locale.US, "%.1f °C", best))
        }
        
        return Pair(0f, "Not Supported")
    }

    private fun fetchDisplayAndOsInfo() {
        val metrics = context.resources.displayMetrics

        screenRes.value = "${metrics.widthPixels} x ${metrics.heightPixels}"
        screenDpi.value = "${metrics.densityDpi} dpi"
        
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        screenSize.value = String.format(Locale.US, "%.1f inches", diagonalInches)
        
        physicalDimensions.value = String.format(Locale.US, "%.1f\" x %.1f\"", widthInches, heightInches)
        
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            val rawHz = windowManager.defaultDisplay.refreshRate
            val roundedHz = Math.round(rawHz)
            refreshRate.value = "$roundedHz Hz"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val modes = context.display?.supportedModes ?: emptyArray()
                if (modes.size > 1) {
                    val rates = modes.map { Math.round(it.refreshRate) }.distinct().sorted()
                    refreshRate.value = rates.joinToString(" / ") { "$it Hz" }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                val hdrCaps = windowManager.defaultDisplay.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)
                val hdrStrings = hdrCaps.map { 
                    when(it) {
                        android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                        android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                        android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                        2 -> "HDR10" 
                        3 -> "HLG"
                        4 -> "HDR10_PLUS"
                        else -> "Unknown ($it)"
                    }
                }
                hdrCapabilities.value = if(hdrStrings.isEmpty()) "Not Supported" else hdrStrings.joinToString(", ")
            } else {
                hdrCapabilities.value = "Not Supported"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (refreshRate.value.isEmpty()) refreshRate.value = "60 Hz (Est)"
            if (hdrCapabilities.value.isEmpty()) hdrCapabilities.value = "Unknown"
        }
    }
}
