package com.gymbro.feature.programs.screenshots

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.TemplateExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.model.WorkoutTemplate
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import com.gymbro.core.preferences.UserPreferences.TrainingGoal
import com.gymbro.feature.programs.ProgramsScreen
import com.gymbro.feature.programs.ProgramsState
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ProgramsScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val sampleTemplates = listOf(
        WorkoutTemplate(
            id = UUID.randomUUID(),
            name = "Push Day",
            description = "Chest, shoulders, triceps",
            exercises = listOf(
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Bench Press",
                    muscleGroup = MuscleGroup.CHEST,
                    targetSets = 4,
                    targetReps = 8,
                ),
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Overhead Press",
                    muscleGroup = MuscleGroup.SHOULDERS,
                    targetSets = 3,
                    targetReps = 10,
                ),
            ),
            lastUsedAt = Instant.now().minus(2, ChronoUnit.DAYS),
        ),
        WorkoutTemplate(
            id = UUID.randomUUID(),
            name = "Pull Day",
            description = "Back and biceps",
            exercises = listOf(
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Barbell Row",
                    muscleGroup = MuscleGroup.BACK,
                    targetSets = 4,
                    targetReps = 8,
                ),
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Pull-ups",
                    muscleGroup = MuscleGroup.BACK,
                    targetSets = 3,
                    targetReps = 10,
                ),
            ),
            lastUsedAt = Instant.now().minus(1, ChronoUnit.DAYS),
        ),
        WorkoutTemplate(
            id = UUID.randomUUID(),
            name = "Leg Day",
            description = "Quads, hamstrings, glutes",
            exercises = listOf(
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Squat",
                    muscleGroup = MuscleGroup.QUADRICEPS,
                    targetSets = 5,
                    targetReps = 5,
                ),
            ),
        ),
    )

    private val samplePlan = WorkoutPlan(
        name = "PPL Hypertrophy",
        description = "Push/Pull/Legs 4-week block",
        goal = TrainingGoal.HYPERTROPHY,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        daysPerWeek = 6,
        weeks = 4,
        workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push A",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Bench Press", sets = 4, repsRange = "6-8"),
                    PlannedExercise(exerciseName = "OHP", sets = 3, repsRange = "8-10"),
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull A",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Barbell Row", sets = 4, repsRange = "6-8"),
                    PlannedExercise(exerciseName = "Pull-ups", sets = 3, repsRange = "8-12"),
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Legs A",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Squat", sets = 5, repsRange = "5"),
                    PlannedExercise(exerciseName = "RDL", sets = 3, repsRange = "8-10"),
                ),
            ),
        ),
    )

    @Test
    fun programsScreen_withTemplates() {
        paparazzi.snapshot {
            GymBroTheme {
                ProgramsScreen(
                    state = ProgramsState(
                        templates = sampleTemplates,
                        activePlan = null,
                        isLoading = false,
                        isGeneratingPlan = false,
                        error = null,
                        showCreateDialog = false,
                        showFirstProgramBanner = false,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun programsScreen_withActivePlan() {
        paparazzi.snapshot {
            GymBroTheme {
                ProgramsScreen(
                    state = ProgramsState(
                        templates = sampleTemplates,
                        activePlan = samplePlan,
                        isLoading = false,
                        isGeneratingPlan = false,
                        error = null,
                        showCreateDialog = false,
                        showFirstProgramBanner = false,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun programsScreen_empty() {
        paparazzi.snapshot {
            GymBroTheme {
                ProgramsScreen(
                    state = ProgramsState(
                        templates = emptyList(),
                        activePlan = null,
                        isLoading = false,
                        isGeneratingPlan = false,
                        error = null,
                        showCreateDialog = false,
                        showFirstProgramBanner = true,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun programsScreen_loading() {
        paparazzi.snapshot {
            GymBroTheme {
                ProgramsScreen(
                    state = ProgramsState(
                        templates = emptyList(),
                        activePlan = null,
                        isLoading = true,
                        isGeneratingPlan = false,
                        error = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }
}
