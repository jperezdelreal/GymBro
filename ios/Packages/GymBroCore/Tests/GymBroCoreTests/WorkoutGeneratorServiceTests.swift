import XCTest
@testable import GymBroCore

final class WorkoutGeneratorServiceTests: XCTestCase {
    var service: WorkoutGeneratorService!

    override func setUp() {
        super.setUp()
        service = WorkoutGeneratorService()
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    // MARK: - Zero History / Graceful Degradation

    func testGenerateWithZeroHistoryProducesBeginnerWorkout() {
        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        XCTAssertFalse(result.isRestDay, "Zero history should not be a rest day")
        XCTAssertFalse(result.exercises.isEmpty, "Should generate exercises even with no history")
        XCTAssertFalse(result.reasoning.isEmpty, "Should include reasoning")
        XCTAssertGreaterThan(result.estimatedDurationMinutes, 0)
    }

    func testGenerateWithNoExercisesReturnsEmptyWorkout() {
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: [],
            timeConstraintMinutes: 60
        )

        XCTAssertTrue(result.exercises.isEmpty, "No exercises available = empty workout")
        XCTAssertFalse(result.isRestDay)
    }

    func testDefaultReadinessUsedWhenNilProvided() {
        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        let readinessReasoning = result.reasoning.first { $0.factor == .readiness }
        XCTAssertNotNil(readinessReasoning, "Should have readiness reasoning")
        XCTAssertTrue(readinessReasoning?.summary.contains("75") ?? false, "Default readiness should be 75")
    }

    // MARK: - Readiness Integration

    func testVeryLowReadinessReturnsRestDay() {
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 30, label: .poor)

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        XCTAssertTrue(result.isRestDay, "Readiness < 40 should recommend rest day")
        XCTAssertTrue(result.exercises.isEmpty, "Rest day should have no exercises")
        XCTAssertEqual(result.estimatedDurationMinutes, 0)
    }

    func testModerateReadinessReducesIntensity() {
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 55, label: .moderate)

        let program = makeProgramWithHeavyDay()

        let result = service.generateWorkout(
            activeProgram: program,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        XCTAssertFalse(result.isRestDay)
        let intensityReasonings = result.reasoning.filter {
            $0.factor == .readiness && $0.impact == .volumeIntensity
        }
        XCTAssertFalse(intensityReasonings.isEmpty, "Should have readiness-based adjustments")
    }

    func testGoodReadinessProceeds() {
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 82, label: .good)

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        XCTAssertFalse(result.isRestDay)
        XCTAssertFalse(result.exercises.isEmpty)
    }

    // MARK: - Muscle Recovery Integration

    func testFatiguedMusclesExcluded() {
        let now = Date()
        // Chest trained 8 hours ago → fatigued
        let recentWorkout = makeWorkout(
            hoursAgo: 8,
            exercises: [("Bench Press", ["Chest"], 100, 8, 4)],
            from: now
        )

        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [recentWorkout],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60,
            currentDate: now
        )

        let hasChestExercise = result.exercises.contains { ex in
            ex.exercise?.muscleGroups.contains(where: { $0.name == "Chest" && $0.isPrimary }) ?? false
        }
        XCTAssertFalse(hasChestExercise, "Fatigued muscles (Chest) should be excluded")

        let recoveryReasoning = result.reasoning.first { $0.factor == .muscleRecovery }
        XCTAssertNotNil(recoveryReasoning, "Should explain muscle recovery exclusion")
    }

    func testMultipleFatiguedMusclesExcluded() {
        let now = Date()
        let recentWorkout = makeWorkout(
            hoursAgo: 6,
            exercises: [
                ("Bench Press", ["Chest"], 100, 8, 4),
                ("Squat", ["Quadriceps"], 140, 6, 4)
            ],
            from: now
        )

        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [recentWorkout],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60,
            currentDate: now
        )

        for exercise in result.exercises {
            let primaryMuscles = exercise.exercise?.muscleGroups.filter(\.isPrimary).map(\.name) ?? []
            XCTAssertFalse(primaryMuscles.contains("Chest"), "Chest should be excluded")
            XCTAssertFalse(primaryMuscles.contains("Quadriceps"), "Quads should be excluded")
        }
    }

    // MARK: - Exercise History (No Back-to-Back)

    func testYesterdayExercisesAvoided() {
        let now = Date()
        let calendar = Calendar.current
        let yesterday = calendar.date(byAdding: .day, value: -1, to: calendar.startOfDay(for: now))!
            .addingTimeInterval(10 * 3600) // 10 AM yesterday

        let yesterdayWorkout = Workout(date: yesterday)
        let benchExercise = Exercise(
            name: "Bench Press",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [MuscleGroup(name: "Chest", isPrimary: true)]
        )
        let set = ExerciseSet(
            exercise: benchExercise,
            workout: yesterdayWorkout,
            weightKg: 100,
            reps: 8,
            setType: .working,
            setNumber: 1
        )
        yesterdayWorkout.sets = [set]

        let exercises = makeExerciseLibrary() + [benchExercise]

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [yesterdayWorkout],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60,
            currentDate: now
        )

        let hasBenchPress = result.exercises.contains { $0.exercise?.id == benchExercise.id }
        XCTAssertFalse(hasBenchPress, "Yesterday's exercises should be excluded to avoid repetition")
    }

    // MARK: - Time Constraint

    func testShortDurationLimitsExercises() {
        let exercises = makeExerciseLibrary()

        let short = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 30
        )

        let long = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 90
        )

        XCTAssertLessThanOrEqual(
            short.exercises.count,
            long.exercises.count,
            "Shorter sessions should have fewer or equal exercises"
        )
        XCTAssertLessThanOrEqual(
            short.estimatedDurationMinutes,
            long.estimatedDurationMinutes,
            "Shorter sessions should have lower estimated duration"
        )
    }

    func testMinimumTwoExercises() {
        let exercises = makeExerciseLibrary()

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 15
        )

        XCTAssertGreaterThanOrEqual(
            result.exercises.count, 2,
            "Even very short sessions should have at least 2 exercises"
        )
    }

    // MARK: - Program Template Integration

    func testFollowsActiveProgramDayRotation() {
        let program = makeSimpleProgram()
        // No history → should get first day
        let result = service.generateWorkout(
            activeProgram: program,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: [],
            timeConstraintMinutes: 60
        )

        XCTAssertEqual(result.sessionName, "Day A")
        let templateReasoning = result.reasoning.first { $0.factor == .programTemplate }
        XCTAssertNotNil(templateReasoning)
        XCTAssertTrue(templateReasoning?.summary.contains("Test Program") ?? false)
    }

    func testAdvancesToNextProgramDay() {
        let program = makeSimpleProgram()
        let dayA = program.days.first { $0.name == "Day A" }!

        // Simulate a completed workout on Day A
        let pastWorkout = Workout(date: Date().addingTimeInterval(-86400), program: program, programDay: dayA)

        let resolved = service.resolveToday(program: program, workoutHistory: [pastWorkout])
        XCTAssertEqual(resolved?.name, "Day B", "Should advance to next day after completing Day A")
    }

    func testWrapsAroundProgramDays() {
        let program = makeSimpleProgram()
        let dayB = program.days.first { $0.name == "Day B" }!

        let pastWorkout = Workout(date: Date().addingTimeInterval(-86400), program: program, programDay: dayB)

        let resolved = service.resolveToday(program: program, workoutHistory: [pastWorkout])
        XCTAssertEqual(resolved?.name, "Day A", "Should wrap around to first day after last day")
    }

    // MARK: - Experience Level Scaling

    func testBeginnerGetsFewerExercises() {
        let exercises = makeExerciseLibrary()
        let beginner = UserProfile(experienceLevel: .beginner)
        let advanced = UserProfile(experienceLevel: .advanced)

        let beginnerWorkout = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: beginner,
            availableExercises: exercises,
            timeConstraintMinutes: 90
        )

        let advancedWorkout = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: advanced,
            availableExercises: exercises,
            timeConstraintMinutes: 90
        )

        XCTAssertLessThanOrEqual(
            beginnerWorkout.exercises.count,
            advancedWorkout.exercises.count,
            "Beginners should get fewer or equal exercises"
        )
    }

    // MARK: - Ad-Hoc Generation Quality

    func testAdHocPrioritizesCompounds() {
        let exercises = makeExerciseLibrary()

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 30
        )

        guard let first = result.exercises.first else {
            XCTFail("Should have at least one exercise")
            return
        }
        XCTAssertEqual(
            first.exercise?.category, .compound,
            "First exercise should be a compound movement"
        )
    }

    func testAdHocCoversDiverseMuscles() {
        let exercises = makeExerciseLibrary()

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        let allMuscles = result.exercises.flatMap { ex in
            ex.exercise?.muscleGroups.filter(\.isPrimary).map(\.name) ?? []
        }
        let uniqueMuscles = Set(allMuscles)

        XCTAssertGreaterThanOrEqual(
            uniqueMuscles.count, 2,
            "Ad-hoc generation should cover multiple muscle groups"
        )
    }

    // MARK: - Swap Candidate

    func testFindSwapCandidateReturnsSameMuscle() {
        let chest = MuscleGroup(name: "Chest", isPrimary: true)
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell, muscleGroups: [chest])
        let dbPress = Exercise(name: "Dumbbell Press", category: .compound, equipment: .dumbbell, muscleGroups: [chest])
        let squat = Exercise(name: "Squat", category: .compound, equipment: .barbell,
                             muscleGroups: [MuscleGroup(name: "Quadriceps", isPrimary: true)])

        let swap = service.findSwapCandidate(
            for: bench,
            in: [dbPress, squat],
            workoutHistory: []
        )

        XCTAssertEqual(swap?.name, "Dumbbell Press", "Swap should target same muscle group")
    }

    func testFindSwapCandidateAvoidsYesterday() {
        let now = Date()
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: Calendar.current.startOfDay(for: now))!
            .addingTimeInterval(10 * 3600)

        let chest = MuscleGroup(name: "Chest", isPrimary: true)
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell, muscleGroups: [chest])
        let dbPress = Exercise(name: "Dumbbell Press", category: .compound, equipment: .dumbbell, muscleGroups: [chest])
        let incline = Exercise(name: "Incline Press", category: .compound, equipment: .barbell, muscleGroups: [chest])

        // dbPress was done yesterday
        let workout = Workout(date: yesterday)
        let set = ExerciseSet(exercise: dbPress, workout: workout, weightKg: 30, reps: 10)
        workout.sets = [set]

        let swap = service.findSwapCandidate(
            for: bench,
            in: [dbPress, incline],
            workoutHistory: [workout],
            currentDate: now
        )

        XCTAssertEqual(swap?.name, "Incline Press", "Swap should avoid yesterday's exercises")
    }

    func testFindSwapCandidateReturnsNilWhenNoneAvailable() {
        let chest = MuscleGroup(name: "Chest", isPrimary: true)
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell, muscleGroups: [chest])

        // Only a leg exercise available — no chest alternative
        let squat = Exercise(name: "Squat", category: .compound, equipment: .barbell,
                             muscleGroups: [MuscleGroup(name: "Quadriceps", isPrimary: true)])

        let swap = service.findSwapCandidate(
            for: bench,
            in: [squat],
            workoutHistory: []
        )

        XCTAssertNil(swap, "Should return nil when no suitable swap exists")
    }

    func testFindSwapCandidatePrefersSameEquipment() {
        let chest = MuscleGroup(name: "Chest", isPrimary: true)
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell, muscleGroups: [chest])
        let closegrip = Exercise(name: "Close Grip Bench", category: .compound, equipment: .barbell, muscleGroups: [chest])
        let dbPress = Exercise(name: "Dumbbell Press", category: .compound, equipment: .dumbbell, muscleGroups: [chest])

        let swap = service.findSwapCandidate(
            for: bench,
            in: [dbPress, closegrip],
            workoutHistory: []
        )

        XCTAssertEqual(swap?.name, "Close Grip Bench", "Should prefer same equipment")
    }

    // MARK: - Reasoning Quality

    func testReasoningTextCoversAllFactors() {
        let now = Date()
        let recentWorkout = makeWorkout(
            hoursAgo: 6,
            exercises: [("Bench Press", ["Chest"], 100, 8, 4)],
            from: now
        )
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 82, label: .good)

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [recentWorkout],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60,
            currentDate: now
        )

        XCTAssertFalse(result.reasoningText.isEmpty, "Reasoning text should not be empty")

        let factors = Set(result.reasoning.map(\.factor))
        XCTAssertTrue(factors.contains(.readiness), "Should include readiness reasoning")
        XCTAssertTrue(factors.contains(.programTemplate), "Should include program/template reasoning")
    }

    func testReasoningIncludesSpecificReadinessScore() {
        let readiness = makeReadiness(score: 65, label: .moderate)
        let exercises = makeExerciseLibrary()

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        let readinessStep = result.reasoning.first { $0.factor == .readiness }
        XCTAssertTrue(
            readinessStep?.summary.contains("65") ?? false,
            "Reasoning should include the actual readiness score"
        )
    }

    // MARK: - Volume/Intensity Adjustments

    func testWeightMultiplierApplied() {
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 55, label: .moderate)
        let program = makeProgramWithHeavyDay()

        let result = service.generateWorkout(
            activeProgram: program,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        // All exercises should have weightMultiplier <= 1.0
        for exercise in result.exercises {
            XCTAssertLessThanOrEqual(
                exercise.weightMultiplier, 1.0,
                "Moderate readiness should reduce weight multiplier"
            )
        }
    }

    func testSetsNotReducedBelowMinimum() {
        let exercises = makeExerciseLibrary()
        let readiness = makeReadiness(score: 45, label: .moderate)

        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: readiness,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        for exercise in result.exercises {
            XCTAssertGreaterThanOrEqual(
                exercise.targetSets, 2,
                "Sets should never go below 2 even with volume reduction"
            )
        }
    }

    // MARK: - Edge Cases

    func testCancelledWorkoutsIgnored() {
        let now = Date()
        let workout = makeWorkout(
            hoursAgo: 2,
            exercises: [("Bench Press", ["Chest"], 100, 8, 4)],
            from: now
        )
        workout.isCancelled = true

        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [workout],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60,
            currentDate: now
        )

        // Cancelled workouts shouldn't affect fatigue — chest should still be available
        let hasChestExercise = result.exercises.contains { ex in
            ex.exercise?.muscleGroups.contains(where: { $0.name == "Chest" && $0.isPrimary }) ?? false
        }
        XCTAssertTrue(hasChestExercise, "Cancelled workouts should not affect muscle recovery")
    }

    func testEmptyProgramDaysReturnsNil() {
        let program = Program(name: "Empty Program")

        let resolved = service.resolveToday(program: program, workoutHistory: [])
        XCTAssertNil(resolved, "Empty program should return nil for today's day")
    }

    func testGeneratedExerciseOrderIsSequential() {
        let exercises = makeExerciseLibrary()
        let result = service.generateWorkout(
            activeProgram: nil,
            workoutHistory: [],
            readinessScore: nil,
            userProfile: nil,
            availableExercises: exercises,
            timeConstraintMinutes: 60
        )

        for (index, exercise) in result.exercises.enumerated() {
            XCTAssertEqual(exercise.order, index + 1, "Exercise order should be sequential from 1")
        }
    }

    // MARK: - Helper Methods

    private func makeExerciseLibrary() -> [Exercise] {
        [
            Exercise(
                name: "Bench Press",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [MuscleGroup(name: "Chest", isPrimary: true), MuscleGroup(name: "Triceps", isPrimary: false)]
            ),
            Exercise(
                name: "Squat",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [MuscleGroup(name: "Quadriceps", isPrimary: true), MuscleGroup(name: "Glutes", isPrimary: false)]
            ),
            Exercise(
                name: "Deadlift",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [MuscleGroup(name: "Back", isPrimary: true), MuscleGroup(name: "Hamstrings", isPrimary: false)]
            ),
            Exercise(
                name: "Overhead Press",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [MuscleGroup(name: "Shoulders", isPrimary: true)]
            ),
            Exercise(
                name: "Barbell Row",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [MuscleGroup(name: "Back", isPrimary: true), MuscleGroup(name: "Biceps", isPrimary: false)]
            ),
            Exercise(
                name: "Lat Pulldown",
                category: .compound,
                equipment: .cable,
                muscleGroups: [MuscleGroup(name: "Lats", isPrimary: true)]
            ),
            Exercise(
                name: "Leg Curl",
                category: .isolation,
                equipment: .machine,
                muscleGroups: [MuscleGroup(name: "Hamstrings", isPrimary: true)]
            ),
            Exercise(
                name: "Bicep Curl",
                category: .isolation,
                equipment: .dumbbell,
                muscleGroups: [MuscleGroup(name: "Biceps", isPrimary: true)]
            ),
            Exercise(
                name: "Tricep Extension",
                category: .accessory,
                equipment: .cable,
                muscleGroups: [MuscleGroup(name: "Triceps", isPrimary: true)]
            ),
            Exercise(
                name: "Face Pull",
                category: .accessory,
                equipment: .cable,
                muscleGroups: [MuscleGroup(name: "Shoulders", isPrimary: true)]
            ),
        ]
    }

    private func makeReadiness(score: Double, label: ReadinessLabel) -> ReadinessScore {
        ReadinessScore(
            date: Date(),
            overallScore: score,
            sleepScore: score,
            hrvScore: score,
            restingHRScore: score,
            trainingLoadScore: score,
            recommendation: ReadinessScoreService.recommendation(for: label),
            label: label
        )
    }

    private func makeWorkout(
        hoursAgo: Int = 0,
        daysAgo: Int = 0,
        exercises: [(name: String, muscles: [String], weightKg: Double, reps: Int, sets: Int)],
        from referenceDate: Date = Date()
    ) -> Workout {
        let date = referenceDate.addingTimeInterval(-Double(daysAgo * 24 * 3600 + hoursAgo * 3600))
        let workout = Workout(date: date)

        for (name, muscles, weight, reps, setCount) in exercises {
            let muscleGroups = muscles.map { MuscleGroup(name: $0, isPrimary: true) }
            let exercise = Exercise(
                name: name,
                category: .compound,
                equipment: .barbell,
                muscleGroups: muscleGroups
            )

            for setNum in 1...setCount {
                let set = ExerciseSet(
                    exercise: exercise,
                    workout: workout,
                    weightKg: weight,
                    reps: reps,
                    setType: .working,
                    setNumber: setNum
                )
                workout.sets.append(set)
            }
        }

        return workout
    }

    private func makeSimpleProgram() -> Program {
        let program = Program(name: "Test Program", durationWeeks: 8, frequencyPerWeek: 2, isActive: true)

        let dayA = ProgramDay(dayNumber: 1, name: "Day A")
        dayA.program = program

        let benchExercise = Exercise(
            name: "Bench Press",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [MuscleGroup(name: "Chest", isPrimary: true)]
        )
        let plannedBench = PlannedExercise(
            order: 1,
            exercise: benchExercise,
            programDay: dayA,
            targetSets: 4,
            targetReps: "6-8",
            targetRPE: 8.0
        )
        dayA.plannedExercises = [plannedBench]

        let dayB = ProgramDay(dayNumber: 2, name: "Day B")
        dayB.program = program

        let squatExercise = Exercise(
            name: "Squat",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [MuscleGroup(name: "Quadriceps", isPrimary: true)]
        )
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squatExercise,
            programDay: dayB,
            targetSets: 4,
            targetReps: "5-6",
            targetRPE: 8.5
        )
        dayB.plannedExercises = [plannedSquat]

        program.days = [dayA, dayB]
        return program
    }

    private func makeProgramWithHeavyDay() -> Program {
        let program = Program(name: "Heavy Program", durationWeeks: 4, frequencyPerWeek: 3, isActive: true)
        let day = ProgramDay(dayNumber: 1, name: "Heavy Day")
        day.program = program

        let exercise = Exercise(
            name: "Squat",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [MuscleGroup(name: "Quadriceps", isPrimary: true)]
        )
        let planned = PlannedExercise(
            order: 1,
            exercise: exercise,
            programDay: day,
            targetSets: 5,
            targetReps: "3-5",
            targetRPE: 9.0
        )
        day.plannedExercises = [planned]
        program.days = [day]
        return program
    }
}
