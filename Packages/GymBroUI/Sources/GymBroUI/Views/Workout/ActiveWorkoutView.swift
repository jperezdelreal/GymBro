import SwiftUI
import GymBroCore

public struct ActiveWorkoutView: View {
    @State private var viewModel: ActiveWorkoutViewModel
    @State private var showingExercisePicker = false
    @State private var showingRPEPicker = false
    @State private var showingSummary = false
    @State private var workoutSummary: WorkoutSummary?
    
    private let unitSystem: UnitSystem
    
    public init(viewModel: ActiveWorkoutViewModel, unitSystem: UnitSystem = .metric) {
        self._viewModel = State(initialValue: viewModel)
        self.unitSystem = unitSystem
    }
    
    public var body: some View {
        ZStack {
            VStack(spacing: 0) {
                // Header
                workoutHeader
                
                ScrollView {
                    VStack(spacing: 24) {
                        // Current exercise
                        if let exercise = viewModel.activeExercise {
                            currentExerciseSection(exercise)
                        } else {
                            emptyStateView
                        }
                        
                        // Completed sets history
                        if !viewModel.completedSetsForActiveExercise.isEmpty {
                            completedSetsSection
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 20)
                    .padding(.bottom, 200)
                }
                
                Spacer()
            }
            
            // Bottom action bar
            VStack {
                Spacer()
                bottomActionBar
            }
        }
        .navigationTitle("Active Workout")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("Finish") {
                    finishWorkout()
                }
                .foregroundStyle(.red)
                .disabled(!viewModel.isWorkoutStarted)
            }
        }
        .sheet(isPresented: $showingSummary) {
            if let summary = workoutSummary {
                WorkoutSummaryView(summary: summary)
            }
        }
        .task {
            HapticFeedbackService.shared.prepare()
        }
    }
    
    private var workoutHeader: some View {
        VStack(spacing: 8) {
            HStack(spacing: 24) {
                statItem(
                    title: "Duration",
                    value: formatDuration(viewModel.workoutDuration)
                )
                
                statItem(
                    title: "Volume",
                    value: String(format: "%.0f kg", viewModel.totalVolume)
                )
                
                statItem(
                    title: "Sets",
                    value: "\(viewModel.totalCompletedSets)"
                )
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color(.systemGray6))
    }
    
    private func statItem(title: String, value: String) -> some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.system(size: 20, weight: .bold, design: .rounded))
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
    
    private func currentExerciseSection(_ exercise: Exercise) -> some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(exercise.name)
                        .font(.system(size: 24, weight: .bold))
                    Text("Set \(viewModel.activeSetNumber)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
            }
            
            // Weight & Reps adjusters
            HStack(spacing: 20) {
                // Weight
                VStack(alignment: .leading, spacing: 8) {
                    Text("Weight")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    
                    HStack(spacing: 12) {
                        Button {
                            adjustWeight(-2.5)
                        } label: {
                            Image(systemName: "minus.circle.fill")
                                .font(.system(size: 36))
                                .foregroundStyle(.blue)
                        }
                        
                        Text(formatWeight(viewModel.currentWeight))
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .frame(minWidth: 100)
                            .multilineTextAlignment(.center)
                        
                        Button {
                            adjustWeight(2.5)
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.system(size: 36))
                                .foregroundStyle(.blue)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                
                // Reps
                VStack(alignment: .leading, spacing: 8) {
                    Text("Reps")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    
                    HStack(spacing: 12) {
                        Button {
                            viewModel.updateReps(viewModel.currentReps - 1)
                            HapticFeedbackService.shared.valueChanged()
                        } label: {
                            Image(systemName: "minus.circle.fill")
                                .font(.system(size: 36))
                                .foregroundStyle(.blue)
                        }
                        
                        Text("\(viewModel.currentReps)")
                            .font(.system(size: 28, weight: .bold, design: .rounded))
                            .frame(minWidth: 60)
                            .multilineTextAlignment(.center)
                        
                        Button {
                            viewModel.updateReps(viewModel.currentReps + 1)
                            HapticFeedbackService.shared.valueChanged()
                        } label: {
                            Image(systemName: "plus.circle.fill")
                                .font(.system(size: 36))
                                .foregroundStyle(.blue)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
            }
            
            // Warmup toggle & RPE
            HStack(spacing: 16) {
                Button {
                    viewModel.toggleWarmup()
                    HapticFeedbackService.shared.lightImpact()
                } label: {
                    HStack {
                        Image(systemName: viewModel.isWarmup ? "checkmark.circle.fill" : "circle")
                        Text("Warmup Set")
                    }
                    .font(.subheadline)
                    .foregroundStyle(viewModel.isWarmup ? .orange : .secondary)
                }
                
                Spacer()
                
                Button {
                    showingRPEPicker = true
                } label: {
                    HStack {
                        Image(systemName: "gauge.with.needle")
                        if let rpe = viewModel.currentRPE {
                            Text("RPE \(Int(rpe))")
                        } else {
                            Text("Add RPE")
                        }
                    }
                    .font(.subheadline)
                    .foregroundStyle(viewModel.currentRPE != nil ? .orange : .secondary)
                }
            }
        }
        .padding(20)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.1), radius: 8, y: 4)
        )
        .confirmationDialog("Select RPE", isPresented: $showingRPEPicker) {
            ForEach(6...10, id: \.self) { rpe in
                Button("RPE \(rpe)") {
                    viewModel.updateRPE(Double(rpe))
                }
            }
            Button("Clear RPE", role: .destructive) {
                viewModel.updateRPE(nil)
            }
        }
    }
    
    private var completedSetsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Completed Sets")
                .font(.headline)
                .foregroundStyle(.secondary)
            
            ForEach(Array(viewModel.completedSetsForActiveExercise.enumerated()), id: \.element.id) { index, set in
                ExerciseSetRow(
                    set: set,
                    setNumber: set.setNumber,
                    unitSystem: unitSystem
                )
            }
        }
    }
    
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "dumbbell")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
            
            Text("Add an exercise to begin")
                .font(.headline)
                .foregroundStyle(.secondary)
            
            Button {
                showingExercisePicker = true
            } label: {
                Label("Add Exercise", systemImage: "plus.circle.fill")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .clipShape(Capsule())
            }
        }
        .padding(.top, 60)
    }
    
    private var bottomActionBar: some View {
        VStack(spacing: 12) {
            // Rest timer
            if viewModel.isRestTimerActive, let endTime = viewModel.restTimerEndTime {
                RestTimerView(endTime: endTime) {
                    viewModel.skipRestTimer()
                }
            }
            
            // Complete set button
            Button {
                viewModel.completeSet()
            } label: {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 24))
                    Text("Complete Set")
                        .font(.system(size: 20, weight: .semibold))
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 60)
                .background(
                    viewModel.activeExercise != nil ? Color.green : Color.gray
                )
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .shadow(color: .black.opacity(0.2), radius: 8, y: 4)
            }
            .disabled(viewModel.activeExercise == nil)
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 32)
        .background(
            Color(.systemBackground)
                .ignoresSafeArea(edges: .bottom)
        )
    }
    
    private func adjustWeight(_ delta: Double) {
        viewModel.updateWeight(max(0, viewModel.currentWeight + delta))
        HapticFeedbackService.shared.valueChanged()
    }
    
    private func formatWeight(_ kg: Double) -> String {
        let weight = unitSystem == .metric ? kg : kg * 2.20462
        let unit = unitSystem == .metric ? "kg" : "lb"
        return String(format: "%.1f %@", weight, unit)
    }
    
    private func formatDuration(_ duration: TimeInterval) -> String {
        let hours = Int(duration) / 3600
        let minutes = Int(duration) / 60 % 60
        let seconds = Int(duration) % 60
        
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            return String(format: "%d:%02d", minutes, seconds)
        }
    }
    
    private func finishWorkout() {
        workoutSummary = viewModel.finishWorkout()
        showingSummary = true
    }
}

struct RestTimerView: View {
    let endTime: Date
    let onSkip: () -> Void
    
    @State private var remainingTime: TimeInterval = 0
    private let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var body: some View {
        HStack {
            Image(systemName: "timer")
                .font(.system(size: 20))
            
            Text("Rest: \(formatTime(remainingTime))")
                .font(.system(size: 18, weight: .semibold, design: .rounded))
            
            Spacer()
            
            Button("Skip") {
                onSkip()
            }
            .font(.subheadline)
            .foregroundStyle(.blue)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 12)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(Color.blue.opacity(0.1))
        )
        .onReceive(timer) { _ in
            remainingTime = max(0, endTime.timeIntervalSinceNow)
            if remainingTime == 0 {
                HapticFeedbackService.shared.mediumImpact()
            }
        }
        .onAppear {
            remainingTime = endTime.timeIntervalSinceNow
        }
    }
    
    private func formatTime(_ seconds: TimeInterval) -> String {
        let mins = Int(seconds) / 60
        let secs = Int(seconds) % 60
        return String(format: "%d:%02d", mins, secs)
    }
}
