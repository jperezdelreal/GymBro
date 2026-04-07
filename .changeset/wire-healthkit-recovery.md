---
'@bradygaster/squad-cli': minor
---

Wire HealthKit to recovery pipeline: HealthKitCoordinator orchestrates HealthKit → SwiftData → ReadinessScore calculation at app launch. Adds HealthKit permission step to onboarding flow with graceful degradation (training-load-only mode if denied). ReadinessScore and SubjectiveCheckIn added to SwiftData model container. Background delivery enabled for automatic HealthKit data syncing.
