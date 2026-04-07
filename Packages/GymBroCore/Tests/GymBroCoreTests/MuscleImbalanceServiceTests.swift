import XCTest
@testable import GymBroCore

/// Comprehensive test suite for MuscleImbalanceService.
/// Tests volume landmark detection and push/pull ratio analysis.
final class MuscleImbalanceServiceTests: XCTestCase {
    
    var service: MuscleImbalanceService!
    
    override func setUp() {
        super.setUp()
        service = MuscleImbalanceService()
    }
    
    override func tearDown() {
        service = nil
        super.tearDown()
    }
    
    // MARK: - Balanced Training
    
    func testNoImbalance_WhenTrainingIsBalanced() {
        let workouts = createBalancedWorkouts(weeks: 4)
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.alerts.isEmpty ?? false)
        XCTAssertTrue(result!.recommendations.contains { $0.contains("well-balanced") })
    }
    
    // MARK: - Push/Pull Ratio
    
    func testPushPullImbalance_WhenPushVolumeMuchHigherThanPull() {
        let workouts = createImbalancedWorkouts(
            weeks: 4,
            chestSets: 20,
            backSets: 10,
            quadsSets: 15,
            hamstringsSets: 8
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.pushPullRatio > 1.5)
        XCTAssertTrue(result?.alerts.contains(where: { $0.type == .pushPullImbalance }) ?? false)
    }
    
    func testNoPushPullImbalance_WhenRatioIsBalanced() {
        let workouts = createBalancedWorkouts(weeks: 4)
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.pushPullRatio >= 0.8 && result!.pushPullRatio <= 1.2)
        let hasImbalanceAlert = result?.alerts.contains(where: { $0.type == .pushPullImbalance }) ?? false
        XCTAssertFalse(hasImbalanceAlert)
    }
    
    func testPosteriorDominance_WhenPullVolumeExcessivelyHigh() {
        let workouts = createImbalancedWorkouts(
            weeks: 4,
            chestSets: 8,
            backSets: 20,
            quadsSets: 10,
            hamstringsSets: 15
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.pushPullRatio < 0.67)
        XCTAssertTrue(result?.alerts.contains(where: { $0.type == .pushPullImbalance }) ?? false)
    }
    
    // MARK: - Volume Landmark Violations (MRV)
    
    func testVolumeTooHigh_WhenChestExceedsMRV() {
        let workouts = createWorkoutsWithMuscleOverload(
            weeks: 4,
            muscleGroup: "Chest",
            isPrimary: true,
            setsPerWeek: 25 // MRV is 22
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.alerts.contains(where: {
            $0.type == .volumeTooHigh && $0.muscleGroup == "Chest"
        }) ?? false)
    }
    
    func testVolumeTooHigh_WhenQuadsExceedMRV() {
        let workouts = createWorkoutsWithMuscleOverload(
            weeks: 4,
            muscleGroup: "Quads",
            isPrimary: true,
            setsPerWeek: 28 // MRV is 25
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.alerts.contains(where: {
            $0.type == .volumeTooHigh && $0.muscleGroup == "Quads"
        }) ?? false)
    }
    
    func testNoVolumeAlert_WhenWithinMAVRange() {
        let workouts = createWorkoutsWithOptimalVolume(
            weeks: 4,
            muscleGroup: "Chest",
            isPrimary: true,
            setsPerWeek: 14 // MAV
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        let hasChestAlert = result?.alerts.contains(where: { $0.muscleGroup == "Chest" }) ?? false
        XCTAssertFalse(hasChestAlert)
    }
    
    // MARK: - Volume Landmark Violations (MEV)
    
    func testVolumeTooLow_WhenSignificantlyBelowMEV() {
        let workouts = createWorkoutsWithMuscleUnderload(
            weeks: 4,
            muscleGroup: "Back",
            isPrimary: true,
            setsPerWeek: 4 // MEV is 10
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.alerts.contains(where: {
            $0.type == .volumeTooLow && $0.muscleGroup == "Back"
        }) ?? false)
    }
    
    func testNoVolumeTooLowAlert_WhenSlightlyBelowMEV() {
        let workouts = createWorkoutsWithMuscleUnderload(
            weeks: 4,
            muscleGroup: "Chest",
            isPrimary: true,
            setsPerWeek: 7 // MEV is 8, but only warn if < 50% of MEV
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        let hasChestAlert = result?.alerts.contains(where: {
            $0.type == .volumeTooLow && $0.muscleGroup == "Chest"
        }) ?? false
        XCTAssertFalse(hasChestAlert)
    }
    
    // MARK: - Primary vs Secondary Muscle Weighting
    
    func testPrimaryMuscleWeighting_CountsFullSet() {
        let workouts = createWorkoutsWithMuscleGroup(
            weeks: 4,
            muscleGroup: "Chest",
            isPrimary: true,
            totalSets: 56 // 14 sets/week
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        let chestVolume = result?.weeklySetCounts["Chest"] ?? 0
        XCTAssertEqual(chestVolume, 14, accuracy: 0.1)
    }
    
    func testSecondaryMuscleWeighting_CountsHalfSet() {
        let workouts = createWorkoutsWithMuscleGroup(
            weeks: 4,
            muscleGroup: "Triceps",
            isPrimary: false,
            totalSets: 56 // Should be 7 sets/week (56 × 0.5 / 4 weeks)
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        let tricepsVolume = result?.weeklySetCounts["Triceps"] ?? 0
        XCTAssertEqual(tricepsVolume, 7, accuracy: 0.1)
    }
    
    // MARK: - Multiple Violations
    
    func testMultipleViolations_GeneratesMultipleAlerts() {
        let workouts = createWorkoutsWithMultipleImbalances()
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertGreaterThan(result?.alerts.count ?? 0, 2)
    }
    
    // MARK: - Recommendations
    
    func testRecommendations_SuggestReducingVolume_WhenExceedingMRV() {
        let workouts = createWorkoutsWithMuscleOverload(
            weeks: 4,
            muscleGroup: "Chest",
            isPrimary: true,
            setsPerWeek: 25
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.recommendations.contains { $0.contains("Reduce volume") })
    }
    
    func testRecommendations_SuggestIncreasingVolume_WhenBelowMEV() {
        let workouts = createWorkoutsWithMuscleUnderload(
            weeks: 4,
            muscleGroup: "Back",
            isPrimary: true,
            setsPerWeek: 4
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.recommendations.contains { $0.contains("Increase volume") })
    }
    
    func testRecommendations_SuggestPosteriorWork_WhenAnteriorDominance() {
        let workouts = createImbalancedWorkouts(
            weeks: 4,
            chestSets: 20,
            backSets: 10,
            quadsSets: 15,
            hamstringsSets: 8
        )
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.recommendations.contains { $0.contains("Anterior dominance") })
        XCTAssertTrue(result!.recommendations.contains { $0.contains("back") || $0.contains("posterior") })
    }
    
    // MARK: - Insufficient Data Handling
    
    func testReturnsNil_WhenLessThanTwoWeeksOfData() {
        let workouts = createBalancedWorkouts(weeks: 1)
        
        let result = service.analyze(workouts: workouts, timeWindow: .oneMonth)
        
        XCTAssertNil(result)
    }
    
    func testReturnsNil_WhenNoWorkouts() {
        let result = service.analyze(workouts: [], timeWindow: .oneMonth)
        
        XCTAssertNil(result)
    }
    
    // MARK: - Test Data Helpers
    
    private func createBalancedWorkouts(weeks: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        let chestExercise = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        chestExercise.muscleGroups = [MuscleGroup(name: "Chest", isPrimary: true)]
        
        let backExercise = Exercise(name: "Barbell Row", category: .compound, equipment: .barbell)
        backExercise.muscleGroups = [MuscleGroup(name: "Back", isPrimary: true)]
        
        let quadsExercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        quadsExercise.muscleGroups = [MuscleGroup(name: "Quads", isPrimary: true)]
        
        let hamstringsExercise = Exercise(name: "Romanian Deadlift", category: .compound, equipment: .barbell)
        hamstringsExercise.muscleGroups = [MuscleGroup(name: "Hamstrings", isPrimary: true)]
        
        for week in 0..<weeks {
            // 3 workouts per week
            for day in [0, 2, 4] {
                let date = Calendar.current.date(byAdding: .day, value: week * 7 + day, to: startDate)!
                let workout = Workout(name: "Workout", date: date, notes: "")
                
                // Balanced: 4 sets chest, 4 sets back, 3 sets quads, 3 sets hamstrings per workout
                addSets(to: workout, exercise: chestExercise, count: 4)
                addSets(to: workout, exercise: backExercise, count: 4)
                addSets(to: workout, exercise: quadsExercise, count: 3)
                addSets(to: workout, exercise: hamstringsExercise, count: 3)
                
                workouts.append(workout)
            }
        }
        
        return workouts
    }
    
    private func createImbalancedWorkouts(weeks: Int, chestSets: Int, backSets: Int, quadsSets: Int, hamstringsSets: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        let chestExercise = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        chestExercise.muscleGroups = [MuscleGroup(name: "Chest", isPrimary: true)]
        
        let backExercise = Exercise(name: "Barbell Row", category: .compound, equipment: .barbell)
        backExercise.muscleGroups = [MuscleGroup(name: "Back", isPrimary: true)]
        
        let quadsExercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        quadsExercise.muscleGroups = [MuscleGroup(name: "Quads", isPrimary: true)]
        
        let hamstringsExercise = Exercise(name: "Romanian Deadlift", category: .compound, equipment: .barbell)
        hamstringsExercise.muscleGroups = [MuscleGroup(name: "Hamstrings", isPrimary: true)]
        
        for week in 0..<weeks {
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            addSets(to: workout, exercise: chestExercise, count: chestSets)
            addSets(to: workout, exercise: backExercise, count: backSets)
            addSets(to: workout, exercise: quadsExercise, count: quadsSets)
            addSets(to: workout, exercise: hamstringsExercise, count: hamstringsSets)
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func createWorkoutsWithMuscleOverload(weeks: Int, muscleGroup: String, isPrimary: Bool, setsPerWeek: Int) -> [Workout] {
        return createWorkoutsWithMuscleGroup(
            weeks: weeks,
            muscleGroup: muscleGroup,
            isPrimary: isPrimary,
            totalSets: setsPerWeek * weeks
        )
    }
    
    private func createWorkoutsWithOptimalVolume(weeks: Int, muscleGroup: String, isPrimary: Bool, setsPerWeek: Int) -> [Workout] {
        return createWorkoutsWithMuscleGroup(
            weeks: weeks,
            muscleGroup: muscleGroup,
            isPrimary: isPrimary,
            totalSets: setsPerWeek * weeks
        )
    }
    
    private func createWorkoutsWithMuscleUnderload(weeks: Int, muscleGroup: String, isPrimary: Bool, setsPerWeek: Int) -> [Workout] {
        return createWorkoutsWithMuscleGroup(
            weeks: weeks,
            muscleGroup: muscleGroup,
            isPrimary: isPrimary,
            totalSets: setsPerWeek * weeks
        )
    }
    
    private func createWorkoutsWithMuscleGroup(weeks: Int, muscleGroup: String, isPrimary: Bool, totalSets: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        let exercise = Exercise(name: "Test Exercise", category: .compound, equipment: .barbell)
        exercise.muscleGroups = [MuscleGroup(name: muscleGroup, isPrimary: isPrimary)]
        
        let setsPerWorkout = totalSets / (weeks * 3) // 3 workouts per week
        
        for week in 0..<weeks {
            for day in [0, 2, 4] {
                let date = Calendar.current.date(byAdding: .day, value: week * 7 + day, to: startDate)!
                let workout = Workout(name: "Workout", date: date, notes: "")
                
                addSets(to: workout, exercise: exercise, count: setsPerWorkout)
                
                workouts.append(workout)
            }
        }
        
        return workouts
    }
    
    private func createWorkoutsWithMultipleImbalances() -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -4, to: Date())!
        
        let chestExercise = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        chestExercise.muscleGroups = [MuscleGroup(name: "Chest", isPrimary: true)]
        
        let backExercise = Exercise(name: "Barbell Row", category: .compound, equipment: .barbell)
        backExercise.muscleGroups = [MuscleGroup(name: "Back", isPrimary: true)]
        
        let quadsExercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        quadsExercise.muscleGroups = [MuscleGroup(name: "Quads", isPrimary: true)]
        
        for week in 0..<4 {
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            // Chest: 25 sets/week (exceeds MRV of 22)
            addSets(to: workout, exercise: chestExercise, count: 25)
            
            // Back: 4 sets/week (below MEV of 10)
            addSets(to: workout, exercise: backExercise, count: 4)
            
            // Quads: 28 sets/week (exceeds MRV of 25)
            addSets(to: workout, exercise: quadsExercise, count: 28)
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func addSets(to workout: Workout, exercise: Exercise, count: Int) {
        for _ in 0..<count {
            let set = ExerciseSet(
                exercise: exercise,
                weightKg: 100,
                reps: 8,
                setType: .working
            )
            set.workout = workout
            workout.sets.append(set)
        }
    }
}
