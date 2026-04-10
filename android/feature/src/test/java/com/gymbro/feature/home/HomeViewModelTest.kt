package com.gymbro.feature.home

import app.cash.turbine.test
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutRepository: WorkoutRepository
    private lateinit var activePlanStore: ActivePlanStore
    private lateinit var personalRecordService: PersonalRecordService
    private lateinit var viewModel: HomeViewModel

    private val testPlan = WorkoutPlan(
        id = "plan-1",
        name = "PPL",
        description = "Push Pull Legs",
        goal = UserPreferences.TrainingGoal.STRENGTH,
        experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
        daysPerWeek = 3,
        workoutDays = listOf(
            WorkoutDay(dayNumber = 1, name = "Push", exercises = listOf(
                PlannedExercise(exerciseName = "Bench Press", sets = 4, repsRange = "6-8"),
            )),
            WorkoutDay(dayNumber = 2, name = "Pull", exercises = listOf(
                PlannedExercise(exerciseName = "Rows", sets = 4, repsRange = "8-10"),
            )),
        ),
    )

    @Before
    fun setup() {
        workoutRepository = mockk(relaxed = true)
        personalRecordService = mockk(relaxed = true)
        activePlanStore = ActivePlanStore()

        coEvery { personalRecordService.getWorkoutHistory() } returns emptyList()
        coEvery { workoutRepository.getDaysSinceLastWorkout() } returns null
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(workoutRepository, activePlanStore, personalRecordService)
    }

    @Test
    fun `initial state with no plan has null activePlan`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.activePlan)
        assertNull(state.todayWorkout)
    }

    @Test
    fun `active plan updates state reactively`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        activePlanStore.setPlan(testPlan)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.activePlan)
        assertEquals("PPL", state.activePlan!!.name)
        assertNotNull(state.todayWorkout)
    }

    @Test
    fun `clearing active plan resets state`() = runTest {
        activePlanStore.setPlan(testPlan)
        viewModel = createViewModel()
        advanceUntilIdle()

        activePlanStore.setPlan(null)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.activePlan)
        assertNull(state.todayWorkout)
    }

    @Test
    fun `empty workout history results in empty recentWorkouts`() = runTest {
        coEvery { personalRecordService.getWorkoutHistory() } returns emptyList()

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.recentWorkouts.isEmpty())
    }

    @Test
    fun `quick start emits NavigateToActiveWorkout effect`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.QuickStartWorkout)
            assertEquals(HomeEffect.NavigateToActiveWorkout, awaitItem())
        }
    }

    @Test
    fun `create first program emits NavigateToPrograms effect`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.CreateFirstProgram)
            assertEquals(HomeEffect.NavigateToPrograms, awaitItem())
        }
    }

    @Test
    fun `view workout detail emits correct effect`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.onEvent(HomeEvent.ViewWorkoutDetail("workout-123"))
            val effect = awaitItem() as HomeEffect.NavigateToWorkoutDetail
            assertEquals("workout-123", effect.workoutId)
        }
    }

    @Test
    fun `days since last workout loads into state`() = runTest {
        coEvery { workoutRepository.getDaysSinceLastWorkout() } returns 3

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.state.value.daysSinceLastWorkout)
    }
}
