# Project Context

- **Owner:** Copilot
- **Project:** GymBro — AI-first iOS gym training app for serious lifters (2026–2027)
- **Stack:** iOS, Swift, SwiftUI, HealthKit, Core ML, CloudKit
- **Focus:** AI coach chat, adaptive training, ultra-fast workout logging, progress insights, recovery-aware training
- **Avoid:** Social media features, influencer-style UX, over-gamification
- **Created:** 2026-04-06

## Learnings

<!-- Append new learnings below. Each entry is something lasting about the project. -->

### 2026-04-06: CI/CD Pipeline Setup (Issue #1)

**What was built:**
- Comprehensive GitHub Actions workflow (`.github/workflows/ci.yml`) with two-stage jobs:
  - Lint job: SwiftLint strict mode for code quality enforcement
  - Build & Test job: Xcode build + unit tests + coverage reporting
- SwiftLint configuration (`.swiftlint.yml`) with modern Swift opt-in rules and custom rules (no print, no force cast)
- PR template (`.github/PULL_REQUEST_TEMPLATE.md`) with testing checklist and coverage tracking
- README with CI status badges and project overview

**Key decisions:**
1. **macOS 15 + Xcode 16.0**: Latest stable toolchain for iOS 18 target
2. **Separate lint job**: Fast feedback on style violations before expensive builds
3. **Aggressive caching**: SPM packages + DerivedData cached via GitHub Actions cache
4. **Coverage artifacts**: Generated but not enforced yet—ready for future coverage gates
5. **iPhone 16 Pro simulator**: Latest device for testing iOS 18 features
6. **Strict SwiftLint**: Errors block merge to enforce quality from day one

**Future-ready design:**
- Workflow references 'GymBro' scheme—will work immediately when Xcode project lands (issue #2)
- Coverage reporting in place for later integration with Codecov/Coveralls
- PR template includes squad assignment field for team workflow
- Branch protection ready to be enabled once first builds pass

**Testing philosophy:**
- Fail fast: Lint before build, build before test
- Coverage tracking from day one (even if not enforced yet)
- CI runs on every PR and push to main—no exceptions

### 2026-04-07: Phase 1+2 Quality Audit (Issues #25-#30)

**What was audited:**
- All 50 Swift source files across GymBroCore (28), GymBroUI (22), and App entry (2)
- All 10 existing test files (9 Core, 1 UI)
- Audited against 6 skill files: swift-testing-pro, swift-testing-expert, swift-concurrency-pro, swift-concurrency-expert, swiftui-performance-audit, swift-swiftui-standards

**Key findings:**
1. **Test coverage at 22%** — 39/50 source files have zero tests. All tests use legacy XCTest, none use Swift Testing framework.
2. **Critical concurrency violations** — CoachChatViewModel and ActiveWorkoutViewModel are @Observable without @MainActor, creating data races on every UI interaction.
3. **Runtime crash risk** — PersonalRecordService has a force unwrap (`completedAt!`) inside a SwiftData predicate that will crash on nil values.
4. **Performance risks** — `.reduce()` in view body, 345-line ActiveWorkoutView, no Equatable on reusable subviews.
5. **No mocking infrastructure** — Tests are tightly coupled to real services (SwiftData, URLSession, UserDefaults).

**Issues created:**
- #25: Test coverage gaps (CRITICAL)
- #26: Concurrency safety violations (CRITICAL)
- #27: Test quality issues (HIGH)
- #28: SwiftUI performance risks (HIGH)
- #29: Code smells (MEDIUM)
- #30: Edge cases & robustness (HIGH)

**Recommendation:** Block MVP ship on #26 (concurrency) and PersonalRecordService crash from #30. Target 60%+ coverage before beta.
