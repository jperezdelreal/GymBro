import SwiftUI
import GymBroCore

/// Training frequency selection step — days per week.
struct FrequencyStepView: View {
    @Binding var selectedFrequency: Int
    let onNext: () -> Void
    let onBack: () -> Void
    
    private let frequencies = Array(2...6)
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("How often do you train?")
                            .font(GymBroTypography.title)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Text("Days per week")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    VStack(spacing: GymBroSpacing.md) {
                        ForEach(frequencies, id: \.self) { days in
                            frequencyCard(days)
                        }
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
    private func frequencyCard(_ days: Int) -> some View {
        let isSelected = selectedFrequency == days
        
        Button {
            selectedFrequency = days
            
            // Haptic feedback
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
        } label: {
            HStack {
                Text("\(days) days/week")
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
    FrequencyStepView(
        selectedFrequency: .constant(3),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
