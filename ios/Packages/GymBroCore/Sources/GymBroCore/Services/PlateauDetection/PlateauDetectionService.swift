import Foundation

/// Triple-method composite plateau detection system.
///
/// Combines three independent signals to proactively detect stagnation:
/// 1. **Trend Forecasting (40%)** — Linear regression predicts expected trajectory
/// 2. **Change Point Detection (35%)** — CUSUM detects distribution shifts
/// 3. **Rolling Average Stagnation (25%)** — Moving average rate-of-change analysis
///
/// Composite score > 0.65 = plateau declared per `docs/AI_ML_APPROACH.md` Decision #5.
public class PlateauDetectionService {

    // MARK: - Weights (per AI_ML_APPROACH.md Decision #5)
    public static let forecastWeight: Double = 0.40
    public static let changePointWeight: Double = 0.35
    public static let rollingAverageWeight: Double = 0.25
    public static let plateauThreshold: Double = 0.65

    /// Minimum number of sessions before analysis is meaningful.
    public static let minimumDataPoints: Int = 6

    private let forecaster: TrendForecaster
    private let changePointDetector: ChangePointDetector
    private let rollingAverageAnalyzer: RollingAverageAnalyzer
    private let e1rmCalculator: E1RMCalculator

    public init(
        forecaster: TrendForecaster = TrendForecaster(),
        changePointDetector: ChangePointDetector = ChangePointDetector(),
        rollingAverageAnalyzer: RollingAverageAnalyzer = RollingAverageAnalyzer(),
        e1rmCalculator: E1RMCalculator = E1RMCalculator()
    ) {
        self.forecaster = forecaster
        self.changePointDetector = changePointDetector
        self.rollingAverageAnalyzer = rollingAverageAnalyzer
        self.e1rmCalculator = e1rmCalculator
    }

    // MARK: - Core Analysis

    /// Analyzes plateau status for an exercise from its set history.
    /// Returns nil if insufficient data (< minimumDataPoints sessions).
    public func analyze(
        sets: [ExerciseSet],
        exerciseId: UUID,
        exerciseName: String,
        previousAnalysis: PlateauAnalysis? = nil
    ) -> PlateauAnalysis? {
        let e1rmValues = extractSessionE1RMs(from: sets)

        guard e1rmValues.count >= Self.minimumDataPoints else { return nil }

        // Run all three detection methods
        let forecastScore = forecaster.analyze(values: e1rmValues)
        let changePointScore = changePointDetector.analyze(values: e1rmValues)
        let rollingAvgScore = rollingAverageAnalyzer.analyze(values: e1rmValues)

        // Weighted composite
        let compositeScore = forecastScore * Self.forecastWeight
            + changePointScore * Self.changePointWeight
            + rollingAvgScore * Self.rollingAverageWeight

        // Determine state transition
        let newState = determineState(
            compositeScore: compositeScore,
            previousState: previousAnalysis?.progressState ?? .progressing,
            previousSessionsInState: previousAnalysis?.sessionsInState ?? 0
        )

        // Generate recommendations
        let recommendations = generateRecommendations(
            compositeScore: compositeScore,
            state: newState.state,
            exerciseName: exerciseName,
            sessionsInState: newState.sessionsInState
        )

        return PlateauAnalysis(
            exerciseId: exerciseId,
            exerciseName: exerciseName,
            compositeScore: compositeScore,
            forecastScore: forecastScore,
            changePointScore: changePointScore,
            rollingAverageScore: rollingAvgScore,
            progressState: newState.state,
            sessionsInState: newState.sessionsInState,
            recommendations: recommendations
        )
    }

    // MARK: - e1RM Extraction

    /// Groups sets by workout session date and picks best e1RM per session.
    public func extractSessionE1RMs(from sets: [ExerciseSet]) -> [Double] {
        let workingSets = sets.filter { $0.setType == .working }

        let grouped = Dictionary(grouping: workingSets) { set in
            Calendar.current.startOfDay(for: set.workout?.date ?? set.createdAt)
        }

        return grouped
            .sorted { $0.key < $1.key }
            .compactMap { (_, daySets) -> Double? in
                daySets
                    .map { e1rmCalculator.calculate(weight: $0.weightKg, reps: $0.reps) }
                    .max()
            }
    }

    // MARK: - State Machine

    /// Transitions: Progressing → Stalling → Plateaued → Recovering
    private func determineState(
        compositeScore: Double,
        previousState: ProgressState,
        previousSessionsInState: Int
    ) -> (state: ProgressState, sessionsInState: Int) {

        switch previousState {
        case .progressing:
            if compositeScore > Self.plateauThreshold {
                return (.plateaued, 1)
            } else if compositeScore > 0.4 {
                return (.stalling, 1)
            }
            return (.progressing, previousSessionsInState + 1)

        case .stalling:
            if compositeScore > Self.plateauThreshold {
                return (.plateaued, 1)
            } else if compositeScore < 0.3 {
                return (.progressing, 1)
            }
            return (.stalling, previousSessionsInState + 1)

        case .plateaued:
            if compositeScore < 0.4 {
                return (.recovering, 1)
            }
            return (.plateaued, previousSessionsInState + 1)

        case .recovering:
            if compositeScore < 0.25 {
                return (.progressing, 1)
            } else if compositeScore > Self.plateauThreshold {
                return (.plateaued, 1)
            }
            return (.recovering, previousSessionsInState + 1)
        }
    }

    // MARK: - Recommendations

    private func generateRecommendations(
        compositeScore: Double,
        state: ProgressState,
        exerciseName: String,
        sessionsInState: Int
    ) -> [String] {
        var recs: [String] = []

        switch state {
        case .progressing:
            break

        case .stalling:
            recs.append("Your \(exerciseName) progress is slowing. Consider a small volume increase.")
            if sessionsInState >= 3 {
                recs.append("Try adding an extra working set or increasing frequency.")
            }

        case .plateaued:
            recs.append("\(exerciseName) has plateaued. Time for a strategic change.")
            recs.append("Consider a deload week (reduce volume 40-60%) to allow supercompensation.")
            if sessionsInState >= 2 {
                recs.append("Try a variation of \(exerciseName) to target the movement from a different angle.")
            }
            if sessionsInState >= 4 {
                recs.append("Extended plateau detected. Consider changing rep ranges or periodization scheme.")
            }

        case .recovering:
            recs.append("Good news — \(exerciseName) is showing signs of recovery. Maintain current approach.")
        }

        return recs
    }
}
