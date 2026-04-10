package com.gymbro.core.service

import com.gymbro.core.model.Exercise
import com.gymbro.core.model.ExerciseCategory
import com.gymbro.core.model.MuscleGroup
import com.gymbro.core.model.PlannedExercise
import com.gymbro.core.model.WorkoutDay
import com.gymbro.core.model.WorkoutPlan
import com.gymbro.core.preferences.UserPreferences
import com.gymbro.core.repository.ExerciseRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class WorkoutPlanGenerator @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
) {

    suspend fun generatePlan(
        goal: UserPreferences.TrainingGoal,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
    ): WorkoutPlan {
        val allExercises = exerciseRepository.getAllExercises().first()

        return when (goal) {
            UserPreferences.TrainingGoal.STRENGTH -> generateStrengthPlan(
                allExercises, experienceLevel, daysPerWeek
            )
            UserPreferences.TrainingGoal.HYPERTROPHY -> generateHypertrophyPlan(
                allExercises, experienceLevel, daysPerWeek
            )
            UserPreferences.TrainingGoal.POWERLIFTING -> generatePowerliftingPlan(
                allExercises, experienceLevel, daysPerWeek
            )
            UserPreferences.TrainingGoal.GENERAL_FITNESS -> generateGeneralFitnessPlan(
                allExercises, experienceLevel, daysPerWeek
            )
        }
    }

    private fun generateStrengthPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
    ): WorkoutPlan {
        val workoutDays = when (daysPerWeek) {
            3 -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Upper Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Lower Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Full Body",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                )
            }
            else -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Squat Focus",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Bench Focus",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Deadlift Focus",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = 5,
                            reps = "5",
                            rest = 180,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = "Upper Accessories",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.SHOULDERS, MuscleGroup.BICEPS, MuscleGroup.TRICEPS),
                            sets = 3,
                            reps = "8-10",
                            rest = 90,
                        ),
                    ),
                )
            }
        }

        return WorkoutPlan(
            name = "Strength Building Program",
            description = "Focus on progressive overload with compound movements. 5x5 protocol for major lifts.",
            goal = UserPreferences.TrainingGoal.STRENGTH,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
        )
    }

    private fun generateHypertrophyPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
    ): WorkoutPlan {
        val workoutDays = when (daysPerWeek) {
            3 -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Push",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Pull",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Legs",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.CALVES),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                )
            }
            4 -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Push A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Pull A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Legs A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = "Upper Hypertrophy",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.SHOULDERS),
                            sets = 3,
                            reps = "12-15",
                            rest = 60,
                        ),
                    ),
                )
            }
            else -> {
                listOf(
                    WorkoutDay(
                        dayNumber = 1,
                        name = "Push A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 2,
                        name = "Pull A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 3,
                        name = "Legs A",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                            sets = 4,
                            reps = "8-12",
                            rest = 90,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 4,
                        name = "Push B",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
                            sets = 3,
                            reps = "12-15",
                            rest = 60,
                        ),
                    ),
                    WorkoutDay(
                        dayNumber = 5,
                        name = "Pull B",
                        exercises = buildExerciseList(
                            exercises,
                            listOf(MuscleGroup.BACK, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
                            sets = 3,
                            reps = "12-15",
                            rest = 60,
                        ),
                    ),
                )
            }
        }

        return WorkoutPlan(
            name = "Hypertrophy Program",
            description = "Push/Pull/Legs split focused on muscle growth. Higher volume, moderate intensity.",
            goal = UserPreferences.TrainingGoal.HYPERTROPHY,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
        )
    }

    private fun generatePowerliftingPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
    ): WorkoutPlan {
        val workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Squat Day",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.QUADRICEPS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CORE),
                    sets = 5,
                    reps = "3-5",
                    rest = 240,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Bench Day",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
                    sets = 5,
                    reps = "3-5",
                    rest = 240,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Deadlift Day",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES),
                    sets = 5,
                    reps = "3-5",
                    rest = 240,
                ),
            ),
            WorkoutDay(
                dayNumber = 4,
                name = "Accessories",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.BACK, MuscleGroup.SHOULDERS, MuscleGroup.CORE),
                    sets = 3,
                    reps = "6-8",
                    rest = 120,
                ),
            ),
        )

        return WorkoutPlan(
            name = "Powerlifting Program",
            description = "Focus on the big three: squat, bench press, and deadlift. Low reps, high intensity.",
            goal = UserPreferences.TrainingGoal.POWERLIFTING,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
        )
    }

    private fun generateGeneralFitnessPlan(
        exercises: List<Exercise>,
        experienceLevel: UserPreferences.ExperienceLevel,
        daysPerWeek: Int,
    ): WorkoutPlan {
        val workoutDays = listOf(
            WorkoutDay(
                dayNumber = 1,
                name = "Full Body A",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.CORE),
                    sets = 3,
                    reps = "10-12",
                    rest = 90,
                ),
            ),
            WorkoutDay(
                dayNumber = 2,
                name = "Full Body B",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.SHOULDERS, MuscleGroup.BACK, MuscleGroup.HAMSTRINGS, MuscleGroup.CORE),
                    sets = 3,
                    reps = "10-12",
                    rest = 90,
                ),
            ),
            WorkoutDay(
                dayNumber = 3,
                name = "Full Body C",
                exercises = buildExerciseList(
                    exercises,
                    listOf(MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.QUADRICEPS, MuscleGroup.BICEPS),
                    sets = 3,
                    reps = "10-12",
                    rest = 90,
                ),
            ),
        )

        return WorkoutPlan(
            name = "General Fitness Program",
            description = "Balanced full-body workouts for overall fitness and health.",
            goal = UserPreferences.TrainingGoal.GENERAL_FITNESS,
            experienceLevel = experienceLevel,
            daysPerWeek = daysPerWeek,
            weeks = 4,
            workoutDays = workoutDays.take(daysPerWeek),
        )
    }

    private fun buildExerciseList(
        allExercises: List<Exercise>,
        targetMuscles: List<MuscleGroup>,
        sets: Int,
        reps: String,
        rest: Int,
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
            if (isolation != null && result.size < 6) {
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

        return result.take(6)
    }
}
