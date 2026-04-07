package com.gymbro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val muscleGroup: String,
    val category: String = "COMPOUND",
    val equipment: String = "BARBELL",
    val description: String = "",
    val youtubeUrl: String? = null,
)
