import SwiftUI
import Charts
import GymBroCore

/// Recovery dashboard showing the readiness gauge, factor breakdown, and 7-day trend.
public struct ReadinessScoreView: View {
    let score: ReadinessScore?
    let trend: [ReadinessScore]

    public init(score: ReadinessScore? = nil, trend: [ReadinessScore] = []) {
        self.score = score
        self.trend = trend
    }

    public var body: some View {
        VStack(spacing: 20) {
            if let score {
                gaugeSection(score)
                factorBreakdown(score)
                recommendationCard(score)
                if !trend.isEmpty {
                    trendChart
                }
            } else {
                emptyState
            }
        }
    }

    // MARK: - Gauge

    private func gaugeSection(_ score: ReadinessScore) -> some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .trim(from: 0, to: 0.75)
                    .stroke(Color(.systemGray5), lineWidth: 16)
                    .rotationEffect(.degrees(135))

                Circle()
                    .trim(from: 0, to: 0.75 * score.overallScore / 100.0)
                    .stroke(
                        colorForScore(score.overallScore),
                        style: StrokeStyle(lineWidth: 16, lineCap: .round)
                    )
                    .rotationEffect(.degrees(135))
                    .animation(.easeInOut(duration: 0.8), value: score.overallScore)

                VStack(spacing: 4) {
                    Text("\(Int(score.overallScore))")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundStyle(colorForScore(score.overallScore))
                    Text(score.label.displayName)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 200, height: 200)

            Text("Recovery Score")
                .font(.headline)
        }
        .padding()
    }

    // MARK: - Factor Breakdown

    private func factorBreakdown(_ score: ReadinessScore) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Factor Breakdown")
                .font(.headline)

            factorRow("Sleep", score: score.sleepScore, icon: "bed.double.fill", weight: "35%")
            factorRow("HRV", score: score.hrvScore, icon: "heart.text.square.fill", weight: "30%")
            factorRow("Resting HR", score: score.restingHRScore, icon: "heart.fill", weight: "15%")
            factorRow("Training Load", score: score.trainingLoadScore, icon: "figure.strengthtraining.traditional", weight: "15%")

            if let subjective = score.subjectiveScore {
                factorRow("Subjective", score: subjective, icon: "person.fill.questionmark", weight: "5%")
            }
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func factorRow(_ name: String, score: Double, icon: String, weight: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundStyle(colorForScore(score))
                .frame(width: 24)

            Text(name)
                .font(.subheadline)

            Spacer()

            Text(weight)
                .font(.caption)
                .foregroundStyle(.secondary)

            ProgressView(value: score, total: 100)
                .tint(colorForScore(score))
                .frame(width: 60)

            Text("\(Int(score))")
                .font(.subheadline.monospacedDigit().bold())
                .foregroundStyle(colorForScore(score))
                .frame(width: 30, alignment: .trailing)
        }
    }

    // MARK: - Recommendation

    private func recommendationCard(_ score: ReadinessScore) -> some View {
        HStack(spacing: 12) {
            Image(systemName: iconForLabel(score.label))
                .font(.title2)
                .foregroundStyle(colorForScore(score.overallScore))

            VStack(alignment: .leading, spacing: 4) {
                Text("Today's Recommendation")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Text(score.recommendation)
                    .font(.subheadline)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colorForScore(score.overallScore).opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Trend Chart

    private var trendChart: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("7-Day Trend")
                .font(.headline)

            Chart(trend, id: \.date) { entry in
                LineMark(
                    x: .value("Date", entry.date, unit: .day),
                    y: .value("Score", entry.overallScore)
                )
                .foregroundStyle(Color.blue.gradient)
                .interpolationMethod(.catmullRom)

                AreaMark(
                    x: .value("Date", entry.date, unit: .day),
                    y: .value("Score", entry.overallScore)
                )
                .foregroundStyle(Color.blue.opacity(0.1).gradient)
                .interpolationMethod(.catmullRom)

                PointMark(
                    x: .value("Date", entry.date, unit: .day),
                    y: .value("Score", entry.overallScore)
                )
                .foregroundStyle(colorForScore(entry.overallScore))
            }
            .chartYScale(domain: 0...100)
            .chartYAxis {
                AxisMarks(values: [0, 25, 50, 75, 100])
            }
            .frame(height: 180)
        }
        .padding()
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    // MARK: - Empty State

    private var emptyState: some View {
        ContentUnavailableView {
            Label("No Recovery Data", systemImage: "heart.text.square")
        } description: {
            Text("Recovery scores will appear here once HealthKit data is available. Enable Health access in Settings.")
        }
    }

    // MARK: - Helpers

    private func colorForScore(_ score: Double) -> Color {
        switch score {
        case 90...100: return .green
        case 70..<90: return .blue
        case 50..<70: return .orange
        default: return .red
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
}
