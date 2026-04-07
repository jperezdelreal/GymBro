import XCTest
import SwiftData
@testable import GymBroCore

final class PRDetectionServiceTests: XCTestCase {
    var modelContext: ModelContext!
    var exercise: Exercise!
    var workout: Workout!
    var service: PRDetectionService!

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
            MuscleGroup.self,
            SupersetGroup.self,
            ProgramWeek.self
        ])

        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        let container = try ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(container)

        exercise = Exercise(name: "Bench Press", category: .compound, equipment: .barbell)
        workout = Workout()
        modelContext.insert(exercise)
        modelContext.insert(workout)
        try modelContext.save()

        service = PRDetectionService(modelContext: modelContext)
    }

    // MARK: - First set is always a PR

    func testFirstWorkingSetIsAlwaysPR() throws {
        let set = ExerciseSet(exercise: exercise, workout: workout, weightKg: 100, reps: 5, setType: .working)
        set.completedAt = Date()
        modelContext.insert(set)
        try modelContext.save()

        let result = service.checkForPR(set: set)

        XCTAssertTrue(result.isPR)
        XCTAssertFalse(result.recordTypes.isEmpty)
        XCTAssertEqual(result.exerciseName, "Bench Press")
        XCTAssertEqual(result.weight, 100)
        XCTAssertEqual(result.reps, 5)
    }

    // MARK: - Weight PR detection

    func testDetectsWeightPR() throws {
        let oldSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 80, reps: 5, setType: .working)
        oldSet.completedAt = Date().addingTimeInterval(-3600)
        modelContext.insert(oldSet)
        try modelContext.save()

        let newSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 85, reps: 5, setType: .working)
        newSet.completedAt = Date()
        modelContext.insert(newSet)
        try modelContext.save()

        let result = service.checkForPR(set: newSet)

        XCTAssertTrue(result.isPR)
        XCTAssertTrue(result.recordTypes.contains(.maxWeight))
    }

    // MARK: - No PR when values don't beat records

    func testNoPRWhenBelowPreviousBest() throws {
        let oldSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 100, reps: 10, setType: .working)
        oldSet.completedAt = Date().addingTimeInterval(-3600)
        modelContext.insert(oldSet)
        try modelContext.save()

        let newSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 80, reps: 5, setType: .working)
        newSet.completedAt = Date()
        modelContext.insert(newSet)
        try modelContext.save()

        let result = service.checkForPR(set: newSet)

        XCTAssertFalse(result.isPR)
        XCTAssertTrue(result.recordTypes.isEmpty)
    }

    // MARK: - Warmup sets excluded

    func testWarmupSetIsNeverPR() throws {
        let set = ExerciseSet(exercise: exercise, workout: workout, weightKg: 200, reps: 1, setType: .warmup)
        set.completedAt = Date()
        modelContext.insert(set)
        try modelContext.save()

        let result = service.checkForPR(set: set)

        XCTAssertFalse(result.isPR)
    }

    // MARK: - Multiple record types

    func testDetectsMultipleRecordTypes() throws {
        let oldSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 60, reps: 3, setType: .working)
        oldSet.completedAt = Date().addingTimeInterval(-3600)
        modelContext.insert(oldSet)
        try modelContext.save()

        // New set beats weight, reps, volume, and e1RM
        let newSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 80, reps: 8, setType: .working)
        newSet.completedAt = Date()
        modelContext.insert(newSet)
        try modelContext.save()

        let result = service.checkForPR(set: newSet)

        XCTAssertTrue(result.isPR)
        XCTAssertGreaterThanOrEqual(result.recordTypes.count, 2)
    }

    // MARK: - Badge text

    func testBadgeTextForE1RM() {
        let result = PRDetectionResult(
            exerciseName: "Squat",
            recordTypes: [.maxE1RM],
            weight: 140,
            reps: 3,
            e1rm: 154
        )
        XCTAssertEqual(result.primaryBadgeText, "New 1RM!")
        XCTAssertTrue(result.detailText.contains("154.0"))
    }

    func testBadgeTextForWeightPR() {
        let result = PRDetectionResult(
            exerciseName: "Bench",
            recordTypes: [.maxWeight],
            weight: 100,
            reps: 5,
            e1rm: 116.7
        )
        XCTAssertEqual(result.primaryBadgeText, "Weight PR!")
    }

    func testBadgeTextForVolumePR() {
        let result = PRDetectionResult(
            exerciseName: "OHP",
            recordTypes: [.maxVolume],
            weight: 60,
            reps: 12,
            e1rm: 84
        )
        XCTAssertEqual(result.primaryBadgeText, "Volume Record!")
    }

    func testBadgeTextForRepPR() {
        let result = PRDetectionResult(
            exerciseName: "Curl",
            recordTypes: [.maxReps],
            weight: 20,
            reps: 20,
            e1rm: 33.3
        )
        XCTAssertEqual(result.primaryBadgeText, "Rep PR!")
    }

    // MARK: - Empty result

    func testEmptyResultHasNoBadgeText() {
        let result = PRDetectionResult(
            exerciseName: "Test",
            recordTypes: [],
            weight: 50,
            reps: 5,
            e1rm: 58.3
        )
        XCTAssertFalse(result.isPR)
        XCTAssertEqual(result.primaryBadgeText, "")
        XCTAssertEqual(result.detailText, "")
    }

    // MARK: - getAllTimePRs

    func testGetAllTimePRsReturnsRecords() throws {
        let set = ExerciseSet(exercise: exercise, workout: workout, weightKg: 100, reps: 5, setType: .working)
        set.completedAt = Date()
        modelContext.insert(set)
        try modelContext.save()

        let records = service.getAllTimePRs(for: exercise)

        XCTAssertFalse(records.isEmpty)
        XCTAssertTrue(records.count <= 4) // max 4 record types
    }

    func testGetAllTimePRsEmptyForNewExercise() throws {
        let newExercise = Exercise(name: "Deadlift", category: .compound, equipment: .barbell)
        modelContext.insert(newExercise)
        try modelContext.save()

        let records = service.getAllTimePRs(for: newExercise)

        XCTAssertTrue(records.isEmpty)
    }

    // MARK: - Priority ordering

    func testRecordTypesAreSortedByPriority() throws {
        let oldSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 40, reps: 2, setType: .working)
        oldSet.completedAt = Date().addingTimeInterval(-3600)
        modelContext.insert(oldSet)
        try modelContext.save()

        let newSet = ExerciseSet(exercise: exercise, workout: workout, weightKg: 100, reps: 10, setType: .working)
        newSet.completedAt = Date()
        modelContext.insert(newSet)
        try modelContext.save()

        let result = service.checkForPR(set: newSet)

        XCTAssertTrue(result.isPR)
        // e1RM should be first (highest priority)
        if let first = result.recordTypes.first {
            XCTAssertEqual(first, .maxE1RM)
        }
    }
}
