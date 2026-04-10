package com.gymbro.feature.home.screenshots

import androidx.compose.material3.SnackbarHostState
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.gymbro.app.ui.theme.GymBroTheme
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import com.gymbro.core.preferences.UserPreferences.TrainingGoal
import com.gymbro.feature.home.HomeScreen
import com.gymbro.feature.home.HomeState
import com.gymbro.feature.home.RecentWorkoutItem
import org.junit.Rule
import org.junit.Test

class HomeScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material3.DayNight.NoActionBar",
        showSystemUi = false,
    )

    private val samplePlan = WorkoutPlan(
        id = "plan-1",
        name = "PPL Hypertrophy",
        description = "Push/Pull/Legs for muscle growth",
        goal = TrainingGoal.HYPERTROPHY,
        experienceLevel = ExperienceLevel.INTERMEDIATE,
        daysPerWeek = 6,
        workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push Day",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Bench Press", sets = 4, repsRange = "8-10"),
                    PlannedExercise(exerciseName = "OHP", sets = 3, repsRange = "8-12"),
                ),
            ),
        ),
    )

    private val sampleRecent = listOf(
        RecentWorkoutItem(
            workoutId = "w-1",
            date = 1717891200000,
            durationSeconds = 3600,
            exerciseCount = 5,
            totalSets = 18,
            totalVolume = 7200.0,
        ),
        RecentWorkoutItem(
            workoutId = "w-2",
            date = 1717718400000,
            durationSeconds = 4200,
            exerciseCount = 4,
            totalSets = 16,
            totalVolume = 6100.0,
        ),
    )

    @Test
    fun homeScreen_loading() {
        paparazzi.snapshot("home_loading") {
            GymBroTheme {
                HomeScreen(
                    state = HomeState(isLoading = true),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun homeScreen_noPlan() {
        paparazzi.snapshot("home_no_plan") {
            GymBroTheme {
                HomeScreen(
                    state = HomeState(
                        isLoading = false,
                        activePlan = null,
                        todayWorkout = null,
                        recentWorkouts = emptyList(),
                        daysSinceLastWorkout = null,
                    ),
                    onEvent = {},
                )
            }
        }
    }

    @Test
    fun homeScreen_withPlanAndRecent() {
        paparazzi.snapshot("home_with_plan") {
            GymBroTheme {
                HomeScreen(
                    state = HomeState(
                        isLoading = false,
                        activePlan = samplePlan,
                        todayWorkout = samplePlan.workoutDays.first(),
                        recentWorkouts = sampleRecent,
                        daysSinceLastWorkout = 1,
                    ),
                    onEvent = {},
                )
            }
        }
    }
}
