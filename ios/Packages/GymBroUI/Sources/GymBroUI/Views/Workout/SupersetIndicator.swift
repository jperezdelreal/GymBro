import SwiftUI
import GymBroCore

/// Visual indicator showing that exercises are part of a superset
public struct SupersetIndicator: View {
    let exercises: [Exercise]
    let currentExercise: Exercise
    
    @ScaledMetric(relativeTo: .caption) private var badgeSize: CGFloat = 12
    
    public init(exercises: [Exercise], currentExercise: Exercise) {
        self.exercises = exercises
        self.currentExercise = currentExercise
    }
    
    public var body: some View {
        HStack(spacing: GymBroSpacing.xs) {
            Image(systemName: "link.circle.fill")
                .font(.system(size: badgeSize))
                .foregroundStyle(GymBroColors.accentBlue)
            
            Text("Superset with \(partnerNames)")
                .font(GymBroTypography.caption2)
                .foregroundStyle(GymBroColors.accentBlue)
        }
        .padding(.horizontal, GymBroSpacing.sm)
        .padding(.vertical, GymBroSpacing.xs)
        .background(
            Capsule()
                .fill(GymBroColors.accentBlue.opacity(0.15))
        )
        .accessibilityLabel("Supersetted with \(partnerNames)")
    }
    
    private var partnerNames: String {
        let partners = exercises.filter { $0.id != currentExercise.id }
        if partners.isEmpty {
            return ""
        } else if partners.count == 1 {
            return partners[0].name
        } else {
            let names = partners.map { $0.name }
            return names.dropLast().joined(separator: ", ") + " and " + (names.last ?? "")
        }
    }
}

/// Visual connector line showing exercises are supersetted
public struct SupersetConnector: View {
    @ScaledMetric(relativeTo: .body) private var lineWidth: CGFloat = 2
    
    public init() {}
    
    public var body: some View {
        HStack(spacing: GymBroSpacing.xs) {
            Rectangle()
                .fill(GymBroColors.accentBlue.opacity(0.4))
                .frame(width: lineWidth)
            
            VStack(spacing: GymBroSpacing.xs) {
                ForEach(0..<3, id: \.self) { _ in
                    Circle()
                        .fill(GymBroColors.accentBlue.opacity(0.6))
                        .frame(width: 4, height: 4)
                }
            }
        }
        .frame(width: 12)
        .accessibilityHidden(true)
    }
}
