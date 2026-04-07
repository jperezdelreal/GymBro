---
name: "apple-appstore-review"
description: "App Store Review Guidelines compliance for GymBro iOS app"
domain: "ios-compliance"
confidence: "high"
source: "adapted from github/awesome-copilot (apple-appstore-reviewer skill)"
---

## Context

GymBro is an iOS app that will be submitted to the Apple App Store. The app uses HealthKit, AI-generated content (training advice), in-app purchases, and Sign in with Apple. Each of these triggers specific App Store Review Guidelines that must be followed to avoid rejection.

## GymBro-Specific Review Risks

### P0 — Likely Rejection If Missed

| Area | Risk | Mitigation |
|------|------|------------|
| HealthKit | Missing `NSHealthShareUsageDescription` / `NSHealthUpdateUsageDescription` | Add clear purpose strings explaining HRV, sleep, workout data usage |
| AI Content | Training advice interpreted as medical advice | Disclaimers on all AI responses: "Not medical advice" |
| IAP | Missing restore purchases button | Implement and test restore flow prominently |
| IAP | Paywall blocking basic features | Free tier must be genuinely useful (unlimited logging) |
| Privacy | Missing Privacy Manifest (`PrivacyInfo.xcprivacy`) | Declare all data collection, required APIs, tracking domains |

### P1 — Common Friction Points

| Area | Risk | Mitigation |
|------|------|------------|
| Sign in with Apple | Required since we use account-based features | Already our sole auth method ✓ |
| Account Deletion | Required for any app with account creation | Implement in-app account deletion flow |
| Subscription Clarity | Price, duration, renewal terms must be visible | Show "$14.99/month, auto-renews" before paywall |
| HealthKit Entitlement | Must only request data types actually used | Request only: workouts, HRV, sleep, resting HR |

## Patterns

### Permission Usage Strings

```xml
<!-- Info.plist — be specific about why -->
<key>NSHealthShareUsageDescription</key>
<string>GymBro reads your workout history, heart rate variability, sleep data, and resting heart rate to personalize your training program and calculate recovery readiness.</string>

<key>NSHealthUpdateUsageDescription</key>
<string>GymBro saves your logged workouts to Apple Health so your training data stays in sync across all your health apps.</string>

<key>NSCameraUsageDescription</key>
<string>GymBro uses your camera to scan exercise equipment barcodes for quick workout setup.</string>
```

### AI Content Disclaimers

- Every AI coach response includes: "This is AI-generated training guidance, not medical advice."
- Programs page shows: "Consult a qualified professional before starting any new exercise program."
- Settings page links to full disclaimer and terms of service

### Subscription Flow

1. Show features comparison (Free vs Premium) before paywall
2. Display price, billing period, and renewal terms clearly
3. Provide "Restore Purchases" button on subscription screen
4. Link to Terms of Service and Privacy Policy
5. StoreKit 2 handles all transactions (no server-side receipt validation for MVP)

### Pre-Submission Checklist

- [ ] All `NS*UsageDescription` strings are specific and non-generic
- [ ] `PrivacyInfo.xcprivacy` declares all collected data types
- [ ] Privacy Policy URL accessible and accurate
- [ ] Terms of Service URL accessible
- [ ] Restore Purchases works
- [ ] Account deletion flow works
- [ ] AI disclaimers appear on all coach responses
- [ ] App works without network (core logging features)
- [ ] Demo/reviewer notes prepared in App Store Connect
- [ ] Screenshots match actual app UI

### Reviewer Notes Template

```
GymBro is a workout logging and AI coaching app for experienced lifters.

To test core features:
1. Launch app → Sign in with Apple
2. Tap "Start Workout" → select any program
3. Log sets by swiping up (completes set with smart defaults)
4. Access AI Coach via floating 💬 button during workout

Premium features (AI Coach unlimited):
- Use sandbox test account: [provided in App Store Connect]

No special hardware required. The app works offline for all logging features.
```

## Anti-Patterns

- ❌ Generic permission strings ("GymBro needs access to Health")
- ❌ AI responses without safety disclaimers
- ❌ Hiding restore purchases in deep settings menu
- ❌ Requiring login before showing any app value
- ❌ Claiming the app provides medical or nutritional diagnosis
- ❌ Requesting HealthKit data types not used by the app
