import SwiftUI
import GymBroCore

struct ExerciseLibraryRow: View {
    let exercise: Exercise
    @ScaledMetric(relativeTo: .body) private var titleSize: CGFloat = 17
    @ScaledMetric(relativeTo: .caption) private var captionSize: CGFloat = 13
    
    var body: some View {
        HStack(spacing: GymBroSpacing.md) {
            VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                HStack(spacing: GymBroSpacing.sm) {
                    Text(exercise.name)
                        .font(.system(size: titleSize, weight: .semibold))
                        .foregroundStyle(GymBroColors.textPrimary)
                    
                    if exercise.isCustom {
                        Text("CUSTOM")
                            .font(.system(size: 9, weight: .bold))
                            .foregroundStyle(GymBroColors.accentCyan)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(GymBroColors.accentCyan.opacity(0.15))
                            )
                    }
                }
                
                HStack(spacing: GymBroSpacing.sm) {
                    categoryBadge
                    
                    if !exercise.muscleGroups.isEmpty {
                        Text("•")
                            .font(.system(size: captionSize))
                            .foregroundStyle(GymBroColors.textTertiary)
                        Text(primaryMuscles)
                            .font(.system(size: captionSize))
                            .foregroundStyle(GymBroColors.textSecondary)
                            .lineLimit(1)
                    }
                }
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(GymBroColors.textTertiary)
        }
        .padding(.vertical, GymBroSpacing.sm)
        .contentShape(Rectangle())
    }
    
    private var categoryBadge: some View {
        Text(exercise.category.rawValue.uppercased())
            .font(.system(size: 10, weight: .bold))
            .foregroundStyle(categoryColor)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(
                RoundedRectangle(cornerRadius: 4)
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
}

// MARK: - Preview

#Preview("Exercise Library Row") {
    List {
        ExerciseLibraryRow(
            exercise: Exercise(
                name: "Barbell Back Squat",
                category: .compound,
                equipment: .barbell,
                muscleGroups: [
                    MuscleGroup(name: "Quadriceps", isPrimary: true),
                    MuscleGroup(name: "Glutes", isPrimary: true)
                ]
            )
        )
        .listRowBackground(GymBroColors.background)
        
        ExerciseLibraryRow(
            exercise: Exercise(
                name: "Dumbbell Curl",
                category: .isolation,
                equipment: .dumbbell,
                muscleGroups: [
                    MuscleGroup(name: "Biceps", isPrimary: true)
                ]
            )
        )
        .listRowBackground(GymBroColors.background)
        
        ExerciseLibraryRow(
            exercise: Exercise(
                name: "Face Pulls",
                category: .accessory,
                equipment: .cable,
                muscleGroups: [
                    MuscleGroup(name: "Rear Delts", isPrimary: true),
                    MuscleGroup(name: "Traps", isPrimary: false)
                ]
            )
        )
        .listRowBackground(GymBroColors.background)
    }
    .listStyle(.plain)
    .scrollContentBackground(.hidden)
    .gymBroDarkBackground()
}
