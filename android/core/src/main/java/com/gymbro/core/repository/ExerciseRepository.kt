package com.gymbro.core.repository

import com.gymbro.core.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getFilteredExercises(muscleGroup: String?, query: String?): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
}
