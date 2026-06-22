# Requirements Specification

**Independent Android App for Phase-Based Heart Rate Training Control with Polar H10**

Version 0.1 - Draft

---

## 1. Background and Objectives

The existing Polar ecosystem (Polar Beat, Polar Flow) does not adequately support the use case of "self-defined phase training with live guidance" when no compatible Polar watch device is available. Polar Beat merely records heart rate data without phase structure, target zones, or alarms. Polar Flow does offer phase targets, but can only synchronize them to a compatible watch—not to a smartphone with direct chest strap connection.

The goal is to develop an independent, native Android application that connects directly via Bluetooth Low Energy (BLE) to the Polar H10 and enables reusable, phase-based zone training with live feedback—serving as a functional replacement for the missing functionality of Polar Beat/Flow in scenarios without a Polar watch.

Primary use case: structured cardio warm-up before strength training (e.g., Blue → Green → Blue, or Blue → 6× [Green/Blue] → Gray), with potential future expansion to other training forms.

Since the official Polar interface (AccessLink/API v4) is exclusively read-only for training data and does not allow automated uploading of custom sessions to Polar Flow (see section 6.2), integration with the Polar ecosystem is achieved through **dual tracking**: the H10 transmits simultaneously to Polar Beat (for standard Polar Flow integration) and to the new app (for phase control and analysis). Both recordings run independently.

---

## 2. Target Audience and Usage Context

- Single user, no multi-user operation required
- Usage in gym (Encompass Elevate) with Android smartphone and Polar H10 chest strap
- Primary training device: treadmill with incline (speed/incline are controlled manually, not by the app)
- Training control is data-driven via heart rate target zones calculated using the Karvonen formula

---

## 3. Scope

### 3.1 In Scope

- Direct BLE connection to Polar H10 (standard GATT Heart Rate Service)
- Management of customizable individual heart rate zones (name, color, bpm range)
- Creation, storage, and management of reusable workout templates with multiple phases, including repeat blocks
- Live workout execution with phase timer, live HR display, overall progress view, and zone feedback (visual, auditory, vibration)
- Local recording and review of past training sessions
- Simultaneous operation of Polar Beat and the new app via H10's multi-connection capability (dual tracking), ensuring Polar Flow data continues to be generated via Beat

### 3.2 Explicitly Out of Scope (Version 1)

- Automated upload or synchronization of training data to Polar Flow via the app itself—technically not possible via the official Polar API (see 6.2); Polar Flow integration occurs indirectly through parallel operation of Polar Beat (dual tracking)
- Cloud synchronization, multi-device sync, account system
- iOS support
- Recording/analysis of RR intervals and HRV metrics (planned for a later phase, see section 7)
- GPS/distance tracking, control of training equipment (e.g., treadmill control)
- Publication in Play Store; installation via manual compilation/sideloading

---

## 4. Functional Requirements

### 4.1 Sensor Connection (BLE)

| ID | Requirement | Priority |
|----|-------------|-----------|
| F-1.1 | The app can scan for Polar H10 devices within Bluetooth range and display a list of found devices. | Must |
| F-1.2 | The app connects to a selected H10 via standard BLE Heart Rate Service and receives HR value (bpm). | Must |
| F-1.3 | The app remembers the last connected device and automatically reconnects on next startup. | Should |
| F-1.4 | Upon connection loss during an active workout, the app automatically attempts reconnection and informs the user of the status. | Should |
| F-1.5 | The app displays the current battery/signal status of the sensor, if available via BLE. | May |
| F-1.6 | The app does not interfere with a parallel active connection from H10 to Polar Beat; both apps can be simultaneously connected and independently receive data (dual tracking, based on H10's multi-connection capability). | Must |
| F-1.7 | The app clearly indicates during connection establishment if a connection to H10 cannot be established, e.g., because two other devices are already connected. | Should |

### 4.2 Zone Management

| ID | Requirement | Priority |
|----|-------------|-----------|
| F-2.1 | The user can create, edit, and delete any number of HR zones, each with name, color, and bpm range (min/max). | Must |
| F-2.2 | The app provides a calculation helper using the Karvonen formula (input: resting HR and HRmax, output: zone boundaries per intensity percentage). | Should |
| F-2.3 | Zones are defined globally and available for phase creation, not redefined per workout. | Must |
| F-2.4 | When saving a completed session, the zone definition (name, color, bpm range) valid at that time is saved as an immutable snapshot with the session—not merely as a reference to the (possibly later modified) global zone. | Must |
| F-2.5 | Later changes to a zone (e.g., after updating resting HR/HRmax) only affect future sessions; already saved sessions remain unchanged with their historical zone snapshot and are not retroactively re-evaluated. | Must |

### 4.3 Workout Templates

| ID | Requirement | Priority |
|----|-------------|-----------|
| F-3.1 | The user can create a workout template with any number of phases. | Must |
| F-3.2 | Each phase has: name, duration (mm:ss), target zone (reference to defined zone). | Must |
| F-3.2a | In addition to individual phases, repeat blocks can be created: a block contains an ordered list of 2 or more individual phases and a **fixed** repeat count (e.g., 6×), set when creating the template. | Must |
| F-3.2b | A template consists of a sequence of individual phases and/or repeat blocks at one level (e.g., individual phase → repeat block → individual phase). Nesting repeat blocks within each other is not supported. | Must |
| F-3.2c | Example structure: Phase "Warm-up" (5 min, Blue zone) → Repeat block 6× [Phase "Load" (2 min, Green zone), Phase "Recovery" (2 min, Blue zone)] → Phase "Cool-down" (5 min, Gray zone). | Must |
| F-3.3 | Phases can be reordered via drag-and-drop or arrow keys. | Should |
| F-3.4 | Templates can be duplicated to create slight variations (e.g., longer main phase, different repeat count). | Should |
| F-3.5 | Templates are persistently stored locally (persistent across app restart and device restart). | Must |
| F-3.6 | The user can rename and delete templates. | Must |

### 4.4 Workout Execution

| ID | Requirement | Priority |
|----|-------------|-----------|
| F-4.1 | The user can select a saved template and start a workout from it. | Must |
| F-4.2 | During the workout, the app displays: current HR (bpm), current phase with name, remaining time for phase, target zone of current phase. | Must |
| F-4.2a | If the current phase is within a repeat block, the app additionally shows progress (e.g., "Repetition 3 of 6"). | Must |
| F-4.3 | Phase transition occurs automatically after configured duration expires. | Must |
| F-4.4 | Upon phase transition, clear feedback is provided (vibration and/or sound). | Must |
| F-4.5 | If current HR leaves the target zone of the running phase (too high/too low), an unobtrusive warning is triggered (visual, optionally vibration). | Must |
| F-4.6 | The workout can be paused, resumed, or ended early at any time. | Must |
| F-4.7 | The current phase can be manually skipped during execution, moving directly to the next phase (even if within the same repeat block). | **Must** |
| F-4.7a | If execution is within a repeat block, the **entire remaining block** can be exited early, moving directly to the phase after the block (e.g., exit after repetition 4 of 6 instead of skipping individually to the end of the block). Both actions (F-4.7 and F-4.7a) are separately selectable during a repeat block. | **Must** |
| F-4.8 | The screen remains on during an active workout (no automatic lock). | Must |
| F-4.9 | The app provides a workout overview that clearly shows the user's current position within the overall plan at any time (see 4.4.1). | **Must** |

#### 4.4.1 Workout Overview (Detail Requirement for F-4.9)

| ID | Requirement | Priority |
|----|-------------|-----------|
| F-4.9a | An overall progress bar shows the position in the complete workout (elapsed time/phases vs. total duration/phase count), independent of phase or block structure. | Must |
| F-4.9b | In addition to the progress bar, an **expandable list** of all phases and repeat blocks from the template is available in chronological order. | Must |
| F-4.9c | The currently active phase is distinctly highlighted in the list; completed phases are marked as done, pending phases as open. | Must |
| F-4.9d | A repeat block is displayed in the list as a single consolidated entry (not as 6 individual entries), which can be expanded to view the contained phases and current repeat progress. | Must |
| F-4.9e | The overview is accessible throughout execution without interrupting the active workout or affecting the timer. | Must |

### 4.5 Recording and History

| ID | Requirement                                                                                                                           | Priority |
|----|---------------------------------------------------------------------------------------------------------------------------------------|-----------|
| F-5.1 | Each completed workout is saved locally: date, template used, phase sequence, HR time series.                                         | Must |
| F-5.2 | The user can view past workouts in a list.                                                                                            | Should |
| F-5.3 | For a past workout, a simple HR progression graph can be displayed (time vs. bpm, with zone color background).                        | Should |
| F-5.4 | The user can export a past workout (e.g., TCX) for external processing.                                                               | May |
| F-5.5 | If a phase or repeat block was exited early (F-4.7/F-4.7a), this is noted in the saved record (e.g., "4 of 6 repetitions completed"). | Should |

---

## 5. Non-Functional Requirements

| ID | Requirement | Priority |
|----|-------------|-----------|
| N-1 | The app runs as a native Android application (Kotlin), target API level corresponds to current Android versions. | Must |
| N-2 | All data (zones, templates, history) is stored exclusively locally on the device; no cloud dependency for the new app itself. The separate, parallel cloud integration via Polar Beat (dual tracking, F-1.6) remains unaffected and is outside the new app. | Must |
| N-3 | The app functions completely without internet connection (pure BLE and local storage functionality). | Must |
| N-4 | Live display during workout is easily readable at 1-2 meters distance (e.g., at treadmill display holder) with large font and high contrast. | Should |
| N-5 | Response time from HR update to display is noticeably under 1 second. | Should |
| N-6 | The app is robust against brief BLE interruptions such that an active workout does not abort but retains the last value and reconnects. | Must |
| N-7 | The workout overview (F-4.9) remains clear and performant even with templates containing many phases/repetitions (no noticeable stutter when expanding the list). | Should |

---

## 6. Technical Framework

- Platform: Android, native, Kotlin + Jetpack Compose (UI)
- Sensor connection: Bluetooth Low Energy, standard GATT Heart Rate Service (0x180D)—no proprietary Polar SDK required for pure HR values
- Local data storage: Room (SQLite) for zones, templates, and workout history
- Build/Installation: Android Studio, manual compilation and installation on target device (no Play Store distribution in V1)
- RR interval capture (HRV) is architecturally prepared via a separate BLE service but not evaluated in V1 (see section 7)

### 6.2 Polar Ecosystem Integration: Technical Context

The official Polar interfaces (AccessLink API v3, AccessLink Dynamic API v4) are consistently designed as read-only interfaces for training data. There is no official, stable method for a third-party app to actively upload a completed training session to Polar Flow. Although v4 API allows managing training goals/favorites (for transfer to a compatible Polar watch), this is not equivalent to uploading results from a third-party device.

Consequence for this specification: Integration with Polar Flow is not via the new app itself but remains via Polar Beat, which connects in parallel to the new app (dual tracking, see F-1.6). The new app is responsible for phase control and its own analysis, while Polar Flow continues to be fed via Beat—without manual export/import, but also without linking data between the two systems.

---

## 7. Future Outlook (Later Phases, Not Part of V1)

- Capture and storage of RR intervals for HRV metrics
- Trend analysis across multiple workouts (e.g., Zone 2 tolerance development)
- Possible export/import of templates between devices
- Possible integration of additional sensor metrics as relevant
- Flexible (non-fixed) repeat counts if the fixed approach from V1 proves too inflexible in practice

---

## 8. Open Questions / To Be Clarified

- Exact definition of initial standard zones (pre-populated on first installation)—proposal: adoption of existing values (resting HR 62, HRmax 179)
- Technical verification of how many simultaneous BLE connections the H10 reliably supports and whether practical constraints arise with Polar Beat + new app in parallel (similar to Milon coupling already observed in the existing Polar ecosystem)
- Scope of history visualization (graph) for V1 vs. later phases
- Exact visual design of the workout overview (F-4.9): e.g., whether the progress bar is permanently visible or shown on demand
