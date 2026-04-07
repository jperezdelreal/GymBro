import Foundation
import Observation
import SwiftData
import GymBroCore

/// ViewModel driving the Recovery Dashboard — aggregates readiness, muscle recovery,
/// anomaly detection, workout adjustments, and push/pull balance.
@MainActor
@Observable
public final class RecoveryDashboardViewModel {

    // MARK: - Published State

    public var readinessScore: ReadinessScore?
    public var readinessTrend: [ReadinessScore] = []
    public var muscleRecoveryMap: [String: MuscleRecoveryStatus] = [:]
    public var anomalies: [ReadinessAnomaly] = []
    public var workoutRecommendation: WorkoutRecommendation?
    public var imbalanceAnalysis: MuscleImbalanceAnalysis?
    public var isLoading = false

    /// Anomaly IDs the user has dismissed this session.
    public var dismissedAnomalyMessages: Set<String> = []

    /// Whether the user chose to override the workout adjustment.
    public var didOverrideAdjustment = false

    // MARK: - Services

    private let recoveryService: MuscleRecoveryService
    private let anomalyDetector: ReadinessAnomalyDetector
    private let programIntegration: ReadinessProgramIntegration
    private let imbalanceService: MuscleImbalanceService

    public init(
        recoveryService: MuscleRecoveryService = MuscleRecoveryService(),
        anomalyDetector: ReadinessAnomalyDetector = ReadinessAnomalyDetector(),
        programIntegration: ReadinessProgramIntegration = ReadinessProgramIntegration(),
        imbalanceService: MuscleImbalanceService = MuscleImbalanceService()
    ) {
        self.recoveryService = recoveryService
        self.anomalyDetector = anomalyDetector
        self.programIntegration = programIntegration
        self.imbalanceService = imbalanceService
    }

    // MARK: - Data Loading

    /// Load all recovery data from SwiftData.
    public func loadData(modelContext: ModelContext) {
        isLoading = true
        defer { isLoading = false }

        // Fetch recent readiness scores (14 days)
        let fourteenDaysAgo = Calendar.current.date(byAdding: .day, value: -14, to: Date()) ?? Date()
        var scoreDescriptor = FetchDescriptor<ReadinessScore>(
            predicate: #Predicate { $0.date >= fourteenDaysAgo },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        scoreDescriptor.fetchLimit = 14
        let scores = (try? modelContext.fetch(scoreDescriptor)) ?? []

        readinessScore = scores.first
        readinessTrend = scores.reversed()

        // Fetch recent workouts (14 days)
        var workoutDescriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.date >= fourteenDaysAgo },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        workoutDescriptor.fetchLimit = 50
        let recentWorkouts = (try? modelContext.fetch(workoutDescriptor)) ?? []

        // Muscle recovery heat map
        muscleRecoveryMap = recoveryService.calculateRecoveryMap(workouts: recentWorkouts)

        // Anomaly detection
        anomalies = anomalyDetector.detect(scores: scores)

        // Muscle imbalance (needs 2+ weeks of data — may return nil)
        let thirtyDaysAgo = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
        var longDescriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.date >= thirtyDaysAgo },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        longDescriptor.fetchLimit = 100
        let monthWorkouts = (try? modelContext.fetch(longDescriptor)) ?? []
        imbalanceAnalysis = imbalanceService.analyze(workouts: monthWorkouts)
    }

    /// Evaluate workout adjustment for a given program day.
    public func evaluateWorkoutAdjustment(programDay: ProgramDayInfo) {
        guard let score = readinessScore else { return }
        didOverrideAdjustment = false
        workoutRecommendation = programIntegration.adjustWorkout(
            programDay: programDay,
            readiness: score,
            muscleRecovery: muscleRecoveryMap
        )
    }

    // MARK: - Actions

    public func dismissAnomaly(_ anomaly: ReadinessAnomaly) {
        dismissedAnomalyMessages.insert(anomaly.message)
    }

    public var activeAnomalies: [ReadinessAnomaly] {
        anomalies.filter { !dismissedAnomalyMessages.contains($0.message) }
    }

    public func overrideAdjustment() {
        didOverrideAdjustment = true
    }

    // MARK: - Computed

    public var pushPullRatio: Double? {
        imbalanceAnalysis?.pushPullRatio
    }

    public var pushPullStatus: String {
        guard let ratio = pushPullRatio else { return "No data" }
        if ratio >= 0.8 && ratio <= 1.2 { return "Balanced" }
        if ratio > 1.2 { return "Push dominant" }
        return "Pull dominant"
    }
}
