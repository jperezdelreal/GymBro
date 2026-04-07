import XCTest
import SwiftData
@testable import GymBroCore

final class SmartDefaultsServiceTests: XCTestCase {

    private var container: ModelContainer!
    private var context: ModelContext!
    private var sut: SmartDefaultsService!

    @MainActor
    override func setUp() {
        super.setUp()
        do {
            let schema = Schema([
                Exercise.self,
                ExerciseSet.self,
                Workout.self,
                Program.self,
                ProgramDay.self,
                PlannedExercise.self,
                UserProfile.self,
                BodyweightEntry.self,
                MuscleGroup.self,
                ChatMessage.self,
                PlateauAnalysis.self,
                ReadinessScore.self,
                SubjectiveCheckIn.self,
                HealthBaseline.self,
                SleepRecord.self,
            ])
            let config = ModelConfiguration(isStoredInMemoryOnly: true)
            container = try ModelContainer(for: schema, configurations: [config])
            context = container.mainContext
            sut = SmartDefaultsService(modelContext: context)
        } catch {
            XCTFail("Failed to create in-memory ModelContainer: \(error)")
        }
    }

    override func tearDown() {
        sut = nil
        context = nil
        container = nil
        super.tearDown()
    }

    // MARK: - Default Values (no history)

    @MainActor
    func testDefaultValues_compoundBarbell_intermediateUser() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 62.5, "Compound barbell default 60 + 2.5 progression")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testDefaultValues_compoundBarbell_beginnerUser() {
        let profile = UserProfile(experienceLevel: .beginner)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 63.75, "Beginner gets 1.5x progression: 60 + 3.75")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testDefaultValues_compoundBarbell_advancedUser() {
        let profile = UserProfile(experienceLevel: .advanced)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 61.25, "Advanced gets 0.5x progression: 60 + 1.25")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testDefaultValues_isolation() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Bicep Curl", category: .isolation, equipment: .dumbbell)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 16.25, "Isolation default 15 + 1.25 progression")
        XCTAssertEqual(reps, 10)
    }

    @MainActor
    func testDefaultValues_accessory() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Face Pull", category: .accessory, equipment: .cable)
        context.insert(exercise)

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 10.0, "Accessory gets 0 progression")
        XCTAssertEqual(reps, 12)
    }

    // MARK: - With History

    @MainActor
    func testSmartDefaults_withHistory_appliesProgression() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 102.5, "Should add 2.5kg compound progression to top weight of 100")
        XCTAssertEqual(reps, 5)
    }

    // MARK: - RPE Integration

    @MainActor
    func testSmartDefaults_rpe_lowRPE_increasesWeight() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Bench", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                rpe: 6.5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        
        // Base: 100, RPE adjustment: +2.5%, Progression: +2.5
        // = 100 * 1.025 + 2.5 = 105
        XCTAssertEqual(weight, 105.0, "Low RPE should increase weight by 2.5% + progression")
    }

    @MainActor
    func testSmartDefaults_rpe_highRPE_reducesWeight() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Bench", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                rpe: 10.0,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        
        // Base: 100, RPE adjustment: -5%, Progression: +2.5
        // = 100 * 0.95 + 2.5 = 97.5
        XCTAssertEqual(weight, 97.5, "High RPE should reduce weight by 5% before progression")
    }

    // MARK: - Intra-Session Fatigue

    @MainActor
    func testSmartDefaults_fatigueBySetNumber() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weightSet1, _) = sut.getSmartDefaults(for: exercise, setNumber: 1)
        let (weightSet3, _) = sut.getSmartDefaults(for: exercise, setNumber: 3)
        
        XCTAssertGreaterThan(weightSet1, weightSet3, "Set 1 should suggest higher weight than set 3 due to fatigue")
    }

    @MainActor
    func testSmartDefaults_fatigueByExercisePosition() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weightFirstExercise, _) = sut.getSmartDefaults(for: exercise, exercisePositionInWorkout: 1)
        let (weightFifthExercise, _) = sut.getSmartDefaults(for: exercise, exercisePositionInWorkout: 5)
        
        XCTAssertGreaterThan(weightFirstExercise, weightFifthExercise, "First exercise should suggest higher weight than 5th due to cumulative fatigue")
    }

    // MARK: - Recovery Integration

    @MainActor
    func testSmartDefaults_highReadiness_increasesWeight() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise, currentReadinessScore: 85.0)
        
        // Base: 100, Recovery multiplier: 1.025, Progression: 2.5
        // = 100 * 1.025 + 2.5 = 105
        XCTAssertEqual(weight, 105.0, "High readiness (85) should apply +2.5% multiplier")
    }

    @MainActor
    func testSmartDefaults_lowReadiness_reducesWeight() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise, currentReadinessScore: 50.0)
        
        // Base: 100, Recovery multiplier: 0.90, Progression: 2.5
        // = 100 * 0.90 + 2.5 = 92.5
        XCTAssertEqual(weight, 92.5, "Low readiness (50) should apply 0.90x multiplier")
    }

    @MainActor
    func testSmartDefaults_poorReadiness_significantReduction() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise, currentReadinessScore: 30.0)
        
        // Base: 100, Recovery multiplier: 0.80, Progression: 2.5
        // = 100 * 0.80 + 2.5 = 82.5
        XCTAssertEqual(weight, 82.5, "Poor readiness (30) should apply 0.80x multiplier for rest day")
    }

    // MARK: - Deload Detection

    @MainActor
    func testSmartDefaults_deloadWeek_holdsWeight() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        // Session 1: 100kg (2 weeks ago)
        let workout1 = Workout(date: Date().addingTimeInterval(-1209600))
        workout1.endTime = Date().addingTimeInterval(-1209600)
        context.insert(workout1)
        
        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout1,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-1209600)
            context.insert(set)
        }

        // Session 2: 105kg (1 week ago)
        let workout2 = Workout(date: Date().addingTimeInterval(-604800))
        workout2.endTime = Date().addingTimeInterval(-604800)
        context.insert(workout2)
        
        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout2,
                weightKg: 105.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-604800)
            context.insert(set)
        }

        // Session 3: 75kg (yesterday — deload week!)
        let workout3 = Workout(date: Date().addingTimeInterval(-86400))
        workout3.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout3)
        
        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout3,
                weightKg: 75.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        
        // Deload detected (75 < 0.85 * 105), should NOT add progression
        XCTAssertEqual(weight, 75.0, "Deload week should hold weight, not add progression")
    }

    // MARK: - Multi-Session Trend Analysis

    @MainActor
    func testSmartDefaults_positiveTrend_fullProgression() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Bench", category: .compound, equipment: .barbell)
        context.insert(exercise)

        // Create 4 sessions with increasing weights (positive trend)
        let weights = [90.0, 95.0, 100.0, 105.0]
        
        for (index, weight) in weights.enumerated() {
            let daysAgo = Double((weights.count - 1 - index) * 7) * 86400
            let workout = Workout(date: Date().addingTimeInterval(-daysAgo))
            workout.endTime = Date().addingTimeInterval(-daysAgo)
            context.insert(workout)
            
            for i in 1...3 {
                let set = ExerciseSet(
                    exercise: exercise,
                    workout: workout,
                    weightKg: weight,
                    reps: 5,
                    setType: .working,
                    setNumber: i
                )
                set.completedAt = Date().addingTimeInterval(-daysAgo)
                context.insert(set)
            }
        }

        try? context.save()

        let (predictedWeight, _) = sut.getSmartDefaults(for: exercise)
        
        // Positive trend → full progression (2.5kg)
        XCTAssertEqual(predictedWeight, 107.5, "Positive trend should apply full progression")
    }

    @MainActor
    func testSmartDefaults_negativeTrend_reducedProgression() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Bench", category: .compound, equipment: .barbell)
        context.insert(exercise)

        // Create 4 sessions with decreasing weights (negative trend/plateau)
        let weights = [105.0, 100.0, 97.5, 95.0]
        
        for (index, weight) in weights.enumerated() {
            let daysAgo = Double((weights.count - 1 - index) * 7) * 86400
            let workout = Workout(date: Date().addingTimeInterval(-daysAgo))
            workout.endTime = Date().addingTimeInterval(-daysAgo)
            context.insert(workout)
            
            for i in 1...3 {
                let set = ExerciseSet(
                    exercise: exercise,
                    workout: workout,
                    weightKg: weight,
                    reps: 5,
                    setType: .working,
                    setNumber: i
                )
                set.completedAt = Date().addingTimeInterval(-daysAgo)
                context.insert(set)
            }
        }

        try? context.save()

        let (predictedWeight, _) = sut.getSmartDefaults(for: exercise)
        
        // Negative trend → half progression (1.25kg instead of 2.5kg)
        XCTAssertEqual(predictedWeight, 96.25, "Negative trend should apply half progression")
    }

    // MARK: - Edge Cases

    @MainActor
    func testSmartDefaults_ignoresWarmupSets() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        let warmup = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 50.0,
            reps: 10,
            setType: .warmup,
            setNumber: 1
        )
        warmup.completedAt = Date().addingTimeInterval(-86400)
        context.insert(warmup)

        let working = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100.0,
            reps: 5,
            setType: .working,
            setNumber: 2
        )
        working.completedAt = Date().addingTimeInterval(-86400)
        context.insert(working)

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 102.5, "Should only use working sets, ignore warmups")
    }

    @MainActor
    func testSmartDefaults_ignoresIncompleteSets() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        let incomplete = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 200.0,
            reps: 1,
            setType: .working,
            setNumber: 1
        )
        context.insert(incomplete)

        try? context.save()

        let (weight, reps) = sut.getSmartDefaults(for: exercise)
        XCTAssertEqual(weight, 62.5, "Should return default when no completed sets")
        XCTAssertEqual(reps, 5)
    }

    @MainActor
    func testSmartDefaults_gracefulDegradation_noRPE() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                rpe: nil,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise)
        
        // Without RPE, should still work: base 100 + progression 2.5
        XCTAssertEqual(weight, 102.5, "Should gracefully handle missing RPE data")
    }

    @MainActor
    func testSmartDefaults_gracefulDegradation_noReadiness() {
        let profile = UserProfile(experienceLevel: .intermediate)
        context.insert(profile)
        
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        context.insert(exercise)

        let workout = Workout(date: Date().addingTimeInterval(-86400))
        workout.endTime = Date().addingTimeInterval(-86400)
        context.insert(workout)

        for i in 1...3 {
            let set = ExerciseSet(
                exercise: exercise,
                workout: workout,
                weightKg: 100.0,
                reps: 5,
                setType: .working,
                setNumber: i
            )
            set.completedAt = Date().addingTimeInterval(-86400)
            context.insert(set)
        }

        try? context.save()

        let (weight, _) = sut.getSmartDefaults(for: exercise, currentReadinessScore: nil)
        
        // Without readiness, should still work: base 100 + progression 2.5
        XCTAssertEqual(weight, 102.5, "Should gracefully handle missing readiness score")
    }
}
