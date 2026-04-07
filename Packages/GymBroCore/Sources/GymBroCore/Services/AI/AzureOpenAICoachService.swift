import Foundation

/// Azure OpenAI implementation of AICoachService.
/// Handles API calls, streaming, rate limiting, and error recovery.
public final class AzureOpenAICoachService: AICoachService {

    private let config: AICoachConfiguration
    private let promptBuilder: PromptBuilder
    private let safetyFilter: SafetyFilter
    private let session: URLSession
    private let rateLimiter: RateLimiter

    public init(
        config: AICoachConfiguration,
        promptBuilder: PromptBuilder = PromptBuilder(),
        safetyFilter: SafetyFilter = SafetyFilter(),
        session: URLSession = .shared
    ) {
        self.config = config
        self.promptBuilder = promptBuilder
        self.safetyFilter = safetyFilter
        self.session = session
        self.rateLimiter = RateLimiter()
    }

    // MARK: - AICoachService

    public func sendMessage(_ message: String, context: CoachContext) async throws -> String {
        try rateLimiter.checkLimit()

        let safetyCheck = safetyFilter.checkUserMessage(message)
        if case .flagged(_, let advisory) = safetyCheck {
            return advisory
        }

        let request = try buildRequest(message: message, context: context, stream: false)
        let (data, response) = try await session.data(for: request)

        try validateHTTPResponse(response)
        let chatResponse = try JSONDecoder().decode(ChatCompletionResponse.self, from: data)

        guard let content = chatResponse.choices.first?.message.content else {
            throw AICoachError.emptyResponse
        }

        rateLimiter.recordRequest()
        return safetyFilter.appendDisclaimerIfNeeded(content)
    }

    public func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            Task {
                do {
                    try self.rateLimiter.checkLimit()

                    let safetyCheck = self.safetyFilter.checkUserMessage(message)
                    if case .flagged(_, let advisory) = safetyCheck {
                        continuation.yield(advisory)
                        continuation.finish()
                        return
                    }

                    let request = try self.buildRequest(message: message, context: context, stream: true)
                    let (bytes, response) = try await self.session.bytes(for: request)

                    try self.validateHTTPResponse(response)

                    var buffer = ""
                    for try await line in bytes.lines {
                        guard line.hasPrefix("data: ") else { continue }
                        let jsonStr = String(line.dropFirst(6))
                        if jsonStr == "[DONE]" { break }

                        guard let jsonData = jsonStr.data(using: .utf8),
                              let chunk = try? JSONDecoder().decode(StreamChunk.self, from: jsonData),
                              let delta = chunk.choices.first?.delta.content else {
                            continue
                        }

                        buffer += delta
                        continuation.yield(delta)
                    }

                    self.rateLimiter.recordRequest()

                    // Append disclaimer as final token
                    let disclaimer = "\n\n*⚠️ AI suggestions are not medical advice. Consult a qualified professional for health concerns.*"
                    continuation.yield(disclaimer)
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }
        }
    }

    // MARK: - Request Building

    private func buildRequest(message: String, context: CoachContext, stream: Bool) throws -> URLRequest {
        let systemPrompt = promptBuilder.buildSystemPrompt(context: context)
        let urlString = "\(config.endpoint)/openai/deployments/\(config.deploymentName)/chat/completions?api-version=\(config.apiVersion)"

        guard let url = URL(string: urlString) else {
            throw AICoachError.invalidConfiguration("Invalid endpoint URL")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(config.apiKey, forHTTPHeaderField: "api-key")
        request.timeoutInterval = 30

        let body = ChatCompletionRequest(
            messages: [
                .init(role: "system", content: systemPrompt),
                .init(role: "user", content: message)
            ],
            maxTokens: config.maxTokens,
            temperature: config.temperature,
            stream: stream
        )

        request.httpBody = try JSONEncoder().encode(body)
        return request
    }

    private func validateHTTPResponse(_ response: URLResponse) throws {
        guard let http = response as? HTTPURLResponse else {
            throw AICoachError.networkError("Invalid response type")
        }

        switch http.statusCode {
        case 200...299:
            return
        case 401:
            throw AICoachError.authenticationFailed
        case 429:
            throw AICoachError.rateLimited
        case 500...599:
            throw AICoachError.serverError(http.statusCode)
        default:
            throw AICoachError.networkError("HTTP \(http.statusCode)")
        }
    }
}

// MARK: - Request/Response DTOs

private struct ChatCompletionRequest: Encodable {
    let messages: [MessageDTO]
    let maxTokens: Int
    let temperature: Double
    let stream: Bool

    enum CodingKeys: String, CodingKey {
        case messages
        case maxTokens = "max_tokens"
        case temperature
        case stream
    }
}

private struct MessageDTO: Codable {
    let role: String
    let content: String
}

private struct ChatCompletionResponse: Decodable {
    let choices: [Choice]

    struct Choice: Decodable {
        let message: MessageDTO
    }
}

private struct StreamChunk: Decodable {
    let choices: [StreamChoice]

    struct StreamChoice: Decodable {
        let delta: Delta
    }

    struct Delta: Decodable {
        let content: String?
    }
}

// MARK: - Rate Limiter

/// Simple in-memory rate limiter. Tracks requests per minute.
final class RateLimiter {
    private var timestamps: [Date] = []
    private let maxRequestsPerMinute: Int = 20

    func checkLimit() throws {
        cleanOldEntries()
        if timestamps.count >= maxRequestsPerMinute {
            throw AICoachError.rateLimited
        }
    }

    func recordRequest() {
        timestamps.append(Date())
    }

    private func cleanOldEntries() {
        let cutoff = Date().addingTimeInterval(-60)
        timestamps.removeAll { $0 < cutoff }
    }
}

// MARK: - Errors

public enum AICoachError: LocalizedError {
    case invalidConfiguration(String)
    case networkError(String)
    case authenticationFailed
    case rateLimited
    case serverError(Int)
    case emptyResponse
    case offlineUnavailable

    public var errorDescription: String? {
        switch self {
        case .invalidConfiguration(let msg): return "Configuration error: \(msg)"
        case .networkError(let msg): return "Network error: \(msg)"
        case .authenticationFailed: return "API authentication failed. Check your API key."
        case .rateLimited: return "Too many requests. Please wait a moment."
        case .serverError(let code): return "Server error (HTTP \(code)). Try again later."
        case .emptyResponse: return "AI returned an empty response."
        case .offlineUnavailable: return "This feature requires an internet connection."
        }
    }
}
