# Feature Specification: Content Drive bulk file upload — backend

**Feature Branch**: `37166-content-drive-bulk-file-upload-multi-file-selection-uploads-only-the-first-file`

**Created**: 2026-08-31

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37166](https://github.com/dotCMS/core/issues/37166) — "Content Drive: bulk file upload (multi-file selection uploads only the first file)".

---

## Context

Content Drive already lets a user select or drag several files at once. It accepts the whole
selection, warns that "only one file will be uploaded", uploads **the first file**, and silently
discards the rest. Every layer above the upload request already carries the full set — what is
missing is a way to submit a batch, and anything on the server able to receive one.

**This specification covers the server side only.** The browser-side work of #37166 — removing
the "not supported" stub, showing batch progress, and reporting the outcome in the interface — is
delivered separately by another developer. The two halves meet at the contract this spec defines
(§Contract Consumed by the Client), which is therefore a mandatory, not incidental, output.

The same contract is reused by the folder copy and bulk delete work
([#37062](https://github.com/dotCMS/core/issues/37062) /
[#37063](https://github.com/dotCMS/core/issues/37063)). Neither of those is built yet, so **this
feature generalizes the outcome shape the bulk refresh work already ships and they consume it**
(decision recorded in §Decisions, Q1 — note this reverses the dependency direction #37166 states,
which is why that issue needs amending).

---

## User Scenarios & Testing *(mandatory)*

<!--
  These are the server's behavior, described from the perspective of the author whose action
  reaches it. Each is verifiable against the server alone — by integration/Postman tests — with
  no browser involved.
-->

### User Story 1 - A batch of files is accepted and every file is created (Priority: P1)

An author's multi-file selection reaches the server as one batch naming a target folder and one
upload type. The server accepts it immediately, creates every file in that folder, and records
the outcome of the run.

**Why this priority**: This is the reported gap, and nothing else in the feature has value until
the batch actually uploads. Today the product silently drops user data — the author believes 30
files were uploaded when 29 were discarded.

**Independent Test**: Submit a batch of 10 valid files against a folder the author can add
children to; confirm the submission is accepted at once, and that all 10 exist in that folder
when the run reports itself finished.

**Acceptance Scenarios**:

1. **Given** a valid batch of 10 files and a folder the author may add children to, **When** the
   batch is submitted, **Then** the server accepts it immediately with a handle the caller can
   use to follow and cancel the run — it does not hold the caller until the files are created.
2. **Given** an accepted batch of 10 files, **When** the run finishes, **Then** all 10 files
   exist in the target folder.
3. **Given** any file in a batch, **When** it is created, **Then** the resulting content is
   indistinguishable from uploading that same file through the existing single-file upload — same
   resolved content type, same permissions applied, same workflow behavior.
4. **Given** a batch submitted at a site root rather than inside a folder, **When** the run
   finishes, **Then** the files land on that site, matching existing single-file behavior.
5. **Given** the existing single-file upload, **When** this feature ships, **Then** its endpoint
   and behavior are unchanged.

---

### User Story 2 - A failing file does not take the batch down with it (Priority: P1)

A batch routinely contains a file that is too large, has a disallowed file type, or collides with an
existing name. The run continues past it and records, per file, what happened and why.

**Why this priority**: Partial failure is the normal case at these batch sizes, not an edge case.
A run that aborts at the first bad file, or records only "upload failed", leaves the author unable
to tell what landed — barely better than today.

**Independent Test**: Submit 5 files of which 2 are invalid (one over the size limit, one with a
disallowed file type); confirm the 3 valid files are created and the recorded outcome reports 3
succeeded / 2 failed, naming both failures with a distinguishable reason.

**Acceptance Scenarios**:

1. **Given** a batch containing a file that fails validation, **When** the run proceeds, **Then**
   every remaining file is still attempted.
2. **Given** a finished run, **When** its outcome is read, **Then** it carries a success count, a
   failure count, a skipped count, and a per-file record.
3. **Given** a file that failed, **When** its record is read, **Then** it names the file, marks it
   failed, and carries both a machine-readable reason and a human-readable message.
4. **Given** a batch where every file fails, **When** the run finishes, **Then** the outcome shows
   zero successes and names every failure — the run is not recorded as a success.
5. **Given** a file its content type would reject for size, **When** the run reaches it, **Then**
   that file is recorded as failed with a size-specific reason and the batch continues — and the
   same file uploaded alone is rejected too, for the same reason.
6. **Given** a file whose name collides with an existing asset in the target folder, **When** the
   run reaches it, **Then** it is recorded as that file's failure — never a silent success and
   never a silent overwrite.
7. **Given** a content type that declares no size ceiling, and a file larger than the configurable
   fallback, **When** the run reaches it, **Then** that file is recorded as failed for size and the
   batch continues — **and the same file uploaded alone succeeds**. This is the one place the
   feature knowingly diverges from single-file behavior (FR-011a); it has a scenario precisely
   because it is the case most likely to be reported as a defect by whoever meets it first.

---

### User Story 3 - The author is told the batch finished, even if they walked away (Priority: P1)

The submission is answered long before the work is done, so something has to close the loop. When
the run reaches a terminal state the submitter is told — pushed to them if they are still looking,
and recorded durably so the outcome survives navigating away or closing the tab.

**Why this priority**: P1, not P2. Without it the author has no way to learn how a batch ended
except by staring at the screen for its duration, which defeats the point of accepting the batch
asynchronously. The product already has this exact mechanism in use (§Legacy Considerations), so
this is reuse rather than new invention.

**Independent Test**: Submit a batch as a given user, let it finish with no client listening, and
confirm that user has a durable record of the outcome afterwards, worded on what actually
happened.

**Acceptance Scenarios**:

1. **Given** a batch that reaches any terminal state — finished, cancelled, or permanently failed
   — **When** it does, **Then** the submitting author is notified of the outcome with its counts.
2. **Given** a batch that finished while the author was on another screen or had closed the tab,
   **When** the author next looks, **Then** the outcome is still available to them.
3. **Given** a finished batch, **When** the author is notified, **Then** the wording reflects what
   happened — full success, partial failure, total failure, or cancellation — and is not a fixed
   "completed" message.
4. **Given** a batch, **When** it is notified, **Then** only its submitting author is notified —
   not every administrator, and not other users.
5. **Given** a notification that cannot be delivered, **When** that happens, **Then** it is logged
   and the run's own outcome is unaffected — the work has already succeeded by that point.

---

### User Story 4 - A running batch can be followed and cancelled (Priority: P2)

While a batch runs, its progress can be read, and it can be stopped. Stopping it leaves the files
already created in place and records how far it got.

**Why this priority**: The feature is correct without it — every file uploads and the outcome is
reported — but a 200-file batch with no visible progress and no stop button is the difference
between usable and merely correct. It also feeds the client-side progress capability #37166 asks
this work to enable.

**Independent Test**: Submit a batch large enough to observe more than one progress change, read
progress mid-run, cancel it, and confirm already-created files remain and the outcome states the
point reached.

**Acceptance Scenarios**:

1. **Given** a running batch, **When** its progress is read, **Then** it advances as files are
   completed.
2. **Given** a running batch, **When** it is cancelled, **Then** files already created stay in
   place — nothing is rolled back.
3. **Given** a cancelled batch, **When** its outcome is read, **Then** files never attempted are
   distinguishable from files that failed, and the outcome states the run was cancelled.
4. **Given** a running batch, **When** cancellation is requested, **Then** it takes effect between
   files — a file is never left half-created.

---

### User Story 5 - An interrupted batch finishes without duplicating anything (Priority: P1)

A restart, a crash, or a lost worker does not cost the author their batch and does not leave them
with two copies of half of it. The run picks up where it stopped, and the author is told once, at
the end, what happened overall.

**Why this priority**: P1, and it is a data-integrity requirement rather than a convenience. The
job framework re-queues a run whose worker stopped reporting, so "what happens on a restart" is
already answered — it is retried. Retrying a batch that created 30 of 50 files, without resuming,
creates 30 duplicate assets. Silently duplicating a user's content is worse than the defect this
feature exists to fix.

**Independent Test**: Start a batch of 20 files, stop the run once part of it has been created,
let it be picked up again, and confirm the folder holds exactly 20 files — not more — and that the
outcome reports 20 succeeded, once.

**Acceptance Scenarios**:

1. **Given** a run that created some of its files and was then interrupted, **When** it resumes,
   **Then** it creates only the files it had not yet created.
2. **Given** a resumed run, **When** it finishes, **Then** the target holds exactly one copy of
   each submitted file.
3. **Given** a resumed run, **When** its outcome is read, **Then** the counts cover the whole
   batch across every attempt, and files created before the interruption read as succeeded rather
   than skipped.
4. **Given** a batch interrupted and resumed, **When** it finishes, **Then** the submitter is
   notified **once**, not once per attempt.
5. **Given** a client that lost the connection while submitting and resubmits the same batch,
   **When** the resubmission is handled, **Then** the author does not end up with two copies of
   the files — either the resubmission is recognised as the same batch, or the duplicates are
   rejected as collisions.
6. **Given** a resubmission resolved by the collision branch, **When** its outcome is read, **Then**
   it is distinguishable from a batch whose files genuinely all collided (FR-040a) — so a
   successful retry is never reported to the author as a total failure.

---

### User Story 6 - Two batches racing for the same name produce one file, not two (Priority: P2)

Two authors, or one author twice, upload a file of the same name into the same folder at the same
time. One wins, the other is told its file collided. Neither silently overwrites the other, and the
folder never ends up with two files of the same name.

**Why this priority**: P2 — it needs a deliberate answer, but it is a narrower window than User
Story 5's, and the feature is usable before it is closed. Recorded rather than left implicit
because the product's existing collision rule is a check followed by a create, so under
concurrency it can pass for both runs.

**Independent Test**: Start two batches at the same time, each containing a file of the same name
targeting the same folder, and confirm the folder ends with exactly one such file and the other
run reports a collision failure for it.

**Acceptance Scenarios**:

1. **Given** two runs that would create the same file name in the same target, **When** both
   proceed, **Then** exactly one succeeds and the other records a collision failure for that file.
2. **Given** the losing run, **When** its outcome is read, **Then** only the contended file
   failed — its other files were created normally.
3. **Given** two runs targeting **different** folders, **When** both proceed, **Then** neither
   waits on the other.

---

### Edge Cases

- **Upload type not decided by the author.** Resolved before the batch is ever submitted: the
  target folder either expresses a preferred upload type or expresses none, in which case the
  interface prompts once for the whole batch. The server receives exactly one type per batch and
  never has to infer one. No server behavior required.
- **A requested upload type that is neither Asset nor File.** Refused at submission, before any
  batch is created. It is a batch-level parameter, so it cannot be a per-file failure.
- **Zero files in the submission.** Refused at submission; no batch is created.
- **More files than the configured maximum.** Refused at submission with a message stating the
  limit; no partial batch is created.
- **A file over the configured per-file size limit, inside an otherwise valid batch.** That file
  fails; the batch does not.
- **The author cannot add children to the target folder.** Refused at submission as a permission
  error — not as N per-file failures.
- **A file collides with an existing asset the author cannot edit, or resolves to a content type
  the author lacks permission to create.** The target-folder check above (FR-003) already passed;
  this is a narrower permission check that only resolves once the file reaches the reused
  single-file path (FR-006). Recorded as that file's own permission-denied failure (FR-016), not
  as a submission-time refusal.
- **The target folder does not exist.** Refused at submission. Creating folders is out of scope.
- **Two files in one batch carrying the same name.** Each is attempted; the second is subject to
  the same collision rule as a pre-existing name.
- **The run is cancelled before reaching some files.** Those files are recorded as skipped, which
  is a distinct outcome from failed.
- **The submitting user cannot be resolved when the run finishes.** Logged; the run's outcome is
  unaffected.
- **The server restarts while a batch is running.** The run resumes and creates only what it had
  not yet created (FR-036). It does not restart from the first file, and it does not orphan.
- **The submission crosses the file-count or total-size ceiling part-way through being read.** The
  request is aborted at the ceiling and refused; whatever was staged before the abort is reclaimed
  (FR-010a, FR-013d). No batch is created, and no more than the ceiling ever reaches disk.
- **The request arrives from another origin, or while the staging layer is switched off.** Refused
  (FR-003a). Neither is inherited from the staging API this endpoint calls underneath, so both are
  this endpoint's own responsibility.
- **The connection drops before the client learns whether its submission was accepted.** Covered by
  FR-040: the client can safely resubmit, and either gets the original batch back or sees the
  already-created files rejected as collisions. What it must never get is a silent second copy.
- **Two batches racing for the same name in the same folder.** Exactly one wins; the other records
  a collision for that file only (FR-041, FR-042).
- **The batch exceeds the configured total size.** Refused at submission, by accumulating the
  measured size of each file as it is staged and aborting the read at the ceiling (FR-013b,
  FR-010a). Distinct from a single file exceeding the per-file ceiling, which is a per-file failure
  inside an accepted batch.
- **A file's content type declares no size ceiling.** The configurable fallback applies (FR-011.2),
  which is deliberately stricter than the single-file upload for that file — see FR-011a.

---

## Requirements *(mandatory)*

### Functional Requirements

**Submission**

- **FR-001**: The system MUST accept a batch in **one submission carrying the files themselves**
  (Q5), together with a target folder (or site root) and one upload type for the whole batch. The
  submission places the content on the product's file-staging layer as it reads it, and the batch
  then runs against what was staged. The client never handles a staging reference.
- **FR-002**: The system MUST answer a valid submission immediately, before the files are created,
  with a handle the caller can use to follow, cancel, and read the outcome of the run.
- **FR-003**: The system MUST validate at submission — and refuse before creating any batch — a
  submission that: carries no files; carries an upload type other than Asset or File; names a
  target that does not exist; exceeds the configured maximum file count; exceeds the configured
  batch total size (FR-013b); or comes from an author without permission to add children to the
  target.
- **FR-003a**: Because this endpoint accepts file content directly rather than routing the author
  through the staging layer's own REST resource, it MUST apply the request-level controls that
  resource applies and that are **not** inherited by calling the staging API underneath: a
  **same-origin check** on the request, and **honouring the staging layer's enabled/disabled
  switch**. Neither is provided by the staging API, and no global filter supplies them — the
  product's referer interceptor protects a fixed list of non-API paths and does not cover `/api/`.
  Both MUST be covered by tests (SC-012). Recorded as a requirement rather than left to the plan
  because the comparable existing endpoint (content import) omits both, so following the precedent
  uncritically would reproduce the gap.
- **FR-004**: A submission refused for lack of permission MUST be distinguishable from one refused
  as malformed.

**Creating the files**

- **FR-005**: The system MUST create every file in the batch in the target folder.
- **FR-006**: Each file MUST be created **observably equivalently** to uploading that same file
  through the existing single-file upload: identical content type resolution, identical permission
  enforcement, identical workflow behavior. The requirement is the equivalence, not the sharing of
  a particular call site — the single-file path is entered over REST and a background run cannot
  re-enter it, so the reuse point is chosen in the plan. What may not vary is the observable
  behavior.
- **FR-007**: A file failing MUST NOT abort the run; every remaining file MUST still be attempted.
- **FR-008**: The system MUST NOT wait for each file to become searchable before starting the
  next. Search-index visibility MUST be resolved for the batch, not serialized per file.
- **FR-008a**: Concretely: each file MUST be created with the **deferred** index policy, and the
  run MUST resolve search-index visibility **once, at the end of the batch, before the completion
  signal is emitted**. Both halves are load-bearing and both are stated here rather than left to
  the plan. The per-file wait-for policy the current one-at-a-time path uses does not merely block
  on an index refresh — it also flushes the system-wide query cache on every file, so a full
  batch charges every other user one cache flush per file. And deferring **alone is not sufficient**: the
  deferred policy only enqueues for reindexing and returns, so a run that reported "finished" would
  hand the author files that a filtered or text search cannot yet find. The batch-level resolution
  is what keeps "finished" meaning "visible".
- **FR-009**: The existing single-file upload endpoint and its behavior MUST be left unchanged.

**Limits**

- **FR-010**: The system MUST enforce a configurable maximum number of files per batch, defaulting
  to **100**. Exceeding it is a submission-time refusal (FR-003). This limit is new — nothing caps
  a selection today — and MUST be operator-configurable through the product's standard configuration
  mechanism rather than hardcoded.
- **FR-010a**: The file-count and batch-total ceilings MUST be enforced **while the request body is
  read**, aborting as soon as either is crossed, rather than after the whole body has been
  received. A submission that exceeds them therefore cannot commit more than the ceiling to disk.
  This is the enforcement point precisely because nothing below it provides one: the staging layer
  caps neither the number of files nor the size of a request, and the servlet container is
  configured with no multipart size limit, so this endpoint is the first and only bound in the
  path.
- **FR-011**: A file rejected for its **size** MUST be recorded as that file's own failure with a
  size-specific reason, and MUST NOT fail the batch. The applicable ceiling is resolved in this
  order:
  1. **The content type's own ceiling**, where an operator has declared one on the binary field.
     This is the rule the single-file upload already applies, so where it is set, a file is
     accepted or rejected identically whether it arrives alone or in a batch.
  2. **A configurable fallback ceiling**, where the content type declares none. Without it a
     batch would have no per-file bound at all, since the content type's rule is unset by default.
- **FR-011a**: The fallback of FR-011.2 is a **deliberate, recorded exception to FR-006**. Where a
  content type declares no ceiling, a file large enough to exceed the fallback is rejected in a
  batch and accepted as a single upload. This is accepted because a batch is a materially larger
  resource commitment than one upload, and because the alternative — no bound at all — was judged
  worse. The divergence MUST be documented wherever the limits are documented, so an operator
  seeing it does not read it as a defect.
- **FR-012**: A file rejected for its **type** MUST likewise be recorded as that file's own
  failure and MUST NOT fail the batch. As with size, the allowed-types rule is declared per content
  type on the binary field and is applied as-is.
- **FR-012a**: This is a **file-type rule, not a file-extension rule**, and MUST be described that
  way everywhere it is surfaced. The product resolves the file's media type — by detection and
  content sniffing, not by trusting the name — and matches it against the content type's allow
  list, which is written in media-type terms and supports wildcards. Renaming a file to change its
  extension therefore does not get it past the rule.
- **FR-012b**: Where the media type **cannot be resolved**, the product skips the allow-list check
  and accepts the file. This feature inherits that behavior rather than tightening it, since
  tightening would break FR-006. It MUST NOT be described to the author as though every file were
  checked, and the plan MUST decide whether an unresolvable type is worth surfacing at all.
- **FR-013**: Sizes MUST be taken from what the staging layer **measured** when it received the
  content, never from a figure the caller declares. A caller can under-declare or omit a declared
  size; a measured one is a fact.
- **FR-013a**: Where the staging layer imposes its own size ceiling, the effective per-file limit is
  the stricter of it and FR-011's, and the plan MUST state which is expected to bind — so that a
  file the staging layer refused is not reported to the author as a content-type rule it did not
  break.
- **FR-013b**: The system MUST enforce a configurable **maximum total size per batch**, refused at
  submission (FR-003) by accumulating the measured size of each file as it is staged. Without it
  the batch is bounded only by FR-010's file count multiplied by a per-file ceiling that may be
  unset, which is to say not bounded at all — an authenticated author could stage unbounded bytes
  on shared storage.
- **FR-013c**: Because the submission carries the content (Q5), the total is not known before the
  body is read. Enforcement is therefore **authoritative while reading** (FR-010a): the sum
  accumulates as each file is staged and the request is aborted the moment the ceiling is crossed,
  so no batch is created and no more than the ceiling reaches disk. Where a caller declares a total
  up front it MAY be used for a fast refusal before reading — a convenience that saves an author
  uploading gigabytes only to be refused — but it is never the enforcement point, since a caller
  can under-declare or omit it.
- **FR-013d**: Content already staged when a submission is refused — by FR-013c or by any other
  submission-time refusal — MUST be reclaimed. FR-033 covers reclaim for runs that reach a terminal
  state; a refused submission never becomes a run and would otherwise leak bytes the author cannot
  see and no run will ever clean up.

**Outcome**

- **FR-014**: A finished run MUST record a total, a success count, a failure count, and a skipped
  count.
- **FR-015**: A finished run MUST record a per-file result naming the file and its status —
  succeeded, failed, or skipped (never attempted).
- **FR-016**: Every failed per-file result MUST carry a machine-readable reason and a
  human-readable message. The reason MUST distinguish at least: over size limit,
  disallowed file type (FR-012a — a media-type rule, not an extension one), name collision,
  permission denied (a per-file check narrower than the target-folder check in FR-003 — see Edge
  Cases), staged content unavailable (FR-032), and an unclassified error.
- **FR-016a**: The **reason** is what the client presents, by mapping it to resolved product copy.
  The **message** is diagnostic and log-only: it MUST NOT be the text shown to the author, which
  the frontend spec's FR-030 already requires. Every reason a failure can carry MUST therefore
  have client copy, or the client will have nothing to show for it — so adding a new reason later
  is a change to both halves, not just this one.
- **FR-017**: The recorded counts MUST be the authoritative report of the run. The number of files
  submitted MUST NOT be used as a stand-in for the number created.
- **FR-018**: This feature MUST **generalize the batch-outcome shape already established by bulk
  refresh** (#36845 / #37131) into a shared contract, rather than defining a second one. Three
  deltas are required and are real work:
  - **A machine-readable reason code.** The shipped per-item record carries a human-readable
    message only; FR-016 needs both. This is an additive change to a shipped contract.
  - **A generic item key.** The shipped record is keyed by contentlet identifier and inodes. A file
    being uploaded has neither — it does not exist until the run creates it — and neither does a
    folder path. The generic key is the substance of this requirement.
  - **One spelling of the counters: `failedCount`.** The shipped counter is `failedCount`; #37166
    originally said `failCount`. `failedCount` wins, matching what already ships. Both halves MUST
    use it; this is settled here and in #37166's amendment, not left to the plan.
- **FR-018a**: The generalized shape MUST carry a batch of folder paths as naturally as a batch of
  files, so #37062 / #37063 adopt it unchanged.

**Notifying the submitter**

- **FR-019**: When a run reaches any terminal state — finished, cancelled, or permanently failed —
  the system MUST notify the submitting author of the outcome and its counts.
- **FR-020**: The notification MUST be durable: an author who was not looking when the run
  finished MUST still be able to learn the outcome afterwards. "The outcome" is the recorded
  outcome of FR-014 … FR-016, counts **and** the per-file results with their reasons, not a
  counts-only summary. An author who stepped away would otherwise learn that three files failed
  without learning which three, leaving them nothing to act on.
- **FR-021**: The notification MUST be addressed to the submitting author only.
- **FR-022**: The notification wording MUST reflect the actual outcome — full success, partial
  failure, total failure, or cancellation — rather than a single fixed message.
- **FR-023**: Notification MUST be best-effort: a failure to notify MUST be logged and MUST NOT
  affect the run's recorded outcome or the batch machinery.

**Progress and cancellation**

- **FR-024**: The system MUST report the run's progress as files complete, readable while the run
  is in flight.
- **FR-025**: A running batch MUST be cancellable.
- **FR-026**: Cancelling MUST leave files already created in place — nothing is rolled back.
- **FR-027**: Cancellation MUST take effect between files, never mid-file.
- **FR-028**: A cancelled run's outcome MUST distinguish files never attempted from files that
  failed.

**Documentation**

- **FR-029**: The published API description MUST state that the operation is asynchronous, and
  point at how to follow, cancel and read the outcome of the run.
- **FR-030**: The generated API document MUST be regenerated from the annotations and committed
  alongside the change.

**Staged file content**

<!--
  The submission is answered before the files are created, so the uploaded bytes must live
  somewhere in between. That gap was unspecified and is where this feature can lose a user's
  files. These requirements bound it.
-->

- **FR-031**: The uploaded content of an accepted batch MUST remain available to the run until the
  run reaches a terminal state, however long the batch waits in queue and however long it takes.
- **FR-032**: A batch MUST NOT lose files because their staged content expired or was reclaimed
  while the batch waited or ran. If content cannot be retrieved, it MUST be recorded as that
  file's own failure with its own distinguishable reason — never a silent loss, and never
  presented as though the author supplied a bad file.
- **FR-033**: Staged content MUST be reclaimed once the run reaches any terminal state —
  completed, failed, or cancelled — including for files the run never reached.
- **FR-034**: Staged content MUST remain readable to the run when it is picked up by a different
  node than the one that accepted the submission.

<!--
  FR-035 was withdrawn: it required the system to "state a position" on cross-batch staged bytes,
  which is a decision for the authors rather than behaviour a test can fail. It now lives in
  §Planning Obligations. The number is left unused rather than reassigned, so references to it in
  the review threads keep resolving.
-->

**Surviving interruption, and not duplicating**

<!--
  The job framework re-queues a run whose worker stopped updating it. Re-running a batch from the
  start would recreate files it already created, so "it gets retried" is only a safe answer if the
  run can resume. These requirements make that the contract rather than an implementation detail.
-->

- **FR-036**: A run interrupted by a restart, a crash, or a lost worker MUST resume rather than
  restart. Files it already created MUST NOT be created a second time.
- **FR-037**: To make FR-036 possible, the run MUST record which files it has completed **durably
  and as it goes**, not only in the final outcome. A record written only at the end is lost in
  exactly the case it is needed for.
- **FR-038**: A resumed run MUST produce the same outcome shape as an uninterrupted one — the
  counts MUST reflect the whole batch across all attempts, not just the final attempt, and a file
  created before the interruption MUST be reported as succeeded rather than skipped.
- **FR-039**: A resumed run MUST still honour cancellation and MUST still notify the submitter on
  its terminal state, once for the batch — an interruption MUST NOT produce a second notification
  for the same batch.
- **FR-040**: Resubmitting a batch after a lost or uncertain response MUST NOT silently create
  duplicates. The system MUST either recognise the resubmission as the same batch and return the
  original handle, or let the per-file collision rule (FR-041) reject the already-created files —
  and whichever it does MUST be stated, because a client cannot know whether its submission
  landed when the connection drops.
- **FR-040a**: If the collision branch is chosen, a duplicate resubmission MUST be **distinguishable
  from a genuinely all-collided batch** — by a reason, a flag on the outcome, or both. The two
  branches protect the author's data equally but do not report equally: under the collision branch
  a resubmission re-uploads every byte, produces a second handle, and reports *"50 of 50 failed —
  file already exists"* for a batch that in fact succeeded. Without a way to tell that apart, the
  client cannot honour its own requirement to treat an uncertain submission as retryable rather
  than as a failure the author must reason about, which is the whole point of FR-040.

**Concurrency**

- **FR-041**: Where two runs would create the same file name in the same target, **exactly one MUST
  succeed**. The other MUST be recorded as that file's own collision failure. Two files with the
  same name in one folder, and a silent overwrite, are both unacceptable outcomes.
- **FR-042**: FR-041 MUST hold when the two runs overlap in time. **The storage layer already
  guarantees it**: a unique index over the lower-cased full path per host (the same one FR-042a
  names) rejects the second writer of a contended name on every existing install. The product's own pre-check is
  a check followed by a create and can pass for both runs, so it is a courtesy that produces a
  clean message, not the guarantee. The requirement is therefore to **catch the constraint
  violation and report it as the same collision failure as the pre-check** — not to build a lock
  for a race the storage layer already loses on the caller's behalf.
- **FR-042a**: "The same file name" is **case-insensitive**. `Report.pdf` and `report.pdf` are one
  contended name, not two. This is existing product behavior rather than a new decision: the
  operative uniqueness guarantee is a unique index over the lower-cased full path per host, and the
  pre-check resolves the target through the same lower-cased path, so the two agree. This feature
  MUST NOT introduce a case-sensitive notion of collision alongside them.
- **FR-043**: A run MUST NOT be blocked or slowed by unrelated runs on other targets. FR-042's
  mechanism satisfies this for free — a uniqueness constraint contends only on the contended key —
  and any additional mechanism MUST preserve it rather than serialize uploads generally.

### Key Entities

- **Upload batch**: One author-initiated bulk upload. Holds the target (folder or site root), the
  chosen upload type, the submitting author, the set of files, its progress, its terminal state,
  and its outcome. Addressable and observable after the submitting caller is gone.
- **Batch outcome**: Total, success count, failure count, skipped count, and the list of per-file
  results. The shared contract of FR-018.
- **Per-file result**: A file's name, its status (succeeded / failed / skipped), and — when failed
  — a machine-readable reason and a human-readable message.
- **Batch limits**: Operator-configured maximum file count per batch, and maximum size per file.

---

## Contract Consumed by the Client *(mandatory — the frontend half of #37166 depends on it)*

The browser-side work is delivered by another developer against this contract. It is listed here
so the boundary is explicit and reviewable; each item is a restatement of a requirement above,
from the consumer's point of view.

- **C-001**: **One call** (Q5). The client sends the files, the target and the upload type in a
  single multipart request and is answered immediately with a handle (FR-001, FR-002). It never
  handles a staging reference — where the bytes wait for the run is the server's business and can
  change without touching the client.
- **C-001a**: What the one-call shape costs the client, recorded so it is a decision rather than a
  surprise: **per-file upload progress** and **retry of a single file** that failed to upload. A
  connection dropped part-way through means resending the batch, not the one file that failed. The
  client MAY reduce the blast radius by submitting smaller batches; nothing on this half prevents
  it.
- **C-001b**: What the one-call shape buys the client: the staging clock starts on the **server**,
  immediately before the batch is created, so no submission can carry content that has already
  expired and there is no window in which an author sits on a confirmation screen while their
  earliest files age out.
- **C-002**: A distinguishable submission refusal for: no files, bad upload type, missing target,
  too many files, batch over the total size ceiling, permission denied (FR-003, FR-004, FR-013b).
- **C-002a**: A defined answer to "I lost the connection while submitting — may I retry?" The
  client MUST be able to resubmit without risking a second copy of the author's files (FR-040).
  Which mechanism provides that — the same batch handle returned, or the duplicates rejected as
  collisions — is fixed in the plan, but the client is entitled to one. **If the plan takes the
  collision branch, a duplicate resubmission MUST be distinguishable from a genuinely all-collided
  batch** (FR-040a); without that the client is handed "every file failed" for a batch that
  succeeded, and cannot keep its own promise that retrying is safe.
- **C-002b**: A stable set of failure reasons, each mapped to product copy on the client side. The
  server's message is diagnostic and is not displayed (FR-016a). Adding a reason later changes both
  halves.
- **C-003**: Readable progress for an in-flight run (FR-024).
- **C-004**: A way to cancel an in-flight run (FR-025).
- **C-005**: A readable terminal state and outcome — counts plus per-file results with reason and
  message (FR-014 … FR-017).
- **C-006**: A pushed completion signal carrying the run's outcome, reaching the browser without
  the client having to poll, plus a durable record of that same outcome (FR-019, FR-020).
  **Outcome here means counts *and* the per-file results**, each failure carrying its reason: that
  is what FR-014 … FR-016 record, and summarising it as "counts" says less than this spec already
  requires. The client's FR-023 depends on the failing file names being present in both, since
  those names are what tell an author which files to choose again.

**Not blocking on the client**: FR-019/FR-020 mean the frontend does **not** need to build a jobs
management screen for the outcome to survive navigation. It needs only to render the pushed signal
while the user is present; the durable record is the server's responsibility.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Submitting N valid files (within the configured maximum) results in N files created
  in the target; zero files are silently discarded.
- **SC-002**: In a batch where some files are invalid, 100% of the valid files are created and
  100% of the invalid ones appear in the outcome with a reason that distinguishes why.
- **SC-003**: A batch at the configured maximum sustains a stated per-file throughput floor, set in the plan as
  an absolute target rather than as a comparison against today's behavior — today's single-file
  upload waits for each file to become searchable, which is the very pathology FR-008 removes, so
  measuring against it would make this criterion true by construction. The batch also does not
  serialize behind a per-file search-index wait — and, once the run reports finished, every created
  file is findable by search, not merely present in the folder (FR-008a).
- **SC-004**: An author who was not present when their batch finished can determine the outcome
  afterwards, in 100% of runs — including cancelled and failed ones.
- **SC-005**: Cancelling a run leaves every already-created file intact and reports the point
  reached, in 100% of runs.
- **SC-006**: The generalized outcome shape is the shipped bulk refresh shape extended, not a
  second one — verified by the shipped consumer still reading its own results through it, and by
  expressing a folder-path batch in it during review so #37062 / #37063 can adopt it unmodified.
- **SC-007**: Existing single-file upload behavior is unchanged, verified by the existing
  single-file scenarios passing without modification.
- **SC-008**: A submission that must be refused is refused before any work is done — no batch is
  created and no file is created — in 100% of refusals, including the batch-total refusal. Where
  the refusal depends on the size of what is being sent (FR-013b), it is decided **while the body
  is read** and the request aborted at the ceiling, so no submission can commit more than the
  ceiling to disk (FR-010a) and anything staged before the abort is reclaimed (FR-013d).
- **SC-009**: A batch interrupted at any point and resumed produces exactly one copy of each
  submitted file — zero duplicates, in 100% of interruptions — and notifies the submitter once.
- **SC-010**: Two runs contending for the same file name in the same target produce exactly one
  file, in 100% of races, with the loser reporting a collision for that file only.
- **SC-011**: No batch can commit more staged bytes than the configured total, and no file can
  exceed the ceiling that applies to it, in 100% of submissions.
- **SC-012**: A submission arriving from another origin is refused, and a submission arriving while
  the staging layer is disabled is refused — each verified by its own test (FR-003a). These are
  stated as criteria rather than left implicit because the comparable shipped endpoint omits both.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content Drive's file upload (a modern surface) and the
  asset-creation and permission machinery beneath it, which is long-standing product surface and
  is also used by the AssetPicker ([#36702](https://github.com/dotCMS/core/issues/36702)). This
  feature introduces a new submission surface but deliberately does **not** introduce a second way
  to create an asset: FR-006 binds it to behavior observably equivalent to the single-file upload,
  with the reuse point chosen in the plan.
- **Security posture inherited by reuse**: whichever staging mechanism the plan chooses, the
  product's existing one already contains the caller-supplied filename against path traversal.
  Reusing it inherits that protection; not reusing it means re-establishing it. Worth stating
  because it is a real argument in FR-006's favour that would otherwise go unrecorded.
- **Backward-compatibility expectations**: The existing single-file upload stays exactly as it is
  (FR-009); the AssetPicker and any external caller keep working unchanged. This feature adds a
  bulk path beside it and replaces nothing. No existing content, API, or admin workflow changes.
- **Known related decisions — an in-repo precedent this MUST follow**: the bulk refresh work
  ([#36845](https://github.com/dotCMS/core/issues/36845), shipped in
  [#37131](https://github.com/dotCMS/core/pull/37131)) already solved the same shape of problem:
  an accepted-immediately batch, a per-item outcome with counts, a configurable submission cap, a
  server-side permission gate, and — the part that answers FR-019/FR-020 — a completion listener
  that both pushes the outcome to the submitter and records it durably so it survives navigation.
  This feature adopts that pattern rather than inventing a parallel one, and where the two differ
  the divergence is justified in the plan. Formal ADR consultation happens in the plan phase.

---

## Out of Scope

- **The browser-side implementation of #37166.** Delivered separately against §Contract Consumed
  by the Client. This spec's deliverable stops at the server.
- **Directory (folder tree) upload.** Dropping a desktop folder, creating the hierarchy, and
  placing assets inside it. Explicitly deferred by #37166 to a follow-up. This feature requires
  the target to already exist (FR-003).
- **Creating a missing target folder as part of an upload**, and any interface for choosing that
  folder's behavior mid-upload. Follows from the line above.
- **Resumable or chunked single-file upload.** Large-single-file behavior is unchanged.
- **Mixing upload types within one batch.** One batch, one type (FR-001).
- **Changing the single-file upload endpoint or its behavior.**
- **A general-purpose background-jobs management screen.**

---

## Decisions

Recorded from the developer's answers on 2026-08-31; the requirements above already reflect them.

- **Q1 — Ownership of the batch-outcome contract**: **This feature owns it** (FR-018, FR-018a).
  #37062 and #37063 consume it. Chosen so this work is not blocked by an unbuilt ticket; the cost
  is that the shape must carry folder paths as naturally as files, which SC-006 verifies.

  Corrected after review: the first draft said this feature *defines* the shape. It does not — the
  bulk refresh work already ships one, and this feature **generalizes** it. As originally written,
  a plan author was licensed to invent a second shape, which is the exact outcome FR-018 exists to
  prevent.

  This reverses the dependency direction #37166 states ("depends on #37062 … which the folder
  endpoints define first"). Both #37062 and #37063 are open and unbuilt, so the reversal holds —
  but **#37166 must be amended to match**, or #37062's author defines a third shape in good faith.
- **Q2 — Limits**: A **maximum file count per batch of 100**, configurable, enforced as a
  submission-time refusal (FR-010).

  **Raised from 50 to 100 after the Q5 restoration**, and the two are connected. Under the one-call
  shape the whole batch travels in a single request, so the count ceiling also sets how long that
  request stays open — which is why it was not raised further. 300 was considered and rejected on
  two grounds: at a few megabytes per file it puts the batch past the **total** ceiling (FR-013b),
  making the count cap decorative and the size cap the surprising one; and it makes a single
  upload long enough to be a live risk against a proxy timeout, which is the one property the
  two-step shape was better at. 100 keeps the realistic batch inside both ceilings. A file over its applicable size ceiling is a per-file failure
  that leaves the batch running (FR-011). **Superseded in part by Q7**: the original decision here
  was that there would be no batch-total size limit; there now is one (FR-013b).

  Refined after review: the size ceiling is **not** a new bulk-specific knob. The product already
  declares one per content type on the binary field, applied on the single-file path, and FR-006's
  equivalence requirement means a batch must not reject what a single upload would accept. So this
  feature applies the existing ceiling and makes its rejection a per-file outcome. The consequence
  is that where an operator has configured nothing, there is no size ceiling — accepted knowingly,
  because diverging from the single-file path would be the worse defect.
- **Q3 — Outcome after navigating away**: The server notifies the submitter on any terminal state,
  both by push and by a durable record (FR-019 … FR-023), following the bulk refresh precedent.
  This removes the need for the frontend to build a jobs screen to satisfy #37166's "its outcome
  is still discoverable".
- **Q4 — Submission entry point**: A **domain endpoint owning its own multipart handling**, rather
  than the framework's generic job-upload entry point that #37166 proposes reusing. Two reasons.
  The generic entry point accepts exactly one file today, so a batch cannot travel through it
  without changing a shared type used by other consumers. And FR-003/FR-004 require refusing a
  submission — for a missing target, a bad upload type, or a permission failure — *before* any
  batch is created; the generic entry point has no notion of a target folder and would enqueue
  first and fail inside the run instead. Recorded here rather than left to the plan so the
  single-file constraint is not rediscovered on day one.
- **Q5 — Client-visible submission shape**: **One call.** The client sends the files, the target
  and the upload type together in a single multipart request. The endpoint hands the content
  straight to the product's file-staging API underneath and then creates the batch against what was
  staged. Staging stays invisible to the client, which never handles a staging reference.

  **This decision was reversed and then restored; the history matters because both reversals came
  from review, and the artifacts must not be read as though the first draft simply survived.**

  The first draft chose one call. After review it was changed to two steps — the client staging on
  the file-staging endpoint itself and submitting only references — on the reasoning that a second
  intake door would have to re-earn whatever cross-cutting file concerns that layer gains later.
  That reasoning still holds and is not disputed.

  It was restored to one call on three findings, in order of weight:

  1. **The one-call shape is the shipped precedent, not a departure from it.** Content import — the
     closest comparable feature — accepts `multipart/form-data` at its own endpoint, calls the
     staging API internally, and stores the resulting reference in its job parameters. The two-step
     draft described import as though the *client* staged; it does not. The shape being argued
     against is the one already in production.
  2. **The two-step shape introduced a defect the one-call shape does not have.** The staging
     layer's expiry is measured per file from its own last-modified time, on a global timer with no
     per-call override. Under two steps that clock starts when the *client* stages, so on a slow
     connection the earliest files of a large batch can expire before the author reaches the submit
     — and a reference that no longer resolves refuses the whole submission. Under one call the
     clock starts on the server, immediately before the batch is created.
  3. **Only the one-call shape can bound what reaches disk.** Nothing below this endpoint caps the
     number of files or the size of a request. Under two steps the bytes are already written by the
     time the batch is submitted, so the ceilings of FR-010 and FR-013b become a policy about what
     will be *processed* rather than a bound on what can be *written*. Under one call the request
     is aborted at the ceiling while it is read (FR-010a).

  What the restoration costs, recorded rather than glossed: **per-file upload progress and per-file
  retry** return to being unavailable (C-001a), and the endpoint does not inherit the request-level
  controls that live on the staging layer's REST resource rather than in its API — which is why
  FR-003a exists.

  What does not change under either shape: the batch is one job with one handle, one authoritative
  outcome, one durable notification, and a resumable run. Those are what a job exists for, and no
  amount of staging reuse provides them. That is the part of this design the review never disputed.

- **Q6 — Interruption**: **Resumable execution.** The run records completed files durably as it
  goes, so a re-queued run continues rather than restarts (FR-036 … FR-039). The alternative —
  marking the processor no-retry so an interrupted batch simply fails — was rejected: at batch
  sizes up to 100 it would make a restart cost the author the whole batch, and resubmitting would
  then collide against the files the first attempt already created.

  Strengthened after review: no-retry would not even have avoided the duplicates. The abandoned-job
  sweep re-queues unconditionally and never consults the retry policy, so an interrupted run is
  retried **whether or not** the processor is marked no-retry. Resumability is therefore the only
  answer available, not the preferred one of two.

  A caution that follows: the content import processor is marked no-retry and creates content from
  staged uploads — the same shape as this feature — so it is already exposed to this duplicate
  risk. That is a defect in import, not a precedent to follow. Import remains a sound precedent for
  the one-call staging shape (Q5) and for nothing about retry.
- **Q7 — Size ceilings**: **Both.** A configurable **total per batch**, refused at submission
  (FR-013b), and **per file** the content type's own ceiling where one is declared, falling back to
  a configurable value where none is (FR-011). The fallback is a knowing exception to FR-006's
  equivalence rule and is recorded as FR-011a rather than left to be discovered.

---

## Planning Obligations

Decided in principle above, but carrying enough hidden work that the plan must confront them
explicitly rather than meet them during implementation.

- **Durable mid-run state for resumability** (FR-036 … FR-038). This is the largest piece of
  hidden work in this specification and the plan must size it before committing to User Story 5.
  The job framework persists three things about a run: its parameters, written once at creation and
  immutable thereafter; its progress, a single number; and its result, harvested once at the
  terminal state. None of them is a mid-run record of which items are done. The resume path carries
  nothing forward either — a re-queued run is reset to pending with no result, no progress and no
  marker that it is a second attempt, and the processor is request-scoped, so it resumes with every
  counter at zero. FR-037 is therefore **either a new job-framework capability or a durable store
  owned by this feature**, and FR-038's "counts across all attempts" needs the same thing. If it
  turns out to require a framework change, that is a separate issue and is far cheaper to find here
  than in implementation.
  - **Consequence for FR-018**: a shape designed only to be emitted once at the terminal state will
    not serve a run that must read its own prior progress back. The generalization and this
    obligation should be planned together.
- **A cross-batch cap, or a recorded decision not to have one.** FR-010 and FR-013b bound a single
  batch; nothing bounds how many batches one author may have in flight, so the staged-bytes total
  is still unbounded per author. Previously carried as a functional requirement that the system
  "state a position", which is a decision for the authors rather than behaviour a test can fail.
  The plan must either set a cross-batch cap or record that there deliberately is none — for
  instance, that the job queue's own concurrency governs it.
- **The generalization of the shipped outcome shape** (FR-018) touches a contract other code
  already depends on. The plan must say whether it extends that type in place or extracts a
  shared one, and what that means for the shipped consumer.
- **Deriving stable failure reasons** (FR-016, FR-016a, C-002b, and the client copy that depends on
  them). The validation layer these outcomes come from distinguishes its cases largely by localized
  message text: the size rejection and the type rejection are the *same exception class* differing
  only in a translated string, and all of them arrive through one validation call. Producing a
  stable, machine-readable reason from that is real work — pre-checking before the create, or
  extending the validation layer to carry a code — and it is the mechanical foundation of the
  per-file reporting this whole feature promises. Plan it alongside the FR-018 generalization,
  which it will travel with.
- **Staged content lifetime** (FR-031 … FR-034). The available mechanism's lifetime ceiling is a
  single global value with no per-call override, so satisfying FR-031 by raising it would change
  behavior for every other consumer. The plan must choose among tolerating the ceiling and
  reporting expiry as a per-file failure, raising it globally and accepting the blast radius, or
  taking the content out of that mechanism's reach for the duration of the run. Note that the
  ceiling is measured per file from its **last-modified** time, which means refreshing it for an
  accepted batch is a cheap fourth option worth pricing — expiry is enforced only on retrieval and
  nothing deletes the content on a schedule.
- **Bounding the request while it is read** (FR-010a, FR-013c, FR-013d). Nothing below this
  endpoint provides a limit — not the staging layer, not the servlet container — so the abort-at-
  ceiling behaviour has to be built rather than configured, and it has to reclaim whatever was
  already staged when it fires. The plan must say where in the multipart read the counter and the
  running total live, and how the reclaim is made reliable when the abort is an exception path.
- **The request-level controls this endpoint does not inherit** (FR-003a, SC-012). Calling the
  staging API underneath supplies neither the same-origin check nor the enabled/disabled switch;
  both live on the staging layer's REST resource. The plan must place them explicitly. Worth
  noting for scope: the comparable shipped endpoint (content import) omits both, so this is a gap
  to close here rather than a pattern to copy — and closing it there is a separate ticket.
- **Test coverage** (Constitution V). The plan must name which layers this feature exercises and
  which it does not, with the reason for each omission. #37166 already enumerates what it expects:
  integration coverage for many files succeeding, mixed partial failure, a rejected file type, an
  oversized file, permission denied, cancellation mid-batch, and a batch large enough to exercise
  progress reporting. Silence is not an acceptable answer to Principle V.

---

## Assumptions

- The interface resolves one upload type per batch before submitting, and prompts the author when
  the target folder expresses no preference. The server receives one type and never infers one.
- Neither the per-file size rule (FR-011) nor the allowed-types rule (FR-012) is new. Both already
  apply to the single-file upload: they are declared per content type on the binary field and
  enforced during contentlet validation, which the creation path runs. Both surface as a validation
  failure the run can catch per file, which is what makes per-file reporting natural rather than
  bespoke. Both default to unconstrained where an operator has set nothing. For **type** that is
  where it ends: out of the box this feature inherits no type ceiling, a deliberate consequence of
  FR-006's equivalence requirement. For **size** it does not: FR-011.2 adds a configurable fallback
  precisely so an unconfigured content type does not leave the batch unbounded, which FR-011a
  records as a knowing exception to that same equivalence rule.

  Three controls here are new, not one: the batch file-count cap (FR-010), the per-file fallback
  ceiling (FR-011.2), and the batch total-size ceiling (FR-013b).
- The content is staged by the **server**, as it reads the submission, on the product's existing
  staging API (Q5) — the same way content import already does it. That API reports each file's
  measured size and resolved media type as it stages, which is what lets FR-013b accumulate a
  running total during the read and FR-016's type reason be a fact rather than an inference.
- The product's existing staging mechanism provides **no ceiling this feature can lean on**. Its
  per-file size limit is unlimited by default for authenticated users; it caps neither the number
  of files nor the total size of a request; and the servlet container is configured with no
  multipart limit either. That is pre-existing product behaviour, not something this feature
  introduces, but it is why FR-010a places the only bound in the path at this endpoint. Where an
  operator *has* set the staging per-file limit, it is narrowable only downward and only by the
  caller, which is why FR-013a requires this feature to enforce FR-011 itself rather than delegate.
- The staging mechanism's content-lifetime ceiling is a single global value with no per-call
  override, so it cannot be tuned for this feature alone without changing it for every other
  consumer. Under the one-call shape the clock starts at submission rather than at file selection,
  which removes the pre-submission expiry window entirely — but a batch that waits long enough in
  queue can still outlive it, which is why FR-031 and FR-032 remain stated as outcomes rather than
  as a configuration change.
- Enforcement of both limits is authoritative on the server. The client may check the file count
  to fail fast, but that check is a convenience and not the enforcement point.
- "Durable record" (FR-020) means the submitting author can learn their own run's outcome after
  the fact; it does not imply an administrator-facing view of all users' runs.
- Existing behavior on name collision is preserved and merely surfaced per file. This feature does
  not introduce overwrite-or-rename semantics.
- No new permission concept is introduced: the right to submit a batch is the right to add
  children to the target, which already exists.
