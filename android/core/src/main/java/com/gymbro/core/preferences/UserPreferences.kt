package com.gymbro.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    companion object {
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val DEFAULT_REST_TIMER = intPreferencesKey("default_rest_timer")
        val AUTO_START_REST_TIMER = booleanPreferencesKey("auto_start_rest_timer")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val USER_NAME = stringPreferencesKey("user_name")
    }

    enum class WeightUnit {
        KG, LBS
    }

    val weightUnit: Flow<WeightUnit> = dataStore.data.map { preferences ->
        when (preferences[WEIGHT_UNIT]) {
            "LBS" -> WeightUnit.LBS
            else -> WeightUnit.KG
        }
    }

    val defaultRestTimer: Flow<Int> = dataStore.data.map { preferences ->
        preferences[DEFAULT_REST_TIMER] ?: 90
    }

    val autoStartRestTimer: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AUTO_START_REST_TIMER] ?: true
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: false
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        dataStore.edit { preferences ->
            preferences[WEIGHT_UNIT] = unit.name
        }
    }

    suspend fun setDefaultRestTimer(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_REST_TIMER] = seconds
        }
    }

    suspend fun setAutoStartRestTimer(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_START_REST_TIMER] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setUserName(name: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = name
        }
    }

    suspend fun clearAllData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    fun isTooltipShown(id: String): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("tooltip_shown_$id")] ?: false
    }

    suspend fun markTooltipShown(id: String) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey("tooltip_shown_$id")] = true
        }
    }
}
