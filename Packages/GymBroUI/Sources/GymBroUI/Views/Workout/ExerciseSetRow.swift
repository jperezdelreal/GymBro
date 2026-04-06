import SwiftUI
import GymBroCore

public struct ExerciseSetRow: View {
    let set: ExerciseSet
    let setNumber: Int
    let unitSystem: UnitSystem
    let onTap: () -> Void
    
    public init(
        set: ExerciseSet,
        setNumber: Int,
        unitSystem: UnitSystem = .metric,
        onTap: @escaping () -> Void = {}
    ) {
        self.set = set
        self.setNumber = setNumber
        self.unitSystem = unitSystem
        self.onTap = onTap
    }
    
    private var weightString: String {
        let weight = set.weightInUnit(unitSystem)
        let unit = unitSystem == .metric ? "kg" : "lb"
        return String(format: "%.1f %@", weight, unit)
    }
    
    public var body: some View {
        HStack(spacing: 16) {
            // Set number
            Text("\(setNumber)")
                .font(.system(size: 20, weight: .bold, design: .rounded))
                .foregroundStyle(set.isWarmup ? .secondary : .primary)
                .frame(width: 40)
            
            // Weight
            VStack(alignment: .leading, spacing: 2) {
                Text(weightString)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(set.completedAt != nil ? .primary : .secondary)
                Text("Weight")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            // Reps
            VStack(alignment: .leading, spacing: 2) {
                Text("\(set.reps)")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(set.completedAt != nil ? .primary : .secondary)
                Text("Reps")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(width: 60, alignment: .leading)
            
            // RPE
            if let rpe = set.rpe {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%.0f", rpe))
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(.orange)
                    Text("RPE")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                }
                .frame(width: 50, alignment: .leading)
            }
            
            // Completion indicator
            if set.completedAt != nil {
                Image(systemName: "checkmark.circle.fill")
                    .font(.system(size: 24))
                    .foregroundStyle(.green)
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 16)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(set.completedAt != nil ? Color.green.opacity(0.1) : Color(.systemGray6))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .strokeBorder(
                    set.isWarmup ? Color.orange.opacity(0.3) : Color.clear,
                    lineWidth: 2
                )
        )
        .contentShape(Rectangle())
        .onTapGesture {
            onTap()
        }
    }
}
