# Data Model: Content Drive bulk folder delete — backend

**Feature**: `specs/37063-bulk-folder-delete-backend` · **Date**: 2026-09-17

Field-level ground truth for the entities in [spec.md](./spec.md) §Key Entities. The wire shapes
live in [contracts/bulk-delete-api.md](./contracts/bulk-delete-api.md); this file covers what is
stored and how it is keyed. **No new database table** — see plan.md Storage and Complexity Tracking
for why this feature does not need bulk upload's (withdrawn) per-item durable state.

---

## 1. Bulk delete run — job parameters

Not a new entity. A run is a row in the existing `job` table, queue `folderBulkDelete`, whose
`parameters` JSONB carries everything the run needs. Written once at `createJob` and immutable
thereafter, same as every other job-queue consumer.

| Parameter | Type | Notes |
|---|---|---|
| `userId` | string | The submitter. The run executes with their permissions (spec Assumptions) and the terminal notification is addressed to them (FR-031). |
| `paths` | array | One entry per **distinct, resolved** path, in the order the client submitted them (after dedup, FR-012). |
| `paths[].path` | string | The site-qualified path, as submitted. This is the outcome's `key` (§3) and what the durable notification shows a day later — human-readable, unlike an identifier (plan.md PO-2). |
| `paths[].folderIdentifier` | string | Resolved once at submission, alongside the permission/validation check that already has to touch the folder — costs nothing extra. Lets a client match its selected rows exactly even across a rename (plan.md PO-2). Not used by the delete itself, which re-resolves by path at execution time exactly as the shipped single delete does — carried for the client's benefit only. |

**Why paths, not identifiers, in the wire submission**: settled by C-001 ("the client sends the
selected folder paths"), not reopened here — see plan.md PO-2 for the full reasoning, including why
the rename-between-listing-and-confirm race this raises is pre-existing (the shipped single-folder
delete has the identical race) rather than new.

**No target, no options.** Unlike bulk upload's `baseType`/`folderId`/`siteId`, this submission
carries nothing beyond the paths themselves (Key Entities: "Bulk delete submission... carries
nothing else") — there is no destination, because delete has none.

---

## 2. Overlap guard — no new storage, read at submission time only

Not a stored entity — a submission-time check against `JobQueueManagerAPI.getActiveJobs
("folderBulkDelete", page, pageSize)` (all pages), comparing each active job's `paths[].path`
against the incoming submission's paths for a prefix relationship in either direction (FR-029).
Closed against the check-then-act race with a transaction-scoped Postgres advisory lock keyed by
site identifier (plan.md PO-6, Complexity Tracking) — the lock itself is not a stored row, just a
`pg_advisory_xact_lock` held for the duration of the check + `createJob` call.

---

## 3. Per-path outcome — the shared batch item result, reused unchanged

`com.dotcms.jobs.business.batch.AbstractBatchItemResult` (generates `BatchItemResult`) — the exact
type bulk upload and bulk refresh already ship, per its own Javadoc naming this feature as an
anticipated consumer (research.md R2). **Not modified.**

| Field | Type | Notes |
|---|---|---|
| `key()` | string | The folder path — see §1 on why path, not identifier, is the key here. |
| `status()` | `BatchItemStatus` | `SUCCESS` \| `FAILED` \| `SKIPPED` |
| `reason()` | `Optional<BatchFailureReason>` | Present only when `FAILED` or `SKIPPED`. Four new enum values this feature adds — see contracts/bulk-delete-api.md §4. |
| `message()` | `Optional<String>` | Diagnostic only, never displayed (FR-017). |

**`SKIPPED` carries two distinct meanings here, both legitimate** (FR-013, FR-027): a path never
reached because the run was cancelled first, and a path that was an ancestor's descendant and was
therefore never attempted as its own unit. Both are honestly "never attempted," which is what
`SKIPPED` means — the client's copy must cover both senses (spec FR-013's own note).

---

## 4. Run outcome — the envelope

A new, feature-specific type (no shared envelope exists yet across bulk upload/refresh/delete — see
research.md R2's gap note), following D-002's field names exactly:

`com.dotcms.rest.api.v1.asset.bulkdelete.AbstractFolderBulkDeleteSubmitResponse` for the submission
response (§1 of the contract), and the terminal outcome read from the job's `result` (harvested from
the processor's in-memory accumulation at the terminal state — no per-item durable table, per
plan.md PO-7):

| Field | Type | Notes |
|---|---|---|
| `total` | int | Equals `submitted` from the submission response, by construction (FR-003, C-003). |
| `processed` | int | Paths actually attempted (excludes `SKIPPED` from cancellation, includes `SKIPPED` from ancestor-removal since those *were* processed as a dedup decision). |
| `successCount` | int | |
| `failedCount` | int | |
| `skippedCount` | int | |
| `results` | array of `BatchItemResult` | §3 — one entry per distinct accepted path, always (FR-014). |
| `stoppedAt` | string, nullable | Only present on a cancelled run — the path (or index) the run had reached, so the remainder can be resubmitted without guessing (FR-028). |

**Accumulated in memory, harvested at the terminal state** — the same approach bulk refresh uses,
and the one bulk upload fell back to once its own durable per-item table was withdrawn (see
`specs/37166-bulk-file-upload/data-model.md` §2). This feature does not need per-item resumability
the way bulk upload originally did: an interrupted top-level folder rolls back completely (FR-023),
so there is nothing partial to resume *within* a folder, and FR-030's "don't misreport an
already-deleted folder" is answered by the `hasJobBeenInState` check (plan.md PO-7), not by replaying
individual item state.

---

## 5. Announcement events — transient, not stored

FR-035a's two per-folder signals (`entering` / `left` a delete) are `SystemEventsAPI.pushAsync` calls
with no durable record of their own — fire-and-forget (plan.md PO-3).

**Both are new `SystemEventType` values fired by `FolderBulkDeleteProcessor` itself** —
`FOLDER_DELETE_STARTED` before a top-level folder's delete is attempted, `FOLDER_DELETE_FINISHED`
after, regardless of whether that attempt succeeded or was caught as a classified failure.
Revised 2026-09-21 from an earlier plan that assumed "left" could reuse the existing `DELETE_FOLDER`
event fired inside `FolderAPIImpl.delete` — checking the frontend half (PR dotCMS/core#37612) found
it subscribes to its own `FOLDER_DELETE_FINISHED` instead, expected on a failed delete too (its own
fixture: "leaving a delete, success or failure alike"), which `DELETE_FOLDER`'s success-only push
cannot provide — see research.md R4.

| Field | Type | Notes |
|---|---|---|
| `jobId` | string | The run this announcement belongs to, so a client following its own submission does not double-count one for a run it did not start. |
| `path` | string | The site-qualified path of the folder entering or leaving the delete. |

Their audience (`PermissionAPI.getRolesWithPermission(folder, PERMISSION_READ)`, pushed with
`Visibility.EXCLUDE_OWNER`) is computed **once per folder**, before the delete attempt, and reused
for both pushes — it is not carried in the job's parameters, since by the time "left" fires a
successful delete has left nothing to compute rights against, which is exactly why "entering" must
be computed before the delete and "left" reuses that same computation rather than repeating it.
