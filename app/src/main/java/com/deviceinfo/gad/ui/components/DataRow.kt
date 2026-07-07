package com.deviceinfo.gad.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RowShimmerPlaceholder(modifier: Modifier = Modifier) {
    val animationsEnabled = com.deviceinfo.gad.ui.components.LocalAnimationsEnabled.current
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    )

    val transition = rememberInfiniteTransition(label = "rowShimmer")
    val translateAnim by if (animationsEnabled) transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rowShimmer"
    ) else remember { androidx.compose.runtime.mutableStateOf(0f) }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .height(16.dp)
            .fillMaxWidth(0.5f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(brush)
    )
}

@Composable
fun DataRow(title: String, value: String, isLoading: Boolean = false) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f).padding(end = 16.dp)
            )
            AnimatedContent(
                targetState = isLoading to value,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400)))
                },
                label = "DataRow"
            ) { (placeholder, animatedValue) ->
                if (placeholder) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        RowShimmerPlaceholder(modifier = Modifier.width(60.dp))
                    }
                } else {
                    Text(
                        text = animatedValue,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )
    }
}
