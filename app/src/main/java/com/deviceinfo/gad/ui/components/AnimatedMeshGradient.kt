package com.deviceinfo.gad.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.deviceinfo.gad.ui.components.LocalAnimationsEnabled

@Composable
fun AnimatedMeshGradient(modifier: Modifier = Modifier) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val infiniteTransition = rememberInfiniteTransition(label = "mesh")

    val offset1 by if (animationsEnabled) infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    ) else remember { mutableStateOf(0.5f) }

    val offset2 by if (animationsEnabled) infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    ) else remember { mutableStateOf(0.5f) }
    
    val color1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val color2 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    val color3 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    val color4 = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color1, Color.Transparent),
                center = Offset(width * offset1, height * offset2),
                radius = width * 1.5f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color2, Color.Transparent),
                center = Offset(width * offset2, height * offset1),
                radius = width * 1.5f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(color3, Color.Transparent),
                center = Offset(width * (1 - offset1), height * (1 - offset2)),
                radius = width * 1.5f
            )
        )
    }
}
