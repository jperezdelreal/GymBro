import Foundation

/// Mock HealthKit service for testing — no HealthKit framework dependency.
/// Configure responses via properties before calling query methods.
public final class MockHealthKitService: HealthKitService, @unchecked Sendable {
    public var isAvailable: Bool
    public var shouldThrowOnAuth: Bool
    public var shouldThrowOnQuery: Bool
    public var mockRestingHeartRate: [HealthDataPoint]
    public var mockHRV: [HealthDataPoint]
    public var mockSleep: [SleepRecord]
    public var mockActiveEnergy: [HealthDataPoint]
    public var authorizationRequested: Bool = false
    public var backgroundDeliveryEnabled: Bool = false

    public init(
        isAvailable: Bool = true,
        shouldThrowOnAuth: Bool = false,
        shouldThrowOnQuery: Bool = false
    ) {
        self.isAvailable = isAvailable
        self.shouldThrowOnAuth = shouldThrowOnAuth
        self.shouldThrowOnQuery = shouldThrowOnQuery
        self.mockRestingHeartRate = []
        self.mockHRV = []
        self.mockSleep = []
        self.mockActiveEnergy = []
    }

    public func requestAuthorization() async throws {
        guard isAvailable else {
            throw HealthKitServiceError.notAvailable
        }
        if shouldThrowOnAuth {
            throw HealthKitServiceError.authorizationDenied
        }
        authorizationRequested = true
    }

    public func fetchRestingHeartRate(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        try guardQuery()
        return mockRestingHeartRate.filter { $0.date >= startDate && $0.date < endDate }
    }

    public func fetchHeartRateVariability(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        try guardQuery()
        return mockHRV.filter { $0.date >= startDate && $0.date < endDate }
    }

    public func fetchSleepAnalysis(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [SleepRecord] {
        try guardQuery()
        return mockSleep.filter { $0.date >= startDate && $0.date < endDate }
    }

    public func fetchActiveEnergy(
        from startDate: Date,
        to endDate: Date
    ) async throws -> [HealthDataPoint] {
        try guardQuery()
        return mockActiveEnergy.filter { $0.date >= startDate && $0.date < endDate }
    }

    public func enableBackgroundDelivery() async throws {
        guard isAvailable else {
            throw HealthKitServiceError.notAvailable
        }
        backgroundDeliveryEnabled = true
    }

    public func disableBackgroundDelivery() async throws {
        backgroundDeliveryEnabled = false
    }

    private func guardQuery() throws {
        guard isAvailable else {
            throw HealthKitServiceError.notAvailable
        }
        if shouldThrowOnQuery {
            throw HealthKitServiceError.queryFailed("Mock query failure")
        }
    }
}
