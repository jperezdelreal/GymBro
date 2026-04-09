package com.gymbro.core.service

import com.gymbro.core.TestFixtures
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AnalyticsServiceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var prService: PersonalRecordService
    private lateinit var service: AnalyticsService

    @Before
    fun setup() {
        workoutDao = mockk()
        exerciseRepository = mockk()
        prService = mockk()
        service = AnalyticsService(workoutDao, exerciseRepository, prService)
    }

    @Test
    fun `getWeeklyVolumeData returns data for requested weeks`() = runTest {
        val workouts = createSampleWorkouts()
        coEvery { workoutDao.getAllCompletedWorkouts() } returns workouts

        val result = service.getWeeklyVolumeData(8)

        assertEquals(8, result.size)
        assertTrue(result.all { it.totalVolume >= 0 })
        assertTrue(result.all { it.workoutCount >= 0 })
    }

    @Test
    fun `getConsistencyMetrics returns zero for empty history`() = runTest {
        coEvery { workoutDao.getAllCompletedWorkouts() } returns emptyList()

        val result = service.getConsistencyMetrics()

        assertEquals(0, result.currentStreak)
        assertEquals(0, result.longestStreak)
        assertEquals(0.0, result.averageWorkoutsPerWeek, 0.01)
        assertEquals(0, result.consistencyScore)
        assertTrue(result.workoutDates.isEmpty())
    }

    @Test
    fun `getMuscleGroupDistribution groups by muscle group`() = runTest {
        val benchPressSet = createSet(
            exerciseId = TestFixtures.benchPress.id.toString(),
            weight = 100.0,
            reps = 5
        )
        val squatSet = createSet(
            exerciseId = TestFixtures.squat.id.toString(),
            weight = 140.0,
            reps = 3
        )
        
        val workout = createWorkout("w1", Instant.now(), listOf(benchPressSet, squatSet))
        
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)
        coEvery { exerciseRepository.getExerciseById(TestFixtures.benchPress.id.toString()) } returns TestFixtures.benchPress
        coEvery { exerciseRepository.getExerciseById(TestFixtures.squat.id.toString()) } returns TestFixtures.squat

        val result = service.getMuscleGroupDistribution()

        assertTrue(result.size >= 2)
        assertTrue(result.any { it.muscleGroup == "Chest" })
        assertTrue(result.any { it.muscleGroup == "Quadriceps" })
    }

    @Test
    fun `getTopExercises returns sorted by volume`() = runTest {
        val benchPressSet = createSet(
            exerciseId = TestFixtures.benchPress.id.toString(),
            weight = 100.0,
            reps = 5
        )
        val squatSet = createSet(
            exerciseId = TestFixtures.squat.id.toString(),
            weight = 140.0,
            reps = 5
        )
        
        val workout = createWorkout("w1", Instant.now(), listOf(benchPressSet, squatSet))
        
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)
        coEvery { exerciseRepository.getExerciseById(TestFixtures.benchPress.id.toString()) } returns TestFixtures.benchPress
        coEvery { exerciseRepository.getExerciseById(TestFixtures.squat.id.toString()) } returns TestFixtures.squat

        val result = service.getTopExercises(10)

        assertEquals(2, result.size)
        // Squat should be first (700 volume) vs Bench (500 volume)
        assertEquals(TestFixtures.squat.name, result[0].exerciseName)
        assertEquals(700.0, result[0].totalVolume, 0.01)
    }

    @Test
    fun `getConsistencyMetrics calculates metrics correctly`() = runTest {
        val today = LocalDate.now()
        val workout = createWorkout(
            "w1",
            today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            listOf(createSet(weight = 100.0, reps = 5))
        )
        
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getConsistencyMetrics()

        assertTrue(result.averageWorkoutsPerWeek >= 0)
        assertTrue(result.consistencyScore >= 0)
        assertEquals(1, result.workoutDates.size)
    }

    private fun createWorkout(
        id: String,
        startedAt: Instant,
        sets: List<WorkoutSetEntity> = emptyList()
    ): WorkoutWithSets {
        val workout = WorkoutEntity(
            id = id,
            startedAt = startedAt.toEpochMilli(),
            completed = true,
            durationSeconds = 3600
        )
        return WorkoutWithSets(workout = workout, sets = sets)
    }

    private fun createSet(
        exerciseId: String = UUID.randomUUID().toString(),
        weight: Double,
        reps: Int,
        isWarmup: Boolean = false
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = "workout-id",
            exerciseId = exerciseId,
            setNumber = 1,
            weight = weight,
            reps = reps,
            isWarmup = isWarmup,
            completedAt = Instant.now().toEpochMilli()
        )
    }

    private fun createSampleWorkouts(): List<WorkoutWithSets> {
        val now = LocalDate.now()
        return (0..7).map { weekOffset ->
            val weekStart = now.minusWeeks(weekOffset.toLong())
            createWorkout(
                id = "w$weekOffset",
                startedAt = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                sets = listOf(createSet(weight = 100.0, reps = 5))
            )
        }
    }
}
