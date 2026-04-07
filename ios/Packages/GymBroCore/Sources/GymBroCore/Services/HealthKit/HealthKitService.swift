import Foundation

/// Protocol abstracting HealthKit operations for testability.
/// The real implementation uses HKHealthStore; tests use MockHealthKitService.
public protocol HealthKitService: Sendable {
    /// Whether HealthKit is available on this device.
    var isAvailable: Bool { get }

    /// Request authorization for the health data types GymBro needs.
    func requestAuthorization() async throws

    /// Fetch resting heart rate for the given date range.
    /// Returns daily averages sorted by date ascending.
    func fetchRestingHeartRate(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint]

    /// Fetch heart rate variability (SDNN) for the given date range.
    /// Returns daily averages sorted by date ascending.
    func fetchHeartRateVariability(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint]

    /// Fetch sleep analysis samples for the given date range.
    /// Returns one SleepRecord per night.
    func fetchSleepAnalysis(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [SleepRecord]

    /// Fetch active energy burned for the given date range.
    /// Returns daily totals sorted by date ascending.
    func fetchActiveEnergy(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint]

    /// Enable background delivery for HRV and sleep data.
    func enableBackgroundDelivery() async throws

    /// Disable background delivery for all registered types.
    func disableBackgroundDelivery() async throws
}

/// A single health data reading with date and value.
public struct HealthDataPoint: Sendable, Equatable {
    public let date: Date
    public let value: Double
    public let unit: String

    public init(date: Date, value: Double, unit: String) {
        self.date = date
        self.value = value
        self.unit = unit
    }
}

/// A night of sleep with stage breakdown.
public struct SleepRecord: Sendable, Equatable {
    public let date: Date
    public let totalMinutes: Double
    public let stages: SleepStageBreakdown

    public init(date: Date, totalMinutes: Double, stages: SleepStageBreakdown) {
        self.date = date
        self.totalMinutes = totalMinutes
        self.stages = stages
    }
}

/// Errors specific to HealthKit operations.
public enum HealthKitServiceError: Error, LocalizedError, Sendable {
    case notAvailable
    case authorizationDenied
    case queryFailed(String)
    case backgroundDeliveryFailed(String)

    public var errorDescription: String? {
        switch self {
        case .notAvailable:
            return "HealthKit is not available on this device."
        case .authorizationDenied:
            return "HealthKit authorization was denied. Enable it in Settings > Privacy > Health."
        case .queryFailed(let detail):
            return "HealthKit query failed: \(detail)"
        case .backgroundDeliveryFailed(let detail):
            return "Background delivery setup failed: \(detail)"
        }
    }
}
