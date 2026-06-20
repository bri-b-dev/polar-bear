# CLAUDE.md

Project memory for Claude Code. Applies to every session in this repo.

## Project Overview

Native Android app for phase-based heart rate training control with the Polar H10 chest strap. Solo project, no team, no Play Store release — installation is performed by manual compilation and sideloading to a single target device.

Authoritative sources, do not duplicate in this file:
- **Requirements:** `docs/requirements-spec.md` — requirement IDs (e.g. F-3.2a) are the authoritative reference. When in doubt, check there rather than guessing.
- **Implementation order:** `docs/plan/00-overview.md` and `docs/plan/0X-*.md` — vertical slices with definition of done. New work fits into these slices; if a slice no longer fits, update the plan document instead of working around the plan.

## Project Status

**Slice 01 (BLE Spike) scaffold complete.** Single `:app` module, package `dev.bri.polarphases`.

### Build & Install

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Install on connected device (USB debugging on)
./gradlew :app:installDebug

# Or via adb after assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **First open in Android Studio:** AS will generate/download `gradle-wrapper.jar` and `gradlew`
> on the first Gradle sync. Until then, `./gradlew` won't work from the terminal.

### Key files

| What | Path |
|------|------|
| BLE scan + GATT connection | `app/src/main/java/dev/bri/polarphases/ble/BleManager.kt` |
| HR Measurement parser (0x2A37) | `app/src/main/java/dev/bri/polarphases/ble/HrParser.kt` |
| UI state machine | `app/src/main/java/dev/bri/polarphases/ble/BleUiState.kt` |
| Single screen (scan → connect → live bpm) | `app/src/main/java/dev/bri/polarphases/ui/screen/HrMonitorScreen.kt` |

## Architecture Guidelines

From the requirements document, apply to the entire implementation:

- **Platform:** Kotlin + Jetpack Compose, native Android. No cross-platform framework.
- **Sensor connection:** Standard BLE-GATT Heart Rate Service (0x180D). No proprietary Polar SDK for raw heart rate values — deliberate decision, see requirements document section 6.
- **Data persistence:** Room (SQLite), exclusively local on the device. No cloud backend, no account system, no sync logic (N-2, N-3). This constraint applies even if a cloud solution appears "attractive" later — this is a deliberate decision, not an oversight, and should not be silently relaxed.
- **Dual tracking:** The app connects to the H10 in parallel without disrupting a simultaneously active Polar Beat connection (F-1.6). Polar Flow integration deliberately continues via Beat, not through this app — see requirements document 6.2 for the rationale (Polar API is read-only for training data, no upload path available).
- **Workout structure:** Templates consist of individual phases and/or repeat blocks at a single level. No nesting of repeat blocks within each other (F-3.2b). Repeat count is fixed at creation time, not variable at runtime (F-3.2a).
- **Zone snapshot:** Each saved session freezes the zone definition valid at that time (F-2.4/F-2.5). Later zone changes must never retroactively alter old sessions — this is a hard requirement, not a detail.

## Known Open Risks

- **BLE multi-connection (highest priority):** Whether the H10 can reliably send to both Polar Beat and this app simultaneously is unverified at planning time (see requirements document section 8, open point). This should be clarified in one of the first slices. Once verified: record the result here (verified on [date], behavior: ...) instead of just referencing the open point.
- **Repeat-block early exit in history:** F-5.5 requires that prematurely exited repeat blocks remain traceable in the workout history. The data model for this is not yet defined — make a deliberate decision at the corresponding slice, do not solve it incidentally.

## Working Approach in This Repo

- Reference requirement IDs from the requirements document in commits/PR descriptions where appropriate (e.g. "F-4.7a: Implement block exit"), so the alignment with the requirements document remains traceable at all times.
- When uncertain between "as described in the requirements document" and "what feels more natural when implementing": the requirements document takes precedence. If the requirements document is truly unclear or incomplete at that point, state it explicitly rather than interpreting silently — same rule as when creating the plan.
- "Must" requirements are non-negotiable within the scope of each slice. "Should"/"Can" requirements may be deferred if a slice would otherwise become too large — but record this explicitly, do not silently omit them.
