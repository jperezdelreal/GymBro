package com.gymbro.feature.onboarding

import app.cash.turbine.test
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import com.gymbro.core.preferences.UserPreferences.TrainingGoal
import com.gymbro.core.preferences.UserPreferences.WeightUnit
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.WorkoutPlanGenerator
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var userPreferences: UserPreferences
    private lateinit var workoutPlanGenerator: WorkoutPlanGenerator
    private lateinit var activePlanStore: ActivePlanStore
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setup() {
        userPreferences = mockk(relaxed = true)
        workoutPlanGenerator = mockk(relaxed = true)
        activePlanStore = ActivePlanStore()
        viewModel = OnboardingViewModel(userPreferences, workoutPlanGenerator, activePlanStore)
    }

    @Test
    fun `initial state is correct`() = runTest {
        val state = viewModel.state.value
        assertEquals(0, state.currentPage)
        assertEquals(WeightUnit.KG, state.selectedUnit)
        assertEquals("", state.userName)
        assertEquals(TrainingGoal.HYPERTROPHY, state.selectedGoal)
    }

    @Test
    fun `page changed updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.PageChanged(2))
        
        val state = viewModel.state.value
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `unit selected updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.UnitSelected(WeightUnit.LBS))
        
        val state = viewModel.state.value
        assertEquals(WeightUnit.LBS, state.selectedUnit)
    }

    @Test
    fun `name changed updates state`() = runTest {
        viewModel.onEvent(OnboardingEvent.NameChanged("John"))
        
        val state = viewModel.state.value
        assertEquals("John", state.userName)
    }

    @Test
    fun `complete onboarding saves preferences and emits navigation effect`() = runTest {
        viewModel.onEvent(OnboardingEvent.UnitSelected(WeightUnit.LBS))
        viewModel.onEvent(OnboardingEvent.NameChanged("John"))
        
        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
            
            val effect = awaitItem()
            assertTrue(effect is OnboardingEffect.NavigateToMain)
            
            coVerify { userPreferences.setWeightUnit(WeightUnit.LBS) }
            coVerify { userPreferences.setUserName("John") }
            coVerify { userPreferences.setOnboardingComplete(true) }
        }
    }

    @Test
    fun `complete onboarding generates plan and stores it`() = runTest {
        val mockPlan = WorkoutPlan(
            name = "Hypertrophy Program",
            description = "Test plan",
            goal = TrainingGoal.HYPERTROPHY,
            experienceLevel = ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            workoutDays = emptyList(),
        )
        coEvery {
            workoutPlanGenerator.generatePlan(any(), any(), any(), any())
        } returns mockPlan

        viewModel.onEvent(OnboardingEvent.GoalSelected(TrainingGoal.HYPERTROPHY))
        viewModel.onEvent(OnboardingEvent.ExperienceSelected(ExperienceLevel.INTERMEDIATE))
        viewModel.onEvent(OnboardingEvent.TrainingDaysSelected(4))

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)
            awaitItem() // NavigateToMain

            coVerify {
                workoutPlanGenerator.generatePlan(
                    TrainingGoal.HYPERTROPHY,
                    ExperienceLevel.INTERMEDIATE,
                    4,
                    UserPreferences.TrainingPhase.MAINTENANCE,
                )
            }

            val storedPlan = activePlanStore.getPlan()
            assertEquals("Your First Program", storedPlan?.name)
            assertTrue(activePlanStore.isFromOnboarding.value)
        }
    }

    @Test
    fun `complete onboarding still navigates if plan generation fails`() = runTest {
        coEvery {
            workoutPlanGenerator.generatePlan(any(), any(), any(), any())
        } throws RuntimeException("Failed to generate plan")

        viewModel.effects.test(timeout = 5.seconds) {
            viewModel.onEvent(OnboardingEvent.CompleteOnboarding)

            // Error effect is emitted, then navigation
            val effects = mutableListOf(awaitItem(), awaitItem())

            assertTrue(
                "Expected ShowPlanGenerationError effect",
                effects.any { it is OnboardingEffect.ShowPlanGenerationError },
            )
            assertTrue(
                "Expected NavigateToMain effect",
                effects.any { it is OnboardingEffect.NavigateToMain },
            )
            val navEffect = effects.filterIsInstance<OnboardingEffect.NavigateToMain>().first()
            assertEquals(false, navEffect.planGenerated)

            assertNull(activePlanStore.getPlan())
            assertNotNull(viewModel.state.value.planGenerationError)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
