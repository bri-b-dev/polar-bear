# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- (Placeholder for next features)

### Changed
- (Placeholder for changes)

### Fixed
- (Placeholder for fixes)

## [0.8.2] - 2026-07-14

### Fixed
- Workout timer no longer stops when the app is moved to the background (e.g. while watching Netflix). An active workout now runs in a foreground service with a wake lock, so phase transitions, vibration feedback and out-of-zone alerts keep working with the screen off or another app in front.
- Phase countdown is now wall-clock based: if the process is briefly throttled, the elapsed time is caught up instead of lost.

## [0.8.1] - 2026-06-22

### Changed
- HR zone seed calculation switched from Karvonen (HRR-based) to straight HRmax percentages: Recovery 50–60%, Endurance 60–70%, Tempo 70–80%, Threshold 80–90%, Peak 90–100%
- Zone seed dialog no longer requires resting HR — only Max HR is needed
- "Karvonen" button on the HR Zones screen relabelled to "% HRmax"

## [0.8.0] - 2026-06-22

### Added
- App remembers the last connected H10 and auto-reconnects on startup — no manual scan needed if the strap was connected before (F-1.3)
- Reconnect status banner shown during active workouts when the sensor connection drops and is being recovered (F-1.4)
- Battery level of the H10 displayed on the monitor screen when available via BLE Battery Service (0x180F) (F-1.5)
- Phase/block reordering in the template builder via up/down arrow buttons (F-3.3)
- Template duplication from the template list — creates a copy with "(copy)" appended to the name (F-3.4)
- TCX export for past workout sessions: "Export TCX" button on the session detail screen shares a standard TCX file with heart rate time series (F-5.4)

## [0.7.2] - 2026-06-22

### Changed
- Project renamed from "Polar Phases" to "Polar Bear" (branding refresh aligning with Polar Beat naming convention)
- Package name changed from `dev.bri.polarphases` to `dev.bri.polarbear`
- App class renamed from `PolarPhasesApp` to `PolarBearApp`
- All theme and resource references updated accordingly

## [0.7.0] - 2026-06-21

### Added
- Workout sessions are saved locally on completion or early exit: date, template name, HR time series, and phase sequence (F-5.1)
- Zone definitions are frozen as an immutable snapshot at save time — editing zones after a session never alters its historical record (F-2.4, F-2.5)
- Early exits are noted in the record; repeat blocks that were exited before all reps completed show "X of Y repetitions completed" in the session detail view (F-5.5)
- Workout history list accessible via "History" button on the Templates screen, showing all past sessions with date, duration, phase count, and early-exit indicator (F-5.2)
- Session detail view with a zone-colored HR graph (time vs. BPM, zone bands as background) and zone legend (F-5.3)
- Room database migrated from version 3 to 4: adds `workout_sessions`, `zone_snapshots`, `hr_samples`, and `session_phase_records` tables with cascade-delete foreign keys

## [0.6.0] - 2026-06-21

### Added
- Workout overview sheet: tap "Overview" in the top bar at any time during a workout to see your position in the full plan (F-4.9)
- Overall time-based progress bar in the overview showing elapsed vs. total planned duration, independent of phase/block structure (F-4.9a)
- Chronological phase list in the overview with status indicators — ▶ active, ✓ done, ○ pending — and zone color dots (F-4.9b, F-4.9c)
- Repeat blocks appear as a single consolidated row showing current rep progress; tap the row to expand and see the individual phases within the block (F-4.9d)
- Opening and closing the overview never affects the workout timer or phase countdown (F-4.9e)
- Phase list uses LazyColumn for smooth scrolling on long templates (N-7)

## [0.5.0] - 2026-06-21

### Added
- Skip phase button: manually advance to the next phase at any time during a workout, including skipping to the next repetition inside a repeat block (F-4.7)
- Exit block button: when inside a repeat block, jump directly to the phase after the entire block, skipping all remaining repetitions (F-4.7a)
- Both Skip and Exit Block controls are shown simultaneously while inside a block and are distinct, clearly labelled actions

## [0.4.1] - 2026-06-21

### Changed
- BPM number colored by the zone the current HR falls into (from all defined zones), not by a fixed compliance color
- Phase name and countdown timer colored by the target zone of the current phase
- Out-of-zone signal (double vibration + beep) fires only once when the user leaves the target zone, and only if they were inside it first; starting a phase already outside the target zone does not signal
- No live HR reading shows "--" instead of the last measured value; stale BPM is never displayed

## [0.4.0] - 2026-06-21

### Added
- Workout execution screen: start a workout from any saved template via the Play button on the template list (F-4.1)
- Live display of current BPM (large font), current phase name, and MM:SS countdown timer (F-4.2)
- Repeat-block display shows "Repetition X of Y" during block phases (F-4.2a)
- Phases auto-advance when their countdown reaches zero (F-4.3)
- Vibration (350 ms) + audible tone on every phase transition (F-4.4)
- Zone compliance indicator: BPM colored green (in zone), red (too high), or orange (too low) with a directional text badge (F-4.5)
- Pause / Resume button during active workout (F-4.6)
- Screen stays on for the entire active workout via `FLAG_KEEP_SCREEN_ON` (F-4.8)
- BLE reconnect resilience: accidental strap drop keeps workout timer running on last-known BPM; GATT reconnects automatically and resumes live readings (N-6)
- Readable at 1–2 m: BPM at 80 sp, timer at 64 sp, high-contrast compliance colors (N-4)

### Changed
- `BleViewModel` is now Activity-scoped so the BLE connection persists when navigating from the monitor screen into a workout
- `BleUiState` gains a `Reconnecting` state to distinguish accidental drops from intentional disconnects

## [0.3.1] - 2026-06-20

### Added
- Edit existing phases and blocks in-place (edit icon on each card, dialog pre-filled with current values)
- Multi-zone support per phase: select multiple HR zones (e.g. Gray + Blue for a cooldown)
- Editing existing templates: edit icon on template list opens builder with all content pre-loaded; save renames and replaces sequence atomically
- Editing blocks: change repeat count, add/remove phases within a block via the block edit dialog

### Changed
- Default phase duration changed from 0:30 to 5:00
- Template list row: rename dialog replaced by full-content edit (name editable at top of builder)
- Database migrated from version 2 to 3: `zoneId INTEGER` column replaced by `zoneIds TEXT` (comma-separated) in both `template_sequence_items` and `block_phases`; existing single-zone data preserved

## [0.3.0] - 2026-06-20

### Added
- Workout template builder with individual phases (name, mm:ss duration, HR zone) (F-3.1, F-3.2)
- Repeat block support: ordered list of ≥2 phases with a fixed repeat count set at creation (F-3.2a, F-3.2b, F-3.2c)
- Flat, one-level sequence model — repeat blocks cannot be nested (F-3.2b)
- Template list screen with rename and delete (F-3.6)
- Room schema for templates: `workout_templates`, `template_sequence_items`, `block_phases` tables with cascade-delete foreign keys
- Database migration from version 1 to 2 (existing HR zone data preserved)
- Navigation from main screen to Templates and from Templates to the builder

## [0.2.2] - 2026-06-20

### Changed
- Adjusted HR Zones to polar-modeled zones

## [0.2.0] - 2026-06-20

### Added
- HR Zone management screen with full CRUD operations (create, edit, delete zones)
- Karvonen formula calculator for automated zone seed generation
- Room database persistence layer with HrZone entity and DAO
- Zone repository pattern for data access abstraction
- ZoneViewModel for state management of zone operations and dialogs
- Zone form validation with name, BPM range (min/max), and color picker
- 10-color palette for zone customization and visual distinction
- App-level dependency injection scaffolding with PolarPhasesApp class

### Changed
- App structure refactored to support multi-screen navigation with MainActivity routes
- Updated Gradle build configuration to support Room database and data persistence

## [0.1.0] - 2026-06-20

### Added
- BLE scan and discovery of Polar H10 chest strap
- GATT connection and HR Measurement characteristic (0x2A37) parsing
- Live heart rate display with real-time BPM
- Dual tracking support (simultaneous connection to Polar Beat without interference)
- UI state machine for scan → connect → monitor flow
