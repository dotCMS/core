# Phase 1 Data Model: type and state deltas

**Feature**: [`spec.md`](./spec.md) · **Plan**: [`plan.md`](./plan.md) · **Research**: [`research.md`](./research.md)

There is no persistent data model here — nothing is stored, migrated or serialized. The "entities"
this fix changes are **TypeScript types and store state**. Each delta below is the authoritative
target shape; `/speckit-tasks` turns them into tasks.

---

## 1. `DotBrowserItemKind` — the VTL-facing kind vocabulary

`core-web/libs/edit-content-bridge/src/lib/interfaces/asset-browser.interface.ts:19`

| Before | After |
|---|---|
| `'file' \| 'dotasset' \| 'page' \| 'folder' \| 'link'` | `'file' \| 'dotasset' \| 'page' \| 'link'` |

**Validation rule** (new, enforced at runtime not by the type — see [R3](./research.md#r3)): a
`kinds` entry that is not one of the four is ignored, and the bridge warns once naming it. A VTL
caller can pass any string, so the type alone guarantees nothing.

**Consumers**: `angular-form-bridge.ts` only (`:18`, `:622`, `:635`, `:677`). Not in the lib's public
index — see [R8](./research.md#r8).

---

## 2. `DotBrowserSelection` — what a pick resolves to

`asset-browser.interface.ts:132-135` and the union that follows

| Member | Fate |
|---|---|
| `DotBrowserAssetSelection` (file / dotasset) | unchanged |
| `DotBrowserPageSelection` | unchanged |
| `DotBrowserLinkSelection` | unchanged |
| **`DotBrowserFolderSelection`** | **removed**, along with its arm of the `DotBrowserSelection` union (`:150-154`) |

**State transition removed**: there is no longer a path by which `onClose(selection)` receives
`kind: 'folder'`. The mapper's `kind === 'folder' || kind === 'link'` branch
(`angular-form-bridge.ts:724`) narrows to links only, and `kindOf`'s `item['type'] === 'folder'`
branch (`:678-679`) is deleted — nothing in `items` can be a folder any more, so the branch is
unreachable rather than merely unused.

---

## 3. `DotAssetPickerBrowseOptions` — the picker's browse opt-ins

`core-web/libs/ui/src/lib/components/dot-asset-picker/store/models.ts:26-32`

| Field | Fate |
|---|---|
| **`showFolders?: boolean`** | **removed** |
| `showLinks?: boolean` | unchanged — the link stream stays (#37112) |
| `showWorking?: boolean` | unchanged |
| `showArchived?: boolean` | unchanged |
| `sortByDesc?: boolean` | unchanged |
| `sortField?: string` | unchanged |

The interface's own TSDoc still says *"Absent, the picker behaves exactly as it always has: assets
only, no folders, no links…"* — that sentence stays true and now describes **every** case, not just
the default. The `showFolders` field doc (which currently reads "List folders alongside assets, and
allow one to be returned") goes with the field.

---

## 4. `DotAssetPickerPage` — the per-page cursor bookmark

`store/models.ts:185-192`, seeded by `store/constants.ts:13-21`

| Field | Fate |
|---|---|
| `contentCursor: number` / `hasMoreContent: boolean` | unchanged |
| **`folderCursor: number`** / **`hasMoreFolders: boolean`** | **removed** ([R4](./research.md#r4), [R5](./research.md#r5)) |
| `linkCursor: number` / `hasMoreLinks: boolean` | unchanged |

`DEFAULT_ASSET_PICKER_PAGE` loses the same two fields. Safe because a `showFolders: false` response
always returns `hasMoreFolders: false` and echoes back the `folderCursor` it was sent — both would
be constants (`BrowserAPIImpl.java:1705-1730`).

---

## 5. The outgoing request — `DotContentDriveSearchRequest`

Built in `store/features/with-asset-browse.feature.ts:118-172`. The **type** is shared with Content
Drive (`libs/dotcms-models/src/lib/dot-content-drive.model.ts:246, 298`) and is **not** changed —
Content Drive still uses both keys.

| Key | Before (picker) | After (picker) |
|---|---|---|
| `showFolders` | `Boolean(browse?.showFolders) && (bookmark?.hasMoreFolders ?? true)` (`:129-130`) | constant **`false`** — never omitted ([R2](./research.md#r2)) |
| `folderCursor` | `bookmark?.folderCursor ?? 0` (`:151`) | **key not sent** (server default `0`) |
| `showLinks` / `linkCursor` | conditional on `browse?.showLinks` | unchanged |
| `contentCursor` | `bookmark?.contentCursor ?? 0` | unchanged |
| everything else | — | unchanged |

> ⚠️ `showFolders` must be **present and `false`**. The server default is `true`
> (`AbstractDriveRequestForm.java:395-397`); omitting the key relists folders.

---

## 6. Derived state — `$totalRecords`

`with-asset-browse.feature.ts:79-89`

| Before | After |
|---|---|
| `hasMoreContent \|\| hasMoreFolders \|\| hasMoreLinks` (`:85`) | `hasMoreContent \|\| hasMoreLinks` |

The *purpose* is unchanged and must stay intact: claim one page beyond while **any** surviving
stream reports more, so a stream that outlives the others stays reachable. Two streams instead of
three; the invariant is the same. This is the #37207 TC-007 surface.

---

## 7. The shipped template

`dotCMS/src/main/webapp/WEB-INF/velocity/static/content/file_browser_field_render_new.vtl:23`

| Before | After |
|---|---|
| `kinds: ["file", "page", "folder"],` | `kinds: ["file", "page"],` |

Nothing else in the template changes — notably not the free-text `#vlUri` input (`:38-44`), which is
how a folder path can still be *set*.

`redirect_custom_field_new.vtl:57` already reads `kinds: ["page", "link"]` and is **not edited**.

---

## 8. Documentation surface (AC-008)

Not types, but part of the contract and therefore listed here so nothing is missed:

| Location | What must change |
|---|---|
| `form-bridge.interface.ts:52-53` | `openBrowserModal` summary — "an asset, a page, a folder or a menu link" drops the folder |
| `asset-browser.interface.ts` `kinds` doc (`:31-40`) | state that folders are navigation-only, cannot be requested and cannot be returned |
| `asset-browser.interface.ts:6-9` (file header) | mentions "a folder or a menu link" as the shapes the old API mis-modelled — keep the history, but stop implying folders are still returnable |
| `store/models.ts:15-24` | browse-options doc block |
| `with-asset-browse.feature.ts:52, 74-77, 148-150, 167-169` | four comment blocks describing folders as a listable, pageable stream |

---

## Tests that must be **inverted**, not just added

Flagged here because it sharpens what "Red" means for the TDD gate — existing tests assert exactly
the behavior being withdrawn and will fail *by design* once rewritten.

> **Corrected during `/speckit-implement`**: this list said four. The real count is **six**. Two more
> surfaced only once the source changed, both in `angular-form-bridge.spec.ts`:
> `should report a folder with its path as the url` (deleted — a folder can no longer be picked, so
> there is no selection to report) and the `folder` case of
> `should not attach contentlet-only fields to a %s` (case removed, `link` kept). A spec-file grep
> for the withdrawn *behavior* would have caught them; a grep for the withdrawn *types* did not.

| Test | File:line | Current assertion | Becomes |
|---|---|---|---|
| `should ask for folders when the caller does` | `dot-asset-picker.store.spec.ts:438` | `$request().showFolders` is `true` | `false` — and the test name must change with it |
| `should no longer pin the folder cursor to zero` | `:475` | `$request().folderCursor` is `5` | removed; the key is no longer sent |
| `should stop asking for folders once there are no more` | `:501` | `true` on page 1, `false` on page 2 | removed; there is no folder stream to exhaust |
| `should map folder and link kinds to browse options` | `angular-form-bridge.spec.ts:957` | `{ showFolders: true, showLinks: true }` | `showLinks` only, plus a warning for the unknown `folder` kind |

Two more that must keep passing **unchanged**, as the regression guard:

- `should keep Next reachable while menu links remain` (`:640`)
- `should settle on the exact total once every stream is exhausted` (`:658`)
