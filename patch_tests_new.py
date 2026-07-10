with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'r') as f:
    text = f.read()

new_tests = """
@Composable
fun RefreshRateTest(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
    val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }
    val refreshRate = display?.refreshRate ?: 60f

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
    val xOffset by infiniteTransition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween((1000f * (120f / refreshRate)).toInt(), easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "xOffset"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Display Refresh Rate", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        Text("Reported Rate: ${refreshRate.toInt()} Hz", fontSize = 18.sp, modifier = Modifier.padding(bottom = 32.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Box(modifier = Modifier
                .androidx.compose.ui.graphics.graphicsLayer { translationX = xOffset }
                .size(50.dp)
                .background(Color.Red, androidx.compose.foundation.shape.CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
fun BurnInTest(onClose: () -> Unit) {
    var colorIndex by remember { mutableStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000)
            colorIndex = (colorIndex + 1) % colors.size
        }
    }
    
    Box(modifier = Modifier
        .fillMaxSize()
        .background(colors[colorIndex])
        .androidx.compose.ui.input.pointer.pointerInput(Unit) {
            androidx.compose.foundation.gestures.detectTapGestures {
                colorIndex = (colorIndex + 1) % colors.size
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onClose, modifier = Modifier.padding(32.dp)) {
            Text("End Burn-in Fixer")
        }
    }
}
"""

text = text + new_tests
with open('app/src/main/java/com/deviceinfo/gad/HardwareTests.kt', 'w') as f:
    f.write(text)
