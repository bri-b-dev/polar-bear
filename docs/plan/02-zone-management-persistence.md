# Slice 02 — Zone Management + Persistence Foundation

**Type:** Must core. First slice that stores data → introduces the local persistence layer.

**Covered IDs:** F-2.1, F-2.2 (Should), F-2.3, N-2, N-3

## Definition of Done (on device)

- I create, edit, and delete any number of HR zones, each with **name, color, bpm min/max**
  (F-2.1).
- Zones are **global** — defined once and available for later phase creation, not redefined per
  workout (F-2.3).
- Zones **survive an app restart and a device reboot** (persistence).
- Optionally, a **Karvonen helper** computes zone boundaries from resting HR + HRmax and lets me
  seed zones quickly (F-2.2).
- All of this works with the device **fully offline** — no internet, BLE not required for this
  screen (N-3).

## What this slice stands up

- **Room (SQLite)** as the local persistence layer (N-2) used by every later slice (templates,
  history). The zone entity/DAO is the first table.
- The Karvonen calculation helper (resting HR + HRmax + intensity % → bpm boundaries), reusable
  when seeding zones for execution testing later.

## Risks / assumptions to clarify

- **Initial default zones (spec §8):** pre-populate on first install using the proposed values
  **resting HR 62 / HRmax 179**. Decision made here.
- Karvonen (F-2.2, Should) is pulled in early because it is cheap and lets me seed realistic
  zones, which makes execution (Slice 04) testable without hand-entering boundaries.
- N-2 / N-3 are established structurally here (local-only Room, no network path) and inherited
  by all later slices.
