# Feature Specification: Content Drive folder duplication (frontend)

**Feature Branch**: `37062-content-drive-folder-copy`

**Created**: 2026-09-21

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37062](https://github.com/dotCMS/core/issues/37062): "Content Drive: folder copy, async job endpoint and frontend wiring", frontend half.

---

## Context

An author has a campaign folder they want to reuse as the starting point for next quarter's work.
Today Content Drive cannot help them: folders are selectable in the listing and appear in the
right-click menu, but there is **no way to duplicate one anywhere**, for a single folder or for
many. The only folder copy in the product lives in the old Site Browser. This specification covers
the browser-side half of fixing that.

**This specification covers the client only.** The server half is specified in
`specs/37062-folder-copy-backend/spec.md`. The two meet at that document's *Contract Consumed by the
Client* (C-001 … C-011), restated here from the consumer's side in §Contract Consumed. Nothing about
server behaviour is re-specified here.

**The operation duplicates a folder in place.** The duplicate lands beside the original, in the same
parent, under a name the server derives so it does not collide. **The author does not choose where
it goes**, so this feature has no destination picker and no configuration step. That is a deliberate
narrowing of #37062's description, taken by the developer and recorded in the backend half as its
D-005, and it is the reason this specification is a fraction of the size of its sibling for bulk
delete.

**Nothing is blocked while a duplication runs.** A folder being duplicated is still a folder: it can
be opened, uploaded into, dragged onto and duplicated again. The elaborate marking machinery bulk
folder delete needs, making a doomed folder inert everywhere it appears and keeping that true across
reloads and authors, has no purpose here and is deliberately absent (backend D-016). What this
client does while a run is in flight is show progress; what it does when the run ends is report what
happened.

**The duplicate's name is chosen by the server, and the report does not restate it.** Duplicating
`campaign` gives `campaign_copy`. An earlier draft of this specification required the report to name
each folder it created, on the reasoning that the author could not otherwise find what was made. The
developer's position, taken on 2026-09-22, is that the suffix is well enough known for that to be
unnecessary, and the server half now publishes the naming rule with its endpoint instead of adding a
field to a contract three features share (backend FR-009a, D-008). The consequence for this half is
that the report speaks in terms of **the folders the author selected**, saying which of them were
duplicated, rather than in terms of the folders that came out.

Throughout this document, **selecting rows** means picking folders in the Content Drive listing. A
**run** is one submitted duplication, identified by a handle the server returns. **Duplicate** is
the folder the run creates.

The operation is a **duplication** throughout, never a "copy", because "copy" invites the question
of where to and there is no answer. The word "copy" survives in two senses only, and neither is the
operation: the existing Site Browser feature, and **client copy**, meaning the wording shown to an
author. A folder is never identified by a path to somewhere else, only by its own.

---

## Clarifications

### Session 2026-09-21

- Q: The action duplicates in place rather than copying to a chosen location. Is it labelled "Copy"? → A: No. It is labelled as a duplication. "Copy" sets up an expectation of choosing a destination, or of a paste step that never comes, and the label is the only thing standing between the author and that expectation. See FR-002 and D-002.
- Q: Does duplicating require a confirmation, as deleting does? → A: No. Deleting confirms because it is permanent and recursive; duplicating creates something the author can simply delete. The bulk action still passes through the existing preview step every bulk action uses, because that is where the author sees what will be acted on and presses the button, but it carries no warning language. The single-folder action runs directly.
- Q: The author selects six folders and may duplicate only four. Does the action submit four or six? → A: Six. *(**Superseded 2026-09-22**, see the next session. The answer is now four, and the count says four. The reasoning below was borrowed from bulk delete without noticing that it rests on delete being destructive, which duplication is not.)* The client does not filter a selection by its own reading of rights; the two the server refuses come back as per-folder permission failures. The action is withheld only when the author can duplicate none of them. This follows the position bulk delete reached, for the same reason: silently shrinking what the author asked for is worse than reporting the refusal.
- Q: Does a running duplication block other Content Drive actions? → A: No. *(Unchanged.)* It follows the existing guard unchanged, which refuses a repeat of the same operation on the same items and nothing wider. Unrelated actions and duplications of other folders run alongside it. Unlike delete, overlapping duplications are not refused by the server either, because two duplications of the same folder cannot harm each other (backend FR-033).

### Session 2026-09-22

- Q: The action center already works out which items an action applies to and acts only on those. Should duplication follow that, or submit the whole selection? → A: Follow it. The action acts on the folders the author may duplicate and states how many of the selection that is. Bulk delete's opposite position rests on delete being destructive, and that reasoning does not carry to an operation that destroys nothing. The count is what keeps the narrowing honest. Supersedes the third answer in the previous session. See FR-005a and D-006.
- Q: If the author cannot add children to the folder they are browsing, can they duplicate anything in it? → A: No, and the action is not offered at all. Every duplicate in the selection lands in that folder, so every one would be refused. This became checkable only once the destination was dropped: the target parent is now the folder Content Drive already has open, along with its rights. See FR-005 and D-006a.
- Q: With a search or filter applied the selection can span several parents. Does the add-children gate still apply? → A: No. There is no single folder being browsed, so there is nothing to check against, and the server's per-folder refusals report it instead. See FR-005b.
- Q: Nothing appears in the listing while a duplication runs, because the new folders do not exist yet. How is the author kept informed? → A: The same way bulk upload does it, which has the same problem and solved it already: no rows to mark, so a background status and a completion report carry the whole story. What that looks like is a plan question, not a specification one. See FR-012 and the reporting requirements.

---

## User Scenarios & Testing *(mandatory)*

<!--
Written as author-visible behaviour in the Content Drive interface. Each is verifiable in the
browser against a server honouring §Contract Consumed, with no knowledge of how the server
implements it.
-->

### User Story 1 - Several folders are duplicated in one action (Priority: P1)

An author selects several folders in the listing and chooses to duplicate them. The action is
submitted, the dialog closes at once, and the author carries on working while the duplicates are
made.

**Why this priority**: This is the feature. Delivered alone it is a complete, usable capability.

**Independent Test**: Select two or more folders, duplicate them, and confirm duplicates of each
appear and the author was never blocked while it happened.

**Acceptance Scenarios**:

1. **Given** several folders are selected, **When** the author opens the action list, **Then** the
   duplicate action is offered and states how many of the selection it will act on.
2. **Given** the action is chosen, **When** the author reaches the point of committing, **Then**
   they are shown what will be acted on and no destination is asked for.
3. **Given** the author commits, **When** the submission is accepted, **Then** the dialog closes
   immediately and the rest of the portlet stays usable.
4. **Given** a run is in flight, **When** the author looks at the folders being duplicated, **Then**
   they behave as ordinary folders: openable, selectable, and valid drop targets.
5. **Given** the author dismisses before committing, **When** the dialog closes, **Then** nothing is
   submitted and the selection is untouched.

---

### User Story 2 - The author is told what actually happened (Priority: P1)

When a run ends, the author is told how many of their selected folders were duplicated and which
were not, each with a reason they can act on.

**Why this priority**: P1. Partial failure is the normal case over a multi-select, and a run that
reports only a count leaves the author unable to tell which folders they still have to deal with.
The report is also the only place a refusal ever surfaces, because the action deliberately submits
folders the client cannot prove are permitted.

**Independent Test**: Duplicate three folders of which one is refused; confirm the report states two
succeeded, names the refused folder, and gives a reason in the product's own words.

**Acceptance Scenarios**:

1. **Given** a run ends, **When** the report appears, **Then** it uses the server's counts of
   duplicated and failed folders, never the size of the author's selection.
2. **Given** a folder could not be duplicated, **When** the report is read, **Then** that folder is
   named along with the reason, in the product's own words.
3. **Given** more failures than the report can show at once, **When** it is read, **Then** it names
   the first few, counts the rest, and leads to the full list rather than ending in a bare count.
4. **Given** a run that duplicated nothing because every folder was refused, **When** the report
   appears, **Then** it does not read as a success.

---

### User Story 3 - One folder is duplicated from the right-click menu (Priority: P1)

An author right-clicks a single folder and duplicates it, without going through a multi-selection.

**Why this priority**: P1 because duplicating one folder is the common case and the right-click menu
is where an author looks for an action on one row. It is also where the absence is most obvious
today, since every other single-folder action already lives there.

**Independent Test**: Right-click a folder, duplicate it, and confirm a duplicate appears and the
outcome is reported the same way the bulk action reports it.

**Acceptance Scenarios**:

1. **Given** a folder the author may duplicate, **When** they open its right-click menu, **Then**
   the duplicate action is offered.
2. **Given** the action is chosen, **When** it is submitted, **Then** it goes through the same
   submission as the bulk action, carrying one folder.
3. **Given** the single-folder action was submitted, **When** the run ends, **Then** the outcome is
   reported through the same path as the bulk action, naming the duplicate that was created.
4. **Given** a folder the author has no right to duplicate, **When** they open its right-click menu,
   **Then** the action is not offered.

---

### User Story 4 - Leaving does not lose the outcome (Priority: P2)

An author duplicates a large folder, navigates away, and comes back. What was made is still
discoverable.

**Why this priority**: P2 because the operation itself succeeds regardless. It matters more here
than for a delete: an author who cannot find out whether a duplication happened, and resubmits, gets
a second duplicate rather than a harmless no-op (backend FR-034).

**Independent Test**: Submit a run, navigate away from the portlet, return after it completes, and
confirm the outcome including the duplicates' names is still reachable.

**Acceptance Scenarios**:

1. **Given** a run is in flight, **When** the author closes the dialog that started it or navigates
   within the portlet, **Then** the run continues and is unaffected.
2. **Given** a run ended while the author was elsewhere, **When** they return, **Then** they can
   determine the full outcome, including which of their folders were duplicated, without having
   watched it.
3. **Given** a run is in flight, **When** the author leaves the portlet entirely, **Then** nothing
   cancels it.

---

### User Story 5 - Both surfaces show the new folders once a run ends (Priority: P2)

The duplicates appear in the listing and in the sidebar tree when the run finishes, without the
author reloading.

**Why this priority**: P2 because the author can reload. Without it the feature looks broken: the
report says six folders were created and the screen shows none of them.

**Independent Test**: Duplicate a folder visible in both the listing and the sidebar tree; confirm
both show the new folder once the run ends, with no reload.

**Acceptance Scenarios**:

1. **Given** a run ends, **When** the listing is showing the parent the duplicates landed in,
   **Then** the new folders appear in it.
2. **Given** a run ends, **When** the sidebar tree is showing that parent, **Then** the new folders
   appear there too. The two surfaces load independently and refreshing one does not refresh the
   other.
3. **Given** a run ends while the author is looking at a folder it did not touch, **Then** the
   report is still shown and the listing they are on need not change.
4. **Given** a run in which some folders failed, **When** it ends, **Then** no duplicate appears for
   those folders and the sources remain exactly as they were.

---

### User Story 6 - The author is not offered a duplicate that can only fail (Priority: P3)

The action is withheld where the author is known to have no right to it, and offered everywhere
else.

**Why this priority**: P3 because the per-folder outcome reports refusals honestly, so a submission
that fails is informative rather than broken. It is still worth doing: offering an action that can
only fail teaches authors to distrust the interface.

**Independent Test**: As an author who cannot add children to the folder being browsed, confirm the
action is not offered at all; as one who may duplicate four of six selected folders, confirm it is
offered, says it will act on four, and acts on four.

**Acceptance Scenarios**:

1. **Given** an author who cannot add children to the folder being browsed, **When** the action list
   opens, **Then** the action is not available, whatever is selected.
2. **Given** a selection where the author may duplicate some of the folders, **When** the action is
   used, **Then** it acts on those and states how many of the selection that is, so the narrowing is
   visible before it happens rather than reported afterwards.
3. **Given** a folder whose rights the client cannot determine, **When** the action is used,
   **Then** that folder is submitted rather than withheld, and the server's refusal is what reports
   it.
4. **Given** a search or filter is applied so the selection spans several parents, **When** the
   action list opens, **Then** the action is offered and the add-children gate is not applied,
   because there is no single folder to apply it against.

---

### Edge Cases

- **The selection mixes folders and files.** The action acts on the folders and says so; the files
  are not part of this run.
- **The selection mixes folders the author may duplicate with folders they may not.** The action
  acts on the ones they may and says how many that is, in the count it already shows. Folders whose
  rights the client cannot determine are submitted rather than withheld, and the server refuses
  those per folder.
- **The author cannot add children to the folder they are browsing.** The action is not offered at
  all, because every duplicate in the selection would land there and every one would be refused.
- **A search or filter is applied, so the selection spans several parents.** There is no single
  folder being browsed, so the add-children gate does not apply and the server's per-folder refusals
  are what report it.
- **A parent and its own child are both selected.** Only the parent is duplicated; the child comes
  back as skipped, because the parent's duplicate already contains it. The report must present this
  as covered rather than as a failure, and the wording must not say the parent removed the child,
  which is bulk delete's reason and is untrue here: the child is still there, untouched.
- **The author duplicates the same folder twice in a row.** Both succeed, with different derived
  names. The report names each one, which is the only way the author can tell them apart.
- **The author is browsing the parent the duplicates land in while the run works.** New rows appear
  as the run progresses rather than all at once. This is expected and is not an error state; the
  completion refresh is what guarantees the final view is right.
- **A run ends while the author is on a different site.** The report is still shown; the listing
  they are on need not change.
- **The server reports a folder as duplicated whose source the listing no longer shows** (deleted by
  someone else in between). The outcome is still reported for it.
- **A run is cancelled from elsewhere.** The report presents it as cancelled, not as a fault, and
  states that the folders not reached were not duplicated.
- **The report names a duplicate whose parent the author cannot currently see.** The name is still
  reported; the client does not suppress an outcome because the folder is off-screen.
- **A failure reason the client does not recognise.** The folder is still named and reported as
  failed, using the general fallback. An unknown reason must not swallow the folder.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Offering the action

- **FR-001**: Authors MUST be able to select one or more folders in the Content Drive listing and
  duplicate them in one action, offered from the same place the other multi-selection actions are.
- **FR-002**: The action MUST be labelled as a **duplication**, not as a copy. "Copy" implies either
  choosing a destination or a paste step to follow, and this operation has neither. The label is the
  only thing standing between the author and that expectation, and no amount of explanatory copy
  further down recovers it.
- **FR-003**: Authors MUST be able to duplicate a single folder from its right-click menu, and that
  action MUST use the same submission as the bulk action, carrying one folder.
- **FR-004**: The action MUST act on the folders in the selection only. A selection containing files
  as well MUST still offer it, acting on the folders, and MUST state how many items it will act on
  rather than implying it covers the whole selection.
- **FR-005**: The action MUST be unavailable outright when the author cannot add children to **the
  folder they are browsing**, because that is where every duplicate in the selection would land.
  This is one check for the whole selection rather than one per folder, and it is a check the client
  can actually make: duplicating in place means the target parent is the folder currently open in
  the listing, which Content Drive already holds along with its rights. Offering an action that can
  only fail for every folder in the selection is the case worth preventing outright.
- **FR-005a**: The action MUST act only on the folders in the selection the author may duplicate,
  and MUST state how many of the selection that is (FR-004). It follows the eligibility behaviour
  the other multi-selection actions already have rather than inventing a second one.

**This reverses an earlier draft**, which required the selection to be submitted whole so the
server's refusals could be reported per folder. That position is bulk delete's, and it was taken
there because silently shrinking a **destructive** action is worse than reporting a refusal: an
author who believes six folders were deleted and finds four is in a different situation from one who
believes six were duplicated. Duplication destroys nothing, so the argument does not carry over, and
consistency with the other actions in the same menu is worth more. The filtering is **never
silent**: the count in FR-004 is what keeps it honest, and an action reading "4 of 6" tells the
author as much as a report of two refusals would, sooner.
- **FR-005b**: Where the selection spans more than one parent, which a search or a filter makes
  possible, the client MUST NOT gate on FR-005 and MUST leave the refusals to the server. There is
  no single folder being browsed in that case, so there is nothing to check add-children against,
  and a gate built on the wrong parent is worse than no gate.
- **FR-005c**: Where the author's rights over an individual folder are unknown, the action MUST
  remain available for it and the server's per-folder refusal MUST be what reports it. The client
  MUST NOT guess a refusal. Filtering under FR-005a acts on rights the client **has**, never on
  their absence, and the per-folder outcome remains the only authority (backend FR-012c).
- **FR-006**: Where a submission is refused for carrying more folders than the configured maximum,
  the client MUST explain the refusal in terms of that limit, using the number the server reports
  with the refusal. The client MUST NOT hold its own copy of the maximum, and MUST NOT gate the
  action on it before submitting: the limit is not exposed ahead of time and the client learns it by
  being refused. An earlier draft required reading the limit up front, which the server half has no
  contract item to satisfy; exposing it would be a new capability rather than a wording change, and
  is not worth one message.

#### Committing

- **FR-007**: The bulk action MUST pass through the same commit step every other bulk action uses,
  where the author sees what will be acted on before pressing the button. It MUST NOT add a
  configuration step, because there is nothing to configure: no destination, no name, no options.
- **FR-008**: The commit step MUST NOT use the language of a destructive confirmation. Nothing is
  removed or overwritten, and copy written for deletion MUST NOT be reused here.
- **FR-009**: The commit step MUST state that each duplicate is created beside its original under an
  automatically chosen name, so an author is not surprised by a name they did not pick.
- **FR-010**: The single-folder action MUST run directly from the right-click menu without a
  confirmation step. Duplicating creates something the author can delete, and a confirmation on a
  non-destructive single action is friction without a purpose.
- **FR-011**: Dismissing before committing MUST submit nothing and leave the selection untouched.

#### While a run is in flight

- **FR-011a**: While a run is in flight there is **nothing in the listing to point at**, because the
  folders it is creating do not exist yet and the ones it is reading are unchanged. This is bulk
  upload's situation rather than bulk delete's, and it MUST be handled the way upload already
  handles it: a background status while the work runs, and a report when it ends. There is no row to
  mark and no row to grey out, so the interface MUST NOT invent one. How that status is presented is
  a plan decision, not a requirement of this document.
- **FR-012**: Progress MUST be visible while a run is in flight, and MUST be rendered as an
  **indeterminate** indicator rather than as a proportion. The server counts completed top-level
  folders and nothing finer, so a proportion would invent precision that does not exist and would
  sit unchanged for many minutes on exactly the selections where the author most wants reassurance.
- **FR-013**: A run MUST survive the dialog that started it closing, the author navigating within
  the portlet, and the author leaving the portlet entirely.
- **FR-014**: The client MUST NOT mark, disable or otherwise make inert any folder while a run is
  working on it. A folder being duplicated stays fully usable: selectable, openable, a valid drop
  target, and available to another action. This is a deliberate difference from bulk folder delete
  and MUST NOT be "fixed" by copying that feature's marking across (backend C-011).
- **FR-015**: The client MUST NOT read the listing of in-flight runs, and MUST NOT restore any
  in-flight state on load. There is nothing to restore: no folder's appearance depends on whether a
  run is working on it.
- **FR-016**: The action MUST join the guard the existing actions already follow, which refuses a
  repeat of the **same operation on the same items** and nothing wider. A duplication running on one
  set of folders MUST NOT prevent an unrelated action, nor a duplication of different folders, from
  starting.
- **FR-017**: The client MUST NOT attempt to detect or refuse overlapping runs. Unlike bulk delete,
  the server does not refuse them either, because two copies of the same folder cannot interfere
  (backend FR-033). No submission refusal for overlap exists, and the client MUST NOT carry copy for
  one.

#### Reporting the outcome

- **FR-018**: When a run ends while the author is present, the client MUST report it without the
  author refreshing or polling anything.
- **FR-019**: The report MUST use the **server's** counts of duplicated, failed and skipped folders,
  never the size of the author's selection.
- **FR-020**: The report MUST identify folders by **the path the author selected**, not by the name
  the duplicate was given, which the outcome does not carry (backend FR-009a). Where the interface
  tells an author what to look for, it MUST describe the naming rule rather than claim a specific
  name it has not been told.
- **FR-021**: Every folder that was not duplicated MUST be identifiable from the report, with its
  reason. Folders MUST NOT be summarised away into a count alone.
- **FR-021a**: A **skipped** folder MUST read as not attempted rather than failed, and the two
  reasons a folder is skipped MUST be told apart: the run was cancelled before reaching it, or an
  ancestor in the same submission already covers it. These are different facts and one message
  cannot serve both. The second is not an error at all and MUST NOT be presented as one: the author
  selected a parent and its child, and got what they asked for once.
- **FR-022**: Where there are more entries than the report can show at once, it MUST name the first
  few and acknowledge the remainder as a count, and that remainder MUST be reachable rather than a
  dead end. How many are named before the overflow begins is a design choice for planning; that the
  overflow leads somewhere is the requirement.
- **FR-023**: Each machine-readable reason the server can return MUST map to copy written in the
  product's own words. Failures, at minimum: no rights on the folder, no rights to add to its
  parent, the folder no longer exists, the folder is protected, and a general fallback. Skips: the
  run was cancelled before reaching the folder, and an ancestor in the same submission already
  covers it. Reasons bulk delete carries which duplication cannot produce MUST NOT be written for
  this feature. The ancestor skip **is** shared with delete in shape but not in wording: delete says
  the ancestor removed the folder, and here the folder is untouched, so its copy MUST say the
  ancestor covers it.
- **FR-024**: The server's diagnostic message MUST NOT be shown to the author. It is written for a
  log.
- **FR-025**: A reason the client does not recognise MUST still name the folder and report it as
  failed, using the general fallback.
- **FR-026**: A clean run, a partial run and a cancelled run MUST read differently, and a cancelled
  run MUST NOT read as a fault. Cancellation copy MUST state that the folders not reached were not
  copied and that no partial duplicate was left behind, which is what the server guarantees (backend
  C-007). Wording taken from #37062's description, which says a partial copy is left at the
  destination, is **wrong** for this design and MUST NOT be used.
- **FR-027**: An outcome MUST be reported once, by whichever part of the interface is responsible
  for presenting it, not once per surface that knows about the run.
- **FR-028**: An author who was absent when a run ended MUST be able to find its outcome afterwards.
  The client MUST NOT build a background-jobs screen to satisfy this; the durable record the server
  keeps is what carries it.

#### After a run ends

- **FR-029**: When a run ends, the listing MUST show the duplicates it created, without the author
  reloading.
- **FR-030**: When a run ends, the sidebar tree MUST show them too. The two surfaces load
  independently and refreshing one does not refresh the other.
- **FR-031**: Folders that failed MUST leave both surfaces exactly as they were. No placeholder row
  and no partially created folder may appear for them.
- **FR-032**: Source folders MUST be untouched in both surfaces throughout and after the run.

#### Refusals at submission

- **FR-033**: A submission refused MUST be distinguishable to the author: an empty selection, too
  many folders, and no entitlement MUST NOT all read the same.

### Key Entities

- **Folder selection**: the folders an author chose in the listing and asked to duplicate.
- **Run**: one submitted duplication, identified by the handle the server returns, with a state the
  client can observe and an outcome it can read.
- **Per-folder outcome**: one record per submitted folder: the source path, whether it was
  duplicated, failed or skipped, and a reason on failure.
- **Run report**: what the author is shown when a run ends: the server's counts, and the folders
  that did not succeed with their reasons.

---

## Contract Consumed *(mandatory: the boundary with the server half)*

Restated from `specs/37062-folder-copy-backend/spec.md` §Contract Consumed by the Client (C-001 …
C-011), from this side.

- **One call, one handle** (C-001, C-002). The client sends the selected folder paths as one
  ordinary request and is answered immediately with a handle and a ready-made address for following
  the run. **No destination travels with the submission.** It never assembles that address itself.
- **A count to display, from the server** (C-003). The number of folders accepted into the run comes
  back with the handle and equals the total the outcome later reports. FR-019 depends on it.
- **Distinguishable refusals** (C-004): nothing submitted, over the maximum, and not entitled.
  FR-033 rests on this. **There is no overlap refusal**, because the server carries no overlap
  guard, which is why FR-017 forbids writing copy for one.
- **A stable, enumerated set of failure and skip reasons** (C-005), each mapped to client copy
  (FR-023). Nearly a subset of bulk delete's: duplication has no "something inside is in use". It
  does carry an ancestor skip, but the folder is still there afterwards, so the wording says the
  ancestor covers it rather than removed it.
- **Progress, and an honest statement of what it counts** (C-006). Completed top-level folders,
  nothing finer. This is why FR-012 requires an indeterminate indicator.
- **A way to cancel, with this operation's guarantee** (C-007). Each folder is left either fully
  duplicated or not created at all, so cancellation copy says the remainder was not duplicated
  rather than warning about partial results. **This client does not expose cancellation in this
  version** (D-005); the requirement on the wording binds whoever surfaces it.
- **A readable terminal state and outcome** (C-008). Counts plus per-folder records, each failure
  carrying its reason, and **nothing added to the shared shape**: a successful record names the path
  that was submitted, not the name the duplicate was given. FR-020 and FR-021 rest on the records
  being present rather than summarised, and FR-020 on their being keyed by the submitted path.
- **A pushed completion signal carrying the outcome, plus a durable record of the same** (C-009).
  FR-018 and FR-028 rest on this. The client does not poll for completion and does not build a jobs
  screen.
- **The signal is emitted after the copies are complete** (C-010). FR-029 and FR-030 refresh on it
  and depend on that ordering, which the client cannot observe for itself.
- **Nothing is announced and nothing needs marking** (C-011). A folder being duplicated stays usable
  throughout, which is what lets FR-014 and FR-015 remove the largest part of the sibling feature's
  client work.

**Explicitly the server's business, not specified here**: how a run is executed, the transaction
boundary, how a duplicate's name is derived, what happens to content within a subtree the author
cannot read, retry and abandonment behaviour, and the durable record's lifetime.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An author duplicates a selection of N folders with one action, where today no folder
  can be duplicated from Content Drive at all.
- **SC-002**: The author regains control of the interface within two seconds of committing,
  regardless of how much content the folders hold.
- **SC-003**: 100% of duplicates created are named in the report, so an author never has to search
  the listing to find out what was made.
- **SC-004**: 100% of folders the server accepted are accounted for in the report: duplicated with a
  name, failed with a reason, or skipped. Zero are silently dropped.
- **SC-005**: The counts the author is shown match the server's record in 100% of runs, including
  partial ones.
- **SC-006**: Every failure reason the server can emit renders as product copy; zero render as a raw
  code, and zero render the server's diagnostic text.
- **SC-007**: A folder being duplicated remains fully usable throughout the run in 100% of cases:
  zero folders are marked, disabled or made undroppable by this feature.
- **SC-008**: An author who was absent when a run ended can determine the full outcome, including
  which of their selected folders were duplicated, without having watched it.
- **SC-009**: After a run ends, both the listing and the sidebar tree show the new folders without a
  reload, in 100% of cases where the author is viewing the parent they landed in.
- **SC-010**: The author is never told a duplication succeeded when it did not, nor that it failed
  when it succeeded.
- **SC-011**: An author can tell from the action's label and its commit step alone, before running
  it, that no destination will be asked for and that the duplicate's name is chosen for them.

---

## Legacy Considerations *(dotCMS-specific, mandatory)*

- **Existing behavior touched**: the Content Drive portlet, which is modern surface rather than
  legacy. Three shared pieces are touched and each has a second consumer that must not regress: the
  folder list view, shared with the AssetPicker work; the portlet's shared execution state, consumed
  by the Action Center and by bulk upload; and the right-click menu, which today offers editing the
  folder, editing its permissions, adding to a bundle, push history and delete. This feature adds
  one item to that menu and one action to the Action Center.
- **Nothing here is new infrastructure.** Bulk file upload (#37166) established every pattern this
  feature reuses: a long operation submitted and answered immediately, its completion pushed rather
  than polled, and execution state held by the portlet rather than the dialog which started it. This
  feature consumes the job-following primitive that ticket owns and MUST NOT build a second one. If
  #37062 lands before #37166, it builds that primitive to #37166's stated requirements and the
  siblings reuse it unchanged.
- **The Action Center already has a destination picker, and this feature does not use it.** A
  destination picker component exists for contentlet bulk move. Duplicate-in-place has no
  destination, so it is not reused here. It remains the right starting point for folder move
  (#37165).
- **Backward-compatibility expectations**: the Site Browser's folder copy is untouched and keeps its
  behaviour, including the naming rule this feature surfaces. No new permission concept is
  introduced: the right to duplicate a folder in bulk is the right to duplicate it singly. The
  AssetPicker must not regress through the shared listing.
- **Known related decisions**: bulk folder delete (#37063) and folder move (#37165) share the
  outcome shape, so the reason-to-copy mapping is written once for all three, with each feature
  using the subset it can actually produce. `/speckit-plan` will consult `dotCMS/platform-adrs`
  formally.

---

## Out of Scope

- **The server-side implementation of #37062.** Specified in
  `specs/37062-folder-copy-backend/spec.md`.
- **Copying a folder to a chosen destination**, and the destination picker #33468 describes. Removed
  by the backend half's D-005; it belongs to folder move (#37165).
- **Marking, blocking or announcing a folder while it is being duplicated** (FR-014, FR-015). Bulk
  folder delete's entire in-flight machinery has no analogue here and is deliberately not built.
- **Cancelling a run from this client** (D-005 below). The server provides cancellation and the
  contract fixes its wording; surfacing it is expected to arrive with the background task manager
  (#33331), where bulk upload's stop also lives.
- **A general-purpose background-jobs screen.** Owned by #33331. FR-028 is satisfied by the server's
  durable record.
- **Copying files in a mixed selection as part of the same run.** Files are copied through the
  existing content path; reconciling two runs into one report is not attempted here (FR-004).
- **Renaming a duplicate at creation time, or letting the author choose its name.** The server
  derives it by a published rule, and the outcome does not restate it (FR-020). Renaming afterwards
  is the existing folder edit.
- **The other folder actions in the right-click menu.** Unchanged.

---

## Planning Obligations

- **Test coverage** (Constitution V). The plan MUST name which layers this feature exercises and
  which it does not, with a reason for each omission; silence is not an acceptable answer to
  Principle V. At minimum it MUST say what is covered by component and store unit tests (Vitest and
  Spectator, since the workspace migrated off Jest under #37444; **#37062's own checklist says Jest
  specs and that wording is stale and MUST NOT be copied**), what is covered end to end (Playwright,
  extending the existing Content Drive suite), and how the pushed completion signal is exercised,
  which no unit test of a single component reaches.
- **How the interface describes where a duplicate went** (FR-020). The report names the folder the
  author selected, not the duplicate that was made, so the plan MUST settle what the interface says
  to an author looking for the result: describing the naming rule is allowed, asserting a specific
  name is not, since the client is never told it.
- **The reason-to-copy mapping is a cross-half dependency.** Every reason the server can emit needs
  client copy before either half is implemented, and the mapping is shared with delete and move. The
  plan MUST name where it lives. Duplication adds no failure reason of its own, but it does need the
  ancestor skip worded for an operation that leaves the folder in place, so the plan MUST settle
  whether the shared value is reworded to cover both operations or a second value is added.
- **The action's label and its commit copy** (FR-002, FR-009). This is a microcopy decision with a
  functional consequence, since the label is what sets the author's expectation about a destination.
  The plan MUST treat it as a deliverable with an owner, not as a string to be filled in during
  implementation.
- **Which surface presents the outcome** (FR-027). Several already can. The plan MUST name one.
- **How the listing and the tree are refreshed on completion** (FR-029, FR-030), given they load
  independently, and what happens when the author is not viewing the affected parent.
- **How a folder is matched between a run and a row.** The client has both a path and an identifier
  for every folder it lists, and which one travels in the submission and in the outcome need not be
  the same choice. Both plans MUST agree.

---

## Assumptions

- The server half honours §Contract Consumed. Where it does not yet, this feature's stories are
  blocked rather than worked around: no client-side polling loop, retry ladder, count estimation or
  name derivation is introduced to compensate.
- Because completion is pushed, the client does not poll for it. It may read progress while the
  author is present, which is a different thing and is what FR-012 renders.
- Folder rights arrive with the folders the listing already loads. Where they are absent, FR-005b
  applies.
- The job-following primitive specified on #37166 is available, or is built here to that ticket's
  requirements and reused unchanged by the siblings.
- An outcome is reported once, by one responsible surface, rather than by each surface that knows
  about the run.
- All new author-facing copy is localisable through the existing message definitions, and no copy is
  hard-coded.
- Authors reaching this are back-office users on a maintained desktop browser; no additional
  compatibility target is introduced.
- The number of folders in one selection is bounded by the server's configured maximum, so the
  client is not designing for an unbounded report.

---

## Decisions

- **D-001: The action duplicates in place, and this client asks for no destination.** Taken by the
  developer on 2026-09-21 and recorded in full in the backend half's D-005. Its consequence here is
  the removal of the destination picker, the configuration step, and the three failure cases that
  only a chosen destination can produce.
- **D-002: The action is labelled as a duplication, not as a copy** *(clarification 2026-09-21,
  FR-002)*. "Copy" implies a destination or a paste step, and this operation has neither. The label
  is load-bearing rather than cosmetic: it is what prevents an author from waiting for a dialog that
  never appears.
- **D-003: No confirmation, and no destructive language** *(clarification 2026-09-21, FR-008,
  FR-010)*. Deleting confirms because it is permanent and recursive. Duplicating creates something
  the author can delete, so the bulk action passes through the ordinary commit step every bulk
  action uses and the single-folder action runs directly. Cheap to reverse if authors report
  duplicating large folders by accident.
- **D-004: Nothing is marked while a run is in flight** *(FR-014, FR-015, backend D-016)*. This is
  the largest reduction relative to bulk folder delete, which needs a doomed folder to look unusable
  everywhere it appears and to stay that way across reloads and authors. A folder being duplicated
  is safe to use, so none of that applies. Recorded explicitly so that a reviewer familiar with the
  sibling specification reads the absence as a decision rather than an omission.
- **D-005: Cancellation is not exposed in this version.** The server provides it and the contract
  fixes its wording; the surface for it is expected to be the background task manager (#33331),
  where bulk upload's stop also lives. A known asymmetry rather than an oversight.
- **D-006: The selection is filtered to what the author may duplicate, and the count says so**
  *(developer decision, 2026-09-22; FR-004, FR-005a)*. An earlier draft submitted the selection
  whole and let the server refuse, which is bulk delete's position. That position rests on delete
  being destructive: shrinking a destructive action behind the author's back leaves them believing
  folders are gone that are not. Duplication destroys nothing, so the argument does not transfer,
  and behaving like every other action in the same menu is worth more than matching a sibling whose
  reason does not apply. The filtering is kept honest by the count rather than by a later report.
- **D-006a: Add-children on the folder being browsed gates the whole action** *(developer decision,
  2026-09-22; FR-005)*. This became checkable only because of duplicate-in-place. While a
  destination was still in the design the target parent was unknown to the client, and an earlier
  draft therefore recorded that the client could see at most one of the two rights the operation
  needs. With the duplicate landing in the folder the author is standing in, the client holds that
  folder and its rights, so the second check is available for free and the action can be withheld
  before anything is submitted. The exception is a selection spanning several parents, which a
  search or filter allows and which leaves no single folder to check (FR-005b).
- **D-007: The single-folder action uses the same asynchronous submission as the bulk one**
  *(FR-003, backend D-010)*. There is no synchronous single-folder endpoint to call. One submission
  path means one outcome shape, one error mapping and one set of copy.
- **D-008: Progress renders as an indeterminate indicator** *(FR-012, C-006)*. Not a client
  preference: the server counts completed top-level folders and nothing finer, so a bar would sit
  unchanged for the whole of the folder that matters and read as a hung feature.

---

## Open Decisions

**None.** Every question this specification raised is closed, either here in §Decisions or inline at
the requirement it governs.

What remains genuinely undecided is not a specification question and is listed under §Planning
Obligations: how the interface describes where a duplicate went, which surface presents the outcome,
how the two surfaces refresh on completion, and what a folder is matched by. All are sized at the
plan phase and none changes what this document requires.
