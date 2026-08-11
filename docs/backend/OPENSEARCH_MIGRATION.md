# ES → OpenSearch Migration

## Executive Summary

Migrate dotCMS indexing infrastructure from ElasticSearch to OpenSearch **transparently**.
No downtime, no data loss, no visible behavior change for the end user.

---

## Migration Phases

Controlled via feature flag: `FEATURE_FLAG_OPEN_SEARCH_PHASE`

| Phase | Code Name                      | Writes   | Reads   | ES Status         |
|-------|--------------------------------|----------|---------|-------------------|
| 0     | PHASE_0_MIGRATION_NOT_STARTED  | ES only  | ES only | active            |
| 1     | PHASE_1_DUAL_WRITE_ES_READS    | ES + OS  | ES only | active            |
| 2     | PHASE_2_DUAL_WRITE_OS_READS    | ES + OS  | OS only | active (fallback) |
| 3     | PHASE_3_OPENSEARCH_ONLY        | OS only  | OS only | decommissioned    |

---

## Phase Transitions

Phases are advanced **manually** by changing the value of `FEATURE_FLAG_OPEN_SEARCH_PHASE`
in `dotmarketing-config.properties` (or the equivalent environment variable).
The system reads the flag on each routing decision, so the **routing** change (which providers receive
writes / serve reads) takes effect without a restart. All nodes in the cluster must be updated
consistently.

> **Startup-only phase setup requires a restart.** Routing is live, but the one-time setup for a phase
> runs only in `InitServlet` → `ContentletIndexAPIImpl.checkAndInitializeIndex()`: the
> `IndexStartupValidator` connectivity / version-3.x / endpoint-separation checks, the automatic
> migration shutdown (`haltMigration()`), and the bootstrap of the OS index pair + its `indicies` rows
> (`initOSCatchup()`, gated on Phase 1/2 requiring *both* providers ready in `indexReady()`). So
> **activating the migration (Phase 0 → 1) needs a restart** — otherwise dual-writes fan out to an OS
> index that has not been created yet (misses swallowed fire-and-forget) and validation never runs.
> Advancing 1 → 2 / 2 → 3 should also restart so the startup validation runs for the phase actually in
> effect (Phase 3 validation is fail-loud — no ES fallback). An automatic shutdown resets to Phase 0
> **in memory only**; persist the change in config and restart to make it stick.

| Transition      | Precondition                                                              |
|-----------------|---------------------------------------------------------------------------|
| Phase 0 → 1     | OS cluster reachable; OS index created via bootstrap or catchup           |
| Phase 1 → 2     | OS data quality validated; mapping schemas must be identical (see Rollback risk) |
| Phase 2 → 3     | OS confirmed stable under production read load; ES decommission approved  |

> There is no automated promotion. Advancing a phase is a deliberate operator action.

---

## Shadow Index Strategy

### Definition

The **shadow index** is the OpenSearch index during the dual-write phases (1 and 2).
It receives every write that goes to ES, acting as a continuously-updated replica.
It is called "shadow" because it follows ES writes rather than owning them.

This is a **transitional state**: once Phase 3 is reached, the shadow index is promoted
to the **primary index** — OS becomes the source of truth and ES is decommissioned.

### Phase lifecycle

| Phase | OS write role | OS read role    | Write failure    | Read failure          |
|-------|---------------|-----------------|------------------|-----------------------|
| 0     | absent        | absent          | —                | —                     |
| 1     | shadow        | absent (ES)     | fire-and-forget  | n/a                   |
| 2     | shadow        | **primary**     | fire-and-forget  | fallback to ES        |
| 3     | **primary**   | **primary**     | propagates       | propagates            |

### Fire-and-forget writes (Phases 1 and 2)

When OS is a shadow, a write failure must **never** affect the ES write or the caller:

- The OS failure is **logged** (warn-level) for observability
- The reindex queue entry is **not** marked as failed
- No `BulkProcessor` rebuild is triggered
- The caller receives a successful result based on ES

Rationale: ES remains the source of truth through Phases 1 and 2. An OS failure is a
consistency concern, not a data-loss event — ES still holds the authoritative state.

### Read fallback (Phase 2 only)

In Phase 2 OS serves reads but ES is still active. If OS throws an exception on a read,
`PhaseRouter` catches it, logs at **ERROR** level, and retries against ES automatically.

- The caller receives a correct result from ES
- The ERROR log makes the OS failure visible for operators
- In Phase 3 there is no fallback — ES is decommissioned and OS failures propagate normally

**Limitation**: the fallback only activates on exceptions. If OS has stale data from a
prior write failure (no exception, just wrong data), the fallback does not trigger — the
caller receives the stale OS data. This is the accepted residual risk of the shadow strategy.

### Implementation

The `shadow` flag on `CompositeBulkProcessor.Entry` carries these semantics.
`BulkProcessorListener.forShadowProvider()` creates a listener that enforces the
fire-and-forget contract: it logs failures but never calls `handleFailure()` on the
reindex queue and never triggers a rebuild.

```
Phase 0 / 3  →  isDualWrite = false  →  shadow = false  →  failures propagate
Phase 1 / 2  →  isDualWrite = true, ops == operationsOS  →  shadow = true  →  fire-and-forget
```

### Accepted limitation

Dual-write alone does not guarantee perfect sync between indices. Adopted strategy:
dual-write for ~2 weeks on low-volume customers → validate → activate Phase 2.

A full OS reindex is implemented — it fans out through the same dual-write journal — but a
concurrent full rebuild is operationally expensive at scale (up to 1000 sites × 100k content
pieces per customer); plan reindex windows accordingly.

---

## Index Architecture

### Two indices per cluster
- **Live index**: Published content only
- **Working index**: Superset of live — includes drafts and unpublished versions

### Index naming convention
```
{cluster_prefix}.{live|working}_{timestamp}

# Examples
cluster_08abc3567e.live_20260305193221
cluster_08abc3567e.working_20260305193221
```

Both index sets follow the same logical name pattern. The new index set (the one being introduced
by this migration) carries an explicit distinction tag — present at every layer, including the
service / API surface. The legacy index set has no tag. **Today the tag value is `.os`**, chosen
because the migration target happens to be OpenSearch; the value is centralized in `IndexTag` and
can change in the future without touching the rest of the architecture.

**Two distinct name layers** — keep them separate:

- **Service / API layer (logical name)**: the form callers pass around in business code. It carries
  the tag if it belongs to the new index set, no tag if it belongs to the legacy set. **No cluster
  prefix.** Examples: `working_20230101` (legacy) · `working_20260406.os` (new).
- **Persistence layer (physical name)**: the form actually sent to a cluster or written to the
  `indicies` DB table. **Adds the `cluster_<id>.` prefix** on top of the logical name. Examples:
  `cluster_08abc3.working_20230101` (legacy) · `cluster_08abc3.working_20260406.os` (new).

The cluster prefix is an infrastructure detail added at the persistence boundary by
`getNameWithClusterIDPrefix` (called from inside `toPhysicalName`). Service-layer code can — and
should — work with logical names; the prefix appears only when a name is about to hit a cluster
or a DB write.

| Layer                                | Legacy name                         | Tagged name (today `.os`)                       |
|--------------------------------------|-------------------------------------|-------------------------------------------------|
| Service / API (logical name)         | `working_20230101`                  | `working_20260406.os`                           |
| Row in the `indicies` DB table       | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406.os`            |
| Physical index in the cluster        | `cluster_08abc3.working_20230101`   | `cluster_08abc3.working_20260406.os`            |

The DB row and the cluster index are the **same string** (both physical names). `VersionedIndicesAPI`
load methods return the canonical physical form (with tag, with cluster prefix) — they do not
strip — so a name resolved from the store can be passed directly to any `OSIndexAPIImpl`,
`MappingOperationsOS`, or search call without further transformation.

#### Distinction tag as the canonical marker

The tag is the **explicit-distinction marker** on every physical name in the new index set. The
choice to make the distinction explicit (rather than relying on cluster separation alone) buys two
things:

1. **DB PK uniqueness** in the shared `indicies` table (`index_name` is the PK) — without an
   explicit marker, the legacy row and the new row for the same logical name would collide.
2. **Cluster name distinction** in single-cluster test profiles where `DOT_ES_ENDPOINTS ==
   OS_ENDPOINTS` (e.g. the `opensearch-upgrade` Maven profile). Without the tag, a Phase 1
   fan-out that writes the same logical name to both providers would hit `resource_already_exists`
   on the second write. With the tag, the new index coexists with the legacy index in the shared
   cluster.

**Tag value**: stored as a single constant in `IndexTag` (`IndexTag.OS.tag(name)` /
`IndexTag.OS.isTagged(name)` / `IndexTag.OS.strip(name)`). Changing the literal — for example
swapping `.os` for something else later — is a one-line change in the enum; no caller hardcodes
the suffix.

**Who applies the tag**:

- **`ContentletIndexOperationsOS.toPhysicalName(name)`** is the canonical entry point. It calls
  `getNameWithClusterIDPrefix` then `IndexTag.OS.tag()`. Idempotent via `IndexTag.OS.isTagged`.
- **`MappingOperationsOS.physicalName(index)`** applies the same transformation internally so
  that mapping operations resolve to the tagged name even when callers pass a logical name.
- **`VersionedIndicesAPIImpl.saveIndices`** applies `IndexTag.OS.tag()` idempotently before
  INSERT as a belt-and-suspenders guard — by the time `saveIndices` is reached, names are already
  tagged in the normal production flow.

**Who does NOT strip the tag**:

- `VersionedIndicesAPI` load methods (`loadIndices`, `loadDefaultVersionedIndices`, `loadAllIndices`,
  `loadNonVersionedIndices`) return the canonical tagged form. The cache holds the same.
- `OSIndexAPIImpl.getIndicesStats`, `listIndices`, `getClusterHealth` strip the `cluster_X.`
  prefix but **preserve the tag** in the returned keys. Callers comparing keys to logical names
  must apply `IndexTag.OS.tag()` to the comparand or strip the tag from the keys explicitly via
  `IndexTag.OS.strip()`.

**Idempotency contract**: every public method on the new-index-set layer accepts any of:

- Logical name without tag (`working_20240101`) — at the service layer, means "no specific provider,
  let the dispatch decide".
- Logical name with tag (`working_20240101.os`) — at the service layer, means "this belongs to the
  new index set".
- Physical name with cluster prefix, no tag (`cluster_X.working_20240101`) — already half-resolved.
- Canonical physical with both prefix and tag (`cluster_X.working_20240101.os`) — fully resolved.

All four flow to the cluster as the same canonical physical string after passing through
`toPhysicalName` / `physicalName`. Idempotency is guaranteed by the `isTagged` and `hasClusterPrefix`
guards inside those methods.

**Why suffix, not prefix**: the logical name (`cluster_{id}.{type}_{timestamp}`) is fully readable
without any leading marker. Stripping is `name.substring(0, len - tag.length())` and detection is
`name.endsWith(tag)`. Collision-free guarantee for the current `.os` value: logical names always
end in `_YYYYMMDDHHMMSS` (numeric) — they can never naturally end in `.os`. A future tag value
must preserve this property (no overlap with the logical-name grammar).

#### The tag is part of the name identity — never strip it on return

A tagged name like `working_20240101.os` **is** the canonical identity of that index — not a
decorated form of `working_20240101`. They are two different indices on two different providers,
and the base name (everything before the tag) matches the ES counterpart by construction, so the
tag is the *only* thing that distinguishes them. Any method that **returns or accepts an index
name** MUST preserve the tag end-to-end:

- **Read getters** — `ContentletIndexAPIImpl.getCurrentIndex()`, `getNewIndex()`, and
  `getActiveIndexName()` return the tagged name verbatim in Phases 2/3 (they strip only the
  `cluster_X.` prefix). A caller that receives `working_20240101.os` knows unambiguously it is
  the OS index; one that receives `working_20240101` is talking to ES. Stripping the tag here
  would erase the only discriminator and reintroduce the ES/OS collision in any `Set<String>` or
  `Map<String, ?>` keyed by index name.
- The same applies to provider methods (`getIndicesStats`, `listIndices`, `getClusterHealth`):
  returned keys keep the tag (as documented above).

**The one legitimate strip — deriving the embedded timestamp.** Some methods extract the
`_YYYYMMDDHHMMSS` value out of a name (`elapsedSinceIndexCreated`,
`VersionedIndicesAPIImpl.extractTimestamp`, the `indexSuffixOS` computation in `initOSCatchup`).
The timestamp parser cannot consume a trailing `.os`, so these MUST strip the tag **locally,
before parsing**: `IndexTag.strip(name).substring(name.lastIndexOf('_') + 1)`. This is *not* a
contradiction of the rule above — the strip is applied to a throwaway local used to parse a
number; the name that is returned or stored keeps its tag. Rule of thumb: **strip to parse a
value out of a name, never to hand a name back.**

#### Exception: Site Search uses a vendor-neutral logical handle

The rule above ("the tag is part of the identity") describes the **content-index** model, where the
ES working/live index and its OS counterpart are tracked as *distinct* indices (separate slots in
the `indicies` table) and the tag is their only discriminator. **Site Search deliberately uses the
opposite model** and is the one sanctioned exception.

A Site Search index is conceptually **one logical index mirrored across both engines**, not two
independent indices. The crawl produces a single bundle that is replicated; a single `putToIndex` /
`deleteFromIndex` / `search` call fans out to every write provider through the phase router. To make
that fan-out possible, the `SiteSearchAPI` surface is expressed in **logical (untagged) names** — a
vendor-neutral *handle* — and each engine adapter translates that handle to its own physical name at
the boundary:

- `ESSiteSearchAPI` uses the handle as-is (ES indices carry no `.os`).
- `OSSiteSearchAPI` applies `.os` internally via `physicalName()`
  (`getNameWithClusterIDPrefix(IndexTag.OS.tag(name))`) for **every** physical operation — create,
  mapping, put, get, delete, search, alias.

Consequences:

- `SiteSearchAPI.listIndices()` **strips** `.os` and **deduplicates** the ES/OS twin into a single
  logical row (issue #36672). This is why it — unlike the content-index provider methods described
  above — does *not* return tagged names. A caller cannot tell ES from OS from this list, and does
  not need to: it hands the logical name back to the API and the router/adapter re-targets per
  engine.
- If the handle carried the tag, the same operation could not fan out to ES (which has no `.os`);
  the OS adapters apply `.os` unconditionally and therefore **expect an untagged handle as input**.

**Load-bearing discipline (the recurring bug class).** Because `.os` is applied only at the OS
adapter, every Site Search index/alias operation MUST go through `SiteSearchAPI`, never through the
content-index router (`APILocator.getESIndexAPI()`) with a logical Site Search name. The content
router builds the OS physical name **without** `.os`, so in Phases 2/3 (OS reads) it queries a name
that does not exist and silently misses (lenient `ignoreUnavailable` → empty result, no error). This
exact leak broke alias resolution (`$sitesearch.search(alias,…)`, the portlet Alias column, the
crawl's incremental/full decision, deactivate-by-alias) and the index-stats join
(`getIndicesStats()` / `getClusterHealth()` key OS entries by `.os`, so a logical-name lookup found
nothing → blank Count/Shards/Replicas/Size/Health). Both were fixed by routing the callers through
`SiteSearchAPI.getAliasToIndexMap()` and by an `.os`-fallback lookup in the portlet (issue #36360).
The abstraction is correct but **enforced by convention** — a call site that reaches for the content
router with an untagged Site Search name reintroduces the bug.

**Display visibility.** The logical-name surface means the Site Search portlet shows untagged names
in every phase, whereas the content-index maintenance page reveals `.os` to the migration QA role
via `MigrationIndexVisibility`. Aligning Site Search's display with that role-gated policy — so QA
can preview `.os` (and the ES/OS twin as distinct rows) while normal users keep the clean logical
view — is the one place the two UIs should converge; it does **not** require changing the internal
handle model, only the display sink.

##### Two alias views: searching vs. managing (issue #36983)

`SiteSearchAPI` exposes the alias map twice, and picking the wrong one is a bug:

| Method | Resolves against | Use it for |
|---|---|---|
| `getAliasToIndexMap()` | the **read provider only** (ES in Phases 0/1, OS in Phases 2/3) | **searching** — resolve an alias against the engine that will actually serve the query |
| `getAliasToIndexMapAllEngines()` | the **same provider set as `listIndices()`** (union in Phases 1/2) | **managing / displaying** — portlet columns, index selectors, choosing an index to crawl |

The reason there are two: **`listIndices()` is a union of both engines in the dual-write phases,
while alias resolution is single-engine.** Any index that lives only on the engine the current phase
does *not* read from therefore appears in the list with a blank alias. Two mirror-image symptoms of
the same defect:

- **Phase 2 + an index created in Phase 0** (Elasticsearch only) — reads come from OpenSearch, alias
  invisible.
- **Phase 1 + an index created in Phase 3** (OpenSearch only; typical after a downgrade 3 → 2 → 1) —
  reads come from Elasticsearch, alias invisible.

`getAliasToIndexMapAllEngines()` merges over the write providers and applies the **read provider
last**, so on a mirror desync (one alias resolving to different indices per engine) the management
view agrees with what a search would hit. In the single-provider phases (0 and 3) there is nothing to
merge and the idle engine is not consulted.

Callers on the management side: `site_search_index_stats.jsp` (Indices tab), `site_search_job_schedule.jsp`
(crawl index selector), `test_site_search.jsp` (Search tab selector) and `SiteSearchJobImpl` (the
crawl's alias resolution — an alias invisible there makes the crawl treat an existing index as new
and drop its alias). Everything on the search path keeps the single-engine method.

**A phase change never builds counterparts retroactively.** An index created in a single-provider
phase exists on that engine only until a crawl runs in a dual-write phase. Downgrading past that
point leaves it listed but unsearchable (its content lives on the engine that no longer serves
reads) — visible in the readiness report as `MISSING_COUNTERPART`; the fix is always a re-crawl.

#### Site Search mirror reconciliation (write path) — self-heal on crawl

The logical-handle model above makes *reads* correct, but a Site Search index can still end up
physically **out of sync** between engines: the ES index and its OS twin may hold different content,
or the twin may be missing entirely. This happens through paths that skip the happy path (a full
crawl in a dual-write phase, which creates both twins and re-points the alias on both):

- **Forward-only phase change** — moving Phase 0→1/2 does not retroactively build OS twins of
  indices that already existed in ES.
- **Phase-0 crawl** builds an ES-only index; its OS twin never existed.
- **Incremental crawl** writes documents *in place* and never calls `createSiteSearchIndex`. Two ways
  it desyncs: (a) if the OS twin is **missing**, the raw `putToIndex` lets OpenSearch **auto-create**
  it with a *dynamic* mapping (`keyword`→`text`, breaking aggregations) holding only the incremental
  delta — a partial, wrongly-mapped twin; (b) if the twins already **drifted** (both exist, different
  content — see the next mode), an incremental only layers the new delta on both and never reconciles
  the pre-existing difference.
- **Fire-and-forget shadow create/write** — a failed OS `createSiteSearchIndex` *or* `putToIndex` in a
  dual-write phase is swallowed at `WARN` (shadow policy). This is the main source of **content
  drift**: the OS twin exists but silently holds fewer documents than ES.
- **Phase-scoped delete** — a delete that fans out only to the current phase's write providers leaves
  a twin on the *other* engine as an orphan after a rollback.

**The fix is self-heal on crawl, not a big-bang rebuild on phase change** (the latter is expensive and
fragile). Two mechanisms (issue #36360):

1. **Incremental-crawl gate — existence *and* document-count parity.**
   `SiteSearchAPI.existsOnAllWriteEngines(name)` reports whether the index exists on *every* current
   write engine; `writeMirrorsInSync(name)` adds a **document-count comparison** across those engines
   (each leaf reports its own physical index's count — ES the plain name, OS the `.os` twin — via
   `SiteSearchAPI.documentCount`). That count is an **exact** total (a dedicated `_count` request on ES,
   a `size:0` match-all with `track_total_hits:true` on OS) — *not* a plain search total, which the ES
   7.x / OpenSearch clients cap at 10,000, so two large mirrors that had genuinely drifted (e.g. 15,000
   vs 12,000) would both read `10000` and wrongly compare equal. A count that fails on any engine
   (`-1`) is treated as **out of sync** (fail-safe rebuild), so a both-failed `0 == 0` is never mistaken
   for "in sync". `SiteSearchJobImpl` gates the incremental crawl on `writeMirrorsInSync`: a missing twin
   **or** a count mismatch demotes the crawl to a **full rebuild**, which recreates identical copies
   (correct mapping) on every engine and re-points the alias. This heals both desync kinds — the
   missing/partial twin *and* content drift from a swallowed shadow write — on the next crawl, and the
   dynamic-mapping auto-create path can no longer be reached through the gated crawl. (Count parity is
   a sound in-sync test here because Site Search is single-writer with immediate refresh and no
   concurrent crawl on the same index, so the copies are quiescent at crawl-planning time.)
2. **Delete sweeps both engines.** `SiteSearchAPIImpl.deleteIndex` deletes on the primary (current
   read provider) authoritatively and best-effort on the *other* engine, so a rollback leftover is
   swept even in a single-provider phase (0/3). The other engine is tolerant: an unreachable or
   decommissioned engine logs a `WARN` and never fails the delete.

**Residual limitation — the no-crawl window.** Because reconciliation is *on crawl*, the gap is only
closed when a crawl actually runs in a dual-write phase (1 or 2). In the window before that:

- In **Phase 2** a *missing* OS twin is caught by the read fallback (OS errors → read from ES), so
  reads stay correct; a *partial-on-create* twin can no longer be created (the gate rebuilds instead),
  and *content drift* is healed on the next crawl by the count-parity gate — but a drifted twin read
  **before** that next crawl still returns incomplete results silently (OS answers with no error, so
  no fallback).
- **Phase 3 is the cliff:** ES is decommissioned, so there is no fallback. Reaching Phase 3 with a
  twin that was never rebuilt, and never crawling, is a hard gap.

**Operational rule (pairs with the code):** before promoting the phase — especially into Phase 3 —
ensure every Site Search index has been crawled at least once so its OS counterpart exists and is in
sync. The migration-readiness endpoint below is what tells the operator *which* indices still need
that crawl, before they change the phase.

#### Migration-readiness endpoint (pre-phase-change advisory)

`GET /api/v1/index/migration/readiness` is an internal, read-only report a support technician runs
**before changing the migration phase** to see whether it is safe and, if not, what to do. It never
mutates anything — the fix is always the operator re-running the crawl / reindex, which self-heals
through the write-path gate above.

- **Not public.** The resource is `@Hidden` (absent from the OpenAPI / API-playground schema) and
  gated to CMS administrators who **also** hold the migration support role
  (`OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY`, default `os_migration_qa`) — a plain admin without the
  role is not enough. Anyone else gets a 403, so regular users never learn a migration is running.
- **What it reports.** The current phase with its read/write engines and a `dualWrite` flag; an
  overall verdict — `safeToAdvance` (toward OpenSearch-only) and `safeToRollback` (downgrade) with an
  `outOfSyncCount`, a human `summary`, and per-index `blockers`; and the per-index ES↔OS mirror diff
  for **both** mirrored families — the versioned content indices (`working`/`live`) and the Site
  Search indices. `content` is a keyed object by slot (`WORKING` / `LIVE` — a fixed pair);
  `siteSearch` is a list (an open set). Each entry carries `{indexName, es:{exists,docCount,physicalName},
  os:{exists,docCount,physicalName}, driftPercent, verdict, recommendation}` — `physicalName` is the
  full name as stored on each server (cluster-prefixed; `.os`-tagged on OpenSearch), and
  `driftPercent` is the signed % the OpenSearch (mirror) count deviates from the Elasticsearch
  (original) — negative = behind, positive = ahead, `null` when a count is unknown — with verdict `IN_SYNC` /
  `MISSING_COUNTERPART` / `COUNT_DRIFT`. The top level also carries the `clusterId` embedded in every
  physical name. The response is the model itself (no `ResponseEntityView` envelope).
- **Site Search entries also carry the `alias`, per engine.** `es.alias` / `os.alias` hold the alias
  that engine has attached to the index (omitted when there is none; never present on content rows,
  which are addressed by name only). Per engine on purpose: an index can hold its alias on one side
  and not the other — e.g. created before dual-write started, counterpart built later — and that
  asymmetry is what the operator needs to see. It is what makes the report usable at all, since a
  site-search index is known by its alias, never by its `sitesearch_<timestamp>_<uuid>` name. One
  alias lookup per engine covers the whole set, not one per index. When an alias is itself shaped
  like an index name, `recommendation` appends a NOTE: that is the fingerprint of the crawl overwrite
  fixed in issue #36983 — the fix stops new occurrences but cannot restore an alias already lost, so
  this is the only way to find the indices that still need theirs restored. It never changes
  `verdict`: the verdict measures data integrity (existence + counts), while a damaged alias costs no
  data and must not block a phase change.
- **Stateless, from live counts.** Every field is derived at request time. Counts are **exact and
  current**: both halves issue a real count query per index — Site Search through
  `SiteSearchAPI.documentCount`, the content half through
  `ContentletIndexOperations.getIndexDocumentCount`. Neither uses a search hit total (which the ES/OS
  clients cap at 10,000 and would hide drift on large indices). Both reconcilers query the two engine
  leaves directly, not the phase-aware router, so the report shows both sides in every phase.
- **Why the count is a query and not `_stats` `docs.count`.** The content half still calls
  `getIndicesStats()` — but only to decide **existence**, one call per engine covering the whole index
  set, so both slots are settled from a single snapshot. The count itself must not come from there:
  `docs.count` is a per-shard counter that only advances when the shard refreshes, so it trails a
  just-written document by seconds. During that window the document is already searchable while the
  report still shows the previous number — and a support technician checking whether a publish reached
  OpenSearch reads that as a **lost write**. This endpoint is the source of truth for exactly that
  question, so it must never report a number the engine can already contradict (issue #36983).
- **What a count still cannot tell you.** A number that does not move is not proof that nothing was
  written: the document id is `identifier_languageId_variant`, so re-publishing content already present
  in that index is an **update**, and the total stays put. And in a dual-write phase the OpenSearch copy
  only ever receives what changes *from that point on* — a mirror sitting at 15 of 683 documents is the
  expected state until a full reindex, so a `+1` there is easy to misread as "nothing happened". To
  settle it for one specific write, ask for the **document**:

  ```bash
  curl -s "http://<os-host>:9200/<clusterPrefix><index>.os/_doc/<identifier>_<languageId>_DEFAULT"
  # "found": true with the modDate of your edit ⇒ the dual-write landed
  ```
- **`safeToRollback` needs no history.** A downgrade routes reads back to Elasticsearch, so it is
  unsafe when any index's ES copy is behind its OpenSearch counterpart (`esDocCount < osDocCount`, or
  the ES copy missing) — that delta, typically content written while OpenSearch served reads, would be
  silently absent after the downgrade until a full reindex. That is derivable from the same snapshot,
  so no per-phase state is persisted. An **unmeasurable** count on either engine (reported as `-1`)
  also makes it unsafe: it is never compared numerically, because `100 < -1` would otherwise read as
  a green while OpenSearch may hold more documents.
- **`outOfSyncCount` is phase-aware.** In Phase 0 the OpenSearch counterparts have not been built yet
  — they are created during dual-write — so a missing OpenSearch copy is the expected state and is not
  counted; otherwise the count would contradict the "nothing to reconcile yet" summary next to it. Any
  *other* mismatch (an OpenSearch index with no ES source, a drift between two existing copies) is
  still unexpected in Phase 0 and stays counted and named in the summary.

Because this endpoint is the source of truth for migration/QA, the index portlets no longer reveal
`.os` indices by role: `MigrationIndexVisibility` is now purely phase-based (hidden in Phases 0/1/2,
shown in Phase 3, for everyone). The role key is retained only to gate this endpoint.

##### How to read the readiness report

**Access — both conditions, or 403.** The caller must be a **CMS administrator** *and* hold the
migration support role. The role key comes from `OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` (default
`os_migration_qa`); the check is `MigrationReadinessResource.isMigrationSupportUser`. A plain admin
without the role gets a 403, and so does a role holder who is not an admin — deliberate, so a regular
user never learns a migration is running. The endpoint is `@Hidden`, so it is absent from
`openapi.yaml` and from the API playground: it will not show up by browsing, only by knowing the URL.

```bash
# Backend session or basic auth; both the admin role and the support role are required.
curl -u admin@dotcms.com:admin http://localhost:8080/api/v1/index/migration/readiness | jq
```

If it returns 403, grant the `os_migration_qa` role to the admin user (Roles & Permissions), or point
`OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` at a role they already hold. There is no envelope: the JSON
**is** the report.

**Read it top-down, in this order:**

1. **`phase`** — `current`/`name`, plus `readEngine`, `writeEngines` and `dualWrite`. Everything below
   is relative to this: which engine answers searches *right now*, and which ones receive writes.
2. **`verdict.safeToAdvance` / `verdict.safeToRollback`** — the go/no-go pair. They answer different
   questions and are not opposites: *advance* is blocked when the OpenSearch mirror is behind
   (promoting would lose data on the OpenSearch-only phase); *rollback* is blocked when OpenSearch is
   **ahead** (downgrading would hide the delta until a reindex). Both can be `false` at once.
3. **`verdict.summary` + `verdict.blockers`** — the sentence to paste into a ticket, then the per-index
   list of what to fix. An empty `blockers` with `safeToAdvance: false` cannot happen; if `blockers` is
   non-empty, each entry names the index and the action.
4. **`content` (keyed `WORKING`/`LIVE`) and `siteSearch` (list)** — the evidence behind the verdict.

**Per-index row.** `es` and `os` each carry `{exists, docCount, physicalName}` — plus `alias` on Site
Search rows. Then:

| Field | How to read it |
|---|---|
| `verdict` | `IN_SYNC` · `MISSING_COUNTERPART` (one engine lacks the index) · `COUNT_DRIFT` (both hold it, different counts) |
| `driftPercent` | `(OS − ES) / ES × 100`, rounded to 2 decimals. `0.0` in sync · negative = mirror **behind** (blocks *advance*) · positive = mirror **ahead** (blocks *rollback*) · `-100.0` mirror empty/absent · `+100.0` the original is empty but the mirror holds data · `null` a count could not be measured |
| `docCount: -1` | The count could **not** be measured. Never read it as "zero" — the verdict treats it as out of sync on purpose |
| `physicalName` | The exact name on that server (cluster-prefixed; `.os`-tagged on OpenSearch) — copy/paste it into `_cat/indices` to verify by hand |
| `recommendation` | The concrete action (re-crawl / reindex). A trailing `NOTE:` flags an alias that is really an index name (see above) |

**Worked example — the downgrade case.** After going 3 → 2 → 1, a Site Search index created by a
crawl while in Phase 3 exists **only** on OpenSearch:

```json
{ "indexName": "sitesearch_20260811155758_6c1f7101-…",
  "es": { "exists": false, "docCount": 0,   "physicalName": "cluster_x.sitesearch_20260811155758_6c1f7101-…" },
  "os": { "exists": true,  "docCount": 412, "physicalName": "cluster_x.sitesearch_20260811155758_6c1f7101-….os",
          "alias": "sitesearch-ph-3" },
  "driftPercent": 100.0, "verdict": "MISSING_COUNTERPART" }
```

Read as: the index and its alias are intact on OpenSearch, but in Phase 1 reads come from
Elasticsearch, where it does not exist — so **its content is unsearchable until it is re-crawled**,
and `safeToRollback` is `false` because OpenSearch holds documents Elasticsearch does not. A phase
change never builds counterparts retroactively; only a crawl (or reindex, for content) does.

Note this is exactly the information the *portlet* could not show before issue #36983: the index list
is a union of both engines while alias resolution was single-engine, so that row rendered with a blank
Alias. The endpoint never had that blind spot — it queries both engine leaves directly, in every
phase — which is why it stays the source of truth even when a portlet column looks empty.

##### Worked example — activating a pre-migration backup content index

dotCMS lets an administrator activate an **old inactive index** (Maintenance → Index → *Make Default*,
or `PUT /api/es/activateindex/…`) to roll back to a previous reindex. If that index **predates the
migration**, it never went through the OpenSearch create fan-out, so it has **no OpenSearch
counterpart** — and activation does not build one.

**What the code actually does.** `ContentletIndexAPIImpl.activateIndex` repoints *both* stores by pure
name transformation: the OpenSearch pointer is set to `operationsOS.toPhysicalName(name)` =
`<cluster>.<name>.os`, with **no `indexExists` check, no create and no guard** (delete has
`assertIndexNotActive`; activate has no equivalent). The OpenSearch store now names an index that has
never existed. In Phases 1/2 the shadow writes to it are best-effort and swallowed, so nothing
complains.

**Why it is dangerous rather than merely wrong:**

| Phase | What you see |
|---|---|
| 1 | Nothing. Silent divergence — writes to the OpenSearch counterpart go nowhere |
| 2 | Still works: the Phase-2 read fallback drops back to Elasticsearch, but logs an `ERROR` per read — the early-warning signal |
| 3 | No fallback exists. The OpenSearch pointer names an index that was never created → empty results or an exception, which reads to the customer as **lost content** |

**What the readiness endpoint says — and when it can say it.** Once the backup is activated it *is*
the `WORKING`/`LIVE` pointer, so the very next call reports it:

```json
"content": {
  "WORKING": {
    "indexName": "working_20251114093012",
    "es": { "exists": true,  "docCount": 148230, "physicalName": "cluster_x.working_20251114093012" },
    "os": { "exists": false, "docCount": 0,      "physicalName": "cluster_x.working_20251114093012.os" },
    "driftPercent": -100.0,
    "verdict": "MISSING_COUNTERPART",
    "recommendation": "The OpenSearch copy of content index 'working_20251114093012' is missing. Run a full reindex to rebuild it before promoting to the OpenSearch-only phase."
  }
}
```

In Phases 1/2 this also flips `verdict.safeToAdvance` to `false` and names the index in
`verdict.blockers` — the promotion gate does its job. **The fix is a full reindex**: that is the only
path that fans out through the router and materializes the OpenSearch copy (a phase change never
does, and neither does activation).

**Three traps worth knowing before relying on this:**

1. **You cannot pre-check a backup.** The content half of the report covers only the *active*
   working/live pair, so a divergent backup is invisible while it sits inactive. Sequence: activate →
   call readiness → reindex if it reports `MISSING_COUNTERPART` → only then change phase.
2. **In Phase 3 the verdict does not protect you.** `safeToAdvance` is forced `true` there (there is no
   phase beyond 3), so a backup activated *while already in Phase 3* still reads green at the top
   level. Read the per-index rows and `outOfSyncCount`, never the boolean alone — and note this is
   precisely the phase where the failure is immediate and customer-visible.
3. **The endpoint reports, it never repairs.** It will not block the activation, and re-running it
   changes nothing on its own.

The durable fix — reconcile-on-activate, rebuilding the counterpart asynchronously through the
existing reindex machinery (a synchronous copy of a large index is not viable, and a naive
point-in-time copy would lose concurrent writes) — is **not implemented**. Until it is, the operational
rule stands: after activating any pre-migration index, run a full reindex before touching the phase.

#### Tag manipulation is the sole responsibility of `IndexTag`

All read/write of the vendor marker on an index name MUST go through the `IndexTag` enum.
`IndexTag` is the only place that knows the literal value (`.os`), whether it is a prefix or a
suffix, and the idempotency rules around it. Any code outside `IndexTag` that handles the marker
directly — even a thin wrapper or a one-liner — is a bug.

| Operation             | ✅ Correct                                                | ❌ Incorrect                                                       |
|-----------------------|----------------------------------------------------------|--------------------------------------------------------------------|
| Apply the marker      | `IndexTag.OS.tag(name)`                                  | `name + ".os"`                                                     |
| Strip the marker      | `IndexTag.OS.untag(name)` / `IndexTag.strip(name)`       | `name.substring(0, name.length() - 3)` / `name.replace(".os","")` |
| Detect the marker     | `IndexTag.OS.isTagged(name)`                             | `name.endsWith(".os")`                                             |
| Identify the vendor   | `IndexTag.resolve(name)` / `IndexTag.vendorOf(name)`     | `name.contains(".os") ? OS : ES`                                   |

This rule applies at every layer — providers, routers, mapping helpers, factories, integration
tests, and debug utilities. A helper method in another class that re-implements `tag()` / `strip()`
/ `isTagged()` (even faithfully) is still a violation: the moment the literal or the
prefix/suffix position changes in `IndexTag`, that helper becomes silently wrong. There are no
"performance shortcuts" or "convenience wrappers" worth the divergence risk.

**Why this is strict, not advisory**: the tag is a load-bearing migration artifact. The codebase
will eventually want to either change its value (e.g. swap `.os` for something neutral) or remove
it entirely (see "Future: tag retirement after ES decommission" below). Both are one-line edits
inside `IndexTag` — but only if no other class has taken on the responsibility. Every direct
string operation on the marker outside `IndexTag` is a hidden coupling that turns a one-line
change into a codebase-wide search-and-replace.

#### Future: tag retirement after ES decommission

The tag exists to solve concrete problems that only apply while both index sets coexist (PK
uniqueness in the shared `indicies` table, cluster name distinction in single-cluster test
profiles, routing between two providers). Once ES is decommissioned (Phase 3 complete, no
rollback window) these problems disappear and the tag becomes purely cosmetic.

Retiring the tag is **not free** — there are three viable strategies, none zero-cost:

1. **Leave it in place.** Treat `.os` as a historical marker on all existing index names. Zero
   ongoing cost; mildly confusing for readers without context.
2. **One-time rename migration.** For each OS index, reindex into a non-tagged name (OpenSearch
   has no direct rename — the standard path is reindex + alias swap + delete). Update the
   corresponding `indicies` rows. Comparable in scope and cost to a full OS reindex.
3. **Configure the tag to `""` in `IndexTag.OS`.** New writes stop adding the tag; existing
   indices keep theirs. The codebase must tolerate both forms indefinitely or until strategy 2
   is executed — usually the worst of both worlds.

The architecture is set up so that strategy 1 requires no action and strategy 3 is a one-line
change in `IndexTag`. Strategy 2 is a planned future migration tracked separately when the ES
decommission plan is finalized.

**Implication for present-day work**: do not hardcode the literal `.os` anywhere except in
`IndexTag.OS`. Comparisons must go through `IndexTag.OS.isTagged()` and `IndexTag.OS.strip()`,
so a future change of the literal — or its removal — does not require touching call sites.

### What lives in the index
| Content                    | Live | Working |
|----------------------------|------|---------|
| Published content          | ✅   | ✅      |
| Draft/unpublished content  | ❌   | ✅      |
| Content types              | ✅   | ✅      |
| Permission representations | ✅   | ✅      |

### Document structure
- Core fields: `identifier`, `inode`, `basetype`, `contenttype`, `languageid`, `variant`
- Status fields: `live`, `working`, `deleted`, `locked`
- Permission fields: `permissions` (whitespace-analyzed text)
- Workflow fields: `wfstep`, `wfscheme`, `wfcurrentstepname`
- Dynamic fields: `*_dotraw` (keyword), `*latlon` (geo), `*_text` (text)
- Content-type specific fields: nested under type name (e.g. `htmlpageasset.url`)

---

## Write Pipeline

### How writes work
- All writes go through a **queue-based system** backed by `dist_reindex_journal` table
- A worker picks up a unit of work and writes/deletes from the index
- **Writes always happen after a DB commit** to guarantee DB ↔ index consistency

### Write triggers
| Trigger                    | Live            | Working         |
|----------------------------|-----------------|-----------------|
| Content published          | ✅ write        | ✅ write        |
| Content saved (draft)      | ❌              | ✅ write        |
| Content deleted            | ✅ delete       | ✅ delete       |
| Content-type created       | ✅ write        | ✅ write        |
| Content-type deleted       | ✅ delete all   | ✅ delete all   |
| Permissions modified       | ✅ update       | ✅ update       |
| Full reindex triggered     | rebuilds index  | rebuilds index  |

### Full reindex behavior
- Creates a new index copy in the background
- Inserts all rows from `contentlet_version_info` into `dist_reindex_journal`
- Keeps the current index live during the process
- **From Phase 1 on, the reindex fans out to OS too**: `initAndPointReindex` creates the OS
  `reindex_working`/`reindex_live` slots (router fan-out), the journal worker dual-writes every
  reindexed document to them (`DualIndexBulkRequest`), and the switchover is phase-aware
  (`fullReindexSwitchover` mirrors the promotion to the OS store; Phase 3 delegates to
  `fullReindexSwitchoverOS`). Only in **Phase 0** does a reindex rebuild ES alone — there is no
  shadow index yet. See [Operations to Replicate in Shadow Index](#operations-to-replicate-in-shadow-index).

---

## Operations to Replicate in Shadow Index

| Operation                            | Replicate to OS? | Notes                                                        |
|--------------------------------------|------------------|--------------------------------------------------------------|
| Content write (publish/save)         | ✅ Yes            |                                                              |
| Content delete                       | ✅ Yes            |                                                              |
| Content-type create                  | ✅ Yes            |                                                              |
| Content-type delete + content cleanup| ✅ Yes            |                                                              |
| Permission update                    | ✅ Yes            |                                                              |
| User-triggered index lifecycle (delete / clear / open / close / replicas) | ✅ Yes | Transparent-mirror principle — the operator sees one index; the action applies to both engines |
| User-triggered reindex               | ✅ Yes (Phase 1+) | Fans out to OS: creates OS reindex slots, dual-writes docs, and switches over phase-aware. Phase 0 rebuilds ES alone (no shadow yet). A full OS reindex on a very large customer is operationally expensive — a caveat, not a code exclusion |
| Site Search index operations         | ✅ Yes (deferred) | In scope, lower priority than core content index            |

---

## Guiding principle: the OS index is a transparent mirror

The operator must never need to know a migration is underway. They keep operating as if a single
Elasticsearch index exists; **every user-triggered index operation is applied faithfully to the full
mirror (ES + its OS twin)**, and the operator owns the outcome exactly as they would in a
single-index cluster.

- Migrated behavior must be **identical** to single-index behavior. If closing "the index" stops
  search in a single-ES cluster, it stops search here too — that is expected, not a bug to guard
  against.
- User-triggered lifecycle ops (**delete / clear / open / close / updateReplicas**) therefore
  **cascade to both engines**. Each op resolves the per-engine physical name (ES → bare,
  OS → `.os`) so it targets the real index on each side. *(This supersedes the earlier stance that
  lifecycle ops were "not user-controllable" on the shadow.)*
- **Full reindex is no mirror exception**: from Phase 1 on it also rebuilds and switches over the
  OS twin (create OS reindex slots → dual-write docs → phase-aware switchover). The old "not viable
  at OS scale" note survives only as an *operational* caveat (a concurrent full OS rebuild is
  expensive on very large customers), not as a code-level exclusion.
- Safety guards (e.g. the active-index delete guard, which blocks deleting the live/working index)
  exist to **reproduce** the single-index UX, not to protect OS from the operator.

### Index pointer ops (`activateIndex` / `deactivateIndex`) are name-driven, not existence-checked

`activateIndex` and `deactivateIndex` update the **pointer stores** (`indicies` for ES,
`VersionedIndices` for OS) by index **name** — they deliberately do **not** verify that the named
index exists in the cluster. This is load-bearing for the mirror model: during **migration catchup**
the ES and OS names diverge (see "Index name divergence between providers") and the OS physical index
may not be built yet, so activation mirrors the pointer **optimistically** and lets catchup fill in
the OS side. The contract is asserted in `ContentletIndexAPIImplMigrationIntegrationTest` — *"the OS
DB pointer reflects the name passed in, regardless of which index the OS cluster actually holds"* and
*"deactivateIndex never validates cluster existence … a pointer-store update driven by the name
pattern, not by cluster state"*.

**Do not add a hard existence guard to `activateIndex`.** The failure it would target is real:
reactivating an **old, pre-migration backup index** (kept as a rollback point) that never received an
OS copy leaves OS pointing at a non-existent index → in Phase 3 (no ES fallback) search returns empty
= "content lost". But at activation time the dangerous case is **indistinguishable** from the
legitimate one:

- ✅ the OS copy will exist in a moment — normal dual-write **catchup** (the index is being built), vs.
- ❌ the OS copy will never exist — an old backup that nothing is migrating.

Both are "no OS copy right now". A point-in-time `indexExists` check cannot tell "not built **yet**"
from "**never** built", so a hard guard blocks the legitimate catchup too and breaks normal migration
operation. This was tried and reverted (PR #36880): the guard failed 6 integration tests in the
OpenSearch Upgrade Suite that assert exactly this catchup behavior — the tests are the design
contract, not stale coverage.

**The intended mitigation is the migration-readiness endpoint, not a guard on activate.** An active
index with no OS counterpart is a *state to report*, not a per-operation precondition to enforce. The
readiness endpoint (see "Migration-readiness endpoint (pre-phase-change advisory)") detects a
`MISSING_COUNTERPART` for the active working/live and marks the phase **not safe to advance** —
gating the Phase-3 promotion, which is where the real damage would occur. Reactivating an un-mirrored
backup is recovered the normal way: turn the migration off (set the phase to 0) to roll back freely,
or run a full reindex to rebuild the OS side.

## Design Rules

- **Never modify ES code** when adding the OS counterpart — zero changes to existing ES classes
- **OS write failures are fire-and-forget** in Phases 1 and 2 — a shadow write failure must never affect the business operation or the ES write
- **OS read failures fall back to ES** in Phase 2 — `PhaseRouter` catches the exception, logs at ERROR, and retries against ES
- **Routing lives in one place**: `IndexAPIImpl`, `ContentletIndexAPIImpl`, and their search equivalents — never dispersed across callers
- **ES is the authoritative write store** during Phases 1 and 2 — OS is the read source in Phase 2 but ES holds the canonical state for recovery

---

## Code Architecture

### Naming conventions
- ES classes keep their original names
- OS counterpart classes use the `OS` suffix — e.g. `ContentSearchRepositoryOS`
- Shared interfaces/abstractions must not carry `ES` or `OS` in their name
- OpenSearch-specific classes must be placed under `com.dotcms.content.index.opensearch`
- General purpose index classes must be placed under `com.dotcms.content.index`

### `@IndexRouter` annotation
Marks the class where the routing decision lives — which index receives a given request.
There must be **one and only one** routing point per functional area. Never duplicate this logic in callers.

---

## Index Operation Dispatch Model

Two semantically distinct operation types govern how index names determine routing. The dispatch
type determines whether the migration phase or a per-call vendor selector decides which provider
handles the operation — getting this wrong leads to writes landing in the wrong provider or
failing entirely.

### Phase-dispatched operations ("broadcast")

> **The migration phase decides which providers receive the call. The index name is a payload, not a routing key.**

Used for coordinated schema and data operations that must keep all active providers in sync:
- `putMapping` — mapping schema changes
- `addContentlet`, `removeContentlet` — content writes/deletes
- `createContentIndex` — index creation during bootstrap or reindex

**Rule**: pass an **untagged name** — logical (`working_20240101`) or cluster-prefixed
(`cluster_X.working_20240101`). Do **not** pass a tagged name (`working_20240101.os` or
`cluster_X.working_20240101.os`) into a fan-out — the presence of the tag is the signal that the
caller already knows which provider owns the name; route via tag-dispatch instead. See "Why
tagged names don't fan out" below.

`PhaseRouter.write()` / `router.writeChecked()` fan out to all write providers for the current
phase. The router class (`ContentletIndexAPIImpl`, `IndexAPIImpl`, …) calls
`provider.toPhysicalName(name)` on each provider before dispatch, so:

- Legacy provider receives `cluster_X.working_20240101` (no tag — its `toPhysicalName` only adds
  the cluster prefix).
- Tagged provider receives `cluster_X.working_20240101.os` (cluster prefix + tag, applied by
  `ContentletIndexOperationsOS.toPhysicalName`).

The name passed BY the caller is a payload — same string for both providers — and each provider's
own `toPhysicalName` localises it to the form that provider's cluster actually holds.

```java
// Correct — name is a payload; each provider's toPhysicalName builds its own form
router.write(impl -> impl.putMapping(impl.toPhysicalName(indexName), mapping));
```

#### Why tagged names don't fan out

A tagged service-layer name (`working_20240101.os` or its cluster-prefixed form) carries semantic
intent: the caller is referring to **a specific index in the new index set**. That index has no
automatic equivalent in the legacy index set because:

- In **fresh install**, both sets share the same logical name minus the tag — so the equivalence
  exists, but stripping the tag to fan out is only a coincidence of that scenario.
- In **migration catchup** (the majority of production customers), the legacy and new sets hold
  indices with **different timestamps** entirely — `working_20230101` (legacy) and
  `working_20260406.os` (new). There is no string transformation that maps one to the other;
  the legacy name has to be resolved from the legacy store directly.

So treating "strip the tag to fan out" as a general rule is incorrect — it breaks the catchup case.
The mapping between tagged and untagged names is a **store lookup**, not a string operation.

Mechanically, what happens today if you pass a tagged name to a fan-out call:

- **Tagged provider**: `toPhysicalName` is idempotent on the tag → name stays unchanged → operation
  hits the correct index. ✅
- **Legacy provider**: its `toPhysicalName` only adds the cluster prefix; it does **not** strip the
  tag (and shouldn't — see above). The tagged string passes through unchanged → operation hits
  `cluster_X.working_20240101.os` in the legacy cluster → that index does not exist → 404 /
  `index_not_found_exception`. ❌

A tagged name implies the caller already knows which provider owns it. That's the **tag-dispatched**
case, not fan-out — route it explicitly via `IndexTag` and skip the fan-out entirely (see next
section). Tagged names resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` must go
through tag-dispatched calls, not phase-dispatched fan-out.

### Tag-dispatched operations ("targeted")

> **The tag on the index name decides which provider handles the call. The phase is irrelevant.**

Used for direct operations against a specific provider's index: diagnostics, catchup creation,
provider-specific reads, or any operation where the caller already knows which index set owns the
name. A tagged name resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` is the
prototypical case.

**Rule**: use `IndexTag.resolve(name)` to select the provider, then pass the name through to the
provider as-is (or via `provider.toPhysicalName(name)` for cluster-prefix idempotency). Do **not**
strip the tag — the new index in the cluster carries the tag in its actual name; stripping would
point the request at a non-existent index.

```java
// Correct — tag decides provider; name flows through unchanged (the cluster index has .os in its name)
final IndexTag       owner  = IndexTag.resolve(indexName);   // legacy or new
final ContentletIndexOperations target =
        (owner == IndexTag.OS) ? osImpl : esImpl;
target.someOperation(target.toPhysicalName(indexName));      // toPhysicalName is idempotent on tag
```

Tag-dispatch is implemented inline in each router class using `IndexTag.resolve()` + direct
provider selection. `PhaseRouter` does not have dedicated tagged-routing methods — the routing
decision is explicit at the call site.

### IndexTag-parameter overload pattern

A lighter alternative to tag-dispatch when the **caller already holds an `IndexTag` value** (not a
tagged string in the index name itself). Instead of relying on the name to carry the routing
signal, pass it as an explicit enum parameter.

**When to use**: targeted catchup operations — e.g. creating or refreshing the mapping on only one
provider without affecting the other — where the caller has an untagged or logical name and
already knows which index set it should land on.

**Rules**:
- `IndexTag` must be the **last parameter** in any public method signature.
- The method resolves the provider from the enum value directly (`tag == IndexTag.OS ? osImpl : esImpl`).
- Inside the overload, do **not** manually call `IndexTag.OS.tag()` or `.strip()` on the index name
  strings. Let the selected provider's `toPhysicalName` (or `physicalName` for mapping ops) do the
  canonicalization — that is where tag application lives, and it is idempotent regardless of which
  form the caller passed in.
- The no-`IndexTag` overload must keep its original phase-dispatch behavior unchanged.
- The `IndexTag` enum value itself must not propagate below the routing layer (it must not reach
  `ESIndexAPI` or `IndexMappingRestOperationsOS`). The tag in the **name string**, on the other
  hand, is allowed to reach those layers — it is part of the canonical OS physical name.

```java
// Correct — IndexTag selects the provider; index names stay plain
public boolean putMapping(List<String> indexes, String mapping, IndexTag tag) throws IOException {
    final IndexMappingRestOperations ops =
            tag == IndexTag.OS ? router.osImpl() : router.esImpl();
    boolean result = true;
    for (final String index : indexes) {
        result &= ops.putMapping(CollectionsUtils.list(index), mapping);
    }
    return result;
}
```

When the operation requires a cascade of private helper methods, thread the `IndexTag` down to the
leaf write calls only. Pure computation methods (JSON building, field resolution) must not receive it.

```java
// Public entry point — IndexTag last, List<String> instead of varargs
public void addCustomMapping(List<String> indexes, IndexTag tag) {
    final String[] arr = indexes.toArray(String[]::new);
    addCustomMappingForRelationships(mappedFields, tag, arr); // tag threaded down
    putContentTypeMapping(contentType, mappingForFields, tag, arr); // leaf write
}

// Leaf write — calls the targeted putMapping overload
private void putContentTypeMapping(ContentType ct, Map<String,JSONObject> fields,
        IndexTag tag, String... indexes) throws JSONException, IOException {
    esMappingAPI.putMapping(CollectionsUtils.list(indexes), json, tag);
}
```

**Existing implementations**: `ContentletIndexAPIImpl#createContentIndex(String, int, IndexTag)`,
`ESMappingAPIImpl#putMapping(List, String, IndexTag)`,
`MappingHelper#addCustomMapping(List, IndexTag)`,
`MappingHelper#addCustomMapping(Field, List, IndexTag)`.

### Decision rule

| Name format | How to route | Routing key |
|---|---|---|
| `working_20240101` (logical, no tag) | `router.write(…)` | Migration phase |
| `cluster_<id>.working_20240101` (physical, no tag) | `router.write(…)` | Migration phase |
| `working_20240101.os` (logical, tagged) | `IndexTag.resolve(name)` → select provider directly | Tag in the name |
| `cluster_<id>.working_20240101.os` (physical, tagged) | `IndexTag.resolve(name)` → select provider directly | Tag in the name |

The cluster prefix (`cluster_<id>.`) is orthogonal to the routing decision — it is just an
infrastructure detail of the persistence form. The tag is what determines the dispatch.

If the caller holds the routing intent as an `IndexTag` enum value (not in the name string), use
the **IndexTag-parameter overload** described above instead of any of the four rows in the table.

### Why both routing keys coexist

The two routing keys — migration phase and tag in the name — are not in tension; they cover
different caller situations:

- **Phase as routing key** is for callers that operate on a logical name with no knowledge of which
  index set should own the operation. Bulk content writes, schema updates, the user-triggered
  `createContentIndex` flow — these all express "synchronize the active providers for this name".
  The migration phase is the right signal because it encodes what "active providers" means today.
- **Tag in the name as routing key** is for callers that already hold a name resolved from a
  specific store (most commonly `versionedIndicesAPI.loadDefaultVersionedIndices()`). The tag in
  the name string IS the proof that the caller already knows which index set owns it — there is
  nothing for the migration phase to decide.

Treating the tag as a routing key would be a category error only if the tag were merely a DB
artifact invisible to service code. In this codebase the tag is **part of the canonical name at
every layer** (see "Distinction tag as the canonical marker"), so a tagged name in a caller's
hand carries genuine semantic intent — and routing by tag is the natural expression of that intent.

---

## Known Gotchas

- `match_phrase_prefix` behaves differently between ES and OS — flag for manual review
- `number_of_replicas` must always be set explicitly in OS index settings
- Index timestamp is part of the identity — never hardcode index names
- Working index is always a superset — never write to live without also writing to working

### Non-finite numbers (`NaN` / `Infinity`) in manual JSON serialization — #36478

**Rule: any `float`/`double` you serialize that originates from an underlying search/index/DB/compute
API can be non-finite. Coerce it to `null` before it reaches the serializer. Never assume a number is
finite just because it is a "score", "distance", or "average".**

**Where non-finite values come from.** Elasticsearch/OpenSearch set a hit `_score` to **`NaN`**
whenever the hit is *not relevance-scored* — any query that **sorts by a field** without
`track_scores: true`, plus `filter` / `constant_score` / aggregation-only (`size: 0`) contexts. The
same hazard applies to suggester option `score`, aggregation metric values (avg/sum/stats on empty or
degenerate buckets), pgvector distances, and any Java-computed ratio/division (`0.0/0.0 → NaN`,
`x/0.0 → Infinity`).

**Both serializers are traps, in different ways:**

| Serializer | Behavior on a non-finite number | Symptom |
|------------|----------------------------------|---------|
| dotCMS `com.dotmarketing.util.json.JSONObject` / `JSONArray` (strict) | `testValidity()` throws `JSONException("JSON does not allow non-finite numbers.")` — **twice over**: eagerly inside `.put(key, value)` *and* again at serialization inside `numberToString` (reached from `.toString()`) | **HTTP 500** |
| Jackson `ObjectMapper` | Does **not** throw by default; writes the bare tokens `NaN` / `Infinity` / `-Infinity`, which are **not valid JSON** | Strict client parsers (`JSON.parse`, most SDKs) **reject the response**; silently non-standard payload. `QUOTE_NON_NUMERIC_NUMBERS` only turns them into `"NaN"` strings — still not a number/null a consumer expects |

**Why the ES cutover exposed this.** The pre-#36398 endpoints returned Elasticsearch's *native*
serializer output (`SearchResponse.toString()`), and ES XContent serializes non-finite as `null`.
#36398 rebuilt the same wire shape through the **strict** dotCMS `JSONObject`, which rejects what ES
tolerated — turning a silently-null field into a 500. Matching ES's native `null` behavior is
therefore the correct fix, not an arbitrary choice.

**The pattern (see `ESContentResourcePortlet.toLegacyEsJson` / `hitsToLegacyJson`):**

- Guard every **explicit** numeric `.put(...)` — the strict writer validates *eagerly*, so the value
  must already be finite-or-`null` when inserted:
  ```java
  .put("_score", finiteOrNull(hit.getScore()))   // NaN/Infinity -> JSONObject.NULL
  ```
- For values that enter a tree **unvalidated** — via `new JSONObject(Map)` / bean-wrapping (e.g. the
  suggester block, `_source` numerics) — a per-field guard is not enough: they skip the eager check
  but still throw at `.toString()`. Either sanitize the source map, or run one recursive pass over the
  built tree that coerces every non-finite `Float`/`Double` to `JSONObject.NULL` before serializing.
  Only `float`/`double` can be non-finite; integral and `BigDecimal` values are safe.

**Regression coverage.** `ESContentResourcePortletNaNScoreTest` (fast unit test) drives the adapter
with `NaN`/`Infinity` scores and asserts `_score: null` instead of a throw. Note the integration
tests in `ESContentResourcePortletTest` use relevance-scored `bool`/`term` queries, so they do **not**
reproduce the trigger — an end-to-end guard needs a *field-sorted* (or `constant_score`/`size:0`)
query asserting HTTP 200.

### Index name divergence between providers

**Fresh install** (Phases 1–3 from day zero): ES and OS indices are created in the same bootstrap
call. The base timestamps match; the OS name carries the `.os` tag on top — e.g.
`working_20260406` (ES) and `working_20260406.os` (OS). Divergence is limited to the tag.

**Migration catchup** (most production customers): An ES index already exists with its own
timestamp (e.g. `working_20230101`). When the OS shadow index is created later it gets a fresh
timestamp and the tag (e.g. `working_20260406.os`). **The two providers now hold indices whose
names differ in both the timestamp AND the tag.** No string transformation can convert one into
the other — the timestamps were generated independently.

This is why a name resolved from one provider cannot be passed unchanged to the other:

1. A name resolved from `legacyIndiciesAPI.loadIndicies()` (ES) — e.g. `working_20230101` — does
   not exist in OS, regardless of any tag manipulation.
2. A name resolved from `versionedIndicesAPI.loadDefaultVersionedIndices()` (OS) — e.g.
   `working_20260406.os` — does not exist in ES, even if the tag is stripped.

**Rule**: every public method on a class annotated with `@IndexRouter` that accepts an index name
must have an `IndexTag`-aware path so callers can target the correct provider when names diverge.
The plain (no-tag) overload retains its **phase-dispatched** behavior and is only safe when the
caller is passing a logical untagged name that is meant to apply to whichever providers the
current phase considers active (fresh install, coordinated reindex, or a freshly minted name the
caller has not yet resolved through a single-provider store).

```java
// Phase-dispatched — pass an untagged logical name; each provider applies its own toPhysicalName.
void someOperation(String indexName) {
    router.write(impl -> impl.someOperation(impl.toPhysicalName(indexName)));
}

// Tag-dispatched — caller already holds a tagged or provider-resolved name; route by tag.
void someOperation(String indexName, IndexTag tag) {
    final ContentletIndexOperations ops =
            tag == IndexTag.OS ? router.osImpl() : router.esImpl();
    ops.someOperation(ops.toPhysicalName(indexName));
}
```

**How to get the correct name per provider:**

- **ES**: `legacyIndiciesAPI.loadIndicies()` returns untagged names like `working_20230101`. Pair
  with `IndexTag.ES` (or call the ES provider directly).
- **OS**: `versionedIndicesAPI.loadDefaultVersionedIndices()` returns canonical tagged names like
  `working_20260406.os`. The tag is already part of the string, so `IndexTag.resolve(name)`
  recovers it without a separate parameter — see "Tag-dispatched routing" above for the call
  shape.

### Rollback risk during dual-write phases

**Context:** In phases 1 and 2 both ES and OS receive mapping writes. If a rollback to N-1 occurs
while the system is in a dual-write phase, N-1 no longer fans out to OS — but OS retains every
mapping that N pushed. The OS index silently drifts ahead of ES.

**Impact:** Benign for ES (which N-1 reads and writes). Invisible in phase 0 rollbacks. Becomes
critical only if the system is later re-upgraded to phase 2 or 3 without first resyncing OS, because
OS would serve stale or inconsistent data.

**Mitigation options (in order of cost):**

1. **Runbook** — document explicitly: *"If rolling back during Phase 1 or 2, trigger a full reindex
   against OS before re-activating the migration."*

2. **Startup drift detection** — in `IndexStartupValidator`, when starting in Phase 1 or 2, compare
   the field mappings of the active ES and OS working indices and log a `WARN` for any divergent
   fields. Does not block startup.

3. **Phase gate validation** — before promoting Phase 1 → 2 (switching reads to OS), assert that ES
   and OS mapping schemas are identical. Reject the promotion if they are not.

Option 2 is the recommended minimum: it makes drift observable from the first restart after a
rollback, with no impact on normal operation.

> **Status**: Option 1 (runbook) is the only mitigation currently in place. Option 2 and 3 are
> not yet implemented — tracked as technical debt before Phase 2 goes to production.

#### Phase rollback during an in-flight full reindex (#36471)

Rolling `FEATURE_FLAG_OPEN_SEARCH_PHASE` back to 0 **while a full reindex is draining the
journal** is a distinct hazard from the mapping drift above. The phase is re-read per journal
batch, so the remaining entries index to ES only and the OS reindex pair freezes partially
populated. The ES switchover then completes in Phase 0.

**Fixed behavior:** the Phase-0 switchover (and abort) now treats this state as an OS reindex
abort — the active OS working/live rows survive in the store (the legacy `indicies` update is
scoped to its own NULL-version rows), the OS reindex slots are cleared, and the partial physical
`.os` pair is deleted from the cluster so a later boot catchup can never adopt it as active. The
abort is logged at WARN with the deleted index names.

**Operational rule:** the OS pair that survives the rollback is the *old* one — it stops
receiving writes in Phase 0 and drifts exactly as described above. Before re-activating Phase 2,
trigger a full reindex so OS is rebuilt in a dual-write phase. Prefer letting an in-flight
reindex finish (or aborting it explicitly) over flipping the phase mid-drain.

---

### Fan-out routing with divergent index names — resolved (#35640)

**Status: routing resolved via the transparent-mirror principle (#35640); expected-miss log
noise on the shadow leg resolved for `delete()` (#36423, QA #36219 TC-041); primary-failure
propagation for `delete()` tracked in #36430; still open for other fan-out methods.**

When a public `@IndexRouter` method accepts an index name and the phase requires fan-out, the
**routing** is settled: the caller passes the logical name and each provider derives its OWN
physical name (ES → bare, OS → `.os`) before touching its cluster — the name is never sent verbatim
to the wrong provider. Deleting by **either** the ES (bare) or the OS (`.os`) name removes the index
in every engine that holds it (bidirectional transparent mirror), so the mirror is never left
half-deleted. This is implemented in `ContentletIndexAPIImpl.delete` (untag → broadcast) and in
`IndexAPIImpl` for the maintenance/lifecycle ops:

- **List ops** (`flushCaches`, `optimize`) partition the incoming list by `IndexTag.resolve` and
  hand each provider only the names it owns.
- **Single-name lifecycle ops** (`clearIndex`, `openIndex`, `closeIndex`, `updateReplicas`) resolve
  a per-provider name via `providerName(impl, name)` — the OS leg gets the `.os`-tagged name, ES the
  bare name. (Site-search is the exception: its OS copy is not `.os`-tagged, so it stays bare.)

**Resolved for `delete()` — expected-miss log noise (#36423):** when an index genuinely exists in
only one engine (divergent names after a catchup), the shadow leg now does an exists-check and
**skips** the cluster delete instead of attempting it and logging an ERROR stack trace for the
expected miss. The skip and any genuine shadow failure are logged through the shadow-write policy
(`DOTCMS_SHADOW_WRITE_LOG_LEVEL`, default WARN); only the primary (read-provider) leg logs at ERROR.
The DB pointer for each engine is still cleared even when its cluster delete is skipped. Surfacing
primary failures to the *caller* (the `PhaseRouter.writeBoolean` contract: re-throw after all
providers were called) is tracked separately in #36430. Covered in `OpenSearchUpgradeSuite` by
`ContentletIndexAPIImplMigrationIntegrationTest` (name only in ES → shadow skip; deleting by the
`.os` name → both engines via the transparent mirror).

**Still open for other fan-out methods** (e.g. mapping and lifecycle operations):

- The same exists-check "expected miss vs. genuine failure" distinction has not been applied
  outside `delete()`; a 404 on the shadow leg of those ops may still surface as ERROR noise where
  it signals an expected divergent-name miss rather than a transient cluster error.
- Verify that `loadProviderIndices` / `ProviderIndices` correctly returns `null` (skip) for a
  provider whose store has no record yet, rather than silently passing a stale or wrong name.

Until test coverage exists for those scenarios, treat other public `@IndexRouter` methods that
accept a raw index name string in dual-write phases as **untested for the name-mismatch case**.

---

## Testing

All tests related to this migration must be added to the **`OpenSearchUpgradeSuite`** test suite.
Never add migration tests to general test suites — keep them isolated and easy to run together.

---

## Deferred (lower priority)
- **Site Search** (`site-search` index) — in scope, but separate pipeline; will be addressed after core content index migration is stable
- **User-facing query routing during dual-write phase** — search queries are not yet phase-aware beyond the read provider selection in `PhaseRouter`