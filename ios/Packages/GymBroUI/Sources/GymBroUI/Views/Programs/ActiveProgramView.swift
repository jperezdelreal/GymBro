import SwiftUI
import SwiftData
import GymBroCore

public struct ActiveProgramView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let program: Program

    @State private var viewModel: ProgramsTabViewModel?
    @State private var showWorkoutPreview = false
    @State private var generatorVM: WorkoutGeneratorViewModel?
    @State private var activeWorkoutVM: ActiveWorkoutViewModel?
    @State private var navigateToWorkout = false
    @ScaledMetric private var statSize: CGFloat = 36

    public init(program: Program) {
        self.program = program
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                progressOverview
                complianceSection
                todaysWorkoutSection
                weekSchedule
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.top, GymBroSpacing.sm)
            .padding(.bottom, GymBroSpacing.xxl)
        }
        .gymBroDarkBackground()
        .navigationTitle(program.name)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if viewModel == nil {
                let vm = ProgramsTabViewModel(modelContext: modelContext)
                vm.loadPrograms()
                viewModel = vm
            }
        }
        .sheet(isPresented: $showWorkoutPreview) {
            if let vm = generatorVM {
                WorkoutPreviewSheet(generatorVM: vm) { workoutVM in
                    activeWorkoutVM = workoutVM
                    navigateToWorkout = true
                }
                .presentationDetents([.large])
            }
        }
        .navigationDestination(isPresented: $navigateToWorkout) {
            if let vm = activeWorkoutVM {
                ActiveWorkoutView(viewModel: vm)
            }
        }
    }

    // MARK: - Progress Overview

    @ViewBuilder
    private var progressOverview: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("PROGRESS")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            GymBroCard(accent: GymBroColors.accentGreen) {
                VStack(spacing: GymBroSpacing.lg) {
                    HStack {
                        statBlock(
                            value: "\(program.currentWeekNumber)",
                            label: "Current Week",
                            color: GymBroColors.accentGreen
                        )

                        Spacer()

                        statBlock(
                            value: "\(program.durationWeeks)",
                            label: "Total Weeks",
                            color: GymBroColors.textSecondary
                        )

                        Spacer()

                        statBlock(
                            value: "\(completedWorkouts)",
                            label: "Workouts",
                            color: GymBroColors.accentCyan
                        )
                    }

                    weekProgressIndicator
                }
            }
        }
    }

    @ViewBuilder
    private func statBlock(value: String, label: String, color: Color) -> some View {
        VStack(spacing: GymBroSpacing.xs) {
            Text(value)
                .font(GymBroTypography.monoNumber(size: statSize))
                .foregroundStyle(color)

            Text(label.uppercased())
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1)
        }
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var weekProgressIndicator: some View {
        HStack(spacing: GymBroSpacing.xs) {
            ForEach(1...program.durationWeeks, id: \.self) { week in
                RoundedRectangle(cornerRadius: 3)
                    .fill(weekColor(week))
                    .frame(height: 8)
            }
        }
        .accessibilityLabel("Week \(program.currentWeekNumber) of \(program.durationWeeks)")
    }

    // MARK: - Compliance Section

    @ViewBuilder
    private var complianceSection: some View {
        let compliance = viewModel?.compliancePercentage ?? 0

        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("COMPLIANCE")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            GymBroCard {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text(complianceLabel(compliance))
                            .font(GymBroTypography.headline)
                            .foregroundStyle(complianceColor(compliance))

                        Text("Based on \(completedWorkouts) workout\(completedWorkouts == 1 ? "" : "s")")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.textTertiary)
                    }

                    Spacer()

                    HeroNumber(
                        value: compliance,
                        format: "%.0f",
                        unit: "%",
                        trend: complianceTrend(compliance)
                    )
                }
            }
        }
    }

    // MARK: - Today's Workout

    @ViewBuilder
    private var todaysWorkoutSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("TODAY'S WORKOUT")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            if let today = program.todaysProgramDay {
                todayCard(day: today)

                Button {
                    let vm = WorkoutGeneratorViewModel(modelContext: modelContext)
                    generatorVM = vm
                    showWorkoutPreview = true
                } label: {
                    HStack(spacing: GymBroSpacing.sm) {
                        Image(systemName: "wand.and.stars")
                        Text("Start Smart Workout")
                    }
                }
                .buttonStyle(.gymBroPrimary)
            } else {
                GymBroCard {
                    HStack(spacing: GymBroSpacing.md) {
                        Image(systemName: "moon.zzz.fill")
                            .font(.title2)
                            .foregroundStyle(GymBroColors.textTertiary)

                        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                            Text("Rest Day")
                                .font(GymBroTypography.headline)
                                .foregroundStyle(GymBroColors.textPrimary)

                            Text("Recovery is part of the program")
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func todayCard(day: ProgramDay) -> some View {
        let weekNumber = program.currentWeekNumber
        let exercises = exercisesForWeek(day: day, weekNumber: weekNumber)

        GymBroCard(accent: GymBroColors.accentGreen) {
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text(day.name)
                            .font(GymBroTypography.title3)
                            .foregroundStyle(GymBroColors.textPrimary)

                        if !day.dayDescription.isEmpty {
                            Text(day.dayDescription)
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textSecondary)
                        }
                    }

                    Spacer()

                    Text("\(exercises.count)")
                        .font(GymBroTypography.monoNumber(size: 28))
                        .foregroundStyle(GymBroColors.accentGreen)
                    +
                    Text(" ex")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }

                if !exercises.isEmpty {
                    Divider()
                        .background(GymBroColors.border)

                    ForEach(exercises, id: \.id) { planned in
                        HStack(spacing: GymBroSpacing.md) {
                            Circle()
                                .fill(GymBroColors.surfaceElevated)
                                .frame(width: 24, height: 24)
                                .overlay(
                                    Text("\(planned.order)")
                                        .font(GymBroTypography.caption2)
                                        .foregroundStyle(GymBroColors.textSecondary)
                                )

                            VStack(alignment: .leading, spacing: 2) {
                                Text(planned.exercise?.name ?? "Unknown")
                                    .font(GymBroTypography.subheadline)
                                    .foregroundStyle(GymBroColors.textPrimary)

                                HStack(spacing: GymBroSpacing.sm) {
                                    Text("\(planned.targetSets)×\(planned.targetReps)")
                                        .font(GymBroTypography.caption)
                                        .foregroundStyle(GymBroColors.accentCyan)

                                    if let rpe = planned.targetRPE {
                                        Text("RPE \(String(format: "%.0f", rpe))")
                                            .font(GymBroTypography.caption)
                                            .foregroundStyle(GymBroColors.accentAmber)
                                    }
                                }
                            }

                            Spacer()
                        }
                        .padding(.vertical, GymBroSpacing.xs)
                    }
                }
            }
        }
    }

    // MARK: - Week Schedule

    @ViewBuilder
    private var weekSchedule: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("THIS WEEK'S SCHEDULE")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            let sortedDays = program.days.sorted { $0.dayNumber < $1.dayNumber }
            ForEach(sortedDays, id: \.id) { day in
                GymBroCard {
                    HStack {
                        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                            Text(day.name)
                                .font(GymBroTypography.headline)
                                .foregroundStyle(GymBroColors.textPrimary)

                            let exercises = exercisesForWeek(day: day, weekNumber: program.currentWeekNumber)
                            Text("\(exercises.count) exercises")
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textSecondary)
                        }

                        Spacer()

                        let dayCompleted = isDayCompleted(day)
                        Image(systemName: dayCompleted ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle(dayCompleted ? GymBroColors.accentGreen : GymBroColors.surfaceElevated)
                            .font(.title3)
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private var completedWorkouts: Int {
        program.workouts.filter { $0.endTime != nil && !$0.isCancelled }.count
    }

    private func weekColor(_ week: Int) -> Color {
        if week < program.currentWeekNumber {
            return GymBroColors.accentGreen
        } else if week == program.currentWeekNumber {
            return GymBroColors.accentGreen.opacity(0.5)
        } else {
            return GymBroColors.surfaceElevated
        }
    }

    private func complianceLabel(_ percentage: Double) -> String {
        switch percentage {
        case 90...: return "Excellent"
        case 75..<90: return "Good"
        case 60..<75: return "Moderate"
        case 1..<60: return "Needs Work"
        default: return "No Data"
        }
    }

    private func complianceColor(_ percentage: Double) -> Color {
        switch percentage {
        case 90...: return GymBroColors.accentGreen
        case 75..<90: return GymBroColors.accentCyan
        case 60..<75: return GymBroColors.accentAmber
        case 1..<60: return GymBroColors.accentRed
        default: return GymBroColors.textTertiary
        }
    }

    private func complianceTrend(_ percentage: Double) -> HeroTrend? {
        guard percentage > 0 else { return nil }
        switch percentage {
        case 75...: return .up
        case 60..<75: return .flat
        default: return .down
        }
    }

    private func exercisesForWeek(day: ProgramDay, weekNumber: Int) -> [PlannedExercise] {
        if let week = day.weeks.first(where: { $0.weekNumber == weekNumber }) {
            return week.plannedExercises.sorted { $0.order < $1.order }
        }
        return day.plannedExercises.sorted { $0.order < $1.order }
    }

    private func isDayCompleted(_ day: ProgramDay) -> Bool {
        guard let startDate = program.startDate else { return false }
        let currentWeek = program.currentWeekNumber
        let weekStart = Calendar.current.date(byAdding: .weekOfYear, value: currentWeek - 1, to: startDate) ?? startDate

        return day.workouts.contains { workout in
            guard let endTime = workout.endTime, !workout.isCancelled else { return false }
            return endTime >= weekStart
        }
    }
}

// MARK: - Preview

#Preview("Active Program") {
    NavigationStack {
        ActiveProgramView(
            program: {
                let p = Program(
                    name: "5/3/1 Wendler",
                    programDescription: "Classic strength program",
                    durationWeeks: 4,
                    frequencyPerWeek: 4,
                    periodizationType: .block,
                    isActive: true,
                    startDate: Calendar.current.date(byAdding: .day, value: -10, to: Date())
                )
                return p
            }()
        )
    }
}
