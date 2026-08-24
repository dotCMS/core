# Issue Resolution Specification: `SystemEventsJob` silently drops the majority of system events in a cluster

**Feature Branch**: `[to be created — see Sequencing]`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#36827](https://github.com/dotCMS/core/issues/36827)

**Input**: User description: "https://github.com/dotCMS/core/issues/36827"

## Problem Statement *(mandatory)*

`SystemEventsJob` polls the `system_event` table every 5 seconds and tracks how far it has read
using a **wall-clock high-water mark**. The mark is advanced incorrectly, so a large fraction of
system events are **never consumed by any other node** — silently, with no error raised on
either the publish or the consume side.

Measured on a two-node cluster over a 7-day window:

| Node | Events authored | Observed by its own poller | Lost |
|---|---|---|---|
| A | 499 | 247 | **50.5%** |
| B | 632 | 231 | **63.4%** |

Both figures are conservative — `DeleteOldSystemEventsJob` purges old rows, shrinking the
denominator and making consumption look better than it is.

Two subsystems ride this queue, and both degrade in proportion:

1. **Cluster-wide configuration invalidation.** `SystemTableUpdatedKeyEvent` is delivered as a
   `CLUSTER_WIDE_EVENT` and converted back to a local notification by
   `SystemEventsJobDelegate:61-66`. Its subscribers (`ConfigExperimentUtil`, `AnalyticsAPIImpl`,
   `AnalyticsWebAPIImpl`, `AnalyticsHelper`, `BayesianAPIImpl`, `CubeJSClient`,
   `ExperimentsAPIImpl`, `AnalyticsTrackWebInterceptor`) latch resolved values into memory, so a
   missed event leaves a node running stale configuration for the lifetime of the JVM.
2. **Real-time admin UI notifications.** Non-cluster events are pushed to the websocket endpoint
   in the same loop, so roughly the same proportion of live UI updates never reach connected
   clients.

**Severity / Impact**: **High — major functionality broken.** Affects every clustered
installation (Cloud multi-node and clustered on-prem). Roughly half to two-thirds of all system
events are lost, continuously, with no signal. Single-node installations are largely unaffected
in effect, since a node skips events it authored itself — but the cursor defect is present
there too and still silently discards nothing of consequence.

In the originating support case (FD #38560) a system-table key was deleted on one node. The
`CLUSTER_WIDE_EVENT` row was written correctly, but the other node never consumed it and
continued serving stale configuration **for over 24 hours** — until the setting was applied
directly against that node. Both cluster-wide events ever recorded in that environment were
lost this way.

## Reproduction *(mandatory)*

**Environment**: Reproduced on **26.07.06-3** (Cloud, two-node cluster). The affected files are
byte-identical between `v26.07.06-3` and `main` as of 2026-07-31, so `main` and every
intervening release are affected. Not version-specific. Requires a **clustered** installation
(two or more nodes sharing one database) to observe loss; the cursor defect itself is
single-node reproducible.

### A. Deterministic — a single cluster-wide event goes missing

1. Stand up a two-node cluster (nodes A and B).
2. Pin requests to node A (confirm with the `X-Dot-Server` response header).
3. `POST /api/v1/system-table` with `{"key":"DOT_TEST_KEY","value":"1"}`.
4. `DELETE /api/v1/system-table/DOT_TEST_KEY` — `delete()` publishes a `CLUSTER_WIDE_EVENT`.
   (`set()` does not, which is the companion defect **#36828**; use `DELETE` here so a row is
   guaranteed.)
5. Confirm the row was persisted:

   ```sql
   SELECT identifier, event_type, server_id, created
   FROM system_event WHERE event_type = 'CLUSTER_WIDE_EVENT'
   ORDER BY created DESC LIMIT 5;
   ```
6. On **node B**, look for `Received event with key [DOT_TEST_KEY]` (logged by
   `AnalyticsAPIImpl:69`, which subscribes to the same event).

**Expected Behavior**: node B logs receipt within roughly one poll interval (~5 seconds).

**Actual Behavior**: frequently never arrives. The row is present in the database and node B's
poller is running, but the event is never returned to it again.

### B. Statistical — quantify the loss rate

7. For each node, count authored events over a fixed window:

   ```sql
   SELECT count(*) FROM system_event
   WHERE server_id = '<node server id>'
     AND created >= (EXTRACT(EPOCH FROM now()) - 7*86400) * 1000;
   ```
8. Count that node's `has been skipped on the server` log lines over the same window (disable
   log-viewer deduplication first, or the count is understated).
9. The two numbers should match — every node reads every event, including the ones it authored,
   and logs exactly one skip line per own-authored event. Observed: 50.5% and 63.4% shortfall.

**Reproducibility**: The statistical loss (B) is consistent and continuous. The deterministic
case (A) is **intermittent by nature** — whether a specific event survives depends on where the
poll boundary falls relative to that event's commit. Repeat step 4 several times; some events
arrive, most do not.

## Scope of Investigation *(mandatory)*

- **Affected area**: Clustering — the system events message queue that carries both cross-node
  cache/configuration invalidation and real-time admin UI notifications. Specifically the
  poll-and-dispatch loop (`SystemEventsJob`), its cursor, the `getEventsSince` query, and the
  `created` stamping on publish.
- **Suspected surface**: **Modern** (`com.dotcms.*`) for the defect itself —
  `com.dotcms.job.system.event.SystemEventsJob`,
  `com.dotcms.job.system.event.delegate.SystemEventsJobDelegate`,
  `com.dotcms.job.system.event.delegate.bean.JobDelegateDataBean`, and
  `com.dotcms.api.system.event.SystemEventsFactory`. One **legacy** touch point:
  `com.dotmarketing.init.DotInitScheduler.addSystemEventsJob()` registers the poller, and the
  `system_event` DDL lives in `dotCMS/src/main/resources/postgres.sql`. Legacy Impact is
  assessed formally in `/speckit-plan`.
- **Related known decisions**: The fix must respect the two-channel event model
  (`LocalSystemEventsAPI` in-JVM vs `SystemEventsAPI.push(CLUSTER_WIDE_EVENT, …)` cross-node),
  and the existing rule that `CLUSTER_WIDE_EVENT` is deliberately **not** sent over the
  websocket (`SystemEventsFactory:145`) but propagated per node instead. Choosing between the
  candidate cursor designs below is an architectural decision that `/speckit-plan` must check
  against `dotCMS/platform-adrs`, and it is a strong candidate for a **proposed** ADR.

## Root-Cause Hypothesis

Two independent defects compound. Either alone loses events; together they lose most of them.

**1. The cursor is advanced from wall-clock `now`, captured *after* processing completes.**

`SystemEventsJob.execute()` (`SystemEventsJob.java:47-56`) queries with the previous mark, then
sets the new mark to `new Date().getTime()` once the delegate has finished. Everything created
during the query-and-dispatch window is jumped over. The gap is exactly the processing duration,
every cycle.

**2. `created` is stamped pre-commit, so it is not monotonic with respect to visibility.**

`created` is stamped when the `SystemEvent` object is **constructed**
(`SystemEvent.java:94`, `this.creationDate = creationDate == null ? new Date() : creationDate`),
and written through as `systemEvent.getCreationDate().getTime()`
(`SystemEventsFactory:159-162`). But the row only becomes **visible** when its enclosing
transaction commits — and `push()` joins any ambient transaction via
`HibernateUtil.startLocalTransactionIfNeeded()`, so for content operations several events are
constructed early in a save and committed together at the end, seconds later.

The query is `WHERE created >= ?` (`SystemEventsFactory:368`). Any poll landing between an
event's `created` stamp and its commit sees nothing, then advances the mark **past** that
`created` value. When the row finally becomes visible it can never satisfy
`created >= lastCallback` again. It is skipped permanently.

This is a high-water mark applied to a non-monotonic, pre-commit column — the classic form of
this bug.

Secondary observations, to be confirmed in planning:

- **No durable cursor across restarts.** `lastCallback` is a `static` field, null on startup, and
  the first poll does nothing except set the mark to `now`. Events committed while a node was
  down, restarting, or before its first poll are never consumed at all.
- **Own-event skip is load-bearing for measurement.** Each node reads *all* events, including its
  own, and discards its own with `Logger.info(… "has been skipped on the server: " …)`
  (`SystemEventsJobDelegate:73-74`). That line is the denominator in reproduction step B. Any fix
  that filters by `server_id` in SQL, or drops the log line, invalidates the existing
  measurement method — so a replacement measurement must be defined alongside.
- **The query is unbounded.** `getEventsSince` has no upper bound and no `LIMIT`, so a large
  backlog (or a cursor reset) can return an arbitrarily large result set in one poll. Relevant
  when picking a fix: a design that stops losing events must not instead deliver an unbounded
  batch. There is an index on `created` (`idx_system_event`), but none on `server_id`.
- **Duplicate delivery becomes possible.** `created >= ?` is inclusive, and any fix that
  guarantees at-least-once delivery may deliver an event more than once. Consumers must be
  verified idempotent, or the design must give at-most-once/exactly-once semantics.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Advance the poll cursor from **the data actually read** — the maximum `created` returned, or
  the query start time — never from a wall-clock `now` captured after processing.
- Make an event eligible for consumption regardless of the gap between when `created` is stamped
  and when its transaction commits. The issue names three acceptable designs, and choosing among
  them is the plan's central decision:
  1. track consumed event IDs;
  2. stamp `created` at commit time;
  3. replace the timestamp cursor with a monotonic sequence.
- Guarantee that an event published as `CLUSTER_WIDE_EVENT` on one node is observed by every
  other node, reliably and repeatably.
- Make event loss and poller lag **observable** — a log line or metric when a node skips events
  or falls behind — instead of failing silently.
- Bound the delivery batch if the chosen design makes an unbounded backlog reachable.
- Automated coverage for the late-commit case, the multi-event-single-transaction case, and
  cross-node delivery.

**Explicitly out of scope / non-goals**:

- **`SystemTableImpl.set()` not publishing a cluster-wide event** — that is the companion defect
  **#36828**, specified separately in `specs/36828-system-table-cluster-event/`. This fix makes
  delivery reliable; #36828 makes `set()` publish in the first place. Both are required for
  system-table changes to converge; they are independent changes and this one lands first.
- Redesigning the websocket notification transport, the admin UI notification UX, or the
  `LocalSystemEventsAPI` in-JVM mechanism.
- Refactoring the consumer pattern that latches resolved values into memory.
- Replacing the polling model with a push/message-broker architecture. Polling stays; only the
  cursor and eligibility rule change.
- Retroactively delivering events already lost, or reconciling nodes that are currently
  divergent (see Data considerations).
- Changing `DeleteOldSystemEventsJob` retention behavior, except where the chosen cursor design
  forces a coordinated change (e.g. purging rows a slow node has not yet consumed).
- Any wholesale rewrite of legacy `com.dotmarketing.*` scheduling or clustering code beyond the
  progressive-enhancement touch needed at `DotInitScheduler`.

## Regression Risk *(mandatory)*

- **Blast radius**: **Wide — this is the queue every cross-node system event travels through.**
  Both riders are affected by any change here: cluster-wide configuration/cache invalidation, and
  every real-time admin UI notification delivered over the websocket. A fix that over-corrects
  turns silent loss into **duplicate delivery** or a **delivery storm**: consumers currently
  receive roughly half of all events, so raising that to 100% is itself a load increase on every
  node, and an unbounded first poll after a cursor reset could dispatch a large backlog at once.
  Consumer `notify()` implementations must be checked for idempotency and for exceptions
  escaping. Because `SystemEventsJob` runs on the shared
  `DotConcurrentFactory.getScheduledThreadPoolExecutor()` with a 5-second fixed delay
  (`SYSTEM_EVENTS_DELAY_SECONDS`), a slower or larger poll also holds that pool longer.
- **Backward compatibility**: **Potentially rollback-unsafe — this needs an explicit decision in
  the plan.** Designs (1) and (3) imply a **DB schema change** (a consumed-event table, or a
  monotonic sequence column on `system_event`), which falls under
  [Rollback-Unsafe Change Categories](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md) and must be
  labeled accordingly. Design (2) changes the meaning of the existing `created` value without a
  schema change, but alters data already being written. **Mixed-version clusters during a rolling
  upgrade are the sharpest risk**: upgraded and un-upgraded nodes must both keep consuming events
  authored by the other, or an upgrade window becomes an outage window for configuration
  propagation. No REST contract, Elasticsearch mapping, or public API signature is expected to
  change; `SystemEventsAPI.getEventsSince(long)` and `JobDelegateDataBean` are internal but are
  used by delegates, so any signature change must be surveyed for OSGi plugin exposure.
- **Data considerations**: No repair of existing `system_event` rows is required — the rows were
  always written correctly; only consumption was faulty. Already-lost events are **not**
  recoverable and are not retroactively delivered: nodes currently holding stale configuration
  converge on the next successfully delivered event for that key, or on restart. If a schema
  change is chosen, existing rows need a sensible default for the new column and the upgrade task
  must be idempotent. A fix that suddenly makes a large historical backlog eligible must not
  replay months of stale events — the plan must state how the cursor is initialized on first run
  after upgrade.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The poll cursor is advanced from the data actually read — max `created` returned, or
  the query start time — never from a wall-clock `now` captured after processing completes.
- **AC-002**: An event remains eligible for consumption regardless of the gap between when
  `created` is stamped and when its transaction commits.
- **AC-003**: An event published as `CLUSTER_WIDE_EVENT` on one node is observed by every other
  node in the cluster, reliably and repeatably. Reproduction case A succeeds on every attempt,
  not intermittently.
- **AC-004**: An event row whose transaction commits *after* the poller has advanced past its
  `created` timestamp is still delivered. *(automated)*
- **AC-005**: Multiple events created inside one long-running transaction, all with `created`
  timestamps predating the commit, are all delivered. *(automated)*
- **AC-006**: An event authored on node A is delivered to node B in a multi-node test.
  *(automated)*
- **AC-007**: Event loss and poller lag are observable — a log line or metric fires when a node
  skips events or falls behind — rather than failing silently.
- **AC-008**: Re-running the measurement in reproduction step B on a two-node cluster over 24
  hours shows consumed events matching authored events within an agreed tolerance. If the fix
  changes the `has been skipped on the server` log line or filters own events in SQL, an
  equivalent replacement measurement is defined and used.
- **AC-009**: No event is delivered more than once to a given node; or, if the chosen design is
  at-least-once, every consumer is verified idempotent and that choice is documented.
- **AC-010**: A node restarting, or polling for the first time, does not replay a large
  historical backlog, and does not silently skip events committed while it was down. The
  first-run cursor behavior is explicit and tested.
- **AC-011**: A rolling upgrade of a mixed-version cluster keeps propagating events in both
  directions — upgraded nodes consume events authored by un-upgraded nodes and vice versa.

- **Verification method**:
  - **Integration tests** (primary), under
    `dotcms-integration/src/test/java/com/dotcms/job/system/event/` (new — no integration test
    for this job exists today; the only related existing coverage is the unit test
    `dotCMS/src/test/java/com/dotcms/system/event/local/LocalSystemEventsAPITest.java`, which
    covers the in-JVM path only). Cover AC-004, AC-005, AC-009, AC-010 by driving
    `SystemEventsJob.execute()` directly against a controlled `system_event` state, including an
    event committed after the cursor has passed its `created` stamp, and several events published
    inside one long transaction. Run with
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=<TestClass>`.
  - **Cross-node delivery (AC-006)**: a genuine second JVM is out of reach in the integration
    suite, so simulate a second node by exercising the delegate with a distinct `server_id` and
    asserting the event is dispatched rather than skipped. The plan decides whether this warrants
    a Karate or containerized multi-node test instead.
  - **Manual cluster verification (AC-003, AC-011)**: run reproduction case A on a real two-node
    cluster, repeatedly, and confirm node B logs receipt every time. Repeat mid-rolling-upgrade
    with one node on each version.
  - **Measurement re-run (AC-008)**: reproduction step B over a 24-hour window post-fix.
  - Per Constitution Principle V, tests are written, developer-approved, and confirmed failing
    (**Red**) before any implementation.

## Sequencing *(this fix goes first)*

This issue is fixed **before** #36828, by decision on 2026-08-24:

- #36827 is the larger defect and already degrades the `delete()` path, which publishes
  correctly today. Landing #36828 first would only add a second publisher to a queue that loses
  half its contents.
- #36828's headline acceptance criterion (both nodes converge) is not demonstrable while events
  are dropped in transit, so it cannot be verified end to end until this fix is in.
- #36828 additionally asks for documentation stating that a system-table change *is* cluster-wide.
  Publishing that claim on top of a lossy queue would replace today's deterministic
  "never propagates" with a non-deterministic "sometimes propagates" — harder to diagnose, and
  actively misleading to operators who currently work around it by applying settings per node.

The two changes touch disjoint files (`SystemEventsJob`/`SystemEventsFactory` here vs
`SystemTableImpl` there), so development may proceed in parallel; it is the **merge order and the
release note** that are sequenced.

## Assumptions

- Polling remains the delivery model. The 5-second `SYSTEM_EVENTS_DELAY_SECONDS` cadence is
  acceptable and is not being tuned as part of this fix.
- The class javadoc describing a Quartz `SYSTEM_EVENTS_CRON_EXPRESSION` is **stale**: the poller
  is actually registered via `DotConcurrentFactory.getScheduledThreadPoolExecutor()
  .scheduleWithFixedDelay(...)` in `DotInitScheduler.addSystemEventsJob()`, driven by
  `SYSTEM_EVENTS_DELAY_SECONDS`. The javadoc is corrected as progressive enhancement while the
  file is open.
- A node skipping events it authored itself is intended behavior and is preserved — the fix is
  about events authored *elsewhere* never arriving.
- The measured 50.5% / 63.4% loss rates are representative enough to justify the fix; the
  post-fix tolerance for AC-008 is agreed during planning rather than assumed here.
- Losing already-dropped events permanently is acceptable; no retroactive replay is expected by
  the reporter or the originating support case.
- `DeleteOldSystemEventsJob` retention is long enough that a correctly functioning poller will
  always see an event before it is purged. If the chosen design makes that assumption load-bearing
  (e.g. a slow node falling behind retention), the plan states it explicitly.
