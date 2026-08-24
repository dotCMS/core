# Spike Specification: Content Drive Folder-Listing Query — Planner Instability at Scale

**Feature Branch**: `37148-content-drive-folder-listing-plan-instability`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Spike / Investigation (not a design or implementation spec — no fix is prescribed here)

**Related GitHub Issue**: [#37183](https://github.com/dotCMS/core/issues/37183). Parent epic: [#36814](https://github.com/dotCMS/core/issues/36814). Originally investigated as item 1 of [#37148](https://github.com/dotCMS/core/issues/37148) (umbrella investigation issue); items 2–4 of that investigation are tracked as separate sibling issues and are out of scope here except where they create a coordination constraint (see "Coordination Risk").

**Input**: Issue #37148 description + comment 1 (which supersedes the description's framing for item 1) + comment 2 (context only) + comment 3 (context only, item 4).

<!--
  No dedicated spike template exists in .specify/templates/ (only spec-template.md for
  features and spec-issue-template.md for defects; .github/ISSUE_TEMPLATE/spike.yaml is a
  GitHub issue form, not a spec-kit spec template). This document reuses the
  issue-resolution shape where it fits (Problem Statement, Scope, Regression Risk) but
  replaces the "Root-Cause Hypothesis → Fix Scope" sections with the sections the task
  explicitly asked for, since a spike's deliverable is a decision-ready investigation
  record, not an acceptance-tested fix. All findings below come from reading code and from
  the measurements already recorded in issue #37148; there is no live/profiled instance
  available in this environment, so every candidate direction is a code-reading-informed
  hypothesis, not a validated result. That limitation is called out again per-section.
-->

## Problem Statement *(mandatory)*

`BrowserAPIImpl` builds one SQL query shape to list a folder's contents for both Content
Drive and the legacy Site Browser. That query joins `contentlet_version_info`, `identifier`,
`structure`, and `contentlet`; filters on `identifier.parent_path` (plus language, deleted,
variant, content-type exclusions); and orders by `contentlet.mod_date` with `LIMIT`/`OFFSET`
pagination. The query and its exact construction:

- `BrowserAPIImpl.selectQuery(BrowserQuery)` — `dotCMS/src/main/java/com/dotcms/browser/BrowserAPIImpl.java:1946`
- Folder predicate: `appendFolderQuery` — line 2134 (`and id.parent_path=?`, line 2137)
- `ORDER BY`: `appendOrderByQuery` — line 2513 (`order by c.mod_date asc|desc`, lines 2514–2519)
- Pagination: `buildPaginatedDotConnect` appends `LIMIT ? OFFSET ?` — line 364
- Chunk-size derivation: `getContentUnderParentFromDB(BrowserQuery, int)` — line 192,
  `chunkSize = max(maxRows * BROWSER_DB_CHUNK_FACTOR, BROWSER_DB_CHUNK_MIN_SIZE)` — line 211
  (`BROWSER_DB_CHUNK_FACTOR` defaults to 10, `BROWSER_DB_CHUNK_MIN_SIZE` to 200 — lines 724–729),
  so a 40-row page requests `LIMIT 400` in production.

**The originally reported "wrong access path" framing does not hold across the dataset.**
The query is fast (0.1 ms–17 ms) on every folder from 0 to 4,188 children, and only degrades
(564 ms) on one folder with 21,383 children — all at the same `LIMIT 400`. This is not "the
query enters by the wrong table"; the query enters by different tables depending on folder
size, and the planner's choice becomes wrong only past a size/`LIMIT` combination it
misjudges. The actual problem is **planner plan-instability tied to `LIMIT`**: below roughly
`LIMIT 500` the planner walks `idx_contentlet_mod_date` hoping to satisfy the `ORDER BY`
early; at `LIMIT 900+` it switches to a gather-and-sort plan that is ~10x faster on the large
folder. The production chunk size (400) sits just above the crossover, i.e. at the worst
point of the slow plan. `contentlet.mod_date` and the folder filter (`identifier.parent_path`)
live in different tables, so no single index can drive both, which is what leaves the planner
guessing.

Layered on top of that, the largest folder shows **bimodal latency independent of the
`LIMIT` effect**: most requests land around 165 ms (baseline sits at 68–85 ms) but roughly a
quarter spike to ~587 ms, without an extra scan-query execution to explain it (still 1.0×
scan calls per request in every sampled case). This spike's cause is undetermined and is
explicitly in scope for this document per the reporter's decision (see "Latency Bimodality").

## Investigation To Date *(mandatory)*

All figures below are from issue #37148 comment 1 unless noted, measured on the same
production-scale dataset (~418k contentlets, ~14k folders, ~411k identifiers), warm, three
runs each at production `LIMIT 400` unless a table states otherwise.

### 1. The query is fast almost everywhere; one folder shape breaks it

| folder children | current query | driving relation |
|---|---|---|
| 0 | 0.1 ms | `structure` |
| 83 | 1.4 ms | `structure` |
| 317 | 4.3 ms | `structure` |
| 2,129 | 13.1 ms | `structure` |
| 4,188 | **17.1 ms** | `structure` |
| 21,383 | **564 ms** | `contentlet` (via `idx_contentlet_mod_date`) |

Folder-size distribution in the dataset: 11,827 folders with 1–50 children, 1,062 with
51–500, 101 with 501–5,000, 4 above that. The pathological plan is rare in absolute folder
count but not necessarily in traffic (large folders are disproportionately likely to be the
ones users page through repeatedly).

### 2. The mechanism is a `LIMIT`-driven plan crossover, not a bad query shape

Same folder (21,383 children), same query, only `LIMIT` varied:

| LIMIT | plan | time |
|---|---|---|
| 40 | Index Scan on `idx_contentlet_mod_date` | 114 ms |
| 200 | Index Scan on `idx_contentlet_mod_date` | 105 ms |
| 400 | Index Scan on `idx_contentlet_mod_date` | **457 ms** |
| 500 | Index Scan on `idx_contentlet_mod_date` | — |
| 900 | Gather + `Sort` on `c.mod_date` | **49 ms** |
| 2000 | Gather + `Sort` on `c.mod_date` | **47 ms** |

The original issue description's `EXPLAIN` (554 ms case) shows why the low-`LIMIT` plan is so
expensive: it scans 104,277 `contentlet` rows via `idx_contentlet_mod_date` and probes
`identifier` 98,349 times to find 400 matching rows, touching 909,424 buffers — the planner's
row-count estimate for "how many `mod_date`-ordered rows until 400 satisfy `parent_path`"
badly undershoots on this folder. `BROWSER_DB_CHUNK_FACTOR`'s default of 10 puts every
production request at `LIMIT 400` — the worst point measured.

### 3. A folder-first rewrite fixes the large folder and regresses mid-size ones

Proof-of-concept: a CTE that materializes `identifier` rows for the folder first, then joins
outward to `contentlet_version_info` / `structure` / `contentlet`:

```sql
with folder_ids as materialized (
  select id, asset_subtype from identifier
  where parent_path = ? and host_inode = ?
)
select cvi.working_inode as inode
from contentlet_version_info cvi
join folder_ids fi on fi.id = cvi.identifier
join structure struc on struc.velocity_var_name = fi.asset_subtype
join contentlet c on c.inode = cvi.working_inode
where cvi.variant_id = ? and struc.inode not in (?)
  and cvi.lang in (?) and cvi.deleted = ?
order by c.mod_date asc LIMIT ? OFFSET ?
```

| folder children | current | folder-first rewrite |
|---|---|---|
| 4,188 | 17.1 ms | **56.4 ms (3× slower)** |
| 21,383 | 564 ms | **112 ms (4.3× faster)** |

Result sets were verified byte-identical in both directions at both sizes — this is a real
regression on the mid-size folder, not a measurement artifact. **A blanket rewrite to this
shape is rejected** because most folders in the dataset are small-to-mid and would get
strictly slower.

### 4. Raising the chunk size (config-only mitigation) is also rejected

`BROWSER_DB_CHUNK_FACTOR` raised from 10 (chunk 400) to 25 (chunk 1000, above the crossover),
measured end-to-end at the endpoint level:

| folder children | chunk 400 | chunk 1000 | change |
|---|---|---|---|
| 1,479 | 174 ms | 247 ms | **+42%** |
| 2,229 | 181 ms | 181 ms | 0% |
| 21,423 | 714 ms | 662 ms | −7% |

Barely helps the large folder, clearly regresses a mid-size one. Ruled out as a solution on
its own.

### 5. Two correctness findings live in the same query (tracked, not this document's focus)

- **Host filtering**: the folder predicate itself (`appendFolderQuery`, line 2134) filters
  only on `id.parent_path`, with no `host_inode` in that clause. Reading the surrounding code
  (`selectQuery`, lines 1958–1974), a *separate* `and (id.host_inode = ?)` clause is added by
  `appendSiteQuery` (line 2113) whenever `browserQuery.folder != null && !ignoreSiteForFolders`
  — i.e., host scoping already exists in current code as an additional `AND`, not merged into
  the folder predicate. The issue's claim that the folder-first rewrite "adds the host filter
  as a side effect" describes the CTE's `where parent_path = ? and host_inode = ?" as a single
  compound predicate, not a capability current code lacks outright. Whether the existing
  separate-clause form is equivalent in all call paths (e.g. when `site` is null but `folder`
  is set) was not re-verified here — flag for whoever designs the fix, not a spike deliverable.
- **No tiebreaker on `ORDER BY c.mod_date`**: ties order non-deterministically; 1.2% of rows in
  the tested folder (256 of 21,423) share a `mod_date` with another row, which can duplicate or
  skip rows across pages. Any candidate direction below should carry a deterministic tiebreaker
  (e.g. `, c.inode`) regardless of which access-path strategy is chosen.

## Candidate Directions *(mandatory — none verified; this section is the spike's core deliverable)*

None of the three directions below have been prototyped end-to-end or measured. Each is
assessed by reading the relevant code and schema, not by benchmarking.

### (a) Carry the ordering key alongside the folder-scoped table

**Idea**: make `identifier` (or something joined cheaply to it) expose `mod_date` directly, so
a single index can satisfy both the folder filter and the sort order, and the planner no
longer needs to estimate a row count to decide whether an index-order scan will pay off early.

**What it would concretely require**, from reading `dotCMS/src/main/resources/postgres.sql`:

- There is **no existing index on `identifier.parent_path` alone**. Equality lookups on
  `parent_path` are currently served incidentally by the unique constraint
  `unique (parent_path, asset_name, host_inode)` (line 1033), whose auto-generated index has
  `parent_path` as its leading column — usable for equality, but it carries no `mod_date`.
  The only other `parent_path`-adjacent index in the schema is
  `idx_identifier_parent_path_trigger` (line 2236), a functional index on
  `lower(parent_path||asset_name||'/')` scoped to `(host_inode, asset_type, ...)` — built to
  support the folder-existence trigger, not folder-listing queries, and not usable here since
  it indexes a concatenated expression, not `parent_path` by itself.
- `mod_date` lives on `contentlet`, one join away from `identifier` (via
  `contentlet_version_info.identifier` → `contentlet.inode`). Making it available "alongside"
  `identifier` for indexing means one of:
  1. **A new denormalized column** — e.g. `identifier.content_mod_date`, kept in sync on every
     contentlet write (working and/or live) — plus a covering index
     `(host_inode, parent_path, content_mod_date)`. This is a genuine schema change with a
     write-side sync obligation (trigger or application-level write) and a backfill of ~411k
     existing identifier rows. It also raises a data-integrity question: `contentlet.mod_date`
     is versioned (working vs. live vs. per-language), so "the" mod_date for an identifier is
     ambiguous without picking a specific version's date to track — that ambiguity has to be
     resolved before this is even a well-formed design, not just an implementation detail.
  2. **A covering index that still lives on `contentlet`/`contentlet_version_info`**, keyed to
     put the join columns before the filter — unlikely to help, since the fundamental problem
     is the planner choosing which relation drives the scan, and a same-table index change
     doesn't remove the cross-table order/filter mismatch.
  3. **A materialized view** pre-joining identifier + latest mod_date per folder-relevant
     content — solves the query shape but adds a refresh-lag component, which risks exactly the
     read-your-writes violation ADR-0018 exists to prevent (see "ADR-0018 Constraint" below) if
     the view isn't refreshed synchronously with every write, and a synchronously-refreshed
     view is essentially the same write-side cost as option 1 with more moving parts.

**Constitution fit**: Principle I (Legacy-Aware Development) requires progressive enhancement
over wholesale legacy rewrites, and treats schema changes as high-scrutiny by default; this
direction is the most invasive of the three and would need explicit justification in a plan's
Complexity Tracking table. It is not disqualified, but it is the direction most likely to draw
a "why not the cheaper option first" review objection.

**Read on promise (non-binding)**: this is the only direction that removes the root cause
(planner estimation risk) rather than working around it, but it is the most expensive to
build, verify, and roll out (schema migration + backfill + write-path change), and it opens a
design question (which mod_date to denormalize, across versions/languages) that has no
obvious answer yet.

### (b) Stabilize the plan at the top end without changing the query shape

**Idea**: keep `selectQuery(...)`'s shape, but prevent the planner from choosing the slow
`idx_contentlet_mod_date`-driven plan at `LIMIT` values near 400.

**What's realistic in this codebase**: a repo-wide search (`dotCMS/src/main/java/com/dotcms/`,
`dotCMS/src/main/resources/db/`) found **no existing use** of Postgres planner hints,
`SET LOCAL enable_seqscan`/`enable_sort`-style session tuning, forced-index query patterns, or
custom statistics/histogram tuning (`ALTER TABLE ... SET STATISTICS`) anywhere in the SQL
building code or migrations. `DotConnect`-built queries throughout the codebase are plain
parameterized SQL with no planner-steering constructs. Postgres itself has no native
`FORCE INDEX` (unlike MySQL); the idiomatic ways to influence this planner decision are:

- `pg_hint_plan` extension (not present in dotCMS's supported Postgres deployment as far as
  this reading found — introducing an extension dependency is itself a deployment-surface
  change that would need its own review, separate from this query's fix);
- `SET LOCAL enable_indexscan = off` (or similar) scoped to this one query/session — technically
  possible from `DotConnect` by issuing an extra statement before the query, but it is a blunt,
  global-to-the-session hammer with no precedent in this codebase, and it would need to be
  proven safe against every other query that might run on the same pooled connection/session
  around the same time;
- Raising `identifier`'s or `contentlet`'s statistics target
  (`ALTER TABLE ... ALTER COLUMN ... SET STATISTICS n; ANALYZE`) so the planner's row-count
  estimate for `parent_path` is more accurate — cheapest to try, but its effect is
  data-distribution-dependent (more accurate on this dataset's folder-size skew is not a
  guarantee it holds on every customer's data) and it is a DB-wide tuning knob, not a
  per-query fix, so its blast radius includes every other query touching those columns;
- Restructuring the query so the planner has a cheaper way to estimate cost — e.g. an explicit
  `OFFSET 0` fence, a `WITH ... AS MATERIALIZED` boundary (as direction (a)'s rejected rewrite
  already tried, which is precisely what regressed mid-size folders), or forcing a
  bitmap-heap-scan shape via query restructuring. This is the "not a schema change, still a
  meaningful query change" middle ground, and it's the part of (b) most worth prototyping
  first since it needs no infra/deployment change.

**Read on promise (non-binding)**: this is the direction most consistent with "don't rewrite
legacy subsystems wholesale" (Constitution Principle I), and has no schema or infra cost, but
none of its sub-options are proven for *this* planner-estimation problem specifically — they
are generic knobs whose effect on this exact query and this exact data-skew is unverified.
Statistics-target tuning is the cheapest thing to try next because it requires no code change
at all, only a migration-time `ALTER TABLE ... ANALYZE`, but "cheap to try" is not the same as
"verified to work," and this document does not verify it.

### (c) Special-case the largest folders with a distinct code path

**Idea**: detect that a folder is "large" and route it to a different query/strategy (e.g. the
folder-first CTE from the rejected blanket rewrite, applied only above a threshold).

**What "largest" would mean operationally**: the two folder-facts needed to route are (i) the
current `LIMIT`/offset being requested and (ii) the folder's child count, or some proxy for it.
Getting (ii) cheaply is the open question:

- **A `count(*)` pre-check** — `select count(*) from identifier where parent_path = ? and
  host_inode = ?` — is itself a scan over the same skewed data; on the 21,383-child folder this
  is not free, and it would run on **every** folder-listing request (small and large alike) to
  decide which path to take, meaning the fix's own detection cost is paid by the 99%+ of
  requests that don't need it. This needs to be measured, not assumed cheap, before being
  proposed as viable — it was not measured in this investigation.
- **A cached/precomputed count** (e.g. incrementally maintained folder child-count, refreshed
  on content create/delete/move) avoids the per-request cost but reintroduces a freshness
  problem: a stale count could route a folder to the wrong strategy right after it crosses the
  threshold, which is a correctness/perf edge case, not a crash, but is exactly the kind of
  silent behavior drift Constitution-adjacent review would flag.
  `contentlet_version_info`/`identifier` have no existing count-cache column to reuse for this.
- **A configured, per-instance threshold** (as comment 1 already flags) is the cheapest to
  implement but is explicitly called out in the issue as "a poor answer" because it has to be
  hand-tuned per dataset, and this dataset already shows the crossover point is
  `LIMIT`-dependent, not purely folder-size-dependent — the same folder can be on either side of
  the crossover depending on chunk size, so a folder-size threshold and a `LIMIT`-based
  crossover are not quite the same axis, and conflating them risks mis-routing.

**Read on promise (non-binding)**: cheapest to prototype and reason about, but its central
question — "can folder size be known cheaply enough to route on it" — was flagged as unproven
in comment 1 and remains unproven here; it is the direction most likely to trade one
unverified assumption (planner stability) for another (count-check cost), rather than
eliminating the uncertainty.

## Open Questions / Unknowns Per Direction

- **(a)**: Which `mod_date` (working vs. live, and per-language) would be denormalized onto
  `identifier`, and does that ambiguity already have a precedent elsewhere in the schema this
  spike didn't find? [NEEDS CLARIFICATION: does "the" content mod_date for an identifier need
  to be per-language, or is a single most-recent-across-languages value acceptable for sort
  purposes?]
- **(a)**: Is a synchronous write-path update to a new denormalized column acceptable
  performance-wise on the write side, given dotCMS's existing indexing pipeline is already
  asynchronous by design (per ADR-0018's description of index lag) — would adding a *synchronous*
  DB-side obligation on every content write be a net-new coupling this codebase has avoided
  elsewhere?
- **(b)**: Does dotCMS's supported Postgres deployment matrix permit adding `pg_hint_plan`, or
  is that ruled out by infra/ops policy before it's worth spending more spike time on it?
  [NEEDS CLARIFICATION: is a Postgres extension dependency (`pg_hint_plan`) acceptable for a
  self-hosted/customer-managed Postgres, or does dotCMS only control schema and query text, not
  installed extensions?]
- **(b)**: Would a statistics-target increase on `identifier.parent_path` (or `contentlet.mod_date`)
  hold across customer datasets with different folder-size skew, or is it tuned to this one
  test dataset's shape? Not measurable without access to varied production-scale datasets.
- **(c)**: What does a `count(*)`-based routing check actually cost, measured, across the full
  folder-size distribution (11,827 small + 1,062 mid + 101 large + 4 huge folders) — this
  spike could not measure it (no live instance) and it is the single biggest unknown blocking
  direction (c) from being taken seriously.
- **(c)**: If folder size is not the real axis (comment 1 shows the crossover is `LIMIT`-driven,
  not purely size-driven), should "largest folders" instead be framed as "requests whose
  effective `LIMIT` sits in the crossover band for that folder's actual row count" — which is
  circular unless the row count is already known?

## Coordination Risk — Item 2 Dependency *(mandatory)*

Item 2 (field-filter chunking, spec'd separately) calls this same `selectQuery(...)` /
`getContentByChunks(...)` machinery and, per issue comment 1 and the code at
`BrowserAPIImpl.java:245–352` (`getContentByChunks`, `getChunkFiltered`), loops over DB chunks
when field criteria route through the ES-narrowing path (`applyESFilter = true`,
`processESDirectly`, line 343). Item 2's spec treats this scan query as a black box invoked
some bounded number of times, and assumes item 1's change will not alter that count for the
**non-filtered, single-pass fast path** — i.e., a plain listing (no field/text filter) that
already satisfies `maxRows` from one chunk must keep executing the scan query exactly once.

Reading the loop in `getContentByChunks` (lines 262–309): the non-ES path already exits after
one chunk whenever `accumulatedContent.size() >= maxRows` after permission filtering (line
291), which is the common case for a plain listing. **Any direction chosen for item 1 must
preserve that single-iteration exit condition** — in particular:

- Direction (a) (schema change) changes what the query looks like but not the surrounding
  chunk-loop control flow, so it is inherently compatible if the rewritten query still returns
  a chunk-sized, correctly-ordered page in one call.
- Direction (c) (size-based routing) must ensure the "large folder" code path is *also* single
  execution per chunk for the non-filtered case — a routing strategy that, say, always fetches
  in smaller sub-chunks for large folders would silently break item 2's assumption even though
  item 2 never touches large-folder logic directly.
- Direction (b) (plan stabilization only) is the least likely to affect this at all, since it
  changes planner behavior, not the query's contract or the loop.

This is a coordination note, not a decision: whoever designs item 1's fix should confirm with
item 2's author (or spec) that the single-chunk-exit invariant is preserved before item 1
merges, and item 2 should not be implemented against item 1's *current* behavior if item 1 is
about to change.

## Latency Bimodality — Findings and Limits *(in scope per explicit decision)*

**Measured symptom** (comment 1, largest folder, 15 requests after 30 warm-up, same baseline
throughout): 11 of 15 requests land at ~165 ms, 4 of 15 spike to ~587 ms; the Site Browser
baseline for the same folder never leaves 68–85 ms. Scan-query execution count is 1.0× per
request in every sampled case — the spike is not caused by an extra loop iteration.

**This investigation has no live/profiled instance available**, so everything below is a
hypothesis generated by reading code, ranked by how much support or contradiction was found
for each, not a confirmed cause:

| hypothesis | supporting/contradicting evidence found by reading code | plausibility |
|---|---|---|
| Postgres plan-cache / generic-vs-custom-plan flip per statement | Every scan-query execution goes through `DotConnect` with a fresh parameterized statement (`buildPaginatedDotConnect`, line 364); Postgres's own planner re-plans on the 6th execution of a prepared statement, deciding generic vs. custom plan based on estimated cost variance — exactly the unstable-estimate scenario comment 1 already documents for `LIMIT`. If `DotConnect`/JDBC driver-level statement caching causes some fraction of requests to get a stale generic plan and others a fresh custom plan, that would produce intermittent flips without changing execution *count*. **This is the most plausible hypothesis found**, precisely because it is the same instability already proven to exist for this query, just triggered by plan caching instead of by `LIMIT` value. Not confirmed: this reading did not verify whether dotCMS's connection pool (`DBConnectionFactory`/`HikariCP`, not inspected in depth here) uses server-side prepared statements at all for `DotConnect` queries — if it doesn't, this hypothesis doesn't apply. |
| Connection-pool contention (waiting for a pooled connection) | Would show up as time before the query starts, not inside it; comment 1's framing (baseline stable, only Content Drive's request spikes) is consistent with per-query cost, not pool wait, since the baseline (Site Browser) presumably shares the same pool. Not ruled out, but nothing in the code reading specifically supports it over the alternatives. |
| GC pause during the request | Plausible for any JVM workload; nothing folder-query-specific found in code to support or exclude it. A 400+ms pause would need to be a Full GC or a large young-gen collection; whether the JVM/GC config used in the benchmark run is known was not stated in the issue. Cannot be assessed further from code alone. |
| OS/DB page-cache miss on a cold buffer for this folder's rows | Contradicted somewhat by "30 warm-up requests" already having been run before the 15 measured ones — if warm-up were enough to fully populate cache, a cold-buffer explanation is weaker, but warm-up requests may have touched a working set that later evicts (e.g. shared_buffers/OS cache pressure from other queries between warm-up and measurement), so this is not fully ruled out. | 
| Autovacuum/auto-analyze running concurrently on `contentlet`/`identifier` | No log or `pg_stat_activity` evidence available in this environment to check; large tables with active writes are plausible autovacuum targets, and an autovacuum worker holding a lock or consuming I/O during a request would produce exactly this kind of periodic, execution-count-invariant spike. Cannot be confirmed or excluded from code reading alone. |

**Recommendation for this specific question**: none of the above can be confirmed without a
live instance and query-level tracing (`pg_stat_statements` mean/stddev per call,
`track_io_timing`, and JVM GC logs correlated to the same 15 requests). The prepared-statement
plan-cache-flip hypothesis is worth checking first because it directly extends a mechanism
already proven for this exact query (`LIMIT`-driven plan instability), but that is a ranking of
plausibility, not a finding.

## Recommended Next Investigation Steps *(mandatory)*

In priority order, all requiring a live/profilable instance that this spike did not have:

1. **Confirm whether `DotConnect` queries in this path use server-side prepared statements**,
   and if so, capture `pg_stat_statements` per-call timing variance (not just mean) for the
   scan query on the largest folder across ≥50 requests, to test the plan-cache-flip
   hypothesis before investing in any of the three directions.
2. **Measure the `count(*)`-style folder-size probe's own cost** across the full folder-size
   distribution (not just the largest folder) — this single number determines whether
   direction (c) is viable at all.
3. **Prototype direction (b)'s statistics-target change** (`ALTER TABLE identifier ALTER COLUMN
   parent_path SET STATISTICS n; ANALYZE`) in isolation and re-run the exact `LIMIT` sweep from
   comment 1 (40/200/400/500/900/2000) on the same large folder, plus the mid-size folders that
   the CTE rewrite regressed, to see whether it moves the crossover without the CTE's
   side-effects. This is the cheapest experiment of the three directions and should run first.
4. **If (3) is insufficient, prototype direction (a)'s denormalized-`mod_date` design** far
   enough to answer the open per-language/per-version question above, before treating it as a
   scoped schema change.
5. **Re-run the item-2 single-chunk-exit check** (a plain listing on a large folder, count scan
   executions) against whichever direction is prototyped, to close the coordination risk before
   either item lands.
6. Whatever direction is eventually chosen, re-verify the two correctness findings (host
   filtering equivalence across call paths, and the missing `ORDER BY` tiebreaker) as part of
   its acceptance criteria — they are cross-cutting and apply regardless of which access-path
   direction wins.

## Assumptions

- The dataset and measurements in issue #37148 (comment 1, the corrective one) are accurate and
  reproducible; this spike did not re-run them.
- "Fix compatible with item 2" means preserving the single-chunk-exit behavior of
  `getContentByChunks` for the non-ES-filtered path specifically — item 2's own spec was not
  read in full as part of this task and may impose additional constraints beyond what is
  inferred here from the shared code.
- Site Browser's reliance on the same `selectQuery(...)` (per the issue's "Scope note") means
  any direction chosen must be re-verified against Site Browser's listings, not just Content
  Drive's — this spike treats that as a shared constraint on all three directions equally
  rather than re-deriving it per direction.
