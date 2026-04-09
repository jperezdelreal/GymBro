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
}
