import SwiftUI
import GymBroCore

/// A button that fires repeatedly while held, with haptic clicks per increment.
/// Used for weight/rep steppers — enables fast one-handed adjustment.
public struct RepeatButton: View {
    let systemImage: String
    let action: () -> Void
    let accessibilityLabel: String

    @State private var repeatTask: Task<Void, Never>?
    @ScaledMetric(relativeTo: .title) private var iconSize: CGFloat = 36

    private let initialDelay: Duration = .milliseconds(400)
    private let repeatInterval: Duration = .milliseconds(120)

    public init(
        systemImage: String,
        accessibilityLabel: String,
        action: @escaping () -> Void
    ) {
        self.systemImage = systemImage
        self.accessibilityLabel = accessibilityLabel
        self.action = action
    }

    public var body: some View {
        Image(systemName: systemImage)
            .font(.system(size: iconSize))
            .foregroundStyle(.blue)
            .contentShape(Rectangle())
            .accessibilityLabel(self.accessibilityLabel)
            .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
                if pressing {
                    action()
                    HapticFeedbackService.shared.valueChanged()
                    startRepeating()
                } else {
                    stopRepeating()
                }
            }, perform: {})
    }

    private func startRepeating() {
        repeatTask = Task { @MainActor in
            try? await Task.sleep(for: initialDelay)
            while !Task.isCancelled {
                action()
                HapticFeedbackService.shared.valueChanged()
                try? await Task.sleep(for: repeatInterval)
            }
        }
    }

    private func stopRepeating() {
        repeatTask?.cancel()
        repeatTask = nil
    }
}
