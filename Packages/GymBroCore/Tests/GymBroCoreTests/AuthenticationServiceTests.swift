import XCTest
@testable import GymBroCore

final class AuthenticationServiceTests: XCTestCase {

    // MARK: - AuthState Tests

    func testAuthStateIsAuthenticated() {
        XCTAssertTrue(AuthState.signedIn(userID: "test-user").isAuthenticated)
        XCTAssertFalse(AuthState.signedOut.isAuthenticated)
        XCTAssertFalse(AuthState.unknown.isAuthenticated)
        XCTAssertFalse(AuthState.signingIn.isAuthenticated)
        XCTAssertFalse(AuthState.error("fail").isAuthenticated)
    }

    func testAuthStateEquality() {
        XCTAssertEqual(AuthState.signedOut, AuthState.signedOut)
        XCTAssertEqual(AuthState.signedIn(userID: "a"), AuthState.signedIn(userID: "a"))
        XCTAssertNotEqual(AuthState.signedIn(userID: "a"), AuthState.signedIn(userID: "b"))
        XCTAssertNotEqual(AuthState.signedOut, AuthState.signingIn)
    }

    // MARK: - AuthError Tests

    func testAuthErrorDescriptions() {
        XCTAssertNotNil(AuthError.invalidCredential.errorDescription)
        XCTAssertNotNil(AuthError.canceled.errorDescription)
        XCTAssertNotNil(AuthError.invalidResponse.errorDescription)
        XCTAssertNotNil(AuthError.notHandled.errorDescription)
        XCTAssertNotNil(AuthError.failed("test").errorDescription)
        XCTAssertNotNil(AuthError.unknown("test").errorDescription)
    }

    func testAuthErrorEquality() {
        XCTAssertEqual(AuthError.canceled, AuthError.canceled)
        XCTAssertEqual(AuthError.failed("x"), AuthError.failed("x"))
        XCTAssertNotEqual(AuthError.canceled, AuthError.invalidCredential)
    }

    // MARK: - SyncStatus Tests

    func testSyncStatusDisplayText() {
        XCTAssertEqual(SyncStatus.idle.displayText, "Ready")
        XCTAssertEqual(SyncStatus.syncing.displayText, "Syncing…")
        XCTAssertEqual(SyncStatus.synced.displayText, "Up to date")
        XCTAssertEqual(SyncStatus.noAccount.displayText, "No iCloud account")
        XCTAssertEqual(SyncStatus.offline.displayText, "Offline")
        XCTAssertTrue(SyncStatus.error("test").displayText.contains("test"))
    }

    func testSyncStatusSystemImage() {
        XCTAssertEqual(SyncStatus.idle.systemImage, "icloud")
        XCTAssertEqual(SyncStatus.synced.systemImage, "checkmark.icloud")
        XCTAssertEqual(SyncStatus.syncing.systemImage, "arrow.triangle.2.circlepath.icloud")
        XCTAssertEqual(SyncStatus.noAccount.systemImage, "icloud.slash")
        XCTAssertEqual(SyncStatus.offline.systemImage, "icloud.slash")
        XCTAssertEqual(SyncStatus.error("x").systemImage, "exclamationmark.icloud")
    }

    // MARK: - KeychainService Integration

    func testKeychainRoundTrip() {
        let key = "test_auth_\(UUID().uuidString)"
        defer { KeychainService.delete(key: key) }

        let stored = KeychainService.set("test-token-123", forKey: key)
        // Keychain may not be available in CI/test environments
        if stored {
            let retrieved = KeychainService.get(key: key)
            XCTAssertEqual(retrieved, "test-token-123")
        }
    }

    func testKeychainDeleteNonexistent() {
        let result = KeychainService.delete(key: "nonexistent_key_\(UUID().uuidString)")
        // Should succeed (errSecItemNotFound is treated as success)
        XCTAssertTrue(result)
    }

    // MARK: - CloudKitSyncService Configuration

    func testModelConfigurationLocalOnly() {
        let config = CloudKitSyncService.makeModelConfiguration(isSignedIn: false)
        XCTAssertNotNil(config)
    }

    func testModelConfigurationCloudKit() {
        let config = CloudKitSyncService.makeModelConfiguration(isSignedIn: true)
        XCTAssertNotNil(config)
    }

    func testContainerIdentifier() {
        XCTAssertEqual(CloudKitSyncService.containerIdentifier, "iCloud.com.gymbro.app")
    }
}
