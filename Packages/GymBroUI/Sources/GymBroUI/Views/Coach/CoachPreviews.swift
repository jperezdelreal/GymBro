import SwiftUI
import GymBroCore

// MARK: - CoachChatView Previews

#Preview("Coach — Welcome") {
    NavigationStack {
        CoachChatView()
            .navigationTitle("Coach")
    }
    .preferredColorScheme(.dark)
}

// MARK: - ChatMessageBubble Previews

#Preview("Chat Bubbles") {
    ScrollView {
        VStack(spacing: 12) {
            ChatMessageBubble(message: ChatMessage(
                role: .user,
                content: "How should I warm up for heavy squats?"
            ))

            ChatMessageBubble(message: ChatMessage(
                role: .assistant,
                content: "Great question! For heavy squats, start with 5 minutes of light cardio to raise your body temperature. Then do 2-3 sets of bodyweight squats, followed by progressively heavier barbell sets: empty bar × 10, 50% × 5, 70% × 3, 85% × 1."
            ))

            ChatMessageBubble(message: ChatMessage(
                role: .assistant,
                content: "",
                isStreaming: true
            ))
        }
        .padding(.horizontal, 16)
    }
    .background(GymBroColors.background)
    .preferredColorScheme(.dark)
}
