# Slice 01 — BLE Spike: Connect, Live HR, Dual Tracking

**Type:** Risk-first spike — proves the make-or-break assumption before any UI is built on it.

**Covered IDs:** F-1.1, F-1.2, F-1.6, F-1.7 (Should), N-1, N-5

## Definition of Done (on device)

- I open the app, tap **Scan**, and see my Polar H10 in a list of found devices.
- I tap it, the app connects via the standard BLE Heart Rate Service (0x180D), and a **live
  bpm number updates** on a single screen.
- With **Polar Beat running on the same phone** and connected to the same H10, **both** the new
  app and Polar Beat show live HR at the same time (dual tracking).
- If a connection can't be established (e.g. the H10 already has two centrals connected), the
  app shows a **clear error message** (F-1.7).
- The displayed bpm updates within well under a second of each sensor notification (N-5).

## What this slice stands up

- The native Android (Kotlin + Compose) app skeleton — first runnable build (N-1).
- The runtime BLE permission flow (scan/connect permissions for the target API level).
- Standard Heart Rate Service parsing (0x180D / HR Measurement characteristic 0x2A37),
  including the 8- vs 16-bit HR value flag. **No proprietary Polar SDK** — pure GATT.

## Risks / assumptions to clarify

The central question (spec §8): **does the H10 allow the new app *and* Polar Beat to receive
HR simultaneously, specifically when both apps run on the same Android phone?** The hard
unknown is not the H10's multi-connection count (well known to be limited, ~2 centrals) but
whether **two apps on one Android device** can each hold an independent GATT connection to the
same peripheral. This is the make-or-break assumption for the whole dual-tracking premise.

### Kill-criterion

The dual-tracking assumption is judged **FAILED** if, on a real device, the new app cannot
read live HR while Polar Beat is simultaneously connected to the same H10 — i.e. Android / the
H10 will not grant two independent GATT connections from one phone.

### Contingency branch (decide here; reshapes Slices 04 and 07)

If FAILED, record the chosen fallback in this file and cross-reference it from
`00-overview.md` gaps, because it changes whether F-1.6/F-1.7 stay in scope downstream:

- **(a) App-only mode** — drop the "parallel with Beat" requirement (F-1.6/F-1.7) to a
  non-goal; the new app owns the single connection and Polar Flow integration is sacrificed.
- **(b) Sequential / handoff** — only one app connected at a time, with a deliberate handoff.
- **(c) Beat-only-when-app-closed** — accept that Beat records only when the app is not running.

The decision affects nothing built yet, which is exactly why this slice comes first.
