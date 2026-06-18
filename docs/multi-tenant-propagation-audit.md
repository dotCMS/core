# dotCMS Tenant-Propagation Surface Audit

> Exploratory architecture audit (not committed guidance). Sizes the work to convert dotCMS
> from process-per-tenant to a single-JVM, multi-schema (tenant-by-DNS) model via a
> `TenantContext` ThreadLocal that must be set on every path and propagated to all detached work.
> 259 sites found, 140 high-risk. Generated 2026-06-18.

## 1. Bottom line

A **project, not a moonshot — but a large one with a hard floor of irreducible scattered work.**
Of 259 sites, the majority funnel through a small number of singletons you control:
`DbConnectionFactory`/`HibernateUtil` (all DB), `ChainableCacheAdministratorImpl`/`CacheLocator`
(all 43+ caches), `DotConcurrentFactory`/`DotSubmitter` (63 of 84 async sites), and Quartz base
classes `DotJob`/`DotStatefulJob`/`DotJobStore`. Fix tenant propagation at ~6–8 chokepoints and you
cover well over half the high-risk surface in one stroke. The catch: ~80 high-risk sites cannot be
fixed centrally — 21 raw-thread/ForkJoinPool sites, 35 per-job tenant-stamping decisions, ~30
static-mutable-state caches — each needing an individual touch and judgment call. That long tail,
plus the completely unpartitioned cache layer (keys built across 43 files), is the real cost driver.
Greenlight it, but budget for the scattered work, not the chokepoints.

## 2. Sizing table

| Dimension | Total | High-risk | Central chokepoint? | Residual scattered sites |
|---|---|---|---|---|
| Thread/Executor creation | 84 | ~10 | partial — 63 via `DotSubmitter`; 21 bypass | 21 raw `new Thread`/`Executors.new`/`CompletableFuture` |
| Quartz scheduled jobs | 35 | 23 | partial — 6 base-class chokepoints, but each job decides scope | ~35 jobs need per-job stamping or per-tenant registration |
| Cache key construction | 43 | 43 | yes (mechanism) / no (key strings) | ~41 cache impls build keys with no tenant dimension |
| DB access outside request | 12 | 8 | yes — `DbConnectionFactory`/`HibernateUtil` | StartupTasksExecutor + 243 runonce tasks (per-tenant iteration) |
| Pub/Sub & cluster threads | 18 | 10 | partial — `CacheTransportTopic.notify()` + payload | inval message `key:group` + `DotPubSubEvent` need tenant field |
| Static/singleton mutable state | 67 | ~45 | partial — APILocator/CacheLocator/Config/WebAppPool | ~30 static caches + ThreadLocals + 328 `getDefault*` calls |

## 3. The chokepoints that save you

- **DB connection** — `com/dotmarketing/db/DbConnectionFactory.java` (ThreadLocal connection holder, ~line 99/237) + `HibernateUtil.java` (session ThreadLocal). Route schema selection (`SET search_path`/datasource pick) through `getConnection()` keyed off `TenantContext`. Covers all 164+ DB call sites by construction.
- **Cache get/put** — `com/dotmarketing/business/ChainableCacheAdministratorImpl.java` (`put`/`get`, lines 263–333) + `CacheLocator.java` (142–417). Prepend tenant to the key at this single layer instead of editing 43 impls — **if** no impl bypasses the key-build path. Highest-leverage fix in the audit.
- **Async submission** — `com/dotcms/concurrent/DotConcurrentFactory.java` (`DotSubmitter.submit()/execute()`, 916–1099) + `getScheduledThreadPoolExecutor()`. Decorator captures `TenantContext` at submit, restores at run. Covers 63 of 84 async sites.
- **Quartz execution** — `com/dotmarketing/quartz/DotJob.java` (`execute()`, 39–46) + `DotStatefulJob.java`. Restore tenant from `JobDataMap` before `run()`. With `DotJobStore.java` (datasource) + `QuartzUtils.scheduleTask()` (stamp tenant at schedule time) = the job-system spine.
- **Pub/Sub dispatch** — `com/dotcms/cache/transport/CacheTransportTopic.java` (`notify()` 83–89, `inval()` 97–106) + a tenant field on `com/dotcms/dotpubsub/DotPubSubEvent.java`. One handler extracts tenant from payload; covers Postgres/JDBC/Redis listeners.
- **Default-identity lookups** — `com/dotmarketing/cms/factories/PublicCompanyFactory.java` (`getDefaultCompanyId`) + `LanguageAPI.getDefaultLanguage()`. The 328 `getDefault*` calls bottom out here; making these tenant-aware neutralizes most.

## 4. The irreducible scattered work (the cost driver)

- **21 async bypass sites** that never touch `DotSubmitter`. Highest-risk: `ShutdownCoordinator.java` (raw `Executors.newSingleThreadExecutor` + no-executor `supplyAsync`, 112/119/340), `JobQueueManagerAPIImpl.java` (`newFixedThreadPool` 206, `newSingleThreadScheduledExecutor` 1344), `IntegrityResource.java` (raw `new Thread` 453), `TimeMachineAjaxAction.java` (346), `PopulateContentletAsJSONUtil.java` (ForkJoinPool 183), `BrowserAPIImpl.java` (`supplyAsync` ForkJoinPool 1081 — inconsistent with its own chokepoint use at 857/998/1093). Each rewritten to route through `DotSubmitter` or wrapped manually.
- **Per-job tenant scoping** across ~35 Quartz jobs. Base class can *restore* tenant, not *decide scope*. `DropOldContentVersionsJob`, `EscalationThread`, `UpdateRatingThread`, `CleanUnDeletedUsersJob`, `StartEndScheduledExperimentsJob`, `PublisherQueueJob` run system-wide today. Each: per-tenant loop or stamp tenant at schedule. No central fix.
- **Cache key strings** in 41 impls (`PermissionCacheImpl`, `HostCacheImpl`, `ContentletCacheImpl`, `IdentifierCacheImpl`, `RoleCacheImpl`, `UserCacheImpl`, …) — individually fixable only if you reject the chokepoint key-rewrite. Either way the cross-cluster inval message format (`key:group`) must carry tenant, interacting with every impl's group naming.
- **~30 static mutable caches / ThreadLocals** with no tenant dimension: `DotTemplateTool.cache`/`layoutCache`, `CustomFieldType.customFieldTypes`, `WebAppPool`, `UserPreferencesFactory.usersPreferences`, `ImportAuditUtil.cancelledImports`, `ClickstreamListener.clickstreams`, plus app-secret/SAML key derivation off `getDefaultCompany()` (`AppsKeyDefaultProvider`, `SAMLHelper`). Each a bespoke partition-or-key change.
- **243 runonce + 10 runalways startup tasks** (`StartupTasksExecutor`, `AbstractJDBCStartupTask`) — need a **per-tenant iteration loop** around the executor, not propagation. `db_version`/`data_version` bookkeeping must become per-schema.

## 5. Silent-corruption hotspots (dictate a fail-loud design)

A missed propagation = tenant A reads/writes tenant B's data. These require **throw on unset TenantContext**, never a default-company fallback:

- **`PermissionCacheImpl.java`** (59/64/77) — ACLs keyed by identifier only → cross-tenant privilege escalation.
- **`RoleCacheImpl.java`** (rootRolesGroup, 53) — root roles cached globally → privilege escalation.
- **`UserCacheImpl.java`** (21–24/42) — users keyed by id/email only → impersonation/enumeration.
- **`AppsCacheImpl.java`** + **`AppsKeyDefaultProvider.java`** / **`SAMLHelper.java`** — app secrets + SAML/encryption keys derived from one `getDefaultCompany()`. One global key encrypting all tenants' secrets = worst-case contamination.
- **`ContentletCacheImpl.java`** (62/88) / **`HostCacheImpl.java`** (shared DEFAULT_HOST, 55–69) / **`IdentifierCacheImpl.java`** (64/69) — content/site/identifier readable by inode/identifier alone across tenants.
- **`DbConnectionFactory.getConnection()`** — if schema/search_path isn't bound to the active tenant before first query, a background thread silently writes to whatever schema the pooled connection last held. The single most dangerous write path.
- **System-wide Quartz jobs** (`DropOldContentVersionsJob`, `EscalationThread`, `CleanUnDeletedUsersJob`) — a missed scope here *deletes* the wrong tenant's data.
- **Cache inval `key:group` message** (`CacheTransportTopic.inval`; `DotPubSubEvent` has no tenant field) — an inval without tenant wipes the wrong tenant's cache cluster-wide.

Common thread: an unset/stale `TenantContext` on a detached thread defaults to *some* tenant. That default must be **no tenant → hard failure**, never "the first/default company," or these become invisible corruption.

## 6. Recommended sequencing

1. **Build the spine.** `TenantContext` ThreadLocal with a **fail-loud default** (DB/cache access with no tenant throws). Wire into the two corruption-gating chokepoints: `DbConnectionFactory.getConnection()` (schema binding) and `ChainableCacheAdministratorImpl.put/get` (key prefixing). Closes the two largest high-risk surfaces; the throw turns every *missed* scattered site into a loud test failure. This is the feasibility gate.
2. **Prove one thread boundary.** Wrap `DotSubmitter` capture/restore; exercise one request → async path end-to-end (e.g. `ContentletIndexAPIImpl` indexing). Then `CacheTransportTopic`/`DotPubSubEvent` tenant field — proves cross-node.
3. **Stamp the Quartz spine.** `QuartzUtils.scheduleTask()` writes tenant into JobDataMap; `DotJob.execute()` restores it. Triage the 35 jobs: per-tenant loop vs. legitimately system-scoped (cluster heartbeat, ES cleanup). Decision-heavy, not mechanical.
4. **Long tail (the real budget).** 21 async bypass sites, ~30 static caches/ThreadLocals, per-tenant startup-task iteration (`StartupTasksExecutor` + 243 tasks), `getDefault*` neutralization. Sequence *after* fail-loud is live so the throw surfaces them as failing tests empirically. Treat `ShutdownCoordinator`, `JobQueueManagerAPIImpl`, and app-secret/SAML key derivation as must-fix-before-prod regardless of test coverage.

**Key risk — RESOLVED (verified 2026-06-18):** the cache dimension is the **cheap** path. All 43 impls funnel through `ChainableCacheAdministratorImpl`'s `get/put/remove/removeLocalOnly/flushGroup/flushGroupLocalOnly`, each of which normalizes `group.toLowerCase()` and passes `(group, key)` to the provider, which partitions storage **by group** (`CaffineCache.getCache(group)` → per-group `DynamicTTLCache`). Prefixing the group with the tenant in those ~6 methods isolates both local storage and the cross-cluster invalidation message (`transport.send(k + ":" + g)` / `"0:" + group`, built in the same layer — tenant rides along for free). No `*CacheImpl` bypasses the funnel; `CacheOSGIService` only registers provider implementations per region, not a key path. The 43 impls stay untouched.

Two riders that ship with the prefix:
- **Per-region config is keyed by group name** — `getCache(cacheName)` resolves size/TTL from `cache.<group>.*` by region name, so tenant-prefixed groups silently fall to defaults. Strip the tenant prefix when resolving region config (one spot).
- **`CaffineCache.groups` is capped at `maximumSize(10000)`** (line 49). Regions become G×tenants; at ~150 groups that's ~66 tenants before whole regions evict → thrash. Derive this ceiling from tenant count.
