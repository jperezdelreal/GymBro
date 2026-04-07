import SwiftUI
import Charts
import GymBroCore

/// Main progress dashboard — Apple Health–style summary → drill-down.
/// Hero metric at top, swipeable insight cards, charts expand on demand.
public struct ProgressDashboardView: View {
    @State private var viewModel = ProgressDashboardViewModel()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .body) private var sparklineHeight: CGFloat = 60

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: GymBroSpacing.lg) {
                    controlsSection
                    contentSection
                }
                .padding(.horizontal, GymBroSpacing.md)
                .padding(.bottom, GymBroSpacing.xl)
            }
            .navigationTitle("Progress")
            .gymBroDarkBackground()
        }
    }

    // MARK: - Controls

    private var controlsSection: some View {
        VStack(spacing: GymBroSpacing.sm) {
            timeWindowPicker
            exercisePicker
        }
    }

    private var timeWindowPicker: some View {
        Picker("Time Range", selection: $viewModel.selectedTimeWindow) {
            ForEach(TimeWindow.allCases, id: \.self) { window in
                Text(window.rawValue).tag(window)
            }
        }
        .pickerStyle(.segmented)
    }

    private var exercisePicker: some View {
        Group {
            if !viewModel.availableExercises.isEmpty {
                Picker("Exercise", selection: Binding(
                    get: { viewModel.selectedExerciseName ?? "" },
                    set: { viewModel.selectedExerciseName = $0.isEmpty ? nil : $0 }
                )) {
                    ForEach(viewModel.availableExercises, id: \.self) { name in
                        Text(name).tag(name)
                    }
                }
            }
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var contentSection: some View {
        if viewModel.isLoading {
            ProgressView("Calculating...")
                .frame(maxWidth: .infinity, minHeight: 200)
        } else if viewModel.availableExercises.isEmpty && viewModel.volumeData.isEmpty {
            ContentUnavailableView {
                Label("No Progress Data", systemImage: "chart.bar")
            } description: {
                Text("Complete a few workouts to see your progress trends here.")
            }
        } else {
            heroSection
            insightCards
            expandedChartSection
        }
    }

    // MARK: - Hero Metric

    private var heroSection: some View {
        GymBroCard {
            VStack(spacing: GymBroSpacing.sm) {
                Text(viewModel.heroLabel.uppercased())
                    .font(GymBroTypography.caption2)
                    .foregroundStyle(GymBroColors.textTertiary)
                    .tracking(2)

                HeroNumber(
                    value: viewModel.heroValue,
                    format: "%.1f",
                    unit: viewModel.heroUnit,
                    trend: viewModel.heroTrend
                )

                if !viewModel.sparklineValues.isEmpty {
                    SparklineView(
                        data: viewModel.sparklineValues,
                        color: viewModel.heroTrend == .down
                            ? GymBroColors.accentRed
                            : GymBroColors.accentGreen
                    )
                    .frame(height: sparklineHeight)
                    .padding(.horizontal, GymBroSpacing.md)
                }

                if let exercise = viewModel.selectedExerciseName {
                    Text(exercise)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textSecondary)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, GymBroSpacing.sm)
        }
    }

    // MARK: - Insight Cards

    @ViewBuilder
    private var insightCards: some View {
        if !viewModel.insights.isEmpty {
            TabView {
                ForEach(viewModel.insights) { insight in
                    InsightCardView(insight: insight) {
                        withAnimation(reduceMotion ? .none : .spring(response: 0.35, dampingFraction: 0.85)) {
                            viewModel.toggleChart(insight.chartType)
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.xs)
                }
            }
            .tabViewStyle(.page(indexDisplayMode: .automatic))
            .frame(height: 130)
        }
    }

    // MARK: - Expanded Chart

    @ViewBuilder
    private var expandedChartSection: some View {
        if let chart = viewModel.expandedChart {
            Group {
                switch chart {
                case .e1rm:
                    E1RMChartView(data: viewModel.e1rmData)
                case .volume:
                    VolumeChartView(data: viewModel.volumeData)
                case .tonnage:
                    TonnageChartView(data: viewModel.tonnageData)
                case .muscleBalance:
                    MuscleBalanceChart(data: viewModel.muscleBalance)
                case .plateau:
                    PlateauDetailSection(analyses: viewModel.plateauAnalyses)
                case .prs:
                    PRTimelineSection(events: viewModel.prEvents)
                }
            }
            .transition(.asymmetric(
                insertion: .move(edge: .bottom).combined(with: .opacity),
                removal: .opacity
            ))

            Button {
                withAnimation(reduceMotion ? .none : .spring(response: 0.3, dampingFraction: 0.8)) {
                    viewModel.expandedChart = nil
                }
            } label: {
                Label("Collapse", systemImage: "chevron.up")
                    .font(GymBroTypography.caption)
                    .foregroundStyle(GymBroColors.textTertiary)
            }
        }
    }
}

// MARK: - Sparkline

/// Minimal sparkline for the hero section — no axes, just the trend shape.
struct SparklineView: View {
    let data: [Double]
    var color: Color = GymBroColors.accentGreen

    var body: some View {
        Chart(Array(data.enumerated()), id: \.offset) { index, value in
            LineMark(
                x: .value("Index", index),
                y: .value("Value", value)
            )
            .interpolationMethod(.catmullRom)
            .foregroundStyle(color)

            AreaMark(
                x: .value("Index", index),
                y: .value("Value", value)
            )
            .interpolationMethod(.catmullRom)
            .foregroundStyle(color.opacity(0.15))
        }
        .chartXAxis(.hidden)
        .chartYAxis(.hidden)
        .chartLegend(.hidden)
    }
}

// MARK: - Insight Card

/// Single insight card — tappable, shows icon + text in GymBroCard.
struct InsightCardView: View {
    let insight: ProgressInsight
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            GymBroCard(accent: insight.accentColor) {
                HStack(spacing: GymBroSpacing.md) {
                    Image(systemName: insight.icon)
                        .font(.title2)
                        .foregroundStyle(insight.accentColor)
                        .frame(width: 36)

                    VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                        Text(insight.title)
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.textPrimary)
                            .lineLimit(1)

                        Text(insight.subtitle)
                            .font(GymBroTypography.caption)
                            .foregroundStyle(GymBroColors.textSecondary)
                            .lineLimit(2)
                    }

                    Spacer(minLength: 0)

                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(insight.title). \(insight.subtitle). Tap to see chart.")
    }
}

// MARK: - Plateau Detail

private struct PlateauDetailSection: View {
    let analyses: [PlateauAnalysis]

    var body: some View {
        ForEach(analyses) { analysis in
            if analysis.isPlateaued {
                GymBroCard(accent: GymBroColors.accentAmber) {
                    VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
                        Label("Plateau Detected", systemImage: "exclamationmark.triangle.fill")
                            .font(GymBroTypography.headline)
                            .foregroundStyle(GymBroColors.accentAmber)

                        Text("\(analysis.exerciseName) — Score: \(analysis.compositeScore, specifier: "%.0f%%")")
                            .font(GymBroTypography.subheadline)
                            .foregroundStyle(GymBroColors.textPrimary)

                        ForEach(analysis.recommendations, id: \.self) { rec in
                            Label(rec, systemImage: "lightbulb")
                                .font(GymBroTypography.caption)
                                .foregroundStyle(GymBroColors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - PR Timeline

private struct PRTimelineSection: View {
    let events: [PREvent]

    var body: some View {
        if !events.isEmpty {
            GymBroCard(accent: GymBroColors.accentAmber) {
                VStack(alignment: .leading, spacing: GymBroSpacing.md) {
                    Text("Personal Records")
                        .font(GymBroTypography.headline)
                        .foregroundStyle(GymBroColors.textPrimary)

                    ForEach(events.suffix(10)) { pr in
                        HStack {
                            Image(systemName: "trophy.fill")
                                .foregroundStyle(GymBroColors.accentAmber)
                            VStack(alignment: .leading) {
                                Text("\(pr.recordType): \(pr.value, specifier: "%.1f") kg")
                                    .font(GymBroTypography.subheadline.bold())
                                    .foregroundStyle(GymBroColors.textPrimary)
                                Text(pr.date, style: .date)
                                    .font(GymBroTypography.caption)
                                    .foregroundStyle(GymBroColors.textSecondary)
                            }
                            Spacer()
                            HStack(spacing: 2) {
                                Image(systemName: "arrow.up.right")
                                Text("+\(pr.value - pr.previousBest, specifier: "%.1f")")
                            }
                            .font(GymBroTypography.caption.bold())
                            .foregroundStyle(GymBroColors.accentGreen)
                        }
                    }
                }
            }
        }
    }
}
