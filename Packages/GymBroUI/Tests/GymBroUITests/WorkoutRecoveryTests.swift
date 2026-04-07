import XCTest
import SwiftData
@testable import GymBroUI
@testable import GymBroCore

@MainActor
final class WorkoutRecoveryTests: XCTestCase {
    var modelContext: ModelContext!

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

        let config = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: true
        )

        let container = try ModelContainer(
            for: schema,
            configurations: [config]
        )

        modelContext = ModelContext(container)
    }

    // MARK: - WorkoutRecoveryService

    func testFindUnfinishedWorkout_returnsActiveWorkout() {
        let workout = Workout()
        workout.isActive = true
        workout.startTime = Date().addingTimeInterval(-600)
        modelContext.insert(workout)
        try! modelContext.save()

        let service = WorkoutRecoveryService(modelContext: modelContext)
        let found = service.findUnfinishedWorkout()

        XCTAssertNotNil(found)
        XCTAssertEqual(found?.id, workout.id)
    }

    func testFindUnfinishedWorkout_ignoresFinishedWorkouts() {
        let workout = Workout()
        workout.isActive = false
        workout.startTime = Date().addingTimeInterval(-600)
        workout.endTime = Date()
        modelContext.insert(workout)
        try! modelContext.save()

        let service = WorkoutRecoveryService(modelContext: modelContext)
        XCTAssertNil(service.findUnfinishedWorkout())
    }

    func testFindUnfinishedWorkout_returnsMostRecent() {
        let older = Workout()
        older.isActive = true
        older.startTime = Date().addingTimeInterval(-3600)
        older.updatedAt = Date().addingTimeInterval(-3600)
        modelContext.insert(older)

        let newer = Workout()
        newer.isActive = true
        newer.startTime = Date().addingTimeInterval(-300)
        newer.updatedAt = Date().addingTimeInterval(-300)
        modelContext.insert(newer)

        try! modelContext.save()

        let service = WorkoutRecoveryService(modelContext: modelContext)
        let found = service.findUnfinishedWorkout()

        XCTAssertNotNil(found)
        XCTAssertEqual(found?.id, newer.id)
    }

    func testDiscardWorkout_marksCancelledAndInactive() {
        let workout = Workout()
        workout.isActive = true
        workout.startTime = Date().addingTimeInterval(-600)
        modelContext.insert(workout)
        try! modelContext.save()

        let service = WorkoutRecoveryService(modelContext: modelContext)
        service.discardWorkout(workout)

        XCTAssertFalse(workout.isActive)
        XCTAssertTrue(workout.isCancelled)
        XCTAssertNotNil(workout.endTime)
    }

    func testCleanupStaleWorkouts_excludesSpecifiedWorkout() {
        let keep = Workout()
        keep.isActive = true
        keep.startTime = Date().addingTimeInterval(-300)
        modelContext.insert(keep)

        let stale = Workout()
        stale.isActive = true
        stale.startTime = Date().addingTimeInterval(-7200)
        modelContext.insert(stale)

        try! modelContext.save()

        let service = WorkoutRecoveryService(modelContext: modelContext)
        service.cleanupStaleWorkouts(excluding: keep.id)

        XCTAssertTrue(keep.isActive)
        XCTAssertFalse(stale.isActive)
        XCTAssertTrue(stale.isCancelled)
    }

    // MARK: - ActiveWorkoutViewModel integration

    func testStartWorkout_setsIsActive() {
        let workout = Workout()
        modelContext.insert(workout)
        try! modelContext.save()

        XCTAssertFalse(workout.isActive)

        let viewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout
        )
        viewModel.startWorkout()

        XCTAssertTrue(workout.isActive)
    }

    func testFinishWorkout_clearsIsActive() {
        let exercise = Exercise(
            name: "Squat",
            category: .compound,
            equipment: .barbell
        )
        modelContext.insert(exercise)

        let workout = Workout()
        workout.isActive = true
        workout.startTime = Date().addingTimeInterval(-600)
        modelContext.insert(workout)
        try! modelContext.save()

        let viewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: [exercise]
        )
        viewModel.completeSet()

        let summary = viewModel.finishWorkout()

        XCTAssertFalse(workout.isActive)
        XCTAssertNotNil(workout.endTime)
        XCTAssertGreaterThan(summary.totalSets, 0)
    }

    func testCancelWorkout_marksInactiveAndCancelled() {
        let workout = Workout()
        workout.isActive = true
        workout.startTime = Date().addingTimeInterval(-600)
        modelContext.insert(workout)
        try! modelContext.save()

        let viewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout
        )
        viewModel.cancelWorkout()

        XCTAssertFalse(workout.isActive)
        XCTAssertTrue(workout.isCancelled)
        XCTAssertNotNil(workout.endTime)
    }

    // MARK: - Recovery with sets

    func testRecovery_preservesCompletedSets() {
        let exercise = Exercise(
            name: "Bench Press",
            category: .compound,
            equipment: .barbell
        )
        modelContext.insert(exercise)

        let workout = Workout()
        workout.isActive = true
        workout.startTime = Date().addingTimeInterval(-600)
        modelContext.insert(workout)
        try! modelContext.save()

        // Simulate completing sets before crash
        let vm = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: [exercise]
        )
        vm.updateWeight(100)
        vm.updateReps(5)
        vm.completeSet()
        vm.completeSet()

        XCTAssertEqual(workout.sets.count, 2)

        // Simulate recovery — re-create VM from persisted workout
        let recoveredVM = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: [exercise]
        )

        XCTAssertEqual(recoveredVM.completedSetsForActiveExercise.count, 2)
        XCTAssertEqual(recoveredVM.activeSetNumber, 3)
        XCTAssertTrue(workout.isActive)
    }

    // MARK: - Workout.exercises computed property

    func testWorkoutExercises_returnsUniqueExercisesInOrder() {
        let bench = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        let squat = Exercise(name: "Squat", category: .compound, equipment: .barbell)
        modelContext.insert(bench)
        modelContext.insert(squat)

        let workout = Workout()
        modelContext.insert(workout)

        let set1 = ExerciseSet(exercise: bench, workout: workout, weightKg: 80, reps: 5, setNumber: 1)
        set1.completedAt = Date()
        modelContext.insert(set1)
        workout.sets.append(set1)

        let set2 = ExerciseSet(exercise: squat, workout: workout, weightKg: 100, reps: 5, setNumber: 1)
        set2.completedAt = Date()
        modelContext.insert(set2)
        workout.sets.append(set2)

        let set3 = ExerciseSet(exercise: bench, workout: workout, weightKg: 82.5, reps: 5, setNumber: 2)
        set3.completedAt = Date()
        modelContext.insert(set3)
        workout.sets.append(set3)

        try! modelContext.save()

        let exercises = workout.exercises
        XCTAssertEqual(exercises.count, 2)
        XCTAssertEqual(exercises[0].name, "Bench Press")
        XCTAssertEqual(exercises[1].name, "Squat")
    }

    func testNoUnfinishedWorkout_returnsNil() {
        let service = WorkoutRecoveryService(modelContext: modelContext)
        XCTAssertNil(service.findUnfinishedWorkout())
    }
}
