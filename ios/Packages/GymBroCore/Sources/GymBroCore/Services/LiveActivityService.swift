import Foundation
import ActivityKit
import os

/// Manages the Live Activity lifecycle for active workout sessions.
/// Handles starting, updating, and ending the Live Activity + Dynamic Island.
@MainActor
@Observable
public final class LiveActivityService {
    public static let shared = LiveActivityService()
    private static let logger = Logger(subsystem: "com.gymbro", category: "LiveActivity")

    public private(set) var isActivityActive: Bool = false
    private var currentActivity: Activity<WorkoutActivityAttributes>?

    private init() {}

    // MARK: - Start

    /// Starts a Live Activity when a workout begins.
    public func startWorkoutActivity(workoutId: String) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            Self.logger.info("Live Activities are disabled by the user.")
            return
        }

        let attributes = WorkoutActivityAttributes(
            workoutId: workoutId,
            workoutStartDate: .now
        )

        let initialState = WorkoutActivityAttributes.ContentState()

        do {
            let activity = try Activity.request(
                attributes: attributes,
                content: .init(state: initialState, staleDate: nil),
                pushType: nil
            )
            currentActivity = activity
            isActivityActive = true
            Self.logger.info("Live Activity started: \(activity.id)")
        } catch {
            Self.logger.error("Failed to start Live Activity: \(error.localizedDescription)")
        }
    }

    // MARK: - Update

    /// Updates the Live Activity with new workout state (set completion, exercise change).
    public func updateWorkoutActivity(
        exerciseName: String,
        currentSetNumber: Int,
        totalPlannedSets: Int = 0,
        completedSets: Int,
        totalVolume: Double,
        elapsedSeconds: Int,
        lastWeight: Double,
        lastReps: Int
    ) {
        guard let activity = currentActivity else { return }

        let updatedState = WorkoutActivityAttributes.ContentState(
            exerciseName: exerciseName,
            currentSetNumber: currentSetNumber,
            totalPlannedSets: totalPlannedSets,
            restTimerEndDate: nil,
            restTimerDuration: 0,
            completedSets: completedSets,
            totalVolume: totalVolume,
            elapsedSeconds: elapsedSeconds,
            lastWeight: lastWeight,
            lastReps: lastReps
        )

        Task {
            await activity.update(.init(state: updatedState, staleDate: nil))
            Self.logger.debug("Live Activity updated: set \(currentSetNumber)")
        }
    }

    /// Updates the Live Activity with rest timer countdown.
    public func updateRestTimer(
        exerciseName: String,
        currentSetNumber: Int,
        totalPlannedSets: Int = 0,
        restDuration: Int,
        completedSets: Int,
        totalVolume: Double,
        elapsedSeconds: Int,
        lastWeight: Double,
        lastReps: Int
    ) {
        guard let activity = currentActivity else { return }

        let restEndDate = Date().addingTimeInterval(Double(restDuration))

        let updatedState = WorkoutActivityAttributes.ContentState(
            exerciseName: exerciseName,
            currentSetNumber: currentSetNumber,
            totalPlannedSets: totalPlannedSets,
            restTimerEndDate: restEndDate,
            restTimerDuration: restDuration,
            completedSets: completedSets,
            totalVolume: totalVolume,
            elapsedSeconds: elapsedSeconds,
            lastWeight: lastWeight,
            lastReps: lastReps
        )

        Task {
            await activity.update(.init(state: updatedState, staleDate: restEndDate))
            Self.logger.debug("Live Activity rest timer updated: \(restDuration)s")
        }
    }

    /// Clears the rest timer from the Live Activity (timer completed or skipped).
    public func clearRestTimer(
        exerciseName: String,
        currentSetNumber: Int,
        totalPlannedSets: Int = 0,
        completedSets: Int,
        totalVolume: Double,
        elapsedSeconds: Int,
        lastWeight: Double,
        lastReps: Int
    ) {
        updateWorkoutActivity(
            exerciseName: exerciseName,
            currentSetNumber: currentSetNumber,
            totalPlannedSets: totalPlannedSets,
            completedSets: completedSets,
            totalVolume: totalVolume,
            elapsedSeconds: elapsedSeconds,
            lastWeight: lastWeight,
            lastReps: lastReps
        )
    }

    // MARK: - End

    /// Ends the Live Activity when workout finishes, showing a brief summary.
    public func endWorkoutActivity(
        totalSets: Int,
        totalVolume: Double,
        durationSeconds: Int
    ) {
        guard let activity = currentActivity else { return }

        let finalState = WorkoutActivityAttributes.ContentState(
            exerciseName: "Workout Complete 🎉",
            currentSetNumber: 0,
            totalPlannedSets: 0,
            restTimerEndDate: nil,
            restTimerDuration: 0,
            completedSets: totalSets,
            totalVolume: totalVolume,
            elapsedSeconds: durationSeconds,
            lastWeight: 0,
            lastReps: 0
        )

        Task {
            await activity.end(
                .init(state: finalState, staleDate: nil),
                dismissalPolicy: .after(.now.addingTimeInterval(30))
            )
            Self.logger.info("Live Activity ended after \(durationSeconds)s")
        }

        currentActivity = nil
        isActivityActive = false
    }

    /// Immediately dismisses the Live Activity (workout cancelled).
    public func dismissWorkoutActivity() {
        guard let activity = currentActivity else { return }

        Task {
            await activity.end(nil, dismissalPolicy: .immediate)
            Self.logger.info("Live Activity dismissed")
        }

        currentActivity = nil
        isActivityActive = false
    }
}
