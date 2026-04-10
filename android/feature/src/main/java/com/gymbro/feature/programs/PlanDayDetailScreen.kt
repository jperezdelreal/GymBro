package com.gymbro.feature.programs

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading

private val AccentGreen = Color(0xFF00FF87)

@Composable
fun PlanDayDetailRoute(
    dayNumber: Int,
    viewModel: PlanDayDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToActiveWorkout: () -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(dayNumber) {
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(dayNumber))
    }

    PlanDayDetailScreen(
        state = state.value,
        dayNumber = dayNumber,
        onNavigateBack = onNavigateBack,
        onStartWorkout = onNavigateToActiveWorkout,
        onRetry = { viewModel.onIntent(PlanDayDetailIntent.Retry) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanDayDetailScreen(
    state: PlanDayDetailState,
    dayNumber: Int,
    onNavigateBack: () -> Unit,
    onStartWorkout: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = if (state.workoutDay != null) {
                        stringResource(
                            R.string.programs_day_detail_title,
                            state.workoutDay.dayNumber,
                            state.workoutDay.name,
                        )
                    } else {
                        stringResource(R.string.programs_day_detail_title, dayNumber, "")
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        when {
            state.isLoading -> {
                FullScreenLoading(
                    message = stringResource(R.string.programs_loading_day),
                    modifier = Modifier.padding(paddingValues),
                )
            }
            state.error != null -> {
                EmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = stringResource(R.string.programs_day_error_title),
                    subtitle = state.error,
                    actionText = stringResource(R.string.common_retry),
                    onActionClick = onRetry,
                    modifier = Modifier.padding(paddingValues),
                )
            }
            state.workoutDay != null -> {
                PlanDayContent(
                    day = state.workoutDay,
                    onStartWorkout = onStartWorkout,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun PlanDayContent(
    day: WorkoutDay,
    onStartWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exerciseCount = day.exercises.size
    val totalSets = day.exercises.sumOf { it.sets }
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        // Summary header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryItem(
                        label = stringResource(R.string.programs_exercises_label),
                        value = exerciseCount.toString(),
                        contentDesc = stringResource(R.string.programs_exercises_count_desc, exerciseCount),
                    )
                    SummaryItem(
                        label = stringResource(R.string.programs_total_sets_label),
                        value = totalSets.toString(),
                        contentDesc = stringResource(R.string.programs_total_sets_desc, totalSets),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Exercise cards
        items(
            items = day.exercises,
            key = { it.id },
        ) { exercise ->
            ExerciseCard(
                exercise = exercise,
                modifier = Modifier.animateItem(),
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Start workout button
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartWorkout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Start this workout"
                    },
                colors = CardDefaults.cardColors(
                    containerColor = AccentGreen,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.programs_start_this_workout),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.Black,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    contentDesc: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = contentDesc
        },
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = AccentGreen,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExerciseCard(
    exercise: PlannedExercise,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val exerciseDesc = stringResource(
        R.string.programs_exercise_accessibility,
        exercise.exerciseName,
        exercise.sets,
        exercise.repsRange,
        exercise.restSeconds,
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = exerciseDesc
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ExerciseDetailItem(
                    label = stringResource(R.string.workout_sets),
                    value = stringResource(
                        R.string.programs_exercise_sets_reps,
                        exercise.sets,
                        exercise.repsRange,
                    ),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    ExerciseDetailItem(
                        label = stringResource(R.string.workout_rest_timer),
                        value = stringResource(
                            R.string.programs_rest_time,
                            exercise.restSeconds,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = AccentGreen,
        )
    }
}
