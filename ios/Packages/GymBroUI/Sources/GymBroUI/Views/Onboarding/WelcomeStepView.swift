import SwiftUI
import GymBroCore

/// Welcome step — app introduction, set expectations.
struct WelcomeStepView: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    
    let onNext: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            Spacer()
            
            // Hero icon with animation
            Image(systemName: "figure.strengthtraining.traditional")
                .font(.system(size: 80))
                .foregroundStyle(GymBroColors.accentGreen)
                .symbolEffect(.bounce, options: .speed(0.3))
                .padding(.bottom, GymBroSpacing.xl)
            
            VStack(spacing: GymBroSpacing.md) {
                Text("Welcome to GymBro")
                    .font(GymBroTypography.largeTitle)
                    .foregroundStyle(GymBroColors.textPrimary)
                
                Text("Your AI-powered training partner")
                    .font(GymBroTypography.title3)
                    .foregroundStyle(GymBroColors.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .padding(.bottom, GymBroSpacing.xl)
            
            VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                featureRow(
                    icon: "bolt.fill",
                    title: "Ultra-fast logging",
                    subtitle: "1-tap set tracking, smart defaults"
                )
                featureRow(
                    icon: "brain.head.profile",
                    title: "AI coaching",
                    subtitle: "Real-time training insights"
                )
                featureRow(
                    icon: "chart.line.uptrend.xyaxis",
                    title: "Adaptive programs",
                    subtitle: "Auto-periodization, plateau detection"
                )
            }
            .padding(.horizontal, GymBroSpacing.xl)
            
            Spacer()
            
            VStack(spacing: GymBroSpacing.sm) {
                Text("Let's personalize your experience")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)
                
                Text("Takes under 2 minutes")
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary.opacity(0.7))
            }
            .padding(.bottom, GymBroSpacing.md)
            
            Button("Get Started") {
                onNext()
            }
            .buttonStyle(.gymBroPrimary)
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.bottom, GymBroSpacing.xl)
        }
    }
    
    @ViewBuilder
    private func featureRow(icon: String, title: String, subtitle: String) -> some View {
        HStack(spacing: GymBroSpacing.md) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(GymBroColors.accentGreen)
                .frame(width: 32)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(GymBroTypography.headline)
                    .foregroundStyle(GymBroColors.textPrimary)
                
                Text(subtitle)
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textSecondary)
            }
            
            Spacer()
        }
    }
}

#Preview {
    WelcomeStepView(onNext: {})
        .gymBroDarkBackground()
}
