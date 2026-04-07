import SwiftUI
import GymBroCore

/// Visual muscle recovery heat map showing front + back body outlines with
/// color-coded muscle groups. Green = Fresh, Yellow = Recovering, Red = Fatigued.
/// Tap a muscle group → detail sheet.
public struct MuscleRecoveryHeatMap: View {
    let recoveryMap: [String: MuscleRecoveryStatus]

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var selectedMuscle: MuscleRecoveryStatus?
    @State private var showingDetail = false
    @ScaledMetric private var bodyHeight: CGFloat = 280

    public init(recoveryMap: [String: MuscleRecoveryStatus]) {
        self.recoveryMap = recoveryMap
    }

    public var body: some View {
        VStack(spacing: GymBroSpacing.md) {
            Text("Muscle Recovery")
                .font(GymBroTypography.title3)
                .foregroundStyle(GymBroColors.textPrimary)
                .frame(maxWidth: .infinity, alignment: .leading)

            legend

            if recoveryMap.isEmpty {
                emptyState
            } else {
                HStack(spacing: GymBroSpacing.lg) {
                    bodyView(side: .front)
                    bodyView(side: .back)
                }
            }
        }
        .sheet(isPresented: $showingDetail) {
            if let muscle = selectedMuscle {
                MuscleDetailSheet(status: muscle)
                    .presentationDetents([.medium])
            }
        }
    }

    // MARK: - Body View

    private enum BodySide: String {
        case front = "Front"
        case back = "Back"
    }

    private func bodyView(side: BodySide) -> some View {
        VStack(spacing: GymBroSpacing.sm) {
            Text(side.rawValue)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
                .tracking(1)

            VStack(spacing: GymBroSpacing.xs) {
                ForEach(muscles(for: side), id: \.name) { muscle in
                    muscleRow(muscle)
                }
            }
            .padding(GymBroSpacing.sm)
            .frame(minHeight: bodyHeight)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .fill(GymBroColors.surfacePrimary)
            )
            .overlay(
                RoundedRectangle(cornerRadius: GymBroRadius.lg)
                    .strokeBorder(GymBroColors.border, lineWidth: 1)
            )
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(side.rawValue) body muscle recovery")
    }

    // MARK: - Muscle Row

    private func muscleRow(_ status: MuscleRecoveryStatus) -> some View {
        Button {
            selectedMuscle = status
            showingDetail = true
        } label: {
            HStack(spacing: GymBroSpacing.sm) {
                Circle()
                    .fill(colorFor(status.status))
                    .frame(width: 10, height: 10)

                Text(status.muscleName)
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textPrimary)
                    .lineLimit(1)

                Spacer()

                recoveryBar(percentage: status.recoveryPercentage, status: status.status)
            }
            .padding(.horizontal, GymBroSpacing.sm)
            .padding(.vertical, GymBroSpacing.xs)
            .background(
                RoundedRectangle(cornerRadius: GymBroRadius.sm)
                    .fill(colorFor(status.status).opacity(0.08))
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(status.muscleName), \(status.status.rawValue), \(Int(status.recoveryPercentage)) percent recovered")
        .accessibilityHint("Double tap for details")
    }

    // MARK: - Recovery Bar

    private func recoveryBar(percentage: Double, status: RecoveryStatus) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 3)
                    .fill(GymBroColors.surfaceElevated)

                RoundedRectangle(cornerRadius: 3)
                    .fill(colorFor(status))
                    .frame(width: geo.size.width * min(percentage / 100, 1.0))
                    .animation(
                        reduceMotion ? nil : .easeInOut(duration: 0.6),
                        value: percentage
                    )
            }
        }
        .frame(width: 50, height: 6)
    }

    // MARK: - Legend

    private var legend: some View {
        HStack(spacing: GymBroSpacing.md) {
            legendItem("Fresh", color: GymBroColors.accentGreen)
            legendItem("Recovering", color: GymBroColors.accentAmber)
            legendItem("Fatigued", color: GymBroColors.accentRed)
        }
        .frame(maxWidth: .infinity)
    }

    private func legendItem(_ text: String, color: Color) -> some View {
        HStack(spacing: GymBroSpacing.xs) {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
            Text(text)
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.textSecondary)
        }
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: GymBroSpacing.sm) {
            Image(systemName: "figure.stand")
                .font(.system(size: 40))
                .foregroundStyle(GymBroColors.textTertiary)
            Text("No recovery data yet")
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)
            Text("Complete a workout to see muscle recovery")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textTertiary)
        }
        .padding(GymBroSpacing.xl)
    }

    // MARK: - Helpers

    private func colorFor(_ status: RecoveryStatus) -> Color {
        switch status {
        case .fresh: return GymBroColors.accentGreen
        case .recovering: return GymBroColors.accentAmber
        case .fatigued: return GymBroColors.accentRed
        }
    }

    /// Muscles grouped by front vs back of body.
    private static let frontMuscles = ["Chest", "Shoulders", "Biceps", "Abs", "Quadriceps", "Forearms"]
    private static let backMuscles = ["Back", "Lats", "Traps", "Triceps", "Glutes", "Hamstrings", "Calves"]

    private func muscles(for side: BodySide) -> [MuscleRecoveryStatus] {
        let muscleNames = side == .front ? Self.frontMuscles : Self.backMuscles
        return muscleNames.compactMap { name in
            recoveryMap[name]
        }
    }
}

// MARK: - Muscle Detail Sheet

/// Detail sheet shown when tapping a muscle group on the heat map.
struct MuscleDetailSheet: View {
    let status: MuscleRecoveryStatus

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: GymBroSpacing.lg) {
                // Status badge
                HStack(spacing: GymBroSpacing.sm) {
                    Circle()
                        .fill(statusColor)
                        .frame(width: 14, height: 14)
                    Text(status.status.rawValue.capitalized)
                        .font(GymBroTypography.headline)
                        .foregroundStyle(statusColor)
                }
                .padding(.horizontal, GymBroSpacing.md)
                .padding(.vertical, GymBroSpacing.sm)
                .background(
                    Capsule().fill(statusColor.opacity(0.15))
                )

                // Recovery percentage
                HeroNumber(
                    value: Int(status.recoveryPercentage),
                    unit: "% recovered"
                )

                // Details
                GymBroCard {
                    VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                        if let lastTrained = status.lastTrainedDate {
                            detailRow(
                                icon: "calendar",
                                label: "Last Trained",
                                value: lastTrained.formatted(date: .abbreviated, time: .shortened)
                            )
                        }

                        if let hours = status.hoursSinceLastTrained {
                            detailRow(
                                icon: "clock",
                                label: "Hours Since Training",
                                value: String(format: "%.0fh", hours)
                            )
                        }

                        detailRow(
                            icon: "dumbbell",
                            label: "Recent Volume",
                            value: String(format: "%.0f kg", status.recentVolume)
                        )

                        Divider()
                            .background(GymBroColors.border)

                        HStack(spacing: GymBroSpacing.sm) {
                            Image(systemName: "lightbulb.fill")
                                .foregroundStyle(GymBroColors.accentAmber)
                            Text(status.recommendation)
                                .font(GymBroTypography.subheadline)
                                .foregroundStyle(GymBroColors.textSecondary)
                        }
                    }
                }

                Spacer()
            }
            .padding(GymBroSpacing.md)
            .navigationTitle(status.muscleName)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundStyle(GymBroColors.accentGreen)
                }
            }
            .gymBroDarkBackground()
        }
    }

    private func detailRow(icon: String, label: String, value: String) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundStyle(GymBroColors.textTertiary)
                .frame(width: 24)
            Text(label)
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textSecondary)
            Spacer()
            Text(value)
                .font(GymBroTypography.subheadline.monospacedDigit())
                .foregroundStyle(GymBroColors.textPrimary)
        }
    }

    private var statusColor: Color {
        switch status.status {
        case .fresh: return GymBroColors.accentGreen
        case .recovering: return GymBroColors.accentAmber
        case .fatigued: return GymBroColors.accentRed
        }
    }
}
