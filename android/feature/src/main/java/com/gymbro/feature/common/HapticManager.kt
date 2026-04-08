package com.gymbro.feature.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

object HapticManager {
    fun performLight(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun performMedium(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun performHeavy(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

@Composable
fun Modifier.hapticClick(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    return this.clickable(enabled = enabled) {
        if (enabled) {
            HapticManager.performLight(haptic)
        }
        onClick()
    }
}

fun Modifier.hapticClickModifier(
    haptic: HapticFeedback,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    return this.clickable(enabled = enabled) {
        if (enabled) {
            HapticManager.performLight(haptic)
        }
        onClick()
    }
}

fun Modifier.hapticTap(
    haptic: HapticFeedback,
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier {
    return this.pointerInput(enabled) {
        if (enabled) {
            detectTapGestures(
                onTap = {
                    HapticManager.performLight(haptic)
                    onTap()
                }
            )
        }
    }
}
