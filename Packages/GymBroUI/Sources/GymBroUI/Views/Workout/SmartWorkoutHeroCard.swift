import SwiftUI
import SwiftData
import GymBroCore

/// "Start Smart Workout" hero card — the first thing users see on the Workout tab.
///
/// Shows a preview of today's AI-generated workout based on active program + recovery.
/// One tap → WorkoutPreviewSheet → confirm → ActiveWorkoutView.
public struct SmartWorkoutHeroCard: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var generatorVM: WorkoutGeneratorViewModel?
    @State private var showPreviewSheet = false
    @State private var activeProgram: Program?
    @State private var readinessScore: Double?
    @State private var todayDayName: String?
    @State private var isPressed = false

    @ScaledMetric private var sparkIconSize: CGFloat = 24
    @ScaledMetric private var chipIconSize: CGFloat = 12

    /// Called when user confirms the workout in the preview sheet.
    let onStartWorkout: (ActiveWorkoutViewModel) -> Void

    public init(onStartWorkout: @escaping (ActiveWorkoutViewModel) -> Void) {
        self.onStartWorkout = onStartWorkout
    }

    public var body: some View {
        Button {
            openPreview()
        } label: {
            cardContent
        }
        .buttonStyle(.plain)
        .scaleEffect(isPressed ? 0.97 : 1.0)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .sheet(isPresented: $showPreviewSheet) {
            if let vm = generatorVM {
                WorkoutPreviewSheet(generatorVM: vm, onStartWorkout: onStartWorkout)
                    .presentationDetents([.large])
            }
        }
        .task {
            loadContext()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Start smart workout. \(summaryText)")
        .accessibilityAddTraits(.isButton)
    }

    // MARK: - Card Content

    private var cardContent: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            // Top row: icon + label
            HStack(spacing: GymBroSpacing.sm) {
                Image(systemName: "wand.and.stars")
                    .font(.system(size: sparkIconSize, weight: .semibold))
                    .foregroundStyle(GymBroColors.accentGreen)

                Text("SMART WORKOUT")
                    .font(GymBroTypography.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(GymBroColors.accentGreen)
                    .tracking(2)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(GymBroColors.accentGreen.opacity(0.7))
            }

            // Session name
            Text(sessionTitle)
                .font(GymBroTypography.title3)
                .foregroundStyle(GymBroColors.textPrimary)

            // Context chips
            HStack(spacing: GymBroSpacing.md) {
                if let dayName = todayDayName {
                    contextChip(icon: "calendar", text: dayName)
                }

                if let readiness = readinessScore {
                    contextChip(
                        icon: "heart.fill",
                        text: "Readiness \(Int(readiness))",
                        color: colorForReadiness(readiness)
                    )
                }

                contextChip(icon: "clock", text: "~45 min")
            }

            // Summary text
            Text(summaryText)
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)
                .lineLimit(2)
        }
        .padding(GymBroSpacing.md + GymBroSpacing.xs)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.xl)
                .fill(GymBroColors.accentGreen.opacity(0.06))
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.xl)
                .strokeBorder(
                    LinearGradient(
                        colors: [GymBroColors.accentGreen.opacity(0.4), GymBroColors.accentGreen.opacity(0.1)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 1.5
                )
        )
        .gymBroElevatedShadow()
    }

    // MARK: - Context Chip

    private func contextChip(icon: String, text: String, color: Color = GymBroColors.textTertiary) -> some View {
        HStack(spacing: GymBroSpacing.xs) {
            Image(systemName: icon)
                .font(.system(size: chipIconSize))
            Text(text)
                .font(GymBroTypography.caption2)
        }
        .foregroundStyle(color)
    }

    // MARK: - Data

    private var sessionTitle: String {
        if let program = activeProgram, let day = program.todaysProgramDay {
            return day.name
        }
        return "Today's Workout"
    }

    private var summaryText: String {
        if let program = activeProgram {
            if let day = program.todaysProgramDay {
                let exerciseNames = day.plannedExercises
                    .sorted { $0.order < $1.order }
                    .prefix(3)
                    .compactMap { $0.exercise?.name }
                let preview = exerciseNames.joined(separator: ", ")
                let suffix = day.plannedExercises.count > 3 ? " +\(day.plannedExercises.count - 3) more" : ""
                return "\(preview)\(suffix) — based on recovery"
            }
            return "Generating from \(program.name) — based on recovery"
        }
        return "AI-generated full body session — adapts to your recovery"
    }

    private func loadContext() {
        let programDescriptor = FetchDescriptor<Program>(
            predicate: #Predicate<Program> { $0.isActive }
        )
        activeProgram = try? modelContext.fetch(programDescriptor).first
        todayDayName = activeProgram?.todaysProgramDay?.name

        let readinessDescriptor = FetchDescriptor<ReadinessScore>(
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        readinessScore = (try? modelContext.fetch(readinessDescriptor).first)?.overallScore
    }

    private func openPreview() {
        let vm = WorkoutGeneratorViewModel(modelContext: modelContext)
        generatorVM = vm
        showPreviewSheet = true
    }

    private func colorForReadiness(_ score: Double) -> Color {
        switch score {
        case 80...100: return GymBroColors.accentGreen
        case 60..<80: return GymBroColors.accentCyan
        case 40..<60: return GymBroColors.accentAmber
        default: return GymBroColors.accentRed
        }
    }
}

// MARK: - Preview

#Preview("Smart Workout Hero Card") {
    VStack {
        SmartWorkoutHeroCard { _ in }
            .padding(.horizontal, GymBroSpacing.md)
    }
    .frame(maxHeight: .infinity)
    .gymBroDarkBackground()
}
