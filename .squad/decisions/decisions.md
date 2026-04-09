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

## 2026-04-08T09-00Z: Android Workstream Prioritization — 3 Parallel Tracks

**Decision by:** Morpheus  
**Date:** 2026-04-08  
**Context:** Android codebase has reached feature completeness (12 screens, all core features). Board is clear. Time to lock down quality before expanding feature scope.

### THE DECISION: Prioritized Sequential Execution

**Priority Order:**

1. **FIRST:** Maestro E2E Validation + CI Hardening (Workstream A) — **BLOCKING**
2. **SECOND:** Unit Test Coverage to 60% (Workstream B) — **AFTER Maestro passes**
3. **LAST:** Android Skills Gap (Workstream C) — **Continuous, start after test coverage hits 40%**

### Key Workstreams

**Workstream A: Maestro E2E Validation + CI Hardening**
- Objective: Validate all 24 Maestro flows pass after testTagsAsResourceId fix. Harden CI to catch UX regressions.
- Timeline: 2-3 hours (Tank execution) + 1 week monitoring
- Success: All 24 flows pass locally, smoke suite runs on every PR, <5% flake rate

**Workstream B: Unit Test Coverage to 60%**
- Objective: Raise coverage from ~9% to 60% for beta readiness. Tier 1 (40%): ViewModels + core Repositories. Tier 2 (60%): DAOs + data sources.
- Timeline: 2 weeks total (1 week per tier)
- Success: 40% coverage, then 60% coverage with all 12 ViewModels tested
- Approach: Use Maestro flows as acceptance criteria for unit tests

**Workstream C: Android Skills (3 Critical)**
- Objective: Add high-leverage skills that eliminate recurring agent mistakes. Quality over quantity.
- Recommended skills: (1) android-coroutines-expert (CRITICAL), (2) compose-performance (HIGH), (3) android-navigation-compose (MEDIUM)
- Timeline: 1 day per skill (start after 40% coverage)
- Success: 3 skills with 5+ code examples, anti-patterns documented, <500 lines each

### Why This Order (Sequential, Not Parallel)

- **E2E First = Regression Safety Net:** Maestro catches UX breaks that unit tests miss. Must validate before investing in test coverage.
- **Unit Tests Second = Code Safety Net:** With E2E passing, acceptance criteria clear. Tests prevent logic bugs and ensure flows stay working.
- **Skills Last = Quality Multiplier:** Test patterns reveal what agents struggle with. Skills codify those lessons, improving agent output.

### Risk Mitigation

- **Risk 1:** Maestro flows fail catastrophically → Triage by category (selectors, timing, data), fix in batches
- **Risk 2:** Test coverage takes too long → Focus on high-value tests (ViewModels), defer DAOs to Tier 2
- **Risk 3:** Skills don't improve agent output → Validate with before/after test, delete low-signal skills

### Dependencies & Timeline

1. **Maestro E2E Validation (Blocking)** → 3 hours + 1 week
2. **Unit Test Coverage: Tier 1 (40%)** → 1 week (after Maestro passes)
3. **Android Skills + Unit Test Coverage: Tier 2 (60%)** → Parallel after 40% coverage
4. **Total:** 3 weeks to completion

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

---

## 2026-04-07T15:29Z: User directive — UX Philosophy

**By:** Copilot (via user request)

**What:** La UI/UX tiene que ser ultra-clean, ágil, fresca. Año 2030, no 2020. Mínimas pulsaciones, mínimos menús. Super agradable de usar. Si algo requiere muchos taps o se siente pesado, está mal. Priorizar simplicidad radical sobre feature density.

**Why:** El usuario sospecha que las features construidas hoy tienen demasiada complejidad visual — muchos menús, muchas opciones. La app debe sentirse como si no tuviera UI — solo flujo.

---

## 2026-04-07T15:35Z: User directive — Android migration planned

**By:** Copilot (via user request)

**What:** After finishing the current iOS audit fixes, plan to migrate/port GymBro to Android (Kotlin + Jetpack Compose). Repo structure: GymBro-ios/ and GymBro-Android/ folders. Skills should be split per platform. Motivation: user has no Mac, can't test iOS app. Android is easier to develop/test on Windows with Android Studio.

**Why:** Practical necessity — user cannot currently run or test the iOS app. Android enables local development, emulation, and testing on Windows.

---

## 2026-04-07T15:40Z: Android migration resources

**By:** Copilot (via user input)

**What:** Reference repos for Android migration:
- Skills: https://github.com/new-silvermoon/awesome-android-agent-skills
- Jetpack Compose skills: https://github.com/anhvt52/jetpack-compose-skills
- Patterns/reference apps: https://github.com/androiddevnotes/awesome-jetpack-compose-android-apps

**Why:** When we start Android port, analyze these for useful skills, patterns, and best practices to integrate into the team's knowledge base.

---

## 2026-04-09: Maestro Cross-Platform Quirks and Test Infrastructure Standards

**Decision by:** Switch (Tester)  
**Date:** 2026-04-09  
**Context:** Issue #311 — Critical regression discovered in Maestro E2E suite after PR #310 (5/23 passing, 18 failures)

### Problem

PR #310 introduced two types of failures affecting 18/23 Maestro flows (78% failure rate):

1. **JavaScript syntax errors** — Using bash/shell `${VAR:=default}` syntax in Maestro's JavaScript evaluator
2. **UTF-8 encoding issues** — Windows Maestro misreads multi-byte UTF-8 characters (e.g., "¡" becomes "í")

These failures were not caught before merge because there was no pre-merge Maestro validation requirement.

### Decision

#### 1. Maestro JavaScript Syntax Standard

**Rule:** All Maestro `${}` expressions MUST use JavaScript syntax, not bash/shell syntax.

**❌ NEVER:**
```yaml
- inputText: "${USER_NAME:=TestUser}"  # bash/shell syntax — BREAKS
```

**✅ ALWAYS:**
```yaml
- inputText: "${USER_NAME || 'TestUser'}"  # JavaScript syntax — WORKS
```

**Why:** Maestro uses Graal.js for `${}` evaluation. The `:=` operator is bash-specific and causes JavaScript parse errors.

#### 2. UTF-8 Text Selector Avoidance on Windows

**Rule:** Avoid non-ASCII text selectors in Maestro flows intended for cross-platform execution.

**❌ AVOID (Windows Maestro):**
```yaml
- tapOn: "¡Vamos!"  # UTF-8 multi-byte char — misread as "íVamos!" on Windows
```

**✅ PREFER (Cross-platform):**
```yaml
- tapOn:
    id: "onboarding_start_button"  # ID selector — works everywhere
```

**✅ ACCEPTABLE (Fallback):**
```yaml
- tapOn: "[¡í]Vamos!"  # Regex handles both encodings
```

**Why:** Maestro 2.4.0 on Windows has UTF-8 multi-byte character handling issues. Files are correctly encoded (UTF-8), but the runtime misreads characters like "¡" (C2 A1) as "í" (C3 AD). This affects Spanish text and likely all accented characters.

**Impact:** Affects 16 flows that use `flow/ensure-post-onboarding.yaml` helper.

#### 3. Pre-Merge Maestro Validation Requirement

**Rule:** All PRs that modify Maestro flows or shared helpers MUST include Maestro validation results.

**Required:**
- Run at least **smoke test suite** (5 core flows) before PR approval
- Run **full suite** (all 23 flows) for PRs touching shared helpers like `flow/ensure-post-onboarding.yaml`
- Post pass/fail results in PR description or as a comment

**Why:** PR #310 was merged with 18 new failures. A single pre-merge smoke test run would have caught both issues.

#### 4. Helper Flow Change Protocol

**Rule:** Shared helper flows (e.g., `flow/ensure-post-onboarding.yaml`) require full suite validation before merge.

**Process:**
1. Make changes to helper flow
2. Run **all flows** that reference the helper (use `grep -r "runFlow.*helper-name" android/.maestro/`)
3. Document results: X/Y flows passing
4. If any failures introduced, investigate root cause before merge
5. Post full validation results to PR

**Why:** Helper flows have wide blast radius. A single bug in `ensure-post-onboarding.yaml` broke 16 flows (70% of suite).

#### 5. Maestro Test Data Standards

**Rule:** Update `test-data.env` to reflect actual JavaScript syntax, not bash/shell syntax.

**Current (WRONG):**
```bash
# Notes:
# - All flows have sensible defaults embedded using ${VAR:=default} syntax
```

**Fixed (CORRECT):**
```bash
# Notes:
# - All flows have sensible defaults embedded using ${VAR || "default"} syntax (JavaScript)
# - Maestro uses Graal.js for ${} evaluation, NOT bash/shell
# - Avoid non-ASCII text selectors (e.g., "¡Vamos!") — use IDs or ASCII text instead
```

### Implementation

**Immediate (Issue #311 blockers):**
1. Fix JavaScript syntax in 2 flows: `onboarding-flow.yaml`, `full-e2e.yaml`
2. Fix UTF-8 encoding in `flow/ensure-post-onboarding.yaml` (use ID selector for "¡Vamos!" button)
3. Re-run full suite validation
4. Update `test-data.env` documentation

**Short-term (CI/CD integration):**
1. Add Maestro smoke tests to GitHub Actions (5 core flows)
2. Gate PR merge on smoke test pass
3. Document Maestro quirks in `android/.maestro/README.md`

**Long-term (quality gate):**
1. Consider full Maestro suite in nightly CI runs
2. Add Maestro YAML linting to pre-commit hooks (detect unsupported syntax)

### Lessons Learned

1. **Helper flow bugs have wide blast radius** — 1 bug in ensure-post-onboarding.yaml broke 16 flows (70% of suite)
2. **Windows Maestro has UTF-8 issues** — Multi-byte UTF-8 chars misread; prefer ASCII or ID selectors
3. **Maestro uses JavaScript, not bash** — `${VAR:=default}` is invalid; use `${VAR || "default"}`
4. **Pre-merge validation prevents regressions** — PR #310 would have been caught by a single smoke test run
5. **Individual flow execution isolates root causes** — Batch mode hides the distinction between error types

### References

- Issue #311: Maestro E2E suite validation (5/23 pass, regression)
- PR #310: Fix that introduced regressions (approved without Maestro validation)
- Maestro docs: https://maestro.mobile.dev/reference/javascript-expressions
- Validation report: Posted to #311 on 2026-04-09

---

## 2026-04-09: Maestro Flow Definition Standards

**Author:** Trinity (iOS/Mobile Dev)  
**Context:** PR #309 — fixed 15 flow failures from 5 root causes.

### Decisions

#### 1. Always escape regex special chars in Maestro assertions
Parentheses `()` in `tapOn` / `assertVisible` text are parsed as regex groups. Use `\(` and `\)` for literals. Example: `"Kilogramos \(kg\)"`.

#### 2. All Maestro text assertions must be bilingual
Since the emulator runs es-ES, every text assertion should use `"Texto Español|English Text"` pattern. Source Spanish strings from `values-es/strings.xml`, not from guessing.

#### 3. Prefer ID-based selectors over text-based
Use `id:` selectors when testTag IDs exist. They're locale-independent and more stable. Text selectors should be the fallback.

#### 4. No `${VAR:=default}` in tapOn/assertTrue selectors
This syntax crashes Maestro's JS evaluator. Hardcode defaults in selectors. Keep `${VAR:=default}` only in `inputText:` where it works fine.

#### 5. Maestro API reference
- `eraseText` (not `clearTextField`)
- Plain `scroll` for scrolling down; `swipe: direction: DOWN/UP` for directional control
- `optional: true` must be nested inside the command's map form, not used with shorthand syntax
