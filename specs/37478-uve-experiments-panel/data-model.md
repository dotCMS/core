# Phase 1 Data Model: The Page's Experiments as a Panel

**Feature**: [#37478](https://github.com/dotCMS/core/issues/37478) | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This work adds **no server-side entity and changes none**. `DotExperiment`, `Variant`, `Goals`,
`TrafficProportion` and the REST contracts behind them are untouched (spec Legacy
Considerations). What follows is the client-side state the panel introduces, and the two
existing shapes it re-uses.

---

## 1. New: `DotExperimentsPanelState`

The panel's whole existence, held by `DotExperimentsPanelStore`. Provided by the **UVE shell**
component so it outlives the panel itself (research R3 / spec D13).

| Field | Type | Meaning | Requirements |
|---|---|---|---|
| `isOpen` | `boolean` | Whether the panel is rendered. `false` covers both "never opened" and "dismissed". | FR-001, FR-003 |
| `view` | `'list' \| 'create' \| 'configure' \| 'results'` | Which screen the single panel surface is showing. Never two at once. | Spec Assumptions ("one panel at a time"), FR-006, FR-007, FR-008, FR-025a |
| `experimentId` | `string \| null` | The experiment `configure`/`results` is showing. `null` on `list` and `create`. | FR-008, FR-023, FR-025b |
| `suspendedForVariant` | `boolean` | The panel is closed *because* the editor left for a variant, not because they dismissed it. The one state in which `view`/`experimentId` survive a closed panel. | FR-021, FR-023, FR-025, FR-039 |

### Derived (not stored)

| Signal | Derivation | Requirements |
|---|---|---|
| `pageId` | Live from `uveStore.pageAsset()?.page?.identifier` — **not** captured at open. | FR-010, FR-015, FR-034 |
| `languageId` | Live from `uveStore.pageLanguageId()` — read at the moment of use, so a mid-session switch is honoured. | FR-022a, FR-034a, D10 |
| `isPanelMode` | The store's mere presence, from a screen's point of view (`inject(…, { optional: true }) !== null`). | FR-040, FR-041 |

### State transitions

```
                    open()                       close()
   [closed, clean] ────────► [open, list] ────────────────► [closed, clean]
                                  │  ▲                            ▲
             selectExperiment(id) │  │ backToList()               │ close()
                                  ▼  │                            │
                            [open, configure] ───────────────────►┘
                                  │  ▲
             openResults() / badge│  │ backToConfigure()
                                  ▼  │
                            [open, results]
                                  │
        suspendForVariant()       │              resumeFromVariant()
   [open, configure] ────────► [closed, SUSPENDED] ────────► [open, configure]
                                (view + experimentId retained)
```

**Invariants**

1. `close()` always resets to the initial state. Nothing about a previous session is visible on
   the next open (FR-039, SC-016).
2. `suspendForVariant()` is the **only** way to reach a closed state with `view`/`experimentId`
   retained, and `resumeFromVariant()` is the only way out of it (FR-023).
   *This is the invariant most likely to be broken by a later "simplification" — it is why the
   two closes are two named methods rather than one with a boolean.*
3. A `pageId` change resets `view` to `list` and `experimentId` to `null`, and clears the list's
   view state (below) (FR-034, FR-034b).
4. A `languageId` change transitions nothing (FR-034a).
5. `open()` while already open on another view **replaces** the view — it never stacks a second
   surface. The toolbar badge on an open panel is the case that proves it (FR-025c, Edge Cases).

---

## 2. Re-used, re-scoped: `DotExperimentsListViewState`

Already declared in
`core-web/libs/portlets/dot-experiments/portlet/src/lib/shared/models.ts`. **The shape does not
change.** What changes is where it is seeded from and whether it is mirrored to the address.

| Field | Portlet mode (today, unchanged) | Panel mode (new) |
|---|---|---|
| `filter` | URL `?filter=`, mirrored back | in memory only |
| `selectedStatuses` | URL, mirrored back | in memory only |
| `selectedGoals` | URL, mirrored back | in memory only |
| `page`, `perPage` | URL, mirrored back | in memory only |
| `orderBy`, `direction` | URL, mirrored back | in memory only |
| `selectedPageId` | URL `?pageId=`, editor-supplied, clearable | **the page in hand**, structural, not clearable |
| `selectedPageUrl` | resolved by the bulk page lookup | unused — the panel is the page |
| `languageId` | URL `?language_id=`, return context | live from the shell, return context |

Requirements: FR-031, FR-032, FR-035, FR-042 (portlet column keeps its behaviour), FR-010,
FR-034b, SC-010, SC-011.

### The narrowing that must be bypassed

`DotExperimentsListState.pageInfoByPageId` exists solely so `siteScopedExperiments` can compare
`pageInfoByPageId[pageId].host` to the current site. In panel mode the bulk lookup does not run
(FR-010 forbids it), so the map is empty and that computed would drop **every** experiment.

> Panel mode must bypass the site narrowing, not merely skip its input. Dropping every row is
> indistinguishable from "this page has no experiments" — the exact confusion FR-029 and SC-008
> exist to prevent.

---

## 3. Re-used unchanged: the round-trip context

| Shape | Where | Panel-mode note |
|---|---|---|
| `DotVariantEditorQueryParams` | `util/dot-experiments-uve-link.util.ts` | Reused for the page/variant params. `experimentReturn` is **omitted** — the panel's return is answered by `suspendedForVariant`, not by the address (FR-031). |
| `EXPERIMENT_RETURN_PARAM` / `EXPERIMENT_RETURN_PORTLET` | `libs/dotcms-models/src/lib/dot-experiments-constants.ts` | Untouched. They stay the flag-off / full-screen-portlet mechanism (FR-046). |
| `CONFIGURE_SECTION_PARAM` | same | Untouched for the portlet path. In the panel, "return to the Variants card" is a scroll intent, not a query param. |

---

## 4. Entity-to-requirement map

| Spec Key Entity | Client shape | Notes |
|---|---|---|
| **Experiment** | `DotExperiment` (`@dotcms/dotcms-models`) | Unchanged. Carries `pageId`, no language — the fact D10 and FR-015 rest on. |
| **Variant** | `DotExperimentVariantDetail` / `trafficProportion.variants` | Unchanged. |
| **The page in hand** | derived signal, §1 | Never stored in the panel state — storing it would let it go stale against the canvas, which FR-034 forbids. |
| **The editor's return context** | derived `languageId`, §1 | Never part of what is created or saved (FR-015). |
| **Panel view state** | §1 `view`/`experimentId` + §2 list view state | Split deliberately: which screen is the panel's, what the list is filtered to is the list's. |

---

## 5. Validation rules

Nothing new. Creation and configuration keep the validation the full-screen screen has
(FR-017), with one field removed rather than re-validated:

- **Page** — in panel mode the field is settled context with an explanatory hint, not a control
  (FR-014, FR-014a). The existing `required` rule on it is satisfied by construction, and the
  Change/Select button and `DotExperimentsChangePageDialogComponent` are not rendered at all
  (FR-014's "including any confirmation flow that would delete variants").
- **Traffic allocation** — unchanged; it shares the Page card's row today and must survive the
  Page control's removal.
