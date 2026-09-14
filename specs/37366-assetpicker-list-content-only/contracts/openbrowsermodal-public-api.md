# Contract: `DotCustomFieldApi.openBrowserModal()` after #37366

**Surface**: JavaScript, called from a custom-field VTL template's `<script>` block.
**Reachable only** when the new Edit Content experience is enabled
(`$structures.isNewEditModeEnabled()`), which is not the default — so this contract is **unshipped**
and may narrow without a deprecation cycle (`asset-browser.interface.ts:1-11`).

**Baseline**: the contract as shipped by #37207 /
[`specs/37207-openbrowsermodal-assetpicker/contracts/openbrowsermodal-public-api.md`](../../37207-openbrowsermodal-assetpicker/contracts/openbrowsermodal-public-api.md).
Only the deltas are spelled out here.

---

## Delta 1 — `kinds` loses `'folder'`

```ts
// before
type DotBrowserItemKind = 'file' | 'dotasset' | 'page' | 'folder' | 'link';
// after
type DotBrowserItemKind = 'file' | 'dotasset' | 'page' | 'link';
```

| Value | Lists | Returnable |
|---|---|---|
| `'file'` | file assets | ✅ |
| `'dotasset'` | dotAssets | ✅ |
| `'page'` | HTML pages | ✅ |
| `'link'` | menu links | ✅ |
| ~~`'folder'`~~ | **never listed** | **never returned** |

**Folders are navigation, not content.** They appear only in the picker's sidebar tree, where
selecting one changes what the list shows. There is no option that makes a folder appear as a list
row, and no option that makes a folder the picked value.

`@default` is unchanged: `['file', 'dotasset']`.

---

## Delta 2 — the returned selection union loses its folder member

```ts
// after
type DotBrowserSelection =
    | DotBrowserAssetSelection   // kind: 'file' | 'dotasset'
    | DotBrowserPageSelection    // kind: 'page'
    | DotBrowserLinkSelection;   // kind: 'link'
```

`onClose(selection)` can no longer be called with `kind: 'folder'`. A consumer that branches on
`kind` needs no change — the branch simply becomes unreachable. A consumer that only reads
`selection.url` (both shipped templates do) is entirely unaffected.

---

## Delta 3 — behavior for a caller that still asks for `'folder'`

TypeScript cannot police a value arriving from a VTL string literal, so this is specified as
**runtime behavior** (the point of spec AC-008):

| Caller passes | Result |
|---|---|
| `kinds: ['file', 'page', 'folder']` | Behaves as `['file', 'page']`. No folder rows. **No error.** One `console.warn` naming `folder` as unsupported |
| `kinds: ['folder']` | No kind maps to a base type, so the picker falls back to its asset-only default (`baseTypesFor` returns `undefined` — `angular-form-bridge.ts:635-642`). One `console.warn` |

The warning follows the idiom already in `browseOptionsFor` for the `link` + `mimeTypes` conflict
(`angular-form-bridge.ts:654-662`): surface the mismatch rather than silently return fewer kinds
than asked for. It never throws — an exception inside a VTL `<script>` would break the whole custom
field.

---

## Unchanged

Everything else in the contract stands exactly as #37207 shipped it:

- `title`, `status` (`'live' | 'working' | 'archived'`), `path`, `mimeTypes`, `sort`, `onClose`
- `DotBrowserController` and its `close()`
- The `link` + `mimeTypes` warning
- The legacy Dojo bridge still resolves `null` with a warning rather than opening anything
- Menu-link listing and paging (#37112)

---

## Shipped callers

| Template | Field | `kinds` before | `kinds` after |
|---|---|---|---|
| `file_browser_field_render_new.vtl:23` | Vanity URL `forwardTo` | `["file","page","folder"]` | **`["file","page"]`** |
| `redirect_custom_field_new.vtl:57` | HTML page `redirecturl` | `["page","link"]` | unchanged |

A folder path can still be **set** on `forwardTo` — the field keeps its free-text input
(`file_browser_field_render_new.vtl:38-44`), which writes straight to the field value. It can no
longer be **picked** from the browser.
