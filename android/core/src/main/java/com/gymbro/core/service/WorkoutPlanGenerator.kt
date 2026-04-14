package com.gymbro.core.service

import android.content.Context
import com.gymbro.core.R
import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.TrainingSplit
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.ExerciseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WorkoutPlanGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exerciseRepository: ExerciseRepository,
) {

    companion object {
        // Time estimation constants (seconds)
        const val WARMUP_TIME_SECONDS = 300       // 5 min general warmup (skill: 5-10 min)
        const val COOLDOWN_TIME_SECONDS = 180     // 3 min cooldown/stretching

        // Transition time by exercise type — compounds need plate loading, rack
        // setup, safety pins; isolation is just grab dumbbells or adjust a machine
        const val TRANSITION_TIME_COMPOUND = 150  // 2.5 min — load plates, set rack height
        const val TRANSITION_TIME_ISOLATION = 60  // 1 min — grab DBs, adjust seat

        // Rest time per training goal (seconds) — from training domain skill
        // Strength/PL: "3-5 minutes between main lifts (full ATP-PC recovery)"
        const val REST_TIME_STRENGTH = 240        // 4 min — mid-range for heavy compounds
        // Hypertrophy: "60-90 seconds (metabolic stress accumulation)"
        const val REST_TIME_HYPERTROPHY = 90      // 90s — upper end, compounds need it
        // General fitness/toning: "30-60 seconds (keeps heart rate elevated)"
        const val REST_TIME_ENDURANCE = 45         // 45s — mid-range
        // Powerlifting main lifts: near-max singles need full ATP-PC recovery
        const val REST_TIME_POWER = 300            // 5 min — serious PL rest for heavy singles

        // Rep duration (seconds per rep) — accounts for brace, eccentric, grind
        const val REP_DURATION_COMPOUND = 5       // ~5s/rep — brace → controlled eccentric → drive
        const val REP_DURATION_ISOLATION = 3      // ~3s/rep — controlled tempo, mind-muscle connection

        // Exercise count bounds
        const val MIN_EXERCISES = 3
        const val MAX_EXERCISES = 12

        /**
         * Estimates how long a single exercise takes in seconds.
         */
        fun estimateExerciseTimeSeconds(
            category: ExerciseCategory,
            sets: Int,
            repsRange: String = "10",
            restSeconds: Int = REST_TIME_HYPERTROPHY,
        ): Int {
            val midReps = parseRepsRangeMidpoint(repsRange)
            val repDuration = when (category) {
                ExerciseCategory.COMPOUND -> REP_DURATION_COMPOUND
                else -> REP_DURATION_ISOLATION
            }
            val transitionTime = when (category) {
                ExerciseCategory.COMPOUND -> TRANSITION_TIME_COMPOUND
                else -> TRANSITION_TIME_ISOLATION
            }
            val timePerSet = (midReps * repDuration) + restSeconds
            return (sets * timePerSet) + transitionTime
        }

        /**
         * Returns the available work time in seconds after warmup and cooldown.
         */
        fun workTimeBudgetSeconds(sessionDurationMinutes: Int): Int {
            val totalSeconds = sessionDurationMinutes * 60
            return maxOf(0, totalSeconds - WARMUP_TIME_SECONDS - COOLDOWN_TIME_SECONDS)
        }

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
    }

    private fun splitName(split: TrainingSplit): String = context.getString(
        when (split) {
            TrainingSplit.FULL_BODY -> R.string.gen_split_full_body
            TrainingSplit.UPPER_LOWER -> R.string.gen_split_upper_lower
            TrainingSplit.PPL -> R.string.gen_split_ppl
            TrainingSplit.PPLUL -> R.string.gen_split_pplul
            TrainingSplit.POWERLIFTING_3DAY -> R.string.gen_split_powerlifting_3day
        },
    )

    private val antagonistPairs = mapOf(
        MuscleGroup.CHEST to MuscleGroup.BACK,
        MuscleGroup.BACK to MuscleGroup.CHEST,
        MuscleGroup.BICEPS to MuscleGroup.TRICEPS,
        MuscleGroup.TRICEPS to MuscleGroup.BICEPS,
        MuscleGroup.QUADRICEPS to MuscleGroup.HAMSTRINGS,
        MuscleGroup.HAMSTRINGS to MuscleGroup.QUADRICEPS,
    )

    private fun markSupersets(
        exercises: List<PlannedExercise>,
        muscleMap: Map<String, MuscleGroup>,
    ): List<PlannedExercise> {
        if (exercises.size < 2) return exercises
        val result = exercises.toMutableList()
        val used = mutableSetOf<Int>()
        for (i in result.indices) {
            if (i in used) continue
            val muscle = muscleMap[result[i].exerciseName] ?: continue
            val antagonist = antagonistPairs[muscle] ?: continue
            for (j in (i + 1) until result.size) {
                if (j in used) continue
                val otherMuscle = muscleMap[result[j].exerciseName] ?: continue
                if (otherMuscle == antagonist) {
                    val groupId = java.util.UUID.randomUUID().toString()
                    result[i] = result[i].copy(supersetGroupId = groupId)
                    result[j] = result[j].copy(supersetGroupId = groupId)
                    used.add(i)
                    used.add(j)
                    break
                }
            }
        }
        return result
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

        val plan = when (goal) {
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
        // Mark antagonist exercise pairs as supersets
        val muscleMap = allExercises.associate { it.name to it.muscleGroup }
        return plan.copy(
            workoutDays = plan.workoutDays.map { day ->
                day.copy(exercises = markSupersets(day.exercises, muscleMap))
            },
        )
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
                        name = context.getString(R.string.gen_day_upper_body),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = context.getString(R.string.gen_day_lower_body),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = baseSets, reps = "3-5", rest = REST_TIME_STRENGTH,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = context.getString(R.string.gen_day_full_body),
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
            name = context.getString(R.string.gen_plan_strength),
            description = context.getString(R.string.gen_plan_strength_desc, splitName(split), daysPerWeek),
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
            name = context.getString(R.string.gen_plan_hypertrophy),
            description = context.getString(R.string.gen_plan_hypertrophy_desc, splitName(split), daysPerWeek),
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
                        name = context.getString(R.string.gen_day_squat),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = context.getString(R.string.gen_day_bench),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = context.getString(R.string.gen_day_deadlift),
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
                        name = context.getString(R.string.gen_day_upper_power),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = context.getString(R.string.gen_day_lower_power),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = context.getString(R.string.gen_day_upper_accessories),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = accessorySets, reps = "6-8", rest = 120,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = context.getString(R.string.gen_day_lower_accessories),
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
                        name = context.getString(R.string.gen_day_squat),
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                            sets = baseSets, reps = "1-5", rest = REST_TIME_POWER,
                            sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = context.getString(R.string.gen_day_bench),
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
            name = context.getString(R.string.gen_plan_powerlifting),
            description = context.getString(R.string.gen_plan_powerlifting_desc, splitName(split)),
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
        // General fitness / endurance: 3 sets, high reps, short rest → most exercises fit
        // Skill: "12-20 reps, 2-3 sets, moderate intensity"
        val baseSets = 3
        val workoutDays = generateFullBodyDays(exercises, sets = baseSets, reps = "12-20", rest = REST_TIME_ENDURANCE, sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier)

        return WorkoutPlan(
            name = context.getString(R.string.gen_plan_general),
            description = context.getString(R.string.gen_plan_general_desc, splitName(split), daysPerWeek),
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
        // Skill: "60-90s for accessories" even in BULK, proportionally less rest than compounds
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
                restSeconds = maxOf((rest * 0.6f).toInt(), 45),
            ))
        }

        // Phase 3: Accessories — shortest rest, highest reps, least setup
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
                restSeconds = maxOf((rest * 0.45f).toInt(), 30),
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
                name = context.getString(R.string.gen_day_full_body_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.CORE),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = context.getString(R.string.gen_day_full_body_b),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = context.getString(R.string.gen_day_full_body_c),
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
                name = context.getString(R.string.gen_day_upper_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.BICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = context.getString(R.string.gen_day_lower_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = context.getString(R.string.gen_day_upper_b),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = context.getString(R.string.gen_day_lower_b),
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
                name = context.getString(R.string.gen_day_push_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = context.getString(R.string.gen_day_pull_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = context.getString(R.string.gen_day_legs_a),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES),
                    sets, reps, rest, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = context.getString(R.string.gen_day_push_b),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets - 1, reps, rest - 30, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = context.getString(R.string.gen_day_pull_b),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets - 1, reps, rest - 30, sessionDurationMinutes, volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 6,
                name = context.getString(R.string.gen_day_legs_b),
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
                name = context.getString(R.string.gen_day_push),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = context.getString(R.string.gen_day_pull),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = context.getString(R.string.gen_day_legs),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                    sets = mainBaseSets, reps = "8-12", rest = REST_TIME_HYPERTROPHY,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = context.getString(R.string.gen_day_upper),
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                    sets = secondaryBaseSets, reps = "12-15", rest = 60,
                    sessionDurationMinutes = sessionDurationMinutes, volumeMultiplier = volumeMultiplier,
                ),
            ),
            WorkoutDay(
                dayNumber = 5,
                name = context.getString(R.string.gen_day_lower),
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
            UserPreferences.TrainingGoal.GENERAL_FITNESS -> Triple(3, "12-20", REST_TIME_ENDURANCE)
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