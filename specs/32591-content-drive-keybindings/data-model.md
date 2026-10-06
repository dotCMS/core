# Phase 1 Data Model: Content Drive Keybindings

No persisted data. Nothing here is stored, transmitted or serialized — these are in-memory runtime
structures with the lifetime of the surfaces that create them. Recorded because the arbitration rules
between them are the substance of the feature.

---

## ShortcutRegistration

A claim on a key combination by a surface.

| Field | Type | Notes |
|-------|------|-------|
| `combination` | key combination | Normalized. Must resolve the platform-appropriate primary modifier so one registration serves every platform. |
| `label` | string (i18n key) | Human-readable description, for the author-facing documentation required by FR-023. Carried on the registration rather than in a separate list so a shortcut cannot ship undocumented. |
| `handler` | callback | What to run. Returns whether it consumed the key (see *Declining*, below). |

**Lifecycle**: created when a surface opens (component construction), withdrawn when it is destroyed.
A registration never outlives its surface, which is what makes the stack self-correcting when a
dialog closes.

**Registered in batches**: a surface passes every combination it wants in one call and gets back a
single withdrawal covering all of them, so a caller never tracks a handle per shortcut.

**Validation rules**
- The same combination may be claimed by several surfaces at once. That is the normal case and is what
  the claim stack resolves.
- A registration with no label is invalid, because FR-023 requires every shipped shortcut to be
  documented, and `activeShortcuts()` is what that documentation is generated from.

**Declining**: a handler returning `false` passes the key to the next claimant and then to the browser
(FR-019). This is for runtime decisions only. Nesting needs no cooperation: the listener runs in the
bubble phase and ignores an already-handled event, so a component handling a key on its own element
is closer to the target and never reaches arbitration.

---

## Claim stack

Claims on one combination, newest last.

| Field | Type | Notes |
|-------|------|-------|
| key | normalised combination | `shift+mod+k` and `mod+shift+k` are the same key. |
| entries | ordered registrations | The **last** entry receives the key. |

**There is deliberately no `Scope` entity.** An earlier design had surfaces push and pop a scope, and
for a given combination the topmost scope holding a claim won. Per-combination last-in-wins reaches
the same outcome in every case examined, including the one that rules out "topmost surface wins
outright" (a dialog claiming one combination must not swallow another it never claimed), without any
scope concept to build, test or explain.

**State transitions**

| Trigger | Stack |
|---------|-------|
| A surface registers | Its claim is appended and becomes the winner |
| That surface is destroyed | Its claim is spliced out **by identity**, so withdrawing an older claim out of order leaves the newer one in charge |
| The last claim on a combination goes | The combination is removed, and with the last combination the document listener detaches |

**Invariant**: exactly one document listener exists no matter how many claims are live, attached
lazily on the first and detached with the last.

## SelectionAnchor

The row a range extends from. Distinct from the focused row, which is where the keyboard cursor sits.

| Field | Type | Notes |
|-------|------|-------|
| `rowIndex` | index into the current page | Page-relative semantics; see the boundary rule below. |
| unset | — | Valid initial state, and the state the table restores on sort and on the value/pagination reset. |

**Dual role, deliberately**: this is the same value the table component uses both to decide which row
is the single tab stop and to anchor a range. Driving one drives the other, which is the mechanism
behind D1 in [research.md](./research.md) and is why the anchor must be seeded even before any
selection exists.

**State transitions**

| Trigger | Anchor |
|---------|--------|
| Listing loads | Seeded to the first row, so a tab stop exists |
| Plain arrow key | Moves to the newly focused row |
| **Shift+Arrow** | **Unchanged** — this is what makes a range a range |
| Row or checkbox click | Moves to the clicked row |
| Shift+click on a checkbox | Unchanged; the range extends to the clicked row |
| Column sort | Cleared by the table, must be re-seeded |
| Page or page-size change | Cleared by the table, must be re-seeded |

**Boundary rule**: the anchor and any range derived from it are bounded by the current page (FR-012).
A page change clears the selection, which the listing's existing "clear selection when the rows
change" behaviour already enforces. That behaviour is therefore load-bearing for this feature and must
not be removed as dead code.

---

## Relationship to existing state

Nothing here replaces existing state. Two pieces of existing state are read or driven:

- **The portlet's selected items** remain owned by the Content Drive store. Range selection must
  produce its result *through* the existing selection output, not by writing to the table directly —
  the shared component re-asserts the effective selection back down through an effect, so a direct
  write would be overwritten. This round trip is the highest-risk part of the feature (D7).
- **The active filters** remain owned by the store. Escape drives the existing clear-all action rather
  than modelling filter state here (D6).
