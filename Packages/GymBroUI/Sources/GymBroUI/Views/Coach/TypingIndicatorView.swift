import SwiftUI

/// Animated typing indicator with pulsing dots -- shows while AI is generating a response.
struct TypingIndicatorView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var animating = false

    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<3, id: \.self) { index in
                Circle()
                    .fill(GymBroColors.accentCyan)
                    .frame(width: 6, height: 6)
                    .scaleEffect(animating ? 1.0 : 0.5)
                    .opacity(animating ? 1.0 : 0.3)
                    .animation(
                        reduceMotion ? nil : .easeInOut(duration: 0.6)
                            .repeatForever(autoreverses: true)
                            .delay(Double(index) * 0.2),
                        value: animating
                    )
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(GymBroColors.surfaceSecondary)
        .clipShape(Capsule())
        .onAppear { animating = true }
        .accessibilityLabel("AI is thinking")
    }
}

#Preview("Typing Indicator") {
    TypingIndicatorView()
        .padding()
        .gymBroDarkBackground()
}
