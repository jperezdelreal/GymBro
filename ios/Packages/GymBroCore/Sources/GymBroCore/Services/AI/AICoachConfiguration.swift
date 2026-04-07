import Foundation

/// Configuration for Azure OpenAI API access.
/// API keys are loaded from Keychain, falling back to environment variables for initial setup.
public struct AICoachConfiguration {
    public let endpoint: String
    public let apiKey: String
    public let deploymentName: String
    public let apiVersion: String
    public let maxTokens: Int
    public let temperature: Double

    private static let keychainKeyAPIKey = "azure_openai_api_key"
    private static let keychainKeyEndpoint = "azure_openai_endpoint"

    public init(
        endpoint: String,
        apiKey: String,
        deploymentName: String = "gpt-4o-mini",
        apiVersion: String = "2024-06-01",
        maxTokens: Int = 1024,
        temperature: Double = 0.7
    ) {
        self.endpoint = endpoint
        self.apiKey = apiKey
        self.deploymentName = deploymentName
        self.apiVersion = apiVersion
        self.maxTokens = maxTokens
        self.temperature = temperature
    }

    /// Load configuration from Keychain, falling back to environment variables.
    /// When loaded from environment, secrets are migrated into Keychain automatically.
    /// Returns nil if no credentials are available from either source.
    public static func fromEnvironment() -> AICoachConfiguration? {
        // Try Keychain first
        if let endpoint = KeychainService.get(key: keychainKeyEndpoint),
           let apiKey = KeychainService.get(key: keychainKeyAPIKey) {
            let deployment = ProcessInfo.processInfo.environment["AZURE_OPENAI_DEPLOYMENT"] ?? "gpt-4o-mini"
            let version = ProcessInfo.processInfo.environment["AZURE_OPENAI_API_VERSION"] ?? "2024-06-01"
            return AICoachConfiguration(
                endpoint: endpoint,
                apiKey: apiKey,
                deploymentName: deployment,
                apiVersion: version
            )
        }

        // Fall back to environment variables, migrate secrets to Keychain
        guard
            let endpoint = ProcessInfo.processInfo.environment["AZURE_OPENAI_ENDPOINT"],
            let apiKey = ProcessInfo.processInfo.environment["AZURE_OPENAI_API_KEY"]
        else {
            return nil
        }

        KeychainService.set(apiKey, forKey: keychainKeyAPIKey)
        KeychainService.set(endpoint, forKey: keychainKeyEndpoint)

        let deployment = ProcessInfo.processInfo.environment["AZURE_OPENAI_DEPLOYMENT"] ?? "gpt-4o-mini"
        let version = ProcessInfo.processInfo.environment["AZURE_OPENAI_API_VERSION"] ?? "2024-06-01"

        return AICoachConfiguration(
            endpoint: endpoint,
            apiKey: apiKey,
            deploymentName: deployment,
            apiVersion: version
        )
    }
}
