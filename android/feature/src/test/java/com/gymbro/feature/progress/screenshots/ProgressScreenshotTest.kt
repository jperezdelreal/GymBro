package com.gymbro.feature.progress.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PersonalRecord
import com.gymbro.core.model.RecordType
import com.gymbro.core.model.WorkoutHistoryItem
import com.gymbro.feature.progress.ExerciseOption
import com.gymbro.feature.progress.ProgressState
import com.gymbro.feature.progress.WeeklyVolume
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ProgressScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val now = Instant.now()

    private val sampleWorkoutHistory = listOf(
        WorkoutHistoryItem(
            workoutId = "workout-1",
            date = now.minus(1, ChronoUnit.DAYS),
            durationSeconds = 3600,
            totalVolume = 5000.0,
            exerciseCount = 3,
            exerciseNames = listOf("Bench Press", "Squat", "Deadlift"),
        ),
        WorkoutHistoryItem(
            workoutId = "workout-2",
            date = now.minus(3, ChronoUnit.DAYS),
            durationSeconds = 4200,
            totalVolume = 5500.0,
            exerciseCount = 4,
            exerciseNames = listOf("Bench Press", "Squat", "Row", "Press"),
        ),
    )

    private val samplePRs = listOf(
        PersonalRecord(
            exerciseId = "bench-press",
            exerciseName = "Bench Press",
            type = RecordType.MAX_E1RM,
            value = 100.0,
            date = now.minus(2, ChronoUnit.DAYS),
            previousValue = 95.0,
        ),
        PersonalRecord(
            exerciseId = "squat",
            exerciseName = "Squat",
            type = RecordType.MAX_VOLUME,
            value = 2000.0,
            date = now.minus(5, ChronoUnit.DAYS),
            previousValue = 1800.0,
        ),
    )

    private val sampleChartData = listOf(
        E1RMDataPoint(date = now.minus(30, ChronoUnit.DAYS), e1rm = 85.0, weight = 75.0, reps = 5),
        E1RMDataPoint(date = now.minus(23, ChronoUnit.DAYS), e1rm = 87.0, weight = 77.5, reps = 5),
        E1RMDataPoint(date = now.minus(16, ChronoUnit.DAYS), e1rm = 90.0, weight = 80.0, reps = 5),
        E1RMDataPoint(date = now.minus(9, ChronoUnit.DAYS), e1rm = 92.0, weight = 82.5, reps = 5),
        E1RMDataPoint(date = now.minus(2, ChronoUnit.DAYS), e1rm = 95.0, weight = 85.0, reps = 5),
    )

    private val sampleWeeklyVolume = listOf(
        WeeklyVolume(weekNumber = 1, volume = 18000.0),
        WeeklyVolume(weekNumber = 2, volume = 20000.0),
        WeeklyVolume(weekNumber = 3, volume = 19500.0),
        WeeklyVolume(weekNumber = 4, volume = 22000.0),
    )

    @Test
    fun progressState_withData() {
        val state = ProgressState(
            workoutHistory = sampleWorkoutHistory,
            personalRecords = samplePRs,
            exerciseOptions = listOf(
                ExerciseOption(id = "bench-press", name = "Bench Press"),
                ExerciseOption(id = "squat", name = "Squat"),
                ExerciseOption(id = "deadlift", name = "Deadlift"),
            ),
            selectedExerciseId = "bench-press",
            chartData = sampleChartData,
            plateauAlerts = emptyList(),
            isLoading = false,
            totalVolume = 42000.0,
            workoutsThisWeek = 3,
            recentPRs = 2,
            weeklyVolumeData = sampleWeeklyVolume,
        )

        paparazzi.snapshot("progress_with_data") {
            GymBroTheme {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Progress Dashboard",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.workoutsThisWeek} workouts this week • ${state.recentPRs} recent PRs",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    @Test
    fun progressState_loading() {
        val state = ProgressState(
            workoutHistory = emptyList(),
            personalRecords = emptyList(),
            exerciseOptions = emptyList(),
            isLoading = true,
        )

        paparazzi.snapshot("progress_loading") {
            GymBroTheme {
                CircularProgressIndicator()
            }
        }
    }
}
