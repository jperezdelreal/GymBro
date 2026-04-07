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

### Recommendation

Do NOT ship MVP without fixing #26 (concurrency) and the PersonalRecordService crash in #30. Test coverage (#25) should reach 60%+ before beta.
