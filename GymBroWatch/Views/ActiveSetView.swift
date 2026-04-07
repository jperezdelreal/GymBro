import SwiftUI
import WatchKit

/// Primary Watch view: shows current exercise, weight/reps adjusters,
/// and a large "Complete Set" button optimized for sweaty-finger tapping.
struct ActiveSetView: View {
    @Bindable var viewModel: WatchWorkoutViewModel
    @State private var showingConfirmation = false

    var body: some View {
        ScrollView {
            VStack(spacing: 8) {
                // Exercise name & set number
                exerciseHeader

                // Weight adjuster with Digital Crown
                weightSection

                // Reps adjuster
                repsSection

                // Complete Set button — large, centered, minimum 44pt
                completeButton

                // Last set confirmation
                if let lastWeight = viewModel.lastCompletedWeight,
                   let lastReps = viewModel.lastCompletedReps {
                    lastSetBanner(weight: lastWeight, reps: lastReps)
                }
            }
            .padding(.horizontal, 4)
        }
        .focusable()
        .digitalCrownRotation(
            $viewModel.crownDelta,
            from: -100,
            through: 100,
            sensitivity: .medium,
            isContinuous: true,
            isHapticFeedbackEnabled: true
        )
        .onChange(of: viewModel.crownDelta) { _, newValue in
            viewModel.applyCrownRotation(newValue)
            viewModel.crownDelta = 0
        }
    }

    // MARK: - Subviews

    private var exerciseHeader: some View {
        VStack(spacing: 2) {
            Text(viewModel.exerciseName)
                .font(.system(.headline, design: .rounded))
                .lineLimit(2)
                .multilineTextAlignment(.center)
                .minimumScaleFactor(0.7)

            Text("Set \(viewModel.setNumber)")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }

    private var weightSection: some View {
        VStack(spacing: 4) {
            Text("WEIGHT")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.secondary)
                .tracking(1)

            HStack(spacing: 12) {
                Button {
                    viewModel.decrementWeight()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Decrease weight")
                .frame(minWidth: 44, minHeight: 44)

                Text(String(format: "%.1f", viewModel.weight))
                    .font(.system(.title2, design: .rounded, weight: .bold))
                    .monospacedDigit()
                    .frame(minWidth: 60)
                    .multilineTextAlignment(.center)

                Button {
                    viewModel.incrementWeight()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Increase weight")
                .frame(minWidth: 44, minHeight: 44)
            }

            Text("kg · Crown to adjust")
                .font(.system(size: 9))
                .foregroundStyle(.tertiary)
        }
    }

    private var repsSection: some View {
        VStack(spacing: 4) {
            Text("REPS")
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(.secondary)
                .tracking(1)

            HStack(spacing: 12) {
                Button {
                    viewModel.decrementReps()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Decrease reps")
                .frame(minWidth: 44, minHeight: 44)

                Text("\(viewModel.reps)")
                    .font(.system(.title2, design: .rounded, weight: .bold))
                    .monospacedDigit()
                    .frame(minWidth: 40)
                    .multilineTextAlignment(.center)

                Button {
                    viewModel.incrementReps()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.blue)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Increase reps")
                .frame(minWidth: 44, minHeight: 44)
            }
        }
    }

    private var completeButton: some View {
        Button {
            viewModel.completeSet()
            showingConfirmation = true
            Task {
                try? await Task.sleep(for: .seconds(1.5))
                showingConfirmation = false
            }
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "checkmark.circle.fill")
                    .font(.title3)
                Text("Complete Set")
                    .font(.system(.headline, design: .rounded))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(minHeight: 50)
            .background(showingConfirmation ? Color.blue : Color.green)
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
        .padding(.top, 4)
        .accessibilityHint("Logs the current set and starts rest timer")
    }

    private func lastSetBanner(weight: Double, reps: Int) -> some View {
        HStack(spacing: 4) {
            Image(systemName: "checkmark")
                .foregroundStyle(.green)
                .font(.caption2)
            Text("\(String(format: "%.1f", weight))kg × \(reps)")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .transition(.opacity)
        .animation(.easeInOut, value: viewModel.lastCompletedWeight)
    }
}
