import UIKit

@MainActor
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
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(100))
            notificationGenerator.notificationOccurred(.success)
            try? await Task.sleep(for: .milliseconds(100))
            notificationGenerator.notificationOccurred(.success)
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
