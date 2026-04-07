import SwiftUI
import GymBroCore

/// In-app recovery alert view showing readiness status and lighter exercise suggestions.
/// Presented when readiness score drops below 50 (deload) or 30 (rest day).
public struct DeloadSuggestionsView: View {
    let alert: RecoveryAlertService.RecoveryAlert
    let onDismiss: () -> Void

    @ScaledMetric(relativeTo: .largeTitle) private var gaugeSize: CGFloat = 120
    @ScaledMetric(relativeTo: .title) private var iconSize: CGFloat = 32

    public init(
        alert: RecoveryAlertService.RecoveryAlert,
        onDismiss: @escaping () -> Void
    ) {
        self.alert = alert
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    alertHeader
                    recommendationCard
                    suggestionsSection
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)
                .padding(.bottom, 32)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Recovery Alert")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done", action: onDismiss)
                        .fontWeight(.semibold)
                }
            }
        }
    }

    // MARK: - Alert Header

    private var alertHeader: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .stroke(Color(.systemGray5), lineWidth: 12)
                    .frame(width: gaugeSize, height: gaugeSize)

                Circle()
                    .trim(from: 0, to: alert.readinessScore / 100.0)
                    .stroke(
                        alertColor,
                        style: StrokeStyle(lineWidth: 12, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .frame(width: gaugeSize, height: gaugeSize)
                    .animation(.easeInOut(duration: 0.6), value: alert.readinessScore)

                VStack(spacing: 2) {
                    Text("\(Int(alert.readinessScore))")
                        .font(.system(size: 36, weight: .bold, design: .rounded))
                        .foregroundStyle(alertColor)
                    Text("/ 100")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
            }

            VStack(spacing: 4) {
                Text(alert.title)
                    .font(.title2.weight(.bold))
                Text(alert.message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
            }
        }
        .padding(.top, 8)
    }

    // MARK: - Recommendation

    private var recommendationCard: some View {
        HStack(spacing: 12) {
            Image(systemName: alert.level == .restDay ? "bed.double.fill" : "exclamationmark.triangle.fill")
                .font(.title2)
                .foregroundStyle(alertColor)

            VStack(alignment: .leading, spacing: 4) {
                Text("Today's Recommendation")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(alert.recommendation)
                    .font(.subheadline)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(alertColor.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Suggestions

    private var suggestionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(alert.level == .restDay ? "Rest Day Options" : "Lighter Alternatives")
                .font(.headline)
                .padding(.leading, 4)

            ForEach(RecoveryAlertService.deloadSuggestions(for: alert.level)) { suggestion in
                suggestionCard(suggestion)
            }
        }
    }

    private func suggestionCard(_ suggestion: DeloadSuggestion) -> some View {
        HStack(spacing: 14) {
            Image(systemName: suggestion.icon)
                .font(.system(size: iconSize))
                .foregroundStyle(alertColor)
                .frame(width: 44, height: 44)

            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(suggestion.name)
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    if suggestion.intensityPercent > 0 {
                        Text("\(suggestion.intensityPercent)% intensity")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color(.systemGray5))
                            .clipShape(Capsule())
                    } else {
                        Text("Rest")
                            .font(.caption.weight(.medium))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(alertColor)
                            .clipShape(Capsule())
                    }
                }
                Text(suggestion.description)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding()
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Helpers

    private var alertColor: Color {
        alert.level == .restDay ? .red : .orange
    }
}
