import SwiftUI
import Charts
import GymBroCore

/// Main progress dashboard showing strength trends, volume, and insights.
public struct ProgressDashboardView: View {
    @StateObject private var viewModel = ProgressDashboardViewModel()

    public init() {}

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    timeWindowPicker
                    exercisePicker

                    if viewModel.isLoading {
                        ProgressView("Calculating...")
                    } else {
                        plateauAlerts
                        E1RMChartView(data: viewModel.e1rmData)
                        VolumeChartView(data: viewModel.volumeData)
                        TonnageChartView(data: viewModel.tonnageData)
                        MuscleBalanceChart(data: viewModel.muscleBalance)
                        prTimeline
                    }
                }
                .padding()
            }
            .navigationTitle("Progress")
        }
    }

    // MARK: - Controls

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

    // MARK: - Plateau Alerts

    private var plateauAlerts: some View {
        ForEach(viewModel.plateauAnalyses) { analysis in
            if analysis.isPlateaued {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Plateau Detected", systemImage: "exclamationmark.triangle.fill")
                        .font(.headline)
                        .foregroundColor(.orange)

                    Text("\(analysis.exerciseName) — Score: \(analysis.compositeScore, specifier: "%.0f%%")")
                        .font(.subheadline)

                    ForEach(analysis.recommendations, id: \.self) { rec in
                        Label(rec, systemImage: "lightbulb")
                            .font(.caption)
                    }
                }
                .padding()
                .background(Color.orange.opacity(0.1))
                .cornerRadius(12)
            }
        }
    }

    // MARK: - PR Timeline

    private var prTimeline: some View {
        Group {
            if !viewModel.prEvents.isEmpty {
                VStack(alignment: .leading, spacing: 12) {
                    Text("Personal Records")
                        .font(.headline)

                    ForEach(viewModel.prEvents.suffix(10)) { pr in
                        HStack {
                            Image(systemName: "trophy.fill")
                                .foregroundColor(.yellow)
                            VStack(alignment: .leading) {
                                Text("\(pr.recordType): \(pr.value, specifier: "%.1f") kg")
                                    .font(.subheadline.bold())
                                Text(pr.date, style: .date)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                            Text("+\(pr.value - pr.previousBest, specifier: "%.1f")")
                                .font(.caption.bold())
                                .foregroundColor(.green)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(radius: 1)
            }
        }
    }
}
