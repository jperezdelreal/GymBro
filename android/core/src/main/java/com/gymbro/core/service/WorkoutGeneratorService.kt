package com.gymbro.core.service

import android.util.Log
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.health.HealthConnectRepository
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.repository.ExerciseRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class SmartWorkoutSuggestion(
    val exercises: List<SuggestedExercise>,
    val targetMuscleGroups: List<MuscleGroup>,
    val reasoning: String,
    val recoveryScore: Int = 0,
    val generatedAt: Instant = Instant.now(),
)

data class SuggestedExercise(
    val exercise: Exercise,
    val targetSets: Int,
    val targetReps: IntRange,
    val suggestedWeight: Double?,
    val progressionHint: String?,
)

data class MuscleGroupHistory(
    val muscleGroup: MuscleGroup,
    val lastTrained: Instant?,
    val hoursSince: Long?,
    val setCount: Int,
)

class WorkoutGeneratorService @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseRepository: ExerciseRepository,
    private val healthConnectRepository: HealthConnectRepository,
) {

    suspend fun generateSmartWorkout(lookbackDays: Int = 7): SmartWorkoutSuggestion {
        return try {
            val allExercises = exerciseRepository.getAllExercises().first()
            val muscleGroupHistory = analyzeMuscleGroupHistory(lookbackDays)
            val recoveryMetrics = healthConnectRepository.getRecoveryMetrics()

            val targetMuscleGroups = selectTargetMuscleGroups(muscleGroupHistory, recoveryMetrics.readinessScore)
            val exercises = selectExercises(allExercises, targetMuscleGroups, recoveryMetrics.readinessScore)
            val reasoning = generateReasoning(muscleGroupHistory, targetMuscleGroups, recoveryMetrics.readinessScore)

            SmartWorkoutSuggestion(
                exercises = exercises,
                targetMuscleGroups = targetMuscleGroups,
                reasoning = reasoning,
                recoveryScore = recoveryMetrics.readinessScore,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate smart workout, returning default", e)
            SmartWorkoutSuggestion(
                exercises = emptyList(),
                targetMuscleGroups = emptyList(),
                reasoning = "Unable to generate workout suggestion. Please try again.",
                recoveryScore = 70,
            )
        }
    }

    private suspend fun analyzeMuscleGroupHistory(lookbackDays: Int): Map<MuscleGroup, MuscleGroupHistory> {
        val recentWorkouts = workoutDao.getRecentWorkouts(limit = 30).first()
        val now = Instant.now()
        val cutoff = now.minus(lookbackDays.toLong(), ChronoUnit.DAYS)

        val muscleGroupMap = mutableMapOf<MuscleGroup, MutableList<Pair<Instant, Int>>>()

        for (workout in recentWorkouts) {
            if (!workout.workout.completed) continue
            val workoutTime = Instant.ofEpochMilli(workout.workout.startedAt)
            if (workoutTime.isBefore(cutoff)) continue

            val exercisesInWorkout = exerciseRepository.getAllExercises().first()
            val exerciseMap = exercisesInWorkout.associateBy { it.id.toString() }

            workout.sets.forEach { set ->
                if (!set.isWarmup) {
                    val exercise = exerciseMap[set.exerciseId]
                    if (exercise != null) {
                        muscleGroupMap.getOrPut(exercise.muscleGroup) { mutableListOf() }
                            .add(workoutTime to 1)
                    }
                }
            }
        }

        return MuscleGroup.entries.associateWith { muscleGroup ->
            val sessions = muscleGroupMap[muscleGroup] ?: emptyList()
            val lastTrained = sessions.maxOfOrNull { it.first }
            val hoursSince = lastTrained?.let { ChronoUnit.HOURS.between(it, now) }
            val setCount = sessions.sumOf { it.second }

            MuscleGroupHistory(
                muscleGroup = muscleGroup,
                lastTrained = lastTrained,
                hoursSince = hoursSince,
                setCount = setCount,
            )
        }
    }

    private fun selectTargetMuscleGroups(
        muscleGroupHistory: Map<MuscleGroup, MuscleGroupHistory>,
        recoveryScore: Int,
    ): List<MuscleGroup> {
        val MIN_RECOVERY_HOURS = 48L

        val candidates = muscleGroupHistory.entries
            .filter { (mg, history) ->
                mg != MuscleGroup.FULL_BODY &&
                (history.hoursSince == null || history.hoursSince >= MIN_RECOVERY_HOURS)
            }
            .sortedBy { it.value.hoursSince ?: Long.MAX_VALUE }
            .reversed()

        if (recoveryScore < 40) {
            return candidates.take(1).map { it.key }
        }

        val primaryGroup = candidates.firstOrNull()?.key ?: MuscleGroup.CHEST
        val secondaryGroup = findComplementaryMuscleGroup(primaryGroup, candidates.map { it.key })

        return listOfNotNull(primaryGroup, secondaryGroup)
    }

    private fun findComplementaryMuscleGroup(primary: MuscleGroup, available: List<MuscleGroup>): MuscleGroup? {
        val complementaryPairs = mapOf(
            MuscleGroup.CHEST to listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
            MuscleGroup.BACK to listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            MuscleGroup.SHOULDERS to listOf(MuscleGroup.TRICEPS, MuscleGroup.CHEST),
            MuscleGroup.QUADRICEPS to listOf(MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
            MuscleGroup.HAMSTRINGS to listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES),
            MuscleGroup.BICEPS to listOf(MuscleGroup.BACK, MuscleGroup.FOREARMS),
            MuscleGroup.TRICEPS to listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
        )

        return complementaryPairs[primary]?.firstOrNull { it in available }
    }

    private suspend fun selectExercises(
        allExercises: List<Exercise>,
        targetMuscleGroups: List<MuscleGroup>,
        recoveryScore: Int,
    ): List<SuggestedExercise> {
        val exercisesByMuscle = allExercises.filter { it.muscleGroup in targetMuscleGroups }
        val suggested = mutableListOf<SuggestedExercise>()

        for (muscleGroup in targetMuscleGroups) {
            val muscleExercises = exercisesByMuscle.filter { it.muscleGroup == muscleGroup }
            
            val compound = muscleExercises.firstOrNull { it.category == ExerciseCategory.COMPOUND }
            val isolation = muscleExercises.filter { it.category == ExerciseCategory.ISOLATION }.take(2)

            if (compound != null) {
                val history = getExerciseHistory(compound.id.toString())
                suggested.add(
                    SuggestedExercise(
                        exercise = compound,
                        targetSets = if (recoveryScore < 40) 2 else 4,
                        targetReps = if (recoveryScore < 40) 8..10 else 6..8,
                        suggestedWeight = history?.weight?.let { it * 1.025 },
                        progressionHint = generateProgressionHint(history),
                    )
                )
            }

            isolation.forEach { exercise ->
                val history = getExerciseHistory(exercise.id.toString())
                suggested.add(
                    SuggestedExercise(
                        exercise = exercise,
                        targetSets = if (recoveryScore < 40) 2 else 3,
                        targetReps = 10..12,
                        suggestedWeight = history?.weight?.let { it * 1.025 },
                        progressionHint = generateProgressionHint(history),
                    )
                )
            }
        }

        return suggested.take(if (recoveryScore < 40) 4 else 6)
    }

    private suspend fun getExerciseHistory(exerciseId: String): ExerciseHistorySummary? {
        return try {
            val sets = workoutDao.getSetsByExercise(exerciseId)
            if (sets.isEmpty()) return null

            val lastSet = sets.maxByOrNull { it.completedAt } ?: return null
            ExerciseHistorySummary(
                weight = lastSet.weight,
                reps = lastSet.reps,
                timestamp = Instant.ofEpochMilli(lastSet.completedAt),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get exercise history for $exerciseId", e)
            null
        }
    }

    private fun generateProgressionHint(history: ExerciseHistorySummary?): String? {
        if (history == null) return "First time — start conservative"
        
        val daysSince = ChronoUnit.DAYS.between(history.timestamp, Instant.now())
        return when {
            daysSince > 14 -> "Long break — reduce weight 10%"
            history.reps >= 12 -> "Increase weight by 2.5kg"
            history.reps >= 8 -> "Try adding 1-2 reps"
            else -> "Maintain current weight"
        }
    }

    private fun generateReasoning(
        muscleGroupHistory: Map<MuscleGroup, MuscleGroupHistory>,
        targetGroups: List<MuscleGroup>,
        recoveryScore: Int,
    ): String {
        val primaryGroup = targetGroups.firstOrNull()
        val secondaryGroup = targetGroups.getOrNull(1)

        val primaryHistory = primaryGroup?.let { muscleGroupHistory[it] }
        val daysSince = primaryHistory?.hoursSince?.div(24)

        val recoveryStatus = when {
            recoveryScore >= 80 -> "Excellent recovery"
            recoveryScore >= 60 -> "Good recovery"
            recoveryScore >= 40 -> "Moderate recovery"
            else -> "Low recovery"
        }

        return buildString {
            append(recoveryStatus)
            append(" • ")
            if (primaryGroup != null) {
                append(primaryGroup.displayName)
                if (daysSince != null) {
                    append(" (${daysSince} days rest)")
                } else {
                    append(" (untrained)")
                }
            }
            if (secondaryGroup != null) {
                append(" + ${secondaryGroup.displayName}")
            }
        }
    }

    private data class ExerciseHistorySummary(
        val weight: Double,
        val reps: Int,
        val timestamp: Instant,
    )

    companion object {
        private const val TAG = "WorkoutGeneratorService"
    }
}
