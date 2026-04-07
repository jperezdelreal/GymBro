import XCTest
@testable import GymBroCore

final class E1RMCalculatorTests: XCTestCase {

    let calculator = E1RMCalculator()

    // MARK: - Epley Formula

    func testEpley_singleRep_returnsWeight() {
        let result = calculator.calculate(weight: 100, reps: 1, formula: .epley)
        XCTAssertEqual(result, 100, accuracy: 0.01)
    }

    func testEpley_fiveReps_100kg() {
        // Expected: 100 * (1 + 5/30) = 116.67
        let result = calculator.calculate(weight: 100, reps: 5, formula: .epley)
        XCTAssertEqual(result, 116.67, accuracy: 0.1)
    }

    func testEpley_tenReps_80kg() {
        // Expected: 80 * (1 + 10/30) = 106.67
        let result = calculator.calculate(weight: 80, reps: 10, formula: .epley)
        XCTAssertEqual(result, 106.67, accuracy: 0.1)
    }

    func testEpley_zeroWeight_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: 0, reps: 5, formula: .epley), 0)
    }

    func testEpley_zeroReps_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: 100, reps: 0, formula: .epley), 0)
    }

    func testEpley_negativeWeight_returnsZero() {
        XCTAssertEqual(calculator.calculate(weight: -50, reps: 5, formula: .epley), 0)
    }

    // MARK: - Brzycki Formula

    func testBrzycki_singleRep_returnsWeight() {
        let result = calculator.calculate(weight: 100, reps: 1, formula: .brzycki)
        XCTAssertEqual(result, 100, accuracy: 0.01)
    }

    func testBrzycki_fiveReps_100kg() {
        // Expected: 100 * (36 / 31) = 116.13
        let result = calculator.calculate(weight: 100, reps: 5, formula: .brzycki)
        XCTAssertEqual(result, 116.13, accuracy: 0.1)
    }

    func testBrzycki_tenReps_80kg() {
        // Expected: 80 * (36 / 26) = 110.77
        let result = calculator.calculate(weight: 80, reps: 10, formula: .brzycki)
        XCTAssertEqual(result, 110.77, accuracy: 0.1)
    }

    // MARK: - bestE1RM

    func testBestE1RM_multipleSets() {
        let sets: [(weight: Double, reps: Int)] = [
            (weight: 100, reps: 5),  // e1RM = 116.67
            (weight: 80, reps: 10),  // e1RM = 106.67
            (weight: 120, reps: 1),  // e1RM = 120.00
        ]
        let best = calculator.bestE1RM(from: sets, formula: .epley)
        XCTAssertEqual(best, 120.0, accuracy: 0.1)
    }

    func testBestE1RM_emptyArray() {
        XCTAssertEqual(calculator.bestE1RM(from: []), 0)
    }
}
