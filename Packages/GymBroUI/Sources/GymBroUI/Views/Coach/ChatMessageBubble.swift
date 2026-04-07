import SwiftUI
import GymBroCore

/// Individual chat message bubble — supports user and assistant messages with streaming indicator.
public struct ChatMessageBubble: View {
    let message: ChatMessage

    public init(message: ChatMessage) {
        self.message = message
    }

    public var body: some View {
        HStack(alignment: .top, spacing: 8) {
            if message.role == .assistant {
                assistantAvatar
            }

            if message.role == .user {
                Spacer(minLength: 60)
            }

            VStack(alignment: message.role == .user ? .trailing : .leading, spacing: 4) {
                Text(message.content.isEmpty && message.isStreaming ? "Thinking..." : message.content)
                    .font(.body)
                    .foregroundStyle(message.role == .user ? .white : .primary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(bubbleBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                if message.isStreaming {
                    streamingIndicator
                }

                Text(formattedTime)
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }

            if message.role == .assistant {
                Spacer(minLength: 60)
            }
        }
    }

    // MARK: - Subviews

    private var assistantAvatar: some View {
        Image(systemName: "brain.head.profile")
            .font(.caption)
            .foregroundStyle(.white)
            .frame(width: 28, height: 28)
            .background(Color.accentColor)
            .clipShape(Circle())
    }

    private var bubbleBackground: some ShapeStyle {
        message.role == .user
            ? AnyShapeStyle(Color.accentColor)
            : AnyShapeStyle(Color(.systemGray6))
    }

    private var streamingIndicator: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { i in
                Circle()
                    .fill(Color.secondary.opacity(0.5))
                    .frame(width: 4, height: 4)
            }
        }
        .padding(.leading, 8)
    }

    private var formattedTime: String {
        let formatter = DateFormatter()
        formatter.timeStyle = .short
        return formatter.string(from: message.createdAt)
    }
}
