package com.gymbro.core.repository

import com.gymbro.core.model.Equipment
import com.gymbro.core.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAllExercises(): Flow<List<Exercise>>
    fun getFilteredExercises(muscleGroup: String?, query: String?): Flow<List<Exercise>>
    suspend fun getExerciseById(id: String): Exercise?
    suspend fun addExercise(exercise: Exercise)
    suspend fun isExerciseNameTaken(name: String): Boolean
    
    /**
     * Finds substitute exercises for a given exercise.
     * Returns exercises with the same muscle group and category, ranked by equipment similarity.
     */
    suspend fun findSubstitutes(
        exerciseId: String,
        availableEquipment: Set<Equipment>? = null,
        limit: Int = 5
    ): List<Exercise>
}
