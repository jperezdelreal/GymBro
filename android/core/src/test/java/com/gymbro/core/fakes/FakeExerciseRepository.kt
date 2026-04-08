package com.gymbro.core.fakes

import com.gymbro.core.model.Exercise
import com.gymbro.core.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class FakeExerciseRepository : ExerciseRepository {
    
    private val exercises = MutableStateFlow<List<Exercise>>(emptyList())
    
    fun setExercises(vararg exercises: Exercise) {
        this.exercises.value = exercises.toList()
    }
    
    fun clearExercises() {
        exercises.value = emptyList()
    }

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exercises
    }

    override fun getFilteredExercises(muscleGroup: String?, query: String?): Flow<List<Exercise>> {
        return exercises.map { allExercises ->
            allExercises.filter { exercise ->
                val matchesMuscleGroup = muscleGroup == null || 
                    exercise.muscleGroup.name.equals(muscleGroup, ignoreCase = true)
                
                val matchesQuery = query == null || 
                    exercise.name.contains(query, ignoreCase = true) ||
                    exercise.description.contains(query, ignoreCase = true)
                
                matchesMuscleGroup && matchesQuery
            }
        }
    }

    override suspend fun getExerciseById(id: String): Exercise? {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return exercises.value.find { it.id == uuid }
    }

    override suspend fun addExercise(exercise: Exercise) {
        exercises.value = exercises.value + exercise
    }

    override suspend fun isExerciseNameTaken(name: String): Boolean {
        return exercises.value.any { it.name.equals(name, ignoreCase = true) }
    }
}