# Issue Resolution Specification: Content deletes are lost silently — index removal is fire-and-forget with no durable record

**Feature Branch**: `37276-silent-index-delete-loss`

**Created**: 2026-08-31

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: dotCMS/core#37276

**Input**: User description: "When a contentlet is destroyed, dotCMS deletes the database rows inside a transaction but hands the search-index removal to an in-memory post-commit listener that keeps no durable record of the pending work. If that listener never completes, the index document is never removed and nothing retries it."

## Problem Statement *(mandatory)*

When a contentlet is destroyed, its database rows are deleted inside a transaction, but the
removal of the matching search-index document is handed to an in-memory task that runs after
the transaction commits. That task keeps no durable record of the work it owes. If it never
completes, the index keeps a document whose content no longer exists, and nothing ever
notices or retries.

The result is a search index that permanently disagrees with the database. It is invisible
from inside the application: no error is raised, no failed-operation record is left, and the
only way to detect it is to count index documents and database rows separately and compare
them.

This matters because the application does not read from the database for these views — it
reads from the index. Search results, listings, URL maps and widget counts are all served
from it, so an index that disagrees with the source of truth produces wrong output to end
users indefinitely.

**Severity / Impact**: Any environment where content is destroyed — that is, all of them. The
user-visible symptom is wrong result counts and short lists in anything backed by a search
query. It degrades quietly: nothing fails, nothing logs, and the divergence accumulates over
time. It is not self-healing; a full reindex is the only remedy, and it must be triggered by
hand. Frequency scales with delete volume and with how often the environment restarts or
comes under index write pressure — a rolling deploy is one of the exposure windows.

## Reproduction *(mandatory)*

**Environment**: dotCMS `main` (verified against `88af0bad55`). Default configuration —
notably `INDEX_POLICY_SINGLE_CONTENT` unset (defaults to `DEFER`) and
`ASYNC_REINDEX_COMMIT_LISTENERS` unset (defaults to `true`). Reproduced conditions are
easiest to force on a multi-node cluster, but nothing about the defect requires one.

**Steps to Reproduce**:

The defect has three independent trigger paths. Each is a separate reproduction; all three
end in the same assertion.

*Path A — index write rejection under load (most likely in the field):*

1. Constrain the search index's write capacity so that bulk writes are rejected — shrink the
   write thread pool and its queue.
2. Create a batch of contentlets of a single content type and confirm they are searchable.
3. Destroy the batch while the index write queue is saturated.
4. Query the index for the destroyed identifiers.

*Path B — process stops in the post-commit window:*

1. Create a contentlet and confirm it is searchable.
2. Destroy it, pausing execution after the database transaction commits but before the
   deferred index task runs.
3. Stop the JVM while paused.
4. Restart, then query the index for the destroyed identifier.

*Path C — index pointers unavailable during the delete:*

1. Create a contentlet and confirm it is searchable.
2. Make the resolution of the active index pointers fail for the primary provider.
3. Destroy the contentlet.
4. Query the index for the destroyed identifier.

**Expected Behavior**: The index document is removed. If the removal cannot be applied at the
moment it is attempted, it is retried until it succeeds — the same guarantee content additions
already have.

**Actual Behavior**: The database rows are gone and the index document remains, permanently.
Loading the contentlet by identifier returns nothing while the index still returns the
document. A search that spans the deleted content reports a total that counts the orphaned
document, but renders one row fewer — an inflated count above a shorter list.

Log evidence differs per path and is part of what makes this hard to spot:

- Path A logs a single error line that says **"Error reindexing"** even though the failed
  operations were deletes. Searching logs for delete- or removal-flavoured wording finds
  nothing.
- Path B logs **nothing at all**.
- Path C logs a warning about provider indices not loading.

**Reproducibility**: Deterministic once the triggering condition is forced. Not reproducible
by ordinary delete traffic on a healthy, stable node — which is why it presents in production
as unexplained drift rather than as a reproducible bug report.

## Scope of Investigation *(mandatory)*

- **Affected area**: Search indexing — specifically the propagation of content deletions to
  the search index, and the durability guarantees around it. Reaches the reindex journal and
  the transaction commit-listener mechanism.
- **Suspected surface**: **Mixed**, which is itself a finding for the Legacy Impact gate. The
  index API and the vendor operations are modern (`com.dotcms.content.elasticsearch.business`,
  `com.dotcms.content.index`), but the reindex journal and the commit-listener machinery are
  legacy (`com.dotmarketing.common.reindex`, `com.dotmarketing.db`). A fix that gives deletes
  the same durability as adds necessarily touches both, so progressive enhancement applies —
  the legacy journal must be used as it stands, not rewritten.
- **Related known decisions**: The reindex journal and its retry thread are long-standing
  design decisions, as is deferring index writes to post-commit listeners. Both are
  deliberate and correct; this fix must respect them rather than replace them. `/speckit-plan`
  formally consults `dotCMS/platform-adrs` — expect decisions on reindex durability and on
  the ES→OS migration to be binding here, since both search providers consume the journal.

## Root-Cause Hypothesis

The delete path and the add path make the same decision — "defer this index operation until
after the transaction commits" — but record that pending work in two different places.

The add path writes its intent to the reindex journal, a database table, inside the same
transaction as the content change. A dedicated thread drains that table and retries each
entry until the index acknowledges it, removing the row only then. A failure leaves the row
in place, so the work survives a process restart.

The delete path records its intent only as an object held in memory for the duration of the
request. Nothing is written to the journal. When the in-memory task is lost — the process
stops, the shared pool rejects it, or the index write comes back with per-item failures that
are logged and treated as success — the intent is gone with it, and no record anywhere says
the removal is still owed.

A durable delete mechanism already exists in the journal and appears to be complete: the
action type, the enqueue method, the read-back flag, and consumers for both search providers,
all covered by integration tests. It has no production callers.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Content destruction records its index-removal intent durably, within the same transaction
  that deletes the database rows, so the removal survives a restart or a rejected task and is
  retried until the index acknowledges it.
- A partially failed index write is no longer indistinguishable from a successful one to its
  caller.
- A skipped index provider during a delete is no longer indistinguishable from a completed
  delete.
- Failure messages distinguish removals from additions, so the condition is findable in logs.
- Correction of documentation in the index-policy provider that states a default which does
  not match the code — it misleads exactly the reader trying to reason about this path.
- **`deleteAllVersionsandBackup`**, the second site that deletes rows and then defers the index
  removal. It builds its version list from `findAllVersions(identifier)`, so every version and
  language of the identifier is going away and an identifier-wide journal entry is correct there.
- **The OpenSearch write leg, once OpenSearch serves reads.** From Phase 2 onwards reads are
  served by OpenSearch while writes still fan out ES-primary / OS-shadow, so a removal lost on
  the OS leg leaves an orphaned document in the very index being queried — this defect, in the
  phase the migration spends the longest in. The shadow treatment is scoped by who serves reads
  rather than by dual-write: OpenSearch stays fire-and-forget while nothing reads it, and becomes
  durable the moment it does.
- The reindex journal's batch assembly distinguishes a pending removal from a pending reindex
  for the same identifier. Today the batch is keyed by identifier alone, so the two silently
  overwrite each other; nothing depends on that today because no production code enqueues
  removals, but this change makes the pair routine — content is saved, then destroyed. Without
  it, a removal can be dropped behind a stale reindex and the defect returns through its own
  fix.

**Explicitly out of scope / non-goals**:

- **The unpublish / archive path.** It reaches the same deferred-removal code and is very
  likely exposed to the same loss, but a journal entry is identifier-wide while unpublish
  removes a single language's live document. Reusing the mechanism there would remove
  languages that are still live. Whether that path needs its own durable mechanism is a
  separate question, recorded as an open question on #37276.
- **Making the search index participate in the database transaction.** The index is not
  transactional and cannot be rolled back. Deferring the write until after commit is the
  correct pattern and stays.
- **Removing or replacing the commit-listener path.** It remains the low-latency route; the
  durable record is added alongside it, not in place of it.
- **Rewriting the reindex journal, the retry thread, or the commit-listener framework.** They
  are legacy but working; this fix uses them as they are.
- **Repairing existing orphaned documents.** A full reindex already does this. Automated
  detection and repair of pre-existing drift is desirable but is separate work.
- **`ContentletAPI#delete(List, User, boolean, boolean)`**, the third site with this shape. It
  can delete a *subset* of an identifier's versions or languages, and a journal entry is
  identifier-wide, so recording one there would remove index documents for languages that still
  exist. Same reasoning as the unpublish path. Whether it needs a per-version durable mechanism
  is separate work.
- **Surfacing index/database drift in the Maintenance portlet** (F4 in the issue's proposed
  fixes). AC-007 makes the residue enumerable with one query, which is the data that feature
  would render; building the operator-facing view is separate work and does not gate this fix.
- **Phase 1 shadow-write durability.** While nothing reads from OpenSearch, a failed shadow write
  is genuinely tolerable and stays fire-and-forget per ADR-0009. Only the Phase 2 assumption —
  that a shadow store is not read from — is corrected here.
- **Retrying a removal past `REINDEX_MAX_FAILURE_ATTEMPTS`, and repairing entries that have
  exhausted it.** Once a removal has failed that many times the cause is not transient and a
  retry loop is not the answer. AC-007 makes the residue visible; acting on it — an operator
  tool, a slower retry tier, or automated repair — is separate work.

## Regression Risk *(mandatory)*

- **Blast radius**: Two of the changes are narrow and one is wide. Recording the removal
  intent durably touches only content destruction. Escalating a skipped provider touches the
  delete path. But changing a partially failed index write from "logged" to "raised" affects
  **every** index write, not just deletes — additions and reindexing included. Any caller or
  test that currently tolerates a partial failure in silence will begin to see it. Enumerating
  those callers is a prerequisite, not a follow-up.
  The batch-assembly change is on the hottest path in the reindex pipeline: it is how **every**
  journal entry reaches the index, full reindex included. It must preserve the existing
  deduplication of repeated entries for one identifier — collapsing that differently would turn
  a redundant-entry optimisation into a per-entry bulk operation and regress full-reindex
  throughput.
- **Backward compatibility**: The journal table already carries a delete action type and both
  search providers already consume it, so no schema change and no new format are introduced —
  a mixed-version cluster during a rolling deploy will see entries it already understands. The
  behavior change is that some failures now surface as errors where they were previously
  invisible; this may look like a regression in environments that have been silently losing
  index writes, when in fact it is the defect becoming visible.
  Making the OpenSearch leg durable from Phase 2 changes failure behaviour for every write in
  that phase, not only removals: an environment whose OpenSearch cluster is unhealthy will begin
  surfacing errors that were previously absorbed. That is the same "the defect becoming visible"
  effect as AC-003, and it is confined to phases where OpenSearch already serves reads.

- **Data considerations**: No migration. Content already orphaned in an index is not repaired
  by this change — a full reindex remains the remedy for existing drift, and that should be
  stated in the release note. Both search providers must be exercised, since the migration
  between them means an environment may be writing to one, the other, or both.

## Acceptance & Verification *(mandatory)*

- **AC-001**: For each reproduction path above, the index document for a destroyed contentlet
  is removed once the triggering condition clears. The pending removal survives the condition
  as a durable record rather than being lost to it, and is retried without operator action.
  The guarantee is at-least-once **up to** `REINDEX_MAX_FAILURE_ATTEMPTS`; behavior beyond
  exhaustion is AC-007. The record must also survive the *acknowledgement* of other work: a
  removal queued for an identifier while an earlier entry for that same identifier is in flight
  is still owed once that entry completes, and must not be discarded with it.
- **AC-002**: After a destroy under a forced index-write failure, a subsequent search returns
  a total that matches the number of contentlets that actually resolve — no inflated count.
- **AC-003**: An index write that comes back with per-item failures is surfaced to its caller
  rather than logged and reported as success.
- **AC-004**: A failed removal is findable in the logs by searching for removal- or
  delete-flavoured wording, not only by knowing to search for the word "reindexing".
- **AC-005**: Regression — content addition, publication and reindexing continue to behave as
  before. The full-reindex path in particular must not be destabilised by the change to
  failure handling. This is the blast-radius check for AC-003.
- **AC-006**: Regression — the unpublish/archive path is unchanged in behavior. No language
  that should remain live is removed from the index.
- **AC-007**: A removal that exhausts its retry attempts is **discoverable**, not silent. The
  journal entry remains in `dist_reindex_journal` above `ERROR` priority, carrying the
  identifier and the last failure cause, so the set of removals still owed to the index can be
  read with a single query. Exhaustion must not delete the record, and must not be reported to
  the caller as a completed removal. Discoverability includes saying what is owed: wherever the
  parked entry is surfaced, it is reported as a **removal**, not as a reindex — an operator sent
  looking for content that no longer exists cannot act on the row.
- **AC-008**: A pending removal and a pending reindex for the same identifier resolve
  deterministically to the newer of the two, and the older is not applied afterwards. Existing
  deduplication of identical repeated entries is preserved.
- **AC-009**: Every in-tree caller of `putToIndex` is enumerated before the partial-failure
  escalation ships, and each is confirmed to behave correctly when it now raises. Out-of-tree
  callers cannot be enumerated from this repository; that residual risk is carried by the
  release note. This is a prerequisite for AC-003, not a follow-up — without it the blast radius
  of AC-003 is unmeasured.
- **AC-010**: In a phase where OpenSearch serves reads, a failed OpenSearch write reaches the
  caller and, on the journal path, marks the entry failed so it is retried. In a phase where
  nothing reads from OpenSearch, a failed OpenSearch write is still logged and swallowed.
- **Verification method**: Integration tests in `dotcms-integration`, named `*Test` and
  registered in the matching suite. At minimum: a test that forces an index write failure
  during a destroy and asserts the document is eventually gone; a test that asserts a partial
  write failure propagates to the caller; and a regression test covering AC-006. Existing
  coverage of the journal's delete entries and of the delete-propagation contract should be
  extended rather than duplicated. Both search providers must be covered, since both consume
  the journal.

## Assumptions

- The customer-observed drift that prompted this issue is attributed to one of the three
  paths by inference from the end state, not from a captured failure. The fix addresses all
  three, so confirming which one occurred is not a prerequisite — but no reproduction should
  be presented as *the* field root cause without evidence.
- The existing journal delete machinery works as its integration tests assert. Planning should
  verify this rather than assume it, given it has never run in production.
- "Retried until acknowledged" inherits whatever retry and error-count semantics the journal
  already applies to additions. Introducing different retry behavior for removals is not
  intended.
- The severity assessment assumes environments where content is destroyed with some regularity
  and where searches expose counts to end users. An environment that never destroys content is
  not affected.
