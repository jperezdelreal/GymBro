import SwiftUI
import GymBroCore

/// Onboarding step requesting HealthKit permissions for recovery tracking.
/// Explains what data we read and why, with a clear opt-out path.
struct HealthKitPermissionStepView: View {
    let onNext: () -> Void
    let onBack: () -> Void
    let onRequestPermission: () async -> Bool

    @State private var isRequesting = false
    @State private var permissionResult: PermissionResult?

    private enum PermissionResult {
        case granted, denied
    }

    var body: some View {
        VStack(spacing: GymBroSpacing.lg) {
            Spacer()

            // Icon
            Image(systemName: "heart.text.square.fill")
                .font(.system(size: 64))
                .foregroundStyle(
                    LinearGradient(
                        colors: [GymBroColors.accentGreen, .green.opacity(0.7)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .padding(.bottom, GymBroSpacing.sm)

            Text("Recovery Tracking")
                .font(.title.bold())
                .foregroundColor(GymBroColors.textPrimary)

            Text("GymBro uses Apple Health data to calculate your daily recovery score — so you know when to push hard and when to back off.")
                .font(.body)
                .foregroundColor(GymBroColors.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, GymBroSpacing.lg)

            // Data types we request
            VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                dataRow(icon: "bed.double.fill", title: "Sleep", detail: "Duration & stage quality")
                dataRow(icon: "waveform.path.ecg", title: "Heart Rate Variability", detail: "Recovery signal from your nervous system")
                dataRow(icon: "heart.fill", title: "Resting Heart Rate", detail: "Baseline cardiovascular recovery")
                dataRow(icon: "flame.fill", title: "Active Energy", detail: "Daily calorie expenditure")
            }
            .padding(GymBroSpacing.md)
            .background(GymBroColors.surfaceSecondary.cornerRadius(12))
            .padding(.horizontal, GymBroSpacing.md)

            // Privacy note
            HStack(spacing: GymBroSpacing.xs) {
                Image(systemName: "lock.shield.fill")
                    .foregroundColor(GymBroColors.accentGreen)
                    .font(.caption)
                Text("Health data stays on your device — never sent to the cloud.")
                    .font(.caption)
                    .foregroundColor(GymBroColors.textSecondary)
            }
            .padding(.horizontal, GymBroSpacing.lg)

            Spacer()

            // Buttons
            VStack(spacing: GymBroSpacing.sm) {
                if let result = permissionResult {
                    resultBanner(result)
                }

                Button(action: requestPermission) {
                    if isRequesting {
                        ProgressView()
                            .tint(.black)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, GymBroSpacing.sm)
                    } else {
                        Text(permissionResult == .granted ? "Continue" : "Allow Health Access")
                            .font(.headline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, GymBroSpacing.sm)
                    }
                }
                .buttonStyle(.borderedProminent)
                .tint(GymBroColors.accentGreen)
                .disabled(isRequesting)

                Button("Skip for now") {
                    onNext()
                }
                .font(.subheadline)
                .foregroundColor(GymBroColors.textSecondary)

                Button("Back") {
                    onBack()
                }
                .font(.subheadline)
                .foregroundColor(GymBroColors.textSecondary)
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.bottom, GymBroSpacing.lg)
        }
    }

    private func dataRow(icon: String, title: String, detail: String) -> some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: icon)
                .foregroundColor(GymBroColors.accentGreen)
                .frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.bold())
                    .foregroundColor(GymBroColors.textPrimary)
                Text(detail)
                    .font(.caption)
                    .foregroundColor(GymBroColors.textSecondary)
            }
            Spacer()
        }
    }

    @ViewBuilder
    private func resultBanner(_ result: PermissionResult) -> some View {
        HStack(spacing: GymBroSpacing.xs) {
            Image(systemName: result == .granted ? "checkmark.circle.fill" : "info.circle.fill")
            Text(result == .granted
                 ? "Health access granted!"
                 : "No worries — recovery will use training data only. You can enable Health access later in Settings.")
                .font(.caption)
        }
        .foregroundColor(result == .granted ? GymBroColors.accentGreen : GymBroColors.textSecondary)
        .padding(GymBroSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            (result == .granted ? GymBroColors.accentGreen : Color.gray)
                .opacity(0.1)
                .cornerRadius(8)
        )
    }

    private func requestPermission() {
        if permissionResult == .granted {
            onNext()
            return
        }

        isRequesting = true
        Task {
            let granted = await onRequestPermission()
            isRequesting = false
            permissionResult = granted ? .granted : .denied
            if granted {
                // Auto-advance after a brief pause so the user sees the success state
                try? await Task.sleep(for: .milliseconds(800))
                onNext()
            }
        }
    }
}
