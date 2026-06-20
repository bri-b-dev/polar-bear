# Slice 08 — Polish & Comfort

**Type:** Polish. **Should / May only — no Must here.** Safe to drop or reorder without
blocking the core product.

**Covered IDs:** F-1.3, F-1.4, F-1.5, F-3.3, F-3.4, F-5.4, plus refinement of N-4 and N-7

## Definition of Done (on device)

- The app **remembers the last H10 and auto-reconnects** on startup (F-1.3).
- During a workout it **shows reconnect status** (the underlying robustness is already covered
  by execution in Slice 04; this adds only the status UI) (F-1.4).
- It shows **battery/signal** of the sensor if available via BLE (F-1.5).
- Templates support **reordering** phases via drag/arrow (F-3.3) and **duplication** (F-3.4).
- I can **export a past workout as CSV** for external processing (F-5.4).
- A final pass on **display readability** (refines N-4) and **overview performance** (refines
  N-7) — refinement of already-covered NFRs, not new coverage.

## Risks / assumptions to clarify

- Everything here is Should/May. None of it is on the critical path; this slice can be trimmed
  or resequenced freely.
- The N-4 / N-7 items are explicitly **refinements** of work whose primary coverage is in
  Slices 04 and 06 respectively — they are not the owning slice for those NFRs.
