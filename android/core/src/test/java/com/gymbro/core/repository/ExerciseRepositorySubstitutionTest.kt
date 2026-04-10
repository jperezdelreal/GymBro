package com.gymbro.core.repository

import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ExerciseRepositorySubstitutionTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl
    private lateinit var exerciseEntities: List<ExerciseEntity>

    @Before
    fun setup() {
        exerciseDao = mockk()
        repository = ExerciseRepositoryImpl(exerciseDao)

        // Setup test data - various chest exercises
        exerciseEntities = listOf(
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Barbell Bench Press",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Dumbbell Bench Press",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.DUMBBELL.name
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Push-ups",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BODYWEIGHT.name
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Cable Fly",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.ISOLATION.name,
                equipment = Equipment.CABLE.name
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Barbell Row",
                muscleGroup = MuscleGroup.BACK.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name
            )
        )
    }

    @Test
    fun `findSubstitutes returns alternatives for target exercise`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id // Barbell Bench Press
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(targetId)

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertEquals(MuscleGroup.CHEST, substitute.muscleGroup)
            assertEquals(ExerciseCategory.COMPOUND, substitute.category)
            assertNotEquals(UUID.fromString(targetId), substitute.id)
        }
    }

    @Test
    fun `findSubstitutes filters by available equipment`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id // Barbell Bench Press
        val availableEquipment = setOf(Equipment.DUMBBELL, Equipment.BODYWEIGHT)
        
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(
            targetId,
            availableEquipment = availableEquipment
        )

        // Then
        assertTrue(substitutes.isNotEmpty())
        substitutes.forEach { substitute ->
            assertTrue(substitute.equipment in availableEquipment)
        }
        
        assertTrue(substitutes.any { it.name == "Dumbbell Bench Press" })
        assertTrue(substitutes.any { it.name == "Push-ups" })
        assertFalse(substitutes.any { it.name == "Cable Fly" }) // Wrong category
    }

    @Test
    fun `findSubstitutes respects limit parameter`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id
        val limit = 1
        
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(targetId, limit = limit)

        // Then
        assertTrue(substitutes.size <= limit)
    }

    @Test
    fun `findSubstitutes returns empty when exercise not found`() = runTest {
        // Given
        val nonExistentId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(nonExistentId) } returns null

        // When
        val substitutes = repository.findSubstitutes(nonExistentId)

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutes handles database errors gracefully`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } throws Exception("Database error")

        // When
        val substitutes = repository.findSubstitutes(targetId)

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutes excludes exercises from different muscle groups`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id // Chest exercise
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(targetId, limit = 10)

        // Then
        substitutes.forEach { substitute ->
            assertEquals(MuscleGroup.CHEST, substitute.muscleGroup)
        }
        
        assertFalse(substitutes.any { it.name == "Barbell Row" })
    }

    @Test
    fun `findSubstitutes excludes exercises from different categories`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id // Compound exercise
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(targetId, limit = 10)

        // Then
        substitutes.forEach { substitute ->
            assertEquals(ExerciseCategory.COMPOUND, substitute.category)
        }
        
        assertFalse(substitutes.any { it.name == "Cable Fly" })
    }

    @Test
    fun `findSubstitutes with null equipment returns all matching exercises`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(
            targetId,
            availableEquipment = null,
            limit = 10
        )

        // Then
        // Should include both dumbbell and bodyweight variations
        assertTrue(substitutes.any { it.equipment == Equipment.DUMBBELL })
        assertTrue(substitutes.any { it.equipment == Equipment.BODYWEIGHT })
    }

    @Test
    fun `findSubstitutes returns empty when no alternatives exist`() = runTest {
        // Given - Exercise with no alternatives in database
        val uniqueExercise = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Unique Exercise",
            muscleGroup = MuscleGroup.CALVES.name,
            category = ExerciseCategory.ISOLATION.name,
            equipment = Equipment.MACHINE.name
        )
        
        coEvery { exerciseDao.getExerciseById(uniqueExercise.id) } returns uniqueExercise
        every { exerciseDao.getAllExercises() } returns flowOf(listOf(uniqueExercise))

        // When
        val substitutes = repository.findSubstitutes(uniqueExercise.id)

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutes works with isolation exercises`() = runTest {
        // Given - Cable Fly (isolation)
        val targetId = exerciseEntities[3].id
        
        // Add another isolation exercise
        val dumbbellFly = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Dumbbell Fly",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.ISOLATION.name,
            equipment = Equipment.DUMBBELL.name
        )
        val entitiesWithFly = exerciseEntities + dumbbellFly
        
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[3]
        every { exerciseDao.getAllExercises() } returns flowOf(entitiesWithFly)

        // When
        val substitutes = repository.findSubstitutes(targetId)

        // Then
        assertTrue(substitutes.isNotEmpty())
        assertTrue(substitutes.any { it.name == "Dumbbell Fly" })
        assertFalse(substitutes.any { it.category == ExerciseCategory.COMPOUND })
    }

    @Test
    fun `findSubstitutes with empty equipment set returns nothing`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id
        val emptyEquipment = emptySet<Equipment>()
        
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(
            targetId,
            availableEquipment = emptyEquipment
        )

        // Then
        assertTrue(substitutes.isEmpty())
    }

    @Test
    fun `findSubstitutes prioritizes similar equipment`() = runTest {
        // Given
        val targetId = exerciseEntities[0].id // Barbell Bench Press
        coEvery { exerciseDao.getExerciseById(targetId) } returns exerciseEntities[0]
        every { exerciseDao.getAllExercises() } returns flowOf(exerciseEntities)

        // When
        val substitutes = repository.findSubstitutes(targetId, limit = 10)

        // Then
        // Dumbbell should rank higher than bodyweight due to equipment similarity
        if (substitutes.size >= 2) {
            val dumbbellIndex = substitutes.indexOfFirst { it.name == "Dumbbell Bench Press" }
            val bodyweightIndex = substitutes.indexOfFirst { it.name == "Push-ups" }
            
            if (dumbbellIndex != -1 && bodyweightIndex != -1) {
                assertTrue(dumbbellIndex < bodyweightIndex)
            }
        }
    }
}
