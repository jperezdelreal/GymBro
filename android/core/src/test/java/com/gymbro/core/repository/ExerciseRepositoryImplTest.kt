package com.gymbro.core.repository

import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ExerciseRepositoryImplTest {

    private lateinit var exerciseDao: ExerciseDao
    private lateinit var repository: ExerciseRepositoryImpl

    @Before
    fun setup() {
        exerciseDao = mockk()
        repository = ExerciseRepositoryImpl(exerciseDao)
    }

    @Test
    fun `getAllExercises returns Flow of exercises`() = runTest {
        // Given
        val entities = listOf(
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Bench Press",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name,
                description = "Classic chest exercise"
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Squat",
                muscleGroup = MuscleGroup.QUADRICEPS.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name,
                description = "Leg exercise"
            )
        )
        
        every { exerciseDao.getAllExercises() } returns flowOf(entities)

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Bench Press", result[0].name)
        assertEquals(MuscleGroup.CHEST, result[0].muscleGroup)
        assertEquals("Squat", result[1].name)
        assertEquals(MuscleGroup.QUADRICEPS, result[1].muscleGroup)
    }

    @Test
    fun `getAllExercises returns empty list when database is empty`() = runTest {
        // Given
        every { exerciseDao.getAllExercises() } returns flowOf(emptyList())

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `getFilteredExercises filters by muscle group`() = runTest {
        // Given
        val chestExercise = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name
        )
        
        every { exerciseDao.getFilteredExercises(MuscleGroup.CHEST.name, null) } returns 
            flowOf(listOf(chestExercise))

        // When
        val result = repository.getFilteredExercises(MuscleGroup.CHEST.name, null).first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Bench Press", result[0].name)
        assertEquals(MuscleGroup.CHEST, result[0].muscleGroup)
    }

    @Test
    fun `getFilteredExercises filters by search query`() = runTest {
        // Given
        val pressExercise = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name
        )
        
        every { exerciseDao.getFilteredExercises(null, "Press") } returns 
            flowOf(listOf(pressExercise))

        // When
        val result = repository.getFilteredExercises(null, "Press").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Bench Press", result[0].name)
    }

    @Test
    fun `getFilteredExercises filters by both muscle group and query`() = runTest {
        // Given
        val benchPress = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Bench Press",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name
        )
        
        every { exerciseDao.getFilteredExercises(MuscleGroup.CHEST.name, "Bench") } returns 
            flowOf(listOf(benchPress))

        // When
        val result = repository.getFilteredExercises(MuscleGroup.CHEST.name, "Bench").first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Bench Press", result[0].name)
        assertEquals(MuscleGroup.CHEST, result[0].muscleGroup)
    }

    @Test
    fun `getFilteredExercises returns empty list when no matches`() = runTest {
        // Given
        every { exerciseDao.getFilteredExercises(MuscleGroup.CHEST.name, "Squat") } returns 
            flowOf(emptyList())

        // When
        val result = repository.getFilteredExercises(MuscleGroup.CHEST.name, "Squat").first()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun `getFilteredExercises with null filters returns all exercises`() = runTest {
        // Given
        val entities = listOf(
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Bench Press",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name
            ),
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Squat",
                muscleGroup = MuscleGroup.QUADRICEPS.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name
            )
        )
        
        every { exerciseDao.getFilteredExercises(null, null) } returns flowOf(entities)

        // When
        val result = repository.getFilteredExercises(null, null).first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getExerciseById returns exercise when found`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        val entity = ExerciseEntity(
            id = exerciseId,
            name = "Deadlift",
            muscleGroup = MuscleGroup.BACK.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name,
            description = "Back exercise",
            youtubeUrl = "https://youtube.com/deadlift"
        )
        
        coEvery { exerciseDao.getExerciseById(exerciseId) } returns entity

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNotNull(result)
        assertEquals(exerciseId, result!!.id.toString())
        assertEquals("Deadlift", result.name)
        assertEquals(MuscleGroup.BACK, result.muscleGroup)
        assertEquals("Back exercise", result.description)
        assertEquals("https://youtube.com/deadlift", result.youtubeUrl)
    }

    @Test
    fun `getExerciseById returns null when not found`() = runTest {
        // Given
        val exerciseId = UUID.randomUUID().toString()
        coEvery { exerciseDao.getExerciseById(exerciseId) } returns null

        // When
        val result = repository.getExerciseById(exerciseId)

        // Then
        assertNull(result)
    }

    @Test
    fun `domain conversion handles all equipment types`() = runTest {
        // Given
        val entities = Equipment.entries.map { equipment ->
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Exercise with ${equipment.name}",
                muscleGroup = MuscleGroup.CHEST.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = equipment.name
            )
        }
        
        every { exerciseDao.getAllExercises() } returns flowOf(entities)

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(Equipment.entries.size, result.size)
        Equipment.entries.forEachIndexed { index, equipment ->
            assertEquals(equipment, result[index].equipment)
        }
    }

    @Test
    fun `domain conversion handles all muscle groups`() = runTest {
        // Given
        val entities = MuscleGroup.entries.map { muscleGroup ->
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Exercise for ${muscleGroup.name}",
                muscleGroup = muscleGroup.name,
                category = ExerciseCategory.COMPOUND.name,
                equipment = Equipment.BARBELL.name
            )
        }
        
        every { exerciseDao.getAllExercises() } returns flowOf(entities)

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(MuscleGroup.entries.size, result.size)
        MuscleGroup.entries.forEachIndexed { index, muscleGroup ->
            assertEquals(muscleGroup, result[index].muscleGroup)
        }
    }

    @Test
    fun `domain conversion handles all exercise categories`() = runTest {
        // Given
        val entities = ExerciseCategory.entries.map { category ->
            ExerciseEntity(
                id = UUID.randomUUID().toString(),
                name = "Exercise category ${category.name}",
                muscleGroup = MuscleGroup.CHEST.name,
                category = category.name,
                equipment = Equipment.BARBELL.name
            )
        }
        
        every { exerciseDao.getAllExercises() } returns flowOf(entities)

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(ExerciseCategory.entries.size, result.size)
        ExerciseCategory.entries.forEachIndexed { index, category ->
            assertEquals(category, result[index].category)
        }
    }

    @Test
    fun `domain conversion defaults to FULL_BODY for invalid muscle group`() = runTest {
        // Given
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Invalid Exercise",
            muscleGroup = "INVALID_GROUP",
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name
        )
        
        every { exerciseDao.getAllExercises() } returns flowOf(listOf(entity))

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(MuscleGroup.FULL_BODY, result[0].muscleGroup)
    }

    @Test
    fun `domain conversion defaults to COMPOUND for invalid category`() = runTest {
        // Given
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Invalid Exercise",
            muscleGroup = MuscleGroup.CHEST.name,
            category = "INVALID_CATEGORY",
            equipment = Equipment.BARBELL.name
        )
        
        every { exerciseDao.getAllExercises() } returns flowOf(listOf(entity))

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(ExerciseCategory.COMPOUND, result[0].category)
    }

    @Test
    fun `domain conversion defaults to OTHER for invalid equipment`() = runTest {
        // Given
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Invalid Exercise",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = "INVALID_EQUIPMENT"
        )
        
        every { exerciseDao.getAllExercises() } returns flowOf(listOf(entity))

        // When
        val result = repository.getAllExercises().first()

        // Then
        assertEquals(Equipment.OTHER, result[0].equipment)
    }

    @Test
    fun `domain conversion handles null youtubeUrl`() = runTest {
        // Given
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Exercise",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name,
            youtubeUrl = null
        )
        
        coEvery { exerciseDao.getExerciseById(entity.id) } returns entity

        // When
        val result = repository.getExerciseById(entity.id)

        // Then
        assertNotNull(result)
        assertNull(result!!.youtubeUrl)
    }

    @Test
    fun `domain conversion handles empty description`() = runTest {
        // Given
        val entity = ExerciseEntity(
            id = UUID.randomUUID().toString(),
            name = "Exercise",
            muscleGroup = MuscleGroup.CHEST.name,
            category = ExerciseCategory.COMPOUND.name,
            equipment = Equipment.BARBELL.name,
            description = ""
        )
        
        coEvery { exerciseDao.getExerciseById(entity.id) } returns entity

        // When
        val result = repository.getExerciseById(entity.id)

        // Then
        assertNotNull(result)
        assertEquals("", result!!.description)
    }
}
