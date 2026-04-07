import XCTest
@testable import GymBroCore

/// Tests for DeterministicCoachFallback — offline rule-based AI coach.
/// Validates pattern matching, safety integration, and response quality.
final class DeterministicCoachFallbackExtendedTests: XCTestCase {

    let sut = DeterministicCoachFallback()
    let emptyContext = CoachContext()

    // MARK: - Safety Integration

    func testSafetyFilter_medicalQueryReturnsSafetyAdvisory() async throws {
        let response = try await sut.sendMessage("Can you diagnose my shoulder pain?", context: emptyContext)
        XCTAssertTrue(response.contains("medical"), "Should return medical safety advisory")
        XCTAssertTrue(response.contains("doctor") || response.contains("physiotherapist"),
            "Should recommend professional consultation")
    }

    func testSafetyFilter_dangerousQueryReturnsSafetyAdvisory() async throws {
        let response = try await sut.sendMessage("Should I train through injury?", context: emptyContext)
        XCTAssertTrue(response.contains("dangerous") || response.contains("qualified coach"),
            "Should return danger safety advisory")
    }

    // MARK: - Rest Time

    func testRestTimeAdvice_containsGuidelines() async throws {
        let response = try await sut.sendMessage("What rest time should I take between sets?", context: emptyContext)
        XCTAssertTrue(response.contains("Rest Time") || response.contains("rest"))
        XCTAssertTrue(response.contains("Compound") || response.contains("compound") || response.contains("3-5"))
    }

    func testRestTimeAdvice_alternativePhrasings() async throws {
        let phrases = ["how long should I rest between sets", "rest period for bench", "rest time guidance"]
        for phrase in phrases {
            let response = try await sut.sendMessage(phrase, context: emptyContext)
            XCTAssertTrue(response.contains("Rest") || response.contains("rest"),
                "Should match rest-time pattern for: '\(phrase)'")
        }
    }

    // MARK: - Deload

    func testDeloadAdvice_usesExperienceLevel() async throws {
        let advancedProfile = UserProfileSnapshot(experienceLevel: "advanced", unitSystem: "metric")
        let context = CoachContext(userProfile: advancedProfile)
        let response = try await sut.sendMessage("When should I deload?", context: context)
        XCTAssertTrue(response.contains("advanced"))
        XCTAssertTrue(response.contains("4-6 weeks"))
    }

    func testDeloadAdvice_intermediateDefault() async throws {
        let response = try await sut.sendMessage("Should I take a recovery week?", context: emptyContext)
        XCTAssertTrue(response.contains("intermediate") || response.contains("6-8 weeks"))
    }

    // MARK: - Plateau

    func testPlateauAdvice_returnsActionableSteps() async throws {
        let response = try await sut.sendMessage("I'm stuck on my bench press", context: emptyContext)
        XCTAssertTrue(response.contains("Plateau") || response.contains("plateau"))
    }

    // MARK: - Warmup

    func testWarmupAdvice_containsProtocol() async throws {
        let response = try await sut.sendMessage("How should I warm up?", context: emptyContext)
        XCTAssertTrue(response.contains("Warm") || response.contains("warm"))
        XCTAssertTrue(response.contains("bar") || response.contains("cardio"))
    }

    // MARK: - RPE / RIR

    func testRPEExplanation_containsScale() async throws {
        let response = try await sut.sendMessage("What is RPE?", context: emptyContext)
        XCTAssertTrue(response.contains("RPE") || response.contains("RIR"))
    }

    func testRIRExplanation_triggered() async throws {
        let response = try await sut.sendMessage("How many reps in reserve should I leave?", context: emptyContext)
        XCTAssertTrue(response.contains("RPE") || response.contains("RIR"))
    }

    // MARK: - Progressive Overload

    func testProgressiveOverload_beginnerIncrement() async throws {
        let profile = UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "metric")
        let context = CoachContext(userProfile: profile)
        let response = try await sut.sendMessage("How should I add weight?", context: context)
        XCTAssertTrue(response.contains("per session"), "Beginner should see per-session advice")
    }

    func testProgressiveOverload_advancedIncrement() async throws {
        let profile = UserProfileSnapshot(experienceLevel: "advanced", unitSystem: "metric")
        let context = CoachContext(userProfile: profile)
        let response = try await sut.sendMessage("How should I increase weight?", context: context)
        XCTAssertTrue(response.contains("per month"), "Advanced should see per-month advice")
    }

    // MARK: - Offline Fallback

    func testUnrecognizedQuestion_returnsOfflineMessage() async throws {
        let response = try await sut.sendMessage("Tell me about quantum physics", context: emptyContext)
        XCTAssertTrue(response.contains("Offline") || response.contains("offline"))
    }

    // MARK: - Streaming

    func testStreamMessage_yieldsTokens() async throws {
        var tokens: [String] = []
        let stream = sut.streamMessage("What is RPE?", context: emptyContext)
        for try await token in stream {
            tokens.append(token)
        }
        XCTAssertFalse(tokens.isEmpty, "Stream should yield at least one token")
        let fullResponse = tokens.joined()
        XCTAssertTrue(fullResponse.contains("RPE"), "Reassembled stream should contain RPE info")
    }

    func testStreamMessage_safetyTrigger() async throws {
        var tokens: [String] = []
        let stream = sut.streamMessage("diagnose my knee pain", context: emptyContext)
        for try await token in stream {
            tokens.append(token)
        }
        let fullResponse = tokens.joined()
        XCTAssertTrue(fullResponse.contains("medical") || fullResponse.contains("doctor"),
            "Stream with safety trigger should return advisory")
    }

    // MARK: - Disclaimer

    func testAllResponsesContainDisclaimer() async throws {
        let safeQuestions = [
            "What rest time should I take?",
            "When should I deload?",
            "I'm stuck on bench press",
            "How should I warm up?",
            "What is RPE?",
            "How do I add weight?",
            "What should I do today?",
            "Tell me about training"
        ]

        for question in safeQuestions {
            let response = try await sut.sendMessage(question, context: emptyContext)
            XCTAssertTrue(
                response.contains("AI suggestions are not medical advice") ||
                response.contains("Offline Mode") ||
                response.contains("medical advice"),
                "Response to '\(question)' should contain disclaimer or safety advisory"
            )
        }
    }

    // MARK: - Empty / Edge Case Input

    func testEmptyMessage_doesNotCrash() async throws {
        let response = try await sut.sendMessage("", context: emptyContext)
        XCTAssertFalse(response.isEmpty)
    }

    func testVeryLongMessage_doesNotCrash() async throws {
        let longMessage = String(repeating: "add weight ", count: 1000)
        let response = try await sut.sendMessage(longMessage, context: emptyContext)
        XCTAssertFalse(response.isEmpty)
    }

    func testSpecialCharactersInMessage() async throws {
        let response = try await sut.sendMessage("How should I warm up? 🏋️‍♂️💪", context: emptyContext)
        XCTAssertFalse(response.isEmpty)
    }

    func testUnicodeMessage() async throws {
        let response = try await sut.sendMessage("ウォームアップ warm up 热身", context: emptyContext)
        XCTAssertTrue(response.contains("Warm") || response.contains("warm") || response.contains("Offline"),
            "Should match partial English or fall back to offline")
    }
}
