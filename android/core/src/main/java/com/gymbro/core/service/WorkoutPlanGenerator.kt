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
            in 0..30 -> 4
            in 31..45 -> 5
            in 46..60 -> 6
            in 61..90 -> 8
            else -> 10  // 120+
        }
    }

    private fun getBaseSetsForDuration(durationMinutes: Int, goalBaseSets: Int): Int {
        val multiplier = when (durationMinutes) {
            in 0..30 -> 0.75f   // 3 sets for 30min
            in 31..45 -> 1.0f   // Keep base sets for 45min
            in 46..60 -> 1.0f   // Keep base sets for 60min (default)
            in 61..90 -> 1.25f  // 4 sets for 90min
            else -> 1.5f        // 4-5 sets for 120min
        }
        return maxOf(3, (goalBaseSets * multiplier).roundToInt())
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
            TrainingSplit.PPLUL -> generatePPLULDays(exercises, volumeMultiplier, maxExercises)
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

        for (muscle in targetMuscles) {
            val muscleExercises = allExercises.filter { it.muscleGroup == muscle }
            
            val compound = muscleExercises.firstOrNull { it.category == ExerciseCategory.COMPOUND }
            if (compound != null) {
                result.add(
                    PlannedExercise(
                        exerciseName = compound.name,
                        sets = sets,
                        repsRange = reps,
                        restSeconds = rest,
                    )
                )
            }
            
            val isolation = muscleExercises.firstOrNull { it.category == ExerciseCategory.ISOLATION }
            if (isolation != null && result.size < maxExercises) {
                result.add(
                    PlannedExercise(
                        exerciseName = isolation.name,
                        sets = maxOf(sets - 1, 3),
                        repsRange = if (reps.contains("-")) reps else "10-12",
                        restSeconds = rest - 30,
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

    private fun generatePPLULDays(exercises: List<Exercise>, volumeMultiplier: Float, maxExercises: Int = 6): List<WorkoutDay> {
        val mainSets = applyVolumeMultiplier(4, volumeMultiplier)
        val secondarySets = applyVolumeMultiplier(3, volumeMultiplier)
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
}