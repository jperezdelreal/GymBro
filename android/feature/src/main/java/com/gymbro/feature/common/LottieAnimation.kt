package com.gymbro.feature.common

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Wrapper for Lottie animations that provides a simplified, opinionated API for GymBro.
 *
 * Automatically handles loading state and playback with sensible defaults:
 * - Plays once by default (iterations = 1)
 * - Auto-plays immediately
 * - Centered in the provided modifier space
 *
 * @param animationResId Raw resource ID of the Lottie JSON file
 * @param modifier Modifier for sizing/positioning the animation
 * @param iterations Number of times to play. Use [LottieConstants.IterateForever] to loop
 * @param speed Playback speed multiplier (1f = normal speed, 2f = 2x speed)
 */
@Composable
fun GymBroLottieAnimation(
    @RawRes animationResId: Int,
    modifier: Modifier = Modifier,
    iterations: Int = 1,
    speed: Float = 1f,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(animationResId)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed,
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
        )
    }
}
