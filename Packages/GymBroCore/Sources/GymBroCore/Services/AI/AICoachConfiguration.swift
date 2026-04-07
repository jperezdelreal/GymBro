import Foundation

/// Configuration for Azure OpenAI API access.
/// API keys are loaded from environment / config — never hardcoded.
public struct AICoachConfiguration {
    public let endpoint: String
    public let apiKey: String
    public let deploymentName: String
    public let apiVersion: String
    public let maxTokens: Int
    public let temperature: Double

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

    /// Load configuration from environment variables.
    /// Returns nil if required vars are missing.
    public static func fromEnvironment() -> AICoachConfiguration? {
        guard
            let endpoint = ProcessInfo.processInfo.environment["AZURE_OPENAI_ENDPOINT"],
            let apiKey = ProcessInfo.processInfo.environment["AZURE_OPENAI_API_KEY"]
        else {
            return nil
        }

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
