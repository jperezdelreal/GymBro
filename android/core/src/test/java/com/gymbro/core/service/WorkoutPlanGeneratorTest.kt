package com.gymbro.core.service

import com.gymbro.core.TestFixtures
import com.gymbro.core.model.TrainingSplit
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkoutPlanGeneratorTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var generator: WorkoutPlanGenerator

    @Before
    fun setup() {
        exerciseRepository = mockk()
        generator = WorkoutPlanGenerator(exerciseRepository)
        
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(TestFixtures.exercises)
    }

    @Test
    fun `generatePlan uses Full Body split for 2 days hypertrophy`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.BEGINNER,
            daysPerWeek = 2
        )
        
        assertEquals(TrainingSplit.FULL_BODY, plan.split)
        assertEquals(2, plan.workoutDays.size)
        assertTrue(plan.description.contains("Full Body"))
    }

    @Test
    fun `generatePlan uses Upper Lower split for 4 days strength`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.STRENGTH,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4
        )
        
        assertEquals(TrainingSplit.UPPER_LOWER, plan.split)
        assertEquals(4, plan.workoutDays.size)
        assertTrue(plan.description.contains("Upper/Lower"))
    }

    @Test
    fun `generatePlan uses PPL split for 6 days hypertrophy`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.ADVANCED,
            daysPerWeek = 6
        )
        
        assertEquals(TrainingSplit.PPL, plan.split)
        assertEquals(6, plan.workoutDays.size)
        assertTrue(plan.description.contains("Push/Pull/Legs"))
    }

    @Test
    fun `generatePlan uses PPLUL split for 5 days hypertrophy`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.ADVANCED,
            daysPerWeek = 5
        )
        
        assertEquals(TrainingSplit.PPLUL, plan.split)
        assertEquals(5, plan.workoutDays.size)
        assertTrue(plan.description.contains("PPL + Upper/Lower"))
    }

    @Test
    fun `generatePlan uses Powerlifting 3-day split for powerlifting goal`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.POWERLIFTING,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 3
        )
        
        assertEquals(TrainingSplit.POWERLIFTING_3DAY, plan.split)
        assertEquals(3, plan.workoutDays.size)
        assertTrue(plan.workoutDays.any { it.name.contains("Squat") })
        assertTrue(plan.workoutDays.any { it.name.contains("Bench") })
        assertTrue(plan.workoutDays.any { it.name.contains("Deadlift") })
    }

    @Test
    fun `generatePlan includes split in description`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.BEGINNER,
            daysPerWeek = 3
        )
        
        assertNotNull(plan.split)
        assertTrue(plan.description.contains("Full Body"))
        assertTrue(plan.description.contains("3 days/week"))
    }

    @Test
    fun `generatePlan creates correct number of workout days`() = runTest {
        for (days in 2..6) {
            val plan = generator.generatePlan(
                goal = UserPreferences.TrainingGoal.HYPERTROPHY,
                experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
                daysPerWeek = days
            )
            
            assertEquals(days, plan.workoutDays.size)
            assertEquals(days, plan.daysPerWeek)
        }
    }

    @Test
    fun `generatePlan assigns exercises to each day`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4
        )
        
        plan.workoutDays.forEach { day ->
            assertTrue("Day ${day.dayNumber} should have exercises", day.exercises.isNotEmpty())
            assertTrue("Day ${day.dayNumber} should have <= 6 exercises", day.exercises.size <= 6)
        }
    }
}
