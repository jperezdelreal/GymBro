import XCTest
import SwiftData
@testable import GymBroCore

final class ProgramComplianceServiceTests: XCTestCase {
    var modelContainer: ModelContainer!
    var modelContext: ModelContext!
    
    override func setUp() async throws {
        let schema = Schema([
            Program.self,
            ProgramDay.self,
            ProgramWeek.self,
            PlannedExercise.self,
            Exercise.self,
            Workout.self,
            ExerciseSet.self
        ])
        
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(modelContainer)
    }
    
    override func tearDown() {
        modelContainer = nil
        modelContext = nil
    }
    
    func createTestExercise(name: String) -> Exercise {
        let exercise = Exercise(
            name: name,
            category: .compound,
            equipment: .barbell,
            instructions: "Test instructions",
            muscleGroups: [MuscleGroup(name: "Chest", isPrimary: true)],
            isCustom: false
        )
        modelContext.insert(exercise)
        return exercise
    }
    
    func testPerfectCompliance() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        let bench = createTestExercise(name: "Barbell Bench Press")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0,
            notes: ""
        )
        modelContext.insert(plannedSquat)
        
        let plannedBench = PlannedExercise(
            order: 2,
            exercise: bench,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "8",
            targetRPE: 7.5,
            notes: ""
        )
        modelContext.insert(plannedBench)
        
        programWeek.plannedExercises = [plannedSquat, plannedBench]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Create sets matching the plan exactly
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 8.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: bench, workout: workout, weight: 80, reps: 8, rpe: 7.5)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertEqual(result.completedExercises, 2, "Should complete both exercises")
        XCTAssertEqual(result.totalExercises, 2, "Should have 2 planned exercises")
        XCTAssertEqual(result.adherencePercentage, 100.0, accuracy: 0.1, "Should have perfect adherence")
        XCTAssertEqual(result.compliance, .excellent, "Should be excellent compliance")
        XCTAssertTrue(result.missedExercises.isEmpty, "Should have no missed exercises")
        XCTAssertTrue(result.extraExercises.isEmpty, "Should have no extra exercises")
    }
    
    func testMissedExercise() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        let bench = createTestExercise(name: "Barbell Bench Press")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0
        )
        modelContext.insert(plannedSquat)
        
        let plannedBench = PlannedExercise(
            order: 2,
            exercise: bench,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "8",
            targetRPE: 7.5
        )
        modelContext.insert(plannedBench)
        
        programWeek.plannedExercises = [plannedSquat, plannedBench]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Only do squat, skip bench
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 8.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertEqual(result.completedExercises, 1, "Should complete 1 exercise")
        XCTAssertEqual(result.totalExercises, 2, "Should have 2 planned exercises")
        XCTAssertEqual(result.adherencePercentage, 50.0, accuracy: 0.1, "Should have 50% adherence")
        XCTAssertEqual(result.compliance, .poor, "Should be poor compliance")
        XCTAssertEqual(result.missedExercises, ["Barbell Bench Press"], "Should list missed bench press")
    }
    
    func testExtraExercise() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        let bench = createTestExercise(name: "Barbell Bench Press")
        let curl = createTestExercise(name: "Barbell Curl")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0
        )
        modelContext.insert(plannedSquat)
        
        let plannedBench = PlannedExercise(
            order: 2,
            exercise: bench,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "8",
            targetRPE: 7.5
        )
        modelContext.insert(plannedBench)
        
        programWeek.plannedExercises = [plannedSquat, plannedBench]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Do planned exercises + extra curl
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 8.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: bench, workout: workout, weight: 80, reps: 8, rpe: 7.5)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Add extra exercise
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: curl, workout: workout, weight: 40, reps: 10, rpe: 7.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertEqual(result.completedExercises, 2, "Should complete both planned exercises")
        XCTAssertEqual(result.extraExercises, ["Barbell Curl"], "Should list extra curl")
        XCTAssertLessThan(result.adherencePercentage, 100.0, "Should penalize for extra exercise")
    }
    
    func testRPEDeviation() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0,
            notes: ""
        )
        modelContext.insert(plannedSquat)
        programWeek.plannedExercises = [plannedSquat]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Do sets with significantly higher RPE (9.5 instead of 8.0)
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 9.5)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertNotNil(result.rpeDeviation, "Should calculate RPE deviation")
        XCTAssertEqual(result.rpeDeviation ?? 0, 1.5, accuracy: 0.1, "Should have 1.5 RPE deviation")
        XCTAssertLessThan(result.adherencePercentage, 100.0, "Should penalize for high RPE deviation")
    }
    
    func testVolumeDeviation() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0,
            notes: ""
        )
        modelContext.insert(plannedSquat)
        programWeek.plannedExercises = [plannedSquat]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Only do 2 sets instead of 3 (33% deviation)
        for _ in 0..<2 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 8.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertNotNil(result.volumeDeviation, "Should calculate volume deviation")
        XCTAssertEqual(result.volumeDeviation ?? 0, 0.33, accuracy: 0.01, "Should have ~33% volume deviation")
        XCTAssertLessThan(result.adherencePercentage, 100.0, "Should penalize for volume deviation")
    }
    
    func testComplianceLevels() throws {
        // Test each compliance level threshold
        
        // Excellent: 90%+
        XCTAssertEqual(
            ProgramComplianceService.ComplianceResult.ComplianceLevel.excellent,
            classifyCompliance(95.0)
        )
        
        // Good: 75-89%
        XCTAssertEqual(
            ProgramComplianceService.ComplianceResult.ComplianceLevel.good,
            classifyCompliance(82.0)
        )
        
        // Moderate: 60-74%
        XCTAssertEqual(
            ProgramComplianceService.ComplianceResult.ComplianceLevel.moderate,
            classifyCompliance(67.0)
        )
        
        // Poor: <60%
        XCTAssertEqual(
            ProgramComplianceService.ComplianceResult.ComplianceLevel.poor,
            classifyCompliance(45.0)
        )
    }
    
    private func classifyCompliance(_ percentage: Double) -> ProgramComplianceService.ComplianceResult.ComplianceLevel {
        switch percentage {
        case 90...:
            return .excellent
        case 75..<90:
            return .good
        case 60..<75:
            return .moderate
        default:
            return .poor
        }
    }
    
    func testEmptyWorkout() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let plannedSquat = PlannedExercise(
            order: 1,
            exercise: squat,
            programWeek: programWeek,
            targetSets: 3,
            targetReps: "5",
            targetRPE: 8.0
        )
        modelContext.insert(plannedSquat)
        programWeek.plannedExercises = [plannedSquat]
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Act - empty workout
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertEqual(result.completedExercises, 0, "Should complete 0 exercises")
        XCTAssertEqual(result.adherencePercentage, 0.0, "Should have 0% adherence")
        XCTAssertEqual(result.compliance, .poor, "Should be poor compliance")
    }
    
    func testNoPlannedExercises() throws {
        // Arrange
        let squat = createTestExercise(name: "Barbell Back Squat")
        
        let programWeek = ProgramWeek(weekNumber: 1)
        modelContext.insert(programWeek)
        
        let workout = Workout(name: "Test Workout", date: Date())
        modelContext.insert(workout)
        
        // Do a workout even though nothing is planned
        for _ in 0..<3 {
            let set = ExerciseSet(exercise: squat, workout: workout, weight: 100, reps: 5, rpe: 8.0)
            modelContext.insert(set)
            workout.exerciseSets.append(set)
        }
        
        // Act
        let result = ProgramComplianceService.calculateCompliance(workout: workout, programWeek: programWeek)
        
        // Assert
        XCTAssertEqual(result.totalExercises, 0, "Should have 0 planned exercises")
        XCTAssertEqual(result.adherencePercentage, 0.0, "Should have 0% adherence when nothing is planned")
    }
}
