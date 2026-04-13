package com.gymbro.core.service

import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.TrainingSplit
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.ExerciseRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WorkoutPlanGenerator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    suspend fun generatePlan(
        goal: UserPreferences.TrainingGoal,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        trainingPhase: UserPreferences.TrainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
        sessionDurationMinutes: Int = 60,
    ): WorkoutPlan {
        // Seed is now synchronous — exercises are guaranteed to be available
        val allExercises = exerciseRepository.getAllExercises().first()
        val split = TrainingSplit.selectOptimalSplit(daysPerWeek, goal)
        val volumeMultiplier = when (trainingPhase) {
            UserPreferences.TrainingPhase.BULK -> 1.2f
            UserPreferences.TrainingPhase.CUT -> 0.8f
            UserPreferences.TrainingPhase.MAINTENANCE -> 1.0f
        }

        return when (goal) {
            UserPreferences.TrainingGoal.STRENGTH -> generateStrengthPlan(
                allExercises, experienceLevel, daysPerWeek, split, volumeMultiplier, sessionDurationMinutes
            )
            UserPreferences.TrainingGoal.HYPERTROPHY -> generateHypertrophyPlan(
                allExercises, experienceLevel, daysPerWeek, split, volumeMultiplier, sessionDurationMinutes
            )
            UserPreferences.TrainingGoal.POWERLIFTING -> generatePowerliftingPlan(
                allExercises, experienceLevel, daysPerWeek, split, volumeMultiplier, sessionDurationMinutes
            )
            UserPreferences.TrainingGoal.GENERAL_FITNESS -> generateGeneralFitnessPlan(
                allExercises, experienceLevel, daysPerWeek, split, volumeMultiplier, sessionDurationMinutes
            )
        }
    }

    private fun getMaxExercisesForDuration(durationMinutes: Int): Int {
        return when (durationMinutes) {
            in 0..20 -> 3
            in 21..30 -> 4
            in 31..45 -> 5
            in 46..60 -> 6
            in 61..75 -> 7
            in 76..90 -> 8
            in 91..105 -> 9
            else -> 10  // 106-120+
        }
    }

    private fun getBaseSetsForDuration(durationMinutes: Int, goalBaseSets: Int): Int {
        val multiplier = when (durationMinutes) {
            in 0..20 -> 0.6f    // 2-3 sets for very short sessions
            in 21..30 -> 0.75f  // 3 sets for 30min
            in 31..45 -> 0.85f  // Slightly reduced for 45min
            in 46..60 -> 1.0f   // Keep base sets for 60min (default)
            in 61..90 -> 1.15f  // Slight bump for 90min
            else -> 1.3f        // More sets for 120min
        }
        return maxOf(2, (goalBaseSets * multiplier).roundToInt())
    }

    private fun generateStrengthPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        split: TrainingSplit,
        volumeMultiplier: Float,
        sessionDurationMinutes: Int,
    ): WorkoutPlan {
        val baseSets = getBaseSetsForDuration(sessionDurationMinutes, 5)
        val adjSets = applyVolumeMultiplier(baseSets, volumeMultiplier)
        val maxExercises = getMaxExercisesForDuration(sessionDurationMinutes)
        val workoutDays = when (split) {
            TrainingSplit.FULL_BODY -> generateFullBodyDays(exercises, sets = adjSets, reps = "5", rest = 180, maxExercises = maxExercises)
            TrainingSplit.UPPER_LOWER -> generateUpperLowerDays(exercises, sets = adjSets, reps = "5", rest = 180, maxExercises = maxExercises)
            else -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Upper Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = adjSets, reps = "5", rest = 180, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Lower Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = adjSets, reps = "5", rest = 180, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Full Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS),
                            sets = adjSets, reps = "5", rest = 180, maxExercises = maxExercises,
                        ),
                    ),
                )
            }
        }

        return WorkoutPlan(
            name = "Strength Building Program",
            description = "Focus on progressive overload with compound movements. 5x5 protocol for major lifts. Using ${split.displayName} split for $daysPerWeek days/week.",
            goal = UserPreferences.TrainingGoal.STRENGTH,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
            split = split,
        )
    }

    private fun generateHypertrophyPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        split: TrainingSplit,
        volumeMultiplier: Float,
        sessionDurationMinutes: Int,
    ): WorkoutPlan {
        val baseSets = getBaseSetsForDuration(sessionDurationMinutes, 4)
        val adjSets = applyVolumeMultiplier(baseSets, volumeMultiplier)
        val maxExercises = getMaxExercisesForDuration(sessionDurationMinutes)
        val workoutDays = when (split) {
            TrainingSplit.FULL_BODY -> generateFullBodyDays(exercises, sets = adjSets, reps = "8-12", rest = 90, maxExercises = maxExercises)
            TrainingSplit.UPPER_LOWER -> generateUpperLowerDays(exercises, sets = adjSets, reps = "8-12", rest = 90, maxExercises = maxExercises)
            TrainingSplit.PPL -> generatePPLDays(exercises, sets = adjSets, reps = "8-12", rest = 90, maxExercises = maxExercises)
            TrainingSplit.PPLUL -> generatePPLULDays(exercises, volumeMultiplier, maxExercises, sessionDurationMinutes)
            else -> generatePPLDays(exercises, sets = adjSets, reps = "8-12", rest = 90, maxExercises = maxExercises)
        }

        return WorkoutPlan(
            name = "Hypertrophy Program",
            description = "${split.displayName} split focused on muscle growth. Higher volume, moderate intensity for $daysPerWeek days/week.",
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
            split = split,
        )
    }

    private fun generatePowerliftingPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        split: TrainingSplit,
        volumeMultiplier: Float,
        sessionDurationMinutes: Int,
    ): WorkoutPlan {
        val baseSets = getBaseSetsForDuration(sessionDurationMinutes, 5)
        val adjSets = applyVolumeMultiplier(baseSets, volumeMultiplier)
        val maxExercises = getMaxExercisesForDuration(sessionDurationMinutes)
        val workoutDays = when (split) {
            TrainingSplit.POWERLIFTING_3DAY -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Squat Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Bench Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Deadlift Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                )
            }
            TrainingSplit.UPPER_LOWER -> {
                val accessorySets = applyVolumeMultiplier(3, volumeMultiplier)
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Upper Power",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Lower Power",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Upper Accessories",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = accessorySets, reps = "6-8", rest = 120, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = "Lower Accessories",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                            sets = accessorySets, reps = "6-8", rest = 120, maxExercises = maxExercises,
                        ),
                    ),
                )
            }
            else -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Squat Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Bench Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = adjSets, reps = "3-5", rest = 240, maxExercises = maxExercises,
                        ),
                    ),
                )
            }
        }

        return WorkoutPlan(
            name = "Powerlifting Program",
            description = "Focus on the big three: squat, bench press, and deadlift. Low reps, high intensity using ${split.displayName} split.",
            goal = UserPreferences.TrainingGoal.POWERLIFTING,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
            split = split,
        )
    }

    private fun generateGeneralFitnessPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        split: TrainingSplit,
        volumeMultiplier: Float,
        sessionDurationMinutes: Int,
    ): WorkoutPlan {
        val baseSets = getBaseSetsForDuration(sessionDurationMinutes, 3)
        val adjSets = applyVolumeMultiplier(baseSets, volumeMultiplier)
        val maxExercises = getMaxExercisesForDuration(sessionDurationMinutes)
        val workoutDays = generateFullBodyDays(exercises, sets = adjSets, reps = "10-12", rest = 90, maxExercises = maxExercises)

        return WorkoutPlan(
            name = "General Fitness Program",
            description = "Balanced full-body workouts for overall fitness and health. ${split.displayName} approach for $daysPerWeek days/week.",
            goal = UserPreferences.TrainingGoal.GENERAL_FITNESS,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
            split = split,
        )
    }

    private fun buildExerciseList(
        allExercises: List<Exercise>,
        targetMuscles: List<MuscleGroup>,
        sets: Int,
        reps: String,
        rest: Int,
        maxExercises: Int = 6,
    ): List<PlannedExercise> {
        val result = mutableListOf<PlannedExercise>()
        val usedNames = mutableSetOf<String>()

        // Phase 1: Add a compound for each target muscle
        for (muscle in targetMuscles) {
            if (result.size >= maxExercises) break
            val compound = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.COMPOUND && it.name !in usedNames
            } ?: continue
            usedNames.add(compound.name)
            result.add(
                PlannedExercise(
                    exerciseName = compound.name,
                    sets = sets,
                    repsRange = reps,
                    restSeconds = rest,
                )
            )
        }

        // Phase 2: Add isolations for each target muscle
        for (muscle in targetMuscles) {
            if (result.size >= maxExercises) break
            val isolation = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.ISOLATION && it.name !in usedNames
            } ?: continue
            usedNames.add(isolation.name)
            result.add(
                PlannedExercise(
                    exerciseName = isolation.name,
                    sets = maxOf(sets - 1, 2),
                    repsRange = if (reps.contains("-")) reps else "10-12",
                    restSeconds = maxOf(rest - 30, 45),
                )
            )
        }

        // Phase 3: Fill remaining slots with accessory exercises (longer sessions)
        if (result.size < maxExercises) {
            for (muscle in targetMuscles) {
                if (result.size >= maxExercises) break
                val accessory = allExercises.firstOrNull {
                    it.muscleGroup == muscle && it.category == ExerciseCategory.ACCESSORY && it.name !in usedNames
                } ?: continue
                usedNames.add(accessory.name)
                result.add(
                    PlannedExercise(
                        exerciseName = accessory.name,
                        sets = maxOf(sets - 1, 2),
                        repsRange = "12-15",
                        restSeconds = maxOf(rest - 45, 30),
                    )
                )
            }
        }

        // Phase 4: Still short? Add second compound variants
        if (result.size < maxExercises) {
            for (muscle in targetMuscles) {
                if (result.size >= maxExercises) break
                val extra = allExercises.firstOrNull {
                    it.muscleGroup == muscle && it.category == ExerciseCategory.COMPOUND && it.name !in usedNames
                } ?: continue
                usedNames.add(extra.name)
                result.add(
                    PlannedExercise(
                        exerciseName = extra.name,
                        sets = maxOf(sets - 1, 2),
                        repsRange = if (reps.contains("-")) reps else "8-10",
                        restSeconds = rest,
                    )
                )
            }
        }

        return result.take(maxExercises)
    }

    private fun generateFullBodyDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        maxExercises: Int = 6,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Full Body A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.CORE),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Full Body B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Full Body C",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.BICEPS),
                    sets, reps, rest, maxExercises,
                ),
            ),
        )
    }

    private fun generateUpperLowerDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        maxExercises: Int = 6,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Upper A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Lower A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Upper B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Lower B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                    sets, reps, rest, maxExercises,
                ),
            ),
        )
    }

    private fun generatePPLDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        maxExercises: Int = 6,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Legs A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Push B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets - 1, reps, rest - 30, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = "Pull B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets - 1, reps, rest - 30, maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 6,
                name = "Legs B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets - 1, reps, rest - 30, maxExercises,
                ),
            ),
        )
    }

    private fun generatePPLULDays(exercises: List<Exercise>, volumeMultiplier: Float, maxExercises: Int = 6, sessionDurationMinutes: Int = 60): List<WorkoutDay> {
        val mainBaseSets = getBaseSetsForDuration(sessionDurationMinutes, 4)
        val secondaryBaseSets = getBaseSetsForDuration(sessionDurationMinutes, 3)
        val mainSets = applyVolumeMultiplier(mainBaseSets, volumeMultiplier)
        val secondarySets = applyVolumeMultiplier(secondaryBaseSets, volumeMultiplier)
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets = mainSets, reps = "8-12", rest = 90, maxExercises = maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets = mainSets, reps = "8-12", rest = 90, maxExercises = maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Legs",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                    sets = mainSets, reps = "8-12", rest = 90, maxExercises = maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Upper",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                    sets = secondarySets, reps = "12-15", rest = 60, maxExercises = maxExercises,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = "Lower",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
                    sets = secondarySets, reps = "12-15", rest = 60, maxExercises = maxExercises,
                ),
            ),
        )
    }

    private fun applyVolumeMultiplier(baseSets: Int, multiplier: Float): Int {
        return maxOf(1, (baseSets * multiplier).roundToInt())
    }

    /**
     * Adjusts a workout day for a new session duration.
     * Re-scales exercise count and set volume based on available time.
     */
    suspend fun adjustDayForDuration(
        day: WorkoutDay,
        newDurationMinutes: Int,
        trainingPhase: UserPreferences.TrainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
    ): WorkoutDay {
        val volumeMultiplier = when (trainingPhase) {
            UserPreferences.TrainingPhase.BULK -> 1.2f
            UserPreferences.TrainingPhase.CUT -> 0.8f
            UserPreferences.TrainingPhase.MAINTENANCE -> 1.0f
        }
        val maxExercises = getMaxExercisesForDuration(newDurationMinutes)
        val baseSets = getBaseSetsForDuration(newDurationMinutes, 4)
        val adjSets = applyVolumeMultiplier(baseSets, volumeMultiplier)

        val allExercises = exerciseRepository.getAllExercises().first()

        val adjusted = if (day.exercises.size > maxExercises) {
            day.exercises.take(maxExercises).map { ex ->
                ex.copy(sets = adjSets)
            }
        } else if (day.exercises.size < maxExercises) {
            val existing = day.exercises.map { ex -> ex.copy(sets = adjSets) }
            val usedNames = existing.map { it.exerciseName }.toMutableSet()
            val extras = mutableListOf<PlannedExercise>()
            val slotsToFill = maxExercises - existing.size

            val candidates = allExercises.filter { it.name !in usedNames }
            val accessories = candidates.filter {
                it.category == ExerciseCategory.ACCESSORY || it.category == ExerciseCategory.ISOLATION
            }
            val fallback = candidates.filter { it.category == ExerciseCategory.COMPOUND }

            for (ex in (accessories + fallback)) {
                if (extras.size >= slotsToFill) break
                if (ex.name in usedNames) continue
                usedNames.add(ex.name)
                extras.add(
                    PlannedExercise(
                        exerciseName = ex.name,
                        sets = maxOf(adjSets - 1, 2),
                        repsRange = "10-12",
                        restSeconds = 60,
                    )
                )
            }
            existing + extras
        } else {
            day.exercises.map { ex -> ex.copy(sets = adjSets) }
        }

        return day.copy(exercises = adjusted)
    }
}