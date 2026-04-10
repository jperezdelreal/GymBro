package com.gymbro.feature.history.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.history.HistoryListState
import com.gymbro.feature.history.WorkoutGroup
import com.gymbro.feature.history.WorkoutListItem
import org.junit.Rule
import org.junit.Test

class HistoryListScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val sampleWorkouts = listOf(
        WorkoutListItem(
            workoutId = "workout-1",
            date = 1717891200000, // 2024-06-09
            durationSeconds = 3600,
            exerciseCount = 4,
            totalVolume = 5200.0,
            totalSets = 16,
            muscleGroups = setOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            prCount = 2,
        ),
        WorkoutListItem(
            workoutId = "workout-2",
            date = 1717718400000, // 2024-06-07
            durationSeconds = 4500,
            exerciseCount = 5,
            totalVolume = 6800.0,
            totalSets = 20,
            muscleGroups = setOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
            prCount = 0,
        ),
        WorkoutListItem(
            workoutId = "workout-3",
            date = 1717545600000, // 2024-06-05
            durationSeconds = 2700,
            exerciseCount = 3,
            totalVolume = 4100.0,
            totalSets = 12,
            muscleGroups = setOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
            prCount = 1,
        ),
    )

    @Test
    fun historyListState_withWorkouts() {
        val state = HistoryListState(
            isLoading = false,
            error = null,
            workouts = sampleWorkouts,
            groupedWorkouts = listOf(
                WorkoutGroup(
                    monthYear = "June 2024",
                    workouts = sampleWorkouts,
                ),
            ),
        )

        paparazzi.snapshot("history_with_workouts") {
            GymBroTheme {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Workout History",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.workouts.size} workouts",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        state.groupedWorkouts.forEach { group ->
                            Text(
                                text = group.monthYear,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            group.workouts.forEach { workout ->
                                Surface(
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row {
                                            Text(
                                                text = "${workout.exerciseCount} exercises • ${workout.totalSets} sets",
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                        Text(
                                            text = "${workout.totalVolume.toInt()} kg total • ${workout.durationSeconds / 60}m",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        if (workout.prCount > 0) {
                                            Text(
                                                text = "⭐ ${workout.prCount} PR${if (workout.prCount > 1) "s" else ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun historyListState_empty() {
        val state = HistoryListState(
            isLoading = false,
            error = null,
            workouts = emptyList(),
            groupedWorkouts = emptyList(),
        )

        paparazzi.snapshot("history_empty") {
            GymBroTheme {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No Workouts Yet",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start your first workout to see history here",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun historyListState_loading() {
        paparazzi.snapshot("history_loading") {
            GymBroTheme {
                CircularProgressIndicator()
            }
        }
    }
}
