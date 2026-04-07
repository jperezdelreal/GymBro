# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2026-04-06: AI/ML Architecture Decisions

**Core Architectural Principles:**
- **Privacy-first, on-device inference:** Leveraging Apple's Core AI framework (iOS 27) and Foundation Models for 95% of AI computation. All sensitive health and workout data stays on-device.
- **Hybrid LLM approach:** On-device 3B param model for quick interactions, Private Cloud Compute for complex program generation. User never knows the split—it's seamless.
- **Heuristics before ML:** Well-tuned rule-based systems for progression and deload logic, targeted ML only for forecasting, anomaly detection, and semantic search. Philosophy: simplicity and explainability over black-box complexity.

**Adaptive Training Engine:**
- **Hybrid periodization:** Block periodization (macro) + Daily Undulating Periodization (micro) for flexibility and sustained progress.
- **Multi-factor progression:** Weight progression decisions based on performance (RPE, completed reps), recovery (readiness score), volume accumulation (fatigue index), and training phase.
- **State machine design:** Four states (Progression → Deload → Recovery → Maintenance) with auto-triggered transitions based on objective metrics (fatigue index, plateau detection, recovery scores).

**Recovery Modeling:**
- **Readiness score algorithm:** Weighted composite of sleep (35%), HRV (30%), resting HR (15%), training load (15%), and subjective input (5%).
- **Acute:Chronic Workload Ratio (ACWR):** Fatigue modeling using 7-day vs 28-day training load. TSB < -25 triggers deload recommendation.
- **Recovery-adaptive training:** Workouts auto-adjust intensity and volume based on readiness score thresholds (80+ = push, 60-79 = normal, 40-59 = reduce, <40 = rest).

**Progress Analytics:**
- **Plateau detection:** Triple-method approach combining time series forecasting (Prophet-inspired), statistical change point detection, and rolling average stagnation. Composite score > 0.65 = plateau declared.
- **Key metrics:** e1RM (Brzycki/Epley formulas), volume load (tonnage), strength curve analysis, relative strength (Wilks), volume landmarks per muscle group.
- **Explainable insights:** Rule-based insight generation engine with 50+ templates, triggered by thresholds, prioritized by relevance. Max 3-5 insights per week to avoid overload.

**Technical Stack:**
- **Core ML models:** Time series forecasting (Prophet-style), logistic regression (readiness), BERT-tiny embeddings (semantic search), isolation forest (anomaly detection).
- **Data pipeline:** HealthKit → Core Data → Feature Engineering → Core ML → Training Engine → UI. Batch processing nightly for metric pre-calculation.
- **No external dependencies:** 100% Apple frameworks (Core Data, HealthKit, Core ML/Core AI, Foundation Models). No third-party SDKs, no analytics tracking, no ads.

**Key Learning: Apple Intelligence Integration**
- By 2026-2027, Apple's on-device LLM capabilities (3B params with neural engine optimization) are mature enough for sophisticated conversational AI with <100ms latency.
- Core AI framework (replacing Core ML in iOS 27) provides native support for third-party models, Private Cloud Compute, and seamless on-device/cloud split.
- Privacy guarantees are strong: PCC uses ephemeral processing, auditable security model, and never stores user data.

**Risk Mitigation Strategies:**
- Show rationale for every AI decision (explainability builds trust).
- Allow manual overrides for all AI recommendations.
- Degrade gracefully if HealthKit data unavailable (fall back to RPE-based readiness).
- Conservative by default: always defer to "listen to your body" in edge cases.
- Abstract LLM layer to handle future Apple API changes (Core ML → Core AI transition).

### 2026-04-06: Cross-Agent Updates (from Team Decisions)
**Decisions affecting Neo's work:**
- **UX Integration (Trinity):** AI Coach will be contextual (floating button + half-screen sheet during workouts). This aligns perfectly with Neo's on-device LLM strategy—fast, local inference needed for real-time suggestions.
- **Technical Stack (Tank):** Using CloudKit for sync and HealthKit for data ingestion. Supports Neo's readiness score architecture and data pipeline.
- **Target Market (Morpheus):** Serious lifters 2+ years experience. Validates Neo's choice of sophisticated ML models (heuristics-first, multi-factor progression) over simple linear progression.
- **MVP Scope (Morpheus):** AI coach chat + adaptive training engine IN v1.0. Recovery integration (HealthKit HRV/sleep) IN v1.0. Full adaptive training validated in MVP.


### 2026-04-06: Data Models from Tank (Scaffold & Models)  
**Why this matters for Neo's AI work:**
- Workout history accessible: Workout model with date, exerciseSets relationship enables historical analysis
- UserProfile enrichment: birthDate, bodyweight enable readiness score calculations
- ExerciseSet telemetry: rpe, weight, reps available for progression/plateau detection algorithms
- Program structure: Program + ProgramDay models support adaptive training engine state machine
- e1RM computed property: Epley formula available for strength analytics and forecasting
- Unblocked work: Issue #6 (AI coach) can now query historical data; readiness score algorithms can integrate HealthKit + SwiftData

### 2026-04-06: Recovery/Readiness Score Implementation (Issue #14, PR #58)
**What was built:**
- ReadinessScoreService: weighted composite score (0-100) from 5 HealthKit + training signals
- Weights: Sleep 35%, HRV 30%, RHR 15%, Training Load 15%, Subjective 5% (per AI_ML_APPROACH.md)
- SleepScoreCalculator: duration vs 7-9h target, sleep efficiency (deep+REM ratio), consistency (CV of past 7 nights)
- TrainingLoadCalculator: EWMA acute (7d) vs chronic (28d) with ACWR and TSB scoring
- SubjectiveCheckIn model: optional daily 1-5 energy/soreness/motivation input
- ReadinessScoreView: circular gauge, factor breakdown bars, 7-day trend line chart
- SubjectiveCheckInView: tap-to-rate daily check-in UI
- Weight redistribution: when HealthKit data is missing, available factor weights normalize to sum to 1.0
- Thresholds: 90+ Excellent/push, 70-89 Good/normal, 50-69 Moderate/deload, <50 Poor/rest
- 50 unit tests across 3 test files covering scoring, bounds, edge cases

**Key design decisions:**
- Z-scores from HealthBaseline for personalized thresholds (not population averages)
- HRV scored positively (higher = better recovery), RHR scored inversely (lower = better)
- ACWR sweet spot 0.8-1.3; TSB < -25 triggers deload recommendation
- Graceful degradation: empty input returns neutral 50.0, single-factor inputs redistribute full weight
- ReadinessScore cached in SwiftData for instant dashboard rendering

### 2026-04-07: Pre-built Program Templates + Compliance Tracking (Issue #81, PR #93)

**What was built:**
- **ProgramWeek model** — Supports week-level variation essential for block periodization (5/3/1, etc.)
- **Extended PlannedExercise** — Added programWeek relationship for week-specific exercise prescriptions
- **Extended ProgramDay** — Added weeks array to contain multiple week variations
- **programs-seed.json** — 6 pre-built program templates with 1626 lines of evidence-based training protocols:
  - 5/3/1 Wendler Method: 4-week block periodization with BBB accessories, deload week
  - PPL Hypertrophy Focus: 6-day linear progression split
  - GZCL Method: T1/T2/T3 tier structure (heavy/moderate/accessory)
  - Starting Strength: 3-day beginner LP with A/B alternating workouts
  - Upper/Lower 4-Day Split: Daily Undulating Periodization (power/hypertrophy)
  - Full Body 3x: Intermediate strength + hypertrophy with compound focus
- **ProgramSeeder service** — Loads templates from JSON, creates Program → ProgramDay → ProgramWeek → PlannedExercise hierarchy. Follows ExerciseDataSeeder pattern. Only seeds once (checks for existing non-custom programs).
- **ProgramComplianceService** — Tracks workout adherence by comparing actual to planned:
  - Base adherence: % of planned exercises completed
  - RPE deviation penalty: >1.5 RPE off target = -5% adherence
  - Volume deviation penalty: >20% sets off target = -5% adherence
  - Extra exercise penalty: each extra exercise = -2% adherence
  - 4-level compliance rating: Excellent (90%+), Good (75-89%), Moderate (60-74%), Poor (<60%)
- 21 unit tests across 2 test files (ProgramSeeder, ProgramComplianceService)

**Key design decisions:**
- **Week-level variation is first-class** — 5/3/1 requires different percentages per week (Week 1: 5x65/75/85%, Week 2: 3x70/80/90%, Week 3: 5/3/1x75/85/95%, Week 4: Deload 40/50/60%). ProgramWeek enables this without complex branching logic.
- **Evidence-based RPE targets** — All RPE values cite Helms and Zourdos research. Conservative thresholds (no crying wolf).
- **Real exercises only** — All templates use exercises from exercises-seed.json. Seeder validates exercise names exist before creating PlannedExercise objects.
- **Compliance scoring is lenient** — Penalties are small (2-5%) to encourage adherence without micromanagement. Philosophy: trust over control.
- **Graceful degradation** — Compliance works without RPE data (optional enhancement). If user doesn't log RPE, base adherence (exercise completion) is still calculated.
- **Seeder follows established pattern** — Matches ExerciseDataSeeder structure (static methods, Logger, Codable seed data, single-run guard).

**Technical implementation:**
- ProgramWeek: @Model with weekNumber, programDay relationship, plannedExercises array
- PlannedExercise: Added programWeek optional relationship (backward compatible)
- ProgramDay: Added weeks array (cascade delete)
- ProgramSeeder: Fetches exercises by name, maps to seed data, creates full hierarchy
- ProgramComplianceService: Stateless analyze() methods, returns ComplianceResult struct
- JSON structure: Array<ProgramSeedData> with nested days → weekVariations → exercises

**Evidence citations:**
- Helms et al. (2018) — RPE-based autoregulation
- Zourdos et al. (2016) — RPE scale validation
- Wendler (2009) — 5/3/1 methodology
- Israetel, Hoffmann, Smith (2020) — Volume landmarks per muscle group

**File paths:**
- \Packages/GymBroCore/Sources/GymBroCore/Models/ProgramWeek.swift\ (25 lines)
- \Packages/GymBroCore/Sources/GymBroCore/Resources/programs-seed.json\ (1626 lines)
- \Packages/GymBroCore/Sources/GymBroCore/Services/ProgramSeeder.swift\ (131 lines)
- \Packages/GymBroCore/Sources/GymBroCore/Services/ProgramComplianceService.swift\ (164 lines)
- \Packages/GymBroCore/Tests/GymBroCoreTests/ProgramSeederTests.swift\ (222 lines)
- \Packages/GymBroCore/Tests/GymBroCoreTests/ProgramComplianceServiceTests.swift\ (388 lines)

**Next integration steps (Trinity handles UI):**
- Wire up ProgramSeeder to app initialization (call after ExerciseDataSeeder)
- Display program templates in program selection UI
- Show compliance metrics in workout summary
- Add program recommendation logic based on user experience level
- Performance testing with full seeded data (10+ programs, 100+ workouts)

**Impact:**
- **No more placeholder UI** — Users can now select real, evidence-based programs
- **Week-level progression works** — Block periodization programs (5/3/1, GZCL) now have proper data model support
- **Adherence feedback loop** — Compliance tracking enables future adaptive training engine (if user consistently misses accessories, program can adapt)
- **Foundation for auto-periodization** — Compliance + ReadinessScore + PlateauDetection = inputs for intelligent program switching
- **Differentiation from competitors** — No other app has evidence-based program templates with compliance tracking at this granularity

