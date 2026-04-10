package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import javax.inject.Inject

class SmartDefaultsService @Inject constructor(
    private val workoutDao: WorkoutDao,
) {
    
    suspend fun getDefaultWeight(exerciseId: String): Double? {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val sets = workoutDao.getSetsByExercise(exerciseId)
                sets.lastOrNull()?.weight
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> null
        }
    }
    
    suspend fun getDefaultReps(exerciseId: String): Int? {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val sets = workoutDao.getSetsByExercise(exerciseId)
                sets.lastOrNull()?.reps
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> null
        }
    }
    
    data class SmartDefaults(
        val weight: Double?,
        val reps: Int?,
    )
    
    suspend fun getDefaults(exerciseId: String): SmartDefaults {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val sets = workoutDao.getSetsByExercise(exerciseId)
                val lastSet = sets.lastOrNull()
                SmartDefaults(
                    weight = lastSet?.weight,
                    reps = lastSet?.reps,
                )
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> SmartDefaults(null, null)
        }
    }
}
