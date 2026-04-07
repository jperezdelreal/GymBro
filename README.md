# GymBro 💪

[![CI](https://github.com/jperezdelreal/GymBro/actions/workflows/ci.yml/badge.svg)](https://github.com/jperezdelreal/GymBro/actions/workflows/ci.yml)
[![Swift](https://img.shields.io/badge/Swift-6.0-orange.svg)](https://swift.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-iOS%20%7C%20Android-lightgrey.svg)]()
[![License](https://img.shields.io/badge/License-Private-red.svg)](LICENSE)

> **AI-first workout companion for serious strength athletes — iOS & Android**

GymBro is a dual-platform app built for powerlifters, Olympic lifters, and bodybuilders who demand more than basic fitness tracking. Combining ultra-fast logging with adaptive AI coaching, GymBro learns your training patterns and helps you break plateaus intelligently.

## 🎯 Core Features

- **⚡ Ultra-Fast Logging**: 1-tap set recording with smart defaults
- **🤖 AI Coach**: Natural language coaching with transparent reasoning
- **📊 Adaptive Training**: Auto-periodization and intelligent deload recommendations
- **📈 Progress Intelligence**: Plateau detection, trend analysis, PR tracking
- **🔒 Privacy-First**: On-device ML with optional cloud fallback

## 📁 Repository Structure

```
GymBro/
├── ios/                  # iOS app (Swift, SwiftUI)
│   ├── GymBro/           # Main iOS app target
│   ├── GymBroWatch/      # Apple Watch companion
│   ├── GymBroWidgets/    # iOS widgets
│   ├── Packages/         # Swift packages (Core, UI, Kit)
│   └── Package.swift     # SPM manifest
├── android/              # Android app (Kotlin, Jetpack Compose) — coming soon
├── shared/               # Platform-agnostic shared resources
│   └── data/             # Seed data, shared configs
├── docs/                 # Documentation
├── .squad/               # AI squad team config
└── .github/              # CI/CD workflows
```

## 🏗️ Tech Stack

### iOS
- **Platform**: iOS 18.0+, iPadOS 18.0+
- **Language**: Swift 6.0
- **UI**: SwiftUI
- **Frameworks**: HealthKit, Core ML, CloudKit, WidgetKit
- **Architecture**: MVVM with Coordinator pattern

### Android (planned)
- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVI (Model-View-Intent)
- **Local DB**: Room
- **DI**: Hilt

## 🚀 Development Status

**Current Phase**: Phase 0 — Foundation & Infrastructure
**Target**: MVP v1.0 — Q2 2026

This project is built by an autonomous AI squad. See [.squad/](.squad/) for team details.

## 📦 Setup

### iOS
```bash
git clone https://github.com/jperezdelreal/GymBro.git
cd GymBro/ios
open Package.swift   # Opens in Xcode
```

### Android
```bash
cd GymBro/android
# Coming soon — see android/README.md
```

## 🧪 Testing

```bash
# iOS — run tests via xcodebuild
cd ios
xcodebuild test \
  -scheme GymBro \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro'

# SwiftLint (from repo root)
swiftlint lint
```

## 📋 Contributing

This is a private project built by an AI development squad. For issues and feature requests, see the [GitHub Issues](https://github.com/jperezdelreal/GymBro/issues) page.

## 📄 License

Private — All Rights Reserved

---

**Built with ❤️ by the Squad** • [Documentation](docs/) • [Technical Approach](docs/TECHNICAL_APPROACH.md)
