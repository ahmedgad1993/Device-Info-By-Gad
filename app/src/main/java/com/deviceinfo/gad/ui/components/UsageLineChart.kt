package com.deviceinfo.gad.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.deviceinfo.gad.ui.components.LocalAnimationsEnabled

@Composable
fun UsageLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00E5FF),
    minValue: Float = 0f,
    maxValue: Float = 1f,
    highUsageThreshold: Float = 0.8f
) {
    if (data.isEmpty()) return

    val animationsEnabled = LocalAnimationsEnabled.current
    val latestValue = data.lastOrNull() ?: 0f
    
    // Shift color towards error tone (red) when nearing high usage
    val isHigh = latestValue >= highUsageThreshold
    val targetColor = if (isHigh) MaterialTheme.colorScheme.error else lineColor
    
    val animatedColor = if (animationsEnabled) {
        androidx.compose.animation.animateColorAsState(targetValue = targetColor, label = "chartColor").value
    } else targetColor
    
    val targetGlowAlpha = if (isHigh) 0.6f else 0.3f
    val animatedGlowAlpha = if (animationsEnabled) {
        animateFloatAsState(targetValue = targetGlowAlpha, animationSpec = tween(500), label = "glowAlpha").value
    } else targetGlowAlpha
    
    var isDrawing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isDrawing = true }
    val drawProgress = if (animationsEnabled) {
        animateFloatAsState(targetValue = if (isDrawing) 1f else 0f, animationSpec = tween(1000), label = "draw").value
    } else 1f

    var touchX by remember { mutableStateOf<Float?>(null) }

    Canvas(modifier = modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed }) {
                    touchX = event.changes.firstOrNull { it.pressed }?.position?.x
                } else {
                    touchX = null
                }
            }
        }
    }) {
        val width = size.width
        val height = size.height
        
        // Draw grid lines
        val gridLineColor = animatedColor.copy(alpha = 0.15f)
        val numGridLines = 3
        for (i in 0 until numGridLines) {
            val y = height * (i.toFloat() / (numGridLines - 1).coerceAtLeast(1))
            drawLine(
                color = gridLineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }
        
        val pointSpacing = width / (data.size - 1).coerceAtLeast(1)
        val path = Path()
        val fillPath = Path()
        
        var lastX = 0f
        var lastY = 0f

        val visiblePoints = (data.size * drawProgress).toInt().coerceAtLeast(1)
        val pointsToDraw = data.take(visiblePoints)
        
        pointsToDraw.forEachIndexed { index, value ->
            val normalizedValue = ((value.coerceIn(minValue, maxValue) - minValue) / (maxValue - minValue))
            val x = index * pointSpacing
            val y = height - (normalizedValue * height)
            
            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                // Cubic Bezier curve for smoothness
                val controlX1 = lastX + (x - lastX) / 2f
                val controlX2 = lastX + (x - lastX) / 2f
                path.cubicTo(controlX1, lastY, controlX2, y, x, y)
                fillPath.cubicTo(controlX1, lastY, controlX2, y, x, y)
            }
            lastX = x
            lastY = y
        }
        
        // Complete the fill path
        fillPath.lineTo(lastX, height)
        fillPath.close()
        
        // Gradient fill
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                animatedColor.copy(alpha = 0.4f),
                animatedColor.copy(alpha = 0.0f)
            ),
            startY = 0f,
            endY = height
        )
        drawPath(path = fillPath, brush = gradientBrush, style = Fill)
        
        // Draw the stroke
        drawPath(
            path = path,
            color = animatedColor,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Glowing marker on the latest data point
        val glowRadius = 8.dp.toPx()
        val markerRadius = 3.dp.toPx()
        
        drawCircle(
            color = animatedColor.copy(alpha = animatedGlowAlpha),
            radius = glowRadius,
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = markerRadius,
            center = Offset(lastX, lastY)
        )
        
        // Tooltip
        touchX?.let { tx ->
            val index = (tx / pointSpacing).toInt().coerceIn(0, pointsToDraw.size - 1)
            val v = pointsToDraw[index]
            val px = index * pointSpacing
            val py = height - (((v.coerceIn(minValue, maxValue) - minValue) / (maxValue - minValue)) * height)
            
            drawLine(color = animatedColor.copy(alpha = 0.5f), start = Offset(px, 0f), end = Offset(px, height), strokeWidth = 2f)
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(px, py))
            drawCircle(color = animatedColor, radius = 3.dp.toPx(), center = Offset(px, py))
            
            val text = "${(v * 100).toInt()}%"
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 36f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(text, px, (py - 20f).coerceAtLeast(40f), textPaint)
        }
    }
}
