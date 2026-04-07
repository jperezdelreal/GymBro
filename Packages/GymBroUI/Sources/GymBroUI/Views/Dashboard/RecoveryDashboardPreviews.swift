import SwiftUI
import GymBroCore

// MARK: - Muscle Recovery Heat Map Previews

#Preview("Heat Map — Full Data") {
    ScrollView {
        MuscleRecoveryHeatMap(recoveryMap: previewRecoveryMap())
            .padding()
    }
    .gymBroDarkBackground()
}

#Preview("Heat Map — Empty") {
    ScrollView {
        MuscleRecoveryHeatMap(recoveryMap: [:])
            .padding()
    }
    .gymBroDarkBackground()
}

// MARK: - Anomaly Alert Previews

#Preview("Anomaly — Medium") {
    VStack(spacing: 16) {
        AnomalyAlertBanner(
            anomaly: ReadinessAnomaly(
                type: .hrvDrop,
                severity: .medium,
                date: Date(),
                message: "HRV dropped 22% below your 7-day baseline",
                recommendation: "This may indicate insufficient recovery, illness, or stress. Consider a rest day or light training.",
                affectedMetrics: ["HRV": 45]
            ),
            onDismiss: {}
        )
    }
    .padding()
    .gymBroDarkBackground()
}

#Preview("Anomaly — High") {
    VStack(spacing: 16) {
        AnomalyAlertBanner(
            anomaly: ReadinessAnomaly(
                type: .multiFactorDecline,
                severity: .high,
                date: Date(),
                message: "Multiple recovery metrics declined: HRV, Sleep, Training Load",
                recommendation: "Significant decline across multiple factors. Take a rest day and monitor symptoms.",
                affectedMetrics: ["HRV": 35, "Sleep": 40, "Training Load": 30]
            ),
            onDismiss: {}
        )
    }
    .padding()
    .gymBroDarkBackground()
}

// MARK: - Workout Adjustment Previews

#Preview("Adjustment — Lighter Variant") {
    ScrollView {
        WorkoutAdjustmentExplainer(
            recommendation: WorkoutRecommendation(
                action: .lighterVariant,
                originalDay: ProgramDayInfo(
                    name: "Heavy Upper Body",
                    isHeavyDay: true,
                    exercises: []
                ),
                adjustedDay: nil,
                rationale: "Your readiness is 52 (Moderate) and you have a heavy day scheduled.\n\nRecommended adjustments:\n• Reduce working weights to 70% of planned\n• Reduce total sets by 25%\n• Focus on technique and controlled tempo",
                intensityAdjustment: -30,
                volumeAdjustment: -25,
                exerciseReplacements: []
            ),
            onOverride: {}
        )
        .padding()
    }
    .gymBroDarkBackground()
}

#Preview("Adjustment — Rest Day") {
    ScrollView {
        WorkoutAdjustmentExplainer(
            recommendation: WorkoutRecommendation(
                action: .restDay,
                originalDay: ProgramDayInfo(
                    name: "Leg Day",
                    isHeavyDay: true,
                    exercises: []
                ),
                adjustedDay: nil,
                rationale: "Your readiness score is 32 (Poor). Your body needs recovery.\n\nConsider:\n• Complete rest day\n• Light walking (20-30 min)\n• Gentle mobility/stretching",
                intensityAdjustment: nil,
                volumeAdjustment: nil,
                exerciseReplacements: []
            ),
            onOverride: {}
        )
        .padding()
    }
    .gymBroDarkBackground()
}

// MARK: - Helpers

private func previewRecoveryMap() -> [String: MuscleRecoveryStatus] {
    [
        "Chest": MuscleRecoveryStatus(
            muscleName: "Chest", status: .fresh,
            hoursSinceLastTrained: 52, lastTrainedDate: Date().addingTimeInterval(-52 * 3600),
            recentVolume: 4500, recoveryPercentage: 100
        ),
        "Shoulders": MuscleRecoveryStatus(
            muscleName: "Shoulders", status: .recovering,
            hoursSinceLastTrained: 28, lastTrainedDate: Date().addingTimeInterval(-28 * 3600),
            recentVolume: 2800, recoveryPercentage: 65
        ),
        "Biceps": MuscleRecoveryStatus(
            muscleName: "Biceps", status: .fresh,
            hoursSinceLastTrained: 40, lastTrainedDate: Date().addingTimeInterval(-40 * 3600),
            recentVolume: 1200, recoveryPercentage: 95
        ),
        "Abs": MuscleRecoveryStatus(
            muscleName: "Abs", status: .recovering,
            hoursSinceLastTrained: 20, lastTrainedDate: Date().addingTimeInterval(-20 * 3600),
            recentVolume: 800, recoveryPercentage: 55
        ),
        "Quadriceps": MuscleRecoveryStatus(
            muscleName: "Quadriceps", status: .fatigued,
            hoursSinceLastTrained: 14, lastTrainedDate: Date().addingTimeInterval(-14 * 3600),
            recentVolume: 8200, recoveryPercentage: 20
        ),
        "Forearms": MuscleRecoveryStatus(
            muscleName: "Forearms", status: .fresh,
            hoursSinceLastTrained: 48, lastTrainedDate: Date().addingTimeInterval(-48 * 3600),
            recentVolume: 600, recoveryPercentage: 100
        ),
        "Back": MuscleRecoveryStatus(
            muscleName: "Back", status: .recovering,
            hoursSinceLastTrained: 30, lastTrainedDate: Date().addingTimeInterval(-30 * 3600),
            recentVolume: 5600, recoveryPercentage: 60
        ),
        "Lats": MuscleRecoveryStatus(
            muscleName: "Lats", status: .recovering,
            hoursSinceLastTrained: 30, lastTrainedDate: Date().addingTimeInterval(-30 * 3600),
            recentVolume: 3200, recoveryPercentage: 58
        ),
        "Traps": MuscleRecoveryStatus(
            muscleName: "Traps", status: .fresh,
            hoursSinceLastTrained: 54, lastTrainedDate: Date().addingTimeInterval(-54 * 3600),
            recentVolume: 1800, recoveryPercentage: 100
        ),
        "Triceps": MuscleRecoveryStatus(
            muscleName: "Triceps", status: .fatigued,
            hoursSinceLastTrained: 10, lastTrainedDate: Date().addingTimeInterval(-10 * 3600),
            recentVolume: 2400, recoveryPercentage: 28
        ),
        "Glutes": MuscleRecoveryStatus(
            muscleName: "Glutes", status: .fatigued,
            hoursSinceLastTrained: 14, lastTrainedDate: Date().addingTimeInterval(-14 * 3600),
            recentVolume: 7000, recoveryPercentage: 18
        ),
        "Hamstrings": MuscleRecoveryStatus(
            muscleName: "Hamstrings", status: .recovering,
            hoursSinceLastTrained: 36, lastTrainedDate: Date().addingTimeInterval(-36 * 3600),
            recentVolume: 4100, recoveryPercentage: 50
        ),
        "Calves": MuscleRecoveryStatus(
            muscleName: "Calves", status: .fresh,
            hoursSinceLastTrained: 48, lastTrainedDate: Date().addingTimeInterval(-48 * 3600),
            recentVolume: 1000, recoveryPercentage: 100
        ),
    ]
}
