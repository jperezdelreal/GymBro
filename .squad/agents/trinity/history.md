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
