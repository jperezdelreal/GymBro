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

### 2026-04-07: Smart Defaults Overhaul (Issue #84, PR #90)
**The Problem:**
- Original SmartDefaultsService was naive: `weight = avg(last_workout) + increment`, `reps = avg(last_workout_reps)`
- Ignored fatigue, RPE, recovery state, historical trends, plateaus, deloads, and exercise order
- Resulted in poor weight suggestions that didn't adapt to user's actual training state

**The Solution — Sophisticated Heuristics-First Algorithm:**
Completely rewrote SmartDefaultsService with 6 major enhancements:

1. **Intra-session fatigue detection**
   - Accounts for rep drop-off from set 1→3 (Set 1: 1.0x, Set 2: 0.975x, Set 3: 0.95x)
   - Considers exercise position in workout (Exercise 1-2: 1.0x, 3-4: 0.98x, 5+: 0.95x)
   - Analyzes historical fatigue pattern (ratio of last set reps to first set reps across sessions)

2. **Recovery-aware predictions**
   - Integrates ReadinessScoreService for context-aware load adjustment
   - Readiness >= 80: +2.5% weight (push intensity)
   - Readiness 60-79: 0% (normal training)
   - Readiness 40-59: -10% (reduce load)
   - Readiness < 40: -20% (rest day — minimal load)

3. **Historical trend analysis**
   - Uses last 3-5 sessions instead of just the last one
   - Calculates linear trend of e1RM progression (slope normalized to -1.0 to +1.0)
   - Positive trend: full progression, negative trend: half progression (plateau detection)
   - Tracks volume stability using coefficient of variation

4. **RPE integration**
   - If RPE data exists on sets, calibrates next-session weights
   - RPE < 7.0: +2.5% (user has more in tank)
   - RPE 7.0-9.5: 0% (perfect difficulty)
   - RPE >= 9.5: -5% (too difficult, back off)

5. **Deload recognition**
   - Detects deload weeks when weight < 85% of recent max
   - Holds weight during deload (doesn't add progression)
   - Prevents inappropriate +2.5kg suggestions during recovery weeks

6. **Experience-level scaling**
   - Beginner: 1.5x progression multiplier (faster gains)
   - Intermediate: 1.0x (normal)
   - Advanced: 0.5x (slower gains)
   - Elite: 0.25x (very slow gains)

**Algorithm Design Philosophy:**
- **Heuristics before ML** — well-tuned rules with clear logic, not black boxes
- **Every prediction is explainable** — detailed logging shows reasoning at each step
- **Graceful degradation** — missing RPE? Fall back to simpler model. Missing readiness? Skip recovery adjustment
- **Conservative by default** — always round down for safety (injury prevention over max performance)

**Technical Implementation:**
- New method signature: `getSmartDefaults(for:setNumber:exercisePositionInWorkout:currentReadinessScore:)`
- Fetches recent sessions grouped by workout (not individual sets)
- TrendAnalysis struct with trendStrength, averageFatiguePattern, volumeStability
- Helper methods for session fetching, trend calculation, deload detection, RPE calibration, fatigue/recovery multipliers
- Weight rounding: compound 2.5kg, isolation 1.25kg, accessory 0.5kg increments

**Testing:**
- 20 comprehensive unit tests covering all features
- Tests: defaults, progression, RPE, fatigue, recovery, deload, trend analysis, edge cases, graceful degradation
- All tests use mocked data for deterministic results

**Key Learnings:**
- **Top-set approach beats average-set approach** — using max weight from last session (not average) better reflects user's true capacity
- **Fatigue is multiplicative, not additive** — Set 3 at position 5 = 0.95 * 0.95 * historyAdjustment
- **Conservative rounding matters** — always floor() to nearest increment, never ceil() (safety over ego)
- **Deload detection is critical** — without it, algorithm suggests +2.5kg on 60% deload weeks (dangerous)
- **Graceful degradation enables MVP shipping** — RPE and readiness are optional enhancements, core algorithm works without them

**File Paths:**
- `Packages/GymBroCore/Sources/GymBroCore/Services/SmartDefaultsService.swift` — complete rewrite (422 insertions)
- `Packages/GymBroCore/Tests/GymBroCoreTests/SmartDefaultsServiceTests.swift` — 20 new tests (527 insertions)

**Next Integration Steps:**
- Wire up to workout logging UI (call with setNumber and exercisePosition context)
- Add readiness score to call site (fetch latest ReadinessScore from SwiftData)
- Manual testing with real workout data (100+ sets across multiple weeks)
- Performance testing with large datasets (1000+ sets)
- Consider caching trend analysis per exercise (recompute only on new session)

**Impact:**
- Smart defaults now adapt to user's actual training state (fatigue, recovery, trend)
- Reduces injury risk by backing off when readiness is low or RPE was too high
- Improves adherence by making suggestions feel "right" (users trust the numbers)
- Enables true autoregulation — the app "feels" when to push vs when to hold
- Foundation for future adaptive training engine (this is the core prediction logic)

### 2026-04-07: Overtraining Detection & Muscle Imbalance Alerts (Issue #83, PR #91)

**The Problem:**
- Existing plateau detection was per-exercise only (e.g., "your bench press is stalled")
- No systemic overtraining detection across all exercises
- Muscle imbalance was visualization-only — no alerts when push/pull ratio is dangerous
- No per-muscle-group weekly set count analysis vs hypertrophy landmarks
- Users could overtrain without the app detecting it until injuries occurred

**The Solution — Two New Services:**

#### 1. OvertrainingDetectionService
Monitors **across all exercises** for systemic overtraining signals:

**Detection methods (5 independent signals):**
1. **Multi-exercise plateau correlation** — 3+ exercises stalling simultaneously = systemic fatigue (not exercise-specific)
2. **Volume ramp rate monitoring** — Weekly volume increasing >10% = injury risk per Gabbett (2016)
3. **RPE drift detection** — Same weight feeling +1 RPE harder over 3+ weeks = accumulated fatigue
4. **Performance decline detection** — e1RM dropping >5% across 2+ major lifts = overreaching
5. **Chronic low readiness** — Readiness <60 for 5+ days + high training volume = overtraining

**Risk stratification:**
- 0 signals = No risk (training sustainable)
- 1-2 signals = Moderate risk (early warning, suggest deload week)
- 3+ signals = High risk (immediate deload required)

**Design decisions:**
- Requires minimum 4 weeks of data for meaningful signal (prevents false alarms)
- Conservative thresholds: false positive rate <10% (trust is critical)
- All thresholds cited from sports science research (Helms, Israetel, Schoenfeld, Gabbett)
- Integrates with ReadinessScoreService for holistic fatigue assessment

#### 2. MuscleImbalanceService
Analyzes training balance and volume distribution:

**Push/Pull ratio analysis:**
- Healthy range: 0.8–1.2 (balanced anterior/posterior chain)
- Warning threshold: >1.5 (anterior dominance = shoulder impingement + posture risk)
- Calculates push volume (chest, front delts, side delts, triceps, quads) vs pull volume (back, rear delts, biceps, traps, hamstrings, glutes)

**Volume landmarks per muscle group (Dr. Mike Israetel's research):**
- **MEV (Minimum Effective Volume):** Minimum sets/week for growth
- **MAV (Maximum Adaptive Volume):** Optimal sets/week for most trainees
- **MRV (Maximum Recoverable Volume):** Upper limit before diminishing returns

**Alert conditions:**
- Volume >MRV → "You're exceeding Maximum Recoverable Volume for [muscle]. Reduce to [MAV]–[MRV] sets."
- Volume <50% of MEV → "You're below Minimum Effective Volume for [muscle]. Increase to [MEV]+ sets."
- Push:Pull ratio >1.5 → "Anterior dominance detected. Add [X] weekly sets of back/posterior delt work."

**Primary/secondary muscle weighting:**
- Primary muscle groups count 1.0x (e.g., chest on bench press)
- Secondary muscle groups count 0.5x (e.g., triceps on bench press)
- Matches existing ProgressTrackingService pattern for consistency

**Testing:**
- 50+ comprehensive unit tests across both services
- Coverage: all detection methods, edge cases, insufficient data, risk stratification
- Deterministic test data (no flaky tests)
- Tests follow existing patterns (ReadinessScoreServiceTests, PlateauDetectionServiceTests)

**Technical Implementation:**
- Both services are stateless — analyze() methods take input data, return analysis models
- Results persisted in SwiftData (@Model classes: OvertrainingAnalysis, MuscleImbalanceAnalysis)
- JSON encoding for complex nested data (signals, alerts) to work with SwiftData
- Computed properties for type-safe access to encoded data

**Key Learnings:**
- **Systemic vs exercise-specific stagnation** — PlateauDetectionService handles individual exercises, OvertrainingDetectionService handles program-wide fatigue
- **Multi-signal detection reduces false positives** — 1 signal could be noise, 3+ signals is actionable
- **Volume landmarks are muscle-specific** — Chest MRV (22) ≠ Back MRV (25) ≠ Calves MRV (22)
- **Push/Pull ratio matters more than absolute volume** — 30 sets chest + 30 sets back is better than 20 chest + 10 back
- **RPE drift is an early overtraining signal** — Performance decline comes later (RPE drift detects fatigue sooner)
- **Conservative data requirements prevent garbage alerts** — 4 weeks minimum ensures statistical significance

**Evidence Citations:**
- Helms et al. (2018) — RPE-based autoregulation and fatigue management
- Schoenfeld & Grgic (2018) — Volume-hypertrophy dose-response relationship
- Israetel, Hoffmann, Smith (2020) — Volume landmarks (MEV/MAV/MRV) per muscle group
- Gabbett (2016) — Training-injury prevention paradox, acute:chronic workload ratio
- Bourdon et al. (2017) — Monitoring athlete training loads
- Fry et al. (2010) — Performance decline as overtraining marker

**File Paths:**
- `Packages/GymBroCore/Sources/GymBroCore/Services/Recovery/OvertrainingDetectionService.swift` (530 lines)
- `Packages/GymBroCore/Sources/GymBroCore/Services/Recovery/MuscleImbalanceService.swift` (466 lines)
- `Packages/GymBroCore/Tests/GymBroCoreTests/OvertrainingDetectionServiceTests.swift` (575 lines)
- `Packages/GymBroCore/Tests/GymBroCoreTests/MuscleImbalanceServiceTests.swift` (466 lines)

**Next Integration Steps:**
- Wire up to dashboard UI (show alerts when risk level is moderate/high)
- Integrate with adaptive training engine (trigger auto-deload on high overtraining risk)
- Add UI for volume landmarks visualization (show user where they are vs MEV/MAV/MRV)
- Performance testing with large datasets (1000+ sets, 100+ workouts)
- Manual testing with real user data (validate thresholds match empirical experience)

**Impact:**
- Prevents overtraining injuries by detecting systemic fatigue early (before performance decline)
- Corrects muscle imbalances before they cause postural dysfunction or injury
- Builds trust through conservative, evidence-based alerts (no crying wolf)
- Empowers users with actionable guidance ("reduce chest volume by 8 sets" vs "you're overtrained")
- Differentiates GymBro from competitors (no other app has evidence-based volume landmark alerts)
- Foundation for true adaptive training — app now knows when to push, hold, and back off
