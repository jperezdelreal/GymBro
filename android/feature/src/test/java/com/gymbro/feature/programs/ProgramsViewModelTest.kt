package com.gymbro.feature.programs

import app.cash.turbine.test
import com.gymbro.core.model.WorkoutTemplate
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.WorkoutTemplateRepository
import com.gymbro.core.service.ActivePlanStore
import com.gymbro.core.service.WorkoutPlanGenerator
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProgramsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var templateRepository: WorkoutTemplateRepository
    private lateinit var workoutPlanGenerator: WorkoutPlanGenerator
    private lateinit var userPreferences: UserPreferences
    private lateinit var activePlanStore: ActivePlanStore
    private lateinit var viewModel: ProgramsViewModel

    @Before
    fun setup() {
        templateRepository = mockk(relaxed = true)
        workoutPlanGenerator = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
        activePlanStore = ActivePlanStore()
        coEvery { templateRepository.observeAllTemplates() } returns flowOf(emptyList())
        coEvery { templateRepository.initializeBuiltInTemplates() } returns Unit
    }

    private fun createViewModel() = ProgramsViewModel(
        templateRepository, workoutPlanGenerator, userPreferences, activePlanStore,
    )

    @Test
    fun `initial state loads templates`() = runTest {
        val templates = listOf(
            WorkoutTemplate(name = "Push Day", description = "Chest and shoulders", exercises = emptyList()),
            WorkoutTemplate(name = "Pull Day", description = "Back and biceps", exercises = emptyList())
        )
        coEvery { templateRepository.observeAllTemplates() } returns flowOf(templates)
        
        viewModel = createViewModel()
        
        val state = viewModel.state.value
        assertEquals(2, state.templates.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `template clicked navigates to create template`() = runTest {
        viewModel = createViewModel()
        
        val template = WorkoutTemplate(name = "Test", description = "", exercises = emptyList())
        
        viewModel.effect.test {
            viewModel.onEvent(ProgramsEvent.TemplateClicked(template))
            
            val effect = awaitItem() as ProgramsEffect.NavigateToCreateTemplate
            assertEquals(template.id.toString(), effect.templateId)
        }
    }

    @Test
    fun `create template clicked shows dialog`() = runTest {
        viewModel = createViewModel()
        
        viewModel.onEvent(ProgramsEvent.CreateTemplateClicked)
        
        val state = viewModel.state.value
        assertTrue(state.showCreateDialog)
    }

    @Test
    fun `delete template calls repository`() = runTest {
        viewModel = createViewModel()
        
        viewModel.onEvent(ProgramsEvent.DeleteTemplate("template-id"))
        
        coVerify { templateRepository.deleteTemplate("template-id") }
    }

    @Test
    fun `start workout from template navigates to active workout`() = runTest {
        viewModel = createViewModel()
        
        val template = WorkoutTemplate(name = "Test", description = "", exercises = emptyList())
        
        viewModel.effect.test {
            viewModel.onEvent(ProgramsEvent.StartWorkoutFromTemplate(template))
            
            val effect = awaitItem() as ProgramsEffect.NavigateToActiveWorkout
            assertEquals(template, effect.template)
            
            coVerify { templateRepository.updateLastUsed(template.id.toString()) }
        }
    }

    @Test
    fun `loads active plan from store on init`() = runTest {
        val plan = com.gymbro.core.model.WorkoutPlan(
            name = "Your First Program",
            description = "Test",
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            workoutDays = emptyList(),
        )
        activePlanStore.setPlanFromOnboarding(plan)

        viewModel = createViewModel()

        val state = viewModel.state.value
        assertEquals("Your First Program", state.activePlan?.name)
        assertTrue(state.showFirstProgramBanner)
    }
}
