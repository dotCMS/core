# Contract: `status` on `POST /api/v1/drive/search`

**Feature**: [../spec.md](../spec.md) | **Date**: 2026-08-24

One new optional field on an existing request body. No new endpoint, no breaking change: a request
that omits `status` behaves byte-identically to today (FR-002).

Resource: `dotCMS/src/main/java/com/dotcms/rest/api/v1/drive/ContentDriveResource.java` (`@Path("/v1/drive")`, `@Path("/search")`, `@POST`).

---

## Request

```jsonc
{
  "assetPath": "//demo.dotcms.com/marketing/",
  "status": ["UNPUBLISHED", "LOCKED"]   // NEW — optional, defaults to []
  // …every existing field unchanged
}
```

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `status` | `string[]` | No | `[]` | Content states to filter by. Accepted: `ARCHIVED`, `UNPUBLISHED`, `LOCKED`. Entries combine with **OR**. |

### Semantics

| Selection | Returns |
|---|---|
| `[]` (or omitted) | Today's behavior: archived excluded, everything else returned |
| `["ARCHIVED"]` | Only archived content |
| `["UNPUBLISHED"]` | Only content with no live version, archived excluded |
| `["LOCKED"]` | Only content with a lock held, by anyone, archived excluded |
| `["UNPUBLISHED","LOCKED"]` | Content that is unpublished **or** locked, archived excluded |
| `["ARCHIVED","UNPUBLISHED"]` | Everything with no live version — archived **or** unpublished |
| `["ARCHIVED","LOCKED"]` | Archived content **or** content with a lock held |
| all three | Anything not cleanly published — archived, unpublished **or** locked |

**OR, not AND.** Selecting more statuses returns *more* content, exactly like `contentTypes`,
`baseTypes` and `language`. Adding a status can never shrink the result set.

**OR applies *within* `status` only. Separate filters still AND with each other**, exactly as they
do today: `{"workflow": [...], "status": ["UNPUBLISHED","LOCKED"]}` means *governed by that workflow*
**and** *(unpublished **or** locked)*. Each filter appends its own `and ( … )` clause server-side
(`BrowserAPIImpl:2338` for workflow), so adding `status` never loosens another filter.

**The archived exclusion is a baseline, not a fourth value, and it sits outside the OR group.**
Every drive request already excludes archived content; the selected statuses are OR-ed together and
that group is AND-ed against the baseline, which only `ARCHIVED` lifts. That is why
`["UNPUBLISHED"]` means "unpublished and not archived" without contradicting the OR rule. Folding
the baseline into the group instead would make `["UNPUBLISHED","LOCKED"]` match nearly every row.
See [../data-model.md](../data-model.md) for the per-selection predicate table.

### Side effects on other fields

| Field | Effect when `status` is non-empty |
|---|---|
| `showFolders` | **Unaffected.** The endpoint honours whatever the caller sent |
| `archived` | Unaffected and unchanged. The legacy inclusive flag keeps its meaning (FR-008) |

**`status` has no side effects on other fields.** Folders carry no status, so the Content Drive UI
sends `showFolders: false` once a status is selected (FR-015) — but that is the client's decision.
Overriding an explicit `showFolders: true` server-side would make the response stop matching the
request, and would leave `folderCursor` / `hasMoreFolders` describing a folder query the caller never
received. A caller that wants folders alongside a status gets them.

The Content Drive UI stops sending `archived: false` altogether (FR-019); the server default already
supplies it.

---

## Responses

### 200 — success

Response shape is unchanged (`ResponseEntityView<PaginatedContents>`). Only the contents of `list`,
and the counts, change.

### 400 — unrecognized status value

```jsonc
// request
{ "assetPath": "//demo.dotcms.com/", "status": ["ARCHIVED", "DRAFT"] }
```

Returns `400` with a message naming the accepted values. Silently ignoring the unknown entry would
return a **wider** result set than the caller asked for, which is worse than failing.

Consistent with the existing `userSearchable` rejection in `ContentDriveHelper` — the same
`BadRequestException` path, thrown explicitly rather than left to Jackson.

### Other statuses

Unchanged: `401` unauthenticated, `403` no portlet access, `500` unexpected.

---

## Query-path guarantees

The same `status` selection must return the same set regardless of which search strategy the
environment runs (FR-009 / SC-005). Both paths are in scope:

| `BROWSE_API_HEURISTIC_TYPE` | Path | How `status` applies |
|---|---|---|
| `HYBRID_SINGLE_CHUNKED_QUERY_ES` (**default**) | `BrowserAPIImpl.selectQuery` supplies the candidate set; the index only narrows by text | SQL clauses — so they apply with **and** without a keyword |
| `PURE_ES` | `BrowserAPIImpl.buildPureESQuery` | Index terms, replacing the hardcoded `+deleted:false` |

### An empty `status` MUST be ignored entirely

An omitted or empty `status` changes **nothing** on either path — the field is skipped, not
translated into a vacuous clause. Both builders MUST return early on an empty set rather than
opening a group they have nothing to fill: `and ( )` is a SQL syntax error and `+()` is invalid
Lucene. This is what makes FR-002's "byte-identical to today" literally true, and it is the case to
assert first, because every existing caller of drive search sends no `status`.

### Multiple statuses MUST be one explicit OR group

In Lucene, `+` means REQUIRED. Emitting `+deleted:true +live:false` is an **AND** — the opposite of
this contract. Multiple statuses go in one explicit group, with the archived baseline left outside
it as its own required clause:

| Selection | Index query |
|---|---|
| `[]` | *(no status terms at all)* |
| `["UNPUBLISHED"]` | `+deleted:false +(live:false)` |
| `["UNPUBLISHED","LOCKED"]` | `+deleted:false +(live:false OR locked:true)` |
| `["ARCHIVED"]` | `+(deleted:true)` |
| `["ARCHIVED","LOCKED"]` | `+(deleted:true OR locked:true)` |

This is already the convention in the method being changed — `BrowserAPIImpl:630` writes
`+(conhost:<id> OR conhost:SYSTEM_HOST)`.

Aligned with [ADR-0018](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0018-database-first-content-drive-search-with-index-deferred-text-filtering.md),
which routes version-info flags (archived/deleted) to the **database**. `PURE_ES` is patched not to
promote it, but because it is a supported configuration where the filter would otherwise silently
no-op.

### One accepted divergence under `PURE_ES`

`UNPUBLISHED` does **not** mean quite the same thing on the two paths, and the difference is
accepted rather than fixed.

| Path | Predicate | Question it answers |
|---|---|---|
| SQL (default) | `cvi.live_inode is null` | does this **content** have a live version anywhere? |
| Index (`PURE_ES`) | `live:false` | is **this version** the live one? |

The index stores `live` per version, so a published item that also has newer unpublished edits has a
working document carrying `live:false` — which the index query matches and the SQL query does not.
Under `PURE_ES`, `UNPUBLISHED` therefore returns that item as well.

**The identifier-scoped meaning is the definition** (see `ContentStatus.UNPUBLISHED`). The index
simply cannot express it: "does any version of this identifier have `live:true`?" is not answerable
from a single document.

This is not a new limitation. ADR-0018 routes structural predicates to the database precisely
because the index cannot answer them reliably, and states that `PURE_ES` forfeits that guarantee for
*every* criterion and must not become the default. `PURE_ES` is opt-in and is not set in any config
in the repository. `ARCHIVED` and `LOCKED` are unaffected, as is the default path.

---

## OpenAPI

`openapi.yaml` is generated by `swagger-maven-plugin` at compile. The field's description goes in the
Java annotations; the regenerated
`dotCMS/src/main/webapp/WEB-INF/openapi/openapi.yaml` is committed alongside:

```bash
./mvnw compile -pl :dotcms-core -DskipTests
git diff -- '*openapi.yaml'
```

CI verifies the committed file matches what the build produces.

---

## Frontend contract

`DotContentDriveSearchRequest.status?: string[]` in
`core-web/libs/dotcms-models/src/lib/dot-content-drive.model.ts`.

The value lives in the shared `filters` bag rather than its own query param, so it inherits every
navigation mechanism the other filters already use — deep link, reload, folder browsing, browser
Back/Forward, and the legacy-editor round-trip (FR-016):

```
…?filters=languageId:1;sharedAssets:true;status:UNPUBLISHED,LOCKED
```

Encoding needs no new code — `encodeFilters` already comma-joins array values. Decoding is one entry
in `decodeByFilterKey` (`status: multiSelector`), which is **required**: without it a single-value
URL (`status:ARCHIVED`) falls through to the comma sniff and decodes as a string rather than an
array.
