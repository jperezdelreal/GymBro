package com.gymbro.feature.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymbro.core.R
import com.gymbro.core.model.ExerciseProgressPoint
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.OnSurfaceVariant
import com.gymbro.feature.common.GymBroCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// --- Date range filter ---

enum class ChartDateRange(val months: Int?) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    SIX_MONTHS(6),
    ALL(null),
}

// --- Chart view mode ---

enum class ChartViewMode {
    WEIGHT,
    RPE,
    VOLUME,
}

@Composable
fun ExerciseProgressChart(
    dataPoints: List<ExerciseProgressPoint>,
    exerciseName: String,
    useKg: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (dataPoints.isEmpty()) return

    var selectedRange by remember { mutableStateOf(ChartDateRange.ALL) }
    var selectedMode by remember { mutableStateOf(ChartViewMode.WEIGHT) }
    var selectedPointIndex by remember { mutableIntStateOf(-1) }

    val filteredData = remember(dataPoints, selectedRange) {
        filterByRange(dataPoints, selectedRange)
    }

    val rpeWarning = remember(filteredData) {
        detectRpeWarning(filteredData)
    }

    GymBroCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Title
            Text(
                text = stringResource(R.string.progress_chart_title, exerciseName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // View mode chips
            val weightLabel = stringResource(R.string.progress_chart_weight)
            val volumeLabel = stringResource(R.string.progress_chart_volume)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modes = listOf(
                    ChartViewMode.WEIGHT to weightLabel,
                    ChartViewMode.RPE to "RPE",
                    ChartViewMode.VOLUME to volumeLabel,
                )
                modes.forEach { (mode, label) ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = {
                            selectedMode = mode
                            selectedPointIndex = -1
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Date range chips
            val label1m = stringResource(R.string.progress_chart_1m)
            val label3m = stringResource(R.string.progress_chart_3m)
            val label6m = stringResource(R.string.progress_chart_6m)
            val labelAll = stringResource(R.string.progress_chart_all)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val ranges = listOf(
                    ChartDateRange.ONE_MONTH to label1m,
                    ChartDateRange.THREE_MONTHS to label3m,
                    ChartDateRange.SIX_MONTHS to label6m,
                    ChartDateRange.ALL to labelAll,
                )
                ranges.forEach { (range, label) ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = {
                            selectedRange = range
                            selectedPointIndex = -1
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooltip for selected point
            if (selectedPointIndex in filteredData.indices) {
                val point = filteredData[selectedPointIndex]
                PointTooltip(point = point, mode = selectedMode, useKg = useKg)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // The chart canvas
            if (filteredData.size >= 2) {
                ProgressChartCanvas(
                    data = filteredData,
                    mode = selectedMode,
                    useKg = useKg,
                    selectedIndex = selectedPointIndex,
                    onPointSelected = { selectedPointIndex = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            } else if (filteredData.size == 1) {
                SinglePointDisplay(point = filteredData.first(), mode = selectedMode, useKg = useKg)
            } else {
                Text(
                    text = stringResource(R.string.progress_chart_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp),
                )
            }

            // RPE warning
            if (rpeWarning && selectedMode == ChartViewMode.RPE) {
                Spacer(modifier = Modifier.height(12.dp))
                RpeWarningChip()
            }
        }
    }
}

// --- Canvas chart ---

@Composable
private fun ProgressChartCanvas(
    data: List<ExerciseProgressPoint>,
    mode: ChartViewMode,
    useKg: Boolean,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data, mode) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val lineColor = when (mode) {
        ChartViewMode.WEIGHT -> AccentCyanStart
        ChartViewMode.RPE -> AccentAmberStart
        ChartViewMode.VOLUME -> AccentGreenStart
    }
    val labelColor = OnSurfaceVariant
    val gridColor = OnSurfaceVariant.copy(alpha = 0.2f)

    Canvas(
        modifier = modifier.pointerInput(data, mode) {
            detectTapGestures { offset ->
                val paddingLeft = 50.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val chartWidth = size.width - paddingLeft - paddingRight
                if (data.size < 2) return@detectTapGestures

                val stepX = chartWidth / (data.size - 1)
                val closestIndex = ((offset.x - paddingLeft) / stepX)
                    .toInt()
                    .coerceIn(0, data.size - 1)

                val pointX = paddingLeft + closestIndex * stepX
                if (abs(offset.x - pointX) < 30.dp.toPx()) {
                    onPointSelected(closestIndex)
                }
            }
        },
    ) {
        val paddingLeft = 50.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 8.dp.toPx()
        val paddingBottom = 28.dp.toPx()

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val values = data.map { point ->
            when (mode) {
                ChartViewMode.WEIGHT -> point.estimatedOneRepMax
                ChartViewMode.RPE -> point.averageRpe ?: 0.0
                ChartViewMode.VOLUME -> point.totalVolume
            }
        }

        val minValue = if (mode == ChartViewMode.RPE) 5.0 else (values.minOrNull() ?: 0.0) * 0.9
        val maxValue = if (mode == ChartViewMode.RPE) 10.0 else (values.maxOrNull() ?: 1.0) * 1.05
        val valueRange = (maxValue - minValue).coerceAtLeast(1.0)

        // Draw horizontal grid lines + Y labels
        val gridLines = 4
        for (i in 0..gridLines) {
            val fraction = i.toFloat() / gridLines
            val y = paddingTop + chartHeight * (1f - fraction)
            val value = minValue + valueRange * fraction

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )

            val label = formatYLabel(value, mode, useKg)
            val textResult = textMeasurer.measure(
                text = label,
                style = TextStyle(fontSize = 10.sp, color = labelColor),
            )
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x = paddingLeft - textResult.size.width - 6.dp.toPx(),
                    y = y - textResult.size.height / 2f,
                ),
            )
        }

        // Calculate points
        val stepX = if (data.size > 1) chartWidth / (data.size - 1) else 0f
        val points = values.mapIndexed { index, value ->
            val normalized = ((value - minValue) / valueRange).toFloat().coerceIn(0f, 1f)
            Offset(
                x = paddingLeft + index * stepX,
                y = paddingTop + chartHeight * (1f - normalized),
            )
        }

        // Animate: only draw up to animationProgress fraction
        val animatedCount = (points.size * animationProgress.value).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(animatedCount)

        // Gradient fill below line
        if (visiblePoints.size >= 2) {
            val fillPath = Path().apply {
                moveTo(visiblePoints.first().x, paddingTop + chartHeight)
                visiblePoints.forEach { lineTo(it.x, it.y) }
                lineTo(visiblePoints.last().x, paddingTop + chartHeight)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight,
                ),
            )

            // Line path
            val linePath = Path().apply {
                moveTo(visiblePoints.first().x, visiblePoints.first().y)
                for (i in 1 until visiblePoints.size) {
                    lineTo(visiblePoints[i].x, visiblePoints[i].y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }

        // Data point circles
        visiblePoints.forEachIndexed { index, offset ->
            val isSelected = index == selectedIndex
            val pointColor = if (mode == ChartViewMode.RPE) {
                rpeColor(data[index].averageRpe ?: 0.0)
            } else {
                lineColor
            }

            drawCircle(
                color = if (isSelected) Color.White else pointColor,
                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                center = offset,
            )
            if (isSelected) {
                drawCircle(
                    color = pointColor,
                    radius = 4.dp.toPx(),
                    center = offset,
                )
            }
        }

        // X-axis date labels (show ~5 labels max)
        val labelInterval = (data.size / 5).coerceAtLeast(1)
        data.forEachIndexed { index, point ->
            if (index % labelInterval == 0 || index == data.size - 1) {
                val label = point.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
                val textResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(fontSize = 9.sp, color = labelColor),
                )
                val x = paddingLeft + index * stepX - textResult.size.width / 2f
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(x.coerceAtLeast(0f), size.height - paddingBottom + 6.dp.toPx()),
                )
            }
        }
    }
}

// --- Tooltip ---

@Composable
private fun PointTooltip(
    point: ExerciseProgressPoint,
    mode: ChartViewMode,
    useKg: Boolean,
) {
    val dateText = point.date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))
    val unit = if (useKg) "kg" else "lbs"
    val weight = if (useKg) point.maxWeight else point.maxWeight * 2.20462
    val e1rm = if (useKg) point.estimatedOneRepMax else point.estimatedOneRepMax * 2.20462

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            )
            .padding(12.dp),
    ) {
        Column {
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            when (mode) {
                ChartViewMode.WEIGHT -> {
                    Text(
                        text = "${point.bestSet} — e1RM: ${"%.1f".format(e1rm)} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                ChartViewMode.RPE -> {
                    Text(
                        text = "RPE: ${"%.1f".format(point.averageRpe ?: 0.0)} — ${"%.1f".format(weight)} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = rpeColor(point.averageRpe ?: 0.0),
                    )
                }
                ChartViewMode.VOLUME -> {
                    val vol = if (useKg) point.totalVolume else point.totalVolume * 2.20462
                    Text(
                        text = stringResource(R.string.progress_chart_volume_value, "%.0f".format(vol), unit),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

// --- Single point display ---

@Composable
private fun SinglePointDisplay(
    point: ExerciseProgressPoint,
    mode: ChartViewMode,
    useKg: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.progress_chart_single_session),
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PointTooltip(point = point, mode = mode, useKg = useKg)
        }
    }
}

// --- RPE warning ---

@Composable
private fun RpeWarningChip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AccentAmberStart.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = AccentAmberStart,
            modifier = Modifier.height(16.dp).width(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.progress_chart_rpe_warning),
            style = MaterialTheme.typography.bodySmall,
            color = AccentAmberStart,
        )
    }
}

// --- Helpers ---

private fun filterByRange(
    data: List<ExerciseProgressPoint>,
    range: ChartDateRange,
): List<ExerciseProgressPoint> {
    val months = range.months ?: return data
    val cutoff = LocalDate.now().minusMonths(months.toLong())
    return data.filter { it.date >= cutoff }
}

private fun detectRpeWarning(data: List<ExerciseProgressPoint>): Boolean {
    if (data.size < 4) return false
    val last4 = data.takeLast(4)
    val rpeLast3 = last4.takeLast(3).mapNotNull { it.averageRpe }
    if (rpeLast3.size < 3) return false

    val rpeIncreasing = rpeLast3.zipWithNext().all { (a, b) -> b > a }
    val weightFlat = last4.last().maxWeight <= last4[last4.size - 4].maxWeight

    return rpeIncreasing && weightFlat
}

private fun rpeColor(rpe: Double): Color = when {
    rpe >= 9.5 -> AccentRed
    rpe >= 9.0 -> Color(0xFFFF6B00)
    rpe >= 8.0 -> AccentAmberStart
    rpe >= 6.0 -> AccentGreenStart
    else -> OnSurfaceVariant
}

private fun formatYLabel(value: Double, mode: ChartViewMode, useKg: Boolean): String = when (mode) {
    ChartViewMode.WEIGHT -> {
        val displayValue = if (useKg) value else value * 2.20462
        "%.0f".format(displayValue)
    }
    ChartViewMode.RPE -> "%.1f".format(value)
    ChartViewMode.VOLUME -> {
        val displayValue = if (useKg) value else value * 2.20462
        when {
            displayValue >= 10000 -> "%.0fk".format(displayValue / 1000)
            displayValue >= 1000 -> "%.1fk".format(displayValue / 1000)
            else -> "%.0f".format(displayValue)
        }
    }
}
