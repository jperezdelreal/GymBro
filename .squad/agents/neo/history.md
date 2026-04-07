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

### 2026-04-07: Smart Defaults Overhaul Complete (Issue #84, PR #90)

**Complete rewrite of SmartDefaultsService with adaptive features**

- **Fatigue Detection:** Multiplier based on set number, exercise position, historical pattern
- **RPE Integration:** Calibrates based on perceived exertion (RPE < 7 = +2.5%, RPE >= 9.5 = -5%)
- **Recovery Awareness:** Readiness score multiplier (>=80 = +2.5%, <40 = -20%)
- **Historical Trends:** 7-day rolling analysis of e1RM progression direction
- **Deload Detection:** Recognizes recovery weeks, holds weight (weight < 85% of max)
- **Experience-Level Scaling:** Beginner 1.5x, Intermediate 1.0x, Advanced 0.5x, Elite 0.25x

**Test Coverage:** 20 unit tests, 527 lines, all edge cases + graceful degradation

**Decision Captured:** Merged to .squad/decisions.md as heuristics-first design pattern

**Impact:** Smart defaults now adapt to user's training state; enables true autoregulation
