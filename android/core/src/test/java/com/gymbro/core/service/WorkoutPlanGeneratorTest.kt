package com.gymbro.core.service

import android.content.Context
import com.gymbro.core.R
import com.gymbro.core.TestFixtures
import com.gymbro.core.model.TrainingSplit
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.ExerciseRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WorkoutPlanGeneratorTest {

    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var generator: WorkoutPlanGenerator
    private lateinit var context: Context

    @Before
    fun setup() {
        exerciseRepository = mockk()
        context = mockk()

        // Return the resource name as the string value for testing
        every { context.getString(any()) } answers {
            val resId = firstArg<Int>()
            stringResources[resId] ?: "unknown_$resId"
        }
        every { context.getString(any(), *anyVararg()) } answers {
            val resId = firstArg<Int>()
            val base = stringResources[resId] ?: "unknown_$resId"
            val args = args.drop(1).toTypedArray()
            if (args.isNotEmpty()) String.format(base, *args) else base
        }

        generator = WorkoutPlanGenerator(context, exerciseRepository)
        
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(TestFixtures.exercises)
    }

    companion object {
        private val stringResources = mapOf(
            R.string.gen_plan_strength to "Strength Building Program",
            R.string.gen_plan_hypertrophy to "Hypertrophy Program",
            R.string.gen_plan_powerlifting to "Powerlifting Program",
            R.string.gen_plan_general to "General Fitness Program",
            R.string.gen_plan_strength_desc to "Focus on progressive overload. Using %1\$s split for %2\$d days/week.",
            R.string.gen_plan_hypertrophy_desc to "%1\$s split focused on muscle growth for %2\$d days/week.",
            R.string.gen_plan_powerlifting_desc to "Focus on the big three using %1\$s split.",
            R.string.gen_plan_general_desc to "Balanced full-body workouts. %1\$s approach for %2\$d days/week.",
            R.string.gen_split_full_body to "Full Body",
            R.string.gen_split_upper_lower to "Upper/Lower",
            R.string.gen_split_ppl to "Push/Pull/Legs",
            R.string.gen_split_pplul to "PPL + Upper/Lower",
            R.string.gen_split_powerlifting_3day to "Powerlifting 3-Day",
            R.string.gen_day_upper_body to "Upper Body",
            R.string.gen_day_lower_body to "Lower Body",
            R.string.gen_day_full_body to "Full Body",
            R.string.gen_day_squat to "Squat Day",
            R.string.gen_day_bench to "Bench Day",
            R.string.gen_day_deadlift to "Deadlift Day",
            R.string.gen_day_upper_power to "Upper Power",
            R.string.gen_day_lower_power to "Lower Power",
            R.string.gen_day_upper_accessories to "Upper Accessories",
            R.string.gen_day_lower_accessories to "Lower Accessories",
            R.string.gen_day_full_body_a to "Full Body A",
            R.string.gen_day_full_body_b to "Full Body B",
            R.string.gen_day_full_body_c to "Full Body C",
            R.string.gen_day_upper_a to "Upper A",
            R.string.gen_day_lower_a to "Lower A",
            R.string.gen_day_upper_b to "Upper B",
            R.string.gen_day_lower_b to "Lower B",
            R.string.gen_day_push_a to "Push A",
            R.string.gen_day_pull_a to "Pull A",
            R.string.gen_day_legs_a to "Legs A",
            R.string.gen_day_push_b to "Push B",
            R.string.gen_day_pull_b to "Pull B",
            R.string.gen_day_legs_b to "Legs B",
            R.string.gen_day_push to "Push",
            R.string.gen_day_pull to "Pull",
            R.string.gen_day_legs to "Legs",
            R.string.gen_day_upper to "Upper",
            R.string.gen_day_lower to "Lower",
        )
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

    @Test
    fun `BULK phase generates more sets than MAINTENANCE`() = runTest {
        val maintenancePlan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            trainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
        )
        val bulkPlan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            trainingPhase = UserPreferences.TrainingPhase.BULK,
        )

        val maintenanceSets = maintenancePlan.workoutDays.flatMap { it.exercises }.sumOf { it.sets }
        val bulkSets = bulkPlan.workoutDays.flatMap { it.exercises }.sumOf { it.sets }
        assertTrue(
            "Bulk ($bulkSets sets) should have more total sets than maintenance ($maintenanceSets sets)",
            bulkSets > maintenanceSets,
        )
    }

    @Test
    fun `CUT phase generates fewer sets than MAINTENANCE`() = runTest {
        val maintenancePlan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            trainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
        )
        val cutPlan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 4,
            trainingPhase = UserPreferences.TrainingPhase.CUT,
        )

        val maintenanceSets = maintenancePlan.workoutDays.flatMap { it.exercises }.sumOf { it.sets }
        val cutSets = cutPlan.workoutDays.flatMap { it.exercises }.sumOf { it.sets }
        assertTrue(
            "Cut ($cutSets sets) should have fewer total sets than maintenance ($maintenanceSets sets)",
            cutSets < maintenanceSets,
        )
    }

    @Test
    fun `MAINTENANCE phase uses default set counts (no multiplier effect)`() = runTest {
        val plan = generator.generatePlan(
            goal = UserPreferences.TrainingGoal.STRENGTH,
            experienceLevel = UserPreferences.ExperienceLevel.INTERMEDIATE,
            daysPerWeek = 2,
            trainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
        )

        // Strength Full Body base sets = 5, multiplier 1.0 → 5
        val compoundSets = plan.workoutDays.first().exercises.first().sets
        assertEquals(5, compoundSets)
    }
}
