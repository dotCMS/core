# Contract: `filters.searchScope` on `POST /api/v1/drive/search`

**Feature**: `37479-content-drive-search-scope` | **Date**: 2026-09-14
**Spec**: [spec.md](../spec.md) | **Data model**: [data-model.md](../data-model.md)

Additive, optional, and backward-compatible by construction. This document is the reference the
Postman and integration assertions are written against.

> **Not in `openapi.yaml`.** `ContentDriveResource`'s `/search` method is annotated `@Hidden`
> (`ContentDriveResource.java:83-84`) and the endpoint does not appear in the committed
> `src/main/webapp/WEB-INF/openapi/openapi.yaml` — verified by grep. **No regeneration step applies
> to this change.** This file is therefore the contract of record for the new field.

---

## The change

One new optional member inside the existing `filters` object.

```jsonc
POST /api/v1/drive/search
{
  "assetPath": "//demo.dotcms.com/",
  "filters": {
    "text": "pricing",
    "filterFolders": true,
    "searchScope": "TITLE"      // NEW — optional; omit for today's behaviour
  },
  "sortBy": "modDate:desc",
  "page": 1,
  "perPage": 40
}
```

| Field | Type | Required | Default | Meaning |
|---|---|---|---|---|
| `filters.searchScope` | `string` enum | no | `"ALL_FIELDS"` | Which fields `filters.text` is matched against |

**Allowed values**

| Value | Behaviour |
|---|---|
| `"ALL_FIELDS"` | Term matched against every indexed field. Identical to today. |
| `"TITLE"` | Term matched against the contentlet title only. |

**Why inside `filters`**: `filters` already holds `text` and `filterFolders`, and `filterFolders`'
Javadoc reads *"when text is provided"*. All three qualify the text search and mean nothing without
it, so they travel together (spec → Review Decision 6). The **browse** scope of #37426 stays at the
top level for the opposite reasons — it qualifies `assetPath`, and "Clear all" resets `filters`.

---

## Request rules

| # | Rule | Response | Requirement |
|---|---|---|---|
| C-1 | `searchScope` omitted | Processed exactly as today | FR-017, SC-005 |
| C-2 | `"searchScope": "ALL_FIELDS"` | Identical to C-1 — byte for byte | FR-009, SC-005 |
| C-3 | `"searchScope": "TITLE"` | Title-only matching for contentlets | FR-008 |
| C-4 | Unrecognized value, e.g. `"headline"` | **`400`**, message **names the offending value**; never silently defaults | FR-018 |
| C-5 | `searchScope` present, `text` absent or blank | **`400`** — the field qualifies `text` and is meaningless alone | FR-025 |
| C-6 | Any `text`, any scope, containing `\ + - ! ( ) : ^ [ ] " { } ~ * ? \| & /` | Matched as **literal text**; never alters query structure | FR-027 |
| C-7 | `text` with consecutive separators, e.g. `"a  b"` | No empty clause emitted; same results as the single-separator form | FR-028 |
| C-8 | Query fails to execute | **Error response** — never `200` with an empty list presented as success | FR-029, SC-011 |

> **C-4 vs. the URL.** An unrecognized value in the *browser address* resolves silently to
> `ALL_FIELDS` (FR-015), which is not an inconsistency: a request is a contract between programs, an
> address is a human artefact that outlives the code that wrote it. See data-model → R-2/R-4.

---

## Response

**Unchanged.** No new field, no changed type, no changed shape. The scope alters *which rows* come
back, never the envelope — which is why Constitution IV's `@Schema`-matches-return-type rule is not
engaged. The only response-level change is C-8, which converts a currently-silent failure into an
error rather than adding anything to the success path.

---

## Compatibility

| Consumer | Sends `searchScope`? | Effect |
|---|---|---|
| **Content Drive** (`DotContentDriveStore` → `DotContentDriveService.search()`) | Only when the author selects `TITLE` | The feature |
| **Asset Picker** (`with-asset-browse.feature.ts` → same service, same endpoint) | **Never** | None — C-1 guarantees today's results. This is the caller FR-017 exists to protect, named rather than covered by "other callers". |
| Any stored/replayed request predating this change | No | None — C-1 |

These are **the only two callers of this endpoint**. Three further callers reach the same underlying
listing by other doors and never construct this request at all: `WebAssetHelper` (assets REST API),
`BrowserAjax` (legacy admin browser), `DotCMSMacroWebAPI` (Velocity viewtool). FR-024 confines the
change to the text-search branch precisely so the blast radius stays at two rather than six; SC-008
measures it.

---

## Test matrix

| Case | Layer | Asserts |
|---|---|---|
| C-1 omitted field | Postman | Results equal the pre-change baseline |
| C-2 explicit default | Postman | Response equals the C-1 response |
| C-3 Title narrows | Integration (`ContentDriveKeywordSearchTest`) | Body-only match excluded; name match kept |
| C-4 bad value | Postman | `400` + the value appears in the message |
| C-5 scope without text | Postman | `400` |
| C-6 reserved set | Integration + unit | Every character in the set, both scopes (SC-010) |
| C-6 ticket 39185 headline | Integration | Found in both scopes (SC-009) — **must fail before the change** |
| C-7 double space | Unit (strategy) | No term-less clause in the generated query |
| C-8 failed query | Integration + Jest | Error state, not an empty success |
| Asset Picker untouched | Jest/Spectator | Existing specs pass **unmodified** (SC-006) |
| Other listing callers untouched | Integration (`BrowserAPITest`) | Identical results before/after (SC-008) |

## Generated-query expectations (internal, asserted by unit tests)

Not part of the public contract — recorded so the Red phase has something concrete to assert
against. Shapes follow research [R1](../research.md#r1-what-lucene-clause-implements-title-scope) and
[R2](../research.md#r2-where-does-the-escaping-fix-go-and-does-the-plumbing-already-exist).

| Scope | Mandatory gate | Forbidden in the gate |
|---|---|---|
| `ALL_FIELDS` | today's `catchall` + `title_dotraw` gate, **with every clause escaped** | — |
| `TITLE` | prefix-seek clauses on the title only | `catchall` (FR-010) · any leading wildcard `*<term>` (FR-010) |

Both scopes: the term is passed through `LuceneQueryUtils.escape` **before** the system appends its
own `*` wildcards, so the wildcards stay outside the escaped token — then through the existing
`BrowserAPIImpl.jsonEscape` so backslashes survive into the request body (research R2).
