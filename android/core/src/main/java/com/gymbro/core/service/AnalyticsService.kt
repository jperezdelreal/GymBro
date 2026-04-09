package com.gymbro.core.service

import android.util.Log
import com.gymbro.core.database.dao.WorkoutDao
import com.gymbro.core.repository.ExerciseRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt

data class WeeklyVolumeData(
    val weekStartDate: LocalDate,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double,
    val workoutCount: Int,
)

data class MuscleGroupDistribution(
    val muscleGroup: String,
    val volumePercentage: Double,
    val totalVolume: Double,
    val setCount: Int,
)

data class WorkoutFrequencyData(
    val weekStartDate: LocalDate,
    val workoutCount: Int,
)

data class ConsistencyMetrics(
    val currentStreak: Int,
    val longestStreak: Int,
    val averageWorkoutsPerWeek: Double,
    val consistencyScore: Int,
    val workoutDates: List<LocalDate>,
)

data class TopExercise(
    val exerciseId: String,
    val exerciseName: String,
    val totalVolume: Double,
    val totalSets: Int,
    val frequency: Int,
)

data class WorkoutDurationTrend(
    val weekStartDate: LocalDate,
    val averageDurationMinutes: Double,
)

data class WeeklySummary(
    val thisWeekVolume: Double,
    val lastWeekVolume: Double,
    val thisWeekWorkouts: Int,
    val lastWeekWorkouts: Int,
    val thisWeekPRs: Int,
    val volumeChange: Double,
)

class AnalyticsService @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val exerciseRepository: ExerciseRepository,
    private val prService: PersonalRecordService,
) {

    suspend fun getWeeklyVolumeData(weeks: Int = 8): List<WeeklyVolumeData> {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val now = LocalDate.now()
        
        (0 until weeks).map { weekOffset ->
            val weekStart = now.minusWeeks(weekOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
            val weekEnd = weekStart.plusDays(6)
            
            val weekWorkouts = workouts.filter {
                val workoutDate = Instant.ofEpochMilli(it.workout.startedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                workoutDate in weekStart..weekEnd
            }
            
            val sets = weekWorkouts.flatMap { it.sets.filter { s -> !s.isWarmup } }
            
            WeeklyVolumeData(
                weekStartDate = weekStart,
                totalSets = sets.size,
                totalReps = sets.sumOf { it.reps },
                totalVolume = sets.sumOf { it.weight * it.reps },
                workoutCount = weekWorkouts.size,
            )
        }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get weekly volume data", e)
            emptyList()
        }
    }

    suspend fun getMuscleGroupDistribution(): List<MuscleGroupDistribution> {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val sets = workouts.flatMap { it.sets.filter { s -> !s.isWarmup } }
        
        val volumeByMuscleGroup = sets
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, exerciseSets) ->
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                exercise?.let { 
                    it.muscleGroup.displayName to exerciseSets.sumOf { s -> s.weight * s.reps }
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, volumes) -> volumes.sum() }
        
        val totalVolume = volumeByMuscleGroup.values.sum()
        
        if (totalVolume == 0.0) return emptyList()
        
        return volumeByMuscleGroup.map { (muscleGroup, volume) ->
            MuscleGroupDistribution(
                muscleGroup = muscleGroup,
                volumePercentage = (volume / totalVolume) * 100,
                totalVolume = volume,
                setCount = sets.count { s ->
                    exerciseRepository.getExerciseById(s.exerciseId)?.muscleGroup?.displayName == muscleGroup
                },
            )
        }.sortedByDescending { it.totalVolume }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get muscle group distribution: ${e.message}", e)
            throw e
        }
    }

    suspend fun getWorkoutFrequency(weeks: Int = 12): List<WorkoutFrequencyData> {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val now = LocalDate.now()
        
        return (0 until weeks).map { weekOffset ->
            val weekStart = now.minusWeeks(weekOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
            val weekEnd = weekStart.plusDays(6)
            
            val weekWorkoutCount = workouts.count {
                val workoutDate = Instant.ofEpochMilli(it.workout.startedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                workoutDate in weekStart..weekEnd
            }
            
            WorkoutFrequencyData(
                weekStartDate = weekStart,
                workoutCount = weekWorkoutCount,
            )
        }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout frequency: ${e.message}", e)
            throw e
        }
    }

    suspend fun getConsistencyMetrics(): ConsistencyMetrics {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val workoutDates = workouts
            .map { 
                Instant.ofEpochMilli(it.workout.startedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .distinct()
            .sortedDescending()
        
        if (workoutDates.isEmpty()) {
            return ConsistencyMetrics(
                currentStreak = 0,
                longestStreak = 0,
                averageWorkoutsPerWeek = 0.0,
                consistencyScore = 0,
                workoutDates = emptyList(),
            )
        }
        
        val currentStreak = calculateCurrentStreak(workoutDates)
        val longestStreak = calculateLongestStreak(workoutDates)
        
        val firstWorkout = workoutDates.minOrNull() ?: LocalDate.now()
        val weeksSinceFirstWorkout = ChronoUnit.WEEKS.between(firstWorkout, LocalDate.now()).coerceAtLeast(1)
        val averageWorkoutsPerWeek = workoutDates.size.toDouble() / weeksSinceFirstWorkout
        
        val consistencyScore = calculateConsistencyScore(currentStreak, averageWorkoutsPerWeek)
        
        return ConsistencyMetrics(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            averageWorkoutsPerWeek = averageWorkoutsPerWeek,
            consistencyScore = consistencyScore,
            workoutDates = workoutDates,
        )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get consistency metrics: ${e.message}", e)
            throw e
        }
    }

    private fun calculateCurrentStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        
        val today = LocalDate.now()
        val mostRecent = sortedDates.first()
        
        val daysSinceLastWorkout = ChronoUnit.DAYS.between(mostRecent, today)
        if (daysSinceLastWorkout > 7) return 0
        
        var streak = 0
        var currentWeek = today.with(java.time.DayOfWeek.MONDAY)
        
        for (date in sortedDates) {
            val dateWeek = date.with(java.time.DayOfWeek.MONDAY)
            if (dateWeek == currentWeek || dateWeek == currentWeek.minusWeeks(streak.toLong())) {
                if (dateWeek != currentWeek) {
                    currentWeek = dateWeek
                    streak++
                } else if (streak == 0) {
                    streak = 1
                }
            } else {
                break
            }
        }
        
        return streak
    }

    private fun calculateLongestStreak(sortedDates: List<LocalDate>): Int {
        if (sortedDates.isEmpty()) return 0
        
        val weekSet = sortedDates.map { it.with(java.time.DayOfWeek.MONDAY) }.distinct().sorted()
        
        var longest = 1
        var current = 1
        
        for (i in 1 until weekSet.size) {
            val weeksBetween = ChronoUnit.WEEKS.between(weekSet[i - 1], weekSet[i])
            if (weeksBetween == 1L) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }
        
        return longest
    }

    private fun calculateConsistencyScore(streak: Int, avgPerWeek: Double): Int {
        val streakScore = (streak * 10).coerceAtMost(50)
        val frequencyScore = (avgPerWeek * 10).roundToInt().coerceAtMost(50)
        return (streakScore + frequencyScore).coerceIn(0, 100)
    }

    suspend fun getTopExercises(limit: Int = 10): List<TopExercise> {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val sets = workouts.flatMap { it.sets.filter { s -> !s.isWarmup } }
        
        val exerciseStats = sets
            .groupBy { it.exerciseId }
            .mapNotNull { (exerciseId, exerciseSets) ->
                val exercise = exerciseRepository.getExerciseById(exerciseId)
                exercise?.let {
                    TopExercise(
                        exerciseId = exerciseId,
                        exerciseName = it.name,
                        totalVolume = exerciseSets.sumOf { s -> s.weight * s.reps },
                        totalSets = exerciseSets.size,
                        frequency = workouts.count { w -> 
                            w.sets.any { s -> s.exerciseId == exerciseId && !s.isWarmup }
                        },
                    )
                }
            }
            .sortedByDescending { it.totalVolume }
            .take(limit)
        
        exerciseStats
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get top exercises: ${e.message}", e)
            throw e
        }
    }

    suspend fun getWorkoutDurationTrend(weeks: Int = 8): List<WorkoutDurationTrend> {
        return try {
            val workouts = workoutDao.getAllCompletedWorkouts()
            val now = LocalDate.now()
        
        return (0 until weeks).map { weekOffset ->
            val weekStart = now.minusWeeks(weekOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
            val weekEnd = weekStart.plusDays(6)
            
            val weekWorkouts = workouts.filter {
                val workoutDate = Instant.ofEpochMilli(it.workout.startedAt)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                workoutDate in weekStart..weekEnd
            }
            
            val avgDuration = if (weekWorkouts.isEmpty()) {
                0.0
            } else {
                weekWorkouts.map { it.workout.durationSeconds / 60.0 }.average()
            }
            
            WorkoutDurationTrend(
                weekStartDate = weekStart,
                averageDurationMinutes = avgDuration,
            )
        }.reversed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout duration trend: ${e.message}", e)
            throw e
        }
    }

    suspend fun getWeeklySummary(): WeeklySummary {
        return try {
            val now = LocalDate.now()
            val thisWeekStart = now.with(java.time.DayOfWeek.MONDAY)
            val lastWeekStart = thisWeekStart.minusWeeks(1)
        
        val workouts = workoutDao.getAllCompletedWorkouts()
        
        val thisWeekWorkouts = workouts.filter {
            val date = Instant.ofEpochMilli(it.workout.startedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            date >= thisWeekStart
        }
        
        val lastWeekWorkouts = workouts.filter {
            val date = Instant.ofEpochMilli(it.workout.startedAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            date >= lastWeekStart && date < thisWeekStart
        }
        
        val thisWeekVolume = thisWeekWorkouts
            .flatMap { it.sets.filter { s -> !s.isWarmup } }
            .sumOf { it.weight * it.reps }
        
        val lastWeekVolume = lastWeekWorkouts
            .flatMap { it.sets.filter { s -> !s.isWarmup } }
            .sumOf { it.weight * it.reps }
        
        val exerciseIds = prService.getExerciseIdsWithHistory()
        val allPRs = exerciseIds.flatMap { id ->
            val exercise = exerciseRepository.getExerciseById(id)
            if (exercise != null) {
                prService.getPersonalRecords(id, exercise.name)
            } else emptyList()
        }
        
        val thisWeekPRs = allPRs.count { pr ->
            val prDate = pr.date.atZone(ZoneId.systemDefault()).toLocalDate()
            prDate >= thisWeekStart
        }
        
        val volumeChange = if (lastWeekVolume > 0) {
            ((thisWeekVolume - lastWeekVolume) / lastWeekVolume) * 100
        } else {
            0.0
        }
        
        return WeeklySummary(
            thisWeekVolume = thisWeekVolume,
            lastWeekVolume = lastWeekVolume,
            thisWeekWorkouts = thisWeekWorkouts.size,
            lastWeekWorkouts = lastWeekWorkouts.size,
            thisWeekPRs = thisWeekPRs,
            volumeChange = volumeChange,
        )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get weekly summary: ${e.message}", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AnalyticsService"
    }
}
