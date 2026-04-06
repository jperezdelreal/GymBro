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
