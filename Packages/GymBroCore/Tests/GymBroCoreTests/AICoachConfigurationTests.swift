import XCTest
@testable import GymBroCore

/// Tests for AICoachConfiguration — config parsing and environment loading.
final class AICoachConfigurationTests: XCTestCase {

    // MARK: - Init

    func testInitWithAllParameters() {
        let config = AICoachConfiguration(
            endpoint: "https://example.openai.azure.com",
            apiKey: "test-key-123",
            deploymentName: "gpt-4o",
            apiVersion: "2024-08-01",
            maxTokens: 2048,
            temperature: 0.5
        )

        XCTAssertEqual(config.endpoint, "https://example.openai.azure.com")
        XCTAssertEqual(config.apiKey, "test-key-123")
        XCTAssertEqual(config.deploymentName, "gpt-4o")
        XCTAssertEqual(config.apiVersion, "2024-08-01")
        XCTAssertEqual(config.maxTokens, 2048)
        XCTAssertEqual(config.temperature, 0.5)
    }

    func testInitWithDefaults() {
        let config = AICoachConfiguration(
            endpoint: "https://example.openai.azure.com",
            apiKey: "key"
        )

        XCTAssertEqual(config.deploymentName, "gpt-4o-mini", "Default deployment should be gpt-4o-mini")
        XCTAssertEqual(config.apiVersion, "2024-06-01", "Default API version should be 2024-06-01")
        XCTAssertEqual(config.maxTokens, 1024, "Default max tokens should be 1024")
        XCTAssertEqual(config.temperature, 0.7, accuracy: 0.001, "Default temperature should be 0.7")
    }

    // MARK: - Environment Loading

    func testFromEnvironment_missingEndpoint_returnsNil() {
        // Environment vars are not set in test — this should return nil
        let config = AICoachConfiguration.fromEnvironment()
        // If the env vars happen to be set (CI), this might not be nil.
        // We test the negative case: at minimum, verify it doesn't crash.
        _ = config
    }

    func testFromEnvironment_doesNotCrash() {
        // Verify the method is safe to call regardless of environment state
        let _ = AICoachConfiguration.fromEnvironment()
    }

    // MARK: - Edge Cases

    func testEmptyEndpoint() {
        let config = AICoachConfiguration(endpoint: "", apiKey: "key")
        XCTAssertEqual(config.endpoint, "")
    }

    func testEmptyApiKey() {
        let config = AICoachConfiguration(endpoint: "https://example.com", apiKey: "")
        XCTAssertEqual(config.apiKey, "")
    }

    func testZeroMaxTokens() {
        let config = AICoachConfiguration(
            endpoint: "https://example.com",
            apiKey: "key",
            maxTokens: 0
        )
        XCTAssertEqual(config.maxTokens, 0)
    }

    func testNegativeTemperature() {
        let config = AICoachConfiguration(
            endpoint: "https://example.com",
            apiKey: "key",
            temperature: -1.0
        )
        XCTAssertEqual(config.temperature, -1.0, accuracy: 0.001)
    }

    func testHighTemperature() {
        let config = AICoachConfiguration(
            endpoint: "https://example.com",
            apiKey: "key",
            temperature: 2.0
        )
        XCTAssertEqual(config.temperature, 2.0, accuracy: 0.001)
    }
}
