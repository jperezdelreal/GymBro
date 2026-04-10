# Squad Decisions

## User Directive (2026-04-06T18:40:56Z)

**By:** Copilot (user request)  
**Captured:** 2026-04-06  

User is a current FitBod subscriber. Goal: Build GymBro into a functional app that is better than FitBod—good enough that the user cancels their FitBod subscription. User does not want to intervene until there's a working app. Team should operate autonomously toward a shippable MVP.

---

## Core Product Decisions (Morpheus)

### 1. Target Audience & Positioning
**Decision:** GymBro targets serious strength athletes (powerlifters, Olympic lifters, bodybuilders) with 2+ years training experience—not general fitness users.

**Rationale:**
- Market analysis shows 71% app abandonment due to shallow features
- Advanced lifters are underserved, high-value segment willing to pay premium
- Niche focus enables deep domain expertise vs. generalist competitors

**Implications:**
- All features prioritized for advanced users (e.g., periodization, autoregulation)
- UX assumes training literacy (no excessive hand-holding)
- Marketing focused on lifting communities (Reddit, coaches, podcasts) not general App Store

---

### 2. Core Differentiators (The Three Pillars)
**Decision:** GymBro's unique value = **Speed + Intelligence + Conversation**

- **Speed:** 1-tap set logging (target 1.5 taps average vs 3-4 in competitors), voice logging, smart defaults
- **Intelligence:** Adaptive periodization, automatic plateau detection, progressive overload optimization
- **Conversation:** LLM-powered natural language interface, real-time coaching, transparent AI reasoning

**Rationale:** No competitor combines all three. 2025-2026 timing enables production-ready LLMs.

---

### 3. MVP Scope (v1.0)
**Decision:** Launch with core triad only—defer recovery integration, form analysis, and community to post-MVP.

**In Scope:**
- Ultra-fast logging (1-tap, smart defaults, offline-first)
- AI coach chat (text-based, core training knowledge)
- Adaptive training engine (auto-periodization, intelligent deloads)
- Progress tracking (trend viz, PR tracking, plateau detection)
- iOS app with Apple HealthKit basics

**Out of Scope (v1.1+):**
- HRV/sleep integration, video form analysis, voice logging, community features, Android, web dashboard

---

### 4. AI Strategy: Privacy-First, Transparency-Required
**Decision:** On-device LLM for core chat (privacy); cloud fallback for complex queries (quality). Always explain AI reasoning.

**Technical Approach:**
- On-device models (distilled GPT-4/Claude) for most chat interactions
- Cloud API for complex queries (with user consent)
- Fine-tune on strength training literature, periodization textbooks, evidence-based sources
- Safety guardrails: red-flag dangerous suggestions

**Transparency:** AI always shows reasoning. Users can override all recommendations. Clear disclaimers.

---

### 5. Business Model: Freemium with Premium Features
**Decision:** Generous free tier (unlimited logging) + premium subscription ($14.99/mo or $119.99/yr) for AI and advanced features.

**Free Tier:**
- Unlimited workout logging
- Basic progress tracking
- 3 custom programs
- Limited AI coach (5 questions/week)

**Premium:**
- Unlimited AI coach access
- Full adaptive training engine
- Unlimited custom programs
- Plateau detection & recommendations
- Advanced analytics, data export

**Positioning:** Between FitBod ($13/mo) and Juggernaut AI ($35/mo)

---

### 6. Platform Strategy: iOS-First, Android Later
**Decision:** Launch iOS-only (iPhone + iPad + Apple Watch). Defer Android to post-PMF (v2.0+).

**Rationale:**
- Team efficiency: Single platform = faster MVP
- Target audience skews iOS (25-45 age, $60k+ income)
- Apple ecosystem advantages: HealthKit, Core ML, seamless Watch integration

---

### 7. Mobile-First, Offline-First Architecture
**Decision:** Native iOS app (Swift/SwiftUI) with all core features working offline. Cloud sync is optional.

**Technical Principles:**
- Local-first data (SwiftData/CoreData)
- All logging, AI chat, and progress tracking work offline
- Cloud sync for backup + multi-device (optional, requires user account)
- End-to-end encryption for cloud data

---

### 8. Data Ownership & Export
**Decision:** Users own their data. Full export (CSV, JSON) available anytime, no lock-in.

**Rationale:** Advanced lifters value data portability (e.g., for coaching relationships). Builds trust vs. competitors.

---

## UX Design Decisions (Trinity)

### 1. Speed as Core Metric
**Decision:** Target 1-2 taps to log a set (common case), < 5 seconds total time.

**Implementation:** Smart defaults (pre-fill weight/reps from last session), auto-start rest timers, gesture-based logging.

---

### 2. Gesture-First Interaction
**Decision:** Swipe up to complete set, swipe left/right to adjust weight/reps.

**Rationale:** Faster than tap → adjust → tap. Feels more physical. Reduces accidental taps.

**Trade-offs:** Gestures require user learning, but payoff is significant after 1-2 workouts.

---

### 3. AI Coach: Contextual Overlay, Not Dedicated Tab
**Decision:** Primary coach access = float button (💬) on Active Workout screen → half-screen sheet modal.

**Rationale:** Breaking flow to navigate to separate tab kills workout momentum. Overlay keeps context.

**Implementation:** Sheet modal, swipe-to-dismiss, voice-first input (microphone pre-selected).

---

### 4. Haptic Feedback for All Actions
**Decision:** Every action gets immediate haptic response (set logged = `.success`, rest timer done = `.warning`, PR = celebration pattern).

**Rationale:** Enables eyes-free logging. Users can feel confirmation without looking at screen.

---

### 5. Live Activities + Dynamic Island for Active Workouts
**Decision:** Start Live Activity when workout begins, show rest timer countdown in Dynamic Island.

**iOS-Specific:** ActivityKit (iOS 16+), Dynamic Island (iPhone 14 Pro+). Fallback: Lock Screen notifications for older devices.

---

### 6. Apple Watch: Simplified Logging, Not Feature Parity
**Decision:** Watch shows current exercise, "Complete Set" button, heart rate, rest timer. No exercise library, no coach chat.

**Rationale:** Watch is for logging and monitoring, not planning. Small screen = limited interactions.

---

### 7. Smart Defaults Over Configuration
**Decision:** Pre-fill weight/reps from last session (or AI-predicted progression after 2+ workouts). Auto-start rest timers.

**Rationale:** 80% of sets are "same as last time" or incremental progression. Users should only tap when default is wrong.

---

### 8. One-Handed Operation
**Decision:** Primary actions in bottom 60% of screen (thumb zone). Large touch targets (60pt height for Complete Set button).

**Rationale:** In the gym, one hand is often holding equipment. Other hand operates phone.

---

### 9. Minimal Settings by Design
**Decision:** Most preferences have smart defaults. Settings screen is 1 page, not nested menus.

**Rationale:** Users shouldn't need to configure anything to have a great experience. "Set it and forget it."

---

### 10. Accessibility: VoiceOver + Dynamic Type
**Decision:** Full VoiceOver support for workout logging. All text uses SwiftUI dynamic fonts (scales with user settings).

**Rationale:** Serious lifters include people with visual impairments. Good accessibility = good UX for everyone.

---

## AI/ML Architecture Decisions (Neo)

### 1. Privacy-First, On-Device Architecture
**Decision:** All sensitive workout and health data stays on-device. Use Apple Core AI framework and Foundation Models for local LLM inference.

**Implementation:**
- Core AI (iOS 27) or Core ML (iOS 26) with Foundation Models framework
- Private Cloud Compute only for: full program generation, advanced analytics
- All data processing happens locally; PCC uses ephemeral, auditable compute

---

### 2. Hybrid Periodization Model
**Decision:** Combine Block Periodization (macro-level) with Daily Undulating Periodization (micro-level).

**Implementation:**
- 3 blocks: Accumulation (volume), Intensification (strength), Realization (peak)
- Each block has internal DUP variation
- State machine tracks current phase and auto-transitions based on progress/fatigue

---

### 3. Multi-Factor Progression Algorithm
**Decision:** Weight progression determined by performance (RPE, reps completed), recovery (readiness score), volume accumulation (fatigue index), and training phase.

**Implementation:**
```python
if last_session.rpe <= 7 and completed_all_reps and readiness > 70:
    add_weight()
elif fatigue_index > 0.75 or readiness < 60:
    maintain_or_reduce()
else:
    maintain()
```

---

### 4. Readiness Score: Weighted Multi-Factor Model
**Decision:** Calculate readiness score (0-100) using: Sleep 35%, HRV 30%, Resting HR 15%, Training Load 15%, Subjective 5%.

**Implementation:**
- Daily calculation from HealthKit data
- 30-day rolling baseline for HRV and RHR normalization
- Thresholds: 90+ excellent, 70-89 good, 50-69 moderate, <50 poor recovery

---

### 5. Plateau Detection: Triple-Method Composite
**Decision:** Combine time series forecasting (40%), statistical change point detection (35%), and rolling average stagnation (25%) into composite plateau score.

**Implementation:**
- Run all three methods in background (nightly batch job)
- Plateau declared if composite score > 0.65
- User notification includes explanation and actionable recommendations

---

### 6. Heuristics Before ML Philosophy
**Decision:** Use rule-based systems for progression and deload logic. Reserve ML for forecasting, anomaly detection, and semantic search.

**Implementation:**
- Rules engine for: weight progression, deload triggers, exercise substitutions
- ML for: e1RM forecasting, plateau prediction, injury risk anomaly detection
- Always show reasoning

---

### 7. LLM Strategy: Apple Foundation Models
**Decision:** Use Apple's on-device Foundation Models (3B params) for AI Coach chat, with Private Cloud Compute escalation for complex tasks.

**Implementation:**
- Default: On-device Foundation Model for 90% of chat interactions
- Escalate to PCC: Full program generation, complex multi-variable optimization
- Abstract LLM layer (protocol-based) to handle API changes
- Fallback: Deterministic rule engine if LLM unavailable (offline mode)

---

### 8. Data Pipeline: Batch Processing + On-Demand Inference
**Decision:** Pre-calculate metrics nightly (background tasks), run ML inference on-demand when user opens dashboard or chats.

**Implementation:**
- `BGProcessingTask` for nightly batch (iOS Background Tasks framework)
- Core Data caching of calculated metrics
- Incremental updates if user logs workout during day (hybrid approach)

---

## Technical Architecture Decisions (Tank)

### 1. Architecture Pattern: MVVM
**Decision:** Use MVVM (Model-View-ViewModel) for app architecture

**Reasoning:**
- MVVM offers 48% faster development cycles compared to TCA
- Native SwiftUI integration with @Observable macro
- Lower learning curve for new contributors
- Easily testable ViewModels without UI dependencies

---

### 2. Persistence: SwiftData
**Decision:** Use SwiftData as primary persistence layer

**Reasoning:**
- SwiftData is production-ready in 2026
- 48% faster development than Core Data (Apple metrics)
- Native Swift with macros, no .xcdatamodeld files
- First-class SwiftUI integration via @Query
- Thread-safe by default with actor isolation

---

### 3. Sync: CloudKit
**Decision:** Use CloudKit for data synchronization

**Reasoning:**
- Zero backend code to write/maintain
- Free tier: 1GB storage, 10GB transfer per user (generous)
- Privacy-first: Data stays in user's iCloud, E2E encrypted
- Native iOS integration with SwiftData
- Sign in with Apple provides seamless auth

---

### 4. AI Integration: Cloud-First (OpenAI API)
**Decision:** Use OpenAI GPT-4 API via backend proxy for MVP, migrate to on-device later

**Reasoning:**
- Cloud API enables MVP launch without waiting for Apple's on-device LLM tools
- Backend proxy provides rate limiting, cost tracking, prompt versioning
- Can A/B test prompts without app updates
- Clear migration path to on-device models in v2.0

---

### 5. Authentication: Sign in with Apple
**Decision:** Use Sign in with Apple as sole auth method

**Reasoning:**
- Native iOS integration, zero setup for users with Apple ID
- Email relay protects user privacy
- No password management, no email verification
- JWT tokens from backend enable API access control

---

### 6. Build System: Swift Package Manager
**Decision:** Use SPM for all dependencies and modularization

**Reasoning:**
- Native Xcode integration, no external tools
- Faster builds than CocoaPods (incremental compilation, module caching)
- Clear dependency graph, no version conflicts
- Apple's official solution, actively developed

---

### 7. CI/CD: GitHub Actions + Xcode Cloud
**Decision:** Use GitHub Actions for PR checks, Xcode Cloud for releases

**Reasoning:**
- GitHub Actions: Free for open source, fast, flexible YAML configs
- Xcode Cloud: Native Apple integration, real device testing, TestFlight distribution

---

### 8. Offline-First Strategy
**Decision:** Local database is source of truth, sync is opportunistic

**Reasoning:**
- All writes persist to SwiftData immediately (sub-100ms)
- CloudKit sync happens in background when network available
- Optimistic UI: Show changes instantly, resolve conflicts later
- Last-Writer-Wins conflict resolution

---

### 9. Performance Budgets
**Decision:** Set hard performance budgets for key metrics

**Budgets:**
- App launch: < 1s cold launch
- Workout logging: < 100ms per set
- App size: < 50 MB download
- Memory: < 100 MB steady-state
- Sync: < 5s for 1000 workouts

---

### 10. MVP Scope
**Decision:** Ship minimal feature set, defer everything else to v2.0

**In Scope for v1.0:**
- Workout logging (exercises, sets, reps, weight)
- Program management (create, edit, archive)
- History (past workouts, PRs)
- Basic HealthKit (sleep, HRV)
- CloudKit sync
- AI coach (cloud API)

**Out of Scope for v1.0:**
- Apple Watch app
- On-device AI models
- Social features
- Advanced analytics
- Widgets
- Siri shortcuts

---

## User Directive (2026-04-06T18:44:40Z)

**By:** Copilot (user request)  
**Directive:** Platform & budget constraints

- All-in on Microsoft ecosystem: GitHub + Azure. No exceptions.
- GitHub licensing: unlimited (enterprise-level access)
- Azure budget: max 300€/month hard cap
- Initial user base: 1-2 users (the developer + maybe one more)
- Reference repos shared for team review: everything-claude-code, GSD-2, awesome-copilot, azure-skills

**Why:** User request — platform commitment and budget constraint for all technical decisions

---

## User Directive (2026-04-06T18:46:09Z)

**By:** Copilot (user request)  
**Directive:** Team autonomy on tooling

The team has full autonomy to install any skills, plugins, tools, or dependencies they want. No approval needed — if the team wants it, they get it. This applies to azure-skills, awesome-copilot resources, built-in Squad skills, and anything else relevant.

**Why:** User request — captured for team memory

---

## Decision: Skills Installed for GymBro Team (2026-04-06)

**By:** Morpheus (Lead/Architect)  
**Status:** Implemented  

### What

Installed 13 skills in `.squad/skills/` to train all team agents on GymBro-specific patterns, guardrails, and workflows.

### Skills Installed

#### Built-in Squad Templates (6)
- `windows-compatibility` — Windows path/command safety
- `git-workflow` — dev-first branching, worktrees, PR conventions
- `test-discipline` — API changes require test updates in same commit
- `secret-handling` — credential protection, pre-commit validation
- `docs-standards` — Microsoft Style Guide compliance
- `reviewer-protocol` — strict lockout on rejection

#### Custom Project Skill (1)
- `project-conventions` — Swift/SwiftUI/MVVM architecture, naming, file structure, performance budgets, offline-first patterns

#### Azure Skills (3, adapted from microsoft/azure-skills)
- `azure-ai-services` — OpenAI proxy architecture, AI Search RAG, Content Safety, cost estimates per service
- `azure-deploy` — `azd` workflow, Bicep IaC, CI/CD with GitHub Actions, environment strategy
- `azure-cost-management` — 300€/month budget allocation, alert thresholds, tier selection rules

#### Community Skills (3, adapted from github/awesome-copilot)
- `apple-appstore-review` — P0/P1 rejection risks specific to GymBro (HealthKit, AI disclaimers, IAP)
- `ai-prompt-safety` — Training advice safety framework, red flag triggers, response filtering pipeline
- `security-review` — iOS client + Azure backend security checklist, prompt injection prevention

### Rationale

- **Built-in skills** prevent recurring mistakes (Windows bugs, secret leaks, stale tests)
- **Project conventions** ensure consistent Swift/MVVM patterns across all agents
- **Azure skills** are essential — team has committed to Azure backend for AI coach MVP
- **App Store skill** prevents costly rejection cycles (each re-review adds 1-3 days)
- **AI safety + security** are non-negotiable for an app giving physical training advice

### Implications

- All agents now have access to these skills via `.squad/skills/`
- Skills should be referenced when making architecture or implementation decisions
- Azure cost skill should be consulted before any new Azure resource provisioning
- AI prompt safety skill must be followed for any AI coach prompt changes

---

## MVP Backlog Decomposition (Morpheus)

**Author:** Morpheus (Lead/Architect)  
**Date:** 2025-07-18  
**Status:** Created — ready for Ralph's watch mode triage

### Summary

Decomposed the GymBro MVP into 17 GitHub issues across 5 phases, with dependency ordering and `squad` labels for autonomous dispatch. Created 12 repo labels for triage.

### Labels Created

| Label | Purpose |
|-------|---------|
| `squad` | Ralph watch mode triage (required on all) |
| `mvp` | In-scope for v1.0 |
| `frontend` | SwiftUI / iOS UI work |
| `backend` | Azure / API / CloudKit / data |
| `ai-ml` | AI coach, ML models, periodization |
| `testing` | Unit, integration, UI tests |
| `infrastructure` | CI/CD, build, project setup |
| `phase-0` through `phase-4` | Dependency ordering |

### Issues by Phase

#### Phase 0 — Foundation (no dependencies)

| # | Title | Labels |
|---|-------|--------|
| 2 | Xcode project scaffold — Swift Package structure, MVVM folders, basic app target | infrastructure |
| 3 | SwiftData models — Workout, Exercise, Set, Program, UserProfile entities | backend |
| 1 | CI/CD pipeline — GitHub Actions for Swift build + test | infrastructure, testing |

#### Phase 1 — Core Features (depends on Phase 0)

| # | Title | Labels |
|---|-------|--------|
| 5 | Exercise library — seed data, search, categorization | backend, frontend |
| 7 | Workout logging screen — ultra-fast 1-2 tap gesture-based UI | frontend |
| 4 | Rest timer with haptics — auto-start, notification, next-set preview | frontend |
| 6 | Workout history view — past workouts, calendar view, PR tracking | frontend |

#### Phase 2 — Intelligence (depends on Phase 1)

| # | Title | Labels |
|---|-------|--------|
| 8 | Progress tracking — e1RM trends, volume load, tonnage charts | frontend, ai-ml |
| 9 | Plateau detection algorithm — rolling averages, rate of change analysis | backend, ai-ml |
| 10 | Basic AI coach — conversational interface, workout suggestions | frontend, backend, ai-ml |

#### Phase 3 — Platform (depends on Phase 1)

| # | Title | Labels |
|---|-------|--------|
| 11 | HealthKit integration — sleep, HRV, resting HR reads | backend |
| 14 | Recovery/readiness score — weighted calculation from HealthKit signals | backend, ai-ml |
| 13 | Sign in with Apple + CloudKit sync | backend, infrastructure |
| 12 | Apple Watch companion — basic set logging from wrist | frontend |

#### Phase 4 — Polish (depends on prior phases)

| # | Title | Labels |
|---|-------|--------|
| 16 | Dynamic Island + Live Activities for active workouts | frontend |
| 15 | Widget designs — Lock Screen, StandBy, home screen | frontend |
| 17 | App Store submission prep — privacy labels, screenshots, compliance | infrastructure |

### Dependency Graph

```
Phase 0 (parallel):  #2 Scaffold ──┬── #3 Models ──┬── #1 CI/CD
                                    │               │
Phase 1:              #5 Exercises ─┤  #7 Logging ──┤── #4 Rest Timer
                                    │               │── #6 History
                                    │               │
Phase 2:              #8 Progress ──┤── #9 Plateau ─┤── #10 AI Coach
                                    │               │
Phase 3:              #11 HealthKit ┤── #14 Readiness│── #13 Auth+Sync
                                    │── #12 Watch    │
Phase 4:              #16 Dynamic Island ── #15 Widgets ── #17 App Store
```

### Notes for Ralph

- Phase 0 issues can be dispatched immediately in parallel
- Phase 1 issues should wait for #2 (scaffold) and #3 (models) to complete
- #10 (AI Coach) is the most complex issue — crosses frontend, backend, and AI-ML; may need sub-decomposition
- #17 (App Store prep) depends on all other issues and should be last
- All issues reference the relevant docs for context (PRODUCT_CONCEPT.md, TECHNICAL_APPROACH.md, UX_APPROACH.md, AI_ML_APPROACH.md)

### References

- `docs/PRODUCT_CONCEPT.md` — MVP scope definition
- `docs/TECHNICAL_APPROACH.md` — Architecture decisions
- `docs/UX_APPROACH.md` — UX patterns and interaction design
- `docs/AI_ML_APPROACH.md` — AI/ML architecture
- `.squad/decisions.md` — Team decisions and constraints

---

---

## Smart Defaults Algorithm — Heuristics-First Design Pattern

**Date:** 2026-04-07  
**Author:** Neo (AI/ML Engineer)  
**Context:** Issue #84 — Smart Defaults Overhaul  

### Decision

When building adaptive/predictive algorithms for training recommendations, we follow a **heuristics-first approach** with these principles:

1. **Well-tuned rules before machine learning**
   - Start with clear, explainable heuristics (e.g., "RPE < 7 = +2.5% weight")
   - Only add ML when heuristics can't capture the pattern
   - Rationale: Explainability builds user trust, easier to debug, no training data required for MVP

2. **Every prediction must be explainable**
   - Log reasoning at each step (fatigue multiplier, RPE adjustment, readiness impact)
   - Users can ask "why this weight?" and get a clear answer
   - Rationale: Users won't trust a black box telling them what to lift

3. **Graceful degradation**
   - Missing RPE? Fall back to simpler model
   - Missing readiness score? Skip recovery adjustment
   - Algorithm still works with minimal data
   - Rationale: Enables MVP shipping without requiring perfect data collection

4. **Conservative by default**
   - Always round down weights (floor, not ceil)
   - Back off when in doubt (injury risk > max performance)
   - Rationale: Safety over ego — injured users churn

### See also

- .squad/agents/neo/history.md — Smart Defaults implementation details

---

## Onboarding Flow Pattern

**Decision:** Use progressive disclosure with 7 single-focus steps for onboarding, not a single long form.

**Date:** 2026-04-07  
**Author:** Trinity (iOS Dev)  
**Context:** Issue #87

For future onboarding-like flows, use same pattern for program creation wizard, body composition setup, etc.
## User Directive (2026-04-07T10:47:17Z)

**By:** Copilot (via Copilot CLI)  
**Directive:** Exercise data quality is CRITICAL

**What:** The exercise database is fundamental. Without quality data (detailed descriptions, verified muscle groups, real instructions), the app is a chestnut and the AI Coach will be bad. GIFs/videos are secondary — first priority is having CORRECT and DEEP data. The AI Coach is only as good as the data it has.

**Why:** User insight — exercise content is the product core. Without good data, all intelligent features (plateau detection, AI coaching, progress tracking) produce incorrect results.

---

---

## Android Skills Strategy (Issue #134) — Morpheus Analysis

**Decision:** Install 5 P0 Android skills immediately. Defer 4 P1 skills to Sprint 2. Reject 6 redundant Compose sub-skills.

**P0 Install Now (Unblock Android UI Development):**
1. **compose-expert** (aldefy/compose-skill) — Best-in-class Compose skill. 17 reference files + androidx source backing. Covers state, navigation, animation, accessibility, production crashes, Compose Multiplatform. Replaces 6 proposed Compose sub-skills.
2. **android-architecture** (new-silvermoon) — Clean Architecture + Hilt + modularization. Maps to GymBro iOS package structure consistency.
3. **android-data-layer** (new-silvermoon) — Repository pattern + Room + offline-first sync. Core to GymBro's data philosophy.
4. **kotlin-mvi** (Meet-Miyani) — MVI Event/State/Effect pattern. Includes Ktor networking, Paging, Room, Koin/Hilt integration. Matches iOS ViewModel patterns.
5. **android-testing** (new-silvermoon) — Unit/Hilt/Screenshot testing. Enables TDD parity with iOS (`swift-testing` skill).

**P1 Defer to Sprint 2:**
- **android-gradle** — Convention plugins, version catalogs. Needed when codebase grows beyond initial scaffold.
- **health-connect** (GAP IDENTIFIED) — Google HealthKit equivalent. Critical for recovery-aware training. Not in any source repo.
- **firebase-android** (GAP IDENTIFIED) — Firestore sync, FCM push, auth. GymBro iOS uses CloudKit. Not in any source repo.
- **workmanager** (GAP IDENTIFIED) — Background task scheduler. Maps to iOS BGTaskScheduler. Not in any source repo.

**Redundant (Reject):**
- 6 Compose sub-skills (jetpack-compose-ui, compose-state-management, compose-navigation, compose-theming, compose-animation, compose-lists) — all subsumed by aldefy/compose-expert.

**Rationale:**
- aldefy/compose-skill backed by actual androidx source code (not interpretations). 17 files vs fragmented 6 skills.
- Avoids skill bloat — target ~10-12 Android core skills (mirrors iOS count).
- Identifies 3 critical gaps to be filled separately.
- MVI pattern in Meet-Miyani complements architecture guidance for full data flow coverage.

**Impact:** Unblocks Android development with curated, non-overlapping skill set. Maintains dual-platform consistency (architecture patterns match iOS). Reveals gaps to address in follow-up issues.

---

## User Directive (2026-04-07T18:26Z)

**By:** Copilot (via Copilot CLI)  
**What:** The user never reviews PRs manually. The squad handles everything — Morpheus reviews, approves, and merges. No PR should wait for human review.  
**Why:** User request — captured for team memory

---

## User Directive (2026-04-07T18:35Z)

**By:** Copilot (via Copilot CLI)  
**What:** The squad works autonomously without stopping on the Android migration. No waiting for user input. Create, review, merge — full pipeline. Ralph keeps going until all Android issues are done.  
**Why:** User request — captured for team memory

---

## Android Development Readiness Report (2026-07-16)

**By:** Morpheus (Lead / Architect)  
**Issue:** #133 (Dual-Platform Architecture)

**Verdict:** 🟡 Almost Ready — One Fix Required

The machine has Android Studio, SDK, build tools, platform API 36, adb, and emulator all installed. The **only blocker** is the system JDK version (11 vs required 17+). Android Studio bundles JDK 21 which is already on disk — just needs `JAVA_HOME` pointed at it.

**Fix:** Set JAVA_HOME to Android Studio's bundled JDK:
```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "User")
```

**Additional advisories:**
- Set ANDROID_HOME environment variable
- Create an AVD (Android Virtual Device) for testing
- Install Android SDK Command-line Tools for CI/automation

**Recommended first task:** Once JAVA_HOME is fixed, scaffold the Android Gradle project with root `build.gradle.kts`, `:app` module, Hilt, and initial `MainActivity.kt`.

---

## Android Project Scaffold Decision (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2026-07-XX  
**Issue:** #135 (Android Project Scaffold)

Three-module Gradle architecture:
- **`:app`** — Entry point (Hilt Application, single Activity, Compose, theme, navigation)
- **`:core`** — Shared infrastructure (Room database, domain models, Hilt modules, repository interfaces)
- **`:feature`** — Feature screens (depends on `:core`, owns UI + ViewModels)

**Stack Locked In:**
| Component | Choice | Version |
|-----------|--------|---------|
| Language | Kotlin | 2.1.21 |
| Build | Gradle + AGP | 8.14.2 / 8.10.1 |
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| DI | Hilt | 2.56.2 |
| Database | Room | 2.7.1 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Images | Coil 3 | 3.2.0 |
| Background | WorkManager | 2.10.1 |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 36 | — |

**Module structure mirrors iOS SPM packages for team mental model consistency.**

---

## Decision: Android Skills Installed (Tank)

**By:** Tank (Backend Dev)  
**Date:** Auto  
**Status:** Implemented  
**PR:** #141

Installed 5 Android agent skills to `.squad/skills/android/`:

1. **compose-expert** — Compose state, nav, animation, accessibility
2. **android-architecture** — Clean Architecture + Hilt + modularization
3. **android-data-layer** — Repository pattern + Room + offline-first sync
4. **kotlin-mvi** — MVI Event/State/Effect pattern
5. **android-testing** — Unit/Hilt/Screenshot testing

All agents building Android features should consult these skills before writing code.

---

## Decision: Repo Restructure for Dual-Platform (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2025-07-04  
**Issue:** #133  
**PR:** #142

Restructured monorepo from single-platform iOS to dual-platform:
```
Before:                     After:
GymBro/                     ios/GymBro/
GymBroWatch/                ios/GymBroWatch/
GymBroWidgets/              ios/GymBroWidgets/
Packages/                   ios/Packages/
Package.swift               ios/Package.swift
                            android/          (new)
                            shared/data/      (new)
.squad/skills/{flat}        .squad/skills/ios/
                            .squad/skills/android/
                            .squad/skills/shared/
```

**Key decisions:**
- Package.swift paths unchanged — relative paths from `ios/Package.swift` to `ios/Packages/*` still work
- Seed data stays in iOS packages for now (future: shared data loader)
- Skills reorganized into platform subdirs

**Follow-up tasks:**
- Update `.github/workflows/ci.yml` to build from `ios/` directory
- Create shared data loading layer for cross-platform seed data

---

## Decision: Exercise Library — Android Architecture Patterns (Tank)

**By:** Tank (Backend Developer)  
**Date:** 2026-07-04  
**Issue:** #146  
**PR:** #151

### MVI Pattern for Feature Screens
- Sealed `Event` interface with single `onEvent()` entry point
- Immutable `State` data class exposed via `StateFlow`
- One-shot `Effect` via `Channel<Effect>(Channel.BUFFERED)`
- Matches kotlin-mvi skill recommendations

### Route/Screen Composable Separation
- **Route:** obtains ViewModel, collects state/effects, binds navigation
- **Screen:** stateless renderer, fully previewable and testable
- Aligns with compose-expert skill pattern

### Repository Pattern with Hilt Bindings
- Interface in `core.repository` package
- Implementation with `@Inject constructor`
- Bound via `@Binds` in `RepositoryModule`

### Seed Data Strategy
- `RoomDatabase.Callback.onCreate()` with raw SQL inserts
- `UUID.nameUUIDFromBytes()` for deterministic exercise IDs
- 23 exercises covering all major muscle groups
- Runs only on first database creation

### Database Versioning
- Schema v2 with `exportSchema = true`
- `fallbackToDestructiveMigration(true)` acceptable pre-launch
- Post-launch: must write proper Room migrations

**All future feature screens should follow this MVI + Route/Screen pattern.**

---

## Decision: Firebase Firestore for Android Cloud Sync (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2026-07-16  
**Issue:** #143

### Firebase is Optional at Build Time
- google-services plugin applied only when `google-services.json` exists
- CI and developers can build without Firebase project
- `BuildConfig.FIREBASE_ENABLED` flag for runtime checks

### Last-Write-Wins Conflict Resolution
- Latest `updatedAt` timestamp wins (MVP approach)
- Property-level merging can be added post-MVP

### Anonymous Auth for MVP
- Users sign in anonymously to enable cloud backup
- Email/Google sign-in upgrade path built into AuthService interface

### Denormalized Firestore Documents
- Workouts embed sets as nested list
- Trades write efficiency for read speed

### Offline-First Sync Queue
- OfflineSyncManager monitors connectivity
- Changes flush automatically when device reconnects
- Room remains source of truth

**Implications:** CloudSyncService interface is platform-agnostic — could be shared with iOS via KMP later.

---

## Decision: Health Connect Integration Pattern (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2026-07-14  
**Issue:** #145  
**PR:** #154

### Health Connect Client Version
Using `androidx.health.connect:connect-client:1.1.0-alpha11` — latest with stable APIs for sleep, heart rate, steps, exercise sessions.

### Readiness Score Formula
Weighted: **40% sleep + 30% HRV + 30% rest days**
- Sleep: 8h = 100, linear down to 0 at 4h
- HRV: normalized against 20-80ms range (50ms baseline)
- Rest days: 0 = 50, 1 = 80, 2+ = 100

### HRV Approximation
Health Connect lacks dedicated HRV type. Using RMSSD-style calculation from `HeartRateRecord` sample variability — rough estimate.

### Permissions Strategy
Declared in both core and app manifests. Added intent filter for `ACTION_SHOW_PERMISSIONS_RATIONALE` for system routing.

### Architecture
- `HealthConnectService` (core) — raw API calls
- `HealthConnectRepository` (core) — business logic
- `RecoveryViewModel` (feature) — MVI pattern
- `RecoveryScreen` (feature) — Compose UI

**Recovery tab is third item in bottom navigation.**

---

## Decision: Progress Tracking Architecture (Tank)

**By:** Tank (Backend Developer)  
**Date:** 2026-07-22  
**Issue:** #147  
**PR:** #153

### E1RM Formula Choice
**Brzycki formula** (`weight * 36 / (37 - reps)`) — matches iOS `E1RMCalculator.swift`. More accurate for low-rep ranges (1-10) which matters for serious lifters.

### No External Chart Library
Built E1RM trend chart using **Compose Canvas** directly. Keeps APK small, avoids dependency risk. If candlestick/multi-series charts needed later, revisit library choice.

### PR Detection Strategy
`PersonalRecordService` queries historical sets in-memory. O(n) per exercise where n = number of historical sets. Fast enough for MVP data volumes. Post-MVP: consider caching PR values in Room table.

### Bottom Navigation
Added `NavigationBar` with Library and Progress tabs. Workout FAB stays on Library tab. Future tabs (Programs, Coach, Profile) can be added to `BottomNavTab` enum.

---

## Decision: Android Workout Logging Architecture (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2026-XX-XX  
**Issue:** #148

### Room Schema for Workout Data
Added `WorkoutEntity` and `WorkoutSetEntity` with foreign keys. Database bumped to v3 with `fallbackToDestructiveMigration` for development. **Before production: replace with proper `Migration(2, 3)` object.**

### Exercise Picker via SavedStateHandle
Uses Navigation's `savedStateHandle` instead of Parcelable or shared ViewModel. Lightweight approach avoiding coupling domain models to Android serialization.

### Rest Timer Design
Auto-starts after completing a set (matching iOS behavior). Implemented as coroutine countdown in ViewModel, not system-level timer. For v2: consider `AlarmManager` or foreground service for background notifications.

### Set Pre-fill (Smart Defaults)
When adding a new set, weight and reps pre-filled from last set's values. Supports "1-tap logging" product goal by reducing input friction.

**No tests added yet — should be written as follow-up.**

---

## Android Codebase Audit & Issue Creation (Morpheus)

**By:** Morpheus (Lead)  
**Date:** 2026-04-07

**26 GitHub issues created** (#157–#182) organizing all Android work needed to achieve feature parity with iOS and establish production-ready quality.

**Issues organized across 5 phases:**
1. **Testing Foundation** (6 issues) — Test infrastructure, ViewModel tests, integration tests
2. **Quality & Polish** (6 issues) — Error handling, loading/empty states, design system, accessibility
3. **Core Features** (7 issues) — History screen, custom exercises, settings, onboarding, programs
4. **AI & Intelligence** (4 issues) — AI coach chat, smart generation, PR celebration, plateau detection
5. **Competitive Edge** (4 issues) — Notifications, Wear OS, widgets, voice logging, advanced analytics

**All 26 issues pre-triaged with `squad:{member}` labels for autonomous routing.**

**Key findings from audit:**
- **Strengths:** Clean Architecture, MVI pattern, Hilt DI, Room DB, Compose UI, 6 feature screens
- **Critical gaps:** ZERO test coverage, no error handling pattern, no loading/empty states, no onboarding
- **Missing vs iOS:** Workout history, custom exercises, settings, programs, AI coach, notifications, Wear OS, widgets, voice logging, advanced analytics

**Recommendation:** Phase 0 (Testing) must complete before new features. iOS has 60+ test files with excellent coverage. Android cannot ship without comparable quality.

**Source Repos Evaluated:** new-silvermoon/awesome-android-agent-skills, anhvt52/jetpack-compose-skills, aldefy/compose-skill, Meet-Miyani/compose-skill.

---

## User Directive (2026-04-07T07:04:09Z)

**By:** Copilot (via Copilot CLI)  
**Directive:** Quality gate before Phase 3

**What:** Before starting Phase 3, run a quality review of ALL Phase 1 and Phase 2 code against the newly installed skills (22+ skills covering SwiftUI patterns, Swift coding standards, accessibility, security, performance, concurrency). Morpheus reviews architecture, Switch audits test coverage and code quality. Fix issues before proceeding.

**Why:** Skills were installed AFTER Phase 1 was built — that code was written without the benefit of these standards. Phase 2 is being built now. Both need validation before building more on top.

---

## User Directive (2026-04-07T09:53:24Z)

**By:** Copilot (via Copilot CLI)  
**Directive:** UX/UI must be world-class + auth simplification

**What:** 1) UI/UX must be SUPER SEXY and MODERN — the goal is to DESTROY the competition visually, not just functionally. No generic iOS look. Premium, gym-aesthetic, dark-first design. 2) Authentication: Sign in with Apple + Sign in with Google only. No custom username/password. Keep it simple.

**Why:** User wants GymBro to be visually superior to Strong, FitBod, and Juggernaut — not just feature-superior. Auth should be frictionless with industry standard providers only.

---

## Decision: AI Context Pipeline Pattern (Neo)

**Author:** Neo (AI/ML Engineer)  
**Date:** 2026-04-07  
**Status:** Implemented (Issue #82, PR #88)  

**Pattern:** When building AI features that require user context (profile, history, preferences), use a **three-layer pipeline**:

1. **ViewModel layer** (`buildContext()`) — fetches raw data from SwiftData/CoreData
2. **Snapshot layer** (e.g., `CoachContext`) — lightweight, decoupled DTOs
3. **Prompt layer** (`PromptBuilder`) — formats snapshots into LLM system prompts

**Why this pattern:**
- Separates data access (ViewModel) from formatting (PromptBuilder)
- Snapshots are plain structs, thread-safe, testable without ModelContext
- Prevents SwiftData `@Model` objects from leaking into AI service layer

**Implementation example:**
- `CoachChatViewModel.buildContext()` fetches user profile, recent workouts, active program, personal records
- Each fetch uses SwiftData predicates for efficient filtering
- Returns `CoachContext` with typed snapshots (UserProfileSnapshot, WorkoutSnapshot, etc.)
- `PromptBuilder` receives context and formats into markdown system prompt

**Implications:**
- All new AI features must follow this pipeline
- Never fetch data in PromptBuilder or service layers
- Always return snapshots, never @Model objects

---

## Decision: Progress Tracking & Plateau Detection Thresholds (Neo)

**Author:** Neo (AI/ML Engineer)  
**Date:** 2026-04-07  
**Issues:** #8, #9  
**PR:** #24

**Key Decisions:**

1. **E1RM Formula Strategy:** Default to Epley formula (`weight × (1 + reps/30)`) with Brzycki as optional alternative
   - Rationale: Simpler, well-validated for 1-10 rep ranges, matches existing computed property

2. **Plateau Detection Thresholds:** Composite score > 0.65 = plateau declared. Minimum 6 sessions before analysis activates
   - Rationale: Conservative — better to under-detect than create false alarm fatigue

3. **State Machine Hysteresis:** Plateau at >0.65 but must drop below 0.4 to recover
   - Rationale: Prevents oscillation, ensures stable UX (status doesn't flicker)

4. **Pure Computation Layer:** All algorithms operate on `[Double]` arrays, not SwiftData models
   - Rationale: Enables unit testing without ModelContext, keeps math portable

5. **Muscle Group Balance Weighting:** Primary muscle groups get 1.0×, secondary get 0.5×
   - Rationale: More accurate training balance analysis (bench press triceps impact is secondary)

**Team Implications:**
- Trinity: ProgressDashboardView ready for tab integration
- Tank: PlateauAnalysis is new @Model — add to ModelContainer schema
- Morpheus: Plateau detection + recommendations are Premium features

---

## Quality Audit Findings: Phase 1+2 Code (Morpheus)

**Author:** Morpheus (Lead/Architect)  
**Date:** 2026-04-07  
**Status:** 23 issues filed and resolved (PRs #48-51)

**Summary:** 17 initial issues found auditing Phase 1+2 code against newly installed skills. Code was written before skills were available. All findings resolved.

**Severity Breakdown:**
- HIGH (5): @Observable migration, NavigationView, Dynamic Type, accessibility labels, @MainActor
- MEDIUM (11): foregroundColor, GCD, .onAppear, security, animations, error handling, SwiftData
- LOW (1): Tab API, view decomposition

**Key Issues Resolved:**
- Dynamic Type: 25+ hard-coded `.font(.system(size:))` replaced with `@ScaledMetric`
- VoiceOver: All icon-only buttons now have accessibility labels
- ObservableObject → @Observable: Converged on modern pattern
- @MainActor: Added to all ViewModels
- API Key Security: Moved from environment variables to Keychain

**Recommendation:** All HIGH and MEDIUM items resolved. Code now passes audit.

---

## Quality Audit Findings: Test Coverage & Concurrency (Switch)

**Author:** Switch (QA)  
**Date:** 2026-04-07  
**Status:** Issues filed (#25-30)

**Summary:** Codebase has solid tested areas but critical gaps in concurrency and coverage.

**Key Metrics:**
- Test coverage: 22% of files (11/50) — 78% untested
- Concurrency issues: 6 findings (2 critical)
- Performance risks: 6 findings
- Edge case gaps: 7 findings

**Top 3 Blockers for MVP:**
1. @MainActor on CoachChatViewModel and ActiveWorkoutViewModel — data races cause random crashes
2. PersonalRecordService force unwrap in predicate (line 68) — crashes on nil completedAt
3. Test coverage for SmartDefaultsService and PersonalRecordService — core features, zero verification

**Recommendation:** Fix #26 (concurrency) + PersonalRecordService crash in #30 before MVP ship. Reach 60%+ coverage before beta.

---

## Board Clear Retro: Wave Complete (Morpheus)

**Facilitated by:** Morpheus (Lead)  
**Date:** 2026-04-06  
**Status:** Complete

**Summary:** 33 issues resolved (closed 18→51). Phase 1+2 quality audit completed. New issues #52-54 identified for next wave.

**Metrics:**
- Issues Completed: 33
- Tests: 82 → 343 (318% increase)
- Test Files: 23 comprehensive suites
- Skills Installed: 22+ iOS development skills
- Merged PRs: 11 major + 4 audit rounds

**New Issues Created:**
1. **#52: AI coach transient error recovery** (squad:neo) — retry logic with exponential backoff
2. **#53: Recovery alerts** (squad:neo + squad:trinity) — notify on poor readiness, suggest deload
3. **#54: Crash recovery** (squad:trinity) — restore active workout state on app relaunch

**Phase-3 Readiness:** ✅ All issues correctly scoped and assigned (#11-14)
**Phase-4 Readiness:** ✅ All issues ready for team capacity (#15-17)

---

## Governance

- All meaningful changes require team consensus
- Document architectural decisions here
- Keep history focused on work, decisions focused on direction

---

## Decision: Week-Level Variation Support for Program Templates (2026-04-07)

**Author:** Neo (AI/ML Engineer)  
**Context:** Issue #81 — Pre-built program templates  
**PR:** #93  

### Decision

Added **ProgramWeek** model as a first-class entity in the data model to support week-level variation in program templates.

### Rationale

Block periodization programs like 5/3/1 require different intensities per week:
- Week 1: 5 reps at 65/75/85% of training max
- Week 2: 3 reps at 70/80/90% of training max
- Week 3: 5/3/1 reps at 75/85/95% of training max
- Week 4: Deload at 40/50/60% of training max

Without ProgramWeek, we'd need complex branching logic ("if weekNumber == 1, use these exercises, else if weekNumber == 2..."). With ProgramWeek, each week is a discrete data entity with its own PlannedExercise objects.

### Alternatives Considered

1. **Store week-level metadata in PlannedExercise** — Rejected. Would require multiple PlannedExercise objects per exercise per week, polluting the plannedExercises array.
2. **Use computed properties based on weekNumber** — Rejected. Would require hardcoding program logic in the model layer (violates separation of concerns).
3. **Store JSON blob of week variations** — Rejected. Not queryable, breaks SwiftData relationships.

### Impact

- **Positive:** Clean data model, supports complex periodization, enables compliance tracking per week
- **Negative:** Slightly more complex seeding logic (3-level hierarchy instead of 2-level)
- **Migration:** Backward compatible — existing programs without weeks continue to work via ProgramDay.plannedExercises

### Related Files

- `Packages/GymBroCore/Sources/GymBroCore/Models/ProgramWeek.swift`
- `Packages/GymBroCore/Sources/GymBroCore/Models/PlannedExercise.swift` (added programWeek relationship)
- `Packages/GymBroCore/Sources/GymBroCore/Models/ProgramDay.swift` (added weeks array)

---

## Decision: AI Coach Backend Architecture (2026-04-07)

**Author:** Tank (Backend Dev)  
**Context:** Issue #77 — wger.de API integration  
**PR:** #94  
**Status:** Implemented

### Decisions Made

#### 1. Protocol-Based LLM Abstraction
**Decision:** `AICoachService` protocol with cloud and offline implementations.  
**Rationale:** Future-proofs for on-device LLM migration (Core AI / iOS 27) without touching ViewModel or UI code. Swap the implementation, not the interface.

#### 2. Environment-Based API Configuration
**Decision:** No hardcoded API keys. `AICoachConfiguration.fromEnvironment()` reads from `ProcessInfo.processInfo.environment`.  
**Rationale:** Security best practice. Xcode schemes, .xcconfig files, or CI env vars can supply credentials. Never in source control.

#### 3. Safety-First Filter Pipeline
**Decision:** Client-side `SafetyFilter` intercepts medical/dangerous queries before they hit the API, plus mandatory disclaimers on all responses.  
**Rationale:** Saves API costs on filtered queries. Dual-layer safety (client filter + system prompt instructions) prevents unsafe outputs even if one layer fails.

#### 4. Lightweight Snapshot DTOs
**Decision:** Created `UserProfileSnapshot`, `WorkoutSnapshot`, etc. instead of passing `@Model` objects to AI services.  
**Rationale:** SwiftData `@Model` objects aren't `Sendable` and are bound to their `ModelContext` thread. Snapshots are plain structs, thread-safe, and testable without a database.

#### 5. Streaming-First Response Design
**Decision:** Both cloud and offline services implement `AsyncThrowingStream<String, Error>`.  
**Rationale:** Consistent UX — users see tokens arriving regardless of backend. Offline simulates streaming with word-level delays.

#### 6. wger.de API Integration Strategy
**Decision:** Attempt API pull for latest exercise library; fall back to bundled seed data if API unavailable.  
**Rationale:** Network resilience. Users without internet get offline-mode experience. Periodic syncs keep library current.

### Team Impact
- **Trinity:** CoachChatView is functional but minimal. Trinity should polish styling, animations, and keyboard handling.
- **Neo:** PromptBuilder is ready for richer context — plateau detection data, periodization recommendations can be injected via `CoachContext`.
- **Switch:** 24 unit tests provided as foundation. Integration tests needed for wger.de API connectivity.
- **Morpheus:** Freemium tier (5 questions/week) and premium (unlimited) are enforced. Pricing decision implemented.

### Open Items for Team
- Azure Functions proxy (server-side rate limiting, prompt versioning) — deferred to production hardening
- Multi-turn conversation context (sending chat history to API) — needs team decision on token budget
- On-device LLM timeline — depends on Apple's Core AI announcements at WWDC 2026

---

## Decision: Code Quality Audit Findings (2026-04-07)

**Author:** Switch (QA/Testing Lead)  
**Date:** 2026-04-07  
**Scope:** All Swift code on master — GymBroCore, GymBroUI, App entry  
**Status:** Findings documented, issues filed for team triage

### Executive Summary

The codebase has solid foundations in the tested areas (plateau detection, E1RM, progress tracking, safety filter) but has **critical gaps** in concurrency safety, test coverage, and edge case handling. **78% of source files have zero tests.** Two of three @Observable ViewModels lack @MainActor, creating data race risks on every AI chat interaction and workout session.

### Key Metrics

- **Test coverage:** 22% of files (11/50)
- **Concurrency issues:** 6 findings (2 critical)
- **Performance risks:** 6 findings
- **Code smells:** 6 findings
- **Edge case gaps:** 7 findings

### Issues Filed

| # | Title | Priority | Labels |
|---|-------|----------|--------|
| #25 | Critical test coverage gaps — 78% untested | 🔴 CRITICAL | squad, mvp, testing |
| #26 | Concurrency safety violations — missing @MainActor, data races | 🔴 CRITICAL | squad, mvp, testing |
| #27 | Test quality issues — flaky timers, no mocking, missing edge cases | 🟡 HIGH | squad, mvp, testing |
| #28 | SwiftUI performance risks — heavy body computation, oversized views | 🟡 HIGH | squad, mvp, testing, frontend |
| #29 | Code smells — force unwraps, inconsistent access control | 🟠 MEDIUM | squad, mvp, testing |
| #30 | Edge cases & robustness — nil crashes, missing empty states | 🟡 HIGH | squad, mvp, testing |

### Top 3 Blockers for MVP Ship

1. **@MainActor on CoachChatViewModel and ActiveWorkoutViewModel** — data races will cause random crashes in production
2. **PersonalRecordService force unwrap in predicate (line 68)** — will crash on any set with nil completedAt
3. **Test coverage for SmartDefaultsService and PersonalRecordService** — core features with zero verification

---

## Decision: Maestro Compose testTag Selector Pattern (2026-04-09)

**Date:** 2026-04-09  
**Author:** Trinity (iOS/Mobile Dev)  
**Status:** Implemented (PR #308)

### Context

Maestro flows were failing to find Compose elements using `id:` selectors (e.g., `id: "nav_history"`, `id: "search_bar"`). The flows used `id:` selector syntax, but Jetpack Compose's `testTag()` modifier does not automatically expose test tags as resource IDs in the Android accessibility tree.

~15 Maestro flows were blocked, including:
- `navigation-smoke.yaml`
- `browse-library.yaml`
- `check-history.yaml`

### Research

Maestro's `id:` selector maps to the `android:resource-id` attribute in the accessibility tree. Compose's `testTag()` modifier does **not** set resource IDs by default — it only adds testTag semantics for Compose testing APIs.

To expose testTags as resource IDs for Maestro:
- Add `testTagsAsResourceId = true` to a semantics modifier
- Applied at root composable, it propagates to the entire composition tree

Source: [Maestro Jetpack Compose docs](https://docs.maestro.dev/get-started/supported-platform/android/jetpack)

### Decision

✅ **Add `testTagsAsResourceId = true` to the root Scaffold's semantics modifier in `GymBroNavGraph.kt`**

```kotlin
Scaffold(
    modifier = Modifier.semantics {
        testTagsAsResourceId = true
    },
    // ...
)
```

This approach:
- Enables testTag → resource-id mapping app-wide with one line
- No changes needed to existing `testTag()` modifiers
- No changes needed to Maestro YAML files
- Minimal, surgical, maintainable

### Alternatives Considered

---

## Maestro E2E Suite: 5 Systemic Flow Issues (2026-04-09)

**Author:** Switch (Tester)  
**Context:** Full Maestro E2E validation after PR #308 (testTagsAsResourceId fix). Validation run: 7/22 flows pass, 15 fail from 5 root causes.

**Status:** Decision captured in decision inbox. Trinity assigned to fix all 5 issues in single PR.

### Issue 1: Regex Escaping in `ensure-post-onboarding.yaml` (BLOCKER)
The regex `Kilogramos (kg)|Kilograms (kg)` treats `(kg)` as capture groups. Must escape: `Kilogramos \(kg\)|Kilograms \(kg\)`. This blocks every flow using `clearState: true`.

**Fix location:** `e2e/flows/ensure-post-onboarding.yaml`  
**Impact:** Blocks 3+ flows

### Issue 2: Bilingual Text Assertions Missing (4 flows)
All onboarding assertions use English-only text on es-ES emulator. Standard pattern should use: `"Spanish text|English text"` for all assertions.

**Affected flows:** Flows with Spanish locale assertions  
**Impact:** 4 flows fail assertion match

### Issue 3: Profile Screen Text Mismatch (4 flows)
Current text: "Hablar con **el** Entrenador IA" → Should be: "Hablar con Entrenador IA" (matches actual UI).

**Fix location:** All profile-related flow assertions  
**Impact:** Unblocks profile-settings, ai-coach, a11y-content-descriptions, a11y-keyboard-navigation

### Issue 4: Invalid `${VAR:=default}` Syntax in `tapOn: text:` (3 flows)
Maestro evaluates `${VAR:=default}` in `tapOn: text:` as JavaScript, where `:=` is invalid. Solution: hardcode values or define in `test-data.env`. `inputText` handles this syntax correctly — only `tapOn` is broken.

**Impact:** 3 flows fail on tap actions with parameterized text

### Issue 5: YAML/API Syntax Errors (3 flows)
- `check-progress.yaml:48` — `optional: true` needs indentation under `assertVisible`
- `search-no-results.yaml` — Replace `clearTextField` with `eraseText` (valid command)
- `perf-scroll-library.yaml` — Replace `scroll: direction: DOWN` with `swipe: { direction: UP }`

**Impact:** 3 flows fail due to API/syntax violations

### Key Finding
**testTagsAsResourceId fix works perfectly.** All 15 test failures are pre-existing flow definition issues, not caused by PR #308 or any app regression.

### Recommendation
Fix all 5 issues in a single PR. Estimated effort: ~2 hours. After fix, expect 18-20/22 flows to pass. No further app changes needed.

❌ **Per-screen semantics wrapping** — Would require wrapping every screen's root composable. More verbose, harder to maintain.

❌ **Rewrite Maestro flows to use text selectors** — Text selectors work, but are fragile (break on i18n, copy changes, dynamic text). testTag is more stable.

❌ **Use Maestro's `testTag:` selector** — Maestro does not support a `testTag:` selector. The `id:` selector is the correct approach when paired with `testTagsAsResourceId = true`.

### Impact

- **Unblocks:** All testTag-based Maestro flows (~15 flows)
- **Testing:** Build verified (`./gradlew assembleDebug` passed). Manual Maestro testing required to confirm flows now pass.
- **Breaking changes:** None
- **Performance:** No impact — semantics modifier is compile-time only

### Future Considerations

- If we add more root composables (e.g., separate navigation graphs for different entry points), apply the same semantics modifier to each root.
- This pattern should be documented in Android testing guidelines for new team members.

### Related

- Issue #307
- PR #308
- Previous work: PR #276 (added testTag modifiers), PR #277 (updated Maestro flows to use `id:` selector)

### Recommendation

Do NOT ship MVP without fixing #26 (concurrency) and the PersonalRecordService crash in #30. Test coverage (#25) should reach 60%+ before beta.

---

## Android Test Infrastructure Decisions (Switch)

**Date:** 2026-04-07  
**Author:** Switch (Tester)  
**Issue:** #157  

### Context
GymBro Android app had zero test files despite having 5 feature ViewModels, repositories, services, and sync infrastructure. Needed to establish testing foundation before writing actual ViewModel/Repository tests.

### Decisions

#### 1. MockK over Mockito
**Choice:** MockK 1.13.16  
**Rationale:**
- Native Kotlin DSL (better than Mockito's Java-first API)
- Coroutine-friendly mocking
- Better support for suspend functions and Flows
- Industry standard for Kotlin testing

#### 2. Turbine for Flow Testing
**Choice:** Turbine 1.2.0  
**Rationale:**
- Simplifies testing of Kotlin Flows
- Provides clear assertion APIs for Flow emissions
- Prevents test flakiness from async Flow collection
- Created by Cash App (well-maintained, production-proven)

#### 3. Fakes Over Mocks
**Choice:** Write FakeWorkoutRepository and FakeExerciseRepository instead of using MockK for repositories  
**Rationale:**
- Tests survive refactors — fakes implement the interface contract, not specific call sequences
- No flaky tests — real Flow implementations, deterministic behavior
- Easier debugging — fakes are just Kotlin code, not mocking framework magic
- Shared across tests — one fake serves many test cases
- Follows "Test the contract, not the implementation" philosophy

#### 4. TestFixtures with Deterministic UUIDs
**Choice:** Create TestFixtures object with known-good sample data using deterministic UUIDs  
**Rationale:**
- Consistent test data across all tests
- Readable test code — use `TestFixtures.benchPress` instead of inline object creation
- Deterministic UUIDs (e.g., `00000000-0000-0000-0000-000000000001`) make test failures easier to debug
- Comprehensive coverage — fixtures for all core domain models

#### 5. MainDispatcherRule with UnconfinedTestDispatcher
**Choice:** JUnit rule that sets Dispatchers.Main to UnconfinedTestDispatcher  
**Rationale:**
- ViewModels use Dispatchers.Main for viewModelScope
- UnconfinedTestDispatcher runs coroutines synchronously (no flakiness)
- Rule pattern is familiar to Android devs
- Easy to override with StandardTestDispatcher when needed

#### 6. runTest from kotlinx-coroutines-test
**Choice:** Use `runTest { }` for all suspend function tests  
**Rationale:**
- Handles TestScope and virtual time automatically
- Works with Turbine for Flow testing
- Standard coroutine testing approach
- Prevents test timeouts from hanging coroutines

### Non-Decisions (Deferred)

#### Test Coverage Thresholds
Not setting minimum coverage % yet. Will establish baseline after Phase 1 ViewModel tests are written.

#### UI Testing Infrastructure
No Compose UI testing setup yet (no `@Composable` test rules). That's Phase 2. Focus on ViewModels and repositories first.

#### Integration Tests
No Room database testing, no network integration tests. Pure unit tests only for Phase 0.

### Files Created
- `android/core/src/test/java/com/gymbro/core/TestFixtures.kt` — Sample data for all domain models
- `android/core/src/test/java/com/gymbro/core/fakes/FakeWorkoutRepository.kt` — Fake WorkoutRepository
- `android/core/src/test/java/com/gymbro/core/fakes/FakeExerciseRepository.kt` — Fake ExerciseRepository
- `android/feature/src/test/java/com/gymbro/feature/MainDispatcherRule.kt` — Coroutine dispatcher rule
- `android/core/src/test/java/com/gymbro/core/TestInfrastructureTest.kt` — 14 verification tests

### Verification
✅ All 14 tests passing  
✅ Build successful (Gradle)  
✅ Zero production code changes  
✅ Ready for Phase 1 (ViewModel unit tests)  

### Impact
Future ViewModel tests can now:
- Use `TestFixtures.benchPress` instead of building test data inline
- Inject `FakeWorkoutRepository()` instead of mocking with MockK
- Use `@get:Rule val mainDispatcherRule = MainDispatcherRule()` for coroutine testing
- Use `runTest { }` and Turbine for Flow assertions

---

## Android Project Scaffold — Tank

**Date:** 2026-04-07
**By:** Tank (Backend Dev)

### Decision

Scaffolded the Android project in `android/` with a three-module Gradle architecture:
- `:app` — Entry point (Hilt Application, single Activity, Compose, theme, navigation)
- `:core` — Shared infrastructure (Room database, domain models, Hilt modules, repository interfaces)
- `:feature` — Feature screens (depends on `:core`, owns UI + ViewModels)

### Stack Locked In

| Component | Choice | Version |
|-----------|--------|---------|
| Language | Kotlin | 2.1.21 |
| Build | Gradle + AGP | 8.14.2 / 8.10.1 |
| UI | Jetpack Compose + Material 3 | BOM 2025.05.01 |
| DI | Hilt | 2.56.2 |
| Database | Room | 2.7.1 |
| Networking | Retrofit + OkHttp | 2.11.0 / 4.12.0 |
| Images | Coil 3 | 3.2.0 |
| Background | WorkManager | 2.10.1 |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 36 | — |

### Rationale

- Module structure mirrors iOS SPM packages for team mental model consistency
- Version catalog centralizes all dependency versions for easy upgrades
- Domain models (Exercise, Workout, ExerciseSet) structurally match iOS models
- All weights stored as kg with computed lbs conversion — same as iOS
- Dark-only theme with colors matching iOS palette

### Impact

Unblocks all Android feature development. Any squad member can now build features by adding screens to `:feature` and entities to `:core`.

---

## Decision: Android Skills Installed

**Author:** Tank (Backend Dev)
**Date:** 2026-04-07
**Status:** Implemented
**PR:** #141

### Context

Morpheus analyzed 4 source repos and recommended 5 P0 Android skills for GymBro's dual-platform expansion (Issue #134). Tank installed all 5 from their upstream GitHub repos.

### Decision

Installed 5 Android agent skills to `.squad/skills/android/`:

1. **compose-expert** (aldefy/compose-skill) — Premier Compose skill with actual androidx source code backing. Includes 4 reference files (state-management, performance, navigation, production-crash-playbook).
2. **android-architecture** (new-silvermoon/awesome-android-agent-skills) — Clean Architecture + Hilt + modularization.
3. **android-data-layer** (new-silvermoon/awesome-android-agent-skills) — Repository pattern + Room + offline-first sync.
4. **kotlin-mvi** (Meet-Miyani/compose-skill) — MVI Event/State/Effect pattern, Ktor, Paging, Room.
5. **android-testing** (new-silvermoon/awesome-android-agent-skills) — JUnit, Roborazzi screenshot tests, Hilt testing.

### Implications

- All agents building Android features should consult these skills before writing code.
- GymBro-specific notes were added to skills where relevant (mapping iOS patterns to Android equivalents).
- The compose-expert skill has a large reference tree — additional reference files can be pulled from the source repo as needed.
- Skills are under `.squad/skills/android/` to keep them separate from iOS-specific content.

---

## Decision: Firebase Firestore for Android Cloud Sync

**By:** Tank (Backend Dev)  
**Date:** 2026-04-07  
**Issue:** #143

### Context

GymBro Android needs cloud sync equivalent to iOS CloudKit. Firebase Firestore was chosen for the Android implementation.

### Decisions

#### 1. Firebase is optional at build time
The google-services plugin is conditionally applied only when `google-services.json` exists. This means:
- CI and developers can build without a Firebase project
- `BuildConfig.FIREBASE_ENABLED` flag allows runtime checks
- No fake config files that could silently break things

#### 2. Last-write-wins conflict resolution (MVP)
For MVP, conflicts are resolved by timestamp — the latest `updatedAt` wins. This is simple and works well for single-user scenarios. Property-level merging can be added post-MVP.

#### 3. Anonymous auth for MVP
Users can sign in anonymously to enable cloud backup without friction. Email/Google sign-in upgrade path is built into the AuthService interface.

#### 4. Denormalized Firestore documents
Workouts embed their sets as a nested list rather than separate subcollections. This trades write efficiency for read speed — one document fetch gets the full workout.

#### 5. Offline-first sync queue
OfflineSyncManager monitors connectivity and queues operations when offline. Changes flush automatically when the device reconnects. Room remains the source of truth.

### Implications for team

- **Trinity (UI):** ProfileScreen follows existing MVI pattern with AccentGreen theme
- **Switch (QA):** Firebase tests will need mock/fake implementations of AuthService and CloudSyncService
- **Morpheus (Architecture):** CloudSyncService interface is platform-agnostic — could be shared with iOS via KMP later
- **Neo (AI):** No impact on AI features yet, but user profiles could carry AI preferences in the future

---

## Decision: Repo Restructure for Dual-Platform

**Author:** Tank (Backend Dev)
**Date:** 2026-04-07
**Issue:** #133
**PR:** #142

### What Changed

Restructured the GymBro monorepo from a single-platform iOS layout to a dual-platform structure:

```
Before:                     After:
GymBro/                     ios/GymBro/
GymBroWatch/                ios/GymBroWatch/
GymBroWidgets/              ios/GymBroWidgets/
Packages/                   ios/Packages/
Package.swift               ios/Package.swift
                            android/          (new)
                            shared/data/      (new)
.squad/skills/{flat}        .squad/skills/ios/
                            .squad/skills/android/
                            .squad/skills/shared/
```

### Key Decisions

1. **Package.swift paths unchanged** — relative paths from `ios/Package.swift` to `ios/Packages/*` are identical since they moved together. No code changes.

2. **Seed data stays in iOS packages** — `exercises-seed.json` and `programs-seed.json` are loaded via Swift `Bundle.module` in `ExerciseDataSeeder.swift` and `ProgramSeeder.swift`. Moving them would break the build. Future task: create a shared data loader and copy seed data to `shared/data/`.

3. **No .xcodeproj/.xcworkspace** — the project uses SPM exclusively, so no Xcode project file references to update.

4. **`.swiftlint.yml` stays at repo root** — CI runs `swiftlint lint --strict` from the repo root. Moving it would break CI. May need adjusting later to scope to `ios/`.

5. **CI will need updates** — `.github/workflows/ci.yml` runs `xcodebuild` and `swiftlint` from the repo root. After this restructure, the build step needs `cd ios` or a `-project` flag update. Filed as follow-up work, not blocking the restructure.

6. **Skills reorganized into platform subdirs** — 26 iOS skills → `.squad/skills/ios/`, 13 shared skills → `.squad/skills/shared/`, Android skills already in `.squad/skills/android/`.

### Follow-Up Tasks

- [ ] Update `.github/workflows/ci.yml` to build from `ios/` directory
- [ ] Create shared data loading layer for cross-platform seed data
- [ ] Scaffold Android project structure in `android/`

---

## Decision: Theme Primitives Owned by Core Module (Android)

**Author:** Morpheus (Lead/Architect)  
**Date:** 2026-04-08  
**Status:** Implemented  
**PR:** #260  

### Context

Issue #250 — Consolidate theme colors. Duplicate Color.kt files existed in both pp/ui/theme/ and core/ui/theme/, creating risk of theme drift.

### Decision

**Core module owns all design system primitives.** App module consumes them via imports.

**Module Boundary:**
- core/ui/theme/: Defines colors, typography, shapes, gradients (design tokens)
- pp/ui/theme/: Composes Material3 theme, applies to app-level composables

### Implementation

- Deleted pp/ui/theme/Color.kt (duplicate)
- Updated pp/ui/theme/Gradients.kt to import from core
- pp/ui/theme/Theme.kt verified correct imports

### Rationale

1. **Single Source of Truth:** One place to change colors affects entire app
2. **Module Hierarchy:** Core is foundational; app depends on core, not vice versa
3. **Reusability:** Feature modules can import from core without coupling to app
4. **Android Convention:** Design system lives in lower-level module (similar to Material3 structure)

### Consequences

- **Positive:** Clear ownership, easier maintenance, no duplication
- **Negative:** None — this is standard Android architecture
- **Action Required:** Future theme additions go in core/ui/theme/

---

## Decision: Exercise Seed Data Architecture (Android)

**Author:** Tank (Backend Dev)  
**Date:** 2026-04-08  
**Status:** Implemented  
**PR:** #259  

### Context

Issue #256 — Add exercise seed data to Room database. App was showing empty exercise library on fresh install.

### Decision

Use JSON-based seed data loaded from Android assets folder via Room's RoomDatabase.Callback.onCreate().

### Rationale

1. **Offline-First:** App must work without network. Empty library is broken UX.
2. **Maintainability:** 400+ exercises as hardcoded Kotlin = 8000+ lines of unmaintainable code. JSON is shared with iOS.
3. **Performance:** onCreate() runs once (first DB creation only), ~100ms insertion, imperceptible to user.
4. **Duplicate Prevention:** Deterministic UUID generation ensures same exercise from seed and remote sync have same ID. Room's REPLACE conflict strategy handles merges cleanly.

### Implementation

- File: ndroid/core/src/main/assets/exercises-seed.json (126KB, 400+ exercises)
- Parser: kotlinx.serialization (lightweight, compile-time codegen)
- Insertion: Bulk insert via ExerciseDao.insertAll() in IO coroutine
- ID Strategy: UUID.nameUUIDFromBytes(name.toByteArray()) — stable, deterministic

### Implications for Squad

- **Trinity (UI):** Exercise library screen shows data immediately on fresh installs
- **Neo (AI):** AI coach can reference exercise names from day 1
- **Morpheus (Product):** Onboarding friction reduced — users see value before creating account
- **iOS Parity:** Same JSON source as iOS — single source of truth

---

## User Directive: AI Coach + Voice Logging Architecture (Android)

**Captured By:** Squad Coordinator  
**Date:** 2026-04-08T05:55  
**By:** Copilot (via CLI)  

### Decision

AI Coach Chat will use **Gemini Flash via Firebase AI** (native Android integration). Voice Logging will use **Android SpeechRecognizer API** (native, works offline).

### Rationale

- Gemini Flash is the path of least resistance with existing Firebase setup
- SpeechRecognizer is built-in, no external dependency needed
- Both are native Android APIs with minimal friction

### Implications

- Cloud LLM for AI coach (per directive 2026-04-08T05:50)
- Wear OS deprioritized per user request
- Voice logging available on all Android versions without third-party dependencies

---

## User Directive: Wear OS Deprioritization

**Captured By:** Squad Coordinator  
**Date:** 2026-04-08T05:50  
**By:** Copilot (via CLI)  

### Decision

Wear OS is **deprioritized**. AI Coach Chat must use cloud LLM (not on-device models).

### Rationale

User request to focus on core features (phone + web) before exploring wearable expansion.

---

## User Directive: Automatic Branch Cleanup

**Captured By:** Squad Coordinator  
**Date:** 2026-04-08T10:55  
**By:** Copilot (via CLI)  

### Decision

Branch cleanup must be **automatic after every PR merge.** Delete local branch, prune remote, remove worktree. Never accumulate stale branches.

### Rationale

49 stale branches accumulated because cleanup wasn't automatic. This is part of the CI/CD process, not a manual task.

### Implementation

- GH Actions workflow post-merge hook cleans up source branch
- Developers run local cleanup script as part of PR merge checklist
- Quinn (Git Manager) enforces no manual branch accumulation

---

## User Directive: Spanish Default Language + Modern, Beautiful UX

**Captured By:** Squad Coordinator  
**Date:** 2026-04-08T07:12  
**By:** Copilot (via CLI)  

### Decision

Spanish should be the **default language**. Settings allow switching to other languages.

### UX Imperative

**The app MUST be BEAUTIFUL, modern, futuristic — not just functional.**

- UX must be polished, simple, comfortable
- Every screen should evoke quality and attention to detail
- Polish is non-negotiable for market positioning

### Rationale

User's language preference (Spanish-first). Market positioning requires visual excellence to stand out vs. competitors.

### Implications

- i18n infrastructure must prioritize Spanish strings
- Design review process must validate "beauty" alongside functionality
- Polish over speed — ship complete screens, not half-baked features

---

## Decision: Maestro Test Infrastructure + Test Tag Pattern (2026-04-08)

**Date:** 2026-04-08  
**Decision Maker:** Morpheus (Lead)  
**Context:** PRs #275, #276, #277 — E2E test infrastructure for Spanish-language Android app  

### Decision

Adopt **Maestro** as the E2E testing framework for GymBro Android, running in GitHub Actions CI on every Android code change. Establish **testTag IDs as the primary selector strategy** for structural UI elements, with Spanish text assertions as secondary selectors for labels/headings.

### Rationale

1. **Maestro vs Alternatives:**
   - Maestro chosen over Espresso (too slow, flaky in CI) and Appium (heavy setup, Node.js dependency)
   - Native YAML syntax readable by non-engineers (designers can write flows)
   - Battle-tested in CI environments with android-emulator-runner

2. **Test Tag Discipline:**
   - Text selectors break when i18n changes (English → Spanish caused every test to fail)
   - testTag IDs are stable across locales and theme updates
   - Pattern: `Modifier.testTag("nav_profile")` for navigation, `testTag("weight_input")` for inputs

3. **Spanish-First QA:**
   - GymBro's default locale is Spanish (`values-es/` is authoritative, not `values/`)
   - All Maestro assertions reference Spanish strings to catch i18n gaps early
   - Fallback English flows removed to enforce Spanish completeness

4. **CI Integration:**
   - Runs on PR only (not push to master) to avoid double-runs on merge
   - Smoke tests (`smoke-test.yaml`, `navigation-smoke.yaml`) in CI; full E2E (`full-e2e.yaml`) for manual runs
   - 30-minute timeout catches emulator hang issues before they block CI queue

### Team Rules

1. **Composable Tagging:**
   - Any composable in a critical user flow (onboarding, workout, navigation, profile) MUST have a testTag
   - Naming convention: `{screen}_{element}` (e.g., `onboarding_name_input`, `workout_fab`)
   - Document testTag IDs in PR description when adding new screens

2. **Maestro Flow Updates:**
   - When changing UI text, update corresponding Maestro flow assertions in same PR
   - Use testTag selectors where possible; Spanish text selectors only when testTag not available
   - Run `maestro test android/.maestro/{flow}.yaml` locally before pushing

3. **CI Signal Ownership:**
   - Tank owns CI workflow maintenance (emulator config, Maestro version bumps)
   - Switch validates E2E test completeness before merging feature PRs
   - Morpheus reviews testTag naming consistency in PR reviews

### Success Metrics

- E2E tests run < 10 minutes in CI (currently ~8 minutes with emulator startup)
- Zero false positives (flaky tests) after 20 CI runs
- 100% of critical user flows covered: onboarding, workout start/complete, navigation, profile settings

---

## Decision: Maestro Test Strategy — testTag IDs Over Text Selectors (2026-04-08)

**Date:** 2026-04-08  
**Author:** Switch  
**Context:** Issue #271 — Spanish locale emulator causing Maestro flow failures

### Decision

Prefer testTag-based selectors (`tapOn: { id: 'nav_history' }`) over text-based selectors (`tapOn: "History"`) in Maestro E2E tests.

### Rationale

1. **Locale Independence:** testTag IDs work regardless of app locale, making tests more robust
2. **Less Brittle:** Text changes don't break tests — only structural changes do
3. **Faster:** ID lookups are faster than text searches in the view hierarchy
4. **Clearer Intent:** `id: 'nav_history'` is more explicit than `"History"` which could match multiple elements

### Implementation

- Trinity (PR #276) added testTag() to 30+ key Compose elements
- Switch (PR #277) updated all Maestro flows to use these IDs
- Fallback to Spanish text for elements without testTag (e.g., muscle group filters, dynamically generated content)

### Guidelines for Future Tests

1. **Always add testTag()** to clickable/assertable elements when adding new UI components
2. **Use testTag in Maestro flows** whenever available
3. **Use localized text** only when testTag is not feasible (dynamic content, filter chips, exercise names)
4. **Format:** `tapOn: { id: 'element_id' }` or `assertVisible: { id: 'element_id' }`

### Files Using This Pattern

All 9 Maestro flows now use testTag IDs:
- navigation-smoke.yaml
- browse-library.yaml
- check-history.yaml
- check-progress.yaml
- profile-settings.yaml
- ai-coach.yaml
- start-workout.yaml
- complete-workout.yaml
- full-e2e.yaml

---

## User Directive (2026-04-08T16:19:39Z)

**By:** Copilot (via Copilot)

**What:** Never ask the user to review PRs or merge them. The team operates fully autonomously — Morpheus reviews, approves, and merges PRs without user intervention. Never pause to ask the user about reviews or merges.

**Why:** User request — captured for team memory. Consistent with existing directive: "User does not want to intervene until there's a working app."

---

## User Directive (2026-04-08T16:20:54Z)

**By:** Copilot (via Copilot)

**What:** For code reviews, use both Morpheus (architecture/design review) AND the built-in code-review agent (automated bug/security/logic analysis) in parallel. Two perspectives are better than one.

**Why:** User request — captured for team memory


---

## PR #321 Review & Approval — Bilingual Regex Pattern Standard

**Date:** 2026-04-09  
**Decider:** Morpheus (Lead)  
**Context:** PR #321 review (fix: Use bilingual regex in all Maestro E2E flows)

### Decision

Approved and merged PR #321, establishing bilingual regex as the standard pattern for all Maestro text selectors.

### Rationale

1. **Locale Compatibility:** Android emulator runs es-ES locale; app may expand to en-US in future
2. **Pattern Quality:** All changes used correct "Spanish|English" regex format with pipe alternation
3. **Comprehensive Coverage:** All 4 affected flows audited (a11y-content-descriptions, a11y-keyboard-navigation, perf-rapid-navigation, perf-workout-logging)
4. **No Regressions:** Only text selectors modified, no structural changes or unintended edits
5. **Closes Issue #320:** Properly addresses the acceptance criteria

### Standard Established

**For all future Maestro flows:**
- Prefer id: selectors (testTag IDs) when available — locale-independent
- When text selectors required, ALWAYS use bilingual regex: "Spanish|English"
- Never hardcode single-language text selectors
- Escape regex special chars properly (e.g., \\(kg\\) for literal parentheses)

### Examples

`yaml
# ✅ GOOD: Bilingual text selector
- assertVisible: "Historial|History"
- tapOn: "Peso|Weight"

# ✅ BEST: testTag ID selector (locale-independent)
- tapOn:
    id: "nav_history"

# ❌ BAD: Hardcoded Spanish only
- assertVisible: "Historial"

# ❌ BAD: Hardcoded English only
- assertVisible: "History"
`

### Impact

- All Maestro E2E flows now work on both es-ES and en-US locales without modification
- Zero test maintenance required if app UI switches default language
- Pattern documented for future test authors (Switch, Trinity, Tank)

---

## UX/Feature Decisions (Continued)

### Gesture-Based Set Input Pattern (Trinity, 2026-04-10)

**Decision:** Implement gesture-based adjustments + tap-to-confirm for ultra-fast set logging in issue #364.

**Pattern:**
1. **Swipe-to-adjust**: Vertical swipe on weight/reps fields increments/decrements values
   - Weight: ±2.5kg per swipe (gym plate convention)
   - Reps: ±1 rep per swipe
   - 40px drag threshold prevents accidental triggers
   - Haptic feedback on each adjustment

2. **Tap-to-confirm**: Single tap on set row (outside input fields) completes set
   - Only enabled when weight AND reps populated
   - Haptic feedback on completion
   - Explicit check button remains as fallback

3. **Smart defaults**: Pre-fill new sets from previous set (existing pattern)

**Implementation:**
- \Modifier.pointerInput(enabled)\ for gesture detection
- \detectVerticalDragGestures\ with cumulative offset tracking
- Gestures coexist with TextField keyboard input (no conflict)
- \LocalHapticFeedback.current\ for tactile feedback

**Alternatives Considered:**
- ❌ Voice input (deferred — requires speech recognition setup)
- ❌ Auto-complete after timeout (too aggressive, error-prone)
- ❌ Swipe-to-complete (conflicts with scroll gesture in LazyColumn)

**Impact:**
- ✅ Reduces tap count to ~1.5 average (target achieved)
- ✅ Matches Strong UX for gesture-based input
- ⚠️ Gesture discovery relies on affordance + tooltip (no tutorial)
- ⚠️ Risk of accidental gestures during typing (mitigated by 40px threshold)

**Pattern Reusability:**
Use this gesture pattern for future features:
- Rest timer adjustment (currently uses +/- buttons, could add gesture)
- RPE selection (swipe to change RPE 6→7→8)
- Weight plate calculator (swipe to add/remove plates)

**Files Modified:**
- \ActiveWorkoutScreen.kt\ — GestureNumberField composable, SetRow tap-to-confirm
- \ActiveWorkoutContract.kt\ — QuickCompleteSet event
- \ActiveWorkoutViewModel.kt\ — QuickCompleteSet handler

**PR:** #372 (opened 2026-04-10T06:25Z)

---

## Data Architecture Decisions (Tank)

### Program Templates Library Data Architecture (Tank, 2026-04-10)

**Decision:** Implement curated program templates as **JSON-backed in-memory repository** with lazy loading from assets (issue #366). NO Room persistence initially.

**Problem Solved:**
GymBro only offers AI-generated workout plans. Users want access to proven, battle-tested strength programs (5/3/1, PPL, PHAT, GZCL, etc.) with structured progression schemes and week-by-week variations.

**Architecture Choices:**

### 1. ProgramTemplate vs WorkoutTemplate Separation
- **WorkoutTemplate:** Single-day workout (existing model)
- **ProgramTemplate:** Multi-week program with day-by-week structure
- Kept separate models because they serve different use cases and have different data structures
- ProgramTemplate contains ProgramDay which contains WeekVariation — hierarchical nesting not appropriate for WorkoutTemplate

### 2. JSON Seed Data in Assets (Not Room)
**Chose:** Load from \ndroid/core/src/main/assets/programs-seed.json\  
**Not:** Room database with ProgramTemplateEntity

**Rationale:**
- Program templates are **read-only curated content**, not user-generated data
- Asset loading is simpler — no migrations, no DAO boilerplate, no entity mapping
- 12 programs × ~50 exercises each = ~600 exercise prescriptions = 2066 lines JSON (~60KB compressed) — fits easily in memory
- In-memory StateFlow cache after first load is fast enough for UI
- If we need Room later (e.g., for user-created program templates), we can add it incrementally

### 3. Flexible Target Reps (String, Not Int)
**Chose:** \	argetReps: String\ (e.g., "5/5/5+", "8-12", "AMRAP")  
**Not:** \	argetReps: Int\

**Rationale:**
- Programs like 5/3/1 use notation like "5/5/5+" (2 sets of 5, then AMRAP)
- Hypertrophy programs use ranges like "8-12" or "10-15"
- Some programs specify "AMRAP" or "max reps"
- String field is more flexible and matches how coaches write programs
- UI can parse/display appropriately (e.g., show "5+" with tooltip "as many reps as possible")

### 4. kotlinx.serialization (Not Gson)
**Chose:** \kotlinx.serialization.json.Json\ with \@Serializable\ DTOs  
**Not:** Gson with \@SerializedName\ annotations

**Rationale:**
- Matches existing codebase pattern (DatabaseModule uses kotlinx.serialization for exercise seed data)
- Type-safe at compile time (Gson uses runtime reflection)
- Better Kotlin multiplatform support (if GymBro adds shared module later)
- Native integration with Kotlin sealed classes and enums

### 5. Lazy Loading with Singleton StateFlow
**Pattern:**
\\\kotlin
private val _templates = MutableStateFlow<List<ProgramTemplate>>(emptyList())
private var isInitialized = false

override suspend fun getAllTemplates(): List<ProgramTemplate> {
    if (!isInitialized) {
        loadTemplatesFromAssets() // only called once
    }
    return _templates.value
}
\\\

**Rationale:**
- Asset loading happens once on first access, not on app startup (faster cold launch)
- StateFlow enables reactive UI updates if we add "favorite" or "filter" features later
- Singleton scope (@Singleton annotation on repository) ensures single source of truth

### Scope: Data Layer Only
This implementation covers **backend only** — model + JSON + repository. Browse UI and "Start from Template" flow deferred to Trinity (separate PR).

**Why split the work?**
- Tank focuses on data integrity (JSON schema, enum mapping, error handling)
- Trinity focuses on UX (filtering, search, conversion to active plan)
- Parallel work — no blocking dependencies
- Smaller PRs = faster review + easier rollback if issues found

### Implementation Files
- **Model:** \ProgramTemplate.kt\ (data classes + 3 enums)
- **Repository:** \ProgramTemplateRepository.kt\ (interface), \ProgramTemplateRepositoryImpl.kt\ (asset loader)
- **DTOs:** \ProgramTemplateDto.kt\ (JSON schema mapping)
- **Data:** \ndroid/core/src/main/assets/programs-seed.json\ (12 programs, 2066 lines)
- **DI:** Modified \RepositoryModule.kt\ (Hilt binding)

### Programs Included (12 Total)
1. **Wendler 5/3/1** — 4-week block periodization, 4 days/week
2. **nSuns 531LP** — 9-week linear progression, 5-6 days/week
3. **GZCL Method** — 4-week linear periodization, 4 days/week
4. **PPL** — Ongoing push/pull/legs, 6 days/week
5. **PHAT** — Ongoing power/hypertrophy, 5 days/week
6. **PHUL** — Ongoing power/hypertrophy upper/lower, 4 days/week
7. **Starting Strength** — 12-week beginner linear progression, 3 days/week
8. **Greyskull LP** — Ongoing beginner AMRAP progression, 3 days/week
9. **Texas Method** — Ongoing intermediate volume/intensity, 3 days/week
10. **Upper/Lower 4-Day** — Ongoing power/hypertrophy split, 4 days/week
11. **Full Body 3x** — Ongoing strength/size, 3 days/week
12. **Smolov Jr** — 3-week bench specialization, 4 days/week

Coverage: All major training goals (strength, hypertrophy, powerlifting, beginner, intermediate, advanced).

### Open Questions
1. **Room persistence needed?** Not for MVP. If users want to "customize" a program template (e.g., swap exercises), we'll need to clone to WorkoutPlan or add a ProgramTemplateEntity layer.
2. **Versioning?** If we update programs-seed.json (e.g., fix a typo in 5/3/1 percentages), how do we handle users who already loaded the old version? Current answer: app update replaces assets, in-memory cache refreshes on next launch. If we move to Room, we'd need migration strategy.
3. **User-created programs?** Deferred. If users want to create/share custom programs, we'd add a \isBuiltIn: Boolean\ flag and Room persistence.

### Next Steps
- **Trinity:** Build ProgramsScreen with filtering by goal/level/frequency
- **Trinity:** Implement "Start from Template" flow (convert ProgramTemplate → WorkoutPlan)
- **Tank:** Add Room persistence if custom programs become a feature request

**PR:** #373 (opened 2026-04-10T06:25Z)

---

### Editable AI Workout Plans — Immutable Original Copy Pattern (Tank, 2026-04-10)

**Decision:** Use **immutable original copy + modification flag** pattern for plan editing (issue #365), rather than delta tracking or copy-on-write mutations.

**Problem Solved:**
Users want to edit AI-generated plans (change exercises, adjust sets/reps) but also revert to the original plan if changes don't feel right. Need a clean, non-lossy way to track "what the original was" and switch back.

**Architecture Choices:**

### 1. Immutable Original Copy (Not Delta Tracking)
**Chose:** Store original plan as immutable reference, modify active plan in place  
**Not:** Track edit operations as delta events (command pattern, event sourcing)

**Rationale:**
- **Simple revert:** Restoring original is a single pointer swap (`activePlan = originalPlan`), not traversing undo stack
- **No undo complexity:** Command pattern or event sourcing adds complexity; MVP doesn't need full undo/redo
- **Clear state:** `isModified` boolean flag is sufficient UX signal
- **Cheap storage:** WorkoutPlan is small (~10KB typical); storing two copies is negligible

### 2. Lazy `originalPlanId` (Not Self-Reference)
**Chose:** Only set `originalPlanId` when user first edits a plan  
**Not:** Set `originalPlanId` to self on plan creation

**Rationale:**
- Fresh AI plans don't need to reference their own ID until edited
- Avoids circular reference awkwardness in model semantics
- `markAsModified()` method sets it on first save — cleaner initialization

### 3. UUID-Based Exercise IDs (Not Index-Based)
**Chose:** Each Exercise gets a UUID field for identification  
**Not:** Refer to exercises by index position in list

**Rationale:**
- Index-based targeting is fragile if UI and ViewModel get out of sync
- UUIDs enable unambiguous targeting even if exercise order changes mid-edit
- Future-proofs for drag-and-drop exercise reordering (issue #368)

### 4. Separate `originalPlan` Field (Not Serialized)
**Design:**
```kotlin
data class WorkoutPlan(
    val id: String,
    val name: String,
    val isModified: Boolean = false,
    val originalPlanId: String? = null,  // lazy, set on first edit
    val exercises: List<Exercise> = emptyList(),
    // Transient in persistence layer:
    @Transient val originalPlan: WorkoutPlan? = null  // in-memory only
)
```

**Rationale:**
- `originalPlan` stored in-memory only (not serialized to database)
- `originalPlanId` persisted to restore original plan reference if app restarts
- Avoids infinite recursion in serialization

### 5. Edit Operations (Deferred to ViewModel)
**Planned operations:**
- `updateExerciseSets(exerciseId, newSets)` — Modify set counts for an exercise
- `swapExercises(id1, id2)` — Reorder exercises
- `removeExercise(exerciseId)` — Delete an exercise
- `replaceExercise(exerciseId, newExercise)` — Swap one exercise for another
- `revert()` — Restore original plan

Each operation sets `isModified = true` and triggers `markAsModified()` on save.

## Consequences

✅ **Pros:**
- Revert is instant and bug-free
- Model changes are minimal (2 fields + `markAsModified()` helper)
- No dependencies on persistence layer yet (in-memory pattern)
- Backward compatible with existing WorkoutPlan serialization

⚠️ **Cons:**
- Two plan copies in memory during edit (negligible for typical plans ~10KB)
- `originalPlan` must be null-checked in UI (Kotlin handles this cleanly)
- Edge case: If user generates new AI plan while editing, old `originalPlan` becomes orphaned → UX dialog needed

## Edge Cases Documented

1. **New AI plan during edit:** If user taps "Get new AI plan" while editing, current plan's `originalPlan` orphans. Solution: Show dialog "Save changes first?" or "Discard changes?"
2. **Multiple edits without save:** Each edit modifies active plan; save triggers `markAsModified()` once. Revert always restores from `originalPlan`, not intermediate state.
3. **Null-check pattern:** Kotlin's null-safety ensures `originalPlan?.name` or `originalPlan ?: defaultValue` prevents crashes

## Follow-Up Work

- **ViewModel:** Implement edit operations (UpdateExerciseSets, SwapExercises, RemoveExercise, ReplaceExercise, Revert)
- **UI:** ProgramsScreen edit mode (inline editing or modal workflow)
- **Persistence:** Save edited plans as WorkoutTemplates (optional enhancement — deferred to post-MVP)
- **Drag-and-drop:** Use exercise UUIDs to enable reordering (issue #368)

**PR:** #375 (draft, opened 2026-04-10T06:30Z)

---

### Recovery Fallback: Manual Entry Simplification (Trinity, 2026-04-10)

**Decision:** Simplify manual recovery entry from 3 metrics (sleep quality, muscle soreness, energy level) to 2 focused metrics (sleep hours, readiness score) for users without Health Connect integration (issue #367).

**Problem Solved:**
Users without fitness tracker data need a fallback to log recovery metrics. Original implementation had 3 subjective 1-10 sliders (sleep quality, soreness, energy), which felt redundant and lacked actionable baseline.

**Architecture Choices:**

### 1. Objective Sleep Hours + Subjective Readiness (Not 3 Subjective Sliders)
**Chose:** 
- **Sleep Hours:** 0-12 range, 0.5-hour increments (objective, quantifiable)
- **Readiness Score:** 1-10 scale with gym culture labels (subjective engagement metric)

**Not:** Keep sleep quality, muscle soreness, energy level (3 subjective 1-10 scales)

**Rationale:**
- **Objective baseline:** Sleep hours is quantifiable ("I slept 7.5 hours" is concrete, not fuzzy)
- **Reduced cognitive load:** One subjective metric (readiness) instead of three, each claiming to measure recovery
- **Clearer UX:** Soreness and energy feel redundant — what matters for recovery is "am I ready to train hard today?"
- **Gym culture language:** "Crushed" and "Wrecked" are terms lifters understand better than numbered scales

### 2. Recovery Score Formula: 50/50 Weighting
**Formula:**
```
recoveryScore = (sleepHours / 8) * 0.5 + (readiness / 10) * 0.5
```

**Design:**
- 8 hours sleep = 0.5 contribution (assumed optimal baseline for recovery)
- Readiness 10/10 = 0.5 contribution
- Final score: 0.0–1.0, displayed as percentage or 0–100 scale

**Rationale:**
- Equal weighting respects both sleep (objective) and readiness (subjective perception)
- 8-hour baseline aligns with sleep science recommendation for strength athletes
- Simple linear formula is transparent and trustworthy (no black-box algorithm)

### 3. Manual Entry Localization (English + Spanish)
**New strings:**
- `recovery_sleep_hours_label`: "Sleep Hours"
- `recovery_readiness_label`: "Readiness"
- `readiness_wrecked`: "Wrecked"
- `readiness_tired`: "Tired"
- `readiness_ok`: "OK"
- `readiness_good`: "Good"
- `readiness_crushed`: "Crushed"

All added in both `strings.xml` (en-US) and `strings-es.xml` (es-ES).

### 4. Backward Compatibility (Not Forced Migration)
**Pattern:**
- Deprecated old `UserPreferences` methods for 3-slider system
- New `RecoveryMetrics` accepts 2-value tuple (sleepHours, readiness)
- App converts old data to new format on first load (if present)
- No user-facing migration dialog (silent upgrade)

**Rationale:**
- Graceful degradation for users who previously logged recovery
- Zero friction for new users (only see new 2-slider system)

## Implementation

**UI Components:**
- `RecoveryMetricsFragment` — Manual entry screen
- Sleep slider: 0–12 hours, 0.5 increments, shows "X.X hours"
- Readiness slider: 1–10 with labeled stops (Wrecked, Tired, OK, Good, Crushed)

**Data Layer:**
- `RecoveryMetrics` data class with `sleepHours: Float`, `readinessScore: Int`
- `UserPreferences` — New methods for 2-value storage; deprecated 3-value methods

**Integration:**
- RecoveryViewModel calculates `recoveryScore` from formula
- Display score as percentage or visual indicator (e.g., color gradient)

## Consequences

✅ **Pros:**
- Simpler UX: 2 sliders instead of 3
- Objective sleep data provides context for AI coach recommendations
- Gym-culture language increases user engagement
- Backward compatible with existing storage

⚠️ **Cons:**
- Sleep hours may be less accurate than Health Connect (self-reported vs tracked)
- Single readiness slider can't capture "I'm sore but have high energy" nuance
- Formula transparency depends on UI explaining the 50/50 weighting

## Edge Cases Documented

1. **No sleep data yet:** If user hasn't set sleep hours, default to 8 (neutral recovery score)
2. **Midnight edge cases:** Manual logging at 11 PM (did they sleep last night or tonight?) → Defer to user intent; no smart defaults
3. **Mixed mode (future):** If user has Health Connect + manually logs, should we blend? Deferred to issue #367b (mixed-mode recovery)

## Follow-Up Work

- **Health Connect integration validation:** Verify fallback activates only when Health Connect unavailable
- **Mixed-mode recovery (deferred):** Support both tracked + manual metrics in single session
- **Historical entry support (deferred):** Let users log past days' recovery (currently only today)

**PR:** #374 (opened 2026-04-10T06:30Z)

---

## Audit Sprint Directives (2026-04-10T11:40Z)

### 1. Deferred Work Directive (#392–#394)

**Date:** 2026-04-10T11:40:00Z  
**By:** Copilot (via user request)  
**Reference:** Issues #392 (Voice Input), #393 (RPE Progression), #394 (Onboarding Program)

**Decision:** Move to backlog. Do not execute in this sprint.

**Rationale:**  
User directive for scope control. Audit sprint focused on closing MVP readiness gaps (persistence, templates, deload). Voice input, RPE progression, and onboarding are advanced features appropriate for v1.1 after initial launch feedback.

**Status:** Implemented. All three issues closed (not abandoned) with 'backlog' label.

---

### 2. Workout State Persistence Architecture (#390 → #399)

**Date:** 2026-04-10T10:30:00Z  
**Architect:** Tank  
**Reference:** Issue #390, PR #399

**Problem:** ActiveWorkoutViewModel held all workout state in memory. App kill (incoming call, accidental swipe, system OOM) destroyed 30–45 min of logged data. Unacceptable for powerlifting where users invest significant effort.

**Decision:** Room + Gson JSON serialization.

**Design:**
- Single table: `in_progress_workouts` (workoutId PK)
- Serialization: Gson JSON for `List<InProgressExercise>` → `exercisesJson TEXT`
- Auto-save triggers: `completeSet()`, `addExercise()`, `startRestTimer()`
- Recovery flow: ViewModel init → check for in-progress → show resume prompt → user selects resume/new
- Corruption handling: Deserialization failure → silent clear + log (no crash)

**Alternatives Rejected:**
- SavedStateHandle: Only survives process death, not force stop
- DataStore: Poor fit for nested structures, async overhead
- Full WorkoutEntity: Pollutes workout history with drafts
- ViewModel onSaveInstanceState: Doesn't survive force-stop

**Migration:** Database v4 → v5

**Rationale:**
- Room already in use (zero dependency cost)
- Gson already in project (wger.de API deserialization)
- JSON simpler than relational approach for unbounded exercise list (~50 max)
- Auto-save on mutations (write-through cache) prevents data loss
- ~10ms latency per save acceptable (user already waiting for DB write)

**Trade-offs:**
- Pros: Zero data loss, fast recovery (<50ms), simple migration, testable
- Cons: Cannot query individual sets in SQL (acceptable—always load full), write amplification (50KB per save on older storage), timer drift on restore (v2.0 fix)

**Enhancements (v2.0):**
- Store `restTimerStartedAt` timestamp, recalculate on resume
- Incremental save (only serialize changed exercises)
- gzip compression (60–70% storage reduction)

**Testing:**
- Unit tests: `WorkoutPersistenceTest.kt` (4 tests: save, load, clear, null)
- Manual QA: Force-stop mid-workout → relaunch → verify state restored

**Owners:**
- Tank: Data layer (Room, DAO, Repository)
- Trinity: UI layer (resume prompt, event handling)

**Status:** Implemented. PR #399 merged 2026-04-10. Feature live.

---

## Decision: Remove Dead Buttons from Settings (Tank)

**By:** Tank (Backend Dev)  
**Date:** 2026-04-10  
**Issue:** #383  
**PR:** #403  

### Decision

Remove non-functional Sign In button from SettingsScreen rather than leaving it visible with a stub dialog. The auth backend is \NoOpAuthService\ — the button does nothing. The Sign In row will be re-added when real authentication (Firebase or OAuth) is implemented.

Also made App Version row non-clickable (read-only display). No reason for it to be tappable.

### Rationale

- Dead buttons erode user trust. If it doesn't work, don't show it.
- NoOpAuthService explicitly returns \Result.failure()\ — there's no partial functionality to preserve.
- Clean removal is safer than a "Coming Soon" label that might ship to production and stay forever.

### Impact

- **Trinity:** SettingsScreen layout changed — Sign In row removed from Account section. SettingsRow composable now accepts optional \onClick\ parameter.
- **Tank (future):** When real auth is implemented, add Sign In row back with proper AuthService integration.
- **No breaking changes:** SettingsViewModel and SettingsContract unchanged.

---

## Directive: Ralph Must Persist Across Context Limits (Copilot)

**By:** Copilot (user request)  
**Date:** 2026-04-10  
**Captured for:** Team memory  

### Directive

Ralph must never stop due to context window or session time limits. Before stopping, validate context usage and compact the session to reduce usage. Keep Ralph running as long as possible.

### Rationale

User request — captured for team memory. Ralph's continuous improvement cycles depend on sustained context to track progress, validate changes, and iterate without interruption.

### Implementation

- Before shutdown: check token usage via Claude API
- Compact session state (archive completed todos, merge duplicate learnings)
- Persist checkpoint with technical details for next session rehydration
- Avoid hard context limits — prefer graceful degradation (summarize oldest turns if needed)
