import SwiftUI
import GymBroCore

public struct RestTimerOverlay: View {
    @State private var timerService = RestTimerService.shared
    @Environment(\.dismiss) private var dismiss
    
    public init() {}
    
    public var body: some View {
        NavigationStack {
            RestTimerView()
                .navigationTitle("Rest Timer")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button {
                            timerService.skip()
                            dismiss()
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .symbolRenderingMode(.hierarchical)
                                .foregroundStyle(.secondary)
                        }
                        .accessibilityLabel("Close timer") // [VERIFY]
                    }
                }
        }
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .onChange(of: timerService.isActive) { _, isActive in
            if !isActive {
                dismiss()
            }
        }
    }
}

#Preview {
    RestTimerOverlay()
        .onAppear {
            RestTimerService.shared.start(
                duration: 90,
                nextSetInfo: NextSetInfo(
                    exerciseName: "Bench Press",
                    setNumber: 4,
                    targetReps: 8,
                    targetWeight: 100.0,
                    weightUnit: "kg"
                )
            )
        }
}
