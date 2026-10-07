# Issue Resolution Specification: OpenSearch reindex batches are bounded by document count only, so large-but-legal documents fail whole batches (HTTP 413) and can crash the reindex thread

**Feature Branch**: `37905-os-bulk-byte-size`

**Created**: 2026-10-07

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37905](https://github.com/dotCMS/core/issues/37905) — "OpenSearch bulk processor flushes by document count only, so large-but-legal documents can exceed http.max_content_length (HTTP 413)"

**Input**: User description: "OpenSearch bulk processor flushes by document count only. Large-but-legal documents exceed http.max_content_length (HTTP 413) and the whole batch fails; the same batching causes OutOfMemoryError in ReindexThread with -Xmx4g. Proposed fix: byte-size-aware flushing in the OpenSearch bulk processor."

## Problem Statement *(mandatory)*

The reindex sends documents to the search engine in batches. On the Elasticsearch path a batch
is closed when it reaches either a number of documents or a size in bytes, whichever comes
first. On the OpenSearch path a batch is closed **only** by the number of documents. Nothing
limits how many bytes one OpenSearch request carries.

When a few documents are large — each one legal, under every per-document limit — a single
batch can exceed the size OpenSearch accepts for one HTTP request (`http.max_content_length`,
100 MB by default). OpenSearch then rejects the whole request with HTTP 413, and **every
contentlet in the batch is recorded as failed**, although none of them is at fault. The
contentlets are retried in the same batches and fail again until they run out of retries.

The same batching also exhausts server memory. The OpenSearch client builds the whole request
body in memory before sending it, in a buffer that grows by doubling; a batch of ten large
documents needs a contiguous allocation of hundreds of MB on top of what it already holds. With
`-Xmx4g` this killed the reindex thread (`ReindexThread terminating on unrecoverable error: Java
heap space`). The thread is started again by the next trigger (a reindex start, a content
operation) and dies again on the same queued entries, so indexing makes no progress while the
reindex screen keeps showing a reindex in progress.

**Severity / Impact**: High from Phase 2 on. In Phases 2 and 3 OpenSearch failures propagate,
so healthy content stays out of the index that users query, and the reindex thread can
crash-loop and stop indexing for everything queued. In Phase 1 OpenSearch is a write shadow:
the rejected batch is only logged, and the shadow index silently misses those documents —
which is what the operator relies on when deciding whether to move to Phase 2. Phase 0 is not
affected. Any install with a few large text bodies (for example HTML pasted with embedded
`data:` images) is exposed during a full reindex, and more so on managed OpenSearch services
whose request limit is lower than 100 MB.

## Reproduction *(mandatory)*

**Environment**: dotCMS built from the #37269 branch (#37924), migration Phase 3
(`PHASE_3_OPENSEARCH_ONLY`), OpenSearch 3.4.0 with the default `http.max_content_length`,
`-Xmx4g`, default `REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS` (10).

**Steps to Reproduce**:

1. Create a content type with two indexed `TextAreaField`s.
2. Create 20 contentlets whose two fields hold 4,500,000 characters each. Each index document
   stays under the per-string parser limit (its `catchall` is about 18,000,000 characters), so
   the per-document check added by #37269 lets it through.
3. Queue them for reindex together (save with `indexPolicy=DEFER`, or run a full reindex).
4. Watch the dotCMS log and `dist_reindex_journal`.

**Expected Behavior**: All 20 contentlets are indexed. No request is rejected for its size and
the reindex thread keeps running.

**Actual Behavior**: Every OpenSearch bulk request is rejected:

```
Bulk process failed entirely: method [POST], host [http://opensearch:9200], URI [/_bulk],
status line [HTTP/1.1 413 Request Entity Too Large]
```

All 20 entries are marked failed in `dist_reindex_journal` with that message. During a full
reindex the reindex thread also dies with `java.lang.OutOfMemoryError: Java heap space` inside
`OpenSearchClient.bulk` (`ApacheHttpClient5Transport.writeNdJson` ←
`OSIndexBulkProcessor.flush`), is started again on the next trigger, and dies again.

**Reproducibility**: Always, given enough large documents in one batch. Comments on #37905
carry the full logs and the stack trace.

## Scope of Investigation *(mandatory)*

- **Affected area**: Search indexing — the reindex queue consumer and the OpenSearch bulk
  write path, in migration Phases 1, 2 and 3.
- **Suspected surface**: Modern. The batching lives in
  `com.dotcms.content.index.opensearch.ContentletIndexOperationsOS` (`OSIndexBulkProcessor`,
  created by `createBulkProcessor`). The legacy queue (`com.dotmarketing.common.reindex.ReindexThread`,
  `ReindexQueueFactory`) is a consumer and is not expected to change.
- **Related known decisions**: OpenSearch content writes are fire-and-forget in Phase 1 and
  propagate from Phase 2 on (`docs/backend/OPENSEARCH_MIGRATION.md`). The Elasticsearch
  processor's byte threshold (`REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE`, MB, default 1) is the
  existing precedent. Configuration follows the `OS_*`/`ES_*` fallback convention in
  `docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md`. The plan consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

`OSIndexBulkProcessor.addAndMaybeFlush` appends each operation to a pending list and flushes
only when the list reaches `maxActions` (`REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS` divided
by the number of reindexing servers). There is no byte accounting. The Elasticsearch processor
gets a byte threshold from `BulkProcessor.builder(...).setBulkSize(...)`; the hand-written
OpenSearch processor never got the equivalent.

Each operation's size is already known when it is added: `addIndexOpToProcessor` receives the
serialized document (`jsonMapping`) before parsing it into the operation. Accounting that size
and flushing the pending batch before an operation would push it over a byte limit bounds both
the request size (the 413) and the client's in-memory request buffer (the OOM). A document
that is larger than the limit on its own is sent in a batch of one, as the Elasticsearch
processor does.

Each document is measured by the character length of `jsonMapping`, counted as if it were
bytes (1 MB = 1,048,576 characters). Decided with the developer: it costs nothing, since the
string is already in hand, and the Elasticsearch processor's own size is an estimate too. For
plain text a character is one byte; for accented or Asian text the request can be somewhat
larger than configured, and the action lines of the bulk body are not counted. With a 1 MB
default against a 100 MB server limit this does not matter; the documentation states that the
limit is approximate and should be set well below the server's `http.max_content_length`.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The OpenSearch reindex bulk processor closes a batch on **either** the document count (as
  today) **or** a byte size, whichever comes first, before adding the operation that would
  exceed it.
- A single document larger than the byte size is sent alone and indexed; it is never split,
  dropped or failed by the batching itself.
- The byte size has its own OpenSearch property (working name
  `REINDEX_THREAD_OPENSEARCH_BULK_SIZE`, MB) that falls back to
  `REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE` when unset, following the `OS_*`/`ES_*` fallback
  convention. Out of the box both engines batch alike; an install whose OpenSearch accepts
  smaller requests than its Elasticsearch can lower only the OpenSearch value.
- The fix applies in every phase that writes to OpenSearch (1, 2 and 3), through the one
  processor they share.
- `docs/backend/OPENSEARCH_CLIENT_CONFIGURATION.md` documents the threshold (in MB, approximate,
  to be set well below the server's `http.max_content_length`) and the workaround below for
  builds without the fix.

**Explicitly out of scope / non-goals**:

- Detecting or surfacing a reindex thread that crash-loops on an `Error` — a separate,
  engine-independent issue.
- The Elasticsearch path: it already batches by bytes.
- The synchronous save path (`putToIndex` with `IndexPolicy.FORCE` / `WAIT_FOR`): one save's
  documents are sent together and are not a reindex batch.
- Raising `http.max_content_length` on the server, streaming the request body, or changing the
  OpenSearch client.
- Documents that exceed the per-document parser limit — handled by #37269.
- Reducing the memory used to map a single large document (copies between `toMap`,
  serialization and the OpenSearch re-parse).

**Workaround until the fix ships**: lower `REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS` (for
example `DOT_REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS=2`); the OpenSearch batch size follows
it, so request size and memory drop proportionally, at the cost of more requests.

## Regression Risk *(mandatory)*

- **Blast radius**: Every reindex write to OpenSearch in Phases 1–3, including full reindex and
  `DEFER` saves. A wrong threshold could make batches too small (many more requests, slower
  reindex) or still too large. With typical small documents the count threshold is reached
  first, so batching must stay exactly as today; this is an acceptance criterion.
- **Backward compatibility**: No change to index mappings, REST contracts, or the
  `dist_reindex_journal` schema. One configuration property is added
  (`REINDEX_THREAD_OPENSEARCH_BULK_SIZE`); existing properties keep their meaning. The listener callbacks (`beforeBulk` / `afterBulk`) are unchanged; only
  how many operations each call carries changes.
- **Data considerations**: Entries already failed by this defect stay parked in
  `dist_reindex_journal`; a reindex after the fix indexes them. No data repair.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With the reproduction above (Phase 3, 20 contentlets with two 4,500,000-character
  fields, default `http.max_content_length`), a reindex indexes all 20, no bulk request is
  rejected with HTTP 413, and `dist_reindex_journal` holds no failure for them.
- **AC-002**: With the same data and `-Xmx4g`, the reindex thread does not terminate with an
  `OutOfMemoryError`.
- **AC-003**: With documents well under the byte threshold, batches are closed by the document
  count exactly as today (same number of requests for the same input).
- **AC-004**: A single document larger than the byte threshold (and under the server limit) is
  sent in a request of its own and indexed.
- **AC-005**: In Phase 1 the large documents reach the OpenSearch shadow index (no silent
  divergence); in Phase 2 they reach both engines.
- **AC-006**: The byte threshold honours the OpenSearch property when set; when unset it equals
  `REINDEX_THREAD_ELASTICSEARCH_BULK_SIZE`, and changing the OpenSearch property does not change
  the Elasticsearch processor's threshold.
- **Verification method**: Unit tests of `OSIndexBulkProcessor` with a fake client that records
  each request's operations and size: flush by count (AC-003), flush by bytes before
  overflowing, a single oversized operation alone (AC-004), configured threshold (AC-006).
  Integration test in `dotcms-integration` (named `*Test`, registered in a `MainSuite*`) that
  queues large-but-legal contentlets and asserts AC-001, run at Phase 1 and Phase 3 through the
  phased run recipe (AC-005). AC-002 is verified manually with the reproduction environment
  (memory behaviour is not deterministic enough for CI), and the result recorded on #37905.

## Assumptions

- The OpenSearch test container in `dotcms-integration` uses the default
  `http.max_content_length` (100 MB), so the integration test can reproduce the 413 before the
  fix.
- Character length of the serialized document is an acceptable proxy for request bytes, given a
  default threshold far below the server limit (decided; see Root-Cause Hypothesis).
- The OOM is dominated by building the whole bulk request in memory (the stack trace points
  there); bounding the request size bounds that peak. Mapping copies per document are not
  addressed here.
