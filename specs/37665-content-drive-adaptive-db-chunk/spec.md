# Feature Specification: Content Drive Adaptive DB Chunk for Permission-Filtered Listings

**Feature Branch**: `issue-37665-content-drive-adaptive-chunk`

**Created**: 2026-09-28

**Status**: Draft

**Type**: New Feature (performance, behind a configuration flag)

**Related GitHub Issue**: [#37665](https://github.com/dotCMS/core/issues/37665). Found during the
permission-scoped QA of [#37229](https://github.com/dotCMS/core/issues/37229). Shares the chunk
loop with [#37184](https://github.com/dotCMS/core/issues/37184) (field-filter chunk size) and
[#37211](https://github.com/dotCMS/core/issues/37211) (time-bounded text-search scan), both
merged.

**Input**: User description: "In BrowserAPIImpl#getContentByChunks, permission-only path only,
grow the per-iteration DB chunk from the observed visible/read ratio; when nothing was visible,
double it. Never below the caller's chunk size, never above a configurable ceiling nor the
remaining scan budget. Behind BROWSER_DB_CHUNK_ADAPTIVE (default on); off restores today's fixed
chunk. The Elasticsearch-narrowed path is untouched."

## Background

When Content Drive lists a folder without a text search, the system reads the folder's items
from the database in fixed-size chunks (400 rows for a 40-item page), drops what the user may not
read, and repeats until the page is full or the folder ends. Each chunk re-runs the whole listing
query further into the result set, and the database cannot jump to row N without producing the N
rows before it. So N chunks re-read the start of the listing N times: **the cost grows with the
square of the chunk count**.

The chunk count depends on how much the permission check discards, not on folder size:

| user | visible in a 21,383-child folder | chunks today |
|---|---|---|
| administrator | everything | 1 |
| reads 1.4% | 151 of 11,000 | 8 |
| reads none | 0 | 54 |

After the first chunk, the loop already knows what fraction of rows the user can see. It can size
the next read to what it still needs instead of asking for the same 400 rows 54 times. The total
number of rows read barely changes. What collapses is how many times the start of the listing is
re-read.

### Measured on a prototype

About 40 lines behind the same flag, measured on a local instance built from `main` @
`ba7ab6f0a2`: 59k identifiers, PostgreSQL 18, 7 repetitions per case, warm-up discarded,
`VACUUM ANALYZE` before each run.

| case | today | adaptive | change |
|---|---|---|---|
| 21,383-child folder, user reads none | 5,026 ms | 1,258 ms | -75% |
| 21,383-child folder, user reads 151 | 5,009 ms | 1,344 ms | -73% |
| 11,000-child folder, user reads none | 1,619 ms | 669 ms | -59% |
| 11,000-child folder, user reads 151 | 510 ms | 242 ms | -52% |
| any folder, administrator | 192 ms | 202 ms | unchanged |

Blocks read per request fell about 9x (5,876,712 → 652,968), and chunks fell from 54 to 6. Walking
every page to the end returned the same items in the same order, with no duplicated or skipped
pages. The worst case lands at 1.3 s, below the 3.5 s that same case cost before #37229.

### Ruled out: keyset pagination

Keyset pagination was prototyped and measured as a dead end for this query: 6% slower than the
current offset approach, with blocks read essentially unchanged (5,767,884 vs 5,876,712). It only
turns a skip into a seek when an index supplies the sort order. Here the order comes from an
explicit sort over a four-table join, and no index can provide it, because the modification date
and the parent path live in different tables. Recorded so nobody spends the time again.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A permission-limited user browses a large folder promptly (Priority: P1)

A content author with read access to only part of a large folder, or none of it, opens that
folder in Content Drive. Today the listing re-reads the folder's content dozens of times before
it can return a page, or conclude that there is nothing to show: about 5 s on a 21k-child folder.
With the adaptive chunk, the same request makes far fewer passes and returns in a fraction of the
time.

**Why this priority**: This is the whole point of the feature. The cost falls on exactly the users
who see the least, and it is the worst latency left in plain folder browsing.

**Independent Test**: Browse a large folder as a user who can read none of it, and as a user who
can read a small slice. Compare the number of database passes and the latency with the flag on and
off.

**Acceptance Scenarios**:

1. **Given** a folder with ~21,000 children and a user who can read none of them, **When** the
   user opens the folder with the flag on, **Then** the request makes a small fraction of the
   passes it makes with the flag off (prototype: 6 vs 54) and latency falls by at least half.
2. **Given** the same folder and a user who can read a sparse ~1% of it, **When** the user opens
   the folder, **Then** the first page holds the same items as with the flag off, and latency falls
   by at least half.
3. **Given** a folder the user can read in full (an administrator), **When** the user opens it,
   **Then** the request still makes exactly one pass and latency does not measurably change.
4. **Given** a folder where the user's visible items are clustered (1 visible in the first chunk,
   the rest of the page right after it), **When** the user opens the folder, **Then** latency with
   the flag on is no worse than with the flag off, within run-to-run noise, and no chunk exceeds
   the per-step growth cap.

---

### User Story 2 - Paging stays exactly as correct as today (Priority: P1)

Any user paging through a folder, whether an administrator or a limited user, gets the same items
in the same order, page after page, whether the flag is on or off. No item appears twice and none
is skipped. The last page correctly reports that there is nothing more.

**Why this priority**: A faster listing that drops or repeats items is a regression, not a
feature. Chunk boundaries now move from one iteration to the next, and chunk boundaries are
exactly where the existing continuation position and "more pages" logic have broken before (see
the #37184 review findings in the chunk loop's comments).

**Independent Test**: Walk every page of a folder to the end with the flag on and with it off, for
an administrator and for a permission-limited user, and compare the two item sequences.

**Acceptance Scenarios**:

1. **Given** a folder and a permission-limited user, **When** every page is walked to the end
   with the flag on and then with it off, **Then** the two sequences are identical: same items,
   same order, no duplicates, no gaps.
2. **Given** the same walk for an administrator, **Then** the sequences are identical as well.
3. **Given** a grown chunk that comes back with fewer rows than were requested for that
   iteration, **When** the loop evaluates it, **Then** it treats the folder as exhausted and
   reports no further pages. It must not treat the short chunk as "full" just because its row
   count exceeds the original fixed chunk size.
4. **Given** a page that fills inside a grown chunk, **When** the next page is requested, **Then**
   it resumes right after the last item returned, not at the end of the grown chunk.

---

### User Story 3 - Operators can turn it off (Priority: P2)

An operator who suspects the adaptive chunk in a production incident can switch it off by
configuration, without a redeploy, and get exactly today's fixed-chunk behavior back.

**Why this priority**: This is a rollback lever for a change in a hot path. It is not user-facing
value, but it is what makes shipping the change with the flag on by default acceptable.

**Independent Test**: With the flag off, the chunk sizes the loop requests match today's fixed
size on every iteration.

**Acceptance Scenarios**:

1. **Given** the flag set to off, **When** any permission-only listing runs, **Then** every chunk
   is requested at today's fixed size and the results match today's behavior.
2. **Given** the flag toggled at runtime, **When** the next request runs, **Then** it uses the new
   setting without a restart.

---

### Edge Cases

- **Nothing visible so far**: there is no ratio to extrapolate from, so the next chunk doubles.
  Geometric growth turns a linear walk over the folder into a logarithmic number of passes. The
  price is overshoot: when the visible items show up early in a doubled chunk, the rows after the
  page's last item are read for nothing. That overshoot is never larger than the rows already
  read in the request, and today's loop reads those same rows anyway, over more passes.
- **Visible items are clustered, not spread evenly** (per subfolder or per content type): the
  observed ratio is only a sample of the rows read so far, and it can badly underestimate the
  stretch that comes next. For example, on a 40-item page, 1 visible item sits in the first 400
  rows and the other 39 sit in the next 400. The ratio projects 39 / (1/400) x 1.5 ≈ 23,400 rows,
  where today's loop needs one more 400-row chunk. Every row read is loaded and
  permission-checked. The rows past the page's last item are wasted work, and the next page
  re-reads them, because it resumes right after that item. The per-step growth cap (FR-003)
  bounds this: here the loop reads 1,600 rows, not the 7,000 ceiling.
- **Caller's chunk size already at or above the ceiling** (a very large page size): the chunk
  never grows, because the floor wins over the ceiling. Behavior matches today.
- **Scan budget nearly used up**: growth never takes a chunk past what is left of the scan budget.
  The chunk is never shrunk below the caller's size, though, so the loop reads no more past the
  budget than today's loop does.
- **Page starts deep in the folder** (the continuation position already at or past the scan budget):
  the first chunk is requested at the caller's size, exactly as today, and the existing cutoff
  then ends the scan after it.
- **Partial last chunk after growth**: treated as the end of the folder (User Story 2, scenario 3).
- **Page fills in the first chunk** (administrator, or a dense folder): no growth ever happens.
- **DB-routed filters combined with the listing** (tag, workflow scheme or step, DB-routed field
  filters): these narrow the SQL candidate set and still take this path. Results must be unchanged
  with the flag on or off.
- **Text search, or a field filter resolved by the search index**: these take the
  Elasticsearch-narrowed path, and nothing about it changes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: On the permission-only listing path (no text search, no index-routed field filter),
  the system MUST size each chunk after the first from what earlier chunks in the same request
  returned:
  - **Some items were visible**: the next size is the number of items still needed divided by
    the observed visible-to-read ratio, multiplied by a safety factor. The ratio covers the whole
    request so far. The factor defaults to 1.5 and is configurable, because the ratio is a sample
    and the next stretch of the folder may be sparser. The product (still needed x rows read x
    safety factor / visible so far) is rounded up to the next whole row, and the limits in FR-003
    apply after rounding.
  - **No items were visible yet**: the next size is double the size *requested* for the previous
    chunk.
- **FR-002**: The first chunk of every request MUST be requested at today's size: the larger of
  the page size times `BROWSER_DB_CHUNK_FACTOR` and `BROWSER_DB_CHUNK_MIN_SIZE`, clamped to
  `BROWSER_DB_MAX_SCAN_ROWS` as today.
- **FR-003**: A grown chunk MUST NOT exceed any of these:
  - a per-step growth cap: a configurable multiple of the size requested for the previous chunk,
    4x by default. The doubling path (2x) never reaches it. It exists to bound the ratio path
    when visibility is clustered (see Edge Cases);
  - the configurable ceiling, which defaults to 7,000 (the same default as
    `BROWSER_SINGLE_PASS_CHUNK_SIZE`);
  - what is left of the `BROWSER_DB_MAX_SCAN_ROWS` budget, measured the same way as the existing
    cutoff.
  A chunk MUST NOT be requested below the caller's chunk size either. When the floor and a limit
  conflict, the floor wins, so the feature never makes the loop read more past the budget than
  today's loop does.
- **FR-004**: The loop MUST decide both of these against the size requested in that same
  iteration, not against the fixed initial size:
  - whether a chunk was "full", which drives whether more pages exist when a page fills;
  - whether a chunk was "partial", which means the folder is exhausted.
- **FR-005**: How a page picks up where the previous one stopped MUST NOT change. The next page
  starts at the row position right after the last item returned, even when that item sits inside a
  grown chunk.
- **FR-006**: With the feature on and with it off, listings MUST return the same items in the same
  order across every page, with no duplicates and no gaps, for both unrestricted and
  permission-limited users.
- **FR-007**: The feature MUST be controlled by `BROWSER_DB_CHUNK_ADAPTIVE`, which defaults to on.
  When it is off, every chunk MUST be requested at today's fixed size. The flag, the ceiling, the
  per-step growth cap and the safety factor MUST take effect on the next request without a restart, the same way
  `BROWSER_DB_MAX_SCAN_ROWS` does.
- **FR-008**: The Elasticsearch-narrowed path (a text search, or an index-routed field filter)
  MUST NOT change. Its chunk size, its time budget (`BROWSER_DB_MAX_SCAN_TIME_MILLIS`) and its row
  hard cap (`BROWSER_DB_MAX_SCAN_ROWS_ES_HARD_CAP`) keep today's behavior. The tests added by
  #37211 and #37184 MUST keep passing unchanged.
- **FR-009**: The unpaginated listing, where every row is fetched at once, MUST NOT change.
- **FR-010**: The SQL and the permission filter MUST NOT change. The feature only changes how many
  rows each chunk requests.
- **FR-011**: The loop's debug logging MUST record the size requested for each chunk, so an
  operator can confirm growth from the logs.

### Key Entities

- **Chunk**: one read of consecutive rows from the folder listing, starting at a row position.
  Today every chunk in a request has the same size. With this feature, each chunk's size can
  differ from the one before.
- **Visible-to-read ratio**: the permission-visible items returned so far in the request, divided
  by the rows read so far in the request.
- **Scan budget**: the existing cap on how far a permission-only scan may read
  (`BROWSER_DB_MAX_SCAN_ROWS`, default 50,000).
- **Continuation position**: the row position the next page resumes from. Its meaning is
  unchanged.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A large folder (~21k children) browsed by a user who can read none of it gets back
  at least 50% faster than with the feature off, on the same dataset and instance. The prototype
  measured -75%.
- **SC-002**: The same folder browsed by a user who can read a sparse slice (~1%) gets back at
  least 50% faster. The prototype measured -73%.
- **SC-003**: An administrator's listing still takes exactly one pass, with no measurable latency
  difference: within run-to-run noise, where the prototype measured 192 vs 202 ms.
- **SC-004**: For a user who can read nothing, the number of passes needed to finish a folder
  grows roughly logarithmically with folder size instead of linearly. In the reference case it
  falls from 54 to 10 or fewer. The prototype measured 6. The bound is looser on purpose, so the
  test holds when the safety factor, the ceiling or the per-step cap are tuned.
- **SC-005**: Walking every page of a folder to the end with the feature on and off gives identical
  sequences, for both an administrator and a permission-limited user: 0 duplicated and 0 skipped
  items.
- **SC-006**: With the feature off, every requested chunk size equals today's fixed size.
- **SC-007**: With clustered visibility (the fixture in User Story 1, scenario 4), latency with
  the feature on is no worse than with it off, within run-to-run noise, and no requested chunk
  exceeds the per-step growth cap.
- **SC-008**: Every existing test for the Elasticsearch-narrowed scan (#37211, #37184) passes
  without modification.

SC-001 to SC-003, and the latency half of SC-007, are latency outcomes. They are verified by a
manual benchmark that repeats the issue's method (same dataset shape, repetitions, warm-up
discarded, statistics refreshed first), with the clustered case added to its matrix. They are not
verified by automated assertions, because wall-clock time is not stable in CI. The automated tests
assert the deterministic proxies: pass count (SC-004), requested chunk sizes (SC-006, and the
per-step cap in SC-007, on a fixture that clusters the visible items right after the first
chunk), and page sequences (SC-005).

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's folder listing on the database-first,
  permission-filtered path, which is the default for browsing without a text search. The same loop
  also serves the Elasticsearch-narrowed text search, which this feature leaves alone. The loop has
  been adjusted by several recent issues (#37184, #37211, #37229), and its comments record
  boundary bugs found in review. Those are the areas at risk.
- **Backward-compatibility expectations**: No API, response shape or content change. The
  continuation position the client sends back keeps its meaning. Existing configuration keeps its
  meaning and defaults. The change is safe to roll back: it adds configuration, with no schema or
  index change, and turning the flag off restores today's behavior exactly.
- **Known related decisions**:
  - #37184 set the widened chunk size for field filters on the Elasticsearch-narrowed path, and
    the rule that the chunk is clamped to the scan budget. This feature leaves that path alone and
    keeps the clamp.
  - #37211 bounded the Elasticsearch-narrowed scan by elapsed time plus a row hard cap. Growing
    chunks there would change how those bounds behave, which is why that path is out of scope.
  - Keyset pagination was measured and rejected (see Background).
  - The plan will formally consult `dotCMS/platform-adrs`.

## Assumptions

- "What is left of the scan budget" uses the same measure as the existing permission-only cutoff,
  which compares the absolute row position against `BROWSER_DB_MAX_SCAN_ROWS`. Changing that
  measure is out of scope.
- The ratio covers the whole current request, not only the last chunk. A single sparse chunk
  therefore doesn't swing the estimate too far.
- The ceiling, the per-step growth cap and the safety factor are new configuration properties.
  Final key names are settled in the plan. The proposed names are
  `BROWSER_DB_CHUNK_ADAPTIVE_MAX_SIZE`, `BROWSER_DB_CHUNK_ADAPTIVE_MAX_GROWTH` and
  `BROWSER_DB_CHUNK_ADAPTIVE_SAFETY_FACTOR`.
- The prototype numbers in Background were measured without the per-step growth cap. The cap does
  not affect the nothing-visible case (doubling stays under 4x), but it can add a pass in the
  sparse-visible cases. The manual benchmark re-measures every case with the cap in place.
- The flag defaults to on, because the gain is measured, and turning it off needs no redeploy.
- Tests follow Constitution Principle V: they are written, approved by a dev, and confirmed failing
  before any implementation code. The existing chunk-loop tests in `BrowserAPITest`
  (`test_getPaginatedContents_scanLimit*`, `*_pagesAreGapAndDuplicateFree`) are the baseline, and
  they must keep passing.
- The prototype measurements are on 59k identifiers, not the 418k reference dataset from #37183.
  Gains at that scale are expected to be at least as large, since the quadratic term dominates
  more there. They are not measured.
