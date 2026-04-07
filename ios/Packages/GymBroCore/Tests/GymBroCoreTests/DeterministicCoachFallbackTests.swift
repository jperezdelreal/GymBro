import XCTest
@testable import GymBroCore

final class DeterministicCoachFallbackTests: XCTestCase {

    let coach = DeterministicCoachFallback()
    let emptyContext = CoachContext()

    func testRestTimeAdvice() async throws {
        let response = try await coach.sendMessage("How long should I rest between sets?", context: emptyContext)
        XCTAssertTrue(response.contains("Rest Time"))
        XCTAssertTrue(response.contains("Compound lifts"))
    }

    func testDeloadAdvice() async throws {
        let response = try await coach.sendMessage("When should I deload?", context: emptyContext)
        XCTAssertTrue(response.contains("Deload"))
    }

    func testDeloadAdviceUsesExperienceLevel() async throws {
        let context = CoachContext(
            userProfile: UserProfileSnapshot(experienceLevel: "advanced", unitSystem: "metric")
        )
        let response = try await coach.sendMessage("When should I deload?", context: context)
        XCTAssertTrue(response.contains("advanced"))
    }

    func testPlateauAdvice() async throws {
        let response = try await coach.sendMessage("I'm stuck on my bench press", context: emptyContext)
        XCTAssertTrue(response.contains("Plateau"))
    }

    func testWarmupAdvice() async throws {
        let response = try await coach.sendMessage("How should I warm up?", context: emptyContext)
        XCTAssertTrue(response.contains("Warm-Up"))
    }

    func testRPEExplanation() async throws {
        let response = try await coach.sendMessage("What is RPE?", context: emptyContext)
        XCTAssertTrue(response.contains("RPE"))
        XCTAssertTrue(response.contains("RIR"))
    }

    func testProgressiveOverload() async throws {
        let response = try await coach.sendMessage("How do I add weight progressively?", context: emptyContext)
        XCTAssertTrue(response.contains("Progressive Overload"))
    }

    func testUnknownQuestionReturnsOfflineMessage() async throws {
        let response = try await coach.sendMessage("What's the meaning of life?", context: emptyContext)
        XCTAssertTrue(response.contains("Offline Mode"))
    }

    func testSafetyFilterTriggersOnMedicalQuery() async throws {
        let response = try await coach.sendMessage("Can you diagnose my torn shoulder?", context: emptyContext)
        XCTAssertTrue(response.contains("medical advice") || response.contains("doctor"))
    }

    func testStreamMessageYieldsTokens() async throws {
        var tokens: [String] = []
        for try await token in coach.streamMessage("How should I warm up?", context: emptyContext) {
            tokens.append(token)
        }
        XCTAssertFalse(tokens.isEmpty, "Stream should yield at least one token")
        let fullText = tokens.joined()
        XCTAssertTrue(fullText.contains("Warm-Up"))
    }
}
