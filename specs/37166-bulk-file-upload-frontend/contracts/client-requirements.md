# What the client needs from the batch contract

**Status: ANSWERED.** The agreed contract is [`specs/37166-bulk-file-upload/contracts/bulk-upload-api.md`](../../37166-bulk-file-upload/contracts/bulk-upload-api.md). This document stated the consumer's side of the boundary so the two halves of #37166 had something concrete to converge on; they have. It is kept as the record of what this half asked for and what it got. **The binding definition is that file, not this one**, and implementation goes against it.

**The agreed contract belongs in `specs/37166-bulk-file-upload/contracts/`**, owned by the half that defines the outcome shape (backend FR-018), reviewed by this half as the consumer. It goes there and not in either `plan.md` because in this repo `plan.md`, `research.md`, `tasks.md`, `quickstart.md` and `checklists/` are gitignored, while `contracts/` is committed. An agreement written in a plan does not survive the branch.

## Open items, and how the contract answered them

All six are closed, which is what unblocks frontend Phase 3.

| # | Question | Raised by | Why the client cares |
|---|---|---|---|
| 1 | `failedCount` or `failCount`? | backend FR-018 | The client reads it. One spelling, both halves. |
| 2 | What is the generic item key? | backend FR-018 | An uploading file has neither identifier nor inode. The client keys per-file results by it to name failures (FR-023). |
| 3 | What makes a retry safe: same handle returned, or duplicates refused as collisions? | backend C-002a / FR-040 / FR-040a | Changes what a successful retry *looks like*. Under the collision branch a retry that worked reports 50 of 50 failed with "already exists", and the client must report that as the success it is rather than a total failure. |
| 4 | Submission field names and endpoint | this plan | Cannot be guessed. |
| 5 | The shape of the handle | this plan | Determines how the client correlates the pushed completion signal to its own run. |
| 6 | How the client declares the batch total size | frontend FR-038 | The fast refusal exists only where the caller declares a total, and the caller is the browser. Without it, the only enforcement left refuses the batch *after* the author has uploaded it. |

### The answers

1. **`failedCount`**, not `failCount`, matching what bulk refresh already ships.
2. **`results[].key`**, the file name. Generic on purpose, so #37062 / #37063 can put a folder path there without a second shape.
3. **The collision branch**, made reportable by **`duplicateSubmission`** on the outcome: `true` when the run was a resubmission of a batch that had already succeeded. That is what lets the client say "already uploaded" instead of "everything failed".
4. **`POST /api/v1/assets/_bulkupload`**, `multipart/form-data`: repeated **`files`** parts, one per file in the order the author chose them, plus a JSON **`form`** part carrying `baseType` (`DOTASSET` or `FILEASSET`), **exactly one** of `folderId` / `siteId`, and an optional `totalSizeBytes`.
5. **`202 Accepted`** with `entity.jobId` and `entity.statusUrl`. Followed through the existing job endpoints (`GET /api/v1/jobs/{jobId}/status`, SSE `/monitor`, `POST .../cancel`); this feature adds none. The queue name `assetBulkUpload` is deliberately not the client's to hardcode.
6. **`totalSizeBytes`** on the `form` part. Optional: omitting or under-declaring it forfeits the fast refusal, and never raises the ceiling.

Refusals arrive as `400` (no files, malformed `form`, bad `baseType`, neither or both target fields, too many files, non-same-origin, staging disabled), `413` (over the total size ceiling, whether declared or accumulated while reading), `403` (may not add children to the target) and `404` (target does not exist). Server-side defaults are 100 files, 1 GB per batch, and a 200 MB per-file fallback where the content type declares no ceiling; the client does not hold copies of these and learns them through refusals.

### What the contract adds that this document did not ask for

Four items the client must now account for, all consequences of the one-call shape being chosen over staging-then-submitting:

- **No per-file upload progress, and no retry of a single file** (spec C-001a). A dropped connection means resending the batch. Progress is a property of the *run*, after the handle exists, not of the upload.
- **Every guarantee begins at the handle** (spec C-001a1). While content is still being sent there is no batch, so an abandoned upload records nothing and notifies nobody. Staged parts are reclaimed, so it costs the author nothing, but User Story 4's promise starts at the handle rather than at the file chooser.
- **The staging clock starts server-side**, immediately before the batch is created (spec C-001b). Nothing can expire between upload and submit, which makes `STAGED_CONTENT_UNAVAILABLE` a rare safety net rather than a routine outcome.
- **The completion signal is emitted after search-index visibility resolves** (spec C-006a). The single refresh fired on it finds every created file, including under an active text filter. The client depends on this ordering without being able to observe it.

## What the client requires, behaviourally

Restating the consumer's side of the frontend spec's §Contract Consumed, at the level the contract document must make concrete.

1. **Submit** several files with one target and one upload type, in a single call carrying the content, answered immediately with a handle. The client never stages content itself and never handles a staging identifier.
2. **Distinguishable submission refusals**, separable by the client: no files; bad upload type; missing target; too many files; batch over the total size ceiling; permission denied. The client shows different copy for each, so they cannot arrive as one undifferentiated error.
3. **Retry after an uncertain submission is safe** — whichever mechanism provides it, plus enough information to tell a successful retry from a genuine failure (item 3 above).
4. **Progress, if available.** Optional by design: the client renders indeterminate when it is absent and must never receive a fabricated position. See plan §Degradation.
5. **A terminal state and outcome**: counts, plus per-item results carrying a reason code for each failure.
6. **A pushed completion signal** carrying counts *and* per-item results, correlatable to the run the client submitted, reaching the browser without polling — plus the durable record behind it.

## Constraints the contract must respect

- **The reason set is closed and shared.** Six reasons (see `../data-model.md`). Each needs client copy; a reason with no copy is a hole the author sees. Adding one later is a change to both halves.
- **The server's human-readable message is diagnostic and is never displayed.** The client maps the reason code to product copy (frontend FR-030, backend FR-016a).
- **Counts are authoritative.** The client will not substitute the number of files the author chose, and will report an outcome whose counts do not close as an error rather than inventing a number.
- **A terminal state the client does not recognise is reported as an error, never a success.** This is what makes deferring cancellation to #33331 safe, so the contract must not assume the client silently tolerates unknown states.
