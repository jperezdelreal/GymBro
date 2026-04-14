package com.gymbro.feature.common

import com.gymbro.core.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TooltipManager @Inject constructor(
    private val userPreferences: UserPreferences,
) {
    suspend fun shouldShow(id: String): Boolean {
        return !userPreferences.isTooltipShown(id).first()
    }

    fun shouldShowFlow(id: String): Flow<Boolean> {
        return userPreferences.isTooltipShown(id)
    }

    suspend fun markShown(id: String) {
        userPreferences.markTooltipShown(id)
    }

    suspend fun resetAll() {
        userPreferences.resetAllTooltips()
    }
}
