# Contract: `DotFolderSearchResultsComponent` (new, shared)

**Location**: `core-web/libs/ui/src/lib/components/dot-folder-search-results/`
**Exported from**: `@dotcms/ui` (`libs/ui/src/index.ts`)
**Consumers**: AssetPicker sidebar, Site/Folder field. Content Drive is the deferred third (FR-034).
**Serves**: FR-014, FR-015, FR-026, FR-027, FR-028, FR-029

---

## What it is

A **presentational list** of folder-search results. It renders rows and emits selections. It owns no state, performs no I/O, and knows nothing about sites, stores, terms or paging.

## What it is deliberately NOT

| Not | Why |
|---|---|
| A mode of `DotFolderTreeComponent` | FR-029 — that component is shared with Content Drive and is being edited concurrently by #37174 |
| A tree | The result set is flat: no expansion, no indentation, no toggler. `role="list"`, not `role="tree"`. |
| The owner of paging | FR-028 — the Site/Folder field pages its results, the picker caps at one page (FR-020) |
| The owner of the empty state | Consumers render their own, so the Site/Folder field's current empty state is preserved byte-for-byte (FR-027 / SC-010) |
| The owner of the search input | That is `DotSearchInputComponent` (picker) or the field's own `p-iconField` |

## Inputs

| Input | Type | Default | Contract |
|---|---|---|---|
| `results` | `TreeNodeItem[]` | `[]` | Flat, pre-filtered. The component does **not** filter, sort or paginate. May contain a trailing `LOAD_MORE_NODE_TYPE` sentinel, which is rendered as the load-more row rather than as a result. |
| `selectedKey` | `string \| null` | `null` | Selection by **key**, not by object reference. Consumers re-publish result arrays (clones) on paging; a reference-based selection silently loses its highlight. |
| `loading` | `boolean` | `false` | Renders the in-list loading affordance. Does not blank the existing rows. |
| `loadMoreLabelKey` | `string` | `''` | i18n key for the load-more row. Omit to render no load-more row. |
| `listTestId` | `string` | `'dot-folder-search-results'` | Consumers render more than one list per screen |
| `rowTestId` | `string` | `'folder-search-result'` | |

## Outputs

| Output | Payload | Contract |
|---|---|---|
| `resultSelect` | `TreeNodeItem` | Emitted on row activation (click **and** keyboard). Never emitted for the load-more sentinel. |
| `loadMore` | `TreeNodeItem` | The sentinel node. Only emitted when a load-more row is rendered. |

## Row rendering contract

Each result row renders, in order:

1. a folder icon, vertically centred, non-shrinking;
2. **line 1** — the folder name via the existing `dotFolderName` pipe, semibold, `truncate`;
3. **line 2** — the full path via `formatFolderSearchPath(node)`, smaller, muted, `truncate`.

Both lines truncate with an ellipsis; **neither wraps, and the row never widens its container** (FR-014, SC-008). The row is a real interactive control (`<button type="button">`) so keyboard activation and focus rings come for free.

Selected state is driven solely by `selectedKey === node.key`.

## `formatFolderSearchPath(node: TreeNodeItem): string`

Pure function, co-located, separately unit-tested. **Moved from** `host-folder-field.component.ts:344` (`formatSearchNodePath`) unchanged in behaviour.

| Input | Output |
|---|---|
| hostname `//demo.dotcms.com`, path `/` (or absent) | `demo.dotcms.com` |
| hostname `//demo.dotcms.com`, path `/activities/` | `demo.dotcms.com / activities` |
| hostname `//demo.dotcms.com`, path `/images/thumbnails/` | `demo.dotcms.com / images / thumbnails` |
| hostname absent | path segments only |

Rules: strip a leading `//` from the hostname; trim leading/trailing slashes from the path; drop empty segments; join with `' / '`.

## Invariants a consumer must uphold

1. `results` is already scoped — the component does no filtering (site scoping per FR-011 is the caller's job, server-enforced).
2. `selectedKey` refers to a node present in `results`, or is `null`.
3. If no `loadMoreLabelKey` is supplied, `results` must not contain a load-more sentinel.

## Consumer wiring

**AssetPicker sidebar** — no load-more (FR-020):

```
results     = store.displayedResults()
selectedKey = store.selectedResultKey()
loading     = store.searchStatus() === LOADING
(no loadMoreLabelKey)
resultSelect → store.selectSearchResult($event)
```

**Site/Folder field** — keeps its existing paging (FR-028), replacing the `dot-folder-tree` + `#folderTreeNodeLabel` block used while `isSearching()`:

```
results          = store.searchResults() ?? []
selectedKey      = store.treeSelection()?.key ?? null
loading          = store.searchLoading()
loadMoreLabelKey = 'dot.file.field.host.folder.action.load.more'
resultSelect     → onFolderSelect(...)
loadMore         → onLoadMoreNode(...)
```

Its browse-mode `dot-folder-tree` block, its empty state, its error states and its footer are **untouched**.

## Acceptance (drives the spec file)

- renders one row per result, name and formatted path on separate lines
- applies truncation classes to both lines; no wrapping
- marks exactly the row whose `key === selectedKey`
- emits `resultSelect` on click and on keyboard activation
- does **not** emit `resultSelect` for a load-more sentinel
- renders no load-more row when `loadMoreLabelKey` is empty
- renders nothing (not an empty state) when `results` is `[]`
- `formatFolderSearchPath` covers: root, one segment, nested, missing hostname, stray slashes
