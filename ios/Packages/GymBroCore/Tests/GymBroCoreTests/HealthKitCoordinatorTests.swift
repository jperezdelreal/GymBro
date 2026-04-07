import XCTest
import SwiftData
@testable import GymBroCore

/// Tests for HealthKitCoordinator — the glue wiring HealthKit to ReadinessScoreService.
/// All tests use MockHealthKitService to avoid HealthKit framework dependency.
final class HealthKitCoordinatorTests: XCTestCase {

    private var coordinator: HealthKitCoordinator!
    private var mockService: MockHealthKitService!
    private var modelContainer: ModelContainer!
    private var modelContext: ModelContext!

    override func setUp() async throws {
        try await super.setUp()
        mockService = MockHealthKitService()
        coordinator = HealthKitCoordinator(healthKitService: mockService)

        let schema = Schema([
            HealthMetric.self,
            HealthBaseline.self,
            ReadinessScore.self,
            SubjectiveCheckIn.self
        ])
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(modelContainer)
    }

    override func tearDown() {
        coordinator = nil
        mockService = nil
        modelContainer = nil
        modelContext = nil
        super.tearDown()
    }

    // MARK: - Authorization

    func testRequestAuthorizationSuccess() async {
        let result = await coordinator.requestAuthorization()
        XCTAssertTrue(result)
        XCTAssertTrue(coordinator.hasRequestedAuthorization)
        XCTAssertTrue(coordinator.authorizationGranted)
        XCTAssertTrue(mockService.authorizationRequested)
    }

    func testRequestAuthorizationDenied() async {
        mockService.shouldThrowOnAuth = true
        let result = await coordinator.requestAuthorization()
        XCTAssertFalse(result)
        XCTAssertTrue(coordinator.hasRequestedAuthorization)
        XCTAssertFalse(coordinator.authorizationGranted)
    }

    func testRequestAuthorizationUnavailableDevice() async {
        mockService.isAvailable = false
        let result = await coordinator.requestAuthorization()
        XCTAssertFalse(result)
        XCTAssertFalse(coordinator.hasRequestedAuthorization)
    }

    // MARK: - Full Pipeline

    func testSyncWithHealthDataProducesReadinessScore() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        populateMockHealthData(today: today)

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>())
        XCTAssertEqual(scores.count, 1)

        let score = scores[0]
        XCTAssertGreaterThan(score.overallScore, 0)
        XCTAssertLessThanOrEqual(score.overallScore, 100)
        XCTAssertFalse(score.recommendation.isEmpty)
        XCTAssertEqual(score.date, today)
    }

    func testSyncCreatesHealthMetricsInCache() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        populateMockHealthData(today: today)

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let metrics = try modelContext.fetch(FetchDescriptor<HealthMetric>())
        XCTAssertGreaterThan(metrics.count, 0, "HealthKit data should be cached in SwiftData")
    }

    func testSyncCreatesBaselines() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        populateMockHealthData(today: today)

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let baselines = try modelContext.fetch(FetchDescriptor<HealthBaseline>())
        XCTAssertGreaterThan(baselines.count, 0, "Baselines should be computed from synced data")
    }

    func testSyncReplacesTodaysExistingScore() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        populateMockHealthData(today: today)

        // Run sync twice
        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)
        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>(
            predicate: #Predicate { $0.date == today }
        ))
        XCTAssertEqual(scores.count, 1, "Should replace, not duplicate, today's score")
    }

    // MARK: - Graceful Degradation

    func testSyncWithUnavailableHealthKitProducesNeutralScore() async throws {
        mockService.isAvailable = false

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>())
        XCTAssertEqual(scores.count, 1)
        // No data → neutral score (weight redistribution gives 50)
        XCTAssertEqual(scores[0].overallScore, 50.0)
    }

    func testSyncWithQueryFailureStillProducesScore() async throws {
        mockService.shouldThrowOnQuery = true

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>())
        XCTAssertEqual(scores.count, 1, "Should degrade gracefully, not crash")
    }

    // MARK: - Calculate from Cache

    func testCalculateFromCacheReturnsScore() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        populateMockHealthData(today: today)

        // First sync to populate cache
        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        // Then calculate from cache only
        let score = coordinator.calculateFromCache(modelContext: modelContext)
        XCTAssertGreaterThan(score.overallScore, 0)
        XCTAssertLessThanOrEqual(score.overallScore, 100)
    }

    // MARK: - Input Assembly

    func testBuildInputFromCacheWithSleepData() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        let stages = SleepStageBreakdown(
            inBedMinutes: 480,
            asleepMinutes: 0,
            awakeMinutes: 30,
            remMinutes: 90,
            deepMinutes: 60,
            coreMinutes: 300
        )
        mockService.mockSleep = [
            SleepRecord(date: today, totalMinutes: 450, stages: stages)
        ]
        mockService.mockHRV = [
            HealthDataPoint(date: today, value: 45.0, unit: "ms")
        ]
        mockService.mockRestingHeartRate = [
            HealthDataPoint(date: today, value: 55.0, unit: "bpm")
        ]

        // Sync to populate cache
        let dataSync = HealthKitDataSync(
            service: mockService,
            modelContext: modelContext
        )
        await dataSync.syncAll()

        let input = coordinator.buildInputFromCache(
            dataSync: dataSync,
            modelContext: modelContext
        )

        XCTAssertNotNil(input.sleepRecord)
        XCTAssertEqual(input.sleepRecord?.totalMinutes, 450)
        XCTAssertNotNil(input.currentHRV)
        XCTAssertEqual(input.currentHRV, 45.0)
        XCTAssertNotNil(input.currentRestingHR)
        XCTAssertEqual(input.currentRestingHR, 55.0)
    }

    func testBuildInputWithSubjectiveCheckIn() async throws {
        let today = Calendar.current.startOfDay(for: Date())
        let checkIn = SubjectiveCheckIn(
            date: today,
            energy: 4,
            soreness: 2,
            motivation: 5
        )
        modelContext.insert(checkIn)
        try modelContext.save()

        let dataSync = HealthKitDataSync(
            service: mockService,
            modelContext: modelContext
        )

        let input = coordinator.buildInputFromCache(
            dataSync: dataSync,
            modelContext: modelContext
        )

        XCTAssertNotNil(input.subjectiveCheckIn)
        XCTAssertEqual(input.subjectiveCheckIn?.energy, 4)
    }

    func testBuildInputWithNoDataReturnsEmptyInput() async throws {
        let dataSync = HealthKitDataSync(
            service: mockService,
            modelContext: modelContext
        )

        let input = coordinator.buildInputFromCache(
            dataSync: dataSync,
            modelContext: modelContext
        )

        XCTAssertNil(input.sleepRecord)
        XCTAssertNil(input.currentHRV)
        XCTAssertNil(input.currentRestingHR)
        XCTAssertNil(input.subjectiveCheckIn)
        XCTAssertTrue(input.dailyTrainingVolumes.isEmpty)
    }

    // MARK: - Background Sync

    func testEnableBackgroundSync() async {
        await coordinator.enableBackgroundSync()
        XCTAssertTrue(mockService.backgroundDeliveryEnabled)
    }

    func testEnableBackgroundSyncWhenUnavailable() async {
        mockService.isAvailable = false
        await coordinator.enableBackgroundSync()
        XCTAssertFalse(mockService.backgroundDeliveryEnabled)
    }

    // MARK: - Score Quality

    func testGoodHealthDataProducesHighScore() async throws {
        let today = Calendar.current.startOfDay(for: Date())

        // Excellent sleep, high HRV, low RHR
        mockService.mockSleep = [
            SleepRecord(
                date: today,
                totalMinutes: 480,
                stages: SleepStageBreakdown(
                    inBedMinutes: 500,
                    awakeMinutes: 20,
                    remMinutes: 100,
                    deepMinutes: 90,
                    coreMinutes: 290
                )
            )
        ]
        populateMultiDayHRV(baseValue: 50.0, days: 30)
        populateMultiDayRHR(baseValue: 52.0, days: 30)

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>())
        XCTAssertEqual(scores.count, 1)
        XCTAssertGreaterThanOrEqual(
            scores[0].overallScore, 60,
            "Good health data should produce a high readiness score"
        )
    }

    func testPoorHealthDataProducesLowScore() async throws {
        let today = Calendar.current.startOfDay(for: Date())

        // Poor sleep, low HRV, high RHR
        mockService.mockSleep = [
            SleepRecord(
                date: today,
                totalMinutes: 180,
                stages: SleepStageBreakdown(
                    inBedMinutes: 300,
                    awakeMinutes: 120,
                    remMinutes: 10,
                    deepMinutes: 10,
                    coreMinutes: 40
                )
            )
        ]
        // Recent HRV well below baseline
        populateMultiDayHRV(baseValue: 20.0, days: 30)
        mockService.mockHRV.append(
            HealthDataPoint(date: today, value: 15.0, unit: "ms")
        )
        // Recent RHR well above baseline
        populateMultiDayRHR(baseValue: 72.0, days: 30)
        mockService.mockRestingHeartRate.append(
            HealthDataPoint(date: today, value: 80.0, unit: "bpm")
        )

        await coordinator.syncAndCalculateReadiness(modelContext: modelContext)

        let scores = try modelContext.fetch(FetchDescriptor<ReadinessScore>())
        XCTAssertEqual(scores.count, 1)
        XCTAssertLessThan(
            scores[0].overallScore, 60,
            "Poor health data should produce a low readiness score"
        )
    }

    // MARK: - Helpers

    private func populateMockHealthData(today: Date) {
        let calendar = Calendar.current

        // 7 days of sleep data
        for dayOffset in 0..<7 {
            let date = calendar.date(byAdding: .day, value: -dayOffset, to: today)!
            mockService.mockSleep.append(
                SleepRecord(
                    date: date,
                    totalMinutes: 420 + Double.random(in: -30...30),
                    stages: SleepStageBreakdown(
                        inBedMinutes: 450,
                        asleepMinutes: 30,
                        awakeMinutes: 30,
                        remMinutes: 80,
                        deepMinutes: 60,
                        coreMinutes: 250
                    )
                )
            )
        }

        populateMultiDayHRV(baseValue: 42.0, days: 7)
        populateMultiDayRHR(baseValue: 57.0, days: 7)

        // Active energy
        for dayOffset in 0..<7 {
            let date = calendar.date(byAdding: .day, value: -dayOffset, to: today)!
            mockService.mockActiveEnergy.append(
                HealthDataPoint(date: date, value: 500 + Double.random(in: -100...100), unit: "kcal")
            )
        }
    }

    private func populateMultiDayHRV(baseValue: Double, days: Int) {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        for dayOffset in 0..<days {
            let date = calendar.date(byAdding: .day, value: -dayOffset, to: today)!
            mockService.mockHRV.append(
                HealthDataPoint(date: date, value: baseValue + Double.random(in: -3...3), unit: "ms")
            )
        }
    }

    private func populateMultiDayRHR(baseValue: Double, days: Int) {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        for dayOffset in 0..<days {
            let date = calendar.date(byAdding: .day, value: -dayOffset, to: today)!
            mockService.mockRestingHeartRate.append(
                HealthDataPoint(date: date, value: baseValue + Double.random(in: -2...2), unit: "bpm")
            )
        }
    }
}
