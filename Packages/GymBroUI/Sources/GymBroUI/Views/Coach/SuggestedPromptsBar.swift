import SwiftUI

/// Horizontal scrolling bar of contextual suggested prompt chips.
struct SuggestedPromptsBar: View {
    let prompts: [SuggestedPrompt]
    let onSelect: (SuggestedPrompt) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: GymBroSpacing.sm) {
                ForEach(prompts) { prompt in
                    Button {
                        onSelect(prompt)
                    } label: {
                        HStack(spacing: GymBroSpacing.xs) {
                            Image(systemName: prompt.icon)
                                .font(.caption2)
                                .foregroundStyle(GymBroColors.accentCyan)
                            Text(prompt.text)
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textPrimary)
                        }
                        .padding(.horizontal, GymBroSpacing.md)
                        .padding(.vertical, GymBroSpacing.sm)
                        .background(GymBroColors.surfaceSecondary)
                        .clipShape(Capsule())
                        .overlay(
                            Capsule()
                                .strokeBorder(GymBroColors.border, lineWidth: 1)
                        )
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
        .padding(.vertical, GymBroSpacing.xs)
    }
}

#Preview("Suggested Prompts") {
    VStack {
        SuggestedPromptsBar(prompts: SuggestedPrompt.defaults) { prompt in
            print("Selected: \(prompt.text)")
        }
    }
    .gymBroDarkBackground()
}
