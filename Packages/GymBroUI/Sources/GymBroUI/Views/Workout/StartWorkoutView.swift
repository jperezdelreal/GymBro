import SwiftUI
import SwiftData
import GymBroCore
import os

public struct StartWorkoutView: View {
    private static let logger = Logger(subsystem: "com.gymbro", category: "StartWorkout")

    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    
    @State private var selectedProgram: Program?
    @State private var selectedProgramDay: ProgramDay?
    @State private var showingActiveWorkout = false
    @State private var activeWorkoutViewModel: ActiveWorkoutViewModel?
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 24) {
                    // Quick Start
                    quickStartSection
                    
                    // From Program
                    programSection
                }
                .padding(.horizontal, 16)
                .padding(.top, 20)
            }
            .navigationTitle("Start Workout")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .navigationDestination(isPresented: $showingActiveWorkout) {
                if let viewModel = activeWorkoutViewModel {
                    ActiveWorkoutView(viewModel: viewModel)
                }
            }
        }
    }
    
    private var quickStartSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Quick Start")
                .font(.title2)
                .fontWeight(.bold)
            
            Button {
                startEmptyWorkout()
            } label: {
                HStack {
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Image(systemName: "dumbbell.fill")
                                .font(.title2)
                            Text("Empty Workout")
                                .font(.title3)
                                .fontWeight(.semibold)
                        }
                        
                        Text("Start logging without a program")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    
                    Spacer()
                    
                    Image(systemName: "arrow.right")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .padding(20)
                .background(
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color.blue.opacity(0.1))
                )
            }
            .buttonStyle(.plain)
        }
    }
    
    private var programSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("From Program")
                .font(.title2)
                .fontWeight(.bold)
            
            Text("Select a program day to begin")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            
            // Placeholder for program selection
            VStack(spacing: 12) {
                ForEach(0..<3) { index in
                    programDayCard(
                        title: "Day \(index + 1) - Push",
                        exercises: "Bench Press, Overhead Press, Dips",
                        onSelect: {
                            // TODO: Implement program day selection
                        }
                    )
                }
            }
        }
    }
    
    private func programDayCard(title: String, exercises: String, onSelect: @escaping () -> Void) -> some View {
        Button {
            onSelect()
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 8) {
                    Text(title)
                        .font(.headline)
                    Text(exercises)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .foregroundStyle(.secondary)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemGray6))
            )
        }
        .buttonStyle(.plain)
    }
    
    private func startEmptyWorkout() {
        let workout = Workout()
        workout.isActive = true
        modelContext.insert(workout)
        
        do {
            try modelContext.save()
            activeWorkoutViewModel = ActiveWorkoutViewModel(
                modelContext: modelContext,
                workout: workout,
                exercises: []
            )
            showingActiveWorkout = true
        } catch {
            Self.logger.error("Failed to create workout: \(error.localizedDescription)")
        }
    }
}
