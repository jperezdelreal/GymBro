import SwiftUI
import SwiftData
import GymBroCore

public struct ProgramsTabView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var viewModel: ProgramsTabViewModel?

    public init() {}

    public var body: some View {
        NavigationStack {
            Group {
                if let viewModel {
                    programsContent(viewModel: viewModel)
                } else {
                    ProgressView()
                        .tint(GymBroColors.accentGreen)
                }
            }
            .navigationTitle("Programs")
            .gymBroDarkBackground()
            .task {
                if viewModel == nil {
                    let vm = ProgramsTabViewModel(modelContext: modelContext)
                    await vm.seedProgramsIfNeeded()
                    viewModel = vm
                }
            }
        }
    }

    @ViewBuilder
    private func programsContent(viewModel: ProgramsTabViewModel) -> some View {
        ScrollView {
            LazyVStack(spacing: GymBroSpacing.lg) {
                if let active = viewModel.activeProgram {
                    activeProgramSection(program: active, compliance: viewModel.compliancePercentage)
                }

                templateSection(programs: viewModel.programs, activeProgram: viewModel.activeProgram)
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.top, GymBroSpacing.sm)
            .padding(.bottom, GymBroSpacing.xxl)
        }
    }

    // MARK: - Active Program Section

    @ViewBuilder
    private func activeProgramSection(program: Program, compliance: Double) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("ACTIVE PROGRAM")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.accentGreen)
                .tracking(2)

            NavigationLink {
                ActiveProgramView(program: program)
            } label: {
                GymBroCard(accent: GymBroColors.accentGreen) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                        HStack {
                            VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                                Text(program.name)
                                    .font(GymBroTypography.title3)
                                    .foregroundStyle(GymBroColors.textPrimary)

                                Text("Week \(program.currentWeekNumber) of \(program.durationWeeks)")
                                    .font(GymBroTypography.caption)
                                    .foregroundStyle(GymBroColors.textSecondary)
                            }

                            Spacer()

                            complianceGauge(percentage: compliance)
                        }

                        weekProgressBar(
                            currentWeek: program.currentWeekNumber,
                            totalWeeks: program.durationWeeks
                        )

                        if let today = program.todaysProgramDay {
                            HStack(spacing: GymBroSpacing.sm) {
                                Image(systemName: "arrow.right.circle.fill")
                                    .foregroundStyle(GymBroColors.accentGreen)

                                Text("Today: \(today.name)")
                                    .font(GymBroTypography.subheadline)
                                    .foregroundStyle(GymBroColors.textPrimary)
                            }
                        }
                    }
                }
            }
            .buttonStyle(.plain)
        }
    }

    // MARK: - Template Grid

    @ViewBuilder
    private func templateSection(programs: [Program], activeProgram: Program?) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("PROGRAM TEMPLATES")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(2)

            let columns = [
                GridItem(.flexible(), spacing: GymBroSpacing.md),
                GridItem(.flexible(), spacing: GymBroSpacing.md)
            ]

            LazyVGrid(columns: columns, spacing: GymBroSpacing.md) {
                ForEach(programs, id: \.id) { program in
                    NavigationLink {
                        ProgramDetailView(program: program, isActive: program.id == activeProgram?.id)
                    } label: {
                        programCard(program: program, isActive: program.id == activeProgram?.id)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    // MARK: - Program Card

    @ViewBuilder
    private func programCard(program: Program, isActive: Bool) -> some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            HStack {
                periodizationBadge(program.periodizationType)
                Spacer()
                if isActive {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(GymBroColors.accentGreen)
                        .font(.caption)
                }
            }

            Text(program.name)
                .font(GymBroTypography.headline)
                .foregroundStyle(GymBroColors.textPrimary)
                .lineLimit(2)
                .multilineTextAlignment(.leading)

            Text(program.programDescription)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
                .lineLimit(3)
                .multilineTextAlignment(.leading)

            Spacer(minLength: GymBroSpacing.xs)

            HStack(spacing: GymBroSpacing.md) {
                Label("\(program.frequencyPerWeek)x/wk", systemImage: "calendar")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)

                Spacer()

                Text(program.difficulty)
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(difficultyColor(program.difficulty))
            }
        }
        .padding(GymBroSpacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .frame(minHeight: 180)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.lg)
                .fill(GymBroColors.surfaceSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.lg)
                .strokeBorder(
                    isActive ? GymBroColors.accentGreen.opacity(0.5) : GymBroColors.border,
                    lineWidth: isActive ? 1.5 : 1
                )
        )
        .gymBroCardShadow()
    }

    // MARK: - Helpers

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

    @ViewBuilder
    private func complianceGauge(percentage: Double) -> some View {
        VStack(spacing: 2) {
            Text(String(format: "%.0f", percentage))
                .font(GymBroTypography.monoNumber(size: 24))
                .foregroundStyle(complianceColor(percentage))

            Text("%")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textTertiary)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(Int(percentage)) percent compliance")
    }

    @ViewBuilder
    private func weekProgressBar(currentWeek: Int, totalWeeks: Int) -> some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(GymBroColors.surfaceElevated)
                    .frame(height: 6)

                RoundedRectangle(cornerRadius: 3)
                    .fill(GymBroColors.accentGreen)
                    .frame(
                        width: geometry.size.width * CGFloat(currentWeek) / CGFloat(max(totalWeeks, 1)),
                        height: 6
                    )
            }
        }
        .frame(height: 6)
        .accessibilityLabel("Week \(currentWeek) of \(totalWeeks)")
    }

    private func badgeColor(for type: PeriodizationType) -> Color {
        switch type {
        case .linear: return GymBroColors.accentCyan
        case .undulating: return GymBroColors.accentAmber
        case .block: return GymBroColors.accentGreen
        case .autoregulated: return Color(hex: 0xBB86FC)
        }
    }

    private func difficultyColor(_ difficulty: String) -> Color {
        switch difficulty {
        case "Beginner": return GymBroColors.accentGreen
        case "Intermediate": return GymBroColors.accentAmber
        case "Advanced": return GymBroColors.accentRed
        default: return GymBroColors.textTertiary
        }
    }

    private func complianceColor(_ percentage: Double) -> Color {
        switch percentage {
        case 90...: return GymBroColors.accentGreen
        case 75..<90: return GymBroColors.accentCyan
        case 60..<75: return GymBroColors.accentAmber
        default: return GymBroColors.accentRed
        }
    }
}

// MARK: - Preview

#Preview("Programs Tab") {
    ProgramsTabView()
        .modelContainer(
            for: [Program.self, ProgramDay.self, ProgramWeek.self, PlannedExercise.self, Exercise.self, Workout.self, ExerciseSet.self],
            inMemory: true
        )
}
