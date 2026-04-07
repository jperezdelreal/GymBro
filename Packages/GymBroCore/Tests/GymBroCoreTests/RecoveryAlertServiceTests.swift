import XCTest
@testable import GymBroCore

final class RecoveryAlertServiceTests: XCTestCase {

    private var service: RecoveryAlertService!

    override func setUp() {
        super.setUp()
        service = RecoveryAlertService()
    }

    // MARK: - Threshold Tests

    func testScoreAbove50ReturnsNoAlert() {
        let score = makeScore(overallScore: 65, label: .moderate)
        let alert = service.evaluate(score: score)
        XCTAssertNil(alert, "Score >= 50 should not trigger an alert")
    }

    func testScoreAt50ReturnsNoAlert() {
        let score = makeScore(overallScore: 50, label: .moderate)
        let alert = service.evaluate(score: score)
        XCTAssertNil(alert, "Score exactly 50 should not trigger an alert")
    }

    func testScoreBelow50TriggersDeloadAlert() {
        let score = makeScore(overallScore: 45, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .deload)
    }

    func testScoreAt49TriggersDeloadAlert() {
        let score = makeScore(overallScore: 49, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .deload)
    }

    func testScoreBelow30TriggersRestDayAlert() {
        let score = makeScore(overallScore: 25, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .restDay)
    }

    func testScoreAt30TriggersDeloadNotRestDay() {
        let score = makeScore(overallScore: 30, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .deload, "Score exactly 30 should be deload, not rest day")
    }

    func testScoreAt29TriggersRestDay() {
        let score = makeScore(overallScore: 29, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .restDay)
    }

    func testScoreZeroTriggersRestDay() {
        let score = makeScore(overallScore: 0, label: .poor)
        let alert = service.evaluate(score: score)
        XCTAssertNotNil(alert)
        XCTAssertEqual(alert?.level, .restDay)
    }

    func testHighScoreReturnsNoAlert() {
        let score = makeScore(overallScore: 92, label: .excellent)
        let alert = service.evaluate(score: score)
        XCTAssertNil(alert)
    }

    // MARK: - Alert Content

    func testDeloadAlertContent() {
        let score = makeScore(overallScore: 42, label: .poor)
        let alert = service.evaluate(score: score)!
        
        XCTAssertEqual(alert.title, "Low Recovery Today")
        XCTAssertTrue(alert.message.contains("42"), "Message should contain the score")
        XCTAssertFalse(alert.recommendation.isEmpty)
        XCTAssertEqual(alert.readinessScore, 42)
    }

    func testRestDayAlertContent() {
        let score = makeScore(overallScore: 18, label: .poor)
        let alert = service.evaluate(score: score)!

        XCTAssertEqual(alert.title, "Recovery Needed")
        XCTAssertTrue(alert.message.contains("18"))
        XCTAssertTrue(alert.recommendation.lowercased().contains("rest"))
    }

    // MARK: - Deload Suggestions

    func testDeloadSuggestionsForDeloadLevel() {
        let suggestions = RecoveryAlertService.deloadSuggestions(for: .deload)
        XCTAssertEqual(suggestions.count, 3)
        XCTAssertTrue(suggestions.allSatisfy { $0.intensityPercent > 0 })
        XCTAssertTrue(suggestions.allSatisfy { $0.intensityPercent <= 70 })
    }

    func testDeloadSuggestionsForRestDayLevel() {
        let suggestions = RecoveryAlertService.deloadSuggestions(for: .restDay)
        XCTAssertEqual(suggestions.count, 3)
        // First suggestion should be complete rest (0%)
        XCTAssertEqual(suggestions.first?.intensityPercent, 0)
        XCTAssertTrue(suggestions.allSatisfy { $0.intensityPercent <= 15 })
    }

    func testSuggestionsHaveUniqueIds() {
        let deloadSuggestions = RecoveryAlertService.deloadSuggestions(for: .deload)
        let ids = Set(deloadSuggestions.map(\.id))
        XCTAssertEqual(ids.count, deloadSuggestions.count, "All suggestions should have unique IDs")
    }

    func testSuggestionsHaveNonEmptyContent() {
        for level in [RecoveryAlertService.AlertLevel.deload, .restDay] {
            let suggestions = RecoveryAlertService.deloadSuggestions(for: level)
            for suggestion in suggestions {
                XCTAssertFalse(suggestion.name.isEmpty, "Suggestion name should not be empty")
                XCTAssertFalse(suggestion.description.isEmpty, "Suggestion description should not be empty")
                XCTAssertFalse(suggestion.icon.isEmpty, "Suggestion icon should not be empty")
            }
        }
    }

    // MARK: - Integration with ReadinessScoreService

    func testEvaluateFromInputWithPoorSleep() {
        let input = ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 180,
                stages: SleepStageBreakdown(
                    inBedMinutes: 300,
                    asleepMinutes: 100,
                    awakeMinutes: 120,
                    remMinutes: 20,
                    deepMinutes: 10,
                    coreMinutes: 50
                )
            ),
            recentSleepDurations: [180, 200, 180, 220, 180, 200, 180],
            currentHRV: 20.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 40.0,
                standardDeviation: 5.0,
                sampleCount: 30
            ),
            currentRestingHR: 72.0,
            rhrBaseline: HealthBaseline(
                type: .restingHeartRate,
                averageValue: 58.0,
                standardDeviation: 3.0,
                sampleCount: 30
            ),
            dailyTrainingVolumes: [2000, 2000, 2000, 2000, 10000, 12000, 15000]
        )

        let alert = service.evaluateFromInput(input)
        XCTAssertNotNil(alert, "Poor recovery input should trigger an alert")
        // With very poor inputs, should be deload or rest day
        XCTAssertTrue(
            alert?.level == .deload || alert?.level == .restDay,
            "Alert level should be deload or restDay, got \(String(describing: alert?.level))"
        )
    }

    func testEvaluateFromInputWithGoodRecovery() {
        let input = ReadinessScoreService.Input(
            sleepRecord: SleepRecord(
                date: Date(),
                totalMinutes: 480,
                stages: SleepStageBreakdown(
                    inBedMinutes: 500,
                    asleepMinutes: 0,
                    awakeMinutes: 20,
                    remMinutes: 100,
                    deepMinutes: 90,
                    coreMinutes: 290
                )
            ),
            recentSleepDurations: [470, 480, 475, 480, 470, 485, 480],
            currentHRV: 55.0,
            hrvBaseline: HealthBaseline(
                type: .heartRateVariability,
                averageValue: 40.0,
                standardDeviation: 5.0,
                sampleCount: 30
            ),
            currentRestingHR: 50.0,
            rhrBaseline: HealthBaseline(
                type: .restingHeartRate,
                averageValue: 58.0,
                standardDeviation: 3.0,
                sampleCount: 30
            ),
            dailyTrainingVolumes: Array(repeating: 5000.0, count: 28)
        )

        let alert = service.evaluateFromInput(input)
        XCTAssertNil(alert, "Good recovery should not trigger an alert")
    }

    // MARK: - Thresholds Constants

    func testThresholdConstants() {
        XCTAssertEqual(RecoveryAlertService.deloadThreshold, 50.0)
        XCTAssertEqual(RecoveryAlertService.restDayThreshold, 30.0)
        XCTAssertTrue(
            RecoveryAlertService.restDayThreshold < RecoveryAlertService.deloadThreshold,
            "Rest day threshold must be lower than deload threshold"
        )
    }

    // MARK: - Helpers

    private func makeScore(overallScore: Double, label: ReadinessLabel) -> ReadinessScore {
        ReadinessScore(
            date: Date(),
            overallScore: overallScore,
            sleepScore: overallScore,
            hrvScore: overallScore,
            restingHRScore: overallScore,
            trainingLoadScore: overallScore,
            recommendation: ReadinessScoreService.recommendation(for: label),
            label: label
        )
    }
}
