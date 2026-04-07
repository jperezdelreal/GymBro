// Re-export WorkoutActivityAttributes from GymBroCore for the widget extension.
// In the Xcode project, add GymBroCore as a dependency of the widget extension target,
// or copy this definition directly. For SPM-based builds, the widget extension should
// depend on the GymBroCore package.

// When building with Xcode, uncomment and remove the local definition below:
// import GymBroCore

// For standalone widget extension compilation, include the attributes directly:
import Foundation
import ActivityKit

/// Defines the static and dynamic data for the workout Live Activity.
/// This is a mirror of the definition in GymBroCore/Models/WorkoutActivityAttributes.swift.
/// Both must stay in sync.
public struct WorkoutActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        public var exerciseName: String
        public var currentSetNumber: Int
        public var totalPlannedSets: Int
        public var restTimerEndDate: Date?
        public var restTimerDuration: Int
        public var completedSets: Int
        public var totalVolume: Double
        public var elapsedSeconds: Int
        public var lastWeight: Double
        public var lastReps: Int

        public init(
            exerciseName: String = "Starting...",
            currentSetNumber: Int = 1,
            totalPlannedSets: Int = 0,
            restTimerEndDate: Date? = nil,
            restTimerDuration: Int = 0,
            completedSets: Int = 0,
            totalVolume: Double = 0,
            elapsedSeconds: Int = 0,
            lastWeight: Double = 0,
            lastReps: Int = 0
        ) {
            self.exerciseName = exerciseName
            self.currentSetNumber = currentSetNumber
            self.totalPlannedSets = totalPlannedSets
            self.restTimerEndDate = restTimerEndDate
            self.restTimerDuration = restTimerDuration
            self.completedSets = completedSets
            self.totalVolume = totalVolume
            self.elapsedSeconds = elapsedSeconds
            self.lastWeight = lastWeight
            self.lastReps = lastReps
        }
    }

    public var workoutId: String
    public var workoutStartDate: Date

    public init(workoutId: String, workoutStartDate: Date = .now) {
        self.workoutId = workoutId
        self.workoutStartDate = workoutStartDate
    }
}

extension WorkoutActivityAttributes.ContentState {
    var restTimerStartDate: Date {
        guard let endDate = restTimerEndDate else { return .now }
        return endDate.addingTimeInterval(-Double(restTimerDuration))
    }
}
