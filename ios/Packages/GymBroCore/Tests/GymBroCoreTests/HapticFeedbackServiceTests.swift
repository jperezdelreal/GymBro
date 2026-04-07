import XCTest
@testable import GymBroCore

@MainActor
final class HapticFeedbackServiceTests: XCTestCase {

    // MARK: - Singleton Access

    func testSharedInstanceExists() {
        let service = HapticFeedbackService.shared
        XCTAssertNotNil(service)
    }

    func testSharedInstanceIsSameObject() {
        let a = HapticFeedbackService.shared
        let b = HapticFeedbackService.shared
        XCTAssertTrue(a === b, "Shared instance should always return the same object")
    }

    // MARK: - Methods Do Not Crash

    func testPrepareDoesNotCrash() {
        HapticFeedbackService.shared.prepare()
    }

    func testSetCompletedDoesNotCrash() {
        HapticFeedbackService.shared.setCompleted()
    }

    func testPersonalRecordAchievedDoesNotCrash() {
        HapticFeedbackService.shared.personalRecordAchieved()
    }

    func testValueChangedDoesNotCrash() {
        HapticFeedbackService.shared.valueChanged()
    }

    func testLightImpactDoesNotCrash() {
        HapticFeedbackService.shared.lightImpact()
    }

    func testMediumImpactDoesNotCrash() {
        HapticFeedbackService.shared.mediumImpact()
    }

    func testHeavyImpactDoesNotCrash() {
        HapticFeedbackService.shared.heavyImpact()
    }

    // MARK: - PR Celebration Pattern

    func testPRCelebrationTriggersMultipleFeedbacks() async throws {
        // Verify the celebratory triple-haptic pattern doesn't crash
        // (actual haptic output verified on device)
        HapticFeedbackService.shared.personalRecordAchieved()
        try await Task.sleep(for: .milliseconds(500))
    }
}
