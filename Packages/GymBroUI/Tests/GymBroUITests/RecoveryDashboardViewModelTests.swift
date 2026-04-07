import XCTest
import SwiftData
@testable import GymBroUI
@testable import GymBroCore

@MainActor
final class RecoveryDashboardViewModelTests: XCTestCase {

    private var viewModel: RecoveryDashboardViewModel!

    override func setUp() {
        super.setUp()
        viewModel = RecoveryDashboardViewModel()
    }

    override func tearDown() {
        viewModel = nil
        super.tearDown()
    }

    // MARK: - Initial State

    func testInitialStateIsEmpty() {
        XCTAssertNil(viewModel.readinessScore)
        XCTAssertTrue(viewModel.readinessTrend.isEmpty)
        XCTAssertTrue(viewModel.muscleRecoveryMap.isEmpty)
        XCTAssertTrue(viewModel.anomalies.isEmpty)
        XCTAssertNil(viewModel.workoutRecommendation)
        XCTAssertNil(viewModel.imbalanceAnalysis)
        XCTAssertFalse(viewModel.isLoading)
        XCTAssertFalse(viewModel.didOverrideAdjustment)
        XCTAssertTrue(viewModel.dismissedAnomalyMessages.isEmpty)
    }

    // MARK: - Anomaly Dismissal

    func testDismissAnomalyFiltersFromActiveList() {
        let anomaly1 = ReadinessAnomaly(
            type: .hrvDrop,
            severity: .medium,
            date: Date(),
            message: "HRV dropped 22%",
            recommendation: "Consider rest",
            affectedMetrics: ["HRV": 45]
        )
        let anomaly2 = ReadinessAnomaly(
            type: .rhrSpike,
            severity: .high,
            date: Date(),
            message: "RHR spiked 15%",
            recommendation: "Monitor symptoms",
            affectedMetrics: ["RHR": 80]
        )

        // Simulate anomalies being loaded
        viewModel.anomalies = [anomaly1, anomaly2]
        XCTAssertEqual(viewModel.activeAnomalies.count, 2)

        // Dismiss first anomaly
        viewModel.dismissAnomaly(anomaly1)
        XCTAssertEqual(viewModel.activeAnomalies.count, 1)
        XCTAssertEqual(viewModel.activeAnomalies.first?.type, .rhrSpike)

        // Dismiss second anomaly
        viewModel.dismissAnomaly(anomaly2)
        XCTAssertTrue(viewModel.activeAnomalies.isEmpty)
    }

    func testDismissingSameAnomalyTwiceIsIdempotent() {
        let anomaly = ReadinessAnomaly(
            type: .sleepDrop,
            severity: .medium,
            date: Date(),
            message: "Sleep dropped",
            recommendation: "Sleep more",
            affectedMetrics: ["Sleep": 35]
        )

        viewModel.anomalies = [anomaly]
        viewModel.dismissAnomaly(anomaly)
        viewModel.dismissAnomaly(anomaly)

        XCTAssertEqual(viewModel.dismissedAnomalyMessages.count, 1)
        XCTAssertTrue(viewModel.activeAnomalies.isEmpty)
    }

    // MARK: - Override

    func testOverrideAdjustmentSetsFlag() {
        XCTAssertFalse(viewModel.didOverrideAdjustment)
        viewModel.overrideAdjustment()
        XCTAssertTrue(viewModel.didOverrideAdjustment)
    }

    // MARK: - Push/Pull Ratio

    func testPushPullRatioWithNoAnalysisReturnsNil() {
        XCTAssertNil(viewModel.pushPullRatio)
        XCTAssertEqual(viewModel.pushPullStatus, "No data")
    }

    // MARK: - Workout Adjustment Evaluation

    func testEvaluateWorkoutAdjustmentRequiresReadinessScore() {
        let programDay = ProgramDayInfo(
            name: "Upper Body",
            isHeavyDay: true,
            exercises: []
        )

        viewModel.evaluateWorkoutAdjustment(programDay: programDay)
        // No readiness score → no recommendation generated
        XCTAssertNil(viewModel.workoutRecommendation)
    }

    func testEvaluateWorkoutAdjustmentResetsOverride() {
        viewModel.didOverrideAdjustment = true

        // Set up readiness score
        let score = ReadinessScore(
            date: Date(),
            overallScore: 55,
            sleepScore: 50,
            hrvScore: 45,
            restingHRScore: 60,
            trainingLoadScore: 65,
            recommendation: "Light training",
            label: .moderate
        )
        viewModel.readinessScore = score

        let programDay = ProgramDayInfo(
            name: "Heavy Squat Day",
            isHeavyDay: true,
            exercises: []
        )

        viewModel.evaluateWorkoutAdjustment(programDay: programDay)

        // Override should reset
        XCTAssertFalse(viewModel.didOverrideAdjustment)
        // Recommendation should be generated (lighter variant since 55 < 60 and heavy day)
        XCTAssertNotNil(viewModel.workoutRecommendation)
        XCTAssertEqual(viewModel.workoutRecommendation?.action, .lighterVariant)
    }

    func testEvaluateWorkoutAdjustmentRestDay() {
        let score = ReadinessScore(
            date: Date(),
            overallScore: 30,
            sleepScore: 25,
            hrvScore: 20,
            restingHRScore: 30,
            trainingLoadScore: 40,
            recommendation: "Rest day",
            label: .poor
        )
        viewModel.readinessScore = score

        let programDay = ProgramDayInfo(
            name: "Leg Day",
            isHeavyDay: true,
            exercises: []
        )

        viewModel.evaluateWorkoutAdjustment(programDay: programDay)

        XCTAssertEqual(viewModel.workoutRecommendation?.action, .restDay)
    }

    func testEvaluateWorkoutAdjustmentProceedAsPlanned() {
        let score = ReadinessScore(
            date: Date(),
            overallScore: 85,
            sleepScore: 80,
            hrvScore: 90,
            restingHRScore: 85,
            trainingLoadScore: 75,
            recommendation: "Train as planned",
            label: .good
        )
        viewModel.readinessScore = score

        let programDay = ProgramDayInfo(
            name: "Pull Day",
            isHeavyDay: false,
            exercises: []
        )

        viewModel.evaluateWorkoutAdjustment(programDay: programDay)

        XCTAssertEqual(viewModel.workoutRecommendation?.action, .proceedAsPlanned)
    }
}
