# Issue Resolution Specification: Site Search job holds one pooled DB connection in an open transaction for the entire crawl

**Feature Branch**: `37321-site-search-job-db-conn`

**Created**: 2026-09-19

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37321](https://github.com/dotCMS/core/issues/37321)

**Input**: GitHub issue 37321 — "Site Search: SiteSearchJobImpl holds one pooled DB connection in an open transaction for the entire crawl, so the audit insert fails on long runs"

## Problem Statement *(mandatory)*

The Site Search crawl job opens a database transaction — and so leases a pooled JDBC connection —
before the crawl starts and keeps it until after the crawl ends. On a large site the crawl runs for
hours, and the connection goes quiet for long stretches (most obviously while the search-engine
publisher reads bundled files off disk and pushes documents, which issues essentially no SQL).

If a quiet stretch outlasts the idle timeout of any stateful network device between dotCMS and
Postgres, the TCP flow is silently evicted. Nothing in the stack recovers: HikariCP never validates
or retires an **in-use** connection, pgjdbc's `tcpKeepAlive` defaults to `false` and is not set
anywhere, and the audit `INSERT` — the job's only database write — is the first statement to touch
the dead socket. It fails, the failure is caught and logged, and the job still reports "Job Finished".

Three consequences:

1. **The audit row is lost silently.** The job reports success; only a careful log reader notices.
2. **Incremental indexing degrades to a full rebuild.** The audit row is the checkpoint an
   incremental crawl anchors its delta on. With no row, the next run re-crawls from an older
   surviving audit or rebuilds entirely — another multi-hour run, which makes recurrence *more*
   likely, not less.
3. **Database-wide autovacuum impact.** A transaction held open for hours pins the `xmin` horizon,
   suppressing dead-tuple cleanup across the entire database — table and index bloat for unrelated
   workloads, not just Site Search.

The transaction buys nothing in exchange. The job's only write is that single-row audit `INSERT`.
Job-detail changes are persisted by Quartz on its own connection, the Site Search bundlers are
read-only on this path, and the crawl's real output goes to the search engine, which no Postgres
transaction can roll back.

**Severity / Impact**: **Medium.** Indexing itself completes correctly, so content is searchable;
what is lost is the audit record and, through it, incremental indexing. Affects customers whose Site
Search crawls run long enough to cross a network idle timeout — i.e. large sites, the ones for whom
a forced full rebuild costs the most. The autovacuum impact of the multi-hour open transaction
reaches beyond Site Search and lifts this above cosmetic. Reported from production via
[Freshdesk #39033](https://dotcms.freshdesk.com/a/tickets/39033).

## Reproduction *(mandatory)*

**Environment**: Reported on **26.07.13-01** (Current Release / dotEvergreen), self-hosted, Postgres
over SSL, with a stateful firewall/NAT between dotCMS and the database. Confirmed still present on
`main`. Reported site: ~31.9K files / 8.6K pages / 10.6K urlmaps; the run spanned ~8h35m from fire
time to failure. Not a regression — the transaction has been in this method since at least the 2016
source reorganisation.

**Steps to Reproduce** (fast synthetic version — the mechanism is a held connection, so it does not
require an eight-hour wait):

1. Run dotCMS against Postgres.
2. Schedule a Site Search job over enough content that the crawl runs for at least a few minutes.
3. While the crawl runs, confirm the held connection:
   ```sql
   SELECT pid, state, wait_event, xact_start, now() - xact_start AS held
   FROM pg_stat_activity
   WHERE application_name LIKE '%dotCMS%' AND state = 'idle in transaction';
   ```
   A session appears whose `xact_start` dates to the beginning of the crawl, with
   `wait_event = ClientRead`.
4. Kill that backend mid-crawl to stand in for the network drop:
   `SELECT pg_terminate_backend(<pid>);`
5. Let the crawl finish.
6. Check for the audit row: `SELECT * FROM sitesearch_audit WHERE job_id = '<job-id>';`
7. Run the job again.

**Expected Behavior**:

- No dotCMS session sits `idle in transaction` for the duration of the crawl (step 3 returns nothing
  for this job).
- The audit row is written even though the connection that existed at the start of the crawl is gone,
  because the `INSERT` runs on a freshly leased, pool-validated connection (step 6 returns a row).
- The next run indexes incrementally (step 7).

**Actual Behavior**:

- Step 3 shows a session `idle in transaction` since the crawl began.
- Step 5 logs the failure and finishes anyway:
  ```
  ERROR job.SiteSearchJobImpl: can't save audit data
  com.dotmarketing.exception.DotDataException: An I/O error occurred while sending to the backend.
    "SQL": ["insert into sitesearch_audit (job_id,job_name,fire_date,…) values (?,?,?,…)"]
  INFO  job.SiteSearchJobImpl: Job Finished
  ```
  preceded by `WARN pool.ProxyConnection: … marked as broken because of SQLSTATE(08006)` and a
  `ProxyLeakTask` "previously reported leaked connection … was returned to the pool (unleaked)"
  notice. The leak warning is a symptom, not the cause: it is `leakDetectionThreshold` (default
  300000 ms) reporting a connection held well past five minutes.
- Step 6 returns no row. Step 7 performs a full rebuild.

**Reproducibility**: Deterministic with the synthetic repro above. Organically, it depends on the
crawl containing a quiet stretch longer than the network device's idle timeout (commonly 30–60
minutes), so it recurs on large sites and is invisible on small ones.

**Diagnostic note**: the reported failure is `SocketException: Connection timed out` (`ETIMEDOUT`),
not `Connection reset` — TCP retransmissions expiring against a black hole. That is the signature of
a stateful device dropping the flow without sending `RST`, and it rules out a Postgres-side kill such
as `idle_in_transaction_session_timeout` (which produces `FATAL: terminating connection…` and a
reset). In the reported environment that timeout was at its default (disabled) and no connection
pooler was in the path.

## Scope of Investigation *(mandatory)*

- **Affected area**: Site Search — the scheduled crawl/index job and its audit trail, plus the
  incremental-indexing checkpoint that depends on that audit row.
- **Suspected surface**: **Mixed.** The job orchestration is modern
  (`com.dotcms.publishing.job.SiteSearchJobImpl`, `SiteSearchJobProxy`); the audit persistence and
  the transaction helper are legacy (`com.dotmarketing.sitesearch.business.SiteSearchAuditAPIImpl` /
  `SiteSearchAuditFactoryImpl`, `com.dotmarketing.db.HibernateUtil`). Expect Legacy Impact to apply.
- **Related known decisions**: Transaction-boundary conventions around `@WrapInTransaction` /
  `@CloseDBIfOpened` and the `WrapInTransactionInterceptor`'s nesting rule (a nested call joins the
  enclosing transaction rather than opening its own). The plan formally consults
  `dotCMS/platform-adrs` for any ADR covering transaction scope and long-running jobs.
- **Related issues**: #34833 is the same defect class in a different feature (long non-transactional
  work inside `@WrapInTransaction` leaving Postgres `idle in transaction`). #36706 is the other way
  this same `sitesearch_audit` row fails to persist (`path varchar(500)` overflow), reported from the
  same environment — both are silently absorbed by the identical `catch`.

### Transactional footprint of the crawl

Established by reading the path, because it determines whether this fix can leave the database in a
half-written state:

- **The job performs exactly one Postgres write: the `sitesearch_audit` INSERT, at the very end.**
  There is therefore no multi-statement unit of work to split into smaller transactions, and no
  ordering between writes that a failure could violate. "Remove the transaction" here does not mean
  "break one big transaction into several" — it means a single-row write stops being wrapped in a
  transaction that spans hours of unrelated work.
- **`publishing_queue_audit` is *not* written by this job.** `PublisherAPIImpl.publish()` does guard
  an update of that table, but only `if (currentStatus != null)` — and `currentStatus` comes from
  `publishAuditAPI.getPublishAuditStatus(config.getId())`, which returns `null` for a Site Search
  crawl. Rows in `publishing_queue_audit` are inserted only by `PublisherQueueJob`, the push-publishing
  queue processor; Site Search bypasses the queue and calls `publisherAPI.publish()` directly with a
  `SiteSearchConfig`, so no audit row for that config id exists and the
  `updatePublishAuditStatus(..., BUNDLING, ...)` call is skipped. The `isPublishRetry(config.getId())`
  check on the same table is a read, and returns `false` for the same reason.
- **Quartz's own `qrtz_*` tables are written by Quartz**, on its own connection, outside this
  transaction — including the `JobDataMap` alias repair that `prepareJob()` performs via
  `persistResolvedAlias`.
- **The crawl's real output is not in Postgres at all.** It goes to the search engine: documents
  written into an index and, on a full rebuild, a new index created and the alias re-pointed. No
  Postgres transaction has ever covered that, and none could.

## Root-Cause Hypothesis

The job opens a transaction at the top of `run()` and closes the session only in the `finally`, so a
single pooled connection is leased across the whole crawl. Because a transaction is already open,
the audit save's `@WrapInTransaction` joins it instead of leasing a fresh, pool-validated connection
— which is what would otherwise have made the write immune to the stale socket. The long-held
connection is never health-checked while in use, and the socket emits no keepalive traffic, so it can
be silently evicted before the `INSERT` runs.

The reporter's assessment is that removing the job-level `startTransaction()` is sufficient: the
audit save is already `@WrapInTransaction` and would then open its own short transaction on a fresh
connection at the end of the crawl. Two things the plan must confirm rather than assume:

- **No other code holds a connection across the crawl.** `SiteSearchJobProxy.run()` is annotated
  `@CloseDBIfOpened`, which closes the connection only when the method exits. If anything inside the
  crawl opens a connection and leaves it in the thread-local without closing it, the connection is
  still held for hours — just without the transaction. Removing `startTransaction()` alone would then
  fix the transaction/autovacuum half and not the stale-connection half.
- **The crawl phase really is read-only against Postgres on this path.** Verified in the report for
  `FileAssetBundler`, `URLMapBundler` and `HTMLPageAsContentBundler` (no raw SQL, no `DotConnect`, no
  `HibernateUtil`); the one write present, `FileAssetBundler`'s
  `pushedAssetUtil.savePushedAssetForAllEnv(...)`, is gated behind `config.shouldManageDependencies()`
  → `isStatic()`, which `SiteSearchConfig` never sets. One caveat the plan should weigh:
  `URLMapBundler` and `HTMLPageAsContentBundler` call `getHTML()`, which executes user-authored
  Velocity — a template or plugin viewtool could in principle write. If it does, that write loses its
  enclosing transaction when this one is removed.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Remove the job-wide transaction/connection hold from the Site Search crawl so no pooled connection
  is leased, and no Postgres transaction is open, across the crawl phase.
- Ensure the audit `INSERT` runs on a freshly leased, pool-validated connection at the end of the
  crawl, so a socket that died during the crawl cannot lose the row.
- Surface a failed audit save instead of swallowing it: **the job fails.** The `catch` that logs
  `"can't save audit data"` and continues is replaced by letting the failure out of `run()`, so
  `SiteSearchJobProxy` wraps it in a `JobExecutionException` and Quartz reports the job as failed
  rather than "Finished".
- Record the expectation that the crawl phase is read-only against Postgres, so a later change that
  sets `setStatic(true)` on a `SiteSearchConfig` cannot silently reintroduce per-asset writes inside
  a now-untransacted crawl.

**Explicitly out of scope / non-goals**:

- **The other symptom of the same `catch`**: the `path varchar(500)` overflow of #36706 is a separate
  defect and is not fixed here. Note the consequence of the decision above: once the audit failure is
  no longer swallowed, a job hitting #36706 will *fail* rather than silently finish. That is correct —
  the row is genuinely missing either way — but it means this fix changes observable behavior for a
  defect it does not repair, and #36706 becomes more urgent, not less.
- **Pool/driver-level hardening as the fix**: configuring `keepaliveTime`, `tcpKeepAlive`, or a
  connection-validation strategy. These are the documented customer *workaround* and a possible
  separate hardening item; they do not address the open transaction and its autovacuum cost.
- **Any redesign of Site Search crawling, bundling, or the incremental checkpoint model.** The audit
  row stays the checkpoint; only the transaction boundary around writing it changes.
- **Durable execution for this job**: an in-progress/completed/failed state machine around the crawl,
  automatic retry of transient failures, or migration onto `com.dotcms.jobs.business`. Deliberately
  deferred, with the constraint that this fix must not make it harder — see *Consistency on failure,
  and the durable-execution question* under Regression Risk for the constraints and the in-repo
  precedents a future effort should start from.
- **Rewriting `HibernateUtil`, the transaction interceptors, or the legacy audit factory.**
  Progressive enhancement only, on lines this fix already touches.
- **The audit of other Quartz jobs using the same start-transaction-then-do-long-work pattern**
  (`ContentImportThread`, `DeleteOldClickstreams`, `CleanUnDeletedUsersJob`, `IdentifierDateJob`,
  `DeleteUserJob`). No code outside Site Search changes here. Instead, a **follow-up GitHub issue** is
  filed recording the pattern and these five candidates — that issue is a deliverable of this work
  (see AC-007), so the knowledge is not lost when this fix merges.

## Regression Risk *(mandatory)*

- **Blast radius**: The Site Search crawl path only — `SiteSearchJobImpl.run()` and everything it
  calls under the removed transaction: `prepareJob()`, the publisher/bundler chain, and the audit
  save. The risk is the inverse of the fix: work that today runs inside the job's transaction would
  afterwards run without one. That is intended for read-only work; the plan must confirm nothing on
  this path relies on the enclosing transaction for atomicity — in particular the Velocity-executing
  bundlers noted above, and any rollback/commit listener registered during the crawl (the job-level
  `startTransaction()` clears the rollback and commit listener lists today, so listener behavior
  changes when it is removed).
- **Backward compatibility**: No API, REST contract, DB schema, or search-engine mapping change is
  expected. The `sitesearch_audit` table, its row shape, and the incremental checkpoint semantics are
  unchanged. Failing the job on a failed audit save is an **observable behavior change** and the
  riskiest part of this fix: a run that previously reported success now reports failure — including
  runs failing for #36706's unrelated overflow reason. **It does not cause the crawl to be re-run**:
  `setRefireImmediately` / `setUnscheduleFiringTrigger` appear nowhere in the dotCMS source, so a
  `JobExecutionException` is logged and the trigger simply fires next on its normal schedule.
  **It also does not lose the job detail's `JobDataMap`** — verified, so the alias repair
  `persistResolvedAlias()` writes during `prepareJob()` survives a failing run (see *Quartz behavior
  on a thrown job*). The one point the plan must still confirm is that the failure is raised *after*
  the crawl's search-engine output is complete, so a failed audit save never leaves the index in a
  worse state than today. This is the behavior a rollback would revert, and it belongs in the release
  notes.
- **Data considerations**: No migration or repair of existing data. Audit rows lost to past
  occurrences are not recoverable and are not recovered here; the first run after the fix on an
  affected site will still be a full rebuild (no checkpoint to anchor on), and incremental behavior
  resumes from the run after that.

### Consistency on failure, and the durable-execution question

The usual hazard when a long transaction is broken up — partial writes leaving the database
internally inconsistent — **does not apply here**, because there is only one write (see
*Transactional footprint of the crawl*). Removing the transaction cannot produce a half-written
database state; there is no second row that could disagree with the first.

The consistency gap in this job is real but lies elsewhere, and it **predates this fix and is
unchanged by it**: the search engine and the audit checkpoint can disagree. A crawl mutates the index
(documents written; on a full rebuild a new index is created and the alias re-pointed) and *then*
records that it happened, in a single non-transactional row. Any failure between those two moments —
including today's silent one — leaves an updated index with no checkpoint. The system already
degrades safely when that happens: the next run finds no recent audit, falls back to a full rebuild,
and converges. That is why this defect costs hours rather than correctness.

This fix must therefore hold to two constraints rather than solve the gap:

1. **Do not widen the window.** The audit write stays immediately after the crawl completes, on its
   own short transaction. The fix must not introduce additional Postgres writes *during* the crawl
   phase — a "progress" row written mid-crawl would reintroduce exactly the connection use this issue
   is about.
2. **Do not foreclose a future state machine.** Recording an in-progress state before the crawl and a
   completed/failed state after it — the durable-execution pattern, with retries for transient
   failures and permanent failure after N attempts — is a legitimate future direction for this job.
   It is out of scope here (see Non-Goals), but nothing in this fix may make it harder: the audit row
   keeps its current shape and meaning, and failure is surfaced rather than swallowed, which is the
   precondition any retry policy would need.

Two in-repo precedents for that future work, so it is not designed from scratch:

- `com.dotcms.publisher.business.PublishAuditStatus.Status` — the push-publishing side already runs a
  status machine (`BUNDLING`, and the rest) over `publishing_queue_audit`, driven by
  `PublisherQueueJob`, with a `numTries` count and an `isPublishRetry` check.
- `com.dotcms.jobs.business` — the modern job framework, with `JobState` (`PENDING`, `RUNNING`,
  `SUCCESS`, `FAILED`, `FAILED_PERMANENTLY`, `ABANDONED`, `ABANDONED_PERMANENTLY`, `CANCEL_REQUESTED`,
  `CANCELLING`) and a pluggable `RetryStrategy`. A future "make Site Search durable" effort is most
  likely a migration onto this, not a bespoke mechanism bolted onto the Quartz job.

On retries specifically: Quartz offers no built-in retry-with-backoff — a job signals re-execution by
setting `refireImmediately` on the `JobExecutionException`, which dotCMS does nowhere. So retry is
not something this fix turns on or off; it is absent today and stays absent. It would also need the
distinction the reporter raises — a stale connection is worth retrying, a malformed search-engine
request never is — plus idempotency analysis per phase (indexing a document by id is idempotent;
creating a new index and re-pointing an alias is not, though a full rebuild is self-correcting by
construction). That analysis belongs to the future work, not here.

### Quartz behavior on a thrown job

Verified against the actually-bundled Quartz, because AC-003 depends on it. dotCMS ships
`com.dotcms.lib:dot.quartz-all:1.8.6_2` — a patched **Quartz 1.8.6** (the `org.quartz-scheduler:quartz`
2.3.2 entry in `bom/application/pom.xml` is annotated "replaces dot.quartz-all" but `dotCMS/pom.xml`
still depends on `dot.quartz-all`, and that is the only Quartz artifact resolved). So the mechanism is
1.x `StatefulJob`, not the 2.x `@PersistJobDataAfterExecution` annotation, and `DotJobStore extends
JobStoreCMT` without overriding completion handling.

Traced through the bundled classes:

1. `JobRunShell.run()` catches the `JobExecutionException` thrown by the job (exception handler over
   the `Job.execute` call site), then continues on the normal path.
2. `Trigger.executionComplete(jec, jobExEx)` yields the instruction. Re-execution happens only for
   `INSTRUCTION_RE_EXECUTE_JOB`, which requires `refireImmediately` on the exception — set nowhere in
   dotCMS. Otherwise `JobRunShell` calls `complete(true)` and then
   `QuartzScheduler.notifyJobStoreJobComplete(...)`.
3. `JobStoreSupport.triggeredJobComplete(...)` then runs
   `if (jobDetail.isStateful()) { …; if (jobDetail.getJobDataMap().isDirty()) getDelegate().updateJobData(conn, jobDetail); }`
   — the branch tests `isStateful()` and `isDirty()` only. The `JobExecutionException` is not consulted.

`SiteSearchJobProxy extends DotStatefulJob implements StatefulJob`, and `persistResolvedAlias()`'s
`dataMap.put(...)` marks the map dirty, so the alias repair is persisted on a failing run exactly as
on a successful one. This matches what `SiteSearchJobImpl`'s existing Javadoc on `persistResolvedAlias`
already asserts ("the JDBC job store re-persists the job detail's data map after every execution,
including one that ended in an exception") — now confirmed rather than assumed.

**Consequence for this fix**: throwing on a failed audit save costs nothing in job-detail state and
triggers no retry. It changes the reported outcome of the run and nothing else.

## Acceptance & Verification *(mandatory)*

- **AC-001**: During a Site Search crawl, no dotCMS session is `idle in transaction` and no pooled
  connection is held for the crawl's duration. The `pg_stat_activity` query in the reproduction
  returns no row for this job at step 3.
- **AC-002**: The audit `INSERT` succeeds on a crawl whose pre-existing connection went stale. With
  the connection killed mid-crawl (repro step 4), the audit row is present at step 6.
- **AC-003**: A failed audit save fails the job. When the audit `INSERT` cannot be written, the
  exception propagates out of `SiteSearchJobImpl.run()`, `SiteSearchJobProxy` reports it as a
  `JobExecutionException`, and no "Job Finished" success entry is logged for that run.
- **AC-004**: Incremental indexing still works across consecutive runs: the audit row is written, the
  next run anchors its delta on it, and no unintended full rebuild occurs. The existing incremental
  scenarios in `SiteSearchJobImplTest` continue to pass.
- **AC-005**: The crawl phase's read-only-against-Postgres expectation is recorded in a form a future
  change would have to confront (documented and/or asserted in a test), covering the
  `shouldManageDependencies()` / `isStatic()` gate on `SiteSearchConfig`.
- **AC-006**: No `ProxyLeakTask` leaked-connection warning is emitted for the Site Search job thread
  on a crawl exceeding the 300000 ms `leakDetectionThreshold`.
- **AC-007**: A follow-up GitHub issue exists recording the same start-transaction-then-do-long-work
  pattern in `ContentImportThread`, `DeleteOldClickstreams`, `CleanUnDeletedUsersJob`,
  `IdentifierDateJob` and `DeleteUserJob`, cross-linked with this issue.
- **AC-008**: The crawl phase issues no Postgres write. The `sitesearch_audit` INSERT remains the
  job's only write and still occurs immediately after the crawl completes — the fix introduces no
  mid-crawl progress/state row, so the window between the index changing and the checkpoint being
  recorded is no wider than it is today.

- **Verification method**:
  - Integration: extend `dotcms-integration/.../com/dotcms/publishing/job/SiteSearchJobImplTest.java`
    (already registered in `MainSuite1a` and `QuickSuite`, so new methods there run in CI) — run with
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dmaven.build.cache.enabled=false -Dit.test=SiteSearchJobImplTest`
    and confirm `Tests run: N` in `target/failsafe-reports/*.txt` rather than trusting the exit code.
    Coverage to add: no open transaction / no held connection across the crawl (AC-001); audit row
    still written when the connection in play at crawl start is invalidated (AC-002); a job whose
    audit save fails reports failure instead of "Finished" (AC-003).
  - Unit: where the surfacing logic can be exercised without a database, a unit test under
    `dotCMS/src/test/java/com/dotcms/publishing/job/` alongside the existing
    `SiteSearchJobAliasResolutionTest`.
  - Manual: the synthetic reproduction above against a Postgres instance, checking
    `pg_stat_activity` mid-crawl and `sitesearch_audit` afterwards, then a second run to confirm it
    goes incremental.
  - Per Constitution Principle V, these tests are written, dev-approved, and confirmed failing (Red)
    before any implementation.

## Assumptions

- The reported environment's diagnosis is accepted as given: a stateful network device evicting an
  idle TCP flow, with `idle_in_transaction_session_timeout` disabled and no connection pooler in the
  path. The fix does not depend on this being the only way the connection can die — any cause of a
  stale in-use connection is addressed by not holding one.
- The crawl's Postgres writes are limited to the audit `INSERT` on the Site Search path — established
  in *Transactional footprint of the crawl* for `publishing_queue_audit` and the publisher path, and
  taken from the issue's bundler verification for the bundlers themselves. If planning finds a write
  that today depends on the job-level transaction, fix scope is revisited before implementation.
- Quartz persists its own job-detail changes (including the alias repair written by
  `persistResolvedAlias`) on its own connection, outside this transaction, so removing it does not
  affect that persistence.
- "Recorded" in AC-005 is satisfied by Javadoc/comment on the relevant code plus a test assertion
  where one is practical; no new documentation file is assumed necessary. The plan may choose
  otherwise.

## Clarifications

### Session 2026-09-19

- **Q: How should a failed audit save be surfaced?** → **A: Fail the Quartz job.** The exception
  propagates out of `run()`; `SiteSearchJobProxy` turns it into a `JobExecutionException` and the run
  reports failure. Chosen over recording on the runtime status or a "Finished with errors" log entry:
  the defect's whole character is that it was invisible, and a failed run is the one signal an
  operator cannot miss. Consequences captured in AC-003, Fix Scope, and Regression Risk — notably
  that jobs failing for #36706's reason will now fail too, and that Quartz's refire behavior for this
  `DotStatefulJob` must be settled in the plan.
- **Q: Are the other same-pattern Quartz jobs in scope?** → **A: Follow-up issue.** This fix stays
  bounded to Site Search; a separate issue records the pattern and the five candidates
  (`ContentImportThread`, `DeleteOldClickstreams`, `CleanUnDeletedUsersJob`, `IdentifierDateJob`,
  `DeleteUserJob`). Tracked as AC-007 so it is a deliverable rather than a good intention.

### Session 2026-09-20 — consistency review

- **Q: Does splitting the transaction risk leaving the database inconsistent, particularly
  `publishing_queue_audit`?** → **A: No, and that table is not involved.** Verified in the code:
  `publishing_queue_audit` rows are inserted only by `PublisherQueueJob` (push publishing); a Site
  Search crawl bypasses the queue, so `getPublishAuditStatus(config.getId())` returns `null` and
  `PublisherAPIImpl`'s `updatePublishAuditStatus(..., BUNDLING, ...)` is skipped. The job's only
  Postgres write is the single `sitesearch_audit` INSERT, so there is nothing to split and no partial
  state to leave behind. Recorded in *Transactional footprint of the crawl*.
- **Q: Should the fix adopt a durable-execution design (in-progress/completed state, retries)?** →
  **A: Not in this fix, but it must not be foreclosed.** The genuine consistency gap is between the
  search engine and the audit checkpoint, it predates this fix, and the system already degrades
  safely (no checkpoint → full rebuild → converges). Captured as two binding constraints (do not
  widen the window; do not foreclose a state machine), a new non-goal, AC-008, and pointers to the
  two in-repo precedents (`PublishAuditStatus.Status`, `com.dotcms.jobs.business`) so future work
  starts from an existing pattern.
- **Q: Does throwing the audit failure make Quartz retry the crawl?** → **A: No.** `refireImmediately`
  is never set anywhere in the dotCMS source, so a `JobExecutionException` is logged and the trigger
  fires next on its normal schedule. Retry is absent today and stays absent.
- **Q: Does throwing lose the `JobDataMap` alias repair?** → **A: No** — checked before committing the
  spec, rather than left for the plan. In the bundled Quartz 1.8.6, `JobStoreSupport.triggeredJobComplete`
  persists a stateful job's data map on `isStateful() && isDirty()` alone, without consulting the
  `JobExecutionException`, and `JobRunShell` reaches that call after catching the exception. Full trace
  in *Quartz behavior on a thrown job*.
