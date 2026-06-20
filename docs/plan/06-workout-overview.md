# Slice 06 — Workout Overview

**Type:** Must core. The "where am I in the whole plan" view (spec §4.4.1).

**Covered IDs:** F-4.9, F-4.9a, F-4.9b, F-4.9c, F-4.9d, F-4.9e, N-7

## Definition of Done (on device)

- During a running workout I open an **overview** that shows my current position in the overall
  plan at any time (F-4.9).
- It has an **overall progress bar** (elapsed vs total), independent of phase/block structure
  (F-4.9a).
- Alongside it is an **expandable list of all phases and repeat blocks** in chronological order
  (F-4.9b).
- The **active phase is distinctly highlighted**; completed phases are marked done, pending ones
  open (F-4.9c).
- A repeat block appears as **one consolidated entry** (not 6 separate rows) that I can expand
  to see its phases and current repeat progress (F-4.9d).
- Opening/closing the overview **never interrupts the workout or affects the timer** (F-4.9e).
- It stays **clear and performant** for templates with many phases/reps — no stutter when
  expanding (N-7).

## Risks / assumptions to clarify

- **Progress-bar "total" (gap #2):** once skips/early-exits (Slice 05) make the duration
  dynamic, decide whether the bar tracks the **planned** total or **recomputes live**. Decided
  here, consistent with Slice 05's progress recomputation.
- **Overview visibility (spec §8, gap #4):** progress bar **always visible** vs **shown on
  demand** — decided here.
- N-7 is the *primary* performance pass for the overview; Slice 08 only refines it.
