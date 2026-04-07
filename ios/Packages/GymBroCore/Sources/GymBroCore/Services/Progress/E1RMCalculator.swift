import Foundation

/// Calculates estimated one-rep max using validated strength science formulas.
/// Supports Epley (default) and Brzycki methods, each accurate in different rep ranges.
public enum E1RMFormula: String, CaseIterable, Sendable {
    case epley
    case brzycki
}

public struct E1RMCalculator: Sendable {

    public init() {}

    // MARK: - Core Calculations

    /// Estimates 1RM from a given weight and rep count.
    /// Returns the weight itself for single reps; returns 0 for invalid input.
    public func calculate(weight: Double, reps: Int, formula: E1RMFormula = .epley) -> Double {
        guard weight > 0, reps > 0 else { return 0 }
        guard reps > 1 else { return weight }

        switch formula {
        case .epley:
            return weight * (1.0 + Double(reps) / 30.0)
        case .brzycki:
            let denominator = 36.0 - Double(reps)
            guard denominator > 0 else { return weight * 2.0 }
            return weight * (36.0 / denominator)
        }
    }

    /// Best e1RM from a collection of (weight, reps) pairs.
    public func bestE1RM(from sets: [(weight: Double, reps: Int)], formula: E1RMFormula = .epley) -> Double {
        sets.map { calculate(weight: $0.weight, reps: $0.reps, formula: formula) }
            .max() ?? 0
    }
}
