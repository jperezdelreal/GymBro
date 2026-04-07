import Foundation
import SwiftData

public struct ProgramComplianceService {
    
    public struct ComplianceResult {
        public let adherencePercentage: Double
        public let completedExercises: Int
        public let totalExercises: Int
        public let missedExercises: [String]
        public let extraExercises: [String]
        public let rpeDeviation: Double?
        public let volumeDeviation: Double?
        public let compliance: ComplianceLevel
        
        public enum ComplianceLevel {
            case excellent  // 90%+
            case good       // 75-89%
            case moderate   // 60-74%
            case poor       // <60%
        }
    }
    
    public static func calculateCompliance(
        workout: Workout,
        programWeek: ProgramWeek
    ) -> ComplianceResult {
        let plannedExercises = programWeek.plannedExercises.sorted { $0.order < $1.order }
        let actualSets = workout.sets
        
        // Group actual sets by exercise
        let actualExerciseNames = Set(actualSets.compactMap { $0.exercise?.name })
        let plannedExerciseNames = Set(plannedExercises.compactMap { $0.exercise?.name })
        
        // Calculate matched exercises
        let matchedExercises = actualExerciseNames.intersection(plannedExerciseNames)
        let missedExercises = Array(plannedExerciseNames.subtracting(matchedExercises))
        let extraExercises = Array(actualExerciseNames.subtracting(matchedExercises))
        
        let totalExercises = plannedExercises.count
        let completedExercises = matchedExercises.count
        
        // Base adherence: percentage of planned exercises completed
        let baseAdherence = totalExercises > 0 ? Double(completedExercises) / Double(totalExercises) : 0.0
        
        // Calculate RPE deviation for matched exercises
        var totalRpeDeviation = 0.0
        var rpeComparisons = 0
        
        for plannedExercise in plannedExercises {
            guard let exerciseName = plannedExercise.exercise?.name,
                  matchedExercises.contains(exerciseName),
                  let targetRPE = plannedExercise.targetRPE else {
                continue
            }
            
            let setsForExercise = actualSets.filter { $0.exercise?.name == exerciseName }
            let actualRPEs = setsForExercise.compactMap { $0.rpe }
            
            if !actualRPEs.isEmpty {
                let avgActualRPE = actualRPEs.reduce(0.0, +) / Double(actualRPEs.count)
                totalRpeDeviation += abs(avgActualRPE - targetRPE)
                rpeComparisons += 1
            }
        }
        
        let avgRpeDeviation = rpeComparisons > 0 ? totalRpeDeviation / Double(rpeComparisons) : nil
        
        // Calculate volume deviation (sets completed vs planned)
        var totalVolumeDeviation = 0.0
        var volumeComparisons = 0
        
        for plannedExercise in plannedExercises {
            guard let exerciseName = plannedExercise.exercise?.name,
                  matchedExercises.contains(exerciseName) else {
                continue
            }
            
            let setsForExercise = actualSets.filter { $0.exercise?.name == exerciseName }
            let actualSetCount = setsForExercise.count
            let plannedSetCount = plannedExercise.targetSets
            
            let deviation = abs(Double(actualSetCount) - Double(plannedSetCount)) / Double(plannedSetCount)
            totalVolumeDeviation += deviation
            volumeComparisons += 1
        }
        
        let avgVolumeDeviation = volumeComparisons > 0 ? totalVolumeDeviation / Double(volumeComparisons) : nil
        
        // Adjust adherence based on RPE and volume compliance
        var adjustedAdherence = baseAdherence
        
        // Penalty for high RPE deviation (>1.5 RPE off target = -5% adherence)
        if let rpeDeviation = avgRpeDeviation, rpeDeviation > 1.5 {
            adjustedAdherence -= 0.05
        }
        
        // Penalty for high volume deviation (>20% = -5% adherence)
        if let volumeDeviation = avgVolumeDeviation, volumeDeviation > 0.2 {
            adjustedAdherence -= 0.05
        }
        
        // Penalty for extra exercises (each extra = -2% adherence)
        adjustedAdherence -= Double(extraExercises.count) * 0.02
        
        // Clamp to 0-1 range
        adjustedAdherence = max(0.0, min(1.0, adjustedAdherence))
        
        let adherencePercentage = adjustedAdherence * 100.0
        
        // Determine compliance level
        let complianceLevel: ComplianceResult.ComplianceLevel
        switch adherencePercentage {
        case 90...:
            complianceLevel = .excellent
        case 75..<90:
            complianceLevel = .good
        case 60..<75:
            complianceLevel = .moderate
        default:
            complianceLevel = .poor
        }
        
        return ComplianceResult(
            adherencePercentage: adherencePercentage,
            completedExercises: completedExercises,
            totalExercises: totalExercises,
            missedExercises: missedExercises.sorted(),
            extraExercises: extraExercises.sorted(),
            rpeDeviation: avgRpeDeviation,
            volumeDeviation: avgVolumeDeviation,
            compliance: complianceLevel
        )
    }
    
    public static func calculateProgramCompliance(
        workouts: [Workout],
        program: Program,
        startDate: Date
    ) -> Double {
        guard !workouts.isEmpty else { return 0.0 }
        
        var totalCompliance = 0.0
        var complianceCount = 0
        
        for workout in workouts {
            // Determine which week of the program this workout belongs to
            let daysSinceStart = Calendar.current.dateComponents([.day], from: startDate, to: workout.date).day ?? 0
            let weekNumber = (daysSinceStart / 7) + 1
            
            // Find the matching program day and week
            guard let programDay = workout.programDay,
                  let programWeek = programDay.weeks.first(where: { $0.weekNumber == weekNumber }) else {
                continue
            }
            
            let result = calculateCompliance(workout: workout, programWeek: programWeek)
            totalCompliance += result.adherencePercentage
            complianceCount += 1
        }
        
        return complianceCount > 0 ? totalCompliance / Double(complianceCount) : 0.0
    }
}
