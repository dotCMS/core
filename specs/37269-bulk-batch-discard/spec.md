# Issue Resolution Specification: One document that cannot be serialized fails its whole reindex batch

**Feature Branch**: `37269-bulk-batch-discard`

**Created**: 2026-09-30

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37269](https://github.com/dotCMS/core/issues/37269) — "A client-side bulk failure discards the entire batch — inherited by OpenSearch in Phase 3"

**Input**: User description: "A client-side bulk failure discards the entire batch — inherited by OpenSearch in Phase 3. A document that cannot be serialized must fail on its own, attributed to the offending contentlet, in both the Elasticsearch and OpenSearch paths."

## Problem Statement *(mandatory)*

The reindex thread sends contentlets to the search engine in groups. When one document in a
group cannot be turned into a request — for example a body larger than the JSON parser allows —
the whole request is abandoned before the engine sees any of it, and **every contentlet in the
group is recorded as failed with that one document's error message**. Healthy content is left
out of the index because it happened to share a group with a bad document.

Measured on a real customer dataset (≈1.55 M contentlets, full reindex): **10** oversized
documents caused **380** reindex failures; **363** of them were contentlets under 100 KB with
nothing wrong with them. All 380 failure records carried the identical message, so the error
points at the wrong content and cannot be used to find the culprits.

The defect is not retired by the Elasticsearch → OpenSearch migration. In Phase 3 OpenSearch
becomes the primary engine and uses the same "whole group failed" handling for any error that
happens before the engine answers.

**Severity / Impact**: High. Content silently missing from the search index (and therefore from
site search, listings and anything that queries the index), scaling with the reindex batch size.
Any install with at least one very large text field is exposed during a full reindex. In
dual-write phases it also makes the two engines disagree, because today the trigger only fails
the Elasticsearch side.

## Reproduction *(mandatory)*

**Environment**: `dotcms/dotcms:trunk`, PostgreSQL 16, Elasticsearch-compatible engine
(OpenSearch 1.3.20) and OpenSearch 3.x. Reproduced in `PHASE_1_DUAL_WRITE_ES_READS`; the
Elasticsearch path behaves the same in Phase 0.

**Steps to Reproduce**:

1. Create a content type with an indexed text field (`WysiwygField`, `TextAreaField` and
   `StoryBlockField` all reproduce).
2. Give one contentlet a field value above the parser's string limit (a value over 20,000,000
   characters is deterministic; in practice ≈9.5 MB of body already trips it because the body is
   emitted several times into the index document). Production vector: HTML pasted from Google
   Docs carrying `data:image/...;base64` images.
3. Queue that contentlet for reindex together with several hundred healthy contentlets so they
   are picked up in the same reindex iteration.
4. Run a full reindex (the issue used `DOT_REINDEX_THREAD_ELASTICSEARCH_BULK_ACTIONS=1000`; the
   blast radius scales with batch size).
5. Inspect `dist_reindex_journal` and the new Elasticsearch index.

**Expected Behavior**: Only the oversized contentlet fails. Every healthy contentlet from the
same group is indexed. The single failure record names the offending contentlet and its real
problem (which field, how large).

**Actual Behavior**: The log shows `Bulk process failed entirely: String value length (20054016)
exceeds the maximum allowed (20000000, ...)`. Every contentlet of the group stays in
`dist_reindex_journal` with that same message, none of them reach the Elasticsearch index, and
the reported length is a parser buffer size, not the document's.

**Reproducibility**: Always, given a field value above the limit in the same reindex group as
other content. Setting `CONTENTLET_JSON_MAX_STRING_LENGTH_MB` has no effect.

## Scope of Investigation *(mandatory)*

- **Affected area**: Search indexing — the reindex queue consumer and the per-engine bulk write
  path, in every migration phase.
- **Suspected surface**: Mixed. The queue consumer and failure bookkeeping are modern
  (`com.dotcms.content.elasticsearch.business.ContentletIndexAPIImpl`,
  `ContentletIndexOperationsES`, `ContentletIndexOperationsOS`, `BulkProcessorListener`); the
  queue itself is legacy (`com.dotmarketing.common.reindex.ReindexThread`,
  `ReindexQueueFactory`). The fix is expected to stay in the modern classes.
- **Related known decisions**: OpenSearch content write failures are fire-and-forget in Phase 1,
  where OpenSearch is a shadow, and must propagate from Phase 2 on, once it serves reads (see
  `docs/backend/OPENSEARCH_MIGRATION.md`). The plan consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Three facts combine.

1. **Where the document is parsed differs by engine.** The Elasticsearch path stores each
   document as a raw string and the client (`elasticsearch-rest-high-level-client` 7.10.2,
   `RequestConverters.bulk()`) re-parses every document while building the single HTTP body.
   Jackson 2.17's default read limit (`StreamReadConstraints`, 20,000,000 characters per string)
   throws in the middle of that loop, so no request exists to send. The OpenSearch path parses
   each document when it is added to the group (`ContentletIndexOperationsOS.parseJsonToMap`,
   same default limit) inside `addIndexOpToProcessor`, before the operation is queued and long
   before `flush()` sends anything. The throw propagates out of `enqueueMappedDocuments` to the
   `catch` in `ContentletIndexAPIImpl.appendBulkRequestToProcessor` (reached via
   `appendToBulkProcessorEntry`), which marks only that entry failed. This is why OpenSearch
   received the collateral documents in the measured run: on the OpenSearch path a serialization
   error is already isolated per document.
2. **A whole-request exception is always treated as "the whole group failed".**
   `BulkProcessorListener.afterBulk(long, Throwable)` marks every record of the iteration's group
   failed with one message. That is correct for transport or access failures (engine down,
   timeout, credentials rejected — nothing was indexed), and wrong for an error caused by one
   document's content. On the OpenSearch path the same handler is reached from `flush()`'s
   `catch (Exception e)`, which covers errors raised by the `client.bulk()` send itself (not the
   add-time parse above) — transport errors, but also any per-document problem the client only
   detects while writing the request. In Phase 3 the primary listener is not a shadow, so the
   OpenSearch path inherits whole-group marking for those. The plan must confirm which
   per-document errors can still reach `flush()` on the OpenSearch path.
3. **One journal row covers several documents, and any success deletes it.** A reindex entry is
   per identifier, but `mapEntry` → `loadVersionInodes` turns it into one document per language
   and per distinct working/live version (`identifier_lang_variant`). On the way back,
   `BulkProcessorListener.afterBulk(long, List)` cuts each result id down to the identifier and
   `handleSuccess` deletes the row (`deleteReindexEntry`), while `markAsFailed` only `UPDATE`s it.
   So if one document of an identifier is rejected at add time but a sibling document of the same
   identifier was queued and indexed, the sibling's success deletes the failure record: the
   rejected document never reaches the index and nothing records it. `loadVersionInodes` uses a
   `HashMap`, so whether the sibling is queued first depends on iteration order. This already
   applies to the OpenSearch path today (it rejects at add time), and it rules out fixing the
   Elasticsearch path at its add site (`ContentletIndexOperationsES.addIndexOpToProcessor`).
   Raised in review by a teammate.

Where the check belongs follows from (3): **before any document of the entry is queued**, in the
mapping step (`mapEntry` / `mapContentletForProcessor`), on the final document `Map` produced by
`toMap()`. That map is computed once and shared by every engine ("compute mapping once; reuse
across all providers"), so one check protects the Elasticsearch and OpenSearch paths alike.
Checking string lengths on the map is cheap (no extra parse). It has to run on the final map
because `catchall` concatenates every field value into one string (`ESMappingAPIImpl`), so it is
the largest string in the document and can cross the limit while the authored body is only about
half of it — which is why a ≈9.5 MB body already fails. The limit is read from Jackson's
effective default (`StreamReadConstraints.defaults().getMaxStringLength()`, 20,000,000 in 2.17),
not hard-coded, so it tracks the value the client's parser will actually enforce.

The unit of damage is the group fetched from the queue in that iteration
(`REINDEX_RECORDS_TO_FETCH`), not only the engine request; records already confirmed indexed by
an earlier request are unaffected because their journal row is gone. This is deduced from the
code and must be confirmed by the reproduction test.

Open questions (to confirm in planning, not blocking the spec):

- Did the 10 oversized documents also fail on the OpenSearch side in the measured run? The
  parser limit there is the same, so they are expected to have failed individually.
- In Phase 1, an OpenSearch add-time parse failure propagates out of `enqueueMappedDocuments` to
  the `catch` in `appendBulkRequestToProcessor`, which appears to mark the entry failed even
  though the Elasticsearch operation (the primary) may already be queued (target order to confirm) — a shadow failure
  counted as a primary failure, contrary to the fire-and-forget rule. Confirm and, if true,
  include the correction in this fix since it lives on the same code path.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- A document that cannot be serialized for the engine fails **individually**; every other
  document in the same group is indexed. This holds for the Elasticsearch path (primary in
  Phases 0–1) and the OpenSearch path (primary in Phase 3).
- The unit that fails is the **document**, not the identifier. When one language or version of
  an identifier is rejected, that document is withheld and the identifier's other documents
  (other languages, the other of working/live) are indexed. The identifier's journal row keeps
  the failure: a sibling document's success in the same batch must not delete it. The result
  must not depend on the order in which an identifier's versions are loaded.
- The rejection happens before anything of the entry is queued, on the final document map, so it
  covers both engines from one place.
- The failure is attributed to the offending contentlet: identifier, inode, the field involved
  where it can be determined, and the value's real size — not the parser's buffer size — instead
  of every record in the group carrying the same message.
- Whole-group failure handling distinguishes an error caused by document content from a
  transport or access error. Transport and access errors keep today's behaviour (the group is
  marked failed and retried).
- In Phase 1, a serialization failure on the shadow engine does not mark the primary's entry
  failed (only if the open question above is confirmed).
- The failed-records download stays usable once oversized content is what fails. Found while
  verifying the fix by hand: `GET /api/v1/esindex/failed` embeds every failed record's full
  contentlet (all field values), so 12 failed records of oversized content made a 168 MB response
  and the Maintenance portlet's "Download Failed Records" button crashed the browser tab (out of
  memory). A new `GET /api/v1/index/failed` (vendor-neutral `index` family, same CMS
  Administrator + `maintenance` portlet access as today) returns each failed record with a typed
  response — identifier, inode, title, content type, language, failure reason, priority and
  operation — and **no content field values**. The button uses it. `GET /api/v1/esindex/failed`
  keeps its response exactly as today and is marked deprecated in favour of the new endpoint.

**Explicitly out of scope / non-goals**:

- Making the 20 MB parser limit configurable, or overriding `StreamReadConstraints` for the whole
  JVM. It only moves the threshold and loosens a protection that also guards API input.
- A queryable record of OpenSearch shadow-write failures in dual-write phases — tracked in
  #37271.
- Retrying failed records, or changing what "clear failed records" does — tracked in #37270.
- Preventing oversized content or `data:` URIs at save time — separate issue (authoring
  guard).
- Changing the index document shape (e.g. how often the body is emitted into the document).
- Reindex batch sizing defaults.
- A configuration switch to fail the whole identifier instead of the single document. Considered
  and rejected: the only motive is cross-language consistency, it doubles the test matrix, and it
  can be added later from the same single decision point if a customer needs it.

## Regression Risk *(mandatory)*

- **Blast radius**: Every contentlet write to the index goes through this path — single saves,
  publishes, workflow actions, and full reindex — in all four migration phases. A mistake in
  the content-vs-transport classification could either keep discarding healthy content
  (classification too narrow) or mark a genuinely unreachable engine's batch as successful /
  individually failed (too broad), exhausting retries during an outage. The size check is a
  walk over the document map's string values (no extra parse), so its cost is expected to be
  negligible. Keeping the journal row when a sibling succeeds means a retry re-sends the
  identifier's healthy documents too; index writes are idempotent per document id, so this is
  safe.
- **Backward compatibility**: No change to index mappings or the `dist_reindex_journal` schema.
  REST: one endpoint is added (`GET /api/v1/index/failed`); `GET /api/v1/esindex/failed` is only
  marked deprecated, its response is unchanged, so existing scripts and the migration runbook
  keep working. The text stored as a failure reason becomes per-document; any
  tooling that grouped failures by identical message will see distinct messages.
- **Data considerations**: Installs that already hit this have healthy content parked as failed
  in `dist_reindex_journal`. This fix does not requeue it; recovery is a reindex after the
  oversized content is corrected (see #37270 for the requeue gap).

## Acceptance & Verification *(mandatory)*

- **AC-001**: With one contentlet whose field exceeds the parser limit queued in the same group
  as N healthy contentlets, after the reindex iteration all N healthy contentlets are present in
  the primary engine's working and live indices, and `dist_reindex_journal` holds exactly one
  failure record: the oversized contentlet's.
- **AC-002**: That failure record identifies the oversized contentlet (identifier and inode) and
  states the real size of the offending value; it does not contain the parser buffer size.
- **AC-003**: AC-001 and AC-002 hold with Elasticsearch as primary (Phase 0) and with OpenSearch
  as primary (Phase 3).
- **AC-004**: In a dual-write phase (Phase 1), the healthy contentlets reach both engines, and a
  failure on the shadow engine alone does not leave a failure record for a contentlet the
  primary indexed.
- **AC-005**: An identifier with a healthy live version and an oversized working version, and an
  identifier with two languages of which one is oversized: after the iteration each identifier
  has exactly one failure record in `dist_reindex_journal`, its healthy documents are present in
  the primary engine's indices, and the oversized document is absent. The outcome is the same
  regardless of the order in which the versions are loaded (exercise both orders).
- **AC-006**: When the engine is unreachable or rejects the request as a whole (connection
  refused / timeout / authentication), every record of the group is still marked failed and
  retried as today.
- **AC-007**: With failed records for oversized content in `dist_reindex_journal`,
  `GET /api/v1/index/failed` returns one entry per failed record carrying identifier, inode,
  title, content type, language, failure reason, priority and operation, and no contentlet field
  values, so its size does not grow with the size of the content. A user without the CMS
  Administrator role gets 401/403 as with the old endpoint. `GET /api/v1/esindex/failed` returns
  the same response as before, documented as deprecated. The Maintenance portlet's "Download
  Failed Records" button calls the new endpoint.
- **Verification method**: A new integration test in `dotcms-integration` (named `*Test`,
  registered in a `MainSuite*` class) that builds the reproduction — one >20,000,000-character
  field plus N healthy contentlets fetched in the same iteration — and asserts AC-001/AC-002,
  plus the sibling scenarios of AC-005, parameterized or repeated for Phase 0, Phase 1 and
  Phase 3. It must fail (Red) on current code before the fix. AC-006 is covered by a unit test of
  the classification with a transport exception. AC-007 is covered by an API test
  (`dotcms-postman`) for the new endpoint's shape and access, plus a unit test that the response
  omits field values. The weekly OpenSearch Phase Sweep exercises the phase variants in CI.

## Assumptions

- The measured production run (Phase 1, OpenSearch 1.3.20 as the Elasticsearch-compatible
  engine plus OpenSearch 3.4.0) is representative of Phase 0 Elasticsearch behaviour, since the
  trigger lives in the client, not the server.
- A >20,000,000-character value is an acceptable deterministic stand-in for the ≈9.5 MB
  real-world threshold in tests.
- "Field involved" in AC-002 is best effort: when the failure cannot be traced to a single field
  the record still names the contentlet and the size of the largest string value.
