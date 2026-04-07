package com.gymbro.feature.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin
import kotlin.random.Random

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentAmber = Color(0xFFFFAB00)

private data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val rotation: Float,
    val size: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
)

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
) {
    val colors = listOf(AccentGreen, AccentCyan, AccentAmber)
    val particles = remember {
        List(80) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -0.3f,
                color = colors.random(),
                rotation = Random.nextFloat() * 360f,
                size = Random.nextFloat() * 8f + 4f,
                speed = Random.nextFloat() * 0.5f + 0.3f,
                swayAmplitude = Random.nextFloat() * 0.05f + 0.02f,
                swayFrequency = Random.nextFloat() * 2f + 1f,
            )
        }
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val progress = animationProgress.value

        particles.forEach { particle ->
            val currentY = particle.y + progress * (1.3f)
            if (currentY in 0f..1.3f) {
                val sway = sin(progress * particle.swayFrequency * 2 * Math.PI).toFloat()
                val currentX = particle.x + sway * particle.swayAmplitude

                val centerX = currentX * canvasWidth
                val centerY = currentY * canvasHeight

                rotate(
                    degrees = particle.rotation + progress * 360f * 2,
                    pivot = Offset(centerX, centerY),
                ) {
                    drawRect(
                        color = particle.color,
                        topLeft = Offset(
                            centerX - particle.size / 2,
                            centerY - particle.size / 2,
                        ),
                        size = androidx.compose.ui.geometry.Size(particle.size, particle.size),
                    )
                }
            }
        }
    }
}
