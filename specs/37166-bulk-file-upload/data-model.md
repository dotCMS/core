# Data Model: Content Drive bulk file upload — backend

**Feature**: `specs/37166-bulk-file-upload` · **Date**: 2026-09-02

Field-level ground truth for the entities in [spec.md](./spec.md) §Key Entities. The wire shapes
live in [contracts/bulk-upload-api.md](./contracts/bulk-upload-api.md); this file covers what is
stored and how it is keyed.

---

## 1. Upload batch — job parameters

Not a new entity. A batch is a row in the existing `job` table, queue `assetBulkUpload`, whose
`parameters` JSONB carries the batch. Written once at `createJob` and **immutable thereafter** —
which is why the checkpoint of §2 exists.

| Parameter | Type | Notes |
|---|---|---|
| `baseType` | string | `DOTASSET` or `FILEASSET`; validated at submission |
| `folderId` | string, nullable | Exactly one of `folderId` / `siteId` is set |
| `siteId` | string, nullable | |
| `userId` | string | The submitter, so the run creates with their permissions and the completion is addressed to them (FR-021) |
| `requestFingerprint` | string | Captured at submission; required alongside the ids to retrieve staged content from a background thread, which has no HTTP request. Spelled to match the product's own `getRequestFingerprint` — the earlier `requestFingerPrint` was a typo in a binding field name |
| `stagedFiles` | array | One entry per referenced file, in submission order |
| `stagedFiles[].tempFileId` | string | The reference the endpoint got back from the staging API when it staged that part |
| `stagedFiles[].fileName` | string | As staging reported it; the name the asset takes, and the outcome's `key` |
| `stagedFiles[].sizeBytes` | number | As staging **measured** it, never as a caller declared it (FR-013) |
| `stagedFiles[].mimeType` | string | As staging **resolved** it. Lets the run name a type rejection as a fact rather than infer it from a validation exception |
| `submissionFingerprint` | string | Stable hash over `userId` + target + the ordered `(fileName, sizeBytes)` list. Recognises a resubmission of the same batch (FR-040) |

**Why the job carries references and not content**: the run executes later and possibly on another
node, so the bytes wait on the shared assets volume. **The endpoint places them there itself**, as
it reads the submission, by calling the staging API underneath (spec §Decisions Q5); the job then
stores only the handles — the same way content import does. The client never sees a handle.

**`sizeBytes` and `mimeType` are copied into the parameters at submission** rather than re-read
from staging when the run starts. Two reasons: the batch total (FR-013b) accumulates from them
while the request is read, before the batch exists, and if the content later expires the run still
knows what it was meant to be processing, so `STAGED_CONTENT_UNAVAILABLE` can name the file rather
than report an anonymous gap.

---

## 2. Job item result — NEW table, deliberately generic

The one piece of new storage. Exists because the job framework persists no mid-run per-item state
(research.md R1): `parameters` is immutable, `progress` is a single float, and `result` is harvested
only at the terminal state. Without it, a re-queued run would restart from the first file and
duplicate everything it had already created.

**Named for jobs, not for uploads, on purpose.** #37062 (folder copy, bulk delete) and #37063 need
exactly this — a durable per-item record for a batch job — and a table per bulk action would mean
four near-identical tables for upload, copy, delete and refresh. The shared outcome shape already
uses a generic `key` (FR-018a), so the storage matches it. This is the same concern as the ADR
proposed in plan.md: whether per-item durable state belongs in the framework at all. Generic naming
keeps that door open instead of nailing it shut with an upload-specific schema.

```sql
CREATE TABLE job_item_result (
    job_id      VARCHAR(255) NOT NULL,
    item_key    VARCHAR(510) NOT NULL,
    seq         INTEGER      NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    reason      VARCHAR(64),
    message     TEXT,
    ref_id      VARCHAR(36),
    updated_at  timestamptz  NOT NULL,
    PRIMARY KEY (job_id, seq)
);

CREATE INDEX idx_job_item_result_key ON job_item_result (job_id, item_key);
```

| Column | Purpose |
|---|---|
| `job_id` + `seq` | Primary key. `seq` is the submission index, which is unique within the batch **and stable across a resume**, so a resumed run's write is idempotent and the rows come back in the order the author chose the files. |
| `item_key` | The file name here, a folder path for #37062. **Deliberately not part of the key**: spec.md §Edge Cases allows two files with the same name in one batch, so keying on it would reject the second write and leave a resume unable to tell the two apart. Indexed, not unique. |
| `status` | `SUCCESS` · `FAILED` · `SKIPPED` — the shared `BatchItemStatus` |
| `reason` | The machine-readable failure reason (FR-016). Null unless `FAILED` |
| `message` | Diagnostic only; never displayed (FR-016a) |
| `ref_id` | What the item produced — the created contentlet's identifier here. Makes a resumed run's "already created" answer verifiable rather than inferred |
| `updated_at` | Diagnostics |

**Lifecycle**: one row written as each item completes, inside that item's own transaction so the
record commits with the work it describes. On start, the run reads the rows for its `job_id` and
skips every `seq` already recorded `SUCCESS` — **by `seq`, not by name**, which is what lets a batch
containing two files of the same name resume correctly. At the terminal state the rows are read once to build the outcome (§3) — so
the same rows serve resumability *and* per-item reporting, rather than the outcome being
accumulated in memory and lost on interruption.

**Retention — and a correction.** An earlier draft of this document said the rows are deleted "when
the job row is purged, on the same schedule". **There is no such schedule**: nothing purges the
`job` table, so job rows persist indefinitely today. That is pre-existing product behaviour, not
something this feature introduces, but it means retention has to be stated rather than inherited.

The rows are **not** deleted when the run finishes, and not conditionally on its outcome. They are
the per-item results the outcome reports and that FR-020 entitles the author to read after the
fact — deleting them on completion would delete exactly what has to remain readable. They live as
long as their job row.

Sizing, so this is a decision rather than an oversight: at 100 items per batch, a hundred batches a
day is 10,000 narrow rows a day. That is not a growth problem on its own, and it grows in step with
the `job` table it hangs off. **A retention policy for finished jobs and their items is worth its
own ticket** — it would cover bulk refresh and content import too, neither of which has one.

**Migration**: additive only — a new table, no changes to existing ones. An older build ignores a
table it does not know, so this stays rollback-safe under
[ROLLBACK_UNSAFE_CATEGORIES](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md).

**Note on bulk refresh**: it created no table of this kind. Its counters are in-memory
(`AtomicInteger` fields, a `CopyOnWriteArrayList`) and are flushed into the job's `result` only at
the terminal state — which is precisely why it is not resumable. This feature is the first to need
durable per-item state, not the second.

---

## 3. Batch outcome — the shared shape

Built from §2 at the terminal state and returned by `getResultMetadata(Job)`. **Generalizes what
bulk refresh already ships** (FR-018) rather than defining a second shape; #37062 / #37063 consume
it for folder paths.

| Field | Type | Notes |
|---|---|---|
| `total` | int | Files submitted |
| `processed` | int | Files **attempted**, across all attempts (FR-038). Skipped files were never attempted, so this is `successCount + failedCount` — it equals `total` only when nothing was skipped |
| `successCount` | int | |
| `failedCount` | int | **`failedCount`, not `failCount`** — matches what ships (FR-018) |
| `skippedCount` | int | Never attempted; a cancelled run's remainder (FR-028) |
| `duplicateSubmission` | boolean | This run was a resubmission of a batch that had already succeeded (FR-040a) |
| `results` | array | Per-item, in `seq` order |

### Per-item result — extracted shared type

`AbstractBatchItemResult` in `com.dotcms.jobs.business.batch`, a package neither feature owns.

| Field | Type | Notes |
|---|---|---|
| `key` | string | **Generic** (FR-018a): a file name here, a folder path for #37062 / #37063. The shipped bulk refresh record is keyed by contentlet identifier and inodes, which an uploading file does not have — it does not exist until the run creates it |
| `status` | enum | `SUCCESS` · `FAILED` · `SKIPPED` |
| `reason` | enum, nullable | `BatchFailureReason`; null unless `FAILED` |
| `message` | string, nullable | Diagnostic |

**Extraction, not modification** (research.md R3): `BulkRefreshItemResult` keeps its
`identifier()` / `inodes()` accessors and is expressed in terms of the shared type, so the shipped
consumer is untouched and SC-006 is verifiable by running its existing tests.

### `BatchFailureReason`

`OVER_SIZE_LIMIT` · `DISALLOWED_FILE_TYPE` · `NAME_COLLISION` · `PERMISSION_DENIED` ·
`STAGED_CONTENT_UNAVAILABLE` · `UNCLASSIFIED`

Derived by pre-check and by exception class, **never by parsing message text** (research.md R4) —
the size and type rejections are the same exception class differing only in a localized string, so
a text discriminator would break the first time a language key is edited.

---

## 4. Validation rules

Where each requirement is enforced, and what it produces.

| Rule | When | Outcome |
|---|---|---|
| At least one reference | submission | `400` |
| Every reference resolvable, and staged by this author | submission | `400` |
| `baseType` ∈ {`DOTASSET`, `FILEASSET`} | submission | `400` (FR-003) |
| Exactly one of `folderId` / `siteId` | submission | `400` — see contracts, ADR-0020 |
| Target exists | submission | `404` |
| Author may add children to target | submission, batch `filterCollection` | `403` (FR-003, FR-004) |
| Reference count ≤ `CONTENT_BULK_UPLOAD_MAX_FILES` | submission | `400` (FR-010) |
| Σ `sizeBytes` ≤ `CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES` | submission, a sum over measured sizes | `400` (FR-013b) |
| Per-file size ≤ content type's ceiling, else the fallback | per file, from `sizeBytes` | `FAILED` / `OVER_SIZE_LIMIT` (FR-011) |
| `mimeType` in the content type's allow list | per file, from `mimeType` | `FAILED` / `DISALLOWED_FILE_TYPE` (FR-012) |
| Name not already taken (case-insensitive) | per file, pre-check **and** unique-index violation | `FAILED` / `NAME_COLLISION` (FR-041, FR-042) |
| Staged content still retrievable | per file, at the run | `FAILED` / `STAGED_CONTENT_UNAVAILABLE` (FR-032) |

**Why the size and type rules stay per-file** even though both values are known at submission:
FR-011 and FR-012 require one bad file to fail on its own rather than take the batch with it. What
staging's measurements buy is a *reliable reason*, not an earlier refusal — see research.md R4,
where deriving these reasons from the validation layer was the harder half of the problem.

These checks name the failure; they never replace the create-time validation, which still runs and
still governs (FR-006). A file the run believes acceptable can still be rejected by validation, and
that rejection is reported like any other — it is simply the case the reasons above make rare.

---

## 5. State transitions

Batch states are the job framework's own — `PENDING` → `RUNNING` → `COMPLETED` / `FAILED` /
`CANCELLED`, with `CANCEL_REQUESTED` / `CANCELLING` in between.

Two transitions this feature must handle explicitly:

- **Interruption** — the abandoned-job sweep re-queues a stalled run to `PENDING` **without
  consulting the retry policy**, so it happens whether or not the processor is marked no-retry. The
  resumed run reads §2 and continues (FR-036). Notification fires once for the batch, not once per
  attempt (FR-039).
- **Cancellation** — honoured between files, never mid-file (FR-027). Files already created stay
  (FR-026); the remainder is written `SKIPPED` so the outcome distinguishes them from failures
  (FR-028).
