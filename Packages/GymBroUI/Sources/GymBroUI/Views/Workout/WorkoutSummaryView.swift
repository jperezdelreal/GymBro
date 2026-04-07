import SwiftUI
import GymBroCore

public struct WorkoutSummaryView: View {
    let summary: WorkoutSummary
    @Environment(\.dismiss) private var dismiss

    @ScaledMetric(relativeTo: .largeTitle) private var celebrationIconSize: CGFloat = 80
    @ScaledMetric(relativeTo: .title) private var titleSize: CGFloat = 32
    @ScaledMetric(relativeTo: .title2) private var cardIconSize: CGFloat = 28
    @ScaledMetric(relativeTo: .title2) private var cardValueSize: CGFloat = 24

    public init(summary: WorkoutSummary) {
        self.summary = summary
    }
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    // Celebration icon
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: celebrationIconSize))
                        .foregroundStyle(.green)
                        .padding(.top, 40)
                        .accessibilityHidden(true)
                    
                    Text("Workout Complete!")
                        .font(.system(size: titleSize, weight: .bold))
                    
                    // Stats grid
                    VStack(spacing: 20) {
                        summaryCard(
                            icon: "clock.fill",
                            title: "Duration",
                            value: formatDuration(summary.duration),
                            color: .blue
                        )
                        
                        summaryCard(
                            icon: "scalemass.fill",
                            title: "Total Volume",
                            value: String(format: "%.0f kg", summary.totalVolume),
                            color: .purple
                        )
                        
                        summaryCard(
                            icon: "chart.bar.fill",
                            title: "Sets Completed",
                            value: "\(summary.totalSets)",
                            color: .orange
                        )
                        
                        if summary.personalRecords > 0 {
                            summaryCard(
                                icon: "star.fill",
                                title: "Personal Records",
                                value: "\(summary.personalRecords)",
                                color: .yellow
                            )
                        }
                    }
                    .padding(.horizontal, 24)
                    
                    // Action buttons
                    VStack(spacing: 16) {
                        Button {
                            dismiss()
                        } label: {
                            Text("Done")
                                .font(.headline)
                                .foregroundStyle(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 16)
                                .background(Color.blue)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 20)
                }
                .padding(.bottom, 40)
            }
            .navigationTitle("Summary")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
    
    private func summaryCard(icon: String, title: String, value: String, color: Color) -> some View {
        HStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: cardIconSize))
                .foregroundStyle(color)
                .frame(width: 50)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.system(size: cardValueSize, weight: .bold, design: .rounded))
            }
            
            Spacer()
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(color.opacity(0.1))
        )
    }
    
    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60
        
        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }
}
