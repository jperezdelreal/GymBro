package com.gymbro.core.sync.model

import com.gymbro.core.database.entity.ExerciseEntity
import com.gymbro.core.database.entity.WorkoutEntity
import com.gymbro.core.database.entity.WorkoutSetEntity

/** Convert Room ExerciseEntity → Firestore document map. */
fun ExerciseEntity.toFirestoreExercise(deviceId: String): FirestoreExercise =
    FirestoreExercise(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        category = category,
        equipment = equipment,
        description = description,
        youtubeUrl = youtubeUrl,
        updatedAt = System.currentTimeMillis(),
        deviceId = deviceId,
    )

/** Convert Firestore document → Room ExerciseEntity. */
fun FirestoreExercise.toEntity(): ExerciseEntity =
    ExerciseEntity(
        id = id,
        name = name,
        muscleGroup = muscleGroup,
        category = category,
        equipment = equipment,
        description = description,
        youtubeUrl = youtubeUrl,
    )

/** Convert Room WorkoutEntity + sets → Firestore document. */
fun WorkoutEntity.toFirestoreWorkout(
    sets: List<WorkoutSetEntity>,
    deviceId: String,
): FirestoreWorkout =
    FirestoreWorkout(
        id = id,
        name = "Workout",
        startedAt = startedAt,
        completedAt = completedAt,
        durationSeconds = durationSeconds,
        notes = notes,
        completed = completed,
        sets = sets.map { it.toFirestoreSet() },
        updatedAt = System.currentTimeMillis(),
        deviceId = deviceId,
    )

/** Convert Room WorkoutSetEntity → Firestore embedded set. */
fun WorkoutSetEntity.toFirestoreSet(): FirestoreWorkoutSet =
    FirestoreWorkoutSet(
        id = id,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        rpe = rpe,
        isWarmup = isWarmup,
        completedAt = completedAt,
    )

/** Convert Firestore workout → Room WorkoutEntity. */
fun FirestoreWorkout.toEntity(): WorkoutEntity =
    WorkoutEntity(
        id = id,
        startedAt = startedAt,
        completedAt = completedAt,
        durationSeconds = durationSeconds,
        notes = notes,
        completed = completed,
    )

/** Convert Firestore embedded set → Room WorkoutSetEntity. */
fun FirestoreWorkoutSet.toEntity(workoutId: String): WorkoutSetEntity =
    WorkoutSetEntity(
        id = id,
        workoutId = workoutId,
        exerciseId = exerciseId,
        setNumber = setNumber,
        weight = weight,
        reps = reps,
        rpe = rpe,
        isWarmup = isWarmup,
        completedAt = completedAt,
    )
