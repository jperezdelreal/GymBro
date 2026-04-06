import XCTest
@testable import GymBroCore

final class GymBroCoreTests: XCTestCase {
    func testModelsCompile() throws {
        let profile = UserProfile()
        XCTAssertEqual(profile.unitSystem, .metric)
        XCTAssertEqual(profile.experienceLevel, .intermediate)
    }
    
    func testExerciseSetE1RM() throws {
        let set = ExerciseSet(weightKg: 100, reps: 5)
        let e1rm = set.estimatedOneRepMax
        XCTAssertGreaterThan(e1rm, 100)
    }
    
    func testWorkoutVolume() throws {
        let workout = Workout()
        let set1 = ExerciseSet(weightKg: 100, reps: 5)
        let set2 = ExerciseSet(weightKg: 100, reps: 5)
        
        XCTAssertEqual(workout.totalVolume, 0)
    }
}
