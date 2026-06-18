# QA Test Plan — ES → OpenSearch Migration

**Issues:** [#34934](https://github.com/dotCMS/core/issues/34934) · [#34164](https://github.com/dotCMS/core/issues/34164)
**Epic:** [#34153 — Migrate Core Infrastructure Classes](https://github.com/dotCMS/core/issues/34153)
**QA Epic:** [#35476 — End-to-End QA Validation](https://github.com/dotCMS/core/issues/35476)
**Fix PR:** [#35632 — Startup hardening, automatic migration shutdown, phase-aware index init, thread-safe formatter](https://github.com/dotCMS/core/pull/35632)
**Scope:** ElasticSearch → OpenSearch dual-write and read migration

**Status:** Phases 0 and 1 are fully testable. Phase 2 dual-write is testable — every ES write is mirrored to OS and can be verified via OpenSearch Dashboards — but the dotCMS query layer has **not yet been migrated**, so application-level read validation is only partial in Phase 2. Phase 3 is **not functionally testable** at this time (documented cases only). PR #35632 introduced an **automatic migration shutdown**: when OpenSearch is unreachable or reports the wrong version in Phase ≥ 1, dotCMS resets to Phase 0 by itself and keeps serving from ES — it does **not** crash or hang. The visible effects of that shutdown are two `ERROR` lines + one `WARN` line (there is **no** `FATAL` on the startup path — see **Helpers / H5**). This plan is written for a tester who exercises the system from the outside; the test cases live in **Groups 1–16** below.

---

## Abstract

**Purpose.** This plan verifies the ElasticSearch → OpenSearch migration introduced by PR #35632: that
dotCMS **dual-writes** content to both engines, that it **degrades safely** when OpenSearch is missing or
misconfigured (it falls back to ES, never crashing or hanging), and that the index lifecycle (create /
delete / reindex) and the `/v1/esindex` REST API stay correct and in sync with the `indicies` DB table.
It is written for a tester who works **from the outside** — admin UI, REST API, startup log, Kibana /
OpenSearch Dashboards, and SQL — without reading source code.

**Minimum setup.** Bring up the migration stack with one command
(`docker compose -f docker/docker-compose-examples/os-migration/docker-compose.yml up -d`), which gives you
ES 7.10 + Kibana and OpenSearch 3.x + OS Dashboards on one network; wait for both engines' health checks to
pass, then start dotCMS (`http://localhost:8082`, `admin:admin`) pointed at that stack. The full table of
services and ports is in **Environment**; the limited-user (non-admin OS) variant is in **Group 16**.

**Required environment variables** (set in `dotmarketing-config.properties`):

| Variable | Purpose |
|---|---|
| `FEATURE_FLAG_OPEN_SEARCH_PHASE` | Selects how far the migration runs: `0` = ES only, `1` = dual-write / ES reads, `2` = dual-write / OS reads, `3` = OS only. This is the switch most cases toggle. |
| `OS_ENDPOINTS` | URL of the OpenSearch (new engine) cluster, e.g. `http://localhost:9201`. Must be a **separate** instance from ES — pointing it at the ES URL or at the ES address is what the safety guards in Groups 1–2 detect. |
| `OS_AUTH_TYPE` | Authentication scheme dotCMS uses to talk to OpenSearch (`BASIC` for these cases). |
| `OS_AUTH_BASIC_USER` / `OS_AUTH_BASIC_PASSWORD` | Credentials for OpenSearch. With the open dev stack these are `admin` / `admin`; the limited-user stack (Group 16) uses the restricted `dotcms-es-user`. |
| `OS_TLS_ENABLED` | Whether the OpenSearch connection uses TLS (`false` for the open dev stack; the limited-user stack uses HTTPS plus `OS_TLS_TRUST_SELF_SIGNED=true`). |

> A phase change is read at startup **and** on each routing decision, so it takes effect without a restart —
> but every cluster node must carry the same value. See **Environment** for the multi-node layout (Group 3).

---

## 1. Objective

Validate that the ES → OpenSearch migration pipeline delivers:

1. **Zero regression** on ES-backed functionality (Phase 0 / no flag set).
2. **Correct dual-write** in Phase 1 — every ES write is mirrored to OS; failures on OS are fire-and-forget (never affect the business operation).
3. **Correct dual-read** in Phase 2 — reads switch to OS; field mappings are structurally equivalent to ES; record counts match between the two indices (partial scope — see Status).
4. **Correct index lifecycle** across all phases — activate, deactivate, delete, and reindex operations behave consistently and keep the `indicies` DB table in sync.
5. **Safe failure modes** — dotCMS starts gracefully when OpenSearch is unavailable. In Phase ≥ 1, an unreachable or mismatched OS automatically shuts the migration off (resets to Phase 0 in memory) and dotCMS keeps running on ES.
6. **REST API parity** — `/v1/esindex` endpoints behave correctly in both single-backend and dual-write phases, and enforce authentication.
7. **Content and Content Type lifecycle sync** — schema changes and content deletes are propagated to the OS index.

---

## 2. Migration Phase Reference

Full specification: [`docs/backend/OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md)

| Phase | Writes     | Reads      | ES Status            | OS Write Failures        |
|-------|------------|------------|----------------------|--------------------------|
| 0     | ES only    | ES only    | Active               | n/a                      |
| 1     | ES + OS    | ES only    | Active               | Fire-and-forget (logged) |
| 2     | ES + OS    | OS only    | Active (fallback)    | Fire-and-forget (logged) |
| 3     | OS only    | OS only    | Decommissioned       | Propagate to caller      |

**Testable scope:**

| Phase | Testable? | Limitation |
|-------|-----------|------------|
| 0 | Yes — fully | No dependencies beyond existing ES stack |
| 1 | Yes — fully | Dual-write verified via Kibana + OS Dashboards |
| 2 | Partially | Dual-write testable via OS Dashboards; application-level reads **not testable** — dotCMS query layer not yet migrated |
| 3 | No | Query layer not migrated; OS-only reads are broken; no ES fallback — activating Phase 3 produces broken application reads |

---

## 3. Key Classes Under Test

| Class | PR(s) | What Changed |
|---|---|---|
| `ContentletIndexAPIImpl` | #35289, #35356, #35389, #35632 | Phase-aware routing, Phase 3 guards, fire-and-forget enforcement, automatic migration shutdown on startup failure, phase-aware `hasEmptyIndices()` |
| `IndexStartupValidator` | #35632 | `validateIndexingConfig()` now returns `boolean`; in Phase 3 connectivity failures become hard errors (`DotRuntimeException`); in Phase 1/2 they trigger the automatic shutdown to Phase 0 |
| `IndexConfigHelper` | #35632 | Added `MigrationPhase.reset()` and the automatic migration shutdown for runtime rollback to Phase 0 |
| `ESIndexResource` | #35632 | `threadSafeTimestampFormatter` (`DateTimeFormatter`) replaces non-thread-safe `SimpleDateFormat` for index name generation |
| `ESMappingAPIImpl` | #35123, #35275 | PhaseRouter migration, `putMapping(List, String, IndexTag)` overload |
| `IndexAPIImpl` | #35123, #35275 | Full PhaseRouter migration |
| `OSIndexAPIImpl` | #35352, #35349 | `getClosedIndexes()` fix, correct settings JSON loaded |
| `OSBulkHelper` | #35390 | `getIndexName()` now queries `VersionedIndicesAPI` instead of returning fallback |
| `ReindexThread` | #35323 | AtomicReference + ConcurrentHashMap for race-free BulkProcessor rebuild |
| `BulkProcessorListener` | #35323, #35356, #35389 | Phase-aware primary `IndexTag`, shadow fire-and-forget contract |
| `ReindexEntry` | #35391 | Immutable value object (Immutables) |
| `VersionedIndicesAPIImpl` | #35289 | `.os` tag management for DB row uniqueness |

---

## Glossary (read once)

| Term used in this plan | What it means in plain words |
|------------------------|------------------------------|
| **Old search engine (ES)** | Elasticsearch — the search engine dotCMS uses today. It always works. Default port `9200`, inspected with **Kibana** (`5601`). |
| **New search engine (OS)** | OpenSearch — the engine we are migrating to. Default port `9201`, inspected with **OpenSearch Dashboards** (`5602`). |
| **Migration phase** | How far along the migration the system is. **Phase 0** = old engine only. **Phase 1** = save to *both* engines, but search only the old one. (Phases 2 and 3 are out of scope here.) |
| **Dual-write** | In Phase 1, every content change is written to *both* search engines at the same time. |
| **Automatic migration shutdown** | A safety feature: if at startup the new engine cannot be reached or is the wrong version, dotCMS **turns the migration off by itself**, drops back to Phase 0 (old engine only) and keeps running normally. It does **not** crash and does **not** hang. (Internally this is the behavior the developers call "halt migration" — you only need to recognize it by its visible effects.) |
| **Index** | The store where a search engine keeps content so it can be searched. Each engine has its own index, and index names contain a timestamp (e.g. `cluster_08abc3.working_20260421120000`). |
| **`.os` suffix (the "tag")** | The marker that tells a new-engine (OpenSearch) index apart from an old-engine (Elasticsearch) one. New-engine index names **end in `.os`** (e.g. `cluster_08abc3.working_20260406.os`); old-engine names do not. This suffix is part of the real index name and is **visible everywhere** — in the cluster, in the database, and in the dotCMS REST API responses. (It is NOT an `os::` prefix — if you see `os::` in any older doc, that doc is outdated.) |
| **Reindex (rebuild)** | Refilling an index from scratch with all content. It is a heavy operation and must **not** happen without a real reason. |
| **`indicies` table** | The database table where dotCMS records which indexes exist. It stores the full physical name (cluster prefix + tag). New-engine rows **end in `.os`**; old-engine rows do not. |

---

## Environment

Start the migration stack once (gives you ES + Kibana and OS + OS Dashboards on one network):

```bash
docker compose -f docker/docker-compose-examples/os-migration/docker-compose.yml up -d
```

| Service               | URL                     | Used for                          |
|-----------------------|-------------------------|-----------------------------------|
| Old engine (ES 7.10)  | http://localhost:9200   | Direct REST queries               |
| Kibana                | http://localhost:5601   | Inspect / query the OLD index     |
| New engine (OS 3.x)   | http://localhost:9201   | Direct REST queries               |
| OpenSearch Dashboards | http://localhost:5602   | Inspect / query the NEW index     |
| dotCMS                | http://localhost:8082   | Admin UI + REST API (`admin:admin`)|

> Wait for both engines' health checks to pass before starting dotCMS:
> ```bash
> curl -s http://localhost:9200/_cluster/health | jq .status
> curl -s http://localhost:9201/_cluster/health | jq .status
> ```

dotCMS migration settings live in `dotmarketing-config.properties`
(full reference: [`docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md)):

```properties
# Migration phase: 0 = old engine only, 1 = dual-write (search old engine)
FEATURE_FLAG_OPEN_SEARCH_PHASE=1
# New-engine connection
OS_ENDPOINTS=http://localhost:9201
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
OS_TLS_ENABLED=false
```

### Multi-node setup (Group 3)

For the cases that need two dotCMS nodes, run two instances against the **same** PostgreSQL DB and the
**same** ES + OS clusters:

- **Node 1** — port `8082`, shared DB, `OS_ENDPOINTS=http://localhost:9201`
- **Node 2** — port `8083`, same DB, same `OS_ENDPOINTS`

Both nodes must share the same phase and endpoint values (in `dotcms-config-cluster.properties`).

---

## Helpers (used by many cases)

**H1 — Find your real index names.** Index names contain a timestamp, so they differ on every
machine. Never guess — list them. **New-engine indexes are the ones whose names end in `.os`;**
old-engine names do not.

```bash
# What dotCMS knows about (names keep the .os tag but drop the cluster prefix):
curl -s -u admin:admin http://localhost:8082/api/v1/esindex | jq .

# The exact physical names each engine actually holds (use THESE for direct curl / Kibana /
# OpenSearch Dashboards queries — they include the cluster prefix and, for OS, the .os suffix):
curl -s -u admin:admin http://localhost:9200/_cat/indices?v   # OLD engine (no .os)
curl -s -u admin:admin http://localhost:9201/_cat/indices?v   # NEW engine (ends in .os)
```
Or in the UI: **Admin → System → Index**. Note the *working* and *live* index names — you will paste
them into later steps.

**H2 — Which phase did the system end up in?** Two independent ways:
- **UI / index list:** Admin → System → Index (or H1). If you see **only old-engine indexes** (no name
  ending in `.os`) → Phase 0. If you also see new-engine indexes (names ending in `.os`) → Phase 1.
- **Log:** in the dotCMS startup log, find the line stating the migration phase (similar to
  `Migration Phase: ...`). When the automatic shutdown fired you will also see an error line saying
  the migration was disabled and the phase was reset to 0.

**H3 — Check the database directly** (SQL console against the dotCMS PostgreSQL DB). The new-engine
marker is the **`.os` suffix** on the name (not an `os::` prefix):

```sql
-- Rows for the NEW engine (should exist only when migration is really active):
SELECT * FROM indicies WHERE index_name LIKE '%.os';
-- Rows for the OLD engine:
SELECT * FROM indicies WHERE index_name NOT LIKE '%.os';
-- Count per engine (Phase 1 fresh install: 2 OLD + 2 NEW; Phase 0: 2 OLD, 0 NEW):
SELECT CASE WHEN index_name LIKE '%.os' THEN 'NEW' ELSE 'OLD' END AS engine,
       COUNT(*) FROM indicies GROUP BY 1;
```

**H4 — Count documents in an index directly** (use the full physical names from H1; the new-engine
one ends in `.os`):

```bash
curl -s -u admin:admin http://localhost:9200/<old-index-name>/_count       # e.g. cluster_X.working_20230101
curl -s -u admin:admin http://localhost:9201/<new-index-name.os>/_count     # e.g. cluster_X.working_20260406.os
```

**H5 — The exact log lines of an automatic migration shutdown.** When the safety feature fires at
startup (Phase 1 or 2), dotCMS writes **three** log lines, in this order. The text below is verbatim
from the code — match it when a case says "the shutdown fired". Note the levels: **two `ERROR` lines
and one `WARN` line — there is NO `FATAL` line on this path.**

```
ERROR  OpenSearch configuration error — halting OS migration, dotCMS will fall back to ES-only: <reason>
ERROR  OpenSearch migration halted: invalid configuration detected at startup. Verify OS_ENDPOINTS, OS version, and FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
WARN   Migration phase reset to PHASE_0_MIGRATION_NOT_STARTED (was PHASE_1_DUAL_WRITE_ES_READS). This change is runtime-only — persist it in dotmarketing-config.properties to survive a restart.
```

`<reason>` on the first line is one of:
- **New engine unreachable / bad URL:** `OpenSearch cluster is not reachable: <cause>. Check OS_ENDPOINTS configuration.`
- **Wrong version:** `OpenSearch version mismatch: expected 3.x but connected cluster reports version <X>. Check OS_ENDPOINTS configuration.`
- **New-engine address equals old-engine address:** `ES and OS clients point to the same endpoint(s): <set>. ES endpoints: <…> — OS endpoints: <…>. Set OS_ENDPOINTS to a separate OpenSearch instance.`

When the migration starts **successfully** (no shutdown) you instead see an `INFO` line
`OS version check passed: <version>` and `Endpoint separation check passed. …`.

---

# Group 1 — Safe startup when the new search engine is unavailable

> What this group proves: when the migration is ON but OpenSearch is not usable, **dotCMS still
> starts normally and keeps serving search from the old engine (ES)**. It does NOT stop, crash, or
> hang. The *migration* (not dotCMS) is what gets switched off, and an error is logged so an
> operator can see why. (The case where OpenSearch answers but with the wrong version moved to
> Group 2 — see TC-008 — because it is induced through configuration.)

## TC-001 — OpenSearch is down at startup → dotCMS starts normally and keeps serving from ES

- **Objective:** When the migration is turned on (Phase 1) but OpenSearch is unreachable, prove that
  **dotCMS starts up normally and keeps working using the old engine (ES)** — it must not stop,
  crash, or hang. The migration switches itself off (drops to Phase 0) and an error is logged.
- **Risk:** High
- **Preconditions:**
  - Old engine running and healthy; new engine **stopped**.
    `docker compose -f docker/docker-compose-examples/os-migration/docker-compose.yml up -d elasticsearch kibana`
  - dotCMS configured with `FEATURE_FLAG_OPEN_SEARCH_PHASE=1` and `OS_ENDPOINTS=http://localhost:9201`.
- **Steps:**
  1. Start dotCMS. Watch the log until startup finishes — do not kill it even if it pauses while it
     tries to reach the new engine.
  2. Check the resulting phase with **H2** (UI and log).
  3. List the indexes with **H1**.
  4. Run **H3** to see whether any new-engine rows (names ending in `.os`) exist.
  5. In the admin UI, create a content item, publish it, then search for it in the content search.
- **Expected Result (pass = all true):**
  - **dotCMS starts and runs normally — it does NOT stop, crash, or hang.** Search keeps working
    against the old engine (ES).
  - The migration is left **off** (Phase 0), and the log shows the expected error lines (see H5:
    two `ERROR` lines + one `WARN`, **not** a `FATAL`), where the reason is
    `OpenSearch cluster is not reachable: …. Check OS_ENDPOINTS configuration.`
  - `/api/v1/esindex` (H1) and the UI list **only old-engine indexes** — no new-engine index.
  - The database query (H3) returns **0 rows** ending in `.os`.
  - Create / publish / search all work normally.
- **Type:** Manual

## TC-003 — Migration off (Phase 0) with the new engine down → nothing is affected (control case)

- **Objective:** Prove that when the migration is **not** active, a down new engine is completely
  irrelevant: dotCMS never even tries to use it.
- **Risk:** High
- **Preconditions:**
  - Old engine running; new engine **stopped** (`docker compose stop opensearch`).
  - dotCMS configured with `FEATURE_FLAG_OPEN_SEARCH_PHASE=0` (or no migration flag at all).
- **Steps:**
  1. Start dotCMS and watch the startup log.
  2. Create a content item, publish it, and search for it.
  3. In Kibana, confirm the new document is in the old-engine index.
- **Expected Result:**
  - dotCMS starts normally. **None** of the H5 shutdown lines appear (no "halting OS migration",
    no "Migration phase reset…"). No "OS version check passed" line either.
  - The log shows **no attempt** to connect to the new engine.
  - Create / publish / search succeed; the document is visible in Kibana.
- **Type:** Manual

## TC-004 — Turning the migration on must NOT trigger a needless full rebuild

- **Objective:** When you switch from Phase 0 to Phase 1, the new engine's index starts empty. Prove
  that dotCMS does **not** mistake "new index is empty" for "all indexes are empty" and launch a full
  rebuild of the old engine. Existing content must be preserved and stay searchable.
- **Risk:** High
- **Preconditions:**
  - dotCMS running in **Phase 0**.
- **Steps:**
  1. In Phase 0, create and publish at least 5 content items.
  2. Find the old-engine working index name (**H1**) and confirm it has content (**H4**, count > 0).
  3. Stop dotCMS. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`. Restart dotCMS.
  4. Watch the startup log for any rebuild/reindex messages.
  5. Find the new working index name (**H1**) and check its count (**H4**).
  6. In the content search UI, search for the items you created in step 1.
- **Expected Result:**
  - dotCMS starts in Phase 1; a new-engine index appears (with a newer timestamp).
  - The new-engine index count is **0** (expected — back-fill is not part of startup).
  - The old-engine index (older timestamp) is **unchanged**, count still > 0.
  - The log does **NOT** contain any reindex/rebuild progress (e.g. no "Reindexation is starting").
  - All pre-existing items are still **fully searchable** in the UI.
- **Type:** Manual

## TC-005 — Creating many indexes at once produces unique, valid names

- **Objective:** Under heavy concurrent load, index names (which are built from a timestamp) must
  never collide or come out malformed. Prove that 20 simultaneous create requests yield 20 distinct,
  well-formed indexes.
- **Risk:** Medium
- **Preconditions:**
  - dotCMS running in **Phase 0**.
- **Steps:**
  1. Fire 20 create requests in parallel:
     ```bash
     seq 20 | xargs -P 20 -I{} curl -s -o /tmp/idx_{}.json -w "%{http_code}\n" \
       -u admin:admin -X PUT http://localhost:8082/api/v1/esindex/create/shards/1
     ```
  2. Collect the 20 returned index names from `/tmp/idx_*.json`.
  3. List the engine's indexes: `curl -s -u admin:admin http://localhost:9200/_cat/indices?v`.
- **Expected Result:**
  - All 20 requests return HTTP **200**.
  - All 20 returned names are well-formed: no spaces, no empty/null segments, no truncated timestamps.
  - All 20 names are **unique** — no two requests produced the same name.
  - The engine shows 20 new indexes with distinct names.
- **Type:** Manual

---

# Group 2 — Invalid `OS_ENDPOINTS` configuration (and how to induce each one)

> What this group proves: with a bad OpenSearch address, **dotCMS still starts normally and keeps
> serving from the old engine (ES)** — it does not crash or hang. The migration is left off and a
> clear error is logged. Each case below also tells you exactly how to reproduce the bad config.

## TC-006 — A malformed `OS_ENDPOINTS` value does not crash dotCMS

- **Objective:** With a clearly invalid address, dotCMS must still start normally and keep serving
  from ES; the bad value is reported in the log and the migration is left off.
- **How to induce:** set a value that is not a valid URL.
- **Risk:** High
- **Preconditions:** `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`, `OS_ENDPOINTS=not-a-valid-url:xyz`.
- **Steps:**
  1. Start dotCMS; confirm the process stays up (no failed deployment).
  2. Check the phase (**H2**) and the DB (**H3**).
  3. Create/publish/search a content item.
- **Expected Result:**
  - **dotCMS starts and runs normally** — no uncaught exception, no failed deployment.
  - The migration is left off (Phase 0); the log shows the expected error lines (H5), where the
    reason is `OpenSearch cluster is not reachable: <cause>. Check OS_ENDPOINTS configuration.`
    (the parse/connection failure is reported as the cause).
  - Content operations work on the old engine. No raw stack trace with no context.
- **Type:** Manual

## TC-007 — A non-existent OpenSearch host doesn't hang dotCMS forever

- **Objective:** If the address points to a host that does not exist, dotCMS must eventually time
  out and finish starting (serving from ES) — it must not wait forever. The migration is left off.
- **How to induce:** point `OS_ENDPOINTS` at a hostname that does not resolve.
- **Risk:** Medium
- **Preconditions:** `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`, `OS_ENDPOINTS=http://nonexistent-host.invalid:9201`.
- **Steps:**
  1. Start dotCMS. It may pause while the connection times out — wait for startup to finish.
  2. Check the phase (**H2**).
- **Expected Result:**
  - **dotCMS finishes starting and is operational on the old engine — it is NOT stuck** waiting.
  - After the timeout, the migration is left off; the log shows the expected error lines (H5) with
    reason `OpenSearch cluster is not reachable: <cause>. Check OS_ENDPOINTS configuration.`
- **Type:** Manual

## TC-008 — `OS_ENDPOINTS` points to something that is not OpenSearch 3.x (e.g. an Elasticsearch cluster)

> Absorbs the former TC-002 ("wrong version at startup"). The scenario is the same; here it is framed
> as a configuration mistake with an explicit recipe to reproduce it.

- **Objective:** When the configured OpenSearch address actually answers but reports the wrong
  version (it is not OpenSearch 3.x), dotCMS must still start normally and keep serving from ES; the
  migration is left off because the version is wrong.
- **How to induce:** start dotCMS with the OpenSearch connection string pointing at the **Elasticsearch**
  URL — i.e. set `OS_ENDPOINTS=http://localhost:9200` (the ES port). ES replies, but it reports
  version 7.10.x, which is not `3.x`.
- **Risk:** Medium
- **Preconditions:** Both engines up; `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`, `OS_ENDPOINTS=http://localhost:9200`.
- **Steps:**
  1. Start dotCMS and read the startup log.
  2. Check the phase (**H2**); list indexes (**H1**) and check the DB (**H3**).
- **Expected Result:**
  - **dotCMS starts and runs normally on the old engine.**
  - The migration is left off; the log shows the expected error lines (H5) with reason
    `OpenSearch version mismatch: expected 3.x but connected cluster reports version 7.10.x. Check OS_ENDPOINTS configuration.`
    (the version number will be whatever ES reports), followed by the phase-reset `WARN`.
  - Only old-engine indexes are listed; no `.os` rows in the database.
  - (Because the OpenSearch address equals the ES address, the version check is what fails first;
    the "same endpoint(s)" check is a secondary guard.)
- **Type:** Manual

---

# Group 3 — Two dotCMS nodes sharing one database (Phase 1)

> For these cases run two dotCMS instances against the **same** PostgreSQL DB and the **same** ES + OS
> clusters: **Node 1** on `8082`, **Node 2** on `8083`, both with identical migration settings.

## TC-009 — Two nodes starting in Phase 1 don't create duplicate index records

- **Objective:** When a second node starts and finds the migration already set up by the first node,
  it must reuse the existing index records — not create duplicates or hit database constraint errors.
- **Risk:** High
- **Preconditions:** Both engines up. Both nodes configured for Phase 1 with the same endpoints.
- **Steps:**
  1. Start **Node 1**. Wait for full startup. Run **H3** and record the row counts.
  2. Start **Node 2**. Wait for full startup. Run **H3** again.
- **Expected Result:**
  - After Node 1: exactly **2 new-engine rows** (a `…working_<ts>.os` and a `…live_<ts>.os`) and **2 old-engine rows**.
  - After Node 2: **still exactly 2 + 2** — no duplicates, no DB constraint violations.
  - Both nodes report Phase 1 in their startup logs.
- **Type:** Manual

## TC-010 — Content written on one node is visible from the other

- **Objective:** In a shared cluster, content published on Node 1 must be searchable from Node 2
  (both read from the old engine in Phase 1), and the document must also have been dual-written to
  the new engine.
- **Risk:** High
- **Preconditions:** Both nodes running as in TC-009.
- **Steps:**
  1. On **Node 1**, create and publish a content item; note its identifier.
  2. Wait ~5 seconds.
  3. On **Node 2**, search for it:
     `curl -s -u admin:admin "http://localhost:8083/api/content/search/-query/+identifier:<id> -live false"`
  4. In **OpenSearch Dashboards**, confirm the same document exists in the new working index.
- **Expected Result:**
  - The item created on Node 1 is found via Node 2.
  - OpenSearch Dashboards confirms the document was dual-written to the new engine.
- **Type:** Manual

## TC-011 — One node misconfigured → only that node falls back; the other keeps migrating

- **Objective:** If one node has a wrong new-engine address, only that node should shut its migration
  off (Phase 0). The healthy node must keep dual-writing. This documents the known "split phase"
  degraded state — it is not a crash.
- **Risk:** Medium
- **Preconditions:** Node 1 with `OS_ENDPOINTS=http://localhost:9999` (wrong port); Node 2 with the
  correct `OS_ENDPOINTS=http://localhost:9201`. Both at Phase 1.
- **Steps:**
  1. Start both nodes; read each node's startup log (**H2**).
  2. Publish a content item from **Node 2**.
  3. Search for it from both nodes.
- **Expected Result:**
  - Node 1 ends in Phase 0 (migration shut off, error logged).
  - Node 2 ends in Phase 1 (dual-write active).
  - Content published from Node 2 reaches the new engine and is searchable from **both** nodes
    (both read the old engine).
- **Type:** Manual

---

# Group 4 — Searchable fields and cascade delete of content

> Important: Content Types themselves are **not** stored in a search index — only their **instances
> (the content items)** are. So we do not check whether a Content Type "appears" in the index. What
> we verify is (a) that a field marked *searchable* really becomes searchable in both engines, and
> (b) that deleting a Content Type removes its content items from both engines.

## TC-012 — A field marked "searchable" is searchable in both engines; a non-searchable one is not

- **Objective:** Prove that the *searchable* flag on a field controls query behavior consistently in
  both engines: items can be found by a searchable field's value, and cannot by a non-searchable one.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1** (dual-write active).
- **Steps:**
  1. Create a Content Type `TestCT_<uuid>` with two text fields: `searchableField` (mark **Searchable
     = yes**) and `plainField` (Searchable = no).
  2. Create and publish 3 items, giving `searchableField` a distinctive value like `findme_<uuid>`
     and `plainField` the value `hidden_<uuid>`.
  3. Find both working index names (**H1**).
  4. In **Kibana** (old index) and **OpenSearch Dashboards** (new index), run the same two queries:
     ```json
     POST /<index>/_search { "query": { "match": { "searchablefield": "findme_<uuid>" } } }
     POST /<index>/_search { "query": { "match": { "plainfield": "hidden_<uuid>" } } }
     ```
- **Expected Result:**
  - The `searchableField` query returns the **3 items in both** engines.
  - The `plainField` query returns them in neither (or the field is not indexed for search) — and the
    behavior is the **same** in both engines.
- **Type:** Manual

## TC-013 — Making a field searchable later takes effect in both engines after republish

- **Objective:** Prove that turning the *searchable* flag on for an existing field, then publishing,
  makes that field queryable consistently in both engines.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1**; a Content Type with a field currently **not** searchable.
- **Steps:**
  1. Edit the Content Type and set the field's **Searchable = yes**. Save.
  2. Create and publish an item with a distinctive value `nowsearchable_<uuid>` in that field.
  3. Run the matching `_search` query (as in TC-012) in **both** Kibana and OpenSearch Dashboards.
- **Expected Result:**
  - The newly published item is found by that field's value in **both** engines.
  - The result is consistent across the two engines.
- **Type:** Manual

## TC-014 — Deleting a Content Type removes its content items from both engines

- **Objective:** Prove that deleting a Content Type deletes its **instances** from both the old and
  the new engine (no orphaned documents left behind on either side).
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create Content Type `DeleteMeCT_<uuid>`; create and publish 3 items of it.
  2. In **both** engines, confirm the 3 items are present:
     `POST /<index>/_search { "query": { "match": { "contenttype": "deletemect_<uuid>" } } }`
  3. In **Admin → Content Types**, delete `DeleteMeCT_<uuid>` and confirm.
  4. Wait ~10 seconds. Re-run the two search queries.
- **Expected Result:**
  - Both engines return **0 hits** for that content type after deletion.
  - No error in the dotCMS log related to the new-engine cleanup.
- **Type:** Manual

---

# Group 5 — Deleting a content item

## TC-015 — Unpublish + delete a content item → gone from both engines

- **Objective:** Prove that deleting a content item removes it from both engines and leaves no
  pending work behind.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create and publish a content item; note its identifier.
  2. Find both working index names (**H1**). Confirm the document exists in each:
     ```bash
     curl -s -u admin:admin http://localhost:9200/<old-index>/_doc/<identifier>   # expect 200/found
     curl -s -u admin:admin http://localhost:9201/<new-index.os>/_doc/<identifier>   # new index ends in .os; expect 200/found
     ```
  3. In the admin UI, **unpublish** then **delete** the item.
  4. Wait ~5 seconds. Re-run both `_doc` requests.
  5. In the SQL console, check for leftover work:
     `SELECT * FROM dist_reindex_journal WHERE ident_to_index = '<identifier>';`
- **Expected Result:**
  - Both engines return **404 / not found** for the document.
  - No error in the dotCMS log about the new-engine delete.
  - `dist_reindex_journal` has **no pending entry** for that identifier.
- **Type:** Manual

---

# Group 6 — Deleting and rebuilding indexes

## TC-016 — Deleting the old-engine index does not touch the new-engine index

> ⚠️ **Behavior to confirm before this is treated as pass/fail (open design question).** The two
> engines hold the same logical index, distinguished only by the `.os` suffix. It is not yet settled
> whether deleting "an index" should remove **only** the named one (what the migration doc implies —
> *"User-triggered index shutdown → not replicated to OS"*) or should **cascade** to delete the
> `.os` counterpart too. **Action: run this case once to observe what the endpoint actually does,
> then confirm the intended behavior with the team.** Until then, the expected result below reflects
> the *documented* design (delete the named index only); flag any divergence rather than failing.

- **Objective:** When the two engines have differently-named indexes, observe whether deleting the
  old-engine index leaves the new-engine index intact (documented design) or also removes it.
- **Risk:** High
- **Preconditions:** Create the divergent-name situation: start in **Phase 0**, create content (the
  old-engine index gets an older timestamp), then switch to **Phase 1** (the new-engine index is
  created later, so it gets both a **newer timestamp and the `.os` suffix** — e.g. old
  `cluster_X.working_20230101` vs new `cluster_X.working_20260406.os`).
- **Steps:**
  1. Confirm the two names differ (different timestamp, and only the new one ends in `.os`):
     `SELECT index_name FROM indicies WHERE index_name LIKE '%working%';`
  2. Delete the old-engine index (the one without `.os`) via the API:
     `curl -s -u admin:admin -X DELETE http://localhost:8082/api/v1/esindex/<old-index-name>`
  3. Confirm in the old engine it is gone: `curl -s -u admin:admin http://localhost:9200/<old-index-name>` → 404.
  4. Confirm in the new engine it still exists: `curl -s -u admin:admin http://localhost:9201/<new-index-name.os>` → 200.
  5. Re-run the SQL from step 1.
- **Expected Result:**
  - The delete returns HTTP **200**.
  - Old-engine index is physically deleted (404).
  - New-engine index still exists (200).
  - The old-engine row is removed from `indicies`; the `.os` row is **unchanged**.
- **Type:** Manual

## TC-017 — Deleting an index that doesn't exist returns a clean 404

- **Objective:** Deleting a name that does not exist must return a readable 404 — not a stack trace —
  and must not change anything.
- **Risk:** Medium
- **Preconditions:** dotCMS running in any phase.
- **Steps:**
  1. `curl -s -i -u admin:admin -X DELETE http://localhost:8082/api/v1/esindex/working_totally_nonexistent_20990101`
  2. Run **H3** before and after to confirm no change.
- **Expected Result:**
  - HTTP **404**.
  - The response body is a readable error message (no raw stack trace).
  - The `indicies` table is unchanged.
- **Type:** Manual

## TC-018 — Deleting the currently active index is rejected

- **Objective:** The active working index is in use; the system must refuse to delete it.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 0**.
- **Steps:**
  1. Get the active working index: `curl -s -u admin:admin http://localhost:8082/api/v1/esindex/active/working`
  2. Attempt to delete it: `curl -s -i -u admin:admin -X DELETE http://localhost:8082/api/v1/esindex/<active-index-name>`
- **Expected Result:**
  - HTTP **4xx** (400 or 403).
  - The response explains that the active index cannot be deleted.
  - The index still exists in the engine; its `indicies` row is unchanged.
- **Type:** Manual

## TC-019 — Deleting a non-active index from the admin UI works end-to-end

- **Objective:** A non-active index deleted through the UI must disappear from the UI, from the engine,
  and from the database — with no error shown to the user.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create a spare (non-active) index: `curl -s -u admin:admin -X PUT http://localhost:8082/api/v1/esindex/create/shards/1`. Note the name.
  2. Go to **Admin → System → Index**, find the new index, click **Delete**, confirm.
  3. In Kibana: `GET /<new-index-name>` → expect 404.
  4. SQL: `SELECT * FROM indicies WHERE index_name = '<deleted_name>';`
- **Expected Result:**
  - The index disappears from the UI list.
  - It is physically deleted from the engine (404).
  - Its row is gone from `indicies`.
  - No error toast in the UI; no unhandled exception in the log.
- **Type:** Manual

## TC-020 — A full rebuild in Phase 1 rebuilds the old engine but preserves the new-engine index

- **Objective:** Triggering a full reindex must create a fresh old-engine index, but it must **not**
  recreate or wipe the new-engine index; dual-write must resume afterward.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1** with divergent index names (old-engine index without
  `.os`; new-engine index with a different timestamp and the `.os` suffix).
- **Steps:**
  1. Note the new-engine index name (ends in `.os`) and its document count (**H1**, **H4**).
  2. **Admin → System → Index → Reindex**. Wait for completion.
  3. In Kibana, identify the now-active old-engine index.
  4. In OpenSearch Dashboards, confirm whether the new-engine index is the same as before.
  5. Run **H3**.
  6. After the rebuild, publish a new content item, then check it landed in both engines (**H4** / search).
  7. Search the content UI for pre-rebuild items.
- **Expected Result:**
  - A new old-engine index with a fresh timestamp is created and activated; the previous old-engine
    index is deactivated/removed after the swap.
  - The new-engine index is the **same** one as before — same name, same document count.
  - New content written after the rebuild goes to the new old-engine index **and** the existing
    new-engine index (dual-write resumed).
  - `indicies` shows the new old-engine row and the **same** `.os` row.
  - Pre-rebuild content is still searchable in the UI.
- **Type:** Manual

---

# Group 7 — Index delete API: happy path and security

## TC-036 — DELETE of an index (happy path) removes it from the engine AND the database

- **Objective:** Calling the delete endpoint as an admin on a deletable (non-active) index must
  remove the index from the search engine **and** delete its row from the `indicies` table — the
  full end-to-end via API (TC-019 covers the same through the UI).
- **Risk:** High
- **Preconditions:** dotCMS running (Phase 0 or 1).
- **Steps:**
  1. Create a spare (non-active) index and note its name:
     `curl -s -u admin:admin -X PUT http://localhost:8082/api/v1/esindex/create/shards/1`
  2. Confirm it exists in the engine (**H1** / `_cat/indices`) and in the DB:
     `SELECT * FROM indicies WHERE index_name = '<created-name>';`
  3. Delete it: `curl -s -i -u admin:admin -X DELETE http://localhost:8082/api/v1/esindex/<created-name>`
  4. Confirm it is gone from the engine: `curl -s -u admin:admin http://localhost:9200/<created-name>` → 404
     (use `:9201` and the `.os` name if you created a new-engine index).
  5. Re-run the SQL from step 2.
- **Expected Result:**
  - The delete returns HTTP **200**.
  - The index is **physically gone** from the engine (404).
  - Its **row is removed** from the `indicies` table (the step-5 query returns 0 rows).
- **Note:** whether deleting one engine's index should also remove the `.os` counterpart is the open
  question tracked in **TC-016** — observe and report, don't assume.
- **Type:** Manual

## TC-021 — Deleting an index without credentials is rejected (401)

- **Objective:** The delete endpoint must require authentication.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. `curl -s -i -X DELETE http://localhost:8082/api/v1/esindex/some_index_name`  (no credentials)
- **Expected Result:**
  - HTTP **401**.
  - No index deleted; `indicies` unchanged.
- **Type:** Manual

## TC-022 — A logged-in non-admin user cannot delete an index (403)

- **Objective:** Only administrators may delete indexes; an authenticated non-admin must be refused.
- **Risk:** High
- **Preconditions:** A non-admin user (e.g. an Editor-role user) exists.
- **Steps:**
  1. `curl -s -i -u editor-user:password -X DELETE http://localhost:8082/api/v1/esindex/some_index_name`
- **Expected Result:**
  - HTTP **403**.
  - No index deleted; `indicies` unchanged.
- **Type:** Manual

---

# Group 8 — Same query, both engines: results must match

> These cases compare what each engine returns for the same query. Use Kibana for the old index and
> OpenSearch Dashboards for the new index, and compare the returned document ids.

## TC-023 — "Starts-with phrase" search returns equivalent results in both engines

- **Objective:** A `match_phrase_prefix` query (matches the beginning of a phrase) must return the
  same documents from both engines. Differences are a known migration gotcha and must be reported.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create and publish 10 Blog posts whose titles start with `Test article`.
  2. Find both working index names (**H1**).
  3. In Kibana (old) and OpenSearch Dashboards (new), run:
     `POST /<index>/_search { "query": { "match_phrase_prefix": { "blog.title": "Test art" } } }`
  4. Compare the document counts and the returned `_id` values.
- **Expected Result:**
  - The document **count is identical** in both engines.
  - Every `_id` returned by the old engine is also returned by the new engine.
  - If they differ: record it as a known gotcha and file a bug with the exact field and mapping.
- **Type:** Manual

## TC-024 — Restricted content is represented the same way in both engines

- **Objective:** Permissions are enforced by dotCMS, not by the index. Prove that a restricted item
  is present in **both** engines' indexes (with identical permission data), yet is hidden from an
  anonymous user going through the dotCMS API.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create a content item with restricted permissions (admin-only, not anonymous). Publish it; note its id.
  2. Query the old engine directly (bypasses permissions):
     `curl -s -u admin:admin "http://localhost:9200/<old-index>/_search?q=identifier:<id>"`
  3. Query the new engine directly:
     `curl -s -u admin:admin "http://localhost:9201/<new-index.os>/_search?q=identifier:<id>"`  (new index ends in .os)
  4. Query through dotCMS **as an anonymous user** (no credentials):
     `curl -s -i "http://localhost:8082/api/content/search/-query/+identifier:<id> -live false"`
- **Expected Result:**
  - Steps 2 and 3: the document is found in **both** engines.
  - The `permissions` field value is **identical** in both documents.
  - Step 4: anonymous gets HTTP 401 or an empty result — restricted content is not exposed.
- **Type:** Manual

## TC-025 — Full-text search returns the same set of documents in both engines

- **Objective:** A full-text `match` query must return the same set of documents from both engines.
  Relevance scores may differ, but the set must not.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create and publish 5 items of a content type with a long-text field (e.g. Blog `body`), all
     containing the phrase `quantum entanglement hypothesis`.
  2. Find both working index names (**H1**).
  3. In Kibana and OpenSearch Dashboards run:
     `POST /<index>/_search { "query": { "match": { "blog.body": "quantum entanglement" } } }`
  4. Compare the returned `_id` values.
- **Expected Result:**
  - The same documents are returned by both engines; counts match.
  - Scores may differ (acceptable); a differing result **set** must be filed as a bug.
- **Type:** Manual

---

# Group 12 — OpenSearch connection & phase behavior (additional)

## TC-037 — Both engines configured with the same address → migration is left off ("same endpoint" guard)

- **Objective:** Even if the address answers as a valid OpenSearch 3.x cluster, dotCMS must refuse to
  run the migration when the **new-engine address is the same as the old-engine address** (they must
  be separate instances). dotCMS keeps serving from ES; the migration is left off.
- **How to induce:** point both ES and OS at the **same OpenSearch 3.x instance**, e.g.
  `ES_ENDPOINTS=http://localhost:9201` and `OS_ENDPOINTS=http://localhost:9201`. (This is the one
  way to reach the separation guard; if you point OS at the ES port instead, the version check fails
  first — that is TC-008.)
- **Risk:** Medium
- **Preconditions:** `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`; ES and OS endpoints set to the same URL.
- **Steps:**
  1. Start dotCMS; read the startup log.
  2. Check the phase (**H2**) and DB (**H3**).
- **Expected Result:**
  - dotCMS starts and runs normally on the old engine.
  - The migration is left off; the log shows the expected error lines (H5) with reason
    `ES and OS clients point to the same endpoint(s): …. Set OS_ENDPOINTS to a separate OpenSearch instance.`
  - No `.os` rows in the database.
- **Type:** Manual

## TC-038 — OpenSearch keeps failing during index work → connection attempts are logged then give up

- **Objective:** Distinct from the startup config check: when OpenSearch is reachable enough to start
  the migration but then fails repeatedly during an index operation, dotCMS must log each retry and
  finally give up clearly, without hanging.
- **How to induce:** start in Phase 1 with OpenSearch up, then **stop OpenSearch** and trigger an
  index operation that targets it (e.g. create a new index, or publish content that forces an OS write).
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1** with OpenSearch initially reachable.
- **Steps:**
  1. Confirm Phase 1 is active (**H2**).
  2. Stop OpenSearch (`docker compose stop opensearch`).
  3. Trigger an index operation against the new engine and watch the log.
- **Expected Result:**
  - The log shows per-attempt errors similar to `OpenSearch Connection Attempt #N: <cause>` (ERROR),
    and finally `Cannot connect to OpenSearch, giving up.` (FATAL).
  - dotCMS does not hang; ES operations remain unaffected (in Phase 1 OS write failures are
    non-blocking).
- **Type:** Manual

## TC-039 — Phase 2: when an OpenSearch read fails, dotCMS falls back to ES (partial scope)

- **Objective:** In Phase 2 the new engine serves reads. Prove that if an OpenSearch read throws,
  dotCMS automatically retries the read against the old engine so the user still gets a correct
  result, and logs the failure for operators.
- **How to induce:** with dotCMS in Phase 2 and content present, make OpenSearch reads fail (e.g.
  stop OpenSearch, or delete/close the OS working index) and then run a content search.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 2**, content already dual-written.
  > Phase 2 is only partially testable in this plan (the higher-level query layer is not fully
  > migrated) — focus on the read-fallback behavior and the log line.
- **Steps:**
  1. Confirm content is searchable in Phase 2.
  2. Break OpenSearch reads (stop OS or remove the OS working index).
  3. Run the same search again.
- **Expected Result:**
  - The search still returns the correct result (served from ES).
  - The log shows an ERROR similar to
    `OS read failed in Phase 2 — falling back to ES. OS index may be stale or unavailable. Cause: …`
- **Type:** Manual

## TC-040 — Phase 3 does NOT auto-rollback (documentation / negative case — out of functional scope)

- **Objective:** Document that the automatic fallback to ES exists only in Phases 1–2. In Phase 3 (ES
  decommissioned) a failed OpenSearch startup validation must NOT silently roll back to ES; it fails
  loudly instead. **Phase 3 is not functionally testable in this plan — this case only pins the
  expected log/behavior so nobody assumes the Phase-1 fallback applies.**
- **Risk:** Low (informational)
- **Preconditions:** Would require Phase 3 — do not actually run; record expected behavior only.
- **Expected Result (documented):**
  - dotCMS does **not** reset to Phase 0. A `DotRuntimeException` is raised whose message starts with
    `OpenSearch startup validation failed in PHASE_3_OPENSEARCH_ONLY.` and the outer handler logs it
    as `FATAL Failed to create new indexes: …`.
- **Type:** Manual (documentation only)

---

# Group 13 — Divergent index names & fan-out error handling (known open issue)

> The migration doc flags this area as **unresolved — test coverage needed**. These cases document
> what actually happens today so the team can decide the intended behavior; expect to file findings
> as bugs rather than simple pass/fail.

## TC-041 — Operating with an index name that exists in only one engine

- **Objective:** In a real catchup deployment ES and OS hold indexes with **different timestamps**
  (and the `.os` tag). Observe how dotCMS behaves when an operation is driven by a name that exists
  in one engine but not the other — the migration doc explicitly lists this as untested.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1** with divergent names (catchup scenario: ES
  `working_<old_ts>`, OS `working_<new_ts>.os`).
- **Steps (observe each variant):**
  1. **Name exists only in ES:** drive an index operation with the ES name; watch both engines + log.
  2. **Name exists only in OS:** drive an operation with the OS (`.os`) name; watch both + log.
  3. **Name exists in neither:** drive an operation with a made-up name; watch the log.
- **Expected Result (observe & record — likely a bug report, not a clean pass):**
  - Record for each variant: HTTP/result, whether the wrong engine returns a 404 /
    `index_not_found_exception`, and whether that error is swallowed (fire-and-forget) or surfaced.
  - Compare against the doc's open question: should a "wrong index name" on OS in dual-write be
    swallowed, logged at ERROR, or propagated? File the observed behavior for the team to decide.
- **Type:** Manual

## TC-042 — Rollback during dual-write leaves the new engine ahead (drift)

- **Objective:** If dotCMS is rolled back to the previous version while in Phase 1/2, the old version
  stops writing to OpenSearch, but OpenSearch keeps whatever the newer version already pushed — so
  OS silently drifts ahead of ES. Prove the drift exists and that the documented runbook step (full
  OS reindex before re-activating) is needed.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1/2**, content/mapping dual-written; a previous build available.
- **Steps:**
  1. In Phase 1/2, add a field / publish content so OS receives mapping + data updates.
  2. Roll back dotCMS to the previous version (which has no dual-write).
  3. Make a few more content changes (now ES-only).
  4. Compare the OS working index against the ES working index (docs + mapping).
- **Expected Result:**
  - The OS index reflects the pre-rollback updates but **not** the ES-only changes made after
    rollback — i.e. OS is ahead/divergent.
  - Confirms the runbook: re-activating the migration without an OS resync would serve inconsistent
    data. Record as a documented operational risk.
- **Type:** Manual

---

# Group 14 — Index settings & content-sync parity (additional)

## TC-043 — New-engine index has `number_of_replicas` set explicitly

- **Objective:** A known OpenSearch gotcha: `number_of_replicas` must be set explicitly on OS index
  settings. Prove the migration-created OS index has it set (not left to a default).
- **Risk:** Low
- **Preconditions:** dotCMS in **Phase 1**; new-engine index created.
- **Steps:**
  1. Find the OS working index name (**H1**, ends in `.os`).
  2. `curl -s -u admin:admin http://localhost:9201/<new-index.os>/_settings | jq .`
- **Expected Result:**
  - The settings include an explicit `number_of_replicas` value (present, not absent/defaulted).
- **Type:** Manual

## TC-044 — Changing permissions on published content updates both engines

- **Objective:** The doc lists "permission update" as an operation that must replicate to OpenSearch.
  Prove that editing an item's permissions updates the `permissions` field in **both** indexes.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1**; a published item exists.
- **Steps:**
  1. Note the item's identifier and its current `permissions` value in both engines (direct `_doc`).
  2. Change the item's permissions in the admin UI and save/publish.
  3. Wait ~5s, then re-read the document in both engines.
- **Expected Result:**
  - The `permissions` field is updated in **both** the ES and OS documents, to the same value.
- **Type:** Manual

## TC-045 — A draft (unpublished) item appears in the *working* index of both engines, not *live*

- **Objective:** Prove the working/live distinction holds across dual-write: a saved-but-unpublished
  item must be in the working index of both engines and absent from the live index of both.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 1**.
- **Steps:**
  1. Create and **save (do not publish)** a content item; note its identifier.
  2. Find the working and live index names for both engines (**H1**).
  3. Look the document up by identifier in all four indexes (ES working/live, OS working/live).
- **Expected Result:**
  - Present in **ES working** and **OS working**.
  - Absent from **ES live** and **OS live**.
- **Type:** Manual

## TC-046 — Multi-language content is dual-written for each language

- **Objective:** Prove that a content item with two language versions produces a document per
  language (`languageid`) in **both** engines.
- **Risk:** Low
- **Preconditions:** dotCMS in **Phase 1**; at least two languages configured.
- **Steps:**
  1. Create and publish an item in language A, then add and publish a language-B version (same identifier).
  2. Find both working index names (**H1**).
  3. In Kibana and OpenSearch Dashboards, search by identifier and inspect `languageid`.
- **Expected Result:**
  - Both engines contain **two documents** for that identifier, one per `languageid`.
  - The set of (identifier, languageid) pairs matches between engines.
- **Type:** Manual

---

# Group 15 — Full reindex across migration phases

> What this group proves: a user-triggered **full reindex** rebuilds the search index from the
> database and must (a) target the right engine(s) for the current phase, (b) finish **complete**
> (every expected document present), and (c) **not hang**. **Desired behavior (decision B):** in
> Phase 1/2 a full reindex must repopulate **both** engines.
>
> ⚠️ **Known divergence from current design.** `OPENSEARCH_MIGRATION.md` today says a user-triggered
> full reindex rebuilds **only ES** in Phase 1/2 (full OS reindex is deferred as "not viable at
> scale"). TC-048/TC-049 below assert the **desired** behavior (both engines); if the current build
> only rebuilds ES, that is the expected outcome **for now** — record it as a **gap/bug to fix**, do
> not silently pass. TC-016 in Group 6 tracks a related index-lifecycle question.

## TC-047 — Phase 0: full reindex rebuilds ES only and completes (baseline)

- **Objective:** With the migration off, a full reindex must rebuild the ES index completely and
  never touch / contact the new engine.
- **Risk:** Medium
- **Preconditions:** dotCMS in **Phase 0** with a known amount of published + draft content.
- **Steps:**
  1. Record the expected content counts (see TC-051 for the queries).
  2. **Admin → System → Index → Reindex.** Wait for completion.
  3. Watch the reindex progress in the UI and the log; check **H4** counts after.
- **Expected Result:**
  - The reindex completes (no hang — see TC-052); a fresh ES index is created and activated.
  - Document counts match the expected content (TC-051).
  - **No** OS connection attempt in the log; no `.os` index created or changed.
- **Type:** Manual

## TC-048 — Phase 1: full reindex must repopulate BOTH engines (decision B)

- **Objective:** In Phase 1 a full reindex must rebuild **both** the ES and the OS index so the new
  engine ends up complete, not just ES.
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 1** with content already present in both engines.
- **Steps:**
  1. Record expected counts (TC-051) and note both current index names (**H1**).
  2. Trigger a full reindex (Admin → System → Index → Reindex). Wait for completion.
  3. After completion, check document counts in **both** engines (**H4**) and that the journal drained (TC-051).
- **Expected Result (desired — decision B):**
  - Both the ES **and** the OS index are rebuilt/repopulated and reach the expected document count.
  - The reindex completes without hanging (TC-052).
- **⚠️ Current-design note:** if this build only rebuilds ES and leaves OS untouched/stale (the
  documented "OS-excluded" behavior), **record it as a gap/bug** with the observed OS count — do not
  pass the case.
- **Type:** Manual

## TC-049 — Phase 2: full reindex must repopulate BOTH engines (decision B)

- **Objective:** Same requirement as TC-048, in Phase 2 (where OS also serves reads, so an
  incomplete OS index would surface directly to users).
- **Risk:** High
- **Preconditions:** dotCMS in **Phase 2** with content present in both engines.
  > Phase 2 is only partially testable in this plan; focus on which engines get rebuilt and on completeness.
- **Steps:** Same as TC-048, run in Phase 2.
- **Expected Result (desired — decision B):**
  - Both engines rebuilt to the expected count; reindex completes without hanging.
  - Because reads come from OS in Phase 2, a search after reindex returns the full expected result set.
- **⚠️ Current-design note:** same as TC-048 — ES-only rebuild is a gap to record, not a pass.
- **Type:** Manual

## TC-050 — Phase 3: full reindex rebuilds OS only

- **Objective:** With ES decommissioned, a full reindex must rebuild **only** the OS index and reach
  full completeness.
- **Risk:** High
- **Preconditions:** Would require **Phase 3** (not fully testable in this plan — the OS write path
  / full-reindex orchestration is deferred). Run only if a Phase 3 environment is available.
- **Steps:**
  1. Record expected counts (TC-051).
  2. Trigger a full reindex. Watch the log closely for the known failure modes.
  3. Check the OS index count and that ES is not written to.
- **Expected Result:**
  - The OS index is rebuilt to the expected count; ES is not touched.
  - Reindex completes without hanging.
- **⚠️ Known issues to watch (likely failures):** orphaned ES rows left behind (#36077) and a hang /
  exception when `VersionedIndices` is empty so the index to hit can't be resolved (#36054). If
  either occurs, capture it per TC-052 and link the issue.
- **Type:** Manual

## TC-051 — Reindex completeness check (all phases)

- **Objective:** Independent of which engine, prove a finished reindex left **no missing or duplicate
  documents** — the index matches the database.
- **Risk:** High
- **Preconditions:** A completed reindex from any of TC-047–TC-050.
- **Steps:**
  1. Get the expected counts from the DB, e.g.:
     - working index ≈ all indexable versions: `SELECT count(*) FROM contentlet_version_info;`
     - live index ≈ published only: `SELECT count(*) FROM contentlet_version_info WHERE live_inode IS NOT NULL;`
     (adjust to the project's exact indexable-content definition.)
  2. Get the index doc counts (**H4**) for the working and live indexes of each target engine.
  3. Confirm the reindex queue fully drained: `SELECT count(*) FROM dist_reindex_journal;` → expect 0.
  4. Spot-check a few identifiers exist in the index (no silently dropped docs).
- **Expected Result:**
  - Index counts match the expected DB counts (within the documented indexable-content rules).
  - `dist_reindex_journal` is empty (no stuck/pending entries).
  - No duplicates (same identifier+languageid appearing twice in one index).
- **Type:** Manual

## TC-052 — Reindex must not hang — and if it does, capture why

- **Objective:** A full reindex must finish in a reasonable time. If it stalls, the tester must be
  able to determine **where and why** so the cause can be filed.
- **Risk:** High
- **Preconditions:** A reindex in progress (any phase).
- **Steps (if the reindex does not complete / appears stuck):**
  1. Check the reindex queue for stuck work: `SELECT count(*), min(serverid) FROM dist_reindex_journal;`
     — a count that stays > 0 and never decreases indicates a stall.
  2. Check the dotCMS log for repeating errors: OpenSearch connection retries
     (`OpenSearch Connection Attempt #N` … `Cannot connect to OpenSearch, giving up.`), or an
     index-resolution failure (`inferIndexToHit` / empty `VersionedIndices`), or bulk errors.
  3. Capture a thread dump of the dotCMS JVM to see whether reindex worker threads are blocked/waiting.
  4. Note the phase and which engine the stall is associated with.
- **Expected Result:**
  - **Healthy:** the reindex completes; the journal drains to 0; no repeating error loop.
  - **If stuck:** the tester produces a clear cause (stuck journal rows + the specific log signature
    + thread state) and files it. Common suspects to confirm: OS connectivity give-up, empty
    `VersionedIndices` in Phase 3 (#36054), orphaned rows (#36077).
- **Type:** Manual

---

# Group 16 — Limited (non-admin) OpenSearch user

> What this group proves: dotCMS can run the migration against an OpenSearch 3.x cluster reached
> through a **restricted, non-admin user** (the managed-cloud setup validated in spike #35922) — not
> only an open, security-disabled cluster. It covers provisioning that user, pointing dotCMS at it,
> and confirming index operations work through the limited role.

## Setup — limited-user stack and the provisioning script

**Stack:** `docker/docker-compose-examples/os-migration/docker-compose.limited-user.yml` (OS 3.x with
the security plugin ON, provisioned with the non-admin `dotcms-es-user`). The
`docker/docker-compose-examples/single-node-os-migration/` variant runs OS 1.x + OS 3.x, both
provisioned the same way.

```bash
docker compose -f docker/docker-compose-examples/os-migration/docker-compose.limited-user.yml up -d
```

**Users on OS 3.x (port 9201, HTTPS):**

| User | Password | Role |
|---|---|---|
| `admin` | `Dev!Search3-Kx9mP-2026` | cluster admin |
| `dotcms-es-user` | `Dev!dotcms-EsUser-2026` | `dotcms-role` (limited) |

**Provisioning script `opensearch.py`** — runs once via the `opensearch-provision` service; creates
per customer an internal user `<customer>-es-user`, action groups, a role `<customer>-role`, and the
role mapping. Run it manually:

```bash
# Re-run the bundled provisioner against the running stack (idempotent):
docker compose -f docker/docker-compose-examples/os-migration/docker-compose.limited-user.yml run --rm opensearch-provision

# Or standalone against any reachable cluster:
./opensearch.py --admin-user admin --admin-pass 'Dev!Search3-Kx9mP-2026' \
  --password 'Dev!dotcms-EsUser-2026' --customer dotcms --host localhost --port 9201
```

The `dotcms-role` grants:
- **Index** (on pattern `cluster_<customer>*`): `indices_all`, `indices_monitor`
- **Cluster:** `cluster:monitor/health`, `cluster:monitor/state`, `cluster:monitor/nodes/stats`,
  `indices:data/write/bulk`, `indices:data/read/scroll`, `indices:data/read/scroll/clear`, and
  **`cluster:monitor/main`** (the GET `/` version probe — the spike #35922 fix; without it dotCMS
  misreads OS as unreachable)
- **All-indices:** `indices:monitor/stats`, `indices:monitor/settings/get`, `indices:admin/aliases/get`

⚠️ **Cluster-id gotcha:** the role only grants index permissions on names matching
`cluster_<customer>*`. dotCMS names its indexes `cluster_<DOT_DOTCMS_CLUSTER_ID>.*`, so
**`DOT_DOTCMS_CLUSTER_ID` must start with the customer name** (e.g. `dotcms-os-migration`) or the
limited user gets 403 on its own indexes.

**dotCMS config to talk to OS as the limited user:**

```properties
DOT_ES_ENDPOINTS=http://<host>:9200
DOT_OS_ENDPOINTS=https://<host>:9201
DOT_OS_AUTH_TYPE=BASIC
DOT_OS_AUTH_BASIC_USER=dotcms-es-user
DOT_OS_AUTH_BASIC_PASSWORD=Dev!dotcms-EsUser-2026
DOT_OS_TLS_TRUST_SELF_SIGNED=true
DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE=1
DOT_DOTCMS_CLUSTER_ID=dotcms-os-migration   # MUST start with the customer name
```

## TC-053 — Provision the limited user and verify the role/permissions

- **Objective:** the script creates the non-admin user, role, and mapping with the expected permissions.
- **Risk:** Medium
- **Steps:**
  1. Launch the limited-user stack; let `opensearch-provision` finish.
  2. List internal users:
     `curl -sk https://localhost:9201/_plugins/_security/api/internalusers?pretty -u admin:'Dev!Search3-Kx9mP-2026'` → `dotcms-es-user` present.
  3. Inspect the role:
     `curl -sk https://localhost:9201/_plugins/_security/api/roles/dotcms-role?pretty -u admin:'Dev!Search3-Kx9mP-2026'`.
  4. Confirm the limited user reaches the cluster root:
     `curl -sk https://localhost:9201/ -u dotcms-es-user:'Dev!dotcms-EsUser-2026'` → HTTP 200.
- **Expected Result:**
  - `dotcms-es-user` and `dotcms-role` exist; the role grants the index/cluster/all-index permissions
    listed in Setup, **including `cluster:monitor/main`**.
  - `GET /` as the limited user returns **200** (not 403).
- **Type:** Manual

## TC-054 — dotCMS starts against OS as the limited user; migration NOT disabled

- **Objective:** with the limited user, dotCMS's startup version/reachability probe (`GET /`) succeeds,
  so the migration stays ON — validates the #35922 grant.
- **Risk:** High
- **Preconditions:** limited-user stack up; dotCMS configured as in Setup (Phase 1, cluster id starts with `dotcms`).
- **Steps:**
  1. Start dotCMS; watch the startup log.
  2. Confirm the phase (Admin → System → Index, or the log).
- **Expected Result:**
  - dotCMS starts and runs normally; the migration is **NOT** auto-disabled — none of the
    "OpenSearch cluster is not reachable" / phase-reset lines appear.
  - The log shows `OS version check passed: 3.x` (or similar).
  - Both ES and OS (`.os`) indexes are listed → Phase 1 active.
- **Type:** Manual

## TC-055 — Dual-write works through the limited role (Phase 1)

- **Objective:** publishing content writes to the OS 3.x index using the limited user's
  `indices:data/write/bulk` on `cluster_dotcms*`.
- **Risk:** High
- **Preconditions:** TC-054 passed.
- **Steps:**
  1. Create and publish a content item; note its identifier.
  2. As the limited user, confirm it in OS (find the index name first, ends in `.os`):
     `curl -sk "https://localhost:9201/<cluster_dotcms….working_….os>/_doc/<id>" -u dotcms-es-user:'Dev!dotcms-EsUser-2026'`.
- **Expected Result:**
  - The document is present in the OS 3.x working index (200/found).
  - No permission (403) errors in the dotCMS log for the OS write.
- **Type:** Manual

## TC-056 — Cluster-id mismatch → limited user gets 403 on its own indexes (gotcha)

- **Objective:** prove the cluster id must match the role pattern; a mismatched id breaks OS ops with 403.
- **Risk:** Medium
- **Preconditions:** limited-user stack up.
- **Steps:**
  1. Configure dotCMS with a cluster id that does NOT start with the customer name, e.g.
     `DOT_DOTCMS_CLUSTER_ID=acme-test`. Start dotCMS in Phase 1.
  2. Publish content; watch the dotCMS log and try an OS write/read as the limited user.
- **Expected Result:**
  - OS operations on `cluster_acme-test.*` fail with **403** (outside the `cluster_dotcms*` grant);
    dotCMS logs the OS write failure (fire-and-forget in Phase 1 — ES unaffected).
  - Fix: set `DOT_DOTCMS_CLUSTER_ID` to start with `dotcms` (or re-provision with a matching
    `--customer`) → the 403 disappears.
- **Type:** Manual

## TC-057 — Read/search works through the limited user

- **Objective:** the limited role's scroll/read permissions are sufficient for dotCMS searches against OS.
- **Risk:** Medium
- **Preconditions:** content dual-written (TC-055); to exercise OS reads use Phase 2 (partial scope) or query OS directly.
- **Steps:**
  1. As the limited user, search the OS working index:
     `curl -sk "https://localhost:9201/<…os>/_search?q=*:*" -u dotcms-es-user:'Dev!dotcms-EsUser-2026'`.
  2. (Phase 2) run a dotCMS content search and confirm results.
- **Expected Result:**
  - Search returns results (no 403); scroll-based reads succeed.
- **Type:** Manual

## TC-058 — (Regression) Without `cluster:monitor/main`, dotCMS misreads OS as unreachable

- **Objective:** confirm the #35922 grant is required — removing it must reproduce the original
  failure (OS misclassified as unreachable → migration disabled).
- **Risk:** Medium
- **Preconditions:** limited-user stack up.
- **Steps:**
  1. Remove `cluster:monitor/main` from `dotcms-role` (edit the role via the security API, or
     re-provision a role without it).
  2. Confirm `GET /` as the limited user now returns **403**.
  3. Restart dotCMS in Phase 1 against this user; watch the startup log.
- **Expected Result:**
  - dotCMS's version probe fails → the migration is auto-disabled (the "OpenSearch cluster is not
    reachable" + phase-reset lines, see Group 1 / H5), and dotCMS falls back to ES.
  - Restoring `cluster:monitor/main` (re-run the provisioner) fixes it → TC-054 passes again.
- **Type:** Manual

---

## Bugs to Validate

| Bug | Issue | PR | Status | QA Action |
|---|---|---|---|---|
| OS exceptions propagated to caller in Phase 1 | #35302 | #35389 | Merged | TC-038 / TC-056 — OS write failure must stay fire-and-forget (never reach the caller) |
| `getClosedIndexes()` stub returned empty list | #35304 | #35352 | Merged | Covered indirectly by TC-019 / TC-036 (list + delete a non-active index); no dedicated closed-index case in this plan |
| `putMapping` 400 on OS 3.4.0 | #35305 | #35349 | Merged | TC-012 / TC-043 — OS index creation does not 400; correct settings JSON loaded |
| `DELETE /v1/esindex` leaves orphaned DB row | #35306 | #35342 | Merged | TC-036 / TC-019 — delete index, verify `indicies` row removed |
| TOCTOU race on BulkProcessor rebuild | #35313 | #35323 | Merged | TC-005 (concurrent creates) + a concurrent reindex+publish load run; no NPE or stale processor |
| `OSBulkHelper.getIndexName()` always returned `dotcms_content` | #35314 | #35390 | Merged | TC-010 / TC-055 — published content reaches the correct OS index name |
| Phase 3 re-creates ES indices | #35356 scope | #35356 | Merged | TC-050 / TC-040 — ES must not be touched in Phase 3 |
| OS connectivity failure at startup allowed dotCMS to boot in Phase ≥ 1 without shutting the migration off | #35631 | #35632 | Merged | TC-001, TC-006, TC-007 — automatic migration shutdown must fire (two `ERROR` + one `WARN`, no `FATAL`) |
| `hasEmptyIndices()` triggered false ES reindex when OS index was legitimately empty in Phase 1 | #35631 | #35632 | Merged | TC-004 — no reindex triggered on Phase 0→1 transition |
| `SimpleDateFormat` race condition in `ESIndexResource` produces corrupted index names | #35631 | #35632 | Merged | TC-005 — 20 concurrent index creates produce 20 unique valid names |

---

## Suggested execution order

1. TC-003 (Phase 0 baseline) → TC-004 (no false rebuild) → TC-001 (OpenSearch down at startup)
2. TC-006, TC-007, TC-008 (invalid OS_ENDPOINTS values — incl. wrong version, formerly TC-002)
3. TC-005 (concurrent index creation)
4. TC-009, TC-010, TC-011 (two nodes)
5. TC-012, TC-013, TC-014 (searchable fields & cascade delete)
6. TC-015 (content delete)
7. TC-036 (delete happy path) → TC-016, TC-017, TC-018, TC-019, TC-020 (index delete & rebuild)
8. TC-021, TC-022 (delete API security)
9. TC-023, TC-024, TC-025 (cross-engine query equivalence)
10. TC-037 (same-endpoint guard), TC-038 (connection give-up), TC-039 (Phase 2 fallback), TC-040 (Phase 3 doc-only)
11. TC-041 (divergent-name fan-out — open issue), TC-042 (rollback drift)
12. TC-043 (replicas setting), TC-044 (permission update), TC-045 (draft working/live), TC-046 (multi-language)
13. TC-047 (Phase 0 reindex baseline) → TC-048 (Phase 1 both) → TC-049 (Phase 2 both) → TC-050 (Phase 3 OS-only); for each run TC-051 (completeness) and TC-052 (no-hang / diagnose)
14. TC-053 (provision limited user) → TC-054 (dotCMS starts as limited user) → TC-055 (dual-write) → TC-056 (cluster-id 403 gotcha) → TC-057 (read/search) → TC-058 (regression: no cluster:monitor/main)

> Numbering note: TC-002 was merged into TC-008. New cases are TC-036–TC-058. The gap at
> **TC-026–TC-035 is intentional** — those IDs belong to QA-G10 (#35752, SearchAPI phase routing),
> a separate workstream under the same epic (#35476). Mapping to the epic: G1–G8 = #35635–#35642;
> G12=#36218, G13=#36219, G14=#36220; G15 = reindex (TC-047–TC-052); G16 = limited-user (TC-053–TC-058).
>
> Lower-confidence / out-of-scope flags: TC-039 & TC-040 (Phase 2/3 — partial/doc-only),
> TC-041 & TC-042 (known open issue — expect bug reports, not clean pass/fail),
> TC-048 & TC-049 (assert DESIRED reindex-to-both per decision B — current build may rebuild ES only;
> record as gap), TC-050 (Phase 3 — needs Phase 3 env; known bugs #36077/#36054).
