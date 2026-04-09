package com.gymbro.feature.workout

import app.cash.turbine.test
import com.gymbro.core.TestFixtures
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.service.SmartWorkoutSuggestion
import com.gymbro.core.service.SuggestedExercise
import com.gymbro.core.service.WorkoutGeneratorService
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SmartWorkoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutGeneratorService: WorkoutGeneratorService
    private lateinit var viewModel: SmartWorkoutViewModel

    @Before
    fun setup() {
        workoutGeneratorService = mockk(relaxed = true)
        
        val suggestion = SmartWorkoutSuggestion(
            exercises = listOf(
                SuggestedExercise(
                    exercise = TestFixtures.benchPress,
                    targetSets = 4,
                    targetReps = 8..10,
                    suggestedWeight = 100.0,
                    progressionHint = "Add 2.5kg from last session"
                )
            ),
            targetMuscleGroups = listOf(MuscleGroup.CHEST),
            reasoning = "Focus on chest strength",
            recoveryScore = 85
        )
        coEvery { workoutGeneratorService.generateSmartWorkout(any()) } returns suggestion
    }

    @Test
    fun `initial state generates workout`() = runTest {
        viewModel = SmartWorkoutViewModel(workoutGeneratorService)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertFalse(state.isGenerating)
        assertEquals(1, state.exercises.size)
        assertEquals("Focus on chest strength", state.reasoning)
        assertEquals(85, state.recoveryScore)
    }

    @Test
    fun `regenerate workout generates new workout`() = runTest {
        viewModel = SmartWorkoutViewModel(workoutGeneratorService)
        
        viewModel.handleEvent(SmartWorkoutEvent.RegenerateWorkout)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.exercises.size)
    }

    @Test
    fun `start workout emits navigation effect`() = runTest {
        viewModel = SmartWorkoutViewModel(workoutGeneratorService)
        
        viewModel.effect.test {
            viewModel.handleEvent(SmartWorkoutEvent.StartWorkout)
            
            val effect = awaitItem() as SmartWorkoutEffect.StartWorkoutWithExercises
            assertEquals(1, effect.exercises.size)
            assertEquals(TestFixtures.benchPress, effect.exercises[0])
        }
    }

    @Test
    fun `navigate back emits effect`() = runTest {
        viewModel = SmartWorkoutViewModel(workoutGeneratorService)
        
        viewModel.effect.test {
            viewModel.handleEvent(SmartWorkoutEvent.NavigateBack)
            
            val effect = awaitItem()
            assertEquals(SmartWorkoutEffect.NavigateBack, effect)
        }
    }

    @Test
    fun `generation error sets error state`() = runTest {
        coEvery { workoutGeneratorService.generateSmartWorkout(any()) } throws RuntimeException("Generation failed")
        
        viewModel = SmartWorkoutViewModel(workoutGeneratorService)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("Generation failed", state.error)
        assertTrue(state.exercises.isEmpty())
    }
}
