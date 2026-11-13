# Durable request-cost telemetry delivery

dotCMS retains completed reporting snapshots in its **own application PostgreSQL database** before retiring their captured counters. No Cloud Control database access, new service, or container-local spool is required. Existing snapshots keep all twelve sender fields, including separate request/job totals. Retry reuses the original serialized body, timestamp and runtime IDs.

## Enable and pause

Set the existing `REQUEST_COST_PUSH_URL` and `REQUEST_COST_PUSH_TOKEN` properties (`DOT_REQUEST_COST_PUSH_URL` and `DOT_REQUEST_COST_PUSH_TOKEN` in the environment). Production destinations must use verified HTTPS. Legacy plain HTTP still warns; certificate and hostname checks are never bypassed.

URLs with userinfo, query strings or fragments are refused: the shared HTTP client's diagnostics could otherwise expose credentials embedded there. Use the bearer setting rather than URL credentials. Correct invalid configuration before expecting new admissions.

Clearing either setting pauses new publication and delivery without deleting pending records. Restore the settings to resume. Credentials are read at send time, so correcting/rotating a token does not rewrite snapshot bodies or store credentials in the outbox.

The outbox stores only a SHA-256 destination digest, not a URL or token. Changing the URL does **not** redirect existing records to a different receiver: records for the previous URL remain paused. Restore that exact URL with an authorized current credential to drain them. Do not delete them as part of configuration cleanup.

## Bounds and failure behavior

- At most **10,000 pending records per application database**, across all nodes, each at most **8 KiB UTF-8**. This bounds pending payload data to roughly 78 MiB, not PostgreSQL's total disk/WAL/vacuum usage.
- One frozen, uncommitted candidate per monitor; no unbounded in-memory retry queue.
- Admission uses a PostgreSQL transaction with `synchronous_commit=on`. A two-second admission lock timeout and ten-second SQL statement timeout defer admission rather than erase accounting.
- One daemon polling task per node, with a one-second delay after each pass. A nonblocking database transaction lock permits at most **one delivery per application database** at a time; each pass handles one record.
- HTTP timeout defaults to **5,000 ms**, preserving `REQUEST_COST_PUSH_TIMEOUT_MS` / `DOT_REQUEST_COST_PUSH_TIMEOUT_MS`; the effective value is clamped to 1–60,000 ms. These are the shared client's connection/acquisition/read timeouts, not a proven absolute end-to-end deadline.
- Transient failures back off per record at 5, 10, 20, 40, 80, 160, 320, 640, then at most **900 seconds**. Other 4xx responses except 408/429 immediately wait 900 seconds. Backoff is persisted; restart does not reset it.
- No age/retry-count expiry, no destructive full-queue policy. Pending 4xx records remain visible and require operator correction, not automatic deletion.
- Responses are discarded after a **64 KiB** bound; response bodies are never logged by the sender. Redirects are disabled.
- Only a completely consumed **HTTP 201** permits local retirement. 200/202/204, redirects, rejection, timeout, partial response and storage uncertainty retain the body.

If persistence is unavailable/full, the monitor retains its frozen candidate and does not subtract captured counters. Subsequent increments remain in its counters; after admission recovers, the following snapshot reports the actual extended window duration. These uncommitted in-memory values are not crash-durable. Neither HTTP retry nor database admission runs on customer request threads.

Idle windows still publish; successful admissions do not wait for delivery. A failed record's delayed retry does not prevent other due records from being selected. Polling/recovery starts at application initialization, independently of new traffic.

## Observe and recover

The sender logs tenant-wide pending count and oldest age every ten minutes while configured. Persistence/capacity/delivery/configuration failures have rate-limited warnings. No credential or response-body dumps are necessary.

For a paused/misconfigured sender, inspect the tenant database using authorized operational access:

```sql
SELECT count(*) AS pending, min(created_at) AS oldest,
       min(next_attempt) AS next_attempt
FROM request_cost_outbox;

SELECT destination, count(*) AS pending, min(created_at) AS oldest
FROM request_cost_outbox
GROUP BY destination;
```

- **Authentication/rejection:** correct the current credential or receiver configuration. Failed records are retained with delayed retry.
- **Capacity:** restore receiver/storage health and let pending records drain. Do not truncate the table or reset counters to clear an alert.
- **Database outage:** restore the tenant database. Pending committed records recover with it; memory-only accounting can be lost if the process also dies.
- **Pod replacement:** no special copy is required; any compatible node using this application database and exact destination can drain pending work.
- **Crash after ACK or uncertain local delete:** identical replay is allowed; the receiver deduplicates the immutable sender fact.

## Upgrade, rollback and limits

Fresh-install SQL and the additive `Task261112CreateRequestCostOutbox` migration create the same table/index. Existing content/accounting tables and receiver schemas are unchanged.

Older binaries do not read or purge this table but also cannot drain it. Prefer draining before rollback; otherwise preserve the table and resume with a compatible sender later. Do not drop pending data or restore an older application database over it without accounting for that loss.

This is **at-least-once delivery of durably admitted snapshots**, not end-to-end losslessness. It cannot recover historical dropped sends, memory-only metrics, or destruction of the backing database. Paris currently completes an ordinary fact ACK after accepting-server insertion without waiting for peer replication (`insert_quorum=0`); once that ACK is locally retired, later accepting-replica data loss is outside sender recovery. Database/receiver backups and restore qualification remain separate operational requirements.
