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

    public init(
        id: UUID = UUID(),
        role: MessageRole,
        content: String,
        isStreaming: Bool = false
    ) {
        self.id = id
        self.createdAt = Date()
        self.role = role
        self.content = content
        self.isStreaming = isStreaming
    }
}

public enum MessageRole: String, Codable {
    case system
    case user
    case assistant
}
