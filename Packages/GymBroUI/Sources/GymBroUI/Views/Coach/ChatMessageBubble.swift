import SwiftUI
import GymBroCore

/// Individual chat message bubble -- dark themed with design system tokens,
/// streaming indicator, and reaction support for assistant messages.
public struct ChatMessageBubble: View {
    let message: ChatMessage
    var onReaction: ((MessageReaction) -> Void)?

    public init(message: ChatMessage, onReaction: ((MessageReaction) -> Void)? = nil) {
        self.message = message
        self.onReaction = onReaction
    }

    public var body: some View {
        HStack(alignment: .top, spacing: GymBroSpacing.sm) {
            if message.role == .assistant {
                assistantAvatar
            }

            if message.role == .user {
                Spacer(minLength: 60)
            }

            VStack(alignment: message.role == .user ? .trailing : .leading, spacing: GymBroSpacing.xs) {
                Text(message.content.isEmpty && message.isStreaming ? " " : message.content)
                    .font(GymBroTypography.body)
                    .foregroundStyle(message.role == .user ? .white : GymBroColors.textPrimary)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(bubbleBackground)
                    .clipShape(RoundedRectangle(cornerRadius: GymBroRadius.lg))

                if message.isStreaming {
                    TypingIndicatorView()
                }

                HStack(spacing: GymBroSpacing.sm) {
                    Text(formattedTime)
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)

                    if message.role == .assistant && !message.isStreaming && onReaction != nil {
                        MessageReactionBar(message: message) { reaction in
                            onReaction?(reaction)
                        }
                    }
                }
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
            .foregroundStyle(GymBroColors.background)
            .frame(width: 28, height: 28)
            .background(GymBroColors.accentCyan)
            .clipShape(Circle())
            .accessibilityHidden(true)
    }

    @ViewBuilder
    private var bubbleBackground: some View {
        if message.role == .user {
            GymBroColors.accentGreen.opacity(0.85)
        } else {
            GymBroColors.surfaceSecondary
        }
    }

    private var formattedTime: String {
        message.createdAt.formatted(date: .omitted, time: .shortened)
    }
}
