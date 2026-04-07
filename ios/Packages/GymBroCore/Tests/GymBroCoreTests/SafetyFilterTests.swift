import XCTest
@testable import GymBroCore

final class SafetyFilterTests: XCTestCase {

    let filter = SafetyFilter()

    // MARK: - Safe Messages

    func testSafeTrainingQuestion() {
        let result = filter.checkUserMessage("How many sets should I do for bench press?")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Expected safe result for training question")
        }
    }

    func testSafeProgressionQuestion() {
        let result = filter.checkUserMessage("When should I add weight to my squat?")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Expected safe result for progression question")
        }
    }

    // MARK: - Medical Flags

    func testFlagsDiagnosisRequest() {
        let result = filter.checkUserMessage("Can you diagnose what's wrong with my shoulder?")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Expected flagged result for diagnosis request")
        }
    }

    func testFlagsSharpPainMention() {
        let result = filter.checkUserMessage("I have a sharp pain in my lower back when deadlifting")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Expected flagged result for pain description")
        }
    }

    func testFlagsSteroidCycleQuestion() {
        let result = filter.checkUserMessage("What's a good steroid cycle for powerlifting?")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Expected flagged result for steroid question")
        }
    }

    func testFlagsTornMuscle() {
        let result = filter.checkUserMessage("I think I have a torn rotator cuff")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Expected flagged result for torn muscle")
        }
    }

    // MARK: - Dangerous Flags

    func testFlagsTrainThroughInjury() {
        let result = filter.checkUserMessage("Should I train through injury?")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .dangerous)
        } else {
            XCTFail("Expected flagged result for training through injury")
        }
    }

    // MARK: - Disclaimer

    func testAppendsDisclaimer() {
        let response = "Do 5 sets of 5 reps."
        let result = filter.appendDisclaimerIfNeeded(response)
        XCTAssertTrue(result.contains("AI suggestions are not medical advice"))
    }

    func testDoesNotDuplicateDisclaimer() {
        let response = "Do 5 sets.\n\n*⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*"
        let result = filter.appendDisclaimerIfNeeded(response)
        XCTAssertEqual(response.count, result.count, "Should not duplicate existing disclaimer")
    }
}

// Equatable conformance for testing
extension SafetyCategory: Equatable {}
