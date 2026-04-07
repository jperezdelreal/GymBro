import Foundation
import Combine
import UIKit

@Observable
public final class RestTimerService {
    public static let shared = RestTimerService()
    
    // Timer state
    public private(set) var isActive: Bool = false
    public private(set) var remainingSeconds: Int = 0
    public private(set) var totalSeconds: Int = 0
    
    // Next set preview
    public private(set) var nextSetInfo: NextSetInfo?
    
    // Timer publisher
    private var timer: AnyCancellable?
    private var backgroundTask: Task<Void, Never>?
    
    // Haptics
    private let impactFeedback = UIImpactFeedbackGenerator(style: .medium)
    private let notificationFeedback = UINotificationFeedbackGenerator()
    
    private init() {
        impactFeedback.prepare()
        notificationFeedback.prepare()
    }
    
    // Start timer with duration and optional next set info
    public func start(
        duration: Int,
        nextSetInfo: NextSetInfo? = nil
    ) {
        guard duration > 0 else { return }
        
        stop()
        
        self.totalSeconds = duration
        self.remainingSeconds = duration
        self.nextSetInfo = nextSetInfo
        self.isActive = true
        
        // Schedule timer
        startTimer()
        
        // Request notification if needed
        scheduleNotification(duration: duration)
    }
    
    public func stop() {
        isActive = false
        remainingSeconds = 0
        timer?.cancel()
        timer = nil
        backgroundTask?.cancel()
        backgroundTask = nil
        NotificationService.shared.cancelRestTimerNotification()
    }
    
    public func skip() {
        stop()
    }
    
    public func addTime(_ seconds: Int) {
        remainingSeconds = max(0, remainingSeconds + seconds)
        totalSeconds = max(totalSeconds, remainingSeconds)
        
        // Reschedule notification with new time
        if isActive {
            NotificationService.shared.cancelRestTimerNotification()
            scheduleNotification(duration: remainingSeconds)
        }
    }
    
    // MARK: - Private
    
    private func startTimer() {
        backgroundTask = Task { @MainActor in
            while isActive && remainingSeconds > 0 {
                try? await Task.sleep(for: .seconds(1))
                
                guard !Task.isCancelled else { return }
                
                remainingSeconds -= 1
                
                // Trigger haptics
                if remainingSeconds == 10 {
                    impactFeedback.impactOccurred()
                } else if remainingSeconds == 0 {
                    notificationFeedback.notificationOccurred(.warning)
                    isActive = false
                }
            }
        }
    }
    
    private func scheduleNotification(duration: Int) {
        guard duration > 0 else { return }
        NotificationService.shared.scheduleRestTimerNotification(duration: duration)
    }
}

public struct NextSetInfo: Equatable {
    public let exerciseName: String
    public let setNumber: Int
    public let targetReps: Int
    public let targetWeight: Double
    public let weightUnit: String
    
    public init(
        exerciseName: String,
        setNumber: Int,
        targetReps: Int,
        targetWeight: Double,
        weightUnit: String
    ) {
        self.exerciseName = exerciseName
        self.setNumber = setNumber
        self.targetReps = targetReps
        self.targetWeight = targetWeight
        self.weightUnit = weightUnit
    }
}

// Convenience methods for different rest durations based on exercise category
extension RestTimerService {
    public static func defaultRestTime(for category: ExerciseCategory) -> Int {
        switch category {
        case .compound:
            return 180 // 3 minutes
        case .isolation:
            return 90  // 90 seconds
        case .accessory:
            return 60  // 60 seconds
        case .cardio:
            return 30  // 30 seconds
        }
    }
}
