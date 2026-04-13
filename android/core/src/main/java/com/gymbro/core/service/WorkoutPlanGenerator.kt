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

    companion object {
        // Time estimation constants (seconds)
        const val WARMUP_TIME_SECONDS = 300       // 5 min warmup
        const val COOLDOWN_TIME_SECONDS = 180     // 3 min cooldown
        const val TRANSITION_TIME_SECONDS = 120   // 2 min between exercises (setup equipment, move)

        // Rest time per training goal (seconds)
        const val REST_TIME_STRENGTH = 180        // 3 min — heavy compounds
        const val REST_TIME_HYPERTROPHY = 90      // 1.5 min — moderate load
        const val REST_TIME_ENDURANCE = 45        // 45s — light, high-rep
        const val REST_TIME_POWER = 240           // 4 min — explosive, full recovery

        // Rep duration (seconds per rep)
        const val REP_DURATION_COMPOUND = 4       // ~4s/rep for compound movements
        const val REP_DURATION_ISOLATION = 3      // ~3s/rep for isolation/accessory

        // Exercise count bounds
        const val MIN_EXERCISES = 3
        const val MAX_EXERCISES = 12
    }

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

    /**
     * Parses a rep range string (e.g. "8-12") and returns the midpoint for time estimation.
     */
    private fun parseRepsRangeMidpoint(repsRange: String): Int {
        val parts = repsRange.split("-")
        return if (parts.size == 2) {
            val low = parts[0].trim().toIntOrNull() ?: 10
            val high = parts[1].trim().toIntOrNull() ?: 12
            (low + high) / 2
        } else {
            repsRange.trim().toIntOrNull() ?: 10
        }
    }

    /**
     * Estimates how long a single exercise takes in seconds, based on:
     *   timePerSet = (reps × repDuration) + restTime
     *   exerciseTime = (sets × timePerSet) + transitionTime
     */
    private fun estimateExerciseTimeSeconds(
        category: ExerciseCategory,
        sets: Int,
        repsRange: String,
        restSeconds: Int,
    ): Int {
        val midReps = parseRepsRangeMidpoint(repsRange)
        val repDuration = when (category) {
            ExerciseCategory.COMPOUND -> REP_DURATION_COMPOUND
            else -> REP_DURATION_ISOLATION
        }
        val timePerSet = (midReps * repDuration) + restSeconds
        return (sets * timePerSet) + TRANSITION_TIME_SECONDS
    }

    /**
     * Returns the available work time in seconds after warmup and cooldown.
     */
    private fun workTimeBudgetSeconds(sessionDurationMinutes: Int): Int {
        val totalSeconds = sessionDurationMinutes * 60
        return maxOf(0, totalSeconds - WARMUP_TIME_SECONDS - COOLDOWN_TIME_SECONDS)
    }

    private fun generateStrengthPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
        split: TrainingSplit,
        volumeMultiplier: Float,
        sessionDurationMinutes: Int,
    ): WorkoutPlan {
        // Strength: 5 sets, heavy compounds, 3-min rest → fewer exercises fit
        val baseSets = 5
        val workoutDays = when (split) {
            TrainingSplit.FULL_BODY -> generateFullBodyDays(exercises, sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
            TrainingSplit.UPPER_LOWER -> generateUpperLowerDays(exercises, sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
            else -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Upper Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Lower Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Full Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS),
                            sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
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
        // Hypertrophy: 4 sets, moderate reps, 90s rest → more exercises fit
        val baseSets = 4
        val workoutDays = when (split) {
            TrainingSplit.FULL_BODY -> generateFullBodyDays(exercises, sets = baseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
            TrainingSplit.UPPER_LOWER -> generateUpperLowerDays(exercises, sets = baseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
            TrainingSplit.PPL -> generatePPLDays(exercises, sets = baseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
            TrainingSplit.PPLUL -> generatePPLULDays(exercises, volumeMultiplier, sessionDurationMinutes)
            else -> generatePPLDays(exercises, sets = baseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)
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
        // Powerlifting: 5 sets, low reps, 4-min rest → fewest exercises
        val baseSets = 5
        val workoutDays = when (split) {
            TrainingSplit.POWERLIFTING_3DAY -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Squat Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Bench Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Deadlift Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                )
            }
            TrainingSplit.UPPER_LOWER -> {
                val accessorySets = 3
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Upper Power",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Lower Power",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Upper Accessories",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = accessorySets, reps = "6-8", rest = 120,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = "Lower Accessories",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                            sets = accessorySets, reps = "6-8", rest = 120,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
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
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Bench Day",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
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
        // General fitness / endurance: 3 sets, higher reps, short rest → most exercises fit
        val baseSets = 3
        val workoutDays = generateFullBodyDays(exercises, sets = baseSets, reps = "10-15", rest = REST_TIME_ENDURANCE, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)

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

    /**
     * Builds an exercise list using bottom-up time estimation.
     * Instead of a rigid exercise count, fills the available session time by
     * estimating each exercise's duration and accumulating until the budget runs out.
     * Priority: compounds → isolations → accessories → extra compound variants.
     * Volume multiplier (BULK/CUT/MAINTENANCE) is applied AFTER exercise selection.
     */
    private fun buildExerciseList(
        allExercises: List<Exercise>,
        targetMuscles: List<MuscleGroup>,
        sets: Int,
        reps: String,
        rest: Int,
        sessionDurationMinutes: Int,
        volumeMultiplier: Float = 1.0f,
    ): List<PlannedExercise> {
        val workBudget = workTimeBudgetSeconds(sessionDurationMinutes)
        val result = mutableListOf<PlannedExercise>()
        val usedNames = mutableSetOf<String>()
        var accumulatedTime = 0

        // Build a prioritized candidate pool (compounds → isolations → accessories → extra compounds)
        data class Candidate(
            val exercise: Exercise,
            val sets: Int,
            val repsRange: String,
            val restSeconds: Int,
        )

        val candidates = mutableListOf<Candidate>()

        // Phase 1: One compound per target muscle
        for (muscle in targetMuscles) {
            val compound = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.COMPOUND && it.name !in usedNames
            } ?: continue
            usedNames.add(compound.name)
            candidates.add(Candidate(compound, sets, reps, rest))
        }

        // Phase 2: One isolation per target muscle
        val isolationNames = mutableSetOf<String>()
        for (muscle in targetMuscles) {
            val isolation = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.ISOLATION && it.name !in usedNames && it.name !in isolationNames
            } ?: continue
            isolationNames.add(isolation.name)
            candidates.add(Candidate(
                isolation,
                sets = maxOf(sets - 1, 2),
                repsRange = if (reps.contains("-")) reps else "10-12",
                restSeconds = maxOf(rest - 30, 45),
            ))
        }

        // Phase 3: Accessories
        val accessoryNames = mutableSetOf<String>()
        for (muscle in targetMuscles) {
            val accessory = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.ACCESSORY
                    && it.name !in usedNames && it.name !in isolationNames && it.name !in accessoryNames
            } ?: continue
            accessoryNames.add(accessory.name)
            candidates.add(Candidate(
                accessory,
                sets = maxOf(sets - 1, 2),
                repsRange = "12-15",
                restSeconds = maxOf(rest - 45, 30),
            ))
        }

        // Phase 4: Extra compound variants
        for (muscle in targetMuscles) {
            val extra = allExercises.firstOrNull {
                it.muscleGroup == muscle && it.category == ExerciseCategory.COMPOUND
                    && it.name !in usedNames && it.name !in isolationNames && it.name !in accessoryNames
            } ?: continue
            candidates.add(Candidate(
                extra,
                sets = maxOf(sets - 1, 2),
                repsRange = if (reps.contains("-")) reps else "8-10",
                restSeconds = rest,
            ))
        }

        // Accumulate exercises within time budget
        for (candidate in candidates) {
            if (result.size >= MAX_EXERCISES) break

            val estimatedTime = estimateExerciseTimeSeconds(
                candidate.exercise.category, candidate.sets, candidate.repsRange, candidate.restSeconds,
            )

            if (accumulatedTime + estimatedTime <= workBudget) {
                result.add(PlannedExercise(
                    exerciseName = candidate.exercise.name,
                    sets = candidate.sets,
                    repsRange = candidate.repsRange,
                    restSeconds = candidate.restSeconds,
                ))
                accumulatedTime += estimatedTime
            } else if (result.size < MIN_EXERCISES) {
                // Under minimum — add with reduced sets to keep session viable
                val reducedSets = maxOf(2, candidate.sets - 1)
                result.add(PlannedExercise(
                    exerciseName = candidate.exercise.name,
                    sets = reducedSets,
                    repsRange = candidate.repsRange,
                    restSeconds = candidate.restSeconds,
                ))
                accumulatedTime += estimateExerciseTimeSeconds(
                    candidate.exercise.category, reducedSets, candidate.repsRange, candidate.restSeconds,
                )
            } else {
                // Over budget — try squeezing in one last exercise with fewer sets
                val reducedSets = maxOf(2, candidate.sets - 1)
                val reducedTime = estimateExerciseTimeSeconds(
                    candidate.exercise.category, reducedSets, candidate.repsRange, candidate.restSeconds,
                )
                if (accumulatedTime + reducedTime <= workBudget) {
                    result.add(PlannedExercise(
                        exerciseName = candidate.exercise.name,
                        sets = reducedSets,
                        repsRange = candidate.repsRange,
                        restSeconds = candidate.restSeconds,
                    ))
                    accumulatedTime += reducedTime
                }
                break // time budget exhausted
            }
        }

        // Apply volume multiplier AFTER base exercise selection
        return result.map { exercise ->
            exercise.copy(sets = maxOf(1, (exercise.sets * volumeMultiplier).roundToInt()))
        }
    }

    private fun generateFullBodyDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        sessionDurationMinutes: Int,
        volumeMultiplier: Float,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Full Body A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.CORE),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Full Body B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Full Body C",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.BICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
        )
    }

    private fun generateUpperLowerDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        sessionDurationMinutes: Int,
        volumeMultiplier: Float,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Upper A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Lower A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Upper B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Lower B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
        )
    }

    private fun generatePPLDays(
        exercises: List<Exercise>,
        sets: Int,
        reps: String,
        rest: Int,
        sessionDurationMinutes: Int,
        volumeMultiplier: Float,
    ): List<WorkoutDay> {
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Legs A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Push B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets - 1, reps, rest - 30, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = "Pull B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets - 1, reps, rest - 30, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 6,
                name = "Legs B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets - 1, reps, rest - 30, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
        )
    }

    private fun generatePPLULDays(exercises: List<Exercise>, volumeMultiplier: Float, sessionDurationMinutes: Int = 60): List<WorkoutDay> {
        val mainBaseSets = 4
        val secondaryBaseSets = 3
        return listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Push",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Pull",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Legs",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Upper",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                    sets = secondaryBaseSets, reps = "12-15", rest = 60,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = "Lower",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
                    sets = secondaryBaseSets, reps = "12-15", rest = 60,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
        )
    }

    /**
     * Adjusts a workout day for a new session duration.
     * Uses time estimation to re-scale exercise count and set volume.
     */
    suspend fun adjustDayForDuration(
        day: WorkoutDay,
        newDurationMinutes: Int,
        trainingPhase: UserPreferences.TrainingPhase = UserPreferences.TrainingPhase.MAINTENANCE,
        goal: UserPreferences.TrainingGoal = UserPreferences.TrainingGoal.HYPERTROPHY,
    ): WorkoutDay {
        val volumeMultiplier = when (trainingPhase) {
            UserPreferences.TrainingPhase.BULK -> 1.2f
            UserPreferences.TrainingPhase.CUT -> 0.8f
            UserPreferences.TrainingPhase.MAINTENANCE -> 1.0f
        }

        // Derive base sets and rest from the goal
        val (baseSets, defaultReps, defaultRest) = when (goal) {
            UserPreferences.TrainingGoal.STRENGTH -> Triple(5, "3-5", REST_TIME_STRENGTH)
            UserPreferences.TrainingGoal.POWERLIFTING -> Triple(5, "1-5", REST_TIME_POWER)
            UserPreferences.TrainingGoal.HYPERTROPHY -> Triple(4, "8-12", REST_TIME_HYPERTROPHY)
            UserPreferences.TrainingGoal.GENERAL_FITNESS -> Triple(3, "10-15", REST_TIME_ENDURANCE)
        }

        val workBudget = workTimeBudgetSeconds(newDurationMinutes)
        val allExercises = exerciseRepository.getAllExercises().first()

        // Re-estimate which existing exercises fit in the new time budget
        val adjusted = mutableListOf<PlannedExercise>()
        var accumulatedTime = 0

        for (ex in day.exercises) {
            if (adjusted.size >= MAX_EXERCISES) break

            val category = allExercises.firstOrNull { it.name == ex.exerciseName }?.category
                ?: ExerciseCategory.COMPOUND
            val exerciseSets = baseSets
            val exerciseReps = ex.repsRange.ifBlank { defaultReps }
            val exerciseRest = if (ex.restSeconds > 0) ex.restSeconds else defaultRest

            val estimatedTime = estimateExerciseTimeSeconds(category, exerciseSets, exerciseReps, exerciseRest)

            if (accumulatedTime + estimatedTime <= workBudget || adjusted.size < MIN_EXERCISES) {
                val finalSets = if (accumulatedTime + estimatedTime > workBudget) {
                    maxOf(2, exerciseSets - 1)
                } else {
                    exerciseSets
                }
                adjusted.add(ex.copy(
                    sets = maxOf(1, (finalSets * volumeMultiplier).roundToInt()),
                    restSeconds = exerciseRest,
                ))
                accumulatedTime += estimateExerciseTimeSeconds(category, finalSets, exerciseReps, exerciseRest)
            } else {
                // Try with reduced sets
                val reducedSets = maxOf(2, exerciseSets - 1)
                val reducedTime = estimateExerciseTimeSeconds(category, reducedSets, exerciseReps, exerciseRest)
                if (accumulatedTime + reducedTime <= workBudget) {
                    adjusted.add(ex.copy(
                        sets = maxOf(1, (reducedSets * volumeMultiplier).roundToInt()),
                        restSeconds = exerciseRest,
                    ))
                    accumulatedTime += reducedTime
                }
                break
            }
        }

        // If new duration is longer and there's time left, add more exercises
        if (adjusted.size < MAX_EXERCISES && accumulatedTime < workBudget) {
            val usedNames = adjusted.map { it.exerciseName }.toMutableSet()
            val candidates = allExercises.filter { it.name !in usedNames }
            val extras = candidates.filter {
                it.category == ExerciseCategory.ACCESSORY || it.category == ExerciseCategory.ISOLATION
            } + candidates.filter { it.category == ExerciseCategory.COMPOUND }

            for (ex in extras) {
                if (adjusted.size >= MAX_EXERCISES) break
                if (ex.name in usedNames) continue

                val extraSets = maxOf(baseSets - 1, 2)
                val estimatedTime = estimateExerciseTimeSeconds(ex.category, extraSets, defaultReps, defaultRest)
                if (accumulatedTime + estimatedTime > workBudget) break

                usedNames.add(ex.name)
                adjusted.add(PlannedExercise(
                    exerciseName = ex.name,
                    sets = maxOf(1, (extraSets * volumeMultiplier).roundToInt()),
                    repsRange = defaultReps,
                    restSeconds = defaultRest,
                ))
                accumulatedTime += estimatedTime
            }
        }

        return day.copy(exercises = adjusted)
    }
}