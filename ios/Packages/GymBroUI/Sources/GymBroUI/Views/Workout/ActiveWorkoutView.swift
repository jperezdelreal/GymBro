import SwiftUI
import GymBroCore

public struct ActiveWorkoutView: View {
    @State private var viewModel: ActiveWorkoutViewModel
    @State private var showingExercisePicker = false
    @State private var showingRPEPicker = false
    @State private var showingSummary = false
    @State private var workoutSummary: WorkoutSummary?

    @ScaledMetric(relativeTo: .title3) private var statFontSize: CGFloat = 20
    @ScaledMetric(relativeTo: .title) private var exerciseNameSize: CGFloat = 24
    @ScaledMetric(relativeTo: .title) private var adjustButtonSize: CGFloat = 36
    @ScaledMetric(relativeTo: .title2) private var adjustValueSize: CGFloat = 28
    @ScaledMetric(relativeTo: .largeTitle) private var emptyIconSize: CGFloat = 60

    private let unitSystem: UnitSystem

    public init(viewModel: ActiveWorkoutViewModel, unitSystem: UnitSystem = .metric) {
        self._viewModel = State(initialValue: viewModel)
        self.unitSystem = unitSystem
    }

    public var body: some View {
        ZStack {
            GymBroColors.background.ignoresSafeArea()

            VStack(spacing: 0) {
                workoutHeader

                ScrollView {
                    VStack(spacing: GymBroSpacing.lg) {
                        if let exercise = viewModel.activeExercise {
                            currentExerciseSection(exercise)
                        } else {
                            emptyStateView
                        }

                        if !viewModel.completedSetsForActiveExercise.isEmpty {
                            completedSetsSection
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    .padding(.top, GymBroSpacing.md + GymBroSpacing.xs)
                    .padding(.bottom, 200)
                }

                Spacer()
            }

            VStack {
                Spacer()
                bottomActionBar
            }

            // PR Celebration Overlay
            if let prResult = viewModel.activePRCelebration {
                PRCelebrationOverlay(result: prResult) {
                    viewModel.dismissPRCelebration()
                }
                .transition(.opacity)
                .zIndex(100)
            }
        }
        .navigationTitle("Active Workout")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Finish") {
                    finishWorkout()
                }
                .foregroundStyle(GymBroColors.accentRed)
                .disabled(!viewModel.isWorkoutStarted)
            }
        }
        .sheet(isPresented: $showingSummary) {
            if let summary = workoutSummary {
                WorkoutSummaryView(summary: summary)
            }
        }
        .task {
            HapticFeedbackService.shared.prepare()
        }
        .preferredColorScheme(.dark)
    }

    // MARK: - Header

    private var workoutHeader: some View {
        HStack(spacing: GymBroSpacing.lg) {
            statItem(title: "Duration", value: formatDuration(viewModel.workoutDuration))
            statItem(title: "Volume", value: String(format: "%.0f kg", viewModel.totalVolume))
            statItem(title: "Sets", value: "\(viewModel.totalCompletedSets)")
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.vertical, GymBroSpacing.md)
        .background(GymBroColors.surfacePrimary)
    }

    private func statItem(title: String, value: String) -> some View {
        VStack(spacing: GymBroSpacing.xs) {
            Text(value)
                .font(GymBroTypography.monoNumber(size: statFontSize))
                .foregroundStyle(GymBroColors.textPrimary)
            Text(title)
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Current Exercise

    private func currentExerciseSection(_ exercise: Exercise) -> some View {
        GymBroCard(accent: GymBroColors.accentGreen) {
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text(exercise.name)
                            .font(.system(size: exerciseNameSize, weight: .bold))
                            .foregroundStyle(GymBroColors.textPrimary)
                        Text("Set \(viewModel.activeSetNumber)")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.accentGreen)
                    }
                    Spacer()
                }

                // Weight & Reps
                HStack(spacing: GymBroSpacing.md + GymBroSpacing.xs) {
                    weightAdjuster
                    repsAdjuster
                }

                // Warmup & RPE
                HStack(spacing: GymBroSpacing.md) {
                    warmupToggle
                    Spacer()
                    rpeButton
                }
            }
        }
        .confirmationDialog("Select RPE", isPresented: $showingRPEPicker) {
            ForEach(6...10, id: \.self) { rpe in
                Button("RPE \(rpe)") {
                    viewModel.updateRPE(Double(rpe))
                }
            }
            Button("Clear RPE", role: .destructive) {
                viewModel.updateRPE(nil)
            }
        }
    }

    private var weightAdjuster: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("WEIGHT")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1)

            HStack(spacing: GymBroSpacing.md) {
                Button { adjustWeight(-2.5) } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: adjustButtonSize))
                        .foregroundStyle(GymBroColors.accentCyan)
                }
                .accessibilityLabel("Decrease weight")

                Text(formatWeight(viewModel.currentWeight))
                    .font(GymBroTypography.monoNumber(size: adjustValueSize))
                    .foregroundStyle(GymBroColors.textPrimary)
                    .frame(minWidth: 100)
                    .multilineTextAlignment(.center)

                Button { adjustWeight(2.5) } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: adjustButtonSize))
                        .foregroundStyle(GymBroColors.accentCyan)
                }
                .accessibilityLabel("Increase weight")
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var repsAdjuster: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("REPS")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1)

            HStack(spacing: GymBroSpacing.md) {
                Button {
                    viewModel.updateReps(viewModel.currentReps - 1)
                    HapticFeedbackService.shared.valueChanged()
                } label: {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: adjustButtonSize))
                        .foregroundStyle(GymBroColors.accentCyan)
                }
                .accessibilityLabel("Decrease reps")

                Text("\(viewModel.currentReps)")
                    .font(GymBroTypography.monoNumber(size: adjustValueSize))
                    .foregroundStyle(GymBroColors.textPrimary)
                    .frame(minWidth: 60)
                    .multilineTextAlignment(.center)

                Button {
                    viewModel.updateReps(viewModel.currentReps + 1)
                    HapticFeedbackService.shared.valueChanged()
                } label: {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: adjustButtonSize))
                        .foregroundStyle(GymBroColors.accentCyan)
                }
                .accessibilityLabel("Increase reps")
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var warmupToggle: some View {
        Button {
            viewModel.toggleWarmup()
            HapticFeedbackService.shared.lightImpact()
        } label: {
            HStack {
                Image(systemName: viewModel.isWarmup ? "checkmark.circle.fill" : "circle")
                Text("Warmup Set")
            }
            .font(GymBroTypography.subheadline)
            .foregroundStyle(viewModel.isWarmup ? GymBroColors.accentAmber : GymBroColors.textSecondary)
        }
    }

    private var rpeButton: some View {
        Button {
            showingRPEPicker = true
        } label: {
            HStack {
                Image(systemName: "gauge.with.needle")
                if let rpe = viewModel.currentRPE {
                    Text("RPE \(Int(rpe))")
                } else {
                    Text("Add RPE")
                }
            }
            .font(GymBroTypography.subheadline)
            .foregroundStyle(rpeColor)
        }
        .animation(.easeInOut(duration: 0.3), value: viewModel.currentRPE)
    }

    private var rpeColor: Color {
        guard let rpe = viewModel.currentRPE else {
            return GymBroColors.textSecondary
        }
        switch Int(rpe) {
        case 6...7: return GymBroColors.accentGreen
        case 8: return GymBroColors.accentAmber
        case 9...10: return GymBroColors.accentRed
        default: return GymBroColors.accentAmber
        }
    }

    // MARK: - Completed Sets

    private var completedSetsSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("COMPLETED SETS")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.5)

            ForEach(Array(viewModel.completedSetsForActiveExercise.enumerated()), id: \.element.id) { _, set in
                HStack(spacing: GymBroSpacing.sm) {
                    ExerciseSetRow(
                        set: set,
                        setNumber: set.setNumber,
                        unitSystem: unitSystem
                    )

                    if let recordTypes = viewModel.prRecordsBySetId[set.id], !recordTypes.isEmpty {
                        PRBadge(recordTypes: recordTypes)
                    }
                }
            }
        }
    }

    // MARK: - Empty State

    private var emptyStateView: some View {
        VStack(spacing: GymBroSpacing.lg) {
            Image(systemName: "dumbbell")
                .font(.system(size: emptyIconSize))
                .foregroundStyle(GymBroColors.textTertiary)
                .accessibilityHidden(true)

            VStack(spacing: GymBroSpacing.sm) {
                Text("Ready to Lift")
                    .font(GymBroTypography.title2)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("Add an exercise to begin your session")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textSecondary)
            }

            Button {
                showingExercisePicker = true
            } label: {
                Label("Add Exercise", systemImage: "plus.circle.fill")
            }
            .buttonStyle(.gymBroPrimary)
            .frame(maxWidth: 240)
        }
        .padding(.top, GymBroSpacing.xxl)
    }

    // MARK: - Bottom Bar

    private var bottomActionBar: some View {
        VStack(spacing: GymBroSpacing.md) {
            if viewModel.isRestTimerActive, let endTime = viewModel.restTimerEndTime {
                InlineRestTimerView(endTime: endTime) {
                    viewModel.skipRestTimer()
                }
            }

            Button {
                viewModel.completeSet()
            } label: {
                Label("Complete Set", systemImage: "checkmark.circle.fill")
            }
            .buttonStyle(.gymBroPrimary)
            .disabled(viewModel.activeExercise == nil)
        }
        .padding(.horizontal, GymBroSpacing.md)
        .padding(.bottom, GymBroSpacing.xl)
        .background(
            GymBroColors.surfacePrimary
                .ignoresSafeArea(edges: .bottom)
        )
    }

    // MARK: - Helpers

    private func adjustWeight(_ delta: Double) {
        viewModel.updateWeight(max(0, viewModel.currentWeight + delta))
        HapticFeedbackService.shared.valueChanged()
    }

    private func formatWeight(_ kg: Double) -> String {
        let weight = unitSystem == .metric ? kg : kg * 2.20462
        let unit = unitSystem == .metric ? "kg" : "lb"
        return String(format: "%.1f %@", weight, unit)
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60
        let seconds = Int(duration) % 60

        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }

    private func finishWorkout() {
        workoutSummary = viewModel.finishWorkout()
        showingSummary = true
    }
}

// MARK: - Inline Rest Timer

struct InlineRestTimerView: View {
    let endTime: Date
    let onSkip: () -> Void

    @ScaledMetric(relativeTo: .title3) private var timerIconSize: CGFloat = 20
    @ScaledMetric(relativeTo: .body) private var timerTextSize: CGFloat = 18

    @State private var remainingTime: TimeInterval = 0
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {
        HStack {
            Image(systemName: "timer")
                .font(.system(size: timerIconSize))
                .foregroundStyle(GymBroColors.accentCyan)

            Text("Rest: \(formatTime(remainingTime))")
                .font(GymBroTypography.monoNumber(size: timerTextSize, weight: .semibold))
                .foregroundStyle(GymBroColors.textPrimary)

            Spacer()

            Button("Skip") {
                onSkip()
            }
            .font(GymBroTypography.subheadline)
            .foregroundStyle(GymBroColors.accentCyan)
        }
        .padding(.horizontal, GymBroSpacing.md + GymBroSpacing.xs)
        .padding(.vertical, GymBroSpacing.md)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .fill(GymBroColors.accentCyan.opacity(0.1))
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .strokeBorder(GymBroColors.accentCyan.opacity(0.3), lineWidth: 1)
        )
        .onReceive(timer) { _ in
            remainingTime = max(0, endTime.timeIntervalSinceNow)
            if remainingTime == 0 {
                HapticFeedbackService.shared.mediumImpact()
            }
        }
        .onAppear {
            remainingTime = endTime.timeIntervalSinceNow
        }
    }

    private func formatTime(_ seconds: TimeInterval) -> String {
        let mins = Int(seconds) / 60
        let secs = Int(seconds) % 60
        return String(format: "%d:%02d", mins, secs)
    }
}
