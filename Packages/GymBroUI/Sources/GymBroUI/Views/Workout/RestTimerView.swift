import SwiftUI
import GymBroCore

public struct RestTimerView: View {
    @State private var timerService = RestTimerService.shared
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @ScaledMetric(relativeTo: .largeTitle) private var timerFontSize: CGFloat = 56

    public init() {}

    public var body: some View {
        VStack(spacing: GymBroSpacing.lg) {
            ZStack {
                Circle()
                    .stroke(GymBroColors.surfaceElevated, lineWidth: 12)
                    .frame(width: 200, height: 200)

                Circle()
                    .trim(from: 0, to: progress)
                    .stroke(
                        timerColor,
                        style: StrokeStyle(lineWidth: 12, lineCap: .round)
                    )
                    .frame(width: 200, height: 200)
                    .rotationEffect(.degrees(-90))
                    .animation(reduceMotion ? nil : .linear(duration: 1), value: progress)

                VStack(spacing: GymBroSpacing.xs) {
                    Text(timeString)
                        .font(.system(size: timerFontSize, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(GymBroColors.textPrimary)

                    Text("remaining")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }

            if let nextSet = timerService.nextSetInfo {
                GymBroCard {
                    VStack(spacing: GymBroSpacing.sm) {
                        Text("UP NEXT")
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textTertiary)
                            .tracking(1.5)

                        Text(nextSet.exerciseName)
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.textPrimary)

                        HStack(spacing: GymBroSpacing.md) {
                            HStack(spacing: GymBroSpacing.xs) {
                                Text("Set")
                                    .foregroundStyle(GymBroColors.textSecondary)
                                Text("\(nextSet.setNumber)")
                                    .fontWeight(.bold)
                                    .foregroundStyle(GymBroColors.textPrimary)
                            }

                            HStack(spacing: GymBroSpacing.xs) {
                                Text("\(Int(nextSet.targetWeight))")
                                    .fontWeight(.bold)
                                    .foregroundStyle(GymBroColors.textPrimary)
                                Text(nextSet.weightUnit)
                                    .foregroundStyle(GymBroColors.textSecondary)
                            }

                            HStack(spacing: GymBroSpacing.xs) {
                                Text("\(nextSet.targetReps)")
                                    .fontWeight(.bold)
                                    .foregroundStyle(GymBroColors.textPrimary)
                                Text("reps")
                                    .foregroundStyle(GymBroColors.textSecondary)
                            }
                        }
                        .font(GymBroTypography.subheadline)
                    }
                }
            }

            HStack(spacing: GymBroSpacing.md) {
                Button {
                    timerService.addTime(-30)
                } label: {
                    Label("-30s", systemImage: "minus.circle.fill")
                }
                .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.accentCyan))
                .disabled(timerService.remainingSeconds <= 30)

                Button {
                    timerService.addTime(30)
                } label: {
                    Label("+30s", systemImage: "plus.circle.fill")
                }
                .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.accentCyan))
            }

            Button {
                timerService.skip()
            } label: {
                Text("Skip Rest")
            }
            .buttonStyle(.gymBroPrimary)
        }
        .padding()
        .gymBroDarkBackground()
    }

    private var progress: Double {
        guard timerService.totalSeconds > 0 else { return 0 }
        return Double(timerService.remainingSeconds) / Double(timerService.totalSeconds)
    }

    private var timeString: String {
        let minutes = timerService.remainingSeconds / 60
        let seconds = timerService.remainingSeconds % 60
        return String(format: "%d:%02d", minutes, seconds)
    }

    private var timerColor: Color {
        if timerService.remainingSeconds <= 10 {
            return GymBroColors.accentRed
        } else if timerService.remainingSeconds <= 30 {
            return GymBroColors.accentAmber
        } else {
            return GymBroColors.accentCyan
        }
    }
}

#Preview("Rest Timer") {
    RestTimerView()
        .onAppear {
            RestTimerService.shared.start(
                duration: 120,
                nextSetInfo: NextSetInfo(
                    exerciseName: "Barbell Squat",
                    setNumber: 3,
                    targetReps: 5,
                    targetWeight: 140.0,
                    weightUnit: "kg"
                )
            )
        }
}
