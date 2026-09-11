# Contract: `DOT_FILTER_FACADE`

**Feature**: `specs/37174-shared-picker-toolbar` | **Date**: 2026-09-03

The interface between a shared filter chip in `@dotcms/ui` and whatever surface is rendering it.
It is the only contract this feature adds; every other interface it touches is frozen (see §6).

**Consumers**: `dot-filter-bar` and every chip it renders.
**Implementors**: the Asset Picker (`asset-picker-filter-facade.ts`) and Content Drive
(`content-drive-filter-facade.ts`). A third surface implements it to opt in.

---

## 1. Interface

```ts
export const DOT_FILTER_FACADE = new InjectionToken<DotFilterFacade>('DOT_FILTER_FACADE');

export interface DotFilterFacade {
  getFilterValue(key: string): string | string[] | undefined;
  patchFilters(patch: Record<string, string | string[]>): void;
  removeFilter(key: string): void;
  clearFilters(): void;
  readonly $hasNonDefaultFilters: Signal<boolean>;
}
```

Provided at the component that owns the surface's store, never in `root` — each open picker gets
its own, exactly as `DotAssetPickerStore` is provided today.

---

## 2. Behavioral obligations

Every implementation MUST satisfy all of these. They are executable as a shared suite (§4).

**O1 — Round-trip.** `patchFilters({ k: v })` followed by `getFilterValue(k)` returns a value
*equal* to `v` after normalization. For `baseType` this crosses an encoding boundary in Content
Drive (names in, numbers stored, names out) and must still hold.

**O2 — Absence is `undefined`.** `getFilterValue` on an unset key returns `undefined`, never `null`,
`''`, or `[]`. An empty array means "set to nothing selected" and is a different state.

**O3 — Removal deletes.** After `removeFilter(k)`, the key is absent from the underlying bag — not
present with an `undefined` value. Verifiable through the surface's own serialization: Content
Drive's URL must not gain an empty parameter.

**O4 — Every write resets paging.** `patchFilters`, `removeFilter` and `clearFilters` each return
the list to page 1 and discard cursor bookmarks (FR-013). A stale cursor must never be applied to a
narrower result set.

**O5 — `clearFilters` restores defaults, not emptiness.** It lands on the same visible state a
freshly opened surface shows, including seeded values (data-model §4). Immediately after it,
`$hasNonDefaultFilters` is `false`.

**O6 — `$hasNonDefaultFilters` ignores defaults.** It is `false` on a fresh surface even though
defaults are present, and `false` when a filter is explicitly set to its own default value
(selecting the default language by hand is indistinguishable from the seeded state). Content
Drive's existing `hasNonDefaultFilters` already encodes this; the picker's implementation must
match it.

**O7 — Normalization is total and lossless.** Values crossing the facade are the normalized
vocabulary in data-model §2. An unmappable stored value is dropped, not passed through raw.

**O8 — Restrictions are unreachable.** No sequence of facade calls may surface content outside the
caller's `mimeTypes` / `allowedBaseTypes` / version state (FR-010). These are not filters and must
not be readable or writable through the facade at all.

**O8a — Restrictions may bound a control's options.** A chip whose options a restriction narrows
receives that bound as an input, not through the facade — the facade carries filter *values*, never
caller restrictions. Version state bounding the Status options (FR-014d) is the case that exists;
`allowedBaseTypes` bounding the content-type options is the precedent.

**O9 — Writes are synchronous and idempotent.** A patch that changes nothing does not notify, does
not refire the search, and does not reset paging. The picker's `$request` already dedupes
structurally; the facade must not defeat that by minting new arrays for unchanged values.

---

## 3. Error reporting

Chips that load their options MUST NOT inject `DotHttpErrorManagerService` or anything transitively
requiring `Router` / `DotEventsSocket` (research R4 — the legacy Dojo host has no `Router`).

Instead a chip exposes:

```ts
readonly error = output<DotFilterChipError>();   // { messageKey: string }
```

| Surface | Handling |
|---|---|
| Content Drive | routes it to `DotHttpErrorManagerService`, as today |
| Asset Picker | routes it to its own toast (`ASSET_PICKER_ERROR_KEYS` + `dot-toast`), as it already does for asset/folder load failures |

**Obligation**: on failure the chip degrades to an empty option list and stays interactive; the
surface stays usable and the picker's dialog stays open (FR-015). The precedent already in the
shared library is `dot-content-type-filter`'s `catchError(() => of([]))` — this contract keeps that
resilience and adds the notification the silent version lacks.

---

## 4. Conformance suite (normative)

A single parameterized Jest suite exported from
`libs/ui/src/lib/components/dot-filter-bar/testing/filter-facade.conformance.ts`:

```ts
export function testFilterFacadeConformance(
  name: string,
  setup: () => { facade: DotFilterFacade; /* surface-specific probes */ }
): void
```

Both surfaces' specs invoke it. It asserts O1–O9. **This suite is the mechanism that makes FR-004's
"zero regression" claim testable** — without it, the two encodings drift silently, which is the
failure mode this whole feature exists to end.

Per-surface probes the suite needs, supplied by `setup()`:
- `readRawBag` — the raw filter set beneath the facade, to assert O3 deletes rather than blanks
- `readPage` / `goToPage2` — so an O4 reset and an O9 non-reset are both observable
- `expectedDefaults` — what this surface's `clearFilters` lands on (O5, O6)
- `encodedFilter` — one key whose value crosses this surface's encoding boundary (O1, O7)
- `writeRaw` + `unmappableRawValue` — to prove O7 drops what it cannot map
- `restrictedKeys` — the caller restrictions O8 must keep hidden

Plus one **capability**, read at `describe` registration time rather than out of the setup object
(Jest builds its test tree before any `beforeEach`, so a test guarded on a setup field would
silently never register):

- `normalizes` — whether this surface's normalization is a real translation. Content Drive's is
  (names ↔ numeric URL keys), so O7's "drop the unmappable" applies. The AssetPicker's is the
  identity, so that state cannot exist and the check is skipped rather than satisfied by inventing
  a requirement.

**Deliberately not a probe: the serialized URL.** Content Drive's round-trip is a property of that
store's own serialization and is already covered by its dedicated specs; reproducing the router
machinery inside the conformance suite would test the harness rather than the contract.

---

## 5. Chip registration

Adding a chip is, in full:

1. One **connected** chip component under `libs/ui/src/lib/components/dot-filter-bar/chips/`,
   injecting `DOT_FILTER_FACADE` and carrying `data-filter-chip="<id>"`. If it wraps an existing
   presentational filter, that filter's API stays frozen (§6).
2. One id appended to `DOT_CANONICAL_FILTER_ORDER` in `dot-filter-bar/constants.ts`, at the
   position it should occupy.
3. One element in each opted-in surface's toolbar template.

Nothing else. This is what SC-003 measures, and step 3 is the "one line of configuration per
surface" it allows.

**Ordering obligation**: the rendered `data-filter-chip` sequence in every surface's toolbar MUST
be a subsequence of `DOT_CANONICAL_FILTER_ORDER` (FR-007). A surface may omit chips; it may not
reorder them. Asserted per surface in its toolbar spec.

**Projection, not a registry**: `dot-filter-bar` renders `<ng-content>` and owns only the wrapping
layout and the Clear all button. It has no per-chip knowledge, which is what lets Content Drive
project its portlet-local Workflow and Status chips into the same row — `@dotcms/ui` cannot import
them, and must not try to.

---

## 6. Frozen interfaces (this feature must not change them)

| Interface | Guard |
|---|---|
| `DotAssetPickerConfig` | Frozen **except** `browse.showArchived`, which is removed — see the note below. No other field added, removed or reinterpreted; no VTL template updated. |
| Content Drive URL filter encoding (`encodeFilters` / `decodeByFilterKey`) | Existing deep links resolve byte-identically. Covered by the existing `utils/functions.spec.ts` plus the store spec. |
| `dot-chip-filter`, `dot-content-type-filter`, `dot-language-filter` public APIs | Unchanged; they are rebound through the bar, not modified. |
| `content-drive.*` i18n keys | Preserved verbatim (research R8). |
| `POST /api/v1/drive/search` request/response | Unchanged. Three already-declared fields (`includeSystemHost`, `userSearchable`, `status`) become populated by a second caller, and one (`archived`) stops being sent; ADR-0018's DB-first routing is untouched. |

### The one deliberate unfreeze: `browse.showArchived`

Removed, along with the `archived` key the picker sends. Justification (FR-014b):

- **It is dead capability.** No code in the repository ever sets it to `true`. Both shipped VTL
  templates pass `status: "live"`, which maps it to `false`, and the two other call sites hard-code
  `false`. The picker has never listed archived content.
- **It duplicates the Status control.** Content condition must have one representation; a boolean
  pin and a chip disagreeing about the same question is the defect, not the chip.
- **Content Drive already did this.** Its store carries the note: *"The `archived: false` pin that
  used to sit here is gone — the endpoint already defaults it, and pinning it would contradict an
  Archived selection."* The picker is being brought to the same shape.
- **The endpoint defaults it.** Omitting the key excludes archived content, so removing the pin
  changes nothing for a picker with no Status selection.

The **public** `openBrowserModal` surface keeps its shape: `status?: 'live' | 'working' | 'archived'`
still accepts all three values. What changes is what `'archived'` does — it seeds the Status filter
instead of setting a flag, so it now means *archived only* rather than *working plus archived*. No
shipped caller passes it, and the version-state axis (`'live'` / `'working'`) is untouched.
