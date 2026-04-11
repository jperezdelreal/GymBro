package com.gymbro.feature.exerciselibrary

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.gymbro.core.TestFixtures
import com.gymbro.core.fakes.FakeExerciseRepository
import com.gymbro.feature.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var fakeExerciseRepository: FakeExerciseRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        fakeExerciseRepository = FakeExerciseRepository()
        context = mockk(relaxed = true)
        // Stub string resources used by the ViewModel
        every { context.getString(any()) } returns "Error message"
    }

    private fun createViewModel(exerciseId: String): ExerciseDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("exerciseId" to exerciseId))
        return ExerciseDetailViewModel(savedStateHandle, fakeExerciseRepository, context)
    }

    @Test
    fun `exercise loads successfully by ID`() = runTest(testDispatcher) {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)

        val viewModel = createViewModel(TestFixtures.benchPress.id.toString())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.exercise)
        assertEquals(TestFixtures.benchPress.id, state.exercise!!.id)
        assertEquals("Bench Press", state.exercise!!.name)
    }

    @Test
    fun `exercise not found shows error state`() = runTest(testDispatcher) {
        fakeExerciseRepository.clearExercises()

        val viewModel = createViewModel("00000000-0000-0000-0000-000000000099")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertNull(state.exercise)
    }

    @Test
    fun `retry reloads exercise`() = runTest(testDispatcher) {
        // Start with no exercises — exercise not found
        fakeExerciseRepository.clearExercises()
        val viewModel = createViewModel(TestFixtures.benchPress.id.toString())
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.exercise)

        // Now add the exercise and retry
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)
        viewModel.onEvent(ExerciseDetailEvent.RetryClicked)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNotNull(state.exercise)
        assertEquals(TestFixtures.benchPress.id, state.exercise!!.id)
    }

    @Test
    fun `loading state transitions correctly`() = runTest(testDispatcher) {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)

        val viewModel = createViewModel(TestFixtures.benchPress.id.toString())

        viewModel.state.test {
            // Initial default state has isLoading = true
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            // After load completes: isLoading set to true explicitly, then false with data
            val loaded = expectMostRecentItem()
            assertFalse(loaded.isLoading)
            assertNotNull(loaded.exercise)
        }
    }

    @Test
    fun `invalid UUID exercise ID shows error`() = runTest(testDispatcher) {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)

        val viewModel = createViewModel("not-a-valid-uuid")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.exercise)
        // FakeExerciseRepository.getExerciseById returns null for invalid UUIDs
        assertNotNull(state.error)
    }

    @Test
    fun `exercise loads correct details`() = runTest(testDispatcher) {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift
        )

        val viewModel = createViewModel(TestFixtures.squat.id.toString())
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.exercise)
        assertEquals("Back Squat", state.exercise!!.name)
        assertEquals(TestFixtures.squat.muscleGroup, state.exercise!!.muscleGroup)
        assertEquals(TestFixtures.squat.category, state.exercise!!.category)
        assertEquals(TestFixtures.squat.equipment, state.exercise!!.equipment)
    }
}
