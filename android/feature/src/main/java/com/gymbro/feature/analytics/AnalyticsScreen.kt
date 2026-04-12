package com.gymbro.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.service.ConsistencyMetrics
import com.gymbro.core.service.MuscleGroupDistribution
import com.gymbro.core.service.TopExercise
import com.gymbro.core.service.WeeklySummary
import com.gymbro.core.service.WeeklyVolumeData
import com.gymbro.feature.common.FullScreenLoading
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val AccentGreen = Color(0xFF00FF87)
private val AccentAmber = Color(0xFFFFAB00)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentPurple = Color(0xFFAA00FF)
private val AccentRed = Color(0xFFFF1744)
private val SurfaceColor = Color(0xFF1A1A1A)
private val SurfaceVariantColor = Color(0xFF2A2A2A)

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

@Composable
fun AnalyticsRoute(
    onNavigateBack: () -> Unit = {},
) {
    val viewModel: AnalyticsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AnalyticsEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    AnalyticsScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsScreen(
    state: AnalyticsState,
    onEvent: (AnalyticsEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.analytics_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AnalyticsEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_navigate_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        if (state.isLoading) {
            FullScreenLoading(message = stringResource(R.string.analytics_loading))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Weekly Summary
            state.summary?.let { summary ->
                item {
                    SectionHeader(title = stringResource(R.string.analytics_this_week), icon = "📊")
                }
                item {
                    WeeklySummarySection(summary)
                }
            }

            // Volume Trend
            if (state.volumeData.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.analytics_volume_trend), icon = "📈")
                }
                item {
                    VolumeChart(data = state.volumeData)
                }
            }

            // Muscle Balance
            if (state.muscleDistribution.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.analytics_muscle_balance), icon = "💪")
                }
                item {
                    MuscleDistributionSection(distribution = state.muscleDistribution)
                }
            }

            // Consistency
            state.consistency?.let { consistency ->
                item {
                    SectionHeader(title = stringResource(R.string.analytics_consistency), icon = "🔥")
                }
                item {
                    ConsistencySection(metrics = consistency)
                }
            }

            // Top Exercises
            if (state.topExercises.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.analytics_top_exercises), icon = "🏋️")
                }
                items(state.topExercises.take(5)) { exercise ->
                    TopExerciseRow(exercise = exercise)
                }
            }
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
private fun WeeklySummarySection(summary: WeeklySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            title = stringResource(R.string.common_volume),
            value = formatVolume(summary.thisWeekVolume),
            change = summary.volumeChange,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.TrendingUp,
        )
        SummaryCard(
            title = stringResource(R.string.progress_workouts_label),
            value = summary.thisWeekWorkouts.toString(),
            change = calculateChange(summary.thisWeekWorkouts, summary.lastWeekWorkouts),
            modifier = Modifier.weight(1f),
            icon = Icons.Default.FitnessCenter,
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SummaryCard(
            title = stringResource(R.string.progress_prs_label),
            value = summary.thisWeekPRs.toString(),
            change = null,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.EmojiEvents,
        )
        SummaryCard(
            title = stringResource(R.string.analytics_last_week),
            value = formatVolume(summary.lastWeekVolume),
            change = null,
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalFireDepartment,
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    change: Double?,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    val changeText = change?.let { ", change: ${formatChange(it)}" } ?: ""
    val summaryCd = stringResource(R.string.analytics_summary_cd, title, value, changeText)
    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = summaryCd
        },
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
            )
            change?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatChange(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it >= 0) AccentGreen else AccentRed,
                )
            }
        }
    }
}

@Composable
private fun VolumeChart(data: List<WeeklyVolumeData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.analytics_last_8_weeks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val maxVolume = data.maxOfOrNull { it.totalVolume } ?: 1.0
                    if (maxVolume == 0.0) return@Canvas
                    
                    val barWidth = size.width / (data.size * 2f)
                    val spacing = barWidth * 0.2f
                    
                    data.forEachIndexed { index, weekData ->
                        val barHeight = (weekData.totalVolume / maxVolume * size.height * 0.8f).toFloat()
                        val x = index * barWidth * 2f + barWidth + spacing
                        val y = size.height - barHeight
                        
                        drawRect(
                            color = AccentGreen,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth - spacing * 2, barHeight),
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = data.firstOrNull()?.weekStartDate?.format(dateFormatter) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = data.lastOrNull()?.weekStartDate?.format(dateFormatter) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MuscleDistributionSection(distribution: List<MuscleGroupDistribution>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Pie chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                PieChart(distribution = distribution)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            distribution.take(5).forEachIndexed { index, muscle ->
                MuscleDistributionRow(
                    muscleGroup = muscle.muscleGroup,
                    percentage = muscle.volumePercentage,
                    color = getMuscleColor(index),
                )
                if (index < distribution.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PieChart(distribution: List<MuscleGroupDistribution>) {
    val colors = listOf(AccentGreen, AccentCyan, AccentAmber, AccentPurple, AccentRed)
    
    Canvas(
        modifier = Modifier.size(160.dp)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        
        var startAngle = -90f
        distribution.take(5).forEachIndexed { index, muscle ->
            val sweepAngle = (muscle.volumePercentage / 100f * 360f).toFloat()
            
            drawArc(
                color = colors[index % colors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            )
            
            startAngle += sweepAngle
        }
        
        // Draw center circle for donut effect
        drawCircle(
            color = SurfaceColor,
            radius = radius * 0.6f,
            center = center,
        )
    }
}

@Composable
private fun MuscleDistributionRow(
    muscleGroup: String,
    percentage: Double,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = muscleGroup,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${percentage.roundToInt()}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ConsistencySection(metrics: ConsistencyMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Consistency Score Circle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center,
            ) {
                ConsistencyScoreCircle(score = metrics.consistencyScore)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ConsistencyStat(
                    label = stringResource(R.string.analytics_current_streak),
                    value = "${metrics.currentStreak} ${if (metrics.currentStreak == 1) stringResource(R.string.analytics_week_singular) else stringResource(R.string.analytics_week_plural)}",
                )
                ConsistencyStat(
                    label = stringResource(R.string.analytics_longest_streak),
                    value = "${metrics.longestStreak} ${if (metrics.longestStreak == 1) stringResource(R.string.analytics_week_singular) else stringResource(R.string.analytics_week_plural)}",
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                ConsistencyStat(
                    label = stringResource(R.string.analytics_avg_per_week),
                    value = stringResource(R.string.analytics_workouts_per_week, metrics.averageWorkoutsPerWeek),
                )
            }
        }
    }
}

@Composable
private fun ConsistencyScoreCircle(score: Int) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(120.dp)
        ) {
            val strokeWidth = 16.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            
            // Background circle
            drawCircle(
                color = SurfaceVariantColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            
            // Progress arc
            val sweepAngle = (score / 100f * 360f)
            drawArc(
                color = AccentGreen,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineLarge,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.common_score),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConsistencyStat(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TopExerciseRow(exercise: TopExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantColor),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${exercise.totalSets} sets • ${exercise.frequency} workouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = formatVolume(exercise.totalVolume),
                style = MaterialTheme.typography.titleMedium,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun formatVolume(volume: Double): String {
    return if (volume >= 1000) {
        String.format("%.1fk", volume / 1000)
    } else {
        volume.roundToInt().toString()
    }
}

private fun formatChange(change: Double): String {
    val sign = if (change >= 0) "+" else ""
    return "$sign${change.roundToInt()}%"
}

private fun calculateChange(current: Int, previous: Int): Double {
    if (previous == 0) return 0.0
    return ((current - previous).toDouble() / previous) * 100
}

private fun getMuscleColor(index: Int): Color {
    val colors = listOf(AccentGreen, AccentCyan, AccentAmber, AccentPurple, AccentRed)
    return colors[index % colors.size]
}
