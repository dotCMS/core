# Feature Specification: Content Drive folder duplication (backend)

**Feature Branch**: `37062-content-drive-folder-copy`

**Created**: 2026-09-21

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37062](https://github.com/dotCMS/core/issues/37062): "Content Drive: folder copy, async job endpoint and frontend wiring".

---

## Context

Content Drive lets an author select several rows at once. Folders are selectable, and there is no
way to duplicate them: not in bulk, and not one at a time. Unlike delete, which shipped a
single-folder context menu item on #35161, **duplication has no presence in Content Drive at all**.
The only folder copy in the product is the Site Browser's, reached through a legacy remoting method
that no modern surface calls.

**This specification covers the server side only.** #37062 carries both halves; they are specified
separately, following #37063 and #37166, because the two are delivered against a contract rather
than as one body of work. §Contract Consumed by the Client is a mandatory output of this document,
and the frontend half restates it from the consumer's side rather than re-deciding it.

**The operation duplicates a folder in place.** A selected folder is duplicated into its own parent,
beside the original, under a name derived so it does not collide. There is no destination: the
author does not choose where the duplicate lands, so this feature has no destination picker, no
destination path in its submission, and none of the target validation that a relocation would need.
This is a deliberate narrowing of what #37062's description proposes, recorded as D-005.

**One word of terminology, because #37062 uses it the other way.** Where this document says a
**path**, it always means the location of a folder the author selected, the string that says which
folder to duplicate, such as `//demo.dotcms.com/projects/alpha/`. It never means a place a duplicate
is sent to. No such thing exists here: the submission carries selected folders and nothing else, and
every duplicate lands in the parent its source already sits in. The issue's description uses
`sourcePaths` alongside a `destinationPath`, so a reader arriving from it may expect the pair; there
is only the first, and this document prefers to name the thing a **folder** wherever it can.

**And one on the verb.** The operation is a **duplication**, and that is what this document calls
it, because "copy" invites the question of where to. The word "copy" is kept for exactly three
things and means something different in each: the existing Site Browser feature, the code that
performs the work (`FolderAPI.copy` and the event it emits), and, in the client half, the wording
shown to an author, which that document calls *client copy*. Everywhere else, a folder is duplicated
and the thing produced is a **duplicate**.

**Why this cannot be a synchronous call.** Copying a folder is a single unbounded transaction over a
recursive walk: `FolderAPIImpl.copy` is `@WrapInTransaction` and the walk beneath it recurses
through file assets, pages, links and child folders, loading each folder's children whole. A folder
holding thousands of assets holds one long transaction and times out at the proxy, and a
multi-select multiplies that by the size of the selection. Designing a synchronous endpoint around
an expected timeout is an asynchronous design without the machinery, so it is built as one.

**Reuse, stated up front.** Three things already exist and this feature consumes them rather than
redefining them: the per-item outcome contract in `com.dotcms.jobs.business.batch`, shared with bulk
upload and bulk refresh; the job-queue framework in `com.dotcms.jobs.business`; and
`FolderAPI.copy`, which already performs the permission checks and emits the copy event. The sibling
tickets #37063 (bulk delete) and #37165 (move) share the outcome contract; **the three must not
diverge**.

**Duplication is not delete with a different verb, and the differences run in both directions.** It
is gentler in the ways that shaped most of delete's specification: nothing is destroyed, so no
folder becomes unusable while a run works on it, no busy signal is broadcast to other authors, and
two runs over the same folders cannot harm each other. It is harsher in one way delete is not:
**duplication is not idempotent**. Re-running a delete over a folder that is already gone changes
nothing; re-running a duplication produces a second duplicate. See FR-034 and D-015.

---

## User Scenarios & Testing *(mandatory)*

<!--
Server behaviour, described from the perspective of the author whose action reaches it. Each is
verifiable against the server alone, through integration or Postman tests, with no browser involved.
-->

### User Story 1 - A selection of folders is duplicated at once (Priority: P1)

An author's multi-row selection reaches the server as one submission naming several folder paths.
The server accepts it immediately with a handle, duplicates each folder in the background beside its
original, and records what happened to each one.

**Why this priority**: This is the reported gap, and nothing else in the feature has value until a
selection can be submitted as one operation. It is also the only story that must ship for the
feature to be usable.

**Independent Test**: Submit five folders the author may duplicate; confirm the submission is
answered at once, and that five new folders exist beside the originals when the run reports itself
finished.

**Acceptance Scenarios**:

1. **Given** five folder paths the author may duplicate, **When** the submission is made, **Then**
   the server answers immediately with a handle the caller can use to follow and cancel the run. It
   does not hold the caller until the duplicates exist.
2. **Given** an accepted submission, **When** the run finishes, **Then** each source folder still
   exists untouched, and a duplicate of each exists in the same parent.
3. **Given** a duplicated folder, **When** its contents are inspected, **Then** its child folders,
   file assets, pages and links are present, with the folder's permissions copied across.
4. **Given** a folder holding content of a type that is neither a file asset, a page nor a link,
   such as a Blog entry or a Document, **When** it is duplicated, **Then** that content is present
   in the duplicate too. The shipped walk does not carry it and this feature adds it (FR-012d).
5. **Given** a folder holding a published page and an unpublished draft, **When** it is duplicated,
   **Then** the page's duplicate is published and the draft's duplicate is not. Each copy keeps the
   state its source had (FR-012e).
6. **Given** a folder holding an archived item, **When** it is duplicated, **Then** that item is
   duplicated too, and its duplicate is archived like its source.
7. **Given** a folder whose parent is a site root, **When** it is duplicated, **Then** the duplicate
   lands at that site root; **Given** a nested folder, **Then** it lands in its parent folder. Both
   cases work.
8. **Given** a submission naming exactly one folder, **When** it is made, **Then** it is accepted
   and runs as a batch of one. There is no separate synchronous endpoint for a single folder, and
   the submission is not special-cased into one.
9. **Given** any folder in the selection, **When** it is duplicated, **Then** the same permission
   rules apply and the same event is emitted as when that folder is copied through the shipped Site
   Browser copy. What a duplicate *holds* deliberately differs, by FR-012d.

---

### User Story 2 - The duplicate is named so it never collides (Priority: P1)

A duplicate lands beside its original, so its name always collides by construction. The server
derives a free name by the rule the shipped copy already applies, rather than refusing.

**Why this priority**: P1 because it is not an edge case here. Under duplicate-in-place **every
single duplicate collides**, so the naming rule is the operation's normal path rather than a rare
branch. A rule that refused, or that overwrote, would make the feature unusable rather than
imperfect.

**Independent Test**: Duplicate the same folder three times; confirm three new folders exist, all
named distinctly, and that no existing folder was disturbed to make room for any of them.

**Acceptance Scenarios**:

1. **Given** a folder is duplicated, **When** the duplicate is created, **Then** it is given a name
   that is free in that parent, derived by the rule the shipped copy already uses.
2. **Given** a folder that has already been duplicated, **When** it is duplicated again, **Then** a
   second duplicate is created under a further derived name, and neither the original nor the first
   is disturbed.
3. **Given** any duplication, **When** it completes, **Then** no existing folder has been renamed,
   replaced or overwritten to make room for the duplicate.

---

### User Story 3 - A folder that cannot be duplicated does not take the run down with it (Priority: P1)

A selection routinely contains a folder the author may not duplicate, one whose path no longer
resolves, or one the system refuses outright. The run continues past it and records, per folder,
what happened and why.

**Why this priority**: Partial failure is the normal case over a multi-select, not an edge case. A
run that aborts at the first refusal leaves the author unable to tell which duplicates were made.

**Independent Test**: Submit five folders of which two cannot be duplicated, one for want of rights
and one whose path does not resolve; confirm the other three were duplicated and the outcome reports
three succeeded and two failed, naming both failures with distinguishable reasons.

**Acceptance Scenarios**:

1. **Given** a selection containing a folder that is refused, **When** the run proceeds, **Then**
   every remaining folder is still attempted.
2. **Given** a finished run, **When** its outcome is read, **Then** it carries a total, a success
   count, a failure count, a skipped count and a per-folder record.
3. **Given** a folder that failed, **When** its record is read, **Then** it names that folder, marks
   it failed, and carries both a machine-readable reason and a human-readable diagnostic message.
4. **Given** a folder the author may not read, or whose parent the author may not add to, **When**
   the run reaches it, **Then** it is recorded as that folder's own failure with a permission
   reason, not as a failure of the submission.
5. **Given** a submitted path that no longer resolves to a folder, **When** the run reaches it,
   **Then** that entry gets its own failure record, distinguishable from a permission refusal.
6. **Given** a selection where every folder fails, **When** the run finishes, **Then** the outcome
   shows zero successes and names every failure. The run is not recorded as a success.

---

### User Story 4 - A cancelled run never leaves a half-copied folder (Priority: P2)

The author cancels a run that is part-way through. Every folder in the selection has either been
fully duplicated or not touched at all. No partially populated duplicate is left behind.

**Why this priority**: P2 rather than P1 because a partial duplicate is recoverable in a way a
partial delete is not: the author can delete it. It still matters, because a half-populated
duplicate that looks complete is a quiet data problem, and because the guarantee is free under the
current transaction boundary and is only lost if someone deliberately gives it up.

**Independent Test**: Cancel a run mid-selection; confirm the folders already completed have
complete duplicates, no partially populated duplicate exists, and the untouched remainder is
recorded as skipped.

**Acceptance Scenarios**:

1. **Given** a run in progress, **When** it is cancelled, **Then** the cancellation takes effect
   between top-level folders and never mid-subtree.
2. **Given** a cancelled run, **When** its outcome is read, **Then** each folder is recorded as
   succeeded, failed or skipped, and the skipped ones are distinguishable from the failed ones.
3. **Given** a cancelled run, **When** the site is inspected afterwards, **Then** no duplicate
   exists that holds only part of its source's contents.
4. **Given** a folder whose duplication fails part-way through for any reason, **When** the failure
   is recorded, **Then** no partial duplicate of it remains.

---

### User Story 5 - The author learns how it ended, even if they walked away (Priority: P2)

The submission is answered long before the work is done, so something has to close the loop. When
the run reaches a terminal state the submitter is told, pushed to them if they are still looking and
recorded durably so the outcome survives navigating away or closing the tab.

**Why this priority**: P2 because the job framework already exposes a readable terminal status, so
the feature works without the push. It is not optional work: an author who duplicated forty folders
and closed the tab must still be able to find out what was made, and under FR-034 they need it more
than a delete author does, because resubmitting blindly creates duplicates rather than doing
nothing.

**Independent Test**: Submit a run, disconnect, reconnect after it finishes, and confirm the
per-folder outcome is still readable and a durable notification was addressed to the submitter.

**Acceptance Scenarios**:

1. **Given** a run reaching any terminal state, **When** it resolves, **Then** the submitter is
   notified, and only the submitter.
2. **Given** a notified run, **When** the notification is read, **Then** its wording reflects what
   happened: a clean run, a partial one and a cancelled one read differently.
3. **Given** a finished run, **When** its outcome is requested later, **Then** the counts and the
   per-folder records, keyed by the folders that were submitted, are still readable.
4. **Given** a run whose notification could not be delivered, **When** that happens, **Then** the
   run is still recorded as having finished.

---

### Edge Cases

- **A selected folder is an ancestor of another selected folder.** The descendant is **skipped**:
  duplicating the ancestor already carries it, so acting on it again produces clutter the author did
  not ask for. It is also the only way to make the run deterministic, which is the stronger reason.
  See FR-016.
- **The same path appears twice in one submission.** Deduplicated before the run, so the author gets
  one duplicate rather than two, and the outcome reports the path once (FR-015).
- **An empty path list, or one over the configured maximum.** Refused at submission, before a job
  exists (FR-004).
- **A folder the author may read but whose parent they may not add to.** A per-folder permission
  failure. The two rights are separate and either can be the one that is missing (FR-012).
- **A folder at a site root.** Duplicated through the site rather than through a parent folder. Both
  paths exist in the shipped API and both are on the hot path here (FR-008).
- **An asset inside the folder that the author cannot read.** It is copied anyway: the walk beneath
  the two entry checks runs as the system user. Documented and unchanged (FR-012b, D-009).
- **An asset that is missing from the search index.** Copy resolves a folder's contents from storage
  rather than from the index, so unindexed content is carried. This is a difference from delete,
  which resolves contents by querying the index.
- **A folder whose name already ends in the suffix the rename rule appends.** The rule appends
  again, so names grow with each duplication. Known and accepted behaviour (FR-010, D-008).
- **Cancellation arriving while the last folder is in progress.** There is nothing left to skip; the
  run finishes that folder and reports as cancelled with an empty skipped set.
- **A run is abandoned and re-queued.** Folders the first attempt completed are duplicated **a
  second time**, because a duplication that succeeded leaves nothing that would make a retry a
  no-op. This is the one place duplication is materially more dangerous than delete on retry. See
  FR-034 and D-015.
- **Two authors duplicate the same folder at the same time.** Both succeed, each duplicate takes a
  distinct derived name, and neither run is refused. There is no overlap guard and none is needed
  (FR-033).
- **Every folder in the submission is refused at submission-time validation.** Still a submission
  refusal, not an accepted job that fails immediately. A caller must be able to tell "you sent me
  nothing usable" from "the work failed".

---

## Requirements *(mandatory)*

### Functional Requirements

#### Submission

- **FR-001**: The system MUST expose a submission that accepts several folder paths in one request
  and answers immediately, before any folder is copied.
- **FR-002**: The answer MUST carry a handle the caller can use to follow the run, cancel it and
  read its outcome, plus a ready-to-use address for doing so. The caller MUST NOT have to assemble
  that address itself.
- **FR-003**: The answer MUST state how many folders the **server** accepted into the run, and that
  number MUST equal the total the outcome later reports.
- **FR-004**: A submission that is malformed, meaning no folders, an empty list, or more folders
  than the configured maximum, MUST be refused before any run is created, with distinguishable
  refusals.
- **FR-004a**: The configured maximum MUST be **advertised** in the application configuration the
  server already publishes at `/api/v1/appconfiguration`, read from the same constant the endpoint
  enforces with so the two cannot disagree. This is how bulk upload exposes its ceilings today
  (`ConfigurationHelper`), and it is what lets the client check a selection before submitting it.
  The refusal remains the authority; the advertised value is the courtesy. The refusal MUST NOT be
  relied on to carry the number, because the shared refusal shape puts detail in free-text wording
  that the contract does not treat as stable.
- **FR-005**: A caller who is not entitled to use the operation at all MUST be refused at
  submission. Per-path permission is a different question and is FR-012.
- **FR-005a**: A submission's folder paths are recorded with the run and are therefore readable by
  any back-end user, including paths on sites they have no rights to, because the listings that
  expose in-flight runs gate on "is a back-end user" and nothing more. This feature inherits that
  from the job framework and **does not depend on it**: unlike bulk delete, which asked for the
  listing to stay readable so it could mark folders across authors (its D-015), duplication marks
  nothing and would be unharmed by a narrower listing. Recorded so the exposure is not later read as
  a choice this feature made.
- **FR-006**: The system MUST NOT change the shipped Site Browser folder copy, its behaviour or its
  entry point. It is the only folder copy in the product today and is not this feature's to move.
- **FR-007**: The maximum number of folders one submission may carry MUST be configurable, and its
  default MUST be **50**, the same as bulk folder delete's, so the two folder operations refuse at
  the same size (D-019). Over the limit the submission is refused before any run exists.

  **The cap bounds the selection, not the tree.** A single selected folder holding a hundred
  thousand items counts as one. What protects the server from one very large folder is not this
  number; see FR-024 and D-019.
- **FR-007a**: The system MUST NOT add a synchronous single-folder duplication endpoint. A single
  folder is submitted through the same asynchronous endpoint as a batch of one. The cost of copying
  is set by the size of the folder rather than the size of the selection, so a synchronous
  single-folder path would carry exactly the proxy-timeout exposure this feature exists to remove,
  and would require a second error mapping and a second set of client copy for the same failures.

#### Where the duplicate lands and what it is called

- **FR-008**: Each folder in the selection MUST be duplicated into **its own parent**. A folder
  whose parent is a site root is duplicated at that site root; a nested folder is duplicated into
  its parent folder. Both cases MUST work, and the submission MUST NOT carry a destination.
- **FR-009**: The duplicate MUST be given a name that is free within that parent, derived by the
  rule the shipped copy already applies. The system MUST NOT refuse a duplication because the
  source's name is taken: under this feature it always is.
- **FR-009a**: The naming rule MUST be stated in the published API description, so a caller can work
  out what a duplicate will be called. The outcome MUST NOT be required to carry the chosen name:
  the suffix the rule appends is well known, and adding a field to the shared per-item contract to
  restate it is not worth the coupling. One consequence belongs in that description rather than
  being discovered: the rule appends to the name it finds rather than counting, so a second
  duplicate of the same folder carries the suffix twice. The name follows from the rule and from
  what is already in the parent, not from the source name alone.
- **FR-010**: Repeated duplication of the same folder MUST keep producing new folders under further
  derived names, and the system MUST NOT overwrite, merge into, or rename an existing folder to make
  room. That the derived names grow longer with each duplication is known, visible behaviour and is
  not corrected here (D-008).
- **FR-011**: A duplication MUST leave its source folder, and everything in it, untouched.

#### Per-path execution and outcome

- **FR-012**: Each folder in the selection MUST be attempted independently: a refusal or failure on
  one MUST NOT abort the run or roll back a folder already duplicated. A per-folder permission
  refusal MUST be recorded as that path's failure with a machine-readable reason. Both rights the
  operation needs, reading the source and adding to its parent, MUST be distinguishable in what is
  reported, because either one alone can be the missing one. **The shipped enum cannot express that
  distinction**: it carries a single permission-denied value. Telling the two apart therefore
  requires a new, additive value, which is exactly the case FR-022 says must be agreed with the
  client half before either is implemented.
- **FR-012a**: The bulk path MUST NOT be more permissive than the shipped copy. The same two checks
  apply, against the same rights, before any folder is duplicated.
- **FR-012b**: The system MUST define what happens to content within the subtree that the acting
  user cannot read. **Today the entire recursive walk beneath the two entry checks runs as the
  system user**: the folder's file assets, pages, links and child folders are all resolved and
  copied with system rights, so content the acting user cannot see is duplicated along with
  everything else. The folder's permissions are copied to the duplicate, so it is no more visible
  than the source was. **This feature does not change that** (D-009). It is recorded here because it
  means a successful duplication can create content the submitting author was never able to read,
  and because a reader who assumes the two entry checks govern the whole operation will be wrong
  about what the feature does.
- **FR-012d**: A duplicate MUST hold **everything its source folder holds**. That includes two
  things the shipped walk leaves behind: **generic contentlets**, meaning content of any type that
  is neither a file asset, a page nor a link, such as a Blog entry, a Document or any custom type
  with a folder field; and **archived items** of every type, which the walk's working filter skips.
  *(Developer decision, 2026-09-23: copy all content in the folder.)*

  **This closes a gap in the shipped walk rather than describing it.** The walk copies exactly four
  things: file assets, working pages, links and child folders. It has no branch for anything else,
  while folder *deletion* resolves every contentlet in the folder regardless of type. So today the
  two operations disagree: delete destroys generic content and copy silently leaves it behind while
  reporting the folder as a success. That is tolerable in the Site Browser, whose tree is mostly
  files and pages. It is not tolerable in Content Drive, whose entire premise is a listing of mixed
  content, where an author duplicating a folder is looking at the very rows the copy would drop.

  **The gap MUST be closed without changing the shared walk** (D-018). The enterprise site-copy job
  already faced this and solved it by carrying generic content itself rather than by extending the
  folder factory, and that is the precedent to follow: this feature's processor is responsible for
  the content the walk does not carry. Extending the factory would change behaviour for the Site
  Browser and for every plugin calling `FolderAPI.copy`, which is the rollback-unsafe shape D-012
  exists to avoid.
- **FR-012e**: The system MUST state which content it carries and in what state, because nothing in
  either half said so and the answer is less obvious than it looks.

  **Every item in the folder is carried**, whatever its state (FR-012d). The shipped walk selects
  its sources on a working basis, file assets only where working and not archived, pages through the
  working-pages lookup, links only where working, so on its own it would skip archived items. This
  feature carries what the walk skips, in the same place it carries generic content.

  **Each copied item keeps the state its source had.** The content copy copies every version of the
  source, oldest first, and reproduces its state: a version that was live is set live on the copy,
  and a version that was archived is marked deleted. So a published page duplicates as a published
  page, a draft as a draft, and an archived item as an archived item. A reading of the working
  filter alone suggests published pages come out as unpublished working copies, which is why this is
  stated rather than left to be inferred.

  Generic and archived content added by this feature MUST follow the same rule, so a duplicate does
  not treat a Blog entry differently from a page. Links are the one type whose state handling was
  not traced, since they are copied through a separate legacy path, and the plan MUST confirm it.

  **"Everything" has two practical limits the plan MUST account for rather than discover.** The
  shipped lookup that resolves a folder's contentlets is an **index search**, not a read of storage,
  so content missing from the search index is invisible to it, exactly as it is to folder deletion.
  And it filters its results by the read rights of the user passed in, so it returns everything only
  when called with rights to see everything, which is what the walk's system-user behaviour implies
  (FR-012b). Whether archived items come back from that search at all MUST be verified rather than
  assumed; if they do not, the plan names a lookup that does.
- **FR-012c**: The contract MUST NOT imply that a client-side permission check can predict the
  outcome, and the server MUST enforce both rights regardless of what the client checked. Under
  duplicate-in-place the client is better placed than bulk delete's, because the target parent is
  the folder it is already browsing, so it can see the add-children right as well as each folder's
  own. That makes its gate a good filter rather than a guess, and it is still not a guarantee:
  rights can change between the check and the run, a selection reached through a search can span
  parents the client never resolved, and a folder's own rights may be unknown to the listing. The
  per-folder outcome remains the only authority.
- **FR-013**: A submitted path that does not resolve to a folder, whether the folder is gone, the
  path names a file, or the path is malformed, MUST be recorded as that entry's own failure,
  distinguishable from a permission refusal.
- **FR-014**: A folder the system protects MUST be recorded as that folder's own failure with a
  reason saying it is protected, and the rest of the selection MUST still run.
- **FR-015**: The same folder named twice in one submission MUST be collapsed before the run, so one
  submission of the same folder twice produces one duplicate and one outcome record.
- **FR-016**: When one selected folder is an ancestor of another selected folder, only the ancestor
  MUST be duplicated. The descendant MUST be recorded as **skipped**, with a reason saying an
  ancestor in the same submission already covers it, and MUST be distinguishable from a folder
  skipped because a cancellation stopped the run before reaching it.

  **Why skipped rather than duplicated**, since the two folders land in different places and an
  earlier draft of this document treated that as reason enough to do both. Acting on both is
  **order-dependent**, and there is no ordering that is correct. Duplicating the descendant first
  creates its duplicate inside the original ancestor, so the ancestor's duplicate, made afterwards,
  contains the descendant *and* the descendant's duplicate. Duplicating the ancestor first produces
  an ancestor duplicate containing only the descendant. Same submission, two different results
  depending on the order the run happens to process the selection. A specification that permits both
  has to pick an order and defend it; skipping the descendant removes the question. The author also
  gets what they almost certainly meant, which is the subtree duplicated once.

  This aligns with bulk delete, which also skips a descendant covered by a selected ancestor, though
  the two reach it differently: delete's descendant no longer exists by the time the run reaches it,
  while this one still does and is deliberately left alone. The wording shown to an author must
  therefore say the ancestor **covers** it, not that the ancestor removed it.
- **FR-017**: Every folder in the run MUST produce exactly one outcome record.
- **FR-018**: The outcome MUST use the shared per-item contract already in use by bulk upload and
  bulk refresh, extended additively if at all.
- **FR-019**: The outcome MUST carry a total, a processed count, a success count, a failure count, a
  skipped count and a per-folder record with a three-valued status.
- **FR-020**: Every failure reason MUST be machine-readable and drawn from a stable, enumerated set,
  and MUST carry a separate human-readable diagnostic message intended for a log rather than for
  display.
- **FR-021**: Reasons MUST be derived from facts the system established, never guessed from the
  shape of an exception.
- **FR-022**: Any new failure or skip reason this feature needs MUST be agreed with the client half
  before either is implemented, and MUST be added to the shared set additively. Duplication needs
  fewer reasons than delete, with one exception: it has no analogue of "something inside is in use",
  but it **does** need a skip reason for a folder an ancestor in the same submission already covers
  (FR-016). Delete's equivalent says the ancestor removed it, which is not true here, so either the
  shared value is worded to cover both or a second value is added. That choice binds both halves and
  belongs in the plan, not in implementation.

#### The transaction boundary

- **FR-023**: One top-level folder MUST be copied as **one transaction**, which is what the shipped
  copy already does.
- **FR-024**: Duplicating a folder **costs more** than copying it through the shipped Site Browser
  path, and the specification MUST say so rather than promise otherwise. The shipped copy carries
  working file assets, pages, links and child folders; a duplicate here also carries generic content
  and archived items, and every item is copied with its full version history (FR-012d, FR-012e). All
  of it sits inside the same single transaction per top-level folder. Duplication MUST NOT be less
  atomic than the shipped copy, and MUST NOT add cost beyond what carrying that extra content
  requires: in particular, the content this feature adds MUST NOT be loaded all at once for the
  whole subtree when it can be read folder by folder alongside the walk.

  An earlier draft required duplication to be no slower or hungrier than the shipped copy. That was
  true while it only wrapped the walk, and stopped being true the moment it was decided to copy
  everything in the folder.
- **FR-025**: The known costs of FR-023 MUST be recorded rather than discovered: a folder's children
  are loaded whole at each level rather than paged, so peak memory is bounded by the widest single
  folder in the subtree; the transaction, and with it a database connection and its locks, is held
  for the whole walk; and one persistence session accumulates every entity the walk touches until it
  commits. Carrying everything in the folder makes each of these larger. **Nobody has measured where
  this becomes dangerous** for duplication; the only measurement in either sibling spec is delete's,
  roughly eight minutes for a folder of twenty thousand files. Finding that number is the first job
  of the deferred bounding work.
- **FR-026**: All-or-nothing MUST hold under **every** interruption: cancellation, process death and
  a failure part-way through a folder. A duplicate that holds only part of its source's contents
  MUST NOT survive any of them. **This corrects #37062**, which states that a cancelled copy leaves
  the partial subtree in place at the destination and that the author can delete it. That is true
  only if the recursion is broken into separate transactions, which D-012 defers, so under this
  specification the stronger guarantee holds and the weaker wording must not be used.

#### Progress, cancellation and concurrency

- **FR-027**: The system MUST report progress while a run is in flight, and MUST report it only when
  the rounded percentage changes rather than on every item.
- **FR-028**: A run that is working MUST NOT be mistaken for one that has stalled. A single folder
  can take many minutes with no progress event, so whatever the framework uses to detect abandoned
  work MUST tolerate that silence.

  **This is load-bearing, not a tolerance.** Under FR-034 an abandoned run is not retried, so being
  wrongly marked abandoned ends a healthy run permanently. The signal that the run is alive
  therefore cannot come from the code doing the work: duplicating one folder is a single blocking
  call that returns only when the whole subtree is copied, exactly as bulk delete's single-folder
  call blocks until the folder is gone. Bulk delete found this first and had to keep the run visibly
  alive from outside the blocking call; the plan MUST say how duplication does the same.
- **FR-029**: Progress MUST be reported at the granularity that actually exists, which is completed
  top-level folders and nothing finer, and the contract MUST say so rather than leaving the client
  to infer precision the number does not carry.
- **FR-030**: An in-flight run MUST be cancellable, and the cancellation MUST take effect between
  folders so that FR-026 holds.
- **FR-031**: Folders not reached when a cancellation takes effect MUST be recorded as skipped, and
  MUST be distinguishable from failures: they were never attempted, not refused.
- **FR-032**: A cancelled run's outcome MUST record where it stopped, so the remainder can be
  resubmitted deliberately.
- **FR-033**: The system MUST NOT refuse a submission because it overlaps an in-flight run, and MUST
  NOT carry an overlap guard. Two runs duplicating the same folder, or duplicating into the same
  parent, cannot interfere: neither destroys anything, and the naming rule gives each duplicate a
  distinct name. **This is a deliberate omission of a requirement bulk delete carries** (its
  FR-029), recorded so its absence reads as a decision rather than an oversight.
- **FR-034**: The processor MUST record that **duplication is not idempotent**: a folder that was
  duplicated successfully leaves nothing that makes a second attempt a no-op, and the framework
  holds no durable per-item state, so a re-run restarts from the first folder and duplicates again
  everything the first attempt completed, under further derived names. There is no backstop of the
  kind bulk upload has, where a unique index on a file asset's lower-cased path rejects the second
  create; the naming rule simply derives the next free name.

  **A re-run can be prevented, and the plan MUST decide whether to.** `@NoRetryPolicy` stops it: an
  abandoned job put back in the queue is routed through the same retry gate as a failed one, which
  for a no-retry processor declines and marks the run permanently abandoned, still firing its
  completion events. An earlier draft of this document said the opposite, relying on a comment in
  `BulkUploadProcessor` that is itself wrong; bulk delete found and corrected the same claim on its
  own processor.

  **The recommendation is to mark the processor no-retry**, and it points the other way from
  delete's choice for a reason worth keeping. Delete left the annotation off because a second pass
  was *useful* to it, correcting its own report. Duplication has nothing to correct and everything
  to repeat. The cost of no-retry MUST be stated with it: a run abandoned mid-way ends permanently,
  and because per-folder results are held in memory until the run finishes, its outcome is lost with
  it. The author learns it ended without learning which folders were duplicated. That is the trade,
  and it is why FR-028 is load-bearing: not being marked abandoned in the first place is the main
  defence.

#### Telling the author

- **FR-035**: On any terminal state the system MUST notify the **submitter**, and only the
  submitter.
- **FR-036**: The notification MUST be both pushed, so a present author sees it without polling, and
  durably recorded, so an absent one can find it later.
- **FR-036a**: "Durable" MUST be given a lifetime. The retention of completed runs and their
  outcomes MUST be stated rather than assumed to be forever.
- **FR-037**: The notification's wording MUST reflect what happened: a clean run, a partial one and
  a cancelled one MUST read differently, and a cancelled run MUST NOT read as a fault.
- **FR-038**: Notification MUST be best-effort. A failure to deliver it MUST NOT change the run's
  recorded outcome.
- **FR-039**: The system MUST emit a distinguishable completion signal type, so a client can tell a
  finished copy from a finished run of any other kind without inspecting its payload.
- **FR-040**: The system MUST NOT add a busy announcement of the kind bulk delete broadcasts, where
  every author who may see a folder is told it has entered a delete and later that it has left one.
  That exists so nobody walks into a folder being destroyed; nothing about a folder being duplicated
  makes it unsafe to use, so it has no purpose here (D-016).

  **One announcement already exists and MUST be left as it is.** `FolderAPIImpl.copy` emits
  `COPY_FOLDER` on commit, once per duplicated folder, to every author with read rights on it,
  excluding the submitter. That is a completion notice, not a busy signal: it arrives after the
  duplicate exists and says nothing about a run in flight. This feature inherits it by calling the
  shipped method, and a client listening for it may use it to refresh a listing another author is
  duplicating into.

#### Contract and documentation

- **FR-041**: The published API description MUST state that the operation is asynchronous, name the
  queue it enqueues onto, and point at the generic job status, cancel and monitor addresses.
- **FR-042**: The published schema MUST match what the endpoint actually returns, and the generated
  API document MUST be regenerated from the annotations and committed alongside the change.
- **FR-043**: Configuration this feature introduces MUST ship with documented defaults.

### Key Entities

- **Submission**: the folder paths an author asked to duplicate, accepted as one unit.
- **Run**: one accepted submission, identified by a handle, with an observable state and an outcome.
- **Per-folder outcome**: one record per submitted folder, carrying an identifying key, a
  three-valued status, and a machine-readable reason plus diagnostic message on failure. Exactly the
  shared shape, with nothing added: the shipped record carries a **key**, a status and an optional
  reason and message, and no field for a path, so the folder's path travels as that key the way an
  uploaded file's name does.
- **Duplicate**: the folder the run created, living in the same parent as its source under a derived
  name.

---

## Contract Consumed by the Client *(mandatory: the frontend half of #37062 depends on it)*

- **C-001**: **One call, one handle.** The client sends the selected folder paths in a single
  ordinary request and is answered immediately with a handle and a ready-made address for following
  the run. No destination travels with the submission (FR-001, FR-002, FR-008).
- **C-002**: **Every guarantee in this contract begins at the handle.** Surviving the author
  leaving, the outcome being readable later, and the completion signal are all properties of the
  run, not of the request that created it (FR-002).
- **C-003**: **A count the client should display is returned at submission** (FR-003), and it equals
  the total the outcome later reports.
- **C-004**: **Distinguishable submission refusals** for: nothing submitted, over the configured
  maximum, and not entitled (FR-004, FR-005). **There is no overlap refusal**, because there is no
  overlap guard (FR-033). A client written against bulk delete's contract must not carry copy for a
  refusal this operation cannot produce. **The maximum itself is read from the advertised
  application configuration** (FR-004a), not from the refusal.
- **C-005**: **A stable, enumerated set of failure and skip reasons**, each mapped to client copy
  (FR-020). The server's message is diagnostic and is never displayed. Failures: no rights on the
  folder, no rights to add to its parent, the folder no longer resolves, the folder is protected,
  and a general fallback. Skips: the run was cancelled before reaching it, and **an ancestor in the
  same submission already covers it** (FR-016). The second skip reason needs wording of its own,
  because delete's equivalent says the ancestor removed the folder and here the folder is still
  there, untouched and usable.

  **Two facts about the shipped enum that bear on this and were checked rather than assumed.** It
  carries seven values today, aimed at bulk upload: over the size limit, disallowed file type, name
  collision, folder filter mismatch, permission denied, staged content unavailable, and
  unclassified. Only permission denied is one duplication emits, so "the folder no longer resolves",
  "the folder is protected" and the ancestor skip are all **additions**, not reuses. And the type is
  named for *failures* while the ancestor case attaches to a **skipped** item, whose shipped
  documentation currently says a skip means the run was cancelled before reaching it. Adding a
  second meaning to skipped is a change to a shared vocabulary, not a local one, so it goes through
  both halves and through whoever owns bulk upload.
- **C-006**: **Progress, and an honest statement of what it counts** (FR-027, FR-029). It counts
  completed top-level folders and nothing finer, so the client must not render a proportion.
- **C-007**: **A way to cancel, with this operation's guarantee and not the one #37062 describes**
  (FR-026, FR-030, FR-031). Each folder is left either fully duplicated or not created at all. Any
  wording stating that a partial copy is left behind is wrong under this specification.
- **C-008**: **A readable terminal state and outcome** (FR-019): counts plus per-folder records,
  each failure carrying its reason. **Nothing is added to the shared shape**: a successful record
  names the path that was submitted, not the name the duplicate was given (FR-009a). The naming rule
  is published with the endpoint instead, so a client that wants to tell an author what to look for
  states the rule rather than reading a field.
- **C-009**: **A pushed completion signal carrying the outcome, plus a durable record of the same**
  (FR-035, FR-036, FR-039). The client renders the push while the author is present and follows the
  durable record afterwards; it does not poll for completion and does not build a jobs screen.
- **C-010**: **The signal is emitted after the copies are complete**, so a listing refreshed on it
  shows the new folders rather than racing them.
- **C-011**: **No busy announcement, and nothing needs marking** (FR-040). A folder being duplicated
  stays fully usable: it can be opened, uploaded into and dragged onto while a run works on it, and
  neither the source nor any other folder is made inert. A client written against bulk delete's
  contract must not carry its marking machinery here. The one event that does reach other authors is
  the existing per-folder `COPY_FOLDER` completion notice, which says a duplicate now exists, never
  that a run is working.

**Explicitly the server's business, not specified by the client half**: how a run is executed, the
transaction boundary, what happens to content within a subtree the author cannot read, retry and
abandonment behaviour, and the durable record's lifetime.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An author duplicates a selection of N folders with one action, where today no folder
  can be duplicated from Content Drive at all.
- **SC-002**: The submission is answered within the same time regardless of how much content the
  selected folders hold, and zero submissions fail for having taken too long to answer.
- **SC-003**: 100% of accepted folders produce exactly one outcome record: succeeded, failed with a
  reason, or skipped with which kind of skip. Zero are silently dropped.
- **SC-004**: 100% of successful duplications are reported against the path that was submitted, so
  an author can tell which of the folders they selected were duplicated and which were not.
- **SC-005**: Zero duplicates exist that hold only part of their source's contents, across
  cancellation, failure and process death.
- **SC-006**: A folder being duplicated remains fully usable throughout the run, in 100% of cases.
- **SC-007**: Every failure reason the server can emit is drawn from the enumerated set, and zero
  diagnostic messages reach an author.
- **SC-008**: An author absent when a run ended can determine the full outcome, including which of
  the folders they selected were duplicated, without having watched it.
- **SC-009**: Concurrent runs over the same folders, from the same or different authors, complete
  without either being refused and without either producing a folder the other disturbed.
- **SC-010**: A submission over the configured maximum of folders is refused before any run exists
  in 100% of cases, at the same default as bulk folder delete's, and the maximum the client reads
  from the advertised configuration matches the one the server enforces.

---

## Legacy Considerations *(dotCMS-specific, mandatory)*

- **Existing behavior touched**: folder copy in `com.dotmarketing.portlets.folders.business`, which
  is long-standing legacy. `FolderAPIImpl.copy` has two overloads, one taking a parent folder and
  one taking a site, both wrapping the whole recursive walk in a single transaction. This feature
  calls them and does not rewrite them.
- **What the walk actually carries, and what it silently does not.** Four things: file assets,
  working pages, links and child folders. There is no branch for anything else, so content of any
  other type living in the folder is not copied. Folder *deletion* takes the opposite approach and
  resolves every contentlet in the folder regardless of type, so the two operations disagree about
  what a folder contains. **No rationale for the omission is recorded anywhere**: the method carries
  only descriptive labels, the interface has no Javadoc on it, `docs/` says nothing, and the history
  back to 2017 is incidental fixes with no commit that touches the question. It reads as an omission
  rather than a policy.
- **The sources are version-filtered**, which nothing in either half said before: file assets are
  read only where working and not archived, pages through the working-pages lookup, links only where
  working. On its own the walk therefore skips archived items. This feature carries them (FR-012d),
  and FR-012e pins the state each duplicate comes out in.
- **Site copy already solved the generic-content problem, and did not use this walk.** The
  enterprise host-assets job builds its own folder structure and copies content separately through
  `copyContentlet` with content-type mapping. That is both the evidence the omission is real and the
  precedent FR-012d follows: carry the missing content in the caller, leave the shared walk alone.
- **There is no REST folder copy today.** `WebAssetResource` under `/v1/assets` exposes asset
  download, delete and archive, a single-folder delete, and folder create and update; it has no
  copy. `FolderResource` under `/v1/folder` has none either. The only shipped folder copy is
  `BrowserAjax.copyFolder` (`BrowserAjax.java:925`), reached through legacy remoting from the Site
  Browser. **This is the largest difference in size from bulk delete**, which extended a shipped
  single-folder endpoint; copy is building its first REST surface, and the plan must not assume a
  synchronous path exists to be wrapped.
- **The two permission checks govern the entry, not the walk.** `FolderAPIImpl.copy` checks read
  rights on the source and add-children rights on the target, then `FolderFactoryImpl` performs the
  entire recursion as the system user, resolving and copying file assets, pages, child folders and
  the contentlets themselves with system rights. Folder permissions are copied to the duplicate. See
  FR-012b and D-009; this is documented, not corrected.
- **Only one of the two overloads validates the folder name.** The site-parented overload validates
  it; the folder-parented one does not. This asymmetry was harmless while only the Site Browser
  called them, and stops being harmless here, because duplicate-in-place puts **both** overloads on
  the hot path: a folder at a site root goes through one and a nested folder through the other. The
  plan must decide whether to even this up as progressive enhancement or to state why not.
- **Copy maintains the search index and delete does not.** The copy walk refreshes content under
  each new folder as it goes, so duplicated content is indexed. Bulk delete's specification records
  that its own path leaves orphaned search documents permanently (its FR-009d); **copy has no
  analogue of that finding**, and a reader moving between the two documents should not carry it
  across.
- **The self-target and descendant-target guards are not in the API.** They live in
  `BrowserAjax.copyFolder` (`BrowserAjax.java:960-967`), not in `FolderAPI.copy`, so any caller
  going directly to the API can copy a folder into its own descendant. Under duplicate-in-place
  neither case is expressible, because the target is always the source's existing parent, so **this
  feature does not need the guards and does not add them**. The gap remains for folder move
  (#37165), which does choose a destination, and should be closed there rather than inherited
  silently. Worth knowing why it matters: the walk re-reads each folder's children level by level,
  so copying into a descendant can pick up folders the copy is itself creating.
- **Backward-compatibility expectations**: the Site Browser copy is untouched (FR-006). The per-item
  outcome contract is shared with shipped features and is consumed here **unchanged**: this feature
  adds no field to it and renames none, which is a large part of why its client half is cheap and
  why it cannot drift from bulk delete or move.
- **Known related decisions**: the job-queue framework and the shared batch outcome contract landed
  with #37131 and #37166 and are consumed here unchanged. That framework holds **no durable per-item
  state**, which for copy is sharper than for delete because of FR-034. Bulk folder delete (#37063)
  and folder move (#37165) share the outcome contract. `/speckit-plan` will consult
  `dotCMS/platform-adrs` formally; the batch permission filtering decision applies to any per-folder
  permission gate this feature adds.

---

## Out of Scope

- **The frontend half of #37062.** Specified separately in
  `specs/37062-folder-copy-frontend/spec.md`; this document's contract is its input.
- **Copying a folder to a chosen destination.** Removed by D-005. Folder move (#37165) is where a
  destination picker belongs, and #33468 carries the intended experience for it.
- **Folder move (#37165) and bulk folder delete (#37063).** They share this feature's outcome
  contract and must not diverge from it, but neither is built here.
- **The job-progress client primitive.** Owned by #37166.
- **Copying contentlets.** A Content Drive selection can contain files as well as folders; this
  feature copies the folders. How a mixed selection is presented is the client's to decide.
- **Bounding the recursive copy**, which #37062 calls "the real work". Deferred by D-012 to a
  sibling of #37565, filed alongside this specification rather than promised by it. What that work
  carries: paging a folder's children instead of loading them whole, and splitting the transaction,
  which would reopen FR-026 and is the larger and riskier half.
- **Batched commits in the style of site copy** (D-019). Considered and rejected for this version:
  it scales, but it gives up the all-or-nothing guarantee and leaves partial duplicates behind, and
  it belongs with the bounding work above rather than here.
- **Correcting what the duplication walk may read** (D-009, FR-012b). Content the author cannot see
  is duplicated. Changing that means changing a path the Site Browser and any plugin calling
  `FolderAPI.copy` share. Note this is about *rights*, not about *coverage*: the generic content the
  walk does not carry is in scope and is closed by FR-012d, in the processor rather than in the
  walk.
- **Evening up the naming rule so derived names do not grow** (FR-010). Changing it changes what
  Site Browser authors have seen for years.
- **Adding the self-target and descendant-target guards to `FolderAPI.copy`.** Not needed here; owed
  by #37165.

---

## Decisions

- **D-001: Asynchronous, not synchronous, and this is settled.** #37062 records that three
  synchronous endpoints were proposed and did not survive review, on the reasoning that designing a
  synchronous endpoint around an expected proxy timeout is an asynchronous design without the
  machinery. Not relitigated here.
- **D-002: The outcome field names follow what shipped, not what the issue text says.** #37062's
  description writes a success count, a failure count and a per-item boolean. What is in the
  product, from bulk upload and bulk refresh, is a total, a processed count, a success count, a
  failure count, a skipped count, and per-item records with a three-valued status. A boolean cannot
  express the cancellation outcome. This feature uses the shipped names, as bulk delete does.
- **D-003: A domain endpoint, not the generic job submission address.** Callers post folder paths to
  a folder-shaped address that enqueues on their behalf, following the content-import precedent,
  with the generic job addresses used for status, cancellation and monitoring.
- **D-004: An ordinary JSON body, not multipart.** Nothing is uploaded.
- **D-005: Duplicate in place. There is no destination.** *(Developer decision, 2026-09-21.)*
  #37062's description specifies a destination path and a picker. The copy instead lands in the
  source folder's own parent. This removes the destination from the submission, removes the picker
  from the client, and removes three of the issue's acceptance criteria outright: copying a folder
  onto itself, copying a folder into one of its own descendants, and the add-children check on a
  chosen destination. None of the three is expressible when the target is always the source's
  existing parent. The submission body is consequently the same shape as bulk delete's, a list of
  folder paths, which is what #37062 anticipated when it said the body would be shared with move.
  Relocation, and the destination picker #33468 describes, belong to #37165.
- **D-006: Two specifications for #37062, one contract.** Recorded because the issue does not say.
  The halves are delivered against §Contract Consumed by the Client rather than as one body of work,
  following #37063 and #37166.
- **D-007: A refusal on one folder is that folder's failure, not the submission's.** A submission is
  refused only for entitlement to the operation itself; rights over an individual folder are the
  run's business (FR-005, FR-012).
- **D-008: The naming rule stays exactly as it shipped, and #37062's collision criterion is
  corrected.** *(Developer decision, 2026-09-21.)* The issue requires that a name collision at the
  destination be a per-folder failure and never a silent success. Under duplicate-in-place that
  criterion would fail **every** copy, since the source's name is always taken in its own parent.
  The shipped rule appends a suffix until the name is free, and that is what this feature uses. The
  cost is accepted and recorded rather than hidden: names grow with each duplication, so a folder
  duplicated three times yields progressively longer derived names. The rule is left alone because
  it is shared with the Site Browser and changing it would change what those authors have seen for
  years. **The outcome does not restate the chosen name either** (FR-009a): an earlier draft
  required it, on the reasoning that the author could not otherwise find what was made. The
  developer's position, taken on 2026-09-22, is that the suffix is well enough known for that to be
  unnecessary, and publishing the rule with the endpoint is the cheaper answer than adding a field
  to a contract three features share. The trade is stated so it can be revisited: the rule appends
  to the name it finds, so the name follows from the rule plus what is already in the parent, not
  from the source name alone.
- **D-009: Copy duplicates content the acting user cannot read, and this feature does not change
  that.** *(Developer decision, 2026-09-21.)* The walk beneath the two entry checks runs as the
  system user (FR-012b). The developer accepted it deliberately: the duplicate receives the source's
  permissions, so the copy is no more visible than the original was. Recorded with its consequence
  stated rather than inherited silently, in the shape of bulk delete's equivalent decision.
  Correcting it would mean changing a path the Site Browser and customer plugins share, which is a
  different piece of work.
- **D-010: One endpoint, asynchronous, and no synchronous single-folder copy** *(FR-007a, developer
  decision, 2026-09-21)*. The context menu submits a batch of one. Bulk delete keeps a synchronous
  single-folder endpoint only because it shipped first on #35161 and its own specification forbids
  special-casing a single path back into it; copy has no such endpoint to preserve, and copying one
  large folder carries the same timeout exposure as copying several.
- **D-011: Cancellation is honoured between folders, and the strong guarantee holds.** Unlike the
  behaviour #37062 describes, no partial duplicate survives (FR-026). This follows from D-012: the
  shipped copy is one transaction per folder, so leaving the recursion alone keeps the guarantee.
- **D-012: The recursive copy is left exactly as it is; only the multi-select is new.** #37062 calls
  bounding that walk "the real work" and says to estimate on it. This specification deliberately
  does not do it, following bulk delete's equivalent decision. The reported gap is that folder copy
  does not exist in Content Drive, and an asynchronous run delivers that plus the timeout fix.
  Bounding means changing code every caller of folder copy shares, so it is either a behaviour
  change for all of them, which is rollback-unsafe, or a second copy path that behaves differently
  depending on how it was invoked, permanently. Filed as a sibling of #37565, not promised here.
- **D-013: The all-or-nothing guarantee needed no new machinery, and #37062's cancellation text is
  corrected.** The issue states that a cancelled copy leaves a partial subtree the user can delete.
  That follows from chunking, which D-012 defers, so the opposite is true under this specification's
  design and the weaker wording must not reach the client (FR-026, C-008).
- **D-014: Behaviour on locked or in-use content is today's behaviour.** Copy reads its sources and
  does not need to destroy or modify them, so it has no analogue of the lock failure delete reports
  per folder. Recorded so its absence from the failure-reason set is deliberate.
- **D-015: Duplication is not idempotent, and the recommendation is not to retry it.** *(FR-034;
  corrected twice, the second time after @dario-daza traced the retry path on 2026-09-23.)* A
  successful duplication leaves nothing that makes a second attempt a no-op. Bulk delete faces the
  same absence of durable per-item state and is unharmed, because a delete that already happened
  simply fails to resolve the second time.

  The first draft offered the plan a choice between disabling re-queue and accepting duplicated
  duplicates. The second draft withdrew that choice on the claim that the abandoned-job sweep
  ignores the retry policy. That claim was wrong: the sweep puts the job back in the queue, and when
  it is picked up the queue manager gates an abandoned job through the same retry check as a failed
  one, so a no-retry processor is not run again. The choice is real, and for duplication it resolves
  toward no-retry, the opposite of delete, because delete gains from a second pass and duplication
  only repeats itself. The price is losing a stalled run's per-folder outcome, which makes staying
  alive (FR-028) the thing that actually protects the author.
- **D-016: Nothing is marked or blocked, and no busy signal is broadcast.** *(Developer decision,
  2026-09-21; FR-040, C-011.)* Bulk delete makes a folder inert everywhere it appears while a run
  works on it, keeps that true across reloads and authors, and broadcasts a folder's entry into and
  exit from a delete. All of it exists because a folder being deleted is about to stop existing.
  Nothing about a folder being duplicated makes it unsafe to open, upload into or drag onto, so this
  feature carries none of that machinery: no marking, no reading the in-flight run listing, no busy
  announcements. The existing per-folder `COPY_FOLDER` completion event is unaffected and still
  reaches other authors (FR-040). The author gets a completion signal and a report. This is the
  single largest reduction in scope relative to the sibling feature and is why the client half is
  substantially smaller than bulk delete's.
- **D-017: A selected folder inside another selected folder is skipped, as it is for delete.**
  *(Developer decision, 2026-09-22; FR-016.)* An earlier draft of this document duplicated both, on
  the reasoning that the two duplicates land in different places so neither is redundant. That was
  wrong, and not for the reason first raised. The redundancy argument is real but soft: the author
  gets an extra folder they did not want. The argument that settles it is that acting on both is
  **order-dependent**, because duplicating the descendant first leaves its duplicate sitting inside
  the ancestor, which the ancestor's duplicate then picks up. The run would produce different
  results depending on the order it happened to process the selection, and no ordering is defensible
  over the other. Skipping the descendant removes the question and matches what the author meant.
  The skip reason needs its own wording (C-005): delete's says the ancestor removed the folder, and
  here the folder is still there.
- **D-018: A duplicate holds everything in the folder, and the gaps are closed in this feature's
  processor, not in the shared walk.** *(Developer decision, 2026-09-23; FR-012d.)* Raised in review
  against the code rather than against the issue: the shipped walk carries file assets, pages, links
  and child folders and nothing else, while folder deletion resolves every contentlet regardless of
  type. Copy therefore drops generic content and reports the folder as a success, which is
  survivable in the Site Browser and not in Content Drive, where the listing an author is looking at
  is mixed content by design. The decision is to close it rather than record it as a limitation, and
  a later decision the same day widened it to **all** content in the folder, so archived items the
  walk's working filter skips are carried too, each duplicate keeping its source's state. **Where**
  it is closed was then settled by a precedent already in the codebase: the enterprise site-copy job
  carries generic content itself and does not use the folder factory's walk. The same processor
  carries the archived items the walk skips. Following that keeps the fix inside this feature,
  leaves the Site Browser and every plugin calling `FolderAPI.copy` untouched, and avoids the
  rollback-unsafe behaviour change that D-012 exists to keep out of this ticket. Extending the
  factory remains the option if the duplication turns out to be worse than the coupling, but it is a
  different piece of work with a different blast radius.
- **D-019: No batching; duplication matches bulk delete's shape.** *(Developer decision, 2026-09-23;
  FR-007, FR-024.)* Raised as a question about very large folder trees, answered by comparing the
  two precedents in the codebase. The enterprise site-copy job handles whole sites by committing
  every 500 items, reading its sources in pages and pausing between batches. That is why it scales,
  and it is also why a failed site copy leaves a partly populated destination: on an error it rolls
  back only the batch in progress, and nothing cleans up what earlier batches committed. Adopting it
  here would trade FR-026's all-or-nothing guarantee for a scale nobody has measured, and a partial
  duplicate left by a crashed node would have no cleanup and, under no-retry, no outcome record.
  Bulk folder delete took the other path: a cap of 50 folders per submission, one transaction per
  folder, and the bounding of a single huge folder deferred to its own ticket. Duplication does the
  same, so the two folder operations share one shape and one deferred problem rather than solving it
  twice in different ways. The cost is stated in FR-024: one very large folder is still one long
  transaction, heavier here than in the shipped copy.

---

## Planning Obligations

- **Test coverage** (Constitution V). The plan MUST name which layers this feature exercises and
  which it does not, with a reason for each omission. At minimum it MUST cover, as integration
  tests: a happy path over several folders, a mixed partial failure, a source the author cannot
  read, a parent the author cannot add to, an unresolvable path, a protected path, a folder at a
  site root and a nested folder, duplicate paths in one submission, an ancestor and its descendant
  both selected (the descendant skipped, the ancestor duplicated once, and the result the same
  whichever order the two were submitted in), repeated duplication of the same folder producing
  distinct names, and cancellation mid-run leaving no partial duplicate. #37062's list additionally
  names a subtree large enough to exercise more than one chunk; under D-012 that case no longer
  applies and **must not be written**.
- **Confirming no-retry and keeping the run alive** (FR-028, FR-034, D-015). The plan MUST confirm
  the recommendation to mark the processor `@NoRetryPolicy` or give a reason not to, and MUST say
  how a run stays visibly alive while a single folder's duplication blocks, since under no-retry a
  false abandonment ends a healthy run and loses its outcome. Bulk delete's processor solved the
  same problem first and is the precedent to read before designing a new one. The
  `BulkUploadProcessor` javadoc that claims the annotation is ineffective is wrong and MUST NOT be
  copied into this processor.
- **How a folder is matched between a run and a row.** The client has both a path and an identifier
  for every folder it lists, and which one travels in the submission, in the run's parameters and in
  the outcome need not be the same choice. Both plans MUST agree, and this half MUST state what the
  outcome is keyed by.
- **Where the naming rule is published** (FR-009a). The outcome does not carry the chosen name, so
  the endpoint's description is the only place a caller can learn what a duplicate will be called.
  The plan MUST say where that text lives and MUST cover the repeated-duplication case, since the
  rule appends rather than counts.
- **Whether the naming asymmetry between the two API overloads is evened up.** Only the
  site-parented overload validates the folder name, and duplicate-in-place puts both on the hot
  path. Progressive enhancement suggests correcting it; the plan MUST decide rather than inherit.
- **The configured maximum** (FR-007) and the durable record's lifetime (FR-036a) both need concrete
  values and a documented default.
- **Not copying anything twice** (FR-012d). File assets and pages **are** contentlets, and the walk
  already carries the working, non-archived ones through dedicated branches that de-duplicate by
  identifier. The processor carries everything else: generic content of any state, and archived file
  assets and pages the walk skipped. A processor that resolves every contentlet in the folder and
  copies it will copy those a second time unless it excludes what the walk already handled. The plan
  MUST say how, and a test MUST prove a folder of mixed content produces one of each rather than two
  of some.
- **Which user the added content copy runs as** (FR-012b, FR-012d). The walk runs as the system
  user. The plan MUST state whether the content this feature adds follows that or uses the
  submitting user, and MUST NOT leave the two halves of one duplication running under different
  identities by accident.
- **Pinning the failure and skip reason names before either half writes copy** (FR-022, C-005).
  Duplication needs additive values: two distinguishable permission failures where the enum has one,
  an unresolvable folder, a protected folder, and the ancestor skip. Bulk delete had to reconcile
  reason *names* late because the client had already written copy against its own vocabulary, and
  that round-trip is avoidable here. The names belong in
  `specs/37062-folder-copy-backend/contracts/`, which is the one place in this repo's Spec-Kit
  layout that survives alongside the spec.
- **Registering the Postman collection so it actually runs** (Constitution V). A collection absent
  from `dotcms-postman/config.json` never runs in CI: green build, zero coverage, the same failure
  mode as an integration test missing from a suite list. It MUST go in the **`default-split`**
  group, where every other job-queue collection already sits. That group shares one dotCMS container
  and one queue across its collections, so this one MUST clean up after every run and MUST NOT
  assert on queue-wide listings, or it will break the collections beside it.
- **Relationships between items duplicated in the same run** (FR-012d). Site copy has dedicated,
  separately batched handling for relationships, because copied items that point at each other, such
  as a Blog entry and its Author, must end up pointing at each other's copies rather than at the
  originals. The folder walk never needed that, since it copied only files and pages. Now that
  duplication carries generic content, the plan MUST determine what the content copy does with a
  relationship whose other end is copied in the same run, and MUST state the outcome rather than
  leave it to whichever order the copies happen in.
- **How a run stays alive while one folder blocks** (FR-028). Bulk folder delete's processor, in the
  change that implements it, carries a configurable abandonment threshold with a thirty-minute
  default, which appears to be its answer to the same problem. The plan MUST read it before
  designing duplication's equivalent.
- **Concrete field names and the exact submission shape** are deliberately left at behaviour
  altitude here and belong in the plan, recorded under `specs/*/contracts/` so they survive.

---

## Open Decisions

**None.** Every question this specification raised is closed, either here in §Decisions or inline at
the requirement it governs.

What remains genuinely undecided is not a specification question and is listed under §Planning
Obligations: the retry position, what the outcome is keyed by, and whether the overload asymmetry is
corrected. All three are sized at the plan phase and none changes what this document requires.

---

## Assumptions

- The job-queue framework's capabilities are as bulk upload and bulk refresh use them today: enqueue
  with parameters, progress reporting, cancellation, a terminal result harvested once, and no
  durable per-item state.
- The shared per-item outcome contract is used exactly as it stands. This feature adds no field to
  it, so it cannot disturb bulk upload, bulk refresh, bulk delete or move.
- `FolderAPI.copy` remains the single place folder copying happens, and this feature calls it rather
  than reimplementing the walk.
- Folder rights arrive with the folders the client already lists, so the client's courtesy gate
  needs no separate rights lookup (FR-012c).
- The number of folders in one submission is bounded by the configured maximum, so neither the run
  nor its report is designed for an unbounded selection.
- Authors reaching this are back-office users; no front-end or anonymous path submits these runs.
