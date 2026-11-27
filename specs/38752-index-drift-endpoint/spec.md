# Feature Specification: List the documents drifting between the database and the OpenSearch index

**Feature Branch**: `issue-38752-index-drift-endpoint`

**Created**: 2026-11-26 · **Revised**: 2026-11-27 (three independent reviews + walkthrough with the
developer)

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#38752 — "Add an endpoint that lists the documents drifting between
the database and the OpenSearch index".

## Context

`GET /api/v1/index/migration/readiness` tells an operator **how much** of a content index is missing,
never **which** documents. Both of its figures are whole-index counts: `driftPercent` compares the
Elasticsearch and OpenSearch copies with each other (meaningless in Phase 3, where the Elasticsearch
copy is frozen at the cutover), and `osIndexedPercent` compares OpenSearch with the database, but only
as a total. When either figure is off, the only way to find the affected content today is a manual
diff of the database against the index.

This feature adds one **read-only report** (`GET /api/v1/index/migration/drift`) that answers *which
documents are wrong*: the set of documents where the database and the OpenSearch index of one slot
(working or live) disagree. It writes nothing. Where the reindex queue recorded a failure for a
drifting document, the report also returns that recorded cause, so *why* comes from what the reindex
already stored rather than from a new write.

### Deviations from issue #38752, decided with the developer

- **No reindex action.** The issue proposed `POST …/drift/{docId}/_reindex`, writing one document to
  OpenSearch synchronously. It is dropped: a second, parallel write path into the index is not wanted,
  and it is also the part that cannot be bounded — mapping a document reads binaries from storage, which
  has no timeout, so a call could hold the drift guard indefinitely, or run on the shared mapping pool
  and trip its circuit breaker for all indexing on the node. Repair uses the existing mechanisms: saving
  or refreshing the content (`PUT /api/v1/content/_refresh/{id}`), a reindex by content type, or
  retrying failed records (#37270).
- **`@Hidden`, so `openapi.yaml` does not change.** Like readiness, the report must not reveal to regular
  users that a migration is running.
- **`journal` has a fourth value, `queued`.** A document waiting its turn in the queue is neither failed
  nor lost; reporting it as `none` ("lost without trace") would be a false diagnosis.
- **One drift call at a time across the cluster**, not "one full scan per node": a per-node limit lets
  N nodes hit the same database at once.
- **No sorted merge.** The issue's `full` mode walked both sides in order and merged them; the two
  engines do not order identifiers the same way (see below), so both directions use lookups instead.
- **No `quick` mode (decided 2026-11-27).** The issue's count-bisection mode was dropped after three
  reviews in a row found its worst defect: count ranges built on identifier prefixes cover different
  sets of documents in PostgreSQL and OpenSearch depending on case and database collation (under
  `en_US`, `':' < '0'`, so a prefix bucket can be empty on one side and a third of the table on the
  other), giving false count gaps on synchronized instances and costing nearly as much as a full walk.
  The report walks the slot in bounded pages (`full`, the only mode); a cheaper mode can be proposed
  later, with measurements, if the full walk proves too slow.

### Data sources and the role of each

| Source | Role |
|---|---|
| `contentlet_version_info` | **Reference.** One row (`identifier`, `lang`, `variant_id`, `working_inode`, `live_inode`) is exactly one index document, id `{identifier}_{languageId}_{variantId}` (`ContentletIndexAPIImpl.mapContentletForProcessor`). |
| OpenSearch index of the slot | **Compared.** Working or live, per `index=`. Elasticsearch takes no part. |
| `identifier` | Only for the `contentType` filter (`asset_subtype`). |
| `dist_reindex_journal` | **Annotation only.** Never decides a status; only explains rows already found drifting. |

**Why a new endpoint and not a readiness parameter.** Readiness is a cheap, parameterless gate with a
fixed-size answer and no pagination. A per-document diff may walk every row; mixing the two would make
the gate slow and change its contract.

### Facts verified in the code

- **Archived content is indexed in the working slot.** The reindex loads every
  `contentlet_version_info` row of an identifier and writes its `working_inode` whatever the `deleted`
  flag (`ContentletIndexAPIImpl.loadVersionInodes`); archiving removes the live document and re-adds the
  working one with `deleted: true`. So archived rows stay on the database side of the working slot. This
  matches readiness, whose database count (`ContentIndexMirrorReconciler`, `COUNT(*)` /
  `COUNT(live_inode)` over `contentlet_version_info`) also includes them.
- **The two engines do NOT order identifiers the same way.** In OpenSearch `identifier` is mapped
  `text` (analyzed), cannot be sorted and is unusable for prefix counts; the sortable copy is
  `identifier_dotraw` (`keyword`, `ignore_above: 8191`, so always present for a 36-character id). Every
  string value is **lower-cased** before it is written (`ESMappingAPIImpl`, the `mapLowered` loop) —
  `identifier`, `variant` (`DEFAULT` → `default`), `inode`, `contenttype` alike. The document `_id` keeps
  the original case. In PostgreSQL `identifier` is `varchar(36)` ordered by the database collation, with
  no `COLLATE "C"` index on `contentlet_version_info`. Identifiers that are not lower-case UUIDs exist —
  the system host is `SYSTEM_HOST` — and land in different positions on each side. A sorted merge of the
  two sides would produce false `MISSING`/`EXTRA` pairs on exactly those rows.
- **`version_ts` is indexed** (`idx_contentlet_vi_version_ts`), contrary to the issue. It does not change
  the design: `since` is applied inside a primary-key window, where that index is not used.
- **`languageid`** is a `long` (sortable); `variant` is `keyword`; `inode` is `text` with a sortable
  `inode_dotraw` copy; `contenttype` is `keyword`.
- **`live_inode` is not part of the primary key**, so any live-slot count must read table rows; readiness
  already documents its live count as a sequential scan (`ContentIndexMirrorReconciler`).
- **Failed reindex attempts store their cause.** `ReindexQueueFactory.markAsFailed` writes the cause to
  `dist_reindex_journal.index_val` (PR #38750). Priorities are `ASAP=0`, `NORMAL=100`, `STRUCTURE=200`,
  `REINDEX=300`, `ERROR=400`; a failed attempt adds 1; `isParked()` is priority ≥ 400.
- **No statement time limit exists today.** Nothing in `dotCMS/src/main/java` sets `statement_timeout` or
  a JDBC query timeout, and `DotConnect` has none; the plan must introduce it (`SET LOCAL` inside the
  call's transaction).
- **OpenSearch timeouts.** The client socket timeout (`OS_SOCKET_TIMEOUT`, 30 s,
  `ConfigurableOpenSearchProvider`) drops the connection but does not stop the search inside
  OpenSearch. A search-body `timeout` (as in `ContentFactoryIndexOperationsOS`) makes OpenSearch return
  HTTP 200 with partial hits and `timed_out: true`.
- **Visibility of what is reused.** `MigrationReadinessResource.isMigrationSupportUser` is package-private;
  `ReindexQueueFactory.FAILED_PREDICATE` (PR #38750) is `private`; `ContentIndexMirrorReconciler`'s
  database-count loader is private. Reusing them means widening their visibility or placing the new code
  beside them.
- **Readiness is `@Hidden`** (`MigrationReadinessResource`, class and method).

### How the set is computed

The report is built from two **steps**, each one window wide, each a lookup rather than a merge.

**Page step (database → OpenSearch; finds `MISSING` and `STALE`)**

```sql
-- keyset on the primary key: constant cost at any depth, never OFFSET
SELECT identifier, lang, variant_id, working_inode, live_inode
FROM contentlet_version_info
WHERE (identifier, lang, variant_id) > (?, ?, ?)
ORDER BY identifier, lang, variant_id
LIMIT :window;                               -- window ≤ 1000
```

then **one** OpenSearch request for those document ids (`_id` lookup): no document → `MISSING`;
inode different (compared lower-cased) → `STALE`; equal → not reported. In the live slot only rows with a
`live_inode` take part; that filter is applied in memory after reading the window, never in the `WHERE`,
so a run of unpublished rows cannot make one statement read more than one window. The id lookup is an
`ids` query through `_search` (not `_mget`), so it can be cancelled on the OpenSearch side. Lookup by id
does not depend on either side's ordering.

**Reverse step (OpenSearch → database; finds `EXTRA`)**

One page of OpenSearch documents walked with `search_after` on `identifier_dotraw`, `languageid`,
`variant`, `inode_dotraw` (doc-value fields, sorted with `unmapped_type` so an empty or new index does not
fail; never sort on `_id`, whose field data is disabled by default in OpenSearch 3.x). The last key is a
tiebreaker: variant names are case-sensitive, so variants `Foo` and `foo` of one content share the
lower-cased `variant`, and without it `search_after` could skip one of them. Then **one** database query
for those keys, taken from each document's `_id` (original case, exact primary-key match). The `_id` is
split anchored on the document's own fields — the identifier is the first `length(identifier_dotraw)`
characters, then `_`, the `languageid`, `_`, and the rest is the variant — because identifiers
(`SYSTEM_HOST`) and variant names may both contain `_`:

```sql
SELECT identifier, lang, variant_id, live_inode
FROM contentlet_version_info
WHERE (identifier, lang, variant_id) IN ((?, ?, ?), …);   -- ≤ 1000 tuples
```

No row → `EXTRA`. In the live slot, a row whose `live_inode` is null also → `EXTRA` (content unpublished
in the database but still in the live index). A row that exists is not reported by this step: `STALE` is
the page step's job, and reporting it here too would list the same document twice.

**Journal annotation** — after each step, **one** query for only the identifiers that came out drifting
(`WHERE ident_to_index IN (…)`, `dist_reindex_index4`), returning priority and `index_val`; skipped when
the step found no drift.

**The walk** runs the page step over the whole slot, then the reverse step over the whole index, each
resumed from its own cursor position across calls. Finds all three statuses.

**Per-call budget.** Every call has a wall-clock deadline. Between statements and requests it checks the
deadline; once spent, it stops starting new work and returns what it found so far plus the cursor — not
an error.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Find every drifting document of a slot (Priority: P1)

A migration support operator sees readiness report the working slot at 99.2 % indexed. They ask the
drift report for the working slot and page through it until the cursor runs out. They get
the exact documents that are missing from OpenSearch, present in OpenSearch with no database row, or
indexed at an older version than the database holds — each with its identifier, language, variant, the
database inode and the indexed inode.

**Why this priority**: it is the whole reason for the feature: turning a percentage into a work list.

**Independent Test**: seed one document of each kind (deleted from the index only; written to the index
only; indexed with an older inode) on an otherwise synchronized instance, page through the report,
and check that exactly those three rows come back with the right status.

**Acceptance Scenarios**:

1. **Given** a database row whose document was removed from the OpenSearch working index, **When** the
   operator runs the report for the working slot, **Then** that document is returned as `MISSING`
   with its database inode and no indexed inode.
2. **Given** a document present in the OpenSearch working index whose key has no database row,
   **When** the report runs, **Then** it is returned as `EXTRA` with its indexed inode, no database
   inode, and its identifier and variant in their original case.
3. **Given** a document whose indexed inode differs from the database `working_inode`, **When** the full
   report runs for the working slot, **Then** it is returned as `STALE` with both inodes.
4. **Given** a database and index fully in sync, including the system host `SYSTEM_HOST`, **When** the
   full report runs, **Then** it returns no rows and no further cursor.
5. **Given** rows with and without a `live_inode`, **When** the report runs for the live slot, **Then**
   only rows with a `live_inode` are considered, compared against it; for the working slot every row is
   considered, archived ones included.
6. **Given** content unpublished in the database (`live_inode` null) whose document is still in the live
   index, **When** the report runs for the live slot, **Then** it is returned as `EXTRA`.

---

### User Story 2 - Know whether a drifting document left a trace (Priority: P2)

For each returned document the operator sees what the reindex queue knows about it — waiting its turn,
being retried after failures, parked after exhausting its retries, or absent — and, when a failure was
recorded, its cause. A missing document with no queue entry is a loss that left no trace and points at a
different bug than a parked one.

**Why this priority**: turns "what is missing" into "what is missing and why"; depends on an unmerged
change (see Dependencies).

**Independent Test**: seed four missing documents — one with a queue row at normal priority, one being
retried with a recorded cause, one parked, one with none — and check each comes back with `queued`,
`retrying` (with its cause), `parked` and `none` respectively.

**Acceptance Scenarios**:

1. **Given** a queue row waiting at a base priority (0, 100, 200 or 300 — e.g. content saved seconds ago,
   or any row during a full reindex), **Then** `journal = queued`.
2. **Given** a queue row matching `FAILED_PREDICATE` that is not parked, **Then** `journal = retrying`.
3. **Given** a parked queue row (`ReindexEntry.isParked()`), **Then** `journal = parked`.
4. **Given** no queue row for the identifier, **Then** `journal = none`.
5. **Given** a `retrying` or `parked` row with a recorded cause, **Then** the row carries it as
   `journalReason`.
6. **Given** several queue rows for one identifier (e.g. a parked one and a fresh one from a new save),
   **Then** the most severe wins: `parked` over `retrying` over `queued`, with that row's cause.

---

### User Story 3 - Only migration support users can use it, and it cannot degrade the instance (Priority: P1)

The report exposes the whole content inventory, so it is available to exactly the users who can read
readiness, is invisible to everyone else, and cannot be used — by mistake or on purpose — to degrade the
database, OpenSearch or the application.

**Why this priority**: a security and availability boundary; ships with the first story or not at all.

**Independent Test**: call the report as a non-admin, as an admin without the migration support role,
and as an admin with it; fire a burst of concurrent calls on two nodes; inspect the public OpenAPI
document.

**Acceptance Scenarios**:

1. **Given** a non-admin backend user, **When** they call the report, **Then** they get 403.
2. **Given** a CMS Administrator without the role named by `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`,
   **Then** they get 403.
3. **Given** a user who can read readiness, **Then** they can use the report.
4. **Given** the role key is changed, **Then** access to readiness and to drift moves together.
5. **Given** a drift call in progress on any node, **When** a second one arrives on any node, **Then** it
   gets 429 immediately.
6. **Given** a burst of calls on one node, **Then** at most one of them borrows a database connection.
7. **Given** the public OpenAPI document, **Then** the report does not appear in it.
8. **Given** a call whose deadline is reached mid-way, **Then** it returns the rows found so far and a
   cursor, and releases the guard.

---

### Edge Cases

- Non-hexadecimal identifiers (`SYSTEM_HOST`) and case differences (`variant` `DEFAULT` vs `default`,
  inode case): must not produce false `MISSING` / `EXTRA` / `STALE` rows.
- A filter that matches nothing (`since` far in the future, an unknown `contentType`): the call still
  returns promptly, with no rows and a cursor or the end of the scan.
- `since` and `EXTRA`: an orphan has no database row and therefore no `version_ts`; with `since` set,
  `EXTRA` is not reported and the response says so.
- `contentType` and `EXTRA`: an orphan has no `identifier.asset_subtype`; with `contentType` set, the
  reverse step filters on the document's own `contenttype` (lower-cased, compared case-insensitively
  with the requested type), and the response says so.
- The OpenSearch index of the slot does not exist or the phase has no OpenSearch target (Phase 0): the
  call fails with a clear message instead of reporting every row as `MISSING`.
- The slot's active index changes between two calls of one walk (switchover, phase change): the next call
  answers 409 and the report must be restarted.
- A full reindex is in progress: the report reads the slot's current (active) index, not the one being
  rebuilt, and says so.
- Rows move between pages while the report walks; each page is a point-in-time view, not a snapshot of
  the whole slot, and the response says so.
- OpenSearch answers with `timed_out: true` or failed shards: the call fails; a partial answer is never
  read as missing documents.
- A statement exceeds its time limit: the call fails with a clear message and is not retried.
- The client disconnects mid-call: the call still ends at its deadline and releases the guard.
- `limit` above 1000 or below 1, a malformed cursor or an unknown `index`: 400.

## Requirements *(mandatory)*

### Functional Requirements

**Report**

- **FR-001**: The system MUST expose `GET /api/v1/index/migration/drift` with parameters `index`
  (`working` | `live`, required) and optional `limit`, `cursor`, `contentType` and `since`. There is no
  `mode` parameter.
- **FR-002**: Each returned row MUST carry `identifier`, `languageId`, `variantId`, `inode` (database,
  null for `EXTRA`), `indexedInode` (null for `MISSING`), `status` (`MISSING` | `EXTRA` | `STALE`) and
  `journal` (`queued` | `retrying` | `parked` | `none`), plus `journalReason` when the queue recorded a
  failure cause. Rows MUST NOT carry any content field value, with one stated exception:
  `journalReason` is returned exactly as the reindex stored it and may quote a field value (e.g. the
  preview OpenSearch embeds in a parsing error); it is visible only behind the access gate and never
  logged. `EXTRA` rows take identifier and variant
  from the document `_id` (original case).
- **FR-003**: The database side of the working slot MUST be every `contentlet_version_info` row,
  archived ones included, compared on `working_inode`; the live slot MUST be the rows with a
  `live_inode`, compared on it — and in the reverse step a row with a null `live_inode` counts as absent.
  The index side is the slot's current OpenSearch index only.
- **FR-004**: The report MUST find every `MISSING`, `EXTRA` and `STALE` document matching the filters,
  using the page step and the reverse step. It MUST NOT rely on a sorted merge of the two sides.
- **FR-005**: withdrawn with the `quick` mode (see Deviations).
- **FR-006**: Keys and inodes MUST be compared in the normalized form the index stores (lower-cased
  identifier, variant and inode), and database lookups from index documents MUST use the original-case
  key from `_id`; the result MUST NOT depend on PostgreSQL and OpenSearch ordering identifiers the same
  way. A synchronized instance that contains `SYSTEM_HOST` MUST report no rows, under any database
  collation.
- **FR-007**: Results MUST be paginated with a stateless cursor carrying the page-step key position, the
  reverse-step `search_after` values, which step is running, and the physical name of the index being
  walked. If the slot's active index is no longer that one (a full reindex switched it, or the phase
  changed), the call MUST answer 409 telling the operator to restart the report, never resume against
  another index. `limit` (rows returned, ≤ 1000) and the window (keys
  scanned per step, ≤ 1000) are separate. The cursor MUST be validated for shape and bounds; a malformed
  one is 400, and no cursor value can widen a step beyond one window.
- **FR-008**: `contentType` MUST narrow the scan to one content type; `since` MUST narrow it to rows
  whose `version_ts` is after the given instant, applied inside the scanned window. With `since`,
  `EXTRA` is not reported and the response states it.
- **FR-009**: The response MUST state the slot, the engine (`OS`), that each page is a point-in-time view, whether the call stopped on its
  deadline, and the next cursor (absent at the end).
- **FR-010**: Every row's `journal` MUST be derived from the reindex queue: `parked` when
  `ReindexEntry.isParked()`; `retrying` when the row matches `ReindexQueueFactory`'s `FAILED_PREDICATE`
  and is not parked; `queued` for any other queue row; `none` when the identifier has no queue row. With
  several rows for one identifier the most severe wins (`parked` > `retrying` > `queued`). The
  classification MUST reuse those two definitions, not copy priority ranges. The queue is an annotation
  only; it never adds or removes rows. A queue row applies to every language and variant of its
  identifier (the reindex loads every version of the identifier). `journalReason` is the cause stored in
  `index_val` by `ReindexQueueFactory.markAsFailed`, returned as stored and never logged by this feature,
  since it may quote field values.
- **FR-011**: When the slot's OpenSearch index cannot be resolved (no OpenSearch target in the current
  phase, or the index is missing), the call MUST fail with a clear message rather than report rows.
- **FR-012**: The journal annotation and the `contentType` resolution MUST each be one query per step,
  over the keys of that step only — never one query per row.

**Database load and availability**

- **FR-013**: Database page reads MUST use keyset pagination on the primary key
  `(identifier, lang, variant_id)` — never `OFFSET`, never an unbounded select.
- **FR-014**: Each step MUST scan at most one window of keys (≤ 1000, a fixed maximum, not configurable
  beyond it — a three-column `IN` list must stay far below PostgreSQL's 65,535 bind parameters), whatever
  the filters; it returns what matched in that window (possibly fewer than `limit`) plus the cursor.
- **FR-015**: withdrawn with the `quick` mode (see Deviations).
- **FR-016**: `contentType` MUST be resolved by joining `identifier` per page on its primary key
  (`identifier.id`) and comparing `asset_subtype` case-insensitively — no join over the whole table. On
  the OpenSearch side it filters on the lower-cased `contenttype`.
- **FR-017**: The page, reverse-lookup and `contentType` queries MUST have an index
  path and no sequential scan on `contentlet_version_info`. No new database index is added by this
  feature.
- **FR-018**: Every statement MUST run in a read-only transaction with a statement time limit set for
  that transaction, configurable with a default; a timeout fails the call with a clear message and is not
  retried.
- **FR-019**: At most one drift call MUST be in progress across the whole cluster at a time. A concurrent
  call gets 429 immediately, without waiting. The guard has two layers, checked in this order after the
  access gate: an in-memory per-node guard (rejects without touching the database or borrowing a
  connection), then a cluster-wide PostgreSQL advisory try-lock, transaction-scoped, taken only by the
  call that passed the node guard and keyed in a key space of its own (two-key form) so it cannot collide
  with other features' locks. Both are released when the call ends; the database lock is also released
  if the node or its connection dies mid-call.
- **FR-020**: Whole-table totals, where reported, MUST reuse readiness's database counts
  (`ContentIndexMirrorReconciler`) and MUST NOT be recomputed per page; those counts are a deliberate
  sequential scan and are excluded from FR-017's check. The unit (one `contentlet_version_info` row per
  document) is the same as readiness, so a slot with no drift reads 100 % in both endpoints.

**Denial-of-service bound** — whatever a caller sends, and however often, the load the report puts on
the database, OpenSearch and the application has a fixed ceiling. There is deliberately no "stop after N
drifting rows" parameter: the readiness count gap is a net difference (`MISSING − EXTRA`, blind to
`STALE`) and cannot serve as a stop condition; the window and the deadline already bound the work.

- **FR-032**: At any moment, across the whole cluster, drift MUST have at most one statement running on
  the database; per node, at most one drift call borrows a database connection (FR-019). Each statement
  reads at most one window of keys and is cut by the statement time limit (FR-018).
- **FR-033**: Authentication and the access gate (FR-028) MUST be checked before the guard and before any
  database or OpenSearch work.
- **FR-034**: Every input MUST be bounded and validated before any database or OpenSearch work:
  `limit` ≤ 1000, a maximum cursor size, `contentType` within its column length, `since` a valid
  instant, `index` from its fixed set. Anything else is 400.
- **FR-035**: Each OpenSearch request MUST be a `_search` (id lookups as an `ids` query, not `_mget`)
  carrying at most one window of ids or one page of `search_after`, MUST ask OpenSearch to cancel the
  search on its side when its time runs out (`cancel_after_time_interval`, not only a client socket
  timeout), and MUST treat a partial answer (`timed_out: true` or failed shards)
  as a failed call, never as a result.
- **FR-036**: A call MUST do all its work within the request: no retries, no background or deferred work
  that outlives the call and its guard.
- **FR-037**: Every call MUST have a wall-clock deadline, configurable with a default, checked between
  statements and OpenSearch requests. On reaching it the call stops starting new work and returns the
  rows found so far, the cursor and a "stopped on deadline" flag. The deadline MUST be shorter than any
  `idle_in_transaction_session_timeout` the database may have, so the guard's transaction is never cut
  from outside (the pool's leak detection only logs, it does not cut; staying under it avoids noise). A client disconnect does not extend the call beyond it.
- **FR-038**: Rejections (403, 429, 400) and timeouts MUST NOT log at a level or rate that a caller
  looping on them can use to flood the logs.
- **FR-039**: Drift's database statements MUST carry an identifying tag (application name or SQL comment)
  so they can be told apart in `pg_stat_activity`.
- **FR-040**: The cost of one reverse-step page MUST NOT grow with the size of the index beyond what is
  measured and accepted in the plan; if a `search_after` page over the whole index proves to cost in
  proportion to the index, each reverse page MUST be restricted to a key range.

**Single-document reindex — withdrawn**

- **FR-021 – FR-027**: withdrawn on 2026-11-27 together with the `_reindex` action (see Deviations). The
  numbers are kept so that references from the review history stay valid.

**Access and documentation**

- **FR-028**: The report MUST call the readiness gate `MigrationReadinessResource.isMigrationSupportUser`
  (backend user who is a CMS Administrator **and** holds the role named by
  `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`, default `os_migration_qa`), not a copy of it; anyone else
  gets 403.
- **FR-029**: The report MUST be read-only: it writes nothing to the database, the reindex queue or any
  index.
- **FR-030**: The report MUST be `@Hidden` from the public OpenAPI document, like readiness;
  `openapi.yaml` therefore does not change. It MUST still carry complete `@Operation` / `@Schema`
  annotations matching the returned type, for maintainers, and be covered by an API test (Karate or
  Postman).
- **FR-031**: `docs/backend/OPENSEARCH_MIGRATION.md` MUST state, as a rule for every endpoint under
  `/api/v1/index/migration` (readiness, drift and any future one), that it is `@Hidden` from OpenAPI and
  gated by `isMigrationSupportUser`, and MUST document the drift report — including its load guards and
  how to repair what it lists with the existing mechanisms — next to the readiness section. Today the doc
  states the rule for readiness only.

### Key Entities

- **Drift row**: one document where the database and the index disagree — key (identifier, language,
  variant), database inode, indexed inode, status, journal annotation and recorded cause. No content
  values.
- **Drift page**: slot, engine, rows, point-in-time notice, deadline flag, next cursor.
- **Cursor**: opaque, stateless, validated and bounded; carries the step being run, the page-step key
  position and the reverse-step `search_after` position.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On an instance seeded with one missing, one orphan and one stale document, the report lists
  exactly those three.
- **SC-002**: On a synchronized instance that includes `SYSTEM_HOST`, the report returns zero rows, under
  both the `C` and an `en_US` database collation.
- **SC-003**: No step reads more than one window of database keys, whatever the filters; a filter that
  matches nothing answers within the same time bound as one that matches everything.
- **SC-004**: The page, reverse-lookup and `contentType` queries show no sequential
  scan on `contentlet_version_info`, verified with `EXPLAIN` in an integration test run with sequential
  scans disabled (a small test table would otherwise make the planner prefer one).
- **SC-005**: A user who cannot read readiness cannot use the drift report, and vice versa, in 100 % of
  the access cases tested.
- **SC-006**: Running the report leaves the database, the reindex queue and both engines'
  indices unchanged.
- **SC-007**: Two drift calls issued at the same time on two nodes: exactly one runs, the other gets 429.
- **SC-008**: The number of database queries per step is constant (page, journal, `contentType`),
  independent of the number of rows in it.
- **SC-009**: Under a burst of concurrent drift calls from several sessions and nodes, `pg_stat_activity`
  never shows more than one tagged drift statement running; per node at most one drift call borrows a
  connection, every other call on that node gets 429 without any drift statement (the authentication
  and role check that runs first may read the database like on any endpoint), and an unauthorized burst
  issues no drift statement at all.
- **SC-010**: No call runs longer than its deadline plus the longer of one statement time limit and one
  OpenSearch request timeout, including when the client disconnects; and no OpenSearch search started by drift keeps running after its call ends.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the ES → OpenSearch migration support surface
  (`MigrationReadinessResource`, `/v1/index/migration`) — modern code under `com.dotcms`. It reads the
  legacy reindex queue (`com.dotmarketing.common.reindex`, `dist_reindex_journal`) and the legacy
  version table `contentlet_version_info`, and queries the OpenSearch content index. It writes nothing
  and changes no existing write path.
- **Backward-compatibility expectations**: readiness keeps its contract unchanged; the new endpoint is
  additive, read-only and hidden. No database schema change and no index mapping change (neither is
  rollback-unsafe here). `PUT /api/v1/content/_refresh/{identifierOrInode}` is not changed; it stays the
  way to requeue a single document once the report has named it.
- **Known related decisions**: no new write path into the index (the issue's `_reindex` action was
  dropped). Transaction-scoped PostgreSQL advisory locks already serialize work in
  `FolderBulkDeleteHelper` (blocking variant); drift uses the non-blocking try variant. Related work: the
  epic #38690, the failed-reindex-records fix #37270 (PR #38750), the orphan-on-destroy fix #37908 (a
  source of `EXTRA` documents), the dual-write cause-in-log-only issue #37271. The plan will consult
  `dotCMS/platform-adrs`.

## Dependencies

- **PR core#38750 (fix for #37270, branch `37270-failed-reindex-records-impl`), open.** It introduces
  `ReindexEntry.isParked()`, `ReindexQueueFactory`'s `FAILED_PREDICATE` and the recorded failure cause the
  journal annotation (FR-010, User Story 2) reuses; `FAILED_PREDICATE` is `private` there and must be
  made reachable. Until it merges, implementation branches off it; everything else in this spec is
  independent of it.

## Assumptions

- The OpenSearch index of each slot holds one document per database row with the id
  `{identifier}_{languageId}_{variantId}` and stores `inode`, `identifier_dotraw`, `languageid`,
  `variant` and `contenttype` on every content document (verified in `ESMappingAPIImpl`).
- `deleted = true` rows remain indexed in the working slot (verified above); if that behavior changes,
  this report changes with it.
- The window (≤ 1000), the statement time limit and the per-call deadline are set in the plan with
  measurements on a production-sized copy.
- To verify in the plan: that `pg_try_advisory_xact_lock` works inside a `READ ONLY` transaction and holds
  across the OpenSearch requests made inside the call; that OpenSearch 3.x honours server-side
  cancellation (`cancel_after_time_interval`) for the `ids` and `search_after` searches used; and the cost of a
  `search_after` page against index size (FR-040).
- Elasticsearch drift, site-search indices and non-content indices are out of scope.
