# GymBro Android

> AI-first Android gym training app for serious lifters. Part of the dual-platform GymBro project.
> 
> **Primary Language:** Spanish (es) | **UI Framework:** Jetpack Compose | **Architecture:** MVI Pattern

## 📋 Features

### Core Workout Features
- **Smart Workout Logging** — Ultra-fast workout entry with voice support and auto-recognition
- **AI Coach Chat** — Real-time fitness coaching powered by Vertex AI with contextual training insights
- **Adaptive Training** — Recovery-aware workout generation based on sleep, fatigue, and plateau detection
- **Plateau Detection** — Automatic identification of performance plateaus with smart progression suggestions

### Progress & Analytics
- **Personal Record Tracking** — Estimated 1RM (e1RM) calculations with historical trends
- **Workout History** — Detailed workout logs with exercise breakdowns and set performance
- **Progress Analytics** — Graphs and dashboards for strength trends, volume trends, and frequency analysis
- **Muscle Group Insights** — Volume and frequency tracking by muscle group

### Recovery & Health
- **Recovery Metrics** — Sleep data integration via HealthKit/Health Connect
- **Recovery Screen** — At-a-glance recovery status (sleep quality, fatigue, readiness)
- **Workout Reminders** — Scheduled notifications to keep training on track
- **Health Connect Integration** — Direct access to health data on Android 14+

### Customization & Program Support
- **Workout Templates** — Save and reuse custom workout templates
- **Exercise Library** — Searchable exercise database with custom exercise creation
- **Training Programs** — Pre-built and custom program support with weekly progression
- **User Preferences** — Customizable app settings and notification preferences

### Cross-Platform Features
- **Cloud Sync** — Optional Firebase Firestore integration for multi-device sync
- **Offline Mode** — Full functionality offline; automatic sync when reconnected
- **Quick-Start Widgets** — App widgets for quick workout launch and stats display

---

## 🏗️ Architecture

### Project Structure

The Android app uses a **multi-module architecture** for scalability and clean separation of concerns:

```
android/
├── app/                    # Application layer (entry point, DI, theme, navigation)
│   ├── src/main/
│   │   ├── java/
│   │   │   └── com/gymbro/app/
│   │   │       ├── GymBroApplication.kt     # Application class with Hilt setup
│   │   │       ├── MainActivity.kt          # Single-activity architecture
│   │   │       ├── navigation/              # Nav graph and routing
│   │   │       ├── ui/theme/                # Material 3 theme and typography
│   │   │       └── widget/                  # App widgets (QuickStart, Stats)
│   │   └── res/                              # App-level resources
│   └── build.gradle.kts
│
├── core/                   # Domain & data layer (models, DB, services, business logic)
│   ├── src/main/
│   │   ├── java/com/gymbro/core/
│   │   │   ├── model/                       # Domain models (Workout, Exercise, etc.)
│   │   │   ├── database/                    # Room database (DAOs, entities)
│   │   │   ├── repository/                  # Repository pattern implementations
│   │   │   ├── service/                     # Business logic services
│   │   │   │   ├── AiCoachService.kt        # Vertex AI integration
│   │   │   │   ├── WorkoutGeneratorService.kt
│   │   │   │   ├── PlateauDetectionService.kt
│   │   │   │   ├── PersonalRecordService.kt
│   │   │   │   └── AnalyticsService.kt
│   │   │   ├── ai/                          # AI/LLM helpers (ChatMessage, prompts)
│   │   │   ├── health/                      # Health Connect integration
│   │   │   ├── sync/                        # Cloud sync & offline support
│   │   │   │   ├── service/CloudSyncService.kt (Firestore)
│   │   │   │   ├── ConnectivityObserver.kt
│   │   │   │   └── retry/RetryPolicy.kt
│   │   │   ├── notification/                # WorkManager-based reminders
│   │   │   ├── voice/                       # Voice input & parsing
│   │   │   ├── auth/                        # Authentication (Firebase)
│   │   │   ├── preferences/                 # User preferences (DataStore)
│   │   │   ├── di/                          # Hilt dependency injection modules
│   │   │   └── error/                       # Error handling & UiError
│   │   ├── schemas/                         # Room database schemas (versioning)
│   │   └── res/                              # Core theme colors & resources
│   └── build.gradle.kts
│
└── feature/                # Feature layer (UI screens, ViewModels, UI logic)
    ├── src/main/
    │   ├── java/com/gymbro/feature/
    │   │   ├── workout/                     # Active workout & smart workout screens
    │   │   ├── coach/                       # AI Coach chat UI
    │   │   ├── recovery/                    # Recovery metrics display
    │   │   ├── progress/                    # Progress analytics & graphs
    │   │   ├── history/                     # Workout history list & detail
    │   │   ├── profile/                     # User profile screen
    │   │   ├── programs/                    # Training programs screen
    │   │   ├── exerciselibrary/             # Exercise library & creation
    │   │   ├── analytics/                   # Analytics dashboard
    │   │   ├── onboarding/                  # First-launch onboarding
    │   │   ├── settings/                    # App settings screen
    │   │   ├── common/                      # Shared UI components & utilities
    │   │   │   ├── BaseViewModel.kt         # Common ViewModel base
    │   │   │   ├── *Contract.kt             # MVI contracts (State/Event/Effect)
    │   │   │   ├── GradientButton.kt
    │   │   │   ├── GymBroCard.kt
    │   │   │   ├── LottieAnimation.kt
    │   │   │   ├── HapticManager.kt
    │   │   │   └── [other reusable components]
    │   │   └── res/                         # Feature-level resources
    │   └── build.gradle.kts
    └── test/                                # Feature-layer unit tests
```

### Key Architectural Patterns

1. **Single-Activity Architecture**
   - `MainActivity` is the only Activity
   - All navigation handled via Jetpack Navigation Compose
   - State management in ViewModels

2. **MVI Pattern (Model-View-Intent)**
   - Each feature has a `Contract` file defining:
     - `State` — UI state representation
     - `Event` — User interactions & lifecycle events
     - `Effect` — One-time side effects (navigation, toasts)
   - ViewModels extend `BaseViewModel` for common MVI boilerplate

3. **Repository Pattern**
   - Clean separation between data and domain layers
   - Repositories abstract data sources (local DB, API, Health Connect)
   - Easy to swap implementations (e.g., `FirestoreSyncService` vs `NoOpCloudSyncService`)

4. **Dependency Injection (Hilt)**
   - All services, repositories, and ViewModels provided via Hilt
   - Modules in `core/di/` configure singleton services
   - App-level Hilt setup in `GymBroApplication`

5. **Offline-First with Sync**
   - Room database as source of truth
   - `OfflineSyncManager` queues changes when offline
   - `CloudSyncService` (Firestore) syncs on reconnection
   - `ConnectivityObserver` monitors network state

### Technology Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **UI** | Jetpack Compose + Material 3 | Declarative, modern UI |
| **Navigation** | Jetpack Compose Navigation | Type-safe routing |
| **Architecture** | Kotlin + Hilt | Clean, testable code |
| **Local DB** | Room + SQLite | Offline storage, schema versioning |
| **Cloud Sync** | Firebase Firestore | Multi-device sync |
| **Auth** | Firebase Auth | Anonymous authentication (MVP) |
| **AI/LLM** | Vertex AI API | Contextual coaching |
| **Health** | Health Connect API | Sleep/recovery data (Android 14+) |
| **Notifications** | WorkManager | Background reminders |
| **Voice** | Android Speech Recognition | Voice input parsing |
| **Images** | Coil + OkHttp | Image loading with caching |
| **HTTP** | Retrofit + OkHttp | API calls |
| **Utilities** | Coroutines, DataStore | Async, preferences |

---

## 🛠️ Build Instructions

### Prerequisites

- **Android Studio** — Arctic Fox (2021.3.1) or later
- **Java 17+** — Required for Kotlin 2.0+
  - Set `JAVA_HOME` to JDK 17+ (not default JRE)
  - Example: `C:\Program Files\Android\Android Studio\jbr` on Windows
- **Android SDK** — API 36 (target), API 26+ (minimum)
- **Gradle 8.0+** — Bundled with Android Studio

### Environment Setup

#### Windows

```powershell
# Set JAVA_HOME to Android Studio's JDK
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Verify Java version
java -version
```

#### macOS / Linux

```bash
# Set JAVA_HOME (add to ~/.zshrc or ~/.bashrc)
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Verify
java -version
```

### Building the App

#### Debug Build

```bash
cd android
./gradlew assembleDebug

# Output: android/app/build/outputs/apk/debug/app-debug.apk
```

#### Release Build

```bash
cd android
./gradlew assembleRelease

# Output: android/app/build/outputs/apk/release/app-release.apk
# Note: Requires signing configuration in build.gradle.kts or local.properties
```

#### Build with Tests

```bash
# Build + run unit tests
./gradlew build

# Build + run unit + instrumentation tests (requires emulator/device)
./gradlew buildAndroidTest
```

#### Clean Build

```bash
./gradlew clean build
```

### IDE Setup (Android Studio)

1. **Open Project**
   - File → Open → Select `android/` directory
   - Wait for Gradle sync to complete

2. **Verify SDK**
   - File → Project Structure → SDK Location
   - Ensure API 36 is installed (Sdk Manager → SDK Platforms)

3. **Select Build Variant**
   - Build → Select Build Variant
   - Choose `debug` or `release`

4. **Run App**
   - Select emulator or connected device
   - Click ▶ (Run) or press Shift+F10

### Gradle Commands

Common gradle wrapper commands for building and testing:

```bash
# Build tasks
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew bundleDebug          # Build debug AAB (for Play Store)
./gradlew bundleRelease        # Build release AAB

# Test tasks
./gradlew test                 # Run unit tests (all modules)
./gradlew testDebugUnitTest    # Run app module debug unit tests
./gradlew connectedAndroidTest # Run instrumentation tests on device/emulator

# Development
./gradlew build                # Full build with tests
./gradlew clean                # Remove build artifacts
./gradlew lint                 # Run Android Lint checks

# Dependency management
./gradlew dependencies         # Show dependency tree
./gradlew help                 # Show available tasks
```

---

## 🔥 Firebase Setup

Firebase is **optional**. The app runs fully offline without Firebase, with cloud features gracefully disabled.

### Enable Firebase

1. **Create Firebase Project**
   - Visit [Firebase Console](https://console.firebase.google.com)
   - Click "Create Project"
   - Name: `GymBro` (or similar)
   - Region: Choose your region

2. **Add Android App**
   - In Firebase Console, click "Add App" → Android
   - **Package name:** `com.gymbro.app`
   - **App nickname:** GymBro
   - **SHA-1 fingerprint:** (optional for development)
   - Click "Register app"

3. **Download Configuration**
   - Firebase will generate `google-services.json`
   - Download and save to: `android/app/google-services.json`

4. **Rebuild**
   ```bash
   ./gradlew clean assembleDebug
   ```

### How It Works

- The build checks for `google-services.json` at compile time
- If found: Firebase plugin applies, `FIREBASE_ENABLED` = `true`
- If not found: Google Services plugin is skipped, `FIREBASE_ENABLED` = `false`
- Code uses `if (BuildConfig.FIREBASE_ENABLED)` to conditionally initialize services

### Firestore Rules (Development)

For development, use open rules (secure before production):

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

### Firebase Services Used

- **Firestore** — Workout/exercise data sync
- **Firebase Auth** — Anonymous authentication
- **Vertex AI** — LLM-powered AI Coach

---

## 🧪 Testing

### Unit Tests

Unit tests use **JUnit 4** with **MockK** for mocking. Test files are in:
- `core/src/test/` — Repository, service, and utility tests
- `feature/src/test/` — ViewModel tests

#### Run Unit Tests

```bash
# All unit tests
./gradlew test

# Specific module
./gradlew :core:test
./gradlew :feature:test

# Specific test class
./gradlew :core:test --tests com.gymbro.core.service.*
```

### Instrumentation Tests

Instrumentation tests run on a device/emulator. Test files are in:
- `app/src/androidTest/` — Integration & UI tests

#### Run Instrumentation Tests

```bash
# Requires connected device or running emulator
./gradlew connectedAndroidTest

# Specific module
./gradlew :app:connectedAndroidTest
```

### Test Coverage

Generate a coverage report:

```bash
./gradlew createDebugCoverageReport

# Report: android/app/build/reports/coverage/debug/index.html
```

### Writing Tests

#### Unit Test Template

```kotlin
class WorkoutRepositoryTest {
    private lateinit var repository: WorkoutRepository
    private val mockWorkoutDao = mockk<WorkoutDao>()

    @Before
    fun setup() {
        repository = WorkoutRepositoryImpl(mockWorkoutDao)
    }

    @Test
    fun `test get workouts returns list`() {
        val workouts = listOf(mockk<Workout>())
        coEvery { mockWorkoutDao.getWorkouts() } returns workouts

        val result = runBlocking { repository.getWorkouts() }

        assertEquals(workouts, result)
    }
}
```

#### Instrumentation Test Template

```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAppLaunches() {
        composeTestRule.setContent {
            GymBroApp()
        }

        composeTestRule.onNodeWithTag("welcome_screen").assertIsDisplayed()
    }
}
```

---

## 🐛 Troubleshooting

### Common Build Issues

#### "JAVA_HOME not set" or Java version mismatch

**Problem:** Build fails with `Error: JAVA_HOME is not set`.

**Solution:**
```bash
# Set JAVA_HOME to Java 17+ (NOT default JRE)
export JAVA_HOME=/path/to/java17
./gradlew --version  # Verify Java version is 17+
```

#### Gradle sync timeout

**Problem:** "Could not download ... Connection timeout"

**Solution:**
```bash
# Increase timeout in gradle.properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.timeout=120000  # 120 seconds
```

#### "google-services.json not found"

**Problem:** Firebase compilation fails, but Firebase is not needed.

**Solution:**
- Ignore the warning; the app compiles without Firebase
- If you need Firebase, download `google-services.json` and place in `android/app/`

#### "Could not find method android()" in build.gradle.kts

**Problem:** AGP (Android Gradle Plugin) version mismatch.

**Solution:**
```bash
./gradlew wrapper --gradle-version=8.4  # Update Gradle wrapper
./gradlew clean build
```

#### App crashes on startup (Hilt injection error)

**Problem:** "MissingBindingException" or "No binding found for".

**Likely Cause:** Missing `@Provides` or `@Binds` in a Hilt module.

**Solution:**
1. Check `core/di/` modules for incomplete configurations
2. Verify `GymBroApplication.kt` has `@HiltAndroidApp`
3. Ensure all dependencies are included in `build.gradle.kts`

#### Unit tests pass but integration tests fail

**Problem:** Tests work locally but fail in CI or on devices.

**Solution:**
- Use `androidTest` for instrumentation (device-specific) tests
- Use `test` for unit tests (pure JVM)
- Check for hardcoded device-specific paths or assumptions

### Performance Issues

#### App is slow to start (first launch)

- Database migrations run on first launch
- Hilt DI graph is built on startup
- Enable profiling: Android Studio → Profiler → CPU

#### Jetpack Compose preview slow

- Reduce preview complexity
- Disable live layout rendering: Studio → Settings → Compose
- Increase heap size in Android Studio settings

#### Network requests are slow

- Check OkHttp logging: enable `HttpLoggingInterceptor.Level.BODY`
- Profile with Network Profiler in Logcat
- Verify Firestore rules aren't blocking reads

### Runtime Errors

#### "IllegalStateException: Cannot access database on main thread"

- Room database queries called off the main thread (correct!)
- Ensure all DB calls use `viewModelScope.launch { }`

#### "OutOfMemoryError: Java heap space"

- Increase heap size: `org.gradle.jvmargs=-Xmx4096m`
- Check for bitmap leaks in Coil image loading
- Profile with Android Profiler

#### Voice recognition returns empty string

- Check that `RECORD_AUDIO` permission is granted
- Test on device (not emulator) for better mic support
- Verify internet connection (online speech recognition requires it)

---

## 📚 Additional Resources

### Official Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Firebase for Android](https://firebase.google.com/docs/android/setup)
- [Health Connect API](https://developer.android.com/health-and-fitness/guides/health-connect)

### Project Documentation
- [Dual-Platform Migration Plan (Issue #133)](https://github.com/jperezdelreal/GymBro/issues/133)
- [GymBro Project README](../README.md)
- [iOS App README](../ios/README.md)
- [Shared Documentation](../docs/)

### Kotlin & Architecture Resources
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [MVI Pattern](https://mobius.io/) (Elm-inspired)
- [Android Architecture Samples](https://github.com/android/architecture-samples)

---

## 🤝 Contributing

When working on Android features:

1. **Create a feature branch** — `squad/{issue-number}-{slug}`
2. **Build & test locally** — `./gradlew build`
3. **Follow MVI pattern** — Use Contract files for state management
4. **Add unit tests** — Aim for 60%+ coverage
5. **Use Hilt** — Inject dependencies, don't construct manually
6. **Document changes** — Update this README if architecture or setup changes

---

**Last Updated:** 2026-04-XX | **Maintainer:** GymBro Android Team
