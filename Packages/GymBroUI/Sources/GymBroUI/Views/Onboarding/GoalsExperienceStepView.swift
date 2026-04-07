import SwiftUI
import GymBroCore

/// Combined goals + experience step — reduces onboarding by one screen.
/// Goals are multi-select chips; experience is a single-select inline picker below.
struct GoalsExperienceStepView: View {
    @Binding var selectedGoals: Set<TrainingGoal>
    @Binding var selectedLevel: ExperienceLevel
    let onNext: () -> Void
    let onBack: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.xl) {
                    // Goals section
                    goalsSection

                    // Divider
                    Rectangle()
                        .fill(GymBroColors.border)
                        .frame(height: 1)
                        .padding(.horizontal, GymBroSpacing.md)

                    // Experience section
                    experienceSection
                }
                .padding(.top, GymBroSpacing.xl)
                .padding(.bottom, GymBroSpacing.xxl * 2)
            }

            buttonTray
        }
    }

    // MARK: - Goals

    @ViewBuilder
    private var goalsSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                Text("What are your goals?")
                    .font(GymBroTypography.title)
                    .foregroundStyle(GymBroColors.textPrimary)

                Text("Select all that apply")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
            .padding(.horizontal, GymBroSpacing.md)

            // Compact chip layout instead of full-width cards
            FlowLayout(spacing: GymBroSpacing.sm) {
                ForEach(TrainingGoal.allCases) { goal in
                    goalChip(goal)
                }
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
    }

    @ViewBuilder
    private func goalChip(_ goal: TrainingGoal) -> some View {
        let isSelected = selectedGoals.contains(goal)

        Button {
            if isSelected {
                selectedGoals.remove(goal)
            } else {
                selectedGoals.insert(goal)
            }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        } label: {
            HStack(spacing: GymBroSpacing.sm) {
                Image(systemName: goal.icon)
                    .font(.system(size: 14))
                Text(goal.rawValue)
                    .font(GymBroTypography.subheadline)
            }
            .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textSecondary)
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.vertical, GymBroSpacing.sm + 2)
            .background(
                Capsule()
                    .fill(isSelected ? GymBroColors.accentGreen : GymBroColors.surfaceSecondary)
            )
            .overlay(
                Capsule()
                    .strokeBorder(
                        isSelected ? GymBroColors.accentGreen : GymBroColors.border,
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : .easeInOut(duration: 0.15), value: isSelected)
    }

    // MARK: - Experience

    @ViewBuilder
    private var experienceSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.md) {
            VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                Text("Experience level")
                    .font(GymBroTypography.title2)
                    .foregroundStyle(GymBroColors.textPrimary)

                Text("Helps personalize your programs")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
            .padding(.horizontal, GymBroSpacing.md)

            VStack(spacing: GymBroSpacing.sm) {
                ForEach([ExperienceLevel.beginner, .intermediate, .advanced], id: \.self) { level in
                    experienceRow(level)
                }
            }
            .padding(.horizontal, GymBroSpacing.md)
        }
    }

    @ViewBuilder
    private func experienceRow(_ level: ExperienceLevel) -> some View {
        let isSelected = selectedLevel == level

        Button {
            selectedLevel = level
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        } label: {
            HStack(spacing: GymBroSpacing.md) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(level.rawValue.capitalized)
                        .font(GymBroTypography.headline)
                        .foregroundStyle(GymBroColors.textPrimary)

                    Text(level.description)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                        .lineLimit(1)
                }

                Spacer()

                if isSelected {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(GymBroColors.accentGreen)
                } else {
                    Circle()
                        .strokeBorder(GymBroColors.borderSubtle, lineWidth: 2)
                        .frame(width: 24, height: 24)
                }
            }
            .padding(GymBroSpacing.md)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .fill(isSelected ? GymBroColors.accentGreen.opacity(0.08) : GymBroColors.surfaceSecondary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .strokeBorder(
                        isSelected ? GymBroColors.accentGreen.opacity(0.4) : Color.clear,
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
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
                .disabled(selectedGoals.isEmpty)
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
    GoalsExperienceStepView(
        selectedGoals: .constant([.strength]),
        selectedLevel: .constant(.intermediate),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
