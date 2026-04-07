import XCTest
import SwiftData
@testable import GymBroCore

@MainActor
final class SupersetTests: XCTestCase {
    var modelContainer: ModelContainer!
    var modelContext: ModelContext!
    var supersetService: SupersetService!
    
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
        supersetService = SupersetService.shared
    }
    
    override func tearDown() {
        modelContainer = nil
        modelContext = nil
    }
    
    func testCreateSupersetGroup() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise1 = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise1)
        
        let exercise2 = Exercise(
            name: "Bent-Over Row",
            category: .compound,
            primaryMuscleGroup: .back,
            equipmentType: .barbell
        )
        modelContext.insert(exercise2)
        
        // When
        let group = supersetService.createSupersetGroup(
            exercises: [exercise1, exercise2],
            workout: workout,
            modelContext: modelContext
        )
        
        // Then
        XCTAssertEqual(group.exerciseOrder.count, 2)
        XCTAssertTrue(group.exerciseOrder.contains(exercise1.id))
        XCTAssertTrue(group.exerciseOrder.contains(exercise2.id))
        XCTAssertEqual(workout.supersetGroups.count, 1)
        XCTAssertEqual(group.workout?.id, workout.id)
    }
    
    func testAddSetToSuperset() throws {
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
        
        let group = SupersetGroup(workout: workout, exerciseOrder: [exercise.id])
        modelContext.insert(group)
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 10
        )
        modelContext.insert(set)
        
        // When
        supersetService.addSetToSuperset(set: set, group: group)
        
        // Then
        XCTAssertEqual(set.supersetGroup?.id, group.id)
        XCTAssertEqual(group.sets.count, 1)
        XCTAssertEqual(group.sets.first?.id, set.id)
    }
    
    func testRemoveSetFromSuperset() throws {
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
        
        let group = SupersetGroup(workout: workout, exerciseOrder: [exercise.id])
        modelContext.insert(group)
        
        let set = ExerciseSet(
            exercise: exercise,
            workout: workout,
            weightKg: 100,
            reps: 10
        )
        modelContext.insert(set)
        
        supersetService.addSetToSuperset(set: set, group: group)
        
        // When
        supersetService.removeSetFromSuperset(set: set, modelContext: modelContext)
        
        // Then
        XCTAssertNil(set.supersetGroup)
        XCTAssertEqual(group.sets.count, 0)
    }
    
    func testIsRoundComplete() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise1 = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise1)
        
        let exercise2 = Exercise(
            name: "Bent-Over Row",
            category: .compound,
            primaryMuscleGroup: .back,
            equipmentType: .barbell
        )
        modelContext.insert(exercise2)
        
        let group = supersetService.createSupersetGroup(
            exercises: [exercise1, exercise2],
            workout: workout,
            modelContext: modelContext
        )
        
        let set1 = ExerciseSet(
            exercise: exercise1,
            workout: workout,
            weightKg: 100,
            reps: 10,
            setNumber: 1
        )
        set1.completedAt = Date()
        modelContext.insert(set1)
        supersetService.addSetToSuperset(set: set1, group: group)
        
        // When - only one exercise completed
        var isComplete = group.isRoundComplete(setNumber: 1)
        
        // Then - not complete yet
        XCTAssertFalse(isComplete)
        
        // When - second exercise completed
        let set2 = ExerciseSet(
            exercise: exercise2,
            workout: workout,
            weightKg: 80,
            reps: 10,
            setNumber: 1
        )
        set2.completedAt = Date()
        modelContext.insert(set2)
        supersetService.addSetToSuperset(set: set2, group: group)
        
        isComplete = group.isRoundComplete(setNumber: 1)
        
        // Then - round is complete
        XCTAssertTrue(isComplete)
    }
    
    func testShouldStartRestTimer() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise1 = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise1)
        
        let exercise2 = Exercise(
            name: "Bent-Over Row",
            category: .compound,
            primaryMuscleGroup: .back,
            equipmentType: .barbell
        )
        modelContext.insert(exercise2)
        
        // Test 1: Normal set (not in superset)
        let normalSet = ExerciseSet(
            exercise: exercise1,
            workout: workout,
            weightKg: 100,
            reps: 10
        )
        modelContext.insert(normalSet)
        
        // When
        let shouldRest1 = supersetService.shouldStartRestTimer(after: normalSet, in: workout)
        
        // Then - should start rest timer
        XCTAssertTrue(shouldRest1)
        
        // Test 2: Superset - only first exercise completed
        let group = supersetService.createSupersetGroup(
            exercises: [exercise1, exercise2],
            workout: workout,
            modelContext: modelContext
        )
        
        let supersetSet1 = ExerciseSet(
            exercise: exercise1,
            workout: workout,
            weightKg: 100,
            reps: 10,
            setNumber: 1
        )
        modelContext.insert(supersetSet1)
        supersetService.addSetToSuperset(set: supersetSet1, group: group)
        
        // When
        let shouldRest2 = supersetService.shouldStartRestTimer(after: supersetSet1, in: workout)
        
        // Then - should NOT start rest timer (round not complete)
        XCTAssertFalse(shouldRest2)
        
        // Test 3: Superset - both exercises completed
        let supersetSet2 = ExerciseSet(
            exercise: exercise2,
            workout: workout,
            weightKg: 80,
            reps: 10,
            setNumber: 1
        )
        modelContext.insert(supersetSet2)
        supersetService.addSetToSuperset(set: supersetSet2, group: group)
        
        // When
        let shouldRest3 = supersetService.shouldStartRestTimer(after: supersetSet2, in: workout)
        
        // Then - should start rest timer (round complete)
        XCTAssertTrue(shouldRest3)
    }
    
    func testGetSupersetPartners() throws {
        // Given
        let workout = Workout()
        modelContext.insert(workout)
        
        let exercise1 = Exercise(
            name: "Bench Press",
            category: .compound,
            primaryMuscleGroup: .chest,
            equipmentType: .barbell
        )
        modelContext.insert(exercise1)
        
        let exercise2 = Exercise(
            name: "Bent-Over Row",
            category: .compound,
            primaryMuscleGroup: .back,
            equipmentType: .barbell
        )
        modelContext.insert(exercise2)
        
        let exercise3 = Exercise(
            name: "Squats",
            category: .compound,
            primaryMuscleGroup: .quads,
            equipmentType: .barbell
        )
        modelContext.insert(exercise3)
        
        supersetService.createSupersetGroup(
            exercises: [exercise1, exercise2],
            workout: workout,
            modelContext: modelContext
        )
        
        // When
        let partners1 = supersetService.getSupersetPartners(for: exercise1, in: workout)
        let partners3 = supersetService.getSupersetPartners(for: exercise3, in: workout)
        
        // Then
        XCTAssertEqual(partners1.count, 1)
        XCTAssertEqual(partners1.first?.id, exercise2.id)
        XCTAssertEqual(partners3.count, 0) // Not in a superset
    }
}
