# Contract: Content Drive bulk folder delete API

**Feature**: [#37063](https://github.com/dotCMS/core/issues/37063) · backend

**Status**: agreed at planning. Restates spec.md §Contract Consumed by the Client (C-001 … C-012) as
a concrete wire shape. Field names below are binding — changing one is a change to both halves of
#37063, and to #37062/#37165, which share the outcome shape (D-002, D-012, FR-015).

---

## 1. Submit a bulk delete

```
POST /api/v1/assets/folders/_bulkdelete
Content-Type: application/json
```

**One call, one handle** (C-001). An ordinary JSON body — no multipart, no staging (D-004): this
operation sends paths, not content.

```json
{
  "assetPaths": [
    "//default/marketing/2024-campaigns",
    "//default/marketing/archive"
  ]
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `assetPaths` | array of string | yes | The selected folders, in the same site-qualified path form the shipped `_delete` endpoint accepts. Deduplicated server-side before the run (FR-012). |

**Response — `202 Accepted`**:

```json
{
  "jobId": "b3f1...",
  "statusUrl": "/api/v1/jobs/b3f1.../status",
  "submitted": 5
}
```

| Field | Type | Description |
|---|---|---|
| `jobId` | string | The run's identifier. |
| `statusUrl` | string | Ready-to-use address for status; the caller never assembles it (FR-002). Generic job address — see §2. |
| `submitted` | number | How many **distinct** paths the server accepted into the run — equals the total the final outcome reports, by construction (FR-003, C-003). After deduplication, so it can be smaller than `assetPaths.length`. |

**Submission refusals — distinguishable by `errorCode`, not by status alone**

Raised on 2026-09-17 during frontend implementation
([issue comment](https://github.com/dotCMS/core/issues/37063#issuecomment-5720585127)): status code
alone cannot separate the two `400` cases, and the client needs to render different copy for each.
Resolved using the REST layer's existing structured-error type,
[`com.dotcms.rest.ErrorEntity`](../../../dotCMS/src/main/java/com/dotcms/rest/ErrorEntity.java)
(`errorCode`, `message`, `fieldName`) — the same mechanism `ValidationException` already builds
its body from — rather than inventing a new ad-hoc shape. `errorCode` is the field to switch on;
`message` is human-readable but not guaranteed stable wording; `fieldName` names which submitted
field is at fault, consistently across every refusal below (never repurposed to carry a value).

**Corrected 2026-09-19 — the `ErrorEntity` is carried inside dotCMS's standard error envelope,
not bare.** An earlier draft of this contract showed the `ErrorEntity` at the response body's top
level. That is not this product's convention: `com.dotcms.rest.ResponseEntityView`, the wrapper
every successful response already uses (via its own `entity` field), also carries an `errors`
array for exactly this case — `ExceptionMapperUtil.createResponse(Status,
DotContentletValidationException)` already builds an error response this same way. So the body is
`{"errors": [{...}], ...}`, one `ErrorEntity` in a one-element array — confirmed against the
frontend half (PR dotCMS/core#37612), which had independently assumed this envelope. Read the
first (only) element of `errors`.

| Status | `errorCode` | `fieldName` | `message` (example) | When |
|---|---|---|---|---|
| `400` | `EMPTY_SELECTION` | `assetPaths` | "no folder paths were submitted" | `assetPaths` missing or empty (FR-004) |
| `400` | `OVER_MAX_PATHS` | `assetPaths` | "selection exceeds the maximum of 50 paths" | `assetPaths` longer than the configured maximum (FR-004; see §5). The ceiling is stated in `message` text as a convenience only — the client's authoritative source is the configuration endpoint (§5, FR-038), so this is never the only place it can be read from. |
| `400` | `EMPTY_SELECTION` | `assetPaths` | "no submitted path resolved to anything usable" | Every path fails submission-time validation — still a `400`, not an accepted job that fails immediately (spec edge case) |
| `403` | `NOT_ENTITLED` | — | "not entitled to bulk-delete folders" | Caller not entitled to the operation at all (FR-005). Per-path permission is not a submission refusal — see §4. |
| `409` | `OVERLAPPING_RUN` | `assetPaths` | "another deletion is already running for `//default/marketing/2024-campaigns`" | A submitted path is the same as, an ancestor of, or a descendant of a path an in-flight run (in this queue, any submitter) is already covering (FR-029, FR-029a). **Names the conflicting folder, never the other submitter** — there is no field in this body where a user id could appear, by construction, which is the direct answer to whether the payload could leak one. Refused before a job is created (FR-029, FR-029b — see plan.md PO-6 for how the check-then-act race is closed). |

```json
{
  "errors": [
    { "errorCode": "OVER_MAX_PATHS", "message": "selection exceeds the maximum of 50 paths", "fieldName": "assetPaths" }
  ],
  "entity": "",
  "messages": [],
  "permissions": []
}
```

---

## 2. Follow, cancel, and read the outcome

**Not a new set of addresses, including for the active listing.** Once accepted, the caller uses
the **generic job-queue** addresses (D-003) — status, cancel, monitor, and active — the same
shapes every job-queue consumer (content import, bulk upload, bulk refresh) already exposes. This
feature adds **no** parallel endpoints under the assets API for any of these.

**Corrected 2026-09-19** — an earlier draft of this contract proposed a domain-scoped
`GET /v1/assets/folders/_bulkdelete/active`, reasoning from content import's own `/active`
endpoint (D-003). On inspection, content import's version exists to **transform** the generic
result into a domain-specific view (`JobViewPaginatedResult`, via `importHelper.view(result)`) —
it is not a bare passthrough. This feature has no such transformation need: the generic
`JobPaginatedResult`'s `jobs[].id` / `jobs[].state` / `jobs[].parameters` already carries
everything a client needs, field-for-field. Building a domain-scoped endpoint here would be
unjustified duplication. Confirmed against the frontend half
([PR #37612](https://github.com/dotCMS/core/pull/37612)), which was already written directly
against the generic shape.

| Action | Address (from `statusUrl` / the generic job API) | Notes |
|---|---|---|
| Status / outcome | `GET /api/v1/jobs/{jobId}/status` | See §4 for the outcome shape once terminal |
| Cancel | `POST /api/v1/jobs/{jobId}/cancel` | Takes effect between top-level folders, never mid-subtree (FR-026) |
| Monitor (live updates) | `GET /api/v1/jobs/{jobId}/monitor` | Server-sent progress; see §3 |
| List active runs | `GET /api/v1/jobs/folderBulkDelete/active` | The generic per-queue active listing (`JobQueueResource`), not a domain-scoped one. **Not filtered by submitter** (FR-005a) — any back-end user can read it. **Includes non-terminal states beyond "running"** — see the client-side warning in spec C-011; the same caveat applies here. Each job's `parameters` carries this run's submitted paths — see the shape note below. |

**Parameter shape inside each listed job — read this before assuming `assetPaths`.** The generic
listing exposes each run's `parameters` exactly as `FolderBulkDeleteHelper` stored them at
`createJob` time: `{"userId": "...", "paths": [{"path": "//site/folder/"}, ...]}` — an array of
objects under `paths`, each carrying its own `path` key, **not** a flat `assetPaths: string[]`.
This is deliberate: `paths[].path` is where this feature's per-folder job parameters live going
forward (a natural place to add more per-path fields later, e.g. `folderIdentifier`, without a
breaking shape change — see PO-2). A client reading the active listing to mark busy folders must
map `job.parameters.paths.map(p => p.path)`, not read `job.parameters.assetPaths` directly.

---

## 3. Progress while in flight

Reported **only when it changes**, at the **only granularity that exists**: completed top-level
folders out of the accepted total (FR-024, FR-025). Nothing inside a folder is observable (D-013/
FR-020). Clients SHOULD render an **indeterminate** indicator, not a determinate bar (C-006) — the
contract makes no claim about how long any one folder takes.

```json
{ "progress": 0.6 }
```

`0.6` here means 3 of 5 accepted folders are done — never a fraction of one folder's contents.

---

## 4. Terminal outcome

Read via `GET /api/v1/jobs/{jobId}/status` once the job reaches a terminal state, or received via
the pushed completion signal (§6).

```json
{
  "total": 5,
  "processed": 5,
  "successCount": 3,
  "failedCount": 1,
  "skippedCount": 1,
  "results": [
    { "key": "//default/marketing/2024-campaigns", "status": "SUCCESS" },
    { "key": "//default/marketing/archive", "status": "FAILED",
      "reason": "PERMISSION_DENIED", "message": "user lacks PERMISSION_EDIT_PERMISSIONS" },
    { "key": "//default/marketing/2024-campaigns/q1", "status": "SKIPPED",
      "reason": "COVERED_BY_PARENT" }
  ]
}
```

Field names are the shipped shape bulk upload/refresh already use (D-002) — not the
`successCount`/`failCount` sketch in #37063's original description, which those two supersede. Every
accepted path produces exactly one record (FR-014) — no silent gaps.

**`status`**: `SUCCESS` · `FAILED` · `SKIPPED` (`BatchItemStatus`, shared, three-valued — a boolean
cannot express the cancellation/ancestor-removed cases, D-012).

**`reason`** (present only when `status: FAILED` or `SKIPPED`), the stable enumerated set (FR-017,
FR-018), reused plus four additions this feature contributes (FR-019):

| Reason | Meaning | New here? |
|---|---|---|
| `PERMISSION_DENIED` | Caller lacks the required rights on this folder (FR-009, FR-009a) | reused |
| `UNCLASSIFIED` | A cause the system could not distinguish — reported honestly rather than guessed (FR-018) | reused |
| `PATH_NOT_FOUND` | Path no longer resolves to a folder — gone, a file, or malformed (FR-010) | **new** |
| `PROTECTED_FOLDER` | System folder or a site root — never deleted (FR-011) | **new** |
| `IN_USE` | Content in the subtree was locked by another author, blocking the delete (D-010) | **new** |
| `COVERED_BY_PARENT` | An ancestor in the same submission removed it first — always paired with `status: SKIPPED`, never `FAILED` (FR-013) | **new** |

`message` is diagnostic only — for logs, **never** displayed to the author (FR-017).

**On a cancelled run**: the outcome additionally records where the run stopped (FR-028), so the
remainder can be resubmitted without guessing — see data-model.md for the field.

---

## 5. Configuration

Exposed through the existing configuration endpoint the client already reads before submitting
(FR-007, FR-038):

| Property | Default | Purpose |
|---|---|---|
| `FOLDER_BULK_DELETE_MAX_PATHS` | **50** | Maximum `assetPaths` length before a `400` (FR-004, FR-007). Configurable via `Config`, same pattern as bulk upload's `CONTENT_BULK_UPLOAD_MAX_FILES` (default `100`) — set lower here because each accepted path is one sequential, unbounded-size blocking transaction (FR-020/D-013), not one bounded file, so the realistic per-submission cost is far higher per item. The spec's own worked examples (a "forty folders" cleanup, User Story 4; "twelve obsolete folders", Context) both fit comfortably under this default. |

---

## 6. Notifications and announcements — two different signals, two different audiences

**Terminal notification (submitter-only)** — FR-031…FR-035. Pushed while the submitter is present,
and recorded durably so it survives navigating away (FR-032). Wording differs for a clean, partial,
and cancelled run (FR-033); delivery failure never changes the recorded outcome (FR-034). Carries a
distinguishable completion signal type (FR-035) and the full outcome (§4) — not a summary (C-009).

**Per-folder announcement (everyone who may read the folder, excluding the submitter)** — FR-035a,
FR-035b, C-012. Two events per folder in the run, **not one per run**:

| Event | Fires | Ordered |
|---|---|---|
| Folder entering a delete | Before that folder's deletion begins | Must precede the deletion |
| Folder left a delete | After that folder is fully deleted | Must follow the deletion (same ordering as the completion signal, C-010) |

Audience for both is derived from `PERMISSION_READ` on the folder, established while the folder
still exists — not from a back-end role check, and not from who submitted the run (FR-035b). This is
not a substitute for reading the `/active` listing (§2): a run that dies never announces its end, so
the client's own load-time establishment of the in-flight set remains the recovery path (C-012).

---

## 7. Explicitly unchanged

`POST /api/v1/assets/folders/_delete` — address, request form, and behavior, all untouched (FR-006).
Not versioned, not deprecated, not routed through this feature's code path.
