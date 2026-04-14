package com.gymbro.core.service

import com.gymbro.core.model.Equipment
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.preferences.UserPreferences.ExperienceLevel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Suggests starting weights for first-time users based on exercise type,
 * equipment, and user experience level. Returns null for advanced users
 * (they have history to rely on).
 */
class BeginnerDefaultsService @Inject constructor(
    private val userPreferences: UserPreferences,
) {

    data class BeginnerDefault(
        val suggestedWeightKg: Double,
        val label: String,
    )

    suspend fun getDefault(
        exerciseName: String,
        category: ExerciseCategory,
        equipment: Equipment,
    ): BeginnerDefault? {
        val level = userPreferences.experienceLevel.first()
        return when (level) {
            ExperienceLevel.ADVANCED -> null
            ExperienceLevel.INTERMEDIATE -> getIntermediateDefault(exerciseName, category, equipment)
            ExperienceLevel.BEGINNER -> getBeginnerDefault(exerciseName, category, equipment)
        }
    }

    private fun getBeginnerDefault(
        exerciseName: String,
        category: ExerciseCategory,
        equipment: Equipment,
    ): BeginnerDefault? {
        return when (equipment) {
            Equipment.BODYWEIGHT -> BeginnerDefault(0.0, "0 kg")
            Equipment.BARBELL -> getBeginnerBarbellDefault(exerciseName, category)
            Equipment.DUMBBELL -> getBeginnerDumbbellDefault(category)
            Equipment.MACHINE -> getBeginnerMachineDefault(category)
            Equipment.CABLE -> BeginnerDefault(10.0, "10 kg")
            Equipment.KETTLEBELL -> BeginnerDefault(8.0, "8 kg")
            Equipment.BAND, Equipment.OTHER -> null
        }
    }

    private fun getBeginnerBarbellDefault(
        exerciseName: String,
        category: ExerciseCategory,
    ): BeginnerDefault {
        val nameLower = exerciseName.lowercase()
        return when {
            nameLower.contains("deadlift") || nameLower.contains("peso muerto") ->
                BeginnerDefault(30.0, "30 kg")
            category == ExerciseCategory.COMPOUND ->
                BeginnerDefault(20.0, "20 kg")
            else ->
                BeginnerDefault(20.0, "20 kg")
        }
    }

    private fun getBeginnerDumbbellDefault(category: ExerciseCategory): BeginnerDefault {
        return when (category) {
            ExerciseCategory.COMPOUND -> BeginnerDefault(8.0, "8 kg")
            ExerciseCategory.ISOLATION -> BeginnerDefault(5.0, "5 kg")
            ExerciseCategory.ACCESSORY -> BeginnerDefault(5.0, "5 kg")
            ExerciseCategory.CARDIO -> BeginnerDefault(0.0, "0 kg")
        }
    }

    private fun getBeginnerMachineDefault(category: ExerciseCategory): BeginnerDefault {
        return when (category) {
            ExerciseCategory.COMPOUND -> BeginnerDefault(25.0, "25 kg")
            ExerciseCategory.ISOLATION -> BeginnerDefault(15.0, "15 kg")
            ExerciseCategory.ACCESSORY -> BeginnerDefault(15.0, "15 kg")
            ExerciseCategory.CARDIO -> BeginnerDefault(0.0, "0 kg")
        }
    }

    private fun getIntermediateDefault(
        exerciseName: String,
        category: ExerciseCategory,
        equipment: Equipment,
    ): BeginnerDefault? {
        val beginner = getBeginnerDefault(exerciseName, category, equipment) ?: return null
        if (beginner.suggestedWeightKg == 0.0) return beginner
        val multiplier = when (equipment) {
            Equipment.BARBELL -> 2.5
            Equipment.DUMBBELL -> 2.0
            Equipment.MACHINE -> 2.0
            else -> 2.0
        }
        val weight = beginner.suggestedWeightKg * multiplier
        return BeginnerDefault(weight, "${weight.toInt()} kg")
    }
}
