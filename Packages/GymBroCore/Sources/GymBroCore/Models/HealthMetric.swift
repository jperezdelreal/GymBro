import Foundation
import SwiftData

/// Cached HealthKit reading stored in SwiftData.
/// HealthKit queries are expensive — cache locally with timestamps and refresh periodically.
@Model
public final class HealthMetric {
    public var id: UUID
    public var type: HealthMetricType
    public var value: Double
    public var unit: String
    public var date: Date
    public var fetchedAt: Date

    /// For sleep: stores the sleep stage breakdown as JSON.
    public var metadata: String?

    public init(
        id: UUID = UUID(),
        type: HealthMetricType,
        value: Double,
        unit: String,
        date: Date,
        fetchedAt: Date = Date(),
        metadata: String? = nil
    ) {
        self.id = id
        self.type = type
        self.value = value
        self.unit = unit
        self.date = date
        self.fetchedAt = fetchedAt
        self.metadata = metadata
    }

    /// Whether this cached reading is stale (older than the given interval).
    public func isStale(after interval: TimeInterval = 3600) -> Bool {
        Date().timeIntervalSince(fetchedAt) > interval
    }
}

public enum HealthMetricType: String, Codable, CaseIterable {
    case restingHeartRate
    case heartRateVariability
    case sleepDuration
    case activeEnergy
}

/// Normalized sleep stage breakdown decoded from HealthMetric.metadata.
public struct SleepStageBreakdown: Codable, Equatable, Sendable {
    public var inBedMinutes: Double
    public var asleepMinutes: Double
    public var awakeMinutes: Double
    public var remMinutes: Double
    public var deepMinutes: Double
    public var coreMinutes: Double

    public init(
        inBedMinutes: Double = 0,
        asleepMinutes: Double = 0,
        awakeMinutes: Double = 0,
        remMinutes: Double = 0,
        deepMinutes: Double = 0,
        coreMinutes: Double = 0
    ) {
        self.inBedMinutes = inBedMinutes
        self.asleepMinutes = asleepMinutes
        self.awakeMinutes = awakeMinutes
        self.remMinutes = remMinutes
        self.deepMinutes = deepMinutes
        self.coreMinutes = coreMinutes
    }

    public var totalSleepMinutes: Double {
        asleepMinutes + remMinutes + deepMinutes + coreMinutes
    }
}

/// Rolling baseline for HRV and RHR — compared against the user's own history.
@Model
public final class HealthBaseline {
    public var id: UUID
    public var type: HealthMetricType
    public var averageValue: Double
    public var standardDeviation: Double
    public var sampleCount: Int
    public var windowDays: Int
    public var updatedAt: Date

    public init(
        id: UUID = UUID(),
        type: HealthMetricType,
        averageValue: Double,
        standardDeviation: Double,
        sampleCount: Int,
        windowDays: Int = 30,
        updatedAt: Date = Date()
    ) {
        self.id = id
        self.type = type
        self.averageValue = averageValue
        self.standardDeviation = standardDeviation
        self.sampleCount = sampleCount
        self.windowDays = windowDays
        self.updatedAt = updatedAt
    }

    /// Returns how many standard deviations the given value is from the baseline.
    /// Positive = above average, negative = below.
    public func zScore(for value: Double) -> Double {
        guard standardDeviation > 0 else { return 0 }
        return (value - averageValue) / standardDeviation
    }
}
