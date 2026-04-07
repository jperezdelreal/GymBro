import SwiftUI
import SwiftData
import GymBroCore
import os

public struct WorkoutRecoveryView: View {
    private static let logger = Logger(subsystem: "com.gymbro", category: "WorkoutRecoveryView")

    @Environment(\.modelContext) private var modelContext

    let workout: Workout
    let onResume: (Workout) -> Void
    let onDiscard: () -> Void

    @ScaledMetric(relativeTo: .largeTitle) private var iconSize: CGFloat = 48

    public init(
        workout: Workout,
        onResume: @escaping (Workout) -> Void,
        onDiscard: @escaping () -> Void
    ) {
        self.workout = workout
        self.onResume = onResume
        self.onDiscard = onDiscard
    }

    public var body: some View {
        VStack(spacing: GymBroSpacing.lg + GymBroSpacing.xs) {
            Spacer()

            Image(systemName: "arrow.counterclockwise.circle.fill")
                .font(.system(size: iconSize))
                .foregroundStyle(GymBroColors.accentAmber)
                .accessibilityHidden(true)

            VStack(spacing: GymBroSpacing.sm) {
                Text("Unfinished Workout")
                    .font(GymBroTypography.title2)
                    .foregroundStyle(GymBroColors.textPrimary)

                Text("You have an unfinished workout from \(formattedDate).")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, GymBroSpacing.xl)
            }

            workoutSummaryCard

            VStack(spacing: GymBroSpacing.md) {
                Button {
                    onResume(workout)
                } label: {
                    Label("Resume Workout", systemImage: "play.fill")
                }
                .buttonStyle(.gymBroPrimary)

                Button(role: .destructive) {
                    let service = WorkoutRecoveryService(modelContext: modelContext)
                    service.discardWorkout(workout)
                    onDiscard()
                } label: {
                    Label("Discard Workout", systemImage: "trash")
                }
                .buttonStyle(.gymBroDestructive)
            }
            .padding(.horizontal, GymBroSpacing.lg)

            Spacer()
        }
        .padding()
        .gymBroDarkBackground()
    }

    private var workoutSummaryCard: some View {
        GymBroCard {
            VStack(spacing: GymBroSpacing.md) {
                HStack(spacing: GymBroSpacing.lg) {
                    summaryItem(title: "Sets", value: "\(workout.totalSets)")
                    summaryItem(title: "Volume", value: String(format: "%.0f kg", workout.totalVolume))
                    summaryItem(title: "Exercises", value: "\(workout.exercises.count)")
                }

                if let startTime = workout.startTime {
                    HStack {
                        Image(systemName: "clock")
                            .foregroundStyle(GymBroColors.textTertiary)
                        Text("Started \(startTime, style: .relative) ago")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                }
            }
        }
        .padding(.horizontal, GymBroSpacing.lg)
    }

    private func summaryItem(title: String, value: String) -> some View {
        VStack(spacing: GymBroSpacing.xs) {
            Text(value)
                .font(GymBroTypography.title3)
                .fontDesign(.rounded)
                .foregroundStyle(GymBroColors.textPrimary)
            Text(title)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
        }
        .frame(maxWidth: .infinity)
    }

    private var formattedDate: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: workout.date, relativeTo: Date())
    }
}
