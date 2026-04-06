# GymBro

AI-powered workout tracking and coaching app for serious strength athletes.

## Overview

GymBro combines ultra-fast workout logging, intelligent training adaptation, and conversational AI coaching to deliver the training app that advanced lifters deserve.

**Target Users:** Powerlifters, Olympic weightlifters, bodybuilders with 2+ years of training experience.

**Core Features:**
- 1-tap set logging with smart defaults
- Adaptive periodization and autoregulation
- AI coach chat with transparent reasoning
- Progress tracking and plateau detection
- Offline-first architecture

## Tech Stack

- **Platform:** iOS 17+
- **Language:** Swift 6
- **UI Framework:** SwiftUI
- **Data:** SwiftData + CloudKit
- **Package Manager:** Swift Package Manager
- **Architecture:** MVVM with modular SPM packages

## Project Structure

```
GymBro/
├── Package.swift                 # Main app package
├── GymBro/                       # App target
│   ├── GymBroApp.swift          # App entry point
│   └── ContentView.swift        # Tab navigation
└── Packages/
    ├── GymBroCore/              # Models, business logic
    │   ├── Sources/Models/      # SwiftData models
    │   └── Tests/
    ├── GymBroUI/                # Views, ViewModels
    │   └── Sources/
    └── GymBroKit/               # Shared utilities
        └── Sources/
```

## Build Instructions

### Requirements
- Xcode 16.0 or later
- macOS 14.0 or later
- iOS 17.0+ deployment target

### Building

1. Clone the repository:
```bash
git clone https://github.com/jperezdelreal/GymBro.git
cd GymBro
```

2. Open in Xcode:
```bash
open Package.swift
```

3. Select the GymBro scheme and an iOS simulator or device

4. Build and run: `⌘R`

### Running Tests

Run tests from Xcode (`⌘U`) or via command line:
```bash
swift test
```

## Development Setup

The project uses Swift Package Manager for all dependencies. No additional setup needed.

### Package Dependencies
- **GymBroCore**: Data models, services, business logic
- **GymBroUI**: SwiftUI views and view models
- **GymBroKit**: Shared utilities and extensions

## Architecture

- **MVVM Pattern**: Clear separation between UI and business logic
- **Offline-First**: SwiftData as source of truth, CloudKit for sync
- **Modular Design**: SPM packages for clean boundaries
- **Performance Budget**: <1s cold launch, <100ms per set log

## Contributing

This is a private project under active development. See `.squad/` directory for team structure and decisions.

## License

Proprietary - All rights reserved.
