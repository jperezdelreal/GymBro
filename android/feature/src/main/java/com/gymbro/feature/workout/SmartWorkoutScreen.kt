package com.gymbro.feature.workout

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymbro.core.model.Exercise

private val AccentGreen = Color(0xFF00FF87)
private val CardBackground = Color(0xFF1E1E1E)
private val SurfaceDark = Color(0xFF121212)

@Composable
fun SmartWorkoutRoute(
    onNavigateBack: () -> Unit = {},
    onStartWorkout: (List<Exercise>) -> Unit = {},
    viewModel: SmartWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SmartWorkoutEffect.NavigateBack -> onNavigateBack()
                is SmartWorkoutEffect.StartWorkoutWithExercises -> onStartWorkout(effect.exercises)
            }
        }
    }

    SmartWorkoutScreen(
        state = state,
        onEvent = viewModel::handleEvent,
    )
}

@Composable
internal fun SmartWorkoutScreen(
    state: SmartWorkoutState,
    onEvent: (SmartWorkoutEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onEvent(SmartWorkoutEvent.NavigateBack) }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = "Smart Workout",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            )
            IconButton(
                onClick = { onEvent(SmartWorkoutEvent.RegenerateWorkout) },
                enabled = !state.isGenerating
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Regenerate",
                    tint = if (state.isGenerating) Color.Gray else AccentGreen,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating smart workout...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            state.error != null -> {
                ErrorCard(
                    error = state.error,
                    onRetry = { onEvent(SmartWorkoutEvent.RegenerateWorkout) },
                )
            }
            else -> {
                // Reasoning Card
                ReasoningCard(
                    reasoning = state.reasoning,
                    recoveryScore = state.recoveryScore,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercises List
                Text(
                    text = "Suggested Exercises",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                state.exercises.forEach { exercise ->
                    ExerciseCard(exercise = exercise)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onEvent(SmartWorkoutEvent.StartWorkout) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGreen,
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = state.exercises.isNotEmpty(),
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start This Workout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onEvent(SmartWorkoutEvent.RegenerateWorkout) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentGreen,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Regenerate", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun ReasoningCard(
    reasoning: String,
    recoveryScore: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Workout Plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                RecoveryBadge(score = recoveryScore)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun RecoveryBadge(score: Int) {
    val (color, label) = when {
        score >= 80 -> AccentGreen to "Ready"
        score >= 60 -> Color(0xFFFFA500) to "Good"
        score >= 40 -> Color(0xFFFFD700) to "Moderate"
        else -> Color(0xFFFF6B6B) to "Light"
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExerciseCard(exercise: SuggestedExerciseUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentGreen,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${exercise.targetSets} sets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "${exercise.targetReps} reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }

            if (exercise.suggestedWeight != null || exercise.progressionHint != null) {
                Spacer(modifier = Modifier.height(12.dp))
                if (exercise.suggestedWeight != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Suggested: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = exercise.suggestedWeight,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (exercise.progressionHint != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 ${exercise.progressionHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D1A1A)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Failed to Generate Workout",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFF6B6B),
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Text(text = "Retry", fontWeight = FontWeight.Bold)
            }
        }
    }
}
