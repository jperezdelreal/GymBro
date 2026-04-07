import Foundation
import SwiftData

@Model
final class MuscleGroup {
    var id: UUID
    var name: String
    var isPrimary: Bool
    
    init(id: UUID = UUID(), name: String, isPrimary: Bool = true) {
        self.id = id
        self.name = name
        self.isPrimary = isPrimary
    }
}
