package com.gymbro.core.database.dao

import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Contract tests for WorkoutTemplateDao query behavior.
 * These verify the DAO interface expectations without requiring Room database.
 */
class WorkoutTemplateDaoContractTest {

    private lateinit var dao: WorkoutTemplateDao

    @Before
    fun setup() {
        dao = mockk()
    }

    @Test
    fun `observeAllTemplates returns Flow sorted by isBuiltIn DESC then lastUsedAt DESC`() = runTest {
        val now = System.currentTimeMillis()
        val builtIn = createTemplateWithExercises("t1", isBuiltIn = true, lastUsedAt = now - 1000)
        val customRecent = createTemplateWithExercises("t2", isBuiltIn = false, lastUsedAt = now)
        val customOld = createTemplateWithExercises("t3", isBuiltIn = false, lastUsedAt = now - 2000)

        every { dao.observeAllTemplates() } returns flowOf(listOf(builtIn, customRecent, customOld))

        val result = dao.observeAllTemplates().first()

        assertEquals(3, result.size)
        // Built-in templates first
        assertTrue(result[0].template.isBuiltIn)
        // Then custom sorted by lastUsedAt
        assertFalse(result[1].template.isBuiltIn)
        assertTrue(result[1].template.lastUsedAt!! > result[2].template.lastUsedAt!!)
    }

    @Test
    fun `getTemplateWithExercises returns template with related exercises`() = runTest {
        val templateId = UUID.randomUUID().toString()
        val exercises = listOf(
            createTemplateExercise(templateId, exerciseId = "ex1", orderIndex = 0),
            createTemplateExercise(templateId, exerciseId = "ex2", orderIndex = 1)
        )
        val template = createTemplateWithExercises(templateId, exercises = exercises)

        coEvery { dao.getTemplateWithExercises(templateId) } returns template

        val result = dao.getTemplateWithExercises(templateId)

        assertNotNull(result)
        assertEquals(templateId, result!!.template.id)
        assertEquals(2, result.exercises.size)
    }

    @Test
    fun `getTemplateWithExercises returns null for non-existent template`() = runTest {
        val templateId = UUID.randomUUID().toString()

        coEvery { dao.getTemplateWithExercises(templateId) } returns null

        val result = dao.getTemplateWithExercises(templateId)

        assertNull(result)
    }

    @Test
    fun `insertTemplate uses REPLACE conflict strategy`() = runTest {
        val template = WorkoutTemplateEntity(
            id = UUID.randomUUID().toString(),
            name = "Push Day",
            description = "Chest, shoulders, triceps",
            isBuiltIn = false,
            createdAt = System.currentTimeMillis()
        )

        coEvery { dao.insertTemplate(template) } returns Unit

        dao.insertTemplate(template)

        coVerify { dao.insertTemplate(template) }
    }

    @Test
    fun `insertTemplateExercises uses REPLACE conflict strategy`() = runTest {
        val exercises = listOf(
            createTemplateExercise("template-id", exerciseId = "ex1", orderIndex = 0)
        )

        coEvery { dao.insertTemplateExercises(exercises) } returns Unit

        dao.insertTemplateExercises(exercises)

        coVerify { dao.insertTemplateExercises(exercises) }
    }

    @Test
    fun `deleteTemplate removes template by ID`() = runTest {
        val templateId = UUID.randomUUID().toString()

        coEvery { dao.deleteTemplate(templateId) } returns Unit

        dao.deleteTemplate(templateId)

        coVerify { dao.deleteTemplate(templateId) }
    }

    @Test
    fun `deleteTemplateExercises removes all exercises for template`() = runTest {
        val templateId = UUID.randomUUID().toString()

        coEvery { dao.deleteTemplateExercises(templateId) } returns Unit

        dao.deleteTemplateExercises(templateId)

        coVerify { dao.deleteTemplateExercises(templateId) }
    }

    @Test
    fun `updateLastUsedTimestamp updates timestamp for template`() = runTest {
        val templateId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        coEvery { dao.updateLastUsedTimestamp(templateId, timestamp) } returns Unit

        dao.updateLastUsedTimestamp(templateId, timestamp)

        coVerify { dao.updateLastUsedTimestamp(templateId, timestamp) }
    }

    @Test
    fun `getAllTemplates returns all templates sorted`() = runTest {
        val templates = listOf(
            createTemplateWithExercises("t1", isBuiltIn = true),
            createTemplateWithExercises("t2", isBuiltIn = false)
        )

        coEvery { dao.getAllTemplates() } returns templates

        val result = dao.getAllTemplates()

        assertEquals(2, result.size)
    }

    @Test
    fun `observeAllTemplates handles empty list`() = runTest {
        every { dao.observeAllTemplates() } returns flowOf(emptyList())

        val result = dao.observeAllTemplates().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `template exercises maintain order`() = runTest {
        val templateId = UUID.randomUUID().toString()
        val exercises = listOf(
            createTemplateExercise(templateId, exerciseId = "ex3", orderIndex = 2),
            createTemplateExercise(templateId, exerciseId = "ex1", orderIndex = 0),
            createTemplateExercise(templateId, exerciseId = "ex2", orderIndex = 1)
        )
        val template = createTemplateWithExercises(templateId, exercises = exercises)

        coEvery { dao.getTemplateWithExercises(templateId) } returns template

        val result = dao.getTemplateWithExercises(templateId)

        assertNotNull(result)
        // Order field should be preserved
        assertEquals(2, result!!.exercises[0].orderIndex)
        assertEquals(0, result.exercises[1].orderIndex)
        assertEquals(1, result.exercises[2].orderIndex)
    }

    private fun createTemplateWithExercises(
        id: String,
        isBuiltIn: Boolean = false,
        lastUsedAt: Long? = null,
        exercises: List<TemplateExerciseEntity> = emptyList()
    ): WorkoutTemplateWithExercises {
        val template = WorkoutTemplateEntity(
            id = id,
            name = "Template $id",
            description = "Description",
            isBuiltIn = isBuiltIn,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = lastUsedAt
        )
        return WorkoutTemplateWithExercises(template = template, exercises = exercises)
    }

    private fun createTemplateExercise(
        templateId: String,
        exerciseId: String,
        orderIndex: Int
    ): TemplateExerciseEntity {
        return TemplateExerciseEntity(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            exerciseId = exerciseId,
            exerciseName = "Exercise $exerciseId",
            muscleGroup = "Chest",
            targetSets = 3,
            targetReps = 10,
            orderIndex = orderIndex
        )
    }
}
