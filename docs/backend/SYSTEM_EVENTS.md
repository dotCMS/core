# System Events — delivery contract and consumer rules

The `system_event` table is dotCMS's database-backed queue for cross-node notification. Two
subsystems ride it: **cluster-wide configuration invalidation** (`CLUSTER_WIDE_EVENT`, unwrapped into
a local notify on each node) and **real-time admin UI notifications** (everything else, pushed to the
websocket endpoint).

Read this before writing a publisher or a subscriber. The rules below are not incidental — they
follow from how the queue is read, and ignoring them produces bugs that only appear on a cluster.

---

## 1. Delivery is at-least-once

> Every committed event is delivered to every running node **at least once**. A node may observe the
> same event **more than once**. **Consumers must be idempotent.**

Duplicates are by design, not by accident:

1. **The overlap window.** Each poll re-reads a bounded window behind its cursor, so an event whose
   transaction commits *after* its `created` timestamp is still delivered rather than skipped forever
   (issue #36827). Everything inside that window is read again on the next poll.
2. **Restart.** The de-duplication set is in memory, so a restarting node may re-deliver the window.

### What is guaranteed

- Delivery to every polling node regardless of the gap between `created` and commit, up to the
  overlap window.
- Delivery survives a restart: the cursor is durable, bounded by
  `SYSTEM_EVENTS_MAX_BACKLOG_MINUTES`.
- Ordering within a single poll is by `created` ascending.

### What is not

- **No exactly-once.**
- **No global ordering** across polls or nodes. Do not derive causality from arrival order.
- **No delivery of events older than retention** (`DELETE_EVENTS_OLDER_THAN`, default 31 days).
- **Not loss-proof against unbounded transactions.** A transaction held open longer than
  `SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS` can still lose its events. This is bounded, configurable and
  *warned about* — as opposed to the unbounded silent loss it replaced.

### The author-node exception

A node does **not** re-deliver events it authored itself; it skips rows whose `server_id` matches its
own, having already handled them at publish time. On a **single-node installation** the poller
therefore delivers nothing in normal operation — which is why cluster-only defects in this area can
go unnoticed for a long time.

---

## 2. Writing a consumer

Processing the same event twice must have the same observable effect as processing it once.

| Pattern | Verdict |
|---|---|
| Invalidate / evict a cache entry | ✅ naturally idempotent |
| Set a value to what the event carries | ✅ idempotent |
| Re-read state from the system of record and apply | ✅ idempotent |
| Increment a counter, append to a list, enqueue follow-on work | ⚠️ **not** idempotent — guard on the event `identifier` |
| Send a user-visible notification | ⚠️ duplicate-visible — dedupe by event id if a duplicate would be noticed |
| Trigger a non-repeatable side effect (email, payment, external POST) | ❌ must guard on event id |

The event's `identifier` is stable across redeliveries and is the correct dedupe key.

**State which row your consumer falls into**, and if it is not naturally idempotent, guard it and
cover the duplicate case with a test.

### Audit of existing consumers (2026-08-27, issue #36827)

| Consumer | Verdict |
|---|---|
| `ConfigExperimentUtil` | ✅ `set(resolveFeatureFlag())` |
| `AnalyticsAPIImpl` | ✅ `set(resolve…())`; duplicate INFO log only |
| `AnalyticsWebAPIImpl` | ✅ `set(Config.getBooleanProperty(…))` |
| `CubeJSClient` | ✅ `set(resolveClientTimeout())` |
| `AnalyticsTrackWebInterceptor` | ✅ `set(Config.getBooleanProperty(…))` |
| `ChangeLoggerLevelEvent` subscriber | ✅ sets a log level |
| `BulkRefreshCompletionListener` | ⚠️ **latent duplicate** — pushes a user notification per event. Currently unreachable via the cluster path because `JobCompletedEvent` does not deserialize (see §3); **fixing that makes the duplicate live**, so guard it then. |
| `SystemEventsWebSocketEndPoint` | ⚠️ duplicate-visible — a duplicate push is a duplicate admin UI update |

---

## 3. Writing a publisher

- **Publish inside the business transaction.** `SystemEventsAPI.push(...)` participates in the
  caller's transaction, so the event is atomic with the change it describes. Never publish a fact
  that has not committed.
- **`CLUSTER_WIDE_EVENT` is never sent over the websocket** — it is for cross-node local notification.
- **Keep payloads small.** Every node reads them on every poll within the window.
- **Payloads reach admin websocket clients.** Do not put anything in one that the receiving client is
  not authorised to see.
- **The payload type must be deserializable by the receiving node.** `Payload` records the payload's
  concrete class name and the receiver reconstructs into it. A class with no usable Jackson creator
  (no `@JsonCreator`, no default constructor) can be *written* but never *read back* — and because a
  batch is converted as a whole, it also destroys delivery of every other event in its window. Two
  instances have been found: `SystemTableUpdatedKeyEvent` (fixed) and `JobCompletedEvent`. The
  batch-level fragility is tracked in **#37249**.

> If you add a payload class, give it an explicit `@JsonCreator` / `@JsonProperty` constructor and a
> test that round-trips it.

---

## 4. Loss tolerance: ≤1%

Consumed events must match authored events within **1%** over a 24-hour window on a two-node cluster.
Because every node observes every event it authored and then skips it, authored-by-me and
observed-by-me must match — the identity used to measure the original 50.5% / 63.4% loss in #36827.

**Why 1% and not zero.** Chosen as a starting point: it absorbs a node restart or the purge boundary
landing mid-measurement, while still decisively proving the reported loss is gone. It is a
*measurement tolerance*, not a licence to drop 1% of events by design — the design target is zero
loss for any transaction shorter than the overlap window.

`SystemEventsReconciliation` runs this comparison in-product (folded into `SystemEventsJob`,
`SYSTEM_EVENTS_RECONCILE_INTERVAL_MINUTES`, default hourly) and warns above the bar.

**If a stricter guarantee is ever needed**, that is a deliberate decision, and the options were
already evaluated in `specs/37133-36827-system-events/research.md` §3c and §3d: per-node
acknowledgement rows (exactly-once, at O(events × nodes) write cost) and a PostgreSQL
snapshot/`xmin`-horizon cursor (exact commit ordering, zero loss and zero duplicates, at the cost of
binding the queue to PostgreSQL internals). Do not re-derive them.

---

## 5. Configuration

| Property | Default | Purpose |
|---|---|---|
| `ENABLE_SYSTEM_EVENTS` | `true` | Master switch for the poller |
| `SYSTEM_EVENTS_CRON_EXPRESSION` | `0/5 * * * * ?` | Poll cadence |
| `SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS` | `120` | How far back each poll re-reads. Must exceed the longest event-bearing transaction; the dominant tuning knob. |
| `SYSTEM_EVENTS_MAX_BACKLOG_MINUTES` | `60` | Bounds recovery after downtime; must stay well below retention |
| `SYSTEM_EVENTS_LAG_WARN_THRESHOLD_PERCENT` | `50` | Warn when commit lag reaches this share of the window |
| `SYSTEM_EVENTS_RECONCILE_INTERVAL_MINUTES` | `60` | Reconciliation cadence |
| `DELETE_EVENTS_OLDER_THAN` | `31` (days) | Retention, enforced by `DeleteOldSystemEventsJob` |

---

## 6. Observability

Loss must never be silent again. Four signals:

| Signal | Meaning | Action |
|---|---|---|
| Commit lag approaching the window (WARN) | The window is too small for this workload — the one remaining way to lose an event | Raise `SYSTEM_EVENTS_OVERLAP_WINDOW_SECONDS` |
| Cursor stale (WARN) | The poller is not running, or every read is failing | Investigate the node |
| Backlog clamped (WARN, with the skipped span) | The node was down longer than the backlog bound; events in that span were never delivered to it | Assess what was missed |
| Reconciliation above tolerance (WARN) | Authored and observed have diverged beyond 1% | Treat as a delivery regression |
