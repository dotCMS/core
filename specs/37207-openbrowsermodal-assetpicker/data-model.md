# Phase 1 Data Model: `openBrowserModal` → AssetPicker

**Feature**: [spec.md](./spec.md) | **Research**: [research.md](./research.md) | **Date**: 2026-08-28

Entities are grouped by the layer that owns them. **New**, **extended** and **replaced** entities
are the work; **untouched** ones are listed only where their stability matters.

---

## 1. `DotCustomFieldApi` browse contract — REPLACED

`core-web/libs/edit-content-bridge/src/lib/interfaces/` — rename
`browser-selector.interface.ts` → `asset-browser.interface.ts`.

The API is **new and unshipped**, so it is redesigned rather than preserved (FR-010). Full shape and
rationale in [Contract 1](./contracts/openbrowsermodal-public-api.md).

| Entity | Status | Note |
|---|---|---|
| `DotBrowserOptions` | ✨ new | `title?`, `kinds?`, `status?`, `path?`, `mimeTypes?`, `extensions?`, `sort?` |
| `DotBrowserSelection` | ✨ new | Union discriminated by `kind` |
| `DotBrowserItemKind` | ✨ new | `'file' \| 'dotasset' \| 'page' \| 'folder' \| 'link'` |
| `DotBrowserHandle` | ✨ new | `{ result: Promise<DotBrowserSelection \| null>; close(): void }` |
| `BrowserSelectorOptions` / `Result` / `Controller` | ❌ removed | Replaced by the above |
| `ContentByFolderParams` | ➖ untouched | Stays in `dotcms-models`; this interface no longer depends on it |

**Validation rules**
- Every variant carries `kind`, `identifier`, `title` and a **non-empty** `url` (FR-011, FR-012).
- Contentlet-only fields appear only on variants that have them — a folder has no `mimeType`.
- `result` resolves exactly once; `null` means cancelled; it never rejects on cancel (FR-013).
- Defaults reproduce today's asset-only browsing: `kinds: ['file','dotasset']`, `status: 'working'`.

### Result population by `kind`

| Field | `file` / `dotasset` | `page` | `folder` | `link` |
|---|---|---|---|---|
| `kind` | `'file'` / `'dotasset'` | `'page'` | `'folder'` | `'link'` |
| `identifier` | ✓ | ✓ | ✓ | ✓ |
| `title` | ✓ | ✓ | ✓ | ✓ |
| `url` | `url ?? urlMap` | page URL | folder `path` | link target |
| `inode` | ✓ | ✓ | ✓ | ✓ |
| `name` | `name ?? fileName` | — | — | — |
| `mimeType` | ✓ | — | — | — |
| `baseType` | ✓ | `'HTMLPAGE'` | — | — |
| `contentType` | ✓ | ✓ | — | — |

Compare with the old shape, where every column was required on every row — which is what made
folders and menu links unrepresentable.

---

## 2. Frontend browse models — EXTENDED

`core-web/libs/dotcms-models/src/lib/dot-content-drive.model.ts`

### 2.1 `DotContentDriveLink` — NEW

Third variant of the item union. Shaped from the link map `BrowserAPIImpl` emits
(`extension: 'link'`, lines 2667 / 2848).

| Field | Type | Note |
|---|---|---|
| `type` | `'link'` | Discriminant, mirroring `DotContentDriveFolder.type: 'folder'` |
| `extension` | `'link'` | What the backend actually stamps; the row-kind test |
| `identifier` | `string` | |
| `inode` | `string` | |
| `title` | `string` | |
| `url` | `string` | The link target — the value `DotBrowserSelection.url` carries |
| `hostId` | `string` | |
| `modDate` | `number` | |

**Relationship**: `DotContentDriveItem = DotCMSContentlet | DotContentDriveFolder | DotContentDriveLink`

**Validation**: `url` must be non-empty — a link with no target cannot satisfy FR-012.

### 2.2 `DotContentDriveSearchRequest` — EXTENDED

| Field | Change |
|---|---|
| `showLinks?: boolean` | **new** — #37112, default `false` |
| `linkCursor?: number` | **new** — #37112, default `0` |
| `extensions?: string[]` | **new** — Group E, default absent |

Existing `showFolders`, `folderCursor`, `contentCursor`, `live`, `archived`, `sortBy`, `baseTypes`,
`mimeTypes` are already present and unchanged.

### 2.3 `DotContentDriveSearchResponse` — EXTENDED

| Field | Change |
|---|---|
| `hasMoreLinks?: boolean` | **new** — #37112 |
| `nextLinkCursor?: number` | **new** — #37112 |

---

## 3. Picker configuration — EXTENDED

`core-web/libs/ui/src/lib/components/dot-asset-picker/store/models.ts`

### 3.1 `DotAssetPickerBrowseOptions` — NEW

Opt-in sub-object. **Absent ⇒ today's behavior, byte-identical** (FR-007).

| Field | Type | Default when `browse` absent |
|---|---|---|
| `showFolders` | `boolean` | `false` (today's pinned invariant) |
| `showLinks` | `boolean` | `false` |
| `showWorking` | `boolean` | picker default |
| `showArchived` | `boolean` | `false` (today's hardcoded `archived: false`) |
| `sortByDesc` | `boolean` | picker default |
| `extensions` | `string[]` | absent |

### 3.2 `DotAssetPickerConfig` — EXTENDED

One new optional field: `browse?: DotAssetPickerBrowseOptions`.

**Validation rules**
- `kinds` containing `'link'` together with `mimeTypes` is unsatisfiable upstream (R5): the endpoint
  drops links. Log a developer warning; do not work around (FR-036).
- `allowedBaseTypes` gains `HTMLPAGE` **only** when the caller asks for pages. The four asset entry
  points keep `[DOTASSET, FILEASSET]` exactly (FR-008).

### 3.3 `DotAssetPickerMode` — EXTENDED

`'file' | 'image' | 'video' | 'audio' | 'browse'`.

`'browse'` is the `openBrowserModal` entry point: no mimetype narrowing, base types derived from
the caller's `kinds`, and the only mode that may set `browse`.

**State transition**: `ASSET_PICKER_MIME_TYPES` has no `'browse'` key, so the existing
"presence in the map makes it a media mode" test (`asset-picker-config.ts`) keeps working
untouched — `'browse'` is a non-media mode like `'file'`.

---

## 4. Picker paging state — EXTENDED

### 4.1 `DotAssetPickerPage` — EXTENDED

Today `{ contentCursor, hasMoreContent }`. Three streams page independently (R3), so a page
bookmark must record all three.

| Field | Status |
|---|---|
| `contentCursor` | existing |
| `hasMoreContent` | existing |
| `folderCursor` | **new** |
| `hasMoreFolders` | **new** |
| `linkCursor` | **new** |
| `hasMoreLinks` | **new** |

**Validation rules**
- Page 1 is `{ 0, 0, 0, true, true, true }`.
- A page's cursors come from the previous response's `next*Cursor`.
- When `hasMoreFolders`/`hasMoreLinks` is `false`, the next request sets
  `showFolders`/`showLinks` to `false` — the endpoint's documented contract.
- Invariant to retire: `folderCursor: 0` is no longer unconditional
  (`with-asset-browse.feature.ts:132`) — it stays `0` only while `showFolders` is `false` (FR-002).

### 4.2 `DotAssetPickerSelectionState` — EXTENDED

`selectedAsset: DotCMSContentlet | null` → `DotContentDriveItem | null` (R1).

**State transitions**
| From | Event | To |
|---|---|---|
| `null` | row chosen | that item |
| item | selection cleared | `null` |
| item | new result set drops it | `null` (existing behavior) |

**Validation**: `confirm()` hydrates **only** when the item is a contentlet. Folders and links close
with the row as-is (R1).

---

## 5. Backend — EXTENDED

### 5.1 `AbstractDriveRequestForm` — EXTENDED

`dotCMS/src/main/java/com/dotcms/rest/api/v1/drive/AbstractDriveRequestForm.java`

| Field | Type | Default | Note |
|---|---|---|---|
| `extensions` | `@Nullable List<String>` | `null` | New. `null`/empty ⇒ no filtering (FR-033) |

`showLinks`, `linkCursor`, `showFolders`, `folderCursor`, `contentCursor`, `live`, `archived`,
`sortBy`, `mimeTypes`, `baseTypes` already exist — no change.

### 5.2 `ContentDriveHelper` — EXTENDED

Wire `requestForm.extensions()` → `BrowserQuery.Builder.showExtensions(...)`.

### 5.3 `BrowserAPIImpl` — EXTENDED

The real work (R4). `getPaginatedContents(...)` must apply the extension predicate **in SQL**,
mirroring `appendMIMETypeQuery` (line 2602).

**Validation rules**
- **Constitution III**: bind parameters. Do **not** copy `appendMIMETypeQuery`'s `String.format`
  interpolation — `extensions` is caller-supplied REST input.
- **ADR-0018**: resolved in the database. Never post-filter a cursor page (breaks
  `hasMoreContent` / `nextContentCursor`, and SC-005).
- Absent/empty `extensions` appends nothing, so every existing query plan is unchanged.

---

## Entity relationship summary

```
DotBrowserOptions  { kinds, status, path, mimeTypes, extensions, sort }   [REDESIGNED]
                                                        │
                                    AngularFormBridge.openBrowserModal()
                                                        │
                                      buildAssetPickerConfig(mode:'browse')
                                                        ▼
                                        DotAssetPickerConfig
                                          └─ browse?: DotAssetPickerBrowseOptions   [NEW]
                                                        │
                                             DotAssetPickerStore
                                          ├─ pages: DotAssetPickerPage[]      [3 cursors]
                                          └─ selectedAsset: DotContentDriveItem
                                                        │
                                    DotContentDriveSearchRequest  [+showLinks,+linkCursor,+extensions]
                                                        ▼
                                        POST /api/v1/drive/search
                                                        │
                                   ContentDriveHelper → BrowserQuery → BrowserAPIImpl
                                                        ▼
                                    DotContentDriveSearchResponse [+hasMoreLinks,+nextLinkCursor]
                                          └─ list: DotContentDriveItem[]
                                               = Contentlet | Folder | Link   [Link NEW]
                                                        │
                                              confirm() ─ contentlet? hydrate : as-is
                                                        ▼
                                    DotBrowserSelection  (union on `kind`)   [REDESIGNED]
                                                        ▼
                                    DotBrowserHandle.result : Promise<… | null>
```
