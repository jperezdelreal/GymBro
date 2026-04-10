---
'@bradygaster/squad-cli': patch
---

perf: optimize Android cold start time

- Defer notification channel creation and reminder scheduling to background thread
- Use dagger.Lazy for non-critical Application injections (NotificationHelper, ReminderScheduler, UserPreferences)
- Add AndroidX SplashScreen compat library for consistent splash experience across API levels
- Fix start destination flash by using null initial value for DataStore preference
- Add reportFullyDrawn() for proper TTFD measurement
- Cache Firebase initialization check to avoid repeated try-catch
- Add @Inject constructor to ExerciseSubstitutionEngine for Hilt compatibility
