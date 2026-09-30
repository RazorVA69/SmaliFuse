package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Modern Android 14/15 & Material Expressive Squiggle / Snake loading bar.
 * Produces a lively undulating sine wave path with smooth phase traveling motion.
 */
@Composable
fun SnakeSquiggleProgressBar(
    modifier: Modifier = Modifier,
    progress: Float = 0f, // 0.0f to 1.0f
    isIndeterminate: Boolean = false,
    strokeWidth: Dp = 4.dp,
    amplitude: Dp = 4.5.dp,
    wavelength: Dp = 26.dp,
    brush: Brush = Brush.horizontalGradient(
        listOf(
            Color(0xFF00897B),
            Color(0xFF26A69A),
            Color(0xFF00ACC1),
            Color(0xFF4DD0E1)
        )
    ),
    trackColor: Color = Color(0xFFE0F2F1)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "snake_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "animated_progress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((amplitude * 3) + strokeWidth)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val strokePx = strokeWidth.toPx()
        val ampPx = amplitude.toPx()
        val wavePx = wavelength.toPx()

        // 1. Draw subtle background track line
        drawLine(
            color = trackColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )

        // 2. Draw active snake squiggle wave
        val wavePath = Path()
        val step = 2f

        if (isIndeterminate) {
            // Full sliding snake wave across the bar
            var first = true
            var x = 0f
            while (x <= width) {
                val y = centerY + ampPx * sin(((x / wavePx) * 2 * PI).toFloat() - phase)
                if (first) {
                    wavePath.moveTo(x, y)
                    first = false
                } else {
                    wavePath.lineTo(x, y)
                }
                x += step
            }
        } else {
            // Determinate wave up to animatedProgress
            val activeWidth = width * animatedProgress
            if (activeWidth > 0f) {
                var first = true
                var x = 0f
                while (x <= activeWidth) {
                    // Softly taper amplitude near the snake head
                    val taper = if (x > activeWidth - wavePx) {
                        ((activeWidth - x) / wavePx).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val y = centerY + (ampPx * taper) * sin(((x / wavePx) * 2 * PI).toFloat() - phase)
                    if (first) {
                        wavePath.moveTo(x, y)
                        first = false
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += step
                }
            }
        }

        drawPath(
            path = wavePath,
            brush = brush,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
