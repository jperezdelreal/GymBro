package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.dao.WorkoutWithSets
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.model.RecordType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class PersonalRecordServiceTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var service: PersonalRecordService

    private val exerciseId = "bench-press-id"
    private val exerciseName = "Bench Press"
    private val baseTime = Instant.parse("2024-01-01T12:00:00Z")

    @Before
    fun setup() {
        workoutDao = mockk()
        service = PersonalRecordService(workoutDao)
    }

    // ========== calculateE1RM Tests ==========

    @Test
    fun `calculateE1RM with 1 rep returns weight unchanged`() {
        val result = service.calculateE1RM(100.0, 1)
        assertEquals(100.0, result, 0.01)
    }

    @Test
    fun `calculateE1RM with 5 reps uses Brzycki formula`() {
        // E1RM = 100 * (36 / (37 - 5)) = 100 * (36/32) = 112.5
        val result = service.calculateE1RM(100.0, 5)
        assertEquals(112.5, result, 0.01)
    }

    @Test
    fun `calculateE1RM with 10 reps`() {
        // E1RM = 80 * (36 / (37 - 10)) = 80 * (36/27) = 106.67
        val result = service.calculateE1RM(80.0, 10)
        assertEquals(106.67, result, 0.01)
    }

    @Test
    fun `calculateE1RM with high reps 20+`() {
        // E1RM = 60 * (36 / (37 - 20)) = 60 * (36/17) = 127.06
        val result = service.calculateE1RM(60.0, 20)
        assertEquals(127.06, result, 0.01)
    }

    @Test
    fun `calculateE1RM with very high reps 36+ caps at 2x weight`() {
        // When reps >= 37, denominator <= 0, returns weight * 2
        val result = service.calculateE1RM(100.0, 37)
        assertEquals(200.0, result, 0.01)
        
        val result2 = service.calculateE1RM(100.0, 50)
        assertEquals(200.0, result2, 0.01)
    }

    @Test
    fun `calculateE1RM with zero weight returns zero`() {
        val result = service.calculateE1RM(0.0, 5)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateE1RM with negative weight returns zero`() {
        val result = service.calculateE1RM(-50.0, 5)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateE1RM with zero reps returns zero`() {
        val result = service.calculateE1RM(100.0, 0)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateE1RM with negative reps returns zero`() {
        val result = service.calculateE1RM(100.0, -5)
        assertEquals(0.0, result, 0.01)
    }

    @Test
    fun `calculateE1RM with decimal weights`() {
        // E1RM = 102.5 * (36 / (37 - 5)) = 102.5 * (36/32) = 115.31
        val result = service.calculateE1RM(102.5, 5)
        assertEquals(115.31, result, 0.01)
    }

    // ========== getPersonalRecords Tests ==========

    @Test
    fun `getPersonalRecords returns empty list when no sets exist`() = runTest {
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()

        val result = service.getPersonalRecords(exerciseId, exerciseName)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getPersonalRecords returns all 4 record types`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = baseTime),
            createSet(weight = 90.0, reps = 10, completedAt = baseTime.plus(1, ChronoUnit.DAYS)),
            createSet(weight = 120.0, reps = 1, completedAt = baseTime.plus(2, ChronoUnit.DAYS)),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)

        assertEquals(4, result.size)
        assertTrue(result.any { it.type == RecordType.MAX_WEIGHT })
        assertTrue(result.any { it.type == RecordType.MAX_REPS })
        assertTrue(result.any { it.type == RecordType.MAX_VOLUME })
        assertTrue(result.any { it.type == RecordType.MAX_E1RM })
    }

    @Test
    fun `getPersonalRecords MAX_WEIGHT finds highest weight`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = baseTime),
            createSet(weight = 120.0, reps = 3, completedAt = baseTime.plus(1, ChronoUnit.DAYS)),
            createSet(weight = 110.0, reps = 4, completedAt = baseTime.plus(2, ChronoUnit.DAYS)),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)
        val maxWeight = result.first { it.type == RecordType.MAX_WEIGHT }

        assertEquals(120.0, maxWeight.value, 0.01)
        assertEquals(baseTime.plus(1, ChronoUnit.DAYS), maxWeight.date)
        assertEquals(100.0, maxWeight.previousValue)
    }

    @Test
    fun `getPersonalRecords MAX_WEIGHT with no previous value`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = baseTime),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)
        val maxWeight = result.first { it.type == RecordType.MAX_WEIGHT }

        assertEquals(100.0, maxWeight.value, 0.01)
        assertNull(maxWeight.previousValue)
    }

    @Test
    fun `getPersonalRecords MAX_REPS finds highest reps`() = runTest {
        val sets = listOf(
            createSet(weight = 60.0, reps = 8, completedAt = baseTime),
            createSet(weight = 50.0, reps = 15, completedAt = baseTime.plus(1, ChronoUnit.DAYS)),
            createSet(weight = 55.0, reps = 10, completedAt = baseTime.plus(2, ChronoUnit.DAYS)),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)
        val maxReps = result.first { it.type == RecordType.MAX_REPS }

        assertEquals(15.0, maxReps.value, 0.01)
        assertEquals(baseTime.plus(1, ChronoUnit.DAYS), maxReps.date)
        assertEquals(8.0, maxReps.previousValue)
    }

    @Test
    fun `getPersonalRecords MAX_VOLUME finds highest weight times reps`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = baseTime),      // volume = 500
            createSet(weight = 80.0, reps = 10, completedAt = baseTime.plus(1, ChronoUnit.DAYS)),  // volume = 800
            createSet(weight = 90.0, reps = 8, completedAt = baseTime.plus(2, ChronoUnit.DAYS)),   // volume = 720
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)
        val maxVolume = result.first { it.type == RecordType.MAX_VOLUME }

        assertEquals(800.0, maxVolume.value, 0.01)
        assertEquals(baseTime.plus(1, ChronoUnit.DAYS), maxVolume.date)
        assertEquals(500.0, maxVolume.previousValue)
    }

    @Test
    fun `getPersonalRecords MAX_E1RM uses Brzycki formula`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 1, completedAt = baseTime),      // E1RM = 100
            createSet(weight = 100.0, reps = 5, completedAt = baseTime.plus(1, ChronoUnit.DAYS)),  // E1RM = 112.5
            createSet(weight = 110.0, reps = 3, completedAt = baseTime.plus(2, ChronoUnit.DAYS)),  // E1RM = 116.47
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)
        val maxE1RM = result.first { it.type == RecordType.MAX_E1RM }

        assertEquals(116.47, maxE1RM.value, 0.01)
        assertEquals(baseTime.plus(2, ChronoUnit.DAYS), maxE1RM.date)
        assertEquals(112.5, maxE1RM.previousValue)
    }

    @Test
    fun `getPersonalRecords all records have correct exercise info`() = runTest {
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = baseTime),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getPersonalRecords(exerciseId, exerciseName)

        result.forEach { record ->
            assertEquals(exerciseId, record.exerciseId)
            assertEquals(exerciseName, record.exerciseName)
        }
    }

    // ========== getE1RMHistory Tests ==========

    @Test
    fun `getE1RMHistory returns empty list when no sets exist`() = runTest {
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns emptyList()

        val result = service.getE1RMHistory(exerciseId)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getE1RMHistory groups sets by day`() = runTest {
        val day1 = baseTime
        val day2 = baseTime.plus(1, ChronoUnit.DAYS)
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = day1),
            createSet(weight = 105.0, reps = 4, completedAt = day1.plus(2, ChronoUnit.HOURS)),
            createSet(weight = 110.0, reps = 3, completedAt = day2),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getE1RMHistory(exerciseId)

        assertEquals(2, result.size)
    }

    @Test
    fun `getE1RMHistory picks best E1RM from each day`() = runTest {
        val day1 = baseTime
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = day1),          // E1RM = 112.5
            createSet(weight = 90.0, reps = 8, completedAt = day1.plus(1, ChronoUnit.HOURS)),  // E1RM = 108
            createSet(weight = 110.0, reps = 3, completedAt = day1.plus(2, ChronoUnit.HOURS)), // E1RM = 116.47
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getE1RMHistory(exerciseId)

        assertEquals(1, result.size)
        assertEquals(116.47, result[0].e1rm, 0.01)
        assertEquals(110.0, result[0].weight, 0.01)
        assertEquals(3, result[0].reps)
    }

    @Test
    fun `getE1RMHistory sorts by date ascending`() = runTest {
        val day1 = baseTime
        val day2 = baseTime.plus(1, ChronoUnit.DAYS)
        val day3 = baseTime.plus(2, ChronoUnit.DAYS)
        val sets = listOf(
            createSet(weight = 110.0, reps = 3, completedAt = day3), // Add in reverse order
            createSet(weight = 100.0, reps = 5, completedAt = day1),
            createSet(weight = 105.0, reps = 4, completedAt = day2),
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getE1RMHistory(exerciseId)

        assertEquals(3, result.size)
        assertTrue(result[0].date.isBefore(result[1].date))
        assertTrue(result[1].date.isBefore(result[2].date))
    }

    @Test
    fun `getE1RMHistory handles same day different hours correctly`() = runTest {
        val morning = baseTime.atZone(java.time.ZoneId.of("UTC")).withHour(8).toInstant()
        val evening = baseTime.atZone(java.time.ZoneId.of("UTC")).withHour(20).toInstant()
        val sets = listOf(
            createSet(weight = 100.0, reps = 5, completedAt = morning),  // E1RM = 112.5
            createSet(weight = 110.0, reps = 3, completedAt = evening),  // E1RM = 116.47
        )
        coEvery { workoutDao.getSetsByExercise(exerciseId) } returns sets

        val result = service.getE1RMHistory(exerciseId)

        // Should group into same day and pick best
        assertEquals(1, result.size)
        assertEquals(116.47, result[0].e1rm, 0.01)
    }

    // ========== getWorkoutHistory Tests ==========

    @Test
    fun `getWorkoutHistory returns empty list when no workouts`() = runTest {
        coEvery { workoutDao.getAllCompletedWorkouts() } returns emptyList()

        val result = service.getWorkoutHistory()

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getWorkoutHistory filters out warmup sets from volume`() = runTest {
        val workout = createWorkout(
            id = "workout-1",
            startedAt = baseTime,
            durationSeconds = 3600,
            sets = listOf(
                createSet(weight = 60.0, reps = 10, isWarmup = true, exerciseId = "ex1"),   // warmup: volume = 600
                createSet(weight = 100.0, reps = 5, isWarmup = false, exerciseId = "ex1"),  // work: volume = 500
                createSet(weight = 100.0, reps = 5, isWarmup = false, exerciseId = "ex1"),  // work: volume = 500
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getWorkoutHistory()

        assertEquals(1, result.size)
        assertEquals(1000.0, result[0].totalVolume, 0.01) // Only work sets
    }

    @Test
    fun `getWorkoutHistory counts distinct exercises`() = runTest {
        val workout = createWorkout(
            id = "workout-1",
            startedAt = baseTime,
            durationSeconds = 3600,
            sets = listOf(
                createSet(weight = 100.0, reps = 5, exerciseId = "ex1"),
                createSet(weight = 100.0, reps = 5, exerciseId = "ex1"),
                createSet(weight = 80.0, reps = 10, exerciseId = "ex2"),
                createSet(weight = 60.0, reps = 8, exerciseId = "ex3"),
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getWorkoutHistory()

        assertEquals(1, result.size)
        assertEquals(3, result[0].exerciseCount)
    }

    @Test
    fun `getWorkoutHistory calculates total volume correctly`() = runTest {
        val workout = createWorkout(
            id = "workout-1",
            startedAt = baseTime,
            durationSeconds = 3600,
            sets = listOf(
                createSet(weight = 100.0, reps = 5, exerciseId = "ex1"),  // 500
                createSet(weight = 80.0, reps = 10, exerciseId = "ex2"),  // 800
                createSet(weight = 60.0, reps = 8, exerciseId = "ex3"),   // 480
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getWorkoutHistory()

        assertEquals(1, result.size)
        assertEquals(1780.0, result[0].totalVolume, 0.01)
    }

    @Test
    fun `getWorkoutHistory handles workout with only warmup sets`() = runTest {
        val workout = createWorkout(
            id = "workout-1",
            startedAt = baseTime,
            durationSeconds = 600,
            sets = listOf(
                createSet(weight = 40.0, reps = 10, isWarmup = true, exerciseId = "ex1"),
                createSet(weight = 60.0, reps = 8, isWarmup = true, exerciseId = "ex1"),
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getWorkoutHistory()

        assertEquals(1, result.size)
        assertEquals(0, result[0].exerciseCount)
        assertEquals(0.0, result[0].totalVolume, 0.01)
    }

    @Test
    fun `getWorkoutHistory maps all workout fields correctly`() = runTest {
        val workoutId = "workout-1"
        val workout = createWorkout(
            id = workoutId,
            startedAt = baseTime,
            durationSeconds = 3600,
            sets = listOf(
                createSet(weight = 100.0, reps = 5, exerciseId = "ex1"),
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout)

        val result = service.getWorkoutHistory()

        assertEquals(1, result.size)
        assertEquals(workoutId, result[0].workoutId)
        assertEquals(baseTime, result[0].date)
        assertEquals(3600L, result[0].durationSeconds)
    }

    @Test
    fun `getWorkoutHistory handles multiple workouts`() = runTest {
        val workout1 = createWorkout(
            id = "workout-1",
            startedAt = baseTime,
            durationSeconds = 3600,
            sets = listOf(
                createSet(weight = 100.0, reps = 5, exerciseId = "ex1"),
            )
        )
        val workout2 = createWorkout(
            id = "workout-2",
            startedAt = baseTime.plus(1, ChronoUnit.DAYS),
            durationSeconds = 4200,
            sets = listOf(
                createSet(weight = 105.0, reps = 5, exerciseId = "ex1"),
                createSet(weight = 80.0, reps = 10, exerciseId = "ex2"),
            )
        )
        coEvery { workoutDao.getAllCompletedWorkouts() } returns listOf(workout1, workout2)

        val result = service.getWorkoutHistory()

        assertEquals(2, result.size)
    }

    // ========== getExerciseIdsWithHistory Tests ==========

    @Test
    fun `getExerciseIdsWithHistory delegates to dao`() = runTest {
        val expectedIds = listOf("ex1", "ex2", "ex3")
        coEvery { workoutDao.getExerciseIdsWithHistory() } returns expectedIds

        val result = service.getExerciseIdsWithHistory()

        assertEquals(expectedIds, result)
    }

    @Test
    fun `getExerciseIdsWithHistory returns empty list when none exist`() = runTest {
        coEvery { workoutDao.getExerciseIdsWithHistory() } returns emptyList()

        val result = service.getExerciseIdsWithHistory()

        assertEquals(emptyList<String>(), result)
    }

    // ========== Helper Functions ==========

    private fun createSet(
        weight: Double,
        reps: Int,
        completedAt: Instant = baseTime,
        isWarmup: Boolean = false,
        exerciseId: String = this.exerciseId,
    ): WorkoutSetEntity {
        return WorkoutSetEntity(
            id = UUID.randomUUID().toString(),
            workoutId = "workout-${UUID.randomUUID()}",
            exerciseId = exerciseId,
            setNumber = 1,
            weight = weight,
            reps = reps,
            isWarmup = isWarmup,
            completedAt = completedAt.toEpochMilli(),
        )
    }

    private fun createWorkout(
        id: String,
        startedAt: Instant,
        durationSeconds: Long,
        sets: List<WorkoutSetEntity>,
    ): WorkoutWithSets {
        val workout = WorkoutEntity(
            id = id,
            startedAt = startedAt.toEpochMilli(),
            completed = true,
            durationSeconds = durationSeconds,
        )
        return WorkoutWithSets(workout = workout, sets = sets)
    }
}
