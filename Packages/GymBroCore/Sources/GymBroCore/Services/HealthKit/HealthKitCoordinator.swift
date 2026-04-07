import Foundation
import SwiftData
import os

/// Orchestrates the full HealthKit → SwiftData → ReadinessScore pipeline.
///
/// This is the missing glue: HealthKitManager, HealthKitDataSync, and ReadinessScoreService
/// all existed but nothing connected them at runtime. HealthKitCoordinator wires the pipeline
/// so recovery scores use real biometric data instead of running on empty inputs.
///
/// Graceful degradation: if HealthKit is unavailable or the user denies permissions,
/// readiness still calculates using training-load-only mode (weight redistribution).
public final class HealthKitCoordinator: @unchecked Sendable {
    private static let logger = Logger(subsystem: "com.gymbro", category: "HealthKitCoordinator")

    private let healthKitService: HealthKitService
    private let readinessService: ReadinessScoreService
    private let baselineWindowDays: Int

    /// Whether HealthKit authorization has been requested this session.
    public private(set) var hasRequestedAuthorization: Bool = false

    /// Whether the last authorization attempt succeeded (no throw).
    public private(set) var authorizationGranted: Bool = false

    public init(
        healthKitService: HealthKitService,
        readinessService: ReadinessScoreService = ReadinessScoreService(),
        baselineWindowDays: Int = 30
    ) {
        self.healthKitService = healthKitService
        self.readinessService = readinessService
        self.baselineWindowDays = baselineWindowDays
    }

    // MARK: - Authorization

    /// Request HealthKit authorization. Safe to call multiple times.
    /// Returns true if authorization succeeded, false if denied or unavailable.
    public func requestAuthorization() async -> Bool {
        guard healthKitService.isAvailable else {
            Self.logger.info("HealthKit not available on this device — training-load-only mode")
            return false
        }

        do {
            try await healthKitService.requestAuthorization()
            hasRequestedAuthorization = true
            authorizationGranted = true
            Self.logger.info("HealthKit authorization granted")
            return true
        } catch {
            hasRequestedAuthorization = true
            authorizationGranted = false
            Self.logger.warning("HealthKit authorization failed: \(error.localizedDescription)")
            return false
        }
    }

    // MARK: - Full Sync Pipeline

    /// Full pipeline: sync HealthKit data → update baselines → calculate readiness score.
    ///
    /// Safe to call even if HealthKit is unavailable — degrades to training-load-only mode.
    /// The calculated ReadinessScore is persisted to SwiftData for the dashboard to read.
    public func syncAndCalculateReadiness(modelContext: ModelContext) async {
        let dataSync = HealthKitDataSync(
            service: healthKitService,
            modelContext: modelContext,
            baselineWindowDays: baselineWindowDays
        )

        // Step 1: Sync HealthKit → SwiftData cache (no-op if unavailable)
        await dataSync.syncAll()

        // Step 2: Build ReadinessScoreService.Input from cached data
        let input = buildInputFromCache(dataSync: dataSync, modelContext: modelContext)

        // Step 3: Calculate and save today's readiness score
        let today = Calendar.current.startOfDay(for: Date())
        let score = readinessService.calculate(from: input, date: today)
        saveReadinessScore(score, for: today, modelContext: modelContext)

        Self.logger.info(
            "Readiness pipeline complete: score=\(score.overallScore, privacy: .public) label=\(score.label.rawValue)"
        )
    }

    /// Calculate readiness from cached SwiftData data only — no HealthKit queries.
    /// Use this for quick recalculations (e.g., after subjective check-in).
    public func calculateFromCache(modelContext: ModelContext) -> ReadinessScore {
        let dataSync = HealthKitDataSync(
            service: healthKitService,
            modelContext: modelContext,
            baselineWindowDays: baselineWindowDays
        )

        let input = buildInputFromCache(dataSync: dataSync, modelContext: modelContext)
        let today = Calendar.current.startOfDay(for: Date())
        let score = readinessService.calculate(from: input, date: today)
        saveReadinessScore(score, for: today, modelContext: modelContext)
        return score
    }

    /// Enable background delivery for HealthKit data updates.
    public func enableBackgroundSync() async {
        guard healthKitService.isAvailable else { return }
        do {
            try await healthKitService.enableBackgroundDelivery()
            Self.logger.info("Background HealthKit delivery enabled")
        } catch {
            Self.logger.warning("Failed to enable background delivery: \(error.localizedDescription)")
        }
    }

    // MARK: - Input Assembly

    /// Build ReadinessScoreService.Input from SwiftData-cached health metrics and baselines.
    func buildInputFromCache(
        dataSync: HealthKitDataSync,
        modelContext: ModelContext
    ) -> ReadinessScoreService.Input {
        // Latest sleep record
        let sleepMetrics = dataSync.getCachedMetrics(type: .sleepDuration, limit: 7)
        let latestSleep = sleepMetrics.first.flatMap { metric -> SleepRecord? in
            let stages: SleepStageBreakdown
            if let metadata = metric.metadata,
               let data = metadata.data(using: .utf8),
               let decoded = try? JSONDecoder().decode(SleepStageBreakdown.self, from: data) {
                stages = decoded
            } else {
                stages = SleepStageBreakdown(asleepMinutes: metric.value)
            }
            return SleepRecord(date: metric.date, totalMinutes: metric.value, stages: stages)
        }

        let recentSleepDurations = sleepMetrics.map(\.value)

        // Latest HRV
        let hrvMetrics = dataSync.getCachedMetrics(type: .heartRateVariability, limit: 1)
        let currentHRV = hrvMetrics.first?.value
        let hrvBaseline = dataSync.getBaseline(for: .heartRateVariability)

        // Latest resting HR
        let rhrMetrics = dataSync.getCachedMetrics(type: .restingHeartRate, limit: 1)
        let currentRHR = rhrMetrics.first?.value
        let rhrBaseline = dataSync.getBaseline(for: .restingHeartRate)

        // Training volumes from recent workouts
        let dailyVolumes = fetchDailyTrainingVolumes(modelContext: modelContext)

        // Today's subjective check-in (if any)
        let checkIn = fetchTodaysCheckIn(modelContext: modelContext)

        return ReadinessScoreService.Input(
            sleepRecord: latestSleep,
            recentSleepDurations: recentSleepDurations,
            currentHRV: currentHRV,
            hrvBaseline: hrvBaseline,
            currentRestingHR: currentRHR,
            rhrBaseline: rhrBaseline,
            dailyTrainingVolumes: dailyVolumes,
            subjectiveCheckIn: checkIn
        )
    }

    // MARK: - SwiftData Queries

    private func fetchDailyTrainingVolumes(modelContext: ModelContext) -> [Double] {
        let twentyEightDaysAgo = Calendar.current.date(
            byAdding: .day, value: -28, to: Date()
        ) ?? Date()

        var descriptor = FetchDescriptor<Workout>(
            predicate: #Predicate { $0.date >= twentyEightDaysAgo },
            sortBy: [SortDescriptor(\.date, order: .forward)]
        )
        descriptor.fetchLimit = 100

        guard let workouts = try? modelContext.fetch(descriptor) else {
            return []
        }

        // Group by day and sum tonnage (weight × reps per set)
        let calendar = Calendar.current
        var dailyVolumes: [Date: Double] = [:]

        for workout in workouts {
            let day = calendar.startOfDay(for: workout.date)
            let volume = workout.sets.reduce(0.0) { $0 + $1.volume }
            dailyVolumes[day, default: 0] += volume
        }

        // Fill missing days with 0 (rest days)
        var result: [Double] = []
        var current = calendar.startOfDay(for: twentyEightDaysAgo)
        let today = calendar.startOfDay(for: Date())

        while current <= today {
            result.append(dailyVolumes[current] ?? 0)
            current = calendar.date(byAdding: .day, value: 1, to: current) ?? today
        }

        return result
    }

    private func fetchTodaysCheckIn(modelContext: ModelContext) -> SubjectiveCheckIn? {
        let todayStart = Calendar.current.startOfDay(for: Date())
        var descriptor = FetchDescriptor<SubjectiveCheckIn>(
            predicate: #Predicate { $0.date >= todayStart },
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        descriptor.fetchLimit = 1

        return try? modelContext.fetch(descriptor).first
    }

    // MARK: - Score Persistence

    private func saveReadinessScore(
        _ score: ReadinessScore,
        for date: Date,
        modelContext: ModelContext
    ) {
        // Delete any existing score for today to avoid duplicates
        let todayStart = Calendar.current.startOfDay(for: date)
        let tomorrow = Calendar.current.date(byAdding: .day, value: 1, to: todayStart) ?? date

        var descriptor = FetchDescriptor<ReadinessScore>(
            predicate: #Predicate { $0.date >= todayStart && $0.date < tomorrow }
        )
        descriptor.fetchLimit = 10

        if let existing = try? modelContext.fetch(descriptor) {
            for old in existing {
                modelContext.delete(old)
            }
        }

        modelContext.insert(score)

        do {
            try modelContext.save()
        } catch {
            Self.logger.error("Failed to save readiness score: \(error.localizedDescription)")
        }
    }
}
