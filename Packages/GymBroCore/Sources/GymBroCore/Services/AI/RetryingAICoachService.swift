import Foundation
import os

/// Decorator that wraps an `AICoachService` with exponential backoff retry logic.
///
/// On transient errors (network timeout, 429, 500, 502, 503), retries up to
/// `RetryPolicy.maxAttempts` times with configurable backoff delays.
/// Falls back to `DeterministicCoachFallback` after all retries are exhausted.
/// Non-transient errors (401, 403, 400) fail immediately without retry.
public final class RetryingAICoachService: AICoachService {

    private let primary: AICoachService
    private let fallback: AICoachService
    private let retryPolicy: RetryPolicy
    private let logger = Logger(
        subsystem: "com.gymbro.core",
        category: "RetryingAICoachService"
    )

    public init(
        primary: AICoachService,
        fallback: AICoachService = DeterministicCoachFallback(),
        retryPolicy: RetryPolicy = .default
    ) {
        self.primary = primary
        self.fallback = fallback
        self.retryPolicy = retryPolicy
    }

    // MARK: - AICoachService

    public func sendMessage(_ message: String, context: CoachContext) async throws -> String {
        var lastError: Error?

        for attempt in 0..<retryPolicy.maxAttempts {
            do {
                try Task.checkCancellation()
                return try await primary.sendMessage(message, context: context)
            } catch {
                lastError = error

                guard AICoachError.isTransientError(error) else {
                    throw error
                }

                logger.warning(
                    "Transient error on attempt \(attempt + 1)/\(self.retryPolicy.maxAttempts): \(error.localizedDescription)"
                )

                // Don't sleep after the last failed attempt
                if attempt < retryPolicy.maxAttempts - 1 {
                    let delay = retryPolicy.delay(forAttempt: attempt)
                    try await Task.sleep(for: delay)
                }
            }
        }

        // All retries exhausted — fall back to offline rule engine
        logger.error("All \(self.retryPolicy.maxAttempts) retries exhausted. Falling back to offline coach.")
        return try await fallback.sendMessage(message, context: context)
    }

    public func streamMessage(_ message: String, context: CoachContext) -> AsyncThrowingStream<String, Error> {
        AsyncThrowingStream { continuation in
            let task = Task {
                var lastError: Error?

                for attempt in 0..<self.retryPolicy.maxAttempts {
                    do {
                        try Task.checkCancellation()

                        let stream = self.primary.streamMessage(message, context: context)
                        var receivedContent = false

                        for try await token in stream {
                            receivedContent = true
                            continuation.yield(token)
                        }

                        if receivedContent {
                            continuation.finish()
                            return
                        }
                    } catch {
                        lastError = error

                        guard AICoachError.isTransientError(error) else {
                            continuation.finish(throwing: error)
                            return
                        }

                        self.logger.warning(
                            "Stream transient error on attempt \(attempt + 1)/\(self.retryPolicy.maxAttempts): \(error.localizedDescription)"
                        )

                        if attempt < self.retryPolicy.maxAttempts - 1 {
                            // Signal reconnection status to the user
                            continuation.yield("\n⟳ Reconnecting...\n")

                            let delay = self.retryPolicy.delay(forAttempt: attempt)
                            do {
                                try await Task.sleep(for: delay)
                            } catch {
                                continuation.finish(throwing: error)
                                return
                            }
                        }
                    }
                }

                // All retries exhausted — stream from offline fallback
                self.logger.error(
                    "All \(self.retryPolicy.maxAttempts) stream retries exhausted. Falling back to offline coach."
                )

                continuation.yield("\n📡 Switching to offline mode...\n")
                let fallbackStream = self.fallback.streamMessage(message, context: context)
                do {
                    for try await token in fallbackStream {
                        continuation.yield(token)
                    }
                    continuation.finish()
                } catch {
                    continuation.finish(throwing: error)
                }
            }

            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }
}
