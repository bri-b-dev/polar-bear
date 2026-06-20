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
