import Foundation
import UserNotifications
import os

public final class NotificationService {
    private static let logger = Logger(subsystem: "com.gymbro", category: "Notifications")

    public static let shared = NotificationService()
    
    private let notificationCenter = UNUserNotificationCenter.current()
    private let restTimerIdentifier = "com.gymbro.rest-timer"
    private let readinessAlertIdentifier = "com.gymbro.readiness-alert"
    
    /// Category for recovery alert notifications — supports "Deload Today" action.
    public static let recoveryAlertCategory = "RECOVERY_ALERT"
    /// Action identifier for the "Deload Today" button on recovery notifications.
    public static let deloadActionIdentifier = "DELOAD_TODAY"
    
    private init() {}
    
    // MARK: - Authorization
    
    /// Request notification permissions.
    public func requestAuthorization() async -> Bool {
        do {
            return try await notificationCenter.requestAuthorization(options: [.alert, .sound, .badge])
        } catch {
            Self.logger.error("Notification authorization failed: \(error.localizedDescription)")
            return false
        }
    }
    
    /// Register notification categories including recovery alert actions.
    public func registerCategories() {
        let deloadAction = UNNotificationAction(
            identifier: Self.deloadActionIdentifier,
            title: "Deload Today",
            options: .foreground
        )
        
        let recoveryCategory = UNNotificationCategory(
            identifier: Self.recoveryAlertCategory,
            actions: [deloadAction],
            intentIdentifiers: [],
            options: .customDismissAction
        )
        
        notificationCenter.setNotificationCategories([recoveryCategory])
    }
    
    // MARK: - Rest Timer
    
    /// Schedule rest timer notification.
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
    
    /// Cancel rest timer notification.
    public func cancelRestTimerNotification() {
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [restTimerIdentifier])
    }
    
    // MARK: - Recovery Alerts
    
    /// Schedule a daily readiness check notification at the specified hour and minute.
    /// The notification repeats daily. Call once at app launch; the system persists the registration.
    public func scheduleDailyReadinessAlert(hour: Int = 7, minute: Int = 0) {
        var dateComponents = DateComponents()
        dateComponents.hour = hour
        dateComponents.minute = minute
        
        let content = UNMutableNotificationContent()
        content.title = "Morning Recovery Check"
        content.body = "Open GymBro to see your readiness score and today's training recommendation."
        content.sound = .default
        content.categoryIdentifier = Self.recoveryAlertCategory
        
        let trigger = UNCalendarNotificationTrigger(
            dateMatching: dateComponents,
            repeats: true
        )
        
        let request = UNNotificationRequest(
            identifier: readinessAlertIdentifier,
            content: content,
            trigger: trigger
        )
        
        notificationCenter.add(request) { error in
            if let error {
                Self.logger.error("Failed to schedule readiness alert: \(error.localizedDescription)")
            } else {
                Self.logger.info("Daily readiness alert scheduled for \(hour, privacy: .public):\(minute, privacy: .public)")
            }
        }
    }
    
    /// Send an immediate recovery alert notification based on a RecoveryAlertService alert.
    public func sendRecoveryAlert(_ alert: RecoveryAlertService.RecoveryAlert) {
        let content = UNMutableNotificationContent()
        content.title = alert.title
        content.body = alert.message
        content.sound = .default
        content.categoryIdentifier = Self.recoveryAlertCategory
        content.userInfo = [
            "alertLevel": alert.level.rawValue,
            "readinessScore": alert.readinessScore
        ]
        
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let identifier = "\(readinessAlertIdentifier)-\(UUID().uuidString)"
        
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: trigger
        )
        
        notificationCenter.add(request) { error in
            if let error {
                Self.logger.error("Failed to send recovery alert: \(error.localizedDescription)")
            } else {
                Self.logger.info("Recovery alert sent: \(alert.level.rawValue, privacy: .public)")
            }
        }
    }
    
    /// Cancel the scheduled daily readiness alert.
    public func cancelDailyReadinessAlert() {
        notificationCenter.removePendingNotificationRequests(withIdentifiers: [readinessAlertIdentifier])
        Self.logger.info("Daily readiness alert cancelled")
    }
}
