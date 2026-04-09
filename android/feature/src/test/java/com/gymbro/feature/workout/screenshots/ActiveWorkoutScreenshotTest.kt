package com.gymbro.feature.workout.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.workout.ActiveWorkoutState
import com.gymbro.feature.workout.WorkoutExerciseUi
import com.gymbro.feature.workout.WorkoutSetUi
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ActiveWorkoutScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val benchPress = Exercise(
        id = UUID.randomUUID(),
        name = "Bench Press",
        muscleGroup = MuscleGroup.CHEST,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    private val squat = Exercise(
        id = UUID.randomUUID(),
        name = "Squat",
        muscleGroup = MuscleGroup.QUADRICEPS,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    @Test
    fun activeWorkoutState_emptyWorkout() {
        // Test state model for empty workout
        val state = ActiveWorkoutState(
            workoutId = "test-workout",
            exercises = emptyList(),
            elapsedSeconds = 0,
            totalVolume = 0.0,
            totalSets = 0,
            isLoading = false
        )
        
        paparazzi.snapshot("empty_workout") {
            GymBroTheme {
                Text(
                    text = "Workout started - ${state.exercises.size} exercises",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }

    @Test
    fun activeWorkoutState_withExercisesAndSets() {
        val state = ActiveWorkoutState(
            workoutId = "test-workout",
            exercises = listOf(
                WorkoutExerciseUi(
                    exercise = benchPress,
                    sets = listOf(
                        WorkoutSetUi(
                            id = "set-1",
                            setNumber = 1,
                            weight = "60",
                            reps = "10",
                            rpe = "7",
                            isWarmup = true,
                            isCompleted = true
                        ),
                        WorkoutSetUi(
                            id = "set-2",
                            setNumber = 2,
                            weight = "80",
                            reps = "8",
                            rpe = "8",
                            isWarmup = false,
                            isCompleted = true
                        ),
                    )
                ),
                WorkoutExerciseUi(
                    exercise = squat,
                    sets = listOf(
                        WorkoutSetUi(
                            id = "set-4",
                            setNumber = 1,
                            weight = "100",
                            reps = "5",
                            rpe = "9",
                            isWarmup = false,
                            isCompleted = true
                        ),
                    )
                ),
            ),
            elapsedSeconds = 1800,
            totalVolume = 1280.0,
            totalSets = 3,
            isLoading = false
        )

        paparazzi.snapshot("workout_with_exercises") {
            GymBroTheme {
                Surface {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Active Workout",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.exercises.size} exercises • ${state.totalSets} sets • ${state.totalVolume.toInt()} kg total",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
