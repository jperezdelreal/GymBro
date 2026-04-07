package com.gymbro.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("templateId"),
        Index("exerciseId"),
    ],
)
data class TemplateExerciseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val templateId: String,
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeightKg: Double? = null,
    val orderIndex: Int = 0,
)
