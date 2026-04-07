import Foundation
import ActivityKit

/// Defines the static and dynamic data for the workout Live Activity.
public struct WorkoutActivityAttributes: ActivityAttributes {
    /// Static data set when the Live Activity starts (does not change).
    public struct ContentState: Codable, Hashable {
        // Current exercise info
        public var exerciseName: String
        public var currentSetNumber: Int
        public var totalPlannedSets: Int

        // Rest timer state
        public var restTimerEndDate: Date?
        public var restTimerDuration: Int

        // Workout progress
        public var completedSets: Int
        public var totalVolume: Double
        public var elapsedSeconds: Int

        // Weight/reps for current set display
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

    // Static attributes set at activity creation
    public var workoutId: String
    public var workoutStartDate: Date

    public init(workoutId: String, workoutStartDate: Date = .now) {
        self.workoutId = workoutId
        self.workoutStartDate = workoutStartDate
    }
}
