package com.gymbro.feature.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.PlateauAlert
import com.gymbro.core.model.PlateauType
import com.gymbro.core.model.RecordType
import com.gymbro.core.model.WorkoutHistoryItem
import com.gymbro.core.ui.theme.AccentAmberEnd
import com.gymbro.core.ui.theme.AccentAmberStart
import com.gymbro.core.ui.theme.AccentCyanEnd
import com.gymbro.core.ui.theme.AccentCyanStart
import com.gymbro.core.ui.theme.AccentGreenEnd
import com.gymbro.core.ui.theme.AccentGreenStart
import com.gymbro.core.ui.theme.AccentRed
import com.gymbro.core.ui.theme.Surface
import com.gymbro.core.ui.theme.SurfaceVariant
import com.gymbro.core.R
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import com.gymbro.feature.common.TooltipOverlay
import com.gymbro.feature.common.TooltipPosition
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
private val fullDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun ProgressRoute(
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCoach: (String) -> Unit = {},
) {
    val viewModel: ProgressViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChartTooltip by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showChartTooltip = viewModel.tooltipManager.shouldShow("progress_chart")
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ProgressEffect.NavigateToWorkoutDetail ->
                    onNavigateToWorkoutDetail(effect.workoutId)
                is ProgressEffect.NavigateToCoach ->
                    onNavigateToCoach(effect.prompt)
            }
        }
    }

    ProgressScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToAnalytics = onNavigateToAnalytics,
        showChartTooltip = showChartTooltip,
        onTooltipDismissed = {
            showChartTooltip = false
            viewModel.viewModelScope.launch {
                viewModel.tooltipManager.markShown("progress_chart")
            }
        }
    )
}

@Composable
private fun ProgressScreen(
    state: ProgressState,
    onEvent: (ProgressEvent) -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    showChartTooltip: Boolean = false,
    onTooltipDismissed: () -> Unit = {},
) {
    if (state.isLoading) {
        FullScreenLoading(message = stringResource(R.string.progress_loading_message))
        return
    }

    if (state.workoutHistory.isEmpty() && state.personalRecords.isEmpty()) {
        EmptyProgressState()
        return
    }

    Box {
        LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Time Period Selector
        item {
            TimePeriodSelector(
                selectedPeriod = state.selectedTimePeriod,
                onPeriodSelected = { onEvent(ProgressEvent.SelectTimePeriod(it)) }
            )
        }

        // Hero KPI Cards (3 across)
        item {
            HeroKPISection(
                totalVolume = state.totalVolume,
                workoutsThisWeek = state.workoutsThisWeek,
                recentPRs = state.recentPRs,
                volumeChangePercent = state.volumeChangePercent,
                workoutFrequencyGoal = state.workoutFrequencyGoal,
            )
        }

        // Top 5 Exercises Section
        if (state.topExercises.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.progress_top_exercises), icon = "💪")
            }
            item {
                TopExercisesSection(exercises = state.topExercises)
            }
        }

        // Volume Chart Section
        if (state.weeklyVolumeData.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.progress_weekly_volume), icon = "📊")
            }
            item {
                WeeklyVolumeChart(data = state.weeklyVolumeData)
            }
        }

        // Plateau Alerts Section
        item {
            SectionHeader(title = stringResource(R.string.progress_plateau_alerts), icon = "⚠️")
        }
        if (state.plateauAlerts.isNotEmpty()) {
            items(state.plateauAlerts) { alert ->
                PlateauAlertCard(
                    alert = alert,
                    onDismiss = { onEvent(ProgressEvent.DismissPlateauAlert(alert.exerciseId)) },
                    onGetCoachingAdvice = { onEvent(ProgressEvent.GetCoachingAdvice(alert.exerciseName, alert.weeksDuration)) }
                )
            }
        } else {
            item {
                PlateauEmptyState()
            }
        }

        // PR Showcase Section
        if (state.recentPRsWithDetails.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.progress_recent_prs), icon = "🏆")
            }
            val displayPRs = state.recentPRsWithDetails.take(5)
            items(displayPRs) { pr ->
                PRShowcaseCard(record = pr)
            }
        }

        // E1RM Chart Section
        if (state.exerciseOptions.isNotEmpty()) {
            item {
                SectionHeader(title = stringResource(R.string.progress_e1rm_trend), icon = "📈")
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
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.progress_no_data),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

        if (showChartTooltip && state.chartData.isNotEmpty()) {
            TooltipOverlay(
                message = stringResource(R.string.progress_evolution),
                position = TooltipPosition.CENTER,
                offsetY = 0,
                onDismiss = onTooltipDismissed
            )
        }
    }
}

@Composable
private fun EmptyProgressState() {
    EmptyState(
        icon = Icons.Default.EmojiEvents,
        title = stringResource(R.string.progress_empty_title),
        subtitle = stringResource(R.string.progress_empty_subtitle_alt),
    )
}

@Composable
private fun AnalyticsNavigationCard(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
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
                        text = stringResource(R.string.progress_advanced_analytics),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.progress_detailed_insights),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = null,
                tint = AccentGreenStart,
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
        colors = CardDefaults.cardColors(containerColor = Surface),
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
                color = AccentGreenStart,
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
                        color = AccentGreenStart,
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
    val haptic = LocalHapticFeedback.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option ->
            FilterChip(
                selected = option.id == selectedId,
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(option.id) 
                },
                label = {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentAmberStart.copy(alpha = 0.2f),
                    selectedLabelColor = AccentAmberStart,
                    containerColor = SurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Current E1RM value
            val latest = data.lastOrNull()
            if (latest != null) {
                Text(
                    text = "${formatNumber(latest.e1rm)} kg",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AccentAmberStart,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.progress_current_est_1rm, latest.weight.toString(), latest.reps),
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
                        color = AccentAmberStart,
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
                    color = AccentAmberStart,
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
                        color = AccentAmberStart,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = Surface,
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
    val haptic = LocalHapticFeedback.current
    val zone = ZoneId.systemDefault()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        colors = CardDefaults.cardColors(containerColor = Surface),
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
                tint = AccentCyanStart,
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
                    tint = AccentGreenStart,
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

@Composable
private fun HeroKPISection(
    totalVolume: Double,
    workoutsThisWeek: Int,
    recentPRs: Int,
    volumeChangePercent: Double?,
    workoutFrequencyGoal: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GlassmorphicCard(
            modifier = Modifier.weight(1f),
            accentColor = Color(AccentGreenStart.value),
        ) {
            Column {
                Text(
                    text = formatNumber(totalVolume),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(AccentGreenStart.value),
                )
                Text(
                    text = stringResource(R.string.progress_total_volume),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (volumeChangePercent != null) {
                        val changeColor = when {
                            volumeChangePercent > 0 -> Color(AccentGreenStart.value)
                            volumeChangePercent < 0 -> Color(AccentRed.value)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "${if (volumeChangePercent > 0) "+" else ""}${String.format("%.1f", volumeChangePercent)}%",
                            fontSize = 11.sp,
                            color = changeColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(AccentGreenStart.value),
                    )
                    Text(
                        text = " kg",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        GlassmorphicCard(
            modifier = Modifier.weight(1f),
            accentColor = Color(AccentCyanStart.value),
        ) {
            Column {
                Text(
                    text = "$workoutsThisWeek",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(AccentCyanStart.value),
                )
                Text(
                    text = stringResource(R.string.progress_workouts_label),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.progress_workout_goal, workoutsThisWeek, workoutFrequencyGoal),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        GlassmorphicCard(
            modifier = Modifier.weight(1f),
            accentColor = Color(AccentAmberStart.value),
        ) {
            Column {
                Text(
                    text = "$recentPRs",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(AccentAmberStart.value),
                )
                Text(
                    text = stringResource(R.string.progress_prs_label),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.progress_last_2_weeks),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WeeklyVolumeChart(data: List<WeeklyVolume>) {
    val textMeasurer = rememberTextMeasurer()
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.progress_last_8_weeks),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
            ) {
                if (data.isEmpty()) return@Canvas

                val paddingLeft = 40.dp.toPx()
                val paddingBottom = 32.dp.toPx()
                val paddingTop = 16.dp.toPx()
                val paddingRight = 16.dp.toPx()

                val chartWidth = size.width - paddingLeft - paddingRight
                val chartHeight = size.height - paddingTop - paddingBottom

                val maxVolume = data.maxOfOrNull { it.volume } ?: 1.0
                val barWidth = chartWidth / data.size * 0.7f
                val spacing = chartWidth / data.size * 0.3f

                data.forEachIndexed { index, weekVolume ->
                    val barHeight = if (maxVolume > 0) {
                        (weekVolume.volume / maxVolume * chartHeight).toFloat()
                    } else 0f

                    val x = paddingLeft + index * (barWidth + spacing)
                    val y = size.height - paddingBottom - barHeight

                    // Gradient bar
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(AccentGreenStart.value),
                                Color(AccentGreenEnd.value),
                            ),
                            startY = y,
                            endY = size.height - paddingBottom,
                        ),
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    )

                    // Week label
                    val label = "W${weekVolume.weekNumber}"
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
                            x + barWidth / 2 - textResult.size.width / 2,
                            size.height - paddingBottom + 8.dp.toPx(),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlateauAlertCard(
    alert: PlateauAlert,
    onDismiss: () -> Unit,
    onGetCoachingAdvice: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val alertColor = when (alert.severity) {
        com.gymbro.core.model.PlateauSeverity.MILD -> Color(AccentAmberStart.value)
        com.gymbro.core.model.PlateauSeverity.MODERATE -> Color(0xFFFF9800)
        com.gymbro.core.model.PlateauSeverity.SEVERE -> AccentRed
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = alertColor.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (alert.type) {
                            PlateauType.STAGNATION -> stringResource(
                                R.string.progress_plateau_detected_stagnation,
                                alert.exerciseName
                            )
                            PlateauType.REGRESSION -> stringResource(
                                R.string.progress_plateau_detected_regression,
                                alert.exerciseName
                            )
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.progress_plateau_duration_days,
                            alert.weeksDuration,
                            alert.daysSinceLastPR
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.progress_plateau_consider) + " " + alert.suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_dismiss),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            androidx.compose.material3.Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onGetCoachingAdvice()
                },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = alertColor,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_plateau_get_coaching),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PlateauEmptyState() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(AccentGreenStart.value).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(AccentGreenStart.value),
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = stringResource(R.string.progress_plateau_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun PRShowcaseCard(record: PersonalRecord) {
    val zone = ZoneId.systemDefault()
    GlassmorphicCard(
        accentColor = Color(AccentAmberStart.value),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🏆",
                fontSize = 32.sp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${record.type.displayName}: ${formatPRValue(record)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(AccentAmberStart.value),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = record.date.atZone(zone).format(fullDateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            record.previousValue?.let { previousValue ->
                val improvement = record.value - previousValue
                if (improvement > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(AccentAmberStart.value),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "+${formatNumber(improvement)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(AccentAmberStart.value),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedPeriod == TimePeriod.THIS_WEEK,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPeriodSelected(TimePeriod.THIS_WEEK)
            },
            label = { Text(stringResource(R.string.progress_period_this_week)) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(AccentGreenStart.value).copy(alpha = 0.2f),
                selectedLabelColor = Color(AccentGreenStart.value),
                containerColor = Color(SurfaceVariant.value),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        FilterChip(
            selected = selectedPeriod == TimePeriod.LAST_WEEK,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPeriodSelected(TimePeriod.LAST_WEEK)
            },
            label = { Text(stringResource(R.string.progress_period_last_week)) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(AccentGreenStart.value).copy(alpha = 0.2f),
                selectedLabelColor = Color(AccentGreenStart.value),
                containerColor = Color(SurfaceVariant.value),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        FilterChip(
            selected = selectedPeriod == TimePeriod.THIS_MONTH,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPeriodSelected(TimePeriod.THIS_MONTH)
            },
            label = { Text(stringResource(R.string.progress_period_this_month)) },
            modifier = Modifier.weight(1f),
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(AccentGreenStart.value).copy(alpha = 0.2f),
                selectedLabelColor = Color(AccentGreenStart.value),
                containerColor = Color(SurfaceVariant.value),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
private fun TopExercisesSection(exercises: List<TopExercise>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(Surface.value)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            exercises.forEachIndexed { index, exercise ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(AccentCyanStart.value),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = exercise.exerciseName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.progress_set_count, exercise.setCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index < exercises.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
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
