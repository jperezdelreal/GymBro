import Foundation
import UserNotifications
import os

public final class NotificationService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "Notifications")

    public static let shared = NotificationService()
    
    private let notificationCenter = UNUserNotificationCenter.current()
    private let restTimerIdentifier = "com.gymbro.rest-timer"
    
    private init() {}
    
    // Request notification permissions
    public func requestAuthorization() async -> Bool {
        do {
            return try await notificationCenter.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            Self.logger.error("Notification authorization failed: \(error.localizedDescription)")
            return false
        }
    }
    
    // Schedule rest timer notification
    public func scheduleRestTimerNotification(duration: Int) {
        let content = UNMutableNotificationContent()
        content.title = "Rest Complete"
        content.body = "Time to crush your next set! 💪"
        content.sound = .default
        content.categoryIdentifier = "REST_TIMER"
        
        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: TimeInterval(duration),
            repeats: false
        )
        
        let request = UNNotificationRequest(
            identifier: restTimerIdentifier,
            content: content,
            trigger: trigger
        )
        
        notificationCenter.add(request) { error in
            if let error = error {
                Self.logger.error("Failed to schedule rest timer notification: \(error.localizedDescription)")
            }
        }
    }
    
    // Cancel rest timer notification
    public func cancelRestTimerNotification() {
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [restTimerIdentifier])
    }
}
