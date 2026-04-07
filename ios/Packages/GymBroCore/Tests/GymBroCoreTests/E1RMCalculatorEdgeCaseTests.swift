import XCTest
@testable import GymBroCore

/// Extended edge case tests for E1RMCalculator.
/// Addresses issue #27: missing extreme values, infinity, NaN, high rep ranges.
final class E1RMCalculatorEdgeCaseTests: XCTestCase {

    let calculator = E1RMCalculator()

    // MARK: - Extreme Weight Values

    func testVeryLargeWeight_Epley() {
        let result = calculator.calculate(weight: 500, reps: 5, formula: .epley)
        // 500 * (1 + 5/30) = 583.33
        XCTAssertEqual(result, 583.33, accuracy: 0.1)
        XCTAssertTrue(result.isFinite, "Result should be finite for large but valid weight")
    }

    func testVerySmallWeight_Epley() {
        let result = calculator.calculate(weight: 0.5, reps: 10, formula: .epley)
        // 0.5 * (1 + 10/30) = 0.667
        XCTAssertEqual(result, 0.667, accuracy: 0.01)
    }

    // MARK: - High Rep Ranges

    func testHighReps_Epley() {
        let result = calculator.calculate(weight: 50, reps: 30, formula: .epley)
        // 50 * (1 + 30/30) = 100
        XCTAssertEqual(result, 100, accuracy: 0.1)
    }

    func testVeryHighReps_Epley() {
        let result = calculator.calculate(weight: 20, reps: 100, formula: .epley)
        // 20 * (1 + 100/30) = 86.67
        XCTAssertEqual(result, 86.67, accuracy: 0.1)
        XCTAssertTrue(result.isFinite)
    }

    func testHighReps_Brzycki_nearDenominatorLimit() {
        // Brzycki: weight * (36 / (36 - reps))
        // At reps = 35: 36 / (36 - 35) = 36 / 1 = 36
        let result = calculator.calculate(weight: 50, reps: 35, formula: .brzycki)
        XCTAssertEqual(result, 1800, accuracy: 0.1)
    }

    func testHighReps_Brzycki_atDenominatorZero() {
        // At reps = 36: denominator = 0, guard returns weight * 2
        let result = calculator.calculate(weight: 50, reps: 36, formula: .brzycki)
        XCTAssertEqual(result, 100, accuracy: 0.1)
    }

    func testHighReps_Brzycki_pastDenominatorZero() {
        // At reps = 40: denominator = -4, guard returns weight * 2
        let result = calculator.calculate(weight: 50, reps: 40, formula: .brzycki)
        XCTAssertEqual(result, 100, accuracy: 0.1)
    }

    // MARK: - Invalid Inputs

    func testNegativeReps_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: 100, reps: -5), 0)
    }

    func testNegativeWeight_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: -100, reps: 5), 0)
    }

    func testBothZero_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: 0, reps: 0), 0)
    }

    func testBothNegative_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: -50, reps: -5), 0)
    }

    // MARK: - Infinity and NaN (Double edge cases)

    func testInfinityWeight_epley() {
        let result = calculator.calculate(weight: Double.infinity, reps: 5)
        // weight > 0 is true for infinity, reps > 0, reps > 1
        // infinity * (1 + 5/30) = infinity
        XCTAssertTrue(result.isInfinite, "Infinity weight should produce infinite result")
    }

    func testNaNWeight_epley() {
        let result = calculator.calculate(weight: Double.nan, reps: 5)
        // NaN > 0 is false, so guard returns 0
        XCTAssertEqual(result, 0, "NaN weight should return 0 due to guard")
    }

    // MARK: - Single Rep Edge Case

    func testSingleRep_returnsExactWeight() {
        XCTAssertEqual(calculator.calculate(weight: 200, reps: 1, formula: .epley), 200)
        XCTAssertEqual(calculator.calculate(weight: 200, reps: 1, formula: .brzycki), 200)
    }

    func testTwoReps_producesModestIncrease() {
        let epley = calculator.calculate(weight: 100, reps: 2, formula: .epley)
        // 100 * (1 + 2/30) = 106.67
        XCTAssertEqual(epley, 106.67, accuracy: 0.1)
        XCTAssertGreaterThan(epley, 100)
    }

    // MARK: - bestE1RM Edge Cases

    func testBestE1RM_singleSet() {
        let sets: [(weight: Double, reps: Int)] = [(weight: 100, reps: 5)]
        let best = calculator.bestE1RM(from: sets)
        XCTAssertEqual(best, 116.67, accuracy: 0.1)
    }

    func testBestE1RM_allZeroWeight() {
        let sets: [(weight: Double, reps: Int)] = [
            (weight: 0, reps: 5),
            (weight: 0, reps: 10),
        ]
        XCTAssertEqual(calculator.bestE1RM(from: sets), 0)
    }

    func testBestE1RM_allZeroReps() {
        let sets: [(weight: Double, reps: Int)] = [
            (weight: 100, reps: 0),
            (weight: 200, reps: 0),
        ]
        XCTAssertEqual(calculator.bestE1RM(from: sets), 0)
    }

    func testBestE1RM_mixedValidAndInvalid() {
        let sets: [(weight: Double, reps: Int)] = [
            (weight: -50, reps: 5),   // invalid → 0
            (weight: 100, reps: 5),   // valid → 116.67
            (weight: 0, reps: 10),    // invalid → 0
        ]
        let best = calculator.bestE1RM(from: sets)
        XCTAssertEqual(best, 116.67, accuracy: 0.1, "Should return best valid e1RM")
    }

    // MARK: - Formula Comparison

    func testEpleyVsBrzycki_fiveReps_similar() {
        let epley = calculator.calculate(weight: 100, reps: 5, formula: .epley)
        let brzycki = calculator.calculate(weight: 100, reps: 5, formula: .brzycki)
        XCTAssertEqual(epley, brzycki, accuracy: 2.0,
            "Epley and Brzycki should produce similar results at moderate rep ranges")
    }

    func testEpleyVsBrzycki_divergeAtHighReps() {
        let epley = calculator.calculate(weight: 50, reps: 20, formula: .epley)
        let brzycki = calculator.calculate(weight: 50, reps: 20, formula: .brzycki)
        // They diverge significantly at high reps
        XCTAssertNotEqual(epley, brzycki, "Formulas should diverge at high rep ranges")
    }
}
