import Foundation
import SwiftData

public struct PersonalRecord {
    public let exerciseSet: ExerciseSet
    public let recordType: RecordType
    
    public enum RecordType {
        case maxWeight
        case maxReps
        case maxVolume
        case maxE1RM
    }
}

public class PersonalRecordService {
    private let modelContext: ModelContext
    
    public init(modelContext: ModelContext) {
        self.modelContext = modelContext
    }
    
    public func getPersonalRecords(for exercise: Exercise) throws -> [PersonalRecord] {
        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate { set in
                set.exercise?.id == exercise.id && set.setType == .working
            },
            sortBy: [SortDescriptor(\.completedAt, order: .reverse)]
        )
        
        let sets = try modelContext.fetch(descriptor)
        
        guard !sets.isEmpty else { return [] }
        
        var records: [PersonalRecord] = []
        
        let maxWeightSet = sets.max(by: { $0.weightKg < $1.weightKg })
        if let maxWeightSet = maxWeightSet {
            records.append(PersonalRecord(exerciseSet: maxWeightSet, recordType: .maxWeight))
        }
        
        let maxRepsSet = sets.max(by: { $0.reps < $1.reps })
        if let maxRepsSet = maxRepsSet {
            records.append(PersonalRecord(exerciseSet: maxRepsSet, recordType: .maxReps))
        }
        
        let maxVolumeSet = sets.max(by: { $0.volume < $1.volume })
        if let maxVolumeSet = maxVolumeSet {
            records.append(PersonalRecord(exerciseSet: maxVolumeSet, recordType: .maxVolume))
        }
        
        let maxE1RMSet = sets.max(by: { $0.estimatedOneRepMax < $1.estimatedOneRepMax })
        if let maxE1RMSet = maxE1RMSet {
            records.append(PersonalRecord(exerciseSet: maxE1RMSet, recordType: .maxE1RM))
        }
        
        return records
    }
    
    public func isPR(set: ExerciseSet, for recordType: PersonalRecord.RecordType) throws -> Bool {
        guard let exercise = set.exercise, set.setType == .working else { return false }
        
        let descriptor = FetchDescriptor<ExerciseSet>(
            predicate: #Predicate { s in
                s.exercise?.id == exercise.id && 
                s.setType == .working &&
                s.completedAt != nil &&
                s.completedAt! < (set.completedAt ?? Date())
            }
        )
        
        let previousSets = try modelContext.fetch(descriptor)
        
        guard !previousSets.isEmpty else { return true }
        
        switch recordType {
        case .maxWeight:
            return previousSets.allSatisfy { $0.weightKg < set.weightKg }
        case .maxReps:
            return previousSets.allSatisfy { $0.reps < set.reps }
        case .maxVolume:
            return previousSets.allSatisfy { $0.volume < set.volume }
        case .maxE1RM:
            return previousSets.allSatisfy { $0.estimatedOneRepMax < set.estimatedOneRepMax }
        }
    }
    
    public func getRecordTypes(for set: ExerciseSet) throws -> [PersonalRecord.RecordType] {
        var recordTypes: [PersonalRecord.RecordType] = []
        
        for type in [PersonalRecord.RecordType.maxWeight, .maxReps, .maxVolume, .maxE1RM] {
            if try isPR(set: set, for: type) {
                recordTypes.append(type)
            }
        }
        
        return recordTypes
    }
}
