import SwiftUI
import GymBroCore

/// Empty state component — clear messaging, single CTA.
public struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    let actionTitle: String
    let action: () -> Void
    
    public init(
        icon: String,
        title: String,
        message: String,
        actionTitle: String,
        action: @escaping () -> Void
    ) {
        self.icon = icon
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }
    
    public var body: some View {
        VStack(spacing: GymBroSpacing.xl) {
            Spacer()
            
            VStack(spacing: GymBroSpacing.lg) {
                Image(systemName: icon)
                    .font(.system(size: 64))
                    .foregroundStyle(GymBroColors.textTertiary)
                
                VStack(spacing: GymBroSpacing.sm) {
                    Text(title)
                        .font(GymBroTypography.title2)
                        .foregroundStyle(GymBroColors.textPrimary)
                        .multilineTextAlignment(.center)
                    
                    Text(message)
                        .font(GymBroTypography.body)
                        .foregroundStyle(GymBroColors.textSecondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, GymBroSpacing.xl)
            }
            
            Button(actionTitle) {
                action()
            }
            .buttonStyle(.gymBroPrimary)
            .padding(.horizontal, GymBroSpacing.xl)
            
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(GymBroColors.background.ignoresSafeArea())
    }
}

#Preview {
    EmptyStateView(
        icon: "figure.strengthtraining.traditional",
        title: "No workouts yet",
        message: "Start your first workout to begin tracking your progress",
        actionTitle: "Start Workout",
        action: {}
    )
}
