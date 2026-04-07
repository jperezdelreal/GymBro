import XCTest
@testable import GymBroCore

final class TrainingLoadCalculatorTests: XCTestCase {

    private var calculator: TrainingLoadCalculator!

    override func setUp() {
        super.setUp()
        calculator = TrainingLoadCalculator()
    }

    // MARK: - EWMA Calculation

    func testEmptyVolumesReturnsZero() {
        let load = calculator.calculate(dailyVolumes: [])
        XCTAssertEqual(load.acuteLoad, 0)
        XCTAssertEqual(load.chronicLoad, 0)
        XCTAssertEqual(load.acwr, 1.0)
    }

    func testSingleDayVolume() {
        let load = calculator.calculate(dailyVolumes: [5000])
        XCTAssertEqual(load.acuteLoad, 5000)
        XCTAssertEqual(load.chronicLoad, 5000)
        XCTAssertEqual(load.acwr, 1.0, accuracy: 0.01)
    }

    func testSteadyTrainingACWRNearOne() {
        // 28 days of consistent training → ACWR ≈ 1.0
        let volumes = Array(repeating: 5000.0, count: 28)
        let load = calculator.calculate(dailyVolumes: volumes)
        XCTAssertEqual(load.acwr, 1.0, accuracy: 0.1)
    }

    func testSpikeInTrainingRaisesACWR() {
        // 21 days moderate, then 7 days heavy
        var volumes = Array(repeating: 3000.0, count: 21)
        volumes.append(contentsOf: Array(repeating: 9000.0, count: 7))
        let load = calculator.calculate(dailyVolumes: volumes)
        XCTAssertGreaterThan(load.acwr, 1.3, "ACWR should be elevated after training spike")
    }

    func testDeloadLowersACWR() {
        // 21 days heavy, then 7 days rest
        var volumes = Array(repeating: 8000.0, count: 21)
        volumes.append(contentsOf: Array(repeating: 1000.0, count: 7))
        let load = calculator.calculate(dailyVolumes: volumes)
        XCTAssertLessThan(load.acwr, 0.8, "ACWR should be low after deload")
    }

    // MARK: - Training Stress Balance

    func testTSBNegativeWhenFatigued() {
        // Recent spike → acute > chronic → TSB negative
        var volumes = Array(repeating: 3000.0, count: 21)
        volumes.append(contentsOf: Array(repeating: 9000.0, count: 7))
        let load = calculator.calculate(dailyVolumes: volumes)
        XCTAssertLessThan(load.trainingStressBalance, 0, "TSB should be negative after spike")
    }

    func testTSBPositiveAfterRest() {
        // Recent deload → acute < chronic → TSB positive
        var volumes = Array(repeating: 8000.0, count: 21)
        volumes.append(contentsOf: Array(repeating: 1000.0, count: 7))
        let load = calculator.calculate(dailyVolumes: volumes)
        XCTAssertGreaterThan(load.trainingStressBalance, 0, "TSB should be positive after deload")
    }

    // MARK: - ACWR Scoring

    func testIdealACWRScoresHigh() {
        let score = calculator.scoreFromACWR(1.0)
        XCTAssertGreaterThanOrEqual(score, 85)
    }

    func testHighACWRScoresLow() {
        let score = calculator.scoreFromACWR(1.8)
        XCTAssertLessThan(score, 60, "ACWR 1.8 should score poorly (injury risk)")
    }

    func testVeryHighACWRScoresVeryLow() {
        let score = calculator.scoreFromACWR(2.0)
        XCTAssertLessThan(score, 40, "ACWR 2.0 should score very poorly")
    }

    func testLowACWRScoresModerate() {
        let score = calculator.scoreFromACWR(0.5)
        XCTAssertGreaterThanOrEqual(score, 50)
        XCTAssertLessThan(score, 85)
    }

    // MARK: - TSB Scoring

    func testPositiveTSBScoresWell() {
        let score = calculator.scoreFromTSB(10.0)
        XCTAssertGreaterThanOrEqual(score, 80)
    }

    func testNegativeTSBScoresLower() {
        let score25 = calculator.scoreFromTSB(-25.0)
        XCTAssertLessThanOrEqual(score25, 55)

        let score50 = calculator.scoreFromTSB(-50.0)
        XCTAssertLessThan(score50, score25)
    }

    // MARK: - Combined Score

    func testCombinedScoreBlending() {
        let steadyLoad = calculator.calculate(dailyVolumes: Array(repeating: 5000.0, count: 28))
        let score = calculator.score(for: steadyLoad)
        XCTAssertGreaterThanOrEqual(score, 70, "Steady training should score well")
    }

    // MARK: - Alpha Values

    func testAlphaCalculation() {
        // Default: acute=7, chronic=28
        // alpha = 2/(N+1)
        XCTAssertEqual(calculator.acuteAlpha, 2.0 / 8.0, accuracy: 0.001)
        XCTAssertEqual(calculator.chronicAlpha, 2.0 / 29.0, accuracy: 0.001)
    }

    func testCustomAlphaValues() {
        let custom = TrainingLoadCalculator(acuteDays: 5, chronicDays: 21)
        XCTAssertEqual(custom.acuteAlpha, 2.0 / 6.0, accuracy: 0.001)
        XCTAssertEqual(custom.chronicAlpha, 2.0 / 22.0, accuracy: 0.001)
    }

    // MARK: - Score Bounds

    func testACWRScoreAlwaysBounded() {
        for acwr in stride(from: 0.0, through: 3.0, by: 0.1) {
            let score = calculator.scoreFromACWR(acwr)
            XCTAssertGreaterThanOrEqual(score, 0, "Score should be >= 0 for ACWR \(acwr)")
            XCTAssertLessThanOrEqual(score, 100, "Score should be <= 100 for ACWR \(acwr)")
        }
    }

    func testTSBScoreAlwaysBounded() {
        for tsb in stride(from: -100.0, through: 100.0, by: 5.0) {
            let score = calculator.scoreFromTSB(tsb)
            XCTAssertGreaterThanOrEqual(score, 0, "Score should be >= 0 for TSB \(tsb)")
            XCTAssertLessThanOrEqual(score, 100, "Score should be <= 100 for TSB \(tsb)")
        }
    }
}
