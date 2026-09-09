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

**One call.** The files, the target and the upload type travel together. The endpoint hands each
file to the product's file-staging API as it reads it, then creates the batch against what was
staged — the same shape content import already uses. The client never sees a staging reference.

Two kinds of part, matching how content import already shapes its own multipart submission
(`@FormDataParam("file")` plus a JSON `@FormDataParam("form")`):

| Part | Type | Required | Description |
|---|---|---|---|
| `files` | file | yes | **Repeated** — one part per file, in the order the author chose them. That order is preserved in the outcome. |
| `form` | JSON | yes | The batch parameters, below. Sent as `application/json` |

```json
{
  "baseType": "DOTASSET",
  "folderId": "abc-123",
  "totalSizeBytes": 812345
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `baseType` | string | yes | `DOTASSET` or `FILEASSET`. One type for the whole batch. Any other value is `400`. |
| `folderId` | string | conditional | Target folder identifier. **Exactly one** of `folderId` / `siteId`. |
| `siteId` | string | conditional | Target site, for an upload at the site root. |
| `totalSizeBytes` | number | no | A declared total, used only for a **fast refusal before reading** — a convenience so an author is not made to upload gigabytes before being told no. **Never the enforcement point** (FR-013c): a caller can under-declare or omit it, and the authoritative total is accumulated while the body is read. |

> **Why two target fields and not one.** The workflow API this feature fires already separates
> `contentHost` (site id) from `hostFolder` (folder id), and carries explicit disambiguation
> messaging because callers confuse them. Content Drive currently sends a site id *in* `hostFolder`
> at the root, which is the overloading ADR-0020 deprecates. Two fields state the intent.

**Why one call rather than staging first and submitting references.** Content import already works
this way — multipart in, staging API called underneath, reference stored in the job. Two further
reasons decided it, both recorded in spec §Decisions Q5: the staging clock starts on the server
immediately before the batch is created, so nothing can expire between upload and submit; and the
ceilings below can abort the request *while it is read*, which is the only way to bound what
actually reaches disk.

**What the client gives up**, stated so it is a decision and not a surprise: per-file upload
progress and retry of a single failed file. A dropped connection means resending the batch. See
spec C-001a.

### Limits, and where they bite

Nothing below this endpoint provides a ceiling — the staging layer caps neither file count nor
request size, and the container is configured with no multipart limit. **This endpoint is the only
bound in the path**, so it enforces while reading and aborts the moment a ceiling is crossed.
Anything staged before the abort is reclaimed (spec FR-010a, FR-013d).

### Request-level controls

This endpoint accepts file content without routing the author through the staging layer's own REST
resource, so two controls that live on that resource are **not** inherited and are applied here
directly (spec FR-003a):

| Control | Behaviour |
|---|---|
| Same-origin check | A request whose `Origin` / `Referer` does not resolve to a host this instance serves is `400` |
| Staging enabled switch | Where an operator has disabled the staging layer, this endpoint refuses too rather than writing staged content behind the switch |

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
| `400` | no `files` parts; malformed or missing `form`; `baseType` neither `DOTASSET` nor `FILEASSET`; neither or both of `folderId`/`siteId`; more files than the configured maximum; request not same-origin; staging layer disabled |
| `413` | the batch is over the configured total size — either the declared `totalSizeBytes` (refused before reading) or the size accumulated **while reading**, which aborts the read there and reclaims what was already staged |
| `403` | the author may not add children to the target |
| `404` | the target folder or site does not exist |

**Every refusal is immediate**, before any batch exists. The checks that need no body — origin,
`form`, target, permission, and a declared `totalSizeBytes` — are decided before a byte is read. The
file count and the real total are decided **while reading**, and a `413` raised mid-read aborts
there rather than after the whole body has arrived, so no submission can commit more than the
ceiling to disk.

Content staged before a refusal aborts the read **is reclaimed** by this feature (FR-013d.1). It
created that content, so it owns cleaning it up — a refused submission never becomes a run, so
nothing else ever would.

The same holds if **the client abandons the upload** — navigating away, closing the tab, or losing
the connection. The parts already staged are reclaimed (FR-013d.2), so an abandoned submission
costs the author nothing and leaves nothing behind. What it does not do is leave a batch: the
handle is what starts every guarantee in this contract, and an abandoned upload never reaches
one.

### Resubmitting after a lost connection

**Safe for `FILEASSET`.** A resubmission never silently produces a second copy (FR-040): the
collision branch rejects the already-created files, and the outcome is flagged so the client can
tell a duplicate resubmission from a batch whose files genuinely all collided (FR-040a) — see
`duplicateSubmission` in §3.

> **Not safe for `DOTASSET`** *(amended 2026-09-09, FR-040b)*. A retry creates **a second copy of
> every file** and reports a clean `successCount: N`. Nothing in the outcome says so, and
> `duplicateSubmission` is `false` whenever the file order differed, because the fingerprint hashes
> the files in the order they were sent.
>
> This is accepted rather than fixed. A dotAsset's identifier takes a per-contentlet
> `asset_name`, so the unique index the collision branch relies on can never contend for one, and
> there is no content-hash rule in the product to fall back on. Refusing duplicates here would make
> this endpoint stricter than every other path that creates a dotAsset.
>
> **What this means for a client**: do not offer "safe to retry" unconditionally. On a `DOTASSET`
> batch whose response was lost, the honest options are to ask the author, or to let them see the
> folder before retrying — not to retry silently.

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
  "processed": 49,
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
| `processed` | Files **attempted**, across all attempts (FR-038). Skipped files were never attempted, so `processed` is `successCount + failedCount` and not `total` — in the example above, 49 of 50. |
| `skippedCount` | Files never attempted, because the run was cancelled before reaching them. Distinct from failed (FR-028). |
| `duplicateSubmission` | `true` when this run was a resubmission of a batch that had already succeeded (FR-040a). Lets the client report "already uploaded" instead of "everything failed". |
| `results[].key` | The file name. Generic on purpose — #37062 / #37063 put a folder path here (FR-018a). |
| `results[].status` | `SUCCESS` · `FAILED` · `SKIPPED` |
| `results[].reason` | Present only on `FAILED`. See the table below. **This is what the client presents**, mapped to product copy. |
| `results[].message` | Diagnostic, for logs. **Never displayed to the author** (FR-016a, frontend FR-030). |

### Failure reasons

Stable set. Every one needs client copy; adding one later is a change to both halves (FR-016a).

> **Changed 2026-09-08**: `FOLDER_FILTER_MISMATCH` added, taking the set from six to seven. Found
> in manual testing — a folder filter mismatch was being reported as `NAME_COLLISION`, which told
> the author to rename a file whose name was never the problem. **The client needs copy for it
> before this ships.**

| `reason` | Meaning |
|---|---|
| `OVER_SIZE_LIMIT` | Larger than the ceiling that applies — the content type's own, or the configured fallback where it declares none (FR-011) |
| `DISALLOWED_FILE_TYPE` | The resolved media type is not in the content type's allow list. **A media-type rule, not an extension rule** (FR-012a): the server sniffs content, so renaming a file does not get past it. A file whose media type cannot be resolved skips the check and is accepted (FR-012b) |
| `NAME_COLLISION` | A file of that name already exists in the target. **Case-insensitive** — `Report.pdf` and `report.pdf` are one name (FR-042a) |
| `FOLDER_FILTER_MISMATCH` | The target folder's own filename filter (`filesMasks`, e.g. `*.jpg`) does not admit this name. **Distinct from `DISALLOWED_FILE_TYPE`**: that is the content type's media-type allow list, decided by sniffing content, while this is a glob on the *file name* configured per folder — so the same file is refused here and accepted one folder over. The two need different copy, because renaming does not get past a media-type rule and moving does not get past a folder one |
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
| `CONTENT_BULK_UPLOAD_MAX_FILES` | `100` | Files per batch (FR-010) |
| `CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES` | `1073741824` (1 GB) | Summed size per batch (FR-013b) |
| `CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES` | `209715200` (200 MB) | Per-file ceiling **only where the content type declares none** (FR-011.2) |

**Two of these are readable by the client** *(added 2026-09-09)*. `GET /api/v1/appconfiguration`
— which the admin client already fetches on startup — carries them under `bulkUpload`:

```json
"bulkUpload": { "maxFiles": 100, "maxTotalBytes": 1073741824 }
```

Served from the same constants the endpoint enforces, so the advertised ceiling and the enforced one
cannot drift. Two things this is for: a refusal can **name the number** rather than say "fewer", and
the client can apply the §1 courtesy check **in the file chooser** instead of after uploading the
bytes. The fallback per-file ceiling is deliberately not exposed — it is resolved per content type
at run time, so a single advertised value would be wrong as often as right.

**Where the defaults come from.** The realistic case is an author dragging in up to 100 images or
PDFs of a few megabytes each — roughly 500 MB — so 1 GB clears it with room and still bounds the
pathological batch. The two ceilings are chosen together: raising the file count much further would
put a realistic batch past the *total*, which would make the count cap decorative and the size cap
the one that surprises people. Under the one-call shape the count also governs how long a single
request stays open, so it is not a free knob. The 200 MB per-file fallback is deliberately generous: it is the ceiling that
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
| Same origin; staging layer enabled | submission, before reading the body | `400` |
| `baseType`, target, exactly one of `folderId`/`siteId` | submission, before reading the body | `400` / `404` |
| Declared `totalSizeBytes` vs the batch ceiling | submission, before reading the body | `413` — a courtesy refusal only; omitting it skips this row |
| Add-children permission on the target | submission, before reading the body | `403` |
| File count vs the configured maximum | submission, **while reading** | `400`, read aborted, staged parts reclaimed |
| Accumulated size vs the batch ceiling | submission, **while reading** | `413`, read aborted, staged parts reclaimed |
| Content type's size ceiling, or the fallback | the run, per file | `FAILED` / `OVER_SIZE_LIMIT` — batch continues |
| Content type's allowed media types | the run, per file | `FAILED` / `DISALLOWED_FILE_TYPE` — batch continues |
| Name collision in the target | the run, per file | `FAILED` / `NAME_COLLISION` — batch continues |
| Target folder's `filesMasks` filter | the run, per file | `FAILED` / `FOLDER_FILTER_MISMATCH` — batch continues |

The content-type rules stay per-file rather than becoming submission refusals even though staging
already reported the size and media type: FR-011 and FR-012 require one bad file to fail on its own
rather than take the batch with it. What staging's data buys is a **reliable reason**, not an
earlier refusal.
