# Issue Resolution Specification: Content Drive search drops all contentlets on large sites — ES inode batch exceeds max query-string length

**Feature Branch**: `issue-37695-content-drive-es-batch-length`

**Created**: 2026-09-23

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37695](https://github.com/dotCMS/core/issues/37695)

**Input**: User description: "#37695 — bound the per-ES-sub-query inode batch by query-string length as well as by boolean-clause count, covering both the text-search and the index-routed field-filter (#37184) paths. Moving the inode restriction to `bool.filter` + `terms` is out of scope. The FR-029 question (silent partial 200 vs. surfacing the failure) stays open and is not decided here."

## Problem Statement *(mandatory)*

On a site with a realistic amount of content, a Content Drive search with a term returns **no
contentlets**, only folders and links (whose names are matched in the database). Contentlets that
match the term, and that the index finds with a direct query, are missing. The response is
HTTP 200, so the user sees a normal-looking but wrong listing.

Typing a term always moves the drive to **All Site Content**, so this is hit by the most common
search a user can make, in both search scopes (Title and All Fields).

**Severity / Impact**: High. Every user on any site whose candidate set is large enough to fill an
ES sub-query batch (about 800+ candidates) gets empty or incomplete content results for a
drive-wide search. It fails every time for those sites, not intermittently. Small sites, folder
browsing and searches narrowed by Content Type keep working, which hides the problem in
CI and demos.

## Reproduction *(mandatory)*

**Environment**: `staging-trunk`, build `ef1de93` (2026-09-22), includes #37554. Site `default`
with ~2,182 Content Drive candidates.

**Steps to Reproduce**:

1. Use a site with roughly 1,000+ contentlets.
2. Create a content type with an indexed `title` (Text), an indexed `body` (Textarea), and a Site
   or Folder field.
3. On site `default`, root folder, create and publish:
   - A: title `qazeta hero asset`
   - B: title `Herd notes`, body `this note mentions qazeta inside the body`
4. Create a folder `qazeta-folder` at the site root.
5. Confirm the index has both: `POST /api/content/_search` with `{"query":"+catchall:qazeta*"}`
   returns A and B.
6. Open Content Drive → **All Site Content** (no `path` in the URL), with Include System Host on
   or off.
7. Type `qazeta` in the search box (All Fields or Title).

**Expected Behavior**: Title scope lists A and `qazeta-folder`. All Fields scope lists A, B and
`qazeta-folder`.

**Actual Behavior**: Only `qazeta-folder` is listed. The response has `contentCount: 0` and
`nextContentCursor: 2182`. The server log shows:

```
ElasticsearchException [type=parse_exception, reason=parse_exception: Query string length exceeds max allowed length 32000 (search.query.max_query_string_length); actual length: 35132]
  at com.dotcms.enterprise.priv.ESSearchAPIImpl.esSearchRaw(ESSearchAPIImpl.java:358)
  at com.dotcms.content.index.SearchAPIImpl.search(SearchAPIImpl.java:75)
```

**Reproducibility**: Always, once the candidate set is large enough to fill an ES sub-query batch.
Controls that return the expected results: searching with `path=/` (the site node), or adding a
Content Types filter. Both shrink the candidate set below one batch.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content Drive listing/search (`/api/v1/drive/search`), specifically the hybrid
  DB + index text filtering and the index-routed field-filter path (#37184).
- **Suspected surface**: Modern — `com.dotcms.browser.BrowserAPIImpl` (`processESDirectly`,
  `calculateMaxInodesPerESQuery`, `processSingleESQuery`, `processMultipleESQueries`). No
  `com.dotmarketing.*` change expected.
- **Related known decisions**:
  - #37479 FR-029 / #37554: the swallow in `processSingleESQuery` and
    `processMultipleESQueries` is documented as deliberate (a Lucene-injection attempt must match
    nothing, not return a 500 — `ContentDriveFieldFilterTest#testMalformedDateBoundIsSafe`). This
    fix must not change that behavior.
  - #37488 / PR #37489 (draft, blocked by this issue): proposes making sub-query failures
    propagate.
  - The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

`processESDirectly` restricts each ES sub-query to its DB candidates by prepending
`+inode:(id1 OR id2 ...)` to the text query inside one `query_string`. The per-query inode cap
from `calculateMaxInodesPerESQuery` (~876 for a typical base query) is sized **only** against the
1024 boolean-clause limit, not against the length of the query string. At ~40 characters per
inode (a 36-character UUID plus ` OR `), a full batch is ~35 KB. That exceeds the index server's
`search.query.max_query_string_length` (32,000 by default), so the server rejects the query with a
`parse_exception`.

The exception is caught and logged, so the failed batch contributes an empty set and the request
still finishes with HTTP 200. On a large site every full batch fails, so the listing loses all of
its contentlets.

The bug is probably older than #37479: the batching dates from #33711 and it reproduces in both
scopes. It is not confirmed when the index side started enforcing the 32,000-character limit
(possibly with the ES → OpenSearch migration). That does not change the fix.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Bound every ES sub-query built by `processESDirectly` by the **length** of the resulting query
  string as well as by the boolean-clause estimate, so no sub-query exceeds the index server's max
  query-string length, whatever the candidate-set size.
- Account for the actual base query (text group plus index-routed field clauses) and the actual
  inode lengths when sizing a batch, instead of assuming a fixed per-inode cost.
- Apply the same bound to both callers of `processESDirectly`: the text-search chunk loop and the
  single-pass field-filter path (#37184). Both go through the same method.
- Keep the limit value configurable through `Config` (default 32,000, the index-server default)
  with a safety margin, so an installation with a different server setting can match it.
- Handle the edge case where the base query alone leaves no room for any inode: it must not
  build an oversized query or loop forever. It must be logged clearly.
- Integration test coverage that seeds more candidates than one ES sub-query can hold and proves
  that matches from every batch come back.

**Explicitly out of scope / non-goals**:

- **Moving the inode restriction out of `query_string`** into `bool.filter` + `terms`. The `inode`
  field is mapped as `text` with `my_analyzer`, whose `char_filter` rewrites UUID hyphens to
  underscores (`es-content-settings.json` / `os-content-settings.json`). An unanalyzed `terms`
  query would not match unless it copied that internal detail of the analyzer. This can be
  proposed later as a performance improvement.
- **Deciding FR-029** (silent partial 200 vs. surfacing a failed sub-query). #37695 lists this as
  its last acceptance criterion, but it is deferred to #37488 / PR #37489 and needs to be settled
  with the #37479 owner. This fix leaves the error-handling behavior in `processSingleESQuery` /
  `processMultipleESQueries` unchanged.
- Changing the DB chunk sizes (`BROWSER_CONTENT_CHUNK_SIZE`, `BROWSER_SINGLE_PASS_CHUNK_SIZE`),
  the result-ordering logic, or the cursor logic.
- Changing index mappings or analyzers.
- Changing the Search portlet or `GlobalSearchAttributeStrategy`.

## Regression Risk *(mandatory)*

- **Blast radius**: Every Content Drive request that goes through `processESDirectly`, meaning
  text search in both scopes and index-routed field filters. Smaller batches mean more ES
  sub-queries per DB chunk. That adds some parallel load on the shared `DotSubmitter` pool and on
  the index, but the batch shrinks by only ~15–20% (from ~876 to roughly 700–750 inodes for a typical query,
  depending on the safety margin).
  Requests whose candidate set fits in one sub-query today see no change.
- **Backward compatibility**: No REST contract, DB schema or index mapping changes. The response
  shape is unchanged; only the results become complete. Safe to roll back. The new `Config` key
  is optional and defaults to current server behavior.
- **Data considerations**: None. No stored data changes.
- **Pagination**: The ordering (results re-projected in DB order) and the cursor logic depend
  on the set of matches per chunk, not on how that chunk is split into sub-queries. Smaller
  sub-queries must not skip or duplicate rows across pages. Tests must cover this.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction steps above produce the expected behavior. A drive-wide search on a
  site whose candidate set spans multiple ES sub-queries returns every matching contentlet, in
  both Title and All Fields scopes.
- **AC-002**: No ES sub-query built by `processESDirectly` exceeds the configured max
  query-string length, whatever the candidate-set size or base-query length. This is checked
  directly on the generated query, not only through search results.
- **AC-003**: Results and pagination (`nextContentCursor` / `hasMoreContent`) stay correct across
  batch boundaries: no skipped or duplicated rows.
- **AC-004**: The index-routed field-filter path (#37184) is covered by the same fix, and a
  regression test proves it.
- **AC-005**: Searches whose candidate set fits in one sub-query return the same results as
  before. The existing `ContentDriveFieldFilterTest` and `ContentDriveLiteralTextSearchTest`
  suites stay green, including `testMalformedDateBoundIsSafe`.
- **AC-006**: When the base query alone leaves no room for inodes, the request does not build an
  oversized query or hang, and the condition is logged with enough detail to diagnose it.
- **Verification method**:
  - Integration test that seeds more candidates than one ES sub-query can hold (so the batch
    fills the limit) and asserts that matches from every batch come back, for text search in both
    scopes and for a field filter. Run with `-Dit.test=<Class>` and
    `-Dmaven.build.cache.enabled=false`, and confirm `Tests run: N` in the failsafe reports.
  - A test of the batch sizing that asserts every generated sub-query stays within the limit for
    large inode sets and long base queries.
  - Re-run `ContentDriveFieldFilterTest` and `ContentDriveLiteralTextSearchTest`.
  - Register any new test class in the appropriate `MainSuite*` so it runs in CI.
  - Manual: the reproduction steps above on a large dataset.

## Assumptions

- The limit being hit is the index server's `search.query.max_query_string_length` (32,000
  default), and it applies to the decoded Lucene string. That is the inode filter plus the base
  query, which is what `processSingleESQuery` passes as the `query_string` value, measured before
  JSON escaping.
- The same limit applies on both the Elasticsearch and OpenSearch read paths. The OpenSearch path
  lowercases the query first, which does not change its length.
- Inodes are usually 36-character UUIDs, but sizing must not rely on that. Legacy non-UUID inodes
  may have other lengths.
- The FR-029 decision is left to #37488 / PR #37489. Until it is made, a sub-query that still
  fails for another reason keeps today's behavior: logged, empty contribution, HTTP 200.
