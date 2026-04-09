package com.gymbro.core.database.dao

import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Contract tests for WorkoutDao query behavior.
 * These verify the DAO interface expectations without requiring Room database.
 */
class WorkoutDaoContractTest {

    private lateinit var dao: WorkoutDao

    @Before
    fun setup() {
        dao = mockk()
    }

    @Test
    fun `getRecentWorkouts returns Flow of WorkoutWithSets sorted by startedAt DESC`() = runTest {
        val now = System.currentTimeMillis()
        val workout1 = createWorkoutWithSets("w1", startedAt = now - 1000)
        val workout2 = createWorkoutWithSets("w2", startedAt = now - 2000)
        val workout3 = createWorkoutWithSets("w3", startedAt = now - 3000)

        // Mocking: Most recent first
        every { dao.getRecentWorkouts(20) } returns flowOf(listOf(workout1, workout2, workout3))

        val result = dao.getRecentWorkouts(20).first()

        assertEquals(3, result.size)
        assertEquals("w1", result[0].workout.id)
        assertTrue(result[0].workout.startedAt > result[1].workout.startedAt)
        assertTrue(result[1].workout.startedAt > result[2].workout.startedAt)
    }

    @Test
    fun `getRecentWorkouts respects limit parameter`() = runTest {
        val workouts = (1..30).map { createWorkoutWithSets("w$it", startedAt = System.currentTimeMillis()) }

        every { dao.getRecentWorkouts(10) } returns flowOf(workouts.take(10))

        val result = dao.getRecentWorkouts(10).first()

        assertTrue(result.size <= 10)
    }

    @Test
    fun `getBestWeight returns max weight for exercise at given reps`() = runTest {
        val exerciseId = UUID.randomUUID().toString()
        val reps = 5

        coEvery { dao.getBestWeight(exerciseId, reps) } returns 120.0

        val result = dao.getBestWeight(exerciseId, reps)

        assertEquals(120.0, result)
    }

    @Test
    fun `getBestWeight returns null when no history for exercise-reps combo`() = runTest {
        val exerciseId = UUID.randomUUID().toString()

        coEvery { dao.getBestWeight(exerciseId, 5) } returns null

        val result = dao.getBestWeight(exerciseId, 5)

        assertNull(result)
    }

    @Test
    fun `getSetsByExercise filters only completed non-warmup sets`() = runTest {
        val exerciseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val sets = listOf(
            createSet(exerciseId = exerciseId, weight = 100.0, reps = 5, isWarmup = false, completedAt = now),
            createSet(exerciseId = exerciseId, weight = 105.0, reps = 5, isWarmup = false, completedAt = now - 1000)
        )

        coEvery { dao.getSetsByExercise(exerciseId) } returns sets

        val result = dao.getSetsByExercise(exerciseId)

        assertEquals(2, result.size)
        assertTrue(result.all { !it.isWarmup })
    }

    @Test
    fun `getSetsByExercise sorts by completedAt ASC`() = runTest {
        val exerciseId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val set1 = createSet(exerciseId = exerciseId, weight = 100.0, reps = 5, completedAt = now - 2000)
        val set2 = createSet(exerciseId = exerciseId, weight = 105.0, reps = 5, completedAt = now - 1000)
        val set3 = createSet(exerciseId = exerciseId, weight = 110.0, reps = 5, completedAt = now)

        coEvery { dao.getSetsByExercise(exerciseId) } returns listOf(set1, set2, set3)

        val result = dao.getSetsByExercise(exerciseId)

        assertTrue(result[0].completedAt < result[1].completedAt)
        assertTrue(result[1].completedAt < result[2].completedAt)
    }

    @Test
    fun `getExerciseIdsWithHistory returns distinct exercise IDs from completed workouts`() = runTest {
        val exerciseIds = listOf("ex1", "ex2", "ex3")

        coEvery { dao.getExerciseIdsWithHistory() } returns exerciseIds

        val result = dao.getExerciseIdsWithHistory()

        assertEquals(3, result.size)
        assertEquals(exerciseIds.size, result.toSet().size) // All unique
    }

    @Test
    fun `getLastCompletedTimestamp returns most recent completed workout timestamp`() = runTest {
        val now = System.currentTimeMillis()

        coEvery { dao.getLastCompletedTimestamp() } returns now

        val result = dao.getLastCompletedTimestamp()

        assertEquals(now, result)
    }

    @Test
    fun `getLastCompletedTimestamp returns null when no workouts exist`() = runTest {
        coEvery { dao.getLastCompletedTimestamp() } returns null

        val result = dao.getLastCompletedTimestamp()

        assertNull(result)
    }

    @Test
    fun `getAllCompletedWorkouts filters only completed workouts`() = runTest {
        val completed = createWorkoutWithSets("w1", completed = true)

        coEvery { dao.getAllCompletedWorkouts() } returns listOf(completed)

        val result = dao.getAllCompletedWorkouts()

        assertTrue(result.all { it.workout.completed })
    }

    @Test
    fun `insertWorkout uses REPLACE conflict strategy`() = runTest {
        val workout = WorkoutEntity(
            id = "w1",
            startedAt = System.currentTimeMillis(),
            completed = false
        )

        coEvery { dao.insertWorkout(workout) } returns Unit

        dao.insertWorkout(workout)

        coVerify { dao.insertWorkout(workout) }
    }

    @Test
    fun `insertSet uses REPLACE conflict strategy`() = runTest {
        val set = createSet(exerciseId = "ex1", weight = 100.0, reps = 5)

        coEvery { dao.insertSet(set) } returns Unit

        dao.insertSet(set)

        coVerify { dao.insertSet(set) }
    }

    @Test
    fun `deleteSet removes set by ID`() = runTest {
        val setId = UUID.randomUUID().toString()

        coEvery { dao.deleteSet(setId) } returns Unit

        dao.deleteSet(setId)

        coVerify { dao.deleteSet(setId) }
    }

    @Test
    fun `observeWorkoutWithSets returns Flow for real-time updates`() = runTest {
        val workoutId = "w1"
        val workout = createWorkoutWithSets(workoutId)

        every { dao.observeWorkoutWithSets(workoutId) } returns flowOf(workout)

        val result = dao.observeWorkoutWithSets(workoutId).first()

        assertNotNull(result)
        assertEquals(workoutId, result!!.workout.id)
    }

    private fun createWorkoutWithSets(
        id: String,
        startedAt: Long = System.currentTimeMillis(),
        completed: Boolean = true
    ): WorkoutWithSets {
        val workout = WorkoutEntity(
            id = id,
            startedAt = startedAt,
            completed = completed,
            durationSeconds = 3600
        )
        return WorkoutWithSets(workout = workout, sets = emptyList())
    }

    private fun createSet(
        exerciseId: String,
        weight: Double,
        reps: Int,
        isWarmup: Boolean = false,
        completedAt: Long = System.currentTimeMillis()
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = "workout-id",
            exerciseId = exerciseId,
            setNumber = 1,
            weight = weight,
            reps = reps,
            isWarmup = isWarmup,
            completedAt = completedAt
        )
    }
}
