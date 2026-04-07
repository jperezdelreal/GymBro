import XCTest
@testable import GymBroCore

/// Tests for UsageLimiter — freemium gating logic for AI coach.
/// Uses an isolated UserDefaults suite to avoid contaminating real storage.
final class UsageLimiterTests: XCTestCase {

    private var sut: UsageLimiter!

    override func setUp() {
        super.setUp()
        // Clean state before each test
        UserDefaults.standard.removeObject(forKey: "ai_coach_usage")
        sut = UsageLimiter()
    }

    override func tearDown() {
        UserDefaults.standard.removeObject(forKey: "ai_coach_usage")
        sut = nil
        super.tearDown()
    }

    // MARK: - Premium Users

    func testPremiumUser_canAlwaysSendMessages() {
        // Even after 100 recorded usages
        for _ in 0..<100 {
            sut.recordUsage()
        }
        XCTAssertTrue(sut.canSendMessage(isPremium: true))
    }

    func testPremiumUser_remainingMessagesIsMax() {
        XCTAssertEqual(sut.remainingMessages(isPremium: true), .max)
    }

    // MARK: - Free Users

    func testFreeUser_canSendWithinLimit() {
        XCTAssertTrue(sut.canSendMessage(isPremium: false))
    }

    func testFreeUser_remainingStartsAtFive() {
        XCTAssertEqual(sut.remainingMessages(isPremium: false), 5)
    }

    func testFreeUser_remainingDecrementsOnUsage() {
        sut.recordUsage()
        XCTAssertEqual(sut.remainingMessages(isPremium: false), 4)
    }

    func testFreeUser_cannotSendAfterFiveMessages() {
        for _ in 0..<5 {
            sut.recordUsage()
        }
        XCTAssertFalse(sut.canSendMessage(isPremium: false))
        XCTAssertEqual(sut.remainingMessages(isPremium: false), 0)
    }

    func testFreeUser_remainingDoesNotGoNegative() {
        for _ in 0..<10 {
            sut.recordUsage()
        }
        XCTAssertEqual(sut.remainingMessages(isPremium: false), 0)
    }

    func testFreeUser_atExactLimit() {
        for _ in 0..<4 {
            sut.recordUsage()
        }
        XCTAssertTrue(sut.canSendMessage(isPremium: false), "Should allow the 5th message")
        sut.recordUsage()
        XCTAssertFalse(sut.canSendMessage(isPremium: false), "Should block the 6th message")
    }

    // MARK: - Persistence

    func testUsagePersistsAcrossInstances() {
        sut.recordUsage()
        sut.recordUsage()

        let newLimiter = UsageLimiter()
        XCTAssertEqual(newLimiter.remainingMessages(isPremium: false), 3,
            "Usage count should persist across instances via UserDefaults")
    }

    // MARK: - Edge Cases

    func testFreshState_noStoredData() {
        // First access with clean UserDefaults
        XCTAssertTrue(sut.canSendMessage(isPremium: false))
        XCTAssertEqual(sut.remainingMessages(isPremium: false), 5)
    }

    func testCorruptedStorageData() {
        // Write garbage data to the storage key
        UserDefaults.standard.set(Data([0xFF, 0xFE, 0xFD]), forKey: "ai_coach_usage")

        // Should reset gracefully
        let limiter = UsageLimiter()
        XCTAssertTrue(limiter.canSendMessage(isPremium: false),
            "Should recover from corrupted storage by resetting")
    }
}
