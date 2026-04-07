package com.gymbro.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long, // epoch millis
    val completedAt: Long? = null,
    val durationSeconds: Long = 0,
    val notes: String = "",
    val completed: Boolean = false,
)
