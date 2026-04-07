# Decisions Log

Team decisions, policy directives, and architectural guidance.

---

## 2026-04-07T14-05-fitbod-teardown: FitBod Competitive Teardown

**Decision by:** Morpheus (Lead/Architect)  
**Date:** 2025-07-18  
**Context:** User is an active FitBod subscriber. This teardown informs GymBro's roadmap priorities and answers the question: *What will it take to cancel FitBod?*

### What FitBod Does Better
1. **Data Moat** — 400M+ logged data points informing recommendations
2. **Exercise Library** — 1,000+ HD video exercises vs. GymBro's 200+ text-based
3. **Auto Workout Generation** — One-tap full session creation (GymBro lacks this)
4. **Muscle Recovery Heat Map** — Intuitive visual UI (GymBro has data, not visualization)
5. **Cross-Platform** — iOS + Android (GymBro iOS-only)
6. **Equipment Profiles** — Multi-profile switching (GymBro single profile)
7. **UX Polish** — 9 years of iteration vs. weeks for GymBro

### What GymBro Does Better
1. **Conversational AI Coach** — Explainable, context-aware (FitBod has no LLM)
2. **True Periodization** — Mesocycle architecture vs. session-by-session
3. **Proactive Plateau Detection** — Change point analysis, trend forecasting
4. **Overtraining & Imbalance Alerts** — Systematic warnings (FitBod doesn't)
5. **Advanced Recovery Intelligence** — HealthKit biometrics vs. volume-based fatigue
6. **Supersets & Rest-Pause Sets** — First-class support
7. **Dynamic Island & Live Activities** — Active workout widgets
8. **Offline-First + CloudKit Sync** — Property-level conflict resolution
9. **Privacy-First AI** — On-device inference option
10. **Open Exercise Library** — wger.de API integration

### Top 5 Strategic Recommendations

| Rank | Feature | Why | Effort |
|------|---------|-----|--------|
| #1 | **Auto Workout Generation** | Closes biggest gap (P0) | Medium |
| #2 | **Exercise Video Content** | Closes perceived quality gap (P1) | Medium |
| #3 | **Custom Exercise Creation** | Power user need (P1) | Low |
| #4 | **Muscle Recovery Visualization** | We have data, need UI (P2) | Low |
| #5 | **Equipment Profile Switching** | QoL improvement (P2) | Low |

### What NOT to Do
- ❌ Build 400M-data-point engine (time/scale mismatch)
- ❌ Chase Android (not yet; iOS-first wins)
- ❌ Add social features (unnecessary)
- ❌ Compete on library size (quality > quantity)

### Where to Double Down
- 🔥 **AI Coach as primary interface** — Our moat, not FitBod's
- 🔥 **Recovery intelligence** — Biometric depth FitBod can't match
- 🔥 **Transparency** — Explained AI > black box
- 🔥 **Free tier generosity** — Trust before money
- 🔥 **Program fidelity** — Serious lifters need real periodization

### Strategic Reality
GymBro is a **specialist for serious lifters**, not a generalist. We don't need to beat FitBod at everything — only at program fidelity, transparent AI coaching, recovery intelligence, and logging speed. **We're already ahead or architecturally positioned to win on those axes.**

---

## 2026-04-07T15-06-17Z: User Directive — Repo Hygiene

**By:** Copilot (via user request)

**What:** Keep the repo clean going forward. Agents must clean up after themselves — delete local branches after PR is merged, don't accumulate stale branches. Repo hygiene is a team responsibility, not an afterthought.

**Why:** Session produced 23 stale branches that needed manual cleanup. Prevention > cleanup.

---

## 2026-04-08: PR Review Batch — Morpheus (Lead/Architect)

**Reviewer:** Morpheus  
**Date:** 2026-04-08  
**Scope:** 9 draft PRs from Phase 3 work wave

### Verdicts

#### ✅ PR #88 — Fix buildContext() (Neo, Issue #82) — APPROVED

**Critical fix.** Three-layer pipeline (ViewModel → Snapshot → PromptBuilder) is clean architecture. 7 unit tests cover all fetch paths and edge cases. SwiftData predicates used correctly. Graceful degradation on empty data. This unblocks the entire AI coach feature.

#### ⚠️ PR #89 — Onboarding Flow (Trinity, Issue #87) — APPROVED WITH NOTES

**Solid progressive disclosure UX.** 7-step flow, reduceMotion gates, `.foregroundStyle()` throughout, design system tokens everywhere. EmptyStateView is a good reusable component.
- **Note 1:** Two hardcoded `.font(.system(size: 64/80))` on hero icons without `@ScaledMetric`. Minor — icon sizing, not body text — but should be wrapped for consistency. **Trinity** should fix in follow-up.
- **Note 2:** No unit tests for onboarding flow. Acceptable for MVP UI code, but model extensions (EquipmentType, goals) should have tests. **Switch** should add in next coverage sweep.

#### ✅ PR #90 — Smart Defaults Overhaul (Neo, Issue #84) — APPROVED

**Excellent heuristics-first design.** 6-factor prediction (fatigue, RPE, recovery, trends, deload, experience scaling) with 20 comprehensive tests (527 lines). `os.Logger` used correctly. Conservative rounding (floor, not ceil) is the right call for safety. Graceful degradation when optional data is missing. This is core product differentiation.

#### ⚠️ PR #91 — Overtraining Detection (Neo, Issue #83) — APPROVED WITH NOTES

**Strong evidence-based services.** 50+ tests, cited research, conservative thresholds. Multi-signal detection approach (3+ signals = high risk) is sound.
- **Note:** `OvertrainingDetectionService` and `MuscleImbalanceService` are stateless final classes but not marked `Sendable`. Their DTOs are correctly `Sendable`. Service classes should conform too since they hold no mutable state. **Tank** should add conformance in follow-up.

#### ⚠️ PR #92 — Exercise Library Expansion (Neo/Tank, Issue #80) — APPROVED WITH NOTES

**Quality exercise data with validation tests.** Seed data expanded, instruction quality tests added.
- **Note:** PR diff appears to include SmartDefaultsService changes overlapping with PR #90. Verify branch is clean before merge — check for contamination. **Tank** should rebase against dev after PR #90 merges.

#### ✅ PR #93 — Program Templates (Neo, Issue #81) — APPROVED

**Clean data model extension.** ProgramWeek as first-class entity enables block periodization. 6 evidence-based program templates (5/3/1, PPL, GZCL, Starting Strength, Upper/Lower, Full Body). ProgramComplianceService is a struct (good — value-type, inherently Sendable). ProgramSeeder follows established ExerciseDataSeeder pattern. 21 unit tests. `os.Logger` used. Backward-compatible model changes.

#### ✅ PR #94 — wger.de API Integration (Tank, Issue #77) — APPROVED

**Exemplary architecture.** Actor-based services (WgerAPIService, ExerciseSyncService) — inherently thread-safe. Offline-first design. 24-hour sync throttle. Rate limit handling. MockURLProtocol testing (15 tests). ExerciseSource enum cleanly extends Exercise model. HTML stripping. Deduplication by name similarity. This is how you build an API integration.

#### ⚠️ PR #95 — Exercise Instruction Views (Trinity, Issue #79) — APPROVED WITH NOTES

**Beautiful UI work.** Proper `.foregroundStyle()`, `@ScaledMetric` (5 instances), `reduceMotion` gate, accessibility labels, FlowLayout custom layout, design system tokens throughout. Exactly what "world-class UX" looks like.
- **Note:** No unit tests for view logic (instruction parsing, section type detection). These are pure functions inside the views — extract and test. **Switch** should add in coverage sweep.

#### ✅ PR #96 — Muscle Recovery Tracking (Neo, Issue #86) — APPROVED

**Best convention adherence of all 9 PRs.** All three services explicitly marked `Sendable`. All DTOs `Sendable`. `os.Logger` throughout. 50+ tests across 3 test suites. Evidence-based recovery windows. ReadinessProgramIntegration bridges recovery → program recommendations cleanly. User always has final say (recommendations, not mandates).

### Summary Table

| PR | Verdict | Author | Tests | Conventions |
|----|---------|--------|-------|-------------|
| #88 | ✅ Approved | Neo | 7 tests | Clean |
| #89 | ⚠️ Notes | Trinity | No new tests | 2 minor nits |
| #90 | ✅ Approved | Neo | 20 tests | Clean |
| #91 | ⚠️ Notes | Neo | 50+ tests | Missing Sendable on services |
| #92 | ⚠️ Notes | Neo/Tank | Tests present | Possible branch contamination |
| #93 | ✅ Approved | Neo | 21 tests | Clean |
| #94 | ✅ Approved | Tank | 15 tests | Exemplary |
| #95 | ⚠️ Notes | Trinity | No tests | Clean UI conventions |
| #96 | ✅ Approved | Neo | 50+ tests | Best of batch |

**No rejections.** All PRs demonstrate competent architecture, correct patterns, and appropriate test coverage for MVP. The notes are follow-up items, not blockers.

### Follow-up Issues to File

1. **@ScaledMetric on onboarding hero icons** (Trinity, low priority)
2. **Sendable conformance on OvertrainingDetectionService + MuscleImbalanceService** (Tank, medium)
3. **Unit tests for onboarding model extensions** (Switch, medium)
4. **Unit tests for instruction parsing logic** (Switch, low)
5. **Verify PR #92 branch cleanliness before merge** (Tank, pre-merge)
