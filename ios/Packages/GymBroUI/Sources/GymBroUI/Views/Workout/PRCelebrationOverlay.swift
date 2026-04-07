import SwiftUI
import GymBroCore

/// Full-screen overlay triggered when a PR is detected.
/// Shows confetti, a PR badge, and auto-dismisses after 3 seconds.
public struct PRCelebrationOverlay: View {
    let result: PRDetectionResult
    let onDismiss: () -> Void

    @State private var isVisible = false
    @State private var badgeScale: CGFloat = 0.3
    @State private var badgeOpacity: Double = 0
    @ScaledMetric(relativeTo: .largeTitle) private var trophySize: CGFloat = 48
    @ScaledMetric(relativeTo: .title) private var badgeTitleSize: CGFloat = 28
    @ScaledMetric(relativeTo: .body) private var detailSize: CGFloat = 16

    public init(result: PRDetectionResult, onDismiss: @escaping () -> Void) {
        self.result = result
        self.onDismiss = onDismiss
    }

    public var body: some View {
        ZStack {
            // Dim background
            Color.black.opacity(isVisible ? 0.6 : 0)
                .ignoresSafeArea()
                .onTapGesture { dismiss() }

            // Confetti layer
            if isVisible {
                ConfettiCelebrationView(particleCount: 80)
                    .ignoresSafeArea()
            }

            // Badge card
            VStack(spacing: GymBroSpacing.md) {
                Image(systemName: "trophy.fill")
                    .font(.system(size: trophySize))
                    .foregroundStyle(GymBroColors.accentAmber)
                    .accessibilityHidden(true)

                Text(result.primaryBadgeText)
                    .font(.system(size: badgeTitleSize, weight: .heavy, design: .rounded))
                    .foregroundStyle(GymBroColors.accentAmber)

                Text(result.exerciseName)
                    .font(GymBroTypography.title3)
                    .foregroundStyle(GymBroColors.textPrimary)

                Text(result.detailText)
                    .font(.system(size: detailSize, weight: .medium))
                    .foregroundStyle(GymBroColors.textSecondary)

                if result.recordTypes.count > 1 {
                    HStack(spacing: GymBroSpacing.sm) {
                        ForEach(result.recordTypes.dropFirst(), id: \.self) { type in
                            PRTypePill(type: type)
                        }
                    }
                    .padding(.top, GymBroSpacing.xs)
                }
            }
            .padding(GymBroSpacing.xl)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.xl)
                    .fill(GymBroColors.surfacePrimary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.xl)
                    .strokeBorder(GymBroColors.accentAmber.opacity(0.4), lineWidth: 2)
            )
            .scaleEffect(badgeScale)
            .opacity(badgeOpacity)
            .padding(.horizontal, GymBroSpacing.xl)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Personal record! \(result.primaryBadgeText) on \(result.exerciseName). \(result.detailText)")
        .onAppear {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) {
                isVisible = true
                badgeScale = 1.0
                badgeOpacity = 1.0
            }

            // Auto-dismiss after 3 seconds
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                dismiss()
            }
        }
    }

    private func dismiss() {
        withAnimation(.easeOut(duration: 0.3)) {
            isVisible = false
            badgeScale = 0.8
            badgeOpacity = 0
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            onDismiss()
        }
    }
}

/// Small pill showing an additional PR type (e.g., when a set is both a weight PR and e1RM PR).
struct PRTypePill: View {
    let type: PersonalRecord.RecordType

    var body: some View {
        Text(label)
            .font(GymBroTypography.caption2)
            .foregroundStyle(GymBroColors.accentAmber)
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                Capsule().fill(GymBroColors.accentAmber.opacity(0.15))
            )
    }

    private var label: String {
        switch type {
        case .maxE1RM:   return "1RM"
        case .maxWeight: return "Weight"
        case .maxVolume: return "Volume"
        case .maxReps:   return "Reps"
        }
    }
}

// MARK: - Preview

#Preview("PR Celebration") {
    ZStack {
        GymBroColors.background.ignoresSafeArea()

        PRCelebrationOverlay(
            result: PRDetectionResult(
                exerciseName: "Bench Press",
                recordTypes: [.maxE1RM, .maxWeight],
                weight: 120.0,
                reps: 3,
                e1rm: 132.0
            ),
            onDismiss: {}
        )
    }
}
