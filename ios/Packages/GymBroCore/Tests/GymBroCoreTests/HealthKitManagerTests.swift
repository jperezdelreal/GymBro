import XCTest
@testable import GymBroCore

final class HealthKitManagerTests: XCTestCase {

    // MARK: - Availability & Authorization

    func testMockServiceIsAvailableByDefault() {
        let service = MockHealthKitService()
        XCTAssertTrue(service.isAvailable)
    }

    func testMockServiceUnavailable() {
        let service = MockHealthKitService(isAvailable: false)
        XCTAssertFalse(service.isAvailable)
    }

    func testRequestAuthorizationSuccess() async throws {
        let service = MockHealthKitService()
        try await service.requestAuthorization()
        XCTAssertTrue(service.authorizationRequested)
    }

    func testRequestAuthorizationThrowsWhenUnavailable() async {
        let service = MockHealthKitService(isAvailable: false)
        do {
            try await service.requestAuthorization()
            XCTFail("Expected error")
        } catch let error as HealthKitServiceError {
            XCTAssertEqual(
                error.errorDescription,
                HealthKitServiceError.notAvailable.errorDescription
            )
        } catch {
            XCTFail("Unexpected error type: \(error)")
        }
    }

    func testRequestAuthorizationThrowsOnDenial() async {
        let service = MockHealthKitService(shouldThrowOnAuth: true)
        do {
            try await service.requestAuthorization()
            XCTFail("Expected error")
        } catch let error as HealthKitServiceError {
            XCTAssertEqual(
                error.errorDescription,
                HealthKitServiceError.authorizationDenied.errorDescription
            )
        } catch {
            XCTFail("Unexpected error type: \(error)")
        }
    }

    // MARK: - Resting Heart Rate

    func testFetchRestingHeartRateReturnsFilteredData() async throws {
        let service = MockHealthKitService()
        let today = Calendar.current.startOfDay(for: Date())
        let yesterday = Calendar.current.date(byAdding: .day, value: -1, to: today)!

        service.mockRestingHeartRate = [
            HealthDataPoint(date: yesterday, value: 58.0, unit: "bpm"),
            HealthDataPoint(date: today, value: 56.0, unit: "bpm")
        ]

        let results = try await service.fetchRestingHeartRate(
            from: yesterday,
            to: Calendar.current.date(byAdding: .day, value: 1, to: today)!
        )
        XCTAssertEqual(results.count, 2)
        XCTAssertEqual(results[0].value, 58.0)
        XCTAssertEqual(results[1].value, 56.0)
    }

    func testFetchRestingHeartRateThrowsWhenUnavailable() async {
        let service = MockHealthKitService(isAvailable: false)
        do {
            _ = try await service.fetchRestingHeartRate(from: Date(), to: Date())
            XCTFail("Expected error")
        } catch {
            XCTAssertTrue(error is HealthKitServiceError)
        }
    }

    // MARK: - HRV

    func testFetchHRVReturnsData() async throws {
        let service = MockHealthKitService()
        let today = Calendar.current.startOfDay(for: Date())

        service.mockHRV = [
            HealthDataPoint(date: today, value: 42.5, unit: "ms")
        ]

        let results = try await service.fetchHeartRateVariability(
            from: today,
            to: Calendar.current.date(byAdding: .day, value: 1, to: today)!
        )
        XCTAssertEqual(results.count, 1)
        XCTAssertEqual(results[0].value, 42.5)
        XCTAssertEqual(results[0].unit, "ms")
    }

    // MARK: - Sleep Analysis

    func testFetchSleepReturnsRecords() async throws {
        let service = MockHealthKitService()
        let today = Calendar.current.startOfDay(for: Date())
        let stages = SleepStageBreakdown(
            asleepMinutes: 300,
            remMinutes: 90,
            deepMinutes: 60,
            coreMinutes: 150
        )

        service.mockSleep = [
            SleepRecord(date: today, totalMinutes: 450, stages: stages)
        ]

        let results = try await service.fetchSleepAnalysis(
            from: today,
            to: Calendar.current.date(byAdding: .day, value: 1, to: today)!
        )
        XCTAssertEqual(results.count, 1)
        XCTAssertEqual(results[0].totalMinutes, 450)
        XCTAssertEqual(results[0].stages.remMinutes, 90)
        XCTAssertEqual(results[0].stages.deepMinutes, 60)
    }

    // MARK: - Active Energy

    func testFetchActiveEnergyReturnsData() async throws {
        let service = MockHealthKitService()
        let today = Calendar.current.startOfDay(for: Date())

        service.mockActiveEnergy = [
            HealthDataPoint(date: today, value: 520.0, unit: "kcal")
        ]

        let results = try await service.fetchActiveEnergy(
            from: today,
            to: Calendar.current.date(byAdding: .day, value: 1, to: today)!
        )
        XCTAssertEqual(results.count, 1)
        XCTAssertEqual(results[0].value, 520.0)
    }

    // MARK: - Background Delivery

    func testEnableBackgroundDelivery() async throws {
        let service = MockHealthKitService()
        try await service.enableBackgroundDelivery()
        XCTAssertTrue(service.backgroundDeliveryEnabled)
    }

    func testDisableBackgroundDelivery() async throws {
        let service = MockHealthKitService()
        try await service.enableBackgroundDelivery()
        try await service.disableBackgroundDelivery()
        XCTAssertFalse(service.backgroundDeliveryEnabled)
    }

    func testBackgroundDeliveryThrowsWhenUnavailable() async {
        let service = MockHealthKitService(isAvailable: false)
        do {
            try await service.enableBackgroundDelivery()
            XCTFail("Expected error")
        } catch {
            XCTAssertTrue(error is HealthKitServiceError)
        }
    }

    // MARK: - Query Failure

    func testQueryFailureThrows() async {
        let service = MockHealthKitService(shouldThrowOnQuery: true)
        do {
            _ = try await service.fetchRestingHeartRate(from: Date(), to: Date())
            XCTFail("Expected error")
        } catch let error as HealthKitServiceError {
            if case .queryFailed(let message) = error {
                XCTAssertEqual(message, "Mock query failure")
            } else {
                XCTFail("Wrong error case")
            }
        } catch {
            XCTFail("Unexpected error type")
        }
    }

    // MARK: - Date Filtering

    func testDataFilteredByDateRange() async throws {
        let service = MockHealthKitService()
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let threeDaysAgo = calendar.date(byAdding: .day, value: -3, to: today)!
        let fiveDaysAgo = calendar.date(byAdding: .day, value: -5, to: today)!

        service.mockHRV = [
            HealthDataPoint(date: fiveDaysAgo, value: 40.0, unit: "ms"),
            HealthDataPoint(date: threeDaysAgo, value: 45.0, unit: "ms"),
            HealthDataPoint(date: today, value: 50.0, unit: "ms")
        ]

        // Query only last 4 days — should exclude 5-day-old reading
        let fourDaysAgo = calendar.date(byAdding: .day, value: -4, to: today)!
        let results = try await service.fetchHeartRateVariability(
            from: fourDaysAgo,
            to: calendar.date(byAdding: .day, value: 1, to: today)!
        )
        XCTAssertEqual(results.count, 2)
        XCTAssertEqual(results[0].value, 45.0)
        XCTAssertEqual(results[1].value, 50.0)
    }

    // MARK: - Model Tests

    func testHealthMetricStaleness() {
        let recentMetric = HealthMetric(
            type: .restingHeartRate,
            value: 58.0,
            unit: "bpm",
            date: Date(),
            fetchedAt: Date()
        )
        XCTAssertFalse(recentMetric.isStale(after: 3600))

        let staleMetric = HealthMetric(
            type: .restingHeartRate,
            value: 58.0,
            unit: "bpm",
            date: Date(),
            fetchedAt: Date(timeIntervalSinceNow: -7200)
        )
        XCTAssertTrue(staleMetric.isStale(after: 3600))
    }

    func testHealthBaselineZScore() {
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 5.0,
            sampleCount: 30
        )

        XCTAssertEqual(baseline.zScore(for: 45.0), 1.0, accuracy: 0.001)
        XCTAssertEqual(baseline.zScore(for: 35.0), -1.0, accuracy: 0.001)
        XCTAssertEqual(baseline.zScore(for: 40.0), 0.0, accuracy: 0.001)
    }

    func testHealthBaselineZScoreWithZeroStdDev() {
        let baseline = HealthBaseline(
            type: .heartRateVariability,
            averageValue: 40.0,
            standardDeviation: 0.0,
            sampleCount: 1
        )
        XCTAssertEqual(baseline.zScore(for: 45.0), 0.0)
    }

    func testSleepStageBreakdownTotalSleep() {
        let stages = SleepStageBreakdown(
            inBedMinutes: 480,
            asleepMinutes: 30,
            awakeMinutes: 30,
            remMinutes: 90,
            deepMinutes: 60,
            coreMinutes: 270
        )
        // Total = asleep + rem + deep + core = 30 + 90 + 60 + 270 = 450
        XCTAssertEqual(stages.totalSleepMinutes, 450.0)
    }

    func testHealthMetricTypeCases() {
        let allCases = HealthMetricType.allCases
        XCTAssertTrue(allCases.contains(.restingHeartRate))
        XCTAssertTrue(allCases.contains(.heartRateVariability))
        XCTAssertTrue(allCases.contains(.sleepDuration))
        XCTAssertTrue(allCases.contains(.activeEnergy))
        XCTAssertEqual(allCases.count, 4)
    }

    func testHealthKitServiceErrorDescriptions() {
        let notAvailable = HealthKitServiceError.notAvailable
        XCTAssertNotNil(notAvailable.errorDescription)
        XCTAssertTrue(notAvailable.errorDescription!.contains("not available"))

        let denied = HealthKitServiceError.authorizationDenied
        XCTAssertTrue(denied.errorDescription!.contains("denied"))

        let queryFailed = HealthKitServiceError.queryFailed("test")
        XCTAssertTrue(queryFailed.errorDescription!.contains("test"))

        let bgFailed = HealthKitServiceError.backgroundDeliveryFailed("bg error")
        XCTAssertTrue(bgFailed.errorDescription!.contains("bg error"))
    }
}
