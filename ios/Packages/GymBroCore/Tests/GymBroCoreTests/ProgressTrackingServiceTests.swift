import XCTest
@testable import GymBroCore

final class ProgressTrackingServiceTests: XCTestCase {

    let service = ProgressTrackingService()

    // MARK: - Helpers

    private func makeSet(
        weightKg: Double,
        reps: Int,
        setType: SetType = .working,
        date: Date = Date()
    ) -> ExerciseSet {
        let set = ExerciseSet(weightKg: weightKg, reps: reps, setType: setType)
        set.createdAt = date
        return set
    }

    // MARK: - e1RM Trend

    func testE1RMTrend_withWorkingSets_returnsChronologicalData() {
        let now = Date()
        let sets = [
            makeSet(weightKg: 100, reps: 5, date: now.addingTimeInterval(-86400 * 7)),
            makeSet(weightKg: 105, reps: 5, date: now.addingTimeInterval(-86400 * 3)),
            makeSet(weightKg: 110, reps: 5, date: now),
        ]

        let trend = service.e1rmTrend(sets: sets, exerciseName: "Squat", timeWindow: .oneMonth)

        XCTAssertEqual(trend.count, 3)
        XCTAssertTrue(trend[0].date < trend[1].date)
        XCTAssertTrue(trend[1].date < trend[2].date)
        XCTAssertTrue(trend[0].e1rm < trend[1].e1rm)
        XCTAssertTrue(trend[1].e1rm < trend[2].e1rm)
    }

    func testE1RMTrend_warmupSetsExcluded() {
        let now = Date()
        let sets = [
            makeSet(weightKg: 60, reps: 10, setType: .warmup, date: now),
            makeSet(weightKg: 100, reps: 5, setType: .working, date: now),
        ]

        let trend = service.e1rmTrend(sets: sets, exerciseName: "Bench", timeWindow: .allTime)

        XCTAssertEqual(trend.count, 1)
        XCTAssertEqual(trend[0].weight, 100)
    }

    func testE1RMTrend_empty_returnsEmpty() {
        let trend = service.e1rmTrend(sets: [], exerciseName: "Deadlift", timeWindow: .allTime)
        XCTAssertTrue(trend.isEmpty)
    }

    // MARK: - PR Detection

    func testDetectPRs_risingE1RM_detectsPRs() {
        let now = Date()
        let sets = [
            makeSet(weightKg: 100, reps: 5, date: now.addingTimeInterval(-86400 * 14)),
            makeSet(weightKg: 105, reps: 5, date: now.addingTimeInterval(-86400 * 7)),
            makeSet(weightKg: 110, reps: 5, date: now),
        ]

        let prs = service.detectPRs(sets: sets, exerciseName: "Squat")

        XCTAssertFalse(prs.isEmpty)
        XCTAssertTrue(prs.contains { $0.recordType == "e1RM" })
        XCTAssertTrue(prs.contains { $0.recordType == "Weight" })
    }

    func testDetectPRs_flatProgress_noPRsAfterFirst() {
        let now = Date()
        let sets = [
            makeSet(weightKg: 100, reps: 5, date: now.addingTimeInterval(-86400 * 7)),
            makeSet(weightKg: 100, reps: 5, date: now),
        ]

        let prs = service.detectPRs(sets: sets, exerciseName: "Bench")
        XCTAssertTrue(prs.isEmpty)
    }

    // MARK: - Weekly Volume

    func testWeeklyVolume_aggregatesCorrectly() {
        let now = Date()
        let sets = [
            makeSet(weightKg: 100, reps: 5, date: now),
            makeSet(weightKg: 100, reps: 5, date: now),
            makeSet(weightKg: 100, reps: 5, date: now),
        ]

        let volume = service.weeklyVolume(sets: sets, timeWindow: .allTime)

        XCTAssertFalse(volume.isEmpty)
        let totalVolume = volume.reduce(0.0) { $0 + $1.totalVolume }
        XCTAssertEqual(totalVolume, 1500.0, accuracy: 0.1)
    }

    // MARK: - Muscle Group Balance

    func testMuscleGroupBalance_empty_returnsEmpty() {
        let balance = service.muscleGroupBalance(sets: [], timeWindow: .allTime)
        XCTAssertTrue(balance.isEmpty)
    }
}
