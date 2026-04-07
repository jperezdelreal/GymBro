package com.gymbro.core.repository

import com.gymbro.core.database.dao.ExerciseDao
import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> =
        exerciseDao.getAllExercises().map { entities -> entities.map { it.toDomain() } }

    override fun getFilteredExercises(muscleGroup: String?, query: String?): Flow<List<Exercise>> =
        exerciseDao.getFilteredExercises(muscleGroup, query).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getExerciseById(id: String): Exercise? =
        exerciseDao.getExerciseById(id)?.toDomain()
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
