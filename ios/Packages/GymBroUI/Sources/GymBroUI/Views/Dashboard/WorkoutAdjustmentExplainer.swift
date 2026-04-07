import SwiftUI
import GymBroCore

/// Transparency card showing when ReadinessProgramIntegration modifies a workout.
/// Displays before/after comparison, the reason why, and an override button.
public struct WorkoutAdjustmentExplainer: View {
    let recommendation: WorkoutRecommendation
    let onOverride: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var showFullRationale = false
    @ScaledMetric private var iconSize: CGFloat = 20

    public init(recommendation: WorkoutRecommendation, onOverride: @escaping () -> Void) {
        self.recommendation = recommendation
        self.onOverride = onOverride
    }

    public var body: some View {
        // Don't show if no adjustment is needed
        if recommendation.action == .proceedAsPlanned {
            EmptyView()
        } else {
            GymBroCard(accent: accentColor) {
                VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                    header
                    comparisonSection
                    rationaleSection
                    overrideButton
                }
            }
        }
    }

    // MARK: - Header

    private var header: some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: actionIcon)
                .font(.system(size: iconSize))
                .foregroundStyle(accentColor)

            VStack(alignment: .leading, spacing: 2) {
                Text("Workout Adjusted")
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                Text(recommendation.action.displayName)
                    .font(GymBroTypography.caption)
                    .foregroundStyle(accentColor)
            }

            Spacer()

            Image(systemName: "sparkles")
                .font(.system(size: 16))
                .foregroundStyle(GymBroColors.accentCyan.opacity(0.6))
        }
    }

    // MARK: - Before/After Comparison

    private var comparisonSection: some View {
        VStack(spacing: GymBroSpacing.sm) {
            // Original plan
            HStack(spacing: GymBroSpacing.sm) {
                Text("PLANNED")
                    .font(GymBroTypography.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(1)

                Spacer()

                Text(recommendation.originalDay.name)
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.textSecondary)
                    .strikethrough(recommendation.action == .restDay, color: GymBroColors.accentRed)
            }
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(GymBroColors.surfaceElevated.opacity(0.5))
            )

            Image(systemName: "arrow.down")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(accentColor)

            // Adjusted plan
            HStack(spacing: GymBroSpacing.sm) {
                Text("ADJUSTED")
                    .font(GymBroTypography.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(accentColor)
                    .tracking(1)

                Spacer()

                adjustmentSummary
            }
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(accentColor.opacity(0.08))
            )

            // Exercise replacements
            if !recommendation.exerciseReplacements.isEmpty {
                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    ForEach(recommendation.exerciseReplacements, id: \.original) { replacement in
                        HStack(spacing: GymBroSpacing.sm) {
                            Image(systemName: "arrow.triangle.swap")
                                .font(.system(size: 10))
                                .foregroundStyle(GymBroColors.textTertiary)

                            Text(replacement.original)
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textSecondary)
                                .strikethrough(color: GymBroColors.accentRed.opacity(0.6))

                            Image(systemName: "arrow.right")
                                .font(.system(size: 8))
                                .foregroundStyle(GymBroColors.textTertiary)

                            Text(replacement.suggestedAction)
                                .font(GymBroTypography.caption)
                                .foregroundStyle(accentColor)
                        }
                    }
                }
                .padding(GymBroSpacing.sm)
                .background(
                    RoundedRectangle(cornerRadius: GymBroRadius.sm)
                        .fill(GymBroColors.surfacePrimary)
                )
            }
        }
    }

    // MARK: - Rationale

    private var rationaleSection: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
            Button {
                withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.2)) {
                    showFullRationale.toggle()
                }
            } label: {
                HStack(spacing: GymBroSpacing.xs) {
                    Image(systemName: "questionmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundStyle(GymBroColors.accentCyan)
                    Text("Why this adjustment?")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.accentCyan)

                    Spacer()

                    Image(systemName: showFullRationale ? "chevron.up" : "chevron.down")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
            .buttonStyle(.plain)

            if showFullRationale {
                Text(recommendation.rationale)
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
                    .padding(GymBroSpacing.sm)
                    .background(
                        RoundedRectangle(cornerRadius: GymBroRadius.sm)
                            .fill(GymBroColors.surfacePrimary)
                    )
            }
        }
    }

    // MARK: - Override Button

    private var overrideButton: some View {
        Button {
            onOverride()
        } label: {
            HStack(spacing: GymBroSpacing.sm) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.system(size: 14))
                Text("Train as planned anyway")
                    .font(GymBroTypography.caption)
                    .fontWeight(.semibold)
            }
            .foregroundStyle(GymBroColors.textSecondary)
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.sm)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.md)
                    .strokeBorder(GymBroColors.border, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Override adjustment and train as originally planned")
    }

    // MARK: - Helpers

    private var adjustmentSummary: some View {
        Group {
            switch recommendation.action {
            case .restDay:
                Text("Rest Day")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.accentRed)
            case .lighterVariant:
                HStack(spacing: GymBroSpacing.xs) {
                    if let intensity = recommendation.intensityAdjustment {
                        Text("\(Int(100 + intensity))% intensity")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(accentColor)
                    }
                    if let volume = recommendation.volumeAdjustment {
                        Text("•")
                            .foregroundStyle(GymBroColors.textTertiary)
                        Text("\(Int(100 + volume))% volume")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(accentColor)
                    }
                }
            case .modifyExercises:
                Text("Modified exercises")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(accentColor)
            case .proceedAsPlanned:
                Text("No changes")
                    .font(GymBroTypography.subheadline)
                    .foregroundStyle(GymBroColors.accentGreen)
            }
        }
    }

    private var accentColor: Color {
        switch recommendation.action {
        case .restDay: return GymBroColors.accentRed
        case .lighterVariant: return GymBroColors.accentAmber
        case .modifyExercises: return GymBroColors.accentAmber
        case .proceedAsPlanned: return GymBroColors.accentGreen
        }
    }

    private var actionIcon: String {
        switch recommendation.action {
        case .restDay: return "bed.double.fill"
        case .lighterVariant: return "arrow.down.right.circle.fill"
        case .modifyExercises: return "arrow.triangle.swap"
        case .proceedAsPlanned: return "checkmark.circle.fill"
        }
    }
}
