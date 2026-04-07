import XCTest
@testable import GymBroCore

/// Extended edge case tests for SafetyFilter.
/// Addresses issue #27: missing case sensitivity, unicode, empty strings, boundary conditions.
final class SafetyFilterEdgeCaseTests: XCTestCase {

    let filter = SafetyFilter()

    // MARK: - Case Sensitivity

    func testMedical_upperCase() {
        let result = filter.checkUserMessage("I have a TORN rotator cuff")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Should flag uppercase medical term 'TORN'")
        }
    }

    func testMedical_mixedCase() {
        let result = filter.checkUserMessage("Can you Diagnose my issue?")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Should flag mixed-case 'Diagnose'")
        }
    }

    func testDangerous_upperCase() {
        let result = filter.checkUserMessage("I want to TRAIN THROUGH INJURY")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .dangerous)
        } else {
            XCTFail("Should flag uppercase 'TRAIN THROUGH INJURY'")
        }
    }

    // MARK: - Empty and Whitespace Input

    func testEmptyString_isSafe() {
        let result = filter.checkUserMessage("")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Empty string should be safe")
        }
    }

    func testWhitespaceOnly_isSafe() {
        let result = filter.checkUserMessage("   \n\t  ")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Whitespace-only string should be safe")
        }
    }

    // MARK: - Boundary Patterns

    func testMedical_patternAtStartOfMessage() {
        let result = filter.checkUserMessage("surgery recovery after ACL repair")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Should flag 'surgery' at start of message")
        }
    }

    func testMedical_patternAtEndOfMessage() {
        let result = filter.checkUserMessage("what should I do after surgery")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Should flag 'surgery' at end of message")
        }
    }

    func testMedical_embeddedInLongerWord() {
        // "diagnose" should be caught even within a longer message
        let result = filter.checkUserMessage("Can someone diagnose what's happening?")
        if case .flagged(let category, _) = result {
            XCTAssertEqual(category, .medical)
        } else {
            XCTFail("Should flag 'diagnose' embedded in sentence")
        }
    }

    // MARK: - All Medical Patterns

    func testAllMedicalPatterns() {
        let medicalPatterns = [
            "diagnose", "diagnosis", "torn", "rupture", "fracture",
            "sharp pain", "shooting pain", "numbness", "tingling",
            "herniated", "slipped disc", "surgery", "medication",
            "prescription", "dosage", "steroid cycle", "trt dose"
        ]

        for pattern in medicalPatterns {
            let result = filter.checkUserMessage("I want to ask about \(pattern)")
            if case .flagged(let category, _) = result {
                XCTAssertEqual(category, .medical,
                    "Pattern '\(pattern)' should be flagged as medical")
            } else {
                XCTFail("Pattern '\(pattern)' should be flagged")
            }
        }
    }

    // MARK: - All Dangerous Patterns

    func testAllDangerousPatterns() {
        let dangerousPatterns = [
            "max out alone", "no spotter", "ego lift",
            "train through injury", "ignore pain"
        ]

        for pattern in dangerousPatterns {
            let result = filter.checkUserMessage("Should I \(pattern)?")
            if case .flagged(let category, _) = result {
                XCTAssertEqual(category, .dangerous,
                    "Pattern '\(pattern)' should be flagged as dangerous")
            } else {
                XCTFail("Pattern '\(pattern)' should be flagged")
            }
        }
    }

    // MARK: - Advisory Content

    func testMedicalAdvisory_containsDoctorRecommendation() {
        let result = filter.checkUserMessage("I think I have a fracture")
        if case .flagged(_, let advisory) = result {
            XCTAssertTrue(advisory.contains("doctor") || advisory.contains("physiotherapist"),
                "Medical advisory should recommend professional consultation")
        }
    }

    func testDangerousAdvisory_containsCoachRecommendation() {
        let result = filter.checkUserMessage("I want to max out alone")
        if case .flagged(_, let advisory) = result {
            XCTAssertTrue(advisory.contains("qualified coach"),
                "Dangerous advisory should recommend working with a coach")
        }
    }

    // MARK: - Disclaimer

    func testAppendDisclaimer_emptyString() {
        let result = filter.appendDisclaimerIfNeeded("")
        XCTAssertTrue(result.contains("AI suggestions are not medical advice"))
    }

    func testAppendDisclaimer_veryLongResponse() {
        let longResponse = String(repeating: "Training advice. ", count: 500)
        let result = filter.appendDisclaimerIfNeeded(longResponse)
        XCTAssertTrue(result.contains("AI suggestions are not medical advice"))
    }

    // MARK: - Safe Messages (False Positive Prevention)

    func testSafeMessage_normalTrainingQuestion() {
        let safeMessages = [
            "How many sets for bench press?",
            "What's a good push/pull/legs split?",
            "Should I train 3 or 4 days per week?",
            "What rep range for hypertrophy?",
            "How much protein should I eat?",
        ]

        for message in safeMessages {
            let result = filter.checkUserMessage(message)
            if case .safe = result {
                // Expected
            } else {
                XCTFail("'\(message)' should be safe but was flagged")
            }
        }
    }

    // MARK: - Special Characters

    func testMessageWithEmoji() {
        let result = filter.checkUserMessage("Best exercises for arms? 💪🏋️")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Message with emoji should be safe")
        }
    }

    func testMessageWithNewlines() {
        let result = filter.checkUserMessage("My routine:\n1. Squat\n2. Bench\n3. Deadlift")
        if case .safe = result {
            // Expected
        } else {
            XCTFail("Message with newlines should be safe")
        }
    }

    // MARK: - Medical Priority Over Dangerous

    func testMedicalTakesPriorityOverDangerous() {
        // A message containing both medical and dangerous patterns
        let result = filter.checkUserMessage("Should I train through injury if I have a torn muscle?")
        if case .flagged(let category, _) = result {
            // Medical patterns are checked first in the implementation
            XCTAssertEqual(category, .medical,
                "Medical should take priority when both patterns match")
        } else {
            XCTFail("Should be flagged")
        }
    }
}
