# Issue Resolution Specification: Destroying content can leave a permanent orphan document in the content index

**Feature Branch**: `issue-37908-destroy-orphan-index-doc`

**Created**: 2026-11-26

**Status**: Draft (revised 2026-11-26 after an independent review)

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: #37908

**Input**: User description: "Destroying content with IndexPolicy FORCE/WAIT_FOR can leave a permanent orphan document in the content index."

## Problem Statement *(mandatory)*

Destroying a contentlet deletes its identifier, contentlet and version rows from the database, but
under some timing the contentlet's document survives in the content index. From then on searches and
counts include content that no longer exists: `PersonaAPI.getPersonasIncludingDefaultPersona`, for
example, counts the orphan but cannot load it, so the total and the returned list disagree. Nothing
removes the document afterwards — only a full reindex does — and nothing reports that it is there.

**Severity / Impact**: Medium. The wrong answer is permanent and silent once it happens, but it
needs a narrow timing window: the content has to be destroyed with `IndexPolicy.FORCE` or
`IndexPolicy.WAIT_FOR` while a reindex row for the same content, committed by an earlier
transaction, is still pending in `dist_reindex_journal`. Almost every save leaves such a row for a
few seconds, whatever its index policy (see Reproduction), so the exposure is any destroy that
follows a save closely: API integrations, import-then-clean-up jobs, integration tests. Manual
editing rarely hits it. It surfaced as the intermittent `PersonaAPITest` failure in the merge queue
(#37886), roughly 1 run in 5.

## Reproduction *(mandatory)*

**Environment**: Any build of `main` (reproduced on `a892eb53ad` 2026-10-05; mechanism re-checked on
`1ac483d17f` and on image `trunk_240d1dc` 2026-11-26). Any search engine and migration phase — the
race is in the shared delete path, not in an engine provider.

**Steps to Reproduce**:

1. Create a contentlet through a workflow action, **with any index policy**. The save commits a
   reindex row for its identifier in `dist_reindex_journal`: `WorkflowAPIImpl.saveWorkflowTask`
   registers an asynchronous commit listener that calls `reindexQueueAPI.addIdentifierReindex`
   regardless of the contentlet's policy. `ContentletDataGen`, which defaults to `FORCE`, leaves
   that row too — which is why `PersonaAPITest` meets the defect although it creates with `FORCE`.
2. Before the reindex thread has acknowledged that row, destroy the contentlet with
   `IndexPolicy.FORCE` (or `WAIT_FOR`).
3. Let the reindex thread map that row while the destroy's transaction is still open — after the
   destroy has removed the document from the index, before it commits.
4. After the destroy commits, query the working index for the destroyed identifier, and the database:
   `select id from identifier where id = ?`.

A deterministic version of steps 2–3 does not need the real reindex thread: open the destroy's
transaction, perform the inline removal, write the document for the pending row from a second
connection (which still sees the uncommitted-deleted rows), then commit.

**Expected Behavior**: After the destroy commits, the content's documents are gone from every content
index, whatever the destroy's `IndexPolicy` and whatever the reindex thread did in the meantime.

**Actual Behavior**: The document is present in the index; the identifier query returns no row.
`PersonaAPITest` logs `[#37886] removing orphaned persona` each time it meets one (PR #37907).

**Reproducibility**: Intermittent with real threads (timing dependent, about 1 in 5 runs of the
original test sequence); deterministic with the two-connection simulation above.

## Scope of Investigation *(mandatory)*

- **Affected area**: content destroy and the content index — the reindex queue
  (`dist_reindex_journal`) and the delete journal introduced by #37276.
- **Suspected surface**: modern `com.dotcms.content.elasticsearch.business` (`ESContentletAPIImpl`
  `destroyContentlets` and `deleteAllVersionsandBackup`, `ContentletIndexAPIImpl` reindex mapping)
  and legacy `com.dotmarketing.common.reindex` (`ReindexQueueFactory`, `ReindexThread`). Confirmed
  in planning.
- **Related known decisions**: commit `1fc4f23bfc` (#37276) limited the durable delete journal to
  `IndexPolicy.DEFER` because journalling every destroy left rows that made `assertEmptyQueue` time
  out across MainSuite 1a. Its message attributes that to the reindex thread not running in
  integration tests; **that is inaccurate** — `RuleWatcher` unpauses `ReindexThread` between tests
  to drain the queue. Which tests are affected, and why, is therefore measured in planning rather
  than assumed (see Regression Risk). ADRs are consulted in `/speckit-plan`.

## Root-Cause Hypothesis

With `FORCE` / `WAIT_FOR`, `ESContentletAPIImpl.destroyContentlets` removes the document from the
index **inline, before the transaction commits**, and records no post-commit removal
(`deferredRemovalIdentifiers` journals only `DEFER`). Between that inline removal and the commit,
other connections still see the content's rows. If the reindex thread maps an
**already-committed, pending** journal row for the same identifier in that window,
`ContentletIndexAPIImpl.mapEntry` loads the rows, maps the document and writes it back. The commit
then deletes the rows, and nothing removes the document again.

**The destroy's own enqueues are not producers.** Two enqueues come from the destroy itself, and both
are harmless:

- `PermissionBitFactoryImpl.resetPermissionReferences` → `addIdentifierReindex`, which the issue as
  filed proposes to stop. Its row is written inside the destroy's transaction (the rig's SQL log
  shows the `insert into dist_reindex_journal` right after `delete from permission_reference` and
  before `DELETE FROM contentlet_version_info`), so the reindex thread cannot see it before the
  commit; after the commit `mapEntry` finds no rows and writes nothing.
- `WorkflowAPIImpl.deleteWorkflowTaskByContentletIdAnyLanguage` registers a commit listener that
  calls `addContentToIndex` on the in-memory contentlet. It runs after the commit (in the rig:
  `Indexing: ... policy: FORCE` 45 ms after the destroy), where `contentlet.isWorking()` /
  `isLive()` read the now-deleted version info, so `mapContentletForProcessor` returns nothing. In
  the rig, the three identifiers destroyed this way returned 0 hits from the content search
  afterwards.

These rows matter to the fix all the same: any check for "a pending row exists" must not count the
destroy's own row, or it is always true (see Fix Scope).

To confirm in planning: whether a delete applied by a later batch can reach the engine before the
stale write of an earlier batch when the bulk processor has more than one request in flight.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- In `destroyContentlets` **and** `deleteAllVersionsandBackup` (same shape: identifier-wide delete,
  inline removal, `deferredRemovalIdentifiers` → `journalContentDeletes`), for identifiers destroyed
  under `IndexPolicy.FORCE` or `IndexPolicy.WAIT_FOR`, record a durable index-removal row in
  `dist_reindex_journal`, in the destroy's transaction, **only when another transaction has already
  committed a pending reindex row for that identifier** — the only case in which the race occurs.
- The "pending row" predicate, pinned: a row for the identifier with `dist_action` = `REINDEX`,
  `priority <= 400` (the rows `loadUpLocalQueue` will load; parked rows above 400 are never mapped)
  and **not written by the destroy's own transaction** (in PostgreSQL, `xmin <>
  pg_current_xact_id()::xid`). No `serverid` filter: a row claimed by any node can still be mapped.
- Insert the removal row **after** the destroy's own enqueues — at today's `journalContentDeletes`
  position, after `contentFactory.delete`. Within a batch the reindex thread keeps only the highest
  row id per identifier (`ReindexQueueFactory.findContentToReindex`), so a removal row written
  before the destroy's own `REINDEX` row would lose to it.
- One statement per batch of identifiers, no extra round trip: `INSERT … SELECT … FROM
  unnest(?) WHERE EXISTS (…)`, answered from `dist_reindex_index4 (ident_to_index, serverid)`.
- Keep the `DEFER` behaviour exactly as today (it already journals unconditionally).
- A regression test that reproduces the race deterministically and fails before the fix.

**Explicitly out of scope / non-goals**:

- `ESContentletAPIImpl.delete(List, …, allVersions)` and `deleteVersion`: both can delete a subset of
  an identifier's versions or languages, and a journal row is identifier-wide — it would remove
  documents for languages that still exist. The same reason #37276 excluded them.
- Removing either of the destroy's own enqueues (Root-Cause Hypothesis): both are harmless, and
  `resetPermissionReferences` serves other permission paths.
- Journalling every `FORCE` / `WAIT_FOR` destroy unconditionally — the variant reverted in
  `1fc4f23bfc`.
- Repairing orphans that already exist in customer indices. A full reindex removes them; detecting
  them is the job of the per-document drift endpoint (#38752, status `EXTRA`).
- Changing `PersonaAPITest`'s defensive orphan purge in this change; it can be reconsidered once the
  fix has run in CI.

**Residual windows (documented, not closed)**:

- **Another transaction enqueues the identifier during the destroy**, after the `EXISTS` ran but
  committing before the destroy: the check did not see it, and its write can race as today.
- **Cluster ordering.** The guarantee "the removal is applied after the stale write" holds on a
  single node. Rows are shared across nodes by `MOD(id, servers)`, so the removal row and the
  pending row can land on different nodes, and the removal can be applied while the other node's
  stale write is still in flight.
- **Engine retries.** A stale write that hit a retryable error (429) can be re-sent by the bulk
  processor's backoff after the removal was applied; on shutdown the Elasticsearch processor waits
  up to 20 s (`awaitClose`) to flush. Either can re-create the document after its removal.
- **A later `REINDEX` row in the same batch.** A `REINDEX` row for the identifier written after the
  removal row (an asynchronous post-commit enqueue) wins the batch merge and drops the removal;
  `mapEntry` then finds no rows and writes nothing, so a stale write still in flight is not undone.
- **Delayed, not lost.** The removal row's id is assigned inside the destroy's transaction; if the
  reindex cursor (`lastIdIndexed`) has already moved past it when it commits, it is picked up only
  when the cursor resets on an empty load.

## Regression Risk *(mandatory)*

- **Blast radius**: `destroyContentlets` and `deleteAllVersionsandBackup` under `FORCE` /
  `WAIT_FOR`. The added statement runs in each such destroy transaction; a row it adds is consumed by
  the reindex thread like any delete-journal row from #37276.
- **Performance** (measured 2026-11-26 on image `trunk_240d1dc`, PostgreSQL 18.6). A `FORCE` destroy
  of one contentlet runs 60–69 statements in 7–17 ms of database time and 63–116 ms wall time. The
  pinned predicate is no longer index-only: it reads the heap rows of the identifier to filter on
  `dist_action`, `priority` and `xmin`. On a copy of the journal holding 1,000,000 rows (same
  structure and indexes, ~1–2 rows per identifier, every priority band, 10% delete rows):

  | Batch | Execution time |
  |---|---|
  | 1 identifier, no row to add | 0.07–0.11 ms |
  | 100 identifiers, none pending (check only) | 0.24–0.35 ms |
  | 100 identifiers, ~all pending (check + ~100 inserts) | 2.5–3.6 ms |
  | 1 identifier with 5,000 parked rows (worst case for the filter) | 2.9–3.1 ms |

  In the common case (one identifier, nothing pending) the cost is under 1% of the destroy's
  database time. The 100-identifier figure with rows to add is dominated by the inserts, which are
  exactly the rows the fix exists to write.
- **Test harness**: the reindex thread runs in integration tests (`RuleWatcher`), but a destroy that
  closely follows a save will now regularly add a removal row, because the save left a pending row
  (step 1 of Reproduction). Tests that destroy and then call `assertEmptyQueue` see one more row to
  drain. Planning measures this on the classes where it is most likely —
  `WorkflowResourceIntegrationTest`, `ReindexAPITest`, `HostBundlerHandlerTest` — plus the classes
  that failed in `1fc4f23bfc`'s run, rather than assuming it.
- **Backward compatibility**: no schema change, no index-mapping change, no REST contract change.
  Rows use the existing delete action already consumed by `ReindexThread` since #37276.
- **Data considerations**: none; existing orphans are not repaired (non-goal).

## Acceptance & Verification *(mandatory)*

- **AC-001**: With a reindex row for a contentlet committed by another transaction, a `FORCE` destroy
  whose inline removal is followed — before the commit — by a write of that contentlet's document
  from another connection ends, once the journal is processed on a single node, with **no**
  document for the identifier in the working or live index.
- **AC-002**: Same as AC-001 under `IndexPolicy.WAIT_FOR`, and same as AC-001 through
  `deleteAllVersionsandBackup`.
- **AC-003**: A `FORCE` or `WAIT_FOR` destroy of a contentlet with **no** pending row from another
  transaction adds **no** removal row — although the destroy itself writes a `REINDEX` row through
  `resetPermissionReferences` in the same transaction.
- **AC-004**: A `FORCE` / `WAIT_FOR` destroy of a contentlet **with** such a pending row adds exactly
  one removal row for that identifier, with a higher id than every row the destroy's transaction
  wrote for it, and none if the destroy rolls back.
- **AC-005**: A pending row that is parked (`priority > 400`) or is itself a removal does **not**
  trigger a removal row.
- **AC-006**: A `DEFER` destroy behaves exactly as today (one removal row, unconditional).
- **AC-007**: Destroying N contentlets in one call issues one check-and-insert statement per batch,
  not one per identifier.
- **AC-008**: The existing delete-journal tests (`ReindexDeleteJournalTest`), the tests that failed
  in `1fc4f23bfc`'s run (`HostBundlerHandlerTest`, `Task240306MigrateLegacyLanguageVariablesTest`),
  `WorkflowResourceIntegrationTest` and `ReindexAPITest` still pass.
- **AC-009**: In 20 runs of `PersonaAPITest`, each in one JVM after a test that leaves a persona on
  SYSTEM_HOST, `[#37886] removing orphaned persona` is logged in none. Before the fix the same
  procedure is expected to log it in some runs (about 1 in 5); if the baseline shows none, the
  criterion is inconclusive and AC-001/AC-002 carry the verification.
- **Verification method**: a new integration test reproducing the race with two connections (fails
  before the fix — Red), integration tests for AC-003 to AC-007, and targeted runs of the classes in
  AC-008 and AC-009, with `-Dmaven.build.cache.enabled=false` and `Tests run:` confirmed in the
  failsafe report.

## Assumptions

- `IndexPolicy.WAIT_FOR` removes inline before the commit, like `FORCE` (`removeContentFromIndex`
  sends every non-`DEFER` policy inline); confirmed in planning.
- `contentFactory.delete` invalidates the version-info cache, so the workflow-task commit listener
  finds no working or live version after the commit (observed in the rig, not traced in code).
- The pending row that enables the race is committed before the destroy's check runs; a row committed
  later is a residual window.
