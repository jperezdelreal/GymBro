import XCTest
@testable import GymBroCore

/// Comprehensive test suite for OvertrainingDetectionService.
/// Tests all 5 detection methods and risk stratification.
final class OvertrainingDetectionServiceTests: XCTestCase {
    
    var service: OvertrainingDetectionService!
    
    override func setUp() {
        super.setUp()
        service = OvertrainingDetectionService()
    }
    
    override func tearDown() {
        service = nil
        super.tearDown()
    }
    
    // MARK: - Multi-Exercise Plateau Detection
    
    func testNoOvertraining_WhenNoPlateaus() {
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.riskLevel, .none)
        XCTAssertTrue(result?.activeSignals.isEmpty ?? false)
    }
    
    func testMultiExercisePlateau_WhenThreeExercisesStalled() {
        let analyses = createPlateauAnalyses(plateauedCount: 3)
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertEqual(result?.riskLevel, .moderate)
        XCTAssertTrue(result?.activeSignals.contains(where: { $0.type == .multiExercisePlateau }) ?? false)
    }
    
    func testNoMultiExercisePlateau_WhenOnlyTwoExercisesStalled() {
        let analyses = createPlateauAnalyses(plateauedCount: 2)
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        let hasMultiPlateauSignal = result?.activeSignals.contains(where: { $0.type == .multiExercisePlateau }) ?? false
        XCTAssertFalse(hasMultiPlateauSignal)
    }
    
    // MARK: - Volume Ramp Rate Detection
    
    func testDangerousVolumeRamp_WhenWeeklyVolumeIncreases15Percent() {
        let workouts = createWorkoutsWithVolumeRamp(
            weeks: 4,
            baseVolume: 1000,
            weeklyIncreaseRate: 0.15
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.activeSignals.contains(where: { $0.type == .volumeRampTooFast }) ?? false)
    }
    
    func testNoVolumeRampSignal_WhenWeeklyVolumeIncreasesSlowly() {
        let workouts = createWorkoutsWithVolumeRamp(
            weeks: 4,
            baseVolume: 1000,
            weeklyIncreaseRate: 0.05
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        let hasVolumeRampSignal = result?.activeSignals.contains(where: { $0.type == .volumeRampTooFast }) ?? false
        XCTAssertFalse(hasVolumeRampSignal)
    }
    
    // MARK: - RPE Drift Detection
    
    func testRPEDrift_WhenSameWeightFeelsHarder() {
        let workouts = createWorkoutsWithRPEDrift(
            weeks: 5,
            constantWeight: 100,
            startingRPE: 7.0,
            endingRPE: 8.5
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.activeSignals.contains(where: { $0.type == .rpeDrift }) ?? false)
    }
    
    func testNoRPEDrift_WhenWeightProgresses() {
        let workouts = createWorkoutsWithProgressingWeight(
            weeks: 5,
            startingWeight: 100,
            weeklyIncrease: 2.5,
            constantRPE: 7.5
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        let hasRPEDrift = result?.activeSignals.contains(where: { $0.type == .rpeDrift }) ?? false
        XCTAssertFalse(hasRPEDrift)
    }
    
    // MARK: - Performance Decline Detection
    
    func testPerformanceDecline_WhenStrengthDropsAcrossMultipleLifts() {
        let workouts = createWorkoutsWithPerformanceDecline(
            exercises: ["Squat", "Bench Press", "Deadlift"],
            weeks: 6,
            declinePercentage: 0.08
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.activeSignals.contains(where: { $0.type == .performanceDecline }) ?? false)
    }
    
    func testNoPerformanceDecline_WhenStrengthMaintained() {
        let workouts = createWorkoutsWithConsistentStrength(
            exercises: ["Squat", "Bench Press"],
            weeks: 6
        )
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        let hasDecline = result?.activeSignals.contains(where: { $0.type == .performanceDecline }) ?? false
        XCTAssertFalse(hasDecline)
    }
    
    // MARK: - Chronic Low Readiness Detection
    
    func testChronicLowReadiness_WhenReadinessLowAndVolumeHigh() {
        let workouts = createWorkoutsWithHighVolume(weeks: 2, workoutsPerWeek: 5)
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 50, days: 5)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result?.activeSignals.contains(where: { $0.type == .chronicLowReadiness }) ?? false)
    }
    
    func testNoChronicLowReadiness_WhenReadinessLowButVolumeReduced() {
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 50, days: 5)
        
        // Remove workouts in last 5 days to simulate deload
        let recentWorkouts = workouts.filter {
            let daysSince = Calendar.current.dateComponents([.day], from: $0.date, to: Date()).day ?? 0
            return daysSince > 5
        }
        
        let result = service.analyze(
            workouts: recentWorkouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        let hasChronicFatigue = result?.activeSignals.contains(where: { $0.type == .chronicLowReadiness }) ?? false
        XCTAssertFalse(hasChronicFatigue)
    }
    
    // MARK: - Risk Level Stratification
    
    func testRiskLevel_None_WhenNoSignals() {
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertEqual(result?.riskLevel, .none)
    }
    
    func testRiskLevel_Moderate_WhenOneOrTwoSignals() {
        let analyses = createPlateauAnalyses(plateauedCount: 3) // 1 signal
        let workouts = createWorkoutsWithConsistentVolume(weeks: 4)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertEqual(result?.riskLevel, .moderate)
    }
    
    func testRiskLevel_High_WhenThreeOrMoreSignals() {
        let analyses = createPlateauAnalyses(plateauedCount: 3) // Signal 1
        let workouts = createWorkoutsWithVolumeRamp(weeks: 4, baseVolume: 1000, weeklyIncreaseRate: 0.15) // Signal 2
        // Add high volume to trigger chronic fatigue
        let highVolumeWorkouts = createWorkoutsWithHighVolume(weeks: 2, workoutsPerWeek: 5)
        let allWorkouts = workouts + highVolumeWorkouts
        let readiness = createConsistentReadiness(score: 50, days: 5) // Signal 3
        
        let result = service.analyze(
            workouts: allWorkouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNotNil(result)
        XCTAssertTrue(result!.activeSignals.count >= 3)
        XCTAssertEqual(result?.riskLevel, .high)
    }
    
    // MARK: - Insufficient Data Handling
    
    func testReturnsNil_WhenLessThanFourWeeksOfData() {
        let workouts = createWorkoutsWithConsistentVolume(weeks: 2)
        let analyses = createPlateauAnalyses(plateauedCount: 0)
        let readiness = createConsistentReadiness(score: 75, days: 7)
        
        let result = service.analyze(
            workouts: workouts,
            plateauAnalyses: analyses,
            recentReadinessScores: readiness
        )
        
        XCTAssertNil(result)
    }
    
    // MARK: - Test Data Helpers
    
    private func createPlateauAnalyses(plateauedCount: Int) -> [PlateauAnalysis] {
        let exercises = ["Squat", "Bench Press", "Deadlift", "Overhead Press", "Barbell Row"]
        
        return exercises.enumerated().map { (index, name) in
            let isPlateau = index < plateauedCount
            return PlateauAnalysis(
                exerciseId: UUID(),
                exerciseName: name,
                compositeScore: isPlateau ? 0.75 : 0.30,
                forecastScore: isPlateau ? 0.8 : 0.2,
                changePointScore: isPlateau ? 0.7 : 0.3,
                rollingAverageScore: isPlateau ? 0.75 : 0.4,
                progressState: isPlateau ? .plateaued : .progressing,
                sessionsInState: 3
            )
        }
    }
    
    private func createWorkoutsWithConsistentVolume(weeks: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        for week in 0..<weeks {
            for day in 0..<3 {
                let date = Calendar.current.date(byAdding: .day, value: week * 7 + day * 2, to: startDate)!
                let workout = Workout(name: "Workout", date: date, notes: "")
                
                for _ in 0..<5 {
                    let set = ExerciseSet(weightKg: 100, reps: 8, setType: .working)
                    set.workout = workout
                    workout.sets.append(set)
                }
                
                workouts.append(workout)
            }
        }
        
        return workouts
    }
    
    private func createWorkoutsWithVolumeRamp(weeks: Int, baseVolume: Double, weeklyIncreaseRate: Double) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        for week in 0..<weeks {
            let multiplier = pow(1.0 + weeklyIncreaseRate, Double(week))
            let weekVolume = baseVolume * multiplier
            let setsPerWorkout = Int(weekVolume / 800) // ~8 reps × 100kg per set
            
            for day in 0..<3 {
                let date = Calendar.current.date(byAdding: .day, value: week * 7 + day * 2, to: startDate)!
                let workout = Workout(name: "Workout", date: date, notes: "")
                
                for _ in 0..<setsPerWorkout {
                    let set = ExerciseSet(weightKg: 100, reps: 8, setType: .working)
                    set.workout = workout
                    workout.sets.append(set)
                }
                
                workouts.append(workout)
            }
        }
        
        return workouts
    }
    
    private func createWorkoutsWithRPEDrift(weeks: Int, constantWeight: Double, startingRPE: Double, endingRPE: Double) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        let exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        
        for week in 0..<weeks {
            let progress = Double(week) / Double(weeks - 1)
            let currentRPE = startingRPE + (endingRPE - startingRPE) * progress
            
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            for _ in 0..<3 {
                let set = ExerciseSet(
                    exercise: exercise,
                    weightKg: constantWeight,
                    reps: 8,
                    rpe: currentRPE,
                    setType: .working
                )
                set.workout = workout
                workout.sets.append(set)
            }
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func createWorkoutsWithProgressingWeight(weeks: Int, startingWeight: Double, weeklyIncrease: Double, constantRPE: Double) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        let exercise = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        
        for week in 0..<weeks {
            let currentWeight = startingWeight + Double(week) * weeklyIncrease
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            for _ in 0..<3 {
                let set = ExerciseSet(
                    exercise: exercise,
                    weightKg: currentWeight,
                    reps: 8,
                    rpe: constantRPE,
                    setType: .working
                )
                set.workout = workout
                workout.sets.append(set)
            }
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func createWorkoutsWithPerformanceDecline(exercises: [String], weeks: Int, declinePercentage: Double) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        for week in 0..<weeks {
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            for exerciseName in exercises {
                let exercise = Exercise(name: exerciseName, category: .compound, equipment: .barbell)
                let baseWeight = 150.0
                
                // Decline in second half
                let weight: Double
                if week < weeks / 2 {
                    weight = baseWeight
                } else {
                    weight = baseWeight * (1.0 - declinePercentage)
                }
                
                for _ in 0..<3 {
                    let set = ExerciseSet(
                        exercise: exercise,
                        weightKg: weight,
                        reps: 5,
                        setType: .working
                    )
                    set.workout = workout
                    workout.sets.append(set)
                }
            }
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func createWorkoutsWithConsistentStrength(exercises: [String], weeks: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        for week in 0..<weeks {
            let date = Calendar.current.date(byAdding: .weekOfYear, value: week, to: startDate)!
            let workout = Workout(name: "Workout", date: date, notes: "")
            
            for exerciseName in exercises {
                let exercise = Exercise(name: exerciseName, category: .compound, equipment: .barbell)
                
                for _ in 0..<3 {
                    let set = ExerciseSet(
                        exercise: exercise,
                        weightKg: 150,
                        reps: 5,
                        setType: .working
                    )
                    set.workout = workout
                    workout.sets.append(set)
                }
            }
            
            workouts.append(workout)
        }
        
        return workouts
    }
    
    private func createWorkoutsWithHighVolume(weeks: Int, workoutsPerWeek: Int) -> [Workout] {
        var workouts: [Workout] = []
        let startDate = Calendar.current.date(byAdding: .weekOfYear, value: -weeks, to: Date())!
        
        for week in 0..<weeks {
            for day in 0..<workoutsPerWeek {
                let date = Calendar.current.date(byAdding: .day, value: week * 7 + day, to: startDate)!
                let workout = Workout(name: "Workout", date: date, notes: "")
                
                for _ in 0..<8 {
                    let set = ExerciseSet(weightKg: 100, reps: 10, setType: .working)
                    set.workout = workout
                    workout.sets.append(set)
                }
                
                workouts.append(workout)
            }
        }
        
        return workouts
    }
    
    private func createConsistentReadiness(score: Double, days: Int) -> [ReadinessScore] {
        var scores: [ReadinessScore] = []
        
        for day in 0..<days {
            let date = Calendar.current.date(byAdding: .day, value: -day, to: Date())!
            let readiness = ReadinessScore(
                date: date,
                overallScore: score,
                sleepScore: score,
                hrvScore: score,
                restingHRScore: score,
                trainingLoadScore: score,
                recommendation: "Test",
                label: .good
            )
            scores.append(readiness)
        }
        
        return scores
    }
}
