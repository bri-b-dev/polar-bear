# Slice 04 — Workout Execution Core

**Type:** Must core. Brings together a saved template + zones + live HR into a running workout.

**Covered IDs:** F-4.1, F-4.2, F-4.2a, F-4.3, F-4.4, F-4.5, F-4.6, F-4.8, N-6, N-4

## Definition of Done (on device)

- I pick a saved template and **start** a workout from it (F-4.1).
- I see, live: **current HR (bpm)**, **current phase name**, **remaining time for the phase**,
  and the **target zone** of the current phase (F-4.2).
- Inside a repeat block I additionally see **"Repetition 3 of 6"** (F-4.2a).
- Phases **auto-advance** when their duration expires (F-4.3), with **vibration and/or sound**
  feedback on each transition (F-4.4).
- If my HR **leaves the phase's target zone** (too high/low) I get an **unobtrusive warning**
  (visual, optionally vibration) (F-4.5).
- I can **pause, resume, and end early** at any time (F-4.6).
- The **screen stays on** for the whole active workout (F-4.8).
- If the strap **briefly drops**, the workout **does not abort** — it keeps running on the last
  value and reconnects (N-6).
- The live display is **readable at 1–2 m** (large font, high contrast) (N-4).
- Displayed bpm tracks the sensor within well under a second (N-5, re-validated from Slice 01).

## Risks / assumptions to clarify

- **Timer reliability:** run the phase timer so it isn't killed (foreground service vs.
  in-Compose state). Accuracy must hold across phase **and** block boundaries.
- **Pause behaviour:** decide whether the HR time series keeps recording while paused (spec is
  silent — §8 gap #6). Decided here; affects what Slice 07 stores.
- **N-6 robustness** is implemented here (last-value retention + auto-reconnect during a
  workout). Richer reconnect *status UI* (F-1.4) and startup auto-reconnect (F-1.3) are Slice 08.
- N-4 is the *primary* readability pass; Slice 08 only refines it.
