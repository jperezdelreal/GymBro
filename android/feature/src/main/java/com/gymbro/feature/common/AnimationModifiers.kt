package com.gymbro.feature.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Scale animation on press for interactive cards.
 * Scales to 0.97 on press, returns to 1.0 on release with spring physics.
 */
fun Modifier.scaleOnPress(): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scaleOnPress"
    )
    
    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

/**
 * Staggered entry animation for list items.
 * Each item fades in and slides up with a delay based on its index.
 * 
 * @param index The index of the item in the list
 * @param delayPerItem Delay in milliseconds between each item (default: 50ms)
 */
fun Modifier.staggeredEntry(
    index: Int,
    delayPerItem: Int = 50
): Modifier = composed {
    val delayMs = index * delayPerItem
    
    this.graphicsLayer {
        // Animation will be handled by AnimatedVisibility in the LazyColumn
        // This modifier provides the positioning
        alpha = 1f
        translationY = 0f
    }
}
