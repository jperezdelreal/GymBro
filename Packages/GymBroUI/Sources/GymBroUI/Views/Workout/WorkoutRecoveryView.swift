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
        VStack(spacing: 28) {
            Spacer()

            Image(systemName: "arrow.counterclockwise.circle.fill")
                .font(.system(size: iconSize))
                .foregroundStyle(.orange)
                .accessibilityHidden(true)

            VStack(spacing: 8) {
                Text("Unfinished Workout")
                    .font(.title2)
                    .fontWeight(.bold)

                Text("You have an unfinished workout from \(formattedDate).")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            workoutSummaryCard

            VStack(spacing: 12) {
                Button {
                    onResume(workout)
                } label: {
                    Label("Resume Workout", systemImage: "play.fill")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color.green)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }

                Button(role: .destructive) {
                    let service = WorkoutRecoveryService(modelContext: modelContext)
                    service.discardWorkout(workout)
                    onDiscard()
                } label: {
                    Label("Discard Workout", systemImage: "trash")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(Color(.systemGray6))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
            }
            .padding(.horizontal, 24)

            Spacer()
        }
        .padding()
    }

    private var workoutSummaryCard: some View {
        VStack(spacing: 12) {
            HStack(spacing: 24) {
                summaryItem(title: "Sets", value: "\(workout.totalSets)")
                summaryItem(title: "Volume", value: String(format: "%.0f kg", workout.totalVolume))
                summaryItem(title: "Exercises", value: "\(workout.exercises.count)")
            }

            if let startTime = workout.startTime {
                HStack {
                    Image(systemName: "clock")
                        .foregroundStyle(.secondary)
                    Text("Started \(startTime, style: .relative) ago")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemGray6))
        )
        .padding(.horizontal, 24)
    }

    private func summaryItem(title: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.title3)
                .fontWeight(.bold)
                .fontDesign(.rounded)
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }

    private var formattedDate: String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return formatter.localizedString(for: workout.date, relativeTo: Date())
    }
}
