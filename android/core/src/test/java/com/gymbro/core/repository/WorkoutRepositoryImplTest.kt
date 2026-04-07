package com.gymbro.core.repository

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.model.ExerciseSet
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class WorkoutRepositoryImplTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var repository: WorkoutRepositoryImpl

    @Before
    fun setup() {
        workoutDao = mockk()
        repository = WorkoutRepositoryImpl(workoutDao)
    }

    @Test
    fun `startWorkout creates entity correctly`() = runTest {
        // Given
        val entitySlot = slot<WorkoutEntity>()
        coEvery { workoutDao.insertWorkout(capture(entitySlot)) } returns Unit

        // When
        val workout = repository.startWorkout()

        // Then
        coVerify { workoutDao.insertWorkout(any()) }
        val capturedEntity = entitySlot.captured
        assertNotNull(capturedEntity)
        assertEquals(workout.id.toString(), capturedEntity.id)
        assertTrue(capturedEntity.startedAt > 0)
        assertNull(capturedEntity.completedAt)
        assertEquals(false, capturedEntity.completed)
    }

    @Test
    fun `addSet inserts set entity with correct foreign key`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val exerciseSet = ExerciseSet(
            id = UUID.randomUUID(),
            exerciseId = UUID.randomUUID(),
            weightKg = 100.0,
            reps = 5,
            rpe = 8.0,
            isWarmup = false,
            completedAt = Instant.now()
        )
        val setSlot = slot<WorkoutSetEntity>()
        coEvery { workoutDao.insertSet(capture(setSlot)) } returns Unit

        // When
        repository.addSet(workoutId, exerciseSet)

        // Then
        coVerify { workoutDao.insertSet(any()) }
        val capturedSet = setSlot.captured
        assertEquals(exerciseSet.id.toString(), capturedSet.id)
        assertEquals(workoutId, capturedSet.workoutId)
        assertEquals(exerciseSet.exerciseId.toString(), capturedSet.exerciseId)
        assertEquals(exerciseSet.weightKg, capturedSet.weight, 0.01)
        assertEquals(exerciseSet.reps, capturedSet.reps)
        assertEquals(exerciseSet.rpe!!, capturedSet.rpe!!, 0.01)
        assertEquals(exerciseSet.isWarmup, capturedSet.isWarmup)
    }

    @Test
    fun `removeSet deletes correctly`() = runTest {
        // Given
        val setId = UUID.randomUUID().toString()
        coEvery { workoutDao.deleteSet(setId) } returns Unit

        // When
        repository.removeSet(setId)

        // Then
        coVerify { workoutDao.deleteSet(setId) }
    }

    @Test
    fun `completeWorkout updates entity with end time`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val workoutEntity = WorkoutEntity(
            id = workoutId,
            startedAt = System.currentTimeMillis() - 3600000,
            completed = false
        )
        val workoutWithSets = WorkoutWithSets(workoutEntity, emptyList())
        val updatedSlot = slot<WorkoutEntity>()
        
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns workoutWithSets
        coEvery { workoutDao.updateWorkout(capture(updatedSlot)) } returns Unit

        val durationSeconds = 3600L
        val notes = "Great workout!"

        // When
        repository.completeWorkout(workoutId, durationSeconds, notes)

        // Then
        coVerify { workoutDao.updateWorkout(any()) }
        val updated = updatedSlot.captured
        assertNotNull(updated.completedAt)
        assertEquals(durationSeconds, updated.durationSeconds)
        assertEquals(notes, updated.notes)
        assertEquals(true, updated.completed)
    }

    @Test
    fun `completeWorkout does nothing for non-existent workout`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns null

        // When
        repository.completeWorkout(workoutId, 3600L, "notes")

        // Then
        coVerify(exactly = 0) { workoutDao.updateWorkout(any()) }
    }

    @Test
    fun `getWorkout returns workout with sets`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val exerciseId = UUID.randomUUID().toString()
        val setId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        
        val workoutEntity = WorkoutEntity(
            id = workoutId,
            startedAt = now,
            completedAt = now + 3600000,
            completed = true,
            notes = "Test workout"
        )
        val setEntity = WorkoutSetEntity(
            id = setId,
            workoutId = workoutId,
            exerciseId = exerciseId,
            setNumber = 1,
            weight = 100.0,
            reps = 5,
            rpe = 8.0,
            isWarmup = false,
            completedAt = now
        )
        val workoutWithSets = WorkoutWithSets(workoutEntity, listOf(setEntity))
        
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns workoutWithSets

        // When
        val result = repository.getWorkout(workoutId)

        // Then
        assertNotNull(result)
        assertEquals(workoutId, result!!.id.toString())
        assertEquals(1, result.sets.size)
        assertEquals(setId, result.sets[0].id.toString())
        assertEquals(exerciseId, result.sets[0].exerciseId.toString())
        assertEquals(100.0, result.sets[0].weightKg, 0.01)
        assertEquals("Test workout", result.notes)
    }

    @Test
    fun `getWorkout returns null for non-existent workout`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns null

        // When
        val result = repository.getWorkout(workoutId)

        // Then
        assertNull(result)
    }

    @Test
    fun `observeWorkout returns Flow of workout`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val workoutEntity = WorkoutEntity(
            id = workoutId,
            startedAt = System.currentTimeMillis(),
            completed = false
        )
        val workoutWithSets = WorkoutWithSets(workoutEntity, emptyList())
        
        every { workoutDao.observeWorkoutWithSets(workoutId) } returns flowOf(workoutWithSets)

        // When
        val result = repository.observeWorkout(workoutId).first()

        // Then
        assertNotNull(result)
        assertEquals(workoutId, result!!.id.toString())
    }

    @Test
    fun `getRecentWorkouts returns sorted list`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val workout1 = WorkoutEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now - 86400000,
            completedAt = now - 86300000,
            completed = true
        )
        val workout2 = WorkoutEntity(
            id = UUID.randomUUID().toString(),
            startedAt = now - 172800000,
            completedAt = now - 172700000,
            completed = true
        )
        
        val workoutsList = listOf(
            WorkoutWithSets(workout1, emptyList()),
            WorkoutWithSets(workout2, emptyList())
        )
        
        every { workoutDao.getRecentWorkouts(20) } returns flowOf(workoutsList)

        // When
        val result = repository.getRecentWorkouts().first()

        // Then
        assertEquals(2, result.size)
        assertEquals(workout1.id, result[0].id.toString())
        assertEquals(workout2.id, result[1].id.toString())
    }

    @Test
    fun `getRecentWorkouts respects limit parameter`() = runTest {
        // Given
        every { workoutDao.getRecentWorkouts(10) } returns flowOf(emptyList())

        // When
        repository.getRecentWorkouts(10).first()

        // Then
        coVerify { workoutDao.getRecentWorkouts(10) }
    }

    @Test
    fun `getBestWeight returns max weight for exercise and reps`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        val reps = 5
        val expectedWeight = 120.0
        
        coEvery { workoutDao.getBestWeight(exerciseId, reps) } returns expectedWeight

        // When
        val result = repository.getBestWeight(exerciseId, reps)

        // Then
        assertEquals(expectedWeight, result)
    }

    @Test
    fun `getBestWeight returns null when no history exists`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        val reps = 5
        
        coEvery { workoutDao.getBestWeight(exerciseId, reps) } returns null

        // When
        val result = repository.getBestWeight(exerciseId, reps)

        // Then
        assertNull(result)
    }

    @Test
    fun `getDaysSinceLastWorkout calculates days correctly`() = runTest {
        // Given
        val twoDaysAgo = Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli()
        coEvery { workoutDao.getLastCompletedTimestamp() } returns twoDaysAgo

        // When
        val result = repository.getDaysSinceLastWorkout()

        // Then
        assertNotNull(result)
        assertTrue(result!! >= 1) // At least 1 day
    }

    @Test
    fun `getDaysSinceLastWorkout returns null when no workouts exist`() = runTest {
        // Given
        coEvery { workoutDao.getLastCompletedTimestamp() } returns null

        // When
        val result = repository.getDaysSinceLastWorkout()

        // Then
        assertNull(result)
    }

    @Test
    fun `addSet to completed workout still inserts set`() = runTest {
        // Given - repository doesn't validate workout state, DAO handles it
        val workoutId = UUID.randomUUID().toString()
        val exerciseSet = ExerciseSet(
            id = UUID.randomUUID(),
            exerciseId = UUID.randomUUID(),
            weightKg = 50.0,
            reps = 10,
            isWarmup = true,
            completedAt = Instant.now()
        )
        coEvery { workoutDao.insertSet(any()) } returns Unit

        // When
        repository.addSet(workoutId, exerciseSet)

        // Then
        coVerify { workoutDao.insertSet(any()) }
    }

    @Test
    fun `domain conversion handles null RPE`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val workoutEntity = WorkoutEntity(
            id = workoutId,
            startedAt = System.currentTimeMillis(),
            completed = false
        )
        val setEntity = WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = workoutId,
            exerciseId = UUID.randomUUID().toString(),
            setNumber = 1,
            weight = 100.0,
            reps = 5,
            rpe = null, // No RPE
            isWarmup = false,
            completedAt = System.currentTimeMillis()
        )
        val workoutWithSets = WorkoutWithSets(workoutEntity, listOf(setEntity))
        
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns workoutWithSets

        // When
        val result = repository.getWorkout(workoutId)

        // Then
        assertNotNull(result)
        assertEquals(1, result!!.sets.size)
        assertNull(result.sets[0].rpe)
    }

    @Test
    fun `domain conversion handles null completedAt`() = runTest {
        // Given
        val workoutId = UUID.randomUUID().toString()
        val workoutEntity = WorkoutEntity(
            id = workoutId,
            startedAt = System.currentTimeMillis(),
            completedAt = null, // Still active
            completed = false
        )
        val workoutWithSets = WorkoutWithSets(workoutEntity, emptyList())
        
        coEvery { workoutDao.getWorkoutWithSets(workoutId) } returns workoutWithSets

        // When
        val result = repository.getWorkout(workoutId)

        // Then
        assertNotNull(result)
        assertNull(result!!.completedAt)
    }
}
