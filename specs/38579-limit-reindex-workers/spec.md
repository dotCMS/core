# Feature Specification: Limit Background Reindex Workers

**Feature Branch**: `issue-38579-limit-reindex-workers-spec`

**Created**: 2026-11-12

**Status**: Draft — awaiting developer review

**Issue**: [dotCMS/core#38579](https://github.com/dotCMS/core/issues/38579)

**Input**: Add `MAX_REINDEXING_NODES` so a six-node dotCMS cluster can use two background reindex workers while all six nodes continue serving requests.

## Problem and Scope

A large reindex backlog lets every application node contribute document preparation and bulk indexing work against the same search infrastructure. Operators need to reduce background participation independently of the number of serving application nodes.

Changing `REINDEX_THREAD_CONCURRENT_REQUESTS` from `1` to `0` is not an equivalent worker limit. The Elasticsearch bulk client permits one in-flight request per processor for either value; zero prevents the producer from preparing the next bulk while the current request is outstanding. It does not promise to halve requests or indexing throughput.

This feature limits participation in the background journal consumer, including ordinary queued updates and deletes as well as full reindex work. It does not limit synchronous indexing performed directly by application requests.

## User Scenarios & Testing

### User Story 1 — Limit background participation without reducing serving capacity (Priority: P1)

As an operator, I can configure `MAX_REINDEXING_NODES=2` across a six-node application cluster so only two selected nodes admit new background indexing batches after membership converges, while all six continue serving application requests.

**Independent Test**: Give six simulated nodes the same eligible membership and a nonempty journal. Verify that the same two nodes participate, the other four admit no new batches, and every eligible journal entry remains reachable by a participant.

**Acceptance Scenarios**:

1. **Given** six eligible nodes with the same configuration and membership, **when** the maximum is two, **then** every node agrees on the same two participants.
2. **Given** a nonselected node, **when** it checks for background work, **then** it neither fetches nor consumes another batch, including entries already in its local prefetch queue.
3. **Given** four nonselected nodes, **when** queued work belongs to the effective two participants, **then** the nonselected nodes cannot bypass the maximum through stalled-share recovery.
4. **Given** a backlog containing updates and deletes, **when** the two participants drain it, **then** reducing worker participation does not strand entries or delete journal entries merely because their former owner is excluded.

### User Story 2 — Preserve defaults and fail over to available workers (Priority: P1)

As an operator, I retain existing behavior unless I opt in, and a failed participant does not require manual reassignment of queued work.

**Independent Test**: Exercise absent, zero, negative, one, two, and oversized limits; then remove a selected node and refresh membership. Verify the expected participant set and continued queue progress.

**Acceptance Scenarios**:

1. **Given** an absent or nonpositive maximum, **when** the cluster indexes queued work, **then** existing unrestricted participation is preserved.
2. **Given** a positive maximum larger than the eligible membership, **when** participants are selected, **then** all eligible nodes can participate.
3. **Given** two selected participants and other healthy nodes, **when** a participant is removed by existing membership detection, **then** membership refresh selects a replacement without deleting pending journal entries.
4. **Given** a formerly selected node with prefetched work, **when** its refreshed membership excludes it, **then** it stops admitting new batches and its unprocessed entries remain recoverable by the selected workers.
5. **Given** membership has converged after a change, **when** selected workers continue draining the journal, **then** old cursors or cached ownership do not permanently skip newly assigned entries.

## Requirements

### Functional Requirements

- **FR-001**: Add the integer configuration property `MAX_REINDEXING_NODES`, exposed to containers as `DOT_MAX_REINDEXING_NODES`, with default `0`. An absent or nonpositive value leaves participation unlimited.
- **FR-002**: For a positive value, the effective participant count MUST be the smaller of the configured maximum and eligible membership size. Selection MUST use a consistent deterministic order, preserving existing oldest-server ordering and its server-ID tie-breaker.
- **FR-003**: All nodes with the same membership and configuration MUST derive the same participant list. An excluded node MUST NOT append itself back into that limited list.
- **FR-004**: Admission to background batches, journal ownership, stalled-share takeover, and bulk sizing MUST agree on the effective participants. Merely pausing workers while retaining journal partitions for all serving nodes is insufficient.
- **FR-005**: Exclusion MUST be checked before admitting another batch, including batches from local prefetch. Already-started batches MAY finish; exclusion MUST NOT discard their persisted pending work.
- **FR-006**: Membership changes MUST preserve eventual processing of eligible entries under existing retry and acknowledgement semantics. Replacement workers MUST use the existing membership/liveness mechanism, not a separate election or locking service.
- **FR-007**: The limit MUST apply consistently to the background worker irrespective of the active search provider. It MUST NOT change direct synchronous indexing, search routing, serving-node membership, or the search provider migration phase.
- **FR-008**: The effective participant list MUST retain the existing bulk action division behavior. Documentation MUST explain that a configured bulk-action budget of `20` yields a count threshold of `3` for six participants and `10` for two, before byte limits and batch-close flushing.
- **FR-009**: Documentation MUST explain that this is a worker-admission limit, not a documents-per-second limiter or a strict distributed bound on all outstanding HTTP requests. Existing in-flight work and membership-cache convergence can temporarily overlap during failover or rolling deployment.
- **FR-010**: Configuration documentation MUST distinguish background participation from `REINDEX_THREAD_CONCURRENT_REQUESTS`, `REINDEX_THREAD_SLEEP`, and `REINDEX_THREAD_MINIMUM_RUNTIME_IN_SEC`. The latter two do not pace active queue ingestion in the investigated release.

### Edge Cases

- One eligible node with a maximum of two: that node remains able to index.
- No eligible membership from the existing membership source: retain existing safe behavior; never divide by zero or silently delete work.
- Equal startup times: the server-ID tie-breaker produces identical selection across nodes.
- Membership replacement while local prefetch is nonempty: relinquished entries remain in the journal and become reachable by the new owners.
- A nonselected node observes a stalled participant: recovery does not promote that excluded node outside the agreed selection.
- An active full reindex reaches an empty local queue: local exclusion or an empty local share does not falsely indicate global completion.
- Inconsistent settings during rollout: a strict global maximum is not guaranteed until configuration and membership agree across nodes; document this operational constraint.

### Key Entities

- **Eligible membership**: Application nodes considered available for reindexing by the existing membership source.
- **Effective participants**: The consistently ordered subset permitted to admit background indexing batches.
- **Journal entries**: Persisted background update/delete work, which must remain recoverable when participation changes.

## Success Criteria

- **SC-001**: With six eligible nodes, a stable shared membership, and a maximum of two, exactly two nodes admit background batches and all six remain eligible to serve requests.
- **SC-002**: Regression tests demonstrate complete journal coverage under the two-worker selection and after replacement of a selected worker, without deleting unprocessed work.
- **SC-003**: Tests prove excluded nodes cannot process prefetched batches or bypass the cap through stalled-share recovery.
- **SC-004**: Unset and nonpositive settings preserve existing behavior; a maximum above membership size does not reduce availability.
- **SC-005**: Documentation accurately describes the worker limit, deployment consistency requirements, failover behavior, and the larger bulk-action threshold caused by fewer participants.

## Legacy Considerations

The background queue implementation is active legacy code under `com.dotmarketing.common.reindex`; membership and provider adapters also consume its participant list. Preserve existing queue acknowledgement, update/delete ordering, retry, provider migration, and full-reindex completion behavior. No database schema or index-mapping migration is required.

The initial use case is six application pods with a configured maximum of two. That is an operator-selected setting, not a new default for every installation. All nodes in one application cluster are expected to use the same value.

Existing membership detection and caching remain authoritative. This feature does not introduce distributed leases or promise instantaneous exclusion of a failed or formerly selected worker.

## Non-Goals

- Deploying the change or setting a production cluster's maximum.
- Changing the configured bulk-action budget, bulk byte limit, fetch count, or concurrent-request setting automatically.
- Introducing documents-per-second pacing, CPU-adaptive throttling, or a new scheduler.
- Increasing OpenSearch replicas, changing instance sizes, or changing storage settings.
- Fixing bulk-processor close-timeout handling or changing HTTP retry behavior.
- Guaranteeing a particular throughput reduction, reindex completion time, or prevention of every search-capacity incident.

## Review and Verification Gates

This PR contains the specification only. Another developer must approve it before implementation planning. Implementation must subsequently provide developer-approved failing tests before production code is changed, following the repository's TDD requirements.

Before production use, compare two-worker queue-drain speed and search CPU/latency under a controlled backlog. Fewer workers can delay routine queued updates as well as full reindex completion; the larger per-request bulk threshold means a worker reduction is not itself a proportional pressure reduction.
