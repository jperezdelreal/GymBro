import SwiftUI
import GymBroCore

/// Experience level selection step — single select with descriptions.
struct ExperienceStepView: View {
    @Binding var selectedLevel: ExperienceLevel
    let onNext: () -> Void
    let onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("What's your experience level?")
                            .font(GymBroTypography.title)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Text("This helps us personalize your programs")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    VStack(spacing: GymBroSpacing.md) {
                        levelCard(.beginner)
                        levelCard(.intermediate)
                        levelCard(.advanced)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                }
                .padding(.top, GymBroSpacing.xl)
                .padding(.bottom, GymBroSpacing.xxl * 2)
            }
            
            // Bottom button tray
            buttonTray
        }
    }
    
    @ViewBuilder
    private func levelCard(_ level: ExperienceLevel) -> some View {
        let isSelected = selectedLevel == level
        
        Button {
            selectedLevel = level
            
            // Haptic feedback
            let generator = UIImpactFeedbackGenerator(style: .medium)
            generator.impactOccurred()
        } label: {
            HStack(spacing: GymBroSpacing.md) {
                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    HStack {
                        Text(level.rawValue.capitalized)
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Spacer()
                        
                        // Radio button
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
                    
                    Text(level.description)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                        .multilineTextAlignment(.leading)
                }
            }
            .padding(GymBroSpacing.md + GymBroSpacing.xs)
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
    ExperienceStepView(
        selectedLevel: .constant(.intermediate),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
