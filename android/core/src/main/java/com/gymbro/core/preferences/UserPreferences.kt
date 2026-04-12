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
        val TRAINING_GOAL = stringPreferencesKey("training_goal")
        val EXPERIENCE_LEVEL = stringPreferencesKey("experience_level")
        val TRAINING_DAYS_PER_WEEK = intPreferencesKey("training_days_per_week")
        val TRAINING_PHASE = stringPreferencesKey("training_phase")
        val MANUAL_SLEEP_HOURS = intPreferencesKey("manual_sleep_hours")
        val MANUAL_READINESS_SCORE = intPreferencesKey("manual_readiness_score")
        val MANUAL_SLEEP_QUALITY = intPreferencesKey("manual_sleep_quality")
        val MANUAL_MUSCLE_SORENESS = intPreferencesKey("manual_muscle_soreness")
        val MANUAL_ENERGY_LEVEL = intPreferencesKey("manual_energy_level")
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    }

    enum class WeightUnit {
        KG, LBS
    }

    enum class TrainingGoal {
        STRENGTH, POWERLIFTING, HYPERTROPHY, GENERAL_FITNESS
    }

    enum class ExperienceLevel {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    enum class TrainingPhase {
        BULK, CUT, MAINTENANCE
    }

    enum class ThemePreference {
        DARK, LIGHT, SYSTEM
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

    val trainingGoal: Flow<TrainingGoal> = dataStore.data.map { preferences ->
        when (preferences[TRAINING_GOAL]) {
            "STRENGTH" -> TrainingGoal.STRENGTH
            "POWERLIFTING" -> TrainingGoal.POWERLIFTING
            "HYPERTROPHY" -> TrainingGoal.HYPERTROPHY
            "GENERAL_FITNESS" -> TrainingGoal.GENERAL_FITNESS
            else -> TrainingGoal.HYPERTROPHY
        }
    }

    val experienceLevel: Flow<ExperienceLevel> = dataStore.data.map { preferences ->
        when (preferences[EXPERIENCE_LEVEL]) {
            "BEGINNER" -> ExperienceLevel.BEGINNER
            "INTERMEDIATE" -> ExperienceLevel.INTERMEDIATE
            "ADVANCED" -> ExperienceLevel.ADVANCED
            else -> ExperienceLevel.INTERMEDIATE
        }
    }

    val trainingDaysPerWeek: Flow<Int> = dataStore.data.map { preferences ->
        preferences[TRAINING_DAYS_PER_WEEK] ?: 4
    }

    val trainingPhase: Flow<TrainingPhase> = dataStore.data.map { preferences ->
        when (preferences[TRAINING_PHASE]) {
            "BULK" -> TrainingPhase.BULK
            "CUT" -> TrainingPhase.CUT
            "MAINTENANCE" -> TrainingPhase.MAINTENANCE
            else -> TrainingPhase.MAINTENANCE
        }
    }

    val themePreference: Flow<ThemePreference> = dataStore.data.map { preferences ->
        when (preferences[THEME_PREFERENCE]) {
            "DARK" -> ThemePreference.DARK
            "LIGHT" -> ThemePreference.LIGHT
            "SYSTEM" -> ThemePreference.SYSTEM
            else -> ThemePreference.SYSTEM
        }
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

    suspend fun setTrainingGoal(goal: TrainingGoal) {
        dataStore.edit { preferences ->
            preferences[TRAINING_GOAL] = goal.name
        }
    }

    suspend fun setExperienceLevel(level: ExperienceLevel) {
        dataStore.edit { preferences ->
            preferences[EXPERIENCE_LEVEL] = level.name
        }
    }

    suspend fun setTrainingDaysPerWeek(days: Int) {
        dataStore.edit { preferences ->
            preferences[TRAINING_DAYS_PER_WEEK] = days
        }
    }

    suspend fun setTrainingPhase(phase: TrainingPhase) {
        dataStore.edit { preferences ->
            preferences[TRAINING_PHASE] = phase.name
        }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE] = theme.name
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

    val manualSleepHours: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MANUAL_SLEEP_HOURS] ?: 7
    }

    val manualReadinessScore: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MANUAL_READINESS_SCORE] ?: 5
    }

    val manualSleepQuality: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MANUAL_SLEEP_QUALITY] ?: 5
    }

    val manualMuscleSoreness: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MANUAL_MUSCLE_SORENESS] ?: 5
    }

    val manualEnergyLevel: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MANUAL_ENERGY_LEVEL] ?: 5
    }

    suspend fun setManualRecoveryMetrics(sleepHours: Int, readinessScore: Int) {
        dataStore.edit { preferences ->
            preferences[MANUAL_SLEEP_HOURS] = sleepHours
            preferences[MANUAL_READINESS_SCORE] = readinessScore
        }
    }

    @Deprecated("Use setManualRecoveryMetrics with sleepHours and readinessScore")
    suspend fun setManualRecoveryMetrics(sleepQuality: Int, muscleSoreness: Int, energyLevel: Int) {
        dataStore.edit { preferences ->
            preferences[MANUAL_SLEEP_QUALITY] = sleepQuality
            preferences[MANUAL_MUSCLE_SORENESS] = muscleSoreness
            preferences[MANUAL_ENERGY_LEVEL] = energyLevel
        }
    }
}
