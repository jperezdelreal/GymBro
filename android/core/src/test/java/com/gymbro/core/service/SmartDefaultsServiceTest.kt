package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutSetEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SmartDefaultsServiceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var service: SmartDefaultsService

    private val exerciseId = "bench-press-id"

    @Before
    fun setup() {
        workoutDao = mockk()
        service = SmartDefaultsService(workoutDao)
    }

    @Test
    fun `getDefaults returns last set weight and reps when history exists`() = runTest {
        // Given: Exercise has 3 sets in history
        val sets = listOf(
            createSet(weight = 100.0, reps = 8, completedAt = 1000L),
            createSet(weight = 105.0, reps = 8, completedAt = 2000L),
            createSet(weight = 110.0, reps = 6, completedAt = 3000L), // Most recent
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        // When
        val result = service.getDefaults(exerciseId)

        // Then: Should return last set's values
        assertEquals(110.0, result.weight)
        assertEquals(6, result.reps)
    }

    @Test
    fun `getDefaults returns null when no history exists`() = runTest {
        // Given: No sets in history
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()

        // When
        val result = service.getDefaults(exerciseId)

        // Then: Should return null for both values
        assertNull(result.weight)
        assertNull(result.reps)
    }

    @Test
    fun `getDefaultWeight returns last weight when history exists`() = runTest {
        // Given
        val sets = listOf(
            createSet(weight = 100.0, reps = 10, completedAt = 1000L),
            createSet(weight = 102.5, reps = 10, completedAt = 2000L),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        // When
        val result = service.getDefaultWeight(exerciseId)

        // Then
        assertEquals(102.5, result)
    }

    @Test
    fun `getDefaultReps returns last reps when history exists`() = runTest {
        // Given
        val sets = listOf(
            createSet(weight = 100.0, reps = 10, completedAt = 1000L),
            createSet(weight = 100.0, reps = 12, completedAt = 2000L),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        // When
        val result = service.getDefaultReps(exerciseId)

        // Then
        assertEquals(12, result)
    }

    @Test
    fun `getDefaultWeight returns null when no history`() = runTest {
        // Given
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()

        // When
        val result = service.getDefaultWeight(exerciseId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getDefaultReps returns null when no history`() = runTest {
        // Given
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()

        // When
        val result = service.getDefaultReps(exerciseId)

        // Then
        assertNull(result)
    }

    private fun createSet(
        weight: Double,
        reps: Int,
        completedAt: Long,
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = "workout-id",
            exerciseId = exerciseId,
            setNumber = 1,
            weight = weight,
            reps = reps,
            rpe = null,
            isWarmup = false,
            completedAt = completedAt,
        )
    }
}
