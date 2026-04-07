# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2024-12-19: UX Approach Defined
- **Core Philosophy:** Speed is a feature. Target 1-2 taps to log a set (common case).
- **Interaction Patterns:** Gesture-based logging (swipe up to complete set, swipe left/right to adjust weight/reps), haptic feedback for all actions, auto-start rest timers.
- **AI Coach Integration:** Contextual overlay (half-screen sheet) during workouts, proactive suggestions limited to 1-2 per workout (high-value only), voice-first interaction.
- **iOS Platform Leverage:** Live Activities for active workouts, Dynamic Island for rest timer countdown, Lock Screen widgets for streak/next workout, Apple Watch companion for simplified logging + health monitoring.
- **Speed Optimizations:** Smart defaults (pre-fill weight/reps from last session), auto-progression to next exercise, inline quick-add for supersets/drop sets/warm-ups.
- **Accessibility:** VoiceOver for all workout logging, Dynamic Type support, one-handed operation (bottom-heavy UI, large touch targets).
- **Competitive Edge vs Strong/Hevy/FitBod:** AI-first coaching (not just analytics), faster logging (1-2 taps vs 3-4), modern iOS features (Live Activities, Dynamic Island).

### 2026-04-06: Cross-Agent Updates (from Team Decisions)
**Decisions affecting Trinity's work:**
- **Product Scope (Morpheus):** MVP includes ultra-fast logging + AI coach + adaptive training. MVP does NOT include voice logging or form video (deferred v1.1+). This validates Trinity's 1-2 tap target and gesture-based UX as sufficient for MVP.
- **AI Strategy (Neo):** AI Coach is conversational (LLM-based). Contextual overlay + sheet modal design is perfect fit for real-time coaching interactions during workouts.
- **Technical Stack (Tank):** SwiftUI + MVVM + Live Activities + ActivityKit. Supports all of Trinity's iOS platform features (Dynamic Island, haptics, Watch companion).
- **Platform Commit (Morpheus):** iOS-first launch (defer Android to v2.0+). Trinity's iOS-native design approach is optimal.

### 2026-04-06: Exercise Library + Workout History Implementation
**Issues #5 & #6 Complete:**
- **Exercise Seed Data:** Created 50-exercise seed dataset with proper categorization (compound/isolation/accessory), equipment types, muscle group tagging (primary/secondary). JSON-based for easy expansion. Seeder service loads on first app launch.
- **SwiftData Architecture:** Established pattern for efficient querying with FetchDescriptor + predicates. ViewModels handle data fetching, Views stay presentation-focused. Clean separation enables testability.
- **Personal Record Tracking:** Implemented PR detection service comparing current sets against historical data across 4 metrics (max weight, max reps, max volume, max e1RM). Foundation for motivation features.
- **UI Patterns:** Searchable lists, grouped sections by date/category, navigation stacks for drill-down. Minimal viable versions shipped — detail views with rich PR displays, calendar views can be enhanced in future iterations.
- **Technical Debt Note:** Views created as MVPs to unblock development. Future iterations should add: detailed exercise instructions UI, calendar grid for history, PR badges on workout details, filtering/sorting controls.

### 2026-04-06: Tech Foundation from Tank (Scaffold & Models)
**Why this matters for Trinity's UX work:**
- Core models now available: UserProfile, Exercise, ExerciseSet, Workout, Program, ProgramDay
- MVVM foundation complete: GymBroApp.swift with ModelContainer, ContentView.swift with 5-tab navigation
- App entry point ready: Trinity can now build UI screens against stable data models
- Performance budget validated: <100ms per operation supports 1-2 tap logging target
- SPM modular architecture: Trinity owns GymBroUI package; can develop independently without merge conflicts on Tank's core models
- Unblocked work: Issue #4 (UI screens) can now proceed without architectural dependencies

### 2026-04-06: Workout Logging UI — Crown Jewel Shipped (Issue #7)
**Implementation highlights:**
- **1-2 tap workflow achieved**: Large 60pt "Complete Set" button in thumb zone, smart defaults eliminate most input
- **Smart defaults working**: SmartDefaultsService queries last workout, applies progression logic (2.5kg compound, 1.25kg isolation)
- **Haptic feedback patterns**: Success on set complete, triple-burst celebration on PR detection (UINotificationFeedbackGenerator)
- **@Observable MVVM**: ActiveWorkoutViewModel drives all state, no manual binding
- **Optimistic UI**: Immediate SwiftData writes, < 100ms set log confirmed in dev testing
- **PR detection**: e1RM calculation with historical comparison, auto-celebration
- **Rest timer integration**: Auto-start after non-warmup sets, skip button, notification support
- **One-handed design**: All primary actions bottom 60%, stepper controls for weight/rep adjustment
- **Comprehensive tests**: 11 test cases cover smart defaults, set completion, warmup flow, PR detection, timer integration
- **Architecture**: Services in GymBroCore (SmartDefaults, HapticFeedback, RestTimer, Notification), Views + ViewModel in GymBroUI
- **Shipped**: PR #20 opened, closes #7
### 2026-04-06: Rest Timer Implementation (Issue #4)
**Pattern Established: Observable Singletons for Global Services**
- Created `RestTimerService` as observable singleton using Swift's @Observable macro (iOS 17+) — preferred over ObservableObject for cleaner syntax and better performance.
- Pattern: Singleton for truly global state (timer), passed service refs via `@State private var service = Service.shared` in views rather than @Environment.
- Avoided @StateObject/@ObservedObject (legacy) in favor of @Observable + @State for iOS 17+ projects.

**Haptic Feedback Design Pattern**
- Medium impact (`UIImpactFeedbackGenerator`) for gentle feedback (10s warning).
- Warning notification (`UINotificationFeedbackGenerator`) for critical events (timer complete).
- Always prepare() generators in init to reduce latency on first trigger.
- Haptics work even in silent mode — critical for gym environment where users silence notifications.

**Timer Implementation: Task-based vs Timer.publish**
- Used `Task.sleep(for: .seconds(1))` over Combine's `Timer.publish` for cleaner cancellation and modern async/await patterns.
- @MainActor annotation ensures UI updates happen on main thread.
- Task cancellation handled gracefully with `Task.isCancelled` checks.

**Background Notifications**
- `UNUserNotificationCenter` for local notifications when app is backgrounded.
- Category identifier "REST_TIMER" enables custom notification actions in future (snooze, skip, add time).
- Automatically cancels notification if timer stopped/skipped to avoid false alerts.

**Model Design: Default Rest Times**
- Added `defaultRestSeconds: Int?` to Exercise model — optional to allow per-exercise overrides.
- Computed property `restTime` provides smart defaults: Compound (180s), Isolation (90s), Accessory (60s).
- Pattern: Model should contain computed logic for business rules (rest time calculation) rather than pushing to ViewModels.

**SwiftUI Animation: Circular Progress**
- `.trim(from:to:)` on Circle() for smooth progress indicator.
- `.animation(.linear(duration: 1), value: progress)` — explicit value binding prevents janky redraws.
- `.rotationEffect(.degrees(-90))` rotates trim to start at top (12 o'clock) instead of right (3 o'clock).
- Color transitions (blue → orange → red) via computed property based on remaining time — clear visual urgency signal.

**Sheet Presentation: Auto-Dismiss Pattern**
- `.presentationDetents([.medium, .large])` enables flexible sheet sizing — medium for quick glance, large for full context.
- `.onChange(of: timerService.isActive)` to auto-dismiss when timer completes — no manual dismiss needed.
- User can still manually close via X button or swipe down.

**Testing Async Services**
- Used `async throws` test methods with `Task.sleep(for: .seconds())` to validate timer countdown behavior.
- Tests must be realistic: Timer counting down over 2-3 seconds is acceptable test duration for accuracy validation.
- Singleton pattern requires careful `setUp()`/`tearDown()` to reset state between tests.

### 2026-04-07: Crash Recovery Implementation (Issue #53)
**Active Workout State Persistence:**
- Added `isActive: Bool` and `isCancelled: Bool` flags to Workout model. `isActive` set true when workout starts (or is created), set false when finished or cancelled.
- WorkoutRecoveryService queries SwiftData for `isActive == true && endTime == nil`, sorted by `updatedAt` descending with `fetchLimit = 1`.
- On app launch, ContentView checks for unfinished workouts via `.task {}` modifier. If found, presents WorkoutRecoveryView sheet with `.interactiveDismissDisabled()` to force decision.
- Resume rebuilds `ActiveWorkoutViewModel` from the persisted Workout — all sets, exercises, and set numbers are preserved since they're already in SwiftData.
- Edge case: multiple stale workouts cleaned up automatically (most recent kept, rest cancelled).

**Recovery UI Pattern:**
- Sheet-based presentation with `.presentationDetents([.medium, .large])` for flexible sizing.
- Summary card shows sets, volume, exercise count, and elapsed time since start.
- Two actions: Resume (green, primary) → navigates to ActiveWorkoutView; Discard (destructive) → marks cancelled.

**Workout.exercises Computed Property:**
- Added `exercises: [Exercise]` computed property to Workout model that returns unique exercises in insertion order by extracting from sets sorted by `createdAt`. Used by recovery view for context and to restore the last active exercise on resume.

**Testing Pattern:**
- 11 test cases using in-memory SwiftData container. Tests cover: find/discard/cleanup service methods, ViewModel isActive lifecycle, cancel flow, set preservation on recovery, exercises ordering, and empty-state nil return.
- PR #57 opened, closes #53.
