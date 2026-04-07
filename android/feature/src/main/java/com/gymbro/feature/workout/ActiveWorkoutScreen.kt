package com.gymbro.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.Exercise

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentAmber = Color(0xFFFFAB00)
private val AccentRed = Color(0xFFCF6679)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceDark = Color(0xFF0A0A0A)

@Composable
fun ActiveWorkoutRoute(
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
    onNavigateToExercisePicker: () -> Unit = {},
    onNavigateToSummary: (Long, Double, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    onNavigateBack: () -> Unit = {},
    pickedExercise: Exercise? = null,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    // Handle picked exercise
    LaunchedEffect(pickedExercise) {
        if (pickedExercise != null) {
            viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(pickedExercise))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ActiveWorkoutEffect.ShowExercisePicker -> onNavigateToExercisePicker()
                is ActiveWorkoutEffect.NavigateToSummary -> onNavigateToSummary(
                    effect.durationSeconds,
                    effect.totalVolume,
                    effect.totalSets,
                    effect.exerciseCount,
                    effect.prsCount,
                )
                is ActiveWorkoutEffect.NavigateBack -> onNavigateBack()
                is ActiveWorkoutEffect.RestTimerFinished -> { /* vibration/sound handled externally */ }
            }
        }
    }

    ActiveWorkoutScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    state: ActiveWorkoutState,
    onEvent: (ActiveWorkoutEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Active Workout",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = formatDuration(state.elapsedSeconds),
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentCyan,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { onEvent(ActiveWorkoutEvent.DiscardWorkout) }) {
                            Icon(Icons.Default.Close, contentDescription = "Discard")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { onEvent(ActiveWorkoutEvent.CompleteWorkout) },
                            enabled = state.totalSets > 0 && !state.isCompleting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = Color.Black,
                                disabledContainerColor = AccentGreen.copy(alpha = 0.3f),
                            ),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Finish", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                )
            },
            containerColor = SurfaceDark,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // Stats bar
                WorkoutStatsBar(
                    elapsedSeconds = state.elapsedSeconds,
                    totalVolume = state.totalVolume,
                    totalSets = state.totalSets,
                )

                // Exercise list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = state.exercises,
                        key = { index, ex -> "${ex.exercise.id}_$index" },
                    ) { exerciseIndex, exerciseUi ->
                        ExerciseCard(
                            exerciseUi = exerciseUi,
                            exerciseIndex = exerciseIndex,
                            onEvent = onEvent,
                        )
                    }

                    item {
                        AddExerciseButton(
                            onClick = { onEvent(ActiveWorkoutEvent.AddExerciseClicked) },
                        )
                    }

                    // Bottom spacer for rest timer overlay
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Rest timer overlay
        AnimatedVisibility(
            visible = state.isRestTimerActive,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RestTimerBar(
                remainingSeconds = state.restTimerSeconds,
                totalSeconds = state.restTimerTotal,
                onSkip = { onEvent(ActiveWorkoutEvent.SkipRestTimer) },
                onAdjust = { delta -> onEvent(ActiveWorkoutEvent.AdjustRestTimer(delta)) },
            )
        }
    }
}

@Composable
private fun WorkoutStatsBar(
    elapsedSeconds: Long,
    totalVolume: Double,
    totalSets: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatItem(label = "Duration", value = formatDuration(elapsedSeconds), color = AccentCyan)
        StatItem(label = "Volume", value = "${totalVolume.toInt()} kg", color = AccentGreen)
        StatItem(label = "Sets", value = "$totalSets", color = AccentAmber)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ExerciseCard(
    exerciseUi: WorkoutExerciseUi,
    exerciseIndex: Int,
    onEvent: (ActiveWorkoutEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(16.dp),
    ) {
        // Exercise header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = exerciseUi.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onEvent(ActiveWorkoutEvent.RemoveExercise(exerciseIndex)) },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove exercise",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = exerciseUi.exercise.muscleGroup.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Column headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Text("SET", style = setHeaderStyle(), modifier = Modifier.width(36.dp))
            Text("KG", style = setHeaderStyle(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("REPS", style = setHeaderStyle(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Text("RPE", style = setHeaderStyle(), modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(40.dp)) // for complete button
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Set rows
        exerciseUi.sets.forEachIndexed { setIndex, setUi ->
            SetRow(
                setUi = setUi,
                exerciseIndex = exerciseIndex,
                setIndex = setIndex,
                onEvent = onEvent,
            )
            if (setIndex < exerciseUi.sets.lastIndex) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add set button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onEvent(ActiveWorkoutEvent.AddSet(exerciseIndex)) }
                .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Set", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SetRow(
    setUi: WorkoutSetUi,
    exerciseIndex: Int,
    setIndex: Int,
    onEvent: (ActiveWorkoutEvent) -> Unit,
) {
    val rowBackground = when {
        setUi.isCompleted -> AccentGreen.copy(alpha = 0.08f)
        setUi.isWarmup -> AccentAmber.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(rowBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Set number + warmup indicator
        Box(
            modifier = Modifier
                .width(36.dp)
                .clickable { onEvent(ActiveWorkoutEvent.ToggleWarmup(exerciseIndex, setIndex)) },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (setUi.isWarmup) "W" else "${setUi.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (setUi.isWarmup) AccentAmber else Color.White,
            )
        }

        // Weight
        CompactNumberField(
            value = setUi.weight,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetWeight(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Reps
        CompactNumberField(
            value = setUi.reps,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetReps(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(4.dp))

        // RPE
        CompactNumberField(
            value = setUi.rpe,
            onValueChange = { onEvent(ActiveWorkoutEvent.UpdateSetRpe(exerciseIndex, setIndex, it)) },
            enabled = !setUi.isCompleted,
            modifier = Modifier.width(48.dp),
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Complete button
        if (setUi.isCompleted) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, AccentGreen.copy(alpha = 0.5f), CircleShape)
                    .clickable { onEvent(ActiveWorkoutEvent.CompleteSet(exerciseIndex, setIndex)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Complete set",
                    tint = AccentGreen.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = { input ->
            // Allow numbers and one decimal point
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                onValueChange(input)
            }
        },
        enabled = enabled,
        modifier = modifier.height(40.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = AccentGreen,
        ),
        shape = RoundedCornerShape(6.dp),
    )
}

@Composable
private fun AddExerciseButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentGreen.copy(alpha = 0.12f),
            contentColor = AccentGreen,
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(vertical = 14.dp),
    ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Exercise", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RestTimerBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
) {
    val timerColor = when {
        remainingSeconds <= 10 -> AccentRed
        remainingSeconds <= 30 -> AccentAmber
        else -> AccentCyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(SurfaceCard)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rest Timer", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDuration(remainingSeconds.toLong()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = timerColor,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(timerColor),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            OutlinedButton(
                onClick = { onAdjust(-30) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("-30s")
            }
            OutlinedButton(
                onClick = { onAdjust(30) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("+30s")
            }
            Button(
                onClick = onSkip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Skip", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun setHeaderStyle() = MaterialTheme.typography.labelSmall.copy(
    color = Color.White.copy(alpha = 0.4f),
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
)

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
