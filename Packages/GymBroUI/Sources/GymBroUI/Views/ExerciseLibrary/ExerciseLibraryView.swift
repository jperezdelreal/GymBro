import SwiftUI
import SwiftData
import GymBroCore

public struct ExerciseLibraryView: View {
    @State private var viewModel = ExerciseLibraryViewModel()
    @Environment(\.modelContext) private var modelContext
    @State private var searchText = ""
    @State private var showCreateExercise = false
    @State private var showFilterSheet = false
    @State private var selectedMuscleFilter: String?
    @State private var selectedCategory: ExerciseCategory?
    @State private var selectedEquipment: Equipment?

    private let quickMuscleGroups = [
        "Chest", "Back", "Shoulders", "Legs", "Arms", "Core"
    ]

    public init() {}

    public var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    ProgressView("Loading exercises…")
                } else if let error = viewModel.errorMessage {
                    ContentUnavailableView {
                        Label("Something Went Wrong", systemImage: "exclamationmark.triangle")
                    } description: {
                        Text(error)
                    } actions: {
                        Button("Retry") { viewModel.retry() }
                    }
                } else if viewModel.exercises.isEmpty {
                    ContentUnavailableView {
                        Label("No Exercises", systemImage: "dumbbell")
                    } description: {
                        Text("Add your first exercise to get started.")
                    } actions: {
                        Button("Add Exercise") { showCreateExercise = true }
                            .buttonStyle(.gymBroPrimary)
                            .frame(maxWidth: 200)
                    }
                } else {
                    VStack(spacing: 0) {
                        muscleChipBar

                        List(filteredExercises) { exercise in
                            NavigationLink(destination: ExerciseDetailView(exercise: exercise)) {
                                ExerciseLibraryRow(exercise: exercise)
                            }
                            .listRowBackground(GymBroColors.background)
                            .listRowSeparatorTint(GymBroColors.border)
                        }
                        .listStyle(.plain)
                        .scrollContentBackground(.hidden)
                    }
                }
            }
            .searchable(text: $searchText)
            .navigationTitle("Exercise Library")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    HStack(spacing: GymBroSpacing.md) {
                        Button {
                            showFilterSheet = true
                        } label: {
                            Image(systemName: hasActiveFilters ? "line.3.horizontal.decrease.circle.fill" : "line.3.horizontal.decrease.circle")
                                .foregroundStyle(hasActiveFilters ? GymBroColors.accentGreen : GymBroColors.textSecondary)
                        }
                        .accessibilityLabel("Filter")

                        Button {
                            showCreateExercise = true
                        } label: {
                            Image(systemName: "plus")
                                .foregroundStyle(GymBroColors.accentGreen)
                        }
                        .accessibilityLabel("Add Exercise")
                    }
                }
            }
            .sheet(isPresented: $showCreateExercise, onDismiss: {
                viewModel.retry()
            }) {
                CreateExerciseView()
            }
            .sheet(isPresented: $showFilterSheet) {
                filterSheet
            }
            .task {
                viewModel.setup(modelContext: modelContext)
            }
        }
    }

    // MARK: - Filtered Results

    private var filteredExercises: [Exercise] {
        viewModel.filteredExercises(
            searchText: selectedMuscleFilter ?? searchText,
            category: selectedCategory,
            equipment: selectedEquipment
        )
    }

    private var hasActiveFilters: Bool {
        selectedCategory != nil || selectedEquipment != nil
    }

    // MARK: - Muscle Chip Bar

    private var muscleChipBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: GymBroSpacing.sm) {
                chipButton(label: "All", isSelected: selectedMuscleFilter == nil) {
                    selectedMuscleFilter = nil
                }

                ForEach(quickMuscleGroups, id: \.self) { group in
                    chipButton(label: group, isSelected: selectedMuscleFilter == group) {
                        selectedMuscleFilter = selectedMuscleFilter == group ? nil : group
                    }
                }
            }
            .padding(.horizontal, GymBroSpacing.md)
            .padding(.vertical, GymBroSpacing.sm)
        }
        .background(GymBroColors.surfacePrimary)
    }

    @ViewBuilder
    private func chipButton(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(GymBroTypography.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .foregroundStyle(isSelected ? GymBroColors.background : GymBroColors.textSecondary)
                .padding(.horizontal, GymBroSpacing.md)
                .padding(.vertical, GymBroSpacing.sm)
                .background(
                    Capsule()
                        .fill(isSelected ? GymBroColors.accentGreen : GymBroColors.surfaceElevated)
                )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Filter Sheet

    @ViewBuilder
    private var filterSheet: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(ExerciseCategory.allCases, id: \.self) { category in
                        Button {
                            selectedCategory = selectedCategory == category ? nil : category
                        } label: {
                            HStack {
                                Text(category.rawValue.capitalized)
                                    .foregroundStyle(GymBroColors.textPrimary)
                                Spacer()
                                if selectedCategory == category {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(GymBroColors.accentGreen)
                                }
                            }
                        }
                    }
                } header: {
                    Text("Category")
                }

                Section {
                    ForEach(Equipment.allCases, id: \.self) { equip in
                        Button {
                            selectedEquipment = selectedEquipment == equip ? nil : equip
                        } label: {
                            HStack {
                                Text(equip.rawValue.capitalized)
                                    .foregroundStyle(GymBroColors.textPrimary)
                                Spacer()
                                if selectedEquipment == equip {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(GymBroColors.accentGreen)
                                }
                            }
                        }
                    }
                } header: {
                    Text("Equipment")
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        selectedCategory = nil
                        selectedEquipment = nil
                    }
                    .foregroundStyle(GymBroColors.accentRed)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") {
                        showFilterSheet = false
                    }
                    .foregroundStyle(GymBroColors.accentGreen)
                }
            }
        }
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
}
