# Polar Bear

**Phase-based heart rate training control with Polar H10 chest strap**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org/)
[![Android API 28+](https://img.shields.io/badge/Android-API%2028%2B-green.svg)](https://www.android.com/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![Status: Early Development](https://img.shields.io/badge/Status-Early%20Development-orange.svg)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Native Android app that connects directly via Bluetooth Low Energy (BLE) to the Polar H10 and enables reusable, phase-based zone training with live feedback. Designed as a functional replacement for missing functionality in Polar Beat/Flow when no compatible Polar watch is available.

## Features

### Core Functionality
- **Direct BLE connection** to Polar H10 via standard GATT Heart Rate Service
- **Heart rate zones** – customizable zones with name, color, and bpm range
- **Workout templates** – reusable phase-based training structures with:
  - Individual phases with duration and target zone
  - Repeat blocks (e.g., 6× [Load → Recovery])
  - Flat structure (no nested repeat blocks)
  - Phase/block reordering and template duplication
- **Live workout execution** with:
  - Real-time HR display, phase timer, and zone feedback
  - Visual, auditory, and vibration alerts on zone boundary violations
  - Manual phase skip or full repeat-block exit
  - Comprehensive workout overview with progress bar and expandable phase list
  - Screen stays on during active workout
- **Workout history** – local recording with HR time series; TCX export for external analysis (e.g., Garmin Connect, Golden Cheetah)
- **Zone snapshots** – historical zone definitions are immutable per session; later zone changes do not affect past records
- **Auto-reconnect** – remembers the last H10 and reconnects automatically on startup; reconnect status shown during active workouts
- **Sensor battery level** – displays H10 battery % when available via BLE Battery Service

### Integration
- **Dual tracking** – simultaneous operation with Polar Beat via H10's multi-connection capability
  - App connects for phase control and analysis
  - Beat continues to feed Polar Flow (no direct sync via app)
  - Both operate independently without interference

## Requirements

### Hardware
- Android device with Bluetooth Low Energy (BLE) support
- Polar H10 chest strap
- Optional: Polar Beat app for simultaneous Polar Flow integration

### Software
- **Android API Level:** 28+ (Android 9+)
- **Build Tool:** Gradle with Android Gradle Plugin
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Storage:** Room (SQLite)

## Installation

### Build & Install

**Prerequisites:**
- Android Studio installed
- Android SDK (API 28+)
- Target device with USB debugging enabled

**Build debug APK:**
```bash
./gradlew :app:assembleDebug
```

**Install on connected device:**
```bash
./gradlew :app:installDebug
```

Or manually:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** On first open in Android Studio, Gradle will download `gradle-wrapper.jar` and generate `gradlew`. Until then, terminal `./gradlew` commands will not work.

## Project Structure

```
polar-bear/
├── app/                                    # Android app module
│   ├── src/main/java/dev/bri/polarbear/
│   │   ├── ble/
│   │   │   ├── BleManager.kt             # BLE scan + GATT connection
│   │   │   ├── HrParser.kt               # HR Measurement parser (0x2A37)
│   │   │   └── BleUiState.kt             # UI state machine
│   │   └── ui/screen/
│   │       └── HrMonitorScreen.kt        # Main workout screen
│   └── build.gradle.kts
├── docs/
│   ├── requirements-spec.md               # Authoritative requirements
│   └── plan/                              # Implementation slices
├── gradle/                                # Gradle wrapper
├── build.gradle.kts
├── settings.gradle.kts
├── CLAUDE.md                              # Project memory for Claude Code
└── README.md                              # This file
```

## Architecture

### Key Design Decisions

- **No proprietary Polar SDK** – uses standard BLE Heart Rate Service (0x180D) for direct, lightweight heart rate access
- **Local-only storage** – all data (zones, templates, history) persists via Room SQLite; no cloud backend or account system
- **Immutable zone snapshots** – ensures historical sessions remain unchanged even after zone definitions are modified
- **Flat template structure** – phases and repeat blocks at a single level (no nested repeat blocks)
- **Fixed repeat counts** – repeat count is defined at template creation time, not dynamically adjusted during execution

### Data Model (Room)

- **Zones** – custom heart rate zones with color and bpm range
- **Templates** – workout structures with phases and repeat blocks
- **Sessions** – recorded workouts with timestamp, template reference, phase sequence, and HR time series
- **Session Zone Snapshots** – immutable zone definitions at time of recording

## Known Open Risks

1. **BLE multi-connection (highest priority)**  
   Whether the H10 reliably sends to both Polar Beat and this app simultaneously is unverified. Should be clarified in early testing (see requirements-spec.md section 8).

2. **Repeat-block early exit history**  
   F-5.5 requires tracking prematurely exited repeat blocks in history. Data model for this requires deliberate design at implementation time.

## Development

### Authoritative References

Refer to these files for architecture and requirements decisions:
- **`docs/requirements-spec.md`** – functional/non-functional requirements with IDs (e.g., F-3.2a)
- **`docs/plan/00-overview.md`** and **`docs/plan/0X-*.md`** – implementation roadmap as vertical slices with definition of done

### Working Approach

- Reference requirement IDs in commits/PRs (e.g., "F-4.7a: Implement block exit")
- Requirements take precedence over convenience; state unclear requirements explicitly rather than interpreting silently
- "Must" requirements are non-negotiable within each slice; "Should"/"Can" may be deferred but must be explicitly recorded

## Future Outlook

- Capture and storage of RR intervals for HRV metrics
- Trend analysis across multiple workouts (zone tolerance development)
- Template export/import between devices
- Flexible (non-fixed) repeat counts if current model proves inflexible

---

**Status:** v0.8.0 — all core slices (01–08) complete. Single `:app` module, package `dev.bri.polarbear`.

**Installation method:** Manual compilation and sideloading (not Play Store).

**Single-user solo project** – no team, no multi-user operation.
