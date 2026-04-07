import SwiftUI
import WatchKit

/// Watch rest timer view with circular countdown, skip action,
/// and haptic alerts at 10s and completion.
struct RestTimerWatchView: View {
    @Bindable var viewModel: WatchWorkoutViewModel

    var body: some View {
        VStack(spacing: 8) {
            if viewModel.isRestTimerActive {
                activeTimerContent
            } else {
                inactiveTimerContent
            }
        }
    }

    // MARK: - Active Timer

    private var activeTimerContent: some View {
        VStack(spacing: 6) {
            Text("REST")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.secondary)
                .tracking(1)

            // Circular progress
            ZStack {
                Circle()
                    .stroke(Color.gray.opacity(0.3), lineWidth: 6)

                Circle()
                    .trim(from: 0, to: 1 - viewModel.restTimerProgress)
                    .stroke(timerColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .animation(.linear(duration: 1), value: viewModel.restTimerProgress)

                VStack(spacing: 2) {
                    Text(viewModel.formattedRestTime)
                        .font(.system(.title, design: .rounded, weight: .bold))
                        .monospacedDigit()

                    Text("remaining")
                        .font(.system(size: 9))
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 110, height: 110)
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Rest timer \(viewModel.formattedRestTime) remaining")

            // Skip button
            Button {
                viewModel.skipRestTimer()
                WKInterfaceDevice.current().play(.click)
            } label: {
                Text("Skip Rest")
                    .font(.system(.caption, design: .rounded, weight: .semibold))
                    .foregroundStyle(.blue)
            }
            .buttonStyle(.plain)
            .frame(minWidth: 44, minHeight: 44)
        }
    }

    // MARK: - Inactive (workout stats)

    private var inactiveTimerContent: some View {
        VStack(spacing: 12) {
            Text("WORKOUT")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.secondary)
                .tracking(1)

            VStack(spacing: 8) {
                statRow(label: "Duration", value: viewModel.formattedDuration)
                statRow(label: "Sets", value: "\(viewModel.totalSetsCompleted)")
                statRow(
                    label: "Volume",
                    value: String(format: "%.0f kg", viewModel.totalVolume)
                )
            }
            .padding(.horizontal, 8)

            Text("Scroll down for next set")
                .font(.system(size: 9))
                .foregroundStyle(.tertiary)
                .multilineTextAlignment(.center)
        }
    }

    private func statRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Spacer()
            Text(value)
                .font(.system(.caption, design: .rounded, weight: .bold))
                .monospacedDigit()
        }
    }

    // MARK: - Timer Color

    private var timerColor: Color {
        if viewModel.restTimerRemaining <= 10 {
            return .red
        } else if viewModel.restTimerRemaining <= 30 {
            return .orange
        }
        return .blue
    }
}
