package com.gymbro.core.model

import java.time.Instant
import java.util.UUID

data class ProgramTemplate(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String,
    val durationWeeks: Int?,
    val frequencyPerWeek: Int,
    val periodizationType: PeriodizationType,
    val targetAudience: ExperienceLevel,
    val primaryGoal: TrainingGoal,
    val expectedOutcome: String,
    val progressionScheme: String,
    val authorCredit: String,
    val days: List<ProgramDay>,
    val createdAt: Instant = Instant.now(),
    val isBuiltIn: Boolean = false,
)

data class ProgramDay(
    val dayNumber: Int,
    val name: String,
    val description: String,
    val weekVariations: List<WeekVariation>,
)

data class WeekVariation(
    val weekNumber: Int,
    val exercises: List<ProgramExercise>,
)

data class ProgramExercise(
    val order: Int,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: String,
    val targetRPE: Double?,
    val notes: String,
)

enum class PeriodizationType {
    LINEAR,
    BLOCK,
    DAILY_UNDULATING,
    ONGOING,
}

enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    ELITE,
}

enum class TrainingGoal {
    STRENGTH,
    HYPERTROPHY,
    POWERLIFTING,
    OLYMPIC_WEIGHTLIFTING,
    GENERAL_FITNESS,
}
