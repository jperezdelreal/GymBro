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
import kotlinx.coroutines.flow.first
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

            // Wait for exercises to be seeded before building templates
            val allExercises = exerciseRepository.getAllExercises().first()
            if (allExercises.isEmpty()) {
                return // No exercises available yet, skip template initialization
            }

            // Helper function to find exercise by name
            fun findExercise(name: String) = allExercises.find { it.name == name }

                // 1. Starting Strength 5x5 - Day A
                val ss5x5DayA = listOf(
                    findExercise("Barbell Back Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 5, targetReps = 5, order = 0)
                    },
                    findExercise("Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 5, targetReps = 5, order = 1)
                    },
                    findExercise("Barbell Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 5, targetReps = 5, order = 2)
                    },
                ).filterNotNull()

                if (ss5x5DayA.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Starting Strength 5×5 - Day A",
                        description = "Classic beginner strength program: Squat, Bench, Row. Linear progression, 3 days/week.",
                        exercises = ss5x5DayA,
                        isBuiltIn = true,
                    ))
                }

                // 2. Starting Strength 5x5 - Day B
                val ss5x5DayB = listOf(
                    findExercise("Barbell Back Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 5, targetReps = 5, order = 0)
                    },
                    findExercise("Barbell Overhead Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 5, targetReps = 5, order = 1)
                    },
                    findExercise("Conventional Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 1, targetReps = 5, order = 2)
                    },
                ).filterNotNull()

                if (ss5x5DayB.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Starting Strength 5×5 - Day B",
                        description = "Alternate day: Squat, Overhead Press, Deadlift. Alternate with Day A.",
                        exercises = ss5x5DayB,
                        isBuiltIn = true,
                    ))
                }

                // 3. PPL - Push Day
                val pplPush = listOf(
                    findExercise("Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 0)
                    },
                    findExercise("Barbell Overhead Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 1)
                    },
                    findExercise("Incline Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 2)
                    },
                    findExercise("Cable Tricep Extension (Rope)")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 3)
                    },
                    findExercise("Lateral Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 4)
                    },
                    findExercise("Cable Crossover")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 5)
                    },
                ).filterNotNull()

                if (pplPush.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "PPL - Push Day",
                        description = "Chest, shoulders, triceps. Hypertrophy-focused with 8-12 reps.",
                        exercises = pplPush,
                        isBuiltIn = true,
                    ))
                }

                // 4. PPL - Pull Day
                val pplPull = listOf(
                    findExercise("Conventional Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 6, order = 0)
                    },
                    findExercise("Pull-Up")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 1)
                    },
                    findExercise("Barbell Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 10, order = 2)
                    },
                    findExercise("Cable Lat Pulldown (Wide Grip)")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 3)
                    },
                    findExercise("Cable Bicep Curl")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 4)
                    },
                    findExercise("Face Pull")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 15, order = 5)
                    },
                ).filterNotNull()

                if (pplPull.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "PPL - Pull Day",
                        description = "Back, biceps, rear delts. Compound pulling movements + accessory work.",
                        exercises = pplPull,
                        isBuiltIn = true,
                    ))
                }

                // 5. PPL - Leg Day
                val pplLegs = listOf(
                    findExercise("Barbell Back Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 0)
                    },
                    findExercise("Romanian Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 1)
                    },
                    findExercise("Leg Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 2)
                    },
                    findExercise("Leg Curl")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 3)
                    },
                    findExercise("Leg Extension")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 4)
                    },
                    findExercise("Standing Calf Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 15, order = 5)
                    },
                ).filterNotNull()

                if (pplLegs.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "PPL - Leg Day",
                        description = "Complete lower body: quads, hamstrings, glutes, calves.",
                        exercises = pplLegs,
                        isBuiltIn = true,
                    ))
                }

                // 6. Upper/Lower - Upper A
                val upperLowerUpperA = listOf(
                    findExercise("Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 0)
                    },
                    findExercise("Barbell Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 1)
                    },
                    findExercise("Barbell Overhead Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 2)
                    },
                    findExercise("Lat Pulldown")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 3)
                    },
                    findExercise("Cable Bicep Curl")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 4)
                    },
                    findExercise("Cable Tricep Extension (Rope)")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 5)
                    },
                ).filterNotNull()

                if (upperLowerUpperA.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Upper/Lower - Upper A",
                        description = "Upper body strength day. Horizontal push/pull focus.",
                        exercises = upperLowerUpperA,
                        isBuiltIn = true,
                    ))
                }

                // 7. Upper/Lower - Lower A
                val upperLowerLowerA = listOf(
                    findExercise("Barbell Back Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 0)
                    },
                    findExercise("Romanian Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 1)
                    },
                    findExercise("Bulgarian Split Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 2)
                    },
                    findExercise("Leg Curl")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 3)
                    },
                    findExercise("Standing Calf Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 15, order = 4)
                    },
                ).filterNotNull()

                if (upperLowerLowerA.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Upper/Lower - Lower A",
                        description = "Lower body strength: squat variation + hip hinge.",
                        exercises = upperLowerLowerA,
                        isBuiltIn = true,
                    ))
                }

                // 8. Upper/Lower - Upper B
                val upperLowerUpperB = listOf(
                    findExercise("Incline Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 0)
                    },
                    findExercise("Pull-Up")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 1)
                    },
                    findExercise("Dumbbell Shoulder Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 2)
                    },
                    findExercise("Cable Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 3)
                    },
                    findExercise("Lateral Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 15, order = 4)
                    },
                    findExercise("Face Pull")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 15, order = 5)
                    },
                ).filterNotNull()

                if (upperLowerUpperB.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Upper/Lower - Upper B",
                        description = "Upper body hypertrophy. Incline press + vertical pull focus.",
                        exercises = upperLowerUpperB,
                        isBuiltIn = true,
                    ))
                }

                // 9. Upper/Lower - Lower B
                val upperLowerLowerB = listOf(
                    findExercise("Conventional Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 5, order = 0)
                    },
                    findExercise("Front Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 1)
                    },
                    findExercise("Leg Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 2)
                    },
                    findExercise("Leg Extension")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 3)
                    },
                    findExercise("Seated Calf Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 15, order = 4)
                    },
                ).filterNotNull()

                if (upperLowerLowerB.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Upper/Lower - Lower B",
                        description = "Lower body: deadlift + front squat focus with accessory volume.",
                        exercises = upperLowerLowerB,
                        isBuiltIn = true,
                    ))
                }

                // 10. Full Body - Day 1
                val fullBodyDay1 = listOf(
                    findExercise("Barbell Back Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 0)
                    },
                    findExercise("Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 1)
                    },
                    findExercise("Barbell Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 2)
                    },
                    findExercise("Barbell Overhead Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 3)
                    },
                    findExercise("Romanian Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 4)
                    },
                    findExercise("Plank")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 1, order = 5)
                    },
                ).filterNotNull()

                if (fullBodyDay1.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Full Body - Day 1",
                        description = "Complete body workout hitting all major movement patterns. Squat + horizontal press focus.",
                        exercises = fullBodyDay1,
                        isBuiltIn = true,
                    ))
                }

                // 11. Full Body - Day 2
                val fullBodyDay2 = listOf(
                    findExercise("Conventional Deadlift")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 5, order = 0)
                    },
                    findExercise("Barbell Overhead Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 1)
                    },
                    findExercise("Pull-Up")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 2)
                    },
                    findExercise("Incline Barbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 8, order = 3)
                    },
                    findExercise("Bulgarian Split Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 4)
                    },
                    findExercise("Ab Wheel Rollout")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 5)
                    },
                ).filterNotNull()

                if (fullBodyDay2.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Full Body - Day 2",
                        description = "Full body with deadlift + vertical press/pull emphasis. Alternate with Day 1.",
                        exercises = fullBodyDay2,
                        isBuiltIn = true,
                    ))
                }

                // 12. Full Body - Day 3
                val fullBodyDay3 = listOf(
                    findExercise("Front Squat")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 6, order = 0)
                    },
                    findExercise("Dumbbell Bench Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 4, targetReps = 8, order = 1)
                    },
                    findExercise("Cable Row")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 2)
                    },
                    findExercise("Dumbbell Shoulder Press")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 10, order = 3)
                    },
                    findExercise("Leg Curl")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 4)
                    },
                    findExercise("Hanging Leg Raise")?.let {
                        TemplateExercise(exerciseId = it.id, exerciseName = it.name, muscleGroup = it.muscleGroup,
                            targetSets = 3, targetReps = 12, order = 5)
                    },
                ).filterNotNull()

                if (fullBodyDay3.isNotEmpty()) {
                    saveTemplate(WorkoutTemplate(
                        name = "Full Body - Day 3",
                        description = "Full body variation workout with dumbbell emphasis and unilateral work.",
                        exercises = fullBodyDay3,
                        isBuiltIn = true,
                    ))
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
