import SwiftUI
import GymBroCore

/// Displays rest-pause set with expandable sub-sets and mini-rest timer
public struct RestPauseSetView: View {
    let set: ExerciseSet
    let setNumber: Int
    let unitSystem: UnitSystem
    let onAddSubSet: (Int) -> Void
    let onComplete: () -> Void
    
    @State private var isExpanded = false
    @State private var miniRestRemaining: Int = 0
    @State private var isMiniResting = false
    
    @ScaledMetric(relativeTo: .body) private var fontSize: CGFloat = 16
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    
    private let miniRestDuration = 15 // seconds
    
    public init(
        set: ExerciseSet,
        setNumber: Int,
        unitSystem: UnitSystem = .metric,
        onAddSubSet: @escaping (Int) -> Void,
        onComplete: @escaping () -> Void
    ) {
        self.set = set
        self.setNumber = setNumber
        self.unitSystem = unitSystem
        self.onAddSubSet = onAddSubSet
        self.onComplete = onComplete
    }
    
    public var body: some View {
        VStack(alignment: .leading, spacing: GymBroSpacing.sm) {
            // Main row showing total
            Button {
                withAnimation(reduceMotion ? nil : .spring(response: 0.3)) {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    Image(systemName: "timer.circle.fill")
                        .foregroundStyle(GymBroColors.accentPurple)
                    
                    Text("Rest-Pause Set \(setNumber)")
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textPrimary)
                    
                    Spacer()
                    
                    if let subReps = set.subSetReps, !subReps.isEmpty {
                        Text(subReps.map(String.init).joined(separator: "+"))
                            .font(GymBroTypography.monoNumber(size: fontSize))
                            .foregroundStyle(GymBroColors.textSecondary)
                        
                        Text("=")
                            .foregroundStyle(GymBroColors.textTertiary)
                        
                        Text("\(set.totalReps)")
                            .font(GymBroTypography.monoNumber(size: fontSize))
                            .foregroundStyle(GymBroColors.accentPurple)
                    }
                    
                    Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
            .buttonStyle(.plain)
            
            if isExpanded {
                Divider()
                    .background(GymBroColors.border)
                
                // Sub-sets
                if let subReps = set.subSetReps {
                    ForEach(Array(subReps.enumerated()), id: \.offset) { index, reps in
                        subSetRow(index: index + 1, reps: reps)
                    }
                }
                
                // Mini rest timer
                if isMiniResting {
                    miniRestTimerView
                }
                
                // Add another sub-set button
                Button {
                    startMiniRest()
                } label: {
                    HStack {
                        Image(systemName: "plus.circle.fill")
                        Text("Add Sub-Set (10-15s rest)")
                            .font(GymBroTypography.caption)
                    }
                    .foregroundStyle(GymBroColors.accentPurple)
                }
                .disabled(isMiniResting)
                .padding(.vertical, GymBroSpacing.xs)
                
                // Complete rest-pause set
                if set.completedAt == nil {
                    GymBroButton(
                        title: "Complete Rest-Pause Set",
                        style: .primary,
                        action: {
                            HapticFeedbackService.shared.setCompleted()
                            onComplete()
                        }
                    )
                }
            }
        }
        .padding(GymBroSpacing.md)
        .background(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .fill(GymBroColors.surfaceSecondary)
        )
        .overlay(
            RoundedRectangle(cornerRadius: GymBroRadius.md)
                .strokeBorder(GymBroColors.accentPurple.opacity(0.3), lineWidth: 1)
        )
    }
    
    private func subSetRow(index: Int, reps: Int) -> some View {
        HStack {
            Text("Sub-set \(index)")
                .font(GymBroTypography.caption)
                .foregroundStyle(GymBroColors.textSecondary)
            
            Spacer()
            
            Text("\(reps) reps")
                .font(GymBroTypography.monoNumber(size: fontSize - 2))
                .foregroundStyle(GymBroColors.textPrimary)
        }
        .padding(.leading, GymBroSpacing.md)
    }
    
    private var miniRestTimerView: some View {
        HStack {
            ProgressView(value: Double(miniRestDuration - miniRestRemaining), total: Double(miniRestDuration))
                .tint(GymBroColors.accentPurple)
            
            Text("\(miniRestRemaining)s")
                .font(GymBroTypography.monoNumber(size: fontSize))
                .foregroundStyle(GymBroColors.accentPurple)
                .frame(width: 40)
        }
        .padding(.vertical, GymBroSpacing.sm)
    }
    
    private func startMiniRest() {
        miniRestRemaining = miniRestDuration
        isMiniResting = true
        HapticFeedbackService.shared.lightImpact()
        
        Task { @MainActor in
            for _ in 0..<miniRestDuration {
                try? await Task.sleep(for: .seconds(1))
                miniRestRemaining -= 1
                
                if miniRestRemaining <= 0 {
                    isMiniResting = false
                    HapticFeedbackService.shared.warning()
                    // Prompt for next sub-set reps
                    onAddSubSet(0) // Placeholder - will be filled by user input
                    break
                }
            }
        }
    }
}
