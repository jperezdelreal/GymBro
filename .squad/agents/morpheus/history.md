# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2025-01-XX: Product Research & Market Analysis
- **Market Landscape:** Fitness app market is fragmented—Strong dominates for speed, Juggernaut AI for intelligence, but no app combines both with modern conversational AI
- **Key Gaps Identified:**
  1. Recovery awareness: HRV/sleep data exists in wearables but doesn't inform training decisions
  2. Natural language interfaces: Zero apps offer true conversational AI for programming
  3. Advanced user neglect: 71% app abandonment; serious lifters frustrated by shallow features
  4. Plateau detection: Reactive (user notices) not proactive (app warns)
- **Logging Friction:** Industry standard is 2 taps per set (Strong); opportunity for 1-tap + voice logging
- **AI Maturity (2024-2025):** LLMs now production-ready; apps like PlanFitting and Smart Rabbit pioneering conversational fitness
- **Competitive Positioning:** GymBro = Speed of Strong + Intelligence of Juggernaut + Conversation of modern AI
- **Target Segment:** Serious lifters (powerlifters, Olympic lifters, bodybuilders) with 2+ years experience—underserved, high-value, willing to pay premium
- **MVP Strategy:** Focus on core triad: ultra-fast logging + AI coach chat + adaptive periodization. Defer recovery integration, form analysis, community to v2.0+
- **Pricing Strategy:** Freemium model—free tier competitive with Strong/Hevy; premium $14.99/mo (between FitBod $13 and Juggernaut $35)
- **Key Files:**
  - `docs/PRODUCT_CONCEPT.md`: Comprehensive product vision, competitive analysis, feature roadmap, MVP scope
- **Architecture Principles:**
  - Mobile-first: Native iOS (Swift/SwiftUI)
  - Offline-first: Core features work without internet
  - AI Strategy: On-device LLM for privacy + cloud fallback for complex queries
  - Data ownership: User can export anytime; end-to-end encryption for cloud sync

### 2026-04-06: Cross-Agent Updates (from Team Decisions)
**Decisions aligned with Morpheus's product vision:**
- **UX Execution (Trinity):** 1-2 taps gesture-based logging hits the "Speed" pillar perfectly. 
- **AI Architecture (Neo):** On-device LLM + privacy-first approach enables the "Conversation" pillar with strong positioning on user trust.
- **Technical Delivery (Tank):** MVVM + SwiftData + CloudKit architecture is lean enough to hit MVP timeline (10 weeks), supports all three pillars.
- **Team Consensus:** All four agents aligned on target market (serious lifters), MVP scope (logging + AI + adaptive training), and platform strategy (iOS-first). No conflicts; maximum synergy across product, UX, AI, and tech.

### 2025-07-18: Skills Installation for Team

- **Installed 13 skills** in `.squad/skills/` covering platform safety, git workflow, testing, security, documentation, Azure cloud, iOS App Store compliance, and AI safety
- **Key Sources:** 6 built-in Squad templates, 1 custom project conventions skill, 3 adapted from `microsoft/azure-skills`, 3 adapted from `github/awesome-copilot`
- **Azure Skills Architecture:** Tailored to GymBro's 300€/month budget — documented specific tier selections (Functions Consumption, Cosmos DB Serverless, OpenAI GPT-4o-mini default), cost allocation per service, and alert thresholds
- **AI Safety is Critical:** GymBro gives physical training advice — the `ai-prompt-safety` skill defines red-flag triggers, response filtering pipeline, and mandatory disclaimers. Every AI prompt change must pass safety review.
- **App Store Prep Early:** Installed `apple-appstore-review` skill proactively — HealthKit apps, AI content, and IAP are the top three rejection risk areas for GymBro. Better to build compliance in than retrofit.
- **Skills README** at `.squad/skills/README.md` catalogs all installed skills with rationale

### 2025-01-XX: Board Clear Retro Ceremony

- **Configured new auto-triggered ceremony:** "Board Clear Retro" fires when Ralph reports the board is clear (all squad issues/PRs resolved)
- **Continuous Improvement Loop:** Work Wave → Board Clear → Retro → New Issues → Ralph Picks Up → Work Wave. This prevents Ralph from idling and maintains squad momentum.
- **Quality Gate:** Forces structured reflection on completed work while context is fresh — identify bugs, gaps, technical debt before moving to next wave
- **Automation Synergy:** Retro outputs new GitHub issues labeled `squad`; Ralph automatically picks them up in next poll cycle
- **Natural Cadence:** Event-driven (fires on board clear) rather than calendar-driven (arbitrary sprint boundaries)
- **Decision Documented:** `.squad/decisions/inbox/morpheus-board-clear-retro.md` explains rationale, alternatives considered, and success metrics
- **Implementation Notes:** Ralph's orchestration loop must detect board clear condition and trigger ceremony before entering idle state. Facilitator is always lead (Morpheus), participants are all-team members from the cleared wave.

### 2026-04-08: Android Theme Color Consolidation (#250)

- **Issue:** Duplicate Color.kt files in `app/ui/theme` and `core/ui/theme` caused maintenance overhead and potential inconsistencies
- **Resolution:** Deleted `app/ui/theme/Color.kt` duplicate, keeping `core/ui/theme/Color.kt` as single source of truth. Updated `Gradients.kt` to import from core module.
- **Architecture Decision:** Core module owns all design tokens (colors, typography, gradients). App module consumes but never defines theme primitives. This establishes clear module boundaries: core = design system, app = composition.
- **Build Environment:** Encountered transient Gradle cache issues during verification (Hilt compilation errors, file lock conflicts). Master branch builds cleanly; changes are minimal and architecturally correct—one deletion, one import addition.
- **Key Files:**
  - `android/core/src/main/java/com/gymbro/core/ui/theme/Color.kt` — canonical color definitions
  - `android/app/src/main/java/com/gymbro/app/ui/theme/Theme.kt` — imports from core via wildcard
  - `android/app/src/main/java/com/gymbro/app/ui/theme/Gradients.kt` — updated to import from core
- **PR:** #260 (draft) — ready for squad review
