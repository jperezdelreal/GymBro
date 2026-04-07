import SwiftUI
import Observation
import SwiftData
import GymBroCore

// MARK: - Insight Model

/// A single insight card shown in the swipeable pager.
public struct ProgressInsight: Identifiable {
    public let id: UUID
    public let title: String
    public let subtitle: String
    public let icon: String
    public let accentColor: Color
    public let chartType: InsightChartType

    public init(
        id: UUID = UUID(),
        title: String,
        subtitle: String,
        icon: String,
        accentColor: Color,
        chartType: InsightChartType
    ) {
        self.id = id
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.accentColor = accentColor
        self.chartType = chartType
    }
}

/// Which chart to expand when an insight card is tapped.
public enum InsightChartType: Sendable {
    case e1rm
    case volume
    case tonnage
    case muscleBalance
    case plateau
    case prs
}

// MARK: - ViewModel

/// ViewModel that drives the progress dashboard.
/// Computes hero metric, insight cards, and on-demand chart data.
@MainActor
@Observable
public final class ProgressDashboardViewModel {

    public var selectedTimeWindow: TimeWindow = .threeMonths
    public var selectedExerciseName: String?
    public var availableExercises: [String] = []

    // Chart data (loaded on demand)
    public var e1rmData: [E1RMDataPoint] = []
    public var volumeData: [VolumeDataPoint] = []
    public var tonnageData: [VolumeDataPoint] = []
    public var frequencyData: [FrequencyDataPoint] = []
    public var muscleBalance: [MuscleGroupBalance] = []
    public var prEvents: [PREvent] = []
    public var plateauAnalyses: [PlateauAnalysis] = []

    // Hero metric
    public var heroValue: Double = 0
    public var heroUnit: String = "kg"
    public var heroLabel: String = "Est. 1RM"
    public var heroTrend: HeroTrend = .flat
    public var sparklineValues: [Double] = []

    // Insights
    public var insights: [ProgressInsight] = []

    // Drill-down state
    public var expandedChart: InsightChartType?

    public var isLoading: Bool = false

    private let progressService: ProgressTrackingService
    private let plateauService: PlateauDetectionService
    private let e1rmCalculator: E1RMCalculator

    public init(
        progressService: ProgressTrackingService = ProgressTrackingService(),
        plateauService: PlateauDetectionService = PlateauDetectionService(),
        e1rmCalculator: E1RMCalculator = E1RMCalculator()
    ) {
        self.progressService = progressService
        self.plateauService = plateauService
        self.e1rmCalculator = e1rmCalculator
    }

    /// Toggle a chart's expanded state.
    public func toggleChart(_ type: InsightChartType) {
        if expandedChart == type {
            expandedChart = nil
        } else {
            expandedChart = type
        }
    }

    /// Refreshes all progress data from SwiftData.
    public func refresh(workouts: [Workout], allSets: [ExerciseSet]) {
        isLoading = true

        let exerciseNames = Set(allSets.compactMap { $0.exercise?.name })
        availableExercises = exerciseNames.sorted()

        if selectedExerciseName == nil {
            selectedExerciseName = availableExercises.first
        }

        if let exerciseName = selectedExerciseName {
            let exerciseSets = allSets.filter { $0.exercise?.name == exerciseName }
            e1rmData = progressService.e1rmTrend(
                sets: exerciseSets,
                exerciseName: exerciseName,
                timeWindow: selectedTimeWindow
            )
            prEvents = progressService.detectPRs(sets: exerciseSets, exerciseName: exerciseName)

            if let exerciseId = exerciseSets.first?.exercise?.id {
                if let analysis = plateauService.analyze(
                    sets: exerciseSets,
                    exerciseId: exerciseId,
                    exerciseName: exerciseName
                ) {
                    plateauAnalyses = [analysis]
                } else {
                    plateauAnalyses = []
                }
            }
        }

        volumeData = progressService.weeklyVolume(sets: allSets, timeWindow: selectedTimeWindow)
        tonnageData = progressService.tonnagePerWorkout(workouts: workouts, timeWindow: selectedTimeWindow)
        frequencyData = progressService.weeklyFrequency(workouts: workouts, timeWindow: selectedTimeWindow)
        muscleBalance = progressService.muscleGroupBalance(sets: allSets, timeWindow: selectedTimeWindow)

        computeHeroMetric()
        computeInsights()

        isLoading = false
    }

    // MARK: - Hero Metric

    private func computeHeroMetric() {
        if !e1rmData.isEmpty {
            let latest = e1rmData.last!.e1rm
            heroValue = latest
            heroUnit = "kg"
            heroLabel = "Est. 1RM"
            sparklineValues = e1rmData.suffix(12).map(\.e1rm)

            if e1rmData.count >= 2 {
                let previous = e1rmData[e1rmData.count - 2].e1rm
                if latest > previous + 0.5 {
                    heroTrend = .up
                } else if latest < previous - 0.5 {
                    heroTrend = .down
                } else {
                    heroTrend = .flat
                }
            } else {
                heroTrend = .flat
            }
        } else if !volumeData.isEmpty {
            let latest = volumeData.last!.totalVolume
            heroValue = latest
            heroUnit = "kg vol"
            heroLabel = "Weekly Volume"
            sparklineValues = volumeData.suffix(12).map(\.totalVolume)

            if volumeData.count >= 2 {
                let previous = volumeData[volumeData.count - 2].totalVolume
                if latest > previous * 1.02 {
                    heroTrend = .up
                } else if latest < previous * 0.98 {
                    heroTrend = .down
                } else {
                    heroTrend = .flat
                }
            } else {
                heroTrend = .flat
            }
        } else {
            heroValue = 0
            heroTrend = .flat
            sparklineValues = []
        }
    }

    // MARK: - Insights

    private func computeInsights() {
        var cards: [ProgressInsight] = []

        // Plateau insight
        if let plateau = plateauAnalyses.first(where: { $0.isPlateaued }) {
            let rec = plateau.recommendations.first ?? "Try varying rep ranges"
            cards.append(ProgressInsight(
                title: "\(plateau.exerciseName) plateau",
                subtitle: rec,
                icon: "exclamationmark.triangle.fill",
                accentColor: GymBroColors.accentAmber,
                chartType: .plateau
            ))
        }

        // Volume trend insight
        if volumeData.count >= 2 {
            let recent = volumeData.suffix(2)
            let current = recent.last!.totalVolume
            let previous = recent.first!.totalVolume
            if previous > 0 {
                let pctChange = ((current - previous) / previous) * 100
                let direction = pctChange >= 0 ? "up" : "down"
                let pctFormatted = String(format: "%.0f", abs(pctChange))
                let icon = pctChange >= 0 ? "arrow.up.right.circle.fill" : "arrow.down.right.circle.fill"
                let color = pctChange >= 0 ? GymBroColors.accentGreen : GymBroColors.accentRed
                cards.append(ProgressInsight(
                    title: "Volume \(direction) \(pctFormatted)%",
                    subtitle: "Week-over-week change",
                    icon: icon,
                    accentColor: color,
                    chartType: .volume
                ))
            }
        }

        // PR insight
        if !prEvents.isEmpty {
            let recentPRs = prEvents.suffix(5)
            let count = recentPRs.count
            cards.append(ProgressInsight(
                title: "\(count) personal record\(count == 1 ? "" : "s")",
                subtitle: recentPRs.last.map { "\($0.exerciseName) — \($0.recordType)" } ?? "",
                icon: "trophy.fill",
                accentColor: GymBroColors.accentAmber,
                chartType: .prs
            ))
        }

        // e1RM trend insight (if we have data and didn't already show plateau)
        if !e1rmData.isEmpty && !plateauAnalyses.contains(where: { $0.isPlateaued }) {
            if e1rmData.count >= 2 {
                let change = e1rmData.last!.e1rm - e1rmData.first!.e1rm
                let direction = change >= 0 ? "Gaining" : "Declining"
                let changeFormatted = String(format: "%@%.1f", change >= 0 ? "+" : "", change)
                cards.append(ProgressInsight(
                    title: "\(direction) strength",
                    subtitle: "\(changeFormatted) kg over period",
                    icon: "bolt.fill",
                    accentColor: GymBroColors.accentCyan,
                    chartType: .e1rm
                ))
            }
        }

        // Muscle balance insight (always useful)
        if !muscleBalance.isEmpty {
            let top = muscleBalance.first!
            let pctFormatted = String(format: "%.0f", top.percentage)
            cards.append(ProgressInsight(
                title: "Top group: \(top.muscleGroup)",
                subtitle: "\(pctFormatted)% of volume",
                icon: "figure.strengthtraining.traditional",
                accentColor: GymBroColors.accentCyan,
                chartType: .muscleBalance
            ))
        }

        insights = cards
    }
}
