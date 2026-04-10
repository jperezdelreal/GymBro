package com.gymbro.feature.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.WorkoutTemplate
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.ObserveErrors
import androidx.compose.ui.res.stringResource
import com.gymbro.core.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)

@Composable
fun ProgramsRoute(
    viewModel: ProgramsViewModel = hiltViewModel(),
    onNavigateToCreateTemplate: (String?) -> Unit = {},
    onNavigateToActiveWorkout: (WorkoutTemplate) -> Unit = {},
    onNavigateToPlanDayDetail: (Int) -> Unit = {},
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveErrors(
        errorFlow = viewModel.errorEvents,
        snackbarHostState = snackbarHostState
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProgramsEffect.NavigateToCreateTemplate -> {
                    onNavigateToCreateTemplate(effect.templateId)
                }
                is ProgramsEffect.NavigateToActiveWorkout -> {
                    onNavigateToActiveWorkout(effect.template)
                }
                is ProgramsEffect.NavigateToPlanDayDetail -> {
                    onNavigateToPlanDayDetail(effect.dayNumber)
                }
            }
        }
    }

    ProgramsScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgramsScreen(
    state: ProgramsState,
    onEvent: (ProgramsEvent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.programs_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                actions = {
                    IconButton(onClick = { onEvent(ProgramsEvent.CreateTemplateClicked) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.programs_create_template),
                            tint = AccentGreen,
                        )
                    }
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
        if (state.isLoading && state.templates.isEmpty() && state.activePlan == null) {
            FullScreenLoading()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Active Plan Section
                item {
                    Text(
                        text = stringResource(R.string.programs_active_plan_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (state.activePlan != null) {
                    item {
                        ActivePlanCard(
                            plan = state.activePlan,
                            onViewDay = { dayNumber ->
                                onEvent(ProgramsEvent.ViewPlanDay(dayNumber))
                            },
                        )
                    }
                } else {
                    item {
                        GenerateNewPlanCard(
                            isGenerating = state.isGeneratingPlan,
                            onGenerate = { onEvent(ProgramsEvent.GenerateNewPlan) },
                        )
                    }
                }

                // Templates Section
                if (state.templates.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.programs_templates_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.templates, key = { it.id.toString() }) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onEvent(ProgramsEvent.TemplateClicked(template)) },
                            onStartWorkout = { onEvent(ProgramsEvent.StartWorkoutFromTemplate(template)) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateCard(
    template: WorkoutTemplate,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (template.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = template.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                
                IconButton(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AccentGreen),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.programs_start_workout),
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InfoChip(
                    label = stringResource(R.string.programs_exercises_count, template.exercises.size),
                    color = AccentCyan,
                )
                
                if (template.isBuiltIn) {
                    InfoChip(
                        label = stringResource(R.string.programs_built_in),
                        color = AccentGreen,
                    )
                }
            }

            if (template.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val muscleGroups = template.exercises.map { it.muscleGroup }.distinct()
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    muscleGroups.forEach { muscle ->
                        MuscleGroupChip(muscleGroup = muscle)
                    }
                }
            }

            template.lastUsedAt?.let { lastUsed ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.programs_last_used, formatDate(lastUsed)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

@Composable
private fun MuscleGroupChip(muscleGroup: MuscleGroup) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = muscleGroup.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GenerateNewPlanCard(
    isGenerating: Boolean,
    onGenerate: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGenerating, onClick = onGenerate),
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
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = AccentGreen,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isGenerating) {
                    stringResource(R.string.programs_generating_plan)
                } else {
                    stringResource(R.string.programs_generate_new_plan)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.programs_generate_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivePlanCard(
    plan: com.gymbro.core.model.WorkoutPlan,
    onViewDay: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = plan.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InfoChip(
                    label = stringResource(R.string.programs_weeks_count, plan.weeks),
                    color = AccentCyan,
                )
                InfoChip(
                    label = stringResource(R.string.programs_days_per_week, plan.daysPerWeek),
                    color = AccentGreen,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.programs_workout_days),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            plan.workoutDays.forEach { day ->
                WorkoutDayItem(
                    day = day,
                    onClick = { onViewDay(day.dayNumber) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WorkoutDayItem(
    day: com.gymbro.core.model.WorkoutDay,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.programs_day_number, day.dayNumber),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = AccentGreen,
                )
                Text(
                    text = day.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Text(
                    text = stringResource(R.string.programs_exercises_in_day, day.exercises.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            day.exercises.take(3).forEach { exercise ->
                Text(
                    text = "• ${exercise.exerciseName} - ${exercise.sets}×${exercise.repsRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            
            if (day.exercises.size > 3) {
                Text(
                    text = stringResource(R.string.programs_more_exercises, day.exercises.size - 3),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatDate(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
