import SwiftUI
import GymBroCore

public struct ExerciseQuickInfoSheet: View {
    let exercise: Exercise
    @Environment(\.dismiss) private var dismiss
    @ScaledMetric(relativeTo: .title2) private var titleSize: CGFloat = 22
    
    public init(exercise: Exercise) {
        self.exercise = exercise
    }
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                    headerSection
                    
                    if !exercise.instructions.isEmpty {
                        ExerciseInstructionSection(instructions: exercise.instructions)
                    } else {
                        emptyState
                    }
                }
                .padding(GymBroSpacing.md)
            }
            .gymBroDarkBackground()
            .navigationTitle(exercise.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.title3)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
    }
    
    private var headerSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            HStack(spacing: GymBroSpacing.sm) {
                categoryBadge
                
                if !exercise.muscleGroups.isEmpty {
                    Text("•")
                        .foregroundStyle(GymBroColors.textTertiary)
                    Text(primaryMuscles)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                }
                
                Spacer()
            }
        }
    }
    
    private var categoryBadge: some View {
        Text(exercise.category.rawValue.uppercased())
            .font(.system(size: 12, weight: .bold))
            .foregroundStyle(categoryColor)
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs - 2)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm - 2)
                    .fill(categoryColor.opacity(0.15))
            )
    }
    
    private var categoryColor: Color {
        switch exercise.category {
        case .compound:
            return GymBroColors.accentGreen
        case .isolation:
            return GymBroColors.accentAmber
        case .accessory:
            return GymBroColors.accentCyan
        case .cardio:
            return GymBroColors.accentRed
        }
    }
    
    private var primaryMuscles: String {
        let primary = exercise.muscleGroups.filter { $0.isPrimary }
        if primary.isEmpty {
            return exercise.muscleGroups.first?.name ?? ""
        }
        return primary.map { $0.name }.joined(separator: ", ")
    }
    
    private var emptyState: some View {
        GymBroCard {
            VStack(spacing: GymBroSpacing.sm) {
                Image(systemName: "doc.text")
                    .font(.system(size: 40))
                    .foregroundStyle(GymBroColors.textTertiary)
                Text("No Instructions Available")
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text("This exercise doesn't have detailed instructions yet.")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.lg)
        }
    }
}

// MARK: - Preview

#Preview("Quick Info Sheet") {
    Color.clear
        .sheet(isPresented: .constant(true)) {
            ExerciseQuickInfoSheet(
                exercise: Exercise(
                    name: "Bench Press",
                    category: .compound,
                    equipment: .barbell,
                    instructions: """
                    SETUP
                    • Lie flat on bench, eyes under barbell
                    • Feet flat on floor, arch in lower back
                    • Grip slightly wider than shoulder-width
                    
                    EXECUTION
                    • Unrack bar, hold over chest
                    • Inhale, lower to mid-chest with control
                    • Touch chest lightly, drive bar up
                    • Exhale through sticking point
                    
                    COMMON MISTAKES
                    • Bouncing bar off chest
                    • Flared elbows (>45° angle)
                    • Lifting hips off bench
                    
                    SAFETY
                    ⚠️ Always use spotter for heavy sets
                    ⚠️ Use safety arms if training alone
                    """,
                    muscleGroups: [
                        MuscleGroup(name: "Chest", isPrimary: true),
                        MuscleGroup(name: "Triceps", isPrimary: false),
                        MuscleGroup(name: "Shoulders", isPrimary: false)
                    ]
                )
            )
        }
}
