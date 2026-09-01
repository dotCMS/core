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

A batch routinely contains a file that is too large, has a blocked extension, or collides with an
existing name. The run continues past it and records, per file, what happened and why.

**Why this priority**: Partial failure is the normal case at these batch sizes, not an edge case.
A run that aborts at the first bad file, or records only "upload failed", leaves the author unable
to tell what landed — barely better than today.

**Independent Test**: Submit 5 files of which 2 are invalid (one over the size limit, one with a
blocked extension); confirm the 3 valid files are created and the recorded outcome reports 3
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

---

## Requirements *(mandatory)*

### Functional Requirements

**Submission**

- **FR-001**: The system MUST accept a batch of several files in one submission, carrying a target
  folder (or site root) and one upload type for the whole batch.
- **FR-002**: The system MUST answer a valid submission immediately, before the files are created,
  with a handle the caller can use to follow, cancel, and read the outcome of the run.
- **FR-003**: The system MUST validate at submission — and refuse before creating any batch — a
  submission that: carries no files; carries an upload type other than Asset or File; names a
  target that does not exist; exceeds the configured maximum file count; or comes from an author
  without permission to add children to the target.
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
- **FR-009**: The existing single-file upload endpoint and its behavior MUST be left unchanged.

**Limits**

- **FR-010**: The system MUST enforce a configurable maximum number of files per batch, defaulting
  to **50**. Exceeding it is a submission-time refusal (FR-003). This limit is new — nothing caps a
  selection today — and MUST be operator-configurable through the product's standard configuration
  mechanism rather than hardcoded.
- **FR-011**: A file rejected for its **size** MUST be recorded as that file's own failure with a
  size-specific reason, and MUST NOT fail the batch. The product already enforces a per-file size
  ceiling on the single-file upload path, declared per content type on the binary field and
  unlimited where an operator has not set one. This feature MUST apply **that** ceiling, not a
  bulk-specific one: FR-006 requires a file to be accepted or rejected identically whether it
  arrives alone or in a batch, so a batch that rejected a file the single-file upload accepts would
  violate it. The requirement here is therefore about **surfacing** the existing rejection per
  file rather than aborting the run — not about introducing a second limit.
- **FR-012**: A file rejected for its **type** MUST likewise be recorded as that file's own
  failure and MUST NOT fail the batch. As with size, the allowed-types rule is declared per content
  type on the binary field and is applied as-is.
- **FR-013**: The system MUST NOT rely on a size limit supplied by the caller. A caller that omits
  one would fall back to an unlimited default, so any caller-supplied ceiling can tighten what the
  system enforces but MUST NOT be the enforcement point.
- **FR-013a**: Where a chosen staging mechanism imposes its own size ceiling, the effective limit
  is the stricter of it and FR-011's, and the plan MUST state which is expected to bind — so that
  a file rejected by staging is not reported to the author as a content-type rule it did not break.

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
- **FR-035**: The system MUST state a position on the total bytes one author may hold staged
  across concurrent batches. "No total limit, deliberately" is an acceptable position; leaving it
  unstated is not, because FR-010 caps files per batch and nothing caps batches.

**Outcome**

- **FR-014**: A finished run MUST record a total, a success count, a failure count, and a skipped
  count.
- **FR-015**: A finished run MUST record a per-file result naming the file and its status —
  succeeded, failed, or skipped (never attempted).
- **FR-016**: Every failed per-file result MUST carry a machine-readable reason and a
  human-readable message. The reason MUST distinguish at least: over size limit, disallowed
  extension, name collision, permission denied (a per-file check narrower than the target-folder
  check in FR-003 — see Edge Cases), and an unclassified error.
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
  - **One spelling of the counters.** The shipped counter is `failedCount`; #37166 and the
    surrounding discussion say `failCount`. The plan MUST pick one and both halves MUST use it.
- **FR-018a**: The generalized shape MUST carry a batch of folder paths as naturally as a batch of
  files, so #37062 / #37063 adopt it unchanged.

**Notifying the submitter**

- **FR-019**: When a run reaches any terminal state — finished, cancelled, or permanently failed —
  the system MUST notify the submitting author of the outcome and its counts.
- **FR-020**: The notification MUST be durable: an author who was not looking when the run
  finished MUST still be able to learn the outcome afterwards.
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

- **C-001**: A way to submit several files with one target and one upload type, in **a single
  call** carrying the file content, answered immediately with a handle (FR-001, FR-002, Q5). The
  client does not stage the content itself and never handles a staging identifier — where the
  bytes wait for the run is the server's business and can change without touching the client.
- **C-002**: A distinguishable submission refusal for: no files, bad upload type, missing target,
  too many files, permission denied (FR-003, FR-004).
- **C-003**: Readable progress for an in-flight run (FR-024).
- **C-004**: A way to cancel an in-flight run (FR-025).
- **C-005**: A readable terminal state and outcome — counts plus per-file results with reason and
  message (FR-014 … FR-017).
- **C-006**: A pushed completion signal carrying the run's counts, reaching the browser without
  the client having to poll, plus a durable record of the same outcome (FR-019, FR-020).

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
- **SC-003**: A batch of 50 files sustains a stated per-file throughput floor, set in the plan as
  an absolute target rather than as a comparison against today's behavior — today's single-file
  upload waits for each file to become searchable, which is the very pathology FR-008 removes, so
  measuring against it would make this criterion true by construction. The batch also does not
  serialize behind a per-file search-index wait.
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
  created and no file is uploaded, in 100% of refusals.

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
- **Q2 — Limits**: A **maximum file count per batch of 50**, configurable, enforced as a
  submission-time refusal (FR-010). Size is enforced **per file, not per batch**, as a per-file
  failure that leaves the batch running (FR-011). There is no batch-total size limit.

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
- **Q5 — Client-visible submission shape**: **One call.** The client sends the files and the batch
  parameters together; staging them for the run is the server's business and stays invisible to
  the client, as it already is for content import. The alternative — the client stages the content
  itself and then submits references — was considered and rejected: it makes the staging mechanism
  part of the contract between the two halves of #37166, so changing it later would require
  changing the frontend.

---

## Planning Obligations

Decided in principle above, but carrying enough hidden work that the plan must confront them
explicitly rather than meet them during implementation.

- **The generalization of the shipped outcome shape** (FR-018) touches a contract other code
  already depends on. The plan must say whether it extends that type in place or extracts a
  shared one, and what that means for the shipped consumer.
- **Staged content lifetime** (FR-031 … FR-034). The available mechanism's lifetime ceiling is a
  single global value with no per-call override, so satisfying FR-031 by raising it would change
  behavior for every other consumer. The plan must choose among tolerating the ceiling and
  reporting expiry as a per-file failure, raising it globally and accepting the blast radius, or
  taking the content out of that mechanism's reach for the duration of the run.
- **Test coverage** (Constitution V). The plan must name which layers this feature exercises and
  which it does not, with the reason for each omission. #37166 already enumerates what it expects:
  integration coverage for many files succeeding, mixed partial failure, a rejected extension, an
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
  bespoke. Both default to unconstrained where an operator has set nothing, so out of the box this
  feature inherits no size or type ceiling — that is a deliberate consequence of FR-006's
  equivalence requirement, not an oversight. Only the batch file-count cap (FR-010) is new.
- The product's existing staging mechanism carries its own size ceiling, unlimited by default and
  narrowable per call but only downward — and narrowable by the *caller*, which is why FR-013a
  requires the system to enforce FR-011 itself rather than delegate to it. Its content-lifetime
  ceiling, by contrast, is a single global value with no per-call override, so it cannot be tuned
  for this feature alone without changing it for every other consumer. That constraint is the
  reason FR-031 and FR-032 are stated as outcomes rather than as a configuration change.
- Enforcement of both limits is authoritative on the server. The client may check the file count
  to fail fast, but that check is a convenience and not the enforcement point.
- "Durable record" (FR-020) means the submitting author can learn their own run's outcome after
  the fact; it does not imply an administrator-facing view of all users' runs.
- Existing behavior on name collision is preserved and merely surfaced per file. This feature does
  not introduce overwrite-or-rename semantics.
- No new permission concept is introduced: the right to submit a batch is the right to add
  children to the target, which already exists.
