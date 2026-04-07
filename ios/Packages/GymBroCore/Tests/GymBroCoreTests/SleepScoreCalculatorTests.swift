import XCTest
@testable import GymBroCore

final class SleepScoreCalculatorTests: XCTestCase {

    private var calculator: SleepScoreCalculator!

    override func setUp() {
        super.setUp()
        calculator = SleepScoreCalculator()
    }

    // MARK: - Duration Scoring

    func testOptimalDurationScoresFull() {
        // 8 hours = 480 minutes — within 7-9h target
        let score = calculator.scoreDuration(480)
        XCTAssertEqual(score, 100.0)
    }

    func testMinTargetDurationScoresFull() {
        // 7 hours = 420 minutes — exactly at minimum
        let score = calculator.scoreDuration(420)
        XCTAssertEqual(score, 100.0)
    }

    func testMaxTargetDurationScoresFull() {
        // 9 hours = 540 minutes — exactly at maximum
        let score = calculator.scoreDuration(540)
        XCTAssertEqual(score, 100.0)
    }

    func testShortSleepScoresLow() {
        // 4 hours = 240 minutes
        let score = calculator.scoreDuration(240)
        XCTAssertLessThan(score, 70)
        XCTAssertGreaterThan(score, 0)
    }

    func testNoSleepScoresZero() {
        let score = calculator.scoreDuration(0)
        XCTAssertEqual(score, 0.0)
    }

    func testOverSleepGentlePenalty() {
        // 11 hours = 660 minutes
        let score = calculator.scoreDuration(660)
        XCTAssertGreaterThanOrEqual(score, 60)
        XCTAssertLessThan(score, 100)
    }

    // MARK: - Efficiency Scoring

    func testGoodEfficiencyScoresHigh() {
        let stages = SleepStageBreakdown(
            inBedMinutes: 40,
            asleepMinutes: 0,
            awakeMinutes: 20,
            remMinutes: 100,
            deepMinutes: 80,
            coreMinutes: 260
        )
        let score = calculator.scoreEfficiency(stages: stages, totalMinutes: 440)
        XCTAssertGreaterThanOrEqual(score, 70)
    }

    func testPoorEfficiencyScoresLow() {
        let stages = SleepStageBreakdown(
            inBedMinutes: 300,
            asleepMinutes: 100,
            awakeMinutes: 150,
            remMinutes: 20,
            deepMinutes: 10,
            coreMinutes: 40
        )
        let score = calculator.scoreEfficiency(stages: stages, totalMinutes: 170)
        XCTAssertLessThan(score, 70)
    }

    func testNoStageDataReturnsDefaultScore() {
        let score = calculator.scoreEfficiency(stages: nil, totalMinutes: 420)
        XCTAssertEqual(score, 70.0)
    }

    func testNoSleepNoStageDataReturnsZero() {
        let score = calculator.scoreEfficiency(stages: nil, totalMinutes: 0)
        XCTAssertEqual(score, 0.0)
    }

    // MARK: - Consistency Scoring

    func testConsistentSleepScoresHigh() {
        let durations = [420.0, 425.0, 430.0, 420.0, 425.0, 430.0, 420.0]
        let score = calculator.scoreConsistency(durations)
        XCTAssertGreaterThanOrEqual(score, 90)
    }

    func testInconsistentSleepScoresLow() {
        let durations = [180.0, 540.0, 300.0, 600.0, 240.0, 480.0, 360.0]
        let score = calculator.scoreConsistency(durations)
        XCTAssertLessThan(score, 60)
    }

    func testInsufficientDataReturnsNeutral() {
        let score = calculator.scoreConsistency([420, 430])
        XCTAssertEqual(score, 70.0)
    }

    func testEmptyDataReturnsNeutral() {
        let score = calculator.scoreConsistency([])
        XCTAssertEqual(score, 70.0)
    }

    // MARK: - Combined Score

    func testCombinedScoreWeightsCorrectly() {
        let stages = SleepStageBreakdown(
            inBedMinutes: 30,
            asleepMinutes: 0,
            awakeMinutes: 10,
            remMinutes: 90,
            deepMinutes: 70,
            coreMinutes: 280
        )
        let recentDurations = [440.0, 450.0, 445.0, 440.0, 450.0, 445.0, 440.0]

        let score = calculator.score(
            totalMinutes: 440,
            stages: stages,
            recentDurations: recentDurations
        )

        XCTAssertGreaterThanOrEqual(score, 0)
        XCTAssertLessThanOrEqual(score, 100)
        // 440 min = 7.33h → within target → duration score 100
        // Good efficiency → ~90+
        // Consistent → ~95+
        // Weighted: 100*0.5 + ~90*0.3 + ~95*0.2 ≈ 96
        XCTAssertGreaterThan(score, 80)
    }

    func testCombinedScoreWithNoOptionalData() {
        // Only total minutes, no stages, no history
        let score = calculator.score(totalMinutes: 420)
        XCTAssertGreaterThan(score, 0)
        XCTAssertLessThanOrEqual(score, 100)
    }

    // MARK: - Custom Target

    func testCustomTargetRange() {
        let custom = SleepScoreCalculator(targetMinHours: 6.0, targetMaxHours: 8.0)
        // 6 hours should be 100 for custom, but less for default
        let customScore = custom.scoreDuration(360) // 6h
        let defaultScore = calculator.scoreDuration(360) // 6h
        XCTAssertGreaterThan(customScore, defaultScore)
    }
}
