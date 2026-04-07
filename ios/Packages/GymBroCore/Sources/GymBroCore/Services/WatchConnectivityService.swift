import Foundation
import WatchConnectivity
import os

// MARK: - Watch ↔ Phone Data Transfer Types

/// Lightweight workout state transferred from iPhone to Watch.
/// Uses only Codable primitives — no SwiftData dependency on Watch side.
public struct WatchWorkoutState: Codable, Equatable, Sendable {
    public let workoutId: String
    public let exerciseName: String
    public let exerciseCategory: String
    public let setNumber: Int
    public let targetWeight: Double
    public let targetReps: Int
    public let totalSetsCompleted: Int
    public let totalVolume: Double
    public let workoutStartTime: Date
    public let isActive: Bool

    public init(
        workoutId: String,
        exerciseName: String,
        exerciseCategory: String,
        setNumber: Int,
        targetWeight: Double,
        targetReps: Int,
        totalSetsCompleted: Int,
        totalVolume: Double,
        workoutStartTime: Date,
        isActive: Bool
    ) {
        self.workoutId = workoutId
        self.exerciseName = exerciseName
        self.exerciseCategory = exerciseCategory
        self.setNumber = setNumber
        self.targetWeight = targetWeight
        self.targetReps = targetReps
        self.totalSetsCompleted = totalSetsCompleted
        self.totalVolume = totalVolume
        self.workoutStartTime = workoutStartTime
        self.isActive = isActive
    }
}

/// Set completion sent from Watch back to iPhone.
public struct WatchSetCompletion: Codable, Equatable, Sendable {
    public let workoutId: String
    public let weightKg: Double
    public let reps: Int
    public let completedAt: Date

    public init(workoutId: String, weightKg: Double, reps: Int, completedAt: Date = Date()) {
        self.workoutId = workoutId
        self.weightKg = weightKg
        self.reps = reps
        self.completedAt = completedAt
    }
}

/// Rest timer state synced between devices.
public struct WatchRestTimerState: Codable, Equatable, Sendable {
    public let isActive: Bool
    public let remainingSeconds: Int
    public let totalSeconds: Int

    public init(isActive: Bool, remainingSeconds: Int, totalSeconds: Int) {
        self.isActive = isActive
        self.remainingSeconds = remainingSeconds
        self.totalSeconds = totalSeconds
    }
}

// MARK: - Message Keys

public enum WatchMessageKey {
    public static let workoutState = "workoutState"
    public static let setCompletion = "setCompletion"
    public static let restTimerState = "restTimerState"
    public static let workoutEnded = "workoutEnded"
}

// MARK: - WatchConnectivityService

/// Bidirectional communication layer between iPhone and Apple Watch.
/// Runs on both iPhone and Watch — each side registers as delegate.
@Observable
@MainActor
public final class WatchConnectivityService: NSObject {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WatchConnectivity")

    public static let shared = WatchConnectivityService()

    // MARK: - Published State

    /// Current workout state (populated on Watch from iPhone messages).
    public private(set) var currentWorkoutState: WatchWorkoutState?

    /// Current rest timer state.
    public private(set) var currentRestTimerState: WatchRestTimerState?

    /// Whether the counterpart device is reachable.
    public private(set) var isReachable: Bool = false

    /// Pending set completions received (consumed by iPhone side).
    public private(set) var pendingSetCompletions: [WatchSetCompletion] = []

    /// Whether a workout just ended (signaled from phone).
    public private(set) var workoutDidEnd: Bool = false

    private var session: WCSession?

    private override init() {
        super.init()
    }

    // MARK: - Activation

    public func activate() {
        guard WCSession.isSupported() else {
            Self.logger.info("WCSession not supported on this device")
            return
        }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        self.session = session
        Self.logger.info("WCSession activation requested")
    }

    // MARK: - Sending (iPhone → Watch)

    /// Send workout state update to the Watch. Uses `updateApplicationContext`
    /// so the Watch always gets the latest state even if not reachable.
    public func sendWorkoutState(_ state: WatchWorkoutState) {
        guard let session, session.activationState == .activated else { return }

        do {
            let data = try JSONEncoder().encode(state)
            let context: [String: Any] = [WatchMessageKey.workoutState: data]
            try session.updateApplicationContext(context)
            Self.logger.debug("Sent workout state to Watch")
        } catch {
            Self.logger.error("Failed to send workout state: \(error.localizedDescription)")
        }
    }

    /// Send rest timer state to the Watch via message (real-time).
    public func sendRestTimerState(_ state: WatchRestTimerState) {
        guard let session, session.isReachable else { return }

        do {
            let data = try JSONEncoder().encode(state)
            session.sendMessage(
                [WatchMessageKey.restTimerState: data],
                replyHandler: nil
            ) { error in
                Self.logger.error("Failed to send rest timer state: \(error.localizedDescription)")
            }
        } catch {
            Self.logger.error("Failed to encode rest timer state: \(error.localizedDescription)")
        }
    }

    /// Signal to Watch that the workout has ended.
    public func sendWorkoutEnded() {
        guard let session, session.activationState == .activated else { return }

        do {
            try session.updateApplicationContext([WatchMessageKey.workoutEnded: true])
            Self.logger.debug("Sent workout ended to Watch")
        } catch {
            Self.logger.error("Failed to send workout ended: \(error.localizedDescription)")
        }
    }

    // MARK: - Sending (Watch → iPhone)

    /// Send a completed set from Watch to iPhone.
    public func sendSetCompletion(_ completion: WatchSetCompletion) {
        guard let session, session.activationState == .activated else { return }

        do {
            let data = try JSONEncoder().encode(completion)
            if session.isReachable {
                session.sendMessage(
                    [WatchMessageKey.setCompletion: data],
                    replyHandler: nil
                ) { error in
                    Self.logger.error("Failed to send set completion: \(error.localizedDescription)")
                }
            } else {
                // Queue for delivery when phone becomes reachable
                session.transferUserInfo([WatchMessageKey.setCompletion: data])
            }
            Self.logger.debug("Sent set completion to iPhone")
        } catch {
            Self.logger.error("Failed to encode set completion: \(error.localizedDescription)")
        }
    }

    // MARK: - Consuming

    /// Consume and clear pending set completions (called by iPhone after processing).
    public func consumeSetCompletions() -> [WatchSetCompletion] {
        let completions = pendingSetCompletions
        pendingSetCompletions.removeAll()
        return completions
    }

    /// Reset workout ended flag.
    public func clearWorkoutEnded() {
        workoutDidEnd = false
    }
}

// MARK: - WCSessionDelegate

extension WatchConnectivityService: WCSessionDelegate {
    nonisolated public func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        Task { @MainActor in
            isReachable = session.isReachable
            if let error {
                Self.logger.error("WCSession activation failed: \(error.localizedDescription)")
            } else {
                Self.logger.info("WCSession activated: \(activationState.rawValue)")
            }
        }
    }

    #if os(iOS)
    nonisolated public func sessionDidBecomeInactive(_ session: WCSession) {
        Self.logger.info("WCSession became inactive")
    }

    nonisolated public func sessionDidDeactivate(_ session: WCSession) {
        Self.logger.info("WCSession deactivated — reactivating")
        session.activate()
    }
    #endif

    nonisolated public func sessionReachabilityDidChange(_ session: WCSession) {
        Task { @MainActor in
            isReachable = session.isReachable
        }
    }

    // Application context (latest-state delivery)
    nonisolated public func session(
        _ session: WCSession,
        didReceiveApplicationContext applicationContext: [String: Any]
    ) {
        Task { @MainActor in
            processIncoming(applicationContext)
        }
    }

    // Real-time messages
    nonisolated public func session(
        _ session: WCSession,
        didReceiveMessage message: [String: Any]
    ) {
        Task { @MainActor in
            processIncoming(message)
        }
    }

    // Queued user info (offline transfers)
    nonisolated public func session(
        _ session: WCSession,
        didReceiveUserInfo userInfo: [String: Any] = [:]
    ) {
        Task { @MainActor in
            processIncoming(userInfo)
        }
    }

    // MARK: - Private

    @MainActor
    private func processIncoming(_ payload: [String: Any]) {
        let decoder = JSONDecoder()

        if let data = payload[WatchMessageKey.workoutState] as? Data,
           let state = try? decoder.decode(WatchWorkoutState.self, from: data) {
            currentWorkoutState = state
            workoutDidEnd = false
            Self.logger.debug("Received workout state: \(state.exerciseName)")
        }

        if let data = payload[WatchMessageKey.setCompletion] as? Data,
           let completion = try? decoder.decode(WatchSetCompletion.self, from: data) {
            pendingSetCompletions.append(completion)
            Self.logger.debug("Received set completion: \(completion.weightKg)kg x \(completion.reps)")
        }

        if let data = payload[WatchMessageKey.restTimerState] as? Data,
           let state = try? decoder.decode(WatchRestTimerState.self, from: data) {
            currentRestTimerState = state
            Self.logger.debug("Received rest timer state: \(state.remainingSeconds)s")
        }

        if let ended = payload[WatchMessageKey.workoutEnded] as? Bool, ended {
            workoutDidEnd = true
            currentWorkoutState = nil
            currentRestTimerState = nil
            Self.logger.debug("Received workout ended signal")
        }
    }
}
