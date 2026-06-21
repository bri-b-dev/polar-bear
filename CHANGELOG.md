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
