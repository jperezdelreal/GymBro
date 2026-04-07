package com.gymbro.app.navigation

import androidx.lifecycle.ViewModel
import com.gymbro.core.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GymBroNavGraphViewModel @Inject constructor(
    val userPreferences: UserPreferences,
) : ViewModel()
