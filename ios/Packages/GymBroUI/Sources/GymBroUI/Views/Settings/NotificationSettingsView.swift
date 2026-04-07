import SwiftUI
import GymBroCore

/// Settings screen for configuring recovery notification preferences.
/// Users can enable/disable daily readiness alerts and set the notification time.
public struct NotificationSettingsView: View {
    @AppStorage("readinessAlertsEnabled") private var alertsEnabled = true
    @AppStorage("readinessAlertHour") private var alertHour = 7
    @AppStorage("readinessAlertMinute") private var alertMinute = 0

    @State private var notificationPermissionGranted = false
    @State private var selectedTime = Date()

    public init() {}

    public var body: some View {
        Form {
            Section {
                Toggle("Daily Readiness Alerts", isOn: $alertsEnabled)
                    .onChange(of: alertsEnabled) { _, enabled in
                        handleToggle(enabled)
                    }
            } header: {
                Text("Recovery Notifications")
            } footer: {
                Text("Receive a morning notification to check your readiness score and get training recommendations.")
            }

            if alertsEnabled {
                Section("Alert Time") {
                    DatePicker(
                        "Notification Time",
                        selection: $selectedTime,
                        displayedComponents: .hourAndMinute
                    )
                    .onChange(of: selectedTime) { _, newValue in
                        updateAlertTime(from: newValue)
                    }
                }

                Section {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .foregroundStyle(.orange)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Deload Alert")
                                .font(.subheadline.weight(.medium))
                            Text("Score below 50 — lighter workout suggested")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }

                    HStack {
                        Image(systemName: "bed.double.fill")
                            .foregroundStyle(.red)
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Rest Day Alert")
                                .font(.subheadline.weight(.medium))
                            Text("Score below 30 — rest day recommended")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                    }
                } header: {
                    Text("Alert Thresholds")
                } footer: {
                    Text("Alerts are triggered automatically based on your readiness score calculated from HealthKit data.")
                }
            }

            if !notificationPermissionGranted {
                Section {
                    Button {
                        Task {
                            let granted = await NotificationService.shared.requestAuthorization()
                            await MainActor.run {
                                notificationPermissionGranted = granted
                            }
                        }
                    } label: {
                        Label("Enable Notifications", systemImage: "bell.badge")
                    }
                } footer: {
                    Text("Notification permission is required for recovery alerts. You can change this in Settings.")
                }
            }
        }
        .navigationTitle("Notifications")
        .onAppear {
            initializeTime()
        }
    }

    // MARK: - Helpers

    private func initializeTime() {
        var components = DateComponents()
        components.hour = alertHour
        components.minute = alertMinute
        if let date = Calendar.current.date(from: components) {
            selectedTime = date
        }
    }

    private func updateAlertTime(from date: Date) {
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        alertHour = components.hour ?? 7
        alertMinute = components.minute ?? 0

        if alertsEnabled {
            NotificationService.shared.scheduleDailyReadinessAlert(hour: alertHour, minute: alertMinute)
        }
    }

    private func handleToggle(_ enabled: Bool) {
        if enabled {
            NotificationService.shared.scheduleDailyReadinessAlert(hour: alertHour, minute: alertMinute)
        } else {
            NotificationService.shared.cancelDailyReadinessAlert()
        }
    }
}
