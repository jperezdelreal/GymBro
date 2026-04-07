import XCTest
@testable import GymBroCore

final class PromptBuilderTests: XCTestCase {

    let builder = PromptBuilder()

    func testBuildSystemPromptWithEmptyContext() {
        let context = CoachContext()
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("GymBro Coach"))
        XCTAssertTrue(prompt.contains("Safety Rules"))
        XCTAssertTrue(prompt.contains("NEVER provide medical advice"))
    }

    func testBuildSystemPromptIncludesUserProfile() {
        let profile = UserProfileSnapshot(
            experienceLevel: "advanced",
            unitSystem: "metric",
            bodyweightKg: 90.0
        )
        let context = CoachContext(userProfile: profile)
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("Athlete Profile"))
        XCTAssertTrue(prompt.contains("advanced"))
        XCTAssertTrue(prompt.contains("90.0"))
    }

    func testBuildSystemPromptIncludesRecentWorkouts() {
        let workouts = [
            WorkoutSnapshot(
                date: Date(),
                exercises: [
                    ExerciseSnapshot(name: "Squat", sets: 5, bestWeight: 140.0, bestReps: 5)
                ],
                totalVolume: 3500
            )
        ]
        let context = CoachContext(recentWorkouts: workouts)
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("Recent Workouts"))
        XCTAssertTrue(prompt.contains("Squat"))
    }

    func testBuildSystemPromptIncludesProgram() {
        let program = ProgramSnapshot(
            name: "Wendler 531",
            periodization: "undulating",
            weekNumber: 3,
            frequencyPerWeek: 4
        )
        let context = CoachContext(activeProgram: program)
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("Active Program"))
        XCTAssertTrue(prompt.contains("Wendler 531"))
    }

    func testBuildSystemPromptIncludesPRs() {
        let prs = [
            PRSnapshot(exerciseName: "Deadlift", weightKg: 220.0, reps: 1, date: Date())
        ]
        let context = CoachContext(personalRecords: prs)
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("Personal Records"))
        XCTAssertTrue(prompt.contains("Deadlift"))
        XCTAssertTrue(prompt.contains("220.0"))
    }

    func testBuildSystemPromptAlwaysIncludesSafety() {
        let context = CoachContext(
            userProfile: UserProfileSnapshot(experienceLevel: "beginner", unitSystem: "imperial"),
            recentWorkouts: [],
            activeProgram: nil,
            personalRecords: []
        )
        let prompt = builder.buildSystemPrompt(context: context)

        XCTAssertTrue(prompt.contains("NEVER provide medical advice"))
        XCTAssertTrue(prompt.contains("consult a qualified medical professional"))
    }
}
