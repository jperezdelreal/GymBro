import SwiftUI
import GymBroCore

/// Injuries/limitations step — optional free text.
struct LimitationsStepView: View {
    @Binding var injuriesText: String
    @FocusState private var isTextFieldFocused: Bool
    
    let onNext: () -> Void
    let onBack: () -> Void
    
    private let commonLimitations = [
        "Lower back issues",
        "Shoulder pain",
        "Knee problems",
        "Wrist pain",
        "None"
    ]
    
    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: GymBroSpacing.lg) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("Any injuries or limitations?")
                            .font(GymBroTypography.title)
                            .foregroundStyle(GymBroColors.textPrimary)
                        
                        Text("Optional — we'll suggest safer alternatives")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    // Common selections
                    VStack(spacing: GymBroSpacing.sm) {
                        ForEach(commonLimitations, id: \.self) { limitation in
                            Button {
                                if limitation == "None" {
                                    injuriesText = ""
                                } else {
                                    if injuriesText.isEmpty {
                                        injuriesText = limitation
                                    } else if !injuriesText.contains(limitation) {
                                        injuriesText += ", \(limitation)"
                                    }
                                }
                                
                                let generator = UIImpactFeedbackGenerator(style: .light)
                                generator.impactOccurred()
                            } label: {
                                HStack {
                                    Text(limitation)
                                        .font(GymBroTypography.subheadline)
                                        .foregroundStyle(GymBroColors.textPrimary)
                                    
                                    Spacer()
                                    
                                    Image(systemName: "plus.circle")
                                        .foregroundStyle(GymBroColors.accentGreen)
                                }
                                .padding(GymBroSpacing.md)
                                .background(
                                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                                        .fill(GymBroColors.surfaceSecondary)
                                )
                                .overlay(
                                    RoundedRectangle(cornerRadius: GymBroRadius.md)
                                        .strokeBorder(GymBroColors.border, lineWidth: 1)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                    
                    // Or free text
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Text("OR DESCRIBE")
                            .font(GymBroTypography.caption2)
                            .foregroundStyle(GymBroColors.textTertiary)
                            .tracking(1.5)
                        
                        TextField("E.g., recovering from shoulder surgery", text: $injuriesText, axis: .vertical)
                            .textFieldStyle(.plain)
                            .font(GymBroTypography.body)
                            .foregroundStyle(GymBroColors.textPrimary)
                            .lineLimit(3...6)
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
                }
                .padding(.top, GymBroSpacing.xl)
                .padding(.bottom, GymBroSpacing.xxl * 2)
            }
            .scrollDismissesKeyboard(.interactively)
            
            // Bottom button tray
            buttonTray
        }
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
    LimitationsStepView(
        injuriesText: .constant(""),
        onNext: {},
        onBack: {}
    )
    .gymBroDarkBackground()
}
