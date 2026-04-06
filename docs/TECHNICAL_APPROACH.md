# GymBro Technical Architecture

**Author:** Tank (Backend Developer)  
**Date:** January 2026  
**Status:** v1.0 Initial Design

---

## Executive Summary

GymBro is an AI-first iOS gym training app designed for serious lifters operating in network-constrained environments. The architecture is **offline-first by design**, treating connectivity as optional rather than required. Every feature must work in airplane mode. Data sync is opportunistic. Losing a user's PR history is unforgivable.

**Core Principles:**
- Offline-first: Full functionality without internet
- Privacy-first: On-device ML where possible, explicit cloud opt-in
- Reliability: Data durability over feature velocity
- Performance: Sub-second app launch, instant workout logging

---

## 1. iOS Architecture

### 1.1 Architecture Pattern: MVVM with SwiftUI

**Decision:** MVVM (Model-View-ViewModel) with modern Swift patterns

**Rationale:**
- **Simplicity for MVP:** MVVM provides the fastest path to production with minimal boilerplate
- **SwiftUI Native:** Leverages `@Observable` macro (iOS 17+) for reactive state management
- **Team Velocity:** Lower learning curve than TCA, easier to onboard contributors
- **Testability:** ViewModels are easily unit-testable without UI layer
- **Future-proof:** Can migrate critical flows to TCA later if complexity demands it

**Implementation:**
```swift
// Example ViewModel pattern
@Observable
final class WorkoutViewModel {
    private let repository: WorkoutRepository
    private let healthKit: HealthKitService
    
    var currentWorkout: Workout?
    var isLogging: Bool = false
    
    func startWorkout(_ program: Program) async {
        // Business logic in ViewModel
        currentWorkout = repository.createWorkout(from: program)
        await healthKit.startWorkoutSession()
    }
}
```

### 1.2 Module Structure

**Feature-based modularization using Swift Package Manager:**

```
GymBro/
├── App/                           # Main app target
├── Features/
│   ├── Workout/                   # Workout logging, active session
│   ├── Programs/                  # Program browser, creator
│   ├── History/                   # Workout history, analytics
│   ├── AICoach/                   # AI coaching features
│   └── Profile/                   # User profile, settings
├── Core/
│   ├── Data/                      # Data models, repositories
│   ├── Persistence/               # SwiftData, sync engine
│   ├── AI/                        # ML/LLM integration
│   ├── HealthKit/                 # HealthKit integration
│   └── UI/                        # Shared UI components
└── Tests/
    ├── UnitTests/
    └── IntegrationTests/
```

**Module Boundaries:**
- Features depend on Core, never on other Features
- Core modules have no upward dependencies
- Each Feature is independently buildable and testable

### 1.3 Dependency Injection

**Protocol-oriented DI with Swift's native property wrappers:**

```swift
// Environment-based injection for SwiftUI
extension EnvironmentValues {
    @Entry var workoutRepository: WorkoutRepository = LocalWorkoutRepository()
    @Entry var aiCoach: AICoachService = LocalAICoach()
}

// Protocol contracts
protocol WorkoutRepository {
    func create(_ workout: Workout) async throws
    func fetch(id: UUID) async -> Workout?
    func fetchAll() async -> [Workout]
}

// Test doubles easily injected
struct MockWorkoutRepository: WorkoutRepository { /* ... */ }
```

### 1.4 Navigation Architecture

**SwiftUI NavigationStack with typed routes:**

```swift
enum AppRoute: Hashable {
    case workoutSession(program: Program)
    case history
    case programDetail(id: UUID)
    case aiCoach
}

@Observable
final class NavigationCoordinator {
    var path: [AppRoute] = []
    
    func navigate(to route: AppRoute) {
        path.append(route)
    }
    
    func pop() {
        path.removeLast()
    }
}
```

**Benefits:**
- Type-safe navigation
- Deep linking ready
- Testable routing logic
- State restoration support

---

## 2. Data Layer

### 2.1 Local Persistence: SwiftData

**Decision:** SwiftData as primary persistence layer

**Rationale:**
- **Modern Swift-native:** Fully embraces Swift macros, property wrappers, and async/await
- **48% faster development** compared to Core Data (2026 benchmarks)
- **SwiftUI Integration:** First-class `@Query` support for reactive queries
- **iOS 17+ Target:** GymBro targets iOS 17+, no legacy support needed
- **Simplified Schema:** `@Model` macro eliminates .xcdatamodeld files
- **Thread Safety:** Actor-isolated contexts by default

**Migration Strategy:**
- Version schema with `VersionedSchema` and `SchemaMigrationPlan`
- Lightweight migrations for additive changes
- Custom migration logic for complex transformations

**Core Data Fallback:** None planned. SwiftData is production-ready in 2026.

### 2.2 Data Models

**Core entities:**

```swift
import SwiftData

@Model
final class Workout {
    @Attribute(.unique) var id: UUID
    var startedAt: Date
    var endedAt: Date?
    var programName: String?
    
    @Relationship(deleteRule: .cascade) var exercises: [WorkoutExercise]
    @Relationship var program: Program?
    
    var duration: TimeInterval? {
        guard let end = endedAt else { return nil }
        return end.timeIntervalSince(startedAt)
    }
}

@Model
final class WorkoutExercise {
    @Attribute(.unique) var id: UUID
    var name: String
    var order: Int
    var notes: String?
    
    @Relationship(deleteRule: .cascade) var sets: [ExerciseSet]
    @Relationship(inverse: \Workout.exercises) var workout: Workout?
}

@Model
final class ExerciseSet {
    @Attribute(.unique) var id: UUID
    var order: Int
    var weight: Double  // in user's preferred unit
    var reps: Int
    var rpe: Double?    // Rate of Perceived Exertion (1-10)
    var completedAt: Date
    
    @Relationship(inverse: \WorkoutExercise.sets) var exercise: WorkoutExercise?
}

@Model
final class Program {
    @Attribute(.unique) var id: UUID
    var name: String
    var createdAt: Date
    var isArchived: Bool = false
    
    @Relationship(deleteRule: .cascade) var days: [ProgramDay]
}

@Model
final class ProgramDay {
    @Attribute(.unique) var id: UUID
    var name: String
    var order: Int
    
    @Relationship(deleteRule: .cascade) var exercises: [ProgramExercise]
    @Relationship(inverse: \Program.days) var program: Program?
}

@Model
final class ProgramExercise {
    @Attribute(.unique) var id: UUID
    var name: String
    var order: Int
    var targetSets: Int
    var targetReps: String  // e.g., "8-12", "AMRAP"
    var restSeconds: Int?
    
    @Relationship(inverse: \ProgramDay.exercises) var day: ProgramDay?
}

@Model
final class UserProfile {
    @Attribute(.unique) var id: UUID
    var name: String?
    var preferredUnit: WeightUnit = .pounds
    var createdAt: Date
    var lastSyncedAt: Date?
    
    // HealthKit preferences
    var healthKitEnabled: Bool = false
    var syncSleep: Bool = false
    var syncHRV: Bool = false
}

enum WeightUnit: String, Codable {
    case pounds, kilograms
}
```

**Design Decisions:**
- Every entity has a UUID for sync compatibility
- Relationships use cascade deletes to prevent orphans
- Computed properties for derived data (duration, PRs)
- Flat structure to avoid deep query chains

### 2.3 Offline-First Strategy

**Principle:** The local database is the source of truth.

**Implementation:**
1. **Write-through cache:** All user actions persist locally immediately
2. **Background sync:** CloudKit sync happens asynchronously when connected
3. **Queue-based writes:** Failed sync operations queue for retry
4. **Optimistic UI:** Show changes instantly, handle sync conflicts later

**User Experience:**
- Workout logging works in airplane mode
- No loading spinners for local data
- Explicit "Syncing..." indicator when pushing to cloud
- "Last synced: X minutes ago" in settings

### 2.4 Sync Architecture: CloudKit

**Decision:** CloudKit with NSPersistentCloudKitContainer

**Rationale:**
- **Zero backend code:** Apple manages servers, scaling, auth
- **Free tier:** Generous limits (1GB storage, 10GB transfer per user)
- **Privacy-first:** Data stays in user's iCloud, end-to-end encrypted
- **Native Integration:** SwiftData + CloudKit is first-class Apple path
- **Sign in with Apple:** Seamless auth, no email/password management

**Architecture:**

```swift
import SwiftData

@ModelActor
actor SyncEngine {
    private let modelContainer: ModelContainer
    
    init(modelContainer: ModelContainer) {
        self.modelContainer = modelContainer
    }
    
    func sync() async throws {
        // CloudKit handles sync automatically via NSPersistentCloudKitContainer
        // This method monitors sync state and handles errors
        
        let cloudKitSchema = Schema([
            Workout.self,
            WorkoutExercise.self,
            ExerciseSet.self,
            Program.self,
            ProgramDay.self,
            ProgramExercise.self,
            UserProfile.self
        ])
        
        // Enable CloudKit sync
        let cloudKitConfig = ModelConfiguration(
            schema: cloudKitSchema,
            isStoredInMemoryOnly: false,
            cloudKitDatabase: .private("iCloud.com.gymbro.app")
        )
    }
}
```

**Sync Behavior:**
- Automatic background sync when network available
- Push notifications for remote changes
- Batch uploads to minimize network usage
- Exponential backoff on failures

### 2.5 Conflict Resolution

**Strategy:** Last-Writer-Wins with property-level merging for critical fields

**Rationale:**
- Workout data rarely conflicts (users don't edit old workouts on multiple devices)
- When conflicts occur, most recent edit usually represents user intent
- Programs can conflict—use custom merge logic

**Implementation:**

```swift
// CloudKit conflict handler
func resolveConflict(local: Workout, remote: Workout) -> Workout {
    // Last-Writer-Wins for most fields
    guard local.endedAt == nil && remote.endedAt != nil else {
        return remote.updatedAt > local.updatedAt ? remote : local
    }
    
    // Special case: Don't overwrite in-progress workout
    return local
}

// For Programs, prefer non-deleted version
func resolveConflict(local: Program, remote: Program) -> Program {
    if local.isArchived && !remote.isArchived {
        return remote  // Un-archive wins
    }
    return remote.updatedAt > local.updatedAt ? remote : local
}
```

**User Visibility:**
- Conflicts are rare and auto-resolved
- No "Which version do you want?" dialogs
- If data loss occurs, user can view "Recently Deleted" (future feature)

### 2.6 Migration Strategy

**SwiftData Schema Versioning:**

```swift
enum WorkoutSchemaV1: VersionedSchema {
    static var versionIdentifier = Schema.Version(1, 0, 0)
    static var models: [any PersistentModel.Type] {
        [Workout.self, WorkoutExercise.self, ExerciseSet.self]
    }
}

enum WorkoutSchemaV2: VersionedSchema {
    static var versionIdentifier = Schema.Version(2, 0, 0)
    static var models: [any PersistentModel.Type] {
        [Workout.self, WorkoutExercise.self, ExerciseSet.self, Program.self]
    }
}

enum WorkoutMigrationPlan: SchemaMigrationPlan {
    static var schemas: [any VersionedSchema.Type] {
        [WorkoutSchemaV1.self, WorkoutSchemaV2.self]
    }
    
    static var stages: [MigrationStage] {
        [migrateV1toV2]
    }
    
    static let migrateV1toV2 = MigrationStage.custom(
        fromVersion: WorkoutSchemaV1.self,
        toVersion: WorkoutSchemaV2.self,
        willMigrate: { context in
            // Pre-migration logic
        },
        didMigrate: { context in
            // Post-migration logic: populate Programs from existing workouts
        }
    )
}
```

**Migration Testing:**
- Unit tests for each migration stage
- Automated tests on production-like datasets
- Canary releases with migration rollback plan

---

## 3. Backend Services

### 3.1 What Requires a Backend?

**CloudKit Handles:**
- User authentication (Sign in with Apple)
- Data sync (workouts, programs, profile)
- Push notifications for sync events

**Custom Backend Needed For:**
1. **AI Coach API Gateway**
   - Proxies requests to OpenAI/Claude
   - Rate limiting per user
   - Cost tracking and quota management
   - Prompt versioning and A/B testing

2. **Exercise Database Updates**
   - Curated exercise library with videos/instructions
   - Community-submitted exercises (future)
   - Search indexing and ranking

3. **Analytics (Optional for MVP)**
   - Aggregate usage metrics (privacy-preserving)
   - Crash reporting
   - Performance monitoring

**MVP Scope:** AI Coach API Gateway only. Exercise DB can be bundled in app initially.

### 3.2 API Design: REST

**Decision:** REST API with JSON, not GraphQL

**Rationale:**
- **Simplicity:** REST is simpler for a small API surface
- **Caching:** HTTP caching works out-of-box
- **Tooling:** Better Xcode integration, URLSession native support
- **Future:** Can add GraphQL layer later if query complexity grows

**Endpoints:**

```
POST   /api/v1/ai/chat                # AI coach conversation
POST   /api/v1/ai/analyze-workout     # AI analysis of completed workout
GET    /api/v1/exercises              # Exercise library
GET    /api/v1/exercises/:id          # Single exercise detail
POST   /api/v1/feedback               # User feedback (support)
```

**Request/Response Example:**

```json
// POST /api/v1/ai/chat
{
  "user_id": "uuid",
  "message": "Should I deload this week?",
  "context": {
    "recent_workouts": [...],  // Last 2 weeks
    "current_program": {...},
    "health_data": {
      "avg_hrv": 45,
      "sleep_hours": 6.5
    }
  }
}

// Response
{
  "reply": "Based on your HRV drop and reduced sleep...",
  "confidence": 0.85,
  "suggested_actions": ["deload", "extra_rest_day"]
}
```

### 3.3 Authentication: Sign in with Apple

**Implementation:**
- User signs in with Apple ID
- JWT token issued by backend, stored in Keychain
- Token includes CloudKit user record name for data scoping
- 90-day token expiration, auto-refresh on API calls

**Privacy:**
- Email relay support (hide real email)
- No password management
- No email verification flow

**Code:**

```swift
import AuthenticationServices

@Observable
final class AuthService {
    var currentUser: User?
    
    func signInWithApple() async throws {
        let provider = ASAuthorizationAppleIDProvider()
        let request = provider.createRequest()
        request.requestedScopes = [.fullName, .email]
        
        // Perform authorization
        let result = try await ASAuthorizationController.authorize(request)
        
        // Exchange Apple token for backend JWT
        let jwt = try await exchangeTokenWithBackend(result.credential)
        
        // Store in Keychain
        try KeychainService.store(jwt, for: "auth_token")
        
        currentUser = User(appleID: result.credential.user)
    }
    
    func signOut() throws {
        try KeychainService.delete("auth_token")
        currentUser = nil
    }
}
```

### 3.4 Rate Limiting & Cost Control

**AI API Rate Limits:**
- 50 AI coach messages per day (free tier)
- 100/day for paid users
- Exponential backoff on 429 responses
- Local cache for repeated questions

**Backend Rate Limiting:**
- 100 requests/minute per user
- 1000 requests/hour per user
- Cloudflare for DDoS protection

**Cost Management:**
- OpenAI API requests: $0.01/request budget
- Claude API as fallback for long-context queries
- On-device LLM for simple queries (MVP: not implemented)

### 3.5 Error Handling

**Error Categories:**

```swift
enum APIError: LocalizedError {
    case networkUnavailable
    case rateLimitExceeded(retryAfter: TimeInterval)
    case unauthorized
    case serverError(message: String)
    case invalidResponse
    
    var errorDescription: String? {
        switch self {
        case .networkUnavailable:
            return "No internet connection. Your data is saved locally."
        case .rateLimitExceeded(let seconds):
            return "AI coach limit reached. Try again in \(Int(seconds))s."
        case .unauthorized:
            return "Please sign in again."
        case .serverError(let msg):
            return "Server error: \(msg)"
        case .invalidResponse:
            return "Unexpected response from server."
        }
    }
}
```

**User Experience:**
- Network errors: Silent retry with exponential backoff
- Rate limits: Clear countdown timer
- Auth errors: Prompt re-login
- Never crash on backend errors

---

## 4. AI Integration Architecture

### 4.1 On-Device Core ML Models

**Initial Scope (MVP):** No on-device LLM. Use cloud APIs only.

**Future (v2.0):** On-device models for:
- Exercise form analysis (computer vision + pose estimation)
- Simple workout recommendations (lightweight transformer)
- Privacy-sensitive queries (e.g., injury history)

**When On-Device Models Are Added:**

```swift
import CoreML
import NaturalLanguage

final class LocalAICoach {
    private let model: MLModel
    
    init() throws {
        // Load quantized model from app bundle
        let config = MLModelConfiguration()
        config.computeUnits = .cpuAndNeuralEngine
        self.model = try WorkoutRecommender(configuration: config).model
    }
    
    func recommendExercises(history: [Workout]) -> [Exercise] {
        let features = extractFeatures(from: history)
        let prediction = try? model.prediction(from: features)
        return parseExercises(from: prediction)
    }
}
```

**Model Update Strategy:**
- Models bundled in app binary (no over-the-air updates initially)
- App updates include new model versions
- Future: Background asset downloads for large models

### 4.2 Server-Side LLM Integration

**Primary:** OpenAI GPT-4 Turbo via API  
**Fallback:** Claude 3.5 Sonnet (for long-context queries)  
**Future:** Apple Intelligence APIs when available in private beta

**Architecture:**

```swift
protocol AICoachService {
    func chat(message: String, context: WorkoutContext) async throws -> AIResponse
    func analyzeWorkout(_ workout: Workout) async throws -> WorkoutAnalysis
}

final class CloudAICoach: AICoachService {
    private let apiClient: APIClient
    private let promptEngine: PromptEngine
    
    func chat(message: String, context: WorkoutContext) async throws -> AIResponse {
        // Build prompt with context
        let prompt = promptEngine.buildPrompt(
            userMessage: message,
            recentWorkouts: context.recentWorkouts,
            currentProgram: context.program,
            healthData: context.healthData
        )
        
        // Call backend (which proxies to OpenAI)
        let request = AIRequest(prompt: prompt, max_tokens: 500)
        let response = try await apiClient.post("/api/v1/ai/chat", body: request)
        
        return AIResponse(
            reply: response.reply,
            confidence: response.confidence,
            suggestedActions: response.suggested_actions
        )
    }
    
    func analyzeWorkout(_ workout: Workout) async throws -> WorkoutAnalysis {
        // Structured analysis of completed workout
        let prompt = promptEngine.buildAnalysisPrompt(workout)
        let response = try await apiClient.post("/api/v1/ai/analyze-workout", body: prompt)
        
        return WorkoutAnalysis(
            overallRating: response.rating,
            strengths: response.strengths,
            improvements: response.improvements,
            nextWorkoutSuggestion: response.next_workout
        )
    }
}
```

**Prompt Engineering:**
- System prompt defines AI persona (experienced powerlifting coach)
- Context includes last 2 weeks of workouts, current program, HealthKit data
- Few-shot examples for consistent response format
- Versioned prompts for A/B testing

### 4.3 Privacy-First Data Pipeline

**Principles:**
1. **Explicit Opt-In:** Users choose to enable AI coach
2. **Minimal Data:** Only send workouts + HealthKit metrics, never identifiable info
3. **Anonymization:** User ID is hashed before sending to AI API
4. **On-Device First:** Analyze locally when possible (future)
5. **No Training:** User data never used to fine-tune models (contractual guarantee)

**Data Flow:**

```
User Input → Local Validation → Anonymization → Backend Proxy → OpenAI API
                                                       ↓
                                                  Rate Limit
                                                  Cost Tracking
                                                  Prompt Versioning
                                                       ↓
OpenAI Response → Backend → Local Cache → User
```

**User Controls:**
- Toggle AI coach on/off
- Delete AI conversation history
- Export all data (GDPR compliance)

### 4.4 Model Update Strategy

**Current (MVP):** No models to update. Cloud API only.

**Future (On-Device Models):**
- **In-App:** Models < 50MB bundled in app
- **On-Demand Resources:** Models 50-500MB download when AI enabled
- **Background Assets:** Models > 500MB download via NSURLSession background tasks
- **Versioning:** Models include version metadata, app checks for updates on launch

---

## 5. HealthKit Integration

### 5.1 Data Sources

**MVP Integration:**
- ✅ Sleep analysis (total sleep, deep/REM stages)
- ✅ Heart Rate Variability (HRV)
- ✅ Resting Heart Rate
- ✅ Workout detection (Apple Watch)
- ✅ Active calories

**Future:**
- Body measurements (weight, body fat %)
- Blood oxygen
- Respiratory rate
- Mindful minutes

### 5.2 Permissions Model

**Request Strategy:**
- Request permissions only when feature is used (lazy permissions)
- Explain why each permission is needed (in-context education)
- Graceful degradation if denied (app works without HealthKit)

**Implementation:**

```swift
import HealthKit

final class HealthKitService {
    private let store = HKHealthStore()
    
    private let readTypes: Set<HKSampleType> = [
        HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!,
        HKObjectType.quantityType(forIdentifier: .heartRateVariabilitySDNN)!,
        HKObjectType.quantityType(forIdentifier: .restingHeartRate)!,
        HKObjectType.workoutType(),
        HKObjectType.quantityType(forIdentifier: .activeEnergyBurned)!
    ]
    
    func requestAuthorization() async throws {
        try await store.requestAuthorization(toShare: [], read: readTypes)
    }
    
    func fetchRecentSleep(days: Int = 7) async throws -> [SleepSample] {
        let sleepType = HKObjectType.categoryType(forIdentifier: .sleepAnalysis)!
        let predicate = HKQuery.predicateForSamples(
            withStart: Date().addingTimeInterval(-Double(days) * 86400),
            end: Date()
        )
        
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKSampleQuery(
                sampleType: sleepType,
                predicate: predicate,
                limit: HKObjectQueryNoLimit,
                sortDescriptors: [NSSortDescriptor(key: HKSampleSortIdentifierStartDate, ascending: false)]
            ) { _, samples, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    let sleepSamples = (samples as? [HKCategorySample] ?? [])
                        .map { SleepSample(from: $0) }
                    continuation.resume(returning: sleepSamples)
                }
            }
            store.execute(query)
        }
    }
    
    func fetchAverageHRV(days: Int = 7) async throws -> Double {
        let hrvType = HKObjectType.quantityType(forIdentifier: .heartRateVariabilitySDNN)!
        let predicate = HKQuery.predicateForSamples(
            withStart: Date().addingTimeInterval(-Double(days) * 86400),
            end: Date()
        )
        
        return try await withCheckedThrowingContinuation { continuation in
            let query = HKStatisticsQuery(
                quantityType: hrvType,
                quantitySamplePredicate: predicate,
                options: .discreteAverage
            ) { _, result, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let average = result?.averageQuantity() {
                    continuation.resume(returning: average.doubleValue(for: HKUnit.secondUnit(with: .milli)))
                } else {
                    continuation.resume(returning: 0)
                }
            }
            store.execute(query)
        }
    }
}
```

### 5.3 Background Refresh Patterns

**Strategy:** Poll HealthKit data on app launch and after workouts

**Why Not Background Refresh?**
- HealthKit background delivery is unreliable (battery optimization)
- Most users open app daily (workout logging)
- Data staleness (hours) is acceptable for sleep/HRV

**Implementation:**

```swift
final class HealthSyncCoordinator {
    private let healthKit: HealthKitService
    private let repository: HealthDataRepository
    
    @MainActor
    func syncOnAppLaunch() async {
        guard UserDefaults.standard.bool(forKey: "healthKitEnabled") else { return }
        
        do {
            // Fetch last 7 days of data
            async let sleep = healthKit.fetchRecentSleep(days: 7)
            async let hrv = healthKit.fetchAverageHRV(days: 7)
            async let restingHR = healthKit.fetchAverageRestingHeartRate(days: 7)
            
            let (sleepData, hrvAvg, hrAvg) = try await (sleep, hrv, restingHR)
            
            // Store in SwiftData for offline access
            await repository.storeSleepData(sleepData)
            await repository.storeMetrics(hrv: hrvAvg, restingHR: hrAvg)
            
        } catch {
            print("HealthKit sync failed: \(error)")
            // Silent failure, don't block app launch
        }
    }
}
```

**Battery Impact:**
- Minimize query frequency (once per app launch)
- Use statistics queries (avg, sum) instead of fetching all samples
- Cache results locally to avoid redundant queries

---

## 6. Performance & Quality

### 6.1 Build System: Swift Package Manager

**Decision:** SPM for all dependencies and modularization

**Rationale:**
- **Native Xcode integration:** First-class support, no external tools
- **Faster builds:** Incremental compilation, module caching
- **Dependency clarity:** Explicit package graph, no version conflicts
- **Future-proof:** Apple's official solution, actively developed

**No CocoaPods/Carthage:**
- CocoaPods: Adds Podfile complexity, slower builds, deprecated by community
- Carthage: Manual framework management, not SwiftUI-friendly

**Dependencies (MVP):**
```swift
// Package.swift
let package = Package(
    name: "GymBro",
    platforms: [.iOS(.v17)],
    products: [
        .library(name: "Workout", targets: ["Workout"]),
        .library(name: "AI", targets: ["AI"]),
        .library(name: "Core", targets: ["Core"])
    ],
    dependencies: [
        // None initially—minimize third-party deps
    ],
    targets: [
        .target(name: "Workout", dependencies: ["Core"]),
        .target(name: "AI", dependencies: ["Core"]),
        .target(name: "Core"),
        .testTarget(name: "WorkoutTests", dependencies: ["Workout"])
    ]
)
```

### 6.2 CI/CD Pipeline: GitHub Actions + Xcode Cloud

**GitHub Actions (Open Source, Free):**
- Pull request checks: SwiftLint, unit tests
- Automated UI tests on PR merge
- Nightly builds on main branch

**Xcode Cloud (Apple-Hosted):**
- TestFlight beta distribution
- App Store release builds
- Parallel testing on real devices

**Workflow:**

```yaml
# .github/workflows/ci.yml
name: CI

on:
  pull_request:
  push:
    branches: [main]

jobs:
  test:
    runs-on: macos-14
    steps:
      - uses: actions/checkout@v4
      - name: Select Xcode
        run: sudo xcode-select -s /Applications/Xcode_16.0.app
      - name: Run SwiftLint
        run: swiftlint lint --strict
      - name: Build and Test
        run: |
          xcodebuild test \
            -scheme GymBro \
            -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
            -enableCodeCoverage YES
      - name: Upload Coverage
        uses: codecov/codecov-action@v4
```

**Release Process:**
1. Merge PR → GitHub Actions runs tests
2. Tag release (`v1.0.0`) → Xcode Cloud builds App Store binary
3. TestFlight beta → Internal testing (48 hours)
4. Phased rollout → 10% → 50% → 100% over 7 days

### 6.3 Performance Budgets

**App Launch Time:** < 1 second cold launch

**Strategies:**
- Lazy load features (defer AI, HealthKit until used)
- Minimize application(_:didFinishLaunchingWithOptions:) work
- Use `@MainActor` to avoid thread hops
- Defer SwiftData container creation until first access

**Workout Logging:** < 100ms per set logged

**Strategies:**
- In-memory caching of active workout
- Batch SwiftData inserts
- Optimistic UI updates
- Background save on set completion

**Sync Time:** < 5 seconds for 1000 workouts

**Strategies:**
- Incremental CloudKit sync
- Batch uploads (max 100 records/request)
- Background sync on app inactive

### 6.4 App Size Budget

**Target:** < 50 MB download size (App Store)

**Current Estimate:**
- App binary: ~15 MB
- Assets (icons, images): ~5 MB
- Exercise database (bundled): ~10 MB
- **Total:** ~30 MB (20 MB buffer for future features)

**Strategies:**
- Use HEIC for images (50% smaller than PNG)
- On-demand resources for exercise videos (future)
- No embedded ML models initially (cloud API only)

### 6.5 Memory & Battery Considerations

**Memory Budget:** < 100 MB steady-state, < 200 MB peak

**Strategies:**
- SwiftData automatic memory management
- Lazy load images (AsyncImage)
- Release old workout data from memory (keep last 30 days)
- Use `@Query` with fetch limits

**Battery Impact:**
- No continuous background location
- HealthKit queries once per app launch
- CloudKit sync opportunistic (when connected to WiFi preferred)
- No background refresh by default (user opt-in)

**Measurement:**
- Xcode Instruments: Leaks, Allocations, Energy Log
- MetricKit for production telemetry
- TestFlight feedback on battery drain

---

## 7. MVP Technical Scope

### 7.1 Must-Have for v1.0

**Core Features:**
1. **Workout Logging**
   - Start workout session
   - Log exercises, sets, reps, weight
   - Timer for rest periods
   - Complete workout → save to SwiftData

2. **Program Management**
   - Create custom program (days + exercises)
   - Start workout from program
   - Archive/unarchive programs

3. **History**
   - View past workouts
   - Filter by exercise
   - View PRs (personal records)

4. **Profile**
   - Sign in with Apple
   - Set weight unit preference
   - Enable/disable HealthKit sync

5. **Offline-First**
   - All features work without internet
   - CloudKit sync when connected

**Infrastructure:**
- SwiftData persistence
- CloudKit sync
- Sign in with Apple
- Basic HealthKit integration (sleep, HRV)

### 7.2 Can Start Simple (Replace Later)

**Exercise Database:**
- **v1.0:** Bundled JSON file with 100 core exercises
- **v2.0:** Backend API with search, videos, community submissions

**AI Coach:**
- **v1.0:** Cloud API (OpenAI) with basic prompts
- **v2.0:** On-device model for simple queries, hybrid approach

**Analytics:**
- **v1.0:** None (focus on core features)
- **v2.0:** MetricKit + Firebase Analytics (privacy-preserving)

**Apple Watch:**
- **v1.0:** None (iPhone only)
- **v2.0:** Companion app for workout logging

**Widgets:**
- **v1.0:** None
- **v2.0:** Today's workout, recent PRs

### 7.3 Third-Party Dependencies

**Minimize dependencies for MVP. Zero is ideal.**

**Considered but Rejected:**
- **Networking:** URLSession is sufficient, no need for Alamofire
- **JSON:** Codable is built-in
- **Keychain:** Native Security framework
- **UI Components:** SwiftUI is enough, no need for custom libraries

**Possible Future Additions:**
- **SwiftLint:** For code style enforcement (dev tool, not runtime)
- **Firebase Crashlytics:** If crash reporting needed (post-launch)
- **Charts:** Swift Charts (Apple's library, technically first-party)

**Dependency Review Process:**
- Must solve a real problem (no "nice to have")
- Must be actively maintained (commits in last 6 months)
- Must have > 1000 GitHub stars (community trust)
- Must be SPM-compatible
- Must not duplicate existing Apple framework

---

## 8. Risk Mitigation

### 8.1 Data Loss Prevention

**Strategies:**
1. **Local-first:** Data persists locally before sync
2. **CloudKit Backup:** Automatic iCloud backup
3. **Export Feature:** Users can export CSV of all workouts
4. **Sync Conflict Tests:** Automated tests for concurrent edits
5. **Staged Rollout:** Catch bugs before full user base

**Testing:**
- Simulate device wipe → restore from CloudKit
- Simulate conflicting edits on 2 devices → merge correctly
- Stress test with 10,000 workouts

### 8.2 Performance Degradation

**Strategies:**
1. **Pagination:** Load workouts in batches (50 at a time)
2. **Indexing:** SwiftData indexes on frequently queried fields
3. **Archiving:** Auto-archive workouts > 2 years old
4. **Pruning:** Offer "Delete old data" in settings

**Monitoring:**
- MetricKit for launch time, scroll performance
- TestFlight feedback surveys on performance
- Instruments profiling before each release

### 8.3 API Cost Overruns

**Strategies:**
1. **Rate Limiting:** 50 AI requests/day per user
2. **Caching:** Store AI responses locally for 7 days
3. **Fallback:** Disable AI coach if budget exceeded (notify users)
4. **Monitoring:** Daily cost dashboard, alerting at 80% budget

**Budget:**
- Target: $500/month for 1000 active users ($0.50 per user)
- OpenAI cost: $0.01 per request
- 50 requests/day = $1.50/month per user → need 333 users to hit budget
- **Reality check:** Most users won't use AI daily. Estimate 10 requests/month avg → $0.10/user → 5000 users to hit budget

### 8.4 CloudKit Quotas

**Free Tier Limits:**
- 1 GB storage per user
- 10 GB transfer per user per month
- 200 requests/second

**Strategies:**
1. **Compression:** Store minimal data (no images in MVP)
2. **Batch Uploads:** Group records into single requests
3. **Monitor Usage:** Dashboard for storage/transfer per user
4. **Paid Tier Plan:** If needed, upgrade to CloudKit+ ($0.03/GB/month)

**Estimate:**
- Average user: 200 workouts/year × 5 KB/workout = 1 MB/year
- 10 years of data = 10 MB per user (well under 1 GB limit)

---

## 9. Future Enhancements (Post-MVP)

### Apple Watch Companion App
- Log workouts from wrist
- Live heart rate during sets
- Rest timer on watch face

### On-Device AI Models
- Exercise form analysis via camera
- Lightweight workout recommendations
- Privacy-sensitive queries handled locally

### Social Features
- Share programs with friends
- Leaderboard (opt-in, privacy-first)
- Community exercise database

### Advanced Analytics
- Strength progression curves
- Volume/intensity tracking
- Fatigue/readiness scores

### Apple Intelligence Integration
- Siri shortcuts ("Log 5 reps at 225")
- Proactive suggestions ("Time to deload?")
- Live Activities for active workout

---

## 10. Technology Stack Summary

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| **UI** | SwiftUI + @Observable | Modern, declarative, minimal boilerplate |
| **Architecture** | MVVM | Simple, testable, fast to develop |
| **Persistence** | SwiftData | Native Swift, faster than Core Data |
| **Sync** | CloudKit | Zero backend code, privacy-first |
| **Auth** | Sign in with Apple | Native, privacy-first, no password mgmt |
| **AI** | OpenAI API (cloud) | MVP simplicity, migrate to on-device later |
| **Health** | HealthKit | Native iOS integration |
| **Build** | Swift Package Manager | Native, fast, clear dependencies |
| **CI/CD** | GitHub Actions + Xcode Cloud | Free tier, Apple-native for release |
| **Monitoring** | MetricKit | Built-in, privacy-preserving |

---

## 11. Open Questions & Future Research

1. **Apple Intelligence Availability:** When will Apple Intelligence APIs be available for third-party apps? (WWDC 2026?)
2. **Core AI Migration:** Should we plan for Core AI framework in iOS 27? Evaluate in Q3 2026.
3. **CloudKit Limits in Production:** Real-world usage patterns may differ from estimates. Monitor closely.
4. **AI Model Fine-Tuning:** Would fine-tuned model (on powerlifting data) improve quality? Cost/benefit analysis needed.
5. **Internationalization:** Non-US users need kg by default, translated exercise names. Plan for v1.1.

---

## 12. Conclusion

This architecture prioritizes **reliability over features**. Losing a PR is worse than lacking a flashy animation. Offline-first isn't a nice-to-have—it's the core user expectation for a gym app.

The tech stack is intentionally boring: SwiftData, CloudKit, SwiftUI. These are Apple's recommended paths in 2026, with years of production hardening. We avoid bleeding-edge frameworks (TCA) for MVP speed, but maintain modularity to adopt them later if needed.

The AI integration is pragmatic: cloud API for MVP, with a clear migration path to on-device models when Apple's Core AI framework matures.

**Next Steps:**
1. Prototype SwiftData models + CloudKit sync
2. Implement workout logging + persistence
3. Build AI coach API gateway
4. Beta test with 50 powerlifters
5. Iterate on feedback, then scale

**Success Metrics:**
- 0 data loss incidents
- < 1s app launch time
- > 4.5 App Store rating
- 80% user retention (90 days)

---

**Document Version:** 1.0  
**Last Updated:** January 2026  
**Next Review:** March 2026 (post-MVP launch)
