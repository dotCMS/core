# Phase 1 Data Model: Site Search crawl connection fix (#37321)

**Plan**: [plan.md](./plan.md) · **Date**: 2026-09-22

**Nothing in this document changes.** It is here to make that explicit and reviewable: this fix
moves a transaction boundary, and the point of recording the model is to show that no entity,
column, state or lifecycle is altered by doing so.

## Entities touched by the job

### `sitesearch_audit` — the crawl checkpoint

Written once per crawl by `SiteSearchAuditFactoryImpl.save()`; read by `findRecentAudits(jobId, 0, 1)`.
**The job's only Postgres write.**

| Field | Meaning | Changed by this fix |
|-------|---------|---------------------|
| `job_id`, `job_name` | Which scheduled job this run belongs to | No |
| `fire_date` | When the run was triggered — **the incremental anchor**: the next incremental crawl uses `recentAudits.get(0).getFireDate()` as its `startDate` | No |
| `incremental` | Whether this run was incremental | No |
| `start_date`, `end_date` | Delta window covered by an incremental run (null on a full rebuild) | No |
| `host_list`, `all_hosts`, `lang_list`, `path`, `path_include` | Crawl scope | No |
| `files_count`, `pages_count`, `urlmaps_count` | Totals summed from `BundlerStatus` after the crawl | No |
| `index_name` | The index this run wrote to | No |

Schema, row shape, nullability and cardinality (one row per successful crawl) are all unchanged.
No migration, no backfill, no repair of rows lost to past occurrences of this defect — the first
run after the fix on an affected site still finds no checkpoint and does a full rebuild, and
incremental behavior resumes from the run after that.

**What does change is the transaction the INSERT runs in**: today it joins the job-wide transaction
opened hours earlier; afterwards `SiteSearchAuditAPIImpl.save()`'s `@WrapInTransaction` is the
outermost boundary and opens its own short-lived one on a freshly leased connection.

### Quartz `JobDataMap` — the job's memory between runs

Persisted by Quartz (1.8.6, `StatefulJob`), on Quartz's own connection, outside anything this fix
touches. `prepareJob()` mutates it via `persistResolvedAlias()` to make the alias repair of issue
#36983 survive to the next run.

Unchanged — including on a failing run: `JobStoreSupport.triggeredJobComplete` persists a stateful
job's data map on `isStateful() && isDirty()` alone and never consults the `JobExecutionException`
(verified; see the spec's *Quartz behavior on a thrown job*).

### `publishing_queue_audit` — not involved

Listed for the reviewer who reasonably expects it to be. Rows there are inserted only by
`PublisherQueueJob`; a Site Search crawl bypasses the publisher queue, so
`getPublishAuditStatus(config.getId())` returns `null` and `PublisherAPIImpl`'s
`updatePublishAuditStatus(..., BUNDLING, ...)` is skipped. Neither read nor written by this job,
before or after the fix.

### The search index — the crawl's real output

Documents written into an OpenSearch/Elasticsearch index; on a full rebuild, a new index created
and the alias re-pointed. Never covered by a Postgres transaction and not coverable by one.
Unchanged by this fix.

## State transitions

The job has no persisted state machine — that is the observation behind the spec's durable-execution
discussion, not a gap this fix fills. Its entire between-run state is (a) the latest
`sitesearch_audit` row and (b) the Quartz `JobDataMap`.

Run outcomes, before and after:

| Outcome | Index | Audit row | Job reports | Next run |
|---------|-------|-----------|-------------|----------|
| Crawl succeeds, audit save succeeds | Updated | Written | Finished | Incremental |
| Crawl succeeds, audit save fails — **today** | Updated | Missing | **Finished** (silently wrong) | Full rebuild |
| Crawl succeeds, audit save fails — **after this fix** | Updated | Missing | **Failed** (AC-003) | Full rebuild |
| Crawl itself fails | Partial or unchanged | Missing | Failed | Full rebuild |

Only the third row changes, and only in the "reports" column. The recovery path — no checkpoint
means the next run rebuilds in full and converges — is identical in every case, which is why AC-008
forbids introducing a mid-crawl progress row that would create a fourth, unrecoverable state.
