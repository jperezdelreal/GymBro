package com.gymbro.feature.programs

import app.cash.turbine.test
import com.gymbro.core.model.WorkoutTemplate
import com.gymbro.core.repository.WorkoutTemplateRepository
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
    private lateinit var viewModel: ProgramsViewModel

    @Before
    fun setup() {
        templateRepository = mockk(relaxed = true)
        coEvery { templateRepository.observeAllTemplates() } returns flowOf(emptyList())
        coEvery { templateRepository.initializeBuiltInTemplates() } returns Unit
    }

    @Test
    fun `initial state loads templates`() = runTest {
        val templates = listOf(
            WorkoutTemplate(name = "Push Day", description = "Chest and shoulders", exercises = emptyList()),
            WorkoutTemplate(name = "Pull Day", description = "Back and biceps", exercises = emptyList())
        )
        coEvery { templateRepository.observeAllTemplates() } returns flowOf(templates)
        
        viewModel = ProgramsViewModel(templateRepository)
        
        val state = viewModel.state.value
        assertEquals(2, state.templates.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `template clicked navigates to create template`() = runTest {
        viewModel = ProgramsViewModel(templateRepository)
        
        val template = WorkoutTemplate(name = "Test", description = "", exercises = emptyList())
        
        viewModel.effect.test {
            viewModel.onEvent(ProgramsEvent.TemplateClicked(template))
            
            val effect = awaitItem() as ProgramsEffect.NavigateToCreateTemplate
            assertEquals(template.id.toString(), effect.templateId)
        }
    }

    @Test
    fun `create template clicked shows dialog`() = runTest {
        viewModel = ProgramsViewModel(templateRepository)
        
        viewModel.onEvent(ProgramsEvent.CreateTemplateClicked)
        
        val state = viewModel.state.value
        assertTrue(state.showCreateDialog)
    }

    @Test
    fun `delete template calls repository`() = runTest {
        viewModel = ProgramsViewModel(templateRepository)
        
        viewModel.onEvent(ProgramsEvent.DeleteTemplate("template-id"))
        
        coVerify { templateRepository.deleteTemplate("template-id") }
    }

    @Test
    fun `start workout from template navigates to active workout`() = runTest {
        viewModel = ProgramsViewModel(templateRepository)
        
        val template = WorkoutTemplate(name = "Test", description = "", exercises = emptyList())
        
        viewModel.effect.test {
            viewModel.onEvent(ProgramsEvent.StartWorkoutFromTemplate(template))
            
            val effect = awaitItem() as ProgramsEffect.NavigateToActiveWorkout
            assertEquals(template, effect.template)
            
            coVerify { templateRepository.updateLastUsed(template.id.toString()) }
        }
    }
}
