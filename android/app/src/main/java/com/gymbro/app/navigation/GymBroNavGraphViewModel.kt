package com.gymbro.app.navigation

import androidx.lifecycle.ViewModel
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.service.WorkoutResultStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GymBroNavGraphViewModel @Inject constructor(
    val userPreferences: UserPreferences,
    val workoutResultStore: WorkoutResultStore,
) : ViewModel()
