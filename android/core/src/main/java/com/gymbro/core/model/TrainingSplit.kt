package com.gymbro.core.model

enum class TrainingSplit(val displayName: String) {
    FULL_BODY("Full Body"),
    UPPER_LOWER("Upper/Lower"),
    PPL("Push/Pull/Legs"),
    PPLUL("PPL + Upper/Lower"),
    POWERLIFTING_3DAY("Powerlifting 3-Day");

    companion object {
        fun selectOptimalSplit(daysPerWeek: Int, goal: com.gymbro.core.preferences.UserPreferences.TrainingGoal): TrainingSplit {
            return when {
                daysPerWeek <= 2 -> FULL_BODY
                daysPerWeek == 3 -> {
                    when (goal) {
                        com.gymbro.core.preferences.UserPreferences.TrainingGoal.POWERLIFTING -> POWERLIFTING_3DAY
                        else -> FULL_BODY
                    }
                }
                daysPerWeek == 4 -> UPPER_LOWER
                daysPerWeek == 5 -> PPLUL
                daysPerWeek >= 6 -> PPL
                else -> FULL_BODY
            }
        }
    }
}
