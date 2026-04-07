# GymBro Android

> Placeholder for the Android app — Kotlin + Jetpack Compose

This directory will contain the Android implementation of GymBro. See [Issue #133](https://github.com/jperezdelreal/GymBro/issues/133) for the dual-platform migration plan.

## Planned Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVI (Model-View-Intent)
- **Local DB:** Room
- **DI:** Hilt
- **Cloud Sync:** Firebase Firestore
- **Auth:** Firebase Auth (anonymous for MVP)

## Firebase Setup

To enable cloud sync, you need a Firebase project:

1. Create a project at [Firebase Console](https://console.firebase.google.com)
2. Add an Android app with package name `com.gymbro.app`
3. Download `google-services.json` and place it in `android/app/`
4. The build automatically detects the file and enables Firebase

**Without `google-services.json`:** The app compiles and runs normally — all Firebase features are gracefully disabled. The `FIREBASE_ENABLED` BuildConfig flag is `false`.
