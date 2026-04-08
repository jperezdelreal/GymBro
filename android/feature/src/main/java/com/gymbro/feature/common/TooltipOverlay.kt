package com.gymbro.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay

enum class TooltipPosition {
    TOP_CENTER,
    BOTTOM_CENTER,
    CENTER
}

@Composable
fun TooltipOverlay(
    message: String,
    position: TooltipPosition = TooltipPosition.CENTER,
    offsetY: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .zIndex(1000f),
        contentAlignment = when (position) {
            TooltipPosition.TOP_CENTER -> Alignment.TopCenter
            TooltipPosition.BOTTOM_CENTER -> Alignment.BottomCenter
            TooltipPosition.CENTER -> Alignment.Center
        }
    ) {
        GlassmorphicCard(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(24.dp)
                .offset { IntOffset(0, offsetY) }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                GradientButton(
                    text = "Entendido",
                    onClick = onDismiss,
                    modifier = Modifier
                )
            }
        }
    }
}
