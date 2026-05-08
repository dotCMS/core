# QA Test Plan — ES → OpenSearch Migration

**Issues:** [#34934](https://github.com/dotCMS/core/issues/34934) · [#34164](https://github.com/dotCMS/core/issues/34164)  
**Epic:** [#34153 — Migrate Core Infrastructure Classes](https://github.com/dotCMS/core/issues/34153)  
**Scope:** ElasticSearch → OpenSearch dual-write and read migration  
**Status:** Phases 0 and 1 are fully testable. Phase 2 dual-write is testable — every ES write is mirrored to OS and can be verified via OpenSearch Dashboards — but the dotCMS query layer has **not yet been migrated**, so application-level read validation is out of scope for Phase 2. Phase 3 is **not testable** at this time: the OS-only read path depends on the same unmigrated query layer and has no ES fallback; activating Phase 3 in a test environment would produce broken reads in the application.

---

## 1. Objective

Validate that the ES → OpenSearch migration pipeline delivers:

1. **Zero regression** on ES-backed functionality (Phase 0 / no flag set).
2. **Correct dual-write** in Phase 1 — every ES write is mirrored to OS; failures on OS are fire-and-forget (never affect the business operation).
3. **Correct dual-read** in Phase 2 — reads switch to OS; field mappings are structurally equivalent to ES; record counts match between the two indices.
4. **Correct index lifecycle** across all phases — activate, deactivate, delete, and reindex operations behave consistently and keep the `indicies` DB table in sync.
5. **Safe failure modes** — dotCMS starts gracefully when one backend is unavailable, depending on the active phase.
6. **REST API parity** — `/v1/esindex` endpoints behave correctly in both single-backend and dual-write phases.

---

## 2. Environment Setup

### 2.1 Docker Compose Stack

The migration test stack launches ES 7.10 + Kibana and OpenSearch 3.x + Dashboards in the same Docker network.

```bash
docker compose -f docker/docker-compose-examples/os-migration/docker-compose.yml up -d
```

| Service               | URL                        | Purpose                          |
|-----------------------|----------------------------|----------------------------------|
| Elasticsearch 7.10    | http://localhost:9200      | ES REST API                      |
| Kibana                | http://localhost:5601      | ES index inspection / queries    |
| OpenSearch 3.x        | http://localhost:9201      | OS REST API                      |
| OpenSearch Dashboards | http://localhost:5602      | OS index inspection / queries    |

> Wait for all four health checks to pass before starting dotCMS. Check with:
> ```bash
> curl -s http://localhost:9200/_cluster/health | jq .status
> curl -s http://localhost:9201/_cluster/health | jq .status
> ```

### 2.2 dotCMS Configuration

All properties below go in `dotmarketing-config.properties` (or `dotcms-config-cluster.properties`).  
Full reference: [`docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md`](../backend/OPENSEARCH_CLIENT_CONFIGURATION.md)

#### Minimal OS connection (no security)

```properties
OS_ENDPOINTS=http://localhost:9201
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
OS_TLS_ENABLED=false
```

#### Phase flag

```properties
# 0 = ES only (default)   1 = dual-write, ES reads   2 = dual-write, OS reads   3 = OS only
FEATURE_FLAG_OPEN_SEARCH_PHASE=0
```

> The flag is read at startup **and** on each routing decision. A restart is not required for the change to take effect, but all cluster nodes must be updated consistently.

---

## 3. Migration Phase Reference

Full specification: [`docs/backend/OPENSEARCH_MIGRATION.md`](../backend/OPENSEARCH_MIGRATION.md)

| Phase | Writes     | Reads      | ES Status            | OS Write Failures        |
|-------|------------|------------|----------------------|--------------------------|
| 0     | ES only    | ES only    | Active               | n/a                      |
| 1     | ES + OS    | ES only    | Active               | Fire-and-forget (logged) |
| 2     | ES + OS    | OS only    | Active (fallback)    | Fire-and-forget (logged) |
| 3     | OS only    | OS only    | Decommissioned       | Propagate to caller      |

**Testable scope for this sprint:**

| Phase | Testable? | Limitation |
|-------|-----------|------------|
| 0 | Yes — fully | No dependencies beyond existing ES stack |
| 1 | Yes — fully | Dual-write verified via Kibana + OS Dashboards |
| 2 | Partially | Dual-write testable via OS Dashboards; application-level reads **not testable** — dotCMS query layer not yet migrated |
| 3 | No | Query layer not migrated; OS-only reads are broken; no ES fallback — activating Phase 3 produces broken application reads |

---

## 4. Key Classes Under Test

These classes were introduced or modified by the PRs in scope:

| Class | PR(s) | What Changed |
|---|---|---|
| `ContentletIndexAPIImpl` | #35289, #35356, #35389 | Phase-aware routing, Phase 3 guards, fire-and-forget enforcement |
| `ESMappingAPIImpl` | #35123, #35275 | PhaseRouter migration, `putMapping(List, String, IndexTag)` overload |
| `IndexAPIImpl` | #35123, #35275 | Full PhaseRouter migration |
| `OSIndexAPIImpl` | #35352, #35349 | `getClosedIndexes()` fix, correct settings JSON loaded |
| `OSBulkHelper` | #35390 | `getIndexName()` now queries `VersionedIndicesAPI` instead of returning fallback |
| `ReindexThread` | #35323 | AtomicReference + ConcurrentHashMap for race-free BulkProcessor rebuild |
| `BulkProcessorListener` | #35323, #35356, #35389 | Phase-aware primary `IndexTag`, shadow fire-and-forget contract |
| `ReindexEntry` | #35391 | Immutable value object (Immutables) |
| `VersionedIndicesAPIImpl` | #35289 | `os::` tag management for DB row uniqueness |

---

## 5. Scenario A — Regression (Phase 0 / No Phase Set)

**Goal:** Confirm that existing ES functionality is completely unaffected by the migration work.

### A-1 — Fresh dotCMS startup, no OS flag

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start dotCMS with **no** `FEATURE_FLAG_OPEN_SEARCH_PHASE` set (or set to `0`). OS stack may be running or not. | dotCMS starts normally. Startup log shows `Phase 0`. No OS connection attempted. |
| 2 | Verify ES indices in Kibana: `GET /_cat/indices?v` | Working and live indices present. |
| 3 | Create a content item and publish it. | Content appears in ES (visible in Kibana). |
| 4 | Run a full reindex from the dotCMS admin panel. | Reindex completes. Record count in ES matches the contentlet count. No OS writes in logs. |
| 5 | Confirm no `os::` tagged rows in the `indicies` DB table. | `SELECT * FROM indicies` shows only ES index names (no `os::` prefix). |

### A-2 — All existing integration tests pass

- Run `ContentletIndexAPIImplTest` and `ReindexQueueAPITest` against Phase 0.
- Expected: all green.

---

## 6. Scenario B — Phase 1 (Dual-Write, ES Reads)

**Goal:** Every ES write is mirrored to OS. Reads still come from ES. OS failures do not affect the caller.

### Setup

```properties
FEATURE_FLAG_OPEN_SEARCH_PHASE=1
OS_ENDPOINTS=http://localhost:9201
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
OS_TLS_ENABLED=false
```

Start dotCMS. Confirm startup log shows `Migration Phase: PHASE_1_DUAL_WRITE_ES_READS`.

### B-1 — Dual-write on content publish

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Create and publish a new content item. | ES write succeeds. OS write fires in the background. |
| 2 | Check Kibana → ES index: search for the new content. | Document found in ES. |
| 3 | Check OpenSearch Dashboards → OS index: search for the same content. | Document found in OS. |
| 4 | Compare field structure (mapping) between the two documents. | Fields match (same names and values). |

### B-2 — Mapping structure comparison (ES vs OS)

Use Kibana DevTools and OpenSearch Dashboards DevTools.

```
# ES — Kibana DevTools
GET /cluster_<id>.working_<timestamp>/_mapping

# OS — OpenSearch Dashboards DevTools  
GET /cluster_<id>.working_<timestamp>/_mapping
```

| Validation | Expected |
|---|---|
| `catchall` field present in both | Yes |
| `*_dotraw` keyword fields present | Yes |
| `permissions` whitespace-analyzed text field | Yes |
| `identifier`, `inode`, `basetype`, `languageid` fields | Yes |
| Dynamic fields for each content type | Yes, in both |

> Flag any field present in ES but absent in OS (or vice versa) as a mapping drift bug.

### B-3 — Index record count comparison

```bash
# ES
curl -s "http://localhost:9200/cluster_<id>.working_<timestamp>/_count" | jq .count

# OS
curl -s "http://localhost:9201/cluster_<id>.working_<timestamp>/_count" | jq .count
```

Expected: counts are equal (or within an acceptable delta if content was created between the two checks).

### B-4 — OS shadow write failure is fire-and-forget

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop the OS container: `docker compose stop opensearch` | OS unavailable. |
| 2 | Publish a content item in dotCMS. | Publish **succeeds**. Content is visible in dotCMS. |
| 3 | Check dotCMS logs. | A `WARN` (or configurable level) log entry shows the OS shadow write failure. No ERROR propagated to caller. |
| 4 | Restart OS: `docker compose start opensearch` | OS comes back online. |
| 5 | Confirm ES content still works normally. | ES reads still succeed. |

> **Config:** The log level for shadow write failures is controlled by `DOTCMS_SHADOW_WRITE_LOG_LEVEL` (default: `WARN`).

### B-5 — Reindex in Phase 1

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Trigger a full reindex from dotCMS Admin → Index → Reindex. | Reindex starts. A new ES index is created. OS is **not** reindexed (by design — see migration doc). |
| 2 | Monitor Kibana: new working index with new timestamp appears. | New index visible in Kibana. |
| 3 | Monitor OS Dashboards: **no new OS index** created. | OS keeps the old index. This is the accepted behavior. |
| 4 | After reindex completes, publish a new content item. | New item appears in the new ES index AND in the OS index (dual-write resumes). |

### B-6 — dotCMS startup in Phase 1 with ES unavailable

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop ES: `docker compose stop elasticsearch`. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`. | — |
| 2 | Start dotCMS. | dotCMS **fails to start** (ES is the primary in Phase 1). Startup log shows a connectivity error to ES after `OS_CONNECTION_ATTEMPTS` retries. |

### B-7 — dotCMS startup in Phase 1 with OS unavailable

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop OS: `docker compose stop opensearch`. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`. | — |
| 2 | Start dotCMS. | dotCMS **starts** — OS is the shadow in Phase 1 and ES is still the primary. Startup log shows an OS connectivity warning but does not block. |
| 3 | Publish a content item. | Publish succeeds. ES write succeeds. OS write fails silently (logged at `WARN` level). |
| 4 | Restart OS: `docker compose start opensearch`. Publish another item. | OS shadow write succeeds again. Both indices receive the write. |

### B-8 — Phase 0 → Phase 1 transition (first activation of dual-write)

**Context:** This is the real-world first step of migration. dotCMS has been running in Phase 0 with an existing ES index. The operator switches to Phase 1 — dotCMS must create the OS index for the first time and resume dual-write without any data loss or downtime.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start dotCMS in **Phase 0**. Create and publish several content items. | ES index populated. `indicies` table has one row per logical index (no `os::` rows). Kibana shows the content. |
| 2 | Note the current ES index name and timestamp. | e.g. `cluster_<id>.working_20260101120000` |
| 3 | Stop dotCMS. Change `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`. Restart dotCMS. | dotCMS starts. Startup log shows `Phase 1`. |
| 4 | Check dotCMS startup log for OS index creation. | Log shows a new OS index being created with a **new** timestamp (different from the ES index timestamp). |
| 5 | Check `indicies` DB table. | Now has `os::` prefixed rows alongside the existing ES rows. Timestamps differ between ES and OS rows. |
| 6 | Check OS Dashboards: `GET /_cat/indices?v` | New OS index visible. Initially empty (content written before Phase 1 is not backfilled — this is by design). |
| 7 | Publish a new content item. | Item appears in **both** ES (existing old index) and OS (new index). Verify in Kibana and OS Dashboards. |
| 8 | Confirm the pre-Phase-1 content is still searchable via the dotCMS UI. | Yes — reads still come from ES in Phase 1, so existing content is unaffected. |

---

## 7. Scenario C — Phase 2 (Dual-Write, OS Reads — Partial)

> **What is testable in Phase 2:** The dual-write pipeline is fully active — every write that goes to ES is also mirrored to OS. These shadow writes can be verified directly via OpenSearch Dashboards and the OS REST API. Record counts, document structure, and field mappings can all be compared between the two backends using those tools.
>
> **What is NOT testable in Phase 2:** The dotCMS application query layer (search API, content search UI) has **not yet been migrated** to read from OS. Even with the phase flag set to `2`, application-level queries still resolve against ES internally. There is no end-to-end path in the application that currently exercises OS as a read source. Do **not** attempt to validate OS reads through the dotCMS UI or REST search endpoints in this phase — results will silently come from ES, making the test meaningless.
>
> **Workaround for read validation:** Use OpenSearch Dashboards DevTools (`http://localhost:5602`) to query the OS index directly and compare with Kibana (`http://localhost:5601`). This is the correct validation surface for Phase 2 write correctness.

### Setup

```properties
FEATURE_FLAG_OPEN_SEARCH_PHASE=2
OS_ENDPOINTS=http://localhost:9201
OS_AUTH_TYPE=BASIC
OS_AUTH_BASIC_USER=admin
OS_AUTH_BASIC_PASSWORD=admin
OS_TLS_ENABLED=false
```

Restart dotCMS. Confirm startup log shows `Migration Phase: PHASE_2_DUAL_WRITE_OS_READS`.

### C-1 — Cross-tool query equivalence

Run the same queries in Kibana (ES) and OpenSearch Dashboards (OS) and compare results:

```
# In both tools — Kibana DevTools / OS Dashboards DevTools
GET /cluster_<id>.working_<timestamp>/_search
{
  "query": { "match_all": {} },
  "size": 10
}
```

| Validation | Expected |
|---|---|
| Document `_id` values match for the same content | Yes |
| `identifier` field values match | Yes |
| `basetype`, `languageid`, `live`, `working` fields match | Yes |
| Record counts are equal | Yes (within acceptable delta) |

### C-2 — Mapping equivalence at the field level

Compare the full mapping output from both backends side by side. Pay attention to:

- Field types (`keyword` vs `text` vs `nested`)
- Analyzer assignments
- Dynamic template definitions
- The `os-content-settings.json` analysis block (custom analyzers) must be present in OS

Expected: mappings are structurally identical. Any divergence must be filed as a bug.

### C-3 — dotCMS startup in Phase 2 with OS unavailable

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop OS: `docker compose stop opensearch`. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=2`. | — |
| 2 | Start dotCMS. | dotCMS **should start** (ES is active fallback in Phase 2). Startup log shows an OS connectivity warning. Reads fall back to ES after OS timeout/exception. |
| 3 | Perform a content search in dotCMS. | Search returns results (from ES fallback). |
| 4 | Publish a content item. | Publish succeeds. ES write succeeds. OS write fails silently (logged at `WARN`). |

### C-4 — Reindex in Phase 2

Same as B-5 but with `FEATURE_FLAG_OPEN_SEARCH_PHASE=2`. Expected behavior is identical: only the ES index is rebuilt by a user-triggered reindex. OS keeps serving from its existing index.

### C-5 — dotCMS startup in Phase 2 with ES unavailable

> In Phase 2, ES is still active as a write fallback. This test validates what happens when ES is completely unreachable at startup time.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Stop ES: `docker compose stop elasticsearch`. Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=2`. | — |
| 2 | Start dotCMS. | Behavior TBD — ES is used for writes in Phase 2. Startup may fail or may degrade gracefully with warnings. Observe startup log carefully. |
| 3 | If dotCMS starts, publish a content item. | ES write will fail. OS write should succeed. Since OS is the read provider in Phase 2, content may appear searchable via OS Dashboards even if ES write failed. |
| 4 | Note any errors or silent failures in the dotCMS log. | Document actual behavior for QA record — this scenario defines the resilience boundary of Phase 2. |

> **Note:** The expected behavior for this scenario is not fully specified in the current design docs. It should be treated as an **exploratory test** — document what actually happens and file a bug if the behavior is unexpected or undocumented.

---

## 8. Scenario D — `/v1/esindex` REST API (Phases 1 and 2)

**Endpoint base:** `GET/POST/PUT/DELETE https://localhost:8082/api/v1/esindex`

Reference class: `ESIndexResource`

### D-1 — List indices (Phase 1)

```bash
curl -s -u admin:admin "http://localhost:8082/api/v1/esindex" | jq .
```

| Validation | Expected |
|---|---|
| Both ES and OS indices appear in the response | Yes — two indices per logical type (working/live) |
| OS index names include the logical name (no `os::` prefix visible) | Yes — `os::` is an internal DB artifact |
| Response HTTP status | 200 |

### D-2 — List indices (Phase 2)

Same request as D-1. Validate that OS indices are listed as active read indices.

### D-3 — Activate an existing inactive index (Phase 1)

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Create an additional ES index (via POST to `/v1/esindex`). | New index created. Appears in Kibana. |
| 2 | Activate the new index: `PUT /v1/esindex/{indexName}/activate` | HTTP 200. Index becomes the active working or live index. |
| 3 | In Phase 1, verify dual-write: new content written to the activated ES index AND to the OS index. | Both indices receive writes. |

### D-4 — Activate a non-existent index

```bash
PUT /api/v1/esindex/nonexistent_index/activate
```

Expected: HTTP 404 with a meaningful error message. No exception stack trace exposed to the caller.

### D-5 — Deactivate an index (Phase 1 and Phase 2)

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Deactivate the working index: `PUT /v1/esindex/{indexName}/deactivate` | HTTP 200. |
| 2 | Check the `indicies` DB table: `SELECT * FROM indicies WHERE index_name LIKE '%working%'` | Row still present but inactive. |
| 3 | Reactivate the index. | HTTP 200. Row becomes active again. |

### D-6 — Delete an index and validate DB cleanup (Phase 1 and Phase 2)

> **Note:** PR #35342 (fix for issue #35306) is **not yet merged**. This test validates the bug and the fix.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Create a secondary (non-active) index. | Index created. Row in `indicies` table. |
| 2 | Delete the index via `DELETE /v1/esindex/{indexName}` | HTTP 200. |
| 3 | Check `indicies` table: `SELECT * FROM indicies WHERE index_name = '<deleted_name>'` | **Expected (after fix #35342 merged):** Row removed. **Current (before fix merged):** Row still present (orphaned). |
| 4 | Check ES/OS via REST: `GET /<indexName>` | HTTP 404 — index physically deleted. |

### D-7 — Delete the active index directly (without deactivating first)

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Attempt to delete the currently active working index. | HTTP 4xx. dotCMS should reject deletion of the active index. |

### D-8 — Closed index lifecycle (Phase 1 and Phase 2)

> Related fix: PR #35352 — `OSIndexAPIImpl.getClosedIndexes()`.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Close a non-active OS index via API. | HTTP 200. |
| 2 | List indices: `GET /v1/esindex` | Closed OS index appears in the list (not invisible). |
| 3 | Re-open the index. | HTTP 200. Index is open and functional. |
| 4 | Attempt delete on the closed index. | HTTP 200. Index deleted. DB row removed (after fix #35342). |

---

## 9. Scenario E — Index Name Scenarios

### E-1 — Fresh install (same index name in ES and OS)

**Context:** When dotCMS starts for the first time with Phase 1 or 2 active, both ES and OS indices are created in the same bootstrap call with the same timestamp. This is the "happy path."

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start a clean dotCMS instance (no existing indices) with `FEATURE_FLAG_OPEN_SEARCH_PHASE=1`. | Bootstrap creates both ES and OS indices simultaneously. |
| 2 | Check `indicies` DB table. | Two rows per logical index: one plain name (ES) and one `os::` prefixed name (OS). The logical names **share the same timestamp**. |
| 3 | Check Kibana and OS Dashboards. | Both indices exist with the same logical name (e.g. `cluster_<id>.working_20260421...`). |
| 4 | Publish content. | Document appears in both ES and OS indices with identical content. |
| 5 | Compare record counts. | Equal. |

### E-2 — Migration catchup (different index names per provider)

**Context:** Production scenario. dotCMS was running in Phase 0 (ES only) and already has an ES index (e.g., `working_20230101`). A restart with Phase 2 active creates a **new OS index** with a different timestamp (`working_20260421`). The two providers hold indices with **different logical names**.

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Start dotCMS in Phase 0. Create content. Let it run until an ES index with a real timestamp exists. | ES index `cluster_<id>.working_<old_timestamp>` present in Kibana. `indicies` table has one row per logical index. |
| 2 | Stop dotCMS. |  |
| 3 | Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=2`. Restart dotCMS. | dotCMS detects no OS index for this cluster. Creates a new OS index with a **new** timestamp. |
| 4 | Check `indicies` DB table. | Two rows for the working index: `cluster_<id>.working_<old>` (ES) and `os::cluster_<id>.working_<new>` (OS). Timestamps are **different**. |
| 5 | Check Kibana and OS Dashboards. | ES index has old timestamp with existing content. OS index has new timestamp, initially empty (or populated via background catchup if implemented). |
| 6 | Publish a new content item. | Item appears in both ES (new write to old index) and OS (new write to new index). |
| 7 | Verify startup log. | Log should note the name divergence. No error. dotCMS operational. |
| 8 | Verify `/v1/esindex` lists both indices correctly. | Both indices shown with their respective timestamps. |

---

## 10. Scenario F — Phase 3 (NOT TESTABLE — for reference only)

> **Phase 3 is not in a testable state.**
>
> Phase 3 (`PHASE_3_OPENSEARCH_ONLY`) routes all reads exclusively to OS and decommissions ES entirely. However, the dotCMS application query layer has **not yet been migrated** to read from OS. Activating Phase 3 in any environment will produce **broken content searches and reads** at the application level — there is no ES fallback once Phase 3 is active, and the OS read path is not wired up in the application yet.
>
> **Do not activate Phase 3 in a QA environment.** The scenarios below are included for documentation purposes only, to be executed once the query migration is complete.

### F-1 — No ES recreation in Phase 3 _(deferred)_

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Set `FEATURE_FLAG_OPEN_SEARCH_PHASE=3`. Stop the ES container. Restart dotCMS. | dotCMS starts. No attempt to connect to ES. No ES index created. |
| 2 | Check startup logs. | Log shows `Phase 3`. No ES health-check attempts. No `indexReadyES()` calls in logs. |
| 3 | Publish content. | Content written to OS only. OS index updated. |
| 4 | Trigger a full reindex. | Reindex runs against OS. BulkProcessor logs show `[OS]` tag. No ES writes. |

### F-2 — OS failure propagates in Phase 3 _(deferred)_

| Step | Action | Expected Result |
|------|--------|----------------|
| 1 | Set Phase 3. Stop OS container. | OS unavailable. |
| 2 | Attempt to publish content in dotCMS. | Publish **fails** (OS is primary in Phase 3 — no fire-and-forget). Error propagated to caller. |

---

## 11. DB Validation Queries

Run these queries on the dotCMS PostgreSQL database to validate index state at any point:

```sql
-- All index rows (both ES and OS)
SELECT * FROM indicies ORDER BY index_name;

-- OS index rows only (identified by os:: prefix)
SELECT * FROM indicies WHERE index_name LIKE 'os::%';

-- ES index rows only
SELECT * FROM indicies WHERE index_name NOT LIKE 'os::%';

-- Verify no orphaned rows after delete
SELECT * FROM indicies WHERE index_name = '<deleted_index_name>';

-- Count rows per provider
SELECT
  CASE WHEN index_name LIKE 'os::%' THEN 'OS' ELSE 'ES' END AS provider,
  COUNT(*) AS row_count
FROM indicies
GROUP BY 1;
```

Expected after fresh install in Phase 1+:
- 1 ES row and 1 OS row for each active index (working + live = 4 rows total minimum).
- OS rows have `os::` prefix in DB; callers always receive the stripped name.

---

## 12. Bugs to Validate (from Sub-Issues)

| Bug | Issue | PR | Status | QA Action |
|---|---|---|---|---|
| OS exceptions propagated to caller in Phase 1 | #35302 | #35389 | Merged | Run Scenario B-4 — OS write failure must not reach caller |
| `getClosedIndexes()` stub returned empty list | #35304 | #35352 | Merged | Run Scenario D-8 — close/list/reopen/delete OS index |
| `putMapping` 400 on OS 3.4.0 | #35305 | #35349 | Merged | Verify `os-content-settings.json` loaded; OS index creation does not 400 |
| `DELETE /v1/esindex` leaves orphaned DB row | #35306 | #35342 | **NOT MERGED** | Run Scenario D-6 — delete index, verify `indicies` row removed |
| TOCTOU race on BulkProcessor rebuild | #35313 | #35323 | Merged | Run concurrent reindex + publish load test; no NPE or stale processor |
| `OSBulkHelper.getIndexName()` always returned `dotcms_content` | #35314 | #35390 | Merged | In Phase 1/2, publish content and confirm OS receives correct index name in bulk request |
| Phase 3 re-creates ES indices | #35356 scope | #35356 | Merged | Run Scenario F-1 — ES must not be touched in Phase 3 |

---

## 13. Test Execution Order

For a clean QA pass, follow this sequence:

1. **A-1, A-2** — Regression Phase 0 — ES baseline, no OS involvement.
2. **B-8** — Phase 0 → Phase 1 transition — first activation of dual-write with existing ES content.
3. **E-1** — Fresh install starting directly in Phase 1 — validates the happy path baseline.
4. **B-1 through B-5** — Phase 1 dual-write: content, mappings, record counts, fire-and-forget.
5. **B-6, B-7** — Phase 1 startup without ES / without OS.
6. **C-1 through C-4** — Phase 2 scenarios: mapping equivalence, cross-tool queries, reindex, startup without OS.
7. **C-5** — Phase 2 startup without ES (exploratory).
8. **D-1 through D-8** — REST API `/v1/esindex` in both phases.
9. **E-2** — Migration catchup (Phase 0 → Phase 2, different index names).
10. **Section 11** — DB validation at each stage transition.
11. **Section 12** — Explicit validation of each known bug fix.
12. **F-1, F-2** — Phase 3 scenarios (deferred — do not run until query migration is complete).

---

## 14. Out of Scope (Deferred)

- **Phase 2 — application read validation:** The dotCMS query layer (search API, content search UI) has not yet been migrated to read from OS. Even with Phase 2 active, application queries silently resolve against ES. Read parity can only be validated directly via OpenSearch Dashboards, not through the dotCMS application. Full application-level Phase 2 testing is deferred until the query migration is delivered.
- **Phase 3 — all scenarios:** Phase 3 requires the query layer to be migrated. Without it, activating Phase 3 produces broken application reads with no ES fallback. No Phase 3 testing should be attempted until the query migration is complete.
- **Site Search (`site-search` index) migration** — separate pipeline, lower priority.
- **Full OS reindex orchestration** — not viable at current scale; deferred.
- **Automated phase promotion gates** (Phase 1 → 2 mapping schema assertion) — documented as tech debt in `OPENSEARCH_MIGRATION.md`.
