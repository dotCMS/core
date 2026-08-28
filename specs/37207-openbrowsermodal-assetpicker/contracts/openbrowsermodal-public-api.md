# Contract 1 — `DotCustomFieldApi.openBrowserModal()` (REDESIGNED)

**Audience**: custom-field VTL template authors.
**Stability**: **new and unshipped** — no customer consumers. Verified: `file_browser_field_render.vtl`
and `redirect_custom_field.vtl` are dispatchers (`#if($structures.isNewEditModeEnabled())` →
`_new.vtl`, else `_old.vtl`), so the `_new` templates only render when the new Edit Content is on,
which is not the default. `browser-selector.interface.ts` has never been versioned (one commit,
#34126).
**Change in this feature**: **replaced**. Backward compatibility is not required (FR-010).

> This freedom is scoped to `DotCustomFieldApi`. `/api/v1/drive/search` has other consumers and
> stays backward-compatible — see [Contract 2](./drive-search-delta.md).

Target file: `core-web/libs/edit-content-bridge/src/lib/interfaces/browser-selector.interface.ts`
(rename to `asset-browser.interface.ts` — the old name refers to the legacy component that is no
longer the implementation).

---

## New shape

```ts
/** What may be listed, and what was selected. */
type DotBrowserItemKind = 'file' | 'dotasset' | 'page' | 'folder' | 'link';

interface DotBrowserOptions {
    /** Dialog title. */
    title?: string;
    /** What may be listed and returned. @default ['file', 'dotasset'] */
    kinds?: DotBrowserItemKind[];
    /** Version state. @default 'working' */
    status?: 'live' | 'working' | 'archived';
    /** Starting folder, dotCMS path form `//site/folder/`. Omit for the picker's default. */
    path?: string;
    /** Narrow file assets by MIME type. Incompatible with `kinds: ['link']` — see V1. */
    mimeTypes?: string[];
    /** Narrow file assets by extension. */
    extensions?: string[];
    /** @default { field: 'modDate', direction: 'asc' } */
    sort?: { field: string; direction: 'asc' | 'desc' };
}

interface DotBrowserSelectionBase {
    kind: DotBrowserItemKind;
    identifier: string;
    title: string;
    /** Always non-empty — the value a field stores. */
    url: string;
}

type DotBrowserSelection =
    | (DotBrowserSelectionBase & {
          kind: 'file' | 'dotasset';
          inode: string; name: string;
          mimeType: string; baseType: string; contentType: string;
      })
    | (DotBrowserSelectionBase & {
          kind: 'page';
          inode: string; baseType: 'HTMLPAGE'; contentType: string;
      })
    | (DotBrowserSelectionBase & { kind: 'folder'; inode: string })
    | (DotBrowserSelectionBase & { kind: 'link';   inode: string });

interface DotBrowserHandle {
    /** Resolves with the selection, or `null` when cancelled. Never rejects on cancel. */
    readonly result: Promise<DotBrowserSelection | null>;
    /** Closes programmatically; `result` resolves `null`. */
    close(): void;
}

openBrowserModal(options?: DotBrowserOptions): DotBrowserHandle;
```

### Removed

`BrowserSelectorOptions`, `BrowserSelectorResult`, `BrowserSelectorController`, and the
`ContentByFolderParams` dependency in this interface. `ContentByFolderParams` itself stays in
`dotcms-models` — other code uses it.

---

## What each change fixes

| # | Was | Now | Fixes |
|---|---|---|---|
| 1 | Result carried `inode`/`mimeType`/`baseType`/`contentType` for everything | Union on `kind` | A folder can no longer be asked for a mimetype. Removes the root cause of the selection-pipeline break (R1) |
| 2 | `onClose(result \| null)` + controller | `handle.result` Promise + `close()` | `await` instead of callback nesting |
| 3 | 5 booleans for content kind | `kinds[]` | Invalid combinations unrepresentable |
| 4 | `showWorking` + `showArchived` | `status` | 3 states in 3 values, not 2 booleans |
| 5 | `sortByDesc: boolean` | `sort: { field, direction }` | Matches the Drive API's `field:direction` |
| 6 | `hostFolderId: string`, **required**, id-based | `path?: string`, optional, path-based | It was required yet unused; path-based aligns with `assetPath` and avoids the `byPath` endpoint ADR-0020 deprecates (R6) |

---

## Behavioral guarantees

| # | Guarantee | Requirement |
|---|---|---|
| B1 | The dialog title is `options.title` | FR-015 |
| B2 | `result` resolves once with a selection whose `url` is non-empty and whose `kind` is correct | FR-011, FR-011a, FR-012 |
| B3 | ✕, `Esc`, or mask click resolves `result` with `null` **exactly once**; never rejects | FR-013 |
| B4 | `handle.close()` closes the dialog and resolves `result` with `null` once | FR-014 |
| B5 | The dialog opens inside the Angular zone (callers are outside Angular) | FR-016 |
| B6 | Every option reaches the browse request per *Capability Mapping* | FR-017 |
| B7 | Defaults reproduce asset-only browsing: `kinds: ['file','dotasset']`, `status: 'working'` | FR-007 |

### Host behavior (spec Governing rule)

| Bridge | Host | Behavior |
|---|---|---|
| `AngularFormBridge` | New Angular Edit Content | Opens `DotAssetPickerComponent` |
| `DojoFormBridge` | Legacy Dojo edit contentlet | Opens nothing. Returns a well-formed handle whose `result` resolves `null`, **plus a developer warning** — today's silent no-op is indistinguishable from a cancel (FR-022, FR-037) |

---

## Consumers — must be migrated in the same change (FR-010a, FR-018)

Both are dotCMS-owned and are the acceptance gate.

### `WEB-INF/velocity/static/content/file_browser_field_render_new.vtl:21`

```diff
- DotCustomFieldApi.openBrowserModal({
-   header: "$text.get('Select-a-file')",
-   params: { showFiles: true, showPages: true, showFolders: true,
-             showDotAssets: false, showWorking: false, showArchived: false, sortByDesc: true },
-   onClose: (result) => { if (result && result.url) { vlUriInput.value = result.url;
-                                                      field.setValue(result.url); } }
- });
+ const { result } = DotCustomFieldApi.openBrowserModal({
+   title: "$text.get('Select-a-file')",
+   kinds: ["file", "page", "folder"],
+   status: "live",
+   sort: { field: "modDate", direction: "desc" }
+ });
+ const selection = await result;
+ if (selection) { vlUriInput.value = selection.url; field.setValue(selection.url); }
```

### `WEB-INF/velocity/static/htmlpage_assets/redirect_custom_field_new.vtl:55`

```diff
- params: { showLinks: true, showPages: true, showFiles: false, showFolders: false,
-           showDotAssets: false, showWorking: false, showArchived: false, sortByDesc: true },
+ kinds: ["page", "link"],
+ status: "live",
+ sort: { field: "modDate", direction: "desc" }
```

Note both drop `showDotAssets: false` — with `kinds` it is simply absent from the list. That is the
redesign's point.

---

## Contract test checklist

- [ ] Each of the five kinds returns the right `kind` and a non-empty `url` (B2, SC-002, SC-003)
- [ ] Contentlet-only fields are absent from folder and link selections (FR-011)
- [ ] All four cancel paths resolve `null` exactly once and never reject (B3, B4, SC-006)
- [ ] Defaults with no `options` produce asset-only browsing (B7)
- [ ] Each option reaches the search request (B6)
- [ ] `DojoFormBridge` resolves `null` and warns (FR-037)
- [ ] Both templates updated; no reference to the removed types remains (FR-010a)
- [ ] Both templates verified manually end to end (SC-001)
