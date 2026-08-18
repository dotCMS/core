# ES → OpenSearch Migration — Getting-Started Guide for Testers

> **Who this is for.** You want to help validate dotCMS's search-engine migration by exercising it
> from the outside — admin UI, REST API, the search dashboards, and SQL. You do **not** need to read
> Java source or know the internals. Everything below is described in terms of what you can *observe*.
>
> **Expect to find bugs.** The migration is close to end-to-end complete (Phase 3 included), but it is
> still under active QA. Finding a bug is a success, not a failure — that is exactly what this exercise
> is for. File what you see against the QA epic:
> **[#35476 — End-to-End QA Validation](https://github.com/dotCMS/core/issues/35476)**.

**Companion documents** (deeper detail, load when you need it):

- [`OPENSEARCH_MIGRATION_RUNBOOK.md`](OPENSEARCH_MIGRATION_RUNBOOK.md) — the operator runbook: how to actually migrate a customer, end to end.
- [`OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md) — the full architecture and design of the migration.
- [`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md) — every configuration property.
- [`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md) — the full, numbered test cases.

This guide compiles the essentials of those three into a single starting point.

---

## 1. What this is and why

dotCMS is migrating its search/indexing infrastructure from an older search engine to **OpenSearch 3.x**.
The goal is that the migration is **transparent**: no downtime, no data loss, and no visible change in
behavior for end users. The system can run with *both* engines active at once, writing to both while it
gradually shifts reads over — so that if anything goes wrong it can keep serving from the old engine.

Your job as a tester is to confirm that transparency holds: that content stays searchable, that both
engines stay in sync, and that dotCMS degrades safely when the new engine is missing or misconfigured.

### Terminology — "source engine" and "target engine"

Because dotCMS can be migrated *from* Elasticsearch **or** from an older OpenSearch, this guide uses
neutral names:

- **Source engine** — the engine dotCMS runs on today (the "old" one). It always works.
- **Target engine** — **OpenSearch 3.x**, the engine we are migrating *to* (the "new" one).

> In the lab stack below, the source engine is **OpenSearch 1.3** (standing in for a legacy
> Elasticsearch / OpenSearch 1.x deployment) and the target engine is **OpenSearch 3.4**. The concepts
> are identical if the source is real Elasticsearch — see the *Elasticsearch variant* note in §4.

### Concepts in two minutes

| Term | What it means, in plain words |
|---|---|
| **Index** | Where a search engine stores content so it can be searched. Each engine has its own. Index names carry a timestamp, e.g. `cluster_08abc3.working_20260421120000`. |
| **Migration phase** | How far the migration has advanced — a number from 0 to 3 (see §3). It is a switch you control. |
| **Dual-write** | In phases 1 and 2, every content change is written to **both** engines at the same time, so they stay in sync. |
| **The `.os` suffix (the "tag")** | The marker that tells a **target-engine** index apart from a source-engine one. Target-engine index names **end in `.os`** (e.g. `cluster_08abc3.working_20260406.os`); source-engine names do not. It is part of the real index name and shows up everywhere — in the cluster, in the database, and in dotCMS REST responses. |
| **Automatic migration shutdown** | A safety feature: if at startup the target engine can't be reached or reports the wrong version, dotCMS **turns the migration off by itself**, drops back to Phase 0 (source engine only), and keeps running normally. It does **not** crash or hang. |
| **Reindex (rebuild)** | Refilling an index from scratch with all content. A heavy operation — it should never happen without a real reason. |
| **`indicies` table** | The database table where dotCMS records which indexes exist. Target-engine rows end in `.os`; source-engine rows do not. |

---

## 2. The migration at a glance

Content writes always go through dotCMS after a database commit, so the database and the indexes stay
consistent. What changes phase to phase is **which engines get the writes** and **which engine answers
reads**:

- **Writes** fan out to whichever engines the current phase considers active.
- **Reads** are served by exactly one engine per phase.
- **Failures degrade safely**: while the source engine is still authoritative (phases 1–2), a failed
  target-engine **write** is logged and ignored (fire-and-forget), and a failed target-engine **read**
  automatically falls back to the source engine. Only in Phase 3, once the source is decommissioned, do
  target-engine failures surface to the caller.

---

## 3. The four phases

The phase is controlled by one setting: **`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`** (values `0`–`3`).

| Phase | Writes | Reads | Source engine | What you should observe |
|---|---|---|---|---|
| **0** | source only | source | active | Only source-engine indices exist (no `.os` names). The target engine is irrelevant — dotCMS never touches it. |
| **1** | source **+** target | source | active | Both engines receive every write; **`.os` indices appear**. Searches are still answered by the source engine. |
| **2** | source **+** target | **target** | active (fallback) | Reads now come from the **target** engine; the source engine is still written and used as a fallback if a target read fails. |
| **3** | **target only** | **target** | decommissioned | Only `.os` indices are used. The source engine is no longer written or read. Target failures now surface (no fallback). |

**Phases are advanced manually** by changing `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE`. The value is re-read
on each routing decision, so the *routing* change (which engine gets writes/reads) takes effect without a
restart — but the one-time setup for a phase does not (see the restart note below), and every node in a
cluster must carry the same value.

> **Phase-transition preconditions** (from [`OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md)):
> - **0 → 1**: target cluster reachable; target index created.
> - **1 → 2**: target data quality validated; source and target mappings identical.
> - **2 → 3**: target confirmed stable under production read load; source decommission approved.
>
> There is no automatic promotion — advancing a phase is always a deliberate action.

> **When you must restart dotCMS.** The flag is re-read live only for **routing**. The **one-time setup
> for a phase runs only at startup** (`InitServlet` → `checkAndInitializeIndex()`): the OS
> connectivity / version-3.x / endpoint-separation validation, the automatic migration shutdown safety
> check, and the **creation of the target-engine index and its `indicies` rows**. So:
> - **Turning the migration on (0 → 1): restart dotCMS.** The target index and its DB rows are
>   bootstrapped at boot (in Phase 1, startup requires *both* engines ready and creates the missing OS
>   pair). If you only flip the flag live, dual-writes route to a target index that does not exist yet —
>   and those shadow-write misses are silently swallowed (fire-and-forget) — while validation never runs.
> - **Advancing 1 → 2 and 2 → 3: restart to re-validate.** Routing switches live, but only a restart
>   re-runs the startup validation for the phase you are now in — and entering Phase 3 that check is
>   fail-loud (no ES fallback). Validate the phase you actually run.
> - **After an automatic shutdown**, the reset to Phase 0 is in memory only — fix the config and restart
>   to make it stick (or to retry the migration).

---

## 4. Quickstart — bring up the lab

The fastest way to get a full environment is the **self-contained migration stack**. One command brings
up the database, dotCMS, both search engines, and both dashboards.

The starting point is the published example compose file:
[`docker/docker-compose-examples/single-node-os-migration/docker-compose.yml`](https://github.com/dotCMS/core/blob/fd8ac59114125468eda920ebd9298921c3c28f43/docker/docker-compose-examples/single-node-os-migration/docker-compose.yml)
(pinned permalink — the default branch may carry newer fixes).

If you already have the repo checked out:

```bash
cd docker/docker-compose-examples/single-node-os-migration
docker compose up -d
```

**If you only want the stack (no full checkout)** — grab the directory. The compose file **cannot run
alone**: its `opensearch1-provision` / `opensearch3-provision` services are `build:`-ed from two sibling
files (`Dockerfile-opensearch-py`, which copies `opensearch.py`), so you need all three in the same
folder. Easiest is a sparse checkout:

```bash
git clone --filter=blob:none --sparse https://github.com/dotCMS/core.git
cd core
git sparse-checkout set docker/docker-compose-examples/single-node-os-migration
cd docker/docker-compose-examples/single-node-os-migration
docker compose up -d
```

Or download just the three files:

```bash
BASE=https://raw.githubusercontent.com/dotCMS/core/fd8ac59114125468eda920ebd9298921c3c28f43/docker/docker-compose-examples/single-node-os-migration
mkdir os-migration && cd os-migration
for f in docker-compose.yml Dockerfile-opensearch-py opensearch.py; do curl -fsSLO "$BASE/$f"; done
docker compose up -d
```

### What comes up

| Service | Role | URL | Notes |
|---|---|---|---|
| dotCMS | app under test | http://localhost:8082 | Admin UI + REST API. Login `admin` / `admin`. |
| Source engine (OpenSearch 1.3) | "old" engine | https://localhost:9200 | HTTPS + auth. |
| Source dashboards | inspect source indices | http://localhost:5601 | Login `admin` / `admin`. |
| Target engine (OpenSearch 3.4) | "new" engine | https://localhost:9201 | HTTPS + auth. |
| Target dashboards | inspect target indices | http://localhost:5602 | Login `admin` / `Dev!Search3-Kx9mP-2026`. |
| Glowroot | JVM profiler | http://localhost:4000 | Optional. |
| PostgreSQL | database | *(not published to host)* | Reach it via `docker compose exec db …` — see §6. |

### Startup order

1. PostgreSQL, the source engine, and the target engine start in parallel.
2. Provisioning containers wait for health checks, then create the `dotcms-es-user` account on both engines.
3. dotCMS starts once both provisioners finish.

> **Wait for the engines to be healthy before trusting dotCMS.** Health-check both clusters (auth
> required, self-signed cert so use `-k`):
> ```bash
> curl -sk -u admin:admin https://localhost:9200/_cluster/health | jq .status              # source
> curl -sk -u admin:'Dev!Search3-Kx9mP-2026' https://localhost:9201/_cluster/health | jq .status  # target
> ```

### Credentials

| Where | User | Password |
|---|---|---|
| dotCMS admin UI / REST | `admin` | `admin` |
| Source engine — admin | `admin` | `admin` |
| Target engine — admin | `admin` | `Dev!Search3-Kx9mP-2026` |
| Both engines — the account dotCMS uses | `dotcms-es-user` | `Dev!dotcms-EsUser-2026` |

> The dashboard consoles (`5601` / `5602`) show a login page — use the matching engine's **admin**
> credentials above (source → `admin`/`admin`, target → `admin`/`Dev!Search3-Kx9mP-2026`). The
> `dotcms-es-user` account is dotCMS's service account and also works for direct engine `curl` calls,
> but log into the consoles as `admin`.
>
> Some older copies of the stack's `README.md` list the target admin password as
> `Dev!Strong-OSAdmin-2026` — that is stale; the value the container actually runs with is
> `Dev!Search3-Kx9mP-2026` (from `OPENSEARCH_INITIAL_ADMIN_PASSWORD` in `docker-compose.yml`).

The default phase in this stack is **Phase 1** (`DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE: '1'`). To test a
different phase, edit that value in `docker-compose.yml` and restart dotCMS
(`docker compose up -d dotcms`).

> **Elasticsearch variant.** If you specifically need the source engine to be **real Elasticsearch
> 7.10** instead of OpenSearch 1.x, there is a second stack at
> `docker/docker-compose-examples/os-migration/`. It brings up ES 7.10 + Kibana and OpenSearch 3.x +
> Dashboards on one network, but it does **not** include dotCMS or the database — you run dotCMS yourself
> and point it at that stack. Use the self-contained stack above for everything else.

---

## 5. The configuration you actually touch

dotCMS reads these from the environment (the `DOT_` prefix maps to the properties in
[`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md)). In the lab they are already
set in `docker-compose.yml`; you mainly change the phase.

The two engines are configured **separately** — the source engine reads its settings directly, while the
target engine resolves each setting through a fallback chain (`OS_*` key → matching `ES_*` key →
built-in default). Keep them in two mental buckets:

### Migration switch (applies to the whole system)

| Variable | Purpose |
|---|---|
| `DOT_FEATURE_FLAG_OPEN_SEARCH_PHASE` | `0`/`1`/`2`/`3` — how far the migration runs. The switch most tests toggle. |
| `DOT_OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` | Role **key** required — in addition to CMS Admin — to read the migration-readiness endpoint `GET /api/v1/index/migration/readiness` (default `os_migration_qa`). It no longer controls index visibility in the UI: since #36360 that is purely phase-based (hidden in phases 0–2, shown in Phase 3, for everyone). |

### Source engine (the "old" one — `DOT_ES_*`)

| Variable | In the lab | Purpose |
|---|---|---|
| `DOT_ES_ENDPOINTS` | `https://opensearch1:9200` | Source cluster URL. |
| `DOT_ES_AUTH_TYPE` | `BASIC` | Auth scheme. |
| `DOT_ES_AUTH_BASIC_USER` / `..._PASSWORD` | `dotcms-es-user` / `Dev!dotcms-EsUser-2026` | Source credentials. |

### Target engine (OpenSearch 3.x — `DOT_OS_*`)

| Variable | In the lab | Purpose |
|---|---|---|
| `DOT_OS_ENDPOINTS` | `https://opensearch3:9200` | Target cluster URL. **Must be a separate instance from the source** — pointing it at the source URL is exactly what the safety guards detect. |
| `DOT_OS_AUTH_TYPE` | `BASIC` | Auth scheme. |
| `DOT_OS_AUTH_BASIC_USER` / `..._PASSWORD` | `dotcms-es-user` / `Dev!dotcms-EsUser-2026` | Target credentials. |
| `DOT_OS_TLS_TRUST_SELF_SIGNED` | `true` | Accept the self-signed dev certificate. |

> Every target-engine key has a source-engine fallback: if a `DOT_OS_*` key is unset, dotCMS tries the
> matching `DOT_ES_*` key before the built-in default. When you report a config-related finding, note
> **which** engine's setting you changed — the resolution paths differ.

---

## 6. How to verify what happened

These helpers are what most test cases lean on. Index names contain a timestamp and differ on every
machine, so **never guess a name — list it.**

**Find your real index names.** Target-engine names end in `.os`; source-engine names do not.

```bash
# What dotCMS knows about (keeps the .os tag, drops the cluster prefix):
curl -s -u admin:admin http://localhost:8082/api/v1/esindex | jq .

# The exact physical names each engine holds (use THESE for direct curl / dashboards):
curl -sk -u dotcms-es-user:'Dev!dotcms-EsUser-2026' https://localhost:9200/_cat/indices?v   # source (no .os)
curl -sk -u dotcms-es-user:'Dev!dotcms-EsUser-2026' https://localhost:9201/_cat/indices?v   # target (ends in .os)
```

Or in the UI: **Admin → System → Index**.

> **⚠️ You will not see the `.os` indices in dotCMS before Phase 3.** In phases 0, 1, and 2 the two
> dotCMS-side views — the `/api/v1/esindex` response and **Admin → System → Index** — **hide the
> target-engine (`.os`) indices from everyone**, because before Phase 3 they are a migration artifact.
> In Phase 3 they are always visible. This is purely phase-based and consults no user or role (#36360).
> The filter applies **only** to those two dotCMS views: the direct engine listing (`_cat/indices` on
> `:9200` / `:9201`) and the database `indicies` table are **never** filtered and always show `.os`,
> so use those to confirm an index really exists.
>
> **To see the migration state from inside dotCMS, use the migration-readiness endpoint instead** —
> `GET /api/v1/index/migration/readiness`. It is the source of truth for ES↔OS mirror state in every
> phase. It requires a user who is **both** a CMS Administrator **and** a member of the migration
> support role:
> 1. Decide the **role key**. The default is `os_migration_qa` — the easiest path, no config change. To
>    use a different key, set `DOT_OS_MIGRATION_INDEX_VISIBILITY_ROLE_KEY` in `docker-compose.yml` and
>    restart dotCMS.
> 2. In dotCMS, create a Role whose **Key** matches that value exactly. It is matched on the role *key*
>    (`Role.getRoleKey()`), **not** the display name — set the Key field, not just the name.
> 3. Assign that role to the dotCMS user you test with (who must also be a CMS Admin).
>
> Anyone missing either condition gets a 403. See
> [`OPENSEARCH_MIGRATION_RUNBOOK.md`](OPENSEARCH_MIGRATION_RUNBOOK.md) §16 for how to read the report.

**Which phase did the system end up in?**
- **Startup log (most reliable):** find the line stating the migration phase. If the automatic shutdown
  fired, you will also see an error saying the migration was disabled and the phase was reset to 0.
- **Database (reliable):** run the `indicies` query below — `.os` rows present → the migration is active.
- **Readiness endpoint (authoritative):** `GET /api/v1/index/migration/readiness` reports
  `phase.current` and `phase.name` directly, in every phase.
- **UI / `/api/v1/esindex`:** these views hide `.os` in phases 0/1/2 for everyone, so they tell you
  nothing about whether the migration is active — seeing `.os` there only means you are in Phase 3.

**Check the database directly.** Postgres is not published to the host — go through the container:

```bash
docker compose exec db psql -U dotcmsdbuser -d dotcms
```
```sql
-- Target-engine rows (should exist only when the migration is really active):
SELECT * FROM indicies WHERE index_name LIKE '%.os';
-- Source-engine rows:
SELECT * FROM indicies WHERE index_name NOT LIKE '%.os';
-- Count per engine (Phase 1 fresh install: 2 source + 2 target; Phase 0: 2 source, 0 target):
SELECT CASE WHEN index_name LIKE '%.os' THEN 'TARGET' ELSE 'SOURCE' END AS engine,
       COUNT(*) FROM indicies GROUP BY 1;
```

**Count documents in an index directly** (use the physical names from the listing above):

```bash
curl -sk -u dotcms-es-user:'Dev!dotcms-EsUser-2026' https://localhost:9200/<source-index-name>/_count
curl -sk -u dotcms-es-user:'Dev!dotcms-EsUser-2026' https://localhost:9201/<target-index-name.os>/_count
```

**Recognize an automatic migration shutdown.** When the safety feature fires at startup (Phase 1 or 2),
dotCMS writes **three** lines — **two `ERROR` and one `WARN`, never a `FATAL`** on this path:

```
ERROR  OpenSearch startup validation FAILED — halting OS migration; dotCMS falls back to ES-only (PHASE_0_MIGRATION_NOT_STARTED): <reason>
ERROR  OpenSearch migration halted: invalid configuration detected at startup. Verify OS_ENDPOINTS, OS version, and FEATURE_FLAG_OPEN_SEARCH_PHASE, then restart dotCMS.
WARN   Migration phase reset to PHASE_0_MIGRATION_NOT_STARTED (was PHASE_1_DUAL_WRITE_ES_READS). This change is runtime-only — persist it in dotmarketing-config.properties to survive a restart.
```

`<reason>` is one of: *target unreachable / bad URL*, *wrong version* (target is not OpenSearch 3.x), or
*source and target point to the same endpoint*. On a **successful** start you instead see
`OS version check passed: <version>` and an endpoint-separation-passed line.

---

## 7. What to validate, by phase

This is a map of the ground; the full, numbered cases live in
[`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md). Work top to bottom.

**Phase 0 — no regression.** With the migration off, everything works exactly as it does today. A
target engine that is down or misconfigured must be completely irrelevant — dotCMS never touches it, and
none of the shutdown log lines appear.

**Safe failure (migration on, target broken).** With the phase set to 1 but the target engine down,
unreachable, malformed, the wrong version, or pointed at the same address as the source: **dotCMS still
starts normally and keeps serving from the source engine.** It never crashes or hangs; the migration is
left off and the shutdown lines are logged. Turning the migration on must **not** trigger a needless full
rebuild of existing content.

**Phase 1 — dual-write.** Every content change is written to both engines. Verify a published item lands
in both the source and the target working indexes (same document, matching data). Deleting a content item
or a content type must remove its documents from **both** engines. A field marked *searchable* must
behave the same way in both engines.

**Phase 2 — reads move to the target.** Reads are now served by the target engine, with the source as a
fallback. Verify that content is searchable, and that if the target engine fails a read, dotCMS falls
back to the source and logs the failure.

**Phase 3 — target only.** The source engine is decommissioned. Verify content is written, searched, and
reindexed entirely against the target engine, and that target failures now surface instead of silently
falling back.

**Same query, both engines.** For a given query, compare the documents returned by each engine directly
(via the dashboards). Relevance *scores* may differ, but the **set** of returned documents should match.
A differing set is a finding worth filing (`match_phrase_prefix` is a known trouble spot).

**Index lifecycle & REST security.** Create / delete / reindex through `/v1/esindex` must keep the
`indicies` table in sync, refuse to delete the active index, return a clean 404 for a missing index, and
enforce authentication (401 with no credentials, 403 for a non-admin).

---

## 8. Validating VTL viewTools

VTL templates are where a lot of migration regressions surface, because customer templates read the
index through **viewTools** — the `$`-prefixed objects available inside `.vtl`. If a viewTool returns a
different shape or different data after the reads move to the target engine, every page built on it
breaks silently. Several real customer tickets have come from exactly this.

**The core test pattern:** run the **same** template, unchanged, and compare its rendered output between a
source-engine phase (Phase 0 or 1) and a target-engine phase (Phase 2 or 3). Same input, same output —
any difference is a finding.

> **How to run a snippet.** The simplest observable path: put the VTL in a **Template** (or a Widget's
> code / an `.vtl` file under the site), assign it to a page, and view that page. Switch the phase
> (§3), republish/re-request the page, and diff the two renders. The demo starter site ships working
> search pages you can reuse — see the note at the end of this section.

### The viewTools that hit the index

| `$key` | Class | What it's for |
|---|---|---|
| `$dotcontent` | `ContentTool` | The main content-pull tool. `pull`, `pullPerPage`, `query`, `count`, `find`. **No aggregation support.** |
| `$estool` | `ESContentTool` | Raw index-query tool — this is where aggregations live. Four query methods, two neutral and two legacy (see below). |
| `$sitesearch` | `SiteSearchWebAPI` | Site Search queries and facets. |

`$estool` has four query methods, and **which one a template uses decides whether it survives the
migration**:

| Method | Returns | Migration status |
|---|---|---|
| `$estool.search(esQuery)` | `ContentSearchResults` (elements are `ContentMap`); `.aggregations` is a neutral map | ✅ **Recommended** — vendor-neutral |
| `$estool.raw(esQuery)` | `ContentSearchResponse` (neutral record) | ✅ Neutral |
| `$estool.esSearch(esQuery)` | `ESSearchResults` (wraps **raw** `Contentlet`s; ES-typed aggregations) | ⚠️ **Legacy** — the source of two known bugs below |
| `$estool.esRaw(esQuery)` | the raw Elasticsearch SDK `SearchResponse` | ⚠️ **Legacy** — exposes ES SDK types directly; breaks on OpenSearch |

All four methods take a **raw Elasticsearch/OpenSearch JSON query body** (not a Lucene string). Note that
`search` and `raw` **lowercase the entire query** before running it, so a mixed-case field like
`contentType` resolves to the physical index field `contenttype` — but exact case-sensitive matching is
not possible on these paths, and aggregation names come back lowercased.

### Regression class 1 — aggregations / facets must be identical

*(Seen in customer tickets #37559 / #36026.)* The migration once flattened `$results.aggregations` and
silently broke templates that walked `.buckets`, `getKeyAsString()`, `getDocCount()`, and nested
`top_hits`. The neutral response type was built specifically to keep those templates working verbatim —
so this test verifies that promise holds.

```velocity
#set($query = '{"query":{"query_string":{"query":"+contentType:Blog +live:true"}},"size":0,"aggs":{"by_type":{"terms":{"field":"contentType"}}}}')
#set($results = $estool.search($query))
Total: $results.totalResults
#foreach($bucket in $results.aggregations.by_type.buckets)
  $bucket.keyAsString = $bucket.docCount
#end
```

- **What to check:** the bucket names and `docCount` values are **the same** in a source-engine phase and
  a target-engine phase. Also test nested aggregations and a `top_hits` sub-aggregation (which comes back
  under `.hits` instead of `.buckets`).
- The neutral aggregation objects expose: `Aggregation.getBuckets()` / `getHits()`, and
  `AggregationBucket.getKey()` / `getKeyAsString()` / `getKeyAsNumber()` / `getDocCount()` /
  `getAggregations()` (nested). Templates written against these keep working across the migration.

### Regression class 2 — raw-Contentlet field shadowing (`$estool.esSearch`)

*(Open customer ticket #37870.)* When a template iterates the **legacy** `$estool.esSearch(...)` result,
each record is a **raw `Contentlet`**, and Velocity resolves `$record.<field>` by calling the matching
getter. If a custom field's variable name collides with a built-in getter, the getter wins. The known
case: a custom field named `contentTypeId` is shadowed by `Contentlet.getContentTypeId()`, which returns
the content type's internal hash instead of the field value.

```velocity
#set($query = '{"query":{"query_string":{"query":"+contentType:MyType +live:true"}}}')
#set($results = $estool.esSearch($query))
#foreach($record in $results)
  getter access:  $!{record.contentTypeId}       ## returns a 32-char hash after migration — BUG #37870
  map access:     $!{record.map.contentTypeId}   ## returns the real custom-field value — the workaround
#end
```

- **What to check:** if a customer reports a field "changing into a hash" or returning the wrong value
  after the migration, look for `esSearch(...)` in their template plus a `$record.<field>` whose name
  matches a `Contentlet` getter. The fix/workaround is `$record.map.<field>` (or switching to
  `$estool.search(...)`, whose records are `ContentMap` and read fields cleanly).

### Regression class 3 — templates bound to ES SDK types

A template that calls `$estool.esSearch(...).getAggregations()` or `$estool.esRaw(...)` receives raw
Elasticsearch SDK objects. Those types do not exist on OpenSearch, so such templates fail once reads move
to the target engine. **What to check:** flag any template touching the legacy `esSearch` / `esRaw`
paths for rework toward the neutral `$estool.search(...)` / `$estool.raw(...)`.

### Site Search via VTL

`$sitesearch` runs Site Search queries and — importantly — facets:

```velocity
#set($ss = $sitesearch.search("<query>", 0, 20))   ## (query, offset, rows) using the default index
Total: $ss.totalResults
#foreach($r in $ss.results)
  $r.title — $r.url
#end
#set($aggs = $sitesearch.getAggregations($ss.index, "<query>"))
#foreach($bucket in $aggs.<aggName>.buckets)
  $bucket.keyAsString = $bucket.docCount
#end
```

- `SiteSearchResults` exposes `getResults()`, `getTotalResults()`, `getQuery()`, `getIndex()`, `getTook()`,
  etc.; each result exposes `getTitle()`, `getUrl()`, `getHost()`, `getMimeType()`, `getScore()`,
  `getHighlights()`, `getMap()`. Facets come from `getAggregations(indexName, query)` (the older
  `getFacets(...)` is deprecated).
- **This is where the Site Search operational edge in §11 becomes visible:** if the target-engine twin
  was auto-created with a *dynamic* mapping (an incremental crawl over a Phase-0 index), term facets on
  fields like `mimeType` / `host` / `url` come back wrong or empty. Run the facet snippet against a
  properly full-crawled index vs a dynamically-created twin to reproduce it.

> **Where the working example templates live.** The classic Site Search demo templates (`ss-aggs.vtl`,
> `ss-facets.vtl`, `search-results.vtl`) are **not** in this repo — they ship inside the **dotCMS demo
> starter site**, not the empty starter this lab uses. To get real `$sitesearch` / aggregation templates
> to test, load the demo starter site. The most authoritative in-repo "known-good" usages of the exact
> signatures are the integration tests `SiteSearchWebAPITest` and `ESContentResourcePortletTest`.

---

## 9. Testing queries & the search endpoints

The search REST endpoints are **phase-aware**: the exact same request hits the source engine in phases
0–1 and the target engine in phases 2–3. So the primary test is again a **cross-phase diff** — run the
same query, compare the results between a source-engine phase and a target-engine phase. All examples use
the lab's dotCMS at `http://localhost:8082` with `admin:admin`.

### Content search — `/api/content/_search` (Lucene query)

dotCMS's own query language (not raw engine DSL). Body is a simple form:

```bash
curl -s -u admin:admin -X POST http://localhost:8082/api/content/_search \
  -H 'Content-Type: application/json' \
  -d '{"query":"+contentType:Blog +live:true","sort":"modDate desc","limit":20,"offset":0,"depth":0}'
```

- `POST /api/content/_search` and its newer wrapper `POST /api/v1/content/_search` behave identically
  (same body, same result).
- Response is the standard envelope: `entity.resultsSize` (total), `entity.queryTook` / `contentTook`
  (timings), and `entity.jsonObjectView.contentlets[]` (the results).
- **Check across phases:** `resultsSize` and the returned identifiers match between a source-engine and a
  target-engine phase for the same query.

There is also a lightweight index-only variant that returns just identifiers/inodes:
`GET /api/content/indexsearch/{query}/sortby/{sortby}/limit/{limit}/offset/{offset}`, and a
`GET /api/content/indexcount/{query}`.

### Higher-level form search — `/api/v1/content/search`

`POST /api/v1/content/search` takes a structured form (`globalSearch`, per-content-type searchable
fields, `orderBy`, `page`, `perPage`) rather than a raw Lucene string, and builds the query internally.
Requires a logged-in backend user. Same `SearchView` response shape.

### Raw engine query — `/api/es/search` and `/api/es/raw`

These take a **raw Elasticsearch/OpenSearch DSL** body — this is the closest a tester gets to querying
the engine through dotCMS while still going through the phase router. **Authentication is required** (401
if anonymous).

```bash
curl -s -u admin:admin -X POST http://localhost:8082/api/es/search \
  -H 'Content-Type: application/json' \
  -d '{"query":{"query_string":{"query":"+contentType:Blog +live:true"}},
       "sort":[{"modDate":"desc"}],"size":20,"from":0,
       "aggs":{"content_types":{"terms":{"field":"contentType"}}}}'
```

**Important for migration testing — the response shape is deliberately kept in the legacy Elasticsearch
wire format** for backward compatibility, even when the target engine served the query:

- `/api/es/search` returns `{ "contentlets": [...], "esresponse": { …legacy ES JSON… } }`.
- `/api/es/raw` returns the legacy ES-wire JSON directly: `took`, `hits.total.{value,relation}`,
  `hits.hits[]` (each with `_id`, `_index`, `_score`, `_source`), and `aggregations`.
- Aggregation keys use the ES "typed key" form, e.g. `sterms#content_types`; buckets carry `key`,
  `key_as_string`, `doc_count`, and nested sub-aggregations. `top_hits` aggregations return a `hits`
  object instead of `buckets`.
- `_score` is coerced to `null` for non-finite values (field-sorted, filter-only, or aggregation-only
  queries). This was a real bug source (#36398) — a `500` from a search response is worth filing.
- **Note:** neither shape emits a per-hit `sort` array. If a test asserts on per-hit `sort`, it will not
  be there.

**Check across phases:** run the same DSL query in a source-engine phase and a target-engine phase and
compare the hit count, the returned `_id`s, and the aggregation buckets/counts. Score *values* may
differ; the result **set** and the aggregation **structure** should not. `match_phrase_prefix` is a known
trouble spot worth targeting specifically.

### GraphQL

Content queries via GraphQL also resolve through the content index: `POST /api/v1/graphql` with a
`{ "query": "...", "variables": {...} }` body. Worth a smoke test across phases if the customer uses it.

---

## 10. Index-access & management endpoints

All index lifecycle operations live under **`/api/v1/esindex`**. **Every endpoint requires a CMS
Administrator who also has the Maintenance portlet** — this is the strictest auth level, so use `admin`.
These endpoints are thin wrappers over the phase-aware routers, so they act on whichever engine(s) the
current phase considers active, and they keep the `indicies` database table in sync (§6).

| Operation | Method & path | Notes |
|---|---|---|
| List all indices | `GET /api/v1/esindex/indexlist` | dotCMS-known names (target-engine names end in `.os`). |
| Full status / stats | `GET /api/v1/esindex/` | Per-index stats + which are active; also the body returned by delete/modify ops. |
| Get active index | `GET /api/v1/esindex/active/type/working` | Path-style (not a query param); use `working` or `live`. |
| Cluster stats | `GET /api/v1/esindex/cluster` | Cluster + node info. |
| Create index | `PUT /api/v1/esindex/create/shards/{n}` | Add `/live/true` and/or `/index/<name>` to the path. `shards` is required. |
| Delete index | `DELETE /api/v1/esindex/{indexName}` | 404 if missing; **400 if the index is active or building** (guard); site-search indices routed to their own API. |
| Activate ("make current") | `PUT /api/v1/esindex/{indexName}?action=activate` | Activate *is* the make-current operation. |
| Deactivate | `PUT /api/v1/esindex/{indexName}?action=deactivate` | |
| Clear / open / close | `PUT /api/v1/esindex/{indexName}?action=…` | `action` = `clear`, `open`, or `close` (unified `modIndex`). |
| Start full reindex | `POST /api/v1/esindex/reindex` | Optional `?contentType=` (default = all). |
| Reindex progress | `GET /api/v1/esindex/reindex` | |
| Stop reindex | `DELETE /api/v1/esindex/reindex?switch=true` | `switch=true` stops **and** switches over. |
| List failed reindex records | `GET /api/v1/esindex/failed` | |
| Clear failed records | `DELETE /api/v1/esindex/failed` | |
| Optimize (force-merge) | `POST /api/v1/esindex/optimize` | |
| Flush caches | `DELETE /api/v1/esindex/cache` | |

> The activate/deactivate/clear/open/close/create paths also have older dedicated forms
> (`PUT .../activate/...` etc.) that are `@Deprecated` in favor of the unified `?action=` form above —
> both still work, but prefer `?action=`.

**Not available as REST endpoints** (don't look for them): there is **no update-replicas** endpoint
(replica count is set at index creation / config level) and **no snapshot/restore** endpoint.

**What to validate here** (details in [`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md)):
create/delete/reindex keep the `indicies` table in sync, deleting the **active** index is rejected (400),
deleting a missing index returns a clean 404 (not a stack trace), and the endpoints enforce auth (401
anonymous, 403 for a non-admin). A ready-made set of example requests lives in the Postman collection
`dotcms-postman/src/main/resources/postman/ESIndexResourceTests.json`.

---

## 11. Operational notes worth knowing

These come from support/QA observations during the migration. They explain behavior that can look like a
bug but is expected — or that is a genuine sharp edge to watch for.

### Site Search behaves a little differently

Site Search indexes are built by the **crawl** (a scheduled job, or "Run Now" from the portlet), not by
ordinary content publishing. That has consequences during migration:

- **Changing the phase does not create mirror indexes retroactively.** A Site Search index created in
  Phase 0 lives only on the source engine; switching to Phase 1 does **not** create its target-engine
  twin. The twin appears only when a **full crawl runs while in a dual-write phase (1 or 2)**.
- **Before advancing a phase, let the scheduled crawls run at least once in the current phase.** The rule
  of thumb is *"transition when every Site Search crawl has run at least once in the current phase"* — not
  merely *"the flag is set to N"*. Otherwise indexes from the previous phase have no target-engine twin
  yet, and search in the new phase falls back or hits an incorrectly-mapped twin.
- **After moving to Phase 1, run one full (not incremental) crawl per index before trusting incrementals.**
  A full crawl builds the target-engine twin with the correct field mapping. An incremental crawl reuses
  the existing index and can auto-create a target-engine twin with the *wrong* (dynamic) mapping — which
  silently breaks term aggregations / facets.
- **Verify the twins match:** `GET _cat/indices/*sitesearch*?v` on both engines should agree on name and
  document count.

### Rollback during dual-write leaves the target ahead (drift)

If dotCMS is rolled back to an older build while in Phase 1 or 2, the old build stops writing to the
target engine, but the target keeps whatever the newer build already pushed — so the target silently
**drifts ahead** of the source. This is benign for the source engine (which the old build reads and
writes), but it means the target now holds stale/divergent data.

**Runbook:** if you roll back during Phase 1 or 2, **trigger a full reindex against the target engine
before re-activating the migration.** Prefer letting an in-flight reindex finish (or aborting it
explicitly) over flipping the phase mid-rebuild.

### Reporting config findings

When you report anything about index or connection settings, split your findings into a **source-engine
section** and a **target-engine section** — they resolve configuration through different paths (see §5).
Don't merge them into one table.

---

## 12. Troubleshooting / FAQ

- **dotCMS pauses for a while at startup.** Expected when the target engine is unreachable — it is timing
  out the connection attempts. Wait for startup to finish; it will fall back to the source engine.
- **`admin` can't see any site / content type right after first boot** (only when running a *locally
  built* `dotcms-test` image — not the `dotcms/dotcms:latest` image this stack uses). The CMS Admin role
  is attached in the DB but the role cache is stale on first boot. **Fix: `docker restart` the dotcms
  container once**; admin resolves on the second boot.
- **`curl` to an engine returns a TLS/certificate error.** The dev engines use self-signed certs — add
  `-k` (and always `-u <user>:<pass>`, since security is on).
- **`/api/v1/temp` or workflow endpoints return 400 "Invalid Origin or referer".** Add
  `-H "Origin: http://localhost:8082" -H "Referer: http://localhost:8082/"` to the request.
- **I can't see the `.os` index in the admin UI or in `/api/v1/esindex`.** Expected: in phases 0–2
  those two dotCMS views hide `.os` indices from everyone, regardless of role. To confirm the index
  exists, check `_cat/indices` on the engine or the `indicies` DB table — or call
  `GET /api/v1/index/migration/readiness`, which reports both engines in every phase (§6).
- **I set the phase but nothing changed.** The phase is re-read live for routing, but the one-time phase
  setup runs only at startup — see the restart note in §3. Content already indexed is not moved
  retroactively; new writes follow the new phase. For Site Search specifically, see §8.
- **I found a bug.** Good — that's the point. File it against
  [#35476](https://github.com/dotCMS/core/issues/35476) with the phase, the exact index names, and what
  you observed on each engine.

---

## 13. References

- [`OPENSEARCH_MIGRATION_RUNBOOK.md`](OPENSEARCH_MIGRATION_RUNBOOK.md) — the operator runbook for a real migration.
- [`OPENSEARCH_MIGRATION.md`](OPENSEARCH_MIGRATION.md) — architecture and design.
- [`OPENSEARCH_CLIENT_CONFIGURATION.md`](OPENSEARCH_CLIENT_CONFIGURATION.md) — full configuration reference.
- [`OPENSEARCH_MIGRATION_TEST_PLAN.md`](OPENSEARCH_MIGRATION_TEST_PLAN.md) — full numbered test cases.
- [QA Epic #35476](https://github.com/dotCMS/core/issues/35476) — where findings go.
- Lab stack: `docker/docker-compose-examples/single-node-os-migration/` ([compose permalink](https://github.com/dotCMS/core/blob/fd8ac59114125468eda920ebd9298921c3c28f43/docker/docker-compose-examples/single-node-os-migration/docker-compose.yml)).
- Index REST examples: `dotcms-postman/src/main/resources/postman/ESIndexResourceTests.json`.
- Known-good VTL viewTool usage (integration tests): `SiteSearchWebAPITest`, `ESContentResourcePortletTest`.