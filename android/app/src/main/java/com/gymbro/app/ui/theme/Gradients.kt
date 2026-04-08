package com.gymbro.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GymBroGradients {
    val strength = Brush.linearGradient(
        colors = listOf(AccentGreenStart, AccentGreenEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val cardio = Brush.linearGradient(
        colors = listOf(AccentCyanStart, AccentCyanEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val celebration = Brush.linearGradient(
        colors = listOf(AccentAmberStart, AccentAmberEnd),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    val glass = Brush.linearGradient(
        colors = listOf(
            GlassOverlay.copy(alpha = 0.15f),
            GlassOverlay.copy(alpha = 0.05f)
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )
}

fun Modifier.gradientBackground(brush: Brush): Modifier = this.background(brush)
