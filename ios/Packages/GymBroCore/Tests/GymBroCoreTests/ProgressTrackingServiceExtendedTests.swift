import XCTest
@testable import GymBroCore

/// Tests for ProgressTrackingService — e1RM trends, volume, frequency, PR detection.
final class ProgressTrackingServiceExtendedTests: XCTestCase {

    let sut = ProgressTrackingService()

    // MARK: - e1RM Trend

    func testE1RMTrend_emptyInput() {
        let result = sut.e1rmTrend(sets: [], exerciseName: "Squat", timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty)
    }

    func testE1RMTrend_onlyWarmupSets_empty() {
        let sets = [
            ExerciseSet(weightKg: 50, reps: 10, setType: .warmup),
            ExerciseSet(weightKg: 60, reps: 8, setType: .warmup),
        ]
        let result = sut.e1rmTrend(sets: sets, exerciseName: "Squat", timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty, "Warmup sets should be excluded from e1RM trend")
    }

    func testE1RMTrend_sortedChronologically() {
        let baseDate = Date().addingTimeInterval(-86400 * 10)
        var sets: [ExerciseSet] = []
        for i in 0..<5 {
            let set = ExerciseSet(weightKg: 100 + Double(i) * 5, reps: 5, setType: .working)
            set.createdAt = baseDate.addingTimeInterval(Double(i) * 86400)
            sets.append(set)
        }
        let result = sut.e1rmTrend(sets: sets, exerciseName: "Bench", timeWindow: .allTime)
        // Verify chronological ordering
        for i in 1..<result.count {
            XCTAssertGreaterThanOrEqual(result[i].date, result[i-1].date)
        }
    }

    func testE1RMTrend_picksHighestE1RMPerSession() {
        let today = Calendar.current.startOfDay(for: Date())
        let lightSet = ExerciseSet(weightKg: 80, reps: 5, setType: .working)
        lightSet.createdAt = today
        let heavySet = ExerciseSet(weightKg: 120, reps: 3, setType: .working)
        heavySet.createdAt = today

        let result = sut.e1rmTrend(sets: [lightSet, heavySet], exerciseName: "Squat", timeWindow: .allTime)
        XCTAssertEqual(result.count, 1, "Same-day sets should be grouped")
        if let first = result.first {
            // 120 * (1 + 3/30) = 132.0 > 80 * (1 + 5/30) = 93.33
            XCTAssertEqual(first.e1rm, 132.0, accuracy: 0.1)
        }
    }

    // MARK: - Weekly Volume

    func testWeeklyVolume_emptyInput() {
        let result = sut.weeklyVolume(sets: [], timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty)
    }

    func testWeeklyVolume_excludesWarmups() {
        let warmup = ExerciseSet(weightKg: 50, reps: 10, setType: .warmup)
        let working = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
        let result = sut.weeklyVolume(sets: [warmup, working], timeWindow: .allTime)
        if let firstWeek = result.first {
            XCTAssertEqual(firstWeek.totalVolume, 500, accuracy: 0.1,
                "Should only count working set volume")
        }
    }

    // MARK: - Tonnage Per Workout

    func testTonnagePerWorkout_emptyInput() {
        let result = sut.tonnagePerWorkout(workouts: [], timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty)
    }

    func testTonnagePerWorkout_sortedByDate() {
        let w1 = Workout(date: Date().addingTimeInterval(-86400 * 7))
        let w2 = Workout(date: Date().addingTimeInterval(-86400 * 3))
        let w3 = Workout(date: Date())

        let result = sut.tonnagePerWorkout(workouts: [w3, w1, w2], timeWindow: .allTime)
        for i in 1..<result.count {
            XCTAssertGreaterThanOrEqual(result[i].periodStart, result[i-1].periodStart)
        }
    }

    // MARK: - Weekly Frequency

    func testWeeklyFrequency_emptyInput() {
        let result = sut.weeklyFrequency(workouts: [], timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty)
    }

    func testWeeklyFrequency_countsWorkouts() {
        let now = Date()
        let w1 = Workout(date: now)
        let w2 = Workout(date: now.addingTimeInterval(86400))

        let result = sut.weeklyFrequency(workouts: [w1, w2], timeWindow: .allTime)
        let totalWorkouts = result.reduce(0) { $0 + $1.workoutCount }
        XCTAssertEqual(totalWorkouts, 2)
    }

    // MARK: - Muscle Group Balance

    func testMuscleGroupBalance_emptyInput() {
        let result = sut.muscleGroupBalance(sets: [], timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty)
    }

    func testMuscleGroupBalance_excludesWarmups() {
        let exercise = Exercise(name: "Curl", category: .isolation, equipment: .dumbbell,
                               muscleGroups: [MuscleGroup(name: "Biceps", isPrimary: true)])
        let warmup = ExerciseSet(exercise: exercise, weightKg: 10, reps: 10, setType: .warmup)
        let result = sut.muscleGroupBalance(sets: [warmup], timeWindow: .allTime)
        XCTAssertTrue(result.isEmpty, "Warmup-only should return empty balance")
    }

    // MARK: - PR Detection

    func testDetectPRs_emptyInput() {
        let result = sut.detectPRs(sets: [], exerciseName: "Bench")
        XCTAssertTrue(result.isEmpty)
    }

    func testDetectPRs_singleSet_noPR() {
        let set = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
        let result = sut.detectPRs(sets: [set], exerciseName: "Bench")
        XCTAssertTrue(result.isEmpty, "First set ever should not be a PR (no previous best)")
    }

    func testDetectPRs_progressingSets_detectsE1RMPR() {
        let baseDate = Date().addingTimeInterval(-86400 * 10)
        var sets: [ExerciseSet] = []
        for i in 0..<5 {
            let set = ExerciseSet(weightKg: 100 + Double(i) * 5, reps: 5, setType: .working)
            set.createdAt = baseDate.addingTimeInterval(Double(i) * 86400)
            sets.append(set)
        }
        let result = sut.detectPRs(sets: sets, exerciseName: "Squat")
        XCTAssertFalse(result.isEmpty, "Should detect PRs in progressively heavier sets")
    }

    func testDetectPRs_flatSets_noPR() {
        let baseDate = Date().addingTimeInterval(-86400 * 10)
        var sets: [ExerciseSet] = []
        for i in 0..<5 {
            let set = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
            set.createdAt = baseDate.addingTimeInterval(Double(i) * 86400)
            sets.append(set)
        }
        let result = sut.detectPRs(sets: sets, exerciseName: "Bench")
        XCTAssertTrue(result.isEmpty, "Identical sets should not trigger PRs")
    }

    func testDetectPRs_warmupSetsIgnored() {
        let warmup = ExerciseSet(weightKg: 200, reps: 1, setType: .warmup)
        let working = ExerciseSet(weightKg: 100, reps: 5, setType: .working)
        let result = sut.detectPRs(sets: [warmup, working], exerciseName: "Bench")
        XCTAssertTrue(result.isEmpty, "Should not count warmup as baseline for PRs")
    }

    // MARK: - Data Transfer Objects

    func testE1RMDataPoint_identifiable() {
        let point = E1RMDataPoint(date: Date(), e1rm: 120, weight: 100, reps: 5, exerciseName: "Squat")
        XCTAssertNotNil(point.id)
    }

    func testVolumeDataPoint_identifiable() {
        let point = VolumeDataPoint(periodStart: Date(), totalVolume: 1000, totalTonnage: 5000, totalSets: 10, totalReps: 50)
        XCTAssertNotNil(point.id)
    }

    func testFrequencyDataPoint_identifiable() {
        let point = FrequencyDataPoint(periodStart: Date(), workoutCount: 3)
        XCTAssertNotNil(point.id)
    }

    func testMuscleGroupBalance_identifiable() {
        let balance = MuscleGroupBalance(muscleGroup: "Chest", totalVolume: 500, percentage: 25)
        XCTAssertNotNil(balance.id)
    }

    func testPREvent_identifiable() {
        let event = PREvent(date: Date(), exerciseName: "Squat", recordType: "e1RM", value: 150, previousBest: 140)
        XCTAssertNotNil(event.id)
    }
}
