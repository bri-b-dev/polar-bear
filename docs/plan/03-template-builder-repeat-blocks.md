# Slice 03 — Template Builder with Repeat Blocks

**Type:** Must core. Settles the template data model that execution and overview both consume.

**Covered IDs:** F-3.1, F-3.2, F-3.2a, F-3.2b, F-3.2c, F-3.5, F-3.6

## Definition of Done (on device)

- I build a workout template made of any number of **individual phases**, each with **name,
  duration (mm:ss), and a target zone** referencing a zone from Slice 02 (F-3.1, F-3.2).
- I add at least one **repeat block**: an ordered list of ≥2 phases plus a **fixed** repeat
  count (e.g. 6×), set at creation time (F-3.2a).
- The template is a **flat, one-level sequence** of individual phases and/or repeat blocks;
  blocks **cannot be nested** (F-3.2b).
- The spec's example is buildable: Warm-up (5 min, Blue) → 6× [Load (2 min, Green), Recovery
  (2 min, Blue)] → Cool-down (5 min, Gray) (F-3.2c).
- I save the template, **kill and reopen the app**, and it is still there (F-3.5).
- I can **rename and delete** templates (F-3.6).

## What this slice stands up

- The **flat phases + non-nested repeat-block** schema (F-3.2b) in Room, which execution
  (04/05) and overview (06) both read. Getting this right now avoids downstream rework — it is
  the central data-model decision of the project.
- Phases reference zones by id; the actual zone *values* are snapshotted later at session save
  (Slice 07), not here.

## Risks / assumptions to clarify

- The schema must represent "block as a first-class ordered unit with a count" so that
  execution can show "Repetition 3 of 6" (F-4.2a) and overview can render a block as one
  consolidated entry (F-4.9d) without re-deriving structure.
- **Reorder (F-3.3)** and **duplicate (F-3.4)**, both Should, are deferred to Slice 08 — they
  are conveniences on top of a correct schema, not part of proving it.
