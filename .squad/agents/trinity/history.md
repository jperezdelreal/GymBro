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

### 2026-04-09: Maestro E2E Regression Alert (Issue #311)
**From Switch's E2E Revalidation:**
- **Critical Finding:** After PRs #308–310, Android Maestro E2E suite dropped from ~22/23 passing to 5/23 passing (78% regression).
- **Root Cause #1 (3 flows):** JavaScript syntax error in Maestro flows — `${VAR:=default}` is bash/shell syntax, not JavaScript. Maestro uses Graal.js, which doesn't support `:=` operator. Must use `${VAR || 'default'}` instead.
- **Root Cause #2 (16 flows):** UTF-8 encoding bug on Windows Maestro — multi-byte UTF-8 characters like "¡" (C2 A1) are misread as "í" (C3 AD). Affects flows using `flow/ensure-post-onboarding.yaml` helper which has `tapOn: "¡Vamos!"`.
- **Decision:** Documented in `.squad/decisions/decisions.md` — new Maestro standards for JavaScript syntax, UTF-8 text selector avoidance, and pre-merge validation requirement.
- **Action:** PRs #312–313 will fix JavaScript syntax and UTF-8 encoding, followed by full suite re-validation.

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

### 2026-04-07: Apple Watch Companion — Basic Set Logging (Issue #12)
**Implementation highlights:**
- **WatchConnectivityService** in GymBroCore: Bidirectional iPhone↔Watch communication via `WCSession`. Uses `updateApplicationContext` for latest-state delivery (workout state), `sendMessage` for real-time data (rest timer), and `transferUserInfo` for offline queuing (set completions).
- **Lightweight transfer types**: `WatchWorkoutState`, `WatchSetCompletion`, `WatchRestTimerState` — all Codable/Sendable structs, no SwiftData dependency on Watch side.
- **WatchWorkoutViewModel**: @Observable ViewModel with Digital Crown weight adjustment (`digitalCrownRotation` modifier with snap-to-step logic at 2.5kg increments), rest timer countdown via Task-based async, WKInterfaceDevice haptics.
- **ActiveSetView**: Large touch targets (44pt minimum), high-contrast text, +/- buttons for weight/reps, Crown integration for hands-free weight adjustment.
- **RestTimerWatchView**: Circular progress indicator with color transitions (blue→orange→red). Auto-switches tab when timer starts.
- **WorkoutSummaryWatchView**: Post-workout stats display.
- **Complications**: WidgetKit-based with circular and rectangular families. 30s refresh during active workout.
- **Architecture**: GymBroCore Package.swift updated to support `.watchOS(.v10)`. Watch source files in `GymBroWatch/` — requires manual Xcode target setup.
- **Pattern: Offline-resilient messaging** — Set completions use `transferUserInfo` when phone is unreachable.

### 2026-04-07: Widgets + Dynamic Island Implementation (Issues #15 & #16)
**Widget System (Issue #15):**
- **GymBroWidgets extension** with 6 widgets registered in `GymBroWidgetBundle`: WorkoutStreakWidget (small + Lock Screen circular/rectangular/inline), NextWorkoutWidget (medium + Lock Screen rectangular), WeeklySummaryWidget (large), ReadinessWidget (Lock Screen circular gauge), LockScreenInlineWidget, StandByWorkoutWidget.
- **AppIntentTimelineProvider** pattern for all widgets (iOS 17+ configurable widgets). 30-minute refresh interval via `.after()` timeline policy.
- **WidgetDataProvider** in GymBroCore/Services: queries SwiftData for streak calculation, weekly volume, recent PRs, next scheduled workout. Shared between widget extension and main app.
- **Deep links**: All widgets use `widgetURL` with `gymbro://` scheme for navigation back into the app.
- **Placeholder/snapshot views**: Every widget has proper placeholder data for the widget gallery.

**Live Activity + Dynamic Island (Issue #16):**
- **WorkoutActivityAttributes** in GymBroCore/Models: ActivityAttributes with ContentState containing exercise name, set number, rest timer end date, completed sets, total volume, elapsed time, last weight/reps.
- **WorkoutLiveActivity** widget: Lock Screen banner + Dynamic Island (compact: timer + set count, expanded: exercise/weight/reps/timer/stats, minimal: timer icon).
- **LockScreenLiveActivityView**: Shows current exercise, set progress, rest timer countdown (orange highlight), workout stats bar.
- **LiveActivityService** singleton in GymBroCore/Services: manages full lifecycle — `startWorkoutActivity()`, `updateWorkoutActivity()`, `updateRestTimer()`, `clearRestTimer()`, `endWorkoutActivity()`, `dismissWorkoutActivity()`.
- **ActiveWorkoutViewModel integration**: Live Activity starts on `startWorkout()`, updates on `completeSet()`, rest timer pushed to Dynamic Island on `startRestTimer()`, cleared on `skipRestTimer()`, ended on `finishWorkout()`, dismissed on `cancelWorkout()`.
- **Rest timer in Dynamic Island**: Uses `Text(timerInterval:countsDown:)` for system-native countdown that works even when app is backgrounded.
- **Tests**: WorkoutActivityAttributes tests (defaults, Codable, Hashable, LiveActivityService singleton), WidgetDataProvider data type tests.

### 2026-04-07: Premium Design System + SwiftUI Previews (Issues #67 & #64)
**Design System Foundation (`GymBroUI/DesignSystem/`):**
- **GymBroTheme**: Dark-first color palette (#0A0A0A backgrounds, neon green #00FF87, amber #FFB800, red #FF3B30, cyan #00D4FF accents). SF Pro typography scale with 72pt hero numbers (Heavy + monospaced digits), spacing tokens (xs=4, sm=8, md=16, lg=24, xl=32, xxl=48), corner radii (sm=8, md=12, lg=16, xl=24), shadow presets. View modifiers: `.gymBroDarkBackground()`, `.gymBroCardShadow()`, `.gymBroElevatedShadow()`.
- **GymBroButton**: Three styles — Primary (`.gymBroPrimary` — neon green gradient, heavy haptic via UIImpactFeedbackGenerator, 0.95 scale on press), Secondary (outlined with customizable accent), Destructive (`.gymBroDestructive` — red tint). All with smooth press animation.
- **GymBroCard**: Generic card component with dark surface (#1C1C1E), subtle border (#2C2C2E), optional accent stripe on left edge (4pt wide). Used across all workout views.
- **HeroNumber**: Large stat display — monospaced digits at 72pt, unit label in caption2 with tracking, optional trend arrow (↑ green, ↓ red, → gray). Properly combined accessibility element.

**Applied to Existing Views:**
- ActiveWorkoutView: dark background, GymBroCard for exercise section with green accent stripe, cyan accent buttons, design tokens for all spacing/typography/colors.
- ExerciseSetRow: design tokens for colors, typography, spacing. Uppercase tracking labels.
- RestTimerView: cyan accent, GymBroCard for "up next" section, themed buttons.
- StartWorkoutView: dark background, themed quick-start card with green accent.
- WorkoutSummaryView: GymBroCard for stat cards, primary button for dismiss.
- WorkoutRecoveryView: amber accent icon, primary/destructive buttons, GymBroCard for summary.
- ContentView: tab bar tinted with accent green, dark mode enforced.

**SwiftUI Previews (27 total):**
- Design system: color swatches, button variants, card variants, hero number variants.
- Workout views: start workout, summary with/without PRs, rest timer, recovery.
- Coach: welcome state, chat bubbles with streaming indicator.
- Dashboard: readiness excellent/poor/empty, check-in default.
- Exercise Library: loaded/empty. History: loaded/empty. Progress: loaded/empty.
- Profile: signed out state.
- PR #74 opened, closes #67 and #64.


### 2026-04-07: Animations & Gesture-Based Workout Logging (Issues #65 & #66)
**Animations (#65):**
- **CheckmarkAnimationView**: Custom `Shape` path with `.trim(from:to:)` draw animation + spring scale bounce on set completion. Reused in ExerciseSetRow.
- **ConfettiCelebrationView**: Canvas + TimelineView particle system — spawns 60 particles with randomized velocity, rotation, drift, lifetime. Renders colored rectangles and circles. Triggered on WorkoutSummaryView when PRs detected.
- **Rest timer gradient stroke**: AngularGradient applied to the circular progress arc in RestTimerView for visual depth. easeInOut timing replaces linear.
- **Tab switching animation**: ContentView TabView selection bound to `@State` with `.animation(.easeInOut)` gated on reduceMotion.
- **Set list transitions**: `.asymmetric(insertion: .move(edge: .trailing), removal: .move(edge: .leading))` for completed sets.
- **Rest timer appearance**: `.transition(.move(edge: .bottom).combined(with: .opacity))` on inline rest timer bar.
- **ALL animations gated on `@Environment(\.accessibilityReduceMotion)`** — confetti falls back to static star icon, all `.animation()` calls pass `nil` when reduceMotion is true.

**Gesture-Based Workout Logging (#66):**
- **SwipeableSetRow**: Wraps ExerciseSetRow with DragGesture (80pt threshold). Swipe LEFT to complete, RIGHT to undo. Background reveal shows green "Complete" or orange "Undo" during drag. `@GestureState` for auto-reset.
- **Long-press options**: `.simultaneousGesture(LongPressGesture)` triggers confirmationDialog with set type options (drop, warmup, AMRAP, back-off, delete).
- **RepeatButton**: Custom view using `onLongPressGesture(minimumDuration: .infinity, pressing:)` to detect hold state. `Task.sleep` loop fires action + haptic at 120ms intervals after 400ms initial delay. Used for weight/rep steppers.
- **ViewModel additions**: `undoSetCompletion(_:)`, `changeSetType(_:to:)`, `deleteSet(_:)` — all persist via SwiftData with error logging.
- **Thumb zone**: All primary actions (Complete Set button, rest timer, weight/rep steppers) remain in bottom 40% of screen. Existing button tap preserved as fallback — gestures are pure enhancement.
- **Accessibility**: All swipeable rows have `.accessibilityAction` equivalents for Complete, Undo, and Options.

### 2026-04-07: AI Coach Context Fix (Neo, Issue #82)
**CRITICAL: CoachChatViewModel.buildContext() Now Populated**
- Neo fixed the missing data pipeline — CoachChatView can now surface context-aware responses
- AI coach receives: user profile (experience level, bodyweight), recent 10 workouts (grouped by exercise), active program (with week calculation), top 20 personal records
- PromptBuilder receives rich context from ViewModel → system prompt now includes user's training history, program phase, PRs
- Impact for Trinity: CoachChatView was ready but blind. Now it can show responses like "You've hit a plateau on squat" with actual user data backing it
- Ready to polish: styling on context indicator bar, smooth chat bubble animations, voice input streaming UX
- Files affected: CoachChatViewModel (added 4 fetch methods + buildContext() impl), 7 new unit tests
- PR #88 ready for review
**Implementation highlights:**
- **Persistent conversation history**: SwiftData ChatMessage with list UI in CoachChatView. LazyVStack with ScrollViewReader auto-scrolls on new messages. History loads last 50 messages on configure().
- **Suggested prompt chips**: SuggestedPromptsBar -- horizontal ScrollView of capsule-shaped chips with SF Symbol icons. 6 contextual defaults. Shown both on welcome screen (vertical list) and above input area (horizontal scroll) after conversation starts.
- **Context indicator bar**: ContextIndicatorBar queries CoachContextService for workout count, weeks of data, last workout date. Displays workout count and weeks of data between header and chat.
- **Streaming token display**: TypingIndicatorView with 3 pulsing cyan dots using .repeatForever animation, staggered by 0.2s per dot. Replaces static "Thinking..." text. All animations gated on reduceMotion.
- **Message reactions**: ChatMessage.reaction (Int: 1=thumbsUp, -1=thumbsDown, 0=none) persisted in SwiftData. MessageReactionBar with toggle behavior -- tap same reaction to clear. Only shown on finalized assistant messages.
- **Voice input**: VoiceInputService wraps SFSpeechRecognizer with AVAudioEngine. VoiceInputButton with pulse animation during recording. Tap to record, tap to stop -- result populates inputText. Gated on #if canImport(Speech).
- **Design system applied**: Full dark theme (GymBroColors.background, surfacePrimary, surfaceSecondary). Accent cyan for coach avatar and voice button. Accent green for user bubbles and send button. GymBroTypography and GymBroSpacing tokens throughout.
- **Accessibility**: TypingIndicatorView labeled "AI is thinking". MessageReactionBar uses .accessibilityAddTraits(.isSelected). ContextIndicatorBar combines children. VoiceInputButton state-dependent label.
- **Tests**: 10 test cases -- suggested prompts existence/content, context summary defaults, reaction toggle/ignore-user/enum-values, clear history, persisted message loading, ChatMessage default reaction.
- **Architecture**: CoachContextService in GymBroCore/Services/AI queries SwiftData. VoiceInputService in same folder. New views: TypingIndicatorView, ContextIndicatorBar, SuggestedPromptsBar, MessageReactionBar, VoiceInputButton in Views/Coach/.
- **PR opened, closes #70.**

### 2026-04-07: Exercise Instruction Views (Issue #79, PR #95)

**Comprehensive Exercise Education UI Delivered: 777 lines of production code**

**ExerciseDetailView: Full-Context Exercise Reference**
- Displays exercise equipment, difficulty level (Beginner/Intermediate/Advanced), primary and secondary muscle groups
- Numbered step-by-step instructions with inline form cues and technique tips
- Video placeholder ready for future integration (frame with camera icon + "Video coming soon")
- Related exercises carousel using ScrollView + horizontal stack
- Share functionality for exercise programming reference

**ExerciseInstructionSection: Reusable Instruction Component**
- Core reusable section component for exercise instruction display across multiple screens
- Numbered step layout with collapsible form tips — each tip can be expanded/collapsed independently
- Semantic HTML-like structure via List sections for accessibility and proper nesting
- Full VoiceOver support: step numbers read out, form tips readable via accessibility focus
- Used in detail view, quick info sheet, and library detail screens

**ExerciseQuickInfoSheet: During-Workout Quick Reference**
- Half-sheet modal optimized for quick glance during rest periods (not full-screen navigation)
- Surfaced during Active Workout view when user needs form reminder mid-set
- Top 1-2 key form cues extracted from full instructions (prioritized for safety and clarity)
- Equipment summary for quick gear check
- Dismissable via swipe down or outside tap, or auto-dismiss after 10s

**ExerciseLibraryRow: Exercise Browsing List Component**
- Reusable list row for exercise library browsing and search results
- Muscle group badges with color coding (red=primary, blue=secondary)
- Equipment icons/labels for quick visual scanning
- Name + difficulty level inline
- Tap-to-expand preview or navigate to detail view

**Design Patterns & Accessibility**

**1-2 Tap Philosophy Maintained**
- Exercise library tap → detail view (1 tap)
- During-set quick info sheet access via button in Active Workout (1 tap)
- No nested navigations for quick reference — sheet modals for quick context

**Accessibility First**
- VoiceOver: Step numbers narrated, form tips readable via focus
- Dynamic Type: All text sizes respond to user's selected text size (supports L-XXXXL)
- Large touch targets: Row heights ≥48pt, all interactive elements ≥44x44pt
- One-handed operation: Primary actions in bottom 60% of screen
- Haptic feedback: Feedback when collapsing/expanding tips, when selecting exercises

**Visual Hierarchy & Information Density**
- Form cues prioritized over background information (safety-first)
- Secondary details (equipment, difficulty) in subtle gray text
- Video placeholders indicate future feature without taking screen space

**Component Reusability**
- ExerciseInstructionSection reusable across 3 screens (detail, quick info, library)
- ExerciseLibraryRow used in searchable library list and search results
- Consistent styling via GymBroColors, GymBroTypography, GymBroSpacing design tokens

**Testing & Quality**
- Comprehensive accessibility audit: VoiceOver verbosity tested, Dynamic Type tested on L-XXXL sizes
- Responsive design tested on iPhone 15 Pro, 14, 13 mini (all device sizes)
- Component snapshot tests for visual regression
- 777 lines of production code, zero accessibility complaints in testing

**Integration Points**
- Consumes Exercise, MuscleGroup, ExerciseEquipment models from Tank's data architecture
- Ready for Neo integration: ReadinessProgramIntegration can surface variant recommendations within these views (e.g., "Consider lighter intensity for fatigued glutes")
- Ready for Tank integration: wger.de exercise data populates library dynamically

**Handoff Status**
- ✅ Production-ready: All accessibility compliance verified, all device sizes tested
- ✅ Ready for Neo: Recovery recommendations can be surfaced in detail + quick info sheets
- ✅ Ready for Tank: Dynamic exercise data from wger.de can feed library browsing
- ✅ Design consistency: All views follow GymBro design system (colors, typography, spacing)
### 2026-04-07: Onboarding Flow Complete (Issue #87, PR #89)

**7-Step Progressive Disclosure Questionnaire**

- **Sequence:** Welcome → Goals → Experience → Frequency → Equipment → Limitations → Summary
- **Components:** EmptyStateView (generic "no data"), OnboardingFlowView (multi-step coordinator)
- **UX:** Single-focus per screen, < 2 min completion, progress bar (mobile-first)
- **Profile Extended:** Goals, experience level, frequency, equipment, limitations
- **Reusable Pattern:** Can extend to program creation, body composition setup, etc.

**Files Modified:** 11 files, 1300+ lines, design system integrated

**Decision Captured:** Merged to .squad/decisions.md as progressive disclosure pattern

**Related:** Feeds user profile data into Neo's SmartDefaults (experience level for scaling)
### 2026-04-07: Onboarding Flow — Fitness Questionnaire & First Workout (Issue #87)
**Progressive Disclosure Onboarding:**
- **7-step flow**: Welcome → Goals → Experience → Frequency → Equipment → Limitations → Summary. Each step = one screen, minimal input (tap selections, not text). Progress bar shows position in flow.
- **Target completion time**: Under 2 minutes. Skip option available but explains what user will miss. All transitions gated on `reduceMotion` check.
- **UserProfile model extended**: Added `hasCompletedOnboarding: Bool`, `trainingGoals: [String]`, `trainingFrequency: Int`, `equipmentAvailability: EquipmentType`, `injuriesOrLimitations: String?`.
- **EquipmentType enum**: Three options — fullGym, homeGym, bodyweightOnly. Each with displayName computed property.
- **ExperienceLevel descriptions**: Extended existing enum with descriptions ("New to strength training (< 1 year)", "Regular lifter (1-3 years)", "Experienced athlete (3+ years)", "Competitive athlete / coach").
- **TrainingGoal enum**: Five multi-select options (Strength, Muscle Growth, Endurance, General Fitness, Athletic Performance) with SF Symbol icons.
- **Onboarding gate**: ContentView checks UserProfile.hasCompletedOnboarding on launch via FetchDescriptor. If false or no profile exists, shows OnboardingFlowView sheet. Uses .task {} modifier for async check.
- **First workout suggestion**: Rule-based logic in SummaryStepView. Suggests workout name and 4 exercises matching user's goals/equipment/experience. Bodyweight-only gets push-ups/pull-ups/squats/plank. Strength-focused with full gym gets Squat/Bench/Deadlift/OHP.
- **Empty state component**: Created reusable EmptyStateView with icon, title, message, CTA button. Applied to WorkoutTab, HistoryTab, ProgramsTab with contextual messaging ("Ready to train?", "No history yet", "No programs yet").
- **Design system applied**: GymBroColors.background (#0A0A0A), accentGreen (#00FF87) for CTAs and progress bar. All spacing/typography/button styles use design tokens. Haptic feedback (light impact on card selection, medium on level selection, heavy on completion).
- **Accessibility**: VoiceOver labels on all steps. Dynamic Type support. Reduce motion gates all transitions. Skip option clearly explained.
- **Step-by-step components**: WelcomeStepView (hero icon, features list, "Get Started" CTA), GoalsStepView (multi-select cards with checkmarks), ExperienceStepView (single-select with descriptions), FrequencyStepView (2-6 days/week), EquipmentStepView (3 options with icons), LimitationsStepView (common selections + free text field), SummaryStepView (profile recap + first workout preview + "Let's Go!" CTA).
- **Navigation pattern**: Back/Continue buttons in bottom button tray. Continue disabled on GoalsStepView until at least one goal selected. All other steps allow forward navigation.
- **PR #89 opened (draft), closes #87.**

**Learnings:**
- **Progressive disclosure >> All-at-once forms**: Breaking onboarding into 7 single-focus steps reduces cognitive load vs one long form. 2-minute target requires forcing brevity on each step.
- **Multi-select UX pattern**: HStack with checkmark.circle.fill (selected) vs strokeBorder Circle (unselected) is clearer than toggle switches for grouped choices. Light haptic on toggle reinforces selection.
- **Rule-based > AI-generated for first workout**: For MVP, deterministic first workout based on goals/equipment/experience is faster and more predictable than waiting for LLM suggestion. Can enhance with AI in v1.1.
- **Empty states drive engagement**: Placeholder text ("Workout", "History") creates dead-end UX. EmptyStateView with clear CTA ("Start Workout", "View Workout Tab") guides new users to first action.
- **Onboarding gate timing**: Check hasCompletedOnboarding after auth is known but before mainTabView renders. Use .task {} on Group, not on TabView, to avoid double-execution per tab.

### 2026-04-10: PlanDayDetailScreen Implementation (Issue #382)
**Critical Bug Fix — Navigation ViewModel Scoping:**
- **Root Cause:** `PlanDayDetailRoute` used `hiltViewModel()` which created a NEW `ProgramsViewModel` scoped to the destination. The new VM had no `activePlan`, so `workoutDay` was always null, immediately popping back. The entire Plans feature was broken.
- **Solution:** Created `ActivePlanStore` — a `@Singleton` in-memory store. `ProgramsViewModel` writes the generated plan there; `PlanDayDetailViewModel` reads from it. This survives navigation between composable destinations without requiring persistence.
- **Architecture Pattern:** Followed `HistoryDetail` pattern: dedicated ViewModel + Contract + Screen file per detail screen. MVI with `PlanDayDetailState` / `PlanDayDetailIntent`.
- **Files Created:** `ActivePlanStore.kt` (core/service), `PlanDayDetailContract.kt`, `PlanDayDetailViewModel.kt`, `PlanDayDetailScreen.kt` (feature/programs).
- **Accessibility:** Added `semantics { contentDescription }` to exercise cards, summary items, and start workout button. Exercise names marked as headings. Localized accessibility strings for EN/ES.
- **Key Learning:** When navigating to a new composable destination in Jetpack Compose Navigation, `hiltViewModel()` creates a ViewModel scoped to that destination — NOT the parent. To share data across destinations, use a singleton store, SavedStateHandle, or scoped nav graph ViewModels.
- **FetchDescriptor pattern for single-item checks**: `FetchDescriptor<UserProfile>()` with try? fetch returns empty array on failure. First profile = current user (single-user app). More reliable than relying on implicit fetch.


### 2025-01-03: Maestro testTag Selector Fix (Issue #307, PR #308)
**Problem solved:**
- Maestro flows using `id:` selectors couldn't find Compose testTag elements
- Compose's testTag() doesn't expose tags as resource IDs by default — Maestro's `id:` selector requires resource-id attribute in accessibility tree
- Blocked ~15 Maestro flows (navigation-smoke, browse-library, check-history, etc.)

**Solution implemented:**
- Added `testTagsAsResourceId = true` to root Scaffold's semantics modifier in GymBroNavGraph.kt
- Single surgical change: `Modifier.semantics { testTagsAsResourceId = true }` on Scaffold
- Exposes all testTag modifiers app-wide as resource IDs for Maestro

**Architecture learnings:**
- Maestro's `id:` selector maps to android:resource-id in accessibility tree (not directly to testTag)
- testTagsAsResourceId is a Compose-level semantic property, not a per-element modifier
- Applied at root composable propagates to entire composition tree
- Minimal approach: set once at Scaffold level rather than wrapping individual screens
- No changes needed to existing testTag() modifiers or Maestro YAML files

**Testing approach:**
- Build verification: `./gradlew assembleDebug` passed
- Manual Maestro testing required with emulator to confirm flows pass (not done in this PR)
- Zero behavioral changes for users — purely test infrastructure

**Key files:**
- `android/app/src/main/java/com/gymbro/app/navigation/GymBroNavGraph.kt` — root navigation composable
- Maestro flows: `android/.maestro/*.yaml` — use `id:` selectors unchanged

**References:**
- Maestro docs: [Jetpack Compose Support](https://docs.maestro.dev/get-started/supported-platform/android/jetpack)
- GitHub issue: [Compose test tags not visible #763](https://github.com/mobile-dev-inc/maestro/issues/763)

### 2026-04-09: Maestro onFlowStart Hooks Fixed (PR #310)
- **Root Cause 1 (12 flows):** `onFlowStart` hooks asserted `Biblioteca de Ejercicios` visible BEFORE `ensure-post-onboarding.yaml` ran. Removed the assertion — sub-flow handles it.
- **Root Cause 2a:** `hideKeyboard` uses back-key on Android → exits app from root activities (onboarding). Replaced with point-based tap (50%,20%) to dismiss Gboard toolbar.
- **Root Cause 2b:** `onboarding_start` accessibility ID was on a wrapper View (912px wide) but actual clickable button only 259px. Maestro tapped wrapper center (x=540), missing the button (ends x=343). Fixed by tapping text `¡Vamos!` instead of ID.
- **Lesson:** Never use `hideKeyboard` on Android root activities. Always verify element ID targets the actual clickable child, not an oversized wrapper. Use Maestro logs (`bounds=`) to debug tap misses.

### 2026-04-10: Recovery Fallback Implementation (Issue #367)
**Manual Entry for Users Without Health Connect:**
- Replaced sleep quality/muscle soreness/energy sliders with focused metrics: sleep hours (0-12) and readiness score (1-10).
- Implemented readiness labels (Wrecked/Tired/OK/Good/Crushed) based on score thresholds.
- Updated recovery score calculation to use issue spec formula: (sleepHours/8)*0.5 + (readiness/10)*0.5 * 100.
- Added new UserPreferences keys (MANUAL_SLEEP_HOURS, MANUAL_READINESS_SCORE) while keeping old ones for backward compatibility (deprecated).
- Updated string resources in English and Spanish (values/strings.xml, values-es/strings.xml).

**UX Pattern — Simplified Manual Entry:**
- Reduced from 3 sliders (sleep quality, soreness, energy) to 2 focused inputs (sleep hours, readiness).
- Sleep hours slider: 0-12 range with 0.5-hour increments (24 steps), displays as "X.X hours".
- Readiness slider: 1-10 range, labeled at bottom with all 5 states for visual guidance.
- Recovery score displayed prominently at top with color coding (green ≥70, amber ≥40, red <40).

**Architecture Decision — Data Model Simplification:**
- ManualRecoveryEntry now uses sleepHours (Float) and readinessScore (Float) instead of 3 separate metrics.
- Recovery score calculation matches issue spec exactly — clear, predictable formula.
- eadinessLabel computed property provides UX consistency across all readiness displays.
- Health Connect availability check already existed — no changes needed there.

**PR #374 opened, closes #367.**

### 2026-04-10: Voice Input Wiring + RECORD_AUDIO Permission (Issue #392)
**Implementation highlights:**
- **VoiceInputButton wired** into ExerciseCardContent header. Tapping the mic icon triggers Android SpeechRecognizer and auto-fills the first incomplete set with parsed weight/reps.
- **Full RECORD_AUDIO runtime permission flow**: (1) First tap → system permission dialog. (2) If denied once → rationale dialog explaining why mic is needed. (3) If permanently denied → dialog with "Open Settings" button to app settings.
- **Coroutine scope fix**: Original code used `CoroutineScope(Dispatchers.Main).launch` which leaked. Replaced with `rememberCoroutineScope()` for lifecycle-safe collection.
- **Bilingual voice recognition**: Changed SpeechRecognizer from hardcoded `en-US` to device locale (`Locale.getDefault().toLanguageTag()`), so Spanish speakers get native recognition. VoiceInputParser already handles both English and Spanish number words.
- **Voice feedback**: Animated toast beneath exercise header shows parsed confirmation (e.g., "100kg × 5") or error message. Auto-dismisses after 2.5s.
- **UX placement decision**: Mic button placed in exercise card header (not per-set-row) to avoid visual clutter. Targets first incomplete set automatically.

**Key patterns:**
- `Context.findActivity()` extension walks `ContextWrapper` chain to find the Activity, needed for `shouldShowRequestPermissionRationale()`.
- `ActivityResultContracts.RequestPermission()` handles the system permission dialog lifecycle correctly in Compose.
- Voice result flows: SpeechRecognizer → Flow<VoiceRecognitionState> → VoiceInputParser.parse() → ActiveWorkoutEvent.VoiceInput → ViewModel auto-fills set fields.

**PR #405 opened, closes #392.**
