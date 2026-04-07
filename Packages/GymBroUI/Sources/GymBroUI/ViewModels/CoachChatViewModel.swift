import Foundation
import SwiftData
import GymBroCore
import os

/// ViewModel for the AI Coach chat interface.
/// Manages conversation state, message history, context summary, reactions, and voice input.
@MainActor
@Observable
public final class CoachChatViewModel {

    private static let logger = Logger(subsystem: "com.gymbro", category: "CoachChat")

    // MARK: - Published State

    public var messages: [ChatMessage] = []
    public var inputText: String = ""
    public var isLoading: Bool = false
    public var isStreaming: Bool = false
    public var errorMessage: String?
    public var remainingFreeMessages: Int = 5
    public var isOfflineMode: Bool = false
    public var contextSummary: CoachContextSummary = CoachContextSummary()

    /// Contextual suggested prompts based on user data.
    public var suggestedPrompts: [SuggestedPrompt] = SuggestedPrompt.defaults

    // MARK: - Dependencies

    private var cloudService: AICoachService?
    private let fallbackService: AICoachService
    private let usageLimiter: UsageLimiter
    private let isPremium: Bool
    private var modelContext: ModelContext?
    private var contextService: CoachContextService?

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
        self.contextService = CoachContextService(modelContext: modelContext)
        loadHistory()
        refreshContextSummary()
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

    /// Send a suggested prompt directly.
    public func sendSuggestedPrompt(_ prompt: SuggestedPrompt) async {
        inputText = prompt.text
        await sendMessage()
    }

    /// Toggle reaction on an assistant message (thumbs up / thumbs down).
    public func toggleReaction(_ reaction: MessageReaction, for message: ChatMessage) {
        guard message.role == .assistant else { return }
        guard let index = messages.firstIndex(where: { $0.id == message.id }) else { return }

        let current = MessageReaction(rawValue: messages[index].reaction) ?? .none
        messages[index].reaction = (current == reaction) ? MessageReaction.none.rawValue : reaction.rawValue

        if let ctx = modelContext {
            do {
                try ctx.save()
            } catch {
                Self.logger.error("Failed to save reaction: \(error.localizedDescription)")
            }
        }
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

    public func refreshContextSummary() {
        guard let service = contextService else { return }
        contextSummary = service.fetchContextSummary()
    }

    // MARK: - Private

    private func selectService() -> AICoachService {
        if isOfflineMode {
            return fallbackService
        }
        guard let service = cloudService else {
            return fallbackService
        }
        return service
    }

    private func buildContext() -> CoachContext {
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
        do {
            try ctx.save()
        } catch {
            Self.logger.error("Failed to persist chat message: \(error.localizedDescription)")
        }
    }

    private func handleError(_ error: Error, messageId: UUID) {
        if let index = messages.lastIndex(where: { $0.id == messageId }) {
            messages[index].content = "Sorry, something went wrong. Please try again."
            messages[index].isStreaming = false
        }
        errorMessage = error.localizedDescription
    }
}

// MARK: - Suggested Prompts

public struct SuggestedPrompt: Identifiable, Sendable {
    public let id: String
    public let text: String
    public let icon: String

    public init(id: String = UUID().uuidString, text: String, icon: String) {
        self.id = id
        self.text = text
        self.icon = icon
    }

    public static let defaults: [SuggestedPrompt] = [
        SuggestedPrompt(text: "How's my squat progressing?", icon: "chart.line.uptrend.xyaxis"),
        SuggestedPrompt(text: "Should I deload?", icon: "arrow.down.circle"),
        SuggestedPrompt(text: "Suggest today's workout", icon: "figure.strengthtraining.traditional"),
        SuggestedPrompt(text: "What's my weak point?", icon: "exclamationmark.triangle"),
        SuggestedPrompt(text: "Help me break a plateau", icon: "arrow.up.forward"),
        SuggestedPrompt(text: "Explain RPE vs RIR", icon: "questionmark.circle"),
    ]
}
