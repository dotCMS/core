# RCA — Pub/sub listener connection churn → pool exhaustion → startup crash-loop

**Issue:** [#36544](https://github.com/dotCMS/core/issues/36544) (spike)
**Incident:** customer `firstmac`, pod `dotcms-firstmac-prod-1-1`, dotCMS v26.07.06-01 (`5efb47a`), 2026-07-08
**Epic:** [#34837](https://github.com/dotCMS/core/issues/34837) · **Related:** [#34923](https://github.com/dotCMS/core/issues/34923), [#34921](https://github.com/dotCMS/core/issues/34921), [#34840](https://github.com/dotCMS/core/issues/34840)
**Provenance:** [#20320](https://github.com/dotCMS/core/issues/20320) (original impl) · [#20995](https://github.com/dotCMS/core/issues/20995) (connection separation) · [#26019](https://github.com/dotCMS/core/issues/26019) (`JDBCPubSubImpl` added, opt-in) · [#26706](https://github.com/dotCMS/core/issues/26706) / [PR #27117](https://github.com/dotCMS/core/pull/27117) (made default)
**Repro harness:** [`docker/docker-compose-examples/pubsub-connection-churn/`](../../../docker/docker-compose-examples/pubsub-connection-churn/)
**CI reproducer:** `dotcms-integration/src/test/java/com/dotcms/dotpubsub/JDBCPubSubImplConnectionChurnReproTest.java`

All line references are against `5efb47a`-era code and were re-verified on `main` at the time of
writing.

---

## Summary

Two nested loops, each individually survivable, combined into a startup that could not converge:

| | Loop | Evidence | Per-iteration cost |
|---|---|---|---|
| **Outer** | `InitServlet.init()` retried by Tomcat | `DELETE FROM import_audit` ×3,512 | full init battery + a cluster rewire |
| **Inner** | `PGListener` rebuilt by `listener()` | `LISTEN cluster_actions` ×3,687 | one pooled-connection borrow + re-`LISTEN` of every topic |
| **Accelerant** | Run-always task battery re-running | `select count(*) as test from inode` ×3,571, 528s | ~148ms × 2 per `MainServlet.init()` pass |

The pool (default 60 connections, 30s borrow timeout for this customer) could not sustain ~6
listener borrows/second each paying 148ms of validation. Once it saturated, every DB consumer
starved simultaneously — `DbConnectionFactory`, `RulesEngine`, `PostgresJobQueue`,
`VelocityServlet` — which kept `init()` failing, which kept the outer loop running.

The dominant mechanism is **unbounded connection churn plus a lock convoy, not a connection
leak.** This reframes the fix priorities; see [Refuted](#refuted) and [Candidate fixes](#candidate-fixes).

Underneath the specific defects is a design decision: the listener's permanently-held connection
is drawn from the same Hikari pool that serves page renders. Readers who want the design context
before the defect mechanics should start at
[How the mechanism works](#how-the-mechanism-works-and-what-depends-on-it) and
[How we got here](#how-we-got-here) — the latter shows that the hardening this class needs already
exists in a sibling implementation that was the default until December 2023.

---

## The key arithmetic

`LISTEN cluster_actions` ×3,687 is not "pub/sub resubscribe churn" in a vague sense. It is a
precise counter.

`JDBCPubSubImpl.listener()` re-issues `LISTEN` for **every** topic in `topicMap` each time it
constructs a listener — [`JDBCPubSubImpl.java:63-66`]:

```java
internalListener = new PGListener();                       // :63  ← borrows a connection (:128)
for (Comparable<String> t : topicMap.keySet()) {
    internalListener.subscribeTopic(t.toString());          // :65  ← LISTEN <topic>
}
internalListener.setName("PGListener Pub/Sub Thread");
internalListener.setDaemon(true);
internalListener.start();                                  // :69  ← thread starts only here
```

`subscribeTopic()` short-circuits on topics it already holds (`:145-147`), but that set is
**per-listener instance**, so a fresh listener always re-`LISTEN`s everything. The topic name
matches exactly: `ClusterManagementTopic.TOPIC = "Cluster_Actions"`
([`ClusterManagementTopic.java:44`]), lowercased on subscribe (`JDBCPubSubImpl.java:267`).

Therefore:

```
3,687 LISTEN cluster_actions
  == ~3,687 PGListener instantiations
  == ~3,687 pooled-connection borrows for the listener alone, in 600s (~6/sec)
```

**This is the number any fix must move.** The harness reads it directly out of
`pg_stat_statements`.

---

## The inner loop: unbounded listener rebuilds

Every entry point funnels through `listener()`, which rebuilds whenever the current listener
is not listening:

| Caller | Line |
|---|---|
| `start()` | `:88` |
| `stop()` | `:102` |
| `subscribe()` → `subscribeToTopicSQL()` | `:275` → `:267` |
| `unsubscribe()` | `:284-285` |
| **`publish()`** | **`:291`** |

`publish()` is the hot one: every cache invalidation reaches it via
`ChainableCacheAdministratorImpl` → `PubSubCacheTransport.send()` → `QueuingPubSubWrapper`. So
during a startup storm, ordinary cache traffic drives listener reconstruction. There is **no
backoff, no rate limit, and no cap.**

Two aggravating details:

1. **A 30s borrow under a static monitor.** `listener()` synchronizes on
   `JDBCPubSubImpl.PGListener.class` (`:59`) and the constructor's borrow (`:121`, `:128`) blocks
   for the full `DB_CONNECTION_TIMEOUT` — 30s for this customer — before throwing. Every
   publisher thread plus the `QueuingPubSubWrapper` submitter pool (up to 10 threads,
   `PUBSUB_QUEUE_DEDUPE_THREADS`) convoys behind one monitor held for 30s at a time.

2. **`stop()` allocates.** `stop()` is `listener().stopListening()` (`:102`). If the listener is
   already dead, `listener()` builds a complete replacement — borrow + `LISTEN` all topics +
   `Thread.start()` — purely to stop it again. Shutdown paths should never allocate.

Note also that the constructor's `internalListener = new PGListener()` assignment (`:63`) only
happens if the constructor *returns*. When the borrow times out, the field keeps pointing at the
old dead listener, so `isListening()` stays false and **every** subsequent caller retries —
each one paying another 30s under the monitor.

---

## The outer loop: `InitServlet.init()` retried by Tomcat

`DELETE FROM import_audit WHERE serverid=? AND (last_inode is null OR last_inode = '')` lives in
`ImportAuditUtil.voidValidateAuditTableOnStartup()`, which has **exactly one caller** in the
repository: [`InitServlet.java:102`]. So 3,512 executions ⇒ `InitServlet.init()` ran ~3,512
times in one JVM.

The mechanism is the servlet contract: `init()` throws `ServletException`
([`InitServlet.java:129`] on `DotInitScheduler.start()` failure, [`:143`] on `findSystemHost`
failure), Tomcat discards the instance, and the next request re-instantiates and re-inits.
Under a load balancer plus readiness probes plus real traffic, ~6 attempts/second is entirely
consistent.

This also explains the reported symptom that **"Startup completed" never appeared** — it is
logged at [`InitServlet.java:135`], *after* `DotInitScheduler.start()`. The `import_audit` delete
at `:102` did run every pass, so failure occurred between those two points.

Each retry re-entered the cluster path:

```
InitServlet.init()
  └─ Task00030ClusterInitialize.executeUpgrade()
       └─ ClusterFactory.initialize()                        ClusterFactory.java:87
            └─ rewireClusterIfNeeded()                                      :336
                 └─ rewireCluster() → addMeToCacheIfNeeded()                :361, :371
                      └─ ChainableCacheAdministratorImpl.setCluster()       :183
                           └─ PubSubCacheTransport.init()                   :46
                                └─ pubsub.start() → listener()   JDBCPubSubImpl.java:88
```

The re-entry is gated by `ChainableCacheAdministratorImpl.java:190`:

```java
if (getTransport().shouldReinit() || !getTransport().isInitialized()) {
    getTransport().init(localServer);
}
```

For `PubSubCacheTransport` both terms reduce to `!initialized` (`:117-126`), so a **healthy**
transport is never re-inited. But `init()` sets `initialized = true` only as its *last*
statement (`:52`), after `pubsub.start()` and `pubsub.subscribe()`. If either throws — which
they do once the pool is drained — the flag stays `false` and **every** subsequent rewire
re-enters `init()`. `addMeToCacheIfNeeded()` then swallows the exception
(`ClusterFactory.java:381-383`), so the failure is logged but never escalates, and
`KNOWN_SERVERS` is still updated.

---

## The accelerant: the run-always startup tasks (secondary question — answered)

**The emitter is application code — the run-always startup task battery re-running — not
HikariCP connection validation.**

> **Correction.** An earlier revision of this document attributed the 3,571 executions to Hikari's
> `connectionTestQuery` via `DOT_DB_VALIDATION_QUERY`, on the reasoning that the application call
> sites "run once per startup". That reasoning was wrong: it held the startup count fixed while the
> whole incident consists of startup *repeating*. The corrected analysis follows. The `DOT_` prefix
> mechanism described below is still real and still worth fixing — it was the *attribution* that was
> incorrect.

`MainServlet.init()` calls `StartupTasksExecutor.executeStartUpTasks()`
(`MainServlet.java:129`), which iterates all nine run-always tasks and invokes `forceRun()` on
**every one, every time**. There is no "already ran" guard
(`StartupTasksExecutor.java:185-191`). Two of those `forceRun()` implementations run this query
unconditionally:

| Call site | What it does |
|---|---|
| `Task00001LoadSchema.java:102-104` (`forceRun`) | `select count(*) as test from inode` on a **direct pool borrow**, deliberately bypassing the ThreadLocal connection so a failure cannot poison the outer transaction |
| `Task00004LoadStarter.java:21-23` (`forceRun`) | `select count(*) as test from inode` via `DotConnect`; returns `test < 1` |

On a **populated** database both return false, so `Task00004`'s `executeUpgrade()` — and therefore
`DotCMSInitDb.isConfigured()` (`DotCMSInitDb.java:44`) — never fires. That gives exactly
**2 executions per `MainServlet.init()` pass**.

```
3,571 ÷ 2  ≈  1,786 MainServlet.init() attempts
```

`MainServlet.init()` throws `DotRuntimeException` on failure, so Tomcat retries it on the next
request exactly as it does `InitServlet` — the same outer loop, on a second servlet.
`InitServlet.init()` ran ~3,512 times, roughly a 2:1 ratio; plausible for two independent servlets
with different retry dynamics, though it cannot be pinned down from 29–40% sampled logs.

At ~148ms each on a large `inode` table, those 3,571 executions are **528s ≈ 88% of the 600s
window**. `Task00001LoadSchema.forceRun()` additionally takes its own dedicated pool borrow every
pass, adding to pool pressure.

(`InodeFactory.java:230` is a different query — `inode, tree` with a parent predicate — and is
unrelated.)

### The `DOT_` prefix mechanism — real, but not what happened here

The spike's secondary question was whether `DOT_DB_VALIDATION_QUERY` could be the emitter. The
mechanism does exist. `SystemEnvironmentProperties.getVariable()`
([`com/liferay/util/SystemEnvironmentProperties.java`]) resolves in four steps:

```java
System.getProperty(name)            // 1
System.getProperty("DOT_" + name)   // 2
System.getenv(name)                 // 3
System.getenv("DOT_" + name)        // 4
```

So `DOT_DB_VALIDATION_QUERY` — as a `-D` property or an env var — does land in
`config.setConnectionTestQuery(...)` at [`SystemEnvDataSourceStrategy.java:87-88`], which has
**no default**, so unset means `null` and HikariCP falls back to the JDBC4 `isValid()` check.
`DockerSecretDataSourceStrategy.java:89` has the same shape. This remains a genuine hazard — a
`count(*)` validation query is a plausible-looking value with a catastrophic cost profile, and
nothing prevents an operator setting it (tracked in #34840).

But attributing the 3,571 to it required assuming the customer had actually set it, which was
never confirmed. The run-always explanation needs no unverified configuration and matches the
observed count to within one execution, so it is the primary conclusion.

**Cheap discriminator, if support can still obtain it:** whether `DOT_DB_VALIDATION_QUERY` was set
in that environment, and whether `StartupTasksExecutor`'s `"Running Startup Tasks"` log line also
repeats ~1,786 times in the sampled logs.

### Consequence: converting these to `EXISTS` is high-value, not negligible

Because these two `forceRun()` methods only need to know whether *any* row exists, and `count(*)`
on a large `inode` table is a full scan, converting them to `EXISTS` / `SELECT 1 … LIMIT 1` would
reduce the 528s to a few seconds. This reverses the initial go/no-go on the conditional follow-up
in the issue's AC #7 — see [Candidate fixes](#candidate-fixes).

---

## How the mechanism works, and what depends on it

### Why a listener must hold a connection open forever

Postgres ships a native messaging primitive: `LISTEN <channel>` / `NOTIFY <channel>, '<payload>'`.
It is cheap and transactional — `NOTIFY` inside a transaction fires only on commit.

The constraint is delivery. A notification is pushed to the **specific backend session** that
issued `LISTEN`. It is not a durable queue that can be polled from anywhere; it belongs to that
one connection. Lose the connection and the node silently stops receiving messages — no error, no
backlog, nothing to replay.

The vanilla pgjdbc driver compounds this: it does not deliver notifications asynchronously.
`PGConnection.getNotifications()` only drains what has already arrived, and the driver only reads
from the socket while executing a statement. So a poll loop must issue something periodically
purely to pump the socket — `SELECT 1` every 500ms here (`JDBCPubSubImpl.java:197-201`).

Those two facts produce the whole design: **one connection, held open forever, with a dedicated
thread spinning on it.** The class comment states it outright ("borrows 1 DB connection and keeps
it open forever"). Note the consequence — the connection is not merely long-lived, it is
*perpetually busy*, so it can never be returned to the pool even momentarily.

### Anatomy

```
JDBCPubSubImpl
  ├─ topicMap            : topics this node cares about
  └─ internalListener    : a single PGListener (a Thread)
         ├─ connection   : Lazy<Connection>   ← borrowed in the ctor (:128)
         ├─ pgConnection : Lazy<PGConnection> ← unwrapped from the above (:122)
         ├─ topics       : channels this instance has LISTENed to
         └─ runstate     : STARTED | STOPPED
```

- `listener()` (`:54-73`) is the gatekeeper: reuse if `isListening()`, else construct (borrow),
  `LISTEN` every topic, `start()` the thread.
- `runInternal()` (`:187-219`) loops while STARTED: verify liveness, `SELECT 1`, drain
  notifications, dispatch, sleep 500ms. `logFailure()` kills the thread after
  `KILL_ON_FAILURES=100` consecutive errors.
- `stopListening()` (`:167-170`) flips `runstate` and closes the connection; `run()`'s `finally`
  invokes it.

### What rides on it

| Channel | Owner | Purpose |
|---|---|---|
| `dotcache_topic` | `CacheTransportTopic` | **Cluster-wide cache invalidation** |
| `cluster_actions` | `ClusterManagementTopic` | Rolling restarts, kill-sessions |
| OSGi restart topic | `OsgiRestartTopic` | Propagating OSGi framework restarts |

Cache invalidation is load-bearing. `PubSubCacheTransport` is the **only functioning
`CacheTransport`** in the tree — the sole alternative is `NullTransport`. So on a clustered
dotCMS, all cache coherence flows through this one held connection. That is the blast radius:
lose it and nodes serve stale content.

And that failure is *silent by construction*. `PubSubCacheTransport.send()` opens with:

```java
if (!this.initialized.get()) {
    return;                     // PubSubCacheTransport.java:58-60
}
```

When `init()` has failed, every invalidation is dropped with no log and no exception. During this
incident the cluster was not merely slow — it was quietly incoherent.

### Is this pattern unusual?

Not as a pattern. "Postgres as a lightweight message bus" is well established for small-to-mid
scale coordination, and avoids operating Kafka or RabbitMQ alongside a database you already run.
The poll-loop-to-drain-notifications workaround is likewise standard pgjdbc practice.

What is dotCMS-specific is the **coupling**: cluster messaging runs on the application's own
database, and the listener's connection is drawn from the application's own request-serving pool.
That is the unusual choice, and it is what let a connection-pool problem become a
cluster-communication problem and then a startup failure.

Alternatives already ship in-tree and are selectable via `DOT_PUBSUB_PROVIDER_OVERRIDE`:
`RedisPubSubImpl`, `RedisStreamsPubSubImpl`, `PostgresPubSubImpl`, `NullDotPubSubProvider`.
Moving cluster messaging off the pooled-Postgres path is a configuration change, not a rewrite.

### Choosing a provider

| | `JDBCPubSubImpl` (default) | `PostgresPubSubImpl` | `RedisPubSubImpl` | `RedisStreamsPubSubImpl` |
|---|---|---|---|---|
| Size | 327 lines | ~270 lines | **103 lines** | 260 lines |
| Driver | vanilla `org.postgresql` | `impossibl` pgjdbc-ng | Lettuce | Lettuce |
| Connection source | **shared JDBC pool** | dedicated `DriverManager` | dedicated Lettuce pool | dedicated Lettuce pool |
| Delivery | fire-and-forget | fire-and-forget | fire-and-forget | **durable, replayable** |
| Poll loop | yes, 500ms | **none** (async callback) | **none** (async callback) | yes, 200ms |
| Reconnect | **none** | linear backoff | Lettuce auto-reconnect | manual loop |
| Retention to manage | no | no | no | yes — `MAXLEN`, default 100,000 |
| Shipped in an example | — | — | yes (`with-redis`) | **no** |

All three alternatives resolve gap 1 structurally, because none of them draw from
`jdbc/dotCMSPool` — cluster messaging stops competing with page rendering for connections.

**`RedisPubSubImpl`** is the natural recommendation where Redis is already in the stack. It is
already the shipped pairing with `RedisCache` in `docker-compose-examples/with-redis`, it is the
smallest of the four, it needs no poll loop, and Lettuce supplies reconnect handling — so it
satisfies gaps 1, 2 and 3 without dotCMS code. Its delivery semantics are *identical to today's*
Postgres `NOTIFY` (fire-and-forget), so it is a resource-isolation win, not a semantic change.

**`RedisStreamsPubSubImpl`** should be reserved for a demonstrated need for replay — very large
clusters, frequent node churn, or where stale content carries real cost. Its costs are real:
stream retention to manage, a reintroduced poll loop, and a **per-server consumer group**
(`Consumer.from(serverId, serverId)`) that must be cleaned up when a node is permanently removed
or pending entries accumulate — structurally the same stale-registration problem as the
`cluster_server` rows in this incident. It is also absent from the examples and has no test
coverage, making it the least battle-tested option.

Caveats that apply to both Redis providers:

- **The single point of failure moves, it is not removed.** If Redis is down, cluster-wide cache
  invalidation stops. The argument in its favour is that a dead Redis is far more *visible* than a
  quietly starved JDBC pool — but this is a dependency shift, not de-risking.
- **Enterprise tier.** `RedisClientFactory` depends on `ClusterFactory`, and the `with-redis`
  README requires a license pack with at least two licenses.
- **No integration coverage** on either Redis provider — the same gap `JDBCPubSubImpl` has.
- **Sessions and pub/sub are separate configuration.** `REDIS_SESSION_*` is a different keyspace
  from `REDIS_LETTUCECLIENT_URLS`. An install using Redis for session storage is **not** thereby
  using Redis for cluster notifications — it remains on the pooled-Postgres path and exposed to
  this incident. This is a common and consequential misreading.

**On making Redis the default:** recommended *as a documented configuration* for clustered
installs already running Redis, and currently under-advertised. **Not** recommended as a new
global default — that would repeat the #26706 pattern of flipping a default onto a less-tested
path, and would make Redis effectively mandatory for clustering. Fix the default Postgres path
first (fixes a and b); promote Redis separately.

---

## How we got here

The hardening this class needs is not speculative — **a sibling implementation in the same
package already has it, and the current default dropped it.**

### Timeline

| When | What | Ref |
|---|---|---|
| Apr–Jun 2021 | `PostgresPubSubImpl` built over ~11 commits, iterating on reconnect and event queuing (including a revert-and-redo). Uses the third-party `impossibl` **pgjdbc-ng** driver, which supports true async notifications via `addNotificationListener` — so it needs **no poll loop**. | #20320 |
| Sep 2021 | "Use a different connection for `PostgresPubSub.publish`" — an explicit connection-separation fix. | #20995 |
| Apr 2022 | OSGi restart made cluster-aware. | #21882 |
| **Sep 2023** | `JDBCPubSubImpl` written in **one commit, 327 lines, zero tests**, because "the impossibl PG driver fails to connect to postgres servers for unknown reasons" (customer ticket Zendesk 110932). Goal: use only the standard `org.postgresql` driver. AC was *"Be able to use another pub/sub mechanism"* — an **alternative**. QA validated it strictly as opt-in; the default stayed `PostgresPubSubImpl`. | #26019 |
| **Dec 27, 2023** | Default flipped to `JDBCPubSubImpl` by a 4-line `Type : Task` labelled **`QA : Not Needed`** — opened, approved by three reviewers and merged the same day, then backported to four LTS lines (22.03.14, 23.01.11, 23.10.24 v3, 24.01.26). | #26706 / PR #27117 |

### The tradeoff

The switch exchanged a **known** failure mode for an **unknown** one. The `impossibl` driver's
problem was loud, immediate and diagnosable: it would not connect. What replaced it is silent and
latent — a pool-coupled listener with no backoff, which only misbehaves under cluster stress at
scale, roughly 2.5 years later.

Dropping pgjdbc-ng also *forced* the poll loop, since vanilla pgjdbc has no async notification
callback. So the "vanilla driver" simplification is what makes the connection permanently busy
rather than merely long-lived.

### What did not carry over

| Concern | `PostgresPubSubImpl` (2021, ~11 commits) | `JDBCPubSubImpl` (2023, 1 commit) |
|---|---|---|
| Connection source | `DriverManager.getConnection(...)` — **outside the pool** (`:226`), with an explicit `POSTGRES_PUBSUB_JDBC_URL` override (`:217`) | Borrows from the shared Hikari pool (`:121`) |
| Reconnect | Linear backoff `min(delay + 1000, 10000)`, reset on success (`:189`, `:99`) | None — immediate, unbounded rebuild |
| Idempotency | Tests the real resource: `connection != null && !connection.isClosed()` (`:146`) | Tests a `runstate` boolean that can disagree with reality |
| Separate connection for `publish` | Yes, the #20995 fix | **Preserved** — `publish()` borrows and closes its own (`:295`) |

Three of four regressed. `PostgresPubSubImpl` remains in the tree and `pgjdbc-ng` is still a
declared dependency (`bom/application/pom.xml:856`, `dotCMS/pom.xml:720`), so reverting the
default is a live option — see [Candidate fixes](#candidate-fixes).

The governance observation, stated without blame: **promoting a diagnostic alternative to the
default is a change in blast radius, not a config tweak.** It was processed as a 4-line task with
QA waived and backported to four LTS lines. The class had no integration tests then and still has
none — this spike adds the first. That is the mechanism by which the hardening gap reached
production.

---

## Confirmed

1. **`LISTEN` count == `PGListener` instantiation count.** `JDBCPubSubImpl.java:63-66`. ⇒ ~3,687
   listener borrows in 600s.
2. **Listener rebuilds are unbounded.** No backoff, rate limit or cap on any of the six
   `listener()` call sites; `publish()` (`:291`) makes ordinary cache traffic a trigger.
3. **A 30s borrow happens under a static class monitor** (`:59`, `:121`), serializing all
   publishers.
4. **`stop()` can construct a listener in order to destroy it** (`:102`).
5. **`InitServlet.init()` ran ~3,512 times**, via the single-caller `import_audit` delete
   (`InitServlet.java:102`) and the `ServletException` retry contract; consistent with
   "Startup completed" (`:135`) never being logged.
6. **A failed `PubSubCacheTransport.init()` guarantees re-entry**, because `initialized` is set
   last (`:52`) and the exception is swallowed upstream (`ClusterFactory.java:381-383`).
7. **The run-always task battery emits 2 `count(*) as test from inode` per `MainServlet.init()`
   pass** (`Task00001LoadSchema.java:102-104`, `Task00004LoadStarter.java:21-23`), with no
   "already ran" guard in `StartupTasksExecutor` (`:185-191`) — accounting for all 3,571
   executions across ~1,786 init attempts.
7b. **`DOT_DB_VALIDATION_QUERY` reaches `connectionTestQuery`**, with no default
   (`SystemEnvDataSourceStrategy.java:87-88`). A real hazard, but not the emitter in this
   incident.
8. **`DB_MAX_WAIT` maps to `setMaxLifetime()`**, defaulting to 60000ms
   (`SystemEnvDataSourceStrategy.java:82-85`) — #34921 confirmed as a naming bug.

## Refuted

1. **The listener's death paths do not leak.** The issue states that `runInternal()`'s `return`
   on `!connectionAlive()` and the `KILL_ON_FAILURES` throw "exit without closing the
   connection". Both are wrapped by `run()`'s `finally { stopListening(); }`
   ([`JDBCPubSubImpl.java:177-184`]), which closes the connection. `subscribeTopic()` failure is
   also safe — it calls `stopListening()` before re-throwing (`:155`).

   The **genuine leak window is narrow**: between the constructor's borrow (`:128`) and
   `Thread.start()` (`:69`). The thread has not started, so the `finally` can never run. A throw
   from `unwrap(PGConnection.class)` (`:122`) or from `Thread.start()` under a thread storm
   leaks permanently. Real, but far too rare to explain 3,687 borrows.

2. **`maxLifetime=60s` does not retire the LISTEN connection.** HikariCP never retires an
   *in-use* connection — only on return to the pool — and the pub/sub connection is checked out
   permanently by design. So AC #5's premise ("a 60s `maxLifetime` retires the held-forever
   LISTEN connection every minute, forcing listener restarts") is wrong. #34921 still does real
   damage by recycling every *other* connection within 60s, which multiplies the 148ms
   validation cost, but it is not the churn trigger.

3. **The `ProxyLeakTask` warning is not leak evidence.**
   `DB_LEAK_DETECTION_THRESHOLD` defaults to 300000ms
   (`SystemEnvDataSourceStrategy.java:96-100`) and this connection is held forever
   intentionally. The warning fires on every healthy boot; the stack in the ticket is a red
   herring.

## Needs measurement

1. **What actually killed the listener repeatedly.** The code paths that end a listener are:
   server-side termination (`pg_terminate_backend`, PgBouncer/proxy/LB idle timeout, network
   blip), `KILL_ON_FAILURES=100` (`:224-227`), and explicit `stop()`/`unsubscribe()`. With no
   thread dumps and 29–40% log sampling, production cannot tell us which dominated. The harness
   forces the server-side variant deterministically.

2. **The `isEnterprise()` TOCTOU oscillation.** `rewireClusterIfNeeded()` returns early when
   `!isEnterprise()` (`ClusterFactory.java:340`), but `addMeToCacheIfNeeded()` re-checks
   independently (`:371`) and its `else` branch calls `transport.shutdown()` → `pubsub.stop()`,
   flipping `initialized` to `false` and guaranteeing a re-init next pass. `getLevel()` is an
   in-memory read (`LicenseManager.java:202-204`), so this only fires if the heartbeat's
   `takeLicenseFromRepoIfNeeded()` downgrades `myLicense` under DB pressure
   (`ServerHeartbeatJob.java:41-42`). Instrument both call sites.

3. **Whether the rewire came from init retries or the heartbeat.**
   `Task00030ClusterInitialize.forceRun()` returns `false` once `DOTCMS_STARTUP_TIME_ES` is set,
   so retries may skip the rewire, leaving only the 60s heartbeat
   (`DotInitScheduler.java:583-584`). Note the heartbeat alone yields ~10 rewires per 600s —
   nowhere near 3,687 — so the heartbeat is *not* sufficient to explain the churn on its own.

4. **The `with-redis` example may not actually enable Redis pub/sub.**
   `docker-compose-examples/with-redis` sets `DOT_DOT_PUBSUB_PROVIDER_OVERRIDE` (**double**
   prefix). But `Config.readEnvironmentVariables()` stores `DOT_`-prefixed env keys **verbatim**
   with no prefix stripping (`Config.java:321-329`), the lookup key is already
   `DOT_PUBSUB_PROVIDER_OVERRIDE`, and `envKey()` returns keys that already start with `DOT_`
   unchanged (`:332-343`). No Docker entrypoint rewrites these. So that variable most likely
   resolves to a property nothing reads, leaving both nodes on `JDBCPubSubImpl` — the very path
   that failed here.

   **Unverified** — a five-minute check with the harness (`docker compose logs | grep
   'PGListener\|RedisPubSub'`). If it holds it is independently reportable, and it undercuts any
   assumption that Redis pub/sub is well exercised in the field.

5. **`unwrap()` cross-contamination.** The raw `PGConnection` obtained at `:122` is used by
   `getNotifications()` (`:201`) while the Hikari *proxy* is what `stopListening()` closes
   (`:169`). After the proxy returns to the pool, a still-running loop iteration can touch a
   connection now owned by a different borrower. Not a leak, but a correctness hazard.

---

## Structural gaps

The individual defects are symptoms of five design gaps. Stating them separately matters because
a fix that only patches the churn counter leaves the same class of bug available next time.

1. **A permanently-held connection must not come from the request-serving pool.** This is the root
   issue; everything else is downstream. One dedicated connection cannot starve page rendering,
   the job queue, or the workflow engine. `PostgresPubSubImpl:226` already does this.

2. **Reconnection needs backoff and a ceiling.** Unbounded immediate retry converts a transient
   blip into a stampede. `PostgresPubSubImpl:189` already has the shape.

3. **Idempotency guards must test the resource, not a flag.** `isListening()` reads `runstate`,
   which lags reality by up to `SLEEP_BETWEEN_RUNS` and diverges entirely when the connection dies
   underneath. `PostgresPubSubImpl:146` checks `!connection.isClosed()`.

4. **Resource acquisition must be fully guarded.** The one genuine leak window exists only because
   the borrow (`:128`) and the `finally` that releases it (`:181`) live in different scopes,
   separated by `Thread.start()` (`:69`).

5. **Degraded cluster messaging must be loud.** A silent `return` in
   `PubSubCacheTransport.send()` (`:58-60`) plus a swallowed exception in
   `ClusterFactory.addMeToCacheIfNeeded()` (`:381-383`) meant a cluster with no cache coherence
   looked healthy until pods crash-looped. This warrants a health check, not a debug log.

The principle underneath all five: **long-lived infrastructure connections and short-lived request
connections need separate budgets, separate failure handling, and separate observability.**
Treating them as interchangeable is what allowed a 148ms validation query to take down cluster
cache coherence.

### Observability consequence of fix (a) — do not lose the trace

Taking the listener connection off the shared pool has a monitoring cost that must be paid
deliberately, because Cloud support relies on Glowroot and HikariCP pool stats today.

What actually changes:

| Signal | After moving off the pool |
|---|---|
| Glowroot statement tracing (`LISTEN`, `SELECT 1`) | **Retained.** Glowroot instruments `Statement`/`PreparedStatement` via bytecode weaving; it does not care where the `Connection` came from |
| Glowroot connection-acquisition timing | Lost for this connection — but for a connection acquired once and held forever this was a single datapoint, not a signal |
| Glowroot transaction attribution of the poll loop | **Already poor today.** The `SELECT 1` polls run on the `PGListener` background thread, outside any web transaction. The 3,687 `LISTEN` count was captured only because listener *construction* happens on the init/request thread inside the startup transaction — and that stays true after the fix |
| `dotcms.db.pool.*` (Micrometer/JMX) | The connection stops counting toward `active`. Arguably a **signal-quality gain**: today a permanently-held connection keeps `active` ≥ 1 and trips `ProxyLeakTask` on every healthy boot, noise that has to be mentally subtracted |
| "Is the listener holding a connection?" | **Genuinely lost** unless replaced — see below |

**Therefore: prefer a dedicated `HikariDataSource` sized 1–2 over raw `DriverManager`.**
`PostgresPubSubImpl` uses `DriverManager` (`:226`), but a small dedicated Hikari pool is strictly
better on the observability axis and equally isolated:

- isolation — churn cannot touch the main pool;
- Hikari metrics retained — it appears as a second named pool;
- Glowroot connection-acquisition instrumentation retained — still a `DataSource.getConnection()`;
- `leakDetectionThreshold` can be disabled for *this* pool only, removing the permanent false
  `ProxyLeakTask` signal;
- `maxLifetime` can be set to infinite for *this* pool, so nothing ever tries to retire it.

Note `DatabaseMetrics` currently hardcodes a single pool — it resolves
`DbConnectionFactory.getDataSource()` and defaults the pool name to `"HikariPool-1"`
(`DatabaseMetrics.java:61-75`) — so a second pool needs a small registration change to be picked
up. Mechanical, but it must be in scope for fix (a) or the metrics silently omit it.

**Independently, add purpose-built instrumentation.** Pool statistics were never the right
instrument for a listener; the incident was diagnosed from `pg_stat_statements`, not from dotCMS
telemetry. Recommended metrics:

| Metric | Why |
|---|---|
| `dotcms.pubsub.listener.rebuilds` (counter) | **This is the 3,687, promoted to a first-class signal.** Would have made this incident obvious in minutes |
| `dotcms.pubsub.listener.listening` (0/1 gauge) | Distinguishes "healthy" from "silently not receiving invalidations" |
| `dotcms.pubsub.listener.connection.age` | Detects premature connection recycling |
| `dotcms.pubsub.events.sent` / `.received` | `DotPubSubTopic` already tracks these (`messagesSent`, `bytesSent`, …) and nothing exposes them |

And a **`PubSubHealthCheck`** registered in `CoreHealthCheckProvider` — the `com.dotcms.health`
framework already supplies `HealthCheckBase`, tolerance config and failure windows, and there is
currently **no** pub/sub or cache-transport health check. That is the concrete home for gap 5.

---

## Candidate fixes

Ranked by expected effect on the 3,687 counter. Each should be A/B'd with the harness before a
bug issue is filed.

| # | Fix | Location | Expected effect |
|---|---|---|---|
| **a** | **Take the listener connection off the shared pool** — prefer a dedicated `HikariDataSource` sized 1–2 (retains metrics and Glowroot acquisition tracing) over the raw `DriverManager` approach of `PostgresPubSubImpl:217-226`. Must include the `DatabaseMetrics` registration change and the pub/sub metrics + health check described in [Observability consequence](#observability-consequence-of-fix-a--do-not-lose-the-trace) | `JDBCPubSubImpl.java:121-122`; `DatabaseMetrics.java:61-75` | **Primary, architectural.** Churn can no longer starve anything else. Addresses gap 1 |
| **b** | Bound listener reconstruction: backoff + attempt counter (copy `PostgresPubSubImpl:189`); close the connection in the constructor's own failure path so the pre-`start()` window cannot leak | `JDBCPubSubImpl.java:59-73, 121-129` | Caps the 3,687 counter directly. Gaps 2 and 4 |
| **c** | `stop()` must never construct a listener | `JDBCPubSubImpl.java:102` | Removes borrows from the shutdown path |
| **d** | Do not hold the static `PGListener.class` monitor across a connection borrow | `JDBCPubSubImpl.java:59, 121` | Removes the 30s convoy |
| **e** | Base the idempotency guard on the connection, not `runstate` | `JDBCPubSubImpl.java:56-62, 163-174` | Gap 3 |
| **f** | **Convert the run-always existence checks from `count(*)` to `EXISTS` / `SELECT 1 … LIMIT 1`** — they only need existence, and `count(*)` is a full scan on a large `inode` table | `Task00001LoadSchema.java:102-104`; `Task00004LoadStarter.java:21-23`; `DotCMSInitDb.java:44` | **Removes ~88% of startup time** (528s → seconds). Cheapest high-leverage fix in the set |
| **f2** | Default `connectionTestQuery` to `SELECT 1`/`isValid()`; reject or warn on an aggregate validation query | `SystemEnvDataSourceStrategy.java:87-88` (#34840) | Closes a real hazard, though not the emitter in this incident |
| **g** | Surface degraded cluster messaging: stop dropping invalidations silently, stop swallowing the rewire failure, add a health check | `PubSubCacheTransport.java:58-60`; `ClusterFactory.java:381-383` | Gap 5 — makes the next occurrence visible |
| **h** | Set `initialized` optimistically or make re-init idempotent | `PubSubCacheTransport.java:46-54` | Breaks the outer loop |
| **i** | Rename `DB_MAX_WAIT` → `DB_MAX_LIFETIME` with a sane default | `SystemEnvDataSourceStrategy.java:82-85` (#34921) | Reduces validation volume |
| **j** | Reap stale `cluster_server` rows; ensure deregistration on SIGKILL | `ClusterFactory`, `ServerAPIImpl` | Reduces rewire triggers |

**Recommended: go** on (a) through (h). (i) and (j) are already tracked.

**Reversal on AC #7.** This document originally recommended **no-go** on converting
`select count(*) as test from inode … >0/<1` to `EXISTS` / `SELECT 1 … LIMIT 1`, on the grounds
that those sites "run once per startup, so impact is negligible". That followed from the
mis-attribution corrected above. Because the run-always battery re-ran ~1,786 times and emitted
2 of these queries per pass at ~148ms, those call sites account for essentially the entire 528s.
It is now fix **(f)** and among the highest-value items in the set — see
[the accelerant](#the-accelerant-the-run-always-startup-tasks-secondary-question--answered).

**Immediate mitigation available without a code change.** Because the alternatives are still
wired and `pgjdbc-ng` is still a declared dependency, an affected install can be moved off the
pooled-Postgres path today:

In `dotmarketing-config.properties`, or as a `-D` JVM property:

```properties
# revert to the pre-Dec-2023 default (dedicated connection + backoff, 3rd-party driver)
DOT_PUBSUB_PROVIDER_OVERRIDE=com.dotcms.dotpubsub.PostgresPubSubImpl
# or move cluster messaging off Postgres entirely (preferred where Redis is already deployed)
DOT_PUBSUB_PROVIDER_OVERRIDE=com.dotcms.dotpubsub.RedisPubSubImpl
```

**Mind the env-var form.** The property name already begins with `DOT_`, and
`Config.readEnvironmentVariables()` stores such keys verbatim, so the container env var is
`DOT_PUBSUB_PROVIDER_OVERRIDE` — **not** `DOT_DOT_PUBSUB_PROVIDER_OVERRIDE`. See item 4 under
[Needs measurement](#needs-measurement); the shipped `with-redis` example uses the double-prefixed
form and probably has no effect. **Always confirm the provider actually changed** by checking the
startup log for `PGListener listening :` (Postgres/JDBC) versus Redis subscriber logging, rather
than assuming the variable took.

Reverting to `PostgresPubSubImpl` reintroduces the `impossibl` connect failures from #26019, so it
is a stopgap for pool-starved clusters rather than a recommendation. Validate either option with
the harness before recommending it to a customer.

**Recommended: no-go** on the conditional follow-up in AC #7 (converting
`select count(*) as test from inode … >0/<1` to `EXISTS`/`LIMIT 1` in `DotCMSInitDb.isConfigured`,
`Task00004LoadStarter.forceRun`, `Task00001LoadSchema.forceRun`). Those three sites execute ~3
times per startup total; the 3,571 executions came from Hikari's `connectionTestQuery`, not from
them. Not worth a task.

---

## Recommendation for future changes

The process gap that let this reach production is generalizable, and
[`docs/core/ROLLBACK_UNSAFE_CATEGORIES.md`](../ROLLBACK_UNSAFE_CATEGORIES.md) is the natural home
for it — it already carries `H-5 — Binary Storage Provider Change`, which is the same shape
(swapping the implementation behind a pluggable subsystem).

Proposed as a new entry in that document. **This is a proposal from this spike, not an adopted
standard** — it needs the owning team's sign-off before it lands.

### Proposed: `H-9 — Default Implementation Swap for a Pluggable Subsystem`

**Context.** dotCMS resolves several subsystems through a configurable provider class: pub/sub
(`DOT_PUBSUB_PROVIDER_OVERRIDE`), cache transport, datasource strategy, binary storage, session
store. Changing *which implementation is the default* is a one-line diff, so it tends to be
reviewed as a configuration tweak.

**Why it is risky.** The diff size is not the blast radius. A default swap silently moves every
install that has not set the property explicitly — including on upgrade — onto a code path that
may have been written and QA'd only as an opt-in alternative. The incumbent implementation has
usually accumulated years of hardening (backoff, resource isolation, reconnect handling) that the
newcomer has not, and none of that is visible in the diff. During a rolling upgrade, nodes on N
and N-1 may resolve *different* providers simultaneously, so wire compatibility between the two
becomes a live requirement rather than a theoretical one.

**Example from dotCMS history.** #26019 added `JDBCPubSubImpl` in September 2023 as an explicit
alternative — acceptance criterion "Be able to use another pub/sub mechanism", QA'd only via
`DOT_PUBSUB_PROVIDER_OVERRIDE`, with `PostgresPubSubImpl` remaining the default. #26706 / PR #27117
then made it the default in December 2023 via a 4-line `Type : Task` labelled `QA : Not Needed`,
merged the same day and backported to four LTS lines. The newcomer lacked the incumbent's
out-of-pool connection, reconnect backoff, and resource-based idempotency guard. Issue #36544
is the result, ~2.5 years later.

**Signals to watch for in code review.**
- A diff that only changes a default class name, a `Config.getStringProperty(..., X.class...)`
  fallback, or a commented-out property in `dotmarketing-config.properties`.
- `QA : Not Needed` on a change that alters runtime behavior for existing installs.
- The newly-default class has no integration test coverage.
- The newly-default class was introduced as a workaround, spike, or diagnostic alternative.
- An LTS backport label on a default swap.

**Safer alternative.**
1. **Parity checklist before the flip.** Enumerate what the incumbent does that the newcomer does
   not — resource isolation, retry/backoff, idempotency, observability — and close the gaps *first*.
   A short table in the PR body is enough.
2. **Require QA on the newly-default path**, exercised under cluster and failure conditions, not
   just a healthy single-node boot.
3. **Require integration test coverage** for the implementation being promoted.
4. **Verify mixed-version interoperability** if a rolling upgrade can put N and N-1 on different
   providers.
5. **Label by blast radius, not diff size** — a default swap is not `Type : Task`.
6. **Treat the LTS backport as a separate decision**, since a default flip changes behavior for
   existing installs on upgrade.

If adopted, add to the Decision Card:

```
Default implementation swap for a pluggable subsystem?  → 🟠 HIGH     (H-9)
```

---

## Acceptance criteria status

| AC | Status |
|---|---|
| Local 2-node cluster repro | **Delivered** — [`pubsub-connection-churn/`](../../../docker/docker-compose-examples/pubsub-connection-churn/), plus a CI reproducer |
| Confirm/refute `JDBCPubSubImpl` leak on listener death | **Refuted as stated.** `run()`'s `finally` covers the death paths; the real window is pre-`Thread.start()`. Churn, not leak, is dominant |
| What drives repeated `rewireClusterIfNeeded()` | **Partly confirmed** — init-retry re-entry confirmed; heartbeat alone insufficient (~10/600s); `isEnterprise()` TOCTOU needs measurement |
| True emitter of `count(*) as test from inode` | **Answered** — the run-always task battery re-running: 2 executions per `MainServlet.init()` pass × ~1,786 passes ≈ 3,571. The `DOT_` prefix mechanism is real but was not the emitter here (initial attribution corrected) |
| How `maxLifetime=60s` amplifies pub/sub churn | **Premise refuted** — Hikari never retires in-use connections. Amplifies validation cost, not listener churn |
| Minimal reproducible case | **Delivered** — deterministic via `pg_terminate_backend` on the LISTEN backend |
| Validate candidate fixes independently | **Pending** — harness supports A/B; see table above |
| Go/no-go for follow-ups | **Delivered** — go on (a)–(e); no-go on the AC #7 `EXISTS` task |

[`JDBCPubSubImpl.java:63-66`]: ../../../dotCMS/src/main/java/com/dotcms/dotpubsub/JDBCPubSubImpl.java
[`JDBCPubSubImpl.java:177-184`]: ../../../dotCMS/src/main/java/com/dotcms/dotpubsub/JDBCPubSubImpl.java
[`ClusterManagementTopic.java:44`]: ../../../dotCMS/src/main/java/com/dotcms/rest/api/v1/maintenance/ClusterManagementTopic.java
[`InitServlet.java:102`]: ../../../dotCMS/src/main/java/com/dotmarketing/servlets/InitServlet.java
[`InitServlet.java:129`]: ../../../dotCMS/src/main/java/com/dotmarketing/servlets/InitServlet.java
[`:143`]: ../../../dotCMS/src/main/java/com/dotmarketing/servlets/InitServlet.java
[`InitServlet.java:135`]: ../../../dotCMS/src/main/java/com/dotmarketing/servlets/InitServlet.java
[`SystemEnvDataSourceStrategy.java:87-88`]: ../../../dotCMS/src/main/java/com/dotmarketing/db/SystemEnvDataSourceStrategy.java
[`com/liferay/util/SystemEnvironmentProperties.java`]: ../../../dotCMS/src/main/java/com/liferay/util/SystemEnvironmentProperties.java
