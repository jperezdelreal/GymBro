import Foundation
import SwiftData

@Model
final class BodyweightEntry {
    var id: UUID
    var date: Date
    var weightKg: Double
    
    @Relationship(deleteRule: .nullify)
    var userProfile: UserProfile?
    
    init(id: UUID = UUID(), date: Date, weightKg: Double) {
        self.id = id
        self.date = date
        self.weightKg = weightKg
    }
    
    func weightInUnit(_ unit: UnitSystem) -> Double {
        switch unit {
        case .metric:
            return weightKg
        case .imperial:
            return weightKg * 2.20462
        }
    }
}
