# Repro harness — pub/sub listener connection churn (#36544)

Diagnostic harness for the spike in [#36544](https://github.com/dotCMS/core/issues/36544):
a clustered startup crash-loop in which `JDBCPubSubImpl` rebuilt its dedicated Postgres
`LISTEN` connection ~3,687 times in a single 600s window, exhausting `jdbc/dotCMSPool`.

Full root-cause analysis: [`docs/core/incidents/36544-pubsub-connection-churn.md`](../../../docs/core/incidents/36544-pubsub-connection-churn.md)

> **This is not a configuration example.** Several settings in `docker-compose.yml` are
> deliberately wrong, to make a rare production race reproduce locally in minutes. Do not
> copy them into a real deployment.

## The one metric that matters

`JDBCPubSubImpl.listener()` re-issues `LISTEN` for **every** subscribed topic each time it
constructs a new `PGListener` ([`JDBCPubSubImpl.java:63-66`][listener]). The cluster topic is
`ClusterManagementTopic.TOPIC = "Cluster_Actions"`, lowercased on subscribe — which is exactly
the `LISTEN cluster_actions` seen in the incident logs.

So:

```
pg_stat_statements.calls for 'LISTEN cluster_actions'
    == number of PGListener instantiations
    == number of pooled-connection borrows for the listener alone
```

That is the 3,687 in the ticket, and it is the number every candidate fix has to move.

## Running it

```bash
docker compose up -d
docker compose logs -f dotcms-node-1     # wait for "Startup completed"

./churn.sh --baseline                    # report, kill nothing
./churn.sh                               # 30 kill cycles, 5s apart
CYCLES=100 INTERVAL=2 ./churn.sh         # harder
```

`churn.sh` terminates the Postgres backend holding the `LISTEN` connection, then requests a
page to force a cache invalidation → `publish()` → `listener()` → rebuild. On unfixed code the
instantiation counter climbs ~1:1 with kill cycles.

Killing the backend server-side is a deliberate shortcut: the churn arm of the bug only
requires the listener's connection to die repeatedly, so this reproduces it deterministically
in seconds without needing to induce a full crash-loop.

### Reproducing the crash-loop arm

The outer loop in production was `InitServlet.init()` re-running ~3,512 times — pinned by
`DELETE FROM import_audit`, which has exactly one caller
([`InitServlet.java:102`][initservlet]). Tomcat re-instantiates a servlet whose `init()` threw
`ServletException`, so every subsequent request retried the whole init battery, and each pass
re-entered `PubSubCacheTransport.init()` because `initialized` stays `false` when `init()`
throws.

To reproduce that arm, make init fail while traffic continues:

```bash
# 1. Stale cluster_server rows from a node that never deregisters (SIGKILLed pod)
docker compose kill -s KILL dotcms-node-2

# 2. Keep membership unstable
while true; do
  docker compose start dotcms-node-2; sleep 45
  docker compose kill -s KILL dotcms-node-2; sleep 45
done

# 3. Hold traffic on node 1 so Tomcat keeps retrying init
while true; do curl -sk -o /dev/null http://localhost:8082/ ; done
```

Then confirm the signature — "Startup completed" (logged at `InitServlet.java:135`) never
appears, while the init battery keeps re-running:

```bash
docker compose logs dotcms-node-1 | grep -c 'InitServlet init Started'
docker compose logs dotcms-node-1 | grep -c 'Startup completed'
```

## Observability queries

```sql
-- Listener instantiation counter (the 3,687)
SELECT calls, query FROM pg_stat_statements
 WHERE query ILIKE 'LISTEN %' ORDER BY calls DESC;

-- The accelerant: ~148ms per validated borrow in production
SELECT calls, round(mean_exec_time::numeric,1) AS ms_avg,
       round(total_exec_time::numeric/1000,1) AS s_total
  FROM pg_stat_statements WHERE query ILIKE '%as test from inode%';

-- Live listener backends. Should be exactly 1 per node; >1 means one was orphaned.
SELECT pid, state, backend_start, state_change
  FROM pg_stat_activity
 WHERE datname='dotcms' AND query='SELECT 1';

-- Stale cluster_server rows from nodes that never deregistered
SELECT server_id, name, last_heartbeat, now() - last_heartbeat AS age
  FROM cluster_server_uptime ORDER BY last_heartbeat DESC;
```

## Reading the results — and two traps

**`ProxyLeakTask` warnings are not evidence of a leak.** The pub/sub connection is held open
forever by design, and `DB_LEAK_DETECTION_THRESHOLD` is set to 2s here (product default:
300000ms, [`SystemEnvDataSourceStrategy.java:96-100`][hikari]). It trips on every healthy boot.
The `ProxyLeakTask` stack in the original ticket is a red herring.

**The listener's normal death paths do not leak.** `runInternal()`'s `return` on
`!connectionAlive()` and the `KILL_ON_FAILURES` throw are both wrapped by
`run()`'s `finally { stopListening(); }` ([`JDBCPubSubImpl.java:177-184`][run]), which closes
the connection. The genuine leak window is narrow: between the `PGListener` constructor's
borrow (`:128`) and `Thread.start()` (`:69`), where the thread never runs and so the `finally`
never fires. `subscribeTopic()` failure is safe — it closes before re-throwing (`:155`).

The dominant mechanism is therefore **churn plus a lock convoy**, not a leak:
`listener()` synchronizes on the static `PGListener.class` monitor (`:59`) and can block inside
it for the full `DB_CONNECTION_TIMEOUT` (30s here) waiting to borrow, serializing every
publisher thread behind it.

## A/B testing the amplifiers

Each of these is a separate follow-up candidate; toggle them independently in
`docker-compose.yml`:

| Change | Tests |
|---|---|
| Remove `DOT_DB_VALIDATION_QUERY` | Whether the ~148ms/borrow validation query is what makes startup unaffordable (#34840). Hikari falls back to JDBC4 `isValid()`. |
| `DB_MAX_WAIT: '1800000'` | The #34921 naming bug. Expect **no** change to listener churn — Hikari never retires an in-use connection, and this one is checked out permanently. |
| `DB_MAX_TOTAL: '60'` | Whether the pool size merely delays exhaustion rather than preventing it. |
| `DOT_SERVER_HEARTBEAT_RUN_EVERY_SECONDS: '60'` | How much of the rewire pressure comes from the heartbeat vs. from init retries. |

## Teardown

```bash
docker compose down -v
```

[listener]: ../../../dotCMS/src/main/java/com/dotcms/dotpubsub/JDBCPubSubImpl.java
[run]: ../../../dotCMS/src/main/java/com/dotcms/dotpubsub/JDBCPubSubImpl.java
[initservlet]: ../../../dotCMS/src/main/java/com/dotmarketing/servlets/InitServlet.java
[hikari]: ../../../dotCMS/src/main/java/com/dotmarketing/db/SystemEnvDataSourceStrategy.java
