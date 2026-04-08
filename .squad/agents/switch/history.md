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

### 2026-04-07: AI Coach Context Pipeline Verification (Neo, Issue #82)
**7 New Unit Tests for Context Fetching**
- Neo added comprehensive test coverage for `CoachChatViewModel.buildContext()` implementation
- Test cases cover: user profile + bodyweight history, recent workouts with filtering, active program week calculation, PRs sorted by e1RM, warmup set exclusion, cancelled workout exclusion, empty database handling
- All fetch paths tested (happy path + empty data)
- Filtering logic verified: cancelled workouts excluded, warmup sets excluded from PRs
- Aggregation logic validated: exercise grouping, week calculation, PR sorting
- No crashes on empty state
- Files affected: `CoachChatViewModelTests.swift` (7 new tests)
- Contribution to overall test coverage improvement — helps address the Phase 1+2 audit recommendation of 60%+ coverage

### 2025-02-06: Fixed Android Unit Test Compilation After UX Refactors (Issue #249)

**What was fixed:**
- **FakeExerciseRepository**: Added missing `override suspend fun addExercise()` and `override suspend fun isExerciseNameTaken()` methods in both core and feature test modules
  - Files: `android/core/src/test/java/com/gymbro/core/fakes/FakeExerciseRepository.kt`, `android/feature/src/test/java/com/gymbro/core/fakes/FakeExerciseRepository.kt`
- **ExerciseLibraryViewModelTest**: Added missing `tooltipManager` parameter (mocked with mockk) to all 12 ViewModel instantiations
  - File: `android/feature/src/test/java/com/gymbro/feature/exerciselibrary/ExerciseLibraryViewModelTest.kt`
- **ProgressViewModelTest**: Added missing `plateauDetectionService` and `tooltipManager` parameters (mocked with mockk) to all 10 ViewModel instantiations
  - File: `android/feature/src/test/java/com/gymbro/feature/progress/ProgressViewModelTest.kt`
- **ActiveWorkoutViewModelTest**: Added missing `personalRecordService` and `tooltipManager` parameters (mocked with mockk)
  - File: `android/feature/src/test/java/com/gymbro/feature/workout/ActiveWorkoutViewModelTest.kt`

**Test Results:**
- Core module: 80/80 tests passing ✅
- Feature module: Compilation fixed, tests running (some pre-existing failures from UX refactors remain, not introduced by this fix)

**Key patterns:**
1. **Repository interface changes**: When adding methods to repository interfaces, update all fake implementations in both main and test source sets
2. **ViewModel constructor changes**: When adding dependencies to ViewModels (like TooltipManager, services), update all test instantiations with mocked dependencies
3. **Mockk usage**: Use `mockk(relaxed = true)` for injected dependencies in tests when the specific behavior isn't being tested
4. **Test file locations**: Android test fakes can exist in both `core/src/test` and `feature/src/test` — both need updating

**Build command:**
```
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd android
.\gradlew.bat test
```

