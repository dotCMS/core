# Feature Specification: Content Drive bulk file upload — frontend

**Feature Branch**: `37166-content-drive-bulk-file-upload-multi-file-selection-uploads-only-the-first-file`

**Created**: 2026-08-31

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37166](https://github.com/dotCMS/core/issues/37166) — "Content Drive: bulk file upload (multi-file selection uploads only the first file)", frontend half.

---

## Context

An author picks thirty files on their computer to upload into Content Drive. They are warned that
"only one file will be uploaded", and then one file is uploaded. The other twenty-nine are
discarded. This specification covers the browser-side half of fixing that.

Throughout this document, **choosing** files means picking them on the author's own computer, in
the system file chooser or by dragging them in. It never means selecting rows in the Content Drive
listing, which is a different act with a different purpose and is called **selecting rows** here.

**This specification covers the client only.** The server side is delivered separately by another
developer and is specified in `specs/37166-bulk-file-upload/spec.md` (PR
[#37300](https://github.com/dotCMS/core/pull/37300)). The two halves meet at that spec's *Contract
Consumed by the Client* (C-001 … C-006), restated here from the consumer's side in §Contract
Consumed. Nothing about server behaviour is re-specified here.

Fixing the upload exposes a second problem that this feature also settles. Content Drive currently
announces "this is running" in four different ways depending on which operation the author
triggered: a persistent marker in the portlet's toolbar, a transient notification, replacing the
entire content listing with placeholder rows, and saying nothing at all. A bulk upload is a long
operation and needs one of these to be right, so this feature picks the one that is right and
converts the others to it.

**Two different drags, and this feature touches both.** Content Drive accepts a drag from *outside*
the browser — files coming in from the author's computer, which is an **upload** and is what this
feature is for — and a drag from *inside* the listing — an existing item dragged from one folder to
another, which is a **move** and creates nothing. They are unrelated operations that happen to share
a gesture. Upload is the subject of this feature. Move appears only in §Requirements FR-014, as one
of the operations converted to the shared in-flight reporting, and is the case that demonstrates two
runs can be in flight at once (User Story 7) — which an upload on its own cannot show.

It also settles a third, smaller problem that the same work uncovers. Some operations report their
outcome by naming what ran and what it ran on ("**Publish** ran on 12 items"); others report
"Workflow Executed" and leave the author to guess. The copy for the missing half was written and
never connected.

---

## User Scenarios & Testing *(mandatory)*

<!--
  Written as author-visible behaviour in the Content Drive interface. Each is verifiable in the
  browser against a server honouring §Contract Consumed, with no knowledge of how the server
  implements it.
-->

### User Story 1 - Every chosen file is uploaded (Priority: P1)

An author brings several files in from their computer — by choosing them in the system file
chooser, or by dropping them onto a folder — and every one of them is uploaded to that folder.
Nothing is silently dropped, and nothing warns that the product cannot do what the author just
asked it to do.

**Why this priority**: This is the reported defect and the reason the feature exists. It is also a
silent data-loss bug: the author is told the upload happened and believes thirty files landed when
one did. No other story in this feature has value while this is broken.

**Independent Test**: Choose ten files through the upload control, and separately drag ten files in
from the computer and drop them onto a folder. Confirm all ten exist in that folder afterwards, in both cases, and that no "not
supported" warning appears.

**Acceptance Scenarios**:

1. **Given** an author viewing a folder they may add children to, **When** they choose several
   files through the upload button, **Then** all of the chosen files are submitted as one batch and
   all of them appear in the folder when the batch finishes.
2. **Given** the same author, **When** they drag several files from their computer onto the folder,
   **Then** the batch behaves identically to the button path.
3. **Given** an author choosing files through the upload button, **When** the file chooser opens,
   **Then** it permits selecting more than one file.
4. **Given** several files chosen at once, **When** they are submitted, **Then** no message claims that
   multiple-file upload is unsupported.
5. **Given** a single file chosen, **When** it is submitted, **Then** the author's experience is
   unchanged from today.

---

### User Story 2 - The author is told what actually happened (Priority: P1)

When a batch finishes, the author is told how many files were created and how many were not. If
some failed, the message names them and says why, and stays on screen long enough to be read.

**Why this priority**: A partial failure is the normal case for a batch of thirty files, not the
exception: one will be too large, one will have a blocked extension, one will collide with an
existing name. Uploading all thirty (Story 1) while reporting "upload complete" over three
silent failures reproduces the original defect in a new place.

**Independent Test**: Submit a batch containing valid files, one oversized file and one with a
disallowed extension. Confirm the resulting message reports the true counts and names both failing
files with a distinguishable reason each.

**Acceptance Scenarios**:

1. **Given** a finished batch where every file succeeded, **When** the outcome is reported,
   **Then** the count shown is the count the server reported, never the number of files the author
   chose.
2. **Given** a finished batch where some files failed, **When** the outcome is reported, **Then**
   the message names each failed file and gives a reason for each.
3. **Given** a finished batch where some files failed, **When** the outcome is reported, **Then**
   it remains readable for longer than a fully successful outcome does.
4. **Given** a batch where every file failed, **When** the outcome is reported, **Then** it is
   presented as a failure and not as a completed operation.
5. **Given** a finished batch, **When** the outcome is reported, **Then** the content listing
   refreshes exactly once, not once per file.

---

### User Story 3 - Work in progress is reported without taking over the screen (Priority: P1)

While any long operation runs, the author can see what is running and what it is running on, in one
consistent place, and can carry on looking at their content while it does.

**Why this priority**: A bulk upload runs long enough that the author will look for evidence it is
happening, so this is load-bearing for Story 1 rather than decorative. It also removes the worst of
the current behaviours: firing a workflow action on one right-clicked row today replaces the entire
listing with placeholder rows, which hides the very row the author acted on and is
indistinguishable from an ordinary page load.

**Independent Test**: Start a batch upload, then move existing items between folders, then fire a
workflow action from the row context menu. In all three cases confirm the in-flight state appears
in the same place, the listing stays rendered, and no transient notification announces the start.

**Acceptance Scenarios**:

1. **Given** any operation that outlives the surface that triggered it, **When** it starts, **Then**
   its in-flight state is reported in the portlet's persistent toolbar indicator.
2. **Given** any such operation, **When** it starts, **Then** no transient notification announces
   that it has started.
3. **Given** a workflow action fired from a row's context menu, **When** it is running, **Then** the
   content listing remains rendered and the acted-on row remains visible.
4. **Given** an operation running on exactly one item, **When** its in-flight state is shown,
   **Then** it names both the action and the item it is running on.
5. **Given** an operation running on several items, **When** its in-flight state is shown, **Then**
   it names the action and the number of items.
6. **Given** a run whose progress the server reports, **When** it is in flight, **Then** the
   indicator shows how far it has got; **and given** a run whose progress is not reported, **Then**
   it shows activity without claiming a false position.
7. **Given** an operation started from a dialog, **When** that dialog closes, **Then** the operation
   continues and its in-flight state remains visible.

---

### User Story 4 - Leaving does not lose the batch (Priority: P2)

An author who starts a large upload and then goes to do something else still learns how it went.

**Why this priority**: The direct consequence of making upload a long operation. An author who must
sit and watch a thirty-file upload has been given a worse product than the one that uploaded one
file quickly.

**Independent Test**: Start a batch, navigate away from the folder and from the portlet, and
confirm the outcome still reaches the author when the batch finishes.

**Acceptance Scenarios**:

1. **Given** a running batch, **When** the author browses to another folder, **Then** the batch
   continues and its in-flight state remains visible.
2. **Given** a running batch, **When** the author leaves the portlet entirely, **Then** the batch
   continues and its outcome still reaches them when it finishes.
3. **Given** a batch that finished while the author was elsewhere, **When** they are notified,
   **Then** the notification does not disturb whatever they are in the middle of doing.
4. **Given** a run started by a different author or a different session, **When** its outcome is
   broadcast, **Then** this author's interface does not report it as their own.

---

### User Story 5 - A running batch can be stopped (Priority: P4 — OPTIONAL, deferred to the task manager)

An author who realises they picked the wrong folder, or the wrong hundred files, can stop the batch
without waiting for it to finish.

**Why this priority**: Deliberately deferred, and it is the one story that may not ship with this
feature. The *need* is real — it is the cost of making upload a long operation — but the *design* is
not settled, and it now looks likely to be settled somewhere else. A general task manager is
anticipated as future work, and a list of running tasks is the natural home for stopping one: it is
where the author goes to act on background work, it can show every run rather than the one this
portlet started, and it does not have to squeeze a destructive-adjacent control into a status
indicator. Building a stop button on the toolbar now risks building the wrong thing in the wrong
place, and then having to remove it.

Deferring is cheap and reversible. The server capability is fully specified and will exist whether
or not this ships (backend FR-025 … FR-028, C-004), so adopting it later — here or in the task
manager — needs no server work and no change to anything else in this feature. Nothing else here
depends on it. Build what is certain first.

Depends on Story 3, since the indicator is the only surface still present once the dialog that
started the run has closed, which is also why its design is the open question.

**Independent Test** *(applies only if this story is built)*: Start a batch of many files, stop it
partway, and confirm the author is told it was stopped and how far it reached.

**Acceptance Scenarios**:

1. **Given** a running batch, **When** the author asks to stop it, **Then** the request is accepted
   and the batch stops.
2. **Given** a cancelled batch, **When** its outcome is reported, **Then** it is presented as
   cancelled rather than as a success or a failure, and reports how far it reached.
3. **Given** a cancelled batch, **When** the listing refreshes, **Then** the files already created
   are present.
4. **Given** an operation that cannot be stopped, **When** it is in flight, **Then** no stop control
   is offered for it.

---

### User Story 6 - Every outcome says what ran and what it ran on (Priority: P2)

Whatever the author did — a workflow action, a folder created, a folder renamed, a lock — the
message afterwards names the operation and the thing it happened to.

**Why this priority**: Independent of the upload work and shippable on its own, which is why it is
not P1. It is in this feature because the upload work sets the standard the rest should match, and
because the copy for the missing cases was already written and never connected, so the gap is a
loose end rather than a new design.

**Independent Test**: Trigger a workflow action from the context menu, create a folder, rename a
folder, and fail a lock. Confirm each resulting message names the operation and the item.

**Acceptance Scenarios**:

1. **Given** any completed operation, **When** its outcome is reported, **Then** the message names
   the operation and the item or count it applied to.
2. **Given** any failed operation, **When** its outcome is reported, **Then** the message likewise
   names the operation and the item.
3. **Given** any operation's outcome, **When** it is shown, **Then** every word of it is
   translatable, with no untranslated literal text.
4. **Given** an operation that failed on the server, **When** the author is told, **Then** they are
   shown resolved product copy rather than a raw server message, and the raw detail is available to
   support through the logs.

---

### User Story 7 - One operation does not block another (Priority: P3)

An author who starts a long upload can still lock a row, fire a workflow action, or move existing
items between folders while it runs.

**Why this priority**: A refinement rather than a defect on its own, but it is what stops the
upload work from making the portlet feel *worse*. Today only one operation may be in flight at a
time, which is tolerable when operations take a second and unacceptable when one of them takes
minutes.

**Independent Test**: Start a batch upload of many files, and while it runs, successfully fire a
workflow action on rows selected in the listing.

**Acceptance Scenarios**:

1. **Given** a running batch upload, **When** the author starts an unrelated operation, **Then** it
   is accepted and runs.
2. **Given** two operations in flight, **When** their in-flight state is shown, **Then** the author
   can tell that two things are running and what they are.
3. **Given** a running operation, **When** the author tries to fire the same operation again over
   the same items, **Then** it is refused.
4. **Given** several operations finishing, **When** each reports its outcome, **Then** each outcome
   names its own operation and is not confused with another's.

---

### Edge Cases

- An author selects more files than the configured maximum. The refusal is reported before anything
  is uploaded, and explains the limit rather than failing opaquely.
- An author selects zero files, or cancels the file chooser after opening it. Nothing is submitted
  and nothing is reported.
- A batch is submitted and the server refuses it for lack of permission on the target. The author is
  told they lack permission, distinguishably from a malformed submission.
- The connection carrying progress drops mid-run. The author is not told the run failed, because the
  client has no evidence that it did.
- A run finishes while the author has a dialog open. The outcome does not interrupt them mid-task.
- A finished run reports counts that do not add up to the number of files submitted. No count is
  invented to fill the gap.
- A content item's title contains markup. It is displayed as text wherever the interface names it,
  and cannot alter the surrounding interface.
- An author uploads to the root of a site rather than into a folder. The site is the target.
- The same author has the portlet open in two tabs. A run submitted in one is not reported as the
  other's.

---

## Requirements *(mandatory)*

### Functional Requirements

**Choosing files and submitting them**

- **FR-001**: Both ways of bringing files in from the author's computer — the upload control, which
  opens the system file chooser, and dropping files onto a folder — MUST accept more than one file.
- **FR-002**: Every file the author chose MUST be submitted as one batch. No file may be discarded, and no
  file may be uploaded without the author having chosen it.
- **FR-003**: One upload type MUST be resolved for the whole batch before submitting. Where the
  target folder does not determine it, the author MUST be asked once for the batch, not once per
  file.
- **FR-004**: The interface MUST NOT tell the author that multi-file upload is unsupported, and the
  copy saying so MUST be removed.
- **FR-005**: The single-file upload experience MUST remain unchanged.
- **FR-006**: The client MAY refuse a batch that exceeds the configured maximum before
  submitting it, as a convenience. The server remains the point of enforcement, and the client MUST
  NOT treat its own check as authoritative.

**Reporting work in progress**

- **FR-007**: Any asynchronous operation that outlives the surface that triggered it MUST report its
  in-flight state in the portlet's persistent toolbar indicator.
- **FR-008**: In-flight state MUST NOT be reported as a transient notification. Transient
  notifications are reserved for terminal states.
- **FR-009**: The content listing's own loading state MUST mean only that the listing is being
  fetched. No operation may use it to report its own progress.
- **FR-010**: The in-flight indicator MUST name the operation and what it is being applied to: the
  item, when there is one item; the number of items otherwise.
- **FR-011**: Any content-supplied text the indicator displays MUST be rendered as text and MUST NOT
  be able to introduce markup into the interface.
- **FR-012**: The indicator MUST show a run's position when the server reports progress, and MUST
  show activity without claiming a position when it does not.
- **FR-013**: A run MUST survive the closing of the dialog, menu or control that started it, and
  MUST survive the author browsing to another folder within the portlet.
- **FR-014**: The following MUST be converted to FR-007: bulk upload; moving existing Content Drive
  items from one folder to another by dragging them; and firing a workflow action from a row's
  context menu.

**Several operations at once**

- **FR-015**: More than one operation MUST be able to be in flight at the same time, and one MUST
  NOT prevent an unrelated one from starting.
- **FR-016**: The guard preventing an operation being fired twice over the same items MUST apply to
  that operation and those items, not to the portlet as a whole.
- **FR-017**: The indicator MUST have a defined behaviour for several concurrent runs that lets the
  author tell how many things are running and what they are.

**Stopping a run** *(this whole group is OPTIONAL — see User Story 5. Stopping is expected to be
delivered by the anticipated task manager rather than by this feature.)*

- **FR-018** *(optional)*: A run the server reports as stoppable MAY offer the author a way to stop
  it, from the in-flight indicator. Deferred pending a settled design for that control.
- **FR-019** *(optional)*: A run that reaches a stopped state SHOULD be reported as stopped,
  distinctly from success and from failure, and report how far it reached. Deferred with the rest of
  this group. The safety net while it is deferred is FR-024: a stopped run is a terminal state the
  client does not handle, so it degrades to an error rather than to a green success.
- **FR-020** *(optional, applies only if FR-018 is built)*: A run that cannot be stopped MUST NOT
  offer a control that does nothing.

**Reporting the outcome**

- **FR-021**: A terminal state — finished, partially finished, failed, or stopped — MUST be reported
  to the author as a transient notification.
- **FR-022**: Every count reported MUST be the server's count. The number of items the author
  selected MUST NOT be used as a stand-in for the number the operation actually affected.
- **FR-023**: A partial outcome MUST name the items that did not succeed and give a reason for each,
  and MUST remain readable longer than a clean success does.
- **FR-024**: An outcome the client cannot account for — counts that do not close, a response
  carrying no outcome, or a terminal state the client does not recognise — MUST be reported as an
  error. No number may be invented to fill a gap, and an unrecognised outcome MUST NOT be reported
  as a success. This is what makes deferring FR-019 safe.
- **FR-025**: The content listing MUST refresh once when a run reaches a terminal state, not per
  item.
- **FR-026**: An outcome arriving while the author is in the middle of something else MUST NOT
  interrupt them.
- **FR-027**: The client MUST only report outcomes for runs it started. An outcome broadcast to this
  author from another session, tab or context MUST NOT be reported as this one's.

**What the outcome says**

- **FR-028**: Every operation's outcome MUST name the operation and the item or count it applied to,
  on success and on failure alike.
- **FR-029**: All author-visible text MUST be translatable. No literal untranslated text may reach
  the interface.
- **FR-030**: Raw server messages MUST NOT be shown to the author. Resolved product copy is shown;
  the raw detail is logged.
- **FR-031**: Message definitions that this work makes reachable MUST be connected, and definitions
  it makes obsolete MUST be removed. No definition may be left defined and unreferenced.

**Reuse**

- **FR-032**: The capability for following a long server-side run — submit, follow, report progress,
  report the terminal state and outcome, stop — MUST be expressed generically, with no knowledge of
  uploads or of Content Drive, so that the folder copy and bulk delete work
  ([#37063](https://github.com/dotCMS/core/issues/37063)) can adopt it unchanged.
- **FR-033**: The interface MUST NOT require a general-purpose background-jobs screen for any of the
  above.

### Key Entities

- **Chosen files**: What the author picked on their own computer before submitting — the files
  themselves, the target folder or site in Content Drive to put them in, and the one upload type
  for the batch.
- **Run**: One operation the author started that the interface is still tracking. Holds what is
  being applied, what it is being applied to, its position when known, and whether it can be
  stopped. Several may exist at once. Outlives the surface that created it.
- **Run outcome**: A run's terminal state and what came of it — the server's counts, and the
  per-item results with a reason for each that did not succeed.

---

## Contract Consumed *(mandatory — the boundary with the server half)*

The server half is specified in `specs/37166-bulk-file-upload/spec.md`. This is that spec's
*Contract Consumed by the Client*, restated from this side so the boundary is reviewable from
either document. Each item is a dependency of this feature, not a requirement of it.

- **C-001** — Submit several files with one target and one upload type, answered immediately with a
  handle. *Consumed by* FR-002.
- **C-002** — Distinguishable submission refusals: no files, bad upload type, missing target, too
  many files, permission denied. *Consumed by* FR-006 and the edge cases.
- **C-003** — Readable progress for a run in flight. *Consumed by* FR-012.
- **C-004** — A way to stop a run in flight. **Not consumed in this pass.** The whole stopping group
  (FR-018 … FR-020) is optional and expected to be delivered by the anticipated task manager. Noted
  here so the boundary stays complete and so it is clear that adopting it later requires no server
  change.
- **C-005** — A readable terminal state and outcome: counts plus per-item results with a reason and
  a message. *Consumed by* FR-021 … FR-024.
- **C-006** — A pushed completion signal carrying the run's counts, reaching the browser without the
  client polling, plus a durable record of the same outcome. *Consumed by* FR-026, FR-033.

**What C-006 buys this feature**: the outcome survives the author walking away without the client
building anything to make that true. The client renders the pushed signal while the author is
present; the durable record is the server's responsibility. This is why FR-033 can hold.

**Not settled at this altitude**: the concrete submission format — field names, the endpoint, and
the shape of the handle. Both halves are being built by different developers, so this MUST be
agreed and written down during planning, and both plans MUST reference the same definition. It is
recorded here as a planning obligation so it cannot be discovered during review.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An author choosing N files on their computer gets N files uploaded. Today they get
  one, and this is measured from what the author picked, not from what the interface chose to
  submit.
- **SC-002**: The number of distinct ways the product reports "this is running" in Content Drive
  goes from four to one.
- **SC-003**: No operation removes the content listing from the screen in order to report its own
  progress. Measured as zero occurrences across every operation the portlet offers.
- **SC-004**: Every author-visible outcome names the operation and what it applied to. Measured as
  100% of outcome messages, success and failure alike.
- **SC-005**: Every count an author is shown matches the count the server reported for that run, in
  every case including partial failure and cancellation.
- **SC-006**: An author who starts a batch and leaves the portlet still learns its outcome.
- **SC-007**: Starting a long operation never prevents an author from starting an unrelated one.
- **SC-008**: The run-following capability is adopted by #37063 without modification to it.
- **SC-009**: No message definition used by Content Drive is left defined and unreferenced, and none
  referenced is left undefined.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive portlet, which is modern rather than legacy
  surface. Three shared pieces are also touched, and each has a second consumer that must not
  regress: the folder list view and the upload control are both shared with the AssetPicker work
  ([#36702](https://github.com/dotCMS/core/issues/36702),
  [#37207](https://github.com/dotCMS/core/issues/37207)); the shared execution state is consumed by
  the Action Center. Two of the operations sharing the toolbar indicator — adding to a bundle and
  push publishing — reach older server endpoints that report their outcome in a different shape.
  Those endpoints are not changed here, but the indicator and outcome rules apply to them, so their
  existing reporting behaviour must be preserved as it is adapted.
- **Backward-compatibility expectations**: The single-file upload experience must be unchanged
  (FR-005). The AssetPicker must not regress through the shared components. Message definitions may
  be removed only where they are proven unreferenced (FR-031). No new permission concept is
  introduced: the right to upload a batch is the existing right to add children to the target.
- **Known related decisions**: The bulk reindex work established the precedent this feature follows
  — a long operation is submitted, answered immediately, and its completion is pushed to the author
  rather than polled for. The Action Center established the other one: execution state is held by
  the portlet rather than by the dialog that started it, so a run survives its dialog closing. This
  feature generalises both rather than inventing a third approach. `/speckit-plan` will consult
  `dotCMS/platform-adrs` formally.

---

## Out of Scope

- **The server-side implementation of #37166.** Delivered separately; this feature consumes
  §Contract Consumed and specifies nothing about how the server satisfies it.
- **Per-item in-flight marking in the listing.** Showing *which* individual rows an operation is
  acting on, rather than only *what* is running, is a separate mechanism with its own semantics to
  settle. It is excluded because **this feature cannot exercise it**: an upload has no rows, since
  the items do not exist until the run creates them. Its real first consumers are the context menu,
  the drag-and-drop move between folders, and the Action Center. Follow-up ticket.
- **Bulk reindex's reporting.** Its exclusion from the in-flight indicator is a deliberate existing
  decision, and the notification record already covers its outcome. Unchanged here.
- **Directory upload.** Dropping a whole folder from the author's computer and recreating its
  hierarchy inside Content Drive. Deferred by #37166.
- **Resumable or chunked single-file upload.**
- **A general-purpose background-jobs management screen** (FR-033). One is anticipated as future
  work and is the expected home for stopping a run (User Story 5), but this feature must not depend
  on it existing, and does not build it.
- **Mixing upload types within one batch** (FR-003).

---

## Assumptions

- The server half honours §Contract Consumed. Where it does not yet, this feature's stories are
  blocked rather than worked around: no client-side polling loop, retry ladder or count estimation
  is introduced to compensate.
- Because C-006 provides a pushed signal, the client does not poll for a run's completion. It may
  still read progress while the author is present.
- The concurrent-run display (FR-017) defaults to naming each run while there are few, and
  collapsing to a count beyond that. The exact threshold is a design choice for planning, not a
  requirement.
- Stopping a run is expected to arrive with a general task manager rather than in this feature. If
  it were instead built here, it would live on the in-flight indicator, since once the dialog that
  started a run has closed the indicator is the only surface still representing it. That placement
  is the open question, and the task manager is the likelier answer to it.
- An outcome is reported once, by whichever part of the interface is responsible for presenting it,
  rather than by each surface that knows about the run.
- Removing a message definition is safe only where it is unreferenced across the whole client, not
  merely within Content Drive.
- The per-batch file count and per-file size limits are configured and enforced on the server. The
  client learns of them through refusals rather than holding its own copy.
- Authors reaching this are back-office users on a maintained desktop browser; no additional
  compatibility target is introduced.
