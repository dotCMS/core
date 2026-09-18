# Feature Specification: Content Drive bulk folder delete — backend

**Feature Branch**: `37063-content-drive-bulk-folder-delete-backend`

**Created**: 2026-09-15

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37063](https://github.com/dotCMS/core/issues/37063) — "Content Drive: bulk folder delete, async job endpoint and frontend wiring".

---

## Context

Content Drive lets an author select several rows at once. Folders are selectable, but the Action
Center cannot delete them: the only folder delete that exists is the **single-folder context menu
item** shipped on #35161, over `POST /api/v1/assets/folders/_delete`, which takes one path. An
author who selects twelve obsolete folders has to delete them one at a time, twelve confirmations
deep, and is told nothing collective about how it went.

**This specification covers the server side only.** #37063 carries both halves; they are specified
separately, as #37166 did, because the two are delivered by different people against a contract
rather than as one body of work. §Contract Consumed by the Client is therefore a mandatory output
of this spec, not an appendix — it is what the frontend half is written against, and the frontend
spec restates it from the consumer's side rather than re-deciding it.

**Why split at all** (the question #37063 leaves open): the backend here is not a thin wrapper over
an existing call. Deleting a folder today is a **single unbounded transaction over a recursive
walk**, and deciding what to do about that is most of the thinking here even though — see
§Decisions D-013 — the answer is to leave it alone for now and ship the multi-select. A combined
spec would bury a frontend reader under transaction semantics they cannot act on, and bury a
backend reader under toast copy. Two specs, one contract.

**The operation is permanent and recursive.** Deleting a folder destroys every asset and subfolder
underneath it, with no workflow action fired on those contents and nothing archived first. That is
existing behaviour, unchanged here; this feature multiplies its blast radius by the size of a
selection, which is why the confirmation obligations land on the client half and the
all-or-nothing obligations land here.

**Reuse, stated up front.** Three things already exist and this feature consumes them rather than
redefining them: the per-item outcome contract in `com.dotcms.jobs.business.batch` (shared by bulk
upload and bulk refresh), the job-queue framework in `com.dotcms.jobs.business`, and the path
resolution and permission gate the shipped single `_delete` already uses. The sibling tickets
#37062 (copy) and #37165 (move) consume the same outcome contract; **the three must not diverge**.

---

## User Scenarios & Testing *(mandatory)*

<!--
  Server behaviour, described from the perspective of the author whose action reaches it. Each is
  verifiable against the server alone — integration or Postman tests — with no browser involved.
-->

### User Story 1 - A selection of folders is accepted at once and every folder is deleted (Priority: P1)

An author's multi-row selection reaches the server as one submission naming several folder paths.
The server accepts it immediately with a handle, deletes each folder in the background, and records
what happened to each one.

**Why this priority**: This is the reported gap. Nothing else in the feature has value until a
selection can actually be submitted as one operation.

**Independent Test**: Submit five folders the author may delete; confirm the submission is answered
at once, and that all five are gone when the run reports itself finished.

**Acceptance Scenarios**:

1. **Given** five folder paths the author may delete, **When** the submission is made, **Then** the
   server answers immediately with a handle the caller can use to follow and cancel the run — it
   does not hold the caller until the folders are gone.
2. **Given** an accepted submission, **When** the run finishes, **Then** none of the five folders
   exists, and neither does anything that was inside them.
3. **Given** any folder in the selection, **When** it is deleted, **Then** the result is
   indistinguishable from deleting that same folder through the shipped single-folder delete — same
   permission rules, same contents destroyed, same events emitted.
4. **Given** this feature ships, **When** the single-folder delete is exercised, **Then** its
   endpoint, its request form and its behaviour are unchanged.
5. **Given** a submission naming exactly one folder, **When** it is made, **Then** it is accepted
   and runs as a batch of one — the endpoint does not special-case a single path back into the
   synchronous endpoint.

---

### User Story 2 - A folder that cannot be deleted does not take the run down with it (Priority: P1)

A selection routinely contains a folder the author may not delete, a path that no longer resolves,
or a folder the system refuses outright. The run continues past it and records, per path, what
happened and why.

**Why this priority**: Partial failure is the normal case over a multi-select, not an edge case. A
run that aborts at the first refusal leaves the author unable to tell what survived — and for a
destructive operation, "I don't know what is still there" is worse than for a creative one.

**Independent Test**: Submit five folders of which two cannot be deleted (one without permission,
one whose path does not resolve); confirm the other three are gone and the outcome reports 3
succeeded / 2 failed, naming both failures with distinguishable reasons.

**Acceptance Scenarios**:

1. **Given** a selection containing a folder that is refused, **When** the run proceeds, **Then**
   every remaining folder is still attempted.
2. **Given** a finished run, **When** its outcome is read, **Then** it carries a total, a success
   count, a failure count, a skipped count and a per-path record.
3. **Given** a path that failed, **When** its record is read, **Then** it names the path, marks it
   failed, and carries both a machine-readable reason and a human-readable diagnostic message.
4. **Given** a path the author may not delete, **When** the run reaches it, **Then** it is recorded
   as that path's own failure with a permission reason — not as a failure of the submission.
5. **Given** a path that does not resolve to a folder — it is gone, it is a file, or it is
   malformed — **When** the run reaches it, **Then** it is that path's own failure, distinguishable
   from a permission refusal.
6. **Given** the system folder or a site root, **When** it appears in a selection, **Then** it is
   refused as that path's own failure with a reason saying it is protected, and the rest of the
   selection still runs.
7. **Given** a selection where every path fails, **When** the run finishes, **Then** the outcome
   shows zero successes and names every failure — the run is not recorded as a success.

---

### User Story 3 - A cancelled run never leaves a half-deleted folder (Priority: P1)

The author cancels a run that is part-way through. Every folder in the selection is either
completely gone or completely untouched. Nothing is left as a tree with holes in it.

**Why this priority**: P1 and non-negotiable. A partially deleted tree is unrecoverable: there is no
undo, no archive, and no record of what used to be inside. This is the one guarantee whose absence
turns a convenience feature into a data-loss incident, and it is the reason delete's cancellation
semantics deliberately differ from copy's on #37062.

**Independent Test**: Cancel a run mid-selection; confirm the folders already completed are gone,
the folder in progress when the cancellation arrived is either fully gone or fully present, no
folder is left partially emptied, and the untouched remainder is recorded as skipped.

**Acceptance Scenarios**:

1. **Given** a run in progress, **When** it is cancelled, **Then** the cancellation takes effect
   between top-level folders and never mid-subtree.
2. **Given** a cancelled run, **When** its outcome is read, **Then** each folder in the selection is
   recorded as succeeded, failed, or skipped, and the skipped ones are distinguishable from the
   failed ones — they were never attempted, not refused.
3. **Given** a cancelled run, **When** the site is inspected afterwards, **Then** no selected folder
   is in a partially deleted state.
4. **Given** a cancelled run, **When** its outcome is read, **Then** it records where the run
   stopped, so the author can resubmit the remainder without guessing.

---

### User Story 4 - The author learns how it ended, even if they walked away (Priority: P2)

The submission is answered long before the work is done, so something has to close the loop. When
the run reaches a terminal state the submitter is told — pushed to them if they are still looking,
and recorded durably so the outcome survives navigating away or closing the tab.

**Why this priority**: P2 rather than P1 only because the job framework already exposes a readable
terminal status the client can poll, so the feature is usable without the push. It is not optional
work — an author who deleted forty folders and closed the tab must still be able to find out
whether all forty went — but it does not block the operation from working.

**Independent Test**: Submit a run, disconnect, reconnect after it finishes, and confirm the
per-path outcome is still readable and a durable notification was addressed to the submitter.

**Acceptance Scenarios**:

1. **Given** a run reaching any terminal state — finished, failed or cancelled — **When** it
   resolves, **Then** the submitter is notified, and only the submitter.
2. **Given** a notified run, **When** the notification is read, **Then** its wording reflects what
   actually happened: a clean run, a partial one, and a cancelled one read differently.
3. **Given** a finished run, **When** its outcome is requested later, **Then** the counts and the
   per-path records are still readable.
4. **Given** a run whose notification could not be delivered, **When** that happens, **Then** the
   run is still recorded as having finished — the notification is best-effort and never changes the
   run's outcome.

---

### User Story 5 - Two runs cannot race for the same subtree (Priority: P2)

A second submission arrives naming a folder that an in-flight run is already deleting, or an
ancestor or descendant of one. It is refused with a reason a person can read, rather than queued to
fail confusingly later.

**Why this priority**: P2 because it needs a concurrent author to occur, but it is cheap to get
right at submission and expensive to diagnose afterwards. Two runs walking overlapping trees produce
failures that look like defects — "folder not found" on a folder the author can see — and interleave
in ways no outcome record explains.

**Independent Test**: Submit a long-running delete, then submit a second one naming an ancestor of a
path in the first; confirm the second is refused with a readable reason and no job is created.

**Acceptance Scenarios**:

1. **Given** an in-flight run, **When** a submission names the same path, an ancestor of it, or a
   descendant of it, **Then** the submission is refused before a job is created and the reason names
   the conflict.
2. **Given** an in-flight run, **When** a submission names only unrelated paths, **Then** it is
   accepted and both run.
3. **Given** a run that has reached a terminal state, **When** a submission names a path it covered,
   **Then** it is accepted — the guard is about in-flight work, not history.

---

### Edge Cases

- **A selected folder is an ancestor of another selected folder.** Deleting the ancestor removes the
  descendant, so the descendant's own entry has nothing left to act on. See FR-013.
- **A path resolves to a file, not a folder.** The shipped single delete rejects this with an
  argument error; here it is that path's own failure (FR-010).
- **The same path appears twice in one submission.** Deduplicated before the run; the outcome
  reports the path once (FR-012).
- **An empty `assetPaths` list, or one over the configured maximum.** Refused at submission, before
  a job exists (FR-004).
- **A folder the author may read but not delete.** Folder deletion requires more than edit rights
  today, and the recursion re-checks per subfolder — see §Legacy Considerations. A refusal deep in
  the tree surfaces as that top-level folder's failure, not as a silent partial delete.
- **A subfolder or asset the author cannot read at all.** Different case, and the more dangerous one:
  the listing that feeds the recursion is permission-filtered, so an unreadable child is never
  returned and is therefore never deleted, never refused, and never reported. See FR-009b.
- **An asset that is missing from the search index.** The contents of a folder are resolved by
  *querying the index*, not the database, so anything unindexed or stale is invisible to the delete
  in exactly the same way an unreadable asset is — and this one has nothing to do with permissions.
- **Any content in the subtree locked by another author.** Aborts the entire folder's delete today,
  before anything in it is removed — *if the author could see it*. If they could not, it is swept
  out of storage with the lock ignored. See FR-009b, FR-022 and D-010.
- **An interrupted delete that is then re-run.** Storage converges; the search index accumulates a
  document with nothing behind it for every item the sweep removed, on every attempt. See FR-009d
  and FR-023.
- **A run is abandoned and re-queued.** There is no durable mid-run record, so a second attempt
  starts from the first path. Folders the first attempt already deleted no longer resolve — see
  FR-030 for how that must be reported, and §Planning Obligations for the cost.
- **A folder whose contents the author cannot read.** The contents lookup the delete performs is
  permission-filtered, so an unreadable asset may be invisible to the walk while still holding a
  reference that blocks the folder's removal. This is existing behaviour; it must surface as that
  folder's own failure rather than as a stuck run.
- **Cancellation arriving while the last folder is in progress.** There is nothing left to skip; the
  run finishes that folder and reports as cancelled with an empty skipped set.
- **Every path in the submission is refused at submission-time validation.** Still a `400`, not an
  accepted job that fails immediately — a caller must be able to tell "you sent me nothing usable"
  from "the work failed".

---

## Requirements *(mandatory)*

### Functional Requirements

#### Submission

- **FR-001**: The system MUST expose a submission that accepts several folder paths in one request
  and answers immediately, before any folder is deleted.
- **FR-002**: The answer MUST carry a handle the caller can use to follow the run, cancel it, and
  read its outcome, plus a ready-to-use address for doing so — the caller MUST NOT have to assemble
  that address itself.
- **FR-003**: The answer MUST state how many paths the **server** accepted into the run, and that
  number MUST equal the total the outcome later reports, so the first screen and the last agree by
  construction.
- **FR-004**: A submission that is malformed — no paths, an empty list, or more paths than the
  configured maximum — MUST be refused before any run is created, with distinguishable refusals.
- **FR-005**: A caller who is not entitled to use the operation at all MUST be refused at
  submission. Per-path permission is a different question and is FR-009.
- **FR-005a**: A submission's folder paths are recorded with the run and are therefore **readable by
  any back-end user**, including paths on sites they have no rights to. The listings that expose
  in-flight runs gate on "is a back-end user" and nothing more, and this feature does not narrow
  them. **Decided by the client half rather than here** (§Decisions D-015): marking a folder as
  in-flight from server state after a reload, and showing a folder another author is deleting,
  both require reading those paths back. Narrowing the listing would remove the second outright.
  The exposure is paths only — names and structure, not content — and narrowing it later changes a
  shared surface every queue consumer depends on, not just this one.
- **FR-006**: The system MUST NOT change the shipped single-folder delete — neither its address, its
  request shape, nor its behaviour. It stays synchronous and stays the path the context menu uses.
- **FR-007**: The maximum number of paths one submission may carry MUST be configurable, with a
  default, and MUST be readable by the client before it submits.

#### Per-path execution and outcome

- **FR-008**: Each top-level folder in the selection MUST be attempted independently: a refusal or
  failure on one MUST NOT abort the run, MUST NOT roll back folders already completed, and MUST NOT
  prevent the remaining ones from being attempted.
- **FR-009**: A per-path permission refusal MUST be recorded as that path's failure, with a
  machine-readable permission reason.
- **FR-009a**: The bulk path MUST NOT be more permissive than the shipped single-folder delete. That
  delete requires **both** edit rights and permission-editing rights on the folder, refuses the
  system folder outright, and re-checks the same rights on every subfolder as it recurses.
- **FR-009b**: The system MUST define what happens to the part of a subtree the author cannot fully
  act on. Today's behaviour is **asymmetric**, and neither half of it is a deliberate design: what
  the author cannot *see* is silently skipped, and what they can see but cannot *act on* aborts the
  whole delete. Four rules are in play at once, and only the first is about the folder —

  | What | Rule | What happens when it fails |
  |---|---|---|
  | The folder, at every level | edit **and** permission-editing rights | Refused, as an error |
  | Listing subfolders | read rights | Omitted from the recursion — then swept (below), which removes the folder itself but **not** its descendants |
  | Listing contents | read rights, **over the search index** | Omitted from the destroy — then swept (below) |
  | Destroying the contents that *were* listed | **publish** rights, on every one | Aborts the whole folder |
  | Destroying the contents that *were* listed | not locked by anyone else | Aborts the whole folder |
  | **Everything still under the folder afterwards** | **none at all** | Deleted directly from storage — no rights checked, no locks honoured, no search index updated |

  The last row is the one that matters and it is easy to miss, because it silently reverses the two
  above it. After the permission-filtered pass, the delete sweeps **everything remaining under the
  folder's path** out of storage directly. So content the author could not see is not left behind —
  it is destroyed, having bypassed the very publish and lock checks that protect the content they
  *could* see. The narrower an author's rights, the *less* scrutiny the content under that folder
  receives. This is existing behaviour and predates this feature; a bulk operation multiplies it.
  **Decided**: today's behaviour is kept and documented, not changed (§Decisions D-014). This
  feature reports honestly on what it can see and does not attempt to make the underlying delete
  safer — that is a separate, larger correction to behaviour the shipped single-folder delete has
  had for years. The consequence has to be stated rather than buried: an outcome record that says a
  folder succeeded is true of the data and **not** of the process.
- **FR-009c**: The contract MUST NOT imply that a client-side permission check can predict the
  outcome. Rights on the *folder* are the only thing a listing can carry, and they do not determine
  whether the delete succeeds: an author with full rights on a folder whose contents they may not
  publish gets a hard failure, and nothing at folder level anticipates it. Gating the control on
  folder rights is therefore correct as far as it goes — it catches the obvious refusals — but it is
  a **courtesy, not a guarantee**, and the per-path outcome remains the only authority. This is
  stated so the client can gate without over-promising, and so a failure that passes the gate reads
  as expected rather than as a defect.
- **FR-009d**: Content this operation destroys MUST leave the search index consistent with storage.
  The direct sweep described in FR-009b removes content from storage **without recording anything
  that would remove it from the search index**, and without the durable record that #37276 added to
  the ordinary content-destroy path — that fix is scoped to the ordinary path and this one does not
  go through it. Every folder delete that reaches the sweep therefore leaves search documents with
  nothing behind them, permanently, until someone reindexes by hand. Pre-existing, and **fixed
  separately in [#37599](https://github.com/dotCMS/core/issues/37599)** rather than here: unlike the
  permission behaviour it sits beside (D-014), this one is a contained bug — the correction reuses
  the durable record #37276 already built, in a transaction the sweep already holds open. It is
  called out in this specification because this feature multiplies how often the sweep runs, and
  because it is the single thing that makes re-running an interrupted delete lossy.
- **FR-010**: A path that does not resolve to a folder — it is gone, it is a file, or it is
  malformed — MUST be recorded as that path's failure, with a reason distinguishable from a
  permission refusal.
- **FR-011**: A path the system protects — the system folder, a site root — MUST be recorded as that
  path's failure with a reason saying so, and MUST NOT be deleted under any circumstances.
- **FR-012**: Duplicate paths within one submission MUST be collapsed before the run, so the outcome
  reports each distinct path once.
- **FR-013**: When one selected path is an ancestor of another selected path, the system MUST behave
  deterministically and MUST NOT report a misleading outcome for the descendant.
  **Decided**: the descendant is deduplicated away before the run and reported as **skipped**.
  Honest — it was never attempted as a unit — and deterministic. It stretches the skipped status,
  which otherwise means "the run was cancelled before reaching it", so the client's copy must cover
  both senses: not attempted, for two different reasons.
- **FR-014**: Every path in the run MUST produce exactly one outcome record. A path that is refused
  before work, that fails during it, that is never attempted, or that succeeds — all four are
  recorded. A silent gap MUST NOT occur.
- **FR-015**: The outcome MUST use the shared per-item contract already in use by bulk upload and
  bulk refresh, keyed by the folder path, rather than defining a second shape. Sibling features
  #37062 and #37165 consume the same one.
- **FR-016**: The outcome MUST carry a total, a processed count, a success count, a failure count, a
  skipped count, and the per-path records, using the **field names the shipped contract already
  uses** — see §Decisions D-002, which corrects the names #37063's own description gives.
- **FR-017**: Every failure reason MUST be machine-readable and drawn from a stable, enumerated set.
  The accompanying message is diagnostic, is for logs, and MUST NOT be presented to the author.
- **FR-018**: Reasons MUST be derived from facts the system established — a resolution that
  returned nothing, a permission check that said no — and never by matching the text of an
  exception. Where two causes genuinely cannot be told apart, the system MUST report the unclassified
  reason rather than guess: a wrong reason is worse than an honest unknown, because the author acts
  on it.
- **FR-019**: Any new failure reason this feature needs MUST be agreed with the client half before
  implementation, because every reason requires client copy and a reason with no copy renders as
  nothing. **Four are agreed** and are the set this feature adds to the shared enumeration:
  - the path no longer resolves, or does not name a folder (FR-010);
  - the folder is one the system protects and never deletes (FR-011);
  - something inside it is locked or otherwise in use, and blocked the delete (D-010);
  - an ancestor in the same submission removed it first (FR-013) — reported as *skipped*, since it
    was never attempted, rather than as a failure.

  Permission refusal (FR-009) and an unclassified cause (FR-018) already exist in that enumeration
  and are reused. A fifth reason MUST NOT be introduced during implementation without routing it
  back to the client half, for the reason above.

#### The transaction boundary

- **FR-020**: One top-level folder MUST be deleted as **one transaction**. This is what the shipped
  delete already does, and this feature deliberately does not change it — see §Decisions D-013.
- **FR-021**: Deleting one folder MUST NOT be made slower, hungrier or less atomic than deleting the
  same folder through the shipped single-folder delete. This feature moves the call into a
  background run; it does not alter the call.
- **FR-022**: The known costs of FR-020 MUST be recorded rather than discovered: a folder's contents
  are loaded whole before any of them is destroyed, so peak memory is bounded by the **widest single
  folder**; a failure late in a large folder rolls the whole folder back, and that rollback can take
  longer than the work it undoes; and a long-running folder produces no observable progress, which
  is what FR-024a exists to handle. None of these is introduced here. All of them are multiplied by
  a selection.
- **FR-023**: All-or-nothing holds under **every** interruption — cancellation, process death and
  the abandonment sweep alike. A top-level folder is either fully deleted or untouched; there is no
  intermediate state to observe, because there is no intermediate commit. Resolved by FR-020: with
  one transaction per folder the guarantee is free, and #37063's original wording is literally true
  with no softening (§Decisions D-008).

#### Progress, cancellation and concurrency

- **FR-024**: The system MUST report progress while a run is in flight, and MUST report it only when
  the reported value actually changes, rather than on every item.
- **FR-024a**: A run that is working MUST NOT be mistaken for one that has stalled. The framework
  declares a run abandoned when it has not updated for a threshold period — thirty minutes by
  default — and **puts it back in the queue**, so a large folder that reports progress only when a
  rounded percentage changes can be re-queued while it is still working, and then deletes the same
  tree a second time. FR-025 and this requirement are the same problem seen from two sides: a
  denominator that cannot move is a run that looks abandoned.
  **Decided**: the run emits a heartbeat independent of the percentage. Raising the threshold only
  moves the problem and accepting the risk is untenable once D-013 removes any in-folder progress.
  Because the delete call blocks until a folder is gone, that heartbeat cannot come from the code
  doing the work — it has to live beside it. Where exactly is a plan question, not a spec one
  (§Planning Obligations).
- **FR-025**: Progress MUST be reported at the granularity that actually exists, and the client MUST
  be told what that granularity is. Under FR-020 nothing inside a folder is observable, so the only
  honest denominator is **completed top-level folders**. On an unbalanced selection — one folder
  holding fifty thousand assets beside nine empty ones — that number is nearly meaningless, and a
  client that renders it as a determinate bar would be inventing precision the server does not have.
  The contract therefore says so explicitly (C-006), so the client can render an indeterminate
  indicator rather than a lie.
- **FR-026**: An in-flight run MUST be cancellable, and the cancellation MUST take effect between
  top-level folders, never mid-subtree.
- **FR-027**: Paths not reached when a cancellation takes effect MUST be recorded as skipped — a
  status distinct from failed, because they were never attempted rather than refused.
- **FR-028**: A cancelled run's outcome MUST record where it stopped, so the remainder can be
  resubmitted without guessing.
- **FR-029**: A submission overlapping an in-flight run's paths — the same path, an ancestor, or a
  descendant — MUST be refused at submission with a readable reason, and MUST NOT be queued. The
  guard applies to in-flight runs only, not to completed ones.
- **FR-029a**: The overlap guard's scope across authors MUST be stated. The listings that expose
  in-flight runs are not filtered by submitter, so guarding across all authors is possible; it is
  also the only version that prevents the race, since a corrupted tree does not care who started the
  second run. The cost is a refusal that has to explain a run the refused author cannot see.
  **Decided**: the guard is across **all authors**. A corrupted tree does not care who started the
  second run, and the listings that expose in-flight runs are not filtered by submitter, so this
  costs nothing extra to implement. The refusal must therefore be worded for someone who cannot see
  the run they are colliding with — "another deletion is already running for this folder", naming
  the folder and not the person.
- **FR-029b**: The window between checking for an overlapping run and recording the new one MUST be
  addressed rather than assumed away — two submissions arriving together can both pass a check that
  neither has yet invalidated.
- **FR-030**: A re-queued run MUST NOT report a folder that a previous attempt already deleted as a
  path failure. Deleting something that is already gone is the intended end state, and reporting it
  as "not found" tells the author their delete failed when it succeeded.

- **FR-030a**: The system MUST state its position on re-running a failed or interrupted delete.
  Declaring the run non-retryable is **not sufficient on its own**: the abandonment sweep returns an
  abandoned run to the queue without consulting the retry policy, so a second attempt can happen
  either way and has to be survivable regardless of what is declared.
  **Decided**: no retry policy — a failure is reported per folder and the author decides, matching
  what the reindex processor already does. The abandonment sweep still re-queues independently of
  that, which is exactly why FR-030 exists and is not made redundant by this answer.

#### Telling the author

- **FR-031**: On any terminal state the system MUST notify the **submitter**, and only the
  submitter — never every administrator.
- **FR-032**: The notification MUST be both pushed, so a present author sees it without polling, and
  recorded durably, so an absent author finds it later.
- **FR-032a**: "Durable" MUST be given a lifetime. No retention, purge or cleanup mechanism was
  found in the job framework, so run records appear to accumulate indefinitely; the promise in
  FR-032 currently rests on that absence rather than on a decision.
  **Decided**: this feature relies on whatever retention the job framework provides and adds none of
  its own. Outcomes persist as long as run records do. Defining a retention window is a job-framework
  concern that would arrive here wearing this feature's clothes, and it applies equally to every
  queue consumer.
- **FR-033**: The notification's wording MUST reflect what happened — a clean run, a partial one and
  a cancelled one MUST read differently, and a cancelled run MUST NOT read as a fault.
- **FR-034**: Notification MUST be best-effort: a failure to deliver it MUST NOT change the run's
  recorded outcome.
- **FR-035**: The system MUST emit a distinguishable completion signal type, so a client can tell
  this run's completion from any other background work.

- **FR-035a**: The system MUST announce, **to every author who may see a folder**, that the folder
  has entered and left a delete. This is a different signal from FR-031 and does not replace or
  widen it: FR-031 carries the *outcome* — counts, per-path reasons — and stays scoped to the
  submitter, because how a run went is the submitter's business. The announcement carries only that
  a folder is being deleted, and later that it no longer is, which is everyone's business, since
  the alternative is an author working inside a folder that is being destroyed under them.
  - **Per folder, not per run.** A run covers folders whose audiences differ; one announcement
    listing all of them cannot be filtered per folder, and sending the union would tell an author
    about a folder they may not see. The existing folder-deleted event is already per folder, and
    this matches it.
  - **The audience for the "left" announcement MUST be established while the folder still exists.**
    It is derived from who may read the folder, and by the time the delete ends there is nothing
    left to derive it from.
  - **A folder's "left" announcement MUST follow that folder's deletion**, never precede it, for the
    same reason C-010 orders the completion signal: a client that unmarks and refreshes on an
    announcement arriving early would read a state the delete has not reached yet, and would show
    the folder back. The "entered" announcement is ordered the other way — it MUST precede the
    folder's deletion, since its whole purpose is to warn while there is still something to warn
    about.
  - **It does not make discovery redundant** (C-012). A run that dies never announces its end, so an
    announcement stream alone leaves folders marked indefinitely. The client's own establishment of
    the in-flight set on load is what recovers from that, and remains required.
- **FR-035b**: Announcements MUST be filtered by the recipient's own rights, not by whether they hold
  a back-end role. An author who may not read a folder MUST NOT receive its announcements at all —
  not receive and discard them.

#### Contract and documentation

- **FR-036**: The published API description MUST state that the operation is asynchronous, that the
  delete is recursive and permanent, name the queue, and point at the status, cancel and monitor
  addresses the caller uses afterwards.
- **FR-037**: The published schema MUST match what the endpoint actually returns, and the generated
  API document MUST be regenerated from the annotations and committed with the change.
- **FR-038**: Configuration this feature introduces MUST ship with documented defaults, and any
  value the client needs before submitting MUST be exposed through the existing configuration
  endpoint.

### Key Entities

- **Bulk delete submission**: the set of folder paths an author asked to delete, as one unit. Carries
  nothing else — no destination, no options.
- **Run handle**: what the submission is answered with. A run identifier plus the address at which
  the run can be followed, cancelled and read.
- **Per-path outcome**: one record per distinct submitted path — the path, whether it succeeded,
  failed or was skipped, and on failure a machine-readable reason plus a diagnostic message.
- **Run outcome**: the counts plus the per-path records, readable after the run has ended and
  durable beyond the author's session.

---

## Contract Consumed by the Client *(mandatory — the frontend half of #37063 depends on it)*

The browser-side work is delivered separately against this contract. Each item restates a
requirement above from the consumer's point of view, so the boundary is explicit and reviewable.

- **C-001**: **One call, one handle.** The client sends the selected folder paths in a single
  ordinary JSON request and is answered immediately with a run identifier and a ready-made address
  for following it (FR-001, FR-002). It never constructs that address itself, and it never sends
  content — this is not an upload, and none of bulk upload's multipart machinery applies.
- **C-002**: **Every guarantee in this contract begins at the handle.** Surviving the author
  leaving, cancellation, the durable outcome — all are properties of a *run*, and no run exists
  until the submission is answered. The submitting request itself is short and unremarkable, but if
  it dies there is nothing to recover.
- **C-003**: **A count the client should display is returned at submission** (FR-003), and it equals
  the total the final outcome reports. The client should show the server's number, not the size of
  its own selection.
- **C-004**: **Distinguishable submission refusals** for: nothing submitted, over the configured
  maximum, not entitled to the operation, and overlapping an in-flight run (FR-004, FR-005, FR-029).
  The overlap refusal is the one a normal author can actually provoke, so it needs its own copy —
  "someone is already deleting one of these folders" is actionable; a generic failure is not.
- **C-005**: **A stable, enumerated set of failure reasons**, each mapped to client copy (FR-017).
  The server's message is diagnostic and MUST NOT be displayed. Adding a reason later changes both
  halves, so the set is agreed before implementation (FR-019).
- **C-006**: **Progress, and an honest statement of what it counts** (FR-024, FR-025). It counts
  **completed top-level folders and nothing finer** — there is no visibility inside a folder to
  report. On an unbalanced selection that number says very little, so the client SHOULD render an
  **indeterminate** indicator rather than a determinate bar. Stated as part of the contract because
  the client cannot infer it from the number alone, and a bar sitting at 0% for twenty minutes reads
  as a hung feature rather than as a large folder.
- **C-007**: **A way to cancel**, with this operation's guarantee and not copy's (FR-026, FR-027).
  The client's confirmation copy MUST say that cancelling leaves each folder either fully deleted or
  untouched. #37062's wording is different and MUST NOT be reused here.
- **C-008**: **A readable terminal state and outcome** — counts plus per-path records, each failure
  carrying its reason (FR-014 … FR-017). The failing folder names are what tell an author what
  survived, so they are present in the outcome and not summarised away.
- **C-009**: **A pushed completion signal carrying the outcome**, plus a durable record of the same
  (FR-031 … FR-035). The client does not need to build a jobs screen for the outcome to survive
  navigation; it renders the push while the author is present, and the durable record is this half's
  responsibility. **Outcome here means the counts *and* the per-path records**, each failure carrying
  its reason — not a summary. Stated explicitly because summarising it as "counts" says less than
  FR-014 … FR-017 already require, and the client depends on the difference: a toast showing the
  first few failures and "and N more" has nowhere to put the rest unless the durable record holds
  them. An author who stepped away would otherwise get "9 of 12 deleted" and no way to learn which
  three survived.
- **C-010**: **The signal is emitted after the deletions are done**, so a listing refreshed on it
  finds the folders gone. Stated because the client depends on ordering it cannot observe.
- **C-011**: **The listing of in-flight runs does not mean what its name says, and the client cannot
  infer that from the name.** The generic endpoint that lists a queue's *active* runs returns every
  run in a **non-terminal** state — which includes runs that have **failed** and runs the abandonment
  sweep has marked, not only runs that are working. A folder whose delete failed therefore stays in
  that list until the framework moves it to its permanent state, so a client reading the list
  naively tells a second author that a folder is being deleted when the delete has already failed
  and the folder is perfectly usable.

  A client using that list to mark folders as in-flight MUST filter on each run's own state and keep
  only the ones that genuinely mean *in progress*. Stated here rather than left to discovery: the
  state is present in the response and the filter is one line, but the failure it prevents surfaces
  in QA as "sometimes folders stay marked forever", which reads as a client defect and is not one.
- **C-012**: **A folder's entry into and exit from a delete is announced to every author who may see
  it** (FR-035a, FR-035b), not only to the submitter. A client can therefore mark a folder that
  another author started deleting *after* the client loaded, which reading the in-flight listing once
  cannot tell it. The announcement carries the folder and nothing about how the run is going;
  outcomes stay with the submitter (C-009).

  **The announcements are ordered around the work**: a folder is announced as entering a delete
  *before* it is deleted, and as having left it *after* — the same ordering C-010 gives the
  completion signal, and for the same reason. A client may therefore refresh on the "left"
  announcement without racing the deletion.

  **It is not a replacement for reading that listing.** A run that dies never announces its exit, so
  a client relying on announcements alone would mark folders indefinitely. Establishing the in-flight
  set on load stays required — it moves from being the only mechanism to being the one that recovers
  when an announcement never arrives. Both, not either.

**Explicitly the client's own business, not specified here**: the confirmation dialog and its
wording, which permissions hide or disable the action, whether the grid and the sidebar tree refresh
separately, the single-action-at-a-time guard, and how a running job is kept discoverable across
navigation. The job-progress primitive the client uses is specified on #37166 and **must not be
built a second time**.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An author can delete a selection of N folders with one confirmation and one
  submission, where today it takes N of each.
- **SC-002**: In a selection where some folders cannot be deleted, 100% of the deletable ones are
  deleted and 100% of the refused ones appear in the outcome with a reason that distinguishes why.
- **SC-003**: Zero folders are silently dropped: the number of outcome records always equals the
  number of distinct paths the server accepted.
- **SC-004**: A folder large enough that deleting it through the shipped endpoint times out at the
  proxy completes successfully through a run — which is the reported failure this feature removes.
  The limits it does **not** remove are stated in FR-022 and measured, not guessed: the point at
  which one folder's contents exhaust the node is recorded as a known ceiling so the follow-up
  ticket has a number to work against.
- **SC-005**: Across repeated cancellation of runs at arbitrary points, zero selected folders are
  left in a partially deleted state.
- **SC-006**: An author who was not present when the run ended can determine the full outcome —
  counts and per-path reasons — without having watched it.
- **SC-007**: Submitting a selection that overlaps an in-flight run produces a refusal naming the
  conflict, and never a run whose outcome contradicts what the author can see.
- **SC-008**: The shipped single-folder delete's behaviour is unchanged, demonstrated by its
  existing coverage continuing to pass untouched.
- **SC-009**: Deleting one ordinary folder through a run takes no longer than deleting it through
  the shipped single delete, beyond the fixed cost of enqueueing.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: folder deletion in the older product surface. The recursive delete
  lives in `com.dotmarketing.portlets.folders.business` and is long-standing legacy: the whole
  subtree walk runs inside one transaction, and each folder's contents are fetched as a complete
  list before being destroyed. **Bounding that walk is a change to legacy code**, and it is the
  riskiest part of this feature — every caller of folder deletion inherits it, not just this one.
  The plan must state whether the bounded path is a new path used only by this run or a change to
  the shared one, and what that means for the shipped single delete, for site deletion, and for any
  other caller.
- **Permission semantics are stricter than "can edit"**: the shipped delete requires edit rights
  **and** permission-editing rights on the folder, refuses the system folder outright, and re-checks
  as it recurses into each subfolder. This feature MUST NOT relax that, and the client half needs to
  know which right actually gates the action so it disables the control on the same rule the server
  enforces.
- **A comment in that code says the opposite of what the code does, and it was never true.** The
  recursion carries a comment stating that sub-deletes run as the system user, on the reasoning that
  rights on the parent imply rights on the children. The recursive call passes the *submitting
  user*, so the stricter rights are re-checked at every level. This is not drift: the comment and
  the call it describes arrived in the **same commit**, in 2012 — the comment has never matched the
  line beneath it. Any analysis quoting it as the current behaviour is wrong. Correcting it is
  progressive enhancement owed while this area is open.
- **Three different permissions govern one delete, and only one of them is on the folder.** The
  folder needs edit and permission-editing rights; its subfolders and contents are *listed* under
  read rights; its contents are *destroyed* under **publish** rights, checked per contentlet, with
  a hard failure if any is missing. Nothing at folder level predicts that last one, which is why
  FR-009c exists and why a client gating its control on folder rights alone will offer an action
  that then fails.
- **A folder's contents are resolved by querying the search index, not the database** — but a
  second, database-driven pass then sweeps whatever the first pass missed out of storage directly,
  by path. Two consequences, and they pull in opposite directions:
  - **Storage converges.** Whatever survived an interrupted delete is swept on a re-run regardless
    of what the index says, so the operation is genuinely idempotent where the data lives. A folder
    is never removed while its contents remain.
  - **The search index does not.** The sweep is raw storage deletion: no rights are checked, no
    locks honoured, and **nothing records that the index still owes a removal**. The durable record
    #37276 introduced sits on the ordinary content-destroy path, which this sweep does not use — it
    is the only production caller of that mechanism. So every delete that reaches the sweep leaves
    search documents behind permanently, and a re-run does it again. See FR-009d.
- **Backward-compatibility expectations**: the single `_delete` endpoint and its request form are
  untouched (FR-006). The per-item outcome contract is shared with shipped features and MUST be
  extended additively — a new reason value is additive, a renamed field is not.
- **Known related decisions**: the job-queue framework and the shared batch outcome contract landed
  with #37131 and #37166 and are consumed here unchanged. **That framework holds no durable per-item
  state**, and nothing in this repository proposes adding it: a run records its parameters once,
  a single progress value, and a result harvested at the terminal state. Whether the framework
  *should* offer such a state is a conversation that has not happened, so this specification treats
  its absence as a fact to design around rather than as a gap something else will close — see FR-030
  and §Planning Obligations. ADR-0020 (batch permission filtering
  over per-item checks) applies to any per-path permission gate this feature adds.

---

## Out of Scope

- **The frontend half of #37063.** Specified separately; this spec's contract is its input.
- **The single-folder context menu delete.** Shipped on #35161, unchanged here (FR-006).
- **Folder copy (#37062) and folder move (#37165).** They share this feature's outcome contract and
  must not diverge from it, but neither is built here.
- **The job-progress client primitive.** Owned by #37166.
- **Bulk delete of contentlets.** A Content Drive selection can contain files as well as folders;
  contentlet deletion already runs through workflow actions and is not re-implemented here. How a
  mixed selection is presented is the client's to decide.
- **Archiving, undo, or a recycle bin for deleted folders.** Folder delete is permanent today and
  stays permanent.
- **Bounding the recursive delete** — the chunking #37063 describes as "the real work". Deferred by
  §Decisions D-013 to [#37565](https://github.com/dotCMS/core/issues/37565), filed alongside this
  specification rather than promised by it. What that ticket carries, so it is not re-derived: paging a
  folder's contents instead of loading them whole (the memory ceiling in FR-022, and the sharper of
  the two problems), and splitting the transaction (which would reopen FR-023 and is the larger,
  riskier half). The two are separable and the first is worth pricing on its own — see §Planning
  Obligations.
- **Correcting how folder deletion decides what it may touch** (D-014, FR-009b). Content the author
  cannot see is destroyed without the checks that protect content they can see. This feature does
  **not** change that: it deletes exactly as the shipped single-folder delete does. Correcting it
  means changing a path site deletion, the context menu and customer plugins all share, which is a
  different piece of work — taking it on here would change what this feature is. Recorded rather
  than deferred silently.
- **Resumability of an interrupted run.** Under FR-020 an interrupted folder rolls back, so there is
  no partial state to resume from. The question the sibling features face does not arise here.

---

## Decisions

- **D-001 — Asynchronous, not synchronous, and this is settled.** #37063 records that a synchronous
  `_bulkdelete` was proposed on 2026-08-24 and did not survive review: the reasoning is that
  designing a synchronous endpoint around an expected proxy timeout is an async design without the
  machinery. Not relitigated here.
- **D-002 — The outcome field names follow what shipped, not what the issue text says.** #37063's
  description writes `successCount` / `failCount`. What is actually in the product, from bulk upload
  and bulk refresh, is `total`, `processed`, `successCount`, `failedCount`, `skippedCount`,
  `results`. This feature uses the shipped names. A third shape would break the "three tickets
  must not diverge" rule the issue itself sets. **Recorded on the issue** as
  [a comment](https://github.com/dotCMS/core/issues/37063#issuecomment-5688852737) rather than as an
  edit to the description, so the correction is dated and attributable; the description still
  carries the original sketch and the comment says plainly that it supersedes it.
- **D-003 — A domain endpoint, not the generic job submission address.** Callers post folder paths
  to the assets API, following the precedent of content import wrapping the job queue, rather than
  being sent to a generic queue endpoint. The caller then uses the generic job addresses for status,
  cancel and monitor — submission is domain-shaped, follow-up is not.
- **D-004 — An ordinary JSON body, not multipart.** None of bulk upload's staging, bounding or
  reclaim machinery applies: this operation sends paths, not content.
- **D-005 — Cancellation is honoured between folders only, unlike copy.** A partially deleted tree
  cannot be undone, so the granularity that makes copy responsive would make delete dangerous. The
  two features deliberately differ, and the client's copy must differ with them (C-007).
- **D-006 — Two specs for #37063, one per half.** Recorded because #37063 does not say. The halves
  are delivered by different people, share only the contract in §Contract Consumed by the Client,
  and the backend half's substance — transaction bounding — is not reviewable by a frontend reader.
  Same split as #37166.
- **D-007 — Per-path permission failures are per-path, not submission failures.** A submission is
  refused only for being unusable (FR-004, FR-005) or conflicting (FR-029). Whether the author may
  delete any given folder is the run's business, because it is the only place the answer can be
  established per path without an N-folder permission sweep at submission time.
- **D-008 — All-or-nothing holds under every interruption, and needed no wording change** (FR-023).
  This went back and forth: first recorded as a cancellation-only guarantee, then withdrawn for the
  team, and finally settled by D-013 rather than on its own merits. Once the bounding is deferred,
  the shipped delete is already one transaction per folder, so the strong guarantee is free and
  #37063's original wording is literally true. The contradiction in that issue's acceptance criteria
  dissolves rather than being resolved. *(Questionnaire B4 = A, by construction.)*
- **D-009 — The shipped `_delete` endpoint stays, whatever the client does with it** (FR-006).
  Integrations keep the synchronous endpoint. Whether Content Drive's context menu stops calling it
  and submits a one-path run instead is a client decision that needs nothing from this half: FR-001
  scenario 5 already requires a one-path submission to be accepted and to run as a batch of one.
  *(Questionnaire B7.)*
- **D-010 — Behaviour on locked or in-use content is today's behaviour** (FR-021), reported per path
  with a machine-readable reason (FR-009, FR-017). This feature changes what is *reported*, never
  what is *destroyed*. **Now confirmed rather than assumed**: destroying a folder's contents calls
  `canLock` on every version of every contentlet, which throws when anything is locked by another
  author, so a single locked descendant aborts that folder's delete before anything in it is
  removed. Today's behaviour therefore already produces the all-or-nothing outcome the questionnaire
  offers as a separate option; what is missing is only the *reporting*, which this feature adds.
  *(Questionnaire B16.)*
- **D-011 — Deleting a folder destroys its contents *usually* without firing a workflow action, and
  the wording matters.** The blunt claim is not accurate: the destroy path checks each contentlet
  for a workflow action mapped to the `DESTROY` system action and, when the content type declares
  one and it is available to that contentlet, **fires it instead of destroying directly**. Most
  types declare none, so most contents die without a workflow step — but "no workflow action is
  fired" is false as an absolute, and the client's confirmation copy must not promise it. Behaviour
  unchanged either way; the alternative of forcing a workflow action per contentlet is a different
  feature. *(Questionnaire B15 — the answer is still "stands", but its premise needed correcting.)*
- **D-012 — The per-path outcome uses the shipped contract, not the shape #37063's description
  sketches.** Restated here as a decision because an external contract draft for this work was
  seeded with the issue's shape — now corrected there, and on the issue itself (see D-002). Three differences matter: `failedCount` not `failCount`;
  a three-valued status (`SUCCESS` / `FAILED` / `SKIPPED`) not a boolean, because a boolean cannot
  express the cancellation outcome FR-027 requires; and cancellation being a *status*, not an error
  code. See D-002.
- **D-013 — The recursive delete is left exactly as it is; only the multi-select is new.** #37063
  calls the bounding "the real work" and says to estimate on it. This feature deliberately does not
  do it, for three reasons. The reported defect is that multi-select delete does not exist, and a
  run delivers that plus the proxy-timeout fix. The bounding means changing code every caller of
  folder deletion shares — site deletion, the context menu, customer plugins — so it is either a
  behaviour change for all of them, which is rollback-unsafe, or a second delete path that behaves
  differently depending on how it was invoked, permanently. And as its own ticket it also improves
  the shipped single-folder delete and can be tested against scale properly, instead of arriving as
  a side effect of a Content Drive feature. **The cost of this decision is stated in FR-022 and in
  §Out of Scope, and the follow-up is filed — [#37565](https://github.com/dotCMS/core/issues/37565),
  not a promise to file one.**
  **What is deferred is not one lump, and the scope line should not be read as if it were.** It
  splits into *paging a folder's contents*, which fixes the memory ceiling — the sharper of the two
  risks — **without touching the transaction boundary**, so FR-020 and FR-023 are unaffected by it;
  and *splitting the transaction*, which is the larger, riskier half and would reopen FR-023. Both
  are deferred here deliberately, so that the cheap half is **priced rather than assumed** — the
  plan is asked to say whether it belongs in this feature after all, with an estimate in hand
  (§Planning Obligations). That is a different act from reopening this decision, and it is not a
  reason to.
  *(Questionnaire B5 = B.)*
- **D-014 — This feature deletes exactly as the shipped single-folder delete does** (FR-009b,
  FR-021). That includes the part of today's behaviour nobody would design on purpose: content the
  author cannot see is destroyed without the checks that protect content they can see. **We know it
  is there. We are not resolving it here**, because doing so is a behaviour change to a path that
  site deletion, the context menu and customer plugins all share, and that is a different piece of
  work with a different blast radius — taking it on would change what this feature is.

  So the position is a scope one rather than a judgement about the risk: **this feature matches the
  shipped delete rather than improving on it.** Nothing an author can do through bulk delete is
  something they could not already do one folder at a time. What this feature does add is frequency,
  and that is recorded here rather than left to be noticed.

  The honest consequence, stated in FR-009b: a folder reported as succeeded is true of the data and
  not of the process. The index half of the same sweep **is** being fixed separately —
  [#37599](https://github.com/dotCMS/core/issues/37599) — because that one is a bug with a contained
  fix rather than a change to how deletion decides what it may touch.
  *(Questionnaire B2 = C, which by construction closes B3 — no stricter rule is adopted, so there is
  nowhere to put it.)*
- **D-015 — Folder paths recorded with a run stay readable by any back-end user** (FR-005a). Not an
  independent judgement: the client half had already answered it twice without either side noticing
  it was the same question. Restoring in-flight marking after a reload needs those paths; showing a
  folder *another* author is deleting needs another author's. Filtering the listings by submitter
  removes the second outright, and replacing paths with an opaque handle removes both. So the only
  answer consistent with the other half is to leave the listing as it is, and to say plainly that
  this is a trade rather than an oversight. *(Questionnaire B14 = A, forced by F1 and F9.)*
- **D-016 — A folder's delete is announced to everyone who may see the folder** (FR-035a, C-012).
  Asked by the client half during joint review, and taken. The reason it is cheap is that the
  mechanism is not new and is not even new *to this method*: deleting a folder already announces
  itself to every author whose roles may read it, excluding the one who did it, and the delivery
  layer already evaluates that audience per recipient rather than trusting the client to filter.
  Volume is not an argument against it either — the existing announcement fires once per folder
  *inside the recursion*, so a tree of five hundred subfolders already emits five hundred of them,
  against which a start and an end per selected folder is noise. What this decision adds is a
  **start**, an explicit **end**, and the acceptance that both are scoped by who may read the folder
  rather than by who submitted the run. FR-031's scoping is untouched: outcomes stay private, the
  fact that a folder is busy does not.

---

## Planning Obligations

Decided in principle above, but carrying enough hidden work that the plan must confront them
explicitly rather than meet them during implementation.

- **Pricing the follow-up, and one piece of it that may belong here** (FR-022, §Out of Scope). The
  deferred work splits in two, and the plan should say so rather than treating it as one lump.
  *Paging a folder's contents* — fetching them in batches instead of loading every contentlet of a
  folder into one list — fixes the memory ceiling, which is the sharper of the two risks, and it can
  be done **without touching the transaction boundary**, so FR-020 and FR-023 survive untouched.
  *Splitting the transaction* is the larger, riskier half and reopens FR-023. If the first turns out
  to be cheap, the plan should say whether it belongs in this feature after all — that is a decision
  worth making with an estimate in hand rather than by inheriting this spec's scope line.
- **How a folder is identified, in three places that need not agree** (FR-005a, C-011). The client
  has both a path and an identifier for every folder it lists, and the server exposes both. Which
  one travels where is three separate decisions, and they have different costs:
  - **In the run's recorded parameters** — free, since nothing outside this feature reads them.
    Carrying both lets the client match its selected rows exactly instead of by string. Worth
    stating so nobody expects more of it than it gives: an identifier names the *selected* folder
    and not its subtree, so a row **beneath** a folder being deleted still has to be matched by
    path. It narrows the string matching to descendants; it does not remove it.
  - **In the submission** — a real decision, and the argument for identifiers is not convenience.
    **A path is not a stable identity.** Between the listing being rendered and the author
    confirming, a folder can be renamed or moved, and a delete resolved by path then acts on
    whatever occupies that path now. For an operation with no undo that is worth weighing against
    the cost: the shipped single-folder delete takes a path, and FR-006 keeps it that way, so a
    bulk endpoint taking only identifiers diverges from its sibling.
  - **In the per-path outcome** — constrained, and the constraint is deliberate. The shared result
    carries one identity field. A path keeps the outcome and the durable notification readable to
    an author reading them a day later; an identifier does not. If both are genuinely needed, the
    plan should carry the second alongside the results rather than widen a type that bulk upload,
    bulk refresh, folder copy and folder move all share (D-012).
- **Capturing an announcement's audience before the folder is gone** (FR-035a). The audience for a
  folder's "delete finished" announcement is derived from who may read that folder, and the folder
  does not exist by then. It has to be resolved at the start and carried, which means it is part of
  what the run records rather than something computed at the end. The plan must say where it lives
  and what happens when it cannot be resolved — an announcement nobody receives is indistinguishable
  from one never sent, and the client's marking stays until it re-establishes state on load.
- **Signalling liveness from outside a blocking call** (FR-024a). The delete does not return until a
  folder is gone, so nothing inside the run can report progress. Whatever mechanism is chosen has to
  live beside the work rather than in it, and the plan must say where it lives, what it updates, and
  how it is stopped when the run ends — including on the failure path, where a heartbeat that
  outlives its run keeps a dead job looking alive.
- **Measuring the ceiling instead of asserting it** (SC-004). FR-022 claims peak memory is bounded
  by the widest single folder. The plan should turn that into a number — the folder size at which a
  node is at risk — because the follow-up ticket needs it and because "large" is not a threshold
  anyone can act on.
- **Overlap detection at submission** (FR-029). The plan must say how in-flight runs' paths are
  discovered and compared. The job framework exposes active jobs per queue with their parameters,
  which makes this possible but not obviously cheap, and the comparison is a path-prefix one in both
  directions.

  **The race in FR-029b is a required output of the plan, not an optional one.** Two submissions
  arriving together can both pass a check neither has yet invalidated, and the guard exists precisely
  to stop the case that corrupts a tree. The plan MUST record one of: the window is closed, and how;
  or the window is accepted, with the reasoning and what the run does when it loses that race. What it
  MUST NOT do is pass over FR-029b in silence, which would leave the guard looking complete while the
  case it was built for stays open.
- **Re-queued runs and already-deleted folders** (FR-030). There is no durable mid-run state: a
  re-queued run restarts from the first path, and the folders the first attempt deleted no longer
  resolve. Reporting them as "not found" would tell an author their delete failed when it succeeded.
  The plan must either make the outcome idempotent — a folder that is already gone is a success —
  first. There is no durable per-item state in the job framework to lean on and nothing proposing
  one, so the plan must solve this with what exists rather than wait. #37166 deliberately did not carry a
  private store for this, and that decision's cost lands here.
- **Where the run gets its context** (a known trap). The worker thread has no HTTP request: anything
  the run needs from the submitting request — the user, the site context — must be captured into the
  run's parameters at submission. The plan must enumerate what is captured. Note also that a null
  parameter value stalls the *shared* processing loop, not just this queue.
- **Failure reason set** (FR-017, FR-019, C-005). The set itself is agreed — FR-019 names the four
  delete adds to the upload-flavoured ones already there. What the plan still owes is the mechanical
  half: adding those values to the shared enumeration without disturbing the features that already
  read it, and confirming each has client copy before either half is implemented. A reason that
  reaches the client with no copy renders as nothing, which is worse than an unclassified one.
- **Test coverage** (Constitution V). The plan must name which layers this feature exercises and
  which it does not, with a reason for each omission. #37063 enumerates what it expects: happy path
  over several folders, mixed partial failure, permission denied, unresolvable path and cancellation
  between folders. The "more than one chunk" case that issue also lists **no longer applies** and
  should not be written — there are no chunks (D-013). Add instead the overlap refusal, the
  re-queued-run reporting of FR-030, and a folder large enough to prove FR-023's rollback actually
  leaves it untouched. Every new integration test class must
  be registered in a suite, or it compiles, passes, and never runs.

---

## Open Decisions

**None.** Every question this specification raised is closed and recorded in §Decisions or inline at
the requirement it governs.

The last one — what a run's folder paths are visible to — was closed by the client half's own
answers rather than by a separate judgement here (D-015), which is the kind of thing that only
surfaces when both halves are reviewed together.

What remains genuinely undecided is deliberately *not* a specification question: where the liveness
signal of FR-024a lives, and what the paging of a folder's contents costs (#37565). Both are sized
at the plan phase, with an estimate in hand, and neither changes what this document requires.

---

---

## Assumptions

- A Content Drive selection submitted to this endpoint contains folders only. Files in a mixed
  selection are deleted through the existing contentlet path; reconciling two runs into one report
  is the client's problem, not this endpoint's.
- The run executes as the submitting author, not as a system user. Every permission rule that
  applies to that author deleting the folder by hand applies inside the run.
- The existing path-resolution used by the single delete is reused per path. Paths are the same
  site-qualified form that endpoint already accepts.
- The job-queue framework is used as-is. This feature adds a queue and a processor; it does not
  extend the framework, and if it turns out to need to — see FR-030 — that is a separate issue.
- Deleting a folder emits the same events it emits today. Downstream consumers of folder deletion
  see no difference between one folder deleted by this run and one deleted by the context menu.
- No new database table is introduced by this feature. The outcome lives in the job's own history,
  as bulk upload's does.
