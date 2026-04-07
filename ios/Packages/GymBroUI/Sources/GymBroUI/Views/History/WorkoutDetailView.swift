import SwiftUI
import SwiftData
import GymBroCore

/// Drill-down view showing full details of a completed workout:
/// exercises performed, sets/reps/weight, total volume, duration, and PRs.
public struct WorkoutDetailView: View {
    let workout: Workout

    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .title2) private var statValueSize: CGFloat = 24
    @ScaledMetric(relativeTo: .title3) private var exerciseNameSize: CGFloat = 18

    public init(workout: Workout) {
        self.workout = workout
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                summaryCards
                exerciseBreakdown
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.top, GymBroSpacing.sm)
            .padding(.bottom, GymBroSpacing.xxl)
        }
        .gymBroDarkBackground()
        .navigationTitle(workoutTitle)
        .navigationBarTitleDisplayMode(.inline)
    }

    // MARK: - Title

    private var workoutTitle: String {
        workout.date.formatted(date: .abbreviated, time: .omitted)
    }

    // MARK: - Summary Cards

    @ViewBuilder
    private var summaryCards: some View {
        VStack(spacing: GymBroSpacing.md) {
            HStack(spacing: GymBroSpacing.md) {
                statCard(
                    icon: "clock.fill",
                    title: "Duration",
                    value: formattedDuration,
                    color: GymBroColors.accentCyan
                )
                statCard(
                    icon: "scalemass.fill",
                    title: "Volume",
                    value: String(format: "%.0f kg", workout.totalVolume),
                    color: GymBroColors.accentGreen
                )
            }

            HStack(spacing: GymBroSpacing.md) {
                statCard(
                    icon: "chart.bar.fill",
                    title: "Sets",
                    value: "\(workout.totalSets)",
                    color: GymBroColors.accentAmber
                )
                statCard(
                    icon: "figure.strengthtraining.traditional",
                    title: "Exercises",
                    value: "\(workout.exercises.count)",
                    color: GymBroColors.accentCyan
                )
            }

            if prCount > 0 {
                GymBroCard(accent: GymBroColors.accentAmber) {
                    HStack(spacing: GymBroSpacing.md) {
                        Image(systemName: "star.fill")
                            .foregroundStyle(GymBroColors.accentAmber)
                        Text("\(prCount) Personal Record\(prCount == 1 ? "" : "s") set")
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.accentAmber)
                        Spacer()
                    }
                }
            }

            if !workout.notes.isEmpty {
                GymBroCard {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text("NOTES")
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textTertiary)
                            .tracking(1.5)
                        Text(workout.notes)
                            .font(GymBroTypography.body)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func statCard(icon: String, title: String, value: String, color: Color) -> some View {
        GymBroCard(accent: color) {
            VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                Image(systemName: icon)
                    .foregroundStyle(color)

                Text(value)
                    .font(GymBroTypography.monoNumber(size: statValueSize))
                    .foregroundStyle(GymBroColors.textPrimary)

                Text(title.uppercased())
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(1)
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Exercise Breakdown

    @ViewBuilder
    private var exerciseBreakdown: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("EXERCISES")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            ForEach(workout.exercises, id: \.id) { exercise in
                exerciseCard(exercise: exercise)
            }
        }
    }

    @ViewBuilder
    private func exerciseCard(exercise: Exercise) -> some View {
        let sets = setsForExercise(exercise)
        let exerciseVolume = sets.reduce(0.0) { $0 + $1.volume }
        let bestSet = sets.max(by: { $0.estimatedOneRepMax < $1.estimatedOneRepMax })

        GymBroCard {
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text(exercise.name)
                            .font(.system(size: exerciseNameSize, weight: .bold))
                            .foregroundStyle(GymBroColors.textPrimary)

                        HStack(spacing: GymBroSpacing.sm) {
                            Text(exercise.category.rawValue.capitalized)
                                .font(GymBroTypography.caption2)
                                .foregroundStyle(categoryColor(exercise.category))

                            Text("•")
                                .foregroundStyle(GymBroColors.textTertiary)

                            Text(String(format: "%.0f kg total", exerciseVolume))
                                .font(GymBroTypography.caption2)
                                .foregroundStyle(GymBroColors.textTertiary)
                        }
                    }
                    Spacer()
                }

                // Sets table
                VStack(spacing: 0) {
                    // Header
                    HStack {
                        Text("SET")
                            .frame(width: 36, alignment: .leading)
                        Text("WEIGHT")
                            .frame(maxWidth: .infinity, alignment: .leading)
                        Text("REPS")
                            .frame(width: 50, alignment: .center)
                        Text("RPE")
                            .frame(width: 40, alignment: .trailing)
                    }
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(1)
                    .padding(.bottom, GymBroSpacing.xs)

                    Divider()
                        .background(GymBroColors.border)

                    ForEach(sets, id: \.id) { set in
                        setRow(set: set, isBest: set.id == bestSet?.id)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func setRow(set: ExerciseSet, isBest: Bool) -> some View {
        HStack {
            HStack(spacing: GymBroSpacing.xs) {
                Text("\(set.setNumber)")
                    .frame(width: 20, alignment: .leading)
                if set.isWarmup {
                    Text("W")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundStyle(GymBroColors.accentAmber)
                }
            }
            .frame(width: 36, alignment: .leading)

            Text(String(format: "%.1f kg", set.weightKg))
                .frame(maxWidth: .infinity, alignment: .leading)

            Text("\(set.totalReps)")
                .frame(width: 50, alignment: .center)

            Group {
                if let rpe = set.rpe {
                    Text(String(format: "%.0f", rpe))
                } else {
                    Text("—")
                }
            }
            .frame(width: 40, alignment: .trailing)
        }
        .font(GymBroTypography.subheadline)
        .foregroundStyle(set.isWarmup ? GymBroColors.textTertiary : GymBroColors.textPrimary)
        .padding(.vertical, GymBroSpacing.sm)
        .background(isBest && !set.isWarmup ? GymBroColors.accentGreen.opacity(0.05) : Color.clear)
        .overlay(alignment: .trailing) {
            if isBest && !set.isWarmup {
                Image(systemName: "star.fill")
                    .font(.system(size: 10))
                    .foregroundStyle(GymBroColors.accentAmber)
                    .padding(.trailing, -GymBroSpacing.xs)
            }
        }
    }

    // MARK: - Helpers

    private func setsForExercise(_ exercise: Exercise) -> [ExerciseSet] {
        workout.sets
            .filter { $0.exercise?.id == exercise.id && $0.completedAt != nil }
            .sorted { $0.setNumber < $1.setNumber }
    }

    private var formattedDuration: String {
        guard let duration = workout.duration else { return "—" }
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        }
        return "\(minutes)m"
    }

    private var prCount: Int {
        guard let prService = try? PersonalRecordService(modelContext: modelContext) as PersonalRecordService? else {
            return 0
        }
        var count = 0
        for exercise in workout.exercises {
            if let records = try? prService.getPersonalRecords(for: exercise) {
                for record in records {
                    if record.exerciseSet.workout?.id == workout.id {
                        count += 1
                    }
                }
            }
        }
        return count
    }

    private func categoryColor(_ category: ExerciseCategory) -> Color {
        switch category {
        case .compound: return GymBroColors.accentGreen
        case .isolation: return GymBroColors.accentAmber
        case .accessory: return GymBroColors.accentCyan
        case .cardio: return GymBroColors.accentRed
        }
    }
}

// MARK: - Preview

#Preview("Workout Detail") {
    NavigationStack {
        WorkoutDetailView(
            workout: {
                let w = Workout(date: Date(), notes: "Great session")
                w.startTime = Date().addingTimeInterval(-3600)
                w.endTime = Date()
                return w
            }()
        )
    }
}
