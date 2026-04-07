package com.gymbro.feature.exerciselibrary

import app.cash.turbine.test
import com.gymbro.core.TestFixtures
import com.gymbro.core.fakes.FakeExerciseRepository
import com.gymbro.core.model.MuscleGroup
import com.gymbro.feature.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ExerciseLibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeExerciseRepository: FakeExerciseRepository
    private lateinit var viewModel: ExerciseLibraryViewModel

    @Before
    fun setup() {
        fakeExerciseRepository = FakeExerciseRepository()
    }

    @Test
    fun `initial state loads all exercises`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift,
            TestFixtures.bicepCurl
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(4, state.exercises.size)
            assertFalse(state.isLoading)
            assertEquals("", state.searchQuery)
            assertEquals(null, state.selectedMuscleGroup)
        }
    }

    @Test
    fun `initial state with empty repository shows empty list`() = runTest {
        fakeExerciseRepository.clearExercises()
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.exercises.isEmpty())
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `filter by muscle group shows matching exercises`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift,
            TestFixtures.bicepCurl
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1) // Initial load

            viewModel.onEvent(ExerciseLibraryEvent.MuscleGroupSelected(MuscleGroup.CHEST))

            val state = awaitItem()
            assertEquals(1, state.exercises.size)
            assertEquals(TestFixtures.benchPress.id, state.exercises[0].id)
            assertEquals(MuscleGroup.CHEST, state.selectedMuscleGroup)
        }
    }

    @Test
    fun `filter by muscle group with no matches returns empty list`() = runTest {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.MuscleGroupSelected(MuscleGroup.TRICEPS))

            val state = awaitItem()
            assertTrue(state.exercises.isEmpty())
            assertEquals(MuscleGroup.TRICEPS, state.selectedMuscleGroup)
        }
    }

    @Test
    fun `search query filters exercises by name`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift,
            TestFixtures.bicepCurl
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("bench"))

            val state = awaitItem()
            assertEquals(1, state.exercises.size)
            assertEquals(TestFixtures.benchPress.id, state.exercises[0].id)
            assertEquals("bench", state.searchQuery)
        }
    }

    @Test
    fun `search query filters exercises by description`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("conventional"))

            val state = awaitItem()
            assertEquals(1, state.exercises.size)
            assertEquals(TestFixtures.deadlift.id, state.exercises[0].id)
        }
    }

    @Test
    fun `search query with no matches returns empty list`() = runTest {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("leg extension"))

            val state = awaitItem()
            assertTrue(state.exercises.isEmpty())
            assertEquals("leg extension", state.searchQuery)
        }
    }

    @Test
    fun `clearing muscle group filter shows all exercises`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.bicepCurl
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.MuscleGroupSelected(MuscleGroup.CHEST))
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.MuscleGroupSelected(null))

            val state = awaitItem()
            assertEquals(3, state.exercises.size)
            assertEquals(null, state.selectedMuscleGroup)
        }
    }

    @Test
    fun `combined filter by muscle group and search query`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift,
            TestFixtures.bicepCurl
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.MuscleGroupSelected(MuscleGroup.CHEST))
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("press"))

            val state = awaitItem()
            assertEquals(1, state.exercises.size)
            assertEquals(TestFixtures.benchPress.id, state.exercises[0].id)
            assertEquals("press", state.searchQuery)
            assertEquals(MuscleGroup.CHEST, state.selectedMuscleGroup)
        }
    }

    @Test
    fun `clicking exercise sends navigation effect`() = runTest {
        fakeExerciseRepository.setExercises(TestFixtures.benchPress)
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.effect.test {
            viewModel.onEvent(ExerciseLibraryEvent.ExerciseClicked(TestFixtures.benchPress))

            val effect = awaitItem()
            assertTrue(effect is ExerciseLibraryEffect.NavigateToDetail)
            assertEquals(TestFixtures.benchPress.id.toString(), (effect as ExerciseLibraryEffect.NavigateToDetail).exerciseId)
        }
    }

    @Test
    fun `case insensitive search query`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("BENCH"))

            val state = awaitItem()
            assertEquals(1, state.exercises.size)
            assertEquals(TestFixtures.benchPress.id, state.exercises[0].id)
        }
    }

    @Test
    fun `empty search query shows all exercises`() = runTest {
        fakeExerciseRepository.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat
        )
        viewModel = ExerciseLibraryViewModel(fakeExerciseRepository)

        viewModel.state.test {
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged("bench"))
            skipItems(1)

            viewModel.onEvent(ExerciseLibraryEvent.SearchQueryChanged(""))

            val state = awaitItem()
            assertEquals(2, state.exercises.size)
            assertEquals("", state.searchQuery)
        }
    }
}
