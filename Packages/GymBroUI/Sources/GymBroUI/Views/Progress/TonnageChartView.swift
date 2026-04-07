import SwiftUI
import Charts
import GymBroCore

/// Area chart showing tonnage (total weight moved) per workout over time.
public struct TonnageChartView: View {
    public let data: [VolumeDataPoint]

    public init(data: [VolumeDataPoint]) {
        self.data = data
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Tonnage per Workout")
                .font(.headline)

            if data.isEmpty {
                emptyState
            } else {
                Chart(data) { point in
                    AreaMark(
                        x: .value("Date", point.periodStart),
                        y: .value("Tonnage (kg)", point.totalTonnage)
                    )
                    .foregroundStyle(.green.opacity(0.3))

                    LineMark(
                        x: .value("Date", point.periodStart),
                        y: .value("Tonnage (kg)", point.totalTonnage)
                    )
                    .foregroundStyle(.green)
                    .interpolationMethod(.catmullRom)
                }
                .chartYAxisLabel("kg")
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
        Text("Log more workouts to see tonnage trends")
            .font(.caption)
            .foregroundColor(.secondary)
            .frame(height: 200)
            .frame(maxWidth: .infinity)
    }

    private var summaryRow: some View {
        HStack {
            if let max = data.max(by: { $0.totalTonnage < $1.totalTonnage }) {
                VStack(alignment: .leading) {
                    Text("Peak Tonnage")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("\(max.totalTonnage, specifier: "%.0f") kg")
                        .font(.title3.bold())
                }
            }
            Spacer()
            VStack(alignment: .trailing) {
                Text("Total Tonnage")
                    .font(.caption)
                    .foregroundColor(.secondary)
                let total = data.reduce(0.0) { $0 + $1.totalTonnage }
                Text("\(total, specifier: "%.0f") kg")
                    .font(.title3.bold())
            }
        }
    }
}
