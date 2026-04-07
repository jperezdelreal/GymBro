import XCTest
@testable import GymBroCore

final class RollingAverageAnalyzerTests: XCTestCase {

    let analyzer = RollingAverageAnalyzer()

    // MARK: - Moving Average

    func testMovingAverage_basicCalculation() {
        let values = [10.0, 20.0, 30.0, 40.0, 50.0]
        let ma = analyzer.movingAverage(values: values, window: 3)

        XCTAssertEqual(ma.count, 3)
        XCTAssertEqual(ma[0], 20.0, accuracy: 0.01) // avg(10,20,30)
        XCTAssertEqual(ma[1], 30.0, accuracy: 0.01) // avg(20,30,40)
        XCTAssertEqual(ma[2], 40.0, accuracy: 0.01) // avg(30,40,50)
    }

    func testMovingAverage_windowLargerThanData() {
        let values = [10.0, 20.0]
        let ma = analyzer.movingAverage(values: values, window: 5)
        XCTAssertEqual(ma.count, 2)
    }

    // MARK: - Slope

    func testSlope_positive() {
        let values = [1.0, 2.0, 3.0, 4.0, 5.0]
        let slope = analyzer.slope(of: values)
        XCTAssertEqual(slope, 1.0, accuracy: 0.01)
    }

    func testSlope_flat() {
        let values = [5.0, 5.0, 5.0, 5.0]
        let slope = analyzer.slope(of: values)
        XCTAssertEqual(slope, 0.0, accuracy: 0.01)
    }

    func testSlope_negative() {
        let values = [5.0, 4.0, 3.0, 2.0, 1.0]
        let slope = analyzer.slope(of: values)
        XCTAssertEqual(slope, -1.0, accuracy: 0.01)
    }

    func testSlope_singleValue_returnsZero() {
        XCTAssertEqual(analyzer.slope(of: [42.0]), 0)
    }

    // MARK: - Stagnation Detection

    func testAnalyze_strongUptrend_lowScore() {
        let values = (0..<8).map { 100.0 * pow(1.05, Double($0)) }
        let score = analyzer.analyze(values: values)
        XCTAssertLessThan(score, 0.3)
    }

    func testAnalyze_perfectlyFlat_highScore() {
        let values = Array(repeating: 100.0, count: 8)
        let score = analyzer.analyze(values: values)
        XCTAssertGreaterThan(score, 0.5)
    }

    func testAnalyze_decliningTrend_highScore() {
        let values = (0..<8).map { 120.0 - Double($0) * 2.0 }
        let score = analyzer.analyze(values: values)
        XCTAssertGreaterThan(score, 0.3)
    }

    func testAnalyze_configurableWindow() {
        let customAnalyzer = RollingAverageAnalyzer(
            shortWindow: 2,
            longWindow: 4,
            stagnationThreshold: 1.0
        )
        let values = Array(repeating: 100.0, count: 8)
        let score = customAnalyzer.analyze(values: values)
        XCTAssertGreaterThan(score, 0.3, "Flat data should trigger stagnation even with custom windows")
    }
}
