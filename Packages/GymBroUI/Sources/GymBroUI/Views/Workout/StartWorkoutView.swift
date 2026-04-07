import SwiftUI
import SwiftData
import GymBroCore
import os

public struct StartWorkoutView: View {
    private static let logger = Logger(subsystem: "com.gymbro", category: "StartWorkout")

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss

    @State private var selectedProgram: Program?
    @State private var selectedProgramDay: ProgramDay?
    @State private var showingActiveWorkout = false
    @State private var activeWorkoutViewModel: ActiveWorkoutViewModel?

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: GymBroSpacing.lg) {
                    quickStartSection
                    programSection
                }
                .padding(.horizontal, GymBroSpacing.md)
                .padding(.top, GymBroSpacing.md + GymBroSpacing.xs)
            }
            .gymBroDarkBackground()
            .navigationTitle("Start Workout")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                    .foregroundStyle(GymBroColors.textSecondary)
                }
            }
            .navigationDestination(isPresented: $showingActiveWorkout) {
                if let viewModel = activeWorkoutViewModel {
                    ActiveWorkoutView(viewModel: viewModel)
                }
            }
        }
    }

    private var quickStartSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("QUICK START")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.5)

            Button {
                startEmptyWorkout()
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        HStack {
                            Image(systemName: "dumbbell.fill")
                                .font(.title2)
                                .foregroundStyle(GymBroColors.accentGreen)
                            Text("Empty Workout")
                                .font(GymBroTypography.title3)
                                .foregroundStyle(GymBroColors.textPrimary)
                        }

                        Text("Start logging without a program")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }

                    Spacer()

                    Image(systemName: "arrow.right")
                        .font(.title3)
                        .foregroundStyle(GymBroColors.accentGreen)
                }
                .padding(GymBroSpacing.md + GymBroSpacing.xs)
                .background(
                    RoundedRectangle(cornerRadius: GymBroRadius.lg)
                        .fill(GymBroColors.accentGreen.opacity(0.08))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: GymBroRadius.lg)
                        .strokeBorder(GymBroColors.accentGreen.opacity(0.2), lineWidth: 1)
                )
            }
            .buttonStyle(.plain)
        }
    }

    private var programSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("FROM PROGRAM")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.5)

            Text("Select a program day to begin")
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)

            VStack(spacing: GymBroSpacing.md) {
                ForEach(0..<3) { index in
                    programDayCard(
                        title: "Day \(index + 1) - Push",
                        exercises: "Bench Press, Overhead Press, Dips",
                        onSelect: {
                            // TODO: Implement program day selection
                        }
                    )
                }
            }
        }
    }

    private func programDayCard(title: String, exercises: String, onSelect: @escaping () -> Void) -> some View {
        Button {
            onSelect()
        } label: {
            GymBroCard {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text(title)
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.textPrimary)
                        Text(exercises)
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                            .lineLimit(2)
                    }

                    Spacer()

                    Image(systemName: "chevron.right")
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
        }
        .buttonStyle(.plain)
    }

    private func startEmptyWorkout() {
        let workout = Workout()
        workout.isActive = true
        modelContext.insert(workout)

        do {
            try modelContext.save()
            activeWorkoutViewModel = ActiveWorkoutViewModel(
                modelContext: modelContext,
                workout: workout,
                exercises: []
            )
            showingActiveWorkout = true
        } catch {
            Self.logger.error("Failed to create workout: \(error.localizedDescription)")
        }
    }
}

// MARK: - Preview

#Preview("Start Workout") {
    StartWorkoutView()
}
