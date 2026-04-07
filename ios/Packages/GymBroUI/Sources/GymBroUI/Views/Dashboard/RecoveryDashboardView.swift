import SwiftUI
import SwiftData
import GymBroCore

/// Unified Recovery Dashboard — enhanced readiness gauge, muscle heat map,
/// active anomaly alerts, workout adjustment explainer, and push/pull ratio.
public struct RecoveryDashboardView: View {
    @State private var viewModel = RecoveryDashboardViewModel()
    @Environment(\.modelContext) private var modelContext
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init() {}

    public var body: some View {
        ScrollView {
            LazyVStack(spacing: GymBroSpacing.lg) {
                // Active anomaly alerts (top priority — user safety)
                anomalyAlerts

                // Enhanced readiness gauge
                readinessSection

                // Muscle heat map
                heatMapSection

                // Workout adjustment explainer
                adjustmentSection

                // Push/Pull ratio
                pushPullSection
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.bottom, GymBroSpacing.xxl)
        }
        .navigationTitle("Recovery")
        .gymBroDarkBackground()
        .task {
            viewModel.loadData(modelContext: modelContext)
        }
        .refreshable {
            viewModel.loadData(modelContext: modelContext)
        }
    }

    // MARK: - Anomaly Alerts

    @ViewBuilder
    private var anomalyAlerts: some View {
        let active = viewModel.activeAnomalies
        if !active.isEmpty {
            VStack(spacing: GymBroSpacing.sm) {
                ForEach(active, id: \.message) { anomaly in
                    AnomalyAlertBanner(anomaly: anomaly) {
                        viewModel.dismissAnomaly(anomaly)
                    }
                }
            }
        }
    }

    // MARK: - Enhanced Readiness Gauge

    @ViewBuilder
    private var readinessSection: some View {
        if let score = viewModel.readinessScore {
            GymBroCard {
                VStack(spacing: GymBroSpacing.md) {
                    enhancedGauge(score)
                    factorPills(score)
                    recommendationBadge(score)
                }
            }
        } else {
            GymBroCard {
                VStack(spacing: GymBroSpacing.sm) {
                    Image(systemName: "heart.text.square")
                        .font(.system(size: 36))
                        .foregroundStyle(GymBroColors.textTertiary)
                    Text("No readiness data")
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textSecondary)
                    Text("Enable HealthKit access for recovery insights")
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .multilineTextAlignment(.center)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, GymBroSpacing.lg)
            }
        }
    }

    private func enhancedGauge(_ score: ReadinessScore) -> some View {
        ZStack {
            // Background arc
            Circle()
                .trim(from: 0, to: 0.75)
                .stroke(GymBroColors.surfaceElevated, lineWidth: 14)
                .rotationEffect(.degrees(135))

            // Colored arc
            Circle()
                .trim(from: 0, to: 0.75 * score.overallScore / 100.0)
                .stroke(
                    AngularGradient(
                        colors: gaugeGradientColors(for: score.overallScore),
                        center: .center,
                        startAngle: .degrees(0),
                        endAngle: .degrees(270)
                    ),
                    style: StrokeStyle(lineWidth: 14, lineCap: .round)
                )
                .rotationEffect(.degrees(135))
                .animation(
                    reduceMotion ? nil : .easeInOut(duration: 0.8),
                    value: score.overallScore
                )

            // Center content
            VStack(spacing: GymBroSpacing.xs) {
                Text("\(Int(score.overallScore))")
                    .font(GymBroTypography.monoNumber(size: 44, weight: .heavy))
                    .foregroundStyle(colorForScore(score.overallScore))
                    .contentTransition(.numericText())

                Text(score.label.displayName.uppercased())
                    .font(GymBroTypography.caption2)
                    .fontWeight(.bold)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(2)
            }
        }
        .frame(width: 180, height: 180)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Recovery score \(Int(score.overallScore)), \(score.label.displayName)")
        .accessibilityValue("\(Int(score.overallScore)) out of 100")
    }

    private func factorPills(_ score: ReadinessScore) -> some View {
        LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: GymBroSpacing.sm) {
            factorPill("Sleep", score: score.sleepScore, icon: "bed.double.fill")
            factorPill("HRV", score: score.hrvScore, icon: "waveform.path.ecg")
            factorPill("Heart Rate", score: score.restingHRScore, icon: "heart.fill")
            factorPill("Load", score: score.trainingLoadScore, icon: "figure.strengthtraining.traditional")
        }
    }

    private func factorPill(_ name: String, score: Double, icon: String) -> some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundStyle(colorForScore(score))
                .frame(width: 20)

            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                Text("\(Int(score))")
                    .font(GymBroTypography.subheadline.monospacedDigit().bold())
                    .foregroundStyle(colorForScore(score))
            }

            Spacer()
        }
        .padding(.horizontal, GymBroSpacing.sm)
        .padding(.vertical, GymBroSpacing.sm)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.sm)
                .fill(GymBroColors.surfacePrimary)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(name) score \(Int(score))")
    }

    private func recommendationBadge(_ score: ReadinessScore) -> some View {
        HStack(spacing: GymBroSpacing.sm) {
            Image(systemName: iconForLabel(score.label))
                .foregroundStyle(colorForScore(score.overallScore))

            Text(score.recommendation)
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(GymBroSpacing.sm)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.sm)
                .fill(colorForScore(score.overallScore).opacity(0.06))
        )
    }

    // MARK: - Heat Map Section

    private var heatMapSection: some View {
        MuscleRecoveryHeatMap(recoveryMap: viewModel.muscleRecoveryMap)
    }

    // MARK: - Workout Adjustment Section

    @ViewBuilder
    private var adjustmentSection: some View {
        if let rec = viewModel.workoutRecommendation, !viewModel.didOverrideAdjustment {
            WorkoutAdjustmentExplainer(recommendation: rec) {
                viewModel.overrideAdjustment()
            }
        }
    }

    // MARK: - Push/Pull Ratio

    @ViewBuilder
    private var pushPullSection: some View {
        if let analysis = viewModel.imbalanceAnalysis {
            GymBroCard {
                VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                    HStack {
                        Image(systemName: "arrow.left.arrow.right")
                            .foregroundStyle(GymBroColors.accentCyan)
                        Text("Push / Pull Balance")
                            .font(GymBroTypography.title3)
                            .foregroundStyle(GymBroColors.textPrimary)
                    }

                    // Ratio bar
                    pushPullBar(ratio: analysis.pushPullRatio)

                    // Status
                    HStack(spacing: GymBroSpacing.sm) {
                        Circle()
                            .fill(pushPullStatusColor(ratio: analysis.pushPullRatio))
                            .frame(width: 8, height: 8)
                        Text(viewModel.pushPullStatus)
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textSecondary)

                        Spacer()

                        Text(String(format: "%.2f:1", analysis.pushPullRatio))
                            .font(GymBroTypography.subheadline.monospacedDigit().bold())
                            .foregroundStyle(pushPullStatusColor(ratio: analysis.pushPullRatio))
                    }

                    // Recommendations
                    if !analysis.recommendations.isEmpty {
                        VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                            ForEach(analysis.recommendations.prefix(2), id: \.self) { rec in
                                Text(rec)
                                    .font(GymBroTypography.caption)
                                    .foregroundStyle(GymBroColors.textSecondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                        }
                        .padding(GymBroSpacing.sm)
                        .background(
                            RoundedRectangle(cornerRadius: GymBroRadius.sm)
                                .fill(GymBroColors.surfacePrimary)
                        )
                    }
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityLabel("Push pull balance ratio \(String(format: "%.2f", analysis.pushPullRatio)) to 1, \(viewModel.pushPullStatus)")
        }
    }

    private func pushPullBar(ratio: Double) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                // Full bar background
                RoundedRectangle(cornerRadius: 4)
                    .fill(GymBroColors.surfaceElevated)

                // Push portion (left)
                let normalizedPush = min(ratio / (ratio + 1), 1.0)
                RoundedRectangle(cornerRadius: 4)
                    .fill(
                        LinearGradient(
                            colors: [GymBroColors.accentCyan.opacity(0.7), GymBroColors.accentCyan],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                    )
                    .frame(width: geo.size.width * normalizedPush)
                    .animation(
                        reduceMotion ? nil : .easeInOut(duration: 0.5),
                        value: ratio
                    )

                // Center marker (ideal 1:1)
                Rectangle()
                    .fill(GymBroColors.textTertiary)
                    .frame(width: 2)
                    .position(x: geo.size.width * 0.5, y: geo.size.height / 2)
            }
        }
        .frame(height: 10)
        .overlay(
            HStack {
                Text("Push")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(GymBroColors.textTertiary)
                Spacer()
                Text("Pull")
                    .font(.system(size: 9, weight: .bold))
                    .foregroundStyle(GymBroColors.textTertiary)
            }
            .offset(y: 12)
        )
        .padding(.bottom, GymBroSpacing.md)
    }

    // MARK: - Color Helpers

    private func colorForScore(_ score: Double) -> Color {
        switch score {
        case 80...100: return GymBroColors.accentGreen
        case 60..<80: return GymBroColors.accentCyan
        case 40..<60: return GymBroColors.accentAmber
        default: return GymBroColors.accentRed
        }
    }

    private func gaugeGradientColors(for score: Double) -> [Color] {
        switch score {
        case 80...100: return [GymBroColors.accentGreen.opacity(0.4), GymBroColors.accentGreen]
        case 60..<80: return [GymBroColors.accentCyan.opacity(0.4), GymBroColors.accentCyan]
        case 40..<60: return [GymBroColors.accentAmber.opacity(0.4), GymBroColors.accentAmber]
        default: return [GymBroColors.accentRed.opacity(0.4), GymBroColors.accentRed]
        }
    }

    private func iconForLabel(_ label: ReadinessLabel) -> String {
        switch label {
        case .excellent: return "bolt.fill"
        case .good: return "checkmark.circle.fill"
        case .moderate: return "exclamationmark.triangle.fill"
        case .poor: return "bed.double.fill"
        }
    }

    private func pushPullStatusColor(ratio: Double) -> Color {
        if ratio >= 0.8 && ratio <= 1.2 { return GymBroColors.accentGreen }
        if ratio > 1.5 || ratio < 0.67 { return GymBroColors.accentRed }
        return GymBroColors.accentAmber
    }
}
