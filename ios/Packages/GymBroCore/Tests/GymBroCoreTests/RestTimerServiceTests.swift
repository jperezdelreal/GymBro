import XCTest
@testable import GymBroCore

final class RestTimerServiceTests: XCTestCase {
    var timerService: RestTimerService!
    
    override func setUp() {
        super.setUp()
        timerService = RestTimerService.shared
        timerService.stop()
    }
    
    override func tearDown() {
        timerService.stop()
        super.tearDown()
    }
    
    func testTimerStartsWithCorrectDuration() {
        // Given
        let duration = 120
        
        // When
        timerService.start(duration: duration)
        
        // Then
        XCTAssertTrue(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, duration)
        XCTAssertEqual(timerService.totalSeconds, duration)
    }
    
    func testTimerStops() {
        // Given
        timerService.start(duration: 60)
        
        // When
        timerService.stop()
        
        // Then
        XCTAssertFalse(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }
    
    func testTimerSkip() {
        // Given
        timerService.start(duration: 60)
        
        // When
        timerService.skip()
        
        // Then
        XCTAssertFalse(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }
    
    func testAddTime() {
        // Given
        timerService.start(duration: 60)
        let initialRemaining = timerService.remainingSeconds
        
        // When
        timerService.addTime(30)
        
        // Then
        XCTAssertEqual(timerService.remainingSeconds, initialRemaining + 30)
        XCTAssertEqual(timerService.totalSeconds, initialRemaining + 30)
    }
    
    func testSubtractTime() {
        // Given
        timerService.start(duration: 120)
        let initialRemaining = timerService.remainingSeconds
        
        // When
        timerService.addTime(-30)
        
        // Then
        XCTAssertEqual(timerService.remainingSeconds, initialRemaining - 30)
    }
    
    func testSubtractTimeDoesNotGoNegative() {
        // Given
        timerService.start(duration: 20)
        
        // When
        timerService.addTime(-30)
        
        // Then
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }
    
    func testNextSetInfo() {
        // Given
        let nextSetInfo = NextSetInfo(
            exerciseName: "Bench Press",
            setNumber: 2,
            targetReps: 8,
            targetWeight: 100.0,
            weightUnit: "kg"
        )
        
        // When
        timerService.start(duration: 90, nextSetInfo: nextSetInfo)
        
        // Then
        XCTAssertEqual(timerService.nextSetInfo, nextSetInfo)
    }
    
    func testDefaultRestTimeForCompound() {
        // When
        let restTime = RestTimerService.defaultRestTime(for: .compound)
        
        // Then
        XCTAssertEqual(restTime, 180)
    }
    
    func testDefaultRestTimeForIsolation() {
        // When
        let restTime = RestTimerService.defaultRestTime(for: .isolation)
        
        // Then
        XCTAssertEqual(restTime, 90)
    }
    
    func testDefaultRestTimeForAccessory() {
        // When
        let restTime = RestTimerService.defaultRestTime(for: .accessory)
        
        // Then
        XCTAssertEqual(restTime, 60)
    }
    
    func testTimerCountsDown() async throws {
        // Given
        timerService.start(duration: 3)
        let initialRemaining = timerService.remainingSeconds
        
        // When
        try await Task.sleep(for: .seconds(2))
        
        // Then
        XCTAssertLessThan(timerService.remainingSeconds, initialRemaining)
        XCTAssertTrue(timerService.isActive)
    }
    
    func testTimerCompletesAndBecomesInactive() async throws {
        // Given
        timerService.start(duration: 2)
        
        // When
        try await Task.sleep(for: .seconds(3))
        
        // Then
        XCTAssertFalse(timerService.isActive)
        XCTAssertEqual(timerService.remainingSeconds, 0)
    }
}
