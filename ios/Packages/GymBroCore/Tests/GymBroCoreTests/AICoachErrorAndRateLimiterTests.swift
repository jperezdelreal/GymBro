import XCTest
@testable import GymBroCore

/// Tests for AICoachError descriptions and RateLimiter behavior.
final class AICoachErrorAndRateLimiterTests: XCTestCase {

    // MARK: - AICoachError Descriptions

    func testInvalidConfigurationError_description() {
        let error = AICoachError.invalidConfiguration("bad endpoint")
        XCTAssertTrue(error.localizedDescription.contains("Configuration"))
        XCTAssertTrue(error.localizedDescription.contains("bad endpoint"))
    }

    func testNetworkError_description() {
        let error = AICoachError.networkError("timeout")
        XCTAssertTrue(error.localizedDescription.contains("Network"))
        XCTAssertTrue(error.localizedDescription.contains("timeout"))
    }

    func testAuthenticationFailedError_description() {
        let error = AICoachError.authenticationFailed
        XCTAssertTrue(error.localizedDescription.contains("authentication") ||
                       error.localizedDescription.contains("API key"))
    }

    func testRateLimitedError_description() {
        let error = AICoachError.rateLimited
        XCTAssertTrue(error.localizedDescription.contains("many requests") ||
                       error.localizedDescription.contains("wait"))
    }

    func testServerError_description() {
        let error = AICoachError.serverError(503)
        XCTAssertTrue(error.localizedDescription.contains("503"))
    }

    func testEmptyResponseError_description() {
        let error = AICoachError.emptyResponse
        XCTAssertTrue(error.localizedDescription.contains("empty"))
    }

    func testOfflineUnavailableError_description() {
        let error = AICoachError.offlineUnavailable
        XCTAssertTrue(error.localizedDescription.contains("internet"))
    }

    // MARK: - RateLimiter

    func testRateLimiter_allowsUnderLimit() async {
        let limiter = RateLimiter()
        // Should not throw for a few requests
        for _ in 0..<5 {
            do {
                try await limiter.checkLimit()
                await limiter.recordRequest()
            } catch {
                XCTFail("Should not throw under limit: \(error)")
            }
        }
    }

    func testRateLimiter_blocksOverLimit() async {
        let limiter = RateLimiter()
        // Fill up to the limit (20 per minute)
        for _ in 0..<20 {
            await limiter.recordRequest()
        }

        do {
            try await limiter.checkLimit()
            XCTFail("Should have thrown rateLimited error")
        } catch let error as AICoachError {
            if case .rateLimited = error {
                // Expected
            } else {
                XCTFail("Expected .rateLimited, got \(error)")
            }
        } catch {
            XCTFail("Expected AICoachError, got \(error)")
        }
    }

    func testRateLimiter_recordRequestIncrements() async {
        let limiter = RateLimiter()
        // Record 19, should still be fine
        for _ in 0..<19 {
            await limiter.recordRequest()
        }
        do {
            try await limiter.checkLimit()
        } catch {
            XCTFail("19 requests should be under the 20/min limit")
        }

        await limiter.recordRequest() // #20
        do {
            try await limiter.checkLimit()
            XCTFail("20 requests should trigger rate limit")
        } catch {
            // Expected
        }
    }

    // MARK: - CoachContext Defaults

    func testCoachContext_defaultValues() {
        let context = CoachContext()
        XCTAssertNil(context.userProfile)
        XCTAssertTrue(context.recentWorkouts.isEmpty)
        XCTAssertNil(context.activeProgram)
        XCTAssertTrue(context.personalRecords.isEmpty)
    }

    func testCoachContext_partialInit() {
        let profile = UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "metric")
        let context = CoachContext(userProfile: profile)
        XCTAssertNotNil(context.userProfile)
        XCTAssertTrue(context.recentWorkouts.isEmpty)
    }

    // MARK: - Snapshot Types

    func testUserProfileSnapshot_optionalBodyweight() {
        let withBW = UserProfileSnapshot(experienceLevel: "advanced", unitSystem: "metric", bodyweightKg: 90)
        XCTAssertEqual(withBW.bodyweightKg, 90)

        let withoutBW = UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "imperial")
        XCTAssertNil(withoutBW.bodyweightKg)
    }

    func testWorkoutSnapshot_optionalDuration() {
        let withDuration = WorkoutSnapshot(
            date: Date(), exercises: [], totalVolume: 1000, durationMinutes: 60
        )
        XCTAssertEqual(withDuration.durationMinutes, 60)

        let withoutDuration = WorkoutSnapshot(
            date: Date(), exercises: [], totalVolume: 0
        )
        XCTAssertNil(withoutDuration.durationMinutes)
    }

    func testExerciseSnapshot_properties() {
        let snap = ExerciseSnapshot(name: "Squat", sets: 5, bestWeight: 140, bestReps: 5)
        XCTAssertEqual(snap.name, "Squat")
        XCTAssertEqual(snap.sets, 5)
        XCTAssertEqual(snap.bestWeight, 140)
        XCTAssertEqual(snap.bestReps, 5)
    }

    func testPRSnapshot_properties() {
        let now = Date()
        let snap = PRSnapshot(exerciseName: "Deadlift", weightKg: 250, reps: 1, date: now)
        XCTAssertEqual(snap.exerciseName, "Deadlift")
        XCTAssertEqual(snap.weightKg, 250)
        XCTAssertEqual(snap.reps, 1)
        XCTAssertEqual(snap.date, now)
    }

    func testProgramSnapshot_properties() {
        let snap = ProgramSnapshot(name: "531", periodization: "linear", weekNumber: 3, frequencyPerWeek: 4)
        XCTAssertEqual(snap.name, "531")
        XCTAssertEqual(snap.periodization, "linear")
        XCTAssertEqual(snap.weekNumber, 3)
        XCTAssertEqual(snap.frequencyPerWeek, 4)
    }

    // MARK: - AICoachError Transient Classification

    func testTransientErrors_areClassifiedCorrectly() {
        XCTAssertTrue(AICoachError.rateLimited.isTransient)
        XCTAssertTrue(AICoachError.emptyResponse.isTransient)
        XCTAssertTrue(AICoachError.serverError(500).isTransient)
        XCTAssertTrue(AICoachError.serverError(502).isTransient)
        XCTAssertTrue(AICoachError.serverError(503).isTransient)
        XCTAssertTrue(AICoachError.networkError("timeout").isTransient)
    }

    func testNonTransientErrors_areClassifiedCorrectly() {
        XCTAssertFalse(AICoachError.authenticationFailed.isTransient)
        XCTAssertFalse(AICoachError.invalidConfiguration("bad").isTransient)
        XCTAssertFalse(AICoachError.offlineUnavailable.isTransient)
        XCTAssertFalse(AICoachError.serverError(401).isTransient)
        XCTAssertFalse(AICoachError.serverError(403).isTransient)
        XCTAssertFalse(AICoachError.serverError(400).isTransient)
    }

    func testRetriesExhausted_description() {
        let inner = AICoachError.serverError(503)
        let error = AICoachError.retriesExhausted(lastError: inner)
        XCTAssertTrue(error.localizedDescription.contains("multiple attempts"))
        XCTAssertFalse(error.isTransient)
    }

    func testIsTransientError_handlesURLError() {
        let timeout = URLError(.timedOut)
        XCTAssertTrue(AICoachError.isTransientError(timeout))

        let connectionLost = URLError(.networkConnectionLost)
        XCTAssertTrue(AICoachError.isTransientError(connectionLost))

        let cancelled = URLError(.cancelled)
        XCTAssertFalse(AICoachError.isTransientError(cancelled))
    }

    // MARK: - RetryPolicy

    func testRetryPolicy_defaultValues() {
        let policy = RetryPolicy.default
        XCTAssertEqual(policy.maxAttempts, 3)
        XCTAssertEqual(policy.delays.count, 3)
    }

    func testRetryPolicy_delayForAttempt() {
        let policy = RetryPolicy.default
        XCTAssertEqual(policy.delay(forAttempt: 0), .milliseconds(100))
        XCTAssertEqual(policy.delay(forAttempt: 1), .seconds(1))
        XCTAssertEqual(policy.delay(forAttempt: 2), .seconds(5))
        // Out of bounds clamps to last
        XCTAssertEqual(policy.delay(forAttempt: 99), .seconds(5))
    }

    func testRetryPolicy_customValues() {
        let policy = RetryPolicy(maxAttempts: 2, delays: [.milliseconds(50), .milliseconds(200)])
        XCTAssertEqual(policy.maxAttempts, 2)
        XCTAssertEqual(policy.delay(forAttempt: 0), .milliseconds(50))
        XCTAssertEqual(policy.delay(forAttempt: 1), .milliseconds(200))
    }

    // MARK: - RetryingAICoachService

    func testRetry_succeedsAfterTransientFailure() async throws {
        let mock = MockAICoachService(
            responses: [
                .failure(AICoachError.serverError(503)),
                .success("recovered response")
            ]
        )
        let retrying = RetryingAICoachService(
            primary: mock,
            retryPolicy: RetryPolicy(maxAttempts: 3, delays: [.milliseconds(10), .milliseconds(10), .milliseconds(10)])
        )

        let result = try await retrying.sendMessage("test", context: CoachContext())
        XCTAssertEqual(result, "recovered response")
        XCTAssertEqual(mock.callCount, 2)
    }

    func testRetry_failsImmediatelyOnAuthError() async {
        let mock = MockAICoachService(
            responses: [.failure(AICoachError.authenticationFailed)]
        )
        let retrying = RetryingAICoachService(
            primary: mock,
            retryPolicy: RetryPolicy(maxAttempts: 3, delays: [.milliseconds(10), .milliseconds(10), .milliseconds(10)])
        )

        do {
            _ = try await retrying.sendMessage("test", context: CoachContext())
            XCTFail("Should have thrown immediately")
        } catch let error as AICoachError {
            if case .authenticationFailed = error {
                // Expected — no retry
            } else {
                XCTFail("Expected .authenticationFailed, got \(error)")
            }
        }
        XCTAssertEqual(mock.callCount, 1, "Auth errors must not be retried")
    }

    func testRetry_fallsBackAfterAllRetriesExhausted() async throws {
        let mock = MockAICoachService(
            responses: [
                .failure(AICoachError.serverError(502)),
                .failure(AICoachError.serverError(502)),
                .failure(AICoachError.serverError(502))
            ]
        )
        let retrying = RetryingAICoachService(
            primary: mock,
            retryPolicy: RetryPolicy(maxAttempts: 3, delays: [.milliseconds(10), .milliseconds(10), .milliseconds(10)])
        )

        let result = try await retrying.sendMessage("rest time", context: CoachContext())
        // DeterministicCoachFallback responds with rest time advice
        XCTAssertTrue(result.contains("Rest") || result.contains("offline") || result.contains("Offline"))
        XCTAssertEqual(mock.callCount, 3)
    }

    func testRetry_stream_succeedsAfterTransientFailure() async throws {
        let mock = MockStreamingAICoachService(
            streamResults: [
                .failure(AICoachError.networkError("timeout")),
                .success(["Hello", " world"])
            ]
        )
        let retrying = RetryingAICoachService(
            primary: mock,
            retryPolicy: RetryPolicy(maxAttempts: 3, delays: [.milliseconds(10), .milliseconds(10), .milliseconds(10)])
        )

        var tokens: [String] = []
        let stream = retrying.streamMessage("test", context: CoachContext())
        for try await token in stream {
            tokens.append(token)
        }
        XCTAssertTrue(tokens.contains("Hello"))
        XCTAssertTrue(tokens.contains(" world"))
    }

    func testRetry_stream_failsImmediatelyOnNonTransient() async {
        let mock = MockStreamingAICoachService(
            streamResults: [.failure(AICoachError.authenticationFailed)]
        )
        let retrying = RetryingAICoachService(
            primary: mock,
            retryPolicy: RetryPolicy(maxAttempts: 3, delays: [.milliseconds(10), .milliseconds(10), .milliseconds(10)])
        )

        var caughtError: Error?
        let stream = retrying.streamMessage("test", context: CoachContext())
        do {
            for try await _ in stream { }
        } catch {
            caughtError = error
        }
        XCTAssertNotNil(caughtError)
        XCTAssertTrue(caughtError is AICoachError)
    }
}

// MARK: - Test Helpers

/// Mock that returns pre-configured responses in order for sendMessage.
private final class MockAICoachService: AICoachService {
    private let responses: [Result<String, Error>]
    private(set) var callCount = 0

    init(responses: [Result<String, Error>]) {
        self.responses = responses
    }

    func sendMessage(_ message: String, context: CoachContext) async throws -> String {
        let index = min(callCount, responses.count - 1)
        callCount += 1
        return try responses[index].get()
    }

    func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { $0.finish() }
    }
}

/// Mock that returns pre-configured stream results in order for streamMessage.
private final class MockStreamingAICoachService: AICoachService {
    private let streamResults: [Result<[String], Error>]
    private var streamCallCount = 0

    init(streamResults: [Result<[String], Error>]) {
        self.streamResults = streamResults
    }

    func sendMessage(_ message: String, context: CoachContext) async throws -> String {
        "mock"
    }

    func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error> {
        let index = min(streamCallCount, streamResults.count - 1)
        streamCallCount += 1
        let result = streamResults[index]

        return AsyncThrowingStream { continuation in
            Task {
                switch result {
                case .success(let tokens):
                    for token in tokens {
                        continuation.yield(token)
                    }
                    continuation.finish()
                case .failure(let error):
                    continuation.finish(throwing: error)
                }
            }
        }
    }
}
