package com.gymbro.core.model

import com.gymbro.core.preferences.UserPreferences
import java.time.Instant
import java.util.UUID

data class WorkoutPlan(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val goal: UserPreferences.TrainingGoal,
    val experienceLevel: UserPreferences.ExperienceLevel,
    val daysPerWeek: Int,
    val weeks: Int = 4,
    val workoutDays: List<WorkoutDay>,
    val createdAt: Instant = Instant.now(),
)

data class WorkoutDay(
    val dayNumber: Int,
    val name: String,
    val exercises: List<PlannedExercise>,
)

data class PlannedExercise(
    val exerciseName: String,
    val sets: Int,
    val repsRange: String,
    val restSeconds: Int = 90,
)
