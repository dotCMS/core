# Contract 3 — `DotAssetPickerConfig` / entry options (additive delta)

**Audience**: internal — `@dotcms/ui` consumers (Story Block, WYSIWYG, File/Image field, and now
`AngularFormBridge`).
**Change**: one optional config sub-object, one new mode.
**Requirements**: FR-001, FR-003, FR-005 – FR-008, FR-007a.

---

## Delta

### `DotAssetPickerMode`

```diff
- type DotAssetPickerMode = 'file' | 'image' | 'video' | 'audio';
+ type DotAssetPickerMode = 'file' | 'image' | 'video' | 'audio' | 'browse';
```

`'browse'` is the `openBrowserModal` entry point. Like `'file'`, it is a **non-media** mode: it has
no entry in `ASSET_PICKER_MIME_TYPES`, so the existing "presence in the map makes it a media mode"
test in `buildAssetPickerConfig` keeps working with no edit.

### `DotAssetPickerBrowseOptions` — new

```ts
interface DotAssetPickerBrowseOptions {
    showFolders?: boolean;
    showLinks?: boolean;
    showWorking?: boolean;
    showArchived?: boolean;
    sortByDesc?: boolean;
    extensions?: string[];
}
```

### `DotAssetPickerConfig`

```diff
+ browse?: DotAssetPickerBrowseOptions;
```

---

## The opt-in guarantee (FR-007, FR-007a)

**`browse` absent ⇒ behavior byte-identical to today.** This is the contract's central promise and
what protects the four existing entry points (FR-008).

| Aspect | `browse` absent | `browse` present |
|---|---|---|
| Folders in results | never | per `showFolders` |
| Menu links | never | per `showLinks` |
| Pages | never | per caller's base types |
| `folderCursor` | pinned `0` | advances (FR-002) |
| `archived` | `false` | per `showArchived` |
| Mimetype narrowing | per mode | none in `'browse'` |

### Behavior explicitly NOT changed *(spec Scope Boundary)*

Layout, toolbar, filter chips, language filter, upload button and dropzone, splitter, footer,
sorting, searching, and how a row responds to click / double-click / keyboard. The picker already
navigates and selects; this work does not touch that.

The one permitted exception, per FR-007a: unpinning the folder cursor (FR-002) and widening the
selection slot (R1) are required to make the new capabilities expressible, and are no-ops for
callers that do not opt in.

---

## Validation rules

| # | Rule | Source |
|---|---|---|
| V1 | `browse.showLinks` + `config.mimeTypes` is unsatisfiable — the endpoint drops links. Log a developer warning; do not work around | R5, FR-036 |
| V2 | `allowedBaseTypes` gains `HTMLPAGE` only when the caller asks for pages | FR-005, FR-008 |
| V3 | `'browse'` is the only mode that may set `browse` | FR-007 |
| V4 | The four media/file modes keep `ASSET_PICKER_ASSET_BASE_TYPES` exactly | FR-008 |

---

## Mapping `DotBrowserOptions` → picker config

Performed by the bridge, per the spec's *Capability Mapping*. Contract 1's redesign simplifies
several rows — notably `path`, which no longer needs id→path resolution.

| Option | Becomes |
|---|---|
| `kinds: ['file']` | `FILEASSET` in `allowedBaseTypes` |
| `kinds: ['dotasset']` | `DOTASSET` in `allowedBaseTypes` |
| `kinds: ['page']` | `HTMLPAGE` in `allowedBaseTypes` |
| `kinds: ['folder']` | `browse.showFolders = true` |
| `kinds: ['link']` | `browse.showLinks = true` |
| `status: 'live'` | `browse.showWorking = false` |
| `status: 'working'` | `browse.showWorking = true` |
| `status: 'archived'` | `browse.showArchived = true` |
| `sort` | `browse.sortByDesc` (from `direction`) + sort field |
| `extensions` | `browse.extensions` → request `extensions` *(needs Contract 2)* |
| `mimeTypes` | `config.mimeTypes` |
| `path` | `config.path` **directly** — both are path-based, no resolution needed |
| `title` | `config.title` |

---

## Contract test checklist

- [ ] `browse` absent produces a config byte-identical to today, for all four existing modes (FR-008)
- [ ] Each `browse` flag reaches the search request (FR-006, FR-017)
- [ ] `'browse'` mode applies no mimetype narrowing
- [ ] V1 logs a warning and does not alter the request
- [ ] `HTMLPAGE` never leaks into the File/Image/video/audio entry points (SC-004)
