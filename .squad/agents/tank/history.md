# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2026-01-XX: Initial Technical Architecture Design

**Architecture Decisions:**
- **MVVM over TCA:** Chose MVVM for MVP speed and simplicity. TCA is overkill for initial scope, but architecture is modular enough to migrate critical flows later if needed.
- **SwiftData over Core Data:** SwiftData is production-ready in 2026, offers 48% faster dev cycles, and is the Apple-recommended path forward. No legacy iOS support needed.
- **CloudKit for Sync:** Zero backend code, free tier is generous, privacy-first. Apple handles servers, scaling, and auth. Perfect fit for offline-first architecture.
- **Cloud AI (OpenAI) First, On-Device Later:** MVP uses OpenAI API via backend proxy for cost control and rate limiting. Migration path to Core AI/on-device models planned for v2.0 when Apple's framework matures.
- **SPM Only, No CocoaPods:** Swift Package Manager is native, fast, and future-proof. Minimize third-party dependencies entirely for MVP.

**Offline-First Strategy:**
- Local database (SwiftData) is source of truth. All writes persist locally immediately.
- CloudKit sync is opportunistic background operation.
- Conflict resolution: Last-Writer-Wins with property-level merging for critical fields.
- Losing user's PR history is unforgivable—architecture prioritizes data durability over feature velocity.

**Performance Budgets:**
- App launch: < 1s cold launch
- Workout logging: < 100ms per set
- App size: < 50 MB download
- Memory: < 100 MB steady-state
- Sync: < 5s for 1000 workouts

**MVP Technical Scope:**
- Core features: Workout logging, program management, history, basic HealthKit (sleep, HRV)
- Infrastructure: SwiftData + CloudKit + Sign in with Apple
- Deferred to v2.0: Apple Watch, on-device AI models, social features, advanced analytics

**Risk Mitigation:**
- Data loss prevention: Local-first, CloudKit backup, export feature, conflict testing
- API cost control: Rate limiting (50 AI requests/day), caching, budget monitoring ($500/month target)
- CloudKit quotas: Batch uploads, compression, usage monitoring (well under 1GB/user limit)

**Technology Stack:**
- UI: SwiftUI + @Observable
- Persistence: SwiftData
- Sync: CloudKit
- Auth: Sign in with Apple
- AI: OpenAI API (cloud)
- Health: HealthKit
- Build: SPM
- CI/CD: GitHub Actions + Xcode Cloud

**Open Questions:**
- Apple Intelligence API availability timeline (WWDC 2026?)
- Core AI framework migration plan for iOS 27
- Real-world CloudKit usage patterns vs estimates
- Fine-tuned AI model cost/benefit for powerlifting domain

### 2026-04-06: Cross-Agent Updates (from Team Decisions)
**Decisions affecting Tank's work:**
- **Product Scope (Morpheus):** MVP is laser-focused—logging + AI + adaptive training. Defer Watch, form video, voice logging, social. Validates Tank's "SPM only, minimal dependencies" approach.
- **UX Performance (Trinity):** Needs <100ms per set logging. Tank's performance budget of <100ms per set is perfect fit.
- **AI Architecture (Neo):** Neo is targeting on-device LLM eventually (v2.0+). Tank's decision to use OpenAI API for MVP is sound—avoids waiting for iOS 27, enables launch in 10 weeks. Clear migration path.
- **Team Consensus:** All four agents agree on MVVM + SwiftData + CloudKit. No architectural conflicts. Tank has green light to begin prototyping immediately.

### 2026-04-06: Xcode Scaffold + SwiftData Models Implementation (Issues #2, #3)

**Project Structure Decisions:**
- **SPM Modular Architecture:** Created 3 separate Swift packages (GymBroCore, GymBroUI, GymBroKit) for clean separation. This enables parallel development across agents without merge conflicts—Trinity can own GymBroUI, Tank owns Core.
- **Package.swift Only:** No Xcode project file (.xcodeproj). Pure SPM approach keeps the repo cleaner and makes CI/CD simpler. Users open Package.swift directly in Xcode 16+.
- **iOS 17.0 Minimum:** SwiftData is production-stable, and our target users (serious lifters) are early adopters who stay current with iOS.

**SwiftData Model Design:**
- **Relationship Strategy:** Used `@Relationship(deleteRule: .cascade/.nullify)` carefully. Cascade for owned children (Workout → Sets), nullify for references (Set → Exercise). Prevents accidental data loss.
- **Naming: ExerciseSet not Set:** "Set" is a Swift collection type. Avoiding namespace collision saved hours of future debugging.
- **Computed Properties for Formulas:** e1RM calculated via Epley formula (weight × (1 + reps/30)). Keeps formulas DRY and testable.
- **Unit Conversion at Model Level:** All weights stored as kg (metric). Conversion to lbs happens on read via computed properties. Single source of truth prevents sync bugs.
- **CloudKit Compatibility by Design:** No optionals on required fields, all types are CloudKit-compatible (String, Int, Double, Date, UUID, enums as Codable). Migration to CloudKit sync (issue #5) will be straightforward.

**Performance Considerations:**
- **Indexes for Queries:** SwiftData auto-indexes relationships and IDs. For date-based queries (History screen), we'll add explicit indexes in future migration if needed.
- **Lazy Loading:** Relationship arrays are lazy-loaded by SwiftData. WorkoutSet.exercise won't fetch until accessed—critical for fast list rendering.

**Key Files Created:**
- `Package.swift` (root): Main app package with local package dependencies
- `Packages/GymBroCore/Package.swift`: Models and services package
- `Packages/GymBroCore/Sources/GymBroCore/Models/`: Workout.swift, Exercise.swift, ExerciseSet.swift, Program.swift, UserProfile.swift
- `GymBro/GymBroApp.swift`: App entry point with ModelContainer configuration for all 6+ model types
- `GymBro/ContentView.swift`: 5-tab navigation shell (Workout, History, Programs, Coach, Profile)
- `README.md`: Build instructions, architecture overview, tech stack

**Testing Strategy:**
- Basic unit tests in `GymBroCoreTests/GymBroCoreTests.swift` to verify model relationships and computed properties compile.
- Full test suite deferred to issue #8 (Switch's domain)—Tank just validates models are testable.

**Unblocked Issues:**
This foundation unblocks all Phase-1 work:
- Issue #4: UI screens (Trinity can now build against models)
- Issue #5: CloudKit sync (Tank can extend models with CKRecord mapping)
- Issue #6: AI coach (Neo can query workout history)
- Issue #7: HealthKit integration (Tank can link bodyweight to UserProfile)

**Risks Mitigated:**
- No Xcode project file means no merge conflicts on .xcodeproj XML
- Modular packages prevent "big ball of mud" codebase
- SwiftData thread-safety via actor isolation prevents race conditions

**Open Questions:**
- ModelContainer configuration: single shared container vs. per-view? Leaning toward single shared (simpler, faster).
- Migration strategy when model schema changes: SwiftData auto-migration works for simple changes, but complex migrations need explicit ModelMigrationPlan.
- CloudKit sync conflict resolution: Last-Writer-Wins is simple but loses data. Evaluate property-level merging for v1.1.
