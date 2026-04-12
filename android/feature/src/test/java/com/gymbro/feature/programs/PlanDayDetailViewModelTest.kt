package com.gymbro.feature.programs

import app.cash.turbine.test
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.feature.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlanDayDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var activePlanStore: ActivePlanStore
    private lateinit var viewModel: PlanDayDetailViewModel

    private val testPlan = WorkoutPlan(
        id = "plan-1",
        name = "Push Pull Legs",
        description = "A classic 3-day split",
        goal = UserPreferences.TrainingGoal.STRENGTH,
        experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
        daysPerWeek = 3,
        workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push Day",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Bench Press", sets = 4, repsRange = "6-8"),
                    PlannedExercise(exerciseName = "OHP", sets = 3, repsRange = "8-10"),
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull Day",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Deadlift", sets = 3, repsRange = "5"),
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Leg Day",
                exercises = listOf(
                    PlannedExercise(exerciseName = "Squat", sets = 5, repsRange = "5"),
                ),
            ),
        ),
    )

    @Before
    fun setup() {
        activePlanStore = ActivePlanStore()
        viewModel = PlanDayDetailViewModel(activePlanStore)
    }

    @Test
    fun `loadDay populates state with correct workout day`() {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(1))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.workoutDay)
        assertEquals("Push Day", state.workoutDay!!.name)
        assertEquals(2, state.workoutDay!!.exercises.size)
        assertEquals("Push Pull Legs", state.planName)
    }

    @Test
    fun `loadDay with no active plan shows error`() {
        // No plan set in store
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(1))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("No active plan"))
        assertNull(state.workoutDay)
    }

    @Test
    fun `loadDay with invalid day number shows error`() {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(99))

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Day 99 not found"))
        assertNull(state.workoutDay)
    }

    @Test
    fun `retry reloads current day`() {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(2))

        val state = viewModel.state.value
        assertEquals("Pull Day", state.workoutDay!!.name)

        // Retry reloads the same day
        viewModel.onIntent(PlanDayDetailIntent.Retry)
        val retryState = viewModel.state.value
        assertEquals("Pull Day", retryState.workoutDay!!.name)
    }

    @Test
    fun `retry before any load is a no-op`() {
        viewModel.onIntent(PlanDayDetailIntent.Retry)

        val state = viewModel.state.value
        // Initial state — isLoading defaults to true, no error
        assertTrue(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadDay for each day returns correct data`() {
        activePlanStore.setPlan(testPlan)

        viewModel.onIntent(PlanDayDetailIntent.LoadDay(3))
        val state = viewModel.state.value
        assertEquals("Leg Day", state.workoutDay!!.name)
        assertEquals(1, state.workoutDay!!.exercises.size)
        assertEquals("Squat", state.workoutDay!!.exercises[0].exerciseName)
    }

    // ──────────────────────────────────────────────────────────────────────
    // StartWorkout intent → NavigateToActiveWorkout effect
    //
    // PlanDayDetailContract now has StartWorkout intent and
    // NavigateToActiveWorkout effect. The ViewModel stores the workout day
    // in ActivePlanStore before emitting the navigation effect.
    // ──────────────────────────────────────────────────────────────────────

    @Test
    fun `startWorkout emits NavigateToActiveWorkout effect`() = runTest {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(1))

        // Verify day loaded correctly first
        val state = viewModel.state.value
        assertEquals(2, state.workoutDay!!.exercises.size)

        // Now fire StartWorkout — expect NavigateToActiveWorkout effect
        viewModel.effect.test {
            viewModel.onIntent(PlanDayDetailIntent.StartWorkout)

            val effect = awaitItem()
            assertTrue(effect is PlanDayDetailEffect.NavigateToActiveWorkout)
        }
    }

    @Test
    fun `startWorkout stores pending workout day in ActivePlanStore`() = runTest {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(1))

        viewModel.effect.test {
            viewModel.onIntent(PlanDayDetailIntent.StartWorkout)
            awaitItem() // consume the effect

            // Verify the workout day was stored for the active workout screen
            val pendingDay = activePlanStore.pendingWorkoutDay.value
            assertNotNull(pendingDay)
            assertEquals("Push Day", pendingDay!!.name)
            assertEquals(2, pendingDay.exercises.size)
            assertEquals("Bench Press", pendingDay.exercises[0].exerciseName)
            assertEquals("OHP", pendingDay.exercises[1].exerciseName)
        }
    }

    @Test
    fun `startWorkout before loadDay is a no-op`() = runTest {
        // No day loaded — should be a no-op, never crash
        viewModel.onIntent(PlanDayDetailIntent.StartWorkout)
        // If no effect channel exists yet, this just verifies no exception
        val state = viewModel.state.value
        assertTrue(state.isLoading) // Still in initial state
    }

    @Test
    fun `startWorkout preserves sets and reps from plan`() = runTest {
        activePlanStore.setPlan(testPlan)
        viewModel.onIntent(PlanDayDetailIntent.LoadDay(1))

        viewModel.effect.test {
            viewModel.onIntent(PlanDayDetailIntent.StartWorkout)
            awaitItem()

            // Verify the planned sets/reps survive the trip via ActivePlanStore
            val pendingDay = activePlanStore.pendingWorkoutDay.value!!
            assertEquals(4, pendingDay.exercises[0].sets)
            assertEquals("6-8", pendingDay.exercises[0].repsRange)
            assertEquals(3, pendingDay.exercises[1].sets)
            assertEquals("8-10", pendingDay.exercises[1].repsRange)
        }
    }
}
