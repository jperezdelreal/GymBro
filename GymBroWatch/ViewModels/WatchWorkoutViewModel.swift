import Foundation
import WatchKit
import Observation
import GymBroCore

/// ViewModel for the Watch workout experience.
/// Manages set logging state, Digital Crown weight adjustment,
/// rest timer countdown, and haptic feedback.
@MainActor
@Observable
final class WatchWorkoutViewModel {
    // MARK: - Workout State

    private(set) var workoutId: String
    private(set) var exerciseName: String
    private(set) var exerciseCategory: String
    private(set) var setNumber: Int
    var weight: Double
    var reps: Int
    private(set) var totalSetsCompleted: Int
    private(set) var totalVolume: Double
    private(set) var workoutStartTime: Date

    // MARK: - Rest Timer

    private(set) var isRestTimerActive: Bool = false
    private(set) var restTimerRemaining: Int = 0
    private(set) var restTimerTotal: Int = 0

    // MARK: - UI State

    private(set) var lastCompletedWeight: Double?
    private(set) var lastCompletedReps: Int?

    /// Digital Crown delta accumulator for weight adjustment.
    var crownDelta: Double = 0

    private var restTimerTask: Task<Void, Never>?
    private let connectivityService = WatchConnectivityService.shared

    // Weight step size (kg)
    private let weightStep: Double = 2.5

    // MARK: - Init

    init(state: WatchWorkoutState) {
        self.workoutId = state.workoutId
        self.exerciseName = state.exerciseName
        self.exerciseCategory = state.exerciseCategory
        self.setNumber = state.setNumber
        self.weight = state.targetWeight
        self.reps = state.targetReps
        self.totalSetsCompleted = state.totalSetsCompleted
        self.totalVolume = state.totalVolume
        self.workoutStartTime = state.workoutStartTime

        observeConnectivityUpdates()
    }

    // MARK: - Actions

    func completeSet() {
        let completion = WatchSetCompletion(
            workoutId: workoutId,
            weightKg: weight,
            reps: reps
        )

        connectivityService.sendSetCompletion(completion)

        // Haptic confirmation
        WKInterfaceDevice.current().play(.success)

        lastCompletedWeight = weight
        lastCompletedReps = reps

        totalSetsCompleted += 1
        totalVolume += weight * Double(reps)
        setNumber += 1

        startRestTimer()
    }

    func incrementWeight() {
        weight = min(999, weight + weightStep)
        WKInterfaceDevice.current().play(.click)
    }

    func decrementWeight() {
        weight = max(0, weight - weightStep)
        WKInterfaceDevice.current().play(.click)
    }

    func incrementReps() {
        reps = min(999, reps + 1)
        WKInterfaceDevice.current().play(.click)
    }

    func decrementReps() {
        reps = max(0, reps - 1)
        WKInterfaceDevice.current().play(.click)
    }

    /// Apply Digital Crown rotation to weight.
    func applyCrownRotation(_ delta: Double) {
        crownDelta += delta
        // Snap to weight steps
        if abs(crownDelta) >= 1.0 {
            let steps = Int(crownDelta)
            weight = max(0, min(999, weight + Double(steps) * weightStep))
            crownDelta -= Double(steps)
            WKInterfaceDevice.current().play(.click)
        }
    }

    // MARK: - Rest Timer

    func startRestTimer() {
        let duration = defaultRestDuration()
        restTimerTotal = duration
        restTimerRemaining = duration
        isRestTimerActive = true

        restTimerTask?.cancel()
        restTimerTask = Task { @MainActor in
            while restTimerRemaining > 0 && !Task.isCancelled {
                try? await Task.sleep(for: .seconds(1))
                guard !Task.isCancelled else { return }
                restTimerRemaining -= 1

                if restTimerRemaining == 10 {
                    WKInterfaceDevice.current().play(.notification)
                }
            }

            if !Task.isCancelled {
                // Timer complete — strong haptic
                WKInterfaceDevice.current().play(.success)
                WKInterfaceDevice.current().play(.success)
                isRestTimerActive = false
            }
        }
    }

    func skipRestTimer() {
        restTimerTask?.cancel()
        restTimerTask = nil
        isRestTimerActive = false
        restTimerRemaining = 0
    }

    // MARK: - Helpers

    var workoutDuration: TimeInterval {
        Date().timeIntervalSince(workoutStartTime)
    }

    var formattedDuration: String {
        let total = Int(workoutDuration)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }

    var restTimerProgress: Double {
        guard restTimerTotal > 0 else { return 0 }
        return Double(restTimerTotal - restTimerRemaining) / Double(restTimerTotal)
    }

    var formattedRestTime: String {
        let minutes = restTimerRemaining / 60
        let seconds = restTimerRemaining % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    private func defaultRestDuration() -> Int {
        switch exerciseCategory {
        case "compound": return 180
        case "isolation": return 90
        case "accessory": return 60
        default: return 120
        }
    }

    // MARK: - Connectivity Observation

    private func observeConnectivityUpdates() {
        Task { @MainActor in
            // Poll for state updates from connectivity service
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(1))

                if let state = connectivityService.currentWorkoutState,
                   state.workoutId == workoutId {
                    exerciseName = state.exerciseName
                    exerciseCategory = state.exerciseCategory
                    if state.setNumber != setNumber {
                        setNumber = state.setNumber
                        weight = state.targetWeight
                        reps = state.targetReps
                    }
                }

                if let timerState = connectivityService.currentRestTimerState {
                    if timerState.isActive && !isRestTimerActive {
                        restTimerTotal = timerState.totalSeconds
                        restTimerRemaining = timerState.remainingSeconds
                        isRestTimerActive = true
                    } else if !timerState.isActive && isRestTimerActive {
                        skipRestTimer()
                    }
                }
            }
        }
    }
}
