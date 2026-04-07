import XCTest
import SwiftData
@testable import GymBroUI
@testable import GymBroCore

@MainActor
final class ActiveWorkoutViewModelTests: XCTestCase {
    var modelContext: ModelContext!
    var workout: Workout!
    var exercise: Exercise!
    var viewModel: ActiveWorkoutViewModel!
    
    override func setUp() async throws {
        let schema = Schema([
            Workout.self,
            Exercise.self,
            ExerciseSet.self,
            Program.self,
            ProgramDay.self,
            PlannedExercise.self,
            UserProfile.self,
            BodyweightEntry.self,
            MuscleGroup.self
        ])
        
        let modelConfiguration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: true
        )
        
        let container = try ModelContainer(
            for: schema,
            configurations: [modelConfiguration]
        )
        
        modelContext = ModelContext(container)
        
        workout = Workout()
        exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            equipment: .barbell
        )
        
        modelContext.insert(workout)
        modelContext.insert(exercise)
        try modelContext.save()
        
        viewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: [exercise]
        )
    }
    
    func testStartWorkout() {
        XCTAssertNil(workout.startTime)
        XCTAssertFalse(viewModel.isWorkoutStarted)
        
        viewModel.startWorkout()
        
        XCTAssertNotNil(workout.startTime)
        XCTAssertTrue(viewModel.isWorkoutStarted)
    }
    
    func testCompleteSetWithSmartDefaults() {
        XCTAssertEqual(viewModel.activeSetNumber, 1)
        XCTAssertGreaterThan(viewModel.currentWeight, 0)
        XCTAssertGreaterThan(viewModel.currentReps, 0)
        
        let initialWeight = viewModel.currentWeight
        let initialReps = viewModel.currentReps
        
        viewModel.completeSet()
        
        XCTAssertEqual(workout.sets.count, 1)
        XCTAssertEqual(viewModel.activeSetNumber, 2)
        
        let completedSet = workout.sets.first!
        XCTAssertEqual(completedSet.weightKg, initialWeight)
        XCTAssertEqual(completedSet.reps, initialReps)
        XCTAssertNotNil(completedSet.completedAt)
    }
    
    func testWarmupSetMarkedCorrectly() {
        viewModel.toggleWarmup()
        XCTAssertTrue(viewModel.isWarmup)
        
        viewModel.completeSet()
        
        let completedSet = workout.sets.first!
        XCTAssertEqual(completedSet.setType, .warmup)
        XCTAssertTrue(completedSet.isWarmup)
        
        XCTAssertFalse(viewModel.isWarmup)
    }
    
    func testUpdateWeightAndReps() {
        viewModel.updateWeight(100.0)
        XCTAssertEqual(viewModel.currentWeight, 100.0)
        
        viewModel.updateReps(5)
        XCTAssertEqual(viewModel.currentReps, 5)
        
        viewModel.updateReps(-1)
        XCTAssertEqual(viewModel.currentReps, 0)
    }
    
    func testUpdateRPE() {
        XCTAssertNil(viewModel.currentRPE)
        
        viewModel.updateRPE(8.0)
        XCTAssertEqual(viewModel.currentRPE, 8.0)
        
        viewModel.completeSet()
        let completedSet = workout.sets.first!
        XCTAssertEqual(completedSet.rpe, 8.0)
    }
    
    func testRestTimerStartsAfterSet() {
        XCTAssertFalse(viewModel.isRestTimerActive)
        
        viewModel.completeSet()
        
        XCTAssertTrue(viewModel.isRestTimerActive)
        XCTAssertNotNil(viewModel.restTimerEndTime)
    }
    
    func testSkipRestTimer() {
        viewModel.completeSet()
        XCTAssertTrue(viewModel.isRestTimerActive)
        
        viewModel.skipRestTimer()
        
        XCTAssertFalse(viewModel.isRestTimerActive)
        XCTAssertNil(viewModel.restTimerEndTime)
    }
    
    func testFinishWorkout() {
        viewModel.completeSet()
        viewModel.completeSet()
        
        XCTAssertNil(workout.endTime)
        
        let summary = viewModel.finishWorkout()
        
        XCTAssertNotNil(workout.endTime)
        XCTAssertEqual(summary.totalSets, 2)
        XCTAssertGreaterThan(summary.totalVolume, 0)
    }
    
    func testTotalVolumeCalculation() {
        viewModel.updateWeight(100.0)
        viewModel.updateReps(10)
        viewModel.completeSet()
        
        viewModel.updateWeight(100.0)
        viewModel.updateReps(10)
        viewModel.completeSet()
        
        XCTAssertEqual(viewModel.totalVolume, 2000.0)
    }
    
    func testCompletedSetsForActiveExercise() {
        XCTAssertEqual(viewModel.completedSetsForActiveExercise.count, 0)
        
        viewModel.completeSet()
        XCTAssertEqual(viewModel.completedSetsForActiveExercise.count, 1)
        
        viewModel.completeSet()
        XCTAssertEqual(viewModel.completedSetsForActiveExercise.count, 2)
    }
    
    func testSetActiveExercise() {
        let newExercise = Exercise(
            name: "Squat",
            category: .compound,
            equipment: .barbell
        )
        modelContext.insert(newExercise)
        try modelContext.save()
        
        viewModel.setActiveExercise(newExercise)
        
        XCTAssertEqual(viewModel.activeExercise?.id, newExercise.id)
        XCTAssertEqual(viewModel.activeSetNumber, 1)
    }
}
