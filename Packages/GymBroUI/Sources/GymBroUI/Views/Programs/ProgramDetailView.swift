import SwiftUI
import SwiftData
import GymBroCore

public struct ProgramDetailView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    let program: Program
    let isActive: Bool

    @State private var viewModel: ProgramsTabViewModel?
    @State private var showStartConfirmation = false
    @State private var showStopConfirmation = false
    @State private var selectedWeek: Int = 1
    @ScaledMetric private var iconSize: CGFloat = 20

    public init(program: Program, isActive: Bool = false) {
        self.program = program
        self.isActive = isActive
    }

    public var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                headerSection
                infoCardsSection
                progressionSection
                weekSelector
                weekBreakdown
                actionButton
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
                viewModel = ProgramsTabViewModel(modelContext: modelContext)
            }
        }
        .confirmationDialog(
            "Start \(program.name)?",
            isPresented: $showStartConfirmation,
            titleVisibility: .visible
        ) {
            Button("Start Program") {
                viewModel?.startProgram(program)
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will replace your current active program if you have one.")
        }
        .confirmationDialog(
            "Stop \(program.name)?",
            isPresented: $showStopConfirmation,
            titleVisibility: .visible
        ) {
            Button("Stop Program", role: .destructive) {
                viewModel?.stopProgram()
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Your progress will be saved but the program will no longer be active.")
        }
    }

    // MARK: - Header

    @ViewBuilder
    private var headerSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            HStack(spacing: GymBroSpacing.sm) {
                periodizationBadge(program.periodizationType)

                Text("\(program.durationWeeks) weeks")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)

                Text("•")
                    .foregroundStyle(GymBroColors.textTertiary)

                Text("\(program.frequencyPerWeek)x/week")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)

                if isActive {
                    Spacer()
                    Label("Active", systemImage: "checkmark.circle.fill")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.accentGreen)
                }
            }

            Text(program.programDescription)
                .font(GymBroTypography.body)
                .foregroundStyle(GymBroColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    // MARK: - Info Cards

    @ViewBuilder
    private var infoCardsSection: some View {
        VStack(spacing: GymBroSpacing.md) {
            if !program.targetAudience.isEmpty {
                infoCard(
                    icon: "person.fill",
                    title: "Who It's For",
                    content: program.targetAudience,
                    accentColor: GymBroColors.accentCyan
                )
            }

            if !program.expectedOutcome.isEmpty {
                infoCard(
                    icon: "target",
                    title: "Expected Outcome",
                    content: program.expectedOutcome,
                    accentColor: GymBroColors.accentGreen
                )
            }
        }
    }

    @ViewBuilder
    private func infoCard(icon: String, title: String, content: String, accentColor: Color) -> some View {
        GymBroCard(accent: accentColor) {
            HStack(alignment: .top, spacing: GymBroSpacing.md) {
                Image(systemName: icon)
                    .font(.system(size: iconSize))
                    .foregroundStyle(accentColor)
                    .frame(width: 28)

                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    Text(title.uppercased())
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .tracking(1.5)

                    Text(content)
                        .font(GymBroTypography.body)
                        .foregroundStyle(GymBroColors.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
        }
    }

    // MARK: - Progression Scheme

    @ViewBuilder
    private var progressionSection: some View {
        if !program.progressionScheme.isEmpty {
            VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                Text("PROGRESSION SCHEME")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(2)

                GymBroCard(accent: GymBroColors.accentAmber) {
                    HStack(alignment: .top, spacing: GymBroSpacing.md) {
                        Image(systemName: "chart.line.uptrend.xyaxis")
                            .font(.system(size: iconSize))
                            .foregroundStyle(GymBroColors.accentAmber)
                            .frame(width: 28)

                        Text(program.progressionScheme)
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
    }

    // MARK: - Week Selector (Swipeable)

    @ViewBuilder
    private var weekSelector: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            HStack {
                Text("WEEKLY PLAN")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(2)

                Spacer()

                Text("Week \(selectedWeek) of \(program.durationWeeks)")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.accentGreen)
            }

            HStack(spacing: GymBroSpacing.md) {
                Button {
                    withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                        selectedWeek = max(1, selectedWeek - 1)
                    }
                } label: {
                    Image(systemName: "chevron.left")
                        .font(.headline)
                        .foregroundStyle(selectedWeek > 1 ? GymBroColors.accentGreen : GymBroColors.textTertiary)
                        .frame(width: 44, height: 44)
                        .background(GymBroColors.surfaceSecondary)
                        .clipShape(Circle())
                }
                .disabled(selectedWeek <= 1)

                Spacer()

                HStack(spacing: GymBroSpacing.xs) {
                    ForEach(1...program.durationWeeks, id: \.self) { week in
                        Circle()
                            .fill(week == selectedWeek ? GymBroColors.accentGreen : GymBroColors.surfaceElevated)
                            .frame(width: week == selectedWeek ? 8 : 6, height: week == selectedWeek ? 8 : 6)
                            .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: selectedWeek)
                    }
                }

                Spacer()

                Button {
                    withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                        selectedWeek = min(program.durationWeeks, selectedWeek + 1)
                    }
                } label: {
                    Image(systemName: "chevron.right")
                        .font(.headline)
                        .foregroundStyle(selectedWeek < program.durationWeeks ? GymBroColors.accentGreen : GymBroColors.textTertiary)
                        .frame(width: 44, height: 44)
                        .background(GymBroColors.surfaceSecondary)
                        .clipShape(Circle())
                }
                .disabled(selectedWeek >= program.durationWeeks)
            }
        }
    }

    // MARK: - Week Breakdown

    @ViewBuilder
    private var weekBreakdown: some View {
        let sortedDays = program.days.sorted { $0.dayNumber < $1.dayNumber }

        VStack(spacing: GymBroSpacing.md) {
            ForEach(sortedDays, id: \.id) { day in
                dayCard(day: day, weekNumber: selectedWeek)
            }
        }
        .id(selectedWeek)
        .transition(.asymmetric(
            insertion: .move(edge: .trailing).combined(with: .opacity),
            removal: .move(edge: .leading).combined(with: .opacity)
        ))
        .gesture(
            DragGesture(minimumDistance: 50)
                .onEnded { value in
                    if value.translation.width < -50 && selectedWeek < program.durationWeeks {
                        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                            selectedWeek += 1
                        }
                    } else if value.translation.width > 50 && selectedWeek > 1 {
                        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.3)) {
                            selectedWeek -= 1
                        }
                    }
                }
        )
    }

    @ViewBuilder
    private func dayCard(day: ProgramDay, weekNumber: Int) -> some View {
        let weekExercises = exercisesForWeek(day: day, weekNumber: weekNumber)

        GymBroCard {
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                HStack {
                    Text(day.name)
                        .font(GymBroTypography.headline)
                        .foregroundStyle(GymBroColors.textPrimary)

                    Spacer()

                    Text("\(weekExercises.count) exercises")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }

                if !day.dayDescription.isEmpty {
                    Text(day.dayDescription)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                }

                if !weekExercises.isEmpty {
                    Divider()
                        .background(GymBroColors.border)

                    ForEach(weekExercises, id: \.id) { planned in
                        exerciseRow(planned: planned)
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func exerciseRow(planned: PlannedExercise) -> some View {
        HStack(spacing: GymBroSpacing.md) {
            Text("\(planned.order)")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .frame(width: 16)

            VStack(alignment: .leading, spacing: 2) {
                Text(planned.exercise?.name ?? "Unknown")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textPrimary)

                HStack(spacing: GymBroSpacing.sm) {
                    Text("\(planned.targetSets)×\(planned.targetReps)")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.accentCyan)

                    if let rpe = planned.targetRPE {
                        Text("RPE \(String(format: "%.1f", rpe))")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.accentAmber)
                    }
                }

                if !planned.notes.isEmpty {
                    Text(planned.notes)
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .lineLimit(2)
                }
            }

            Spacer()
        }
        .padding(.vertical, GymBroSpacing.xs)
    }

    // MARK: - Action Button

    @ViewBuilder
    private var actionButton: some View {
        if isActive {
            Button("Stop Program") {
                showStopConfirmation = true
            }
            .buttonStyle(.gymBroDestructive)
        } else {
            Button("Start This Program") {
                showStartConfirmation = true
            }
            .buttonStyle(.gymBroPrimary)
        }
    }

    // MARK: - Helpers

    private func exercisesForWeek(day: ProgramDay, weekNumber: Int) -> [PlannedExercise] {
        if let week = day.weeks.first(where: { $0.weekNumber == weekNumber }) {
            return week.plannedExercises.sorted { $0.order < $1.order }
        }
        return day.plannedExercises.sorted { $0.order < $1.order }
    }

    @ViewBuilder
    private func periodizationBadge(_ type: PeriodizationType) -> some View {
        Text(type.displayName.uppercased())
            .font(.system(size: 9, weight: .bold, design: .rounded))
            .tracking(1)
            .foregroundStyle(badgeColor(for: type))
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                Capsule()
                    .fill(badgeColor(for: type).opacity(0.15))
            )
    }

    private func badgeColor(for type: PeriodizationType) -> Color {
        switch type {
        case .linear: return GymBroColors.accentCyan
        case .undulating: return GymBroColors.accentAmber
        case .block: return GymBroColors.accentGreen
        case .autoregulated: return Color(hex: 0xBB86FC)
        }
    }
}

// MARK: - Preview

#Preview("Program Detail") {
    NavigationStack {
        ProgramDetailView(
            program: Program(
                name: "5/3/1 Wendler",
                programDescription: "Classic 4-week strength program with main lifts at prescribed percentages.",
                durationWeeks: 4,
                frequencyPerWeek: 4,
                periodizationType: .block,
                targetAudience: "Intermediate to Advanced (2+ years)",
                expectedOutcome: "5-10lb monthly increases on main lifts",
                progressionScheme: "Week 1: 5×65/75/85%, Week 2: 3×70/80/90%, Week 3: 5/3/1"
            )
        )
    }
}
