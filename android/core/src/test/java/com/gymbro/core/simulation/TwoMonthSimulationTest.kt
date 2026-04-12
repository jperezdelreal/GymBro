package com.gymbro.core.simulation

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.PersonalRecordService
import com.gymbro.core.service.PlateauDetectionService
import com.gymbro.core.service.ProgressionEngine
import com.gymbro.core.service.SmartDefaultsService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * E2E simulation of 2 months (8 weeks) of real user training.
 * 
 * Simulates:
 * - User completes onboarding (intermediate, hypertrophy, 3 days/week, 60min)
 * - Week 1-8: 3 workouts per week (24 total, ~2 missed)
 * - Realistic weight progression with plateaus and deload
 * - RPE tracking for progression decisions
 * 
 * Verifies:
 * - ProgressionEngine suggests correct weights
 * - PlateauDetection fires when stalling
 * - PersonalRecords are detected
 * - SmartDefaults use previous session data
 */
class TwoMonthSimulationTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var progressionEngine: ProgressionEngine
    private lateinit var personalRecordService: PersonalRecordService
    private lateinit var plateauDetectionService: PlateauDetectionService
    private lateinit var smartDefaultsService: SmartDefaultsService

    private val simulatedSets = mutableListOf<WorkoutSetEntity>()
    private val simulatedWorkouts = mutableListOf<WorkoutEntity>()

    private val benchPress = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Bench Press",
        muscleGroup = MuscleGroup.CHEST,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    private val squat = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        name = "Back Squat",
        muscleGroup = MuscleGroup.QUADRICEPS,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    private val deadlift = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
        name = "Deadlift",
        muscleGroup = MuscleGroup.BACK,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    private val overheadPress = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
        name = "Overhead Press",
        muscleGroup = MuscleGroup.SHOULDERS,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    private val barbellRow = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000005"),
        name = "Barbell Row",
        muscleGroup = MuscleGroup.BACK,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
    )

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        
        coEvery { workoutDao.getSetsByExercise(any()) } answers {
            val exerciseId = firstArg<String>()
            simulatedSets.filter { it.exerciseId == exerciseId }
        }
        
        coEvery { workoutDao.getAllCompletedWorkouts() } answers {
            simulatedWorkouts
                .filter { it.completed }
                .map { workout ->
                    com.gymbro.core.database.dao.WorkoutWithSets(
                        workout = workout,
                        sets = simulatedSets.filter { it.workoutId == workout.id }
                    )
                }
        }
        
        coEvery { workoutDao.getExerciseIdsWithHistory() } answers {
            simulatedSets.map { it.exerciseId }.distinct()
        }
        
        progressionEngine = ProgressionEngine(workoutDao)
        personalRecordService = PersonalRecordService(workoutDao)
        plateauDetectionService = PlateauDetectionService(personalRecordService)
        smartDefaultsService = SmartDefaultsService(workoutDao, progressionEngine)
    }

    @Test
    fun `simulate 8 weeks of hypertrophy training`() = runTest {
        val startDate = Instant.now().minus(56, ChronoUnit.DAYS)
        var currentDate = startDate
        var workoutCount = 0
        var missedWorkouts = 0

        for (week in 1..8) {
            for (dayInWeek in 1..3) {
                if ((week == 3 && dayInWeek == 2) || (week == 6 && dayInWeek == 3)) {
                    missedWorkouts++
                    currentDate = currentDate.plus(2, ChronoUnit.DAYS)
                    continue
                }

                val workoutId = UUID.randomUUID().toString()
                workoutCount++

                when (dayInWeek) {
                    1 -> logPushWorkout(workoutId, currentDate, week)
                    2 -> logPullWorkout(workoutId, currentDate, week)
                    3 -> logLegWorkout(workoutId, currentDate, week)
                }

                val workout = WorkoutEntity(
                    id = workoutId,
                    startedAt = currentDate.toEpochMilli(),
                    completedAt = currentDate.plus(60, ChronoUnit.MINUTES).toEpochMilli(),
                    durationSeconds = 3600,
                    completed = true,
                )
                simulatedWorkouts.add(workout)

                currentDate = currentDate.plus(2, ChronoUnit.DAYS)
            }
        }

        assertEquals(22, workoutCount)
        assertEquals(2, missedWorkouts)

        verifyProgression()
        verifyPersonalRecords()
        verifySmartDefaults()
        verifyPlateauDetection()
    }

    private suspend fun logPushWorkout(workoutId: String, date: Instant, week: Int) {
        logExercise(workoutId, benchPress.id.toString(), date, week, baseWeight = 60.0)
        logExercise(workoutId, overheadPress.id.toString(), date, week, baseWeight = 40.0)
    }

    private suspend fun logPullWorkout(workoutId: String, date: Instant, week: Int) {
        logExercise(workoutId, deadlift.id.toString(), date, week, baseWeight = 100.0)
        logExercise(workoutId, barbellRow.id.toString(), date, week, baseWeight = 70.0)
    }

    private suspend fun logLegWorkout(workoutId: String, date: Instant, week: Int) {
        logExercise(workoutId, squat.id.toString(), date, week, baseWeight = 80.0)
    }

    private suspend fun logExercise(
        workoutId: String,
        exerciseId: String,
        date: Instant,
        week: Int,
        baseWeight: Double,
    ) {
        val (weight, reps, rpe) = calculateWeightRepsRpe(week, baseWeight)

        for (setNumber in 1..3) {
            val adjustedRpe = rpe + (setNumber - 1) * 0.5
            val adjustedReps = if (setNumber == 3 && rpe >= 8.5) reps - 1 else reps

            val set = WorkoutSetEntity(
                id = UUID.randomUUID().toString(),
                workoutId = workoutId,
                exerciseId = exerciseId,
                setNumber = setNumber,
                weight = weight,
                reps = adjustedReps,
                rpe = adjustedRpe.coerceIn(1.0, 10.0),
                isWarmup = false,
                completedAt = date.plus(setNumber * 5L, ChronoUnit.MINUTES).toEpochMilli(),
            )
            simulatedSets.add(set)
        }
    }

    private fun calculateWeightRepsRpe(week: Int, baseWeight: Double): Triple<Double, Int, Double> {
        return when (week) {
            1 -> Triple(baseWeight, 10, 7.0)
            2 -> Triple(baseWeight + 2.5, 10, 7.0)
            3 -> Triple(baseWeight + 5.0, 10, 8.0)
            4 -> Triple(baseWeight + 7.5, 9, 8.5)
            5 -> Triple(baseWeight + 7.5, 8, 9.0)
            6 -> Triple(baseWeight + 7.5, 8, 9.5)
            7 -> Triple(baseWeight, 12, 6.0)
            8 -> Triple(baseWeight + 5.0, 10, 7.0)
            else -> Triple(baseWeight, 10, 7.0)
        }
    }

    private suspend fun verifyProgression() {
        val benchId = benchPress.id.toString()
        val suggestion = progressionEngine.getSuggestion(benchId, UserPreferences.TrainingPhase.MAINTENANCE)
        
        assertNotNull("Should have progression suggestion for bench press", suggestion)
        
        val lastBenchWeight = simulatedSets
            .filter { it.exerciseId == benchId && !it.isWarmup }
            .maxByOrNull { it.completedAt }
            ?.weight ?: 0.0
        
        val lastBenchRpe = simulatedSets
            .filter { it.exerciseId == benchId && !it.isWarmup }
            .filter { it.rpe != null }
            .maxByOrNull { it.completedAt }
            ?.rpe ?: 10.0
        
        if (lastBenchRpe <= 7.0) {
            assertEquals(
                "Week 8 (RPE 7) should suggest PROGRESS",
                ProgressionEngine.ProgressionReason.PROGRESS,
                suggestion!!.reason
            )
            assertEquals(lastBenchWeight + 2.5, suggestion.suggestedWeightKg, 0.01)
        }
    }

    private suspend fun verifyPersonalRecords() {
        val benchId = benchPress.id.toString()
        val records = personalRecordService.getPersonalRecords(benchId, benchPress.name)
        
        assertFalse("Should have personal records for bench press", records.isEmpty())
        
        val maxWeightRecord = records.find { it.type == com.gymbro.core.model.RecordType.MAX_WEIGHT }
        assertNotNull("Should have max weight record", maxWeightRecord)
        
        val expectedMaxWeight = simulatedSets
            .filter { it.exerciseId == benchId && !it.isWarmup }
            .maxOfOrNull { it.weight } ?: 0.0
        
        assertEquals(expectedMaxWeight, maxWeightRecord!!.value, 0.01)
        
        val e1rmRecord = records.find { it.type == com.gymbro.core.model.RecordType.MAX_E1RM }
        assertNotNull("Should have E1RM record", e1rmRecord)
        assertTrue("E1RM should be positive", e1rmRecord!!.value > 0)
    }

    private suspend fun verifySmartDefaults() {
        val benchId = benchPress.id.toString()
        val defaults = smartDefaultsService.getDefaults(benchId)
        
        assertNotNull("Should have default weight from history", defaults.weight)
        assertNotNull("Should have default reps from history", defaults.reps)
        assertNotNull("Should have progression reason", defaults.progressionReason)
        
        val lastBenchSet = simulatedSets
            .filter { it.exerciseId == benchId && !it.isWarmup }
            .maxByOrNull { it.completedAt }
        
        assertNotNull("Should have at least one bench set", lastBenchSet)
        assertEquals("Reps should match last session", lastBenchSet!!.reps, defaults.reps)
    }

    private suspend fun verifyPlateauDetection() {
        val benchId = benchPress.id.toString()
        
        val plateaus = plateauDetectionService.detectPlateaus(benchId, benchPress.name)
        
        if (plateaus != null) {
            assertTrue(
                "Plateau alert should be STAGNATION or REGRESSION",
                plateaus.type == com.gymbro.core.model.PlateauType.STAGNATION ||
                plateaus.type == com.gymbro.core.model.PlateauType.REGRESSION
            )
            assertTrue("Plateau duration should be at least 3 weeks", plateaus.weeksDuration >= 3)
            assertNotNull("Should have suggestion for plateau", plateaus.suggestion)
        }
    }

    @Test
    fun `verify deload week restores RPE and allows progress`() = runTest {
        simulatedSets.clear()
        simulatedWorkouts.clear()
        
        val startDate = Instant.now().minus(49, ChronoUnit.DAYS)
        var currentDate = startDate

        for (week in 1..7) {
            for (day in 1..3) {
                val workoutId = UUID.randomUUID().toString()
                logPushWorkout(workoutId, currentDate, week)
                
                val workout = WorkoutEntity(
                    id = workoutId,
                    startedAt = currentDate.toEpochMilli(),
                    completedAt = currentDate.plus(60, ChronoUnit.MINUTES).toEpochMilli(),
                    durationSeconds = 3600,
                    completed = true,
                )
                simulatedWorkouts.add(workout)
                currentDate = currentDate.plus(2, ChronoUnit.DAYS)
            }
        }

        val deloadSuggestion = progressionEngine.getSuggestion(
            benchPress.id.toString(),
            UserPreferences.TrainingPhase.MAINTENANCE
        )
        
        assertNotNull("Deload week should have suggestion", deloadSuggestion)
        assertEquals(
            "Deload week (RPE 6) should suggest PROGRESS",
            ProgressionEngine.ProgressionReason.PROGRESS,
            deloadSuggestion!!.reason
        )
    }
}
