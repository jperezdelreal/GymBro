import SwiftUI
import GymBroCore

/// Equipment availability selection step.
struct EquipmentStepView: View {
    @Binding var selectedEquipment: EquipmentType
    let onNext: () -> Void
    let onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("What equipment do you have?")
                            .font(GymBroTypography.title)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Text("We'll customize exercises accordingly")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    VStack(spacing: GymBroSpacing.md) {
                        equipmentCard(.fullGym, icon: "building.columns.fill")
                        equipmentCard(.homeGym, icon: "house.fill")
                        equipmentCard(.bodyweightOnly, icon: "figure.arms.open")
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
    private func equipmentCard(_ type: EquipmentType, icon: String) -> some View {
        let isSelected = selectedEquipment == type
        
        Button {
            selectedEquipment = type
            
            // Haptic feedback
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
        } label: {
            HStack(spacing: GymBroSpacing.md) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundStyle(isSelected ? GymBroColors.accentGreen : GymBroColors.textSecondary)
                    .frame(width: 32)
                
                Text(type.displayName)
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
    EquipmentStepView(
        selectedEquipment: .constant(.fullGym),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
