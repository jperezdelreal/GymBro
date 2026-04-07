import SwiftUI
import GymBroCore

// MARK: - CoachChatView Previews

#Preview("Coach -- Welcome") {
    NavigationStack {
        CoachChatView()
            .navigationTitle("Coach")
    }
    .preferredColorScheme(.dark)
}

// MARK: - ChatMessageBubble Previews

#Preview("Chat Bubbles") {
    ScrollView {
        VStack(spacing: GymBroSpacing.md) {
            ChatMessageBubble(message: ChatMessage(
                role: .user,
                content: "How should I warm up for heavy squats?"
            ))

            ChatMessageBubble(
                message: ChatMessage(
                    role: .assistant,
                    content: "Great question! For heavy squats, start with 5 minutes of light cardio. Then do bodyweight squats, followed by progressively heavier barbell sets."
                )
            ) { _ in }

            ChatMessageBubble(message: ChatMessage(
                role: .assistant,
                content: "",
                isStreaming: true
            ))
        }
        .padding(.horizontal, GymBroSpacing.md)
    }
    .gymBroDarkBackground()
}

// MARK: - Typing Indicator Preview

#Preview("Typing Indicator") {
    TypingIndicatorView()
        .padding()
        .gymBroDarkBackground()
}

// MARK: - Context Indicator Previews

#Preview("Context Bar -- With Data") {
    ContextIndicatorBar(summary: CoachContextSummary(
        workoutCount: 12,
        weeksOfData: 3,
        lastWorkoutDate: Date()
    ))
    .gymBroDarkBackground()
}

#Preview("Context Bar -- Empty") {
    ContextIndicatorBar(summary: CoachContextSummary())
        .gymBroDarkBackground()
}

// MARK: - Suggested Prompts Preview

#Preview("Suggested Prompts") {
    SuggestedPromptsBar(prompts: SuggestedPrompt.defaults) { _ in }
        .gymBroDarkBackground()
}