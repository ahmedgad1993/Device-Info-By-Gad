with open('app/src/main/java/com/deviceinfo/gad/SensorsViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'sm.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI)',
    'try { sm.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI) } catch(e: Exception) {}'
).replace(
    'sm.unregisterListener(sensorEventListener)',
    'try { sm.unregisterListener(sensorEventListener) } catch(e: Exception) {}'
)

with open('app/src/main/java/com/deviceinfo/gad/SensorsViewModel.kt', 'w') as f:
    f.write(content)
