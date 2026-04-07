import Foundation
import SwiftData

/// Progress state machine for per-exercise plateau tracking.
public enum ProgressState: String, Codable, Sendable {
    case progressing
    case stalling
    case plateaued
    case recovering
}

/// Persisted result of plateau analysis for an exercise.
@Model
public final class PlateauAnalysis {
    public var id: UUID
    public var exerciseId: UUID
    public var exerciseName: String
    public var analyzedAt: Date

    /// Composite score (0.0–1.0). > 0.65 = plateau declared.
    public var compositeScore: Double

    /// Individual method scores
    public var forecastScore: Double
    public var changePointScore: Double
    public var rollingAverageScore: Double

    /// Current state in the progress state machine
    public var progressStateRaw: String

    /// Number of consecutive sessions in current state
    public var sessionsInState: Int

    /// Actionable recommendations
    public var recommendations: [String]

    public init(
        id: UUID = UUID(),
        exerciseId: UUID,
        exerciseName: String,
        compositeScore: Double,
        forecastScore: Double,
        changePointScore: Double,
        rollingAverageScore: Double,
        progressState: ProgressState,
        sessionsInState: Int = 1,
        recommendations: [String] = []
    ) {
        self.id = id
        self.exerciseId = exerciseId
        self.exerciseName = exerciseName
        self.analyzedAt = Date()
        self.compositeScore = compositeScore
        self.forecastScore = forecastScore
        self.changePointScore = changePointScore
        self.rollingAverageScore = rollingAverageScore
        self.progressStateRaw = progressState.rawValue
        self.sessionsInState = sessionsInState
        self.recommendations = recommendations
    }

    public var progressState: ProgressState {
        get { ProgressState(rawValue: progressStateRaw) ?? .progressing }
        set { progressStateRaw = newValue.rawValue }
    }

    public var isPlateaued: Bool {
        compositeScore > 0.65
    }
}
