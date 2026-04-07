package com.gymbro.feature.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GymBroCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val AccentGreen = Color(0xFF00FF87)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentAmber = Color(0xFFFFAB00)
private val SurfaceCard = Color(0xFF1A1A1A)
private val SurfaceDark = Color(0xFF0A0A0A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailRoute(
    workoutId: String,
    onNavigateBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(workoutId) {
        viewModel.onIntent(HistoryDetailIntent.LoadWorkout(workoutId))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    FullScreenLoading(message = "Loading workout...")
                }
                state.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Close,
                        title = "Error",
                        subtitle = state.error ?: "Unknown error",
                        actionText = "Retry",
                        onActionClick = { viewModel.onIntent(HistoryDetailIntent.Retry) },
                    )
                }
                state.workoutDetail != null -> {
                    HistoryDetailContent(detail = state.workoutDetail!!)
                }
            }
        }
    }
}

@Composable
private fun HistoryDetailContent(detail: WorkoutDetail) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WorkoutHeader(detail = detail)
        }

        item {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    icon = Icons.Default.FitnessCenter,
                    label = "Volume",
                    value = "${detail.totalVolume.toInt()} kg",
                    color = AccentGreen,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Default.BarChart,
                    label = "Sets",
                    value = "${detail.totalSets}",
                    color = AccentAmber,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (detail.prExerciseIds.isNotEmpty()) {
            item {
                GymBroCard(accentColor = AccentAmber) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(28.dp),
                        )
                        Column {
                            Text(
                                text = "${detail.prExerciseIds.size} Personal Record${if (detail.prExerciseIds.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = "New PRs set in this workout",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Exercises",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        items(detail.exercises) { exercise ->
            ExerciseCard(exercise = exercise)
        }

        if (detail.volumeByMuscleGroup.isNotEmpty()) {
            item {
                Text(
                    text = "Volume by Muscle Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            item {
                GymBroCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        detail.volumeByMuscleGroup.entries.sortedByDescending { it.value }.forEach { (muscle, volume) ->
                            MuscleVolumeRow(muscle = muscle.displayName, volume = volume)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WorkoutHeader(detail: WorkoutDetail) {
    val date = Instant.ofEpochMilli(detail.date)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = formatDuration(detail.durationSeconds),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseDetail) {
    GymBroCard(accentColor = if (exercise.hasPR) AccentGreen else null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = exercise.muscleGroup.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                if (exercise.hasPR) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Personal Record",
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                exercise.sets.forEach { set ->
                    SetRow(set = set)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Total Volume:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = "${exercise.totalVolume.toInt()} kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentCyan,
                )
            }
        }
    }
}

@Composable
private fun SetRow(set: SetDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Set ${set.setNumber}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${set.weight} kg × ${set.reps}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            if (set.rpe != null) {
                Text(
                    text = "RPE ${set.rpe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentAmber,
                )
            }
        }
    }
}

@Composable
private fun MuscleVolumeRow(muscle: String, volume: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = muscle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Text(
            text = "${volume.toInt()} kg",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AccentCyan,
        )
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
