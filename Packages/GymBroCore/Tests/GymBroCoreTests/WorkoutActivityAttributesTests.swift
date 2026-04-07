import Testing
import Foundation
@testable import GymBroCore

@Suite("WorkoutActivityAttributes Tests")
struct WorkoutActivityAttributesTests {

    @Test("Default ContentState has sensible defaults")
    func defaultContentState() {
        let state = WorkoutActivityAttributes.ContentState()
        #expect(state.exerciseName == "Starting...")
        #expect(state.currentSetNumber == 1)
        #expect(state.totalPlannedSets == 0)
        #expect(state.restTimerEndDate == nil)
        #expect(state.restTimerDuration == 0)
        #expect(state.completedSets == 0)
        #expect(state.totalVolume == 0)
        #expect(state.elapsedSeconds == 0)
        #expect(state.lastWeight == 0)
        #expect(state.lastReps == 0)
    }

    @Test("ContentState initializes with custom values")
    func customContentState() {
        let endDate = Date().addingTimeInterval(120)
        let state = WorkoutActivityAttributes.ContentState(
            exerciseName: "Bench Press",
            currentSetNumber: 3,
            totalPlannedSets: 5,
            restTimerEndDate: endDate,
            restTimerDuration: 120,
            completedSets: 2,
            totalVolume: 500,
            elapsedSeconds: 600,
            lastWeight: 100,
            lastReps: 5
        )

        #expect(state.exerciseName == "Bench Press")
        #expect(state.currentSetNumber == 3)
        #expect(state.totalPlannedSets == 5)
        #expect(state.restTimerEndDate == endDate)
        #expect(state.restTimerDuration == 120)
        #expect(state.completedSets == 2)
        #expect(state.totalVolume == 500)
        #expect(state.elapsedSeconds == 600)
        #expect(state.lastWeight == 100)
        #expect(state.lastReps == 5)
    }

    @Test("Attributes store workout ID and start date")
    func attributesInit() {
        let startDate = Date()
        let attrs = WorkoutActivityAttributes(workoutId: "test-123", workoutStartDate: startDate)
        #expect(attrs.workoutId == "test-123")
        #expect(attrs.workoutStartDate == startDate)
    }

    @Test("ContentState conforms to Codable")
    func contentStateCodable() throws {
        let original = WorkoutActivityAttributes.ContentState(
            exerciseName: "Squat",
            currentSetNumber: 2,
            totalPlannedSets: 4,
            completedSets: 1,
            totalVolume: 300,
            elapsedSeconds: 420,
            lastWeight: 140,
            lastReps: 5
        )

        let encoder = JSONEncoder()
        let data = try encoder.encode(original)
        let decoder = JSONDecoder()
        let decoded = try decoder.decode(WorkoutActivityAttributes.ContentState.self, from: data)

        #expect(decoded.exerciseName == original.exerciseName)
        #expect(decoded.currentSetNumber == original.currentSetNumber)
        #expect(decoded.totalPlannedSets == original.totalPlannedSets)
        #expect(decoded.completedSets == original.completedSets)
        #expect(decoded.totalVolume == original.totalVolume)
        #expect(decoded.lastWeight == original.lastWeight)
        #expect(decoded.lastReps == original.lastReps)
    }

    @Test("ContentState conforms to Hashable")
    func contentStateHashable() {
        let state1 = WorkoutActivityAttributes.ContentState(exerciseName: "Bench", currentSetNumber: 1)
        let state2 = WorkoutActivityAttributes.ContentState(exerciseName: "Bench", currentSetNumber: 1)
        let state3 = WorkoutActivityAttributes.ContentState(exerciseName: "Squat", currentSetNumber: 2)

        #expect(state1 == state2)
        #expect(state1 != state3)
    }

    @Test("LiveActivityService singleton exists")
    @MainActor
    func singletonExists() {
        let service = LiveActivityService.shared
        #expect(service.isActivityActive == false)
    }
}
