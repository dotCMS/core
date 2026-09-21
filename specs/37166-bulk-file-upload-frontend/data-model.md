# Phase 1 Data Model — client-side shapes

Client-side only. Nothing here is a wire format; the wire format is the shared contract and is not settled (see `contracts/client-requirements.md`).

## Run

One operation the interface is still tracking. Several may exist at once (FR-015). Outlives the surface that started it (FR-013).

| Field | Meaning | Notes |
|---|---|---|
| `runId` | Client-generated identity | Allocated at submission, before the server handle arrives — see research R-003. This is the registry key. |
| `handle` | The server's identifier for the run | Absent until submission is answered. Present only for job-backed runs. |
| `operation` | Which operation this is | Drives the repeat-fire guard together with `targets`. |
| `actionLabel` | Resolved label shown to the author | Already-resolved copy, not a message key. |
| `targetLabel` | The single item's name, when there is one | Optional. **Must be escaped before display** (FR-011). |
| `total` | How many items the run covers | |
| `processed` | How many are done | **Optional.** Absent when the run does not report progress — see research R-005. Absence means indeterminate, never zero. |
| `targets` | What the run is acting on | Used by the guard (FR-016) and to reject a stale target. |

### Run states

`submitting → running → (succeeded | partiallySucceeded | failed | stopped | unrecognised)`

`unrecognised` is not a server state. It is what the client resolves any terminal state it does not know into, and it renders as an **error, never a success** (FR-024). It is the safety net that makes deferring cancellation safe.

## Run outcome

What a terminal run produced. Rendered once, as a transient notification (FR-021).

| Field | Meaning | Notes |
|---|---|---|
| `successCount`, `failCount`, `skippedCount` | The server's counts | **Never** substituted with the number of items the author chose (FR-022). |
| `total` | What the run covered | Used to detect counts that do not close (FR-024). |
| `results` | Per-item outcomes | Each names the item and, on failure, carries a reason code. |

## Per-item result

| Field | Meaning |
|---|---|
| `itemLabel` | The file name or item name to show the author |
| `status` | succeeded / failed / skipped |
| `reason` | Failure reason code. Mapped to product copy client-side (FR-036) |

## Failure reasons

The closed set the client must be able to speak to (FR-036). Each needs its own message definition; a reason with no copy is a hole the author sees.

| Reason | Author-facing meaning | Notes |
|---|---|---|
| Over size limit | The file is larger than allowed | |
| Disallowed type | The file type is not allowed | Copy says **type**, not extension (FR-039) — the server resolves it by detection, not by trusting the name |
| Name collision | A file with that name already exists | Case-insensitive; the copy must not suggest a name differing only by case (FR-036) |
| Permission denied | The author may not create that file | Per-file, narrower than the target-folder check |
| Staged content unavailable | The uploaded content is no longer available | The batch waited longer than the staged content lived |
| Unclassified | Something else went wrong | Needs **real copy**, not a fallback for reasons nobody wrote |

**Adding a reason later is a change to both halves**, never to the server alone.

## Chosen files

What the author picked on their own computer, before submission.

| Field | Notes |
|---|---|
| `files` | The files themselves |
| `target` | The folder, or the site when at the root |
| `uploadType` | One per batch (FR-003); resolved before submitting, never inferred server-side |
| `declaredTotalSize` | Required so the server can refuse an oversized batch fast (FR-038). Without it the only enforcement left runs after the content has already been uploaded |

---

**Commit-worthiness**: this file is worth committing. The failure-reason set and the `unrecognised` state are decisions a future developer needs and cannot read off the spec, and the reason set is half of a cross-team contract.
