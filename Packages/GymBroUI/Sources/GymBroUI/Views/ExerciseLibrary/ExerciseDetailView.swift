import SwiftUI
import GymBroCore

public struct ExerciseDetailView: View {
    let exercise: Exercise
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .title) private var titleSize: CGFloat = 28
    @ScaledMetric(relativeTo: .headline) private var badgeSize: CGFloat = 14
    
    public init(exercise: Exercise) {
        self.exercise = exercise
    }
    
    public var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                headerSection
                
                if !exercise.muscleGroups.isEmpty {
                    muscleGroupsSection
                }
                
                equipmentSection
                
                if !exercise.instructions.isEmpty {
                    ExerciseInstructionSection(instructions: exercise.instructions)
                }
                
                addToWorkoutButton
            }
            .padding(GymBroSpacing.md)
        }
        .gymBroDarkBackground()
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private var headerSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            HStack(spacing: GymBroSpacing.sm) {
                categoryBadge
                if exercise.isCustom {
                    customBadge
                }
                Spacer()
            }
            
            Text(exercise.name)
                .font(.system(size: titleSize, weight: .bold))
                .foregroundStyle(GymBroColors.textPrimary)
                .accessibilityAddTraits(.isHeader)
        }
    }
    
    private var categoryBadge: some View {
        Text(exercise.category.rawValue.uppercased())
            .font(.system(size: badgeSize, weight: .bold))
            .foregroundStyle(categoryColor)
            .padding(.horizontal, GymBroSpacing.sm + 2)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(categoryColor.opacity(0.15))
            )
            .accessibilityLabel("Category: \(exercise.category.rawValue)")
    }
    
    private var customBadge: some View {
        Text("CUSTOM")
            .font(.system(size: badgeSize, weight: .bold))
            .foregroundStyle(GymBroColors.accentCyan)
            .padding(.horizontal, GymBroSpacing.sm + 2)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(GymBroColors.accentCyan.opacity(0.15))
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
        }
    }
    
    private var muscleGroupsSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("MUSCLE GROUPS")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.2)
            
            FlowLayout(spacing: GymBroSpacing.sm) {
                ForEach(exercise.muscleGroups, id: \.id) { muscle in
                    MuscleGroupTag(muscle: muscle)
                }
            }
        }
    }
    
    private var equipmentSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text("EQUIPMENT")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.2)
            
            HStack(spacing: GymBroSpacing.sm) {
                Image(systemName: equipmentIcon)
                    .foregroundStyle(GymBroColors.accentGreen)
                Text(exercise.equipment.rawValue.capitalized)
                    .font(GymBroTypography.body)
                    .foregroundStyle(GymBroColors.textPrimary)
            }
        }
    }
    
    private var equipmentIcon: String {
        switch exercise.equipment {
        case .barbell:
            return "figure.strengthtraining.traditional"
        case .dumbbell:
            return "dumbbell.fill"
        case .kettlebell:
            return "figure.strengthtraining.functional"
        case .machine:
            return "gearshape.2.fill"
        case .cable:
            return "cable.connector"
        case .bodyweight:
            return "figure.arms.open"
        case .band:
            return "bandage.fill"
        case .other:
            return "figure.mixed.cardio"
        }
    }
    
    private var addToWorkoutButton: some View {
        Button {
            // TODO: Implement add to workout
        } label: {
            HStack {
                Image(systemName: "plus.circle.fill")
                Text("Add to Workout")
                    .font(GymBroTypography.headline)
            }
            .foregroundStyle(GymBroColors.background)
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.md)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .fill(GymBroColors.greenGradient)
            )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Supporting Views

struct MuscleGroupTag: View {
    let muscle: MuscleGroup
    @ScaledMetric(relativeTo: .caption) private var fontSize: CGFloat = 13
    
    var body: some View {
        HStack(spacing: 4) {
            if muscle.isPrimary {
                Circle()
                    .fill(GymBroColors.accentGreen)
                    .frame(width: 6, height: 6)
            }
            Text(muscle.name)
                .font(.system(size: fontSize, weight: muscle.isPrimary ? .semibold : .regular))
        }
        .foregroundStyle(muscle.isPrimary ? GymBroColors.textPrimary : GymBroColors.textSecondary)
        .padding(.horizontal, GymBroSpacing.sm + 2)
        .padding(.vertical, GymBroSpacing.xs)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.sm)
                .fill(muscle.isPrimary 
                    ? GymBroColors.accentGreen.opacity(0.15)
                    : GymBroColors.surfaceElevated)
        )
        .accessibilityLabel("\(muscle.isPrimary ? "Primary" : "Secondary") muscle: \(muscle.name)")
    }
}

struct FlowLayout: Layout {
    var spacing: CGFloat
    
    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }
    
    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            subview.place(at: result.positions[index], proposal: .unspecified)
        }
    }
    
    struct FlowResult {
        var size: CGSize
        var positions: [CGPoint]
        
        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var positions: [CGPoint] = []
            var size: CGSize = .zero
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0
            
            for subview in subviews {
                let subviewSize = subview.sizeThatFits(.unspecified)
                
                if currentX + subviewSize.width > maxWidth && currentX > 0 {
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }
                
                positions.append(CGPoint(x: currentX, y: currentY))
                currentX += subviewSize.width + spacing
                lineHeight = max(lineHeight, subviewSize.height)
                size.width = max(size.width, currentX - spacing)
            }
            
            size.height = currentY + lineHeight
            self.size = size
            self.positions = positions
        }
    }
}

// MARK: - Preview

#Preview("Exercise Detail") {
    NavigationStack {
        ExerciseDetailView(
            exercise: Exercise(
                name: "Barbell Back Squat",
                category: .compound,
                equipment: .barbell,
                instructions: """
                SETUP
                • Position barbell on upper traps (high bar) or rear delts (low bar)
                • Feet shoulder-width, toes slightly out (15-30°)
                • Engage core, chest up, eyes neutral
                
                EXECUTION
                • Inhale at top, brace core
                • Break at hips and knees simultaneously
                • Descend until thighs parallel or below (full ROM)
                • Drive through midfoot, exhale on ascent
                • Maintain neutral spine throughout
                
                COMMON MISTAKES
                • Knees caving inward (valgus collapse)
                • Excessive forward lean
                • Heels lifting off ground
                • Losing core tension at bottom
                
                SAFETY
                ⚠️ Use safety bars or squat in a power rack
                ⚠️ Bail properly if failing — drop bar behind you, step forward
                ⚠️ Advanced exercise — ensure proper ankle/hip mobility before loading heavy
                """,
                muscleGroups: [
                    MuscleGroup(name: "Quadriceps", isPrimary: true),
                    MuscleGroup(name: "Glutes", isPrimary: true),
                    MuscleGroup(name: "Hamstrings", isPrimary: false),
                    MuscleGroup(name: "Core", isPrimary: false)
                ],
                isCustom: false
            )
        )
        .navigationTitle("Exercise Detail")
    }
}
