import SwiftUI
import GymBroCore

/// Thumbs up/down reaction buttons on AI assistant messages for future fine-tuning.
struct MessageReactionBar: View {
    let message: ChatMessage
    let onReaction: (MessageReaction) -> Void

    private var currentReaction: MessageReaction {
        MessageReaction(rawValue: message.reaction) ?? .none
    }

    var body: some View {
        HStack(spacing: GymBroSpacing.md) {
            reactionButton(
                icon: "hand.thumbsup",
                filledIcon: "hand.thumbsup.fill",
                isActive: currentReaction == .thumbsUp,
                activeColor: GymBroColors.accentGreen,
                reaction: .thumbsUp,
                label: "Helpful"
            )

            reactionButton(
                icon: "hand.thumbsdown",
                filledIcon: "hand.thumbsdown.fill",
                isActive: currentReaction == .thumbsDown,
                activeColor: GymBroColors.accentRed,
                reaction: .thumbsDown,
                label: "Not helpful"
            )
        }
    }

    private func reactionButton(
        icon: String,
        filledIcon: String,
        isActive: Bool,
        activeColor: Color,
        reaction: MessageReaction,
        label: String
    ) -> some View {
        Button {
            onReaction(reaction)
        } label: {
            Image(systemName: isActive ? filledIcon : icon)
                .font(.caption)
                .foregroundStyle(isActive ? activeColor : GymBroColors.textTertiary)
                .frame(width: 28, height: 28)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}
