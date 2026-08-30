# Phase 1 — Data Model: AssetPicker new sidebar UI

**Feature**: [spec.md](./spec.md) | **Plan**: [plan.md](./plan.md) | **Date**: 2026-08-25

No database, no REST contract, no serialized-payload change. "Data model" here is **client state**: the shape of the AssetPicker store's folder-tree slice and the entities that flow through the sidebar.

---

## Entities

### `DotAssetPickerSite` *(existing — unchanged)*

The one site the picker is pinned to. Already in `store/models.ts`.

| Field | Type | Notes |
|---|---|---|
| `identifier` | `string` | Site id — scopes every folder query (`siteId`) |
| `hostname` | `string` | Display label; also used to build node keys and result paths |

Sourced from `DotSiteComponent`'s selection (a `DotSite`, narrowed to these two fields) or from the remembered location.

---

### `TreeNodeItem` *(existing — unchanged)*

Shared node shape (`@dotcms/dotcms-models`) used for both tree nodes and flat search results. Relevant fields:

| Field | Type | Notes |
|---|---|---|
| `key` | `string` | Identity for selection re-resolution after a tree clone |
| `label` | `string` | Folder name (rendered through `dotFolderName`) |
| `data.type` | `'site' \| 'folder' \| 'load-more'` | The single `All` root keeps `'site'` — see research R5 |
| `data.id` | `string` | Folder or site identifier |
| `data.path` | `string` | Path within the site; `'/'` or absent for the root |
| `data.hostname` | `string` | Owning site's hostname — the first segment of a result's path line |
| `children` / `leaf` / `expanded` / `loading` | | Tree-only; unused by the flat result list |

**Invariant (new)**: in the picker, exactly one node with `data.type === 'site'` exists, and it is always the root. This is what lets `resolveSiteId()` and `selectNode()`'s site-root branch keep working unchanged.

---

### `DotAssetPickerLocation` *(existing — unchanged)*

The `localStorage` payload (`dotcms.asset-picker.lastPath`). **Shape is not modified** — including `readLastAssetLocation`'s legacy bare-path branch. What changes is that its `siteId` now seeds a *user-visible control* rather than an implicit expanded root, which is why FR-006 adds a visibility check that does not exist today.

---

## Store state: `DotAssetPickerFolderTreeState`

### Before

```
folders:       TreeNodeItem[]      // roots = every browsable site
selectedNode:  TreeNodeItem | null
foldersStatus: ComponentStatus
treeSearch:    string              // ONE term, applied to sites AND folders
```

### After

```
folders:        TreeNodeItem[]           // exactly one root: the `All` site root
selectedNode:   TreeNodeItem | null
foldersStatus:  ComponentStatus
folderSearch:   string                   // renamed; folders only, never sites
searchResults:  TreeNodeItem[] | null    // null = not searching; [] = searched, no matches
searchStatus:   ComponentStatus          // distinct from foldersStatus (FR-018)
searchHasMore:  boolean                  // FR-020 — drives the "narrow your search" hint
```

**Why each addition**

| Field | Requirement | Why it cannot be derived |
|---|---|---|
| `searchResults` as `TreeNodeItem[] \| null` | FR-013, FR-018 | `null` (not searching) and `[]` (searched, nothing matched) must be distinguishable, or the empty state fires before the user has searched. Mirrors the Site/Folder field's existing `searchResults: TreeNodeItem[] \| null`. |
| `searchStatus` separate from `foldersStatus` | FR-018 | A failed *search* and a failed *tree load* render differently; one status field collapses them, which is the bug the current sidebar has. |
| `searchHasMore` | FR-020 | Comes from the response's `pagination` via `hasMorePages()`; nothing in `searchResults` records that more exist. |

### Removed

| Removed | Why |
|---|---|
| Sites as tree roots | Replaced by the explicit selector (FR-022) |
| `sitesPage()` helper | The store no longer queries sites at all |
| `SITES_LOAD_MORE_KEY` branch in `loadMore` | There is no sites level left to page |
| `searchFoldersInBrowsingSite`'s root re-synthesis | The workaround's cause is gone (research R6) |

---

## Derived state (computed)

| Name | Derivation | Serves |
|---|---|---|
| `isSearchingFolders` | `folderSearch().trim().length >= MIN_TREE_SEARCH_LENGTH` | FR-012, FR-013 — the tree/results switch |
| `displayedResults` | `searchResults() ?? []` | FR-013 |
| `showResultsEmptyState` | `isSearchingFolders() && searchStatus() === LOADED && displayedResults().length === 0` | FR-018 — never fires on a failure or mid-flight |
| `showRefineHint` | `isSearchingFolders() && searchHasMore()` | FR-020 |
| `selectedResultKey` | `selectedNode()?.key ?? null` | FR-015 — selection survives result-list re-publishes |

---

## State transitions

```
                    ┌──────────────── clear term (FR-016) ─────────────────┐
                    │                                                      │
              ┌─────▼─────┐   term ≥ 2 chars (FR-012)   ┌──────────────┐   │
   ┌─────────►│   TREE    │ ──────────────────────────► │   SEARCHING  │───┘
   │          │  (All …)  │                             │ (flat list)  │
   │          └─────┬─────┘                             └──────┬───────┘
   │                │                                          │
   │      select node → scope assets                  select result → scope assets,
   │      (FR-023 root / FR-024 folder)                LIST STAYS OPEN (FR-015)
   │                                                          │
   └──────── change site (FR-017: clears term, reloads tree) ◄─┘
```

**The two rules that are easy to get wrong**

1. **Selecting a search result must not leave SEARCHING.** FR-015 requires the list to stay up with the row marked selected. The existing tree code already models this distinction: `TreeLoadResult.selectedNode` is *optional* precisely so a search can change what is shown without moving where the assets are pointed — but here the opposite is needed, so the result selection sets `selectedNode` **and** leaves `searchResults` intact.
2. **Changing site is the only transition that clears the term.** FR-017. Clearing the input (FR-016) returns to the tree but leaves the site alone.

---

## Validation rules

| Rule | Where enforced | Requirement |
|---|---|---|
| Folder term ≥ 2 characters before any request | Client (`isSearchingFolders`) **and** server (`FolderResource.java:608`) | FR-012 |
| Folder search always carries `siteId` of the browsed site | Store, when building the request | FR-011 — server-enforced via permission filtering (ADR-0020) |
| Folder search is `recursive: true` | Store | FR-010 |
| System Host never offered as a browsable site | `<dot-site [showSystemHost]="false">` | FR-007 |
| Remembered site must still resolve and be visible, else fall back to the entry site | `buildAssetPickerConfig` / store init | FR-006 — **gap today**: `browseSite` is taken from `localStorage` with no existence check |
| Results superseded by a newer term are discarded | `switchMap` in the search `rxMethod` + `dot-search-input`'s debounce | FR-019 |

---

## Component data contracts

Full contracts in [`contracts/`](./contracts/). Summary of what flows where:

| Component | In | Out |
|---|---|---|
| `DotSiteComponent` | `value` (site identifier), `showSystemHost=false`, `icon` (new, optional) | `onChange(identifier)` → `store.setBrowsingSite(...)` |
| `DotSearchInputComponent` | `value` = `folderSearch()`, `placeholder`, `testId`, debounce | `search(term)` → `store.setFolderSearch(term)` |
| `DotFolderSearchResultsComponent` *(new)* | `results`, `selectedKey`, `loading`, optional `hasMore` | `resultSelect(node)`, optional `loadMore(node)` |
| `DotFolderTreeComponent` *(unchanged)* | `folders` (one `All` root), `selectedNode`, `pt`, testIds | `onNodeSelect`, `onNodeExpand`, `loadMore` |
