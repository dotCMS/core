# Feature Specification: Content Drive folder copy (frontend)

**Feature Branch**: `37062-content-drive-folder-copy`

**Created**: 2026-09-21

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37062](https://github.com/dotCMS/core/issues/37062): "Content Drive: folder copy, async job endpoint and frontend wiring", frontend half.

---

## Context

An author has a campaign folder they want to reuse as the starting point for next quarter's work.
Today Content Drive cannot help them: folders are selectable in the listing and appear in the
right-click menu, but there is **no copy action anywhere**, for one folder or for many. The only
folder copy in the product lives in the old Site Browser. This specification covers the browser-side
half of fixing that.

**This specification covers the client only.** The server half is specified in
`specs/37062-folder-copy-backend/spec.md`. The two meet at that document's *Contract Consumed by the
Client* (C-001 … C-012), restated here from the consumer's side in §Contract Consumed. Nothing about
server behaviour is re-specified here.

**The operation duplicates a folder in place.** The copy lands beside the original, in the same
parent, under a name the server derives so it does not collide. **The author does not choose where
it goes**, so this feature has no destination picker and no configuration step. That is a
deliberate narrowing of #37062's description, taken by the developer and recorded in the backend
half as its D-005, and it is the reason this specification is a fraction of the size of its sibling
for bulk delete.

**Nothing is blocked while a copy runs.** A folder being copied is still a folder: it can be opened,
uploaded into, dragged onto and copied again. The elaborate marking machinery bulk folder delete
needs, making a doomed folder inert everywhere it appears and keeping that true across reloads and
authors, has no purpose here and is deliberately absent (backend D-016). What this client does while
a run is in flight is show progress; what it does when the run ends is report what was made.

**The one thing this feature must get right that its siblings do not.** A duplicate's name is chosen
by the server, not by the author, and it is not predictable: duplicating `campaign` gives
`campaign_copy`, duplicating it again gives a further derived name. An author who duplicates six
folders and is told only "6 succeeded" has to go hunting through the listing to find out what was
made. The report must name the folders that were created (C-006), and that obligation shapes more of
this document than the progress indicator does.

Throughout this document, **selecting rows** means picking folders in the Content Drive listing. A
**run** is one submitted copy, identified by a handle the server returns. **Duplicate** is used for
the folder the run creates.

---

## Clarifications

### Session 2026-09-21

- Q: The action duplicates in place rather than copying to a chosen location. Is it labelled "Copy"? → A: No. It is labelled as a duplication. "Copy" sets up an expectation of choosing a destination, or of a paste step that never comes, and the label is the only thing standing between the author and that expectation. See FR-002 and D-002.
- Q: Does duplicating require a confirmation, as deleting does? → A: No. Deleting confirms because it is permanent and recursive; duplicating creates something the author can simply delete. The bulk action still passes through the existing preview step every bulk action uses, because that is where the author sees what will be acted on and presses the button, but it carries no warning language. The single-folder action runs directly.
- Q: The author selects six folders and may copy only four. Does the action submit four or six? → A: Six. The client does not filter a selection by its own reading of rights; the two the server refuses come back as per-folder permission failures. The action is withheld only when the author can copy none of them. This follows the position bulk delete reached, for the same reason: silently shrinking what the author asked for is worse than reporting the refusal.
- Q: Does a running copy block other Content Drive actions? → A: No. It follows the existing guard unchanged, which refuses a repeat of the same operation on the same items and nothing wider. Unrelated actions and copies of other folders run alongside it. Unlike delete, overlapping copies are not refused by the server either, because two copies of the same folder cannot harm each other (backend FR-033).

---

## User Scenarios & Testing *(mandatory)*

<!--
  Written as author-visible behaviour in the Content Drive interface. Each is verifiable in the
  browser against a server honouring §Contract Consumed, with no knowledge of how the server
  implements it.
-->

### User Story 1 - Several folders are duplicated in one action (Priority: P1)

An author selects several folders in the listing and chooses to duplicate them. The action is
submitted, the dialog closes at once, and the author carries on working while the copies are made.

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
4. **Given** a run is in flight, **When** the author looks at the folders being copied, **Then**
   they behave as ordinary folders: openable, selectable, and valid drop targets.
5. **Given** the author dismisses before committing, **When** the dialog closes, **Then** nothing is
   submitted and the selection is untouched.

---

### User Story 2 - The author is told what was created (Priority: P1)

When a run ends, the author is told how many folders were duplicated, **what each duplicate is
called**, and which folders were not copied and why.

**Why this priority**: P1 and the story that distinguishes this feature from its siblings. The
server names each duplicate rather than the author naming it, and those names are not predictable
from the source. A report that gives only counts leaves the author to search the listing for what
changed, which is the task the feature was supposed to do for them.

**Independent Test**: Duplicate a folder that has already been duplicated once, alongside a folder
the author may not copy; confirm the report names the new folder exactly as it was created and names
the refused folder with a reason.

**Acceptance Scenarios**:

1. **Given** a run ends, **When** the report appears, **Then** it uses the server's counts of
   duplicated and failed folders, never the size of the author's selection.
2. **Given** a folder was duplicated, **When** the report is read, **Then** the name the duplicate
   was given is shown, so the author can find it without searching.
3. **Given** a folder could not be duplicated, **When** the report is read, **Then** that folder is
   named along with the reason, in the product's own words.
4. **Given** more failures than the report can show at once, **When** it is read, **Then** it names
   the first few, counts the rest, and leads to the full list rather than ending in a bare count.
5. **Given** a run that duplicated nothing because every folder was refused, **When** the report
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
than for a delete: an author who cannot find out whether a copy happened, and resubmits, gets a
second duplicate rather than a harmless no-op (backend FR-034).

**Independent Test**: Submit a run, navigate away from the portlet, return after it completes, and
confirm the outcome including the duplicates' names is still reachable.

**Acceptance Scenarios**:

1. **Given** a run is in flight, **When** the author closes the dialog that started it or navigates
   within the portlet, **Then** the run continues and is unaffected.
2. **Given** a run ended while the author was elsewhere, **When** they return, **Then** they can
   determine the full outcome, including what was created, without having watched it.
3. **Given** a run is in flight, **When** the author leaves the portlet entirely, **Then** nothing
   cancels it.

---

### User Story 5 - Both surfaces show the new folders once a run ends (Priority: P2)

The duplicates appear in the listing and in the sidebar tree when the run finishes, without the
author reloading.

**Why this priority**: P2 because the author can reload. Without it the feature looks broken:
the report says six folders were created and the screen shows none of them.

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

The action is withheld where the author is known to have no right to it, and offered everywhere else.

**Why this priority**: P3 because the per-folder outcome reports refusals honestly, so a submission
that fails is informative rather than broken. It is still worth doing: offering an action that can
only fail teaches authors to distrust the interface.

**Independent Test**: With an author who may duplicate none of the selected folders, confirm the
action is unavailable; with one who may duplicate some, confirm it is offered and acts on all of
them.

**Acceptance Scenarios**:

1. **Given** a selection where the author may duplicate none of the folders, **When** the action
   list opens, **Then** the action is not available.
2. **Given** a selection where the author may duplicate some, **When** the action is used, **Then**
   every selected folder is submitted and the refusals come back per folder.
3. **Given** a folder whose rights the client cannot determine, **When** the action list opens,
   **Then** the action remains available and the server's refusal is what reports it.

---

### Edge Cases

- **The selection mixes folders and files.** The action acts on the folders and says so; the files
  are not part of this run.
- **The selection mixes folders the author may duplicate with folders they may not.** All are
  submitted; the refused ones come back as per-folder permission failures.
- **A parent and its own child are both selected.** Both are duplicated, and the report shows two
  successes. This is correct and must not be presented as an anomaly, unlike bulk delete where the
  child is reported as skipped because the parent already removed it.
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
  states that the folders not reached were not copied.
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
- **FR-005**: The action MUST be unavailable when the author is known to have no right to duplicate
  **any** of the selected folders, judged from the rights carried with the folders in the listing.
  This is the whole-selection case only.
- **FR-005a**: The client MUST NOT drop folders from a submission based on its own reading of
  rights. A selection the author may only partly duplicate MUST be submitted whole and the refusals
  reported per folder, rather than quietly omitted.
- **FR-005b**: Where the author's rights over a folder are unknown, the action MUST remain available
  and the server's per-folder refusal MUST be what reports it. The client MUST NOT guess a refusal.
  The rights the operation needs are reading the folder and adding to its **parent**, and the row
  carries the folder's own rights rather than its parent's, so the client can check one of the two
  at most. The gate is a courtesy that removes obviously futile submissions, not a prediction
  (backend FR-012c).
- **FR-006**: The client MUST respect the maximum number of folders one submission may carry,
  reading that limit from the server rather than holding its own copy, and MUST explain a refusal
  caused by it in terms of the limit.

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

- **FR-012**: Progress MUST be visible while a run is in flight, and MUST be rendered as an
  **indeterminate** indicator rather than as a proportion. The server counts completed top-level
  folders and nothing finer, so a proportion would invent precision that does not exist and would
  sit unchanged for many minutes on exactly the selections where the author most wants reassurance.
- **FR-013**: A run MUST survive the dialog that started it closing, the author navigating within
  the portlet, and the author leaving the portlet entirely.
- **FR-014**: The client MUST NOT mark, disable or otherwise make inert any folder while a run is
  working on it. A folder being duplicated stays fully usable: selectable, openable, a valid drop
  target, and available to another action. This is a deliberate difference from bulk folder delete
  and MUST NOT be "fixed" by copying that feature's marking across (backend C-012).
- **FR-015**: The client MUST NOT read the listing of in-flight runs, and MUST NOT restore any
  in-flight state on load. There is nothing to restore: no folder's appearance depends on whether a
  run is working on it.
- **FR-016**: The action MUST join the guard the existing actions already follow, which refuses a
  repeat of the **same operation on the same items** and nothing wider. A copy running on one set of
  folders MUST NOT prevent an unrelated action, nor a copy of different folders, from starting.
- **FR-017**: The client MUST NOT attempt to detect or refuse overlapping runs. Unlike bulk delete,
  the server does not refuse them either, because two copies of the same folder cannot interfere
  (backend FR-033). No submission refusal for overlap exists, and the client MUST NOT carry copy for
  one.

#### Reporting the outcome

- **FR-018**: When a run ends while the author is present, the client MUST report it without the
  author refreshing or polling anything.
- **FR-019**: The report MUST use the **server's** counts of duplicated, failed and skipped folders,
  never the size of the author's selection.
- **FR-020**: For every folder that was duplicated, the report MUST show **the name the duplicate
  was given**. The author did not choose it, cannot predict it, and cannot otherwise tell which new
  folder came from which source. A report of counts alone does not satisfy this requirement.
- **FR-021**: Every folder that was not duplicated MUST be identifiable from the report, with its
  reason. Folders MUST NOT be summarised away into a count alone.
- **FR-022**: Where there are more entries than the report can show at once, it MUST name the first
  few and acknowledge the remainder as a count, and that remainder MUST be reachable rather than a
  dead end. How many are named before the overflow begins is a design choice for planning; that the
  overflow leads somewhere is the requirement.
- **FR-023**: Each machine-readable reason the server can return MUST map to copy written in the
  product's own words. At minimum: no rights on the folder, no rights to add to its parent, the
  folder no longer exists, the folder is protected, and a general fallback. This set is a **subset**
  of the one bulk delete needs, and reasons that operation carries which copy cannot produce MUST
  NOT be written for this feature.
- **FR-024**: The server's diagnostic message MUST NOT be shown to the author. It is written for a
  log.
- **FR-025**: A reason the client does not recognise MUST still name the folder and report it as
  failed, using the general fallback.
- **FR-026**: A clean run, a partial run and a cancelled run MUST read differently, and a cancelled
  run MUST NOT read as a fault. Cancellation copy MUST state that the folders not reached were not
  copied and that no partial duplicate was left behind, which is what the server guarantees (backend
  C-008). Wording taken from #37062's description, which says a partial copy is left at the
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
- **Run**: one submitted copy, identified by the handle the server returns, with a state the client
  can observe and an outcome it can read.
- **Per-folder outcome**: one record per submitted folder: the source, whether it was duplicated,
  failed or skipped, the name of the duplicate on success, and a reason on failure.
- **Run report**: what the author is shown when a run ends: the server's counts, the names of what
  was created, and the folders that did not succeed with their reasons.

---

## Contract Consumed *(mandatory: the boundary with the server half)*

Restated from `specs/37062-folder-copy-backend/spec.md` §Contract Consumed by the Client (C-001 …
C-012), from this side.

- **One call, one handle** (C-001, C-002). The client sends the selected folder paths as one
  ordinary request and is answered immediately with a handle and a ready-made address for following
  the run. **No destination travels with the submission.** It never assembles that address itself.
- **A count to display, from the server** (C-003). The number of folders accepted into the run comes
  back with the handle and equals the total the outcome later reports. FR-019 depends on it.
- **Distinguishable refusals** (C-004): nothing submitted, over the maximum, and not entitled.
  FR-033 rests on this. **There is no overlap refusal**, because the server carries no overlap
  guard, which is why FR-017 forbids writing copy for one.
- **A stable, enumerated set of failure reasons** (C-005), each mapped to client copy (FR-023). A
  subset of bulk delete's set: copy has no "something inside is in use" and no "an ancestor already
  removed it".
- **The name each duplicate was given, per successful path** (C-006). This is the addition copy
  makes to the shared outcome shape, and FR-020 depends on it entirely. Without it this client
  cannot tell the author what was created, and no client-side derivation substitutes: the naming
  rule appends repeatedly, so the name depends on what already existed at the moment the copy ran.
- **Progress, and an honest statement of what it counts** (C-007). Completed top-level folders,
  nothing finer. This is why FR-012 requires an indeterminate indicator.
- **A way to cancel, with this operation's guarantee** (C-008). Each folder is left either fully
  duplicated or not created at all, so cancellation copy says the remainder was not copied rather
  than warning about partial results. **This client does not expose cancellation in this version**
  (D-005); the requirement on the wording binds whoever surfaces it.
- **A readable terminal state and outcome** (C-009). Counts plus per-folder records, each success
  carrying its duplicate's name and each failure its reason. FR-020 and FR-021 depend on the records
  being present rather than summarised.
- **A pushed completion signal carrying the outcome, plus a durable record of the same** (C-010).
  FR-018 and FR-028 rest on this. The client does not poll for completion and does not build a jobs
  screen.
- **The signal is emitted after the copies are complete** (C-011). FR-029 and FR-030 refresh on it
  and depend on that ordering, which the client cannot observe for itself.
- **Nothing is announced and nothing needs marking** (C-012). A folder being copied stays usable
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
  the names of what was created, without having watched it.
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
- **Marking, blocking or announcing a folder while it is being copied** (FR-014, FR-015). Bulk
  folder delete's entire in-flight machinery has no analogue here and is deliberately not built.
- **Cancelling a run from this client** (D-005 below). The server provides cancellation and the
  contract fixes its wording; surfacing it is expected to arrive with the background task manager
  (#33331), where bulk upload's stop also lives.
- **A general-purpose background-jobs screen.** Owned by #33331. FR-028 is satisfied by the server's
  durable record.
- **Copying files in a mixed selection as part of the same run.** Files are copied through the
  existing content path; reconciling two runs into one report is not attempted here (FR-004).
- **Renaming a duplicate at creation time, or letting the author choose its name.** The server
  derives it; this feature reports it (FR-020). Renaming afterwards is the existing folder edit.
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
- **Where the duplicate's name is surfaced in the report** (FR-020). This is the requirement most
  likely to be lost in implementation, because every sibling feature's report is built from counts
  and reasons alone and this one needs a third column. The plan MUST name the component that renders
  it and confirm the field is read from the per-folder record rather than derived.
- **The reason-to-copy mapping is a cross-half dependency.** Every reason the server can emit needs
  client copy before either half is implemented, and the mapping is shared with delete and move.
  The plan MUST name where it lives, and MUST record that copy uses a subset rather than adding
  reasons of its own.
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
  everywhere it appears and to stay that way across reloads and authors. A folder being copied is
  safe to use, so none of that applies. Recorded explicitly so that a reviewer familiar with the
  sibling specification reads the absence as a decision rather than an omission.
- **D-005: Cancellation is not exposed in this version.** The server provides it and the contract
  fixes its wording; the surface for it is expected to be the background task manager (#33331),
  where bulk upload's stop also lives. A known asymmetry rather than an oversight.
- **D-006: The selection is submitted whole, not pre-filtered** *(clarification 2026-09-21,
  FR-005a)*. Following the position bulk delete reached, and for the same reason: shrinking what the
  author asked for behind their back is worse than reporting the refusal, and the client's reading
  of rights is the less reliable of the two. It matters slightly less here, because copy refuses
  less often, and slightly more, because the client can only see one of the two rights involved.
- **D-007: The single-folder action uses the same asynchronous submission as the bulk one**
  *(FR-003, backend D-010)*. There is no synchronous single-folder endpoint to call. One submission
  path means one outcome shape, one error mapping and one set of copy.
- **D-008: Progress renders as an indeterminate indicator** *(FR-012, C-007)*. Not a client
  preference: the server counts completed top-level folders and nothing finer, so a bar would sit
  unchanged for the whole of the folder that matters and read as a hung feature.

---

## Open Decisions

**None.** Every question this specification raised is closed, either here in §Decisions or inline at
the requirement it governs.

What remains genuinely undecided is not a specification question and is listed under §Planning
Obligations: where the duplicate's name is surfaced, which surface presents the outcome, how the two
surfaces refresh on completion, and what a folder is matched by. All are sized at the plan phase and
none changes what this document requires.
