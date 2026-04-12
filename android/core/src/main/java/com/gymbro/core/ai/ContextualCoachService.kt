package com.gymbro.core.ai

import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.repository.ExerciseRepository
import com.gymbro.core.service.RpeTrendService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextualCoachService @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseRepository: ExerciseRepository,
    private val rpeTrendService: RpeTrendService,
) {
    data class CoachSuggestion(
        val id: String,
        val message: String,
        val context: String,
        val exerciseId: String? = null,
        val exerciseName: String? = null,
    )

    suspend fun generateSuggestions(
        currentExerciseId: String?,
        currentWeight: Double?,
        currentRpe: Double?,
        setCount: Int,
    ): List<CoachSuggestion> {
        val suggestions = mutableListOf<CoachSuggestion>()
        if (currentExerciseId == null) return suggestions
        
        val exercise = try { exerciseRepository.getExerciseById(currentExerciseId) } catch (_: Exception) { null }
        val exerciseName = exercise?.name ?: return suggestions
        val trend = try { rpeTrendService.getTrend(currentExerciseId) } catch (_: Exception) { null }
        
        if (trend != null && trend.isFatigueWarning) {
            suggestions.add(CoachSuggestion("fatigue_$currentExerciseId", "RPE trending high on $exerciseName", "Your RPE has been rising on $exerciseName. Consider reducing weight by 10%.", currentExerciseId, exerciseName))
        }
        if (currentRpe != null && currentRpe >= 9.0) {
            suggestions.add(CoachSuggestion("high_rpe_$currentExerciseId", "RPE ${currentRpe.toInt()} — consider reducing load", "You logged RPE $currentRpe on $exerciseName. Consider reducing weight by 5-10%.", currentExerciseId, exerciseName))
        }
        if (setCount >= 6) {
            suggestions.add(CoachSuggestion("high_volume_$currentExerciseId", "$setCount sets — watch for form breakdown", "You've done $setCount sets of $exerciseName. Watch for form breakdown.", currentExerciseId, exerciseName))
        }
        return suggestions
    }
}
