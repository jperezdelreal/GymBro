import SwiftUI
import GymBroCore

/// Combined frequency + equipment step with optional limitations.
/// Frequency uses a segmented-style picker; equipment is icon cards.
/// Limitations are hidden behind a "Got injuries?" toggle — progressive disclosure.
struct FrequencyEquipmentStepView: View {
    @Binding var selectedFrequency: Int
    @Binding var selectedEquipment: EquipmentType
    @Binding var injuriesText: String

    let onNext: () -> Void
    let onBack: () -> Void

    @State private var showLimitations = false
    @FocusState private var isTextFieldFocused: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.xl) {
                    frequencySection
                    equipmentSection
                    limitationsSection
                }
                .padding(.top, GymBroSpacing.xl)
                .padding(.bottom, GymBroSpacing.xxl * 2)
            }
            .scrollDismissesKeyboard(.interactively)

            buttonTray
        }
    }

    // MARK: - Frequency

    @ViewBuilder
    private var frequencySection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                Text("How often do you train?")
                    .font(GymBroTypography.title)
                    .foregroundStyle(GymBroColors.textPrimary)

                Text("Days per week")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
            .padding(.horizontal, GymBroSpacing.md)

            // Compact frequency chips in a single row
            HStack(spacing: GymBroSpacing.sm) {
                ForEach(2...6, id: \.self) { days in
                    frequencyChip(days)
                }
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
    }

    @ViewBuilder
    private func frequencyChip(_ days: Int) -> some View {
        let isSelected = selectedFrequency == days

        Button {
            selectedFrequency = days
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        } label: {
            VStack(spacing: GymBroSpacing.xs) {
                Text("\(days)")
                    .font(GymBroTypography.title2)
                    .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textPrimary)

                Text("days")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(isSelected ? GymBroColors.background.opacity(0.8) : GymBroColors.textTertiary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.md)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .fill(isSelected ? GymBroColors.accentGreen : GymBroColors.surfaceSecondary)
            )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isSelected)
    }

    // MARK: - Equipment

    @ViewBuilder
    private var equipmentSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Text("Equipment")
                .font(GymBroTypography.title2)
                .foregroundStyle(GymBroColors.textPrimary)
                .padding(.horizontal, GymBroSpacing.md)

            HStack(spacing: GymBroSpacing.sm) {
                equipmentCard(.fullGym, icon: "building.columns.fill", label: "Full Gym")
                equipmentCard(.homeGym, icon: "house.fill", label: "Home Gym")
                equipmentCard(.bodyweightOnly, icon: "figure.arms.open", label: "Bodyweight")
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
    }

    @ViewBuilder
    private func equipmentCard(_ type: EquipmentType, icon: String, label: String) -> some View {
        let isSelected = selectedEquipment == type

        Button {
            selectedEquipment = type
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        } label: {
            VStack(spacing: GymBroSpacing.sm) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(isSelected ? GymBroColors.accentGreen : GymBroColors.textSecondary)

                Text(label)
                    .font(GymBroTypography.caption)
                    .foregroundStyle(isSelected ? GymBroColors.textPrimary : GymBroColors.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.md + GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .fill(isSelected ? GymBroColors.accentGreen.opacity(0.1) : GymBroColors.surfaceSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .strokeBorder(
                        isSelected ? GymBroColors.accentGreen.opacity(0.4) : GymBroColors.border,
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Limitations (Progressive Disclosure)

    @ViewBuilder
    private var limitationsSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            Button {
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.25)) {
                    showLimitations.toggle()
                }
            } label: {
                HStack(spacing: GymBroSpacing.sm) {
                    Image(systemName: "heart.text.square")
                        .foregroundStyle(GymBroColors.textTertiary)

                    Text("Any injuries or limitations?")
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textSecondary)

                    Spacer()

                    Text("Optional")
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)

                    Image(systemName: showLimitations ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
            .buttonStyle(.plain)
            .padding(.horizontal, GymBroSpacing.md)

            if showLimitations {
                VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                    // Quick-select chips
                    FlowLayout(spacing: GymBroSpacing.sm) {
                        ForEach(["Lower back", "Shoulder", "Knee", "Wrist"], id: \.self) { area in
                            Button {
                                if injuriesText.isEmpty {
                                    injuriesText = area
                                } else if !injuriesText.contains(area) {
                                    injuriesText += ", \(area)"
                                }
                                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            } label: {
                                Text(area)
                                    .font(GymBroTypography.caption)
                                    .foregroundStyle(GymBroColors.textSecondary)
                                    .padding(.horizontal, GymBroSpacing.sm + 4)
                                    .padding(.vertical, GymBroSpacing.xs + 2)
                                    .background(
                                        Capsule()
                                            .fill(GymBroColors.surfaceElevated)
                                    )
                            }
                            .buttonStyle(.plain)
                        }
                    }

                    TextField("Or describe briefly…", text: $injuriesText, axis: .vertical)
                        .textFieldStyle(.plain)
                        .font(GymBroTypography.body)
                        .foregroundStyle(GymBroColors.textPrimary)
                        .lineLimit(2...4)
                        .padding(GymBroSpacing.md)
                        .background(
                            RoundedRectangle(cornerRadius: GymBroRadius.md)
                                .fill(GymBroColors.surfaceSecondary)
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: GymBroRadius.md)
                                .strokeBorder(
                                    isTextFieldFocused ? GymBroColors.accentGreen : GymBroColors.border,
                                    lineWidth: 1
                                )
                        )
                        .focused($isTextFieldFocused)
                }
                .padding(.horizontal, GymBroSpacing.md)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
    }

    // MARK: - Button Tray

    @ViewBuilder
    private var buttonTray: some View {
        VStack(spacing: GymBroSpacing.md) {
            Divider()
                .background(GymBroColors.border)

            HStack(spacing: GymBroSpacing.md) {
                Button("Back") {
                    onBack()
                }
                .buttonStyle(GymBroSecondaryButtonStyle(accent: GymBroColors.textSecondary))
                .frame(maxWidth: 100)

                Button("Continue") {
                    onNext()
                }
                .buttonStyle(.gymBroPrimary)
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
        .padding(.bottom, GymBroSpacing.md)
        .background(
            GymBroColors.background
                .ignoresSafeArea(edges: .bottom)
        )
    }
}

#Preview {
    FrequencyEquipmentStepView(
        selectedFrequency: .constant(4),
        selectedEquipment: .constant(.fullGym),
        injuriesText: .constant(""),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
