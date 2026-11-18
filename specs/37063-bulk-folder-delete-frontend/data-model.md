# Phase 1 Data Model: Content Drive bulk folder delete — frontend

**Plan**: [plan.md](./plan.md) | **Research**: [research.md](./research.md) | **Date**: 2026-09-17

Client-side entities only. Wire shapes are in [contracts/client-requirements.md](./contracts/client-requirements.md);
this file is what the client holds, where it holds it, and how it changes.

---

## 1. Submission

What leaves the client when the author confirms.

| Field | Type | Notes |
|---|---|---|
| `assetPaths` | `string[]` | Site-qualified folder paths, the same form the shipped single delete accepts. Every selected folder, never a rights-filtered subset (FR-004a). |

**Validation before sending**: non-empty, and within the server's configured maximum read from the
configuration endpoint (FR-006). Both are also enforced server-side; the client checks them to give a
better message, not to be the authority.

---

## 2. Run handle

What comes back, immediately, in place of an outcome.

| Field | Type | Notes |
|---|---|---|
| `jobId` | `string` | The run's identity, and the correlation key for every later message about it. |
| `statusUrl` | `string` | Used as given. The client never assembles it (C-001). |
| `submitted` | `number` | Paths the **server** accepted. This is the number shown and the number marked — never the selection size (C-003, FR-025). |

---

## 3. In-flight folder set

The heart of the feature. Owned by the new store feature `withFolderDeleteRuns`.

| Field | Type | Notes |
|---|---|---|
| `paths` | `Set<string>` \| `Record<string, string>` | Folder paths currently being deleted, by any author. **Keyed by path** because that is the identity every server message uses (R1). Value, where a map is used, is the `jobId` responsible, so one run ending clears only its own folders. |
| `established` | `boolean` | Whether the load-time read has answered. Marking renders from whatever is known; this only distinguishes "nothing in flight" from "not yet asked" for diagnostics. |

**Derived** — one computed, read by both the grid and the sidebar tree, so they cannot disagree:

| Computed | Shape | Notes |
|---|---|---|
| `inFlightFolderPaths` | `string[]` | The union of server-derived paths and the paths of runs this client fired. |
| `folderBusyRowKeys` | `string[]` | Resolved against the rows currently listed, targeting **both** `inode` and `identifier` (R1). Merged into what the list view already receives. |

### Lifecycle

| Transition | Trigger | Effect |
|---|---|---|
| **Establish** | Content Drive opens | Read the queue's in-flight runs; keep only those whose state genuinely means *in progress* (FR-019a, C-011); seed `paths` from their recorded folders. Never blocks first render (FR-022, FR-023). |
| **Add** | A delete-started announcement | Add that folder's path. Covers runs started by any author after this page opened (FR-020a). |
| **Add** | This client's own submission is accepted | Add the accepted paths immediately, without waiting for the announcement round-trip. |
| **Remove** | A delete-finished announcement | Remove that folder's path, whether the delete succeeded or failed (FR-021). |
| **Remove** | This client's own run completes | Remove its paths, from the pushed completion. |
| **Re-establish** | Content Drive opens again | Rebuilt from the server, not inherited. This is what recovers a folder whose run died without announcing (FR-020b). |
| **Degrade** | The load-time read fails | `paths` stays empty and the listing renders unmarked and fully usable. Never surfaced to the author as an error about their own action (FR-022). |

**Invariant**: a path leaves the set on any terminal outcome. A mark that outlives its run is
indistinguishable to the author from a folder nobody can touch (FR-021, SC-009).

---

## 4. Run registration (existing shape, reused)

Delete registers on the Content Drive store's existing multi-run execution state rather than adding a
parallel one.

| Field | Type | Notes |
|---|---|---|
| `runId` | `string` | Client-allocated. |
| `operation` | `string` | Paired with `targets` to form the repeat guard — *this operation over these items*, not a portlet-wide lock (FR-018). |
| `targets` | `string[]` | Row keys the run marks. For folders: **both** `inode` and `identifier` (R1). |
| `actionName` | `string` | Resolved label, straight into the indicator. |
| `total` | `number` | The server's accepted count, not the selection size. |

---

## 5. Per-folder outcome

One record per submitted path, read from the finished run.

| Field | Type | Notes |
|---|---|---|
| `key` | `string` | The folder path. Generic in the shared type — a file name for upload, a folder path here. |
| `status` | `'SUCCESS' \| 'FAILED' \| 'SKIPPED'` | A boolean cannot express skipped, which is why the shared shape uses three values. |
| `reason` | reason value | Present on `FAILED` only. Mapped to copy; see §6. |
| `message` | `string` | **Diagnostic. Never rendered** (FR-029, C-005). |

### Run outcome

| Field | Type | Notes |
|---|---|---|
| `total` | `number` | |
| `processed` | `number` | Attempted across every attempt — `successCount + failedCount`, not `total`. |
| `successCount` | `number` | |
| `failedCount` | `number` | Note the name: `failedCount`, not `failCount`. |
| `skippedCount` | `number` | Never attempted. |
| `results` | per-folder outcome`[]` | Present; the report depends on it rather than on counts alone (C-009). |

---

## 6. Reason → copy mapping

Owned by `utils/folder-delete-outcome.ts`, shared with #37062 and #37165.

| Reason | Meaning to the author |
|---|---|
| `PERMISSION_DENIED` | No rights on the folder, or on something inside it. |
| `PATH_NOT_FOUND` | The folder no longer exists, or the path does not name a folder. |
| `PROTECTED_FOLDER` | A system folder or site root; refused outright. |
| `IN_USE` | Locked or referenced content inside blocked the delete. |
| `COVERED_BY_PARENT` | An ancestor in the same submission removed it first. **Not a failure to be alarmed by.** |
| `UNCLASSIFIED` | Something else. The fallback, and also what an **unrecognised** reason renders as — the folder is still named and still reported as failed (FR-030). |

**`SKIPPED` needs two messages, not one** (FR-031): cancelled before the run reached it, versus already
removed by an ancestor. Both are "not attempted", for entirely different reasons, and one sentence
cannot carry both.

---

## 7. Announcement payloads

Two events the client subscribes to, in addition to the completion it already handles.

| Field | Type | Notes |
|---|---|---|
| `jobId` | `string` | Correlates to a run; lets an announcement for a run this client fired be reconciled rather than double-counted. |
| `path` | `string` | The folder entering or leaving a delete. |

**Delivery is already filtered by the recipient's rights** (backend FR-035b) — an author who may not
read the folder never receives the announcement. The client does **not** re-filter, and must not be
written as though it has to.

---

## Entity relationships

```text
Submission ──► Run handle ──► (in-flight folder set) ◄── announcements (any author's run)
                   │                     │
                   │                     └──► grid rows + tree nodes, via one computed
                   │
                   └──► pushed completion ──► Run outcome ──► per-folder outcomes ──► report
                                                                       │
                                                                       └──► durable record (overflow)
```
