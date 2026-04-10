package com.gymbro.core.repository

import com.gymbro.core.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class ProgramTemplateDto(
    val name: String,
    @SerialName("programDescription")
    val description: String,
    val durationWeeks: Int?,
    val frequencyPerWeek: Int,
    val periodizationType: String,
    val targetAudience: String,
    val primaryGoal: String,
    val expectedOutcome: String,
    val progressionScheme: String,
    val authorCredit: String,
    val days: List<ProgramDayDto>,
)

@Serializable
internal data class ProgramDayDto(
    val dayNumber: Int,
    val name: String,
    @SerialName("dayDescription")
    val description: String,
    val weekVariations: List<WeekVariationDto>,
)

@Serializable
internal data class WeekVariationDto(
    val weekNumber: Int,
    val exercises: List<ProgramExerciseDto>,
)

@Serializable
internal data class ProgramExerciseDto(
    val order: Int,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: String,
    val targetRPE: Double?,
    val notes: String,
)

internal fun ProgramTemplateDto.toDomain(): ProgramTemplate {
    return ProgramTemplate(
        id = UUID.randomUUID(),
        name = name,
        description = description,
        durationWeeks = durationWeeks,
        frequencyPerWeek = frequencyPerWeek,
        periodizationType = parsePeriodizationType(periodizationType),
        targetAudience = parseExperienceLevel(targetAudience),
        primaryGoal = parseTrainingGoal(primaryGoal),
        expectedOutcome = expectedOutcome,
        progressionScheme = progressionScheme,
        authorCredit = authorCredit,
        days = days.map { it.toDomain() },
        isBuiltIn = true,
    )
}

internal fun ProgramDayDto.toDomain(): ProgramDay {
    return ProgramDay(
        dayNumber = dayNumber,
        name = name,
        description = description,
        weekVariations = weekVariations.map { it.toDomain() },
    )
}

internal fun WeekVariationDto.toDomain(): WeekVariation {
    return WeekVariation(
        weekNumber = weekNumber,
        exercises = exercises.map { it.toDomain() },
    )
}

internal fun ProgramExerciseDto.toDomain(): ProgramExercise {
    return ProgramExercise(
        order = order,
        exerciseName = exerciseName,
        targetSets = targetSets,
        targetReps = targetReps,
        targetRPE = targetRPE,
        notes = notes,
    )
}

private fun parsePeriodizationType(value: String): PeriodizationType {
    return when (value.uppercase()) {
        "LINEAR" -> PeriodizationType.LINEAR
        "BLOCK" -> PeriodizationType.BLOCK
        "DAILY_UNDULATING" -> PeriodizationType.DAILY_UNDULATING
        "ONGOING" -> PeriodizationType.ONGOING
        else -> PeriodizationType.LINEAR
    }
}

private fun parseExperienceLevel(value: String): ExperienceLevel {
    return when {
        value.contains("BEGINNER", ignoreCase = true) -> ExperienceLevel.BEGINNER
        value.contains("INTERMEDIATE", ignoreCase = true) -> ExperienceLevel.INTERMEDIATE
        value.contains("ADVANCED", ignoreCase = true) -> ExperienceLevel.ADVANCED
        value.contains("ELITE", ignoreCase = true) -> ExperienceLevel.ELITE
        else -> ExperienceLevel.BEGINNER
    }
}

private fun parseTrainingGoal(value: String): TrainingGoal {
    return when (value.uppercase()) {
        "STRENGTH" -> TrainingGoal.STRENGTH
        "HYPERTROPHY" -> TrainingGoal.HYPERTROPHY
        "POWERLIFTING" -> TrainingGoal.POWERLIFTING
        "OLYMPIC_WEIGHTLIFTING" -> TrainingGoal.OLYMPIC_WEIGHTLIFTING
        "GENERAL_FITNESS" -> TrainingGoal.GENERAL_FITNESS
        else -> TrainingGoal.GENERAL_FITNESS
    }
}
