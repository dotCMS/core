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

> **Scope note (addenda §7–§8):** §1–6 cover the **Postgres plane only**. Two follow-up audits extend the picture: **§7** — other isolation planes (filesystem assets need *no* work — UUID keys make a shared folder safe; but Elasticsearch needs a per-tenant index strategy, the push-publish/integrity tree needs tenant keys, and **cross-tenant delete jobs are the highest-severity, rollback-unsafe gate**). **§8** — shared-key/runtime security hazards: the **JWT signing key** (`jwt_secret.dat`, one per JVM → cross-tenant auth bypass) and **app-secrets encryption key** (one installation key over all tenants) are pure-security must-fix gates; **OSGi/Felix** is one container per JVM with *no clean fix* — a likely **fundamental blocker** if the product must allow tenant-installed or untrusted plugins. Decide the plugin-trust promise first; it's the biggest fork in the road.

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

## 7. Addendum: isolation planes beyond the Postgres schema

### The core realization

Multi-schema-by-DNS isolates **exactly one plane: Postgres rows**. dotCMS persists tenant content across more planes the schema boundary never touches — the **filesystem/object-store asset tree**, the **Elasticsearch index set**, and **server-scoped runtime files** (push-publish bundles, integrity CSVs). But not all of these need partitioning: **assets are keyed by `inode`/identifier, which are random globally-unique UUIDs**, so a shared asset folder is *correct as-is* — two tenants can never collide on a path (confirmed with the codeowner). That removes the largest item the raw audit flagged (no asset-path partitioning, no asset-tree migration). What genuinely remains: **Elasticsearch** (a separate datastore with no tenant dimension), the **publishing/integrity tree** (carries tenant content with no tenant key), and a cross-cutting **delete-job hazard** that is now the *primary* filesystem concern. The original spine (`TenantContext` + fail-loud) gates DB and cache and does nothing for ES or the delete jobs.

### Per-plane sizing

| Plane | Isolated by schema split? | Work needed | Residual risk |
|---|---|---|---|
| Filesystem asset storage | No, but **doesn't need to be** — inode/identifier are unique UUIDs | None for storage; shared folder is safe | Only via cleanup jobs (below) + harder per-tenant backup/restore (assets interleaved by UUID) |
| Object store (S3) | Same — UUID keys, shared bucket fine | None for collision | Same as filesystem |
| `/assets/server` + publishing tree | No — global `/assets/bundles`, `/assets/integrity/<endpoint_id>` | Tenant-key the bundle/integrity path generators; add `host_inode` to `*_ir` PKs | Push-publish bundles & integrity CSVs carry tenant content with no tenant key → cross-tenant bundle apply / CSV leakage |
| Elasticsearch index | No — separate datastore | **New design**: index-per-tenant or tenant-aware resolution; tenant column on `indicies` + `dist_reindex_journal` | All tenants index into one working/live pair → cross-tenant search results |
| Reindex worker | No — one daemon, one journal | Make journal + `loadIndicies` tenant-aware, or index-per-tenant | `ReindexThread` JVM singleton writes to global indices |
| Cleanup / delete jobs | No — date/inode-driven, system-wide | **Per-job tenant scoping** (highest severity) | Deletes span DB+FS(+ES) with no `host_id` filter → erases wrong tenant irreversibly |

### Filesystem assets — shared folder is fine (corrected)

The path is built in `FileAssetAPIImpl.getRealAssetPath(inode, fileName, ext)` (604–623) as `ROOT/{inode[0]}/{inode[1]}/{inode}/<field>/{fileName}` off global `ASSET_REAL_PATH`/`ASSET_PATH`. Because `inode` is a random UUID, two tenants never produce the same path, so **no tenant segment is needed in the asset root or the S3 key, and no on-disk migration is required.** Access isolation also holds without FS partitioning: files are served through dotCMS's permission layer keyed by identifier within the active schema, so you can't request another tenant's asset (you don't know its UUID and the permission check is schema-scoped); raw-FS adjacency isn't an exposure. Two real consequences remain:
- **Orphan-reconciliation cleanup is the only filesystem hazard** — see delete jobs below. A job that walks `/assets` and deletes files whose inode is "not in the DB" sees *other tenants'* files as orphans when it checks only the current schema.
- **Per-tenant backup/restore/migration is harder** — assets are interleaved by UUID in one tree, so extracting one tenant's assets requires enumerating its inodes from its schema. Operational, not a blocker.

### Elasticsearch + ReindexThread (the genuinely new plane)

`ReindexThread` is a **JVM singleton** (`com/dotmarketing/common/reindex/ReindexThread.java`: static `instance` line 87, `getInstance()` 317 w/ double-checked locking). One daemon serves every tenant, polling one `dist_reindex_journal` (`ReindexQueueFactory.loadUpLocalQueue` 318–354, sharded only by `MOD(id,numServers)` — no tenant column), resolving its write target from a single `indicies` table row (`IndiciesFactory.loadIndicies` 36–72). Index names are built in one chokepoint — `IndiciesInfo.createNewIndiciesName` (159–173), `cluster_<clusterId>.<working|live|sitesearch>_<ts>` — which *can* take a tenant prefix, but naming is the easy half: **every read** (`ContentFactoryIndexOperationsES.inferIndexToHit` 372–387) and write resolves from the global `indicies` table that has no tenant column. The decision:
- **Per-tenant iteration on the shared daemon** — keep one `ReindexThread`, make the journal query and `loadIndicies` tenant-aware. Lower memory; every resolution callsite must carry tenant context; journal needs a tenant column.
- **Index-per-tenant** — prefix in `createNewIndiciesName`, add a tenant column to `indicies` (row per tenant), resolve off `TenantContext`. More indices (shard pressure × tenants) but clean isolation, the obvious fit for multi-schema.

Either way needs **new tenant columns on `indicies` and `dist_reindex_journal`** — this is new engineering, not propagation, and warrants its own design spike.

### /assets/server — split by who owns the bytes

- **Installation-global, keep shared (correct as-is):** `/assets/license`, `server_id.dat`, `/assets/server/<serverId>/heartbeat.dat` (`ServerAPIImpl.readServerId` 112–142, `writeHeartBeatToDisk` 196–217). One-per-JVM cluster artifacts; partitioning them would be wrong.
- **Tenant content, must partition:** push-publish bundles (`ConfigUtils.getBundlePath` → `/assets/bundles/<uuid>.tar.gz`, `BundlePublisher.process` 131–183) and integrity data (`ConfigUtils.getIntegrityPath` → `/assets/integrity/<endpoint_id>/*.csv`). Keyed by bundle UUID / `endpoint_id` with **no source/target tenant**, so a shared endpoint lets Tenant A's bundle apply against Tenant B, and the `*_ir` tables (PK `(local_inode, endpoint_id)`, no `host_inode`) mix tenants' conflict sets. Fix: tenant-prefix the bundle/integrity path generators and add `host_inode` to the `*_ir` PKs.

### Cross-tenant delete jobs (highest severity)

The finding that should drive the verdict. **15 delete-path sites**; the worst run system-wide and span planes, so a single missed scope **destroys the wrong tenant's data across DB *and* disk at once** — irreversible.

| Job / thread | System-wide? | Planes | Why fail-loud scoping is mandatory |
|---|---|---|---|
| `DropOldContentletRunner.deleteOldContent` (35–48 SQL, 209–228 FS) | Yes — `mod_date`, **no `host_id` WHERE** | DB + FS | Deletes rows *and* `deleteFromAssetsDir` binaries; a date batch spanning tenants erases Tenant B's files |
| `DropOldContentVersionsJob` (138–191) | Yes — by date | DB + FS | Excess-version deletes lack site scope |
| `BinaryCleanupJob` (65–145) + `TrashUtils.emptyTrash` (58–74) | Yes — purges shared trash/bundle/tmp by mtime | FS | Empties all tenants' pending data |
| `CleanAssetsThread.deleteAssetsWithNoInode` (120–171) | Yes — walks `/assets`, unscoped `find(inode)` | DB-check + FS | **The shared-folder hazard:** inode absent in the *current* tenant's view → deletes another tenant's binary |
| `ContentTypeDeleteJob` → `ContentTypeDestroyAPI.destroy` | Yes — no `host_id` | DB (+FS cascade) | Destroys all contentlets of a CT shared across sites |
| `DeleteFieldJob` / `CleanUpFieldReferencesJob` → `cleanField` | Yes — structure-wide | DB | Clears a field's data across every site using that structure |
| `IntegrityUtil` / `ContentPageIntegrityChecker` | Per-endpoint, **not per-tenant** | DB(`*_ir`) + FS(CSV) | Shared endpoint → fixing Tenant A's conflicts mutates Tenant B's |
| `DeleteInactiveLiveWorkingIndicesJob` | Yes — ES indices | ES | With index-per-tenant, prunes the wrong tenant's index |
| `DeleteUserJob` / `UsersToDeleteThread` | Yes — single-company user table | DB | User is Liferay-global; content reassignment crosses tenants |

Structural reason all are deadly: **FS paths are keyed by inode with no site dir, while DB rows *have* `host_id` but the delete queries don't filter on it.** Both planes must be scoped *together*; today neither is. This is why the fail-loud rule must extend past DB/cache to the **job execution context**: a delete job running with an unset/defaulted tenant must **throw, not fall back to default company** — a silent default on a delete path is multi-plane data loss. Per-job, decision-heavy: add `host_id` to every version/asset delete WHERE clause, scope FS traversal to the tenant's known inodes, batch by site, and pass tenant via the `JobDataMap` from the Quartz spine. See `docs/core/ROLLBACK_UNSAFE_CATEGORIES.md` — a bug here is permanent.

### Revised verdict

The original "feasible but expensive" call holds, and the **UUID insight pulls the estimate back down** from the raw audit's "4× surface": filesystem asset storage needs *no* partitioning and *no* migration. The genuinely-new work beyond the DB/cache spine is three things, not a blanket 4×:

1. **Elasticsearch** — a separate datastore needing an index-per-tenant-vs-resolution decision + new tenant columns on `indicies`/`dist_reindex_journal` + rethinking the singleton `ReindexThread`. **Its own design spike.**
2. **Publishing/integrity tree** — tenant-key the bundle/integrity path generators + `host_inode` on `*_ir` PKs. Bounded.
3. **Delete-job scoping** — highest severity, rollback-unsafe, per-job adversarial verification (not mechanical sweeps). **Gating, must-fix-before-any-prod-tenant** alongside the DB/cache spine.

The trap to flag: if schema isolation ships *alone*, a nightly cleanup job deletes the wrong tenant's assets via the shared folder — a *worse* outcome than no isolation, because it looks isolated. So delete-job scoping is a hard gate even though filesystem storage itself is free.

## 8. Addendum: shared-key & shared-runtime hazards

### The new failure class

Sections 5–7 were about **partitioning storage** — Postgres rows, cache groups, ES indices, files. A *missed* site there corrupts or leaks data, and a fail-loud `TenantContext` turns the miss into a test failure. This addendum is a different animal. These three surfaces are not about storage partitioning at all — they are **shared cryptographic keys and a shared code-execution runtime** baked into JVM-global singletons. The failure mode is not corruption you can detect; it is **cross-tenant authentication bypass, wholesale secret disclosure, and arbitrary cross-tenant code execution**. Crucially, two of the three are *pure security* problems, not isolation gaps: the JWT signing key and the app-secrets encryption key would be a cross-tenant break **even if every storage plane were perfectly partitioned**, because a single key validates/decrypts for all tenants regardless of where the bytes live. OSGi is partly isolation and partly pure code-execution trust. None of the three is closed by schema splitting, and a fail-loud `TenantContext` does **not** surface them — a token forged with the shared key is *cryptographically valid*, so nothing throws.

### 8.1 JWT signing key — the sharpest, lead here

There is exactly **one** symmetric JWT signing key per JVM, and it authenticates every tenant.

- **Where it lives / chokepoint:** `{ASSET_REAL_PATH}/server/jwt_secret.dat`, a single file. Path is built with zero host/schema dimension in `KeyFactoryUtils` (`SECRET_FILE_NAME = "jwt_secret.dat"` line 16; path assembly lines 123–143; `readSecretFromDisk()` lines 96–111) on top of the JVM-global assets root from `ConfigUtils.getAbsoluteAssetsRootPath()` (lines 166–168). Default impl that reads it: `SecretKeySpecFactoryImpl.getAndProcessSecret()` (lines 45–84) — no tenant/host parameter exists in the signature.
- **JVM singleton:** yes. `JsonWebTokenFactory.JsonWebTokenServiceImpl` caches the key in a `volatile signingKey` field (lines 110, 113–132), loaded once and reused. `signToken()` (177–183) signs *all* issuances with it; `parseToken()` (222–226) validates *all* tokens against it. The issuer claim is `ClusterFactory.getClusterId()` (line 137) — cluster-wide, not per-host, confirming no tenant dimension anywhere in the token.
- **Concrete attack:** a JWT (API token or remember-me cookie) minted while serving **tenant A** is cryptographically valid when presented to **tenant B**, because the signature check is the same key. The validation layer never receives the host: `JsonWebTokenUtils.getUser()` (65–69) and `DefaultAutoLoginWebInterceptor` (65–71, 81–83) call `ApiTokenAPI.fromJwt()` with no host/schema context. If the claimed user also exists under tenant B (common for shared admin identities or guessable IDs), this is a direct auth bypass. The DB schema reinforces it: `api_token_issued` (`ApiTokenSQL` 41–57) has no `host_id`/`tenant_id` column, so tokens are globally scoped at rest too.
- **What per-tenant signing requires:** namespace the secret by host/schema — `assets/server/{hostId}/jwt_secret.dat` — and thread a tenant identifier through `getSigningKey()` / `signToken()` / `parseToken()` so a token carries and is validated against its issuing tenant's key (and add a tenant claim + check). This is a real but **bounded** change: the chokepoints are few (`KeyFactoryUtils`, `JsonWebTokenServiceImpl`, `SecretKeySpecFactoryImpl`). `schemaSplitHelps: true` here — a per-schema key store is a clean fit.

### 8.2 App config / secrets — one installation key protects everyone

**Stated plainly: a single installation-wide key encrypts every tenant's app secrets.** Per-host composite aliases (`{hostId}:{appKey}`, `AppsAPIImpl.internalKey()` 105–113) provide only a logical namespace *inside* one shared store — they do **not** isolate the cryptography.

- **Where stored:** one PKCS12 keystore, `{ASSET_REAL_PATH}/server/secrets/dotSecretsStore.p12` (`SecretsKeyStoreHelper.getSecretStorePath()` 71–76) — single file for the whole installation.
- **Key derivation:** `AppsKeyDefaultProvider.getKey()` (7–15) returns `getDefaultCompany().getKeyObj()` — the **default company key**, one value for all tenants — and `SecretsKeyStoreHelper.key()` (284–296) falls back to it with no per-host derivation. The keystore *password* is `digest(ClusterFactory.getClusterSalt())` (`SecretsKeyStoreHelper` 37–40; cached in `SecretCachedKeyStoreImpl` 36–46) — installation-wide from `dot_cluster.cluster_salt`. Compromise of the file **or** the default-company key discloses **all tenants' secrets at once**; `resolveSecrets()` (`AppsAPIImpl` 250–310) decrypts everyone with the same key.
- **Chokepoint to make it per-tenant:** `AppsKeyDefaultProvider.getKey()` / `SecretsKeyStoreHelper.key()` — derive a per-host key (HKDF from cluster salt + `hostId`, or a per-host company key) so the alias namespace is matched by a key namespace. `APPS_KEY_PROVIDER_CLASS` already allows a custom provider, so the seam exists; the fix is a tenant-aware provider plus per-host keystore files (or per-host aliases encrypted under distinct keys).

### 8.3 OSGi / Felix — the hard one, likely a true blocker

There is **one Felix `Framework` per JVM**, and it has no tenant concept at any layer.

- `OSGIUtil` is a singleton holding one `felixFramework` (113–119, 136), created once in `initializeFramework()`. Bundles deploy to one shared dir (`FELIX_FILEINSTALL_DIR` / load, 177–188) and `HostActivator` exposes one `BundleContext` for the whole JVM (12–19). Every registration is global: `registerActionlet()` adds to a JVM-wide map and `OSGIUtil.setWorkflowOsgiService()` (`GenericBundleActivator` 562–576); `registerPortlets()` (343–375) makes portlets visible to all hosts and copies resources to webapp-global `/html/osgi/{jar}` and `/velocity/osgi/{jar}`. Plugin code calls `APILocator.getWorkflowAPI()` etc. with **no tenant context at init** (218–227). `OSGIResource.undeploy()` (310–348) is JVM-wide — one tenant can pull a plugin out from under another.
- **Is per-tenant plugin isolation even possible without re-architecting Felix?** Realistically, no — not as a chokepoint fix. The framework, its classloaders, static fields, and service registry are one shared graph in a single JVM. The honest options:
  1. **Global-plugins-only, documented limitation (recommended near-term).** Treat all installed bundles as trusted, installation-global code; only super-admins (not tenant admins) may deploy/undeploy; document that plugins see all tenants. Cheapest, ships now, but means *you must trust every plugin author with all tenants' data*.
  2. **Classloader-per-tenant.** Load bundles in per-tenant child classloaders to separate static state. Large, invasive, fights Felix's own classloading; brittle and high-maintenance. Not recommended.
  3. **Framework-per-tenant (or process-per-tenant).** A true Felix instance per tenant gives real isolation but breaks the single-JVM model this whole audit assumes — effectively a deployment-topology change, not a code change.
- **Verdict: no clean chokepoint fix.** Unlike JWT and app-secrets, there is no small set of methods to tenant-ize. This is an architectural property, not a bug.

### 8.4 Severity-ranked actions

Ordered by security severity (all three rated CRITICAL by finders; ranked here by sharpness × fixability):

1. **JWT signing key (CRITICAL, pure security).** Chokepoints: `KeyFactoryUtils` path derivation + `JsonWebTokenServiceImpl.getSigningKey/sign/parse` + `SecretKeySpecFactoryImpl`. **Must-fix-before-any-multi-tenant-prod — gate.** This is the cheapest of the three to fix and the most dangerous to ship broken (silent auth bypass).
2. **App config / secrets (CRITICAL, pure security).** Chokepoint: `AppsKeyDefaultProvider.getKey()` / `SecretsKeyStoreHelper.key()` → per-host key derivation; custom-provider seam already exists. **Must-fix-before-any-multi-tenant-prod — gate** (single-compromise discloses all tenant secrets).
3. **OSGi / Felix (CRITICAL, isolation + code-execution trust).** **Verdict: no clean fix.** Not a gate *if* you ship option (1): global-plugins-only as a documented, super-admin-gated limitation. It becomes a hard gate the moment the product promises tenant-admin-installable plugins or untrusted-tenant isolation — that requires framework-per-tenant.

### 8.5 Impact on the overall verdict

For the go/no-go call: **JWT and app-secrets do not flip the verdict.** They are genuinely scary (silent cross-tenant auth bypass; one key over all secrets) but each is a bounded, chokepoint-shaped change with an existing extension seam — they fit the "hard but doable" framing as **hard must-fix gates**, not blockers.

**OSGi is the one that can flip it, and the answer depends on the product promise.** If the multi-tenant product can live with **trusted, installation-global plugins administered only by the platform operator** (not tenant admins), OSGi is a *documented limitation*, not a blocker, and the project stays "hard but doable." If the product must let tenants install plugins or must isolate untrusted tenants from each other's plugin code, then OSGi is a **fundamental blocker** in the single-JVM model — there is no chokepoint that fixes it, only a topology change to framework-/process-per-tenant. **Recommendation to the senior engineer:** make the plugin-trust decision *explicitly and first*; it is the single largest fork in the road and it sits outside the chokepoint strategy that makes the rest of this migration tractable.
