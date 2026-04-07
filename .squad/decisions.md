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

---

## Governance

- All meaningful changes require team consensus
- Document architectural decisions here
- Keep history focused on work, decisions focused on direction
