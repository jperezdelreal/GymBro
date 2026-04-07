package com.gymbro.core

import com.gymbro.core.model.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object TestFixtures {
    
    // Sample Exercises
    val benchPress = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        name = "Bench Press",
        muscleGroup = MuscleGroup.CHEST,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Classic barbell bench press",
        youtubeUrl = "https://youtube.com/example1"
    )

    val squat = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        name = "Back Squat",
        muscleGroup = MuscleGroup.QUADRICEPS,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Barbell back squat",
        youtubeUrl = "https://youtube.com/example2"
    )

    val deadlift = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000003"),
        name = "Deadlift",
        muscleGroup = MuscleGroup.BACK,
        category = ExerciseCategory.COMPOUND,
        equipment = Equipment.BARBELL,
        description = "Conventional deadlift"
    )

    val bicepCurl = Exercise(
        id = UUID.fromString("00000000-0000-0000-0000-000000000004"),
        name = "Bicep Curl",
        muscleGroup = MuscleGroup.BICEPS,
        category = ExerciseCategory.ISOLATION,
        equipment = Equipment.DUMBBELL,
        description = "Dumbbell bicep curl"
    )

    val exercises = listOf(benchPress, squat, deadlift, bicepCurl)

    // Sample Exercise Sets
    val benchPressSet1 = ExerciseSet(
        id = UUID.fromString("10000000-0000-0000-0000-000000000001"),
        exerciseId = benchPress.id,
        weightKg = 100.0,
        reps = 5,
        rpe = 8.0,
        isWarmup = false,
        completedAt = Instant.now().minus(2, ChronoUnit.HOURS)
    )

    val benchPressSet2 = ExerciseSet(
        id = UUID.fromString("10000000-0000-0000-0000-000000000002"),
        exerciseId = benchPress.id,
        weightKg = 100.0,
        reps = 5,
        rpe = 8.5,
        isWarmup = false,
        completedAt = Instant.now().minus(1, ChronoUnit.HOURS)
    )

    val squatWarmup = ExerciseSet(
        id = UUID.fromString("10000000-0000-0000-0000-000000000003"),
        exerciseId = squat.id,
        weightKg = 60.0,
        reps = 10,
        rpe = 5.0,
        isWarmup = true,
        completedAt = Instant.now().minus(3, ChronoUnit.HOURS)
    )

    val squatWorkSet = ExerciseSet(
        id = UUID.fromString("10000000-0000-0000-0000-000000000004"),
        exerciseId = squat.id,
        weightKg = 140.0,
        reps = 3,
        rpe = 9.0,
        isWarmup = false,
        completedAt = Instant.now().minus(30, ChronoUnit.MINUTES)
    )

    // Sample Workouts
    val activeWorkout = Workout(
        id = UUID.fromString("20000000-0000-0000-0000-000000000001"),
        name = "Push Day",
        startedAt = Instant.now().minus(2, ChronoUnit.HOURS),
        completedAt = null,
        sets = listOf(benchPressSet1, benchPressSet2),
        notes = ""
    )

    val completedWorkout = Workout(
        id = UUID.fromString("20000000-0000-0000-0000-000000000002"),
        name = "Leg Day",
        startedAt = Instant.now().minus(48, ChronoUnit.HOURS),
        completedAt = Instant.now().minus(47, ChronoUnit.HOURS),
        sets = listOf(squatWarmup, squatWorkSet),
        notes = "Felt strong today"
    )

    val emptyWorkout = Workout(
        id = UUID.fromString("20000000-0000-0000-0000-000000000003"),
        name = "New Workout",
        startedAt = Instant.now(),
        completedAt = null,
        sets = emptyList(),
        notes = ""
    )

    // Sample Personal Records
    val benchPressMaxWeight = PersonalRecord(
        exerciseId = benchPress.id.toString(),
        exerciseName = benchPress.name,
        type = RecordType.MAX_WEIGHT,
        value = 120.0,
        date = Instant.now().minus(7, ChronoUnit.DAYS),
        previousValue = 115.0
    )

    val squatMaxE1RM = PersonalRecord(
        exerciseId = squat.id.toString(),
        exerciseName = squat.name,
        type = RecordType.MAX_E1RM,
        value = 180.0,
        date = Instant.now().minus(3, ChronoUnit.DAYS),
        previousValue = 175.0
    )

    // Sample E1RM Data Points
    val e1rmDataPoints = listOf(
        E1RMDataPoint(
            date = Instant.now().minus(30, ChronoUnit.DAYS),
            e1rm = 150.0,
            weight = 135.0,
            reps = 5
        ),
        E1RMDataPoint(
            date = Instant.now().minus(14, ChronoUnit.DAYS),
            e1rm = 165.0,
            weight = 140.0,
            reps = 6
        ),
        E1RMDataPoint(
            date = Instant.now().minus(7, ChronoUnit.DAYS),
            e1rm = 175.0,
            weight = 150.0,
            reps = 5
        ),
        E1RMDataPoint(
            date = Instant.now().minus(1, ChronoUnit.DAYS),
            e1rm = 180.0,
            weight = 155.0,
            reps = 5
        )
    )

    // Sample Recovery Metrics
    val goodRecovery = RecoveryMetrics(
        sleepHours = 8.5,
        restingHR = 55.0,
        hrv = 65.0,
        steps = 10000,
        daysSinceLastWorkout = 1,
        readinessScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 8.5,
            hrv = 65.0,
            daysSinceLastWorkout = 1
        )
    )

    val poorRecovery = RecoveryMetrics(
        sleepHours = 5.0,
        restingHR = 70.0,
        hrv = 30.0,
        steps = 3000,
        daysSinceLastWorkout = 0,
        readinessScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 5.0,
            hrv = 30.0,
            daysSinceLastWorkout = 0
        )
    )

    val unknownRecovery = RecoveryMetrics(
        sleepHours = 7.0,
        restingHR = null,
        hrv = null,
        steps = 5000,
        daysSinceLastWorkout = null,
        readinessScore = RecoveryMetrics.calculateReadiness(
            sleepHours = 7.0,
            hrv = null,
            daysSinceLastWorkout = null
        )
    )

    // Sample Workout History Items
    val workoutHistoryItems = listOf(
        WorkoutHistoryItem(
            workoutId = completedWorkout.id.toString(),
            date = completedWorkout.startedAt,
            exerciseCount = 1,
            totalVolume = 1680.0, // (60*10) + (140*3*3) = 600 + 1260
            durationSeconds = 3600,
            exerciseNames = listOf("Back Squat"),
            prCount = 1
        ),
        WorkoutHistoryItem(
            workoutId = "20000000-0000-0000-0000-000000000004",
            date = Instant.now().minus(96, ChronoUnit.HOURS),
            exerciseCount = 2,
            totalVolume = 2500.0,
            durationSeconds = 4500,
            exerciseNames = listOf("Bench Press", "Bicep Curl"),
            prCount = 0
        )
    )
}
