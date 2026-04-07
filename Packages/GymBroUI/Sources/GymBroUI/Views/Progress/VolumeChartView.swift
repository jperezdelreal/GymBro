import SwiftUI
import Charts
import GymBroCore

/// Bar chart showing weekly volume load (sets × reps × weight).
public struct VolumeChartView: View, Equatable {
    public let data: [VolumeDataPoint]

    public init(data: [VolumeDataPoint]) {
        self.data = data
    }

    public static func == (lhs: VolumeChartView, rhs: VolumeChartView) -> Bool {
        lhs.data.count == rhs.data.count &&
        lhs.data.indices.allSatisfy {
            lhs.data[$0].periodStart == rhs.data[$0].periodStart &&
            lhs.data[$0].totalVolume == rhs.data[$0].totalVolume &&
            lhs.data[$0].totalSets == rhs.data[$0].totalSets
        }
    }

    private var totalSets: Int {
        data.reduce(0) { $0 + $1.totalSets }
    }

    private var avgVolume: Double {
        data.isEmpty ? 0 : data.reduce(0.0) { $0 + $1.totalVolume } / Double(data.count)
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Weekly Volume")
                .font(.headline)

            if data.isEmpty {
                emptyState
            } else {
                chart
                VolumeSummaryRow(totalSets: totalSets, avgVolume: avgVolume)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 1)
    }

    private var emptyState: some View {
        Text("Log more workouts to see volume trends")
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(height: 200)
            .frame(maxWidth: .infinity)
    }

    private var chart: some View {
        Chart(data) { point in
            BarMark(
                x: .value("Week", point.periodStart, unit: .weekOfYear),
                y: .value("Volume (kg)", point.totalVolume)
            )
            .foregroundStyle(.purple.gradient)
        }
        .chartYAxisLabel("kg")
        .chartXAxis {
            AxisMarks(values: .stride(by: .weekOfYear)) { _ in
                AxisGridLine()
                AxisValueLabel(format: .dateTime.month(.abbreviated).day())
            }
        }
        .frame(height: 200)
    }
}

/// Extracted summary row — lightweight value-driven subview.
private struct VolumeSummaryRow: View, Equatable {
    let totalSets: Int
    let avgVolume: Double

    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Total Sets")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(totalSets)")
                    .font(.title3.bold())
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Avg Weekly Volume")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text("\(avgVolume, specifier: "%.0f") kg")
                    .font(.title3.bold())
            }
        }
    }
}
