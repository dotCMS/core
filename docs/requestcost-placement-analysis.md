# RequestCost — profile-driven gap analysis and action list

Glowroot main-thread profiles, 7-day windows, read 2026-08-07.

| Tenant | Samples | Shape | Top transactions |
|---|---|---|---|
| Tenant F | 17,706 | traditional site | page renders, `/dA/` binaries |
| Tenant A | 68,378 | write-heavy API | workflow fire 54%, graphql 23%, es/search 16% |
| Tenant B | 12,126 | read-heavy API | graphql 60%, `/api/vtl/*` 31%, page render 7% |
| Tenant C | 8,781 | velocity + remote API | product browse pages |
| Tenant D | 893 | graphql | graphql 54%, es/search 12%, page render 10%, content 20% |
| Tenant E | 53 | — | **ignored — 53 samples is noise** |

## Headline

Six instances, six different cost centres. The model's prices are the dominant
cost for exactly one of them.

| Tenant | Where the time actually goes | Costed? |
|---|---|---|
| Tenant F | filter chain (DB), velocity render, binaries | partly |
| Tenant A | `addContentToIndex` — ~10% of all samples, blocking | **no** |
| Tenant B | prerender park 36%, `NavTool.getNav` 11% | **no** |
| Tenant C | `CircuitBreakerUrl` outbound HTTP ~50% | yes — but was priced 4, now 100 |
| Tenant D | GraphQL execute + transform + relationship N+1 | **no** |

Standing caveat: a sampled profile gives **time share, not invocation counts**.
It reliably shows where instrumentation is *missing*; it cannot set per-call
prices. Only the cost model itself, once placed, produces the counts you need.

---

# Ranked

Ordered by (currently-invisible, client-controllable cost made visible) ÷ effort.

**All items are resolved.** #14 was decided in favour of the **estimate model**, with
the line drawn at what the customer can influence — see §4.

| # | Do | Effort | Why here |
|---|---|---|---|
| 1 | ✅ Charge `CONTENT_FROM_CACHE` per contentlet + `CONTENT_FROM_DB` per miss in `findContentlets(List)` | 2 lines + 1 API overload | Universal: GraphQL, `/api/content`, `/api/es/search`, page render all bulk-load here. Makes cost scale with **rows returned** — the main lever a client has. §1.1 |
| 2 | ✅ `CONTENT_INDEX` on `addContentToIndex` | 1 annotation + funnel check | ~10% of the largest tenant's samples, blocking, and no indexing price exists at all. §2 |
| 3 | ✅ Reprice `HTTP_FETCH` (4 → 100) | one number | ~50% of Tenant C's samples. Was barely above `VELOCITY_MERGE`. §3 |
| 4 | ✅ Reprice `CONTENT_GET_RELATED` (1 → 10) | one number | Hottest priced constant in API traffic, and it recurses. One `?depth=` bump multiplies work at no cost. §3 |
| 5 | ✅ `HTTP_FETCH` on `PreRenderSEOWebAPIImpl` | 1 annotation | 36.2% of Tenant B parked in `Unsafe.park`; own HttpClient means the existing charge never fires. §1.7 |
| 6 | ✅ `NAV_BUILD` on `NavTool.getNav` | 1 annotation + funnel check | 11% of Tenant B's total samples, DB-blocked, recursive. §2 |
| 7 | ✅ `ES_QUERY` on `enterprise.priv.ESSearchAPIImpl.esSearchRaw` | 1 annotation | `/api/es/search` = 12% Tenant D, 16% Tenant A, currently free. §1.6 |
| 8 | ✅ `VELOCITY_MERGE` on `DotDirective.render` (miss branch) | 1 annotation | Biggest *relative* distortion for velocity tenants — 30 containers price like 1. §1.4 |
| 9 | ✅ `VELOCITY_MERGE` on `VTLResource.processRequest` | 1 annotation | 31% of Tenant B's transactions charge no merge. §1.5 |
| 10 | ✅ `CONTENT_FROM_DB` on **both** `findInDb` variants | 2 annotations | The cache-miss surcharge on the single-contentlet path. Not a delete — the two are not duplicates, see §1.2. |
| 11 | ✅ `find()` charges the `CONTENT_FROM_CACHE` base fee | comment | Correct as-is once the miss surcharge lands in `findInDb`. §1.3 |
| 12 | ✅ Guard the unconditional log concat in `RequestCostApiImpl` | 1 line | Done by the author. §6.4 |
| 13 | ✅ `GRAPHQL_QUERY` on `DotGraphQLHttpServlet.handleRequest` | 1 annotation | ~4% in parse+validate, and the only charge that scales with **query size** rather than rows. §2 |
| 14 | ✅ **Decided: estimate, split on what the customer controls** | design call | Cache-vs-DB is priced (theirs); statements-per-miss is not (ours). `DB_QUERY` retired. §4 |
| 15 | ~~`LANGUAGE_VARIABLE`~~ — **dropped, already metered** | — | The ES query it triggers is charged as `ES_QUERY` on the cache-miss branch. Adding it would double-charge. See below. |
| 16 | ~~per-fetcher `GRAPHQL_FIELD_FETCH`~~ — **dropped** | — | The fetchers resolve content, now charged per row by #1, and parse/validate by #13. Adding it would count the same work twice at a different altitude. |
| — | ✅ Filed as [#36970](https://github.com/dotCMS/core/issues/36970) | — | ~20–25× per request-profile, pure waste. Not a costing item. §7.1 |
| — | ✅ Filed as [#36971](https://github.com/dotCMS/core/issues/36971) | — | One user query per response row. §7.2 |
| — | **Don't** charge the filter chain | — | ~15% of the velocity profile but fixed per request and not client-controllable. §5 |

Rows 1–4 are where the return is concentrated: they are four small changes that
cover every tenant archetype in the sample.

# Action list

## 0. Applied

16 files, +218/−46. Compiles clean — verified with `javac` against the built
classpath, error count identical to the HEAD baseline. (The full `mvn` build
fails in `dotcms-core-web`'s nx step and `dotCMS/target/classes` is stale
relative to HEAD; both pre-existing and unrelated.)

**Content-op terminals** (`ESContentletAPIImpl`) — each verified as the method
its overload chain funnels into:

| Price | Method | Why this one |
|---|---|---|
| `CONTENT_CHECKIN` | `internalCheckin` (private terminal) | 12 `checkin` overloads chain into it |
| `CONTENT_CHECKOUT` | `checkout(String, User, boolean)` | list variants loop over it → per-contentlet |
| `CONTENT_COPY` | 7-arg `copyContentlet(…, ContentType, Host, Folder, …)` | 8 overloads funnel here |
| `CONTENT_MOVE` | 4-arg `move(…, Host, Folder, boolean)` | 3 overloads funnel here |

Copy calls checkin internally, so a copy costs 25+50=75 — a composite, worth
knowing when reading traces.

**Per-contentlet content charge** — `ESContentFactoryImpl.findContentlets(List)`
charges `inodes.size()` × `CONTENT_FROM_CACHE` as a base fee, plus
`missingCons.size()` × `CONTENT_FROM_DB` as a cache-miss surcharge. The surcharge
is per missed **row**, not per SQL statement — the 200-row batching is ours, not
the customer's. Uses a new
`incrementCost(Price, Class, String, Object[], int times)` on `RequestCostApi`
(the 4-arg form delegates with `times = 1`; the HTML accounting entry and the log
line report the multiplied cost).

**New terminals annotated:**

| Price | Method | File |
|---|---|---|
| `CONTENT_INDEX` (new, 25) | `addContentToIndex(List<Contentlet>)` | `ContentletIndexAPIImpl` |
| `NAV_BUILD` (new, 10) | `getNav(Host, String, long, User)` — terminal of 5 overloads | `NavTool` |
| `ES_QUERY` | private `esSearchRaw(JSONObject, …)` — terminal of both public paths | `enterprise.priv.ESSearchAPIImpl` |
| `HTTP_FETCH` | `proxyPrerenderedPageResponse` — the actual HTTP call, not the eligibility check | `PreRenderSEOWebAPIImpl` |
| `VELOCITY_MERGE` | private `evalVelocity` | `VTLResource` |
| `VELOCITY_MERGE` | `DotDirective.render`, imperative, **past** the `getFromCache()` short-circuit | `DotDirective` |

**The whole price table was re-based on resource-time.** A price is now an
order-of-magnitude estimate of the CPU, heap, or parked-thread time an operation
consumes. Cache reads are the unit; everything is a ratio to that:

```
  1  in-memory cache read              CONTENT_FROM_CACHE, ES_CACHE, FILE_METADATA_FROM_CACHE
  2  per-item work in memory           VELOCITY_BUILD_CONTEXT, BLOCK_EDITOR_HYDRATION
  5  render a template fragment        VELOCITY_MERGE
 10  CPU parse/compile, or one DB hop  VELOCITY_PARSE, XSLT_PARSE, GRAPHQL_QUERY, CONTENT_FROM_DB, CONTENT_GET_RELATED, NAV_BUILD, …
 25  one ES round trip, multi-qry write  ES_QUERY, ES_COUNT, CONTENT_INDEX, CONTENT_MOVE, CONTENT_COPY
 50  heavy CPU + heap, or a write txn  IMAGE_FILTER_TRANSFORM, FILE_METADATA_GENERATE, CONTENT_CHECKIN, CONTENT_DELETE
100  one remote HTTP round trip        HTTP_FETCH, XML/XSLT_FETCH_AND_PARSE
```

**Why a DB query is only 10x a cache read when it is ~1000x the latency:** what
is metered is capacity consumed *on this node*, not wall time. A query parks the
thread and burns the cycles on Postgres. The Velocity-tenant profile bears this
out — template rendering dominates those requests, not DB frames — and at an
earlier `DB_QUERY` of 25 the price table told a story the profile contradicted
(DB 50% of a page render vs. merges 37%). At 10 the same page is merges 59%, DB
31%, ES 10%, which matches. **If a node ever exhausts request threads before CPU,
this reasoning inverts and the DB / ES / HTTP tiers should go back up.**

The two biggest corrections this forced: `IMAGE_FILTER_TRANSFORM` and
`FILE_METADATA_GENERATE` were priced 2 and 3 — decoding/re-encoding an image and
running Tika over a binary burn a core and a large buffer, so they are 50.

**Content pricing took three passes to land** — worth recording, because two of
the three looked right at the time:

1. A full `CONTENT_FROM_DB` per missed row. **Wrong on units:** misses are fetched
   in batches of 200, so 1,000 rows priced as 1,000 queries when it is 5.
2. Cache-hit / miss-row / per-batch-query split. **Right on resource-time, wrong
   commercially:** charging per batch prices our implementation detail.
3. Base fee per contentlet + surcharge per missed row, no per-statement charge.
   **The one that shipped** — see §4 for why the line sits there.

**Reported tokens stay in their old range, and stay integral.** The larger
internal scale is divided back out on the way to anyone outside the JVM:

- `REQUEST_COST_DENOMINATOR` default 1.0 → **10.0**, so one reported token ≈ one
  DB round trip, which is roughly what a token meant under the old table.
- The `x-dotrequest-cost` header is **rounded** but still formatted `"%.2f"` — it
  has always looked like `"23.00"` and has always been a whole number, so neither
  the format nor the integrality changes for anything parsing it.
- `windowTokens` / `lifetimeTokens` in the pushed `RequestCostSnapshot` are
  rounded for the same reason: the field is typed `double` but has only ever
  carried whole numbers, and a collector parsing them as ints would break on a
  fractional value. The per-request *averages* are left fractional — they always
  were (`sum / count`).

Worked examples at denominator 10:

| Request | raw | reported |
|---|---|---|
| page: 30 containers, 8 DB queries, 1 ES query | 255 | 26 |
| the same page plus one remote API call | 355 | 36 |
| workflow fire (checkin + index) | 75 | 8 |
| single image resize | 50 | 5 |
| 1000-row cached GraphQL response | 1010 | 101 |
| the same 1000 rows cold (2/row + 5 batched queries) | 2060 | 206 |

**Rate-limit defaults scaled with the table** (`LeakyTokenBucketImpl`):
`RATE_LIMIT_REFILL_PER_SECOND` 500 → 5000, `RATE_LIMIT_MAX_BUCKET_SIZE`
10000 → 100000, preserving the old ratio. `RATE_LIMIT_ENABLED` still defaults
false. These knobs are undocumented and unset everywhere, so the defaults are the
only values in play — but note for whenever the limiter is turned on that **the
bucket drains in raw units, not denominated ones**: a limit is expressed on the
Price scale (one remote HTTP call = 100), not on the reported-token scale.

## 1. Metering holes on prices that already exist (no new constants)

**1.1 — `ESContentFactoryImpl.findContentlets(List<String>)` is the big one. FIXED.**
This is the bulk loader behind every search result: GraphQL, `/api/content`,
`/api/es/search`, page render. It reads `contentletCache` per inode, then
batch-SELECTs the misses 200 at a time. It was **uncosted**, and it bypasses
`ESContentletAPIImpl.find()` entirely — so the content annotation there never
fired for a search result. A GraphQL query returning 100 contentlets charged
zero for the content.

Now charged in two parts — base fee for everything asked for, surcharge for the
rows that missed:

```java
incrementCost(Price.CONTENT_FROM_CACHE, …, inodes.size());     // base
incrementCost(Price.CONTENT_FROM_DB,    …, missingCons.size()); // surcharge
```

The surcharge is per missed **row**, not per SQL statement — the 200-row batching
is ours, not the customer's.

Required one new API surface: a count-taking
`incrementCost(Price, Class, String, Object[], int times)` on `RequestCostApi`.
This makes cost scale with **rows returned** — the lever the client actually
controls — and with nothing else.

**1.2 — the single-contentlet DB path. Now deliberately unpriced.** `CONTENT_FROM_DB`
used to sit on `ContentletFactory.findInDb(String, String variant)`, an **interface
default method**. Java doesn't inherit method annotations and ByteBuddy matches
declared methods, so it only fired for `find(inode, variant)`; the common path
landed on `ESContentFactoryImpl.findInDb(String, boolean)` and charged nothing.

An early recommendation to "delete the duplicate override" was also wrong: the two
run the same SQL but differ in post-processing — the interface default filters by
`variantId`, the impl honours `ignoreStoryBlock` — so neither can delegate to the
other as written. The duplicated SQL is still a real smell and deserves its own
issue.

Both now carry `@RequestCost(Price.CONTENT_FROM_DB)` — reaching either method *is*
the cache miss, and `find()` has already charged the base fee, so a warm find
costs 1 and a cold one 11.

**1.3 — `find()` charges `CONTENT_FROM_CACHE` unconditionally, and that is
correct.** It reads as a bug in isolation, but it is the base fee; the miss
surcharge is added deeper, in `findInDb`. Warm 1, cold 11, without this method
needing to know which happened.

**1.4 — `#dotParse` / `#parseContainer` are free.** `DotDirective.render` →
`renderTemplate()` → `((SimpleNode) t.getData()).render(...)`, never through the
annotated `VelocityUtil.mergeTemplate`. A page with 30 containers charges the
same `VELOCITY_MERGE` as a page with 1. The velocity profile shows
`ASTDirective.render` nesting 5–6 deep at 7–11% — that nesting *is* the container
tree. `getFromCache()` already short-circuits, so mirror the `cachedIndexCount`
pattern: cheap on hit, full price on miss.

**1.5 — `/api/vtl/*` merges are free.** `VTLResource.processRequest:552` calls
`VelocityUtil.getEngine().evaluate(...)` directly, not the annotated
`VelocityUtil.eval`. 31% of Tenant B's transactions.

**1.6 — `/api/es/search` bypasses `ES_QUERY`.** `ESContentResourcePortlet.searchPost`
→ `com.dotcms.enterprise.priv.ESSearchAPIImpl.esSearchRaw` →
`RestHighLevelClient.performRequest`, never touching
`ContentFactoryIndexOperationsES.cachedIndexSearch` where the imperative
`ES_QUERY` charge lives. 12% of Tenant D, 16% of Tenant A. The class is at
`dotCMS/src/enterprise/java/...` and `com.dotcms.*` is in the ByteBuddy
whitelist, so a plain annotation works.

**1.7 — Prerender holds a request thread for free.** `PreRenderSEOWebAPIImpl`
uses its own `CloseableHttpClient`, not `CircuitBreakerUrl`, so `HTTP_FETCH`
never fires. Tenant B: `SimpleWebInterceptorDelegateImpl.intercept` →
`Unsafe.park` TIMED_WAITING at **36.2%**. Content-dependent (bot UA + eligible
page), so unlike the rest of the filter chain it *is* chargeable.

## 2. New prices

| Price | Placement | Evidence |
|---|---|---|
| `CONTENT_INDEX` | `ContentletIndexAPIImpl.addContentToIndex` — 3 overloads (`:2265/:2270/:2325`), find the terminal | Tenant A: `Object.wait0` WAITING at 9.9 / 9.5 / 9.3 / 8.3% ≈ **10% of all samples**. No indexing price exists at all. |
| `GRAPHQL_QUERY` | `DotGraphQLHttpServlet.handleRequest` | Tenant D: `parseInvocationInput` 1.8% + `ParseAndValidate.validate` / `LanguageTraversal.traverseImpl` ~2.5%. Real CPU **before any field is fetched** — this is the only charge that scales with query size rather than row count. |
| `NAV_BUILD` | `NavTool.getNav` — 4 overloads (`:319/:343/:347/:363`), terminal only | Tenant B: → `BrowserAPIImpl.getFolderContentList` → `Net.poll` = **11.4% + 10.3% of all samples**, DB-blocked, recursive via `NavResultHydrated.getChildren`. |
| ~~`LANGUAGE_VARIABLE`~~ | **dropped — already metered** | The profile frames are real (Tenant D: `getLanguageVariable` → `ESContentletAPIImpl.search` → `indexSearch` at 1.0 / 0.8 / 0.7 / 0.6 / 0.4%), but the path is `KeyValueAPIImpl.get` (own cache) → on miss → `indexSearch` → `searchHits` → `internalSearchHits` → `cachedIndexSearch`, which already charges `ES_QUERY` on **its** miss branch. Two cache layers, both already respected. A price here would double-charge. The N+1 shape is still real and is now *visible*: 50 language-variable misses on a page cost 50 × `ES_QUERY`. |

**Dropped from an earlier draft:** `CONTENT_TRANSFORM` on
`AbstractTransformStrategy.apply`. Once 1.1 charges per row, this is largely
redundant — it scales with the same row count, and it fires per-*strategy*
(several run per contentlet), so it would triple-count.

**Optional:** per-fetcher `GRAPHQL_FIELD_FETCH` on the 7 data fetchers
(`ContentletDataFetcher`, `ContentMapDataFetcher`, `FileFieldDataFetcher`,
`SiteFieldDataFetcher`, `UserDataFetcher`, `page.PageDataFetcher`,
`page.ContainersDataFetcher`). Only worth it if you want query *depth* priced
separately from rows — 1.1 + `GRAPHQL_QUERY` covers most of it. Note
`ContainersDataFetcher` → `PageRenderUtil.<init>` at 2.9%: a GraphQL page query
runs the full container pipeline.

## 3. Repricing — the strongest evidence in the set

- **`HTTP_FETCH` = 4 is far too low.** Tenant C: `CircuitBreakerUrl` →
  `ProtocolExec.execute` → `Net.poll` across branches sums to roughly **half of
  all samples**. A template makes a handful of these per request while doing many
  merges, yet `HTTP_FETCH`(4) sits barely above `VELOCITY_MERGE`(3).
- **`CONTENT_GET_RELATED` = 1 is the cheapest non-free price and the hottest one
  in API traffic.** Tenant D: `ContentHelper.addRelationshipsToJSON` **recurses**
  (`:537` → `:600` → `addRelatedContentToJsonArray:782` → `toMaps` → transform → …)
  at 3.9 / 3.8 / 3.7 / 3.0 / 2.9 / 2.7%, bottoming out in
  `RelationshipFactoryImpl.dbRelatedContent` → `DotConnect.loadResult` → poll.
  One `?depth=` bump multiplies this without touching the cost.

## 4. The pricing model — DECIDED

Two coherent models, and they double-count if combined:

- **Choke-point** — meter primitives: every SQL statement at
  `DotConnect.executeQuery`, every cache miss. Physically accurate.
- **Estimate** — meter customer-visible operations at a published price.

**Chosen: estimate, with the line drawn at what the customer can influence** —
which is not the same as the API boundary. Two things that look alike sit on
opposite sides:

| | Whose? | Priced? |
|---|---|---|
| Did this need the database at all? | **theirs** — cacheable containers/pages, TTLs, query shape | **yes** |
| How many statements, what batch size, which plan? | ours | **no** |

So content is priced in two parts — `CONTENT_FROM_CACHE`(1) as a base fee per
contentlet asked for, plus `CONTENT_FROM_DB`(10) as a surcharge on the ones that
had to be read from Postgres. Warm is 1, cold is 11. There is deliberately **no
generic `DB_QUERY` price**: the 200-row batching in `findContentlets` is our
implementation detail, so the surcharge is per missed *row*, not per statement.

The same shape already existed elsewhere and is now consistent across the model:

| Cheap (cache hit) | Expensive (miss) |
|---|---|
| `CONTENT_FROM_CACHE` 1 | `CONTENT_FROM_DB` 10 |
| `ES_CACHE` 1 | `ES_QUERY` 25 / `ES_COUNT` 25 |
| `FILE_METADATA_FROM_CACHE` 1 | `FILE_METADATA_FROM_DB` 10 / `GENERATE` 50 |
| `DotDirective` cache hit — free | `VELOCITY_MERGE` 5 past `getFromCache()` |

**Hibernate remains a blind spot** either way:
`PermissionBitAPIImpl.getPermissionsByRole` goes through
`net.sf.hibernate.loader.Loader` → `QueryExecutorImpl.execute` (1.7 / 1.8% in
Tenant D), never touching `DotConnect`.

## 5. Base fee — probably do NOT charge

`PageMode.get`, `HostWebAPIImpl.getCurrentHost`, `VanityURLFilter`,
`VisitorFilter`, `DefaultAutoLoginWebInterceptor`. DB-blocked and uncosted,
~15% of the velocity profile — but fixed per request and not client-controllable.
Charging them adds a constant and tells you nothing. `RequestCostFilter` is #2 in
`web.xml`, so they stay attributable if you later decide otherwise.

## 6. Verify before annotating — five ways to silently ship a zero

1. **Request-thread assumption.** `RequestCostApiImpl.incrementCost` opens with
   `HttpServletRequestThreadLocal.INSTANCE.getRequest(); if (request == null) return;`
   — charges on any other thread are **dropped silently, no error**. The Tenant D
   profile puts the GraphQL fetchers on the servlet thread today, but graphql-java
   goes async the moment a fetcher returns a `CompletableFuture` or DataLoader
   batching lands. Same question for whatever `addContentToIndex` blocks on.
2. **Funnel rule.** ByteBuddy weaves bytecode, so **self-invocation fires the
   advice** (unlike CDI proxies). Annotating two methods in one overload chain
   double-charges. Verify the terminal for `addContentToIndex` and `NavTool.getNav`
   the way §0 did for checkin/copy/move.
3. **Cache-wrapper trap.** Charge the miss branch, not the wrapper — the
   `indexCount` → `cachedIndexCount` pattern. Applies to `LANGUAGE_VARIABLE` and
   `DotDirective.render`.
4. **Hot-path cost.** `RequestCostApiImpl` used to build the log string with
   unconditional concatenation *before* checking whether debug was on — since
   fixed with suppliers. Worth re-checking if the number of charges per request
   ever grows by an order of magnitude.
5. **Annotations don't inherit.** §1.2 is the live example — an annotation on an
   interface/abstract method does nothing for an override.

## 7. Bugs the profiles exposed — not costing items

1. **Config reads round-trip Postgres.** `AppsAPIImpl.getSecrets` /
   `hasEnvBackedSecrets` → `Config.getSystemTableValue` →
   `SystemTableConfigSource.getValue` → `SystemTableFactoryImpl.find` → DB.
   ~20× in Tenant B, ~25× in Tenant D, several times per request from several
   interceptors. Should be cached. Worth its own issue.
2. **Per-row owner lookup.** `DefaultTransformStrategy` →
   `UserAPIImpl.loadUserById` → `DotConnect.executeQuery` → poll, 1.0% in Tenant D.
   One user query per row in the response.
3. **Duplicate SQL.** `ESContentFactoryImpl.findInDb(String, boolean)` re-implements
   the interface default's query verbatim (§1.2).

## 8. Not a bug — closing an earlier claim

The bare `@RequestCost` in `ImageFilterApiImpl` (`:116`, `:243`) is **not**
mispriced. `IMAGE_FILTER_TRANSFORM` is charged imperatively at
`ImageFilter.overwrite():158`, once per filter that regenerates; the bare
annotations are a deliberate second layer at 1 each. Only the default constant's
*name* (`COSTING_INIT`) is misleading. Same story for `ES_QUERY`, `ES_COUNT` and
`WORKFLOW_ACTION_RUN` — all charged imperatively, invisible to a `@RequestCost`
grep.

## 9. Calibration loop

Place §1 and §2, run a week, then per endpoint compare Glowroot's mean response
time against the model's mean cost. Endpoints where that ratio is an outlier are
the mispriced constants. Do not set prices from the profile alone.
