# Release note — silent index delete loss (#37276)

## What changed

Content deletion now records its index removal durably. Previously the removal was handed to an
in-memory post-commit task with no record of the pending work: if that task was lost — the JVM
stopped between commit and execution, the shared pool rejected it, or the bulk write came back
with per-item failures that were logged and treated as success — the index kept a document whose
content no longer existed, and nothing ever retried.

Deletions are now journalled in `dist_reindex_journal` inside the same transaction that deletes
the rows, and retried by `ReindexThread` until the index acknowledges them — the same guarantee
content additions already had.

Acknowledging a processed entry no longer clears every journal row for that identifier. It clears
only rows up to the one applied, so a removal queued while an earlier entry for the same content
was still in flight stays owed instead of being discarded with it. Existing deduplication of
redundant entries is unaffected: superseded rows are still cleared with the entry that supersedes
them.

## What operators will notice

**New errors on index writes that used to be silent.** A bulk write that comes back with per-item
failures (a saturated write queue, an unavailable shard, a version conflict) now raises instead of
being logged and reported as success.

**This is not a regression.** Those failures were always happening; only their visibility changed.
An environment that has been quietly losing index writes will start surfacing them at the moment
of the write rather than as unexplained index drift weeks later. Treat a new `putToIndex` failure
as a pre-existing condition now made visible — most often index write-queue saturation — and
investigate the index cluster, not this release.

**During the ES→OpenSearch migration, Phase 2 writes can now fail where they previously did
not.** From Phase 2 onwards OpenSearch serves reads while writes still go to both clusters. A
failed OpenSearch write used to be logged and ignored as a shadow divergence — which left
orphaned documents in the very index being queried. It now reaches the caller and, on the
reindex-journal path, marks the entry for retry.

This affects every write in Phase 2, not only removals. An environment whose OpenSearch cluster
is unhealthy will begin surfacing errors that were previously absorbed. Phase 1 is unchanged:
while nothing reads from OpenSearch, a failed shadow write is still logged and ignored.

**Log wording changed.** Bulk failures previously read `Error reindexing` regardless of the
operation, including removals. They now read `Error applying (N) index operation(s)`. Any log
alerting or saved search matching the old string needs updating.

## Existing orphaned documents are not repaired

This change prevents new divergence. It does not clean up documents already orphaned in an index.
A full reindex remains the remedy for existing drift.

Because the index document `_id` maps one-to-one onto `contentlet_version_info`'s primary key,
the current orphan count can be measured directly: compare the live-index document count against

```sql
SELECT count(*) FROM contentlet_version_info WHERE live_inode IS NOT NULL;
```

## Removals that exhaust their retries

A removal that fails `REINDEX_MAX_FAILURE_ATTEMPTS` times is parked in `dist_reindex_journal`
above `ERROR` priority, where the drain query no longer reaches it. It is not retried further and
not deleted — deliberately, so the residue stays enumerable:

```sql
SELECT ident_to_index, priority, index_val
FROM dist_reindex_journal
WHERE dist_action = 2 AND priority > 400;
```

Rows returned by that query are removals the index still owes. A non-empty result means something
is persistently rejecting index writes and warrants investigation; the `index_val` column carries
the last failure cause.

The same residue is listed by the Maintenance portlet and by `GET /api/es/index/failed`, which now
distinguish a parked removal from a parked reindex rather than reporting both as reindexes.

## Compatibility

No database schema change, no index mapping change, no REST contract change. `dist_reindex_journal`
already carried a delete action type and both search providers already consumed it, so a
mixed-version cluster during a rolling deploy sees nothing unfamiliar. **Rollback-safe.**

`putToIndex` is on the public `ContentletIndexAPI` interface. Its signature is unchanged and the
change is source- and binary-compatible, but an out-of-tree plugin that relied on it returning
normally after a partial failure will now see an exception.

## Paths deliberately unchanged

Unpublish and archive keep the existing deferred-removal path. A journal entry is
identifier-wide, and a removal there is per language — reusing the mechanism would drop languages
that are still live. The same reasoning excludes
`ContentletAPI#delete(List, User, boolean, boolean)`, which can delete a subset of an
identifier's versions.
