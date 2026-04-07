# Decision: Firebase Firestore for Android Cloud Sync

**By:** Tank (Backend Dev)  
**Date:** 2026-07-16  
**Issue:** #143

## Context

GymBro Android needs cloud sync equivalent to iOS CloudKit. Firebase Firestore was chosen for the Android implementation.

## Decisions

### 1. Firebase is optional at build time
The google-services plugin is conditionally applied only when `google-services.json` exists. This means:
- CI and developers can build without a Firebase project
- `BuildConfig.FIREBASE_ENABLED` flag allows runtime checks
- No fake config files that could silently break things

### 2. Last-write-wins conflict resolution (MVP)
For MVP, conflicts are resolved by timestamp — the latest `updatedAt` wins. This is simple and works well for single-user scenarios. Property-level merging can be added post-MVP.

### 3. Anonymous auth for MVP
Users can sign in anonymously to enable cloud backup without friction. Email/Google sign-in upgrade path is built into the AuthService interface.

### 4. Denormalized Firestore documents
Workouts embed their sets as a nested list rather than separate subcollections. This trades write efficiency for read speed — one document fetch gets the full workout.

### 5. Offline-first sync queue
OfflineSyncManager monitors connectivity and queues operations when offline. Changes flush automatically when the device reconnects. Room remains the source of truth.

## Implications for team

- **Trinity (UI):** ProfileScreen follows existing MVI pattern with AccentGreen theme
- **Switch (QA):** Firebase tests will need mock/fake implementations of AuthService and CloudSyncService
- **Morpheus (Architecture):** CloudSyncService interface is platform-agnostic — could be shared with iOS via KMP later
- **Neo (AI):** No impact on AI features yet, but user profiles could carry AI preferences in the future
