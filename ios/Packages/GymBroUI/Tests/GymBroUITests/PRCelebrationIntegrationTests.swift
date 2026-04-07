import XCTest
import SwiftData
@testable import GymBroUI
@testable import GymBroCore

@MainActor
final class PRCelebrationIntegrationTests: XCTestCase {
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
            MuscleGroup.self,
            SupersetGroup.self,
            ProgramWeek.self
        ])

        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        let container = try ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(container)

        workout = Workout()
        exercise = Exercise(name: "Squat", category: .compound, equipment: .barbell)

        modelContext.insert(workout)
        modelContext.insert(exercise)
        try modelContext.save()

        viewModel = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: [exercise]
        )
    }

    // MARK: - PR Celebration triggers on first set

    func testFirstSetTriggersPRCelebration() {
        XCTAssertNil(viewModel.activePRCelebration)

        viewModel.updateWeight(100)
        viewModel.updateReps(5)
        viewModel.completeSet()

        XCTAssertNotNil(viewModel.activePRCelebration)
        XCTAssertTrue(viewModel.activePRCelebration!.isPR)
        XCTAssertEqual(viewModel.activePRCelebration!.exerciseName, "Squat")
    }

    // MARK: - PR badge stored for set

    func testPRBadgeStoredForCompletedSet() {
        viewModel.updateWeight(100)
        viewModel.updateReps(5)
        viewModel.completeSet()

        let completedSet = workout.sets.first!
        XCTAssertNotNil(viewModel.prRecordsBySetId[completedSet.id])
        XCTAssertFalse(viewModel.prRecordsBySetId[completedSet.id]!.isEmpty)
    }

    // MARK: - Dismiss celebration

    func testDismissPRCelebration() {
        viewModel.updateWeight(100)
        viewModel.updateReps(5)
        viewModel.completeSet()

        XCTAssertNotNil(viewModel.activePRCelebration)

        viewModel.dismissPRCelebration()

        XCTAssertNil(viewModel.activePRCelebration)
    }

    // MARK: - Non-PR set doesn't trigger celebration

    func testNonPRSetDoesNotTriggerCelebration() {
        // Complete a strong first set
        viewModel.updateWeight(100)
        viewModel.updateReps(10)
        viewModel.completeSet()

        // Dismiss celebration from first set
        viewModel.dismissPRCelebration()
        XCTAssertNil(viewModel.activePRCelebration)

        // Complete a weaker set
        viewModel.updateWeight(60)
        viewModel.updateReps(3)
        viewModel.completeSet()

        // Should not trigger a new celebration
        XCTAssertNil(viewModel.activePRCelebration)
    }

    // MARK: - Non-PR set has no badge

    func testNonPRSetHasNoBadge() {
        viewModel.updateWeight(100)
        viewModel.updateReps(10)
        viewModel.completeSet()
        viewModel.dismissPRCelebration()

        viewModel.updateWeight(60)
        viewModel.updateReps(3)
        viewModel.completeSet()

        let secondSet = workout.sets.sorted { ($0.completedAt ?? .distantPast) < ($1.completedAt ?? .distantPast) }.last!
        XCTAssertNil(viewModel.prRecordsBySetId[secondSet.id])
    }

    // MARK: - Warmup set never triggers celebration

    func testWarmupSetNeverTriggersCelebration() {
        viewModel.toggleWarmup()
        viewModel.updateWeight(200)
        viewModel.updateReps(1)
        viewModel.completeSet()

        XCTAssertNil(viewModel.activePRCelebration)
    }

    // MARK: - getAllTimePRs returns data

    func testGetAllTimePRsAfterSets() {
        viewModel.updateWeight(100)
        viewModel.updateReps(5)
        viewModel.completeSet()

        let records = viewModel.getAllTimePRs()
        XCTAssertFalse(records.isEmpty)
    }

    // MARK: - Multiple PRs track correctly

    func testMultiplePRsTrackedAcrossSets() {
        // First set — always a PR
        viewModel.updateWeight(80)
        viewModel.updateReps(5)
        viewModel.completeSet()
        viewModel.dismissPRCelebration()

        let firstSetId = workout.sets.first!.id
        XCTAssertNotNil(viewModel.prRecordsBySetId[firstSetId])

        // Second set — heavier, should also be a PR
        viewModel.updateWeight(100)
        viewModel.updateReps(5)
        viewModel.completeSet()

        XCTAssertNotNil(viewModel.activePRCelebration)
        let secondSetId = workout.sets.sorted { ($0.completedAt ?? .distantPast) < ($1.completedAt ?? .distantPast) }.last!.id
        XCTAssertNotNil(viewModel.prRecordsBySetId[secondSetId])

        // Both sets should have badges
        XCTAssertEqual(viewModel.prRecordsBySetId.count, 2)
    }
}
