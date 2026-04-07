package com.gymbro.feature.history

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymbro.core.model.MuscleGroup
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
fun HistoryListRoute(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: HistoryListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History", fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
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
                    FullScreenLoading(message = "Loading history...")
                }
                state.error != null -> {
                    EmptyState(
                        icon = Icons.Default.Close,
                        title = "Error",
                        subtitle = state.error ?: "Unknown error",
                        actionText = "Retry",
                        onActionClick = { viewModel.onIntent(HistoryListIntent.Retry) },
                    )
                }
                state.workouts.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.History,
                        title = "No Workouts Yet",
                        subtitle = "Start training to build your history!",
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groupedWorkouts.forEach { group ->
            item {
                Text(
                    text = group.monthYear,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            items(group.workouts) { workout ->
                WorkoutCard(
                    workout = workout,
                    onClick = { onWorkoutClick(workout.workoutId) },
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun WorkoutCard(
    workout: WorkoutListItem,
    onClick: () -> Unit,
) {
    val date = Instant.ofEpochMilli(workout.date)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d"))

    GymBroCard(
        modifier = Modifier.clickable(onClick = onClick),
        accentColor = if (workout.prCount > 0) AccentGreen else null,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = formatDuration(workout.durationSeconds),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
                if (workout.prCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Personal Records",
                            tint = AccentAmber,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${workout.prCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentAmber,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatChip(
                    icon = Icons.Default.FitnessCenter,
                    label = "${workout.exerciseCount} exercises",
                    color = AccentCyan,
                )
                StatChip(
                    icon = Icons.Default.Timer,
                    label = "${workout.totalVolume.toInt()} kg",
                    color = AccentGreen,
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

@Composable
private fun StatChip(
    icon: ImageVector,
    label: String,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceCard)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MuscleGroupTag(muscleGroup: MuscleGroup) {
    Text(
        text = muscleGroup.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.7f),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
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
