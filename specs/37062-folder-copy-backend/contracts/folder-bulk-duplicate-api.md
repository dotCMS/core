# Contract: Content Drive bulk folder duplication API

The boundary between the two halves of #37062, pinned before either writes product copy. The server
half defines it; the client half (`specs/37062-folder-copy-frontend/spec.md`) consumes it. It shares
its outcome shape with bulk folder delete (#37063) and folder move (#37165), and differs from delete's
contract only where the operations genuinely differ; each difference is marked.

Every reason name below is final for this version. A reason not listed here MUST NOT appear during
implementation without coming back through this document.

---

## 1. Submit a duplication

`POST /api/v1/assets/folders/_bulkduplicate`, `Content-Type: application/json`

### Request

| Field | Type | Required | Description |
|---|---|---|---|
| `assetPaths` | array of string | yes | The selected folders, each a site-qualified folder path such as `//demo.dotcms.com/projects/alpha/`. Each names a folder **to duplicate**. There is no destination field: every duplicate lands in the parent its source already sits in. De-duplicated server-side before the run. |

Same field name and shape as bulk delete's request.

### `202 Accepted`

| Field | Type | Description |
|---|---|---|
| `jobId` | string | The run's identifier. |
| `statusUrl` | string | Ready-to-use status address; the caller never assembles it. |
| `submitted` | number | Distinct folders accepted into the run. Equals the final outcome's `total` by construction, and can be smaller than `assetPaths.length` after de-duplication. |

### Refusals, before any job exists

| Status | `errorCode` | `fieldName` | When |
|---|---|---|---|
| `400` | `EMPTY_SELECTION` | `assetPaths` | Missing or empty, or no submitted path is usable |
| `400` | `OVER_MAX_PATHS` | `assetPaths` | More distinct paths than the configured maximum (§5). The number may appear in `message`, but `message` is diagnostic text; the client's source for the number is the advertised configuration. |
| `403` | `NOT_ENTITLED` | none | Caller not entitled to the operation at all. Rights on an individual folder are **not** a submission refusal; see §4. |

**Differs from delete**: there is **no `409 OVERLAPPING_RUN`**. Duplication carries no overlap guard,
because two runs over the same folders cannot interfere: nothing is destroyed and the naming rule gives
each duplicate its own name. A client MUST NOT write copy for an overlap refusal.

---

## 2. Follow, cancel, read the outcome

Identical to delete, on the generic job API.

| Action | Address | Notes |
|---|---|---|
| Status and outcome | `GET /api/v1/jobs/{jobId}/status` | Outcome shape in §4 once terminal |
| Cancel | `POST /api/v1/jobs/{jobId}/cancel` | Takes effect between top-level folders, never mid-subtree |
| Monitor | `GET /api/v1/jobs/{jobId}/monitor` | Server-sent progress; §3 |

**How long it is kept.** A finished run's outcome stays readable from the status address
indefinitely: the job framework does not purge finished jobs. The durable notification (§6) stays
until the author dismisses it.

**Differs from delete**: a client has **no reason to read the active-runs listing**. Nothing is
marked while a duplication runs, so there is no in-flight state to restore after a reload.

---

## 3. Progress

Progress counts **completed top-level folders** and nothing finer. One large folder can hold the
number still for many minutes, so a client MUST render an indeterminate indicator, not a proportion.
The run stays alive through a heartbeat that never changes the progress value.

---

## 4. Terminal outcome

Read from the terminal job's result metadata. Keys identical to bulk upload and bulk delete.

| Key | Type | Meaning |
|---|---|---|
| `total` | number | Folders in the run |
| `processed` | number | Folders the run reached: every folder except those a cancellation left unreached. Includes folders skipped as `COVERED_BY_PARENT`, which were decided, not unreached |
| `successCount` | number | |
| `failedCount` | number | |
| `skippedCount` | number | Both kinds of skip |
| `results` | array | One record per folder, in submission order |
| `stoppedAt` | string, optional | Only on a cancelled run: the first submitted folder it never reached, so the remainder can be resubmitted deliberately |

### Per-folder record

| Field | Type | Description |
|---|---|---|
| `key` | string | The folder path exactly as submitted |
| `status` | `SUCCESS` \| `FAILED` \| `SKIPPED` | |
| `reason` | string, optional | Present on every `FAILED`; on `SKIPPED` only for `COVERED_BY_PARENT` |
| `message` | string, optional | Diagnostic, for logs. **Never displayed.** |

**Nothing is added to the shared shape.** A successful record does not carry the name the duplicate
was given. The naming rule is published instead: the duplicate takes the source's name followed by
`_copy`, and the suffix is appended again until the name is free, so a folder duplicated twice yields a
second duplicate carrying the suffix twice.

### Reasons

| Reason | `status` | Meaning here | New? |
|---|---|---|---|
| `PERMISSION_DENIED` | `FAILED` | No read rights on the selected folder | shipped |
| `PARENT_PERMISSION_DENIED` | `FAILED` | No rights to add to the folder's parent, where its duplicate would land | **new, this feature** |
| `PATH_NOT_FOUND` | `FAILED` | The path no longer resolves to a folder: gone, a file, or malformed | #37063 |
| `PROTECTED_FOLDER` | `FAILED` | The system folder or another folder the system refuses to act on | #37063 |
| `UNCLASSIFIED` | `FAILED` | Anything the server could not attribute, reported rather than guessed | shipped |
| `COVERED_BY_PARENT` | `SKIPPED` | Another selected folder contains this one, so duplicating that folder already carries it | #37063 |
| *(none)* | `SKIPPED` | The run was cancelled before reaching this folder | shipped convention |

**`COVERED_BY_PARENT` is shared, its meaning is not.** For delete it means the ancestor removed the
folder first; here the folder is still there, untouched and usable. Client copy MUST say the parent
*covers* it, never that it was removed. The value's own documentation in `BatchFailureReason` is
broadened when this feature lands so it describes both.

Values this operation never emits: `IN_USE`, which only delete produces, and the upload-only
`OVER_SIZE_LIMIT`, `DISALLOWED_FILE_TYPE`, `NAME_COLLISION`, `FOLDER_FILTER_MISMATCH` and
`STAGED_CONTENT_UNAVAILABLE`. A client MUST NOT write copy for them for this operation; an unknown
value falls back to the general message and still names the folder.

---

## 5. Configuration

| Property | Default | Purpose |
|---|---|---|
| `FOLDER_BULK_DUPLICATE_MAX_PATHS` | **50** | Maximum distinct `assetPaths` before `400 OVER_MAX_PATHS`. Same default as bulk delete's `FOLDER_BULK_DELETE_MAX_PATHS`, for the same reason: each accepted folder is one sequential blocking transaction of unbounded size. The cap bounds the selection, not the size of any one folder. |

Advertised in `GET /api/v1/appconfiguration`, read from the same constant the endpoint enforces with:

```json
{ "entity": { "config": { "folderBulkDuplicate": { "maxPaths": 50 } } } }
```

Absent on an instance older than this feature; a client then submits without a pre-check and relies on
the refusal.

---

## 6. Events

| Event | Audience | When | Meaning |
|---|---|---|---|
| `BULK_FOLDER_DUPLICATE_COMPLETED` | Submitter only, pushed | The run reaches any terminal state | The outcome in §4 is ready |
| Durable notification | Submitter only | Same | The same outcome, for an author who was away |
| `COPY_FOLDER` | Every author with read rights on the folder, **excluding the submitter** | Each duplicated folder commits | A duplicate now exists; a listing showing that parent MAY refresh |

**Differs from delete**: no "started" or "finished" busy announcement. A folder being duplicated
stays fully usable, so there is nothing to warn other authors about. `COPY_FOLDER` is the existing,
unchanged event and is a completion notice, never a busy signal.

---

## 7. What a duplicate holds

Stated here because the client's report and the author's expectations depend on it.

- **Everything in the folder**: child folders, file assets, pages, links, content of any other type,
  and archived items.
- **Each item keeps its state**: published stays published, a draft stays a draft, archived stays
  archived. The one exception is a link that is archived but still has a working version, which comes
  out un-archived.
- **Relationships point at the originals.** A duplicated item related to another item keeps pointing
  at the original, even when that other item was duplicated in the same run. One-to-one and
  one-to-many relationships are not carried.
- **Permissions**: the duplicate folder takes its source's permissions. Its contents are copied with
  system rights, so it can include content the submitter could not read.
