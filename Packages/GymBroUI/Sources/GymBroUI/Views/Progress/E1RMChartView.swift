import SwiftUI
import Charts
import GymBroCore

/// Line chart showing estimated 1-rep max trends over time.
public struct E1RMChartView: View {
    public let data: [E1RMDataPoint]

    public init(data: [E1RMDataPoint]) {
        self.data = data
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Estimated 1RM")
                .font(.headline)

            if data.isEmpty {
                emptyState
            } else {
                Chart(data) { point in
                    LineMark(
                        x: .value("Date", point.date),
                        y: .value("e1RM (kg)", point.e1rm)
                    )
                    .interpolationMethod(.catmullRom)
                    .foregroundStyle(.blue)

                    PointMark(
                        x: .value("Date", point.date),
                        y: .value("e1RM (kg)", point.e1rm)
                    )
                    .foregroundStyle(.blue)
                    .symbolSize(30)
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
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 1)
    }

    private var emptyState: some View {
        Text("Log more workouts to see e1RM trends")
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(height: 200)
            .frame(maxWidth: .infinity)
    }

    private var summaryRow: some View {
        HStack {
            if let best = data.max(by: { $0.e1rm < $1.e1rm }) {
                VStack(alignment: .leading) {
                    Text("Best e1RM")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("\(best.e1rm, specifier: "%.1f") kg")
                        .font(.title3.bold())
                }
            }
            Spacer()
            if let latest = data.last, let first = data.first {
                let change = latest.e1rm - first.e1rm
                VStack(alignment: .trailing) {
                    Text("Change")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    HStack(spacing: 2) {
                        Image(systemName: change >= 0 ? "arrow.up.right" : "arrow.down.right")
                        Text("\(change >= 0 ? "+" : "")\(change, specifier: "%.1f") kg")
                    }
                    .font(.title3.bold())
                    .foregroundStyle(change >= 0 ? .green : .red)
                }
            }
        }
    }
}
