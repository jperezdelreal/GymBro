import SwiftUI
import SwiftData
import GymBroCore

/// Preview sheet for a generated workout — shows exercises, reasoning trail, and confirm/cancel.
///
/// Presented as a sheet from any entry point (hero card, program detail, active program).
/// User reviews the plan, then confirms to start ActiveWorkoutView.
public struct WorkoutPreviewSheet: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var generatorVM: WorkoutGeneratorViewModel
    @State private var showReasoning = false
    @State private var activeWorkoutVM: ActiveWorkoutViewModel?
    @State private var navigateToWorkout = false

    @ScaledMetric private var exerciseIconSize: CGFloat = 28

    /// Called when the user confirms and starts the workout.
    private let onStartWorkout: (ActiveWorkoutViewModel) -> Void

    public init(
        generatorVM: WorkoutGeneratorViewModel,
        onStartWorkout: @escaping (ActiveWorkoutViewModel) -> Void
    ) {
        self._generatorVM = State(initialValue: generatorVM)
        self.onStartWorkout = onStartWorkout
    }

    public var body: some View {
        NavigationStack {
            ZStack {
                GymBroColors.background.ignoresSafeArea()

                if generatorVM.isGenerating {
                    generatingState
                } else if let workout = generatorVM.generatedWorkout {
                    if workout.isRestDay {
                        restDayState(workout)
                    } else {
                        workoutPreview(workout)
                    }
                } else {
                    emptyState
                }
            }
            .navigationTitle("Smart Workout")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(GymBroColors.textSecondary)
                }
            }
            .task {
                if generatorVM.generatedWorkout == nil && !generatorVM.isGenerating {
                    generatorVM.generateWorkout()
                }
            }
        }
    }

    // MARK: - Generating State

    private var generatingState: some View {
        VStack(spacing: GymBroSpacing.lg) {
            ProgressView()
                .tint(GymBroColors.accentGreen)
                .scaleEffect(1.5)

            Text("Building your workout…")
                .font(GymBroTypography.headline)
                .foregroundStyle(GymBroColors.textPrimary)

            Text("Analyzing recovery, readiness, and program")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
        }
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: GymBroSpacing.lg) {
            Image(systemName: "wand.and.stars")
                .font(.system(size: 48))
                .foregroundStyle(GymBroColors.textTertiary)

            Text("Tap to generate")
                .font(GymBroTypography.headline)
                .foregroundStyle(GymBroColors.textSecondary)

            Button("Generate Workout") {
                generatorVM.generateWorkout()
            }
            .buttonStyle(.gymBroPrimary)
            .padding(.horizontal, GymBroSpacing.xl)
        }
    }

    // MARK: - Rest Day

    private func restDayState(_ workout: GeneratedWorkout) -> some View {
        VStack(spacing: GymBroSpacing.lg) {
            Image(systemName: "moon.zzz.fill")
                .font(.system(size: 56))
                .foregroundStyle(GymBroColors.accentAmber)

            Text("Rest Day Recommended")
                .font(GymBroTypography.title2)
                .foregroundStyle(GymBroColors.textPrimary)

            Text(workout.recommendation.rationale)
                .font(GymBroTypography.body)
                .foregroundStyle(GymBroColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, GymBroSpacing.lg)

            reasoningSection(workout)

            Button("Got It") { dismiss() }
                .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.accentAmber))
                .padding(.horizontal, GymBroSpacing.lg)
        }
        .padding(.vertical, GymBroSpacing.xxl)
    }

    // MARK: - Workout Preview

    private func workoutPreview(_ workout: GeneratedWorkout) -> some View {
        VStack(spacing: 0) {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    sessionHeader(workout)
                    exerciseList(workout)
                    reasoningSection(workout)
                }
                .padding(.horizontal, GymBroSpacing.md)
                .padding(.top, GymBroSpacing.md)
                .padding(.bottom, 120)
            }

            confirmBar(workout)
        }
    }

    // MARK: - Session Header

    private func sessionHeader(_ workout: GeneratedWorkout) -> some View {
        GymBroCard(accent: GymBroColors.accentGreen) {
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                Text(workout.sessionName)
                    .font(GymBroTypography.title3)
                    .foregroundStyle(GymBroColors.textPrimary)

                HStack(spacing: GymBroSpacing.lg) {
                    Label("\(workout.exercises.count) exercises", systemImage: "dumbbell.fill")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)

                    Label("~\(workout.estimatedDurationMinutes) min", systemImage: "clock")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)

                    Spacer()
                }

                actionBadge(workout.recommendation.action)
            }
        }
    }

    @ViewBuilder
    private func actionBadge(_ action: RecommendedAction) -> some View {
        let (text, color, icon) = actionInfo(action)
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 12))
            Text(text)
                .font(GymBroTypography.caption)
        }
        .foregroundStyle(color)
        .padding(.horizontal, GymBroSpacing.sm)
        .padding(.vertical, GymBroSpacing.xs)
        .background(
            Capsule().fill(color.opacity(0.12))
        )
    }

    private func actionInfo(_ action: RecommendedAction) -> (String, Color, String) {
        switch action {
        case .proceedAsPlanned:
            return ("Full send", GymBroColors.accentGreen, "bolt.fill")
        case .lighterVariant:
            return ("Lighter variant", GymBroColors.accentAmber, "arrow.down.circle")
        case .modifyExercises:
            return ("Modified exercises", GymBroColors.accentCyan, "arrow.triangle.2.circlepath")
        case .restDay:
            return ("Rest day", GymBroColors.accentRed, "moon.zzz.fill")
        }
    }

    // MARK: - Exercise List

    private func exerciseList(_ workout: GeneratedWorkout) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("EXERCISES")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            ForEach(Array(workout.exercises.enumerated()), id: \.element.id) { index, exercise in
                exercisePreviewRow(exercise, index: index)
            }
        }
    }

    private func exercisePreviewRow(_ exercise: GeneratedExercise, index: Int) -> some View {
        GymBroCard {
            HStack(spacing: GymBroSpacing.md) {
                Circle()
                    .fill(GymBroColors.surfaceElevated)
                    .frame(width: exerciseIconSize, height: exerciseIconSize)
                    .overlay(
                        Text("\(index + 1)")
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textSecondary)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(exercise.exerciseName)
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textPrimary)

                    HStack(spacing: GymBroSpacing.sm) {
                        Text("\(exercise.targetSets)×\(exercise.targetReps)")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.accentCyan)

                        if let rpe = exercise.targetRPE {
                            Text("RPE \(String(format: "%.0f", rpe))")
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.accentAmber)
                        }

                        if exercise.weightMultiplier < 1.0 {
                            Text("\(Int(exercise.weightMultiplier * 100))% load")
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.accentAmber)
                        }
                    }

                    if !exercise.notes.isEmpty {
                        Text(exercise.notes)
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textTertiary)
                            .lineLimit(2)
                    }
                }

                Spacer()

                Button {
                    generatorVM.swapExercise(at: index)
                } label: {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.system(size: 14))
                        .foregroundStyle(GymBroColors.textTertiary)
                        .frame(width: 36, height: 36)
                        .background(GymBroColors.surfaceElevated, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel("Swap \(exercise.exerciseName)")
            }
        }
    }

    // MARK: - Reasoning Section

    private func reasoningSection(_ workout: GeneratedWorkout) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Button {
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                    showReasoning.toggle()
                }
            } label: {
                HStack(spacing: GymBroSpacing.sm) {
                    Image(systemName: "brain.head.profile")
                        .foregroundStyle(GymBroColors.accentCyan)

                    Text("WHY THIS WORKOUT?")
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .tracking(2)

                    Spacer()

                    Image(systemName: showReasoning ? "chevron.up" : "chevron.down")
                        .font(.system(size: 12))
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(showReasoning ? "Hide reasoning" : "Show reasoning")

            if showReasoning {
                GymBroCard(accent: GymBroColors.accentCyan) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        ForEach(workout.reasoning.indices, id: \.self) { idx in
                            let step = workout.reasoning[idx]
                            reasoningRow(step)
                        }
                    }
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    private func reasoningRow(_ step: ReasoningStep) -> some View {
        HStack(alignment: .top, spacing: GymBroSpacing.sm) {
            Image(systemName: reasoningIcon(step.factor))
                .font(.system(size: 12))
                .foregroundStyle(reasoningColor(step.impact))
                .frame(width: 16, alignment: .center)

            Text(step.summary)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private func reasoningIcon(_ factor: ReasoningFactor) -> String {
        switch factor {
        case .muscleRecovery: return "figure.cooldown"
        case .readiness: return "heart.fill"
        case .overtraining: return "exclamationmark.triangle"
        case .programTemplate: return "calendar"
        case .exerciseHistory: return "clock.arrow.circlepath"
        case .timeConstraint: return "clock"
        case .userProfile: return "person.fill"
        }
    }

    private func reasoningColor(_ impact: ReasoningImpact) -> Color {
        switch impact {
        case .exerciseSelection: return GymBroColors.accentCyan
        case .volumeIntensity: return GymBroColors.accentAmber
        case .restDay: return GymBroColors.accentRed
        }
    }

    // MARK: - Confirm Bar

    private func confirmBar(_ workout: GeneratedWorkout) -> some View {
        VStack(spacing: GymBroSpacing.sm) {
            Divider().background(GymBroColors.border)

            Button {
                startGeneratedWorkout()
            } label: {
                HStack(spacing: GymBroSpacing.sm) {
                    Image(systemName: "flame.fill")
                    Text("Let's Go")
                }
            }
            .buttonStyle(.gymBroPrimary)
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.bottom, GymBroSpacing.md)
        }
        .background(GymBroColors.background)
    }

    // MARK: - Actions

    private func startGeneratedWorkout() {
        guard let workout = generatorVM.startWorkout() else { return }

        let exercises = workout.exercises
        let vm = ActiveWorkoutViewModel(
            modelContext: modelContext,
            workout: workout,
            exercises: exercises
        )
        dismiss()
        onStartWorkout(vm)
    }
}

// MARK: - Preview

#Preview("Workout Preview") {
    WorkoutPreviewSheet(
        generatorVM: WorkoutGeneratorViewModel(
            modelContext: try! ModelContext(
                ModelContainer(for: Program.self, Exercise.self, Workout.self, ExerciseSet.self,
                               configurations: ModelConfiguration(isStoredInMemoryOnly: true))
            )
        ),
        onStartWorkout: { _ in }
    )
}
