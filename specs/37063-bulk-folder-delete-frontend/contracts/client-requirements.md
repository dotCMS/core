# Client Requirements: Content Drive bulk folder delete — frontend

**Plan**: [../plan.md](../plan.md) | **Server contract**: `specs/37063-bulk-folder-delete-backend/spec.md` §Contract Consumed by the Client (C-001 … C-012)

What this client needs from the server half, stated as the consumer's checklist. The server spec is
authoritative; this file exists so the frontend plan has one written definition to type against while
the server half is still being built, and so drift is caught at a named place rather than discovered.

**Status of the other half**: contract merged, implementation not started. Nothing below has been
exercised against a running endpoint.

---

## CR-01 — Submission (C-001, C-002, C-003)

- One ordinary JSON request carrying the selected folder paths. No multipart, no content.
- Answered **immediately** with a run handle: a run id, a ready-made address for following it, and the
  count of paths the **server** accepted.
- The client never assembles the follow address, and never displays its own selection size in place of
  the accepted count.
- **Client obligation**: send every selected folder, not a rights-filtered subset (FR-004a).

## CR-02 — Submission refusals (C-004)

Four refusals must be **distinguishable**, because each has different copy:

| Refusal | Client behaviour |
|---|---|
| Nothing submitted / empty list | Should not be reachable; the action is unavailable with no selection. |
| Over the configured maximum | Tell the author the limit (FR-006), not a generic failure. |
| Not entitled to the operation | The action should already have been withheld (FR-004); if it is reached, report plainly. |
| Overlaps an in-flight run | **The only one an ordinary author can provoke.** Names the folder, never the person who started the other run (FR-040). |

## CR-03 — The maximum, readable before submitting (backend FR-007, FR-038)

- Exposed through the existing configuration endpoint, alongside the bulk-upload limits already there.
- **Client obligation**: read it; never hard-code a copy of it.

## CR-04 — Failure reasons are a closed, enumerated set (C-005)

- Each maps to client copy. **Delete adds four values** the shared set did not carry:
  `PATH_NOT_FOUND`, `PROTECTED_FOLDER`, `IN_USE`, `COVERED_BY_PARENT`, alongside the existing
  `PERMISSION_DENIED` and `UNCLASSIFIED`.
- The server's `message` is **diagnostic and must not be displayed**.
- **Client obligation**: an unrecognised reason still names the folder and reports it as failed, via
  the fallback (FR-030). Never swallow the folder.

## CR-05 — Progress counts completed top-level folders, and nothing finer (C-006)

- There is no visibility inside a folder to report.
- **Client obligation**: render an **indeterminate** indicator. A determinate bar would invent
  precision the server does not have and would sit at zero through the folder that matters (FR-016).

## CR-06 — Cancellation exists, with delete's guarantee (C-007)

- The server provides it. Its wording must say each folder is left either fully deleted or untouched;
  folder copy's wording must not be reused.
- **This client does not surface cancellation in this version** (spec D-011). The obligation binds
  whoever surfaces it — expected to be the background task manager (#33331).

## CR-07 — Terminal state and outcome (C-008)

- Counts **plus** per-folder records, each failure carrying its reason.
- **Client obligation**: name the failing folders (FR-026). The counts alone do not tell an author what
  survived.

## CR-08 — Pushed completion, plus a durable record (C-009)

- Completion is **pushed** to the submitter; the client does not poll for it and does not build a jobs
  screen.
- The durable record holds the **full** per-folder set, not a summary — this is what makes the report's
  "and N more" reachable rather than a dead end (FR-027a).
- **Client obligation**: link the overflow to that record.

## CR-09 — Ordering: the completion follows the deletions (C-010)

- A listing refreshed on the completion finds the folders gone.
- **Client obligation**: refresh both the grid and the sidebar tree on it (FR-035, FR-036); they load
  separately. ADR-0018 means the listing reads the database, so there is no index lag to retry around.

## CR-10 — The in-flight listing includes runs that are not running (C-011)

**The single most consequential item in this file.**

- The queue's *active* listing returns every run in a **non-terminal** state — **failed** and
  **abandoned** runs included, not only working ones.
- **Client obligation**: filter on each run's own state and keep only what genuinely means *in
  progress* (FR-019a). Reading it naively marks folders whose delete already failed, and they stay
  marked until the framework moves them on.
- The symptom is *"sometimes folders stay marked forever"*, which reads as a client defect. It is not.

## CR-11 — Folder paths recorded with a run are readable (backend FR-005a, D-015)

- Readable by any back-end user, including paths on sites they have no rights to. Paths only — names
  and structure, not content.
- This is what makes the load-time in-flight read possible at all. It was **decided by this half's
  requirements**, not inherited: marking after a reload and marking another author's run both need it.

## CR-12 — Announcements: a folder enters and leaves a delete (C-012, backend FR-035a/b)

- Emitted per folder, to **everyone who may read that folder** — distinct from the completion, which
  reaches only the submitter.
- Delivery is filtered server-side by the recipient's rights. **The client does not re-filter** and must
  not be written as though it has to.
- **Client obligation**: keep the in-flight set current from these, and do **not** drop the load-time
  read in favour of them (FR-020b). A run whose process dies never announces that it ended; the
  load-time read is the only thing that recovers it.

---

## CR-02 resolved — refusals are told apart by `errorCode`

**Raised 2026-09-17 from this side, settled 2026-09-18 by the backend half** (`9f6d4eb9cb`, and
dotCMS/core#37063 comment 5720585127).

Status alone could not separate the two `400` cases. The answer is `ErrorEntity` — the structured
error type the REST layer already builds `ValidationException` bodies from — rather than a field
invented for this feature:

```json
{ "errors": [{ "errorCode": "OVER_MAX_PATHS", "message": "...", "fieldName": "assetPaths" }] }
```

| Status | `errorCode` | Client copy |
|---|---|---|
| `400` | `EMPTY_SELECTION` | Nothing was submitted |
| `400` | `OVER_MAX_PATHS` | Names the limit, read from configuration |
| `403` | `NOT_ENTITLED` | Not allowed to bulk delete |
| `409` | `OVERLAPPING_RUN` | Something in the selection is already being deleted |

The client switches on `errorCode`, and falls back to `UNCLASSIFIED` rather than guessing between
the two `400`s when a body carries no code.

**The ceiling is 50 by default** (`FOLDER_BULK_DELETE_MAX_PATHS`), lower than bulk upload's 100
because each accepted path is one sequential blocking transaction rather than one bounded file. The
client's authoritative source stays the configuration endpoint; the number in the refusal `message`
is prose.

### One shortfall this leaves, recorded rather than worked around

**FR-040 asks the client to name the folder an overlapping run collided on. The body does not carry
one.** The folder appears inside `message` as server-generated English, which is not localised and
so is not rendered. So the refusal says *something in this selection is already being deleted*
without saying which.

Parsing the sentence was considered and rejected: it would break the first time the wording changes,
and it would put server English on screen by the back door. Closing it properly needs a structured
field — worth raising if authors find the refusal hard to act on.

## Typing and drift

Every shape above is typed **once**, in `@dotcms/dotcms-models`, and consumed from there. If the server
lands differently, the divergence is a compile error at a handful of sites rather than runtime
breakage spread through the UI.

The first-contact checklist — what to re-verify the day the client talks to the real server — is in
[../quickstart.md](../quickstart.md).

---

## Explicitly not this client's business

Per the server spec: how a run executes, the transaction boundary, what happens to a subtree the author
cannot fully act on, retry and abandonment behaviour, and the durable record's lifetime.
