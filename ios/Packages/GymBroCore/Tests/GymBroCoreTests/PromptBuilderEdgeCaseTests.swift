import XCTest
@testable import GymBroCore

/// Extended edge case tests for PromptBuilder.
/// Addresses issue #27: special characters, empty context, very long inputs, boundary conditions.
final class PromptBuilderEdgeCaseTests: XCTestCase {

    let builder = PromptBuilder()

    // MARK: - Nil and Empty Context Sections

    func testNoProfile_noProfileSection() {
        let context = CoachContext(userProfile: nil)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertFalse(prompt.contains("Athlete Profile"))
    }

    func testEmptyWorkouts_noWorkoutsSection() {
        let context = CoachContext(recentWorkouts: [])
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertFalse(prompt.contains("Recent Workouts"))
    }

    func testNoProgram_noProgramSection() {
        let context = CoachContext(activeProgram: nil)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertFalse(prompt.contains("Active Program"))
    }

    func testEmptyPRs_noPRSection() {
        let context = CoachContext(personalRecords: [])
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertFalse(prompt.contains("Personal Records"))
    }

    // MARK: - Full Context

    func testFullContext_includesAllSections() {
        let context = CoachContext(
            userProfile: UserProfileSnapshot(experienceLevel: "elite", unitSystem: "imperial", bodyweightKg: 100),
            recentWorkouts: [WorkoutSnapshot(date: Date(), exercises: [ExerciseSnapshot(name: "Squat", sets: 5, bestWeight: 200, bestReps: 5)], totalVolume: 5000)],
            activeProgram: ProgramSnapshot(name: "5/3/1", periodization: "undulating", weekNumber: 2, frequencyPerWeek: 4),
            personalRecords: [PRSnapshot(exerciseName: "Deadlift", weightKg: 250, reps: 1, date: Date())]
        )
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("GymBro Coach"))
        XCTAssertTrue(prompt.contains("Athlete Profile"))
        XCTAssertTrue(prompt.contains("Recent Workouts"))
        XCTAssertTrue(prompt.contains("Active Program"))
        XCTAssertTrue(prompt.contains("Personal Records"))
        XCTAssertTrue(prompt.contains("Safety Rules"))
    }

    // MARK: - Special Characters in Data

    func testExerciseNameWithSpecialChars() {
        let workouts = [
            WorkoutSnapshot(
                date: Date(),
                exercises: [
                    ExerciseSnapshot(name: "Squat (Pause) — 3-0-1", sets: 3, bestWeight: 100, bestReps: 5)
                ],
                totalVolume: 1500
            )
        ]
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertTrue(prompt.contains("Squat (Pause)"), "Should handle special chars in exercise names")
    }

    func testExerciseNameWithEmoji() {
        let workouts = [
            WorkoutSnapshot(
                date: Date(),
                exercises: [ExerciseSnapshot(name: "Bench 🏋️", sets: 3, bestWeight: 100, bestReps: 5)],
                totalVolume: 1500
            )
        ]
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertTrue(prompt.contains("Bench 🏋️"))
    }

    // MARK: - Profile Edge Cases

    func testProfileWithNilBodyweight() {
        let profile = UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "metric", bodyweightKg: nil)
        let context = CoachContext(userProfile: profile)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertTrue(prompt.contains("beginner"))
        XCTAssertFalse(prompt.contains("Bodyweight"), "Should not include bodyweight line when nil")
    }

    func testProfileWithZeroBodyweight() {
        let profile = UserProfileSnapshot(experienceLevel: "intermediate", unitSystem: "metric", bodyweightKg: 0)
        let context = CoachContext(userProfile: profile)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertTrue(prompt.contains("0.0"), "Should include even zero bodyweight if provided")
    }

    // MARK: - Workout Limits

    func testMoreThanFiveWorkouts_onlyShowsFive() {
        let workouts = (0..<10).map { i in
            WorkoutSnapshot(
                date: Date().addingTimeInterval(-Double(i) * 86400),
                exercises: [ExerciseSnapshot(name: "Exercise \(i)", sets: 3, bestWeight: 100, bestReps: 5)],
                totalVolume: 1500
            )
        }
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)

        // Should show "last 10" in header but only render 5
        XCTAssertTrue(prompt.contains("last 10"))
        let exerciseMatches = prompt.components(separatedBy: "Exercise ").count - 1
        XCTAssertLessThanOrEqual(exerciseMatches, 5, "Should display at most 5 workouts")
    }

    func testMoreThanTenPRs_onlyShowsTen() {
        let prs = (0..<15).map { i in
            PRSnapshot(exerciseName: "Exercise \(i)", weightKg: Double(100 + i), reps: 5, date: Date())
        }
        let context = CoachContext(personalRecords: prs)
        let prompt = builder.buildSystemPrompt(context: context)

        let prLineCount = prompt.components(separatedBy: "\n").filter { $0.contains("Exercise ") }.count
        XCTAssertLessThanOrEqual(prLineCount, 10, "Should display at most 10 PRs")
    }

    // MARK: - Duration Formatting

    func testWorkoutWithDuration() {
        let workouts = [
            WorkoutSnapshot(
                date: Date(),
                exercises: [ExerciseSnapshot(name: "Squat", sets: 5, bestWeight: 140, bestReps: 5)],
                totalVolume: 3500,
                durationMinutes: 75.0
            )
        ]
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertTrue(prompt.contains("75min"), "Should include duration when available")
    }

    func testWorkoutWithoutDuration() {
        let workouts = [
            WorkoutSnapshot(
                date: Date(),
                exercises: [ExerciseSnapshot(name: "Squat", sets: 5, bestWeight: 140, bestReps: 5)],
                totalVolume: 3500,
                durationMinutes: nil
            )
        ]
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)
        XCTAssertFalse(prompt.contains("min)"), "Should not include duration when nil")
    }

    // MARK: - Safety Rules Always Present

    func testSafetyRulesIncludedInEveryPrompt() {
        let contexts = [
            CoachContext(),
            CoachContext(userProfile: UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "metric")),
            CoachContext(recentWorkouts: []),
        ]

        for (index, context) in contexts.enumerated() {
            let prompt = builder.buildSystemPrompt(context: context)
            XCTAssertTrue(prompt.contains("Safety Rules"),
                "Context \(index) should include Safety Rules")
            XCTAssertTrue(prompt.contains("NEVER provide medical advice"),
                "Context \(index) should include medical safety rule")
        }
    }
}
