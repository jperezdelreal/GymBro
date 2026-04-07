import UIKit

public final class HapticFeedbackService {
    public static let shared = HapticFeedbackService()
    
    private let impactLight = UIImpactFeedbackGenerator(style: .light)
    private let impactMedium = UIImpactFeedbackGenerator(style: .medium)
    private let impactHeavy = UIImpactFeedbackGenerator(style: .heavy)
    private let notificationGenerator = UINotificationFeedbackGenerator()
    private let selectionGenerator = UISelectionFeedbackGenerator()
    
    private init() {
        impactLight.prepare()
        impactMedium.prepare()
        impactHeavy.prepare()
    }
    
    public func prepare() {
        impactMedium.prepare()
    }
    
    public func setCompleted() {
        notificationGenerator.notificationOccurred(.success)
        impactHeavy.prepare()
    }
    
    public func personalRecordAchieved() {
        notificationGenerator.notificationOccurred(.success)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            self?.notificationGenerator.notificationOccurred(.success)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) { [weak self] in
            self?.notificationGenerator.notificationOccurred(.success)
        }
    }
    
    public func valueChanged() {
        selectionGenerator.selectionChanged()
    }
    
    public func lightImpact() {
        impactLight.impactOccurred()
    }
    
    public func mediumImpact() {
        impactMedium.impactOccurred()
    }
    
    public func heavyImpact() {
        impactHeavy.impactOccurred()
    }
}
