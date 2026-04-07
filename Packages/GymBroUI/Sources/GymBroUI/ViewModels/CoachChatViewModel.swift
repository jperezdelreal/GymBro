import Foundation
import SwiftData
import GymBroCore

/// ViewModel for the AI Coach chat interface.
/// Manages conversation state, message history, and AI service coordination.
@MainActor
@Observable
public final class CoachChatViewModel {

    // MARK: - Published State

    public var messages: [ChatMessage] = []
    public var inputText: String = ""
    public var isLoading: Bool = false
    public var isStreaming: Bool = false
    public var errorMessage: String?
    public var remainingFreeMessages: Int = 5
    public var isOfflineMode: Bool = false

    // MARK: - Dependencies

    private var cloudService: AICoachService?
    private let fallbackService: AICoachService
    private let usageLimiter: UsageLimiter
    private let isPremium: Bool
    private var modelContext: ModelContext?

    public init(isPremium: Bool = false) {
        self.isPremium = isPremium
        self.fallbackService = DeterministicCoachFallback()
        self.usageLimiter = UsageLimiter()

        if let config = AICoachConfiguration.fromEnvironment() {
            self.cloudService = AzureOpenAICoachService(config: config)
            self.isOfflineMode = false
        } else {
            self.cloudService = nil
            self.isOfflineMode = true
        }

        self.remainingFreeMessages = usageLimiter.remainingMessages(isPremium: isPremium)
    }

    // MARK: - Setup

    public func configure(modelContext: ModelContext) {
        self.modelContext = modelContext
        loadHistory()
    }

    // MARK: - Actions

    public func sendMessage() async {
        let text = inputText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        guard !isLoading else { return }

        // Check usage limits
        guard usageLimiter.canSendMessage(isPremium: isPremium) else {
            errorMessage = "You've used all 5 free AI coach messages this week. Upgrade to Premium for unlimited access."
            return
        }

        // Add user message
        let userMessage = ChatMessage(role: .user, content: text)
        messages.append(userMessage)
        persistMessage(userMessage)
        inputText = ""
        errorMessage = nil

        // Create placeholder assistant message for streaming
        let assistantMessage = ChatMessage(role: .assistant, content: "", isStreaming: true)
        messages.append(assistantMessage)

        isLoading = true
        isStreaming = true

        let context = buildContext()
        let service = selectService()

        do {
            var fullResponse = ""
            for try await token in service.streamMessage(text, context: context) {
                fullResponse += token
                if let index = messages.lastIndex(where: { $0.id == assistantMessage.id }) {
                    messages[index].content = fullResponse
                }
            }

            // Finalize
            if let index = messages.lastIndex(where: { $0.id == assistantMessage.id }) {
                messages[index].isStreaming = false
                persistMessage(messages[index])
            }

            usageLimiter.recordUsage()
            remainingFreeMessages = usageLimiter.remainingMessages(isPremium: isPremium)
        } catch {
            // On cloud failure, try fallback
            if !isOfflineMode {
                do {
                    let fallbackResponse = try await fallbackService.sendMessage(text, context: context)
                    if let index = messages.lastIndex(where: { $0.id == assistantMessage.id }) {
                        messages[index].content = fallbackResponse
                        messages[index].isStreaming = false
                        persistMessage(messages[index])
                    }
                    isOfflineMode = true
                } catch {
                    handleError(error, messageId: assistantMessage.id)
                }
            } else {
                handleError(error, messageId: assistantMessage.id)
            }
        }

        isLoading = false
        isStreaming = false
    }

    public func clearHistory() {
        messages.removeAll()
        if let ctx = modelContext {
            do {
                let descriptor = FetchDescriptor<ChatMessage>()
                let existing = try ctx.fetch(descriptor)
                for msg in existing {
                    ctx.delete(msg)
                }
                try ctx.save()
            } catch {
                // Non-critical: history will be stale
            }
        }
    }

    // MARK: - Private

    private func selectService() -> AICoachService {
        if isOfflineMode || cloudService == nil {
            return fallbackService
        }
        return cloudService!
    }

    private func buildContext() -> CoachContext {
        // In production, this would query SwiftData for real data.
        // For now, return empty context — will be enriched as models are populated.
        CoachContext()
    }

    private func loadHistory() {
        guard let ctx = modelContext else { return }
        do {
            var descriptor = FetchDescriptor<ChatMessage>(
                sortBy: [SortDescriptor(\.createdAt, order: .forward)]
            )
            descriptor.fetchLimit = 50
            messages = try ctx.fetch(descriptor)
        } catch {
            messages = []
        }
    }

    private func persistMessage(_ message: ChatMessage) {
        guard let ctx = modelContext else { return }
        ctx.insert(message)
        try? ctx.save()
    }

    private func handleError(_ error: Error, messageId: UUID) {
        if let index = messages.lastIndex(where: { $0.id == messageId }) {
            messages[index].content = "Sorry, something went wrong. Please try again."
            messages[index].isStreaming = false
        }
        errorMessage = error.localizedDescription
    }
}
