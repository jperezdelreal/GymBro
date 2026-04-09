package com.gymbro.core.service

import com.gymbro.core.model.E1RMDataPoint
import com.gymbro.core.model.PlateauAlert
import com.gymbro.core.model.PlateauType
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class PlateauDetectionService @Inject constructor(
    private val prService: PersonalRecordService,
) {

    suspend fun detectPlateaus(exerciseId: String, exerciseName: String): PlateauAlert? {
        val history = prService.getE1RMHistory(exerciseId)
        if (history.isEmpty()) return null

        val now = Instant.now()
        val fourWeeksAgo = now.minus(28, ChronoUnit.DAYS)
        val sixWeeksAgo = now.minus(42, ChronoUnit.DAYS)

        val recentData = history.filter { it.date.isAfter(sixWeeksAgo) }
        if (recentData.size < 2) return null

        val regressionAlert = detectRegression(recentData, exerciseId, exerciseName, now)
        if (regressionAlert != null) return regressionAlert

        val stagnationAlert = detectStagnation(recentData, exerciseId, exerciseName, now, fourWeeksAgo)
        return stagnationAlert
    }

    private fun detectRegression(
        data: List<E1RMDataPoint>,
        exerciseId: String,
        exerciseName: String,
        now: Instant,
    ): PlateauAlert? {
        val sortedData = data.sortedBy { it.date }
        if (sortedData.size < 2) return null

        val weeklyAverages = groupByWeek(sortedData, now)
        if (weeklyAverages.size < 2) return null

        var consecutiveDecreasingWeeks = 0
        for (i in 1 until weeklyAverages.size) {
            if (weeklyAverages[i] < weeklyAverages[i - 1]) {
                consecutiveDecreasingWeeks++
            } else {
                consecutiveDecreasingWeeks = 0
            }
        }

        if (consecutiveDecreasingWeeks >= 2) {
            val suggestions = listOf(
                "Take a deload week at 60-70% of your normal working weight",
                "Check your recovery: ensure adequate sleep and nutrition",
                "Review your form to ensure you're not compensating with poor technique",
            )
            return PlateauAlert(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                type = PlateauType.REGRESSION,
                weeksDuration = consecutiveDecreasingWeeks,
                suggestion = suggestions.random(),
            )
        }

        return null
    }

    private fun detectStagnation(
        data: List<E1RMDataPoint>,
        exerciseId: String,
        exerciseName: String,
        now: Instant,
        fourWeeksAgo: Instant,
    ): PlateauAlert? {
        val recentData = data.filter { it.date.isAfter(fourWeeksAgo) }
        if (recentData.isEmpty()) return null

        val sortedData = recentData.sortedBy { it.date }
        val earliest = sortedData.first().e1rm
        val latest = sortedData.last().e1rm

        if (earliest <= 0.0) return null

        val percentIncrease = ((latest - earliest) / earliest) * 100.0

        if (percentIncrease < 2.0) {
            val weeksDuration = ChronoUnit.DAYS.between(sortedData.first().date, sortedData.last().date).toInt() / 7
            if (weeksDuration >= 3) {
                val suggestions = listOf(
                    "Try a different rep scheme: if doing 3x8, switch to 5x5 or 4x6",
                    "Incorporate a variation exercise: e.g., paused reps, tempo work, or a similar movement pattern",
                    "Add volume: increase sets by 20-30% for 2-3 weeks",
                    "Take a deload week and return with refreshed intensity",
                )
                return PlateauAlert(
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    type = PlateauType.STAGNATION,
                    weeksDuration = weeksDuration,
                    suggestion = suggestions.random(),
                )
            }
        }

        return null
    }

    private fun groupByWeek(data: List<E1RMDataPoint>, now: Instant): List<Double> {
        val weeklyData = mutableMapOf<Int, MutableList<Double>>()

        data.forEach { point ->
            val weekIndex = ChronoUnit.DAYS.between(point.date, now).toInt() / 7
            weeklyData.getOrPut(weekIndex) { mutableListOf() }.add(point.e1rm)
        }

        return weeklyData.keys.sorted().mapNotNull { week ->
            weeklyData[week]?.average()
        }
    }

    suspend fun detectAllPlateaus(exerciseIds: List<Pair<String, String>>): List<PlateauAlert> {
        return exerciseIds.mapNotNull { (id, name) ->
            detectPlateaus(id, name)
        }
    }
}
