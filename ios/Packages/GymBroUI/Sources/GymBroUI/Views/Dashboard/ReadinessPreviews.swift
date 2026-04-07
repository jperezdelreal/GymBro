import SwiftUI
import Charts
import GymBroCore

// MARK: - ReadinessScoreView Previews

#Preview("Readiness — Excellent") {
    ScrollView {
        ReadinessScoreView(
            score: ReadinessScore(
                date: Date(),
                overallScore: 92,
                sleepScore: 95,
                hrvScore: 88,
                restingHRScore: 90,
                trainingLoadScore: 85,
                subjectiveScore: 90,
                recommendation: "You're fully recovered. Go heavy today — your body is primed for a PR attempt.",
                label: .excellent
            ),
            trend: (0..<7).map { dayOffset in
                ReadinessScore(
                    date: Calendar.current.date(byAdding: .day, value: -dayOffset, to: Date())!,
                    overallScore: Double.random(in: 75...95),
                    sleepScore: Double.random(in: 70...100),
                    hrvScore: Double.random(in: 65...95),
                    restingHRScore: Double.random(in: 70...95),
                    trainingLoadScore: Double.random(in: 60...90),
                    recommendation: "",
                    label: .good
                )
            }
        )
    }
    .gymBroDarkBackground()
}

#Preview("Readiness — Poor") {
    ScrollView {
        ReadinessScoreView(
            score: ReadinessScore(
                date: Date(),
                overallScore: 38,
                sleepScore: 30,
                hrvScore: 40,
                restingHRScore: 45,
                trainingLoadScore: 35,
                recommendation: "Rest day recommended. Your body needs recovery. Light stretching or walking only.",
                label: .poor
            )
        )
    }
    .gymBroDarkBackground()
}

#Preview("Readiness — Empty") {
    ScrollView {
        ReadinessScoreView()
    }
    .gymBroDarkBackground()
}
