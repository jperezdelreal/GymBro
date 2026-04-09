package com.gymbro.feature.exerciselibrary.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.exerciselibrary.ExerciseLibraryScreen
import com.gymbro.feature.exerciselibrary.ExerciseLibraryState
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class ExerciseLibraryScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val sampleExercises = listOf(
        Exercise(
            id = UUID.randomUUID(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BARBELL,
            description = "Classic chest builder"
        ),
        Exercise(
            id = UUID.randomUUID(),
            name = "Squat",
            muscleGroup = MuscleGroup.QUADRICEPS,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BARBELL,
            description = "King of exercises"
        ),
        Exercise(
            id = UUID.randomUUID(),
            name = "Deadlift",
            muscleGroup = MuscleGroup.BACK,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BARBELL,
            description = "Full body power"
        ),
        Exercise(
            id = UUID.randomUUID(),
            name = "Pull-ups",
            muscleGroup = MuscleGroup.BACK,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.BODYWEIGHT,
            description = "Back width builder"
        ),
    )

    @Test
    fun exerciseLibraryScreen_withExercises() {
        paparazzi.snapshot {
            GymBroTheme {
                ExerciseLibraryScreen(
                    state = ExerciseLibraryState(
                        exercises = sampleExercises,
                        searchQuery = "",
                        selectedMuscleGroup = null,
                        isLoading = false,
                        error = null
                    ),
                    onEvent = {}
                )
            }
        }
    }

    @Test
    fun exerciseLibraryScreen_loading() {
        paparazzi.snapshot {
            GymBroTheme {
                ExerciseLibraryScreen(
                    state = ExerciseLibraryState(
                        exercises = emptyList(),
                        searchQuery = "",
                        selectedMuscleGroup = null,
                        isLoading = true,
                        error = null
                    ),
                    onEvent = {}
                )
            }
        }
    }

    @Test
    fun exerciseLibraryScreen_withSearch() {
        paparazzi.snapshot {
            GymBroTheme {
                ExerciseLibraryScreen(
                    state = ExerciseLibraryState(
                        exercises = sampleExercises.filter { it.name.contains("Pull", ignoreCase = true) },
                        searchQuery = "Pull",
                        selectedMuscleGroup = null,
                        isLoading = false,
                        error = null
                    ),
                    onEvent = {}
                )
            }
        }
    }

    @Test
    fun exerciseLibraryScreen_filteredByMuscleGroup() {
        paparazzi.snapshot {
            GymBroTheme {
                ExerciseLibraryScreen(
                    state = ExerciseLibraryState(
                        exercises = sampleExercises.filter { it.muscleGroup == MuscleGroup.BACK },
                        searchQuery = "",
                        selectedMuscleGroup = MuscleGroup.BACK,
                        isLoading = false,
                        error = null
                    ),
                    onEvent = {}
                )
            }
        }
    }
}
