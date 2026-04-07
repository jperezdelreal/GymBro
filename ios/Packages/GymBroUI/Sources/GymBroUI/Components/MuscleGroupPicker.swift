import SwiftUI
import GymBroCore

/// Reusable multi-select tag picker for muscle groups.
/// Displays chips in a flow layout with visual feedback on selection.
struct MuscleGroupPicker: View {
    let title: String
    let muscles: [String]
    let selectedMuscles: Set<String>
    let accentColor: Color
    let onToggle: (String) -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .caption) private var chipFontSize: CGFloat = 13

    var body: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            Text(title.uppercased())
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1.2)

            FlowLayout(spacing: GymBroSpacing.sm) {
                ForEach(muscles, id: \.self) { muscle in
                    MuscleChip(
                        name: muscle,
                        isSelected: selectedMuscles.contains(muscle),
                        accentColor: accentColor,
                        fontSize: chipFontSize
                    ) {
                        onToggle(muscle)
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }
}

// MARK: - MuscleChip

private struct MuscleChip: View {
    let name: String
    let isSelected: Bool
    let accentColor: Color
    let fontSize: CGFloat
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .bold))
                }
                Text(name)
                    .font(.system(size: fontSize, weight: isSelected ? .semibold : .regular))
            }
            .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textSecondary)
            .padding(.horizontal, GymBroSpacing.sm + 4)
            .padding(.vertical, GymBroSpacing.xs + 2)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(isSelected ? accentColor : GymBroColors.surfaceElevated)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .strokeBorder(
                        isSelected ? accentColor : GymBroColors.border,
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isSelected)
        .accessibilityLabel(name)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
        .accessibilityHint(isSelected ? "Double tap to deselect" : "Double tap to select")
    }
}

// MARK: - Preview

#Preview("Muscle Group Picker") {
    struct PreviewWrapper: View {
        @State private var primary: Set<String> = ["Chest", "Triceps"]
        @State private var secondary: Set<String> = ["Front Delts"]

        var body: some View {
            ScrollView {
                VStack(spacing: GymBroSpacing.lg) {
                    MuscleGroupPicker(
                        title: "Primary Muscles",
                        muscles: CreateExerciseViewModel.allMuscleGroups,
                        selectedMuscles: primary,
                        accentColor: GymBroColors.accentGreen
                    ) { muscle in
                        if primary.contains(muscle) {
                            primary.remove(muscle)
                        } else {
                            primary.insert(muscle)
                        }
                    }

                    MuscleGroupPicker(
                        title: "Secondary Muscles (Optional)",
                        muscles: CreateExerciseViewModel.allMuscleGroups.filter { !primary.contains($0) },
                        selectedMuscles: secondary,
                        accentColor: GymBroColors.accentAmber
                    ) { muscle in
                        if secondary.contains(muscle) {
                            secondary.remove(muscle)
                        } else {
                            secondary.insert(muscle)
                        }
                    }
                }
                .padding(GymBroSpacing.md)
            }
            .gymBroDarkBackground()
        }
    }

    return PreviewWrapper()
}
