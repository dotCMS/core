# Phase 1 Data Model: Content Drive listing projection

No persisted entity, DB schema, or ES/OpenSearch mapping is introduced or changed. This feature
is a transform-time projection over an already-loaded `Contentlet`. The "entities" below are the
in-memory shapes involved.

## Entity: Listing Row (`Map<String, Object>`, unchanged shape)

The response row produced by `BrowserAPIImpl#dotContentMap`, keyed the same as today. This
feature does not add or remove keys — see field-level notes below.

| Key (representative) | Type before | Type after | Change |
|---|---|---|---|
| `identifier`, `inode`, `title`, `contentType`, `baseType`, `languageId`, `live`, `working`, `archived`, `hasLiveVersion`, `modUser`, `modUserName`, `modDate`, `permissions`, `__icon__`, `mimeType`, `extension`, `hasTitleImage`, `owner`, `url`/`path` | as today | unchanged | None — required by AC-002; `title` in particular is populated by `COMMON_PROPS`, independent of the field-key path this feature touches (AC-008). |
| `<wysiwygFieldVariable>` (e.g. `body`) | full HTML string | ≤150-char plain-text string (Jsoup-stripped) | Value shortened/re-encoded under the same key. No type change. |
| `<textAreaFieldVariable>` | full plain/HTML string | ≤150-char plain-text string | Same as WYSIWYG. |
| `<storyBlockFieldVariable>` | `LinkedHashMap` (post `StoryBlockViewStrategy`), or raw string / `null` on malformed content | ≤150-char plain-text **string** | **Type change**: object → string for the well-formed case. This is the one case flagged in the spec's rollback-classification exception (Regression Risk / OQ-6) — still judged rollback-safe (N-1 restores the pre-existing object shape, a forward-compat concern, not a rollback break). |
| Any other field (JSON Field, Custom Field, Key/Value, Constant, Text) | as today | unchanged | Out of scope per OQ-4. |

## Value object: `TransformOptions.LONG_TEXT_PREVIEW`

- **Kind**: Enum constant, `com.dotmarketing.portlets.contentlet.transform.strategy.TransformOptions`.
- **Position**: Declared after `STORY_BLOCK_VIEW` and `JSON_VIEW` — ordinal position is the
  entire correctness mechanism (see research.md R1). This is not a stylistic choice; a unit test
  must assert it.
- **Semantics**: Option-triggered (not in `defaultOptions()`). When present in a transformer's
  option set, `LongTextPreviewStrategy` runs after all default and other option-triggered
  strategies, and replaces the map entry for every field of type `WysiwygField`, `TextAreaField`,
  or `StoryBlockField` with a ≤150-character plain-text preview string.

## Value object: extracted preview (conceptual, not a persisted type)

- **Input**: the field's current map value at the time `LongTextPreviewStrategy` runs (a raw HTML
  string for WYSIWYG/TextArea; a `LinkedHashMap`/string/`null` for Story Block, per
  `StoryBlockViewStrategy`'s output).
- **Extraction**:
  - WYSIWYG/TextArea: `Jsoup.parse(rawHtml).text()`.
  - Story Block: new recursive traversal — walk `content` arrays in the block tree, collect
    `text` leaf-node string values, join with a single space; must tolerate a non-map value
    (raw string fallback) or `null` (parse failure) without throwing, per
    `StoryBlockViewStrategy`'s documented fallback branches.
- **Truncation**: `preview.length() > 150 ? preview.substring(0, 150) : preview` — applied to the
  **extracted plain text**, never to the raw stored value.
- **Output**: a `String`, written back into the row map under the field's existing key via
  `map.put(field.variable(), preview)`.

## State / lifecycle

No state transitions. This is a stateless, per-request, per-field transform applied once per row
during `dotContentMap`'s existing strategy-resolution pass. No caching, no persistence, no async
step introduced.

## Validation rules

- The 150-character bound applies to the **extracted plain-text preview**, not to the field's
  stored value length (a 200-char plain-text body extracted from 2000 chars of HTML is still
  truncated to 150, not left at 200).
- A `title` field must never be sourced from a `LongTextPreviewStrategy`-processed map entry, even
  if the content type's title field happens to be a WYSIWYG/TextArea field (AC-008) — `title` is
  populated by `COMMON_PROPS` from a separate code path and this feature must not change that.
- A transformer built without `LONG_TEXT_PREVIEW` in its option set must produce a byte-identical
  map to today for every field (AC-007) — the strategy must be a strict no-op when the option is
  absent (this is really "the option isn't registered as active", not runtime logic inside the
  strategy, but is called out here as a data-shape invariant to test against).
