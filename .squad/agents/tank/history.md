# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2026-01-XX: Initial Technical Architecture Design

**Architecture Decisions:**
- **MVVM over TCA:** Chose MVVM for MVP speed and simplicity. TCA is overkill for initial scope, but architecture is modular enough to migrate critical flows later if needed.
- **SwiftData over Core Data:** SwiftData is production-ready in 2026, offers 48% faster dev cycles, and is the Apple-recommended path forward. No legacy iOS support needed.
- **CloudKit for Sync:** Zero backend code, free tier is generous, privacy-first. Apple handles servers, scaling, and auth. Perfect fit for offline-first architecture.
- **Cloud AI (OpenAI) First, On-Device Later:** MVP uses OpenAI API via backend proxy for cost control and rate limiting. Migration path to Core AI/on-device models planned for v2.0 when Apple's framework matures.
- **SPM Only, No CocoaPods:** Swift Package Manager is native, fast, and future-proof. Minimize third-party dependencies entirely for MVP.

**Offline-First Strategy:**
- Local database (SwiftData) is source of truth. All writes persist locally immediately.
- CloudKit sync is opportunistic background operation.
- Conflict resolution: Last-Writer-Wins with property-level merging for critical fields.
- Losing user's PR history is unforgivable—architecture prioritizes data durability over feature velocity.

**Performance Budgets:**
- App launch: < 1s cold launch
- Workout logging: < 100ms per set
- App size: < 50 MB download
- Memory: < 100 MB steady-state
- Sync: < 5s for 1000 workouts

**MVP Technical Scope:**
- Core features: Workout logging, program management, history, basic HealthKit (sleep, HRV)
- Infrastructure: SwiftData + CloudKit + Sign in with Apple
- Deferred to v2.0: Apple Watch, on-device AI models, social features, advanced analytics

**Risk Mitigation:**
- Data loss prevention: Local-first, CloudKit backup, export feature, conflict testing
- API cost control: Rate limiting (50 AI requests/day), caching, budget monitoring ($500/month target)
- CloudKit quotas: Batch uploads, compression, usage monitoring (well under 1GB/user limit)

**Technology Stack:**
- UI: SwiftUI + @Observable
- Persistence: SwiftData
- Sync: CloudKit
- Auth: Sign in with Apple
- AI: OpenAI API (cloud)
- Health: HealthKit
- Build: SPM
- CI/CD: GitHub Actions + Xcode Cloud

**Open Questions:**
- Apple Intelligence API availability timeline (WWDC 2026?)
- Core AI framework migration plan for iOS 27
- Real-world CloudKit usage patterns vs estimates
- Fine-tuned AI model cost/benefit for powerlifting domain

### 2026-04-06: Cross-Agent Updates (from Team Decisions)
**Decisions affecting Tank's work:**
- **Product Scope (Morpheus):** MVP is laser-focused—logging + AI + adaptive training. Defer Watch, form video, voice logging, social. Validates Tank's "SPM only, minimal dependencies" approach.
- **UX Performance (Trinity):** Needs <100ms per set logging. Tank's performance budget of <100ms per set is perfect fit.
- **AI Architecture (Neo):** Neo is targeting on-device LLM eventually (v2.0+). Tank's decision to use OpenAI API for MVP is sound—avoids waiting for iOS 27, enables launch in 10 weeks. Clear migration path.
- **Team Consensus:** All four agents agree on MVVM + SwiftData + CloudKit. No architectural conflicts. Tank has green light to begin prototyping immediately.

### 2026-04-06: Xcode Scaffold + SwiftData Models Implementation (Issues #2, #3)

**Project Structure Decisions:**
- **SPM Modular Architecture:** Created 3 separate Swift packages (GymBroCore, GymBroUI, GymBroKit) for clean separation. This enables parallel development across agents without merge conflicts—Trinity can own GymBroUI, Tank owns Core.
- **Package.swift Only:** No Xcode project file (.xcodeproj). Pure SPM approach keeps the repo cleaner and makes CI/CD simpler. Users open Package.swift directly in Xcode 16+.
- **iOS 17.0 Minimum:** SwiftData is production-stable, and our target users (serious lifters) are early adopters who stay current with iOS.

**SwiftData Model Design:**
- **Relationship Strategy:** Used `@Relationship(deleteRule: .cascade/.nullify)` carefully. Cascade for owned children (Workout → Sets), nullify for references (Set → Exercise). Prevents accidental data loss.
- **Naming: ExerciseSet not Set:** "Set" is a Swift collection type. Avoiding namespace collision saved hours of future debugging.
- **Computed Properties for Formulas:** e1RM calculated via Epley formula (weight × (1 + reps/30)). Keeps formulas DRY and testable.
- **Unit Conversion at Model Level:** All weights stored as kg (metric). Conversion to lbs happens on read via computed properties. Single source of truth prevents sync bugs.
- **CloudKit Compatibility by Design:** No optionals on required fields, all types are CloudKit-compatible (String, Int, Double, Date, UUID, enums as Codable). Migration to CloudKit sync (issue #5) will be straightforward.

**Performance Considerations:**
- **Indexes for Queries:** SwiftData auto-indexes relationships and IDs. For date-based queries (History screen), we'll add explicit indexes in future migration if needed.
- **Lazy Loading:** Relationship arrays are lazy-loaded by SwiftData. WorkoutSet.exercise won't fetch until accessed—critical for fast list rendering.

**Key Files Created:**
- `Package.swift` (root): Main app package with local package dependencies
- `Packages/GymBroCore/Package.swift`: Models and services package
- `Packages/GymBroCore/Sources/GymBroCore/Models/`: Workout.swift, Exercise.swift, ExerciseSet.swift, Program.swift, UserProfile.swift
- `GymBro/GymBroApp.swift`: App entry point with ModelContainer configuration for all 6+ model types
- `GymBro/ContentView.swift`: 5-tab navigation shell (Workout, History, Programs, Coach, Profile)
- `README.md`: Build instructions, architecture overview, tech stack

**Testing Strategy:**
- Basic unit tests in `GymBroCoreTests/GymBroCoreTests.swift` to verify model relationships and computed properties compile.
- Full test suite deferred to issue #8 (Switch's domain)—Tank just validates models are testable.

**Unblocked Issues:**
This foundation unblocks all Phase-1 work:
- Issue #4: UI screens (Trinity can now build against models)
- Issue #5: CloudKit sync (Tank can extend models with CKRecord mapping)
- Issue #6: AI coach (Neo can query workout history)
- Issue #7: HealthKit integration (Tank can link bodyweight to UserProfile)

**Risks Mitigated:**
- No Xcode project file means no merge conflicts on .xcodeproj XML
- Modular packages prevent "big ball of mud" codebase
- SwiftData thread-safety via actor isolation prevents race conditions

**Open Questions:**
- ModelContainer configuration: single shared container vs. per-view? Leaning toward single shared (simpler, faster).
- Migration strategy when model schema changes: SwiftData auto-migration works for simple changes, but complex migrations need explicit ModelMigrationPlan.
- CloudKit sync conflict resolution: Last-Writer-Wins is simple but loses data. Evaluate property-level merging for v1.1.

### 2026-04-07: AI Coach Implementation (Issue #10)

**Architecture Decisions:**
- **Protocol-based AICoachService:** Abstraction layer (`AICoachService` protocol) with `AzureOpenAICoachService` (cloud) and `DeterministicCoachFallback` (offline) implementations. This enables seamless swap to on-device models when Core AI / iOS 27 matures.
- **Lightweight Snapshots over @Model References:** Created `UserProfileSnapshot`, `WorkoutSnapshot`, `PRSnapshot` structs instead of passing SwiftData `@Model` objects to the AI service layer. Prevents threading issues (SwiftData models aren't `Sendable`) and keeps the AI service testable without a model container.
- **Environment-based Configuration:** `AICoachConfiguration` loads Azure OpenAI endpoint/key from `ProcessInfo.processInfo.environment`. No hardcoded secrets. Production will use Xcode scheme env vars or a config file.
- **Streaming-first API Design:** Both cloud and offline services implement `AsyncThrowingStream<String, Error>` for response streaming. The offline fallback simulates streaming by yielding words with delays — consistent UX regardless of backend.

**Safety Architecture:**
- **SafetyFilter runs pre-API:** Medical/dangerous queries are intercepted before hitting Azure OpenAI, saving API costs and preventing unsafe responses from ever being generated.
- **Mandatory Disclaimer:** Every AI response gets a disclaimer appended. Cannot be turned off.
- **Dual-layer Safety:** SafetyFilter catches keywords client-side; system prompt instructs the model to refuse medical advice server-side. Belt and suspenders.

**Offline-First Design:**
- `DeterministicCoachFallback` covers 7 common training topics: rest times, deloads, plateaus, warm-ups, RPE/RIR, progressive overload, workout suggestions.
- Rule engine uses the user's experience level to personalize advice (e.g., different deload frequency for beginners vs advanced).
- ViewModel auto-falls back to offline service on cloud failure — transparent to the user.

**Cost Control:**
- `UsageLimiter`: 5 free questions/week persisted in UserDefaults, weekly auto-reset.
- `RateLimiter`: 20 requests/minute in-memory cap to prevent runaway API costs.
- GPT-4o-mini chosen for cheapest per-token cost on Azure.

**Files Created (19 files, ~1600 lines):**
- Models: `ChatMessage.swift` (SwiftData)
- Services: `AICoachService.swift`, `AzureOpenAICoachService.swift`, `DeterministicCoachFallback.swift`, `PromptBuilder.swift`, `SafetyFilter.swift`, `AICoachConfiguration.swift`, `UsageLimiter.swift`
- UI: `CoachChatView.swift`, `ChatMessageBubble.swift`, `CoachFloatingButton.swift`, `CoachChatViewModel.swift`
- Tests: `PromptBuilderTests.swift`, `SafetyFilterTests.swift`, `DeterministicCoachFallbackTests.swift`

**Open Questions:**
- Azure Functions proxy for production (rate limiting, prompt versioning server-side) — not implemented yet, using direct client→Azure OpenAI for MVP
- Conversation history size management — currently fetches last 50 messages; may need cleanup strategy for long-running users
- Multi-turn context: currently each request sends only system + user message; should we send conversation history for multi-turn coherence?

### 2026-04-07: Sign in with Apple + CloudKit Sync (Issue #13)

**Architecture Decisions:**
- **ASAuthorizationController Delegate Pattern:** Used `withCheckedThrowingContinuation` to bridge the delegate-based Apple Sign In API into async/await. AuthenticationService is both the service and the delegate, keeping the flow self-contained.
- **Keychain for Credentials:** User identifier, identity token, display name, and email stored in Keychain via existing `KeychainService`. Using `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` for background sync access.
- **CloudKit via SwiftData Built-in Integration:** `ModelConfiguration(cloudKitDatabase: .private(containerIdentifier))` — zero custom CloudKit code. SwiftData handles all CKRecord mapping, zone setup, and conflict resolution automatically.
- **Local-Only Mode by Default:** App is fully functional without signing in. `ModelConfiguration(cloudKitDatabase: .none)` for unsigned users. No data gating behind auth.
- **Typed State Machine:** `AuthState` enum (unknown → signedOut → signingIn → signedIn → error) drives UI reactively. No boolean flags.
- **Last-Writer-Wins Conflict Resolution:** SwiftData+CloudKit default. Property-level merging for workout sets deferred to v1.1 per retro decision.

**Service Architecture:**
- `AuthenticationService` — owns auth flow, credential lifecycle, session restoration
- `CloudKitSyncService` — monitors sync events via NotificationCenter, tracks iCloud account status, provides `ModelConfiguration` factory method
- Both services are `@Observable` + `@MainActor`, injected from `GymBroApp` → `ContentView` → `ProfileView`

**Files Created (7 new, 3 modified):**
- Services: `AuthenticationService.swift`, `AuthState.swift`, `CloudKitSyncService.swift`
- UI: `SignInView.swift`, `ProfileView.swift`, `SyncStatusView.swift`
- Config: `GymBro.entitlements`
- Tests: `AuthenticationServiceTests.swift`
- Modified: `GymBroApp.swift`, `ContentView.swift`

**Deferred to v1.1:**
- Offline sync queue (writes queued when offline, replayed on reconnect)
- Property-level merge conflict resolution for workout sets
- iCloud storage usage display (CKContainer doesn't expose per-app storage easily)


### 2026-04-07: HealthKit Integration (Issue #11, PR #56)

**Architecture Decisions:**
- **Protocol-based HealthKitService:** `HealthKitService` protocol with `HealthKitManager` (real) and `MockHealthKitService` (testing) implementations. Same pattern as AI coach — swap implementations cleanly.
- **Read-only Access:** GymBro only reads health data (sleep, HRV, resting HR, active energy). No writes to HealthKit — keeps App Review simple and privacy posture clean.
- **Offline-first Cache:** `HealthMetric` and `HealthBaseline` SwiftData models cache HealthKit readings locally. Expensive HK queries run once, cached results served until stale (1hr default).
- **User's Own Baseline:** 30-day rolling average with standard deviation. Z-score comparison against user's own history, not population averages. This is critical for recovery signals — a 45ms HRV means different things for different athletes.

**Data Types Requested:**
- `.restingHeartRate` — daily resting HR (discrete average)
- `.heartRateVariabilitySDNN` — HRV in ms (discrete average)
- `.sleepAnalysis` — category samples with stage breakdown (inBed, awake, core, deep, REM, unspecified)
- `.activeEnergyBurned` — daily kcal (cumulative sum)

**Key Implementation Details:**
- `HKStatisticsCollectionQuery` for time-series daily aggregates (RHR, HRV, energy)
- `HKSampleQuery` for sleep analysis (category samples need stage-level granularity)
- Sleep samples grouped by "night" — shifted by -12h to bucket overnight sessions correctly
- Background delivery: `enableBackgroundDelivery` + `HKObserverQuery` with `defer { completionHandler() }` pattern
- Graceful degradation: `HKHealthStore.isHealthDataAvailable()` checked before every operation, returns empty/cached data on iPad

**Files Created (7 files, ~1130 lines):**
- Models: `HealthMetric.swift` (HealthMetric, HealthBaseline, SleepStageBreakdown, HealthMetricType)
- Services: `HealthKitService.swift` (protocol + DTOs), `HealthKitManager.swift` (real impl), `MockHealthKitService.swift` (testing), `HealthKitDataSync.swift` (SwiftData cache sync)
- Tests: `HealthKitManagerTests.swift` (20 tests)
- Modified: `GymBroApp.swift` (added HealthMetric + HealthBaseline to ModelContainer)

**Privacy:**
- Health data never leaves device — not sent to cloud AI or CloudKit
- Only request types actually used by the app (App Review rejects over-requesting)
- Info.plist needs NSHealthShareUsageDescription (to be configured in Xcode project)

**Open Questions:**
- Background delivery frequency: `.hourly` for all types — might want `.immediate` for sleep if recovery score needs to update faster
- HealthKit entitlement + Info.plist usage descriptions: need Xcode project file configuration (currently SPM-only repo)
- Stale data cleanup: currently inserts new HealthMetric rows on each sync — need a cleanup strategy to prevent unbounded growth
- Integration with recovery/readiness score engine (downstream consumer of this data)

### 2026-04-08: wger.de API Integration (Issue #77, PR #94)

**Architecture Decisions:**
- **ExerciseSource Enum:** Added `ExerciseSource` (seed/wger/custom) to `Exercise` model for origin tracking. Existing seeded exercises default to `.seed`, user-created to `.custom`, and API-imported to `.wger`.
- **Actor-based Services:** Both `WgerAPIService` and `ExerciseSyncService` are Swift actors for thread-safe concurrent access.
- **Offline-First Design:** API sync enhances the exercise library but never blocks core functionality. All sync failures are logged and swallowed — the app works fine without network.
- **24-Hour Sync Throttle:** `ExerciseSyncService` uses UserDefaults-based timestamp to avoid hammering wger.de. Respects their public API rate limits.

**Key Implementation Details:**
- `WgerAPIService`: Handles paginated fetching of exercises, muscles, and equipment from wger.de REST API. Returns clean DTOs (`WgerExerciseData`, `WgerMuscleData`, `WgerEquipmentData`). Handles HTTP 429 (rate limited) as a distinct error case.
- `ExerciseSyncService`: Orchestrates multi-page fetch, maps wger data to GymBro models (categories, equipment, muscle groups), deduplicates by name similarity (case-insensitive + substring matching), strips HTML from descriptions, and batch-inserts into SwiftData.
- Wger muscle IDs mapped to GymBro's 19 muscle groups via static dictionary with API-name fallback.
- Wger equipment mapped by name keywords (barbell, dumbbell, machine, etc.).
- Wger categories mapped by ID (legs/chest/back → compound, arms/abs/calves → isolation, others → accessory).

**Files Created/Modified (4 files, ~823 lines):**
- Modified: `Exercise.swift` (added `ExerciseSource`, `source`, `wgerId`, `lastSyncedAt`)
- Created: `WgerAPIService.swift` (API client with pagination and error handling)
- Created: `ExerciseSyncService.swift` (sync orchestration, mapping, deduplication)
- Created: `WgerAPIServiceTests.swift` (15 tests with MockURLProtocol)

**Testing Strategy:**
- Mock `URLProtocol` subclass injects fake HTTP responses — no real network calls in tests.
- Tests cover: successful decoding, pagination extraction, rate limit handling, HTTP errors, decoding errors, empty results, language/page params, muscle/equipment endpoints, ExerciseSource codability.

### 2026-01-07: wger.de API Integration for Exercise Library Expansion (Issue #77)

**Problem:** Limited exercise library (80 seed exercises). Need runtime expandability without maintaining a massive static JSON file.

**Solution — wger.de REST API Integration:**
- **WgerAPIService:** Actor-based HTTP client for wger.de API endpoints (/exercise/, /muscle/, /equipment/)
  - Pagination support (API returns 20 results per page)
  - Rate limit detection (429 handling)
  - English language filter (language=2)
  - Comprehensive error handling (network, HTTP, decoding)
  - Returns normalized data structures for downstream processing

- **ExerciseSyncService:** Orchestrates sync between wger.de and local SwiftData cache
  - 24-hour sync throttle (UserDefaults-backed) to be good API citizen
  - Fetches metadata first (muscles, equipment) to build lookup tables
  - Pagination loop with 10-page cap (~200 exercises per sync)
  - Deduplication by lowercase name comparison against existing exercises
  - Quality filters: skip test data, require min 3-char name + 10-char description
  - HTML tag stripping from wger descriptions (many include <p>, <strong>, etc.)
  - Muscle group mapping: wger's 15 muscle groups → GymBro's 19 muscle groups
    - e.g., "Biceps brachii" → "Biceps", "Quadriceps femoris" → "Quadriceps"
    - Handles primary vs secondary muscle designations
  - Equipment mapping: 13 wger types → 8 GymBro enums (.barbell, .dumbbell, etc.)
  - Category heuristic: bodyweight → accessory, 2+ primary muscles → compound, else isolation
  - NEVER modifies user custom exercises (isCustom flag protection)

- **Exercise Model Extension:**
  - Added ExerciseSource enum: .seed, .wger, .custom
  - Added source: ExerciseSource field to track origin
  - Added optional wgerId: Int? for wger.de exercise ID reference
  - Added optional lastSyncedAt: Date? for incremental sync capability (future enhancement)

**Offline-First Architecture:**
- Local SwiftData is source of truth — API sync is background enhancement, not a blocker
- App works fully without internet
- Cached wger exercises persist locally, available offline
- Sync failures are logged but don't crash or show errors to user

**Testing Strategy:**
- 15 unit tests using MockURLProtocol (not URLSession subclass — proper mocking)
- Coverage: successful fetch, pagination, empty results, rate limits, HTTP errors, network failures, decoding errors
- Codable conformance tests for all data structures
- No live API calls in tests (fully mocked HTTP layer)

**Design Decisions:**
- **Actor isolation:** WgerAPIService is an actor to prevent race conditions on session state
- **Max 10 pages per sync:** Safety cap to prevent runaway API consumption if wger.de pagination breaks
- **Case-insensitive name deduplication:** "Barbell Bench Press" matches "barbell bench press" (prevents near-duplicates)
- **HTML stripping:** wger.de descriptions often include markup — strip it for clean display
- **Muscle mapping is incomplete:** Only 15 of wger's muscles mapped (enough for MVP). Rest will get skipped (empty muscle array check).
- **Equipment fallback:** Unmapped equipment defaults to .other rather than failing

**Future Enhancements (not in this PR):**
- Incremental sync using lastSyncedAt timestamps
- User preference for auto-sync on/off
- Sync progress UI (currently silent background operation)
- More comprehensive muscle mapping coverage
- Image support (wger.de has exercise images, but we skip them for MVP)
- wger.de workout templates (separate API endpoint, defer to v2.0)

**Files Changed:**
- Packages/GymBroCore/Sources/GymBroCore/Models/Exercise.swift — added source tracking
- Packages/GymBroCore/Sources/GymBroCore/Services/API/WgerAPIService.swift — 231 lines
- Packages/GymBroCore/Sources/GymBroCore/Services/API/ExerciseSyncService.swift — 291 lines
- Packages/GymBroCore/Tests/GymBroCoreTests/API/WgerAPIServiceTests.swift — 257 lines (15 tests)

**Branch:** squad/77-wger-api-integration  
**PR:** #94 (draft)  
**Next Steps:** CI will validate build, then merge to master

### 2026-04-07: Round 4 Summary and Team Integration

**Neo Collaboration (Week-Level Variation):**
- Neo's ProgramWeek model (issue #81, PR #93) provides the data structure that AI coach now injects periodization recommendations into
- Tank's PromptBuilder now accepts CoachContext with currentWeek, weekIntensity, and plannedProgression data
- Downstream: Readiness-driven week adjustments become possible (skip deload week, extend accumulation if user is fresh)

**Switch Quality Audit (Findings Documented):**
- Switch's peer review (issue #29) filed 6 critical/high issues: 78% test coverage gap, 2 @MainActor violations, concurrency bugs
- Tank acknowledges: CoachChatViewModel and ActiveWorkoutViewModel are @Observable but missing @MainActor — data race risk on every chat/workout
- Next sprint: Tank will handle concurrency fixes as blocking work before MVP ship
### 2026-04-07: AI Coach Context Pipeline Complete (Neo, Issue #82)
**No Schema Changes Needed — Uses Existing Models**
- Neo completed the three-layer data pipeline: ViewModel → Snapshots → PromptBuilder
- `CoachChatViewModel.buildContext()` now fetches user profile, recent workouts, active program, personal records

### 2026-04-07: Android Skills Deep Analysis (Morpheus, Issue #134)
**Cross-Agent Note from Scribe:** Morpheus's analysis recommends 5 specific Android skills to install immediately (P0):
1. **compose-expert** (aldefy/compose-skill) — Premier Compose skill. 17 files + androidx source backing. Replaces 6 redundant Compose sub-skills.
2. **android-architecture** (new-silvermoon) — Clean Architecture + Hilt + modularization. Maps to GymBro iOS structure.
3. **android-data-layer** (new-silvermoon) — Repository + Room + offline-first. Aligns with Tank's offline-first philosophy.
4. **kotlin-mvi** (Meet-Miyani) — Event/State/Effect pattern. Matches iOS ViewModel approach (similar to Tank's MVVM).
5. **android-testing** (new-silvermoon) — Unit/Hilt/Screenshot testing. Enables Android TDD parity (mirrors Swift testing work).

**P1 Deferred (Sprint 2):**
- android-gradle, health-connect (GAP), firebase-android (GAP), workmanager (GAP)

**Key Insight for Tank:** Android's offline-first + MVI pattern directly complements iOS MVVM + SwiftData approach. No architectural conflicts—consistent data layer strategy across platforms (both are local-first with cloud sync).

**Critical Gaps Identified:** Health Connect (recovery data), Firebase (cloud/push), WorkManager (background sync) not in source repos—need separate skills.

**Documentation:** See decisions.md, orchestration-log/2026-04-07T17-06-morpheus.md
- All fetching done efficiently via SwiftData predicates (not in-memory iteration)
- Uses existing Workout, ExerciseSet, Program, UserProfile models — no new @Model types
- PromptBuilder infrastructure confirmed working; ready to receive rich context
- Impact for Tank: No backend changes needed. Integration test ready (manual verification of PromptBuilder receiving full context)
- Files affected: CoachChatViewModel (4 new fetch methods), 7 new unit tests
- PR #88 ready for review

### 2026-04-07: Exercise Library Expansion (Tank, Issue #80, PR #92)

**Exercise Data Scale-Up:**
- Expanded exercise library from 80 to 200 exercises (150% growth)
- Average instruction length improved from 80 to 226 chars (3x more detailed)
- All new exercises include: setup, execution, breathing cues, common mistakes, safety warnings
- Instructions written for practical gym use, not generic descriptions

**Exercise Coverage Added:**
- **Barbell variations:** High/low bar squat, SSB squat, Anderson squat, Hatfield squat, trap bar deadlift, snatch grip deadlift, block pulls, paused deadlifts, Spoto press, Larsen press, board press, reverse band bench, feet-up bench, tempo variations, pin squats
- **Olympic lifts:** Hang power clean/snatch, push jerk, split jerk, clean pulls, snatch pulls, power snatch, overhead squat, thrusters
- **Cable exercises:** 10+ new movements including lateral raises, rear delt flies, wood chops, pull-throughs, Pallof press, upright rows, Y-raises
- **Kettlebell:** Clean, snatch, windmill, double front squat, overhead press, double swing, single-arm row
- **Dumbbell variations:** Floor press, pullover, neutral grip press, single-arm press, RDL, leaning lateral raise, chest-supported row, split squat, step-up, farmer's march
- **Machine exercises:** Smith machine variations, hack squat, lying/seated leg curls, chest/shoulder press machines, lat pulldown variations (wide/close grip), seated cable row, leg press variations (narrow/wide stance), calf raises
- **Bodyweight progressions:** Archer push-up, pseudo planche push-up, L-sit, dragon flag, ring dips/push-ups, front/back lever, weighted pull-ups/dips
- **Band exercises:** Face pulls (dedicated exercise), dislocates, assisted pull-ups, resisted push-ups, good mornings, resisted squats
- **Isolation movements:** Bayesian cable curl, sissy squat, cable kickback, glute-ham raise, wrist roller, wrist curls (regular/reverse), side bends, spider curl, overhead cable tricep extension, close/wide-grip EZ bar curls, JM press, Tate press, Zottman curl, cable hammer curl
- **Strongman movements:** Yoke walk, sandbag carry, tire flip, sled push/drag
- **Specialty movements:** Landmine press/row, one-arm barbell row, Bradford press, dumbbell incline fly, decline dumbbell press, reverse pec deck, cable Y-raise, Copenhagen plank, landmine rotation, suitcase carry, birddogs

### 2026-07-XX: Android Project Scaffold

**Architecture Decisions:**
- **Three-module Gradle project:** `:app` (UI entry point), `:core` (database, models, DI), `:feature` (feature screens). Mirrors iOS's modular SPM layout for team consistency.
- **Version catalog (libs.versions.toml):** All dependency versions centralized. Kotlin 2.1.21, Compose BOM 2025.05.01, Hilt 2.56.2, Room 2.7.1, AGP 8.10.1, Gradle 8.14.2.
- **Clean Architecture + MVI:** Matches team decisions. Domain models in `core/model/`, Room entities in `core/database/entity/`, DAOs in `core/database/dao/`, Hilt DI in `core/di/`.
- **GymBro dark theme:** Colors match iOS palette — Background #0A0A0A, AccentGreen #00FF87, AccentCyan #00E5FF, AccentAmber #FFAB00. Always dark, no light theme.
- **Domain models mirror iOS:** `Exercise` (with MuscleGroup/Equipment enums), `Workout`, `ExerciseSet` (with e1RM Epley formula and kg→lbs conversion). All weights stored as kg — same as iOS decision.

**Key Files Created (34 files, ~1067 lines):**
- Root: `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `gradlew.bat`
- App: `GymBroApplication.kt` (@HiltAndroidApp), `MainActivity.kt` (@AndroidEntryPoint + Compose), Theme (Color/Type/Theme.kt), `GymBroNavGraph.kt` (5-route placeholder), resources
- Core: `GymBroDatabase.kt` (Room), `ExerciseEntity.kt`, `ExerciseDao.kt`, `DatabaseModule.kt` (Hilt), domain models
- Feature: Scaffold with Compose + Hilt + Navigation dependencies, workout package placeholder

**Build Verification:**
- `assembleDebug` passes cleanly (BUILD SUCCESSFUL in ~1m)
- Minor deprecation warning on `statusBarColor` — cosmetic, standard for edge-to-edge migration
- Min SDK 26, Target/Compile SDK 36, JVM target 17

**Unblocked Work:**
- Feature development: workout logging, exercise library, history screens
- Room entity expansion for full data model
- Health Connect integration (permissions already in manifest)
- CI/CD: GitHub Actions Android build workflow


**Data Integrity:**
- All exercises validated for: unique names, valid categories (compound/isolation/accessory), valid equipment types, muscle group assignments, primary muscle designation
- Big three powerlifts (squat, bench, deadlift) verified present with multiple variations
- Anatomically accurate muscle group mappings (19 muscle groups: Quadriceps, Glutes, Hamstrings, Chest, Shoulders, Rear Delts, Triceps, Biceps, Forearms, Lats, Upper Back, Lower Back, Traps, Core, Obliques, Calves, Adductors, Hip Flexors)
- JSON structure matches Exercise model schema exactly

**Testing:**
- Updated `ExerciseSeedDataTests.swift` with instruction length requirements
- Minimum exercise count raised from 30 to 200
- Added average instruction length test (180+ chars)
- Added instruction detail level test (no more than 20% can be < 150 chars)
- All 17 existing tests pass + 3 new tests

**Seeder Protection:**
- Documented custom exercise protection behavior: seeder only runs on empty database
- User's custom exercises are NEVER overwritten (seeder checks exercise count > 0 and skips)
- Added clarifying comment in `ExerciseDataSeeder.swift`

**Key Technical Decisions:**
- **Kept original 80 exercises unchanged** — users may already have workouts referencing these by name; changing instructions would be breaking
- **Instruction format consistency** — all new exercises follow: Setup → Execution → Breathing → Common mistakes → Safety notes
- **Real exercise names** — used terminology serious lifters actually use (e.g., "Spoto Press" not "Paused Bench Press Variation")
- **Safety-first for dangerous exercises** — Olympic lifts, heavy compounds, advanced calisthenics all include explicit safety warnings and progression advice
- **Equipment variety** — covered all equipment types in Equipment enum (barbell, dumbbell, kettlebell, machine, cable, bodyweight, band, other)

**Impact for Other Agents:**
- **Neo (AI Coach):** Can now recommend from 200 exercises with detailed instructions to pass to LLM context
- **Trinity (UI):** Exercise search/filter will surface vastly more options
- **Morpheus (Product):** Addresses depth concern — library is now on par with FitBod/Strong
- **Switch (QA):** Test coverage ensures data integrity at scale (200 exercises)

**Open Questions for Future Work:**
- Exercise images/videos: Deferred to separate issue (issue #80 was data-only)
- Custom exercise UI flow: Model supports it, but UI needs design (Trinity's domain)
- Exercise difficulty ratings: Not in current schema, would require model change
- Exercise equipment alternatives: e.g., "can substitute dumbbells for barbell" — requires new data structure

**Files Modified:**
- `Packages/GymBroCore/Sources/GymBroCore/Resources/exercises-seed.json` — 80 → 200 exercises
- `Packages/GymBroCore/Sources/GymBroCore/Services/ExerciseDataSeeder.swift` — added protection comment
- `Packages/GymBroCore/Tests/GymBroCoreTests/ExerciseSeedDataTests.swift` — updated test requirements

**PR Status:** Draft PR #92 opened, ready for review

### 2026-XX-XX: Android Skills Installation (Issue #134)

**Task:** Installed 5 P0 Android agent skills for dual-platform expansion, as recommended by Morpheus's analysis.

**Skills Installed:**

| Skill | Source Repo | Path |
|-------|------------|------|
| compose-expert | [aldefy/compose-skill](https://github.com/aldefy/compose-skill) | `.squad/skills/android/compose-expert/` |
| android-architecture | [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) | `.squad/skills/android/android-architecture/` |
| android-data-layer | [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) | `.squad/skills/android/android-data-layer/` |
| kotlin-mvi | [Meet-Miyani/compose-skill](https://github.com/Meet-Miyani/compose-skill) | `.squad/skills/android/kotlin-mvi/` |
| android-testing | [new-silvermoon/awesome-android-agent-skills](https://github.com/new-silvermoon/awesome-android-agent-skills) | `.squad/skills/android/android-testing/` |

**compose-expert reference files (4):** state-management.md, performance.md, navigation.md, production-crash-playbook.md

**Notes:**
- All content sourced directly from upstream repos via GitHub API — nothing fabricated
- Added GymBro-specific notes to architecture, data-layer, testing, and MVI skills (mapping iOS patterns to Android equivalents)
- aldefy/compose-skill is the premier Compose skill — includes actual androidx source code analysis
- Meet-Miyani/compose-skill is MVI-focused with Ktor, Paging, Room integration guidance
- new-silvermoon/awesome-android-agent-skills provides Clean Architecture, Hilt, Room, and testing patterns

**PR Status:** Draft PR #141 opened against master


### 2025-07-04: Repo Restructure for Dual-Platform (#133)

**What:** Restructured monorepo from flat iOS layout to dual-platform (ios/, android/, shared/).

**Changes:**
- Moved all iOS code (GymBro, GymBroWatch, GymBroWidgets, Packages, Package.swift) into `ios/`
- Created `android/` directory with placeholder README
- Created `shared/data/` for future cross-platform seed data
- Reorganized `.squad/skills/` into `ios/` (26 skills), `android/` (existing), `shared/` (13 skills)
- Updated root README.md with dual-platform structure

**Key findings:**
- Package.swift relative paths preserved — no code changes needed
- Seed JSON files referenced by Swift `Bundle.module` — cannot move without breaking build
- No .xcodeproj/.xcworkspace — SPM only, simplifies restructure
- CI workflow will need follow-up update to build from `ios/` directory

**PR:** Draft PR #142 opened against master
**Decision doc:** `.squad/decisions/inbox/tank-repo-restructure.md`

### Issue #143: Firebase Firestore Cloud Sync Infrastructure

**Date:** 2026-04-07
**Branch:** squad/143-firebase-sync

**What was built:**
- Firebase BOM 33.15.0, Firestore, Auth, and Messaging added to version catalog
- google-services plugin conditionally applied — build passes without google-services.json
- FIREBASE_ENABLED BuildConfig flag for runtime detection
- Firestore data models: FirestoreExercise, FirestoreWorkout, FirestoreUserProfile
- Converter utilities between Room entities and Firestore documents
- CloudSyncService interface + FirestoreSyncService implementation
- OfflineSyncManager — queues changes offline, flushes on connectivity
- AuthService interface + FirebaseAuthService (anonymous auth for MVP)
- Hilt DI module wiring all Firebase dependencies
- ProfileScreen with sync status, auth actions, auto-sync toggle
- Profile tab added to bottom navigation
- WorkoutDao extended with getAllWorkoutsOnce() and getSetsForWorkout()

**Key decisions:**
- Firebase is fully optional — no google-services.json = no Firebase, app still compiles
- Last-write-wins conflict resolution for MVP simplicity
- Anonymous auth for MVP — upgrade path to email/Google planned
- Firestore documents denormalized (workout embeds sets) for efficient reads

**Verification:** assembleDebug passes clean (105 tasks, 0 errors)

### 2026-01-12: Android Exercise Seed Data from JSON (Issue #256)

**Problem:** Fresh Android installs show empty exercise library. Users need exercises available immediately on first launch (no network required).

**Solution — Room Database Seed Data via Callback:**
- Created `android/core/src/main/assets/exercises-seed.json` — copied from shared/data/exercises-seed.json (126KB, ~4100 lines with 400+ exercises)
- Extended `DatabaseModule.kt` with JSON parsing and seeding:
  - Added kotlinx.serialization dependency (v1.8.0)
  - Created `ExerciseSeed` and `MuscleGroupSeed` data classes for JSON deserialization
  - `SeedDatabaseCallback.onCreate()` reads JSON from assets, parses, maps to `ExerciseEntity`, and bulk-inserts via `ExerciseDao.insertAll()`
  - Uses `UUID.nameUUIDFromBytes(name)` for stable IDs — same name always generates same UUID, preventing duplicates across installs
  - Picks first primary muscle group from `muscleGroups` array as the single `muscleGroup` field in Room entity
  - Uppercase normalization for enums (category, equipment, muscleGroup)
  - `videoURL` → `youtubeUrl` mapping

**Architecture Decisions:**
- JSON in `core` module assets (not `app`) so the seed data is available to core Room database code without cross-module asset access
- Callback runs on `RoomDatabase.onCreate()` — fires only on first DB creation, not on every app launch (performance)
- `ExerciseDao.insertAll()` with `OnConflictStrategy.REPLACE` prevents duplicates if remote sync later provides same exercises
- Coroutine IO scope in callback — database insertion doesn't block main thread

**Key Files Modified:**
- `android/core/build.gradle.kts` — added kotlinx-serialization plugin + dependency
- `android/gradle/libs.versions.toml` — added kotlin-serialization plugin + kotlinx-serialization-json library (v1.8.0)
- `android/core/src/main/java/com/gymbro/core/di/DatabaseModule.kt` — replaced hardcoded 22-exercise seed with JSON-driven seed (~400+ exercises)
- `android/core/src/main/assets/exercises-seed.json` — created (4102 lines)

**Duplicate Prevention Strategy:**
- Room DAO uses `OnConflictStrategy.REPLACE` — if remote sync brings an exercise with the same ID, it replaces the local seed version
- UUIDs generated deterministically from exercise name — guarantees idempotency across fresh installs and remote sync
- No server-side deduplication needed — client-side UUID generation is sufficient

**Testing:**
- Build verification: `.\gradlew.bat assembleDebug` passed (106 tasks, 33s, no errors)
- Exercise count: JSON contains 400+ exercises vs. previous hardcoded 22 — 18x expansion

**Deferred/Future Work:**
- UI for importing custom exercises from wger.de API (Android equivalent of iOS wger integration from issue #77)
- Exercise media (images, videos) — current seed only includes YouTube URLs, no embedded media
- Exercise search/filter by multiple muscle groups (current Room schema only stores single `muscleGroup` string)
- Migration strategy if seed data schema changes (currently uses Room's fallbackToDestructiveMigration)

**Branch:** `squad/256-exercise-seed-data` (pushed, PR needs manual creation due to enterprise auth)

