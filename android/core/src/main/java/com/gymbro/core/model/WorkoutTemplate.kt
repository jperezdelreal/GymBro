package com.gymbro.core.model

import java.time.Instant
import java.util.UUID

data class WorkoutTemplate(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val description: String = "",
    val exercises: List<TemplateExercise> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val lastUsedAt: Instant? = null,
    val isBuiltIn: Boolean = false,
)

data class TemplateExercise(
    val id: UUID = UUID.randomUUID(),
    val exerciseId: UUID,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double? = null,
    val order: Int = 0,
)
