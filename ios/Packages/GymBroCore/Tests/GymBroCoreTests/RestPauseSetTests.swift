import XCTest
import SwiftData
@testable import GymBroCore

@MainActor
final class RestPauseSetTests: XCTestCase {
    var modelContainer: ModelContainer!
    var modelContext: ModelContext!
    
    override func setUp() async throws {
        let schema = Schema([
            Workout.self,
            Exercise.self,
            ExerciseSet.self,
            SupersetGroup.self,
            UserProfile.self,
            Program.self,
            ProgramDay.self
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        modelContainer = try ModelContainer(for: schema, configurations: [modelConfiguration])
        modelContext = ModelContext(modelContainer)
    }
    
    override func tearDown() {
        modelContainer = nil
        modelContext = nil
    }
    
    func testRestPauseSetType() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        // When
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 14,
            setType: .restPause,
            subSetReps: [8, 4, 2]
        )
        modelContext.insert(set)
        
        // Then
        XCTAssertEqual(set.setType, .restPause)
        XCTAssertTrue(set.isRestPause)
        XCTAssertFalse(set.isWarmup)
    }
    
    func testRestPauseSubSetReps() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        let subReps = [8, 4, 2]
        
        // When
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 14,
            setType: .restPause,
            subSetReps: subReps
        )
        modelContext.insert(set)
        
        // Then
        XCTAssertEqual(set.subSetReps, subReps)
        XCTAssertEqual(set.subSetReps?.count, 3)
    }
    
    func testTotalRepsCalculation() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        // Test 1: Rest-pause set with sub-sets
        let restPauseSet = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 14,
            setType: .restPause,
            subSetReps: [8, 4, 2]
        )
        modelContext.insert(restPauseSet)
        
        // When
        let total1 = restPauseSet.totalReps
        
        // Then
        XCTAssertEqual(total1, 14) // 8 + 4 + 2
        
        // Test 2: Normal set (no sub-sets)
        let normalSet = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 10
        )
        modelContext.insert(normalSet)
        
        // When
        let total2 = normalSet.totalReps
        
        // Then
        XCTAssertEqual(total2, 10)
    }
    
    func testRestPauseWithEmptySubSets() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        // When - rest-pause with empty sub-sets
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 10,
            setType: .restPause,
            subSetReps: []
        )
        modelContext.insert(set)
        
        // Then - falls back to reps
        XCTAssertEqual(set.totalReps, 10)
    }
    
    func testRestPauseWithNilSubSets() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        // When - rest-pause with nil sub-sets
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 10,
            setType: .restPause
        )
        modelContext.insert(set)
        
        // Then - falls back to reps
        XCTAssertEqual(set.totalReps, 10)
        XCTAssertNil(set.subSetReps)
    }
    
    func testVolumeCalculationWithRestPause() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise)
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 14,
            setType: .restPause,
            subSetReps: [8, 4, 2]
        )
        modelContext.insert(set)
        
        // When
        let volume = set.volume
        
        // Then - volume uses the reps value (14), not sub-sets
        XCTAssertEqual(volume, 1400) // 100 kg * 14 reps
    }
}
