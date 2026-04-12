package com.gymbro.feature.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.ObserveErrors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToActiveWorkout: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToWorkoutDetail: (String) -> Unit = {},
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
                // Workout Streak Badge
                if (state.workoutStreak > 0) {
                    item {
                        WorkoutStreakBadge(streak = state.workoutStreak)
                    }
                }

                // PR Celebration Banner
                if (state.showPRCelebration && state.recentPR != null) {
                    item {
                        PRCelebrationBanner(
                            pr = state.recentPR,
                            onDismiss = { onEvent(HomeEvent.DismissPRBanner) },
                            weightUnit = weightUnit,
                        )
                    }
                }

                // Quick Start Button — the hero action
                item {
                    QuickStartCard(
                        daysSinceLastWorkout = state.daysSinceLastWorkout,
                        onQuickStart = { onEvent(HomeEvent.QuickStartWorkout) },
                    )
                }

                // Today's Workout (if active plan exists)
                if (state.activePlan != null && state.todayWorkout != null) {
                    item {
                        TodayWorkoutCard(
                            plan = state.activePlan,
                            todayWorkout = state.todayWorkout,
                            onStartWorkout = {
                                onEvent(HomeEvent.StartTodayWorkout(state.todayWorkout.dayNumber))
                            },
                            onViewPrograms = { onEvent(HomeEvent.ViewAllPrograms) },
                        )
                    }
                } else {
                    // No active plan — CTA to create one
                    item {
                        CreateProgramCta(
                            onCreateProgram = { onEvent(HomeEvent.CreateFirstProgram) },
                        )
                    }
                }

                // Recent Workouts
                if (state.recentWorkouts.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.home_recent_workouts),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                    }

                    items(
                        items = state.recentWorkouts,
                        key = { it.workoutId },
                    ) { workout ->
                        RecentWorkoutCard(
                            workout = workout,
                            onClick = { onEvent(HomeEvent.ViewWorkoutDetail(workout.workoutId)) },
                            weightUnit = weightUnit,
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

@Composable
private fun QuickStartCard(
    daysSinceLastWorkout: Int?,
    onQuickStart: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_start_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AccentGreen.copy(alpha = 0.15f),
                            AccentCyan.copy(alpha = 0.10f),
                        )
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val subtitle = when {
                    daysSinceLastWorkout == null -> stringResource(R.string.home_quick_start_first)
                    daysSinceLastWorkout == 0 -> stringResource(R.string.home_quick_start_today)
                    daysSinceLastWorkout == 1 -> stringResource(R.string.home_quick_start_yesterday)
                    else -> stringResource(R.string.home_quick_start_days_ago, daysSinceLastWorkout)
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.home_quick_start_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onQuickStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("quick_start_button"),
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
                        text = stringResource(R.string.home_quick_start),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayWorkoutCard(
    plan: WorkoutPlan,
    todayWorkout: WorkoutDay,
    onStartWorkout: () -> Unit,
    onViewPrograms: () -> Unit,
) {
    val viewAllProgramsLabel = stringResource(R.string.home_cd_view_all_programs)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("today_workout_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // TODAY badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_today_badge),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = AccentGreen,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_todays_workout),
                        style = MaterialTheme.typography.labelLarge,
                        color = AccentGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = todayWorkout.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onStartWorkout,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.home_cd_start_workout),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.home_start),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoChip(
                    label = stringResource(R.string.programs_exercises_count, todayWorkout.exercises.size),
                    color = AccentCyan,
                )
                InfoChip(
                    label = stringResource(R.string.programs_day_number, todayWorkout.dayNumber),
                    color = AccentGreen,
                )
            }

            // Show first 3 exercises as preview
            if (todayWorkout.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                todayWorkout.exercises.take(3).forEach { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${exercise.exerciseName} — ${exercise.sets} × ${exercise.repsRange}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (todayWorkout.exercises.size > 3) {
                    Text(
                        text = stringResource(
                            R.string.programs_more_exercises,
                            todayWorkout.exercises.size - 3,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 22.dp, top = 2.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_view_all_programs),
                style = MaterialTheme.typography.labelMedium,
                color = AccentCyan,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .semantics { contentDescription = viewAllProgramsLabel }
                    .clickable(onClick = onViewPrograms)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun CreateProgramCta(
    onCreateProgram: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCreateProgram)
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
private fun RecentWorkoutCard(
    workout: RecentWorkoutItem,
    onClick: () -> Unit,
    weightUnit: UserPreferences.WeightUnit = UserPreferences.WeightUnit.KG,
) {
    val dateFormatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
    val formattedDate = remember(workout.date) {
        Instant.ofEpochMilli(workout.date)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    val workoutDescription = stringResource(
        R.string.home_cd_recent_workout,
        formattedDate,
        workout.exerciseCount,
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = workoutDescription }
            .clickable(onClick = onClick),
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatLabel(
                        icon = Icons.Default.FitnessCenter,
                        text = stringResource(R.string.home_exercises_count, workout.exerciseCount),
                    )
                    StatLabel(
                        icon = Icons.Default.Timer,
                        text = formatDuration(workout.durationSeconds),
                    )
                }
            }

            Text(
                text = "%.0f %s".format(workout.totalVolume, if (weightUnit == UserPreferences.WeightUnit.LBS) "lb" else "kg"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = AccentGreen,
            )
        }
    }
}

@Composable
private fun StatLabel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
private fun WorkoutStreakBadge(streak: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("workout_streak_badge"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.15f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = stringResource(R.string.streak_badge, streak),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PRCelebrationBanner(
    pr: PersonalRecord,
    onDismiss: () -> Unit,
    weightUnit: UserPreferences.WeightUnit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pr_celebration_banner"),
        colors = CardDefaults.cardColors(
            containerColor = AccentGreen.copy(alpha = 0.15f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = pr.type.emoji,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.pr_banner_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val formattedValue = when (pr.type) {
                        com.gymbro.core.model.RecordType.MAX_WEIGHT -> {
                            "%.1f ${weightUnit.symbol}".format(pr.value)
                        }
                        com.gymbro.core.model.RecordType.MAX_REPS -> {
                            "${pr.value.toInt()} reps"
                        }
                        com.gymbro.core.model.RecordType.MAX_VOLUME -> {
                            "%.1f ${weightUnit.symbol}".format(pr.value)
                        }
                        com.gymbro.core.model.RecordType.MAX_E1RM -> {
                            "%.1f ${weightUnit.symbol}".format(pr.value)
                        }
                    }
                    Text(
                        text = stringResource(R.string.pr_banner, pr.exerciseName, formattedValue),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface HomePreferencesEntryPoint {
    fun userPreferences(): UserPreferences
}
