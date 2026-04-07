package com.gymbro.feature.workout

import app.cash.turbine.test
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.ExerciseSet
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.Workout
import com.gymbro.core.repository.WorkoutRepository
import com.gymbro.feature.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

// Test fixtures
private object TestFixtures {
    val benchPress = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Bench Press",
        muscleGroup = MuscleGroup.CHEST,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Classic barbell bench press",
        youtubeUrl = "https://youtube.com/example1"
    )

    val squat = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        name = "Back Squat",
        muscleGroup = MuscleGroup.QUADRICEPS,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Barbell back squat",
        youtubeUrl = "https://youtube.com/example2"
    )

    val deadlift = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
        name = "Deadlift",
        muscleGroup = MuscleGroup.BACK,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Conventional deadlift"
    )
}

// Fake repository for testing
private class FakeWorkoutRepository : WorkoutRepository {
    
    private val workouts = MutableStateFlow<Map<String, Workout>>(emptyMap())
    
    private fun addWorkout(workout: Workout) {
        workouts.value = workouts.value + (workout.id.toString() to workout)
    }

    override suspend fun startWorkout(): Workout {
        val workout = Workout(
            id = UUID.randomUUID(),
            name = "New Workout",
            startedAt = Instant.now(),
            completedAt = null,
            sets = emptyList(),
            notes = ""
        )
        addWorkout(workout)
        return workout
    }

    override suspend fun addSet(workoutId: String, set: ExerciseSet) {
        val workout = workouts.value[workoutId] ?: return
        val updatedWorkout = workout.copy(sets = workout.sets + set)
        workouts.value = workouts.value + (workoutId to updatedWorkout)
    }

    override suspend fun removeSet(setId: String) {
        workouts.value = workouts.value.mapValues { (_, workout) ->
            workout.copy(sets = workout.sets.filterNot { it.id.toString() == setId })
        }
    }

    override suspend fun completeWorkout(workoutId: String, durationSeconds: Long, notes: String) {
        val workout = workouts.value[workoutId] ?: return
        val updatedWorkout = workout.copy(
            completedAt = Instant.now(),
            notes = notes
        )
        workouts.value = workouts.value + (workoutId to updatedWorkout)
    }

    override suspend fun getWorkout(workoutId: String): Workout? {
        return workouts.value[workoutId]
    }

    override fun observeWorkout(workoutId: String): Flow<Workout?> {
        return workouts.map { it[workoutId] }
    }

    override fun getRecentWorkouts(limit: Int): Flow<List<Workout>> {
        return workouts.map { workoutsMap ->
            workoutsMap.values
                .filter { it.completedAt != null }
                .sortedByDescending { it.completedAt }
                .take(limit)
        }
    }

    override suspend fun getBestWeight(exerciseId: String, reps: Int): Double? {
        val exerciseUuid = try {
            UUID.fromString(exerciseId)
        } catch (e: IllegalArgumentException) {
            return null
        }
        
        return workouts.value.values
            .filter { it.completedAt != null }
            .flatMap { it.sets }
            .filter { it.exerciseId == exerciseUuid && it.reps == reps && !it.isWarmup }
            .maxOfOrNull { it.weightKg }
    }

    override suspend fun getDaysSinceLastWorkout(): Int? {
        val lastWorkout = workouts.value.values
            .filter { it.completedAt != null }
            .maxByOrNull { it.completedAt!! }
            ?: return null
        
        val now = Instant.now()
        val daysBetween = java.time.Duration.between(lastWorkout.completedAt, now).toDays()
        return daysBetween.toInt()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private lateinit var workoutRepository: FakeWorkoutRepository
    private lateinit var viewModel: ActiveWorkoutViewModel

    @Before
    fun setup() = runTest(testDispatcher) {
        workoutRepository = FakeWorkoutRepository()
        viewModel = ActiveWorkoutViewModel(workoutRepository)
        advanceUntilIdle()
    }

    @Test
    fun initialStateHasWorkoutIdSet() = runTest(testDispatcher) {
        val state = viewModel.state.value
        assertNotNull(state.workoutId)
        assertTrue(state.exercises.isEmpty())
        assertEquals(0.0, state.totalVolume, 0.001)
        assertEquals(0, state.totalSets)
    }

    @Test
    fun addExerciseClickedEmitsEffect() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(ActiveWorkoutEvent.AddExerciseClicked)
            assertEquals(ActiveWorkoutEffect.ShowExercisePicker, awaitItem())
        }
    }

    @Test
    fun exercisePickedAddsExerciseWithOneSet() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.exercises.size)
        assertEquals(TestFixtures.benchPress, state.exercises[0].exercise)
        assertEquals(1, state.exercises[0].sets.size)
    }

    @Test
    fun addSetIncrementsSetNumber() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.AddSet(0))
        advanceUntilIdle()

        val sets = viewModel.state.value.exercises[0].sets
        assertEquals(2, sets.size)
        assertEquals(1, sets[0].setNumber)
        assertEquals(2, sets[1].setNumber)
    }

    @Test
    fun addSetPrefillsWeightAndReps() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "100"))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetReps(0, 0, "8"))
        viewModel.onEvent(ActiveWorkoutEvent.AddSet(0))
        advanceUntilIdle()

        val newSet = viewModel.state.value.exercises[0].sets[1]
        assertEquals("100", newSet.weight)
        assertEquals("8", newSet.reps)
    }

    @Test
    fun updateSetWeightUpdatesCorrectly() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "100.5"))
        advanceUntilIdle()

        assertEquals("100.5", viewModel.state.value.exercises[0].sets[0].weight)
    }

    @Test
    fun toggleWarmupTogglesFlag() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.exercises[0].sets[0].isWarmup)
        viewModel.onEvent(ActiveWorkoutEvent.ToggleWarmup(0, 0))
        assertTrue(viewModel.state.value.exercises[0].sets[0].isWarmup)
    }

    @Test
    fun completeSetMarksAsCompletedAndSavesToRepository() = runTest(testDispatcher) {
        val workoutId = viewModel.state.value.workoutId!!

        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "100"))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetReps(0, 0, "8"))
        viewModel.onEvent(ActiveWorkoutEvent.CompleteSet(0, 0))
        advanceUntilIdle()

        assertTrue(viewModel.state.value.exercises[0].sets[0].isCompleted)
        val workout = workoutRepository.getWorkout(workoutId)
        assertEquals(1, workout?.sets?.size)
    }

    @Test
    fun completeSetUpdatesTotalVolume() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "100"))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetReps(0, 0, "10"))
        viewModel.onEvent(ActiveWorkoutEvent.CompleteSet(0, 0))
        advanceUntilIdle()

        assertEquals(1000.0, viewModel.state.value.totalVolume, 0.001)
        assertEquals(1, viewModel.state.value.totalSets)
    }

    @Test
    fun warmupSetsExcludedFromVolumeCalculation() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "60"))
        viewModel.onEvent(ActiveWorkoutEvent.UpdateSetReps(0, 0, "10"))
        viewModel.onEvent(ActiveWorkoutEvent.ToggleWarmup(0, 0))
        viewModel.onEvent(ActiveWorkoutEvent.CompleteSet(0, 0))
        advanceUntilIdle()

        assertEquals(0.0, viewModel.state.value.totalVolume, 0.001)
        assertEquals(0, viewModel.state.value.totalSets)
    }

    @Test
    fun removeSetRenumbersRemainingsets() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.AddSet(0))
        viewModel.onEvent(ActiveWorkoutEvent.AddSet(0))
        viewModel.onEvent(ActiveWorkoutEvent.RemoveSet(0, 1))
        advanceUntilIdle()

        val sets = viewModel.state.value.exercises[0].sets
        assertEquals(2, sets.size)
        assertEquals(1, sets[0].setNumber)
        assertEquals(2, sets[1].setNumber)
    }

    @Test
    fun removeExerciseRemovesFromList() = runTest(testDispatcher) {
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
        viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.squat))
        viewModel.onEvent(ActiveWorkoutEvent.RemoveExercise(0))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.exercises.size)
        assertEquals(TestFixtures.squat, viewModel.state.value.exercises[0].exercise)
    }

    @Test
    fun completeWorkoutSavesAndEmitsNavigateEffect() = runTest(testDispatcher) {
        val workoutId = viewModel.state.value.workoutId!!
        
        viewModel.effect.test {
            viewModel.onEvent(ActiveWorkoutEvent.ExercisePicked(TestFixtures.benchPress))
            viewModel.onEvent(ActiveWorkoutEvent.UpdateSetWeight(0, 0, "100"))
            viewModel.onEvent(ActiveWorkoutEvent.UpdateSetReps(0, 0, "8"))
            viewModel.onEvent(ActiveWorkoutEvent.CompleteSet(0, 0))
            advanceUntilIdle()

            viewModel.onEvent(ActiveWorkoutEvent.CompleteWorkout)
            advanceUntilIdle()

            val effect = awaitItem() as ActiveWorkoutEffect.NavigateToSummary
            assertEquals(800.0, effect.totalVolume, 0.001)
            assertEquals(1, effect.totalSets)
            assertEquals(1, effect.exerciseCount)

            val workout = workoutRepository.getWorkout(workoutId)
            assertNotNull(workout?.completedAt)
        }
    }

    @Test
    fun discardWorkoutEmitsNavigateBackEffect() = runTest(testDispatcher) {
        viewModel.effect.test {
            viewModel.onEvent(ActiveWorkoutEvent.DiscardWorkout)
            advanceUntilIdle()

            assertEquals(ActiveWorkoutEffect.NavigateBack, awaitItem())
        }
    }
}
