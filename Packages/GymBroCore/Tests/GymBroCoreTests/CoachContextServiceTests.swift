import XCTest
import SwiftData
@testable import GymBroCore

@MainActor
final class CoachContextServiceTests: XCTestCase {

    private var modelContainer: ModelContainer!
    private var modelContext: ModelContext!

    override func setUp() {
        super.setUp()
        let schema = Schema([Workout.self, Exercise.self, ExerciseSet.self, ChatMessage.self])
        let config = ModelConfiguration(isStoredInMemoryOnly: true)
        modelContainer = try! ModelContainer(for: schema, configurations: [config])
        modelContext = ModelContext(modelContainer)
    }

    override func tearDown() {
        modelContainer = nil
        modelContext = nil
        super.tearDown()
    }

    func testEmptyDataReturnsZeros() {
        let service = CoachContextService(modelContext: modelContext)
        let summary = service.fetchContextSummary()
        XCTAssertEqual(summary.workoutCount, 0)
        XCTAssertEqual(summary.weeksOfData, 0)
        XCTAssertNil(summary.lastWorkoutDate)
    }

    func testContextSummaryDefaultInit() {
        let summary = CoachContextSummary()
        XCTAssertEqual(summary.workoutCount, 0)
        XCTAssertEqual(summary.weeksOfData, 0)
        XCTAssertNil(summary.lastWorkoutDate)
    }

    func testContextSummaryWithValues() {
        let date = Date()
        let summary = CoachContextSummary(workoutCount: 10, weeksOfData: 4, lastWorkoutDate: date)
        XCTAssertEqual(summary.workoutCount, 10)
        XCTAssertEqual(summary.weeksOfData, 4)
        XCTAssertEqual(summary.lastWorkoutDate, date)
    }
}
