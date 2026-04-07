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
}
