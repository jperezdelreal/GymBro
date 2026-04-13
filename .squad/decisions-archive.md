# Decision Archive

**Archived:** 2026-04-13  
**Rationale:** decisions.md exceeded 20KB; archive contains decisions older than 30 days (before 2026-03-14) to reduce file size and improve navigation.

---

## Archived Decisions (2025-01-21 to 2026-03-13)

### 2025-01-21: Synchronous Database Seeding in DI Providers
Tank (Backend Dev) — Issue #465

Use synchronous seeding in DI providers with `runBlocking` for DI-critical seed data. Room's async callbacks are too uncertain for data that must be available immediately.

---

### 2025-07-04: Exercise Test Data & Seeding Strategy
Neo (AI/ML) — Issue #390

Database seeding populates 209 standard exercises. Future: consider exercise variants (feet position, bar path, equipment swaps).

---

### 2025-07-18: RPE vs Velocity for Auto-Progression
Neo (AI/ML) — Issue #391

Use RPE as primary progression signal (simpler, more reliable for untrained users than velocity). Velocity tracking kept as secondary metric.

---

### 2025-07-22: Test Coverage Gaps for UX Bugs
Switch (Tester)

4 critical UX bugs identified with test gaps: PlanDayDetail exercise handoff, Exercise Detail placeholder, Start Workout assertions, Onboarding re-show regression. Infrastructure exists; tests need to be uncommented.

---

### 2026-03-10: Voice Logging Architecture — One Mic per Exercise
Trinity (Mobile) & Neo (AI) — Issue #456

Single mic button per exercise (auto-targets first incomplete set). VoiceRecognitionService parses natural language into set, reps, weight. Three-state RECORD_AUDIO permission handling.

Strings must be bilingual (en-US + es-ES). Device locale respected instead of hardcoded locale.

---

### 2026-03-13: RPE-Based Progression Engine Design
Neo (AI/ML) — Issue #393

Progression logic uses heuristic rules (not ML):
- **Progress:** All working sets RPE ≤7 → +2.5kg
- **Regress:** Last 2 sets RPE 10 → −5% (rounded to 2.5kg)
- **Maintain:** RPE 8-9 → same weight

RPE picker is tap-to-cycle widget (6→7→8→9→10→clear) for speed during active workouts.

---

