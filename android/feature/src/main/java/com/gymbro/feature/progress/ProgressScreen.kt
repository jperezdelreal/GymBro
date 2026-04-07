package com.gymbro.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.RecordType
import com.gymbro.core.model.WorkoutHistoryItem
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentGreen = Color(0xFF00FF87)
private val AccentAmber = Color(0xFFFFAB00)
private val AccentCyan = Color(0xFF00E5FF)
private val SurfaceColor = Color(0xFF1A1A1A)
private val SurfaceVariantColor = Color(0xFF2A2A2A)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun ProgressRoute(
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
) {
    val viewModel: ProgressViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProgressEffect.NavigateToWorkoutDetail ->
                    onNavigateToWorkoutDetail(effect.workoutId)
            }
        }
    }

    ProgressScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToAnalytics = onNavigateToAnalytics,
    )
}

@Composable
private fun ProgressScreen(
    state: ProgressState,
    onEvent: (ProgressEvent) -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
) {
    if (state.isLoading) {
        FullScreenLoading(message = "Loading progress...")
        return
    }

    if (state.workoutHistory.isEmpty() && state.personalRecords.isEmpty()) {
        EmptyProgressState()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Analytics Card
        item {
            AnalyticsNavigationCard(onClick = onNavigateToAnalytics)
        }
        
        // Section: Personal Records
        if (state.personalRecords.isNotEmpty()) {
            item {
                SectionHeader(title = "Personal Records", icon = "🏆")
            }

            val selectedRecords = if (state.selectedExerciseId != null) {
                state.personalRecords.filter { it.exerciseId == state.selectedExerciseId }
            } else {
                state.personalRecords
            }

            item {
                PRGrid(records = selectedRecords)
            }
        }

        // Section: E1RM Chart
        if (state.exerciseOptions.isNotEmpty()) {
            item {
                SectionHeader(title = "Estimated 1RM Trend", icon = "📈")
            }

            item {
                ExerciseChipRow(
                    options = state.exerciseOptions,
                    selectedId = state.selectedExerciseId,
                    onSelect = { onEvent(ProgressEvent.SelectExercise(it)) },
                )
            }

            if (state.chartData.isNotEmpty()) {
                item {
                    E1RMChart(data = state.chartData)
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No data for this exercise yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Section: Workout History
        if (state.workoutHistory.isNotEmpty()) {
            item {
                SectionHeader(title = "Workout History", icon = "📋")
            }

            items(state.workoutHistory, key = { it.workoutId }) { workout ->
                WorkoutHistoryRow(
                    item = workout,
                    onClick = { onEvent(ProgressEvent.ViewWorkoutDetail(workout.workoutId)) },
                )
            }
        }
    }
}

@Composable
private fun EmptyProgressState() {
    EmptyState(
        icon = Icons.Default.EmojiEvents,
        title = "No workouts yet",
        subtitle = "Start training to see your progress!",
    )
}

@Composable
private fun AnalyticsNavigationCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "📊",
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Advanced Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "View detailed training insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).semantics { heading() }
    ) {
        Text(
            text = icon,
            fontSize = 20.sp,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PRGrid(records: List<PersonalRecord>) {
    val grouped = records.groupBy { it.type }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Show 2 cards per row
        grouped.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (_, prs) ->
                    val pr = prs.maxByOrNull { it.value } ?: return@forEach
                    PRCard(
                        record = pr,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PRCard(record: PersonalRecord, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${record.type.displayName}: ${formatPRValue(record)} for ${record.exerciseName}"
        },
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.type.emoji,
                    fontSize = 16.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = record.type.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatPRValue(record),
                style = MaterialTheme.typography.headlineSmall,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
            )
            if (record.exerciseName.isNotEmpty()) {
                Text(
                    text = record.exerciseName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            val prev = record.previousValue
            if (prev != null) {
                val improvement = record.value - prev
                if (improvement > 0) {
                    Text(
                        text = "+${formatNumber(improvement)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseChipRow(
    options: List<ExerciseOption>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                label = {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentAmber.copy(alpha = 0.2f),
                    selectedLabelColor = AccentAmber,
                    containerColor = SurfaceVariantColor,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun E1RMChart(data: List<E1RMDataPoint>) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Current E1RM value
            val latest = data.lastOrNull()
            if (latest != null) {
                Text(
                    text = "${formatNumber(latest.e1rm)} kg",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Current Est. 1RM (${latest.weight}kg × ${latest.reps})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            ) {
                if (data.size < 2) {
                    // Single point — draw a dot
                    val cx = size.width / 2
                    val cy = size.height / 2
                    drawCircle(
                        color = AccentAmber,
                        radius = 6.dp.toPx(),
                        center = Offset(cx, cy),
                    )
                    return@Canvas
                }

                val paddingLeft = 48.dp.toPx()
                val paddingBottom = 28.dp.toPx()
                val paddingTop = 8.dp.toPx()
                val paddingRight = 8.dp.toPx()

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                val minE1RM = data.minOf { it.e1rm } * 0.95
                val maxE1RM = data.maxOf { it.e1rm } * 1.05
                val range = (maxE1RM - minE1RM).coerceAtLeast(1.0)

                fun xForIndex(i: Int): Float =
                    paddingLeft + (i.toFloat() / (data.size - 1)) * chartWidth

                fun yForValue(v: Double): Float =
                    paddingTop + ((maxE1RM - v) / range).toFloat() * chartHeight

                // Grid lines (3 horizontal)
                val gridCount = 3
                for (i in 0..gridCount) {
                    val v = minE1RM + (range * i / gridCount)
                    val y = yForValue(v)
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(paddingLeft, y),
                        end = Offset(size.width - paddingRight, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                    // Y-axis label
                    val label = "${v.toInt()}"
                    val textResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                        ),
                    )
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(
                            paddingLeft - textResult.size.width - 4.dp.toPx(),
                            y - textResult.size.height / 2,
                        ),
                    )
                }

                // Line path
                val path = Path()
                data.forEachIndexed { i, point ->
                    val x = xForIndex(i)
                    val y = yForValue(point.e1rm)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = AccentAmber,
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                // Data points
                data.forEachIndexed { i, point ->
                    val x = xForIndex(i)
                    val y = yForValue(point.e1rm)
                    drawCircle(
                        color = AccentAmber,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = SurfaceColor,
                        radius = 2.dp.toPx(),
                        center = Offset(x, y),
                    )
                }

                // X-axis date labels (first and last)
                val zone = ZoneId.systemDefault()
                listOf(0, data.size - 1).forEach { i ->
                    val point = data[i]
                    val label = point.date.atZone(zone).format(dateFormatter)
                    val textResult = textMeasurer.measure(
                        text = label,
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp,
                        ),
                    )
                    val x = xForIndex(i)
                    drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(
                            (x - textResult.size.width / 2)
                                .coerceIn(paddingLeft, size.width - paddingRight - textResult.size.width),
                            size.height - paddingBottom + 4.dp.toPx(),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryRow(
    item: WorkoutHistoryItem,
    onClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.date.atZone(zone).format(fullDateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatChip(
                        icon = Icons.Default.FitnessCenter,
                        text = "${item.exerciseCount} exercises",
                    )
                    StatChip(
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        text = "${formatNumber(item.totalVolume)} kg",
                    )
                    if (item.durationSeconds > 0) {
                        StatChip(
                            icon = Icons.Default.Timer,
                            text = formatDuration(item.durationSeconds),
                        )
                    }
                }
            }

            if (item.prCount > 0) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = "${item.prCount} PRs",
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun StatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPRValue(record: PersonalRecord): String {
    return when (record.type) {
        RecordType.MAX_WEIGHT -> "${formatNumber(record.value)} kg"
        RecordType.MAX_REPS -> "${record.value.toInt()} reps"
        RecordType.MAX_VOLUME -> "${formatNumber(record.value)} kg"
        RecordType.MAX_E1RM -> "${formatNumber(record.value)} kg"
    }
}

private fun formatNumber(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        "%.1f".format(value)
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
