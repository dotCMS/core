# Bounded search reads

Content search/count, raw content search, scroll batches and Site Search share one fail-fast
admission gate **per engine per JVM**, after successful query-cache lookups. Defaults are eight
outstanding ES reads and eight outstanding OS reads. There is **no admission queue**: excess calls
fail before starting HTTP I/O, leaving Tomcat workers available for other requests.

## Configuration

These are startup settings in `dotcms-config-cluster.properties`. Restart to apply changes.
Environment overrides use the automatic `DOT_` prefix, e.g. `DOT_SEARCH_DEADLINE_MS`.
Every limit/timeout must be positive; per-route connections must not exceed total connections.

| Property | Default | Meaning |
| --- | ---: | --- |
| `search.admission.max-concurrent` | 8 | Outstanding reads per engine, including requests waiting for a transport connection |
| `search.deadline-ms` | 5000 | Caller deadline per read/batch, including client-side retries, not a whole-page rendering deadline |
| `ES_MAX_CONNECTIONS` | 30 | Total connections in the shared ES client (unchanged from the previous library default) |
| `ES_MAX_CONNECTIONS_PER_ROUTE` | 10 | ES connections per endpoint (previous library default) |
| `ES_CONNECTION_TIMEOUT` | 1000 | ES connection establishment timeout, ms (previous library default) |
| `ES_SOCKET_TIMEOUT` | 30000 | ES socket read timeout, ms (previous library default) |
| `ES_CONNECTION_REQUEST_TIMEOUT` | 100 | ES connection-pool lease timeout, ms |
| `OS_CONNECTION_REQUEST_TIMEOUT` | 100 | OS connection-pool lease timeout, ms |

Existing `OS_CONNECTION_TIMEOUT`, `OS_SOCKET_TIMEOUT`, `OS_MAX_CONNECTIONS` and
`OS_MAX_CONNECTIONS_PER_ROUTE` still configure the OS transport. Custom ES client providers must
apply their own finite transport settings; admission still applies to the core read entry points.

Do **not** increase concurrency just because rejections appear. First check cluster search latency,
CPU/GC and queue/rejection metrics, and compare the admission budget with the per-route connection
limit. The default ES budget leaves two of its ten per-route connections available to other
operations sharing that client; retain headroom when overriding it. Eight is a conservative draft
starting point, **not a production load-tested optimum**. Larger connection pools can move overload from dotCMS into the search cluster. Index lifecycle
and bulk writes are not admission-controlled; they still share the transport. Successful cached
reads do not consume admission slots. Core plugins bypassing these read entry points are not covered.

## Deadlines and cancellation

Apache's legacy async pool checks lease expiry when pool activity processes pending requests;
a configured lease timeout alone is **not** a prompt wall-clock bound on an otherwise stalled pool.
The caller deadline is the authoritative bound, including while acquiring a connection.

No additional executor, worker threads or background waiting queue is created. ES uses its existing
asynchronous API and cancels the transport request at deadline/interruption. Cancellation does not
promise that the search server has stopped executing the query.

**The current OS transport does not wire `CompletableFuture.cancel` to underlying HTTP cancellation.**
The caller still returns at its deadline, but the slot remains occupied until the actual transport
completes or reaches its finite transport timeout. The same rule applies to delayed ES cancellation:
never release a slot merely because its caller stopped waiting, or repeated timeouts could accumulate
unbounded orphan I/O. Monitor active slots together with deadline counters. Slow OS transports may
need a lower `OS_SOCKET_TIMEOUT` to recover slots faster.

Each scroll batch gets a new deadline; no permit is held between batches. Scroll cleanup bypasses
admission and is asynchronous. Late scroll responses and abandoned raw response bodies are cleaned
up without blocking a timed-out caller. Cleanup is best effort; server-side scroll keepalive remains
the final expiry mechanism if cleanup cannot reach the cluster.

## Overload is not missing content

Admission rejection, deadline/interruption, transport failure, HTTP 429/503, server-side timeout and
partial shard results raise `SearchUnavailableException`. They do not populate query caches with
empty hits or `-1` counts, and Phase 2 does **not** retry them on the other engine. That avoids
amplifying overload and starting a second full deadline after the first has expired. Ordinary query
errors retain their existing handling and migration fallback behavior.

REST and buffered Velocity page rendering return a generic **503**, `Retry-After: 1` and
`Cache-Control: no-store`. No query, credentials or backend response body is exposed in that error. REST overload warnings
are rate-limited to one per five seconds; counters retain every rejected/failed call.
Velocity's legacy exception handlers propagate the failure; a request-scoped marker also prevents
page and `#dotcache`/`#dotParse` cache fills if another helper swallows it. No new thread-local is used.
An already-committed streaming response cannot be recalled or changed to 503; the servlet throws
an I/O failure rather than writing an error into the page or completing partial output normally.
Real container/proxy behavior for that abort still needs deployment validation.

No database/index mappings, successful response shapes or template APIs change. Rolling back restores
previous unbounded behavior without any data migration; temporary 503 handling is a new failure mode
that callers should treat as retryable (with backoff, not immediate retry loops).

## Metrics

The existing Micrometer global registry exports these through the configured management metrics
endpoint. All have only the bounded `engine` tag (`elasticsearch` or `opensearch`); never query/user/URL
labels. The standard registry common tags still apply.

- `dotcms.search.admission.active`: transports not yet complete, including abandoned calls.
- `dotcms.search.admission.limit`: configured concurrency budget.
- `dotcms.search.admission.rejected`: fail-fast rejections before transport starts.
- `dotcms.search.admission.deadlines`: expired caller deadlines.
- `dotcms.search.admission.interrupted`: interrupted callers.
- `dotcms.search.admission.failures`: asynchronous transport failures.
- `dotcms.search.admission.backend-unavailable`: backend rejection, partial results, timeout or I/O failure.
- `dotcms.search.admission.duration`: admitted caller durations (includes failed calls, excludes rejections).

OpenSearch also exposes `dotcms.search.connections.leased`, `.pending`, `.available` and `.max`
from its existing connection manager. These gauges follow client rebuilds. The legacy ES client's
connection manager is not exposed by the supported client API, so this PR does not claim direct
ES pool measurements or use reflection to obtain them.

Admission metrics are **not** direct measurements of the HTTP connection pool or server-side
search queue. A deadline followed by a nonzero active gauge can be expected until transport completes.
