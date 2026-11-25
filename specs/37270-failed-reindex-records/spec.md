# Issue Resolution Specification: Failed reindex records are never retried, and their tracking is lost silently

**Feature Branch**: `37270-failed-reindex-records`

**Created**: 2026-11-25

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: #37270 (re-read against `main` after #37269 / PR #37924 merged)

**Input**: User description: "Failed reindex records are never retried, and clearing them silently
abandons unindexed content. Parking is intentional; the missing piece is a retry path. Narrow the
clear to parked records and log the identifiers it drops. Fold in the sibling-erasure finding
recorded on the issue."

## Problem Statement *(mandatory)*

When content fails to index, dotCMS records the failure as a row in the reindex journal
(`dist_reindex_journal`). That row is the only evidence that a piece of content is missing from the
search index. Today that evidence is lost in three ways, and once it is lost nothing tells anyone the
content is missing:

1. **A failure that exhausts its retries is never retried again.** The row is parked out of reach
   of the reindex queue, and there is no action that brings it back short of a full reindex of
   everything. Fixing the underlying cause (more heap, a corrected document) does not make the
   content come back.
2. **"Clear failed records" deletes the evidence and requeues nothing.** It also deletes rows that
   are still being retried, not only the ones that gave up.
3. **A successful document of the same content erases the failure of its sibling.** One journal row
   covers every document of a content item — each language, the working and the live version. When
   one of those writes is rejected and another succeeds, the success deletes the row. A "sibling" is
   any other write of the same identifier in the batch: another language, the other version, or even
   the same version written to the other index (a published item whose working and live versions
   are the same inode is written twice, once to each index). The rejected
   document stays out of the index with no failed record at all.

A fourth, smaller problem makes the first three harder to see: the failed-records listing and the
failed-records count use different definitions of "failed", so the number an administrator reads
and the list they download do not describe the same set.

The content itself is never lost — it stays in the database and a full reindex queues it again. What
is lost is the knowledge that it is missing. The defect is that the loss is silent.

**Severity / Impact**: Content silently absent from search, listings, URL maps and Site Search
crawls, with no record pointing at it. Affects every phase of the ElasticSearch → OpenSearch
migration and every customer running a large reindex, which is when transient engine rejections
(HTTP 429 circuit breaker, timeouts) are most likely. It is the mechanism that turns a transient
failure during a migration reindex into permanent, unexplained drift between the database and the
index.

## Reproduction *(mandatory)*

**Environment**: `main` after #37269 (or any build before it — the three paths predate it).
PostgreSQL; Elasticsearch (Phase 0) or OpenSearch (Phase 3). To make failures park quickly:
`RETRY_FAILED_INDEX_TIMES=1`.

**Steps to Reproduce**:

*A — parked records are never retried*

1. Cause a content item to fail indexing until it exhausts its retries. Any per-document failure
   works: an engine rejection, or a document over the parser limit (withheld by #37269).
2. Confirm its journal row is parked: its priority is `ERROR (400) + original priority`, e.g. 705
   for a reindex entry or 505 for a save.
3. Remove the cause (restart the engine with more heap, edit the content so it fits).
4. Wait, restart dotCMS, wait again.

*B — clearing deletes in-retry rows and requeues nothing*

5. With a mix of parked rows and rows still being retried (priority 301–305), press Maintenance →
   Index → **Clear failed records** (`DELETE /api/v1/esindex/failed`).
6. Check `dist_reindex_journal` and the index.

*C — a sibling's success erases a failure*

7. Take a content item with two documents in the same reindex batch (two languages, or a working
   and a live version) and make the engine reject exactly one of them while accepting the other.
8. Let the bulk response come back. Check the journal and the index.

**Expected Behavior**:

- A: there is an explicit way to send the parked rows back to the queue; after the cause is removed,
  using it indexes the content.
- B: clearing removes only rows that gave up, rows still being retried keep being retried, and the
  identifiers that were dropped are recorded where an operator can find them.
- C: the content item keeps a failed record naming the rejected document; the accepted document is
  indexed.
- The failed count and the failed listing describe the same rows.

**Actual Behavior**:

- A: the row stays parked indefinitely. The dequeue query reads only `priority <= 400`, so a parked
  row (≥ 401) is never selected. Observed on a real install: 380 rows at 705, unchanged across a
  restart and 15 hours of uptime.
- B: every row above 300 is deleted, including the ones still retrying; nothing is requeued and
  nothing records which content was dropped.
- C: the failed row is deleted by the sibling's success and the rejected document stays out of the
  index with no record. Reproduced on 2026-10-09 (Phase 3, OpenSearch rejected one document with
  `429 circuit_breaking_exception`, its sibling succeeded, row gone) and on 2026-11-25 (Phase 0, build
  without #37269: a healthy content item missing from the live index with no failed record, because
  its live document's request died entirely and its working document succeeded in another request).
  On `main` an oversized document no longer kills its request (#37269 withholds it alone, and its
  siblings are left untracked rather than acknowledged), so the cross-request form now needs a
  transport failure (connection, timeout, authentication) of one request while a sibling's request
  succeeds; the same-response form needs one per-document engine rejection.
- The count (`failedRecordCount`) uses `priority >= 400`; the listing (`getFailedReindexRecords`,
  behind `GET /api/v1/index/failed` and the deprecated `/api/v1/esindex/failed`) uses
  `priority > 300`. A reindex row on its second attempt is listed but not counted; a save row on its
  second attempt (priority 101) is neither.

**Reproducibility**: A, B and the count/list mismatch: always. C: always once the engine rejects one
document of an item and accepts another in the same batch; which engine conditions produce a single
per-document rejection vary (heap pressure, mapping conflicts), so the automated reproduction feeds
the listener a mixed bulk response directly.

## Scope of Investigation *(mandatory)*

- **Affected area**: search indexing — the reindex journal, the reindex thread's handling of bulk
  results, and the Maintenance → Index failed-records actions and their REST endpoints.
- **Suspected surface**: legacy `com.dotmarketing.common.reindex.*` (`ReindexQueueFactory`,
  `ReindexQueueAPIImpl`, `BulkProcessorListener`, `ReindexThread`) plus the REST resources under
  `com.dotcms.rest.api.v1.index` (`ESIndexResource`, `FailedReindexRecordsResource`) and the
  Maintenance JSP. Phase-independent: the journal is shared by both engines.
- **Related known decisions**: #37269 made a document over the parser limit fail on its own and
  introduced the failed-records listing and the parked-content notifications
  (`FailedIndexingNotifier`); this fix must keep both working. Parking itself is intentional — it
  stops a permanently failing row from looping — and stays. The plan consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

- **A**: `ReindexQueueFactory.markAsFailed` moves an exhausted row to `ERROR + priority`; every
  dequeue path filters on `priority <= ERROR`. The parking is by design; no code ever moves a row
  back below the threshold.
- **B**: `ReindexQueueFactory.deleteFailedRecords` deletes `priority > REINDEX (300)`, which is
  wider than "parked" and is a bare delete with no requeue and no log.
- **C**: `BulkProcessorListener.afterBulk(…, results)` classifies each *document* result but
  acknowledges per *journal entry*: a failed document calls `markAsFailed(entry)`, a successful one
  adds the same entry to `successful`, and `handleSuccess` then deletes it through
  `deleteReindexEntry` (`DELETE … WHERE ident_to_index = ? AND id <= ?`). The same happens across
  requests of one batch: a whole-request failure marks the entry failed in
  `afterBulk(…, Throwable)`, and a later request carrying a sibling document deletes it. #37269 closed
  this only for documents withheld at mapping time (`withhold` removes the entry from tracking).
- **Count/list mismatch**: two queries written at different times with different thresholds.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- **Retry failed records**: an explicit action (REST and Maintenance → Index) that sends the parked
  rows back to the queue at a priority the reindex thread processes, with a fresh retry budget.
- **Clear only what gave up, and say what was dropped**: clearing deletes parked rows only; rows
  still being retried stay. Before deleting, the identifiers being dropped are written to the log,
  and the response tells the caller how many were dropped.
- **No sibling erasure**: a journal entry is acknowledged (deleted) only when every one of its
  documents in the batch was indexed. If any document of the entry failed — per document or with
  its whole request — the entry is marked failed and is not deleted by its siblings' success.
- **One definition of "failed"** (decided 2026-11-25): a failed record is **every row that has
  failed at least once** — parked rows and rows still being retried, whichever priority they were
  queued at (a save, a structure change or a reindex). The count and the listing
  (`GET /api/v1/index/failed` and the deprecated `/api/v1/esindex/failed`) show that same set, and
  each record says whether it is **parked** (gave up; only a retry or a new save brings it back) or
  **retrying** (the queue will try it again). The clear action acts only on the parked subset. This
  keeps the shape #37269 shipped (`failedAttempts`, `retryPolicy.maxRetries` already describe rows
  that are still retrying) and shows content at risk from its first failure instead of after it
  gives up.

**Explicitly out of scope / non-goals**:

- **Automatic retry of parked rows** (a timer or job). Retrying is an operator decision; a parked row
  that fails for a permanent reason must not loop.
- Changing how many times a row is retried, or the parking threshold itself.
- Re-indexing content as part of "clear" — clear stays a tracking operation, now with a record.
- Reporting which engine failed in dual-write phases (#37271).
- Shadow-engine (Phase 1) failures: they are fire-and-forget by design and never touch the journal.
- A tool that lists which documents differ between the database and the index — a separate idea.
- "Stop Reindex and Make Active" discarding pending rows during the first 30 s (#38735).
- Renaming `downloadRemainingRecordsAsCsv` (returns JSON despite the name): the endpoint is
  deprecated; the rename is cosmetic.

## Regression Risk *(mandatory)*

- **Blast radius**: every save and every reindex goes through the journal acknowledgement path
  changed by the sibling-erasure fix. A mistake there either leaves successful rows in the journal
  (the queue never drains, a full reindex never switches over) or deletes rows too early (the
  erasure persists). Full-reindex switchover waits for the journal to drain, so this must be
  verified with a full reindex in Phases 0 and 3. The parked-content notification added by #37269
  fires when a row parks; a retried row that parks again must not spam administrators. The
  end-of-reindex message ("N contents failed to reindex and were skipped") reads the failed set: with
  the wider definition it must keep counting what was skipped (parked), not rows still retrying.
- **Backward compatibility**: `DELETE /api/v1/esindex/failed` keeps its path and response shape but
  stops deleting rows still being retried — a behaviour change, documented in its `@Operation`.
  The new retry action is additive. `GET /api/v1/index/failed` gains a per-record parked/retrying
  field (additive) and starts listing save rows that are still retrying, which it omits today; the
  failed count shown in Maintenance → Index rises to match the listing. No DB schema change; no index
  mapping change.
- **Data considerations**: installs that already hold parked rows (possibly years old, for content
  since deleted) gain a way to retry them. Retrying a row whose content no longer exists must not
  fail the batch or loop; it should be acknowledged as a delete or dropped with a log line.

## Acceptance & Verification *(mandatory)*

- **AC-001**: After a row parks and its cause is removed, the retry action requeues it and the next
  reindex iteration indexes the content; the row leaves the journal. Parked rows of content that no
  longer exists are handled without failing the batch.
- **AC-002**: A retried row that fails again parks again after its retry budget, and does not loop.
- **AC-003**: Clear deletes only parked rows; rows still being retried remain and are retried. The
  dropped identifiers appear in the log, and the response states how many rows were dropped.
- **AC-004**: When one document of a content item is rejected and another is accepted in the same
  bulk response, the item keeps a failed record and the accepted document is in the index. The same
  holds when the rejected document's whole request failed and the accepted one came in another
  request of the batch.
- **AC-005**: When every document of a content item is indexed, its journal row is deleted exactly
  as today; a full reindex in Phase 0 and in Phase 3 drains the journal and switches over.
- **AC-006**: The failed count and the failed listing return the same set: every row that has failed
  at least once, whether it was queued by a save or by a reindex, each marked parked or retrying. A
  save on its second attempt is listed and counted; the clear action removes only the parked ones.
- **AC-007**: A row that parks again after a retry does not send a second parked-content
  notification for the same save.
- **Verification method**:
  - Unit tests (`dotCMS/src/test`): `BulkProcessorListener` fed mixed bulk responses (one document
    failed and its sibling succeeded; a whole-request failure followed by a sibling success) —
    AC-004/AC-005; `ReindexQueueFactory` threshold queries — AC-003/AC-006.
  - Integration test in `dotcms-integration` (named `*Test`, registered in the `MainSuite*` that
    `just suite-timings` names): park a row, requeue it through the retry action, assert it is
    indexed (AC-001/AC-002); clear with parked and in-retry rows (AC-003); run in Phase 0 and
    Phase 3. Each must fail (Red) on current `main` first.
  - API test for the new retry endpoint and the changed clear (access control: CMS Administrator
    only).
  - Manual post-merge QA on a rig: full reindex in Phases 0 and 3 with forced per-document
    rejections, counter-proof on a build without the fix.

## Assumptions

- A retried row starts with a fresh retry budget at its original priority (a save at NORMAL, a
  reindex entry at REINDEX), so it is processed in the same order it would have been originally.
- Access to retry and clear stays CMS Administrator only, as the existing failed-records endpoints.
- The journal row's identifier is enough to requeue: the reindex thread reloads the content's
  current versions when it processes the row, so a row parked against an old version indexes the
  current one.
