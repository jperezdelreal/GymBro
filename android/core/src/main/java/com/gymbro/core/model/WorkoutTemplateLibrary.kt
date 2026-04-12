package com.gymbro.core.model

object WorkoutTemplateLibrary {
    
    enum class TargetAudience {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED
    }
    
    data class CuratedTemplate(
        val name: String,
        val description: String,
        val targetAudience: TargetAudience,
        val daysPerWeek: Int,
        val splitType: String,
        val days: List<TemplateDay>
    )
    
    data class TemplateDay(
        val dayName: String,
        val exercises: List<TemplateExercise>
    )
    
    data class TemplateExercise(
        val name: String,
        val setsAndReps: String
    )
    
    val templates = listOf(
        // 1. 5/3/1 (Wendler)
        CuratedTemplate(
            name = "5/3/1 (Wendler)",
            description = "Jim Wendler's time-tested strength program. Build strength with main lifts using 5/3/1 progression, then finish with volume accessories. Perfect for intermediate to advanced lifters.",
            targetAudience = TargetAudience.INTERMEDIATE,
            daysPerWeek = 4,
            splitType = "Strength",
            days = listOf(
                TemplateDay(
                    dayName = "Squat Day",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x5/3/1"),
                        TemplateExercise("Leg Press", "5x10"),
                        TemplateExercise("Leg Curl", "5x10"),
                        TemplateExercise("Calf Raise", "4x12")
                    )
                ),
                TemplateDay(
                    dayName = "Bench Day",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "3x5/3/1"),
                        TemplateExercise("Incline Barbell Bench Press", "5x10"),
                        TemplateExercise("Tricep Pushdown", "5x15"),
                        TemplateExercise("Lateral Raise", "4x12")
                    )
                ),
                TemplateDay(
                    dayName = "Deadlift Day",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "3x5/3/1"),
                        TemplateExercise("Romanian Deadlift", "5x10"),
                        TemplateExercise("Barbell Row", "5x10"),
                        TemplateExercise("Face Pull", "4x15")
                    )
                ),
                TemplateDay(
                    dayName = "OHP Day",
                    exercises = listOf(
                        TemplateExercise("Barbell Overhead Press", "3x5/3/1"),
                        TemplateExercise("Dumbbell Shoulder Press", "5x10"),
                        TemplateExercise("Barbell Curl", "5x10"),
                        TemplateExercise("Overhead Tricep Extension", "4x12")
                    )
                )
            )
        ),
        
        // 2. PPL (Push/Pull/Legs)
        CuratedTemplate(
            name = "PPL (Push/Pull/Legs)",
            description = "Classic 6-day split separating push, pull, and leg movements. Run twice per week for balanced muscle development and high training frequency.",
            targetAudience = TargetAudience.INTERMEDIATE,
            daysPerWeek = 6,
            splitType = "Hypertrophy",
            days = listOf(
                TemplateDay(
                    dayName = "Push",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Barbell Overhead Press", "3x8-10"),
                        TemplateExercise("Incline Barbell Bench Press", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Tricep Pushdown", "3x12-15"),
                        TemplateExercise("Overhead Tricep Extension", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Pull",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "4x6-8"),
                        TemplateExercise("Pull-Up", "3x8-10"),
                        TemplateExercise("Barbell Row", "3x8-10"),
                        TemplateExercise("Cable Row", "3x10-12"),
                        TemplateExercise("Barbell Curl", "3x10-12"),
                        TemplateExercise("Hammer Curl", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Legs",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "4x8-10"),
                        TemplateExercise("Romanian Deadlift", "3x10-12"),
                        TemplateExercise("Leg Press", "3x10-12"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Leg Extension", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                ),
                TemplateDay(
                    dayName = "Push",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Barbell Overhead Press", "3x8-10"),
                        TemplateExercise("Incline Barbell Bench Press", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Tricep Pushdown", "3x12-15"),
                        TemplateExercise("Overhead Tricep Extension", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Pull",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "4x6-8"),
                        TemplateExercise("Pull-Up", "3x8-10"),
                        TemplateExercise("Barbell Row", "3x8-10"),
                        TemplateExercise("Cable Row", "3x10-12"),
                        TemplateExercise("Barbell Curl", "3x10-12"),
                        TemplateExercise("Hammer Curl", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Legs",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "4x8-10"),
                        TemplateExercise("Romanian Deadlift", "3x10-12"),
                        TemplateExercise("Leg Press", "3x10-12"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Leg Extension", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                )
            )
        ),
        
        // 3. Upper/Lower
        CuratedTemplate(
            name = "Upper/Lower Split",
            description = "4-day program alternating upper and lower body. Ideal balance of frequency and recovery for intermediate lifters chasing strength and size.",
            targetAudience = TargetAudience.INTERMEDIATE,
            daysPerWeek = 4,
            splitType = "Hypertrophy",
            days = listOf(
                TemplateDay(
                    dayName = "Upper A",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Barbell Row", "4x8-10"),
                        TemplateExercise("Barbell Overhead Press", "3x10-12"),
                        TemplateExercise("Pull-Up", "3x8-10"),
                        TemplateExercise("Barbell Curl", "3x10-12"),
                        TemplateExercise("Tricep Pushdown", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Lower A",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "4x8-10"),
                        TemplateExercise("Romanian Deadlift", "3x10-12"),
                        TemplateExercise("Leg Press", "3x12-15"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                ),
                TemplateDay(
                    dayName = "Upper B",
                    exercises = listOf(
                        TemplateExercise("Incline Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Cable Row", "4x10-12"),
                        TemplateExercise("Dumbbell Shoulder Press", "3x10-12"),
                        TemplateExercise("Lat Pulldown", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Overhead Tricep Extension", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Lower B",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "4x6-8"),
                        TemplateExercise("Front Squat", "3x8-10"),
                        TemplateExercise("Lunges", "3x10-12"),
                        TemplateExercise("Leg Extension", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                )
            )
        ),
        
        // 4. Full Body 3x
        CuratedTemplate(
            name = "Full Body 3x",
            description = "Train your entire body three times per week. Perfect for beginners or busy lifters who want maximum results with minimal time commitment.",
            targetAudience = TargetAudience.BEGINNER,
            daysPerWeek = 3,
            splitType = "Full Body",
            days = listOf(
                TemplateDay(
                    dayName = "Day 1",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x8-10"),
                        TemplateExercise("Barbell Bench Press", "3x8-10"),
                        TemplateExercise("Barbell Row", "3x8-10"),
                        TemplateExercise("Barbell Overhead Press", "3x10-12"),
                        TemplateExercise("Barbell Curl", "3x10-12"),
                        TemplateExercise("Calf Raise", "3x15-20")
                    )
                ),
                TemplateDay(
                    dayName = "Day 2",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "3x6-8"),
                        TemplateExercise("Incline Barbell Bench Press", "3x8-10"),
                        TemplateExercise("Pull-Up", "3x8-10"),
                        TemplateExercise("Dumbbell Shoulder Press", "3x10-12"),
                        TemplateExercise("Tricep Pushdown", "3x12-15"),
                        TemplateExercise("Leg Curl", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Day 3",
                    exercises = listOf(
                        TemplateExercise("Front Squat", "3x8-10"),
                        TemplateExercise("Dumbbell Bench Press", "3x10-12"),
                        TemplateExercise("Cable Row", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Hammer Curl", "3x10-12"),
                        TemplateExercise("Leg Extension", "3x12-15")
                    )
                )
            )
        ),
        
        // 5. Bro Split
        CuratedTemplate(
            name = "Bro Split",
            description = "Old school 5-day bodybuilding split. One muscle group per day with high volume. Great for hypertrophy and mind-muscle connection.",
            targetAudience = TargetAudience.INTERMEDIATE,
            daysPerWeek = 5,
            splitType = "Hypertrophy",
            days = listOf(
                TemplateDay(
                    dayName = "Chest",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Incline Barbell Bench Press", "4x8-10"),
                        TemplateExercise("Dumbbell Bench Press", "3x10-12"),
                        TemplateExercise("Cable Fly", "3x12-15"),
                        TemplateExercise("Dip", "3x10-12")
                    )
                ),
                TemplateDay(
                    dayName = "Back",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "4x6-8"),
                        TemplateExercise("Pull-Up", "4x8-10"),
                        TemplateExercise("Barbell Row", "4x8-10"),
                        TemplateExercise("Cable Row", "3x10-12"),
                        TemplateExercise("Lat Pulldown", "3x10-12"),
                        TemplateExercise("Face Pull", "3x15-20")
                    )
                ),
                TemplateDay(
                    dayName = "Shoulders",
                    exercises = listOf(
                        TemplateExercise("Barbell Overhead Press", "4x8-10"),
                        TemplateExercise("Dumbbell Shoulder Press", "4x8-10"),
                        TemplateExercise("Lateral Raise", "4x12-15"),
                        TemplateExercise("Front Raise", "3x12-15"),
                        TemplateExercise("Face Pull", "4x15-20")
                    )
                ),
                TemplateDay(
                    dayName = "Arms",
                    exercises = listOf(
                        TemplateExercise("Barbell Curl", "4x10-12"),
                        TemplateExercise("Hammer Curl", "4x10-12"),
                        TemplateExercise("Preacher Curl", "3x12-15"),
                        TemplateExercise("Close-Grip Bench Press", "4x8-10"),
                        TemplateExercise("Tricep Pushdown", "4x12-15"),
                        TemplateExercise("Overhead Tricep Extension", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Legs",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "4x8-10"),
                        TemplateExercise("Romanian Deadlift", "4x10-12"),
                        TemplateExercise("Leg Press", "4x10-12"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Leg Extension", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                )
            )
        ),
        
        // 6. PHUL
        CuratedTemplate(
            name = "PHUL",
            description = "Power Hypertrophy Upper Lower. 4 days alternating power (strength) and hypertrophy (size) training. Best of both worlds for well-rounded development.",
            targetAudience = TargetAudience.INTERMEDIATE,
            daysPerWeek = 4,
            splitType = "Powerbuilding",
            days = listOf(
                TemplateDay(
                    dayName = "Power Upper",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "4x4-6"),
                        TemplateExercise("Barbell Row", "4x4-6"),
                        TemplateExercise("Barbell Overhead Press", "3x6-8"),
                        TemplateExercise("Pull-Up", "3x6-8"),
                        TemplateExercise("Barbell Curl", "3x6-8")
                    )
                ),
                TemplateDay(
                    dayName = "Power Lower",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "4x4-6"),
                        TemplateExercise("Conventional Deadlift", "3x4-6"),
                        TemplateExercise("Leg Press", "3x8-10"),
                        TemplateExercise("Leg Curl", "3x8-10"),
                        TemplateExercise("Calf Raise", "4x8-10")
                    )
                ),
                TemplateDay(
                    dayName = "Hypertrophy Upper",
                    exercises = listOf(
                        TemplateExercise("Incline Barbell Bench Press", "4x10-12"),
                        TemplateExercise("Cable Row", "4x10-12"),
                        TemplateExercise("Dumbbell Shoulder Press", "3x10-12"),
                        TemplateExercise("Lat Pulldown", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Tricep Pushdown", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Hypertrophy Lower",
                    exercises = listOf(
                        TemplateExercise("Front Squat", "4x10-12"),
                        TemplateExercise("Romanian Deadlift", "4x10-12"),
                        TemplateExercise("Lunges", "3x10-12"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Leg Extension", "3x12-15"),
                        TemplateExercise("Calf Raise", "4x15-20")
                    )
                )
            )
        ),
        
        // 7. Starting Strength
        CuratedTemplate(
            name = "Starting Strength",
            description = "Mark Rippetoe's novice strength program. Simple A/B alternating workouts focused on compound movements. The gold standard for beginners.",
            targetAudience = TargetAudience.BEGINNER,
            daysPerWeek = 3,
            splitType = "Strength",
            days = listOf(
                TemplateDay(
                    dayName = "Workout A",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x5"),
                        TemplateExercise("Barbell Bench Press", "3x5"),
                        TemplateExercise("Conventional Deadlift", "1x5")
                    )
                ),
                TemplateDay(
                    dayName = "Workout B",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x5"),
                        TemplateExercise("Barbell Overhead Press", "3x5"),
                        TemplateExercise("Power Clean", "5x3")
                    )
                ),
                TemplateDay(
                    dayName = "Workout A",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x5"),
                        TemplateExercise("Barbell Bench Press", "3x5"),
                        TemplateExercise("Conventional Deadlift", "1x5")
                    )
                )
            )
        ),
        
        // 8. GZCLP
        CuratedTemplate(
            name = "GZCLP",
            description = "Cody Lefever's beginner linear progression. T1 heavy compounds, T2 lighter compounds, T3 accessories. Structured but flexible for individual needs.",
            targetAudience = TargetAudience.BEGINNER,
            daysPerWeek = 4,
            splitType = "Powerbuilding",
            days = listOf(
                TemplateDay(
                    dayName = "Day 1",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "5x3 (T1)"),
                        TemplateExercise("Barbell Bench Press", "3x10 (T2)"),
                        TemplateExercise("Lat Pulldown", "3x15 (T3)")
                    )
                ),
                TemplateDay(
                    dayName = "Day 2",
                    exercises = listOf(
                        TemplateExercise("Barbell Overhead Press", "5x3 (T1)"),
                        TemplateExercise("Conventional Deadlift", "3x10 (T2)"),
                        TemplateExercise("Dumbbell Row", "3x15 (T3)")
                    )
                ),
                TemplateDay(
                    dayName = "Day 3",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "5x3 (T1)"),
                        TemplateExercise("Barbell Back Squat", "3x10 (T2)"),
                        TemplateExercise("Dumbbell Shoulder Press", "3x15 (T3)")
                    )
                ),
                TemplateDay(
                    dayName = "Day 4",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "5x3 (T1)"),
                        TemplateExercise("Barbell Overhead Press", "3x10 (T2)"),
                        TemplateExercise("Barbell Curl", "3x15 (T3)")
                    )
                )
            )
        ),
        
        // 9. PPL for Beginners
        CuratedTemplate(
            name = "PPL for Beginners",
            description = "Simplified 3-day push/pull/legs split. Lower volume than the 6-day version, perfect for those new to training who want to learn the PPL structure.",
            targetAudience = TargetAudience.BEGINNER,
            daysPerWeek = 3,
            splitType = "Hypertrophy",
            days = listOf(
                TemplateDay(
                    dayName = "Push",
                    exercises = listOf(
                        TemplateExercise("Barbell Bench Press", "3x8-10"),
                        TemplateExercise("Barbell Overhead Press", "3x8-10"),
                        TemplateExercise("Incline Barbell Bench Press", "3x10-12"),
                        TemplateExercise("Lateral Raise", "3x12-15"),
                        TemplateExercise("Tricep Pushdown", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Pull",
                    exercises = listOf(
                        TemplateExercise("Conventional Deadlift", "3x6-8"),
                        TemplateExercise("Barbell Row", "3x8-10"),
                        TemplateExercise("Lat Pulldown", "3x10-12"),
                        TemplateExercise("Face Pull", "3x15-20"),
                        TemplateExercise("Barbell Curl", "3x10-12")
                    )
                ),
                TemplateDay(
                    dayName = "Legs",
                    exercises = listOf(
                        TemplateExercise("Barbell Back Squat", "3x8-10"),
                        TemplateExercise("Romanian Deadlift", "3x10-12"),
                        TemplateExercise("Leg Press", "3x10-12"),
                        TemplateExercise("Leg Curl", "3x12-15"),
                        TemplateExercise("Calf Raise", "3x15-20")
                    )
                )
            )
        ),
        
        // 10. Bodyweight Basics
        CuratedTemplate(
            name = "Bodyweight Basics",
            description = "No equipment needed. Build strength and muscle anywhere with push-ups, pull-ups, squats, and core work. Perfect for home training or traveling.",
            targetAudience = TargetAudience.BEGINNER,
            daysPerWeek = 3,
            splitType = "Full Body",
            days = listOf(
                TemplateDay(
                    dayName = "Day 1",
                    exercises = listOf(
                        TemplateExercise("Push-Up", "4x10-15"),
                        TemplateExercise("Pull-Up", "4x5-10"),
                        TemplateExercise("Goblet Squat", "3x12-15"),
                        TemplateExercise("Dip", "3x8-12"),
                        TemplateExercise("Lunges", "3x10-12")
                    )
                ),
                TemplateDay(
                    dayName = "Day 2",
                    exercises = listOf(
                        TemplateExercise("Push-Up", "4x12-20"),
                        TemplateExercise("Chin-Up", "4x5-10"),
                        TemplateExercise("Bulgarian Split Squat", "3x10-12"),
                        TemplateExercise("Dip", "3x10-15"),
                        TemplateExercise("Good Morning", "3x12-15")
                    )
                ),
                TemplateDay(
                    dayName = "Day 3",
                    exercises = listOf(
                        TemplateExercise("Push-Up", "5x10-15"),
                        TemplateExercise("Pull-Up", "5x5-10"),
                        TemplateExercise("Goblet Squat", "4x12-15"),
                        TemplateExercise("Dip", "4x8-12"),
                        TemplateExercise("Lunges", "3x12-15")
                    )
                )
            )
        )
    )
}
