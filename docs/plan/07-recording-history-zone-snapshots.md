# Slice 07 — Recording & History + Zone Snapshots

**Type:** Must core (+ Should). Where execution output is persisted and zone snapshots become
meaningful.

**Covered IDs:** F-5.1, F-2.4, F-2.5, F-5.5 (Should), F-5.2 (Should), F-5.3 (Should)

## Definition of Done (on device)

- After I finish a workout it is **saved locally**: date, template used, phase sequence, and the
  **HR time series** (F-5.1).
- The save includes an **immutable snapshot** of the zone definitions (name, color, bpm range)
  valid at that time — not a reference to the global zones (F-2.4).
- Editing a zone **afterwards does not change any already-saved session**; saved sessions keep
  their historical snapshot and are not re-evaluated (F-2.5).
- Early exits are **noted** in the record, e.g. "4 of 6 repetitions completed" (F-5.5).
- I can open a **list of past workouts** (F-5.2) and view one, including a **simple HR graph**
  (time vs bpm, zone-colored background) (F-5.3).

## Risks / assumptions to clarify

- **HR time-series storage** shape/size in Room — sampling rate vs row count; keep it simple
  but queryable for the graph.
- **Snapshot correctness (F-2.4/F-2.5)** is the core Must of this slice: prove that editing a
  zone after a session is saved leaves that session visually and numerically unchanged.
- **Graph scope (spec §8, gap #3):** kept deliberately simple for V1. F-5.3 is Should — if the
  graph turns out non-trivial, push *only the graph* to Slice 08 and keep the list (F-5.2) and
  saving (F-5.1, F-2.4/F-2.5) here. The Musts stay in this slice regardless.
