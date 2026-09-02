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
| `requestFingerPrint` | string | Required alongside the staging ids to retrieve staged content |
| `stagedFiles` | array | One entry per submitted file, in submission order |
| `stagedFiles[].fileName` | string | The name the asset takes; also the outcome's `key` |
| `stagedFiles[].tempFileId` | string | Staging handle, resolved by `TempFileAPI` |
| `stagedFiles[].sizeBytes` | number | Measured while staging, not as declared by the caller (FR-013) |
| `submissionFingerprint` | string | Stable hash over `userId` + target + the ordered `(fileName, sizeBytes)` list. Recognises a resubmission of the same batch (FR-040) |

**Why `stagedFiles` is a list of ids, not content**: the run executes later and possibly on another
node, so the bytes wait on the shared assets volume and the job carries only handles. This follows
`ImportContentletsProcessor`, which stores `tempFileId` + `requestFingerPrint` the same way.

---

## 2. Batch file checkpoint — NEW table

The one piece of new storage. Exists because the job framework persists no mid-run per-item state
(research.md R1): `parameters` is immutable, `progress` is a single float, and `result` is harvested
only at the terminal state. Without this, a re-queued run would restart from the first file and
duplicate everything it had already created.

```sql
CREATE TABLE bulk_upload_file_result (
    job_id      VARCHAR(255) NOT NULL,
    file_name   VARCHAR(510) NOT NULL,
    seq         INTEGER      NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    reason      VARCHAR(64),
    message     TEXT,
    identifier  VARCHAR(36),
    updated_at  timestamptz  NOT NULL,
    PRIMARY KEY (job_id, file_name)
);

CREATE INDEX idx_bulk_upload_file_result_job ON bulk_upload_file_result (job_id, seq);
```

| Column | Purpose |
|---|---|
| `job_id` + `file_name` | Primary key. A file appears once per batch, so a resumed run's write is naturally idempotent |
| `seq` | Submission order, so the outcome lists results in the order the author chose the files |
| `status` | `SUCCESS` · `FAILED` · `SKIPPED` — the shared `BatchItemStatus` |
| `reason` | The machine-readable failure reason (FR-016). Null unless `FAILED` |
| `message` | Diagnostic only; never displayed (FR-016a) |
| `identifier` | The created contentlet, on `SUCCESS`. Makes a resumed run's "already created" answer verifiable rather than inferred |
| `updated_at` | Diagnostics |

**Lifecycle**: one row written as each file completes, inside that file's own transaction so the
record commits with the work it describes. On start, the run reads the rows for its `job_id` and
skips every `SUCCESS`. At the terminal state the rows are read once to build the outcome (§3) — so
the same rows serve resumability *and* per-file reporting, rather than the outcome being
accumulated in memory and lost on interruption.

**Retention**: rows are deleted when the job row is purged, on the same schedule. They are not a
second audit log.

**Migration**: additive only — a new table, no changes to existing ones. An older build ignores a
table it does not know, so this stays rollback-safe under
[ROLLBACK_UNSAFE_CATEGORIES](../../docs/core/ROLLBACK_UNSAFE_CATEGORIES.md).

---

## 3. Batch outcome — the shared shape

Built from §2 at the terminal state and returned by `getResultMetadata(Job)`. **Generalizes what
bulk refresh already ships** (FR-018) rather than defining a second shape; #37062 / #37063 consume
it for folder paths.

| Field | Type | Notes |
|---|---|---|
| `total` | int | Files submitted |
| `processed` | int | Attempted across all attempts (FR-038) |
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
| At least one file | submission | `400` |
| `baseType` ∈ {`DOTASSET`, `FILEASSET`} | submission | `400` (FR-003) |
| Exactly one of `folderId` / `siteId` | submission | `400` — see contracts, ADR-0020 |
| Target exists | submission | `404` |
| Author may add children to target | submission, batch `filterCollection` | `403` (FR-003, FR-004) |
| File count ≤ `CONTENT_BULK_UPLOAD_MAX_FILES` | submission | `400` (FR-010) |
| Declared total ≤ ceiling | submission, if declared | `400` (FR-013c.1) |
| Actual total ≤ ceiling | while reading | `413`, staged content reclaimed (FR-013c.2, FR-013d) |
| Per-file size ≤ content type's ceiling, else the fallback | per file, pre-check | `FAILED` / `OVER_SIZE_LIMIT` (FR-011) |
| Media type in the content type's allow list | per file, pre-check | `FAILED` / `DISALLOWED_FILE_TYPE` (FR-012) |
| Name not already taken (case-insensitive) | per file, pre-check **and** unique-index violation | `FAILED` / `NAME_COLLISION` (FR-041, FR-042) |
| Staged content retrievable | per file | `FAILED` / `STAGED_CONTENT_UNAVAILABLE` (FR-032) |

The per-file pre-checks name the failure; they never replace the create-time validation, which
still runs and still governs (FR-006).

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
