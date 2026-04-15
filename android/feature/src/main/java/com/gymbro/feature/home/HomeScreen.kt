package com.gymbro.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.R
import com.gymbro.core.model.PlateauAlert
import com.gymbro.core.model.PlateauSeverity
import com.gymbro.core.model.PlateauType
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.WorkoutPlanGenerator
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.ObserveErrors

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToActiveWorkout: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToWorkoutDetail: (String) -> Unit = {},
    onNavigateToCoach: (String) -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val userPreferences = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            HomePreferencesEntryPoint::class.java,
        ).userPreferences()
    }
    val weightUnit = userPreferences.weightUnit.collectAsStateWithLifecycle(initialValue = UserPreferences.WeightUnit.KG)

    ObserveErrors(
        errorFlow = viewModel.errorEvents,
        snackbarHostState = snackbarHostState,
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
                is HomeEffect.NavigateToPrograms -> onNavigateToPrograms()
                is HomeEffect.NavigateToWorkoutDetail -> onNavigateToWorkoutDetail(effect.workoutId)
                is HomeEffect.NavigateToCoachWithContext -> onNavigateToCoach(effect.context)
            }
        }
    }

    HomeScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        weightUnit = weightUnit.value,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    weightUnit: UserPreferences.WeightUnit = UserPreferences.WeightUnit.KG,
) {
    val haptic = LocalHapticFeedback.current
    val hasTodayWorkout = state.activePlan != null && state.todayWorkout != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (hasTodayWorkout) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onEvent(
                                HomeEvent.StartTodayWorkout(
                                    state.todayWorkout!!.dayNumber,
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_workout_bottom_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.home_cd_start_workout),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.home_start_workout),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading && state.recentWorkouts.isEmpty() && state.activePlan == null) {
            FullScreenLoading()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.activePlan != null && state.todayWorkout != null) {
                    // Hero: Workout Header
                    item {
                        WorkoutHeader(
                            dayName = state.todayWorkout.name,
                            planName = state.activePlan.name,
                            exerciseCount = state.todayWorkout.exercises.size,
                            canSwapDay = state.activePlan.workoutDays.size > 1,
                            onSwapDay = { onEvent(HomeEvent.SwapDay) },
                            modifier = Modifier.testTag("today_workout_card"),
                        )
                    }

                    // Context Chips — tappable duration selector
                    item {
                        DurationChip(
                            exercises = state.todayWorkout.exercises,
                            selectedMinutes = state.targetDurationMinutes,
                            isAdjusting = state.isAdjustingDuration,
                            onSelectDuration = { minutes -> onEvent(HomeEvent.SetTargetDuration(minutes)) },
                        )
                    }

                    // Compact Plateau Alerts
                    if (state.plateauAlerts.isNotEmpty()) {
                        items(
                            items = state.plateauAlerts,
                            key = { it.exerciseId },
                        ) { alert ->
                            CompactPlateauAlert(
                                alert = alert,
                                onDismiss = { onEvent(HomeEvent.DismissPlateauAlert(alert.exerciseId)) },
                                onTalkToCoach = { onEvent(HomeEvent.OpenCoachForPlateau(alert)) },
                            )
                        }
                    }

                    // Exercise Preview Cards
                    items(
                        items = state.todayWorkout.exercises,
                        key = { it.id },
                    ) { exercise ->
                        ExercisePreviewCard(exercise = exercise)
                    }

                    // Bottom spacer for sticky button
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                } else {
                    // No active plan — CTA to create one
                    item {
                        CreateProgramCta(
                            onCreateProgram = { onEvent(HomeEvent.CreateFirstProgram) },
                        )
                    }
                }
            }
        }
    }

    if (state.showNoPlanDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(HomeEvent.DismissNoPlanDialog) },
            title = {
                Text(
                    text = stringResource(R.string.home_no_plan_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(text = stringResource(R.string.home_no_plan_message))
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(HomeEvent.NoPlanGoToPrograms) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.home_no_plan_go_to_programs),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(HomeEvent.DismissNoPlanDialog) }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

// ─── WorkoutHeader: Hero section showing today's workout day ───
@Composable
private fun WorkoutHeader(
    dayName: String,
    planName: String,
    exerciseCount: Int,
    canSwapDay: Boolean,
    onSwapDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_exercises_subtitle, planName, exerciseCount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (canSwapDay) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwapDay()
                        })
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.SwapHoriz,
                        contentDescription = stringResource(R.string.home_change_day),
                        modifier = Modifier.size(18.dp),
                        tint = AccentCyan,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_change_day),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentCyan,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ─── DurationChip: Interactive duration selector ───
@Composable
private fun DurationChip(
    exercises: List<PlannedExercise>,
    selectedMinutes: Int,
    isAdjusting: Boolean,
    onSelectDuration: (Int) -> Unit,
) {
    val durations = listOf(30, 45, 60, 90)
    var expanded by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Main chip — tappable to expand options
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentCyan.copy(alpha = 0.15f))
                .border(1.5.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isAdjusting) {
                        stringResource(R.string.home_adjusting_duration)
                    } else {
                        stringResource(R.string.home_target_duration, selectedMinutes)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = AccentCyan,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = AccentCyan.copy(alpha = 0.8f),
                )
            }
        }

        // Expanded: show alternative duration options
        if (expanded) {
            durations.filter { it != selectedMinutes }.forEach { minutes ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            onSelectDuration(minutes)
                            expanded = false
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${minutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

// ─── ExercisePreviewCard: Enriched card for each exercise ───
@Composable
private fun ExercisePreviewCard(
    exercise: PlannedExercise,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Exercise icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = AccentGreen,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exerciseName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_sets_reps, exercise.sets, exercise.repsRange),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── CompactPlateauAlert: Banner-style plateau warning ───
@Composable
private fun CompactPlateauAlert(
    alert: PlateauAlert,
    onDismiss: () -> Unit,
    onTalkToCoach: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val alertColor = when (alert.severity) {
        PlateauSeverity.MILD -> Color(0xFFFFA726)
        PlateauSeverity.MODERATE -> Color(0xFFFF9800)
        PlateauSeverity.SEVERE -> Color(0xFFEF5350)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(alertColor.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = alertColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (alert.type) {
                PlateauType.STAGNATION -> stringResource(
                    R.string.home_plateau_stagnation_weeks,
                    alert.exerciseName,
                    alert.weeksDuration,
                )
                PlateauType.REGRESSION -> stringResource(
                    R.string.home_plateau_regression_weeks,
                    alert.exerciseName,
                    alert.weeksDuration,
                )
            },
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.home_plateau_talk_to_coach),
            style = MaterialTheme.typography.labelSmall,
            color = alertColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTalkToCoach()
                }
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDismiss()
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_dismiss),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun CreateProgramCta(
    onCreateProgram: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCreateProgram()
            })
            .testTag("create_program_cta"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = stringResource(R.string.home_cd_create_program),
                modifier = Modifier.size(48.dp),
                tint = AccentGreen,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.home_create_program_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_create_program_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    color: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = color,
        )
    }
}

/**
 * Estimates total workout duration in minutes using the same time constants as
 * [WorkoutPlanGenerator.estimateExerciseTimeSeconds]. Because [PlannedExercise]
 * does not carry an [ExerciseCategory], we average the compound and isolation
 * rep-duration and transition-time values for a reasonable mid-range estimate.
 */
private fun estimateWorkoutDurationMinutes(exercises: List<PlannedExercise>): Int {
    if (exercises.isEmpty()) return 0

    val avgRepDuration = (WorkoutPlanGenerator.REP_DURATION_COMPOUND +
            WorkoutPlanGenerator.REP_DURATION_ISOLATION) / 2              // 4 s/rep
    val avgTransitionTime = (WorkoutPlanGenerator.TRANSITION_TIME_COMPOUND +
            WorkoutPlanGenerator.TRANSITION_TIME_ISOLATION) / 2           // 105 s

    var totalSeconds = WorkoutPlanGenerator.WARMUP_TIME_SECONDS +
            WorkoutPlanGenerator.COOLDOWN_TIME_SECONDS                    // 8 min overhead

    for (ex in exercises) {
        val midReps = parseRepsRangeMidpoint(ex.repsRange)
        val timePerSet = (midReps * avgRepDuration) + ex.restSeconds
        totalSeconds += (ex.sets * timePerSet) + avgTransitionTime
    }
    return ((totalSeconds + 30) / 60)   // round to nearest minute
}

private fun parseRepsRangeMidpoint(repsRange: String): Int {
    val parts = repsRange.split("-")
    return if (parts.size == 2) {
        val low = parts[0].trim().toIntOrNull() ?: 10
        val high = parts[1].trim().toIntOrNull() ?: 12
        (low + high) / 2
    } else {
        repsRange.trim().toIntOrNull() ?: 10
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface HomePreferencesEntryPoint {
    fun userPreferences(): UserPreferences
}
