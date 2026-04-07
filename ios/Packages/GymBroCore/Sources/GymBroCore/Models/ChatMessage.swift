import Foundation
import SwiftData

/// Persisted chat message for AI Coach conversations.
@Model
public final class ChatMessage {
    public var id: UUID
    public var createdAt: Date
    public var role: MessageRole
    public var content: String
    public var isStreaming: Bool
    /// User reaction for fine-tuning data: 1 = thumbs up, -1 = thumbs down, 0 = none.
    public var reaction: Int

    public init(
        id: UUID = UUID(),
        role: MessageRole,
        content: String,
        isStreaming: Bool = false,
        reaction: Int = 0
    ) {
        self.id = id
        self.createdAt = Date()
        self.role = role
        self.content = content
        self.isStreaming = isStreaming
        self.reaction = reaction
    }
}

public enum MessageRole: String, Codable {
    case system
    case user
    case assistant
}

public enum MessageReaction: Int, Sendable {
    case none = 0
    case thumbsUp = 1
    case thumbsDown = -1
}
