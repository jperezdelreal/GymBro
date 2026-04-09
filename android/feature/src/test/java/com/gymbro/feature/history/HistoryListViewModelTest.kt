package com.gymbro.feature.history

import com.gymbro.core.TestFixtures
import com.gymbro.core.fakes.FakeExerciseRepository
import com.gymbro.core.fakes.FakeWorkoutRepository
import com.gymbro.core.model.WorkoutHistoryItem
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HistoryListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var exerciseRepository: FakeExerciseRepository
    private lateinit var personalRecordService: PersonalRecordService
    private lateinit var viewModel: HistoryListViewModel

    @Before
    fun setup() {
        workoutRepository = FakeWorkoutRepository()
        exerciseRepository = FakeExerciseRepository()
        personalRecordService = mockk(relaxed = true)
        
        exerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift
        )
    }

    @Test
    fun `initial state loads history`() = runTest {
        coEvery { personalRecordService.getWorkoutHistory() } returns TestFixtures.workoutHistoryItems
        coEvery { personalRecordService.getPersonalRecords(any(), any()) } returns emptyList()
        
        viewModel = HistoryListViewModel(workoutRepository, exerciseRepository, personalRecordService)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `empty history shows empty list`() = runTest {
        coEvery { personalRecordService.getWorkoutHistory() } returns emptyList()
        
        viewModel = HistoryListViewModel(workoutRepository, exerciseRepository, personalRecordService)
        
        val state = viewModel.state.value
        assertTrue(state.workouts.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `error loading history sets error state`() = runTest {
        coEvery { personalRecordService.getWorkoutHistory() } throws RuntimeException("Network error")
        
        viewModel = HistoryListViewModel(workoutRepository, exerciseRepository, personalRecordService)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.error)
    }

    @Test
    fun `retry loads history again`() = runTest {
        coEvery { personalRecordService.getWorkoutHistory() } returns TestFixtures.workoutHistoryItems
        coEvery { personalRecordService.getPersonalRecords(any(), any()) } returns emptyList()
        
        viewModel = HistoryListViewModel(workoutRepository, exerciseRepository, personalRecordService)
        viewModel.onIntent(HistoryListIntent.Retry)
        
        val state = viewModel.state.value
        assertFalse(state.isLoading)
    }
}
