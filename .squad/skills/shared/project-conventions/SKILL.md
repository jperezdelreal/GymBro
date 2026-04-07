---
name: "project-conventions"
description: "GymBro iOS app coding conventions and patterns"
domain: "project-conventions"
confidence: "high"
source: "team-decision (Morpheus + Tank architecture decisions)"
---

## Context

GymBro is an AI-first iOS gym training app targeting serious lifters. The codebase uses Swift/SwiftUI with MVVM architecture, SwiftData for persistence, and CloudKit for sync. All agents must follow these conventions.

## Patterns

### Architecture: MVVM with @Observable

- **ViewModels** use `@Observable` macro (not `ObservableObject`)
- **Views** are pure SwiftUI — no business logic
- **Models** are SwiftData `@Model` classes
- **Services** are protocol-based for testability
- ViewModels never import SwiftUI — they return data, not views

### Naming Conventions

- **Files:** PascalCase matching the primary type (`WorkoutViewModel.swift`)
- **Types:** PascalCase (`ExerciseSet`, `TrainingBlock`)
- **Properties/methods:** camelCase (`currentWorkout`, `logSet()`)
- **Protocols:** Adjective or `-able` suffix (`Persistable`, `TrainingAdaptable`)
- **Enums:** PascalCase type, camelCase cases (`enum MuscleGroup { case upperBack }`)

### File Structure

```
GymBro/
├── App/              # App entry point, configuration
├── Models/           # SwiftData @Model classes
├── ViewModels/       # @Observable ViewModels
├── Views/            # SwiftUI views
│   ├── Workout/      # Grouped by feature
│   ├── Coach/
│   ├── Progress/
│   └── Settings/
├── Services/         # Business logic, API clients
│   ├── AI/           # AI coach, LLM integration
│   ├── Training/     # Periodization, progression
│   └── Health/       # HealthKit integration
├── Utilities/        # Extensions, helpers
└── Resources/        # Assets, localizations
```

### Error Handling

- Use Swift's typed throws where possible
- Define domain-specific error enums (`TrainingError`, `SyncError`)
- Never force-unwrap in production code — use `guard let` or `if let`
- Log errors to a centralized logger, not `print()`

### Testing

- Test framework: XCTest (migrate to Swift Testing when stable)
- Test location: `GymBroTests/` mirroring `GymBro/` structure
- Run command: `xcodebuild test` or Xcode Test Navigator
- All ViewModels must have unit tests
- AI/ML components need integration tests with mock data

### Code Style

- SwiftLint enforced via build phase
- Max line length: 120 characters
- Prefer `let` over `var`
- Use trailing closure syntax
- Use `// MARK: -` to section files

### Performance Budgets

- App launch: < 1s cold
- Set logging: < 100ms
- App size: < 50 MB
- Memory: < 100 MB steady-state

### Offline-First

- SwiftData is source of truth — all writes are local first
- CloudKit sync is opportunistic, never blocking
- Every feature must work without network
- Use optimistic UI — show changes instantly

## Examples

```swift
// ✓ Correct: ViewModel pattern
@Observable
final class WorkoutViewModel {
    private let trainingService: TrainingServiceProtocol
    var currentWorkout: Workout?
    var sets: [ExerciseSet] = []

    func logSet(weight: Double, reps: Int) {
        // Business logic here, not in View
    }
}

// ✓ Correct: SwiftData model
@Model
final class ExerciseSet {
    var weight: Double
    var reps: Int
    var rpe: Double?
    var timestamp: Date
    var exercise: Exercise?
}
```

## Anti-Patterns

- **Fat Views** — putting logic in SwiftUI views instead of ViewModels
- **Force unwrapping** — `!` in production code
- **Singletons** — use dependency injection via protocols
- **Blocking main thread** — heavy computation must be async
- **Ignoring offline** — assuming network is always available
- **Social features** — explicitly out of scope per product decisions
