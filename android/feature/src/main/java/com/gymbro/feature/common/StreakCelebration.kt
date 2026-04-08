package com.gymbro.feature.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private val AccentGreen = Color(0xFF00FF87)
private val AccentAmber = Color(0xFFFFAB00)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceDark = Color(0xFF0A0A0A)

/**
 * Displays a celebration overlay for workout streak milestones.
 *
 * @param streakDays Number of consecutive workout days
 * @param onDismiss Callback when celebration is dismissed
 * @param modifier Modifier for sizing/positioning
 */
@Composable
fun StreakCelebration(
    streakDays: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val view = LocalView.current

    LaunchedEffect(Unit) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
        delay(3500)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300),
        )
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark.copy(alpha = 0.95f * alpha.value))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        ConfettiOverlay(useLottie = true)

        Column(
            modifier = Modifier
                .scale(scale.value)
                .padding(24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                GymBroLottieAnimation(
                    animationResId = com.gymbro.feature.R.raw.anim_workout,
                    modifier = Modifier.size(80.dp),
                    iterations = 1,
                    speed = 1f,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (streakDays) {
                    7 -> "7 Day Streak!"
                    30 -> "30 Day Streak!"
                    else -> "$streakDays Day Streak!"
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = AccentGreen,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (streakDays) {
                    7 -> "One week of consistency — you're building serious momentum!"
                    30 -> "One month strong — you're unstoppable!"
                    else -> "Keep the fire burning!"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap to continue",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}
