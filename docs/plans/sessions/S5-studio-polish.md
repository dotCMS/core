# S5 — Studio polish (the delightful layer)

> **Goal:** the three a11y-specific elements that make the Studio feel alive — the animated
> score widget, violation overlays on a live preview, and the working/live toggle — plus
> the final publish footer. Pure frontend on top of S4's stream.
> **Plan:** [§7 UX spec](../a11y-agent-plan.md), [§8.4 event shape](../a11y-agent-plan.md), [§8.2 working URL](../a11y-agent-plan.md).

## Entry state
- S4 done: the Studio receives live `step` / `score` / `violation` / `done` events.

## Tasks (in order)
1. **Score widget** (§7): `before → after` issue count that **animates down** as `score`
   events arrive. The one orchestrated motion — respect `prefers-reduced-motion`.
2. **Live preview iframe + working/live toggle** (§7): **Live** = published URL, **Working** =
   `…?host_id=<id>&mode=EDIT_MODE` (§8.2). Toggle = the rendered before/after diff.
3. **Violation overlays**: pins on the iframe at each violation's DOM location. The scanner
   returns axe `selector` + `html` (NOT pixel bounds — §8.4 note), so resolve the selector
   against the iframe to position the pin. *Reported* (unfixable) issues stay visible in a
   distinct color so the human sees what's left.
4. **Publish footer**: `N fixed · M reported` + **Publish** (promotes working → live, the
   only publish, human-triggered). No Discard (§3 — undo via dotCMS version history).
5. **Polish pass**: step-log readability, score animation timing, overlay toggle, focus
   states, reduced-motion, responsive. Match complexity to the vision — keep it disciplined.

## Contract to honor
- Drive everything off the §8.4 events + §6 report already flowing — no new backend calls
  except the preview iframe URLs.
- Overlays computed from `selector` against the live iframe (no pixel bounds exist).

## Scope fences (do NOT)
- No new agent/proxy/loop work — this is the frontend layer only.
- No per-fix accept/reject, no free-form chat (both deferred — §7 "Why the spine survives").
- No multi-page batch (deferred).

## Definition of done
- Watching a run: the score ticks down live, steps stream, overlays pin violations on the
  preview, and the working/live toggle shows the rendered before/after.
- Publish promotes the working batch; the page reflects the fixes.
- Tests for the score widget, the working/live toggle, and overlay positioning; reduced-motion verified.
