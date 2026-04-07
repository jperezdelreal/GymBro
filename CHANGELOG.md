# GymBro Changelog

All notable changes to this project are documented below.

---

## [1.0.0] – 2026-04-15

### ✨ Features

**Workout Logging**
- Ultra-fast 1-tap set logging with smart defaults
- Swipe-to-complete set entry (target: <100ms per set)
- Exercise library with built-in exercises + custom exercises
- Barcode scanning for quick equipment setup
- Offline-first: all logging works without internet
- Real-time form feedback from video analysis (MVP: deferred to v1.1)

**Adaptive Training Programs**
- AI-powered periodization engine (auto-adjusts intensity, volume, frequency)
- Autoregulation based on performance feedback (RPE, RIR)
- Recovery-aware training adjustments (integrates HRV, sleep, fatigue)
- Plateau detection and intervention recommendations
- Deload management (automatic scheduling + manual override)
- 50+ pre-built programs (strength, hypertrophy, power)
- Unlimited custom programs in Premium tier

**AI Coach**
- Real-time conversational training guidance (text-based)
- Answers questions on rest times, exercise selection, programming, recovery
- Personalized responses based on your workout history and profile
- Offline fallback with rule-based recommendations
- Rate limiting (5 free questions/week, unlimited in Premium)
- Safety filters for medical/dangerous queries
- Full disclaimer on all responses

**Recovery Insights**
- Readiness score (combines HRV, sleep, resting HR, training fatigue)
- Sleep analysis (duration, stage breakdown, quality trends)
- Resting heart rate monitoring (daily average + trends)
- Heart rate variability tracking (SDNN, compared to user baseline)
- Training load calculation (volume, intensity, fatigue accumulation)
- Recovery timeline projections

**Progress Tracking & Analytics**
- Personal record (PR) tracking and visualization
- Strength curve analysis (1RM estimates, trend lines)
- Volume trends (total reps × weight per muscle group)
- Workout history with full edit/delete capabilities
- Export data as JSON (Settings → Data Export)
- Periodization insights (plan vs actual volume, intensity distribution)

**Account & Sync**
- Sign in with Apple (privacy-first authentication)
- CloudKit sync across iOS devices (encrypted)
- Automatic cloud backup of workouts, programs, chat history
- Account deletion (Settings → Profile → Delete Account)
- Multi-device sync with conflict resolution (Last-Writer-Wins)

**HealthKit Integration**
- Read access to HealthKit data (read-only, no writes)
- Resting heart rate (daily average)
- Heart rate variability (HRV SDNN)
- Sleep analysis (duration, stage breakdown)
- Active energy burned (kcal)
- Workout synchronization from Apple Health
- Local caching (30-day rolling window, auto-refresh)

**UI & UX**
- 5-tab navigation: Workout, History, Programs, Coach, Profile
- Dark mode support (respects iOS settings)
- Haptic feedback on set completion
- Swipe gestures for quick actions
- Floating AI coach button (always accessible during workout)
- Keyboard shortcuts (iPad)

**Settings & Configuration**
- Cloud sync toggle (on/off)
- AI coach preferences (creativity level, detail level)
- Recovery score settings (weighting of HRV vs sleep vs fatigue)
- Data export (JSON, timestamped)
- HealthKit permissions management
- Account settings (email, name, deletion)
- App version + build number display

### 🛡️ Security & Privacy

**Data Protection**
- On-device encryption via SwiftData
- HTTPS/TLS 1.3 for all cloud communication
- CloudKit encryption (Apple-managed)
- No hardcoded API keys (runtime injection via secure environment)
- HealthKit data never transmitted (read-only, local only)

**Privacy-First Architecture**
- Minimal data collection (only what's necessary)
- No third-party data sharing (except Apple, OpenAI for coaching)
- No analytics tracking (no Google Analytics, Mixpanel, etc.)
- No advertising or marketing data collection
- Health data stays on device (not sent to cloud AI)
- GDPR compliant (right to access, delete, portability)
- CCPA compliant (California Consumer Privacy Act)

**Compliance**
- HealthKit app review compliance (clear usage descriptions, safe handling)
- AI safety filters (medical queries refused)
- Subscription terms disclosed before purchase
- Privacy policy and terms of service provided
- Export compliance: ECCN EAR99 (standard HTTPS encryption, exempt)
- Sign in with Apple as sole authentication method

**Credentials & Secrets**
- No API keys in source code
- Azure OpenAI credentials via environment variables
- CloudKit entitlements in code signing
- HealthKit entitlements in app capabilities

### 📱 Platform Support

**iOS Compatibility**
- Minimum: iOS 17.0 (SwiftData is production-stable)
- Tested on: iPhone 12–16 Pro Max, iPad Air 5+
- Dark mode: Supported
- Dynamic Type: Supported (accessibility text sizes)
- Landscape mode: iPhone only (iPad coming in v1.1)

**Device Support**
- iPhone 12, 12 Pro/Max, 13, 13 Pro/Max, 14, 14 Pro/Max, 15, 15 Pro/Max, 16, 16 Pro/Max
- iPad Air (5th gen), iPad Pro 11" (4th gen+), iPad Pro 12.9" (6th gen+)

**Frameworks & Minimum Requirements**
- SwiftUI (iOS 17.0+)
- SwiftData (iOS 17.0+)
- HealthKit (iOS 17.0+)
- CloudKit (iOS 17.0+)
- Sign in with Apple (iOS 13.0+)

### ⚙️ Technical Details

**Architecture**
- MVVM with @Observable pattern
- SPM (Swift Package Manager) modular architecture
- 3 packages: GymBroCore, GymBroUI, GymBroKit
- SwiftData for local persistence
- CloudKit for cloud sync
- Azure OpenAI for AI coaching

**Performance Budgets**
- App launch: <1 second (cold start)
- Set logging: <100ms per entry
- Workout view: <200ms to display 50 sets
- Cloud sync: <5 seconds for 1000 workouts
- App size: <50 MB download
- Memory: <100 MB steady state

**Build Configuration**
- Language: Swift 5.9+
- Build System: SPM
- CI/CD: GitHub Actions + Xcode Cloud
- Testing: Swift Testing framework
- Code Sign: Apple Developer Program certificate

**Data Models** (7 core SwiftData models)
- `Workout` — Date, duration, program reference
- `Exercise` — Name, muscle groups, equipment
- `ExerciseSet` — Weight, reps, RPE, date, completed
- `Program` — Name, days, phases
- `ProgramDay` — Exercises for a given day
- `UserProfile` — Name, age, experience level, goals, preferences
- `ChatMessage` — AI coach conversation history

### 🧪 Testing

**Unit Tests**
- Model validation (relationships, computed properties)
- PromptBuilder (AI response formatting)
- SafetyFilter (medical query detection)
- DeterministicCoachFallback (offline responses)
- ReadinessScoreService (recovery calculations)
- TrainingLoadCalculator (fatigue accumulation)

**UI Tests** (Xcode UI Testing)
- Sign in flow
- Workout logging flow
- AI coach conversation
- Settings and data deletion

**Manual Testing**
- HealthKit permission grant/revoke
- CloudKit sync across devices
- Offline functionality (flight mode)
- Subscription purchase and restore (sandbox environment)
- Free trial activation and auto-renewal

### 🚀 Deployment

**App Store Configuration**
- App Store Connect metadata: description, keywords, screenshots
- Privacy labels configured (health data, user ID, crash data)
- HealthKit entitlements enabled
- Screenshots for iPhone 6.7", 6.1", iPad Pro
- Launch screen configured
- App icon (1024×1024 + all sizes)

**Submission Checklist**
- Version 1.0.0 + Build 1
- All NSUsageDescription strings finalized
- Privacy Policy URL provided
- Terms of Service URL provided
- Reviewer notes included (AI coach, HealthKit, offline features)
- No hardcoded secrets
- All unit tests passing
- No App Store violations

### 📋 Documentation

- `docs/APP_STORE_SUBMISSION.md` — Full submission guide
- `docs/PRIVACY_POLICY.md` — Privacy practices and user rights
- `docs/TERMS_OF_SERVICE.md` — Legal terms and subscription policy
- `docs/TECHNICAL_APPROACH.md` — Architecture and design decisions
- `docs/PRODUCT_CONCEPT.md` — Product vision and competitive analysis
- `README.md` — Build instructions and quick start

### 🐛 Known Issues & Limitations

**MVP Scope (Deferred to v1.1+)**
- ❌ Voice logging (coming v1.1)
- ❌ Apple Watch app (coming v1.2, separate submission)
- ❌ Video form analysis (coming v2.0)
- ❌ Community/social features (coming v2.0+)
- ❌ Custom form feedback (coming v2.0)
- ❌ Android app (no plans)

**Known Limitations**
- iPad landscape mode: Deferred to v1.1
- On-device AI models: Deferred to v2.0 (iOS 27+ when Apple Intelligence is ready)
- Batch import from Fitbod/Strong: Deferred to v1.1
- Integration with coaching platforms: Deferred to v1.1
- Video library: Deferred to v1.1

**Device Limitations**
- iPad Mini: Not tested (screen too small for workout UI)
- iPhone SE (2nd gen): Not tested (iOS 16 compatibility uncertain)
- Requires iOS 17.0 minimum (no iOS 16 support)

### 🎯 Success Metrics (Target v1.0 Launch)

- [ ] 0 App Review rejections (or < 2 iterations)
- [ ] <100ms per set logging (performance budget met)
- [ ] < 1 second cold start
- [ ] <50 MB app size
- [ ] 4.5+ star rating on App Store
- [ ] <2% crash rate in first week
- [ ] <5 minute average session in first week
- [ ] 50+ 1RM calculations per week per user

---

## Future Versions (Roadmap)

### v1.0.1 – Bug Fixes & Stability
- Performance optimizations (large workout histories)
- HealthKit sync reliability improvements
- CloudKit sync edge cases

### v1.1.0 – Enhanced UX
- Voice logging (Whisper API)
- iPad landscape support
- Batch import (Fitbod, Strong, Hevy)
- Coaching integration (share programs with coaches)

### v1.2.0 – Apple Watch
- Watchkit app for workout logging
- Complication support (readiness score on watch face)
- Haptic feedback on set completion

### v2.0.0 – Intelligence & Video
- On-device AI models (Core AI / iOS 27)
- Video form analysis (pose estimation)
- Community features (public programs, athlete profiles)
- Advanced periodization (custom training blocks)
- Android app

---

## Contributors

**Core Team:**
- **Tank** (Backend): Architecture, HealthKit integration, AI coach, CloudKit sync
- **Trinity** (Frontend): UI/UX, animations, accessibility
- **Neo** (AI): Prompt engineering, safety filters, model selection
- **Morpheus** (Product): Vision, positioning, roadmap

---

## License

GymBro v1.0.0 — Proprietary. All rights reserved.

---

**Release Date:** April 15, 2026  
**Status:** Submitted to App Store  
**Target Availability:** April 20–25, 2026 (pending App Review)

For questions, contact: support@gymbro.app
