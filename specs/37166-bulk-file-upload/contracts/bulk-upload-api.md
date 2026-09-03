# Contract: Content Drive bulk file upload API

**Feature**: [#37166](https://github.com/dotCMS/core/issues/37166) · backend
**Status**: agreed at planning. **Both halves of #37166 reference this file** — the frontend spec's
§Contract Consumed records the submission format as unsettled at spec altitude and requires one
written definition. This is it.

Field names below are binding. Changing one is a change to both halves.

---

## 1. Stage the content

```
POST /api/v1/temp
Content-Type: multipart/form-data
```

The product's existing file-staging endpoint. **Not new, and not this feature's** — its own
documentation prescribes exactly this pattern: *"Use this endpoint to supply files for binary and
image fields. After uploading, pass `tempFiles[0].id` as the field value."*

Send all the files as parts of one request. Optional `?maxFileLength=` tightens the per-file
ceiling for that call.

```json
{
  "tempFiles": [
    { "id": "temp_5311313004", "fileName": "hero.jpg", "length": 84471,
      "mimeType": "image/jpeg", "referenceUrl": "/dA/temp_5311313004/tmp/hero.jpg" },
    { "id": "temp_5311313005", "fileName": "brochure.pdf", "length": 1249881,
      "mimeType": "application/pdf" }
  ]
}
```

The client keeps the `id` of each. `length` and `mimeType` are **measured and resolved by the
server**, which is why step 2 can validate size and type without reading anything.

**Why the client makes this call rather than the batch endpoint proxying it**: staging is the
product's designed intake for files and the right home for cross-cutting file concerns; a second
intake door would have to re-earn them. It also hands the client per-file upload progress and
per-file retry for free — see C-001a. Recorded as spec §Decisions Q5.

**Timing matters.** The staging layer expires content on a global timer (default 30 minutes) with
no per-call override. Stage close to submitting, not at file-selection time — references that have
expired are refused in step 2.

---

## 2. Submit a batch

```
POST /api/v1/assets/_bulkupload
Content-Type: application/json
```

No content, only references.

```json
{
  "baseType": "DOTASSET",
  "folderId": "abc-123",
  "tempFileIds": ["temp_5311313004", "temp_5311313005"]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `baseType` | string | yes | `DOTASSET` or `FILEASSET`. One type for the whole batch. Any other value is `400`. |
| `folderId` | string | conditional | Target folder identifier. **Exactly one** of `folderId` / `siteId`. |
| `siteId` | string | conditional | Target site, for an upload at the site root. |
| `tempFileIds` | string[] | yes | The ids from step 1, in the order the author chose the files. That order is preserved in the outcome. |

> **Why two target fields and not one.** The workflow API this feature fires already separates
> `contentHost` (site id) from `hostFolder` (folder id), and carries explicit disambiguation
> messaging because callers confuse them. Content Drive currently sends a site id *in* `hostFolder`
> at the root, which is the overloading ADR-0020 deprecates. Two fields state the intent.

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
| `400` | empty `tempFileIds`; `baseType` neither `DOTASSET` nor `FILEASSET`; neither or both of `folderId`/`siteId`; more ids than the configured file count; **summed size over the configured batch ceiling**; a reference that cannot be resolved or that the author did not stage |
| `403` | the author may not add children to the target |
| `404` | the target folder or site does not exist |

**Every refusal is immediate**, before any batch exists. The submission carries no content, so the
total-size check is a sum over what staging already measured — there is no partial upload to unwind
and no `413`.

Content staged for a submission that is then refused is left to the staging layer's own expiry.
This feature did not create it and does not reclaim it.

### Resubmitting after a lost connection

Safe. A resubmission of the same batch never silently produces a second copy (FR-040). Where the
implementation resolves it by the collision branch, the outcome is flagged so the client can tell a
duplicate resubmission from a batch whose files genuinely all collided (FR-040a) — see
`duplicateSubmission` in §4.

---

## 3. Follow, cancel

Existing job-framework endpoints. This feature adds none.

| Purpose | Call |
|---|---|
| Status and progress | `GET /api/v1/jobs/{jobId}/status` |
| Live monitoring | `GET /api/v1/jobs/{jobId}/monitor` (SSE) |
| Cancel | `POST /api/v1/jobs/{jobId}/cancel` |

Queue name: `assetBulkUpload`. The client does not need it — `statusUrl` is absolute enough to
follow — and should not hardcode it.

---

## 4. The outcome

Read from the job's `result`, and carried in the completion event (§5). This is the **shared batch
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

## 5. Completion notification

Pushed to the submitting author over the websocket the admin UI already holds, and recorded as a
durable notification so the outcome survives navigating away (FR-019 … FR-023). Follows
`BulkRefreshCompletionListener` (#37131) one-for-one.

- **Event**: `BULK_UPLOAD_COMPLETED`, `Visibility.USER`, addressed to the submitter only (FR-021)
- **Payload**: the §4 outcome — **counts _and_ `results`**, not counts alone. The frontend needs the
  failing file names to tell the author which files to choose again (spec C-006)
- **Fires on any terminal state** — completed, cancelled, or permanently failed (FR-019) — and
  **once per batch**, even across an interruption and resume (FR-039)
- **Best-effort**: a failed notification is logged and does not affect the recorded outcome (FR-023)

---

## Configuration

| Property | Default | Governs |
|---|---|---|
| `CONTENT_BULK_UPLOAD_MAX_FILES` | `50` | References per batch (FR-010) |
| `CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES` | `1073741824` (1 GB) | Summed size per batch (FR-013b) |
| `CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES` | `209715200` (200 MB) | Per-file ceiling **only where the content type declares none** (FR-011.2) |

**Where the defaults come from.** The realistic case is an author dragging in 50 images or PDFs of
a few megabytes each — roughly 250 MB — so 1 GB clears it with room and still bounds the
pathological batch. The 200 MB per-file fallback is deliberately generous: it is the ceiling that
makes a file fail in a batch and succeed on its own (FR-011a), so the less often it bites, the less
it surprises. In practice the batch total is the binding constraint. **These are reasoned defaults,
not measured ones — worth checking against what customers actually upload.**

The per-file ceiling prefers the content type's own `maxFileLength` field variable wherever an
operator has set one, so a file is accepted or rejected identically alone or in a batch. The
fallback applies only where none is declared — a knowing exception to that equivalence, recorded as
FR-011a.

---

## Where each check happens

Useful when reading the two halves side by side.

| Check | Where | On failure |
|---|---|---|
| Per-file size ceiling (`maxFileLength` query param, system config) | staging, step 1 | that file is not staged; the client sees it in step 1 |
| Reference resolvable, and staged by this author | submission, step 2 | `400` for the whole submission |
| `baseType`, target, exactly one of `folderId`/`siteId`, file count | submission, step 2 | `400` / `404` |
| Add-children permission on the target | submission, step 2 | `403` |
| Summed size vs the batch ceiling | submission, step 2 | `400` |
| Content type's size ceiling, or the fallback | the run, per file | `FAILED` / `OVER_SIZE_LIMIT` — batch continues |
| Content type's allowed media types | the run, per file | `FAILED` / `DISALLOWED_FILE_TYPE` — batch continues |
| Name collision in the target | the run, per file | `FAILED` / `NAME_COLLISION` — batch continues |

The content-type rules stay per-file rather than becoming submission refusals even though staging
already reported the size and media type: FR-011 and FR-012 require one bad file to fail on its own
rather than take the batch with it. What staging's data buys is a **reliable reason**, not an
earlier refusal.
