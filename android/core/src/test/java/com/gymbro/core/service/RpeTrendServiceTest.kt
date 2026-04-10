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

class RpeTrendServiceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var service: RpeTrendService

    private val exerciseId = "squat-id"

    @Before
    fun setup() {
        workoutDao = mockk()
        service = RpeTrendService(workoutDao)
    }

    @Test
    fun `returns null when no history exists`() = runTest {
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()
        assertNull(service.getTrend(exerciseId))
    }

    @Test
    fun `returns null when no RPE data exists`() = runTest {
        val sets = listOf(
            createSet(rpe = null, completedAt = System.currentTimeMillis()),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets
        assertNull(service.getTrend(exerciseId))
    }

    @Test
    fun `detects rising trend when recent RPE is higher than previous`() = runTest {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val sets = listOf(
            // Previous week: RPE ~7
            createSet(rpe = 7.0, completedAt = now - 10 * oneDay),
            createSet(rpe = 7.0, completedAt = now - 10 * oneDay),
            createSet(rpe = 7.5, completedAt = now - 9 * oneDay),
            // Recent week: RPE ~9
            createSet(rpe = 9.0, completedAt = now - 2 * oneDay),
            createSet(rpe = 9.0, completedAt = now - 2 * oneDay),
            createSet(rpe = 9.5, completedAt = now - 1 * oneDay),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getTrend(exerciseId)
        assertNotNull(result)
        assertEquals(RpeTrendService.TrendDirection.RISING, result!!.trend)
        assertTrue(result.isFatigueWarning)
    }

    @Test
    fun `detects falling trend when recent RPE is lower`() = runTest {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val sets = listOf(
            // Previous week: RPE ~9
            createSet(rpe = 9.0, completedAt = now - 10 * oneDay),
            createSet(rpe = 9.0, completedAt = now - 10 * oneDay),
            // Recent week: RPE ~7
            createSet(rpe = 7.0, completedAt = now - 2 * oneDay),
            createSet(rpe = 7.0, completedAt = now - 2 * oneDay),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getTrend(exerciseId)
        assertNotNull(result)
        assertEquals(RpeTrendService.TrendDirection.FALLING, result!!.trend)
        assertFalse(result.isFatigueWarning)
    }

    @Test
    fun `detects stable trend when RPE change is small`() = runTest {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        val sets = listOf(
            // Previous week
            createSet(rpe = 8.0, completedAt = now - 10 * oneDay),
            createSet(rpe = 8.0, completedAt = now - 10 * oneDay),
            // Recent week — similar RPE
            createSet(rpe = 8.0, completedAt = now - 2 * oneDay),
            createSet(rpe = 8.5, completedAt = now - 2 * oneDay),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getTrend(exerciseId)
        assertNotNull(result)
        assertEquals(RpeTrendService.TrendDirection.STABLE, result!!.trend)
        assertFalse(result.isFatigueWarning)
    }

    @Test
    fun `getFatigueWarnings returns only exercises with fatigue flag`() = runTest {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L

        // Exercise with rising RPE (fatigue)
        val fatiguedSets = listOf(
            createSet(rpe = 7.0, completedAt = now - 10 * oneDay, exerciseId = "ex-1"),
            createSet(rpe = 9.5, completedAt = now - 1 * oneDay, exerciseId = "ex-1"),
        )

        // Exercise with stable RPE (no fatigue)
        val stableSets = listOf(
            createSet(rpe = 8.0, completedAt = now - 10 * oneDay, exerciseId = "ex-2"),
            createSet(rpe = 8.0, completedAt = now - 1 * oneDay, exerciseId = "ex-2"),
        )

        coEvery { workoutDao.getExerciseIdsWithHistory() } returns listOf("ex-1", "ex-2")
        coEvery { workoutDao.getSetsByExercise("ex-1") } returns fatiguedSets
        coEvery { workoutDao.getSetsByExercise("ex-2") } returns stableSets

        val warnings = service.getFatigueWarnings()
        assertEquals(1, warnings.size)
        assertEquals("ex-1", warnings[0].exerciseId)
    }

    private fun createSet(
        rpe: Double?,
        completedAt: Long,
        exerciseId: String = this.exerciseId,
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = "workout-id",
            exerciseId = exerciseId,
            setNumber = 1,
            weight = 100.0,
            reps = 5,
            rpe = rpe,
            isWarmup = false,
            completedAt = completedAt,
        )
    }
}
