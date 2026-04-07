# GymBro: App Store Submission Guide

**Version:** 1.0.0  
**Target Platform:** iOS 17.0+  
**Status:** Pre-submission preparation  
**Last Updated:** 2026-04-07

---

## 1. Privacy Nutrition Labels

### Overview

GymBro's privacy nutrition label must accurately reflect all data collection practices. Apple requires this disclosure in App Store Connect before submission.

### Data Collection Summary

| Data Category | Collected | Linked to Identity | Used for Tracking |
|---|---|---|---|
| **Health Data (HealthKit)** | ✅ Yes | ❌ No | ❌ No |
| **Fitness Data** | ✅ Yes | ❌ No | ❌ No |
| **User ID (Sign in with Apple)** | ✅ Yes | ✅ Yes | ❌ No |
| **Workout History** | ✅ Yes | ✅ Yes | ❌ No |
| **App Activity** | ✅ Limited | ✅ Yes | ❌ No |
| **Crash & Performance Data** | ✅ Yes | ❌ No | ❌ No |

### Detailed Data Practices

#### 1. **Health & Fitness Data (Collected via HealthKit)**

**Data Types Requested:**
- Resting Heart Rate (HRV: SDNN)
- Sleep Analysis (stages, duration)
- Active Energy Burned (kcal)
- Workout history (synchronized from Apple Health)

**Purpose:**
- Personalize adaptive training programs
- Calculate recovery readiness scores
- Detect training plateaus
- Optimize training load recommendations

**Data Retention:**
- Retained locally on-device indefinitely
- **Never transmitted to cloud** except via CloudKit sync (encrypted, Apple's infrastructure)
- User can export/delete via Settings

**Linked to User Identity?**
- No. Health data is tied to local CloudKit container (associated with Sign in with Apple account but not shared)

**Used for Advertising/Marketing?**
- ❌ **No.** Health data is never shared with third parties, never used for advertising targeting, never monetized

---

#### 2. **User Account & Authentication**

**Data Collected:**
- Email address (via Sign in with Apple)
- User ID (cryptographic identifier)
- Name (optional, user-provided)

**Purpose:**
- Account creation and authentication
- CloudKit sync across devices
- AI coach conversation history (on-device + cloud encrypted)

**Data Retention:**
- Retained as long as user's account is active
- Deleted if user deletes account (via in-app delete flow)

**Linked to Identity?**
- ✅ **Yes.** Account data is explicitly tied to user's Sign in with Apple identity

**Used for Advertising?**
- ❌ **No.**

---

#### 3. **Workout & Training Data**

**Data Collected:**
- Exercise names, weights, reps, sets, dates, times
- Rest times, RPE ratings
- Program names and structure
- Custom notes

**Purpose:**
- Display workout history
- Calculate adaptive training recommendations
- Provide progress tracking and analytics
- Train/personalize AI coach responses

**Storage:**
- Local SwiftData database (on-device)
- CloudKit sync (encrypted)

**Linked to Identity?**
- ✅ **Yes.** Tied to CloudKit container / Sign in with Apple account

**Used for Advertising?**
- ❌ **No.**

---

#### 4. **AI Coach Conversations**

**Data Collected:**
- Chat messages (user questions + AI responses)
- Context snapshots (recent workout history, user profile, PRs)
- Timestamps

**Purpose:**
- Provide personalized AI coaching
- Improve response quality via fine-tuning (future)
- Maintain conversation context

**Storage:**
- Local SwiftData (encrypted)
- Azure OpenAI API (stateless per request; conversation history not stored server-side in MVP)
- User can review/delete chats in-app

**Linked to Identity?**
- ✅ **Yes.** Chat history tied to user account (CloudKit sync)

**Used for Advertising?**
- ❌ **No.**

---

#### 5. **Crash & Diagnostic Data (Apple-Managed)**

**Data Collected:**
- Crash logs, performance metrics, diagnostic events
- **Not collected by GymBro directly**—handled by Apple's automatic crash reporter

**Purpose:**
- App stability monitoring
- Performance optimization

**Linked to Identity?**
- ❌ **No** (anonymous aggregate data per Apple's privacy practices)

---

### App Store Connect Privacy Configuration

**To configure in App Store Connect:**

1. Go to your app → **App Privacy** section
2. Fill in the following required answers:

   **Q: Does your app collect or track personal data?**
   - ✅ **Yes**

   **Q: Does your app sell personal data?**
   - ❌ **No**

   **Q: Does your app use advertising or marketing purposes?**
   - ❌ **No**

3. **Data Collection Table:** Add the following entries:

   | Data Type | Linked to User | Used for Tracking | Category |
   |---|---|---|---|
   | Health & Fitness Data | No | No | Health & Fitness |
   | User ID | Yes | No | User ID |
   | Workout History | Yes | No | Fitness Data |
   | App Activity | Yes | No | App Functionality |
   | Crash Data | No | No | Diagnostics |
   | Email (via Sign in with Apple) | Yes | No | Contact Info |

4. **Contact Information & Transparency:**
   - Privacy Policy URL: `https://gymbro.app/privacy` (create this before submission)
   - Support Email: `support@gymbro.app`

---

## 2. HealthKit Compliance

### Permissions & Usage Descriptions

GymBro requests only the HealthKit data types it actually uses. Update `Info.plist` in Xcode project settings with these keys:

```xml
<!-- Health Data Reading -->
<key>NSHealthShareUsageDescription</key>
<string>GymBro reads your workout history, heart rate variability, sleep data, and resting heart rate to personalize your training program and calculate recovery readiness. Your health data stays on your device—we never sell, share, or use it for advertising.</string>

<!-- Health Data Writing -->
<key>NSHealthUpdateUsageDescription</key>
<string>GymBro saves your logged workouts to Apple Health so your training data syncs across all your health and fitness apps. You can delete this access from Settings at any time.</string>

<!-- Capabilities (Background) -->
<key>NSHealthShareUsageDescription</key>
<string>GymBro accesses your HealthKit data to provide recovery insights and adaptive training recommendations. Your data is always on your device.</string>
```

### Requested HealthKit Types

Add to `GymBro.entitlements`:

```xml
<key>com.apple.developer.healthkit</key>
<true/>

<key>com.apple.developer.healthkit.access</key>
<array>
    <string>HKQuantityTypeIdentifierHeartRateVariabilitySDNN</string>
    <string>HKQuantityTypeIdentifierRestingHeartRate</string>
    <string>HKCategoryTypeIdentifierSleepAnalysis</string>
    <string>HKQuantityTypeIdentifierActiveEnergyBurned</string>
    <string>HKWorkoutTypeIdentifier</string>
</array>
```

### Data Usage Restrictions

✅ **Allowed:**
- Storing HealthKit data locally on-device
- Using data for training personalization
- Syncing via CloudKit (encrypted)
- Displaying health metrics to user

❌ **Never:**
- Share HealthKit data with third parties
- Use for advertising/marketing/targeting
- Sell or monetize health data
- Use for medical diagnosis (add disclaimer instead)
- Disclose health data without explicit user action

### Compliance Checklist

- [ ] All requested HealthKit types are actually used (no over-requesting)
- [ ] Usage description strings are clear and specific
- [ ] No generic strings like "Access Health"
- [ ] Data is never transmitted to advertising partners
- [ ] User can revoke access in Settings → Health
- [ ] App continues functioning if HealthKit access is denied (graceful degradation)
- [ ] No claim that app provides medical diagnosis
- [ ] HealthKit data is not used to train LLMs without explicit consent

---

## 3. AI Content Compliance

### AI Disclosure & Disclaimers

GymBro uses AI to provide training recommendations. Apple requires clear disclosure that content is AI-generated and not medical advice.

### Mandatory Disclaimer Language

**Display in these locations:**

1. **AI Coach Chat Screen** (before first message):
   ```
   🤖 AI Coach

   This is AI-generated training guidance based on your history and
   recovery data. Not medical advice. Consult a qualified fitness
   professional before starting any new program.

   Read full disclaimer →
   ```

2. **Every AI Response (appended):**
   ```
   ⚠️  Disclaimer: This is AI-generated training guidance, not medical advice.
   Your safety is your responsibility. Stop any exercise that causes pain.
   ```

3. **Settings → About AI Coach:**
   ```
   GymBro uses OpenAI's GPT-4o model for training recommendations.
   AI responses are generated in real-time and are not guaranteed to be
   medically accurate. We always recommend consulting with a certified
   strength coach or medical professional before significant training
   changes.

   Safety measures:
   • AI refuses to provide medical diagnosis
   • Responses are filtered for dangerous suggestions
   • You can always override AI recommendations
   • Your data is not shared with OpenAI
   ```

4. **First Launch / Onboarding:**
   ```
   ✅ You agree that GymBro's AI coach provides guidance only, not
   professional medical advice. You assume full responsibility for
   your training decisions.
   ```

### AI Safety Measures

**Implemented in `SafetyFilter.swift`:**
- Block requests containing medical keywords (diagnose, treat, cure, medication)
- Red-flag dangerous exercise queries (modify without appropriate cues)
- Refuse to provide nutrition advice outside scope

**Implemented in Prompt (Azure OpenAI):**
- System prompt includes: *"You are a strength training coach, not a doctor. Never diagnose medical conditions or provide medical treatment advice."*
- Model configured to refuse medical requests

**Implementation Reference:**
- See `Packages/GymBroCore/Sources/GymBroCore/Services/AI/SafetyFilter.swift`
- See `Packages/GymBroCore/Sources/GymBroCore/Services/AI/PromptBuilder.swift`

### App Store Review Notes

Add these points to your **Reviewer Notes** in App Store Connect:

```
GymBro's AI Coach Feature:

1. Safety Architecture:
   - Client-side safety filter blocks dangerous queries
   - Server-side system prompt instructs model to refuse medical advice
   - All responses include "Not medical advice" disclaimer
   - Users can report unsafe responses via in-app feedback

2. How to Test:
   - Tap the chat bubble 💬 during a workout
   - Ask a training question: "How long should I rest between sets?"
   - AI response includes disclaimer
   - Try a medical question: "I have back pain, what should I do?"
   - AI will refuse and redirect to medical professional

3. Data Privacy:
   - Conversations are stored locally and encrypted
   - User's health data is not sent to OpenAI
   - Only text questions + anonymized context sent
   - No persistent conversation logs on server (stateless API calls)
```

---

## 4. Subscription & In-App Purchase Compliance

### Pricing & Subscription Tiers

**Free Tier (Forever Free):**
- Unlimited workout logging
- Basic progress tracking (charts, PRs)
- 3 custom programs
- 5 AI coach questions per week (rate-limited)
- HealthKit integration (read-only)

**Premium Tier ($14.99/month or $119.99/year):**
- Unlimited AI coach questions
- Full adaptive training engine
- Unlimited custom programs
- Advanced analytics (periodization insights, autoregulation recommendations)
- Early access to new features
- Cloud sync (explicit mention: "Your data syncs securely across devices")

### Subscription Configuration (StoreKit 2)

**In App Store Connect:**

1. Add product:
   - Type: **Auto-Renewable Subscription**
   - Product ID: `com.gymbro.premium.monthly` and `com.gymbro.premium.yearly`
   - Subscription Group: `PremiumGroup`
   - Reference Name: `GymBro Premium Monthly / Yearly`

2. **Pricing Tier:** Select $14.99/month and $119.99/year

3. **Billing Cycles:** 
   - Monthly: 1 month, auto-renews
   - Yearly: 12 months, auto-renews

4. **Free Trial** (optional, recommended for launch):
   - Offer: 7-day free trial (see if user engages before charging)

### Paywall & Purchase Flow Compliance

**Requirements Met:**
- ✅ Price displayed **before** showing paywall
- ✅ Auto-renewal terms clearly visible
- ✅ "Manage Subscription" button in-app (Settings → Subscriptions)
- ✅ Restore Purchases button available
- ✅ Unsubscribe option not hidden (linked to Settings)
- ✅ Free tier is actually useful (unlimited logging)

**In-App Messages:**

```swift
// Before paywall
"GymBro Premium: $14.99/month (auto-renews). Free first 7 days. 
Manage in Settings."

// After purchase
"✅ Premium activated! Unlimited AI Coach + Advanced Analytics.
Manage your subscription in Settings."

// In Settings → Subscriptions
"Active Subscription: GymBro Premium Monthly
Next Billing Date: [date]
[Cancel Subscription] [Manage Family Sharing]"
```

### Restoring Purchases

Implement in `PremiumView.swift`:

```swift
Button("Restore Purchases") {
    Task {
        await StoreKit2Manager.shared.restorePurchases()
        // Refresh premium status
    }
}
```

**Required for compliance:** Users must be able to restore previous purchases without repaying.

---

## 5. Export Compliance (ECCN Classification)

### Category: Encryption & Export Control

GymBro uses HTTPS for all cloud communication. **Classification:**

**ECCN Code:** EAR99 (Encryption Exempt)

**Rationale:**
- Uses **only standard HTTPS/TLS** (not custom/proprietary encryption)
- No encryption algorithm implementation in the app
- No end-to-end encryption between users
- Data encrypted only in transit (standard web protocol)

**App Store Submission:**

When submitting, Apple asks: *"Does your app use or contain encryption?"*

- ✅ **Yes** (standard HTTPS)
- Select: **"Exempt — uses standard encryption for transmission"**

**Compliance Confirmation:**

From Apple's App Store Review Guidance:
> "Apps that use standard encryption for transmission over HTTPS are exempt from export compliance."

GymBro qualifies—no additional export documentation required.

---

## 6. Platform-Specific Configuration

### Info.plist Requirements

Add the following keys to the Xcode project settings (or create `Info.plist`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
 "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <!-- App Display -->
    <key>CFBundleName</key>
    <string>GymBro</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>

    <!-- HealthKit Permissions -->
    <key>NSHealthShareUsageDescription</key>
    <string>GymBro reads your workout history, heart rate variability, 
sleep data, and resting heart rate to personalize your training program 
and calculate recovery readiness. Your health data stays on your device.</string>
    
    <key>NSHealthUpdateUsageDescription</key>
    <string>GymBro saves your logged workouts to Apple Health so your 
training data syncs across all your health and fitness apps.</string>

    <!-- Background Processing (for HealthKit sync) -->
    <key>NSBonjourServiceTypes</key>
    <array/>
    <key>UIBackgroundModes</key>
    <array>
        <string>fetch</string>
        <string>processing</string>
    </array>

    <!-- Sign in with Apple -->
    <key>NSUserActivityTypes</key>
    <array>
        <string>NSUserActivityTypeBrowsingWeb</string>
    </array>

    <!-- Minimum iOS Version -->
    <key>MinimumOSVersion</key>
    <string>17.0</string>
    
    <!-- Supported Orientations -->
    <key>UISupportedInterfaceOrientations</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationPortraitUpsideDown</string>
    </array>
    
    <key>UISupportedInterfaceOrientations~ipad</key>
    <array>
        <string>UIInterfaceOrientationPortrait</string>
        <string>UIInterfaceOrientationPortraitUpsideDown</string>
        <string>UIInterfaceOrientationLandscapeLeft</key>
        <string>UIInterfaceOrientationLandscapeRight</string>
    </array>
</dict>
</plist>
```

### Capabilities (Xcode Project)

Enable in **Xcode → Target → Signing & Capabilities:**

- ✅ HealthKit
- ✅ Sign in with Apple
- ✅ Push Notifications (for future Apple Watch sync)
- ✅ Background Processing

### Launch Screen

Create `LaunchScreen.storyboard` or SwiftUI view with:
- GymBro logo (1024x1024 primary)
- Brand color (dark/light mode)
- No loading spinner (static image)
- Minimal text

---

## 7. App Store Metadata

### App Description (max 170 chars)

```
AI-powered gym training app for serious lifters. 
1-tap set logging + adaptive programming + AI coach.
```

### Subtitle (max 30 chars)

```
AI Coach for Serious Lifters
```

### Keywords (comma-separated, 100 chars max)

```
workout,training,gym,strength,powerlifting,program,ai coach,fitness
```

### Support URL

```
https://gymbro.app/support
```

### Privacy Policy URL

```
https://gymbro.app/privacy
```

### Primary Category

```
Health & Fitness
```

### Secondary Category

```
Sports (if selecting multiple)
```

### Content Rating Questionnaire

**Violence:** None
**Profanity:** None
**Sexual Content:** None
**Alcohol/Tobacco:** None
**Medical:** Yes (HealthKit, recovery tracking) → Allow based on legitimate health use
**User-Generated Content:** No (isolated accounts, no social features in MVP)

---

## 8. App Store Connect Screenshots

### Required Sizes

Provide screenshots for:
- iPhone 6.7" (largest iPhone)
- iPhone 6.1" (standard iPhone)
- iPad Pro 12.9" (one screenshot minimum)

### Recommended Screenshots (5–10)

1. **Workout Logging (1-tap set):**
   - Show empty workout → swiped set card → completed set
   - Caption: "Log sets in one swipe. No friction."

2. **Adaptive Training:**
   - Show program with auto-adjusted weights
   - Caption: "AI learns your strength & adapts your program."

3. **Recovery Insights:**
   - Show readiness score with HRV, sleep, fatigue
   - Caption: "See your recovery status. Train smart."

4. **AI Coach Chat:**
   - Show Q&A conversation
   - Caption: "Real-time training coaching. Questions answered instantly."

5. **Progress Analytics:**
   - Show PR history, body weight trend, volume chart
   - Caption: "Track every lift. See your progress."

### Screenshot Best Practices

- ✅ Show actual app UI (not mock-ups)
- ✅ Use text overlays sparingly (mostly show UI)
- ✅ Consistent font, brand colors, dark mode
- ✅ No fake data (use real user data or clean sample data)
- ✅ 5–10 screenshots (not too many)
- ✅ Include premium features to showcase value

---

## 9. Review Compliance Checklist

### Pre-Submission Verification

- [ ] All NSUsageDescription strings are specific (not generic)
- [ ] HealthKit data is read-only for MVP (no writes until v2.0)
- [ ] Privacy Policy URL is live and accurate
- [ ] Terms of Service URL is live and includes subscription terms
- [ ] AI disclaimers appear before first coach response
- [ ] Restore Purchases works (test with TestFlight)
- [ ] App runs offline for core features (workout logging)
- [ ] Account deletion implemented (Settings → Account → Delete)
- [ ] No hardcoded API keys or secrets in code
- [ ] App icon is 1024x1024 + all device sizes
- [ ] Launch screen is configured (no loading spinners)
- [ ] Screenshots are up-to-date and match UI
- [ ] Version number is 1.0.0 in Info.plist
- [ ] Build number incremented in Xcode
- [ ] Entitlements file includes HealthKit capability
- [ ] No excessive crash reports in TestFlight
- [ ] Performance: app launches < 2s, core features < 100ms

### Reviewer Notes Template

Add to App Store Connect under "App Review Information":

```
GymBro is an AI-powered gym training app designed for serious strength 
athletes (powerlifters, Olympic lifters, bodybuilders). 

CORE FEATURES:
• Ultra-fast workout logging (1-tap set entry)
• Adaptive training programs (auto-adjusts based on performance)
• AI Coach (real-time training questions)
• Recovery insights (HealthKit integration)
• Progress tracking (charts, PRs, trends)

TO TEST:
1. Launch app → Sign in with Apple
2. Tap "Start Workout" → select a program
3. During workout: swipe up to log a set (completes with smart defaults)
4. Tap 💬 chat button for AI Coach
   - Try: "How long should I rest between sets?"
   - Notice: disclaimer before response
5. Check Recovery Score (Dashboard tab)
6. Premium: Use TestFlight sandbox credentials [provide here]

HEALTHKIT:
App reads resting heart rate, HRV, sleep data, and workouts to calculate
recovery readiness. No data is shared with third parties. User can revoke
access anytime in Settings → Health.

AI SAFETY:
All AI responses include "Not medical advice" disclaimer. App refuses to
provide medical diagnosis (try asking for medical advice—AI will decline).

OFFLINE:
Core logging works without internet. Cloud sync is background process.

ACCOUNT DELETION:
Settings → Profile → Delete Account. Removes all local data + cloud data.

Thank you for reviewing GymBro. Questions? Contact: tank@gymbro.app
```

---

## 10. Version & Release Management

### Version Numbering

**For v1.0.0:**
- Bundle Short Version String: `1.0.0`
- Bundle Version (Build): `1`

**For future updates:**
- v1.0.1: Bug fixes (Bundle Version → `2`)
- v1.1.0: Minor features (Short Version → `1.1.0`, Build → `1`)
- v2.0.0: Major features (Short Version → `2.0.0`, Build → `1`)

### Release Branch Strategy

```bash
# After approval
git checkout -b release/1.0.0
# Bump version numbers, finalize metadata
git commit -m "Release: v1.0.0"
git tag -a v1.0.0 -m "App Store submission v1.0.0"
git push origin release/1.0.0 && git push origin v1.0.0
```

### Changelog (CHANGELOG.md)

```markdown
# GymBro Releases

## [1.0.0] – 2026-04-15

### ✨ Features
- Ultra-fast 1-tap workout logging with smart defaults
- Adaptive training programs (auto-periodization)
- AI Coach with real-time training guidance
- Recovery readiness score (HRV, sleep, fatigue)
- Progress tracking and analytics
- Apple HealthKit integration
- CloudKit sync across devices
- Sign in with Apple

### 🛡️ Security
- End-to-end CloudKit sync (encrypted)
- Privacy-first architecture (no third-party data sharing)
- AI safety filters (medical advice refused)

### 📱 Compatibility
- iOS 17.0+
- iPhone 12–16 Pro Max
- iPad Air/Pro (11"+)

### 🐛 Known Limitations (MVP)
- Voice logging deferred to v1.1
- Apple Watch app in separate submission
- Form analysis (video) planned for v2.0
```

---

## 11. Submission Workflow

### Step 1: Final Testing in Xcode

```bash
# Build for generic iOS device
xcodebuild -scheme GymBro -configuration Release -derivedDataPath build
```

### Step 2: Upload to TestFlight

```bash
# Using Xcode Organizer or transporter
xcode-select --install  # if needed
xcrun altool --upload-app -f build.ipa \
  -t ios -u [apple-id] -p [password]
```

### Step 3: External Testing (Internal Testers First)

- Invite 5–10 internal testers
- Run full test flow:
  - [ ] Sign in
  - [ ] Log workout
  - [ ] Test AI Coach
  - [ ] Check recovery score
  - [ ] Restore subscription
  - [ ] Revoke HealthKit, verify app still works
  - [ ] Delete account
- Monitor crash logs, performance, battery drain

### Step 4: App Store Connect Metadata

- [ ] Complete all screenshots
- [ ] Fill in description, keywords, support URL
- [ ] Add reviewer notes
- [ ] Configure privacy questions
- [ ] Select release date (manual or automatic)
- [ ] Set pricing tier and subscription terms

### Step 5: Submit for Review

1. Go to **App Store Connect** → Your App → **Builds**
2. Select your TestFlight build
3. Click **Submit for Review**
4. Confirm answers to compliance questions
5. Submit

### Step 6: Monitor Review Status

- Check **App Store Connect** → **Activity** for review status
- Expect: 24–48 hours for first review
- If rejected: read feedback, fix, resubmit

---

## 12. Post-Submission

### If Rejected

Common rejection reasons for health/AI apps:

1. **Overly Broad HealthKit Requests:** Remove unused data types from entitlements
2. **Medical Claims:** Ensure no "diagnose," "treat," or "cure" language
3. **AI Not Clearly Labeled:** Add more prominent AI disclaimer
4. **Subscription Issues:** Verify Restore Purchases works
5. **Privacy Policy Vague:** Make specific statements about data handling

**Response:** Address feedback, bump build number, resubmit within 48 hours

### If Approved

1. **Tap "Release"** in App Store Connect (or schedule automatic)
2. **Monitor:** First 24h crash reports, user ratings, reviews
3. **Respond to Reviews:** Address feedback publicly
4. **Plan v1.1:** Collect user data, prioritize next features

### Post-Launch Support

- Monitor crash logs daily for first week
- Respond to user support emails within 24h
- Plan v1.1 (voice logging, Apple Watch)
- Collect analytics on feature usage
- Plan off-device ML model migration for v2.0

---

## 13. References & Links

### Documentation
- [Apple App Store Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)
- [HealthKit Framework Guide](https://developer.apple.com/documentation/healthkit/)
- [Privacy & Security Guidelines](https://developer.apple.com/privacy/)
- [StoreKit 2 Documentation](https://developer.apple.com/documentation/storekit)

### GymBro Team Resources
- `.squad/skills/apple-appstore-review/SKILL.md` — Review checklist and patterns
- `.squad/skills/healthkit/SKILL.md` — HealthKit best practices
- `.squad/skills/ai-prompt-safety/SKILL.md` — AI safety architecture
- `docs/TECHNICAL_APPROACH.md` — Architecture & privacy-first design
- `docs/PRODUCT_CONCEPT.md` — Feature list & positioning

### Key Contacts
- **App Review:** Apple Developer Relations (in-app during review)
- **GymBro Support:** tank@gymbro.app
- **Privacy Questions:** privacy@gymbro.app

---

## 14. Quick Reference: Configuration Checklist

| Item | Status | File | Notes |
|------|--------|------|-------|
| Privacy Labels | ⏳ Configure in App Store Connect | — | Data collection declared |
| Info.plist Keys | ⏳ Add to Xcode project | `GymBro/Info.plist` or Xcode settings | NSHealthShareUsageDescription, NSHealthUpdateUsageDescription |
| HealthKit Entitlement | ⏳ Enable in Xcode | `GymBro/GymBro.entitlements` | Add health share capability |
| AI Disclaimers | ✅ Implemented | `GymBroUI/Views/Coach/` | On chat screen + each response |
| Subscription Flow | ✅ Implemented | `GymBroUI/Views/Paywall/` | StoreKit 2 configured |
| Account Deletion | ⏳ Implement | `GymBroUI/Views/Settings/` | Delete account from Profile |
| Privacy Policy | ⏳ Draft | `docs/PRIVACY_POLICY.md` | Link in App Store metadata |
| Terms of Service | ⏳ Draft | `docs/TERMS_OF_SERVICE.md` | Link in App Store metadata |
| Screenshots | ⏳ Create | — | 5–10 for iOS, 1+ for iPad |
| App Icon | ⏳ Create | `GymBro/Assets.xcassets/AppIcon.appiconset/` | 1024x1024 + all sizes |
| Launch Screen | ⏳ Design | `GymBro/LaunchScreen.storyboard` or SwiftUI | Static, no spinner |
| Version 1.0.0 | ⏳ Finalize | Info.plist | Bump before submission |
| Changelog | ⏳ Create | `CHANGELOG.md` | Release notes for v1.0.0 |

---

**Last Section Complete.** Ready for App Store submission. Begin configuration in order: Info.plist → HealthKit entitlements → Privacy documents → Screenshots → App Store Connect metadata → Submit for review.
