package com.deviceinfo.gad

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SensorsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val compassAzimuth = MutableStateFlow(0f)
    val bubblePitch = MutableStateFlow(0f)
    val bubbleRoll = MutableStateFlow(0f)
    
    val accelerometerData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val lightData = MutableStateFlow(0f)

    private val _sensorsFlow = MutableStateFlow<Map<String, String>>(emptyMap())
    val sensorsFlow: StateFlow<Map<String, String>> = _sensorsFlow

    private val _lockedSensorsMap = ConcurrentHashMap<String, String>()

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    gravity = it.values.clone()
                    bubblePitch.value = it.values[1] 
                    bubbleRoll.value = it.values[0]  
                    accelerometerData.value = it.values.clone()
                }
                if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = it.values.clone()
                }
                if (it.sensor.type == Sensor.TYPE_GYROSCOPE) {
                    gyroscopeData.value = it.values.clone()
                }
                if (it.sensor.type == Sensor.TYPE_LIGHT) {
                    lightData.value = it.values[0]
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

                val valuesStr = it.values.joinToString(", ") { v -> String.format(Locale.US, "%.2f", v) }
                _lockedSensorsMap[it.sensor.name] = valuesStr
                _sensorsFlow.value = _lockedSensorsMap.toMap()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startMonitoring(sensors: List<Sensor>) {
        sensors.forEach { sensor ->
            try { sm.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI) } catch(e: Exception) {}
        }
    }

    fun stopMonitoring() {
        try { sm.unregisterListener(sensorEventListener) } catch(e: Exception) {}
        _lockedSensorsMap.clear()
        _sensorsFlow.value = emptyMap()
    }
}
