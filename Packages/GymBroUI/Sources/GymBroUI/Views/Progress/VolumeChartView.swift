import SwiftUI
import Charts
import GymBroCore

/// Bar chart showing weekly volume load (sets × reps × weight).
public struct VolumeChartView: View {
    public let data: [VolumeDataPoint]

    public init(data: [VolumeDataPoint]) {
        self.data = data
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Weekly Volume")
                .font(.headline)

            if data.isEmpty {
                emptyState
            } else {
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

                summaryRow
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 1)
    }

    private var emptyState: some View {
        Text("Log more workouts to see volume trends")
            .font(.caption)
            .foregroundColor(.secondary)
            .frame(height: 200)
            .frame(maxWidth: .infinity)
    }

    private var summaryRow: some View {
        HStack {
            VStack(alignment: .leading) {
                Text("Total Sets")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text("\(data.reduce(0) { $0 + $1.totalSets })")
                    .font(.title3.bold())
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Avg Weekly Volume")
                    .font(.caption)
                    .foregroundColor(.secondary)
                let avgVolume = data.isEmpty ? 0 : data.reduce(0.0) { $0 + $1.totalVolume } / Double(data.count)
                Text("\(avgVolume, specifier: "%.0f") kg")
                    .font(.title3.bold())
            }
        }
    }
}
