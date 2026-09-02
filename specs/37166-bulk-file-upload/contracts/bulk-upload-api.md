# Contract: Content Drive bulk file upload API

**Feature**: [#37166](https://github.com/dotCMS/core/issues/37166) · backend
**Status**: agreed at planning. **Both halves of #37166 reference this file** — the frontend spec's
§Contract Consumed records the submission format as unsettled at spec altitude and requires one
written definition. This is it.

Field names below are binding. Changing one is a change to both halves.

---

## 1. Submit a batch

```
POST /api/v1/assets/_bulkupload
Content-Type: multipart/form-data
```

One call carrying the file content. The client never stages content itself and never handles a
staging identifier (spec §Decisions Q5).

### Multipart parts

| Part | Repeats | Required | Description |
|---|---|---|---|
| `files` | yes, once per file | yes | The binary content. Repeat the part for each file; its `filename` is the name the asset takes. |
| `form` | no | yes | A JSON string with the batch parameters below. |

`form` follows the `_import` precedent (`ContentImportParams`), where the binary rides in its own
part and everything else arrives as one JSON string.

### `form` fields

| Field | Type | Required | Description |
|---|---|---|---|
| `baseType` | string | yes | `DOTASSET` or `FILEASSET`. One type for the whole batch. Any other value is `400`. |
| `folderId` | string | conditional | Target folder identifier. **Exactly one** of `folderId` / `siteId` must be present. |
| `siteId` | string | conditional | Target site, for an upload at the site root. |
| `totalSizeBytes` | number | no | The client's declared total. Enables the fast refusal below; it is **not** the enforcement point. |

> **Why two target fields and not one.** The single-file upload sends one `hostFolder` carrying
> either a folder or a site id. ADR-0020 deprecated an endpoint for exactly that — one string
> encoding several concerns — so this endpoint asks which one you mean. See plan.md §ADR Alignment.

### Responses

**`202 Accepted`** — the batch is queued. The work has not been done.

```json
{
  "entity": {
    "jobId": "e6d9bae8-657b-4e2f-8524-c0222db66355",
    "statusUrl": "/api/v1/jobs/e6d9bae8-657b-4e2f-8524-c0222db66355/status"
  }
}
```

| Status | When |
|---|---|
| `400` | no files; `baseType` neither `DOTASSET` nor `FILEASSET`; neither or both of `folderId`/`siteId`; more than the configured file count; declared total over the configured batch ceiling |
| `403` | the author may not add children to the target |
| `404` | the target folder or site does not exist |
| `413` | the batch exceeded the total ceiling while being read (see below) |

**On `413`**: the total-size ceiling can only be enforced authoritatively while the content is read
(spec FR-013c.2), so a caller that under-declares or omits `totalSizeBytes` is refused part-way
through the upload rather than up front. Content staged before the refusal is reclaimed (FR-013d).
Declaring `totalSizeBytes` turns this into an immediate `400`, which is why the client should send
it — it is a courtesy to the author, not a limit the client enforces.

**No batch is created for any refusal.** A `400`/`403`/`404`/`413` never leaves a job behind.

### Resubmitting after a lost connection

Safe. A resubmission of the same batch never silently produces a second copy (FR-040). Where the
implementation resolves it by the collision branch, the outcome is flagged so the client can tell a
duplicate resubmission from a batch whose files genuinely all collided (FR-040a) — see
`duplicateSubmission` in §3.

---

## 2. Follow, cancel

Existing job-framework endpoints. This feature adds none.

| Purpose | Call |
|---|---|
| Status and progress | `GET /api/v1/jobs/{jobId}/status` |
| Live monitoring | `GET /api/v1/jobs/{jobId}/monitor` (SSE) |
| Cancel | `POST /api/v1/jobs/{jobId}/cancel` |

Queue name: `assetBulkUpload`. The client does not need it — `statusUrl` is absolute enough to
follow — and should not hardcode it.

---

## 3. The outcome

Read from the job's `result`, and carried in the completion event (§4). This is the **shared batch
outcome shape** (FR-018): the same shape bulk refresh emits, generalized so a batch of folder paths
reads the same as a batch of files, for #37062 / #37063 to adopt unchanged.

```json
{
  "total": 50,
  "processed": 50,
  "successCount": 47,
  "failedCount": 2,
  "skippedCount": 1,
  "duplicateSubmission": false,
  "results": [
    { "key": "brochure.pdf",  "status": "SUCCESS" },
    { "key": "huge.mov",      "status": "FAILED",  "reason": "OVER_SIZE_LIMIT",
      "message": "File exceeds the maximum size of 50 MB" },
    { "key": "notes.exe",     "status": "FAILED",  "reason": "DISALLOWED_FILE_TYPE",
      "message": "File type application/x-msdownload is not allowed" },
    { "key": "leftover.png",  "status": "SKIPPED" }
  ]
}
```

### Fields

| Field | Notes |
|---|---|
| `successCount` / `failedCount` / `skippedCount` | **`failedCount`, not `failCount`** — matches what bulk refresh already ships (FR-018). These counts are authoritative; never substitute the number of files submitted (FR-017). |
| `skippedCount` | Files never attempted, because the run was cancelled before reaching them. Distinct from failed (FR-028). |
| `duplicateSubmission` | `true` when this run was a resubmission of a batch that had already succeeded (FR-040a). Lets the client report "already uploaded" instead of "everything failed". |
| `results[].key` | The file name. Generic on purpose — #37062 / #37063 put a folder path here (FR-018a). |
| `results[].status` | `SUCCESS` · `FAILED` · `SKIPPED` |
| `results[].reason` | Present only on `FAILED`. See the table below. **This is what the client presents**, mapped to product copy. |
| `results[].message` | Diagnostic, for logs. **Never displayed to the author** (FR-016a, frontend FR-030). |

### Failure reasons

Stable set. Every one needs client copy; adding one later is a change to both halves (FR-016a).

| `reason` | Meaning |
|---|---|
| `OVER_SIZE_LIMIT` | Larger than the ceiling that applies — the content type's own, or the configured fallback where it declares none (FR-011) |
| `DISALLOWED_FILE_TYPE` | The resolved media type is not in the content type's allow list. **A media-type rule, not an extension rule** (FR-012a): the server sniffs content, so renaming a file does not get past it. A file whose media type cannot be resolved skips the check and is accepted (FR-012b) |
| `NAME_COLLISION` | A file of that name already exists in the target. **Case-insensitive** — `Report.pdf` and `report.pdf` are one name (FR-042a) |
| `PERMISSION_DENIED` | A per-file check narrower than the submission-time one that already passed |
| `STAGED_CONTENT_UNAVAILABLE` | The uploaded content could not be retrieved when the run reached it (FR-032). **Not the author's fault** — the copy should not suggest they supplied a bad file |
| `UNCLASSIFIED` | Anything else; `message` carries the detail for logs |

---

## 4. Completion notification

Pushed to the submitting author over the websocket the admin UI already holds, and recorded as a
durable notification so the outcome survives navigating away (FR-019 … FR-023). Follows
`BulkRefreshCompletionListener` (#37131) one-for-one.

- **Event**: `BULK_UPLOAD_COMPLETED`, `Visibility.USER`, addressed to the submitter only (FR-021)
- **Payload**: the §3 outcome — **counts _and_ `results`**, not counts alone. The frontend needs the
  failing file names to tell the author which files to choose again (spec C-006)
- **Fires on any terminal state** — completed, cancelled, or permanently failed (FR-019) — and
  **once per batch**, even across an interruption and resume (FR-039)
- **Best-effort**: a failed notification is logged and does not affect the recorded outcome (FR-023)

---

## Configuration

| Property | Default | Governs |
|---|---|---|
| `CONTENT_BULK_UPLOAD_MAX_FILES` | `50` | Files per batch (FR-010) |
| `CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES` | see plan | Total size per batch (FR-013b) |
| `CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES` | see plan | Per-file ceiling **where the content type declares none** (FR-011.2) |

The per-file ceiling prefers the content type's own `maxFileLength` field variable wherever an
operator has set one, so a file is accepted or rejected identically alone or in a batch. The
fallback applies only where none is declared — a knowing exception to that equivalence, recorded as
FR-011a, and the reason a large file can be refused in a batch and accepted on its own.
