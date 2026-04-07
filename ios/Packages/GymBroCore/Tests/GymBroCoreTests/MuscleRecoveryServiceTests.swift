import XCTest
@testable import GymBroCore

final class MuscleRecoveryServiceTests: XCTestCase {
    var service: MuscleRecoveryService!
    
    override func setUp() {
        super.setUp()
        service = MuscleRecoveryService()
    }
    
    override func tearDown() {
        service = nil
        super.tearDown()
    }
    
    // MARK: - Empty Input Tests
    
    func testEmptyWorkoutsReturnsEmptyMap() {
        let result = service.calculateRecoveryMap(workouts: [])
        XCTAssertTrue(result.isEmpty, "Empty workouts should return empty recovery map")
    }
    
    // MARK: - Fresh Status Tests
    
    func testMuscleIsFreshAfter72Hours() {
        // Large muscle (quads) should be fresh after 72h
        let workout = makeWorkout(
            daysAgo: 4,
            exercises: [("Squat", ["Quadriceps"], 100, 5, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        XCTAssertEqual(result.count, 1)
        let quads = result["Quadriceps"]
        XCTAssertNotNil(quads)
        XCTAssertEqual(quads?.status, .fresh)
        XCTAssertGreaterThan(quads?.recoveryPercentage ?? 0, 95)
    }
    
    func testSmallMuscleIsFreshAfter36Hours() {
        // Small muscle (biceps) should be fresh after 36h
        let workout = makeWorkout(
            daysAgo: 2,
            exercises: [("Curl", ["Biceps"], 20, 10, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let biceps = result["Biceps"]
        XCTAssertNotNil(biceps)
        XCTAssertEqual(biceps?.status, .fresh)
    }
    
    // MARK: - Recovering Status Tests
    
    func testMuscleIsRecoveringAt24To48Hours() {
        // Train chest 30 hours ago (in recovery window)
        let workout = makeWorkout(
            hoursAgo: 30,
            exercises: [("Bench Press", ["Chest"], 100, 8, 4)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let chest = result["Chest"]
        XCTAssertNotNil(chest)
        XCTAssertEqual(chest?.status, .recovering)
        XCTAssertGreaterThan(chest?.recoveryPercentage ?? 0, 30)
        XCTAssertLessThan(chest?.recoveryPercentage ?? 100, 100)
    }
    
    // MARK: - Fatigued Status Tests
    
    func testMuscleIsFatiguedWithin24Hours() {
        // Train back 12 hours ago
        let workout = makeWorkout(
            hoursAgo: 12,
            exercises: [("Deadlift", ["Back"], 150, 5, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let back = result["Back"]
        XCTAssertNotNil(back)
        XCTAssertEqual(back?.status, .fatigued)
        XCTAssertLessThan(back?.recoveryPercentage ?? 100, 50)
    }
    
    func testHighVolumeExtendsFatigueWindow() {
        // High volume session (6 sets vs baseline ~3)
        let workout1 = makeWorkout(
            daysAgo: 7,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 3)]
        )
        let workout2 = makeWorkout(
            hoursAgo: 36,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 6)] // Double volume
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout1, workout2])
        
        let quads = result["Quadriceps"]
        XCTAssertNotNil(quads)
        // With high volume, might still be recovering at 36h instead of fresh
        XCTAssertTrue(
            quads?.status == .recovering || quads?.status == .fatigued,
            "High volume should extend recovery time"
        )
    }
    
    func testHighIntensityExtendsFatigueWindow() {
        // High intensity (low reps, simulating heavy weight)
        let workout = makeWorkout(
            hoursAgo: 36,
            exercises: [("Squat", ["Quadriceps"], 200, 2, 4)] // Very high intensity
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let quads = result["Quadriceps"]
        XCTAssertNotNil(quads)
        // High intensity should keep muscle in recovering state longer
        XCTAssertNotEqual(quads?.status, .fresh, "High intensity should delay full recovery")
    }
    
    // MARK: - Multiple Muscles Tests
    
    func testMultipleMusclesTrackedIndependently() {
        let workout = makeWorkout(
            hoursAgo: 30,
            exercises: [
                ("Bench Press", ["Chest"], 100, 8, 4),
                ("Squat", ["Quadriceps"], 140, 6, 4),
                ("Curl", ["Biceps"], 20, 10, 3)
            ]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        XCTAssertEqual(result.count, 3)
        XCTAssertNotNil(result["Chest"])
        XCTAssertNotNil(result["Quadriceps"])
        XCTAssertNotNil(result["Biceps"])
    }
    
    func testDifferentRecoveryRatesPerMuscleSize() {
        // All muscles trained at same time, different sizes
        let workout = makeWorkout(
            hoursAgo: 40,
            exercises: [
                ("Squat", ["Quadriceps"], 140, 6, 4),      // Large: 72h base
                ("Bench Press", ["Chest"], 100, 8, 4),     // Medium: 48h base
                ("Curl", ["Biceps"], 20, 10, 3)            // Small: 36h base
            ]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        // After 40 hours:
        // - Biceps (36h base) should be fresh or nearly fresh
        // - Chest (48h base) should be recovering
        // - Quads (72h base) should still be fatigued or early recovery
        
        let biceps = result["Biceps"]
        let chest = result["Chest"]
        let quads = result["Quadriceps"]
        
        // Biceps should have highest recovery percentage
        XCTAssertGreaterThan(
            biceps?.recoveryPercentage ?? 0,
            chest?.recoveryPercentage ?? 100
        )
        
        // Chest should have higher recovery than quads
        XCTAssertGreaterThan(
            chest?.recoveryPercentage ?? 0,
            quads?.recoveryPercentage ?? 100
        )
    }
    
    // MARK: - Volume Tracking Tests
    
    func testRecentVolumeCalculation() {
        let workout1 = makeWorkout(
            hoursAgo: 12,
            exercises: [("Bench Press", ["Chest"], 100, 10, 3)] // 3000 volume
        )
        let workout2 = makeWorkout(
            daysAgo: 7,
            exercises: [("Bench Press", ["Chest"], 100, 10, 2)] // 2000 volume (old, not recent)
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout1, workout2])
        
        let chest = result["Chest"]
        XCTAssertNotNil(chest)
        // Recent volume should be ~3000 (only last 48h)
        XCTAssertEqual(chest?.recentVolume ?? 0, 3000, accuracy: 10)
    }
    
    // MARK: - Time Calculation Tests
    
    func testHoursSinceLastTrainedAccurate() {
        let workout = makeWorkout(
            hoursAgo: 25,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let quads = result["Quadriceps"]
        XCTAssertNotNil(quads)
        XCTAssertEqual(quads?.hoursSinceLastTrained ?? 0, 25, accuracy: 1)
    }
    
    func testLastTrainedDateRecorded() {
        let date = Date().addingTimeInterval(-30 * 3600)
        let workout = makeWorkout(
            at: date,
            exercises: [("Deadlift", ["Back"], 150, 5, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let back = result["Back"]
        XCTAssertNotNil(back)
        XCTAssertNotNil(back?.lastTrainedDate)
        XCTAssertEqual(
            back?.lastTrainedDate?.timeIntervalSince1970 ?? 0,
            date.timeIntervalSince1970,
            accuracy: 1
        )
    }
    
    // MARK: - Multiple Workouts Tests
    
    func testMostRecentWorkoutUsedForRecovery() {
        let old = makeWorkout(
            daysAgo: 5,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 3)]
        )
        let recent = makeWorkout(
            hoursAgo: 20,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 3)]
        )
        
        let result = service.calculateRecoveryMap(workouts: [old, recent])
        
        let quads = result["Quadriceps"]
        XCTAssertNotNil(quads)
        // Should be based on recent workout (20h ago), not old one
        XCTAssertLessThan(quads?.hoursSinceLastTrained ?? 100, 24)
    }
    
    // MARK: - Warmup Set Filtering Tests
    
    func testWarmupSetsIgnored() {
        let workout = Workout(date: Date().addingTimeInterval(-24 * 3600))
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [MuscleGroup(name: "Chest", isPrimary: true)]
        )
        
        // Add warmup set (should be ignored)
        let warmupSet = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 40,
            reps: 10,
            setType: .warmup,
            setNumber: 1
        )
        
        // Add working set (should count)
        let workingSet = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 8,
            setType: .working,
            setNumber: 2
        )
        
        workout.sets = [warmupSet, workingSet]
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        let chest = result["Chest"]
        XCTAssertNotNil(chest)
        // Volume should only count working set
        XCTAssertEqual(chest?.recentVolume ?? 0, 800, accuracy: 1) // 100kg * 8 reps
    }
    
    // MARK: - Edge Cases
    
    func testCancelledWorkoutsIgnored() {
        let workout = makeWorkout(
            hoursAgo: 12,
            exercises: [("Squat", ["Quadriceps"], 100, 8, 3)]
        )
        workout.isCancelled = true
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        // Cancelled workout should be ignored
        XCTAssertTrue(result.isEmpty)
    }
    
    func testExerciseWithoutMuscleGroupsIgnored() {
        let workout = Workout(date: Date().addingTimeInterval(-24 * 3600))
        let exercise = Exercise(
            name: "Mystery Exercise",
            category: .compound,
            equipment: .barbell,
            muscleGroups: [] // No muscle groups
        )
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 8
        )
        workout.sets = [set]
        
        let result = service.calculateRecoveryMap(workouts: [workout])
        
        XCTAssertTrue(result.isEmpty, "Exercise without muscle groups should not appear in recovery map")
    }
    
    // MARK: - Helper Methods
    
    /// Create a workout with exercises.
    private func makeWorkout(
        daysAgo: Int = 0,
        hoursAgo: Int = 0,
        exercises: [(name: String, muscles: [String], weightKg: Double, reps: Int, sets: Int)]
    ) -> Workout {
        let date = Date().addingTimeInterval(-Double(daysAgo * 24 * 3600 + hoursAgo * 3600))
        return makeWorkout(at: date, exercises: exercises)
    }
    
    /// Create a workout at a specific date.
    private func makeWorkout(
        at date: Date,
        exercises: [(name: String, muscles: [String], weightKg: Double, reps: Int, sets: Int)]
    ) -> Workout {
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
}
