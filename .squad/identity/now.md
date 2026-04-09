---
updated_at: 2026-04-09T18:15:00.000Z
focus_area: Test coverage & CI infrastructure
active_issues: [313, 320, 314, 315, 312, 316]
status: testing_phase
---

# What We're Focused On

Post-MVP: increasing test coverage and CI infrastructure. Unit tests + Maestro E2E.

## Ralph Priority Order

Process issues in this order. Dependencies noted — respect them.

| Priority | Issue | Title | Agent | Status | Depends On |
|----------|-------|-------|-------|--------|------------|
| 1 | #313 | Unit tests Tier 1 — ViewModels & core repos (40% coverage) | Switch | Exploration DONE, implementation pending | — |
| 2 | #320 | Bilingual regex for all Maestro flows (es-ES locale) | Switch | Not started | — |
| 3 | #314 | CI: Validate Maestro E2E in GitHub Actions | Switch | Not started | — |
| 4 | #315 | Unit tests Tier 2 — DAOs, data sources (60% coverage) | Switch | Not started | #313 |
| 5 | #312 | Create 3 Android skills — coroutines, compose-perf, navigation | Neo, Switch | Not started | — (can parallel) |
| 6 | #316 | Visual regression testing with Paparazzi | Switch | Needs research | — |

## Key Context for #313
- Switch's `history.md` contains complete codebase map: 7 ViewModels without tests, 7 fakes needed, test infrastructure documented
- Branch `squad/313-unit-tests-tier1` exists with exploration commit
- Start with OnboardingViewModel (simplest, 1 dep), then HistoryListViewModel, then rest
- Pattern to follow: existing ActiveWorkoutViewModelTest

## Notes
- User may add more issues before activating Ralph — check this file for updates
- Emulator runs es-ES locale — Maestro flows need bilingual regex
- AI Coach: Vertex AI API activated in Firebase console — verify on next build
- JAVA_HOME must be set before Gradle: `$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'`
- Git auth: use `gh auth switch --user jperezdelreal` before PR creation
