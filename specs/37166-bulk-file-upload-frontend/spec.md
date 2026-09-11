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
6. **Given** an author viewing a folder they may **not** add children to, **When** they look for a
   way to upload into it, **Then** every route in is offered disabled with the reason shown, rather
   than hidden or left to fail after they have chosen files.
7. **Given** that same folder, **When** the author drops files onto it anyway, **Then** nothing is
   submitted and they are told why.

---

### User Story 2 - The author is told what actually happened (Priority: P1)

When a batch finishes, the author is told how many files were created and how many were not. If
some failed, the message names them and says why, and stays on screen long enough to be read.

**Why this priority**: A partial failure is the normal case for a batch of thirty files, not the
exception: one will be too large, one will have a file type that is not allowed, one will collide
with an existing name. Uploading all thirty (Story 1) while reporting "upload complete" over three
silent failures reproduces the original defect in a new place.

**Independent Test**: Submit a batch containing valid files, one oversized file and one whose file
type is not allowed. Confirm the resulting message reports the true counts and names both failing
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
not settled, and it is already owned elsewhere.
[#33331](https://github.com/dotCMS/core/issues/33331) — *"[EPIC] Task manager UI to manage large
uploads and moves"* — covers exactly this, and says so in its own words: *"From this task manager
UI, we can also enable the user to cancel individual file uploads, or cancel the whole upload or
move."* A list of running tasks is the natural home for stopping one: it is where the author goes to
act on background work, it can show every run rather than only the one this portlet started, and it
does not have to squeeze a destructive-adjacent control into a status indicator. Building a stop button on the toolbar now risks building the wrong thing in the wrong
place, and then having to remove it.

Deferring is cheap and reversible. The server capability is fully specified and will exist whether
or not this ships (backend FR-025 … FR-028, C-004), so adopting it later — here or in #33331 —
needs no server work and no change to anything else in this feature. Nothing else here
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

- An author selects more files than the configured maximum, or a set of files whose total size
  exceeds the configured ceiling. Either refusal says which limit was hit rather than failing
  opaquely: "too many files" and "too much data" are different problems with different fixes. The
  count refusal lands before anything is uploaded. The size refusal only does so when the client
  declares the batch total (FR-038); the server's authoritative check runs while it reads the
  content (backend FR-013c.2), so an undeclared over-ceiling batch is refused only after the author
  has watched it upload. That is the case the copy has to survive.
- The connection drops while a batch is being submitted, so the author cannot tell whether it
  landed. Retrying is safe and the interface says so, rather than leaving them to check the folder
  and guess.
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
- **FR-006**: The client MAY refuse a batch that exceeds the configured maximum file count, or the
  configured total size ceiling, before submitting it, as a convenience. The server remains the
  point of enforcement, and the client MUST NOT treat its own check as authoritative.
- **FR-038**: The client MUST declare the batch's total size when it submits. The server's fast
  refusal of an over-ceiling batch exists only where the caller declares a total (backend
  FR-013c.1); without one the only enforcement left is the authoritative check taken while the
  content is read (backend FR-013c.2), which refuses the batch after the author has already uploaded
  it. The declared total is a courtesy to the author, not a limit the client enforces: FR-006 still
  applies, and the client MUST NOT treat its own arithmetic as authoritative. The field is
  `totalSizeBytes` on the submission's `form` part, and under-declaring it only forfeits the fast
  refusal rather than raising the ceiling.
- **FR-042**: While the upload phase is in flight the client MUST warn the author before the page
  unloads. The loss is silent and total, with no run, no record and no notification (C-001a1), so
  the only moment the interface can intervene is before the navigation happens. The warning MUST
  NOT outlive the upload phase: once the handle exists, leaving is safe and prompting would be
  false.
- **FR-037** *(amended 2026-09-11 — scoped to `FILEASSET`)*: When the connection is lost or the
  answer is uncertain while a batch is being submitted, the client MUST be able to resubmit the same
  batch, and MUST NOT make the author work out whether the first attempt landed. **For `FILEASSET`**
  resubmitting cannot produce a second copy of their files: the server either recognises the
  resubmission as the same batch or refuses the duplicates (backend FR-040, C-002a). There the client
  treats an uncertain submission as retryable, not as a failure the author has to reason about, and
  never asks them to check the folder first.
  - **FR-037a** *(added by amendment, 2026-09-11)*: **That guarantee does not hold for `DOTASSET`,
    and the client MUST NOT imply that it does.** Backend FR-040b retired it: a dotAsset's identifier
    takes a per-contentlet `asset_name`, so the unique index the guarantee rests on can never contend
    for one, and a resubmitted batch creates a second copy of every file and reports clean success.
    `duplicateSubmission` is additionally `false` whenever the file order differed, because the
    server's fingerprint is order-sensitive. C-002a1 makes the consequence this client's obligation:
    where the outcome of a submission is genuinely unknown — a dropped connection, a failure that is
    neither of the two ceiling refusals — the copy MUST NOT offer an unqualified retry for that base
    type, because acting on it can leave two copies of everything. Naming the folder as the thing to
    check is the correct answer there, and is the one case where FR-037's "never asks them to check
    the folder first" is overridden rather than followed.
  - *This amendment retires a sentence PR 1 was approved against and therefore needs re-approval,
    per Quick Start §3.*
  Where the server takes the collision branch, the client depends on a duplicate resubmission being
  distinguishable from a genuinely all-collided batch (backend FR-040a, C-002a), and MUST report
  such a retry as the success it is rather than as "every file failed, file already exists". The
  contract settles this as `duplicateSubmission` on the outcome: `true` means this run was a
  resubmission of a batch that had already succeeded.
- **FR-034**: The existing permission gate MUST continue to hold, unchanged, for every route into an
  upload: the upload control, the create menu, and dropping files onto a folder. An author without
  the right to add children to the target MUST be shown that route disabled **with the reason
  given**, rather than the route being hidden or being offered and then failing after they have
  chosen their files. A drop onto a target they may not add children to MUST be refused before
  anything is submitted, with the reason. This includes the site root, whose permission is resolved
  separately from a folder's because the root has no folder to carry it. As with FR-006, this gate
  is an affordance: the server remains the point of enforcement (backend FR-003, FR-004), and the
  client MUST NOT treat its own check as authoritative.

  *This requirement protects existing behaviour rather than adding any. It is stated because this
  feature changes the upload control and the drop path directly, so the gate is squarely in the
  blast radius.*

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
- **FR-041**: A batch has **two phases with different guarantees**, and the interface MUST
  distinguish them. While the content is still being sent there is no run: the submission can be
  lost and nothing is recorded, reported or notified (C-001a1). Once the handle is answered the run
  belongs to the server and survives navigation (FR-013). The indicator MUST report the two as
  distinct states, and the arrival of the handle MUST be visible to the author, because that is the
  moment leaving becomes safe. The client MUST NOT tell the author they are free to move around
  before the handle exists.
- **FR-041a** *(amended 2026-09-11 — downgraded to SHOULD, with the reason)*: The upload phase
  **SHOULD** report a real position wherever the browser can supply one. Bytes sent against the
  declared total (FR-038) does not require the server, so this phase is not covered by FR-012's
  "activity without claiming a position" fallback where it is available. C-001a rules out *per-file*
  progress, not progress.
  - **It is not available in this application, by decision.** Angular 22 made `FetchBackend` the
    default `HttpBackend`, and fetch has no equivalent of `xhr.upload`, so
    `HttpEventType.UploadProgress` never fires and a `reportProgress` request reports nothing on the
    way up. `withXhr()` restores it and was proven end to end against a live instance; the developer
    **declined it** because XHR sits too near the deprecation line, its server-side half already
    being slated for removal in Angular 23. So the upload phase reports activity without a position,
    which the indicator already renders as activity rather than as zero (FR-012), and the phase stays
    distinguishable from the run phase by its wording rather than by a number (FR-041).
  - *Was a MUST when PR 1 was approved. The downgrade is the decision, not an oversight.*
- **FR-035**: The in-flight indicator MUST remain announced to assistive technology, and the
  additions this feature makes to it MUST NOT make it noisy. Specifically: a run starting, reaching a
  terminal state, or being stopped are worth announcing; continuous progress is not, and MUST NOT be
  announced on every update. Where progress is shown, its current value MUST be available to
  assistive technology on request, so a user can ask for it without having it pushed at them
  repeatedly. This holds when several runs are in flight, which is when the risk of a chatty live
  region is highest.
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
delivered by the task manager epic [#33331](https://github.com/dotCMS/core/issues/33331) rather than
by this feature.)*

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
  and MUST remain readable longer than a clean success does. This holds for **both** surfaces the
  outcome reaches: the transient notification shown while the author is present, and the durable
  record they can read afterwards (C-006). Neither may degrade to counts alone, because the file
  names are what tell an author which files to choose again.
- **FR-023a**: Reasons MUST be **grouped**, not repeated once per file: "3 files were larger than
  the limit, 1 already existed" rather than four rows carrying four reasons. A batch of 50 that
  wholly fails MUST read as one statement about the batch, not as 50 identical rows. Failing file
  names MUST still be recoverable from the outcome; grouping governs how it reads, not what it
  carries.
- **FR-023b** *(added by amendment, 2026-09-11 — narrows FR-023)*: **Where naming a file cannot help
  the author, the count stands in for the names.** FR-023's requirement to name the failures holds
  for reasons the author can act on — a name already taken, a name the folder's glob refuses, a type
  the content type will not admit, a file over a ceiling — because those names are a list of things
  to go and change. It does **not** hold for reasons no per-file action resolves: a permission they
  do not hold, content that expired before the run reached it, and the unclassified fallback. Those
  read as a count, however few there are, because a list the author can act on no part of is noise
  presented as help. A warning group also becomes count-led past eight names, for the reason FR-023a
  gives: naming the first eight of ninety reads as "eight failed".
  - **What this gives up, recorded rather than implied**: past those thresholds the names are not on
    screen anywhere, and the durable notification carries counts only. They survive in the job's own
    record, which the status endpoint returns, so this is a screen nobody has built rather than data
    the server discarded — see the task manager, #33331, where a list of ninety belongs.
- **FR-023c** *(added by amendment, 2026-09-11)*: **An outcome MAY raise one message per severity
  rather than one message.** FR-021, FR-023 and FR-028 speak of "the message" in the singular; an
  outcome that carries both an error-severity reason and a warning-severity one raises two, errors
  first, with the counts stated once in the message read first. The reason is the same one FR-023b
  turns on: a wall the author cannot pass and a file they can rename are different kinds of news, and
  one notification carrying both leaves them to work out which half is theirs to act on.
- **FR-036**: Every failure reason the server can return MUST have its own product copy on the
  client. The server sends a machine-readable reason plus a diagnostic message; the reason is what
  the client maps to text for the author, and the message is never shown (FR-030, backend FR-016a).
  The reasons the client MUST be able to speak to are: the file is larger than the limit; the file
  type is not allowed; a file with that name already exists; the author may not create that file;
  the staged content is no longer available; and an unclassified failure. A reason with no copy is
  a hole the author sees, so the unclassified case MUST also read as a real sentence rather than a
  fallback for a reason nobody wrote. **Adding a reason later is a change to both halves**, never
  to the server alone. A name collision is **case-insensitive** (backend FR-042a): `Report.pdf` and
  `report.pdf` are one contended name, so the client MUST NOT present collision as case-sensitive,
  and any alternative name it suggests MUST differ by more than case.
- **FR-039**: Copy about what may be uploaded MUST describe a **file type** rule, never a file
  extension one: the server resolves the media type by detection and content sniffing rather than by
  trusting the name, so renaming a file does not get it past the rule (backend FR-012a). Where the
  media type cannot be resolved the server skips the check and accepts the file (backend FR-012b),
  so no copy may tell the author that every file is checked.
- **FR-024**: An outcome the client cannot account for — counts that do not close, a response
  carrying no outcome, or a terminal state the client does not recognise — MUST be reported as an
  error. No number may be invented to fill a gap, and an unrecognised outcome MUST NOT be reported
  as a success. This is what makes deferring FR-019 safe.
- **FR-044**: A reload MUST happen only where the folder the author is currently viewing is a
  folder the run actually changed. A refetch of a listing that cannot contain the run's results
  costs a request and, given FR-043, takes the author's selection for no visible gain. Resolved per
  operation, because it is a folder comparison and not a property of the operation:
  - **Upload**: only when the current folder is the run's **destination**. An author who uploaded
    into `/images/` and then browsed to `/docs/` sees no reload of a listing that cannot contain
    the new files.
  - **Move**: when the current folder is the run's **source**, where rows leave it, or its
    **destination**, where rows arrive. The source is the case a destination-only rule would miss,
    since the visible change there is a removal.

  Where neither holds, the listing is left alone. The author is still told the run finished
  (FR-021), the message names the folder it ran on (FR-028), and navigating there loads fresh
  regardless. Announcing is a separate decision from reloading and is not governed by this
  requirement.
- **FR-025**: The content listing MUST refresh once when a run reaches a terminal state, not per
  item, and only where FR-044's predicate says the current view can show it. That refresh MAY be
  **deferred**, and MUST be wherever firing it immediately would disturb the author (FR-043).
  C-006a is what makes deferring safe: search-index visibility is already resolved before the
  completion signal arrives, so the refresh is correct whenever it runs rather than only
  immediately.
- **FR-043**: A refresh MUST NOT be fired while the author is mid-task. Today's refresh clears the
  current selection unconditionally, so a run completing while the author has rows selected for a
  workflow action would take that selection away from them, which is what FR-026 forbids. The
  refresh MUST be held while a dialog is open, a selection is live, or an inline edit is in flight,
  and MUST run at the next boundary where none of those hold. The durable outcome record is what
  makes holding safe: a deferred refresh is a later outcome, never a lost one.
- **FR-026**: An outcome arriving while the author is in the middle of something else MUST NOT
  interrupt them.
- **FR-027**: The client MUST only report outcomes for runs it started. An outcome broadcast to this
  author from another session, tab or context MUST NOT be reported as this one's.

**What the outcome says**

- **FR-028**: Where an operation's outcome is announced, the message MUST name the operation and
  the item or count it applied to, on success and on failure alike. For a run that finished away
  from the author's attention this MUST include the folder it ran on, since they may be looking at
  a different one (FR-044).
- **FR-028a**: **Whether** an outcome is announced turns on whether the author was there to watch
  it. A **backgrounded** run always announces on reaching a terminal state (FR-021): it may settle
  minutes later, in another folder, or with its reload deferred, so there is no moment at which the
  listing speaks for it. A **synchronous** row action MAY stay silent on success where its rows
  visibly change, which is the existing behaviour this feature preserves rather than the rule it
  sets. Failures always announce, since no listing shows an absence.
- **FR-029**: All author-visible text MUST be translatable. No literal untranslated text may reach
  the interface.
- **FR-030**: Raw server messages MUST NOT be shown to the author. Resolved product copy is shown;
  the raw detail is logged.
- **FR-031** *(amended 2026-09-11 — one exception, named)*: Message definitions that this work makes
  reachable MUST be connected, and definitions it makes obsolete MUST be removed. No definition may
  be left defined and unreferenced, **except the four this work stops using and another consumer
  still references**: `content-drive.file-upload-in-progress` and its `-detail`,
  `content-drive.work-in-progress`, and `content-drive.multiple-files-warning`. The Asset Picker
  still reads them and still uploads a single file by design, so its warning remains true there.
  Deleting them would break a surface this feature does not own; removal belongs to #37370, which
  owns that duplicate upload path. **Content Drive's own usage is removed**, which is what this
  requirement was for.

**Reuse**

- **FR-032**: The capability for following a long server-side run — submit, follow, report progress,
  report the terminal state and outcome, stop — MUST be expressed generically, with no knowledge of
  uploads or of Content Drive, so that the folder copy and bulk delete work
  ([#37062](https://github.com/dotCMS/core/issues/37062) /
  [#37063](https://github.com/dotCMS/core/issues/37063)) can adopt it unchanged.
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

- **C-001** — Submit several files with one target and one upload type in **one call** carrying
  the content, answered immediately with a handle. The client never stages content itself and never
  handles a staging reference. *Consumed by* FR-002.
- **C-001a** — What the one-call shape **costs** this half, recorded as a decision rather than a
  surprise: there is no **per-file upload progress** and no **retry of a single file**. A connection
  dropped part-way through means resending the whole batch. The client MAY reduce the blast radius
  by submitting smaller batches, and nothing on the server side prevents it. *Consumed by* FR-012
  (progress is of the run, not of the upload) and FR-037.
- **C-001a1** — **Every guarantee in this contract begins at the handle.** Surviving the author
  leaving, resumability, cancellation and the durable outcome are all properties of a *run*, and no
  run exists until the submission is answered. While content is still being sent there is no batch:
  if that request dies, nothing is recorded and nobody is notified. Anything already staged is
  reclaimed, so an abandoned upload costs the author nothing and leaves nothing behind, but it also
  produces no outcome to report. *Consumed by* FR-037 and User Story 4, whose promise starts at the
  handle and not at the file chooser.
- **C-001b** — What the one-call shape **buys** this half: the staging clock starts on the server
  immediately before the batch is created, so no submission can carry content that has already
  expired, and there is no window in which an author sits on a confirmation screen while their
  earliest files age out. This is why `STAGED_CONTENT_UNAVAILABLE` is a rare safety net rather than
  a routine outcome. *Consumed by* FR-036.
- **C-002** — Distinguishable submission refusals: no files, bad upload type, missing target, too
  many files, batch over the total size ceiling, permission denied. *Consumed by* FR-006, FR-034
  and the edge cases.
- **C-002a** — A defined answer to "the connection dropped while I was submitting, may I retry?"
  The client may resubmit without risking a second copy of the author's files. *Consumed by*
  FR-037. Which mechanism provides it, the same handle returned or the duplicates refused as
  collisions, is fixed in the plan; the client only depends on retry being safe. If the plan takes
  the collision branch, the client additionally depends on a duplicate resubmission being
  distinguishable from a genuinely all-collided batch (backend FR-040a). Without that, a successful
  retry reports as "every file failed" and FR-037 cannot hold. Settled as the `duplicateSubmission`
  flag on the outcome.
- **C-002b** — A stable set of failure reasons, each mapped to product copy on the client side. The
  server's message is diagnostic and is not displayed. *Consumed by* FR-036. Adding a reason later
  changes both halves.
- **C-003** — Readable progress for a run in flight. *Consumed by* FR-012.
- **C-004** — A way to stop a run in flight. **Not consumed in this pass.** The whole stopping group
  (FR-018 … FR-020) is optional and expected to be delivered by the task manager epic #33331. Noted
  here so the boundary stays complete and so it is clear that adopting it later requires no server
  change.
- **C-005** — A readable terminal state and outcome: counts plus per-item results with a reason and
  a message. *Consumed by* FR-021 … FR-024.
- **C-006** — A pushed completion signal carrying the run's outcome, reaching the browser without
  the client polling, plus a durable record of that same outcome. **Outcome means counts *and* the
  per-file results**, each failure carrying its reason: that is what backend FR-014 … FR-016
  record, and what backend FR-020 entitles the author to read after the fact. *Consumed by*
  FR-023, FR-026, FR-033.
- **C-006a** — The completion signal is emitted **after** the run has resolved search-index
  visibility for the batch, never before. This half depends on the ordering without being able to
  see it: the single refresh FR-025 fires on this signal must find every created file, including
  under an active text filter, which is the one criterion routed to the index rather than the
  database. Reversing the two would surface here as a refresh that misses files. *Consumed by*
  FR-025.

**What C-006 buys this feature**: the outcome survives the author walking away without the client
building anything to make that true. The client renders the pushed signal while the author is
present; the durable record is the server's responsibility. This is why FR-033 can hold.

**Why the wording matters**: both specs previously summarised this as "the run's counts", which says
less than backend FR-014 … FR-016 actually record. A counts-only durable record would leave an
author who stepped away with "27 of 30 created" and no way to learn which three, which FR-023
forbids. The per-file results are already in the contract; this states it so no plan reads the
summary and builds the thinner thing.

**Settled during planning**: the concrete submission format is agreed and written down in
[`specs/37166-bulk-file-upload/contracts/bulk-upload-api.md`](../37166-bulk-file-upload/contracts/bulk-upload-api.md),
which both halves reference. It pins `POST /api/v1/assets/_bulkupload` (multipart: repeated `files`
parts plus a JSON `form` part carrying `baseType`, exactly one of `folderId` / `siteId`, and an
optional `totalSizeBytes`), the `202` handle as `entity.jobId` plus `entity.statusUrl`, the outcome
field names, and the closed reason set. **Field names there are binding**: changing one is a change
to both halves.

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
- **SC-008**: The run-following capability is adopted by #37062 / #37063 without modification to it.
- **SC-009** *(amended 2026-09-11)*: No message definition used by Content Drive is left defined and
  unreferenced **other than the four FR-031 names**, and none referenced is left undefined. The
  second half is the one that matters and is met in full: every failure reason and every upload key
  the client can resolve has a definition, checked mechanically rather than by eye.
- **SC-010**: An author without the right to add children to a target cannot reach an upload into it
  by any route, and in every case is told why rather than being left to discover it.
- **SC-011**: An author using a screen reader learns that work started and how it ended, and is not
  interrupted repeatedly while it runs.
- **SC-012**: Every failure reason the server can return renders as product copy the author can act
  on. Measured as zero reasons that fall through to a blank, a code, or a raw server message.

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
  introduced: the right to upload a batch is the existing right to add children to the target, and
  the existing gate on every route into an upload must survive this work unchanged (FR-034).
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
  the drag-and-drop move between folders, and the Action Center. Filed as
  [#37322](https://github.com/dotCMS/core/issues/37322).
- **Bulk reindex's reporting.** Its exclusion from the in-flight indicator is a deliberate existing
  decision, and the notification record already covers its outcome. Unchanged here.
- **Directory upload.** Dropping a whole folder from the author's computer and recreating its
  hierarchy inside Content Drive. Deferred by #37166.
- **Resumable or chunked single-file upload.**
- **A general-purpose background-jobs management screen** (FR-033). One is owned by
  [#33331](https://github.com/dotCMS/core/issues/33331) and is the expected home for stopping a run
  (User Story 5), but this feature must not depend on it existing, and does not build it.
- **Mixing upload types within one batch** (FR-003).

---

## Planning Obligations

- **Test coverage** (Constitution V). The plan MUST name which layers this feature exercises and
  which it does not, with the reason for each omission. Silence is not an acceptable answer to
  Principle V. At minimum it MUST say what is covered by component and store unit tests
  (Jest/Spectator), what is covered end to end (Playwright, extending the existing
  `apps/dotcms-ui-e2e/src/tests/content-drive` suite), and how the pushed completion signal (C-006)
  and the in-flight reporting (User Story 3) are exercised, since neither is reachable from a unit
  test of a single component. The per-story **Independent Test** steps above are acceptance criteria
  for a reviewer, not a substitute for this.
- **The submission format**, already recorded in §Contract Consumed: field names, the endpoint and
  the shape of the handle are not settled at this altitude, and the two halves are being built by
  different developers, so both plans MUST reference one written definition. This now also covers
  how the client declares the batch total that FR-038 requires.
- **Progress readback is the riskiest dependency.** User Story 3 is P1 and rests on C-003, which the
  backend spec now carries as its *first* Planning Obligation and its largest piece of hidden work:
  the job framework's outcome shape is emitted once at the terminal state and cannot serve a run
  that must read its own progress back, so a store for it is either a framework capability or
  feature-owned. The frontend plan MUST NOT assume progress readback is free, and MUST say what
  User Story 3 degrades to if it arrives later than the rest of the feature.

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
- Stopping a run is expected to arrive with the task manager epic #33331 rather than in this
  feature. If it were instead built here, it would live on the in-flight indicator, since once the
  dialog that started a run has closed the indicator is the only surface still representing it.
  That placement is the open question, and #33331 is the likelier answer to it.
- An outcome is reported once, by whichever part of the interface is responsible for presenting it,
  rather than by each surface that knows about the run.
- Removing a message definition is safe only where it is unreferenced across the whole client, not
  merely within Content Drive.
- The per-batch file count and per-file size limits are configured and enforced on the server. The
  client learns of them through refusals rather than holding its own copy.
- Authors reaching this are back-office users on a maintained desktop browser; no additional
  compatibility target is introduced.
