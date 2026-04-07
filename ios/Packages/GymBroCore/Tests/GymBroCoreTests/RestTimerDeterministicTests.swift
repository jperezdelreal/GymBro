import XCTest
@testable import GymBroCore

/// Deterministic tests for RestTimerService logic — no real timing dependencies.
/// Addresses issue #27: flaky timer tests that rely on real Task.sleep.
final class RestTimerDeterministicTests: XCTestCase {

    var timerService: RestTimerService!

    @MainActor
    override func setUp() {
        super.setUp()
        timerService = RestTimerService.shared
        timerService.stop()
    }

    @MainActor
    override func tearDown() {
        timerService.stop()
        super.tearDown()
    }

    // MARK: - Zero / Negative Duration Guard

    @MainActor
    func testStart_zeroDuration_doesNotActivate() {
        timerService.start(duration: 0)
        XCTAssertFalse(timerService.isActive, "Zero duration should not activate timer")
    }

    @MainActor
    func testStart_negativeDuration_doesNotActivate() {
        timerService.start(duration: -10)
        XCTAssertFalse(timerService.isActive, "Negative duration should not activate timer")
    }

    // MARK: - Start Overwrites Previous Timer

    @MainActor
    func testStart_overwritesPreviousTimer() {
        timerService.start(duration: 120)
        XCTAssertEqual(timerService.totalSeconds, 120)

        timerService.start(duration: 60)
        XCTAssertEqual(timerService.totalSeconds, 60, "Second start should overwrite first")
        XCTAssertEqual(timerService.remainingSeconds, 60)
    }

    // MARK: - AddTime Edge Cases

    @MainActor
    func testAddTime_zeroDoesNothing() {
        timerService.start(duration: 60)
        timerService.addTime(0)
        XCTAssertEqual(timerService.remainingSeconds, 60)
    }

    @MainActor
    func testAddTime_largeNegative_clampsToZero() {
        timerService.start(duration: 60)
        timerService.addTime(-1000)
        XCTAssertEqual(timerService.remainingSeconds, 0, "Should not go below zero")
    }

    @MainActor
    func testAddTime_largePositive() {
        timerService.start(duration: 60)
        timerService.addTime(3600)
        XCTAssertEqual(timerService.remainingSeconds, 3660)
        XCTAssertEqual(timerService.totalSeconds, 3660, "Total should expand to match")
    }

    @MainActor
    func testAddTime_totalSeconds_doesNotShrinkBelowRemaining() {
        timerService.start(duration: 120)
        timerService.addTime(60)
        // remaining = 180, total should be max(120, 180) = 180
        XCTAssertEqual(timerService.totalSeconds, 180)
    }

    // MARK: - Stop / Skip State

    @MainActor
    func testStop_clearsAllState() {
        let nextSet = NextSetInfo(
            exerciseName: "Squat", setNumber: 3,
            targetReps: 5, targetWeight: 140, weightUnit: "kg"
        )
        timerService.start(duration: 120, nextSetInfo: nextSet)

        timerService.stop()
        XCTAssertFalse(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }

    @MainActor
    func testSkip_equivalentToStop() {
        timerService.start(duration: 120)
        timerService.skip()
        XCTAssertFalse(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }

    @MainActor
    func testStop_idempotent() {
        // Calling stop multiple times should not crash
        timerService.stop()
        timerService.stop()
        timerService.stop()
        XCTAssertFalse(timerService.isActive)
    }

    // MARK: - NextSetInfo

    @MainActor
    func testNextSetInfo_nilByDefault() {
        timerService.start(duration: 60)
        XCTAssertNil(timerService.nextSetInfo)
    }

    @MainActor
    func testNextSetInfo_setOnStart() {
        let info = NextSetInfo(
            exerciseName: "Deadlift", setNumber: 2,
            targetReps: 3, targetWeight: 200, weightUnit: "kg"
        )
        timerService.start(duration: 180, nextSetInfo: info)
        XCTAssertEqual(timerService.nextSetInfo?.exerciseName, "Deadlift")
        XCTAssertEqual(timerService.nextSetInfo?.setNumber, 2)
        XCTAssertEqual(timerService.nextSetInfo?.targetReps, 3)
        XCTAssertEqual(timerService.nextSetInfo?.targetWeight, 200)
        XCTAssertEqual(timerService.nextSetInfo?.weightUnit, "kg")
    }

    // MARK: - Default Rest Times (static)

    func testDefaultRestTime_allCategories() {
        XCTAssertEqual(RestTimerService.defaultRestTime(for: .compound), 180)
        XCTAssertEqual(RestTimerService.defaultRestTime(for: .isolation), 90)
        XCTAssertEqual(RestTimerService.defaultRestTime(for: .accessory), 60)
    }

    // MARK: - NextSetInfo Equatable

    func testNextSetInfo_equalityCheck() {
        let a = NextSetInfo(exerciseName: "Bench", setNumber: 1, targetReps: 5, targetWeight: 100, weightUnit: "kg")
        let b = NextSetInfo(exerciseName: "Bench", setNumber: 1, targetReps: 5, targetWeight: 100, weightUnit: "kg")
        let c = NextSetInfo(exerciseName: "Squat", setNumber: 1, targetReps: 5, targetWeight: 100, weightUnit: "kg")
        XCTAssertEqual(a, b)
        XCTAssertNotEqual(a, c)
    }
}
