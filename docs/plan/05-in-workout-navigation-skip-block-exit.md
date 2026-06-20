# Slice 05 — In-Workout Navigation: Skip & Block Exit

**Type:** Must core. Manual navigation on top of the execution engine.

**Covered IDs:** F-4.7, F-4.7a

## Definition of Done (on device)

- During a workout I can **skip the current phase**, moving directly to the next phase — even
  when that next phase is the next repetition inside the same repeat block (F-4.7).
- Inside a repeat block I separately see and can use **(a) skip this phase** and **(b) exit the
  entire remaining block**, jumping straight to the phase *after* the block (e.g. exit after
  repetition 4 of 6 instead of skipping individually to the block's end) (F-4.7a).
- **Both controls are available simultaneously** while inside a block, and are distinct actions.

## Risks / assumptions to clarify

- **State transitions when exiting mid-block:** define exactly what "the phase after the block"
  resolves to after, say, rep 4 of 6, and how the repeat counter is finalized for recording
  (Slice 07 records "4 of 6 repetitions completed", F-5.5).
- **Progress recomputation:** a skip or early block-exit changes how much workout remains. How
  remaining time / total progress recompute couples directly with Slice 06's progress bar — the
  two slices share the decision flagged in `00-overview.md` gap #2 (planned-total vs live
  recompute), resolved in Slice 06.
