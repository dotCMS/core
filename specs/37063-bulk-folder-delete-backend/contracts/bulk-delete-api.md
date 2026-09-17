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
  "acceptedCount": 5
}
```

| Field | Type | Description |
|---|---|---|
| `jobId` | string | The run's identifier. |
| `statusUrl` | string | Ready-to-use address for status; the caller never assembles it (FR-002). Generic job address — see §2. |
| `acceptedCount` | number | How many **distinct** paths the server accepted into the run — equals the total the final outcome reports, by construction (FR-003, C-003). After deduplication, so it can be smaller than `assetPaths.length`. |

**Submission refusals — `400`**:

| Condition | When it fires |
|---|---|
| `assetPaths` missing or empty | FR-004 |
| `assetPaths` longer than the configured maximum | FR-004 — see §5 for where the maximum is published |
| Every path fails submission-time validation | Still `400`, not an accepted job that fails immediately — spec edge case |

**Submission refusals — `403`**: caller not entitled to the operation at all (FR-005). Per-path
permission is not a submission refusal — see §3.

**Submission refusals — `409`**: a submitted path is the same as, an ancestor of, or a descendant of
a path an in-flight run (in this queue, any submitter) is already covering (FR-029, FR-029a). The
message names the conflicting folder, not the other submitter: *"another deletion is already running
for `//default/marketing/2024-campaigns`."* Refused before a job is created (FR-029, FR-029b — see
plan.md PO-6 for how the check-then-act race is closed).

---

## 2. Follow, cancel, and read the outcome

**Not a new set of addresses.** Once accepted, the caller uses the **generic job-queue** addresses
(D-003) — status, cancel, monitor — the same shapes every job-queue consumer (content import, bulk
upload, bulk refresh) already exposes. This feature adds no parallel status/cancel/monitor endpoints
under the assets API.

| Action | Address (from `statusUrl` / the generic job API) | Notes |
|---|---|---|
| Status / outcome | `GET /api/v1/jobs/{jobId}/status` | See §4 for the outcome shape once terminal |
| Cancel | `POST /api/v1/jobs/{jobId}/cancel` | Takes effect between top-level folders, never mid-subtree (FR-026) |
| Monitor (live updates) | `GET /api/v1/jobs/{jobId}/monitor` | Server-sent progress; see §3 |
| List active runs (domain-scoped) | `GET /api/v1/assets/folders/_bulkdelete/active` | Mirrors content import's domain-scoped `/active` (D-003). **Not filtered by submitter** (FR-005a) — any back-end user can read it. **Includes non-terminal states beyond "running"** — see the client-side warning in spec C-011; the same caveat applies here. |

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
      "reason": "ANCESTOR_REMOVED" }
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
| `NOT_FOUND` | Path no longer resolves to a folder — gone, a file, or malformed (FR-010) | **new** |
| `PROTECTED_FOLDER` | System folder or a site root — never deleted (FR-011) | **new** |
| `LOCKED` | Content in the subtree was locked by another author, blocking the delete (D-010) | **new** |
| `ANCESTOR_REMOVED` | An ancestor in the same submission removed it first — always paired with `status: SKIPPED`, never `FAILED` (FR-013) | **new** |

`message` is diagnostic only — for logs, **never** displayed to the author (FR-017).

**On a cancelled run**: the outcome additionally records where the run stopped (FR-028), so the
remainder can be resubmitted without guessing — see data-model.md for the field.

---

## 5. Configuration

Exposed through the existing configuration endpoint the client already reads before submitting
(FR-007, FR-038):

| Property | Default | Purpose |
|---|---|---|
| `FOLDER_BULK_DELETE_MAX_PATHS` | TBD at implementation, documented in code | Maximum `assetPaths` length before a `400` (FR-004, FR-007) |

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
