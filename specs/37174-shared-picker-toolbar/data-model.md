# Phase 1 Data Model: Shared Filter Toolbar

**Feature**: `specs/37174-shared-picker-toolbar` | **Date**: 2026-09-03

No persisted data changes. These are the client-side shapes the feature introduces or modifies, and
the rules that govern them. Types are illustrative of the shape and its invariants — final
signatures belong in the code.

---

## 1. `DotFilterFacade` — the store-agnostic seam

The contract every chip talks to instead of a store. One implementation per surface, provided at
the component that owns that surface's store.

| Member | Shape | Rule |
|---|---|---|
| `getFilterValue` | `(key: string) => string \| string[] \| undefined` | Returns the **normalized** value (see §2). `undefined` means the filter is not set — distinct from an empty array. |
| `patchFilters` | `(patch: Record<string, string \| string[]>) => void` | Merges. Every write MUST reset the result list to page 1 (FR-013). |
| `removeFilter` | `(key: string) => void` | Deletes the key entirely rather than setting `undefined`, so the bag never carries empty entries. |
| `clearFilters` | `() => void` | Returns to that surface's **defaults**, not to `{}` — see §4. |
| `$hasNonDefaultFilters` | `Signal<boolean>` | Drives "Clear all" visibility. Must be false on a freshly opened surface even though defaults are present. |

**Invariant**: a chip may only reference filter keys it owns. No chip reads another chip's key.

**Invariant**: the facade is the *only* way a shared chip reaches surface state. A chip that
injects a store fails review — this is what FR-003 forbids and what caused the original drift.

Full contract, including the conformance obligations, in
[`contracts/filter-facade.contract.md`](./contracts/filter-facade.contract.md).

---

## 2. Normalized filter values

Chips speak one vocabulary; each surface's facade translates to and from its own storage encoding.

| Filter key | Normalized value seen by chips | Content Drive storage | Asset Picker storage |
|---|---|---|---|
| `title` | `string` | same | same |
| `languageId` | `string[]` (language ids) | same | same |
| `contentType` | `string[]` (variables) | same | same |
| `baseType` | `string[]` of **names** (`DOTASSET`, `FILEASSET`, …) | **numeric keys** (`MAP_BASE_TYPES_TO_NUMBERS`) so they survive the URL | names — the picker has no URL to survive |
| `sharedAssets` | `'true' \| 'false'` | same | **new key** |
| `us_<variable>` | `string \| string[]` | same | **new**, admitted by the index signature |

**Why the divergence stays**: collapsing it would mean either putting numeric base-type codes into
a store that has no URL to justify them, or breaking Content Drive's frozen URL encoding. The
translation already exists today inside `DotContentDriveContentTypeFilterComponent`; the facade is
where it moves, not where it is invented.

**Rule**: normalization is total and lossless in both directions. An unmapped base-type number is
dropped rather than passed through — the current `.filter(Boolean)` behavior, preserved.

---

## 3. `dot-filter-bar` — the chip row

A layout wrapper, not a renderer of a chip registry. It owns exactly two things:

| Responsibility | Detail |
|---|---|
| Layout | The wrapping flex row (`flex-wrap`) the chips sit in, identical on both surfaces (FR-016) |
| Clear all | Rendered when `facade.$hasNonDefaultFilters()` is true; calls `facade.clearFilters()` (FR-009) |

Chips arrive by **content projection** (`<ng-content>`), and each one injects
`DOT_FILTER_FACADE` itself:

```html
<dot-filter-bar>
  <dot-shared-assets-filter />
  <dot-content-type-filter-chip />
  <dot-language-filter-chip />
  <dot-field-filter-menu />
</dot-filter-bar>
```

**Why projection rather than a config array of chip ids.** A registry inside `@dotcms/ui` cannot
render Content Drive's Workflow and Status chips: those stay in the portlet (FR-014a), and
`@dotcms/ui` must not import portlet code — that dependency runs the wrong way. Projection lets a
surface mix shared chips with its own, and keeps the bar free of any per-chip knowledge.

**Ordering (FR-007)**: the chip order is the surface's template order, anchored by a shared
constant that documents the canonical sequence:

```ts
// dot-filter-bar/constants.ts — the order both surfaces must follow
export const DOT_CANONICAL_FILTER_ORDER = [
  'sharedAssets', 'contentType', 'workflow', 'status', 'language', 'fieldFilters'
] as const;
```

Each chip carries a `data-filter-chip="<id>"` attribute, and a test asserts that the rendered
order in each toolbar is a subsequence of this constant. Order is therefore enforced by a test
rather than by machinery — the alternative (dynamic component outlets driven by a registry) buys
compile-time ordering at the cost of a component-resolution layer that neither surface needs.

| Surface | Chips, in order |
|---|---|
| Content Drive | `sharedAssets`, `contentType`, `workflow`, `status`, `language`, `fieldFilters` |
| Asset Picker | `sharedAssets`, `contentType`, `status`, `language`, `fieldFilters` |

**Rule (FR-005)**: the picker's exclusion of Workflow is recorded as a comment in its toolbar
template citing FR-014a, next to the chips it does render — the exclusion is visible where someone
would add it. Status is **not** excluded: it moves to the shared library alongside the others.

## 4. Surface defaults, and what "Clear all" means

"Default" is per-surface, which is why `clearFilters` and `$hasNonDefaultFilters` live behind the
facade rather than in the bar.

| Surface | Defaults after `clearFilters()` |
|---|---|
| Content Drive | `withFilterDefaults(...)` — environment default `languageId`, `sharedAssets: 'true'`. Unchanged from today. |
| Asset Picker | The caller's seeds only: `config.languageId`, `config.baseTypes`. **Changed** — today it clears to `{}` (research R6a). `sharedAssets` is deliberately **not** seeded: Content Drive spells it out so the applied state is visible in the URL, and the picker has no URL, so an absent key simply means on — which is how the chip already reads it. |

**Rule (FR-009)**: one seeding function feeds both `initPicker` and `clearFilters`, so the two can
never disagree. This mirrors Content Drive's `withFilterDefaults`, which is already applied on
every path that builds filters from scratch.

**Rule (FR-009a)**: `title` is part of the bag, so `clearFilters()` clears the search box too, on
both surfaces.

**Rule (FR-009b)**: the browsed location (`path` / `selectedNode`) is **not** in the bag and
`clearFilters()` must not touch it. Searching does move the scope to the site root — on both
surfaces — but clearing afterwards leaves the editor there rather than restoring `config.path`.

**Rule (FR-014d)**: a caller restriction may bound a control's **options** without becoming a
filter itself. Version state does this to Status: pinned to published-only, the control offers
`LOCKED` and withholds `ARCHIVED` / `UNPUBLISHED`, because the platform would otherwise force the
whole query onto the working version (`BrowserQuery` lines 176-178) and describe content by a
version the caller did not ask for. Same shape as `allowedBaseTypes` bounding the content-type
control's options today.

| Caller's version state | Status options offered |
|---|---|
| Published only (`live: true`) | `LOCKED` |
| Working included (the default) | `ARCHIVED`, `UNPUBLISHED`, `LOCKED` |

**Rule (FR-010)**: `config.mimeTypes` and `config.allowedBaseTypes` are **not** filters and never
enter the bag. They are applied when the request is built and survive every clear. The existing
comment on `DotAssetPickerConfig.mimeTypes` — "Deliberately NOT part of `DotAssetPickerFilters`" —
states the invariant; this feature must not weaken it.

---

## 5. Modified: `DotAssetPickerFilters`

Currently a closed interface with four optional keys, which cannot hold `sharedAssets` or the
dynamic `us_<variable>` keys.

**Change**: widen to the shape Content Drive already uses for the same problem —
named known keys intersected with an index signature:

```ts
type DotAssetPickerFilters = Partial<DotKnownAssetPickerFilters> & {
  [key: string]: string | string[];
};
```

Additive; no existing key changes meaning or type. Keeps autocompletion and type-checking on the
known keys while admitting the dynamic ones.

---

## 6. Modified: the picker's search request

Two fields on the existing `DotContentDriveSearchRequest` — both already declared, neither
currently populated by the picker.

| Field | Today | After |
|---|---|---|
| `includeSystemHost` | hardcoded `true` | derived: `getFilterValue('sharedAssets') !== 'false'` — off only when explicitly off, matching Content Drive |
| `userSearchable` | omitted | `buildUserSearchablePayload(filters, fields)` when field chips are active, `undefined` otherwise |
| `status` | omitted | the Status selection when non-empty, `undefined` otherwise — identical to Content Drive |
| `archived` | `browse?.showArchived ?? false` | **removed entirely** (FR-014b). The endpoint defaults it to false, so an unfiltered picker is unchanged; an Archived selection now travels through `status` and is not contradicted |
| `live` | `browse?.showWorking === false ? true : omitted` | unchanged — version state is a separate axis (FR-014c) |

**Rule**: an absent `userSearchable` must leave the request byte-identical to one that never
mentioned it, so an unfiltered picker issues exactly the request it does today. Same discipline
Content Drive applies to its `status` key.

**Rule**: the field-metadata fetch keeps its per-content-type cache and `switchMap` cancellation
when it moves. Two surfaces must not mean two fetches of the same content type within a surface.

---

## 7. Unchanged by contract

Recorded so a reviewer can check them quickly:

- `DotAssetPickerConfig` — no field added, removed, or reinterpreted. No caller updates.
- Content Drive's URL filter encoding (`encodeFilters` / `decodeByFilterKey`) — untouched; the
  facade translates above it.
- `DotContentDriveFilters` — unchanged.
- `dot-chip-filter`, `dot-content-type-filter`, `dot-language-filter` public APIs — unchanged.
- The `content-drive.*` i18n keys — unchanged (research R8).
