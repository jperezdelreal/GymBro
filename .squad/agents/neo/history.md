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
