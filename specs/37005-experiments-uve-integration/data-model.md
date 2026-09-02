# Phase 1 Data Model: UVE Integration for the Experiments Portlet

**Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

This work adds **no persisted entity, no table, no index and no REST payload field**. Every entity
below already exists; what follows is what each one contributes to the round-trip and the switch,
and the two places a field is added to a *client-side view model*.

Nothing here is rollback-unsafe: the only server-side artifacts are a string constant, two
allow-list entries and one property line, all additive (see [Legacy Impact](./plan.md#legacy-impact)).

---

## Existing entities

### Experiment (`DotExperiment`)

`libs/dotcms-models/src/lib/dot-experiments.model.ts:12-26`

| Field | Used for |
|---|---|
| `id` | The return destination's identity (FR-005). A page may host several experiments, so this — not `pageId` — is what the round-trip is keyed on. |
| `pageId` | Resolving the page the variant deep link targets, via the store's prefill lookup. |
| `status` | `DRAFT` vs anything else decides editable vs read-only (FR-009), through `$isLocked()`. |
| `trafficProportion.variants` | The rows the Variants card draws. |

**Not present, and deliberately not added**: any language, site, edited-at, revision or
has-own-content field. `DotExperiment` carries no language, which is why `languageId` has to come
from the page lookup (see below) rather than from the experiment. FR-007a forbids the rest.

### Variant (`Variant`)

`libs/dotcms-models` (`AbstractExperimentVariant` on the backend)

| Field | Used for |
|---|---|
| `id` | The `variantName` query param UVE reads to render the page in that variant. |
| `name` | Row label, and — via `isControlVariant` — whether this is the control (FR-008). |
| `weight`, `url`, `promoted` | Unchanged by this work. |

**State transitions**: none. A variant has no lifecycle this work observes. FR-007a is a constraint
*on this entity*: no field is added to record that its content was edited, on either side of the
wire, and no client-side surrogate (session storage, a store flag, a derived signal) stands in for
one.

### Page — as the portlet holds it (`DotExperimentConfigurePage`)

`libs/portlets/dot-experiments/portlet/src/lib/shared/models.ts:123`,
built by `toConfigurePage` / `fromBrowserPage`
(`util/dot-experiments-configure.util.ts:223-240`)

| Field | Today | After |
|---|---|---|
| `pageId` | present | unchanged |
| `title` | present | unchanged |
| `path` | present | unchanged — becomes the deep link's `url` |
| **`languageId: number`** | **absent** | **added** |

**Why `languageId` is added**: `editEmaGuard` substitutes `language_id=1` when the param is missing
(`edit-ema.guard.ts:30-42`), so omitting it does not fail — it silently opens the wrong language's
content on any multilingual site. FR-004 forbids a partially-formed destination, so the value has to
travel with the page. Both constructors already read a source that carries it (the `htmlpageasset`
contentlet's `languageId`, and `DotPageBrowserPage.languageId` at
`dot-pages-browser.models.ts:68`), so this is a narrowing that was simply not needed until the deep
link existed.

**Validation rule**: a page with no `path` or no `languageId` is not a valid deep-link target. The
open-in-editor action refuses with a stated reason (FR-004) rather than navigating.

### Page lock state (`DotPageLockInfo`)

`libs/data-access/src/lib/dot-pages/dot-pages-browser.models.ts:82-86`, resolved into the store as
`pageLockInfo` via `pagesBrowserService.getPageLockState(pageId)`

| Field | Used for |
|---|---|
| `locked` | Gate for FR-010. |
| `lockedBy` | Compared to `globalStore.loggedUser()?.userId` — a page locked by *me* stays editable. |
| `lockedByName` | The reason stated to the user (FR-010). |

**Freshness**: resolved when the page is selected or prefilled, not polled. The spec's edge case
"the page is locked while UVE is open" is therefore satisfied by the store re-resolving on the
Configure screen's next load, which is what the return navigation triggers.

### Entry-point switch (`FEATURE_FLAG_EXPERIMENTS_PORTLET`)

A configuration property, not an entity. Its full contract — name, storage, default, wire format,
read path and failure behavior — is in
[contracts/entry-point-switch.md](./contracts/entry-point-switch.md).

Distinct from `FEATURE_FLAG_EXPERIMENTS`, which keeps its name, meaning, default (`true`) and
consumers untouched (FR-015a). The two are related only by naming family.

---

## Added / changed client-side view state

Two additions, both local to the frontend. No wire format changes.

### 1. `DotExperimentConfigurePage.languageId`

Covered above. One field, two constructors, one type.

### 2. List page filter — `DotExperimentsListState.selectedPageId`

`libs/portlets/dot-experiments/portlet/src/lib/store/dot-experiments-list.store.ts`

| Member | Type | Default | Notes |
|---|---|---|---|
| `selectedPageId` | `string \| null` | `null` | `null` is "no page filter", i.e. the full site-wide list. |

Sits alongside the existing `filter`, `selectedStatuses` and `selectedGoals` and behaves like them:

- **Reducer**: a `pageAssetFilterChanged` page event sets it and resets paging to page 1, exactly as
  `filterChanged` does (`:307-310`).
- **Derivation**: a `pageAssetFilteredExperiments` computed narrows on
  `experiment.pageId === selectedPageId`, inserted into the existing chain after `siteScoped` and
  **before** the status/goal counts, so the chip counts describe the narrowed set.
- **URL**: serialised as `pageAsset` by `toQueryParams` and read back by `parseViewState`
  (`util/dot-experiments-list-store.util.ts`). Written as `null` when unset, so a pristine list
  still carries no query string. **Never named `page`** — that key is pagination
  (`:158`).
- **Display**: the chip's label comes from the `pageInfoByPageId` map the Page column already
  resolves, so adding the filter costs no request.

**Invariant**: `selectedPageId` is an identifier, matched by equality. It is never a path and never a
substring match — that distinction is what makes FR-021b's "no other page's rows" true on a site
where one path prefixes another.

---

## Relationships

```
Experiment ──1:1──> Page          (Experiment.pageId; a Page may host many Experiments)
Experiment ──1:N──> Variant       (trafficProportion.variants; exactly one is the control)
Page       ──1:1──> DotPageLockInfo
```

The round-trip traverses this graph in both directions and the direction matters:

- **Outbound** (portlet → UVE): `Experiment` + `Variant` + `Page` → a deep link. Needs
  `Page.path` and `Page.languageId`, `Variant.id`, `Experiment.id`, and the read-only decision from
  `Experiment.status` + `DotPageLockInfo` + `isControlVariant`.
- **Inbound** (UVE → portlet): `Experiment.id` alone. Resolving by `Page` is insufficient — a page
  may host several experiments, which is the whole of FR-005 and US1 scenario 4.

---

## What is explicitly *not* modelled

| Not added | Requirement |
|---|---|
| Any "edited" / "modified" / "has own content" signal on `Variant`, persisted, derived or session-held | FR-007a |
| Any edited badge, edited meta line or last-modified indicator in the UI | FR-007a, SC-008 |
| Any new meaning for `FEATURE_FLAG_EXPERIMENTS` | FR-011a, FR-015a |
| Any change to how UVE stores, saves or locks page content | Out of Scope |
| A `results` route on the new portlet | #37004's; see [research.md R6](./research.md) |
