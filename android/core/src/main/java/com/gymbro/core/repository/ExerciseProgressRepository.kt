package com.gymbro.core.repository

import com.gymbro.core.model.ExerciseProgressPoint
import kotlinx.coroutines.flow.Flow

interface ExerciseProgressRepository {
    fun getExerciseProgressData(exerciseId: String): Flow<List<ExerciseProgressPoint>>
}
