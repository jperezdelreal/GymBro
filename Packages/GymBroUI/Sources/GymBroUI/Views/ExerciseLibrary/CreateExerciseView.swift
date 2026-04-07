import SwiftUI
import SwiftData
import GymBroCore

public struct CreateExerciseView: View {
    @State private var viewModel = CreateExerciseViewModel()
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var sectionHeaderSize: CGFloat = 12
    @FocusState private var focusedField: Field?

    private enum Field: Hashable {
        case name, instructions
    }

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    nameSection
                    categorySection
                    equipmentSection
                    primaryMusclesSection
                    secondaryMusclesSection
                    instructionsSection

                    if let error = viewModel.validationError {
                        errorBanner(error)
                    }

                    saveButton
                }
                .padding(GymBroSpacing.md)
            }
            .gymBroDarkBackground()
            .navigationTitle("New Exercise")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .foregroundStyle(GymBroColors.textSecondary)
                }
            }
            .task {
                viewModel.setup(modelContext: modelContext)
            }
            .onChange(of: viewModel.didSave) { _, saved in
                if saved { dismiss() }
            }
        }
    }

    // MARK: - Name

    private var nameSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            sectionLabel("EXERCISE NAME")

            TextField("e.g. Landmine Press", text: $viewModel.name)
                .font(GymBroTypography.body)
                .foregroundStyle(GymBroColors.textPrimary)
                .padding(GymBroSpacing.md)
                .background(
                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                        .fill(GymBroColors.surfaceSecondary)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                        .strokeBorder(
                            viewModel.isNameEmpty && viewModel.validationError != nil
                                ? GymBroColors.accentRed.opacity(0.6)
                                : GymBroColors.border,
                            lineWidth: 1
                        )
                )
                .focused($focusedField, equals: .name)
                .textInputAutocapitalization(.words)
                .autocorrectionDisabled()
                .submitLabel(.next)
                .onSubmit { focusedField = .instructions }
                .accessibilityLabel("Exercise name")
        }
    }

    // MARK: - Category

    private var categorySection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            sectionLabel("CATEGORY")

            HStack(spacing: GymBroSpacing.sm) {
                ForEach(ExerciseCategory.allCases, id: \.self) { cat in
                    categoryChip(cat)
                }
            }
        }
    }

    private func categoryChip(_ cat: ExerciseCategory) -> some View {
        let isSelected = viewModel.category == cat

        return Button {
            viewModel.category = cat
        } label: {
            Text(cat.rawValue.capitalized)
                .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textSecondary)
                .padding(.horizontal, GymBroSpacing.sm + 4)
                .padding(.vertical, GymBroSpacing.sm)
                .background(
                    RoundedRectangle(cornerRadius: GymBroRadius.sm)
                        .fill(isSelected ? categoryColor(cat) : GymBroColors.surfaceElevated)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: GymBroRadius.sm)
                        .strokeBorder(isSelected ? categoryColor(cat) : GymBroColors.border, lineWidth: 1)
                )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isSelected)
        .accessibilityLabel("Category: \(cat.rawValue)")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func categoryColor(_ cat: ExerciseCategory) -> Color {
        switch cat {
        case .compound: return GymBroColors.accentGreen
        case .isolation: return GymBroColors.accentAmber
        case .accessory: return GymBroColors.accentCyan
        case .cardio: return GymBroColors.accentRed
        }
    }

    // MARK: - Equipment

    private var equipmentSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            sectionLabel("EQUIPMENT")

            FlowLayout(spacing: GymBroSpacing.sm) {
                ForEach(Equipment.allCases, id: \.self) { eq in
                    equipmentChip(eq)
                }
            }
        }
    }

    private func equipmentChip(_ eq: Equipment) -> some View {
        let isSelected = viewModel.equipment == eq

        return Button {
            viewModel.equipment = eq
        } label: {
            HStack(spacing: 4) {
                Image(systemName: equipmentIcon(eq))
                    .font(.system(size: 12))
                Text(eq.rawValue.capitalized)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
            }
            .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textSecondary)
            .padding(.horizontal, GymBroSpacing.sm + 4)
            .padding(.vertical, GymBroSpacing.sm)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(isSelected ? GymBroColors.accentGreen : GymBroColors.surfaceElevated)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .strokeBorder(isSelected ? GymBroColors.accentGreen : GymBroColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isSelected)
        .accessibilityLabel("Equipment: \(eq.rawValue)")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private func equipmentIcon(_ eq: Equipment) -> String {
        switch eq {
        case .barbell: return "figure.strengthtraining.traditional"
        case .dumbbell: return "dumbbell.fill"
        case .kettlebell: return "figure.strengthtraining.functional"
        case .machine: return "gearshape.2.fill"
        case .cable: return "cable.connector"
        case .bodyweight: return "figure.arms.open"
        case .band: return "bandage.fill"
        case .other: return "figure.mixed.cardio"
        }
    }

    // MARK: - Muscle Groups

    private var primaryMusclesSection: some View {
        MuscleGroupPicker(
            title: "Primary Muscles",
            muscles: CreateExerciseViewModel.allMuscleGroups,
            selectedMuscles: viewModel.selectedPrimaryMuscles,
            accentColor: GymBroColors.accentGreen
        ) { muscle in
            viewModel.togglePrimaryMuscle(muscle)
        }
    }

    private var secondaryMusclesSection: some View {
        MuscleGroupPicker(
            title: "Secondary Muscles (Optional)",
            muscles: viewModel.availableSecondaryMuscles,
            selectedMuscles: viewModel.selectedSecondaryMuscles,
            accentColor: GymBroColors.accentAmber
        ) { muscle in
            viewModel.toggleSecondaryMuscle(muscle)
        }
    }

    // MARK: - Instructions

    private var instructionsSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            sectionLabel("INSTRUCTIONS (OPTIONAL)")

            TextField("Setup, execution, cues…", text: $viewModel.instructions, axis: .vertical)
                .font(GymBroTypography.body)
                .foregroundStyle(GymBroColors.textPrimary)
                .lineLimit(4...8)
                .padding(GymBroSpacing.md)
                .background(
                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                        .fill(GymBroColors.surfaceSecondary)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                        .strokeBorder(GymBroColors.border, lineWidth: 1)
                )
                .focused($focusedField, equals: .instructions)
                .accessibilityLabel("Exercise instructions")
        }
    }

    // MARK: - Error

    private func errorBanner(_ message: String) -> some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(GymBroColors.accentRed)
            Text(message)
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.accentRed)
        }
        .padding(GymBroSpacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .fill(GymBroColors.accentRed.opacity(0.08))
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .strokeBorder(GymBroColors.accentRed.opacity(0.3), lineWidth: 1)
        )
        .accessibilityLabel("Error: \(message)")
    }

    // MARK: - Save

    private var saveButton: some View {
        Button {
            focusedField = nil
            viewModel.saveExercise()
        } label: {
            HStack {
                if viewModel.isSaving {
                    ProgressView()
                        .tint(GymBroColors.background)
                } else {
                    Image(systemName: "plus.circle.fill")
                    Text("Create Exercise")
                }
            }
        }
        .buttonStyle(.gymBroPrimary)
        .disabled(!viewModel.canSave)
        .padding(.top, GymBroSpacing.sm)
    }

    // MARK: - Helpers

    private func sectionLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: sectionHeaderSize, weight: .bold))
            .foregroundStyle(GymBroColors.textTertiary)
            .tracking(1.2)
    }
}

// MARK: - Preview

#Preview("Create Exercise") {
    CreateExerciseView()
        .modelContainer(for: Exercise.self, inMemory: true)
}
