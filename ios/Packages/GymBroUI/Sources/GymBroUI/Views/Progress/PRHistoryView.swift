import SwiftUI
import SwiftData
import GymBroCore

/// Shows all-time personal records for a given exercise, one row per record type.
public struct PRHistoryView: View {
    let exercise: Exercise
    let records: [PersonalRecord]
    let unitSystem: UnitSystem

    @ScaledMetric(relativeTo: .title2) private var headerSize: CGFloat = 24
    @ScaledMetric(relativeTo: .title3) private var valueSize: CGFloat = 20

    public init(exercise: Exercise, records: [PersonalRecord], unitSystem: UnitSystem = .metric) {
        self.exercise = exercise
        self.records = records
        self.unitSystem = unitSystem
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: GymBroSpacing.lg) {
                // Header
                VStack(spacing: GymBroSpacing.sm) {
                    Image(systemName: "trophy.fill")
                        .font(.system(size: headerSize))
                        .foregroundStyle(GymBroColors.accentAmber)
                        .accessibilityHidden(true)

                    Text(exercise.name)
                        .font(.system(size: headerSize, weight: .bold))
                        .foregroundStyle(GymBroColors.textPrimary)

                    Text("All-Time Personal Records")
                        .font(GymBroTypography.subheadline)
                        .foregroundStyle(GymBroColors.textSecondary)
                }
                .padding(.top, GymBroSpacing.lg)

                if records.isEmpty {
                    emptyState
                } else {
                    VStack(spacing: GymBroSpacing.md) {
                        ForEach(Array(records.enumerated()), id: \.offset) { _, record in
                            PRRecordCard(record: record, unitSystem: unitSystem, valueSize: valueSize)
                        }
                    }
                    .padding(.horizontal, GymBroSpacing.md)
                }
            }
            .padding(.bottom, GymBroSpacing.xxl)
        }
        .gymBroDarkBackground()
        .navigationTitle("PR History")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var emptyState: some View {
        VStack(spacing: GymBroSpacing.md) {
            Image(systemName: "chart.line.uptrend.xyaxis")
                .font(.system(size: 48))
                .foregroundStyle(GymBroColors.textTertiary)
                .accessibilityHidden(true)

            Text("No records yet")
                .font(GymBroTypography.title3)
                .foregroundStyle(GymBroColors.textSecondary)

            Text("Complete working sets to start tracking PRs")
                .font(GymBroTypography.subheadline)
                .foregroundStyle(GymBroColors.textTertiary)
                .multilineTextAlignment(.center)
        }
        .padding(.top, GymBroSpacing.xxl)
    }
}

/// Single PR record card showing type, value, and date.
struct PRRecordCard: View {
    let record: PersonalRecord
    let unitSystem: UnitSystem
    let valueSize: CGFloat

    var body: some View {
        GymBroCard(accent: GymBroColors.accentAmber) {
            HStack(spacing: GymBroSpacing.md) {
                Image(systemName: iconName)
                    .font(.system(size: 24))
                    .foregroundStyle(GymBroColors.accentAmber)
                    .frame(width: 40)

                VStack(alignment: .leading, spacing: GymBroSpacing.xs) {
                    Text(typeLabel)
                        .font(GymBroTypography.caption2)
                        .foregroundStyle(GymBroColors.textTertiary)
                        .tracking(0.5)

                    Text(valueText)
                        .font(GymBroTypography.monoNumber(size: valueSize))
                        .foregroundStyle(GymBroColors.textPrimary)
                }

                Spacer()

                if let date = record.exerciseSet.completedAt {
                    Text(date, style: .date)
                        .font(GymBroTypography.caption)
                        .foregroundStyle(GymBroColors.textTertiary)
                }
            }
        }
    }

    private var typeLabel: String {
        switch record.recordType {
        case .maxE1RM:   return "ESTIMATED 1RM"
        case .maxWeight: return "MAX WEIGHT"
        case .maxVolume: return "MAX VOLUME"
        case .maxReps:   return "MAX REPS"
        }
    }

    private var iconName: String {
        switch record.recordType {
        case .maxE1RM:   return "flame.fill"
        case .maxWeight: return "scalemass.fill"
        case .maxVolume: return "chart.bar.fill"
        case .maxReps:   return "repeat"
        }
    }

    private var valueText: String {
        let set = record.exerciseSet
        switch record.recordType {
        case .maxE1RM:
            return String(format: "%.1f %@", e1rmInUnit, unitLabel)
        case .maxWeight:
            return String(format: "%.1f %@ × %d", weightInUnit(set.weightKg), unitLabel, set.reps)
        case .maxVolume:
            return String(format: "%.0f %@", volumeInUnit(set.volume), unitLabel)
        case .maxReps:
            return "\(set.reps) reps @ \(String(format: "%.1f", weightInUnit(set.weightKg))) \(unitLabel)"
        }
    }

    private var e1rmInUnit: Double {
        let e1rm = record.exerciseSet.estimatedOneRepMax
        return unitSystem == .metric ? e1rm : e1rm * 2.20462
    }

    private func weightInUnit(_ kg: Double) -> Double {
        unitSystem == .metric ? kg : kg * 2.20462
    }

    private func volumeInUnit(_ kgVol: Double) -> Double {
        unitSystem == .metric ? kgVol : kgVol * 2.20462
    }

    private var unitLabel: String {
        unitSystem == .metric ? "kg" : "lb"
    }
}

// MARK: - Preview

#Preview("PR History — Empty") {
    NavigationStack {
        PRHistoryView(
            exercise: Exercise(name: "Bench Press", category: .compound, equipment: .barbell),
            records: []
        )
    }
}
