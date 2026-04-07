import SwiftUI

/// Dark card surface with subtle border and optional accent stripe.
public struct GymBroCard<Content: View>: View {
    let accentColor: Color?
    let content: Content

    public init(
        accent: Color? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.accentColor = accent
        self.content = content()
    }

    public var body: some View {
        HStack(spacing: 0) {
            if let accentColor {
                RoundedRectangle(cornerRadius: 2)
                    .fill(accentColor)
                    .frame(width: 4)
                    .padding(.vertical, GymBroSpacing.sm)
            }

            content
                .padding(GymBroSpacing.md + GymBroSpacing.xs)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.lg)
                .fill(GymBroColors.surfaceSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.lg)
                .strokeBorder(GymBroColors.border, lineWidth: 1)
        )
        .gymBroCardShadow()
    }
}

// MARK: - Previews

#Preview("GymBro Cards") {
    ScrollView {
        VStack(spacing: GymBroSpacing.md) {
            GymBroCard {
                VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                    Text("Barbell Squat")
                        .font(GymBroTypography.title3)
                        .foregroundStyle(GymBroColors.textPrimary)
                    Text("4 sets • 5 reps • 140 kg")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                }
            }

            GymBroCard(accent: GymBroColors.accentGreen) {
                VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                    Text("Bench Press")
                        .font(GymBroTypography.title3)
                        .foregroundStyle(GymBroColors.textPrimary)
                    Text("In progress — Set 3 of 4")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.accentGreen)
                }
            }

            GymBroCard(accent: GymBroColors.accentAmber) {
                HStack {
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text("🏆 New PR!")
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.accentAmber)
                        Text("Deadlift — 220 kg × 1")
                            .font(GymBroTypography.body)
                            .foregroundStyle(GymBroColors.textPrimary)
                    }
                    Spacer()
                }
            }

            GymBroCard(accent: GymBroColors.accentRed) {
                HStack(spacing: GymBroSpacing.md) {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.title2)
                        .foregroundStyle(GymBroColors.accentRed)
                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text("Plateau Detected")
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.textPrimary)
                        Text("Overhead Press stalled for 3 weeks")
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                }
            }
        }
        .padding(GymBroSpacing.md)
    }
    .gymBroDarkBackground()
}
