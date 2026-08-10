# RequestCost — instrumentation review and pricing model

**Status:** implemented, uncommitted, on branch `issue-36947-core-web-cacheable`.
**Companion doc:** [requestcost-placement-analysis.md](requestcost-placement-analysis.md) — the full evidence and the ranked backlog.
**Date:** 2026-08-10.

---

## What this was

`@RequestCost` exists to score the "heaviness" of a request, token-style, so that
usage can be metered and rate-limited. This session audited **where the annotation
is actually placed** against real production behaviour, using Glowroot main-thread
profiles from six tenant instances (7-day windows), and then reworked the price
table.

Tenants are anonymised as A–F below. Shapes, not names, are what matter — and the
shapes turned out to be the whole story.

## The finding that framed everything

Six production instances, six different cost centres. The model's prices were the
dominant cost for exactly one of them.

| Tenant | Shape | Where the time actually went | Metered before? |
|---|---|---|---|
| A | write-heavy API | `addContentToIndex` — ~10% of all samples, blocking | **no** |
| B | GraphQL 60% / VTL 31% | prerender park 36%, `NavTool.getNav` 11% | **no** |
| C | Velocity + remote API | `CircuitBreakerUrl` outbound HTTP ~50% | yes, priced 4 |
| D | GraphQL 54% | GraphQL execute, transform, relationship N+1 | **no** |
| F | traditional Velocity | filter chain, render, binaries | partly |
| E | — | 53 samples, discarded as noise | — |

A GraphQL-only customer could saturate a node and accrue almost nothing. The
instrumentation was concentrated on the Contentlet API; the traffic was not.

## The decision that matters most

Midway through, the question surfaced as: **should cost track what the server
actually did, or what the customer asked for?**

Two coherent models:

- **Choke-point** — meter primitives (every SQL statement, every cache miss).
  Accurate to real resource use, but the customer's bill then moves with *our*
  cache hit rate, batch sizes and query plans.
- **Estimate** — meter customer-visible operations at a flat, published price.
  Less physically precise, stable and optimisable.

**We chose estimate — but the line is drawn at what the customer can influence,
not at the API boundary.** Two things that look similar are on opposite sides:

- **Did this need the database?** — theirs. Customers control caching through
  cacheable containers and pages, cache TTLs, and how they shape their queries.
  Cached content is priced at a tenth of uncached content, so the optimisation
  visibly pays off.
- **How did we ask the database?** — ours. Batch sizes, query plans, how many SQL
  statements a miss took. A customer cannot see or change any of it, so there is
  deliberately **no generic per-statement `DB_QUERY` price**.

Content is therefore priced in two parts: `CONTENT_FROM_CACHE` (1) as a base fee
per contentlet asked for, plus `CONTENT_FROM_DB` (10) as a surcharge on the ones
that had to be read from Postgres. One contentlet costs 1 warm and 11 cold; a
thousand cost 1,000 warm and 11,000 cold.

The same shape already existed for ES queries (`ES_CACHE` 1 / `ES_QUERY` 25) and
file metadata (`FILE_METADATA_FROM_CACHE` 1 / `FROM_DB` 10 / `GENERATE` 50), and
was extended to Velocity directives — `#dotParse` is charged past the
`getFromCache()` short-circuit, so cacheable containers are cheaper than
uncacheable ones. The model is consistent across all four.

What that looks like, at the default denominator of 10:

| Scenario | raw | reported |
|---|---|---|
| 1,000 contentlets, all cached | 1,000 | **100** |
| 1,000 contentlets, 90% cached | 2,000 | 200 |
| 1,000 contentlets, all from DB | 11,000 | **1,100** |

An 11× spread between fully warm and fully cold — large enough that tuning cache
config is worth a customer's time, which is the whole point.

The reasoning is written into `RequestPrices.Price` so the next person doesn't
"simplify" it away.

## The price table

Re-based twice. First onto **resource-time** — a price is an order-of-magnitude
estimate of the CPU, heap or parked-thread time an operation consumes. Then the DB
tier was compressed after a challenge that it was too high:

```
  1  in-memory cache read              CONTENT_FROM_CACHE, FILE_METADATA_FROM_CACHE, ES_CACHE
  2  per-item work in memory           VELOCITY_BUILD_CONTEXT, BLOCK_EDITOR_HYDRATION
  5  render a template fragment        VELOCITY_MERGE
 10  CPU parse/compile, or one DB hop  VELOCITY_PARSE, XSLT_PARSE, GRAPHQL_QUERY, CONTENT_FROM_DB, CONTENT_GET_RELATED, NAV_BUILD
 25  one ES round trip, or a write     ES_QUERY, ES_COUNT, CONTENT_INDEX, CONTENT_MOVE, CONTENT_COPY
 50  heavy CPU + heap, or a write txn  IMAGE_FILTER_TRANSFORM, FILE_METADATA_GENERATE, CONTENT_CHECKIN, CONTENT_DELETE
100  one remote HTTP round trip        HTTP_FETCH, XML/XSLT_FETCH_AND_PARSE
```

**Why a DB query is only 10× a cache read when it is ~1000× the latency:** what is
metered is capacity consumed *on this node*, not wall time. A query parks the
thread and burns the cycles on Postgres. Tenant F's profile bore this out —
template rendering dominated, not DB frames. At a DB price of 25 the table said DB
was 50% of a page render; the profile said otherwise. At 10 it reads merges 59% /
DB 31% / ES 10%, which matches.

**If a node ever exhausts request threads before CPU, that reasoning inverts** and
the DB/ES/HTTP tiers should go back up. That caveat is in the code.

Two prices were badly wrong and are worth calling out: `IMAGE_FILTER_TRANSFORM`
was **2** and `FILE_METADATA_GENERATE` was **3**. Decoding and re-encoding an
image, or running Tika over a binary, burns a core and a large buffer — both are
now 50.

## What shipped

16 files, ~+220/−50. Compiles clean (verified with `javac` against the built
classpath; error count identical to the HEAD baseline).

**Placement gaps closed**

| Price | Where | Why it mattered |
|---|---|---|
| `CONTENT_FROM_CACHE` + `CONTENT_FROM_DB` | `ESContentFactoryImpl.findContentlets(List)` | The bulk loader behind *every* search result — GraphQL, `/api/content`, `/api/es/search`, page render. It bypasses `find()`, so nothing metered it. Base fee per contentlet, surcharge per missed row. |
| `CONTENT_FROM_CACHE` | `ESContentletAPIImpl.find(...)` | Base fee, single-contentlet path |
| `CONTENT_FROM_DB` | both `findInDb` variants | The miss surcharge. Annotations aren't inherited, so the interface default and the impl override each need their own |
| `CONTENT_INDEX` (new) | `ContentletIndexAPIImpl.addContentToIndex(List)` | ~10% of Tenant A's samples, blocking, no price existed |
| `NAV_BUILD` (new) | `NavTool.getNav(Host, String, long, User)` | 11% of Tenant B's samples, DB-blocked, recursive |
| `GRAPHQL_QUERY` (new) | `DotGraphQLHttpServlet.handleRequest` | Parse + validate, before any field is fetched — the only charge that scales with *query size* |
| `ES_QUERY` | `enterprise.priv.ESSearchAPIImpl.esSearchRaw` | `/api/es/search` bypassed the existing charge entirely |
| `HTTP_FETCH` | `PreRenderSEOWebAPIImpl.proxyPrerenderedPageResponse` | Own `HttpClient`, so `CircuitBreakerUrl`'s charge never fired. 36% of Tenant B parked here |
| `VELOCITY_MERGE` | `DotDirective.render` | `#dotParse` / `#parseContainer` were free — a 30-container page priced like a 1-container page |
| `VELOCITY_MERGE` | `VTLResource.evalVelocity` | `/api/vtl/*` bypassed the annotated merge. 31% of Tenant B's transactions |
| `CONTENT_CHECKIN` / `CHECKOUT` / `COPY` / `MOVE` | `ESContentletAPIImpl` terminals | Prices existed in the enum, nothing charged them |

**API change:** `RequestCostApi` gained
`incrementCost(Price, Class, String, Object[], int times)` so a charge can scale
with result-set size. The 4-arg form delegates with `times = 1`.

**Reporting:** internal scale grew, so `REQUEST_COST_DENOMINATOR` now defaults to
10 to keep reported tokens in their historical range. The `x-dotrequest-cost`
header and the pushed snapshot totals are **rounded** — both have only ever
carried whole numbers, and a collector parsing them as ints would break on a
fractional value. Per-request averages stay fractional, as they always were.

**Rate limiting:** `LeakyTokenBucketImpl` defaults scaled with the table
(500→5000 refill, 10000→100000 bucket). Still disabled by default, and confirmed
that nothing overrides these anywhere.

## Three traps for whoever extends this

1. **The enum is not an index of what is metered.** Four prices — `ES_QUERY`,
   `ES_COUNT`, `WORKFLOW_ACTION_RUN`, `IMAGE_FILTER_TRANSFORM` — are charged with
   direct `incrementCost(...)` calls, invisible to a `@RequestCost` grep. This
   caused three wrong conclusions during the session before it was caught. Grep
   for **both** forms.
2. **ByteBuddy weaves bytecode, so self-invocation fires the advice** — unlike CDI
   proxies. `checkin` has 12 overloads chaining into each other; annotating two in
   one chain double-charges. Always find the terminal.
3. **Charges are silently dropped off the request thread.** `incrementCost` opens
   with `if (request == null) return;`. Anything that moves to a
   `CompletableFuture` or a worker pool stops being metered with no error. Relevant
   if GraphQL DataLoader batching ever lands.

Also: annotations are **not inherited**, so a `@RequestCost` on an interface or
abstract method does nothing for an override. This had already made
`CONTENT_FROM_DB` dead on the common single-find path.

## Open

- **Deliberately not charged:** the pre-render filter chain (`PageMode.get`,
  `HostWebAPIImpl.getCurrentHost`, Vanity/Visitor filters). ~15% of Tenant F's
  profile, but fixed per request and not client-controllable — charging it adds a
  constant and tells you nothing.
- **Hibernate is a blind spot.** `PermissionBitAPIImpl.getPermissionsByRole` goes
  through `net.sf.hibernate.loader.Loader`, never touching `DotConnect`. Any
  future DB-level metering would miss permission loading entirely.
- **Nothing is validated against a running instance.** Prices are
  order-of-magnitude estimates from sampled time-share; placements were verified by
  reading call chains, not by watching a request. The calibration loop — run a
  week, compare per-endpoint mean response time against mean cost, investigate
  outlier ratios — is what turns these into measured numbers.

## Bugs filed

Two performance defects surfaced by the profiles, neither a costing issue:

- [#36970](https://github.com/dotCMS/core/issues/36970) — Config and App-secret
  reads round-trip Postgres on every request, several times per request from
  several interceptors.
- [#36971](https://github.com/dotCMS/core/issues/36971) — N+1: one
  `loadUserById` per row of every REST/GraphQL response.

Both are `Team : Platform`. The Technology project field is unset on both — that
needs a `gh auth refresh -s read:project -s project` from someone with the scope.
