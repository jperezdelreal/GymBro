import SwiftUI
import GymBroCore

/// Proactive health alert banner surfaced when ReadinessAnomalyDetector fires.
/// Medium severity = amber, High severity = red. Dismissable per-session.
public struct AnomalyAlertBanner: View {
    let anomaly: ReadinessAnomaly
    let onDismiss: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isVisible = true
    @ScaledMetric private var iconSize: CGFloat = 24

    public init(anomaly: ReadinessAnomaly, onDismiss: @escaping () -> Void) {
        self.anomaly = anomaly
        self.onDismiss = onDismiss
    }

    public var body: some View {
        if isVisible {
            GymBroCard(accent: accentColor) {
                VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                    // Header row
                    HStack(spacing: GymBroSpacing.sm) {
                        Image(systemName: anomaly.type.icon)
                            .font(.system(size: iconSize))
                            .foregroundStyle(accentColor)

                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: GymBroSpacing.xs) {
                                Text(severityLabel)
                                    .font(GymBroTypography.caption2)
                                    .fontWeight(.bold)
                                    .foregroundStyle(accentColor)
                                    .textCase(.uppercase)
                                    .tracking(1)

                                Text("•")
                                    .foregroundStyle(GymBroColors.textTertiary)

                                Text(anomaly.type.displayName)
                                    .font(GymBroTypography.caption2)
                                    .foregroundStyle(GymBroColors.textTertiary)
                            }

                            Text(anomaly.message)
                                .font(GymBroTypography.subheadline)
                                .foregroundStyle(GymBroColors.textPrimary)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        Spacer()

                        Button {
                            withAnimation(reduceMotion ? nil : .easeOut(duration: 0.25)) {
                                isVisible = false
                            }
                            onDismiss()
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundStyle(GymBroColors.textTertiary)
                                .frame(width: 28, height: 28)
                                .background(
                                    Circle().fill(GymBroColors.surfaceElevated)
                                )
                        }
                        .accessibilityLabel("Dismiss alert")
                    }

                    // Recommendation
                    HStack(spacing: GymBroSpacing.sm) {
                        Image(systemName: "lightbulb.fill")
                            .font(.system(size: 14))
                            .foregroundStyle(GymBroColors.accentAmber)

                        Text(anomaly.recommendation)
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .padding(GymBroSpacing.sm)
                    .background(
                        RoundedRectangle(cornerRadius: GymBroRadius.sm)
                            .fill(accentColor.opacity(0.06))
                    )
                }
            }
            .transition(reduceMotion ? .opacity : .move(edge: .top).combined(with: .opacity))
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Health alert: \(anomaly.type.displayName). \(anomaly.message). Recommendation: \(anomaly.recommendation)")
            .accessibilityAddTraits(.isButton)
        }
    }

    // MARK: - Helpers

    private var accentColor: Color {
        switch anomaly.severity {
        case .medium: return GymBroColors.accentAmber
        case .high: return GymBroColors.accentRed
        }
    }

    private var severityLabel: String {
        switch anomaly.severity {
        case .medium: return "Warning"
        case .high: return "Alert"
        }
    }
}
