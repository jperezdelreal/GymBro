import SwiftUI
import Charts
import GymBroCore

/// Horizontal bar chart showing volume distribution across muscle groups.
public struct MuscleBalanceChart: View {
    public let data: [MuscleGroupBalance]

    public init(data: [MuscleGroupBalance]) {
        self.data = data
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Muscle Group Balance")
                .font(.headline)

            if data.isEmpty {
                emptyState
            } else {
                Chart(data) { item in
                    BarMark(
                        x: .value("Volume %", item.percentage),
                        y: .value("Muscle Group", item.muscleGroup)
                    )
                    .foregroundStyle(by: .value("Muscle Group", item.muscleGroup))
                    .annotation(position: .trailing) {
                        Text("\(item.percentage, specifier: "%.0f")%")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                .chartLegend(.hidden)
                .chartXAxisLabel("% of Total Volume")
                .frame(height: CGFloat(data.count) * 36 + 20)
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .shadow(radius: 1)
    }

    private var emptyState: some View {
        Text("Log workouts with exercises to see muscle balance")
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(height: 150)
            .frame(maxWidth: .infinity)
    }
}
