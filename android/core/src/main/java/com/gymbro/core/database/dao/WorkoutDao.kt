package com.gymbro.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val sets: List<WorkoutSetEntity>,
)

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    suspend fun getWorkoutWithSets(workoutId: String): WorkoutWithSets?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId")
    fun observeWorkoutWithSets(workoutId: String): Flow<WorkoutWithSets?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE completed = 1 ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentWorkouts(limit: Int = 20): Flow<List<WorkoutWithSets>>

    @Query("SELECT MAX(weight) FROM workout_sets WHERE exerciseId = :exerciseId AND reps >= :reps")
    suspend fun getBestWeight(exerciseId: String, reps: Int): Double?

    @Query(
        """
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workouts w ON ws.workoutId = w.id
        WHERE ws.exerciseId = :exerciseId AND w.completed = 1 AND ws.isWarmup = 0
        ORDER BY ws.completedAt ASC
        """
    )
    suspend fun getSetsByExercise(exerciseId: String): List<WorkoutSetEntity>

    @Query(
        """
        SELECT DISTINCT ws.exerciseId FROM workout_sets ws
        INNER JOIN workouts w ON ws.workoutId = w.id
        WHERE w.completed = 1 AND ws.isWarmup = 0
        """
    )
    suspend fun getExerciseIdsWithHistory(): List<String>

    @Transaction
    @Query("SELECT * FROM workouts WHERE completed = 1 ORDER BY startedAt DESC")
    suspend fun getAllCompletedWorkouts(): List<WorkoutWithSets>

    @Query("SELECT MAX(completedAt) FROM workouts WHERE completed = 1")
    suspend fun getLastCompletedTimestamp(): Long?

    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    suspend fun getAllWorkoutsOnce(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun getSetsForWorkout(workoutId: String): List<WorkoutSetEntity>
}
