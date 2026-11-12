# Feature Specification: Durable sender retry for request-cost telemetry

**Feature Branch**: `issue-38583-durable-telemetry-retry`

**Created**: 2026-11-12

**Status**: Proposed; awaiting developer approval before implementation planning

**Issue**: https://github.com/dotCMS/core/issues/38583 — contains this full specification and acceptance criteria.

**Input**: Add durable dotCMS sender retry for failed telemetry submissions; keep the specification in the issue itself.

## Problem and current behavior
The existing monitor resets request/job window counters before submitting an asynchronous HTTP publish. The publisher drops failed sends. A timeout, TLS/network failure, executor rejection, receiver rejection, or process restart can therefore lose a completed reporting window.

This is a follow-up to #35750. Its original drop-on-failure behavior remains the current contract until this change ships. The Paris receiver already deduplicates identical sender facts; retries must preserve that identity rather than generate a new timestamp or recalculate totals.

## User scenarios

**P1 — Recover from interrupted delivery:** As an operator, I need completed, durably retained windows to reach the receiver after an outage without being counted twice.

1. Given a durably retained snapshot, when a send times out or returns an unconfirmed result, then the snapshot remains pending and every attempt uses the identical payload.
2. Given pending work, when dotCMS restarts with the same durable storage, then delivery resumes without requiring a new monitor window or new customer traffic.
3. Given a confirmed completion ACK, when local retirement succeeds, then that pending snapshot is removed. If dotCMS crashes before local retirement, replay is allowed and must retain the original identity.

**P1 — Do not erase work when persistence fails:** As an operator, I need a failed admission or full outbox to be visible rather than silently erase accounting.

1. Given unavailable or full durable storage, when the monitor tries to close a window, then its accounting is not irreversibly reset without a durable owner.
2. Given a full outbox, when another snapshot cannot be admitted, then already-admitted snapshots are not evicted and resource usage remains bounded.
3. Given concurrent request/job increments during a monitor tick, when a snapshot is captured and committed, then retiring that captured accounting does not erase increments belonging to subsequent work.

**P2 — Keep normal application behavior:** As an administrator, I need retry to preserve the payload, idle-window reporting, credential configuration, and request responsiveness.

1. Given an idle application and available capacity, when the reporting interval elapses, then its zero-usage snapshot is still retained and published.
2. Given unavailable HTTP service, when customer requests execute, then they do not wait for telemetry HTTP calls or retry backoff.
3. Given a credential change or temporary publishing disablement, when pending work exists, then it is retained; sends use the currently configured credential, not a persisted secret.

## Functional requirements

- **FR-001 — Durable admission before retirement:** A snapshot must be durably committed before its captured window accounting is irreversibly retired and before its first send. On serialization, persistence, capacity, or scheduling failure, accounting must remain owned either by the monitor or durable pending work. Advancing the monitor without either owner is forbidden. A crash must not cause already-persisted accounting to be admitted again as a newly identified window.
- **FR-002 — Immutable replay:** Freeze the exact serialized body once. Retries, including after restart, must preserve all twelve existing fields: `clusterId`, `serverId`, `timestamp`, `windowSeconds`, `windowRequests`, `windowTokens`, `windowAvgTokensPerRequest`, `lifetimeRequests`, `lifetimeTokens`, `lifetimeAvgTokensPerRequest`, `windowJobTokens`, and `lifetimeJobTokens`. Do not re-read runtime IDs, change the timestamp, recompute totals, merge admitted windows, or add a new wire-level identity. Existing numeric meanings, rounding, and request/job separation remain unchanged.
- **FR-003 — Completion ACK only:** Remove retained work only after a complete, recognized receiver completion ACK. For the current Paris endpoint, HTTP **201** means the accepting server completed insertion; HTTP **202** is not a completion ACK. A connection loss, timeout, partial response, or failed local retirement must leave the original work recoverable. Do not follow redirects as an ACK or forward the bearer token to another origin.
- **FR-004 — Retry without destructive expiry:** Retain work after transport failure, open circuit, timeout, 429, 5xx, authentication failure, other rejection, or uncertain response. Use bounded, delayed backoff rather than a tight retry loop. Do not silently delete on age, retry count, 4xx, or shutdown. Authentication/configuration and permanent payload failures must be visible to operators without continually hammering the receiver.
- **FR-005 — Bounded work and explicit pressure:** Bound persistent storage, queued/in-flight work, and concurrency; specify their concrete limits and retry timings in the implementation plan and operator documentation. At capacity or on persistence failure, preserve admitted snapshots and visibly refuse/defer new admission. Do not accumulate an unbounded in-memory queue or silently evict old work. If admission is deferred, preserve uncommitted accounting and report the actual accumulated window duration rather than mislabel it as a normal interval. This does not require blocking or failing customer requests.
- **FR-006 — Restart and ownership:** Pending snapshots survive an ordinary process/container restart when the configured durable backing store survives. Recovery and any concurrent workers/nodes must not lose another producer's records or acknowledge a record that was not successfully delivered. Retained work must resume independently of fresh traffic. Ephemeral container files alone do not satisfy durability.
- **FR-007 — Preserve activation and security:** Preserve activation through `REQUEST_COST_PUSH_URL` plus `REQUEST_COST_PUSH_TOKEN`, existing timeout configuration, and source authentication. Pausing publishing must not delete pending work. Resolve credentials at send time; never persist bearer tokens, Authorization headers, signing keys, or secret-bearing URLs in pending records or logs. Preserve strict TLS certificate/hostname verification; introduce no TLS bypass, central control-plane DB access, or new production routing.
- **FR-008 — Request isolation and visibility:** Keep HTTP/retry waits off customer request threads and keep retry resource use bounded. Expose pending count/oldest age and persistence, capacity, rejection, and delivery failures through existing operational facilities, with rate-limited, credential-safe diagnostics. No new dashboard or observability framework is required.
- **FR-009 — Compatibility and operation:** Preserve normal reporting cadence, idle snapshots, existing accounting/header behavior, and stable receiver payload semantics. Document durable-storage prerequisites, capacity behavior, pause/resume, credential correction, and upgrade/rollback handling. A rollback to a sender without outbox support must not silently purge pending data.

## Key entities
- **Snapshot:** One immutable reporting window with the existing twelve sender fields.
- **Pending delivery:** A durably admitted snapshot plus non-secret delivery state; it remains owned until confirmed ACK and successful local retirement.
- **Uncommitted window:** Accounting still owned by the monitor because durable admission has not completed; it is not falsely reported as durably recoverable.

## Acceptance criteria
- [ ] A persistence/serialization/capacity failure before admission does not irreversibly reset the window or initiate a send.
- [ ] Concurrent increments during admission are neither erased nor included in two newly identified windows by the capture/retirement transition.
- [ ] A receiver insert followed by a lost response is retried with a byte-identical body and produces one logical receiver fact, not another timestamped fact.
- [ ] Restart with pending work resumes the original bodies and identifiers without new traffic; a crash between durable admission and monitor retirement does not create a differently identified duplicate.
- [ ] Only a complete HTTP 201 from the current receiver permits retirement; 202, redirect, 400, 401/403, 429, 5xx, timeout, and connection failure retain pending work.
- [ ] A crash after receiver ACK but before local retirement permits identical replay; local deletion failure does not lose the pending record.
- [ ] Capacity exhaustion respects documented bounds, preserves every admitted record, produces a visible pressure signal, and does not create an unbounded RAM backlog.
- [ ] Deferred admission does not discard new accounting, and any extended window reports its actual duration.
- [ ] Idle-window publication and separate request/job token meanings remain intact; customer requests do not wait on telemetry HTTP/retry.
- [ ] Credential rotation and pause/re-enable retain pending records; persisted data and diagnostics contain no credentials.
- [ ] Focused verification evidence covers these failure boundaries using isolated fixtures, not synthetic production accounting. No full-suite or production fault-injection run is required by this specification.
- [ ] Operator documentation states storage prerequisites, exact resource/backoff limits, failure handling, and delivery limitations.

## Legacy considerations and regression risk
The intended scope is the modern `com.dotcms.cost` publisher, its monitor capture/retirement boundary, existing snapshot coverage, and associated configuration/operator documentation. No unrelated `com.dotmarketing.*` rewrite is requested. Legacy configuration/logging utilities remain supported. Any persistent schema or storage-format change must explicitly document migration and rollback implications during planning.

The main regression risks are double-counting after an ambiguous send or admission crash, losing increments while replacing `sumThenReset`, changing idle-window cadence, changing the twelve-field payload, credential disclosure, and an unbounded retry backlog. Preserve the existing distinction between request cost and background-job cost.

## Non-goals and limits
- No receiver/ClickHouse schema, ingestion ACK policy, DNS, ingress, sender-key authority, historical backfill, or reporting-UI changes. Do not restore the retired collector or build a general messaging platform.
- No retroactive recovery of already-dropped sends and no change to the completed Paris cutover.
- No guarantee for metrics still only in process memory before durable admission, or after destruction/loss of the durable backing store. Full-storage admission failure remains an explicit availability/durability boundary, not a losslessness claim.
- Paris currently ACKs an ordinary fact after accepting-server insertion without waiting for peer replication (`insert_quorum=0`). A sender cannot recover a fact it already ACKed/retired if that accepting replica later loses it before replication. Receiver replication and backup policy are separate work.
- Storage implementation and concrete capacity/backoff values belong to the implementation plan after spec approval; they must satisfy this contract rather than weaken it silently.

## Delivery workflow
This is significant durability/accounting work: open a spec-only PR first and obtain another developer's approval before implementation planning. Consult applicable platform ADRs during planning. Do not claim that a specification PR implements retry or closes this issue. No implementation or manual test runs are part of this issue/spec publication step.

## Related
- Original publishing feature: #35750.
- Previously proposed durable-delivery contract: https://github.com/dotCMS/core/issues/35750#issuecomment-6815132921.
- Completed Paris ingestion preparation: dotCMS/ovh-k8s-cluster#1167.
