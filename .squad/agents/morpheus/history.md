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
