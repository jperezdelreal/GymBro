package com.gymbro.core

import com.gymbro.core.fakes.FakeExerciseRepository
import com.gymbro.core.fakes.FakeWorkoutRepository
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.RecoveryMetrics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TestInfrastructureTest {

    @Test
    fun `test fixtures provide valid sample data`() {
        // Verify Exercise fixtures
        assertEquals("Bench Press", TestFixtures.benchPress.name)
        assertEquals(MuscleGroup.CHEST, TestFixtures.benchPress.muscleGroup)
        assertEquals(ExerciseCategory.COMPOUND, TestFixtures.benchPress.category)
        assertEquals(Equipment.BARBELL, TestFixtures.benchPress.equipment)

        // Verify ExerciseSet fixtures
        assertEquals(TestFixtures.benchPress.id, TestFixtures.benchPressSet1.exerciseId)
        assertEquals(100.0, TestFixtures.benchPressSet1.weightKg, 0.01)
        assertEquals(5, TestFixtures.benchPressSet1.reps)
        assertFalse(TestFixtures.benchPressSet1.isWarmup)

        // Verify Workout fixtures
        assertEquals("Push Day", TestFixtures.activeWorkout.name)
        assertNull(TestFixtures.activeWorkout.completedAt)
        assertEquals(2, TestFixtures.activeWorkout.sets.size)

        assertEquals("Leg Day", TestFixtures.completedWorkout.name)
        assertNotNull(TestFixtures.completedWorkout.completedAt)

        // Verify Recovery metrics
        assertTrue(TestFixtures.goodRecovery.readinessScore > TestFixtures.poorRecovery.readinessScore)
        assertEquals("Ready to Train", TestFixtures.goodRecovery.readinessLabel)
    }

    @Test
    fun `ExerciseSet calculates E1RM correctly`() {
        // Epley formula: weight × (1 + reps/30)
        // 100kg × (1 + 5/30) = 100 × 1.1667 = 116.67
        val expectedE1RM = 100.0 * (1.0 + 5.0 / 30.0)
        assertEquals(expectedE1RM, TestFixtures.benchPressSet1.estimatedOneRepMax, 0.01)

        // For 1 rep, E1RM should equal the weight
        val oneRepSet = TestFixtures.benchPressSet1.copy(reps = 1)
        assertEquals(100.0, oneRepSet.estimatedOneRepMax, 0.01)
    }

    @Test
    fun `ExerciseSet converts kg to lbs correctly`() {
        // 100kg = 220.462 lbs
        val expectedLbs = 100.0 * 2.20462
        assertEquals(expectedLbs, TestFixtures.benchPressSet1.weightLbs, 0.01)
    }

    @Test
    fun `FakeExerciseRepository stores and retrieves exercises`() = runTest {
        val repo = FakeExerciseRepository()
        
        // Initially empty
        assertTrue(repo.getAllExercises().first().isEmpty())
        
        // Add exercises
        repo.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        
        val exercises = repo.getAllExercises().first()
        assertEquals(2, exercises.size)
        assertTrue(exercises.contains(TestFixtures.benchPress))
        assertTrue(exercises.contains(TestFixtures.squat))
    }

    @Test
    fun `FakeExerciseRepository filters by muscle group`() = runTest {
        val repo = FakeExerciseRepository()
        repo.setExercises(
            TestFixtures.benchPress,  // CHEST
            TestFixtures.squat,       // QUADRICEPS
            TestFixtures.deadlift,    // BACK
            TestFixtures.bicepCurl    // BICEPS
        )

        val chestExercises = repo.getFilteredExercises(
            muscleGroup = "CHEST",
            query = null
        ).first()
        
        assertEquals(1, chestExercises.size)
        assertEquals(TestFixtures.benchPress, chestExercises[0])
    }

    @Test
    fun `FakeExerciseRepository filters by query`() = runTest {
        val repo = FakeExerciseRepository()
        repo.setExercises(
            TestFixtures.benchPress,
            TestFixtures.squat,
            TestFixtures.deadlift
        )

        val results = repo.getFilteredExercises(
            muscleGroup = null,
            query = "squat"
        ).first()
        
        assertEquals(1, results.size)
        assertEquals(TestFixtures.squat, results[0])
    }

    @Test
    fun `FakeExerciseRepository gets exercise by ID`() = runTest {
        val repo = FakeExerciseRepository()
        repo.setExercises(TestFixtures.benchPress, TestFixtures.squat)
        
        val exercise = repo.getExerciseById(TestFixtures.benchPress.id.toString())
        assertEquals(TestFixtures.benchPress, exercise)
        
        val notFound = repo.getExerciseById("00000000-0000-0000-0000-999999999999")
        assertNull(notFound)
    }

    @Test
    fun `FakeWorkoutRepository stores and retrieves workouts`() = runTest {
        val repo = FakeWorkoutRepository()
        
        // Add workout
        repo.setWorkouts(TestFixtures.activeWorkout)
        
        val workout = repo.getWorkout(TestFixtures.activeWorkout.id.toString())
        assertEquals(TestFixtures.activeWorkout, workout)
    }

    @Test
    fun `FakeWorkoutRepository starts new workout`() = runTest {
        val repo = FakeWorkoutRepository()
        
        val workout = repo.startWorkout()
        assertNotNull(workout.id)
        assertEquals("New Workout", workout.name)
        assertNull(workout.completedAt)
        assertTrue(workout.sets.isEmpty())
    }

    @Test
    fun `FakeWorkoutRepository adds sets to workout`() = runTest {
        val repo = FakeWorkoutRepository()
        repo.setWorkouts(TestFixtures.emptyWorkout)
        
        repo.addSet(TestFixtures.emptyWorkout.id.toString(), TestFixtures.benchPressSet1)
        
        val workout = repo.getWorkout(TestFixtures.emptyWorkout.id.toString())
        assertEquals(1, workout?.sets?.size)
        assertEquals(TestFixtures.benchPressSet1, workout?.sets?.get(0))
    }

    @Test
    fun `FakeWorkoutRepository completes workout`() = runTest {
        val repo = FakeWorkoutRepository()
        repo.setWorkouts(TestFixtures.activeWorkout)
        
        repo.completeWorkout(
            workoutId = TestFixtures.activeWorkout.id.toString(),
            durationSeconds = 3600,
            notes = "Great workout!"
        )
        
        val workout = repo.getWorkout(TestFixtures.activeWorkout.id.toString())
        assertNotNull(workout?.completedAt)
        assertEquals("Great workout!", workout?.notes)
    }

    @Test
    fun `FakeWorkoutRepository gets recent workouts`() = runTest {
        val repo = FakeWorkoutRepository()
        repo.setWorkouts(TestFixtures.completedWorkout, TestFixtures.activeWorkout)
        
        // Only completed workouts should be returned
        val recent = repo.getRecentWorkouts(limit = 10).first()
        assertEquals(1, recent.size)
        assertEquals(TestFixtures.completedWorkout, recent[0])
    }

    @Test
    fun `FakeWorkoutRepository gets best weight for exercise and reps`() = runTest {
        val repo = FakeWorkoutRepository()
        
        // Create a completed workout with bench press sets
        val completedBenchWorkout = TestFixtures.activeWorkout.copy(
            completedAt = java.time.Instant.now()
        )
        repo.setWorkouts(completedBenchWorkout, TestFixtures.completedWorkout)
        
        // Both bench sets are 100kg for 5 reps
        val bestWeight = repo.getBestWeight(
            exerciseId = TestFixtures.benchPress.id.toString(),
            reps = 5
        )
        assertEquals(100.0, bestWeight!!, 0.01)
        
        // No 3-rep bench sets exist
        val noMatch = repo.getBestWeight(
            exerciseId = TestFixtures.benchPress.id.toString(),
            reps = 3
        )
        assertNull(noMatch)
    }

    @Test
    fun `RecoveryMetrics calculates readiness score correctly`() {
        // Good recovery: 8.5h sleep, HRV 65, 1 day rest
        val goodScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 8.5,
            hrv = 65.0,
            daysSinceLastWorkout = 1
        )
        assertTrue("Good recovery should score high", goodScore >= 80)
        
        // Poor recovery: 5h sleep, HRV 30, 0 days rest
        val poorScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 5.0,
            hrv = 30.0,
            daysSinceLastWorkout = 0
        )
        assertTrue("Poor recovery should score low", poorScore < 60)
        
        // With null HRV
        val unknownScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 7.0,
            hrv = null,
            daysSinceLastWorkout = null
        )
        assertTrue("Unknown recovery should be moderate", unknownScore in 40..80)
    }
}
