package com.gymbro.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed

@Composable
fun SparklineChart(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier,
) {
    if (dataPoints.isEmpty()) {
        return
    }

    if (dataPoints.size == 1) {
        // Single data point - just draw a dot
        Canvas(modifier = modifier.width(48.dp).height(24.dp)) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            drawCircle(
                color = AccentGreenStart,
                radius = 3.dp.toPx(),
                center = Offset(centerX, centerY),
            )
        }
        return
    }

    // Determine trend: upward or downward
    val trend = when {
        dataPoints.last() > dataPoints.first() -> Trend.UPWARD
        dataPoints.last() < dataPoints.first() -> Trend.DOWNWARD
        else -> Trend.NEUTRAL
    }

    val lineColor = when (trend) {
        Trend.UPWARD -> AccentGreenStart
        Trend.DOWNWARD -> AccentRed
        Trend.NEUTRAL -> AccentGreenStart
    }

    Canvas(modifier = modifier.width(48.dp).height(24.dp)) {
        val width = size.width
        val height = size.height
        val padding = 2.dp.toPx()

        // Find min and max for scaling
        val minValue = dataPoints.minOrNull() ?: 0.0
        val maxValue = dataPoints.maxOrNull() ?: 1.0
        val range = maxValue - minValue

        // Avoid division by zero
        val normalizedRange = if (range == 0.0) 1.0 else range

        // Calculate step size between points
        val stepX = (width - padding * 2) / (dataPoints.size - 1).toFloat()

        val path = Path()

        dataPoints.forEachIndexed { index, value ->
            // Normalize value to [0, 1] range, then map to canvas coordinates
            val normalizedValue = if (normalizedRange > 0) {
                ((value - minValue) / normalizedRange).toFloat()
            } else {
                0.5f
            }

            // Invert Y because canvas coordinates are top-down
            val x = padding + index * stepX
            val y = height - padding - (normalizedValue * (height - padding * 2))

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw the line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        // Draw dots at each data point
        dataPoints.forEachIndexed { index, value ->
            val normalizedValue = if (normalizedRange > 0) {
                ((value - minValue) / normalizedRange).toFloat()
            } else {
                0.5f
            }

            val x = padding + index * stepX
            val y = height - padding - (normalizedValue * (height - padding * 2))

            drawCircle(
                color = lineColor,
                radius = 2.5.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

private enum class Trend {
    UPWARD,
    DOWNWARD,
    NEUTRAL,
}
