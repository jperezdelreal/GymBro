package com.gymbro.core.repository

import app.cash.turbine.test
import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.dao.WorkoutTemplateWithExercises
import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.TemplateExercise
import com.gymbro.core.model.WorkoutTemplate
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.UUID

class WorkoutTemplateRepositoryImplTest {

    private lateinit var templateDao: WorkoutTemplateDao
    private lateinit var exerciseRepository: ExerciseRepository
    private lateinit var repository: WorkoutTemplateRepositoryImpl

    @Before
    fun setup() {
        templateDao = mockk(relaxed = true)
        exerciseRepository = mockk(relaxed = true)
        repository = WorkoutTemplateRepositoryImpl(templateDao, exerciseRepository)
    }

    @Test
    fun `observeAllTemplates returns flow of templates`() = runTest {
        val templateId = UUID.randomUUID().toString()
        val templateEntity = WorkoutTemplateEntity(
            id = templateId,
            name = "Push Day",
            description = "Chest workout",
            createdAt = Instant.now().toEpochMilli(),
            lastUsedAt = null,
            isBuiltIn = false
        )
        val templateWithExercises = WorkoutTemplateWithExercises(
            template = templateEntity,
            exercises = emptyList()
        )
        coEvery { templateDao.observeAllTemplates() } returns flowOf(listOf(templateWithExercises))

        repository.observeAllTemplates().test {
            val templates = awaitItem()
            assertEquals(1, templates.size)
            assertEquals("Push Day", templates[0].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getTemplate returns correct template`() = runTest {
        val templateId = UUID.randomUUID().toString()
        val templateEntity = WorkoutTemplateEntity(
            id = templateId,
            name = "Pull Day",
            description = "Back workout",
            createdAt = Instant.now().toEpochMilli(),
            lastUsedAt = null,
            isBuiltIn = false
        )
        val templateWithExercises = WorkoutTemplateWithExercises(
            template = templateEntity,
            exercises = emptyList()
        )
        coEvery { templateDao.getTemplateWithExercises(templateId) } returns templateWithExercises

        val result = repository.getTemplate(templateId)

        assertNotNull(result)
        assertEquals("Pull Day", result?.name)
    }

    @Test
    fun `saveTemplate inserts template and exercises`() = runTest {
        val template = WorkoutTemplate(
            name = "Test Template",
            description = "Test description",
            exercises = listOf(
                TemplateExercise(
                    exerciseId = UUID.randomUUID(),
                    exerciseName = "Bench Press",
                    muscleGroup = MuscleGroup.CHEST,
                    targetSets = 4,
                    targetReps = 8,
                    order = 0
                )
            ),
            isBuiltIn = false
        )

        repository.saveTemplate(template)

        coVerify { templateDao.insertTemplate(any()) }
        coVerify { templateDao.deleteTemplateExercises(template.id.toString()) }
        coVerify { templateDao.insertTemplateExercises(any()) }
    }

    @Test
    fun `deleteTemplate calls dao`() = runTest {
        val templateId = "template-123"

        repository.deleteTemplate(templateId)

        coVerify { templateDao.deleteTemplate(templateId) }
    }

    @Test
    fun `updateLastUsed updates timestamp`() = runTest {
        val templateId = "template-123"

        repository.updateLastUsed(templateId)

        coVerify { templateDao.updateLastUsedTimestamp(templateId, any()) }
    }

    @Test
    fun `initializeBuiltInTemplates skips if templates already exist`() = runTest {
        val existingTemplate = WorkoutTemplateWithExercises(
            template = WorkoutTemplateEntity(
                id = UUID.randomUUID().toString(),
                name = "Existing",
                description = "",
                isBuiltIn = true,
            ),
            exercises = emptyList()
        )
        coEvery { templateDao.getAllTemplates() } returns listOf(existingTemplate)

        repository.initializeBuiltInTemplates()

        coVerify(exactly = 0) { templateDao.insertTemplate(any()) }
    }

    @Test
    fun `initializeBuiltInTemplates creates Starting Strength templates`() = runTest {
        coEvery { templateDao.getAllTemplates() } returns emptyList()
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(
            createMockExercise("Barbell Back Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Barbell Row", MuscleGroup.BACK),
            createMockExercise("Barbell Overhead Press", MuscleGroup.SHOULDERS),
            createMockExercise("Conventional Deadlift", MuscleGroup.HAMSTRINGS),
        ))

        repository.initializeBuiltInTemplates()

        coVerify(atLeast = 2) { templateDao.insertTemplate(
            match { it.name.contains("Starting Strength 5×5") }
        ) }
    }

    @Test
    fun `initializeBuiltInTemplates creates PPL templates`() = runTest {
        coEvery { templateDao.getAllTemplates() } returns emptyList()
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(
            createMockExercise("Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Barbell Overhead Press", MuscleGroup.SHOULDERS),
            createMockExercise("Incline Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Cable Tricep Extension (Rope)", MuscleGroup.TRICEPS),
            createMockExercise("Lateral Raise", MuscleGroup.SHOULDERS),
            createMockExercise("Cable Crossover", MuscleGroup.CHEST),
            createMockExercise("Conventional Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Pull-Up", MuscleGroup.BACK),
            createMockExercise("Barbell Row", MuscleGroup.BACK),
            createMockExercise("Cable Lat Pulldown (Wide Grip)", MuscleGroup.BACK),
            createMockExercise("Cable Bicep Curl", MuscleGroup.BICEPS),
            createMockExercise("Face Pull", MuscleGroup.SHOULDERS),
            createMockExercise("Barbell Back Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Romanian Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Leg Press", MuscleGroup.QUADRICEPS),
            createMockExercise("Leg Curl", MuscleGroup.HAMSTRINGS),
            createMockExercise("Leg Extension", MuscleGroup.QUADRICEPS),
            createMockExercise("Standing Calf Raise", MuscleGroup.CALVES),
        ))

        repository.initializeBuiltInTemplates()

        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "PPL - Push Day" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "PPL - Pull Day" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "PPL - Leg Day" }
        ) }
    }

    @Test
    fun `initializeBuiltInTemplates creates Upper Lower templates`() = runTest {
        coEvery { templateDao.getAllTemplates() } returns emptyList()
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(
            createMockExercise("Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Barbell Row", MuscleGroup.BACK),
            createMockExercise("Barbell Overhead Press", MuscleGroup.SHOULDERS),
            createMockExercise("Lat Pulldown", MuscleGroup.BACK),
            createMockExercise("Cable Bicep Curl", MuscleGroup.BICEPS),
            createMockExercise("Cable Tricep Extension (Rope)", MuscleGroup.TRICEPS),
            createMockExercise("Barbell Back Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Romanian Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Bulgarian Split Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Leg Curl", MuscleGroup.HAMSTRINGS),
            createMockExercise("Standing Calf Raise", MuscleGroup.CALVES),
            createMockExercise("Incline Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Pull-Up", MuscleGroup.BACK),
            createMockExercise("Dumbbell Shoulder Press", MuscleGroup.SHOULDERS),
            createMockExercise("Cable Row", MuscleGroup.BACK),
            createMockExercise("Lateral Raise", MuscleGroup.SHOULDERS),
            createMockExercise("Face Pull", MuscleGroup.SHOULDERS),
            createMockExercise("Conventional Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Front Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Leg Press", MuscleGroup.QUADRICEPS),
            createMockExercise("Leg Extension", MuscleGroup.QUADRICEPS),
            createMockExercise("Seated Calf Raise", MuscleGroup.CALVES),
        ))

        repository.initializeBuiltInTemplates()

        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Upper/Lower - Upper A" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Upper/Lower - Lower A" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Upper/Lower - Upper B" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Upper/Lower - Lower B" }
        ) }
    }

    @Test
    fun `initializeBuiltInTemplates creates Full Body templates`() = runTest {
        coEvery { templateDao.getAllTemplates() } returns emptyList()
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(listOf(
            createMockExercise("Barbell Back Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Barbell Row", MuscleGroup.BACK),
            createMockExercise("Barbell Overhead Press", MuscleGroup.SHOULDERS),
            createMockExercise("Romanian Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Plank", MuscleGroup.CORE),
            createMockExercise("Conventional Deadlift", MuscleGroup.HAMSTRINGS),
            createMockExercise("Pull-Up", MuscleGroup.BACK),
            createMockExercise("Incline Barbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Bulgarian Split Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Ab Wheel Rollout", MuscleGroup.CORE),
            createMockExercise("Front Squat", MuscleGroup.QUADRICEPS),
            createMockExercise("Dumbbell Bench Press", MuscleGroup.CHEST),
            createMockExercise("Cable Row", MuscleGroup.BACK),
            createMockExercise("Dumbbell Shoulder Press", MuscleGroup.SHOULDERS),
            createMockExercise("Leg Curl", MuscleGroup.HAMSTRINGS),
            createMockExercise("Hanging Leg Raise", MuscleGroup.CORE),
        ))

        repository.initializeBuiltInTemplates()

        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Full Body - Day 1" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Full Body - Day 2" }
        ) }
        coVerify(atLeast = 1) { templateDao.insertTemplate(
            match { it.name == "Full Body - Day 3" }
        ) }
    }

    @Test
    fun `initializeBuiltInTemplates handles missing exercises gracefully`() = runTest {
        coEvery { templateDao.getAllTemplates() } returns emptyList()
        coEvery { exerciseRepository.getAllExercises() } returns flowOf(emptyList())

        repository.initializeBuiltInTemplates()

        coVerify(exactly = 0) { templateDao.insertTemplate(any()) }
    }

    private fun createMockExercise(name: String, muscleGroup: MuscleGroup) =
        com.gymbro.core.model.Exercise(
            id = UUID.randomUUID(),
            name = name,
            muscleGroup = muscleGroup,
        )
}
