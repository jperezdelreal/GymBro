package com.gymbro.feature.analytics

import com.gymbro.core.service.ConsistencyMetrics
import com.gymbro.core.service.MuscleGroupDistribution
import com.gymbro.core.service.TopExercise
import com.gymbro.core.service.WeeklySummary
import com.gymbro.core.service.WeeklyVolumeData
import java.time.LocalDate

data class AnalyticsState(
    val summary: WeeklySummary? = null,
    val volumeData: List<WeeklyVolumeData> = emptyList(),
    val muscleDistribution: List<MuscleGroupDistribution> = emptyList(),
    val consistency: ConsistencyMetrics? = null,
    val topExercises: List<TopExercise> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface AnalyticsEvent {
    data object RefreshData : AnalyticsEvent
    data object NavigateBack : AnalyticsEvent
}

sealed interface AnalyticsEffect {
    data object NavigateBack : AnalyticsEffect
}
