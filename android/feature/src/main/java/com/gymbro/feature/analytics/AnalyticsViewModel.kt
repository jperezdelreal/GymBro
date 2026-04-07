package com.gymbro.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.service.AnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsService: AnalyticsService,
) : ViewModel() {

    private val _state = MutableStateFlow(AnalyticsState())
    val state: StateFlow<AnalyticsState> = _state.asStateFlow()

    private val _effects = Channel<AnalyticsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadData()
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.RefreshData -> loadData()
            is AnalyticsEvent.NavigateBack -> {
                viewModelScope.launch {
                    _effects.send(AnalyticsEffect.NavigateBack)
                }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val summary = analyticsService.getWeeklySummary()
            val volumeData = analyticsService.getWeeklyVolumeData(weeks = 8)
            val muscleDistribution = analyticsService.getMuscleGroupDistribution()
            val consistency = analyticsService.getConsistencyMetrics()
            val topExercises = analyticsService.getTopExercises(limit = 10)

            _state.update {
                it.copy(
                    summary = summary,
                    volumeData = volumeData,
                    muscleDistribution = muscleDistribution,
                    consistency = consistency,
                    topExercises = topExercises,
                    isLoading = false,
                )
            }
        }
    }
}
