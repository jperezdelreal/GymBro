import XCTest
@testable import GymBroCore

/// Tests the integrity of the exercises-seed.json data file.
/// Validates structure, uniqueness, and enum compatibility without needing SwiftData.
final class ExerciseSeedDataTests: XCTestCase {

    private struct ExerciseSeedData: Codable {
        let name: String
        let category: String
        let equipment: String
        let instructions: String
        let muscleGroups: [MuscleGroupSeed]
    }

    private struct MuscleGroupSeed: Codable {
        let name: String
        let isPrimary: Bool
    }

    private var seedData: [ExerciseSeedData]!

    override func setUp() {
        super.setUp()
        // Navigate from test file to the seed data in the source target
        let testFile = URL(fileURLWithPath: #filePath)
        let packageRoot = testFile
            .deletingLastPathComponent() // GymBroCoreTests/
            .deletingLastPathComponent() // Tests/
            .deletingLastPathComponent() // GymBroCore package root
        let seedURL = packageRoot
            .appendingPathComponent("Sources")
            .appendingPathComponent("GymBroCore")
            .appendingPathComponent("Resources")
            .appendingPathComponent("exercises-seed.json")

        do {
            let data = try Data(contentsOf: seedURL)
            seedData = try JSONDecoder().decode([ExerciseSeedData].self, from: data)
        } catch {
            XCTFail("Failed to load exercises-seed.json from \(seedURL.path): \(error)")
        }
    }

    // MARK: - Basic Integrity

    func testSeedFileIsNotEmpty() {
        XCTAssertFalse(seedData.isEmpty, "Seed data must contain exercises")
    }

    func testSeedFileContainsMinimumExercises() {
        XCTAssertGreaterThanOrEqual(seedData.count, 200,
            "Seed data should contain at least 200 exercises for a comprehensive exercise library")
    }

    // MARK: - Name Uniqueness

    func testExerciseNamesAreUnique() {
        let names = seedData.map(\.name)
        let uniqueNames = Set(names)
        XCTAssertEqual(names.count, uniqueNames.count,
            "Exercise names must be unique. Duplicates: \(names.filter { name in names.filter { $0 == name }.count > 1 })")
    }

    func testNoEmptyExerciseNames() {
        for exercise in seedData {
            XCTAssertFalse(exercise.name.trimmingCharacters(in: .whitespaces).isEmpty,
                "Exercise name must not be empty or whitespace-only")
        }
    }

    // MARK: - Category Validation

    func testAllCategoriesAreValid() {
        let validCategories = Set(["compound", "isolation", "accessory"])
        for exercise in seedData {
            XCTAssertTrue(validCategories.contains(exercise.category),
                "'\(exercise.name)' has invalid category '\(exercise.category)'. Valid: \(validCategories)")
        }
    }

    func testContainsCompoundExercises() {
        let compounds = seedData.filter { $0.category == "compound" }
        XCTAssertGreaterThan(compounds.count, 0, "Seed data must include compound exercises")
    }

    func testContainsIsolationExercises() {
        let isolations = seedData.filter { $0.category == "isolation" }
        XCTAssertGreaterThan(isolations.count, 0, "Seed data must include isolation exercises")
    }

    // MARK: - Equipment Validation

    func testAllEquipmentTypesAreValid() {
        let validEquipment = Set(["barbell", "dumbbell", "kettlebell", "machine", "cable", "bodyweight", "band", "other"])
        for exercise in seedData {
            XCTAssertTrue(validEquipment.contains(exercise.equipment),
                "'\(exercise.name)' has invalid equipment '\(exercise.equipment)'. Valid: \(validEquipment)")
        }
    }

    // MARK: - Muscle Groups

    func testAllExercisesHaveAtLeastOneMuscleGroup() {
        for exercise in seedData {
            XCTAssertFalse(exercise.muscleGroups.isEmpty,
                "'\(exercise.name)' must have at least one muscle group")
        }
    }

    func testAllExercisesHaveAtLeastOnePrimaryMuscle() {
        for exercise in seedData {
            let hasPrimary = exercise.muscleGroups.contains { $0.isPrimary }
            XCTAssertTrue(hasPrimary,
                "'\(exercise.name)' must have at least one primary muscle group")
        }
    }

    func testNoEmptyMuscleGroupNames() {
        for exercise in seedData {
            for mg in exercise.muscleGroups {
                XCTAssertFalse(mg.name.trimmingCharacters(in: .whitespaces).isEmpty,
                    "Muscle group name must not be empty for exercise '\(exercise.name)'")
            }
        }
    }

    // MARK: - Instructions

    func testAllExercisesHaveInstructions() {
        for exercise in seedData {
            XCTAssertFalse(exercise.instructions.trimmingCharacters(in: .whitespaces).isEmpty,
                "'\(exercise.name)' must have non-empty instructions")
        }
    }
    
    func testInstructionsAreDetailed() {
        // Target: 200+ chars average (significantly more detailed than original 80 chars)
        let shortInstructions = seedData.filter { $0.instructions.count < 150 }
        
        // Allow some exercises to have shorter instructions, but most should be detailed
        let percentageShort = Double(shortInstructions.count) / Double(seedData.count)
        XCTAssertLessThan(percentageShort, 0.2,
            "More than 20% of exercises have instructions under 150 chars. Short instructions: \(shortInstructions.map { "\($0.name) (\($0.instructions.count) chars)" }.joined(separator: ", "))")
    }
    
    func testAverageInstructionLength() {
        let totalLength = seedData.reduce(0) { $0 + $1.instructions.count }
        let average = Double(totalLength) / Double(seedData.count)
        
        XCTAssertGreaterThanOrEqual(average, 180.0,
            "Average instruction length should be at least 180 chars (currently: \(Int(average)) chars)")
    }

    // MARK: - Key Exercises Present

    func testContainsBigThree() {
        let names = Set(seedData.map { $0.name.lowercased() })
        let hasSquat = names.contains { $0.contains("squat") }
        let hasBench = names.contains { $0.contains("bench press") }
        let hasDeadlift = names.contains { $0.contains("deadlift") }

        XCTAssertTrue(hasSquat, "Seed data must include a squat variation")
        XCTAssertTrue(hasBench, "Seed data must include a bench press variation")
        XCTAssertTrue(hasDeadlift, "Seed data must include a deadlift variation")
    }

    // MARK: - Enum Compatibility

    func testCategoriesParseToCoreEnum() {
        for exercise in seedData {
            let parsed = ExerciseCategory(rawValue: exercise.category)
            XCTAssertNotNil(parsed,
                "Category '\(exercise.category)' for '\(exercise.name)' must parse to ExerciseCategory enum")
        }
    }

    func testEquipmentParseToCoreEnum() {
        for exercise in seedData {
            let parsed = Equipment(rawValue: exercise.equipment)
            XCTAssertNotNil(parsed,
                "Equipment '\(exercise.equipment)' for '\(exercise.name)' must parse to Equipment enum")
        }
    }
}
