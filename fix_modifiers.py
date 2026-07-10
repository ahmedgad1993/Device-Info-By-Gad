with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'r') as f:
    text = f.read()

text = text.replace('.androidx.compose.ui.graphics.graphicsLayer', '.graphicsLayer')
text = text.replace('.androidx.compose.ui.input.pointer.pointerInput', '.pointerInput')
text = text.replace('androidx.compose.foundation.gestures.detectTapGestures', 'androidx.compose.foundation.gestures.detectTapGestures')

with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'w') as f:
    f.write(text)
