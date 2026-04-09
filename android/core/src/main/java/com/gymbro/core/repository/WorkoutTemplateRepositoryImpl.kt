package com.gymbro.core.repository

import android.util.Log
import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.dao.WorkoutTemplateWithExercises
import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.TemplateExercise
import com.gymbro.core.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val templateDao: WorkoutTemplateDao,
    private val exerciseRepository: ExerciseRepository,
) : WorkoutTemplateRepository {

    override fun observeAllTemplates(): Flow<List<WorkoutTemplate>> {
        return templateDao.observeAllTemplates()
            .map { list -> list.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Error observing all templates", e)
                emit(emptyList())
            }
    }

    override suspend fun getAllTemplates(): List<WorkoutTemplate> {
        val result = retryWithBackoff {
            runCatchingAsResult { templateDao.getAllTemplates().map { it.toDomain() } }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get all templates: ${result.error.message}")
                emptyList()
            }
        }
    }

    override suspend fun getTemplate(templateId: String): WorkoutTemplate? {
        val result = retryWithBackoff {
            runCatchingAsResult { templateDao.getTemplateWithExercises(templateId) }
        }
        return when (result) {
            is AppResult.Success -> result.data?.toDomain()
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get template $templateId: ${result.error.message}")
                null
            }
        }
    }

    override suspend fun saveTemplate(template: WorkoutTemplate) {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val entity = WorkoutTemplateEntity(
                    id = template.id.toString(),
                    name = template.name,
                    description = template.description,
                    createdAt = template.createdAt.toEpochMilli(),
                    lastUsedAt = template.lastUsedAt?.toEpochMilli(),
                    isBuiltIn = template.isBuiltIn,
                )
                templateDao.insertTemplate(entity)

                val exerciseEntities = template.exercises.map { exercise ->
                    TemplateExerciseEntity(
                        id = exercise.id.toString(),
                        templateId = template.id.toString(),
                        exerciseId = exercise.exerciseId.toString(),
                        exerciseName = exercise.exerciseName,
                        muscleGroup = exercise.muscleGroup.name,
                        targetSets = exercise.targetSets,
                        targetReps = exercise.targetReps,
                        targetWeightKg = exercise.targetWeightKg,
                        orderIndex = exercise.order,
                    )
                }
                templateDao.deleteTemplateExercises(template.id.toString())
                templateDao.insertTemplateExercises(exerciseEntities)
            }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to save template ${template.name}: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun deleteTemplate(templateId: String) {
        val result = retryWithBackoff {
            runCatchingAsResult { templateDao.deleteTemplate(templateId) }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to delete template $templateId: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun updateLastUsed(templateId: String) {
        val result = retryWithBackoff {
            runCatchingAsResult { 
                templateDao.updateLastUsedTimestamp(templateId, System.currentTimeMillis()) 
            }
        }
        when (result) {
            is AppResult.Success -> Unit
            is AppResult.Error -> {
                Log.e(TAG, "Failed to update last used for template $templateId: ${result.error.message}")
            }
        }
    }

    override suspend fun initializeBuiltInTemplates() {
        try {
            val existingTemplates = templateDao.getAllTemplates()
            if (existingTemplates.any { it.template.isBuiltIn }) {
                return // Already initialized
            }

            // Get exercises from the database to build templates
            exerciseRepository.getAllExercises().collect { allExercises ->
            // Push Day Template
            val pushExercises = listOf(
                allExercises.find { it.name.contains("Bench Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 8,
                        order = 0,
                    )
                },
                allExercises.find { it.name.contains("Overhead Press", ignoreCase = true) || it.name.contains("Military Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 10,
                        order = 1,
                    )
                },
                allExercises.find { it.name.contains("Incline", ignoreCase = true) && it.name.contains("Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 10,
                        order = 2,
                    )
                },
                allExercises.find { it.name.contains("Tricep", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 12,
                        order = 3,
                    )
                },
            ).filterNotNull()

            if (pushExercises.isNotEmpty()) {
                val pushTemplate = WorkoutTemplate(
                    name = "Push Day",
                    description = "Chest, shoulders, and triceps workout",
                    exercises = pushExercises,
                    isBuiltIn = true,
                )
                saveTemplate(pushTemplate)
            }

            // Pull Day Template
            val pullExercises = listOf(
                allExercises.find { it.name.contains("Pull", ignoreCase = true) && it.name.contains("Up", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 8,
                        order = 0,
                    )
                },
                allExercises.find { it.name.contains("Row", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 10,
                        order = 1,
                    )
                },
                allExercises.find { it.name.contains("Curl", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 12,
                        order = 2,
                    )
                },
            ).filterNotNull()

            if (pullExercises.isNotEmpty()) {
                val pullTemplate = WorkoutTemplate(
                    name = "Pull Day",
                    description = "Back and biceps workout",
                    exercises = pullExercises,
                    isBuiltIn = true,
                )
                saveTemplate(pullTemplate)
            }

            // Leg Day Template
            val legExercises = listOf(
                allExercises.find { it.name.contains("Squat", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 8,
                        order = 0,
                    )
                },
                allExercises.find { it.name.contains("Deadlift", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 6,
                        order = 1,
                    )
                },
                allExercises.find { it.name.contains("Leg Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 12,
                        order = 2,
                    )
                },
                allExercises.find { it.name.contains("Calf", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 15,
                        order = 3,
                    )
                },
            ).filterNotNull()

            if (legExercises.isNotEmpty()) {
                val legTemplate = WorkoutTemplate(
                    name = "Leg Day",
                    description = "Lower body workout",
                    exercises = legExercises,
                    isBuiltIn = true,
                )
                saveTemplate(legTemplate)
            }

            // Upper Body Template
            val upperExercises = listOf(
                allExercises.find { it.name.contains("Bench Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 8,
                        order = 0,
                    )
                },
                allExercises.find { it.name.contains("Row", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 4,
                        targetReps = 8,
                        order = 1,
                    )
                },
                allExercises.find { it.name.contains("Overhead Press", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 10,
                        order = 2,
                    )
                },
                allExercises.find { it.name.contains("Curl", ignoreCase = true) }?.let {
                    TemplateExercise(
                        exerciseId = it.id,
                        exerciseName = it.name,
                        muscleGroup = it.muscleGroup,
                        targetSets = 3,
                        targetReps = 12,
                        order = 3,
                    )
                },
            ).filterNotNull()

            if (upperExercises.isNotEmpty()) {
                val upperTemplate = WorkoutTemplate(
                    name = "Upper Body",
                    description = "Full upper body workout",
                    exercises = upperExercises,
                    isBuiltIn = true,
                )
                saveTemplate(upperTemplate)
            }
        }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize built-in templates: ${e.message}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "WorkoutTemplateRepositoryImpl"
    }
}

private fun WorkoutTemplateWithExercises.toDomain(): WorkoutTemplate {
    return WorkoutTemplate(
        id = UUID.fromString(template.id),
        name = template.name,
        description = template.description,
        exercises = exercises.sortedBy { it.orderIndex }.map { exercise ->
            TemplateExercise(
                id = UUID.fromString(exercise.id),
                exerciseId = UUID.fromString(exercise.exerciseId),
                exerciseName = exercise.exerciseName,
                muscleGroup = MuscleGroup.entries.find { it.name == exercise.muscleGroup } ?: MuscleGroup.FULL_BODY,
                targetSets = exercise.targetSets,
                targetReps = exercise.targetReps,
                targetWeightKg = exercise.targetWeightKg,
                order = exercise.orderIndex,
            )
        },
        createdAt = Instant.ofEpochMilli(template.createdAt),
        lastUsedAt = template.lastUsedAt?.let { Instant.ofEpochMilli(it) },
        isBuiltIn = template.isBuiltIn,
    )
}
