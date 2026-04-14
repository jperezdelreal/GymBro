package com.gymbro.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymbro.core.ui.theme.GlassBorder
import com.gymbro.core.ui.theme.GlassOverlay

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cardModifier = if (onClick != null) {
        modifier.fillMaxWidth().scaleOnPress()
    } else {
        modifier.fillMaxWidth()
    }

    val cardContent: @Composable () -> Unit = {
        Column {
            if (accentColor != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(accentColor)
                )
            }
            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }

    if (onClick != null) {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = onClick,
        ) { cardContent() }
    } else {
        Card(
            modifier = cardModifier,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GlassOverlay),
            border = BorderStroke(1.dp, GlassBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) { cardContent() }
    }
}
