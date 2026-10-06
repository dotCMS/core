# Contract: AssetPicker sidebar ↔ store

**Components**: `DotAssetPickerSidebarComponent`, `withAssetFolderTree`, `DotAssetPickerStore`
**Serves**: FR-001 – FR-025, FR-030 – FR-032

---

## Sidebar composition (FR-001, FR-008, FR-013)

Three stacked controls, in this order. The host grid becomes three rows: `min-content min-content 1fr`.

```
┌─────────────────────────────────────┐
│ 1  dot-site        globe + hostname + chevron   │  min-content
├─────────────────────────────────────┤
│ 2  dot-search-input   "Search folders..."       │  min-content
├─────────────────────────────────────┤
│ 3  dot-folder-search-results  OR  dot-folder-tree│  1fr, scrolls
└─────────────────────────────────────┘
```

Row 3 is an either/or, never both:

```
@if (store.isSearchingFolders()) {
    @if (store.showResultsEmptyState()) { …empty state… }
    @else { <dot-folder-search-results …/>  @if (store.showRefineHint()) { …hint… } }
} @else {
    <dot-folder-tree …/>
}
```

---

## 1. Site selector — `DotSiteComponent`

| Binding | Value | Requirement |
|---|---|---|
| `value` | `store.browsingSite()?.identifier ?? null` | FR-001 |
| `showSystemHost` | `false` | FR-007 |
| `icon` *(new, optional)* | globe glyph | FR-001 |
| `class` | full width of the sidebar column | |
| `(onChange)` | `→ onSiteChange(identifier)` | FR-005 |

Sites-only filtering (FR-002) and lazy paging (FR-003) are **inherited**, not implemented — the component already queries `DotSiteService.getSites({ filter })` server-side and virtual-scrolls 40/page.

`onSiteChange` resolves the identifier to `{ identifier, hostname }` and calls `store.setBrowsingSite(...)`.

---

## 2. Folder search — `DotSearchInputComponent`

| Binding | Value | Requirement |
|---|---|---|
| `value` | `store.folderSearch()` | |
| `placeholder` | `'dot.asset.picker.sidebar.folder.search.placeholder'` (new key, *"Search folders..."*) | FR-008 |
| `testId` | `'asset-picker-folder-search-input'` | |
| `(search)` | `→ store.setFolderSearch($event)` | |

Debounce and the clear (`×`) affordance are inherited — they satisfy FR-019 and FR-016 without new code.

> The existing `dot.asset.picker.sidebar.search.placeholder` key (*"Search sites & folders"*) is retired with the control it labelled.

---

## 3a. Results — `DotFolderSearchResultsComponent`

See [its contract](./dot-folder-search-results.contract.md). Picker wiring supplies **no** `loadMoreLabelKey` (FR-020).

## 3b. Tree — `DotFolderTreeComponent` *(unchanged component, FR-029)*

Bindings are as today except `folders` now holds a **single root**. Two overrides make that root read as `All`, and **neither touches `DotBrowsingService.mapSiteToTreeNode()`** (which Content Drive depends on for its globe-icon, hostname-labelled site root) — see research R5.

**Label** — in the picker's own `#folderTreeNodeLabel` slot:

```
@if (node.data?.type === 'site') { All } @else { {{ node.label | dotFolderName }} }
```

**Icon** — on the node instance the store builds in `loadFolders()`, because PrimeNG reads it from the node, not from the label template:

```
expandedIcon:  'pi pi-folder-open'    // mapSiteToTreeNode gives site nodes 'pi pi-globe'
collapsedIcon: 'pi pi-folder'
```

The root must carry the **folder** affordance of the nodes beneath it (FR-021). The globe belongs to the site selector above it now; leaving it on the root would show the same idea twice and read as a second site control.

---

## Store surface

### New / changed methods

| Method | Behaviour | Requirements |
|---|---|---|
| `setBrowsingSite(site)` | Sets `browsingSite`; **clears `folderSearch` and `searchResults`**; clears `path`; resets paging; reloads the tree; re-scopes the asset list to the new site's root. | FR-005, FR-017 |
| `setFolderSearch(term)` | No-op if unchanged. Below 2 chars: clears `searchResults` to `null` (back to the tree), issues **no** request. At or above: runs the site-scoped recursive search. | FR-009 – FR-013, FR-016, FR-019 |
| `selectSearchResult(node)` | Scopes assets to that folder and sets `selectedNode`. **Leaves `searchResults` and `folderSearch` intact** — the list stays open. | FR-015 |
| `loadFolders()` | Builds the single `All` root from `browsingSite` and hydrates it down to `path`. **No sites query.** | FR-021, FR-022, FR-025 |
| `selectNode(node)` *(existing)* | Unchanged. Its `type === 'site'` branch is what makes selecting `All` scope to the whole site. | FR-023 |
| `expandNode` / `loadMore` *(existing)* | Unchanged **except** the removal of `loadMore`'s `isSitesLevel` / `SITES_LOAD_MORE_KEY` branch. | FR-024 |

### Removed

`sitesPage()`, the `SITES_LOAD_MORE_KEY` branch, and `searchFoldersInBrowsingSite`'s root re-synthesis (`with-asset-folder-tree.feature.ts:152-157`) — all dead once the term no longer filters the sites query.

---

## Search request contract

Unchanged endpoint, unchanged shape — `GET /api/v1/folder/search` via `DotBrowsingService.searchFolders()`:

```
siteId:   store.browsingSite().identifier   // FR-011, permission-filtered server-side
path:     '/'
recursive: true                             // FR-010
name:     <term>                            // FR-009, contains; ≥ 2 chars (FR-012)
page:     1                                 // FR-020 — first page only
per_page: DOT_FOLDER_TREE_PAGE_SIZE
```

`searchHasMore` ← `hasMorePages(pagination)` → drives the refine hint (FR-020).

---

## Initialisation contract (FR-006)

`buildAssetPickerConfig` already derives `browseSite` from the remembered location, falling back to the entry site for a legacy bare-path payload. **What is missing today** and must be added:

> the remembered site must be confirmed to still exist and be visible to the editor; if it does not resolve, fall back to `config.site` and open on its root — **without surfacing an error**.

Order of preference: remembered site (if it resolves) → entry site from the global site switcher.

---

## Non-goals (assert these stay true)

| Must not change | Requirement | Guarded by |
|---|---|---|
| Right column: toolbar, chips, table, pagination | FR-030 | existing specs pass unmodified |
| `withAssetBrowse`, `withAssetSelection` | FR-030 | existing specs |
| Mimetype / content-type / locale restrictions | FR-031 | existing specs |
| Picker absent from the legacy Dojo editor | FR-032 | `dot-asset-picker.component.legacy-host.spec.ts` unmodified |
| `DotFolderTreeComponent`, `site-tree.utils`, `DotBrowsingService` | FR-029 | no diff in those files |
| Content Drive sidebar and store | FR-034 | no diff in `libs/portlets/dot-content-drive/**` |
| `localStorage` payload shape | — | `last-asset-path.spec.ts` unmodified |
