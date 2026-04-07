package com.gymbro.core.repository

import com.gymbro.core.database.dao.WorkoutTemplateDao
import com.gymbro.core.database.dao.WorkoutTemplateWithExercises
import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.TemplateExercise
import com.gymbro.core.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val templateDao: WorkoutTemplateDao,
    private val exerciseRepository: ExerciseRepository,
) : WorkoutTemplateRepository {

    override fun observeAllTemplates(): Flow<List<WorkoutTemplate>> {
        return templateDao.observeAllTemplates().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllTemplates(): List<WorkoutTemplate> {
        return templateDao.getAllTemplates().map { it.toDomain() }
    }

    override suspend fun getTemplate(templateId: String): WorkoutTemplate? {
        return templateDao.getTemplateWithExercises(templateId)?.toDomain()
    }

    override suspend fun saveTemplate(template: WorkoutTemplate) {
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

    override suspend fun deleteTemplate(templateId: String) {
        templateDao.deleteTemplate(templateId)
    }

    override suspend fun updateLastUsed(templateId: String) {
        templateDao.updateLastUsedTimestamp(templateId, System.currentTimeMillis())
    }

    override suspend fun initializeBuiltInTemplates() {
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
