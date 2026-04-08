package com.gymbro.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gymbro.feature.common.EmptyState
import com.gymbro.feature.common.FullScreenLoading
import com.gymbro.feature.common.GlassmorphicCard
import java.time.Instant
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
                title = { Text("Workout Details", fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() }) },
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
    var itemIndex = 0
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            val currentIndex = itemIndex++
            WorkoutHeader(detail = detail, index = currentIndex)
        }

        item {
            val currentIndex = itemIndex++
            AnimatedItem(index = currentIndex) {
                Text(
                    text = "Resumen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
                )
            }
        }

        item {
            val currentIndex = itemIndex++
            AnimatedItem(index = currentIndex) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(
                        icon = Icons.Default.FitnessCenter,
                        label = "Volumen",
                        value = "${detail.totalVolume.toInt()} kg",
                        gradientColors = listOf(AccentGreenStart, AccentGreenEnd),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        icon = Icons.Default.BarChart,
                        label = "Series",
                        value = "${detail.totalSets}",
                        gradientColors = listOf(AccentCyanStart, AccentCyanEnd),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (detail.prExerciseIds.isNotEmpty()) {
            item {
                val currentIndex = itemIndex++
                AnimatedItem(index = currentIndex) {
                    GlassmorphicCard(
                        accentColor = AccentAmberStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            AccentAmberStart.copy(alpha = 0.2f),
                                            AccentAmberEnd.copy(alpha = 0.1f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentAmberStart,
                                modifier = Modifier.size(32.dp),
                            )
                            Column {
                                Text(
                                    text = "¡${detail.prExerciseIds.size} Récord${if (detail.prExerciseIds.size > 1) "s" else ""}!",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = "Nuevos récords personales",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            val currentIndex = itemIndex++
            AnimatedItem(index = currentIndex) {
                Text(
                    text = "Ejercicios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
                )
            }
        }

        itemsIndexed(detail.exercises) { exerciseIndex, exercise ->
            val currentIndex = itemIndex++
            ExerciseCard(exercise = exercise, index = currentIndex)
        }

        if (detail.volumeByMuscleGroup.isNotEmpty()) {
            item {
                val currentIndex = itemIndex++
                AnimatedItem(index = currentIndex) {
                    Text(
                        text = "Volumen por Grupo Muscular",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp).semantics { heading() },
                    )
                }
            }

            item {
                val currentIndex = itemIndex++
                AnimatedItem(index = currentIndex) {
                    GlassmorphicCard {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            detail.volumeByMuscleGroup.entries.sortedByDescending { it.value }.forEach { (muscle, volume) ->
                                MuscleVolumeRow(muscle = muscle.displayName, volume = volume)
                            }
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
private fun AnimatedItem(
    index: Int,
    content: @Composable () -> Unit
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
        content()
    }
}

@Composable
private fun WorkoutHeader(detail: WorkoutDetail, index: Int) {
    val date = Instant.ofEpochMilli(detail.date)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM"))

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
            accentColor = AccentGreenStart
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                AccentGreenStart.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = AccentCyanStart,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = formatDuration(detail.durationSeconds),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier,
        accentColor = gradientColors.first()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors.map { it.copy(alpha = 0.15f) }
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = gradientColors.first(), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ExerciseCard(exercise: ExerciseDetail, index: Int) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    
    val accentColor = if (exercise.hasPR) AccentAmberStart else AccentGreenStart
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
    ) {
        GlassmorphicCard(accentColor = accentColor) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                    if (exercise.hasPR) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(AccentAmberStart, AccentAmberEnd)
                                    )
                                )
                                .padding(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Récord Personal",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.sets.forEach { set ->
                        SetRow(set = set)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AccentCyanStart.copy(alpha = 0.2f),
                                    AccentCyanEnd.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Volumen Total:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "${exercise.totalVolume.toInt()} kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyanStart,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetRow(set: SetDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Serie ${set.setNumber}",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${set.weight} kg × ${set.reps}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (set.rpe != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(AccentAmberStart.copy(alpha = 0.3f), AccentAmberEnd.copy(alpha = 0.2f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "RPE ${set.rpe}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentAmberStart,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleVolumeRow(muscle: String, volume: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        AccentGreenStart.copy(alpha = 0.15f),
                        AccentCyanStart.copy(alpha = 0.1f)
                    )
                )
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = muscle,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = "${volume.toInt()} kg",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = AccentCyanStart,
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
