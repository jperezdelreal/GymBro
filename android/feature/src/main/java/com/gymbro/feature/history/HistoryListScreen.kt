package com.gymbro.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymbro.core.R
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val AccentGreenStart = Color(0xFF00FF87)
private val AccentGreenEnd = Color(0xFF00D9B5)
private val AccentCyanStart = Color(0xFF00D4FF)
private val AccentCyanEnd = Color(0xFF0091FF)
private val AccentAmberStart = Color(0xFFFFB800)
private val AccentAmberEnd = Color(0xFFFF8A00)

private val SurfaceDark = Color(0xFF0A0A0A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToActiveWorkout: () -> Unit = {},
    viewModel: HistoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = SurfaceDark,
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when {
                state.isLoading -> {
                    FullScreenLoading(message = stringResource(R.string.history_loading))
                }
                state.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Close,
                        title = stringResource(R.string.history_error_title),
                        subtitle = state.error ?: "Unknown error",
                        actionText = stringResource(R.string.action_retry),
                        onActionClick = { viewModel.onIntent(HistoryListIntent.Retry) },
                    )
                }
                state.workouts.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.history_empty_title),
                        subtitle = stringResource(R.string.history_empty_subtitle),
                        actionText = stringResource(R.string.history_empty_cta),
                        onActionClick = onNavigateToActiveWorkout,
                    )
                }
                else -> {
                    HistoryListContent(
                        groupedWorkouts = state.groupedWorkouts,
                        onWorkoutClick = { workoutId ->
                            onNavigateToDetail(workoutId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryListContent(
    groupedWorkouts: List<WorkoutGroup>,
    onWorkoutClick: (String) -> Unit,
) {
    var itemIndex = 0
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groupedWorkouts.forEach { group ->
            item {
                val currentIndex = itemIndex++
                MonthHeader(
                    monthYear = group.monthYear,
                    index = currentIndex,
                )
            }

            itemsIndexed(group.workouts) { workoutIndex, workout ->
                val currentIndex = itemIndex++
                WorkoutCard(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.workoutId) },
                    index = currentIndex,
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MonthHeader(
    monthYear: String,
    index: Int,
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        GlassmorphicCard(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = monthYear,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(AccentGreenStart, AccentCyanStart)
                        )
                    )
                    .padding(16.dp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutListItem,
    onClick: () -> Unit,
    index: Int,
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    
    val workoutDate = Instant.ofEpochMilli(workout.date)
        .atZone(ZoneId.systemDefault())
    val date = workoutDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    val relativeTime = getRelativeTime(workoutDate.toLocalDate())
    
    val accentColor = getAccentColorForMuscleGroups(workout.muscleGroups)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        GlassmorphicCard(
            onClick = onClick,
            accentColor = accentColor,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                    if (workout.prCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(AccentAmberStart, AccentAmberEnd)
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = stringResource(R.string.history_prs),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "${workout.prCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatChip(
                        icon = Icons.Default.Timer,
                        label = formatDuration(workout.durationSeconds),
                        gradientColors = listOf(AccentCyanStart, AccentCyanEnd),
                    )
                    StatChip(
                        icon = Icons.Default.FitnessCenter,
                        label = stringResource(R.string.history_exercises_count, workout.exerciseCount),
                        gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatChip(
                        icon = Icons.Default.FitnessCenter,
                        label = "${workout.totalVolume.toInt()} kg",
                        gradientColors = listOf(AccentAmberStart, AccentAmberEnd),
                    )
                }

                if (workout.muscleGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        workout.muscleGroups.take(3).forEach { muscleGroup ->
                            MuscleGroupTag(muscleGroup = muscleGroup)
                        }
                        if (workout.muscleGroups.size > 3) {
                            Text(
                                text = "+${workout.muscleGroups.size - 3}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getRelativeTime(workoutDate: LocalDate): String {
    val today = LocalDate.now()
    val daysBetween = Duration.between(workoutDate.atStartOfDay(), today.atStartOfDay()).toDays()
    
    return when {
        daysBetween == 0L -> stringResource(R.string.history_today)
        daysBetween == 1L -> stringResource(R.string.history_yesterday)
        daysBetween < 7 -> stringResource(R.string.history_days_ago, daysBetween.toInt())
        daysBetween < 14 -> stringResource(R.string.history_one_week_ago)
        daysBetween < 30 -> stringResource(R.string.history_weeks_ago, (daysBetween / 7).toInt())
        else -> stringResource(R.string.history_months_ago, (daysBetween / 30).toInt())
    }
}

private fun getAccentColorForMuscleGroups(muscleGroups: Set<MuscleGroup>): Color? {
    val primaryMuscle = muscleGroups.firstOrNull() ?: return null
    
    return when (primaryMuscle) {
        MuscleGroup.CHEST -> AccentGreenStart
        MuscleGroup.BACK -> AccentCyanStart
        MuscleGroup.SHOULDERS -> AccentAmberStart
        MuscleGroup.BICEPS -> Color(0xFF00D4FF)
        MuscleGroup.TRICEPS -> Color(0xFFFF3B30)
        MuscleGroup.QUADRICEPS -> AccentGreenStart
        MuscleGroup.HAMSTRINGS -> AccentCyanStart
        MuscleGroup.GLUTES -> Color(0xFFFF8A00)
        MuscleGroup.CALVES -> AccentAmberStart
        MuscleGroup.CORE -> Color(0xFFFFB800)
        MuscleGroup.FOREARMS -> Color(0xFF00E5FF)
        MuscleGroup.FULL_BODY -> AccentGreenStart
    }
}

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    gradientColors: List<Color>,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(colors = gradientColors.map { it.copy(alpha = 0.2f) })
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = gradientColors.first(),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MuscleGroupTag(muscleGroup: MuscleGroup) {
    val gradientColors = getMuscleGroupGradient(muscleGroup)
    
    Text(
        text = muscleGroup.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(colors = gradientColors.map { it.copy(alpha = 0.3f) })
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun getMuscleGroupGradient(muscleGroup: MuscleGroup): List<Color> {
    return when (muscleGroup) {
        MuscleGroup.CHEST -> listOf(AccentGreenStart, AccentGreenEnd)
        MuscleGroup.BACK -> listOf(AccentCyanStart, AccentCyanEnd)
        MuscleGroup.SHOULDERS -> listOf(AccentAmberStart, AccentAmberEnd)
        MuscleGroup.BICEPS -> listOf(Color(0xFF00D4FF), Color(0xFF0091FF))
        MuscleGroup.TRICEPS -> listOf(Color(0xFFFF3B30), Color(0xFFFF1744))
        MuscleGroup.QUADRICEPS -> listOf(AccentGreenStart, AccentGreenEnd)
        MuscleGroup.HAMSTRINGS -> listOf(AccentCyanStart, AccentCyanEnd)
        MuscleGroup.GLUTES -> listOf(AccentAmberStart, AccentAmberEnd)
        MuscleGroup.CALVES -> listOf(Color(0xFFFFB800), Color(0xFFFF8A00))
        MuscleGroup.CORE -> listOf(Color(0xFFFFB800), Color(0xFFFF8A00))
        MuscleGroup.FOREARMS -> listOf(AccentCyanStart, AccentCyanEnd)
        MuscleGroup.FULL_BODY -> listOf(AccentGreenStart, AccentCyanStart)
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}
