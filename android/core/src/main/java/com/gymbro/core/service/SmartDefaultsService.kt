package com.gymbro.core.service

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.Exercise
import javax.inject.Inject

class SmartDefaultsService @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val progressionEngine: ProgressionEngine,
    private val beginnerDefaultsService: BeginnerDefaultsService,
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
        val progressionReason: ProgressionEngine.ProgressionReason? = null,
        val beginnerSuggestion: String? = null,
    )
    
    suspend fun getDefaults(exerciseId: String): SmartDefaults {
        val result = retryWithBackoff {
            runCatchingAsResult {
                val suggestion = progressionEngine.getSuggestion(exerciseId)
                val sets = workoutDao.getSetsByExercise(exerciseId)
                val lastSet = sets.lastOrNull()
                SmartDefaults(
                    weight = suggestion?.suggestedWeightKg ?: lastSet?.weight,
                    reps = lastSet?.reps,
                    progressionReason = suggestion?.reason,
                )
            }
        }
        return when (result) {
            is AppResult.Success -> result.data
            is AppResult.Error -> SmartDefaults(null, null)
        }
    }

    /**
     * Fallback chain: History-based defaults → BeginnerDefaults → empty.
     * Returns beginnerSuggestion as placeholder text when no history exists.
     */
    suspend fun getDefaultsWithFallback(exerciseId: String, exercise: Exercise): SmartDefaults {
        val historyDefaults = getDefaults(exerciseId)
        if (historyDefaults.weight != null) return historyDefaults

        val beginner = try {
            beginnerDefaultsService.getDefault(
                exerciseName = exercise.name,
                category = exercise.category,
                equipment = exercise.equipment,
            )
        } catch (_: Exception) {
            null
        }

        return historyDefaults.copy(
            beginnerSuggestion = beginner?.let { 
                if (it.suggestedWeightKg == 0.0) null 
                else "${it.suggestedWeightKg.toInt()} kg" 
            },
        )
    }
}
