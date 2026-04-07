package com.gymbro.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.gymbro.core.database.entity.TemplateExerciseEntity
import com.gymbro.core.database.entity.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

data class WorkoutTemplateWithExercises(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId",
    )
    val exercises: List<TemplateExerciseEntity>,
)

@Dao
interface WorkoutTemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: WorkoutTemplateEntity)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    @Query("DELETE FROM workout_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId")
    suspend fun getTemplateWithExercises(templateId: String): WorkoutTemplateWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY isBuiltIn DESC, lastUsedAt DESC, createdAt DESC")
    fun observeAllTemplates(): Flow<List<WorkoutTemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY isBuiltIn DESC, lastUsedAt DESC, createdAt DESC")
    suspend fun getAllTemplates(): List<WorkoutTemplateWithExercises>

    @Query("UPDATE workout_templates SET lastUsedAt = :timestamp WHERE id = :templateId")
    suspend fun updateLastUsedTimestamp(templateId: String, timestamp: Long)
}
