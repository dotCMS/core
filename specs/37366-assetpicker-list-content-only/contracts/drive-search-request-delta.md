# Contract: what the Asset Picker sends to `POST /api/v1/drive/search`

**The endpoint is not changed by #37366.** No Java, no `@Schema`, no `openapi.yaml`. This file pins
down the **client side** of the contract — the request body the picker builds — because that is where
the fix is enforced.

Server-side reference: `AbstractDriveRequestForm.java` (request shape),
`ContentDriveHelper.java` (mapping to `BrowserQuery`), `BrowserAPIImpl.java:1690-1790` (paging).
Behavior is DB-first per
[ADR-0018](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0018-database-first-content-drive-search-with-index-deferred-text-filtering.md).

---

## The rule that governs this delta

```java
// AbstractDriveRequestForm.java:395-397
@JsonProperty("showFolders")
@Value.Default
default boolean showFolders(){return true; }
```

**`showFolders` defaults to `true`.** A request that omits it gets folders. So the picker must send
an explicit `false` — omitting the key is not equivalent and would silently reintroduce the bug this
fix removes. `ContentDriveResource.java:62` states the endpoint "honours `showFolders` as sent", and
`ContentDriveHelper.java:234-236` refuses to override an explicit value.

`folderCursor`, by contrast, defaults to `0` (`AbstractDriveRequestForm.java:480-482`), so that key
*is* safe to drop.

---

## Request delta

Built in `with-asset-browse.feature.ts:118-172`.

| Key | Before — non-browse callers (File, Image, video, audio) | Before — browse caller asking for folders | **After — every caller** |
|---|---|---|---|
| `showFolders` | `false` (computed: `Boolean(undefined) && …`) | `true`, until `hasMoreFolders` came back `false` | **`false`, always, explicitly** |
| `folderCursor` | `0` (pinned) | advanced from the page bookmark (`5`, `10`, …) | **key omitted** |
| `showLinks` | omitted | `true` while links remain | unchanged |
| `linkCursor` | omitted | advanced from the bookmark | unchanged |
| `contentCursor` | advanced | advanced | unchanged |
| `assetPath`, `includeSystemHost`, `filters`, `language`, `contentTypes`, `baseTypes`, `mimeTypes`, `maxResults`, `sortBy`, `archived`, `live` | — | — | unchanged |

### After the change — the browse request for `forwardTo`

```jsonc
{
  "assetPath": "//demo.dotcms.com/",
  "includeSystemHost": true,
  "filters": { "text": "", "filterFolders": true },
  "baseTypes": ["FILEASSET", "HTMLPAGE"],   // from kinds: ["file", "page"]
  "contentCursor": 0,
  "maxResults": 20,
  "sortBy": "modDate:desc",
  "archived": false,
  "live": true,                              // from status: "live"
  "showFolders": false                       // explicit — the server default is true
}
```

`showLinks` / `linkCursor` appear only for a caller that asks for `'link'` — which `forwardTo` does
not, and `redirecturl` does.

---

## Response fields the picker stops reading

The response shape is unchanged; the picker simply ignores two fields.

| Field | Value when `showFolders: false` | Why it can be ignored |
|---|---|---|
| `hasMoreFolders` | always `false` | initialised `false` and only reassigned inside `if (browserQuery.showFolders)` — `BrowserAPIImpl.java:1705-1730` |
| `nextFolderCursor` | echoes the `folderCursor` sent, i.e. `0` | same block |

Consequences in the picker:

- `DotAssetPickerPage` drops `folderCursor` / `hasMoreFolders` (see [data-model.md §4](../data-model.md#4-dotassetpickerpage--the-per-page-cursor-bookmark)).
- `$totalRecords` unions `hasMoreContent || hasMoreLinks` only — two streams, same invariant
  (see [§6](../data-model.md#6-derived-state--totalrecords)).

`response.list` now contains contentlets only, which is the whole point: `items` never receives a
folder, so `DotFolderListViewComponent` never renders a folder row — without the component changing
at all.

---

## Side effect worth knowing (not a goal)

The server spends the page budget on folders **before** content:

```java
// BrowserAPIImpl.java:1725
maxResults -= folderCount;
```

With folders off, a page returns up to the full `maxResults` in contentlets instead of fewer. Pages
will therefore look *fuller* after the fix. This is an improvement, not a regression — but it means
row counts in any manual comparison against the pre-fix behavior will legitimately differ.
