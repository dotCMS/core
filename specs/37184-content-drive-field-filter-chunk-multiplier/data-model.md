# Phase 1 Data Model: Content Drive Field-Filter Chunk Multiplier

This fix introduces **no new persistent entity, DB table/column, or index mapping**. It
changes control flow (how many times an existing query/call runs) inside
`com.dotcms.browser.BrowserAPIImpl`, over data shapes that already exist. This document
records the existing in-memory entities/fields the fix reads or reasons about, since the
spec's Key Entities section names them at a conceptual level and the plan needs to tie them
to concrete code.

## Field-filter search request (`BrowserQuery`)

Existing class (`dotCMS/src/main/java/com/dotcms/browser/BrowserQuery.java`), unchanged by
this fix. Fields relevant to the single-pass condition (FR-002):

| Field | Type | Relevance to this fix |
|-------|------|------------------------|
| `fieldCriteria` | `List<FieldSearchCriteria>` | Source of the DB-vs-index routing bucket per criterion; single-pass requires none of these to be `RoutingBucket.DB`. |
| `workflowSchemeIds` | `Set<String>` | Single-pass requires this to be empty (workflow filters must stay on the existing chunked path per FR-005). |
| `workflowStepIds` | `Set<String>` | Same as above. |
| `filter` | `String` | Free-text term; single-pass requires this unset (`!UtilMethods.isSet(...)`). |
| `fileName` | `String` | File-name term; same requirement as `filter`. |
| `useElasticsearchFiltering` | `boolean` | Must already be `true` for the request to reach `doElasticSearchTextFiltering` at all — precondition, not part of the new check. |
| `contentCursor` | `int` | DB row offset for the current page; read and advanced identically to today — pagination semantics are not changed by this fix (see research.md R3). |

No new field is added to `BrowserQuery`. See research.md R2 for why the single-pass
condition is computed from existing fields rather than a new precomputed flag.

## Field criterion (`FieldSearchCriteria`)

Existing class (`dotCMS/src/main/java/com/dotcms/browser/FieldSearchCriteria.java`),
unchanged by this fix.

| Field | Type | Relevance to this fix |
|-------|------|------------------------|
| `bucket` | `RoutingBucket` (`DB` \| `INDEX`) | The single fact this fix's new condition reads per criterion — already assigned upstream per ADR-0018's routing table (Tag/Relationship → `DB`; Text/Date/Multi-select/Category → `INDEX`). |
| `field` | `Field` (content-type field model) | Used elsewhere (e.g. `instanceof TagField`/`RelationshipField` checks) — not newly consumed by this fix beyond what `bucket` already summarizes. |
| `values` / `from`/`to` (per `FilterKind`) | `List<String>` / `String` | Unchanged; carried through to the (unchanged) ES query construction for index-bucket criteria. |

## Candidate content set (in-memory, not persisted)

The list of DB-ordered candidate inodes produced by `selectQuery(...)` + a `DotConnect`
fetch. Today assembled incrementally across chunk iterations
(`accumulatedContent` in `getContentByChunks`); after this fix, for the single-pass case,
assembled from a single fetch bounded by `BROWSER_DB_MAX_SCAN_ROWS` (existing config,
default 50,000) instead of `BROWSER_CONTENT_CHUNK_SIZE` (existing config, default 900)
per-iteration slices. No new state is introduced — this is a change to how many round trips
populate the same in-memory list.

## Elasticsearch filtering call (`processESDirectly`)

Existing package-private method; unchanged internally. Consumed differently: today invoked
once per DB chunk (up to ~23 times for the spec's worst case); after this fix, invoked once
per request for the single-pass case, with the full candidate set. Internally it still may
split into multiple physical ES queries above `calculateMaxInodesPerESQuery` (~900 inodes) —
see research.md R4 and plan.md Complexity Tracking Q1 for the open question this raises for
the SC-001 metric.

## State transitions

None. This is a stateless, per-request control-flow change — no entity moves between states
as part of this fix.
