# GymBro 💪

[![CI](https://github.com/jperezdelreal/GymBro/actions/workflows/ci.yml/badge.svg)](https://github.com/jperezdelreal/GymBro/actions/workflows/ci.yml)
[![Swift](https://img.shields.io/badge/Swift-6.0-orange.svg)](https://swift.org)
[![Platform](https://img.shields.io/badge/Platform-iOS%2018.0+-lightgrey.svg)](https://www.apple.com/ios/)
[![License](https://img.shields.io/badge/License-Private-red.svg)](LICENSE)

> **AI-first workout companion for serious strength athletes**

GymBro is an iOS app built for powerlifters, Olympic lifters, and bodybuilders who demand more than basic fitness tracking. Combining ultra-fast logging with adaptive AI coaching, GymBro learns your training patterns and helps you break plateaus intelligently.

## 🎯 Core Features

- **⚡ Ultra-Fast Logging**: 1-tap set recording with smart defaults
- **🤖 AI Coach**: Natural language coaching with transparent reasoning
- **📊 Adaptive Training**: Auto-periodization and intelligent deload recommendations
- **📈 Progress Intelligence**: Plateau detection, trend analysis, PR tracking
- **🔒 Privacy-First**: On-device ML with optional cloud fallback

## 🏗️ Tech Stack

- **Platform**: iOS 18.0+, iPadOS 18.0+
- **Language**: Swift 6.0
- **UI**: SwiftUI
- **Frameworks**: HealthKit, Core ML, CloudKit, WidgetKit
- **Architecture**: MVVM with Coordinator pattern
- **Testing**: XCTest, XCUITest

## 🚀 Development Status

**Current Phase**: Phase 0 — Foundation & Infrastructure  
**Target**: MVP v1.0 — Q2 2026

This project is built by an autonomous AI squad. See [.squad/](.squad/) for team details.

## 📦 Setup

```bash
# Clone the repository
git clone https://github.com/jperezdelreal/GymBro.git
cd GymBro

# Open in Xcode
open GymBro.xcodeproj

# Or use Xcode Cloud / GitHub Actions for CI
```

## 🧪 Testing

```bash
# Run tests via xcodebuild
xcodebuild test \
  -scheme GymBro \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro'

# Run SwiftLint
swiftlint lint
```

## 📋 Contributing

This is a private project built by an AI development squad. For issues and feature requests, see the [GitHub Issues](https://github.com/jperezdelreal/GymBro/issues) page.

## 📄 License

Private — All Rights Reserved

---

**Built with ❤️ by the Squad** • [Documentation](docs/) • [Technical Approach](docs/TECHNICAL_APPROACH.md)
