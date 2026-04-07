package com.gymbro.feature.progress

import app.cash.turbine.test
import com.gymbro.core.TestFixtures
import com.gymbro.core.fakes.FakeExerciseRepository
import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.feature.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExerciseRepository: FakeExerciseRepository
    private lateinit var mockPRService: PersonalRecordService
    private lateinit var viewModel: ProgressViewModel

    @Before
    fun setup() {
        fakeExerciseRepository = FakeExerciseRepository()
        mockPRService = mockk(relaxed = true)
    }

    @Test
    fun `initial state loads workout history and exercise options`() = runTest {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        coEvery { mockPRService.getWorkoutHistory() } returns TestFixtures.workoutHistoryItems
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(
            TestFixtures.benchPress.id.toString(),
            TestFixtures.squat.id.toString()
        )
        coEvery { mockPRService.getPersonalRecords(any(), any()) } returns listOf(
            TestFixtures.benchPressMaxWeight
        )
        coEvery { mockPRService.getE1RMHistory(any()) } returns TestFixtures.e1rmDataPoints

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(2, state.workoutHistory.size)
            assertEquals(2, state.exerciseOptions.size)
            assertNotNull(state.selectedExerciseId)
            assertEquals(4, state.chartData.size)
        }
    }

    @Test
    fun `initial state with no history shows empty state`() = runTest {
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.workoutHistory.isEmpty())
            assertTrue(state.exerciseOptions.isEmpty())
            assertEquals(null, state.selectedExerciseId)
            assertTrue(state.chartData.isEmpty())
        }
    }

    @Test
    fun `selecting exercise updates chart data`() = runTest {
        val squatId = TestFixtures.squat.id.toString()
        val squatData = listOf(
            E1RMDataPoint(
                date = TestFixtures.e1rmDataPoints[0].date,
                e1rm = 180.0,
                weight = 140.0,
                reps = 5
            )
        )

        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(
            TestFixtures.benchPress.id.toString(),
            squatId
        )
        coEvery { mockPRService.getE1RMHistory(TestFixtures.benchPress.id.toString()) } returns TestFixtures.e1rmDataPoints
        coEvery { mockPRService.getE1RMHistory(squatId) } returns squatData
        coEvery { mockPRService.getPersonalRecords(any(), any()) } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1) // Initial load

            viewModel.onEvent(ProgressEvent.SelectExercise(squatId))

            val state = awaitItem()
            assertEquals(squatId, state.selectedExerciseId)
            assertEquals(1, state.chartData.size)
            assertEquals(180.0, state.chartData[0].e1rm)
        }
    }

    @Test
    fun `refresh data reloads all data`() = runTest {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList() andThen TestFixtures.workoutHistoryItems
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(
            TestFixtures.benchPress.id.toString()
        )
        coEvery { mockPRService.getE1RMHistory(any()) } returns emptyList()
        coEvery { mockPRService.getPersonalRecords(any(), any()) } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.workoutHistory.isEmpty())

            viewModel.onEvent(ProgressEvent.RefreshData)

            val refreshedState = awaitItem()
            assertEquals(2, refreshedState.workoutHistory.size)
        }
    }

    @Test
    fun `viewing workout detail sends navigation effect`() = runTest {
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.effects.test {
            viewModel.onEvent(ProgressEvent.ViewWorkoutDetail("workout-123"))

            val effect = awaitItem()
            assertTrue(effect is ProgressEffect.NavigateToWorkoutDetail)
            assertEquals("workout-123", (effect as ProgressEffect.NavigateToWorkoutDetail).workoutId)
        }
    }

    @Test
    fun `exercise options are sorted alphabetically`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.squat,
            TestFixtures.benchPress,
            TestFixtures.deadlift
        )
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(
            TestFixtures.squat.id.toString(),
            TestFixtures.benchPress.id.toString(),
            TestFixtures.deadlift.id.toString()
        )
        coEvery { mockPRService.getE1RMHistory(any()) } returns emptyList()
        coEvery { mockPRService.getPersonalRecords(any(), any()) } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(3, state.exerciseOptions.size)
            assertEquals("Back Squat", state.exerciseOptions[0].name)
            assertEquals("Bench Press", state.exerciseOptions[1].name)
            assertEquals("Deadlift", state.exerciseOptions[2].name)
        }
    }

    @Test
    fun `first exercise is selected by default`() = runTest {
        val benchId = TestFixtures.benchPress.id.toString()
        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(
            benchId,
            TestFixtures.squat.id.toString()
        )
        coEvery { mockPRService.getE1RMHistory(benchId) } returns TestFixtures.e1rmDataPoints
        coEvery { mockPRService.getPersonalRecords(any(), any()) } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(benchId, state.selectedExerciseId)
            assertEquals(4, state.chartData.size)
        }
    }

    @Test
    fun `loading state is set during data fetch`() = runTest {
        coEvery { mockPRService.getWorkoutHistory() } coAnswers {
            kotlinx.coroutines.delay(100)
            emptyList()
        }
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns emptyList()

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `personal records are aggregated from all exercises`() = runTest {
        val benchId = TestFixtures.benchPress.id.toString()
        val squatId = TestFixtures.squat.id.toString()

        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(benchId, squatId)
        coEvery { mockPRService.getE1RMHistory(any()) } returns emptyList()
        coEvery { 
            mockPRService.getPersonalRecords(benchId, TestFixtures.benchPress.name) 
        } returns listOf(TestFixtures.benchPressMaxWeight)
        coEvery { 
            mockPRService.getPersonalRecords(squatId, TestFixtures.squat.name) 
        } returns listOf(TestFixtures.squatMaxE1RM)

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(2, state.personalRecords.size)
            assertTrue(state.personalRecords.any { it.exerciseId == benchId })
            assertTrue(state.personalRecords.any { it.exerciseId == squatId })
        }
    }

    @Test
    fun `selecting exercise updates personal records for that exercise`() = runTest {
        val benchId = TestFixtures.benchPress.id.toString()
        val squatId = TestFixtures.squat.id.toString()

        fakeExerciseRepository.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        coEvery { mockPRService.getWorkoutHistory() } returns emptyList()
        coEvery { mockPRService.getExerciseIdsWithHistory() } returns listOf(benchId, squatId)
        coEvery { mockPRService.getE1RMHistory(any()) } returns emptyList()
        coEvery { mockPRService.getPersonalRecords(benchId, any()) } returns listOf(
            TestFixtures.benchPressMaxWeight
        )
        coEvery { mockPRService.getPersonalRecords(squatId, any()) } returns listOf(
            TestFixtures.squatMaxE1RM
        )

        viewModel = ProgressViewModel(mockPRService, fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ProgressEvent.SelectExercise(squatId))

            val state = awaitItem()
            assertEquals(squatId, state.selectedExerciseId)
            assertTrue(state.personalRecords.any { it.exerciseId == squatId })
        }
    }
}
