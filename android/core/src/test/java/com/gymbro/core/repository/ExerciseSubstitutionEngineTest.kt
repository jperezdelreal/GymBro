package com.gymbro.core.repository

import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ExerciseSubstitutionEngineTest {

    private lateinit var engine: ExerciseSubstitutionEngine
    private lateinit var exercisePool: List<Exercise>

    @Before
    fun setup() {
        engine = ExerciseSubstitutionEngine()
        
        // Create a diverse pool of exercises for testing
        exercisePool = listOf(
            // Chest - Compound - Barbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Barbell Bench Press",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.BARBELL
            ),
            // Chest - Compound - Dumbbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Dumbbell Bench Press",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.DUMBBELL
            ),
            // Chest - Compound - Bodyweight
            Exercise(
                id = UUID.randomUUID(),
                name = "Push-ups",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.BODYWEIGHT
            ),
            // Chest - Compound - Machine
            Exercise(
                id = UUID.randomUUID(),
                name = "Machine Chest Press",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.MACHINE
            ),
            // Chest - Isolation - Cable
            Exercise(
                id = UUID.randomUUID(),
                name = "Cable Fly",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.ISOLATION,
                equipment = Equipment.CABLE
            ),
            // Chest - Isolation - Dumbbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Dumbbell Fly",
                muscleGroup = MuscleGroup.CHEST,
                category = ExerciseCategory.ISOLATION,
                equipment = Equipment.DUMBBELL
            ),
            // Back - Compound - Barbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Barbell Row",
                muscleGroup = MuscleGroup.BACK,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.BARBELL
            ),
            // Back - Compound - Dumbbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Dumbbell Row",
                muscleGroup = MuscleGroup.BACK,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.DUMBBELL
            ),
            // Quadriceps - Compound - Barbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Barbell Squat",
                muscleGroup = MuscleGroup.QUADRICEPS,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.BARBELL
            ),
            // Quadriceps - Compound - Dumbbell
            Exercise(
                id = UUID.randomUUID(),
                name = "Goblet Squat",
                muscleGroup = MuscleGroup.QUADRICEPS,
                category = ExerciseCategory.COMPOUND,
                equipment = Equipment.DUMBBELL
            ),
            // Quadriceps - Isolation - Machine
            Exercise(
                id = UUID.randomUUID(),
                name = "Leg Extension",
                muscleGroup = MuscleGroup.QUADRICEPS,
                category = ExerciseCategory.ISOLATION,
                equipment = Equipment.MACHINE
            )
        )
    }

    @Test
    fun `findSubstitutes returns exercises with same muscle group and category`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val substitutes = engine.findSubstitutes(targetExercise, exercisePool)

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertEquals(MuscleGroup.CHEST, substitute.muscleGroup)
            assertEquals(ExerciseCategory.COMPOUND, substitute.category)
            assertNotEquals(targetExercise.id, substitute.id)
        }
    }

    @Test
    fun `findSubstitutes excludes the target exercise`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val substitutes = engine.findSubstitutes(targetExercise, exercisePool)

        // Then
        assertFalse(substitutes.any { it.id == targetExercise.id })
    }

    @Test
    fun `findSubstitutes ranks same equipment higher`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val substitutes = engine.findSubstitutesWithScores(targetExercise, exercisePool)

        // Then
        assertTrue(substitutes.isNotEmpty())
        
        // Equipment from the same family (free weights) should score higher than machines
        val dumbbellSubstitute = substitutes.find { it.exercise.name == "Dumbbell Bench Press" }
        val machineSubstitute = substitutes.find { it.exercise.name == "Machine Chest Press" }
        
        assertNotNull(dumbbellSubstitute)
        assertNotNull(machineSubstitute)
        assertTrue(dumbbellSubstitute!!.score > machineSubstitute!!.score)
    }

    @Test
    fun `findSubstitutes respects equipment filter`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }
        val availableEquipment = setOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT)

        // When
        val substitutes = engine.findSubstitutes(
            targetExercise,
            exercisePool,
            availableEquipment = availableEquipment
        )

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertTrue(substitute.equipment in availableEquipment)
        }
        
        // Should include dumbbell bench and push-ups, but not barbell or machine
        assertTrue(substitutes.any { it.name == "Dumbbell Bench Press" })
        assertTrue(substitutes.any { it.name == "Push-ups" })
        assertFalse(substitutes.any { it.name == "Machine Chest Press" })
    }

    @Test
    fun `findSubstitutes respects limit parameter`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }
        val limit = 2

        // When
        val substitutes = engine.findSubstitutes(targetExercise, exercisePool, limit = limit)

        // Then
        assertTrue(substitutes.size <= limit)
    }

    @Test
    fun `findSubstitutes returns empty list when no matches`() {
        // Given - Create an exercise with unique muscle group/category combo
        val uniqueExercise = Exercise(
            id = UUID.randomUUID(),
            name = "Unique Exercise",
            muscleGroup = MuscleGroup.CALVES,
            category = ExerciseCategory.ISOLATION,
            equipment = Equipment.MACHINE
        )

        // When
        val substitutes = engine.findSubstitutes(uniqueExercise, exercisePool)

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutes returns empty list when equipment filter excludes all options`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }
        val unavailableEquipment = setOf(Equipment.KETTLEBELL, Equipment.BAND)

        // When
        val substitutes = engine.findSubstitutes(
            targetExercise,
            exercisePool,
            availableEquipment = unavailableEquipment
        )

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutesWithScores provides scoring details`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val scoredSubstitutes = engine.findSubstitutesWithScores(targetExercise, exercisePool)

        // Then
        assertTrue(scoredSubstitutes.isNotEmpty())
        scoredSubstitutes.forEach { scored ->
            assertTrue(scored.score > 0)
            assertTrue(scored.matchReasons.isNotEmpty())
            assertTrue(scored.matchReasons.any { it.contains("Same") })
        }
    }

    @Test
    fun `findSubstitutesWithScores shows equipment scoring`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val scoredSubstitutes = engine.findSubstitutesWithScores(targetExercise, exercisePool, limit = 10)

        // Then
        val dumbbellSub = scoredSubstitutes.find { it.exercise.name == "Dumbbell Bench Press" }
        assertNotNull(dumbbellSub)
        
        // Should have "Similar equipment" reason
        assertTrue(dumbbellSub!!.matchReasons.any { it.contains("Similar equipment") || it.contains("equipment") })
    }

    @Test
    fun `scoring prioritizes free weights over machines`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val scoredSubstitutes = engine.findSubstitutesWithScores(targetExercise, exercisePool)

        // Then
        val dumbbellIndex = scoredSubstitutes.indexOfFirst { it.exercise.name == "Dumbbell Bench Press" }
        val machineIndex = scoredSubstitutes.indexOfFirst { it.exercise.name == "Machine Chest Press" }
        
        if (dumbbellIndex != -1 && machineIndex != -1) {
            assertTrue(dumbbellIndex < machineIndex)
        }
    }

    @Test
    fun `compound exercises receive bonus points`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val scoredSubstitutes = engine.findSubstitutesWithScores(targetExercise, exercisePool, limit = 10)

        // Then
        scoredSubstitutes.forEach { scored ->
            if (scored.exercise.category == ExerciseCategory.COMPOUND) {
                assertTrue(scored.matchReasons.any { it.contains("Compound") })
                assertTrue(scored.score >= 120) // Base 100 + compound bonus 20
            }
        }
    }

    @Test
    fun `isolation exercises get different matches than compound`() {
        // Given
        val isolationExercise = exercisePool.first { it.name == "Cable Fly" }

        // When
        val substitutes = engine.findSubstitutes(isolationExercise, exercisePool)

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertEquals(ExerciseCategory.ISOLATION, substitute.category)
            assertEquals(MuscleGroup.CHEST, substitute.muscleGroup)
        }
        
        // Should include dumbbell fly but not compound movements
        assertTrue(substitutes.any { it.name == "Dumbbell Fly" })
        assertFalse(substitutes.any { it.name == "Barbell Bench Press" })
    }

    @Test
    fun `filterByEquipment returns only matching equipment`() {
        // Given
        val availableEquipment = setOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT)

        // When
        val filtered = engine.filterByEquipment(exercisePool, availableEquipment)

        // Then
        assertTrue(filtered.isNotEmpty())
        filtered.forEach { exercise ->
            assertTrue(exercise.equipment in availableEquipment)
        }
    }

    @Test
    fun `filterByEquipment returns empty when no equipment matches`() {
        // Given
        val unavailableEquipment = setOf(Equipment.KETTLEBELL)

        // When
        val filtered = engine.filterByEquipment(exercisePool, unavailableEquipment)

        // Then
        assertTrue(filtered.isEmpty())
    }

    @Test
    fun `groupExercises organizes by muscle group and category`() {
        // When
        val grouped = engine.groupExercises(exercisePool)

        // Then
        assertTrue(grouped.isNotEmpty())
        
        // Check chest compound group
        val chestCompound = grouped[Pair(MuscleGroup.CHEST, ExerciseCategory.COMPOUND)]
        assertNotNull(chestCompound)
        assertEquals(4, chestCompound!!.size)
        
        // Check chest isolation group
        val chestIsolation = grouped[Pair(MuscleGroup.CHEST, ExerciseCategory.ISOLATION)]
        assertNotNull(chestIsolation)
        assertEquals(2, chestIsolation!!.size)
        
        // Check back compound group
        val backCompound = grouped[Pair(MuscleGroup.BACK, ExerciseCategory.COMPOUND)]
        assertNotNull(backCompound)
        assertEquals(2, backCompound!!.size)
    }

    @Test
    fun `different muscle groups do not cross-contaminate`() {
        // Given
        val backExercise = exercisePool.first { it.name == "Barbell Row" }

        // When
        val substitutes = engine.findSubstitutes(backExercise, exercisePool)

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertEquals(MuscleGroup.BACK, substitute.muscleGroup)
        }
        
        // Should not include chest exercises
        assertFalse(substitutes.any { it.muscleGroup == MuscleGroup.CHEST })
    }

    @Test
    fun `bodyweight exercises are included in substitutions`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Dumbbell Bench Press" }

        // When
        val substitutes = engine.findSubstitutes(targetExercise, exercisePool, limit = 10)

        // Then
        // Push-ups should be a valid substitute for chest compound movements
        assertTrue(substitutes.any { it.name == "Push-ups" })
    }

    @Test
    fun `equipment similarity scoring - free weight family`() {
        // Given - Two free weight exercises
        val barbell = exercisePool.first { it.name == "Barbell Bench Press" }
        val dumbbell = exercisePool.first { it.name == "Dumbbell Bench Press" }

        // When
        val scored = engine.findSubstitutesWithScores(barbell, exercisePool)
        val dumbbellScore = scored.find { it.exercise.id == dumbbell.id }

        // Then
        assertNotNull(dumbbellScore)
        assertTrue(dumbbellScore!!.score > 100) // Base score + similarity bonus
    }

    @Test
    fun `equipment similarity scoring - machine family`() {
        // Given - Create cable and machine exercises for the same muscle
        val cableExercise = Exercise(
            id = UUID.randomUUID(),
            name = "Cable Row",
            muscleGroup = MuscleGroup.BACK,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.CABLE
        )
        val machineExercise = Exercise(
            id = UUID.randomUUID(),
            name = "Machine Row",
            muscleGroup = MuscleGroup.BACK,
            category = ExerciseCategory.COMPOUND,
            equipment = Equipment.MACHINE
        )
        val poolWithMachines = exercisePool + listOf(cableExercise, machineExercise)

        // When
        val scored = engine.findSubstitutesWithScores(cableExercise, poolWithMachines)
        val machineScore = scored.find { it.exercise.id == machineExercise.id }

        // Then
        assertNotNull(machineScore)
        assertTrue(machineScore!!.score > 100) // Should get similarity bonus
    }

    @Test
    fun `returns up to limit even with many matches`() {
        // Given - Target with many potential substitutes
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }
        val limit = 2

        // When
        val substitutes = engine.findSubstitutes(targetExercise, exercisePool, limit = limit)

        // Then
        assertEquals(limit, substitutes.size)
    }

    @Test
    fun `substitutes are sorted by score descending`() {
        // Given
        val targetExercise = exercisePool.first { it.name == "Barbell Bench Press" }

        // When
        val scored = engine.findSubstitutesWithScores(targetExercise, exercisePool, limit = 10)

        // Then
        for (i in 0 until scored.size - 1) {
            assertTrue(scored[i].score >= scored[i + 1].score)
        }
    }
}
