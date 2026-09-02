# Contract: Listing row long-text projection

This feature has no new REST endpoint and no new request/response schema. The "contract" that
matters is a **behavioral** one on the existing, shared row-shaping internal API surface
(`BrowserAPI` → `dotContentMap`), consumed by two REST endpoints, one legacy DWR path, one
Velocity viewtool, and (unenumerable) OSGi plugins. This document pins that behavior so
implementation and tests agree on what "done" means.

## Affected consumers (blast radius — see spec's Regression Risk table)

| Consumer | Entry point | Gets the new truncation? |
|---|---|---|
| Content Drive portlet | `POST /api/v1/drive/search` (`ContentDriveResource`, `@Hidden`, internal, no compat guarantee) | Yes |
| Site Browser | `POST /api/v1/browser` (`BrowserResource`) | Yes (OQ-2, resolved — no branch split) |
| Legacy Dojo Site Browser / file dialog | `BrowserAjax` (DWR) | Yes in principle; in practice calls pass `onlyFiles=true` so `dotContentMap`'s generic-Content branch usually isn't reached |
| Velocity viewtool | `DotCMSMacroWebAPI` | Yes in principle; same `onlyFiles=true` caveat |
| Asset picker / File field | `POST /api/v1/drive/search` pinned to `['DOTASSET','FILEASSET']` | **No** — takes `fileAssetMap`/`dotAssetMap` branches, never `dotContentMap` |
| OSGi plugins via `APILocator.getBrowserAPI()` | any | Possible, unenumerable — public interface |
| `POST /api/v1/content/_search`, GraphQL, Content Editor | different `DotTransformerBuilder` option sets (`defaultOptions()` untouched) | **No** — must remain byte-identical (AC-007) |

## Behavioral contract — generic Content row, `dotContentMap`

**Given** a generic-Content (base type `CONTENT`) row is produced with `LONG_TEXT_PREVIEW`
in the transformer's option set (i.e. via `BrowserAPIImpl#dotContentMap`):

1. Every key present in today's row is still present (no removals) — required keys enumerated in
   AC-002 are unconditionally checked.
2. For every field of type `WysiwygField` or `TextAreaField` on the content type: the map value
   at that field's variable key is a plain-text string, contains no `<`/`>` characters if the
   source contained HTML tags, and is ≤150 characters.
3. For every field of type `StoryBlockField`: the map value at that field's variable key is a
   **string** (not a `LinkedHashMap`, not raw JSON), contains no unresolved `{`/`}` JSON
   structure markers if the source was well-formed block JSON, and is ≤150 characters. Malformed
   Story Block content (raw string or `null` fallback from `StoryBlockViewStrategy`) is handled
   without throwing.
4. `title` is correct and untruncated even when the content type's title field is itself a
   WYSIWYG/TextArea field (AC-008).
5. Every other field type (JSON Field, Custom Field, Key/Value, Constant, plain Text) is
   unaffected — same value as today.

**Given** a transformer is built **without** `LONG_TEXT_PREVIEW` in its option set (i.e. every
other current `DotTransformerBuilder` consumer, including `defaultOptions()`-based ones):

6. The resulting map is byte-identical to the pre-change behavior — no field is truncated, no key
   is added or removed, no ordinal shift in `TransformOptions` changes any other option's
   resolution order (guarded by asserting `LONG_TEXT_PREVIEW`'s ordinal is strictly after
   `STORY_BLOCK_VIEW` and `JSON_VIEW`, and that inserting it there doesn't renumber unrelated
   already-tested option interactions).

## Documentation contract (AC-005)

- `ContentDriveResource#search`'s `@Operation`/`@Schema` description text states that listing
  rows carry a reduced/preview projection for long-text fields.
- `./mvnw compile -pl :dotcms-core -DskipTests` regenerates `openapi.yaml`; because the endpoint
  is `@Hidden`, the regenerated file must show **no diff** for this endpoint (confirmed
  pre-existing in the spec — this is a check, not new behavior to build).

## Postman contract (AC-006)

- `ContentDriveResource.postman_collection.json` → folder "Search and Filtering Tests" → request
  "Text Search - Alpha Filter" → test "Search filters out non-matching items": the dead
  `var body = item.body || '';` read (feeding an `allItemsMatch` variable that is computed but
  never asserted) is removed or replaced with an assertion that actually runs against the new
  truncated-preview value. The other 59 assertions in the collection are unaffected and must
  still pass.
