# Maestro E2E Testing

GymBro uses [Maestro](https://maestro.mobile.dev/) for declarative, YAML-based end-to-end UI testing on Android.

## Prerequisites

- **Android Emulator** running (API 29+)
- **Maestro CLI** installed (v2.4.0+)
- **Java 17+** (Android Studio's bundled JBR works)
- **ADB** on PATH with a connected device/emulator

## Installing Maestro

### Windows (PowerShell)

Download and extract from GitHub releases:

```powershell
$maestroDir = "$env:USERPROFILE\.maestro"
New-Item -ItemType Directory -Force -Path $maestroDir | Out-Null
Invoke-WebRequest -Uri "https://github.com/mobile-dev-inc/Maestro/releases/latest/download/maestro.zip" -OutFile "$maestroDir\maestro.zip" -UseBasicParsing
Expand-Archive -Path "$maestroDir\maestro.zip" -DestinationPath $maestroDir -Force
Remove-Item "$maestroDir\maestro.zip"
```

Add to PATH for current session:

```powershell
$env:PATH = "$env:USERPROFILE\.maestro\maestro\bin;$env:PATH"
```

To persist across sessions, add `%USERPROFILE%\.maestro\maestro\bin` to your system PATH.

### macOS / Linux

```bash
curl -fsSL "https://get.maestro.mobile.dev" | bash
```

### Verify Installation

```bash
maestro --version
```

## Project Structure

```
android/.maestro/
├── config.yaml          # Global config (appId)
└── smoke-test.yaml      # Basic launch + visibility test
```

## Running Tests

### 1. Start the emulator

```bash
# Via Android Studio or command line
emulator -avd <your_avd_name>
```

### 2. Build and install the app

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd android
.\gradlew.bat installDebug
```

### 3. Run a single flow

```bash
maestro test android/.maestro/smoke-test.yaml
```

### 4. Run all flows in the directory

```bash
maestro test android/.maestro/
```

## Writing New Flows

Maestro flows are YAML files. Each flow starts with metadata (appId) followed by a sequence of commands.

### Example: Navigation Test

```yaml
appId: com.gymbro.app
---
- launchApp:
    clearState: true
- assertVisible: "GymBro"
- tapOn: "Get Started"
- assertVisible: "Dashboard"
```

### Common Commands

| Command | Description |
|---------|-------------|
| `launchApp` | Launch the app (with optional `clearState`) |
| `assertVisible` | Assert text/element is visible |
| `tapOn` | Tap on text or element |
| `inputText` | Type text into a focused field |
| `scrollDown` | Scroll down |
| `swipe` | Swipe in a direction |
| `waitForAnimationToEnd` | Wait for animations |
| `back` | Press Android back button |
| `takeScreenshot` | Capture screenshot |

### Full reference: [Maestro Commands](https://maestro.mobile.dev/api-reference/commands)

## CI Integration

Maestro can run in CI with a headless Android emulator. Example GitHub Actions step:

```yaml
- name: Run Maestro E2E tests
  run: |
    maestro test android/.maestro/ --no-ansi
```

> **Note:** CI requires a running Android emulator. Use `reactivecircus/android-emulator-runner` action to set one up.

## Troubleshooting

- **"No devices found"**: Ensure `adb devices` shows a connected device/emulator.
- **Test timeout**: Increase timeout with `maestro test --timeout 120000 <flow>`.
- **App not installed**: Run `gradlew installDebug` before testing.
- **Flaky tests**: Add `waitForAnimationToEnd` or explicit waits between steps.
