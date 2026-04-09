package com.gymbro.core.repository

import android.util.Log
import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAllExercises()
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Error getting all exercises", e)
                emit(emptyList())
            }

    override fun getFilteredExercises(muscleGroup: String?, query: String?): Flow<List<Exercise>> =
        exerciseDao.getFilteredExercises(muscleGroup, query)
            .map { entities -> entities.map { it.toDomain() } }
            .catch { e ->
                Log.e(TAG, "Error getting filtered exercises", e)
                emit(emptyList())
            }

    override suspend fun getExerciseById(id: String): Exercise? {
        val result = retryWithBackoff {
            runCatchingAsResult { exerciseDao.getExerciseById(id) }
        }
        return when (result) {
            is AppResult.Success -> result.data?.toDomain()
            is AppResult.Error -> {
                Log.e(TAG, "Failed to get exercise by id $id: ${result.error.message}")
                null
            }
        }
    }

    override suspend fun addExercise(exercise: Exercise) {
        val result = retryWithBackoff {
            runCatchingAsResult { exerciseDao.insertExercise(exercise.toEntity()) }
        }
        when (result) {
            is AppResult.Success -> Log.d(TAG, "Successfully added exercise ${exercise.name}")
            is AppResult.Error -> {
                Log.e(TAG, "Failed to add exercise ${exercise.name}: ${result.error.message}")
                throw Exception(result.error.message)
            }
        }
    }

    override suspend fun isExerciseNameTaken(name: String): Boolean {
        val result = retryWithBackoff {
            runCatchingAsResult { exerciseDao.countExercisesByName(name) }
        }
        return when (result) {
            is AppResult.Success -> result.data > 0
            is AppResult.Error -> {
                Log.e(TAG, "Failed to check if exercise name is taken: $name: ${result.error.message}")
                false
            }
        }
    }

    companion object {
        private const val TAG = "ExerciseRepositoryImpl"
    }
}

private fun ExerciseEntity.toDomain(): Exercise = Exercise(
    id = UUID.fromString(id),
    name = name,
    muscleGroup = MuscleGroup.entries.find { it.name == muscleGroup } ?: MuscleGroup.FULL_BODY,
    category = ExerciseCategory.entries.find { it.name == category } ?: ExerciseCategory.COMPOUND,
    equipment = Equipment.entries.find { it.name == equipment } ?: Equipment.OTHER,
    description = description,
    youtubeUrl = youtubeUrl,
)

private fun Exercise.toEntity(): ExerciseEntity = ExerciseEntity(
    id = id.toString(),
    name = name,
    muscleGroup = muscleGroup.name,
    category = category.name,
    equipment = equipment.name,
    description = description,
    youtubeUrl = youtubeUrl,
)
