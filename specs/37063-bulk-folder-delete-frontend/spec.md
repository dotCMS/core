# Feature Specification: Content Drive bulk folder delete — frontend

**Feature Branch**: `37063-content-drive-bulk-folder-delete-frontend`

**Created**: 2026-09-16

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [dotCMS/core#37063](https://github.com/dotCMS/core/issues/37063) — "Content Drive: bulk folder delete", frontend half.

---

## Context

An author selects six old campaign folders in Content Drive and wants them gone. Today they cannot:
folders are selectable in the listing, but the only delete available acts on one folder at a time
from the right-click menu, and on a folder of any real size it times out before it finishes. This
specification covers the browser-side half of fixing that.

**This specification covers the client only.** The server half is delivered separately by another
developer and is specified in `specs/37063-bulk-folder-delete-backend/spec.md`. The two halves meet
at that document's *Contract Consumed by the Client* (C-001 … C-012), restated here from the
consumer's side in §Contract Consumed. Nothing about server behaviour is re-specified here.

**Deleting a folder deletes everything inside it, permanently, and there is no undo.** That single
fact sets the shape of this feature. It is why the confirmation matters more than the progress
indicator, why a folder that is mid-deletion must never look like a folder an author can still use,
and why the outcome has to name what survived rather than report a number.

Throughout this document, **selecting rows** means picking folders in the Content Drive listing.
**In-flight** describes a folder a run is currently working on, wherever it appears. A **run** is one
submitted bulk delete, identified by a handle the server returns.

**Most of the machinery already exists.** Bulk file upload (#37166) shipped the pieces this feature
consumes: the listing can already mark individual rows as busy, the portlet already holds execution
state that outlives the dialog which started it, and a long operation is already submitted, answered
immediately, and has its completion pushed to the author rather than polled for. This feature reuses
all three and builds none of them a second time. What it adds is the folder-shaped parts: the action
itself, the same marking in the sidebar tree, and restoring that marking from server state after a
reload.

**Why restoring after a reload is a requirement and not a refinement.** A measured delete of a folder
holding twenty thousand files took roughly eight minutes. An author will reload, navigate away, open
a second tab, or hand the screen to a colleague inside that window. If the client's only memory of a
run is in the page that started it, everything after a reload is a lie: the folder is shown as
ordinary, it can be opened, uploaded into and dragged onto, and it then disappears under whoever is
using it. The server records the folders a run is working on and exposes the in-flight runs, so the
client can recover the truth — and the decision to depend on that is what fixed the server's own
answer about who may read those paths (backend D-015).

---

## Clarifications

### Session 2026-09-16

- Q: How is a folder's in-flight state made perceivable to an author using assistive technology, given the existing marking is conveyed only by reduced opacity and disabled pointer events? → A: Each in-flight row and tree node carries an accessible busy/disabled state, so assistive technology reports it when the author reaches the folder.
- Q: The author selects six folders and may delete only four — does Delete act on four, or on six? → A: On six. The client does not filter a selection by its own reading of rights; every selected folder is submitted, and the two the server refuses come back as per-folder permission failures in the report. The action is still withheld when the author can delete none of them.
- Q: Does a running delete block other Content Drive actions? → A: No. Delete follows the existing guard unchanged — it refuses a repeat of the same delete on the same folders, and nothing else. Unrelated actions and deletes of other folders run alongside it; overlapping subtrees are refused by the server, not by the client.
- Q: The report names the first few failures and counts the rest — how does the author reach the rest? → A: The report links to the durable record, so the overflow count is followed to the full per-folder list rather than being a dead end.
- Q: A run started by another author sends this client no completion signal, so how does its marking clear? → A: In-flight runs are established once when Content Drive opens, and kept current from the announcements the server makes as folders enter and leave a delete. No repeated polling. *(Revised 2026-09-16: this was first answered with an accepted limitation — a run started after the page opened would not be marked until the next load. It was put to the backend half as a question, that half took the change, and the limitation is gone. See FR-020a, FR-020b and backend D-016.)*

---

## User Scenarios & Testing *(mandatory)*

<!--
  Written as author-visible behaviour in the Content Drive interface. Each is verifiable in the
  browser against a server honouring §Contract Consumed, with no knowledge of how the server
  implements it.
-->

### User Story 1 - Several folders are deleted with one confirmation (Priority: P1)

An author selects several folders in the listing and chooses Delete. They are told plainly that the
selected folders and everything inside them will be permanently deleted and cannot be recovered. On
confirming, the dialog closes at once, the chosen folders are marked as in-flight, and the author
carries on working elsewhere in the portlet.

**Why this priority**: This is the feature. Every other story exists to make this one safe or honest.
Delivered alone it is a complete, usable capability.

**Independent Test**: Select two or more folders, delete, and confirm the folders are gone and the
author was never blocked while it happened.

**Acceptance Scenarios**:

1. **Given** several folders are selected, **When** the author opens the action list, **Then** Delete
   is offered and states how many of the selection it will act on.
2. **Given** Delete is chosen, **When** the confirmation appears, **Then** it says the selected
   folders **and all their contents** are permanently deleted and cannot be recovered.
3. **Given** the author confirms, **When** the submission is accepted, **Then** the confirmation
   closes immediately and the rest of the portlet stays usable.
4. **Given** the submission is accepted, **When** the author looks at the listing, **Then** the
   folders the run accepted are marked as in-flight and cannot be selected into another action.
5. **Given** the author cancels the confirmation, **When** the dialog closes, **Then** nothing is
   submitted and the selection is untouched.

---

### User Story 2 - A folder being deleted is unusable everywhere it appears (Priority: P1)

While a run is working on a folder, that folder must not behave like a folder anywhere on screen. In
the listing its row is marked and inert. In the sidebar tree its node is marked too, and cannot be
opened, expanded, or used as a drop target.

**Why this priority**: Without the tree, the listing's marking is a door left open. An author can
walk into, upload into, or drag content onto a folder that is being destroyed, and the content they
put there dies with it.

**Independent Test**: Start a delete on a folder visible in the sidebar tree; confirm the node is
marked and refuses navigation, expansion and drops for the duration.

**Acceptance Scenarios**:

1. **Given** a run is working on a folder present in the sidebar tree, **When** the author looks at
   the tree, **Then** that node is marked as in-flight.
2. **Given** a tree node is marked as in-flight, **When** the author clicks it, expands it, or drops
   content on it, **Then** nothing happens.
3. **Given** a folder is marked as in-flight in the listing, **When** the author tries to select it
   or open it, **Then** it is refused, consistent with how the listing already treats a busy row.
4. **Given** a run ends, **When** its outcome is known, **Then** the marking is removed from both the
   listing and the sidebar tree, whether the run succeeded, failed or was cancelled.
5. **Given** a folder's delete failed, **When** the marking is removed, **Then** the folder is usable
   again rather than left inert.

---

### User Story 3 - Reloading the page does not make a doomed folder look safe (Priority: P1)

An author starts a delete of a large folder and reloads the browser, or opens Content Drive in a
second tab, or a colleague opens it on another machine. The folder still shows as in-flight, because
the client reads which folders are being worked on from the server rather than from its own memory.

**Why this priority**: This is the correctness story. It is the difference between an interface that
tells the truth after a reload and one that presents a folder that is actively being destroyed as
available. It is P1 for the same reason the confirmation is: the failure mode is data loss the author
did not intend.

**Independent Test**: Start a delete on a folder large enough to still be running, reload, and confirm
the folder is still marked and still inert.

**Acceptance Scenarios**:

1. **Given** a run is in flight, **When** the author reloads Content Drive, **Then** the folders that
   run is working on are marked as in-flight, in both the listing and the sidebar tree.
2. **Given** a run is in flight, **When** the author opens Content Drive in a second tab, **Then**
   that tab marks the same folders.
3. **Given** a run started by **another** author was already in flight when this author opened Content
   Drive, **When** this author browses the same site, **Then** those folders are marked for them too.
3a. **Given** a run started by another author began **after** this author opened Content Drive,
   **When** it begins, **Then** those folders are marked for this author without a reload, from the
   announcement the server makes (FR-020a).
3b. **Given** a run another author started ends without announcing it — the process behind it died —
   **When** this author next opens Content Drive, **Then** the marking is gone, because the in-flight
   set is established again on load rather than inherited from what the client was told (FR-020b).
4. **Given** the marking was restored from server state, **When** the run ends, **Then** the marking
   clears without the author reloading again.
5. **Given** no run is in flight, **When** Content Drive loads, **Then** nothing is marked and the
   listing behaves exactly as it does today.
6. **Given** the in-flight state cannot be read, **When** Content Drive loads, **Then** the listing
   still renders and remains usable, with nothing marked — an unreadable run must degrade to today's
   behaviour, never to a broken listing.

---

### User Story 4 - The author is told what actually happened (Priority: P1)

A run ends. The author is told how many folders were deleted and how many were not, using the
server's own counts, and every folder that was not deleted is named with a reason they can act on.

**Why this priority**: A bulk destructive operation that reports only "done" is unusable — the author
has no way to know what survived, and re-selecting to find out means looking for folders that may or
may not still exist.

**Independent Test**: Delete a mixed selection where some folders are refused; confirm the counts come
from the server and the refused folders are named with distinguishable reasons.

**Acceptance Scenarios**:

1. **Given** a run ends, **When** the outcome is presented, **Then** the counts shown are the
   server's, never the number of rows the author selected.
2. **Given** some folders failed, **When** the outcome is presented, **Then** the failing folders are
   named together with a reason expressed in the product's own words.
3. **Given** more folders failed than the outcome can show at once, **When** the outcome is presented,
   **Then** the first few are named and the remainder is acknowledged as a count, never silently
   dropped.
3a. **Given** the outcome acknowledges a remainder, **When** the author follows it, **Then** they reach
   the full list of folders that did not succeed, each with its reason.
4. **Given** a folder was skipped, **When** the outcome is presented, **Then** it reads as not
   attempted rather than as a failure, and says which kind of skip it was.
5. **Given** the run ended cleanly, **When** the outcome is presented, **Then** it does not read as a
   partial or faulted run.
6. **Given** a reason arrives that the client does not recognise, **When** the outcome is presented,
   **Then** the folder is still named and reported as failed, with a general explanation.

---

### User Story 5 - Leaving does not lose the outcome (Priority: P2)

An author submits a delete and navigates to another portlet, or closes the tab, or their session
ends. The run continues, and when they come back they can still find out how it went.

**Why this priority**: Directly implied by the operation's length. It is P2 rather than P1 because the
run completing correctly does not depend on it — only the author's ability to learn the result does.

**Independent Test**: Submit a delete, navigate away before it ends, return, and confirm the outcome
is discoverable.

**Acceptance Scenarios**:

1. **Given** a run is in flight, **When** the author navigates away from Content Drive or closes the
   dialog, **Then** the run continues.
2. **Given** the author is present when the run ends, **When** it ends, **Then** they are told without
   having to refresh anything.
3. **Given** the author was absent when the run ended, **When** they return, **Then** the outcome —
   counts and per-folder reasons — is still available to them.
4. **Given** the author is looking at a different folder when a run ends, **When** it ends, **Then**
   they are still told, and the report says which folders it was about.

---

### User Story 6 - Both surfaces agree once a run ends (Priority: P2)

When a run finishes, the listing and the sidebar tree both stop showing the deleted folders. They
load independently, so refreshing one is not refreshing the other.

**Why this priority**: A tree still offering a folder the listing has already dropped is how an author
navigates into nothing. Separate from Story 2 because that one is about the run being *in flight* and
this one about it being *over*.

**Independent Test**: Delete a folder visible in both surfaces and confirm both stop showing it
without a manual reload.

**Acceptance Scenarios**:

1. **Given** a run deleted folders, **When** it ends, **Then** the listing no longer shows them.
2. **Given** a run deleted folders, **When** it ends, **Then** the sidebar tree no longer shows them.
3. **Given** the author is browsing inside a folder that was deleted, **When** the run ends, **Then**
   they are moved somewhere valid rather than left on a location that no longer exists.
4. **Given** a folder failed to delete, **When** the run ends, **Then** it is still present in both
   surfaces.

---

### User Story 7 - The author is not offered a delete that will be refused (Priority: P3)

Delete is not offered for a selection the author has no right to delete, and a submission that the
server refuses outright is explained in terms the author can act on.

**Why this priority**: A refinement of Story 1 rather than a separate journey, and the server refuses
correctly regardless. It matters because offering an action and then refusing it teaches authors to
distrust the action list.

**Independent Test**: As an author without rights over the selected folders, confirm Delete is
unavailable; as an author colliding with a running delete, confirm the refusal explains itself.

**Acceptance Scenarios**:

1. **Given** the author lacks the right to delete **any** of the selected folders, **When** the action
   list is shown, **Then** Delete is unavailable for that selection.
1a. **Given** the author may delete some of the selected folders but not others, **When** they delete,
   **Then** all of them are submitted, the permitted ones are deleted, and the refused ones are named
   in the report as permission failures — none is dropped before submission.
2. **Given** a submission overlaps a delete already running, **When** it is refused, **Then** the
   author is told which folder is already being deleted, and never who is deleting it.
3. **Given** a submission carries more folders than the server accepts, **When** it is refused,
   **Then** the author is told the limit rather than shown a generic failure.
4. **Given** the author's rights cannot be determined for a folder, **When** the action list is shown,
   **Then** Delete is offered and the server's per-folder refusal is what reports it.

---

### Edge Cases

- **The selection mixes folders and files.** Delete acts on the folders and says so; the files are not
  part of this run.
- **The selection mixes folders the author may delete with folders they may not.** All are submitted;
  the refused ones come back as per-folder permission failures (FR-004a). Only a selection where the
  author may delete none of them withholds the action.
- **A parent and its own child are both selected.** The server deduplicates and reports the child as
  skipped; the outcome must read as "already covered", not as a failure.
- **A run is cancelled from elsewhere** (an administrator, a future task manager). The client is not
  the only thing that can end a run: the marking must clear and the outcome must present as cancelled,
  not as a fault.
- **A run ends while the author is looking at a folder it did not touch.** The report still names its
  folders; the listing the author is on need not change.
- **Two runs are in flight at once**, one of them a delete. Already supported: the existing in-flight
  reporting covers several concurrent runs, and delete joins it rather than replacing it (FR-018).
- **The author starts a second delete on different folders while the first runs.** Allowed. Only a
  repeat of the same delete on the same folders is refused locally, and an overlapping subtree is
  refused by the server.
- **The server reports a folder as deleted that the listing never showed** (deleted by someone else in
  between). The outcome is still reported for it; the listing simply has nothing to remove.
- **A run's folders lie on a site the author is not currently browsing.** The marking applies wherever
  those folders are visible, and nowhere else.
- **The in-flight listing is momentarily unavailable on load.** Nothing is marked and the listing works
  (Story 3, scenario 6) — the client does not block rendering on it.
- **Another author starts a delete while this page is open.** Marked here as it begins, from the
  announcement the server makes — no reload needed (FR-020a, backend C-012).
- **An observed run ends while the author is looking at a different site.** The mark clears anyway; it
  is held per folder, not per screen.
- **A discovered run's observation drops** (connection lost, server restart). The mark MUST NOT be
  stranded: losing the ability to observe a run is treated as not knowing its state, and the folder
  returns to unmarked rather than staying inert forever.
- **A run that already failed is still in the list of in-flight runs.** It marks nothing (FR-019a). The
  folder it failed on is intact and must behave as an ordinary folder.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Offering the action

- **FR-001**: Authors MUST be able to select one or more folders in the Content Drive listing and
  delete them in one action.
- **FR-002**: Delete MUST be offered from the same place the other multi-selection actions are
  offered, so authors find it where they already look.
- **FR-003**: Delete MUST act on the folders in the selection only. A selection containing files as
  well MUST still offer Delete, acting on the folders, and MUST state how many items it will act on
  rather than implying it covers the whole selection.
- **FR-004**: Delete MUST be unavailable when the author is known to have no right to delete **any** of
  the selected folders, judged from the rights carried with the folders in the listing. This is the
  whole-selection case only — an action that can only fail is not offered.
- **FR-004a**: The client MUST NOT drop folders from a submission based on its own reading of rights.
  A selection the author may only partly delete MUST be submitted whole, and the folders the server
  refuses MUST be reported as per-folder permission failures (FR-026) rather than quietly omitted.
  Silently shrinking a destructive action the author explicitly asked for is worse than reporting a
  refusal: the author sees six folders selected, and nothing tells them two were never attempted.
- **FR-005**: Where the author's rights over a folder are unknown, Delete MUST remain available and
  the server's per-folder refusal MUST be what reports it. The client MUST NOT guess a refusal.
- **FR-006**: The client MUST respect the maximum number of folders one submission may carry, reading
  that limit from the server rather than holding its own copy, and MUST explain a refusal caused by it
  in terms of the limit.

#### Confirming

- **FR-007**: Deleting MUST require an explicit confirmation before anything is submitted.
- **FR-008**: The confirmation MUST state that the selected folders **and everything inside them** are
  permanently deleted and cannot be recovered.
- **FR-009**: The confirmation MUST state how many folders it is about, using the number the client is
  submitting.
- **FR-010**: The confirmation MUST NOT claim that no workflow runs on the contents. That claim is not
  accurate: a content type declaring an action for destruction will run it. The copy states what is
  certain — the contents go with the folder, permanently — and asserts nothing about workflow.
- **FR-011**: Dismissing the confirmation MUST submit nothing and leave the selection untouched.

#### While a run is in flight

- **FR-012**: Once a submission is accepted, the folders the **server** accepted MUST be marked as
  in-flight in the listing, following the marking the listing already provides. A second marking
  mechanism MUST NOT be introduced.
- **FR-013**: A folder marked as in-flight MUST be inert: not selectable into another action, not
  openable, and not a valid drop target.
- **FR-014**: A folder marked as in-flight that is visible in the sidebar tree MUST be marked there
  too, and MUST be equally inert — not navigable, not expandable, not a drop target.
- **FR-014a**: In-flight marking MUST be perceivable without sight. Every marked row and every marked
  tree node MUST carry an accessible busy/disabled state, so an author reaching that folder with
  assistive technology is told it is being deleted. Reduced opacity and disabled pointer events are
  not sufficient on their own — they convey the state to one sense only, and the consequence of
  missing it is acting on a folder that is about to cease to exist.
- **FR-015**: Marking MUST apply to the folders submitted and their own tree nodes. It MUST NOT
  propagate to their descendants or to the contents shown inside them.
- **FR-016**: Progress MUST be visible while a run is in flight, and MUST be rendered as an
  **indeterminate** indicator rather than as a proportion. The server counts completed top-level
  folders and nothing finer, so a proportion would invent precision that does not exist and would sit
  unchanged for many minutes on exactly the selections where the author most wants reassurance.
- **FR-017**: A run MUST survive the dialog that started it closing, the author navigating within the
  portlet, and the author leaving the portlet entirely.
- **FR-018**: Delete MUST join the guard the existing actions already follow, which refuses a repeat of
  the **same operation on the same items** — and nothing wider. A delete running on one set of folders
  MUST NOT prevent an unrelated action, nor a delete of different folders, from starting. A run lasts
  minutes, so a guard that froze the portlet for its duration would undo the reason this operation was
  made asynchronous.
- **FR-018a**: The client MUST NOT attempt to guard overlapping subtrees itself. Whether a submission
  collides with a run already in flight — the same folder, an ancestor, or a descendant, started by
  **any** author — is refused at submission by the server, which is the only party that can see every
  run. The client's part is to report that refusal well (FR-040), not to predict it.

#### Surviving a reload

- **FR-019**: On loading Content Drive, the client MUST establish which folders are currently being
  deleted from server state, and mark them, without depending on anything the client remembered.
- **FR-019a**: Only runs that are genuinely **in progress** MUST mark folders. The listing the client
  reads returns every run in a non-terminal state, which includes runs that have already **failed** and
  runs the abandonment sweep has marked (C-011). Each run's own state MUST be checked, and a run that
  is not working MUST NOT mark anything. A folder whose delete failed is intact and usable, and marking
  it tells every author a lie that clears only when the framework gets around to it.
- **FR-020**: Marking restored this way MUST cover runs submitted by **any** author, not only the
  current one, so a folder someone else is deleting is not presented as usable.
- **FR-020a**: The client MUST establish the in-flight set **once** when Content Drive opens, and MUST
  then keep it current from the announcements the server makes as folders enter and leave a delete
  (C-012). It MUST NOT poll for the state of runs it already knows about. A folder another author
  starts deleting *after* this page opened is therefore marked here too, without a reload — the
  window this requirement previously accepted as a limitation is closed by the other half taking
  that change (backend D-016).
- **FR-020b**: Establishing the set on load MUST NOT be dropped in favour of announcements alone. A
  run that dies never announces that it ended, so a client listening only to announcements would
  mark a folder indefinitely with nothing to correct it. The two mechanisms answer different
  questions — the listing says what is in flight *now*, announcements say what changed *since* — and
  the client needs both. Re-establishing the set on load is what recovers from an announcement that
  never arrived.
- **FR-021**: Marking MUST clear when the run behind it ends, without the author reloading. For a run
  this author submitted, the completion the server pushes is what clears it. For a run discovered
  under FR-020a, the individual observation of that run is what clears it. A folder MUST NOT remain
  inert after the run acting on it has ended — a failed delete leaves the folder in place and usable
  again, and a mark that outlives its run is indistinguishable to the author from a folder nobody can
  touch.
- **FR-022**: Failure to establish in-flight state MUST degrade to an unmarked, fully working listing.
  It MUST NOT block the listing from rendering, and MUST NOT be reported to the author as an error
  about their own action.
- **FR-023**: Reading in-flight state MUST NOT add a perceptible delay to opening Content Drive.

#### Reporting the outcome

- **FR-024**: When a run ends while the author is present, the client MUST report it without the
  author refreshing or polling anything.
- **FR-025**: The report MUST use the **server's** counts of deleted, failed and skipped folders, never
  the size of the author's selection.
- **FR-026**: Every folder that was not deleted MUST be identifiable from the outcome, with its reason.
  Folders MUST NOT be summarised away into a count alone.
- **FR-027**: Where there are more failures than the report can show at once, it MUST name the first
  few and acknowledge the remainder as a count.
- **FR-027a**: That remainder MUST be reachable, not merely recorded somewhere. The report MUST link to
  the durable record holding the full per-folder list, so an author reading "and 7 more" can follow it
  to the seven. A count with no route to what it counts tells the author only that something is being
  withheld. How many are named before the overflow begins is a design choice for planning, not a
  requirement; that the overflow leads somewhere is the requirement.
- **FR-028**: Each machine-readable reason the server can return MUST map to copy written in the
  product's own words. At minimum: no rights on the folder, the folder no longer exists, the folder is
  protected, something inside is locked or in use, an ancestor already removed it, and a general
  fallback.
- **FR-029**: The server's diagnostic message MUST NOT be shown to the author. It is written for a log.
- **FR-030**: A reason the client does not recognise MUST still name the folder and report it as
  failed, using the general fallback. An unknown reason MUST NOT swallow the folder.
- **FR-031**: Skipped folders MUST read as not attempted rather than failed, and MUST distinguish the
  two reasons a folder is skipped: the run was cancelled before reaching it, or an ancestor in the same
  submission removed it first. These are different facts and one message cannot serve both.
- **FR-032**: A clean run, a partial run and a cancelled run MUST read differently, and a cancelled run
  MUST NOT read as a fault.
- **FR-033**: An outcome MUST be reported once, by whichever part of the interface is responsible for
  presenting it — not once per surface that knows about the run.
- **FR-034**: An author who was absent when a run ended MUST be able to find its outcome afterwards.
  The client MUST NOT build a background-jobs screen to satisfy this; the durable record the server
  keeps is what carries it.

#### After a run ends

- **FR-035**: When a run ends, the listing MUST stop showing the folders it deleted.
- **FR-036**: When a run ends, the sidebar tree MUST stop showing the folders it deleted. The two
  surfaces load independently and refreshing one does not refresh the other.
- **FR-037**: Folders that failed MUST remain in both surfaces.
- **FR-038**: An author browsing inside a folder that a run deleted MUST be moved to the site root.
- **FR-039**: A run's outcome MUST clear that run's marking in both surfaces, for every terminal state
  — succeeded, failed, or cancelled — including when the run was ended by something other than this
  client.

#### Refusals at submission

- **FR-040**: A submission refused because it overlaps a delete already running MUST be explained by
  naming the folder involved, and MUST NOT identify the author who started the other run.
- **FR-041**: A submission refused for any other reason MUST be distinguishable to the author — an
  empty selection, too many folders, and no entitlement MUST NOT all read the same.

### Key Entities

- **Folder selection**: the folders an author chose in the listing and asked to delete.
- **Run**: one submitted bulk delete, identified by the handle the server returns, with a state the
  client can observe and an outcome it can read.
- **In-flight set**: the folders any currently running delete is working on, wherever they are visible —
  established from server state rather than remembered locally.
- **Per-folder outcome**: one record per submitted folder — the folder, whether it was deleted, failed
  or skipped, and on failure a reason the client maps to its own copy.
- **Run report**: what the author is shown when a run ends — the server's counts, plus the folders that
  did not succeed and why.

---

## Contract Consumed *(mandatory — the boundary with the server half)*

Restated from `specs/37063-bulk-folder-delete-backend/spec.md` §Contract Consumed by the Client
(C-001 … C-012), from this side. Where this feature depends on something, it says so here rather than
assuming it.

- **One call, one handle** (C-001, C-002). The client sends the selected folder paths as one ordinary
  request and is answered immediately with a handle and a ready-made address for following the run. It
  never assembles that address itself, and none of bulk upload's multipart machinery applies — this
  sends no content.
- **A count to display, from the server** (C-003). The number of folders accepted into the run comes
  back with the handle and equals the total the outcome later reports. FR-012 and FR-025 both depend on
  it: the client marks and reports the server's set, not its own selection.
- **Distinguishable refusals** (C-004). Nothing submitted, over the maximum, not entitled, and
  overlapping an in-flight run are told apart. FR-040 and FR-041 rest on this; the overlap refusal is
  the one an ordinary author can actually provoke and carries its own copy.
- **A stable, enumerated set of failure reasons** (C-005). Each maps to client copy (FR-028). The
  server's message is diagnostic and is never displayed (FR-029). **Delete adds four reasons the shared
  set did not have**, now fixed on the other half too (backend FR-019): the folder no longer resolves,
  the folder is protected, something inside is in use, and an ancestor in the same submission removed
  it first — the last reported as *skipped* rather than failed, since it was never attempted. Each
  needs its copy written before either half is implemented, and a fifth must not appear during
  implementation without coming back through this boundary.
- **Progress, and an honest statement of what it counts** (C-006). Completed top-level folders, nothing
  finer. This is why FR-016 requires an indeterminate indicator: the contract says the number cannot
  carry a bar, so the client does not render one.
- **A way to cancel, with delete's guarantee** (C-007). The server provides it and requires that the
  wording say each folder is left either fully deleted or untouched — copy's wording must not be
  reused. **This client does not expose cancellation in this version** (§Decisions D-011); the
  requirement on the wording binds whoever surfaces it.
- **A readable terminal state and outcome** (C-008). Counts plus per-folder records, each failure
  carrying its reason. FR-026 and FR-027 depend on the records being present rather than summarised.
- **A pushed completion signal carrying the outcome, plus a durable record of the same** (C-009). This
  is what FR-024 and FR-034 rest on. The client does not poll for completion and does not build a jobs
  screen; it renders the push while the author is present, and the durable record answers them later.
  The record holds the **full** per-folder set, which is what makes FR-027's "and N more" honest.
- **The signal is emitted after the deletions are done** (C-010). FR-035 and FR-036 refresh on it and
  depend on that ordering, which the client cannot observe for itself.
- **The list of in-flight runs does not mean what its name says** (C-011). It returns every run in a
  non-terminal state, failed and abandoned ones included, so reading it naively marks folders whose
  delete already failed and leaves them marked until the framework moves them on. FR-019a is this
  half's side of it: filter on each run's state and keep only what is genuinely in progress. Recorded
  here because the symptom — "sometimes folders stay marked forever" — reads as a client defect and
  would be diagnosed as one.
- **A folder's entry into and exit from a delete is announced to every author who may see it**
  (C-012). Not only to the submitter, so this client learns about a run another author started after
  it loaded — which reading the in-flight list once cannot tell it. The announcement says a folder is
  busy and later that it is not; it says nothing about how the run is going, and outcomes stay with
  the submitter (C-009). The server filters by the recipient's own rights, so a folder this author
  may not see produces no announcement for them at all (backend FR-035b) — this half does no
  filtering of its own and must not be written as though it does.

  **They are ordered around the work**: a folder is announced as entering a delete before it is
  deleted, and as having left it after. The client may therefore refresh on the "left" announcement
  without racing the deletion, the same guarantee C-010 gives the completion signal.

  **It does not replace reading the list** (FR-020b). A run whose process dies never announces its
  exit, so announcements alone would mark a folder forever. The load-time read is what recovers from
  that.
- **In-flight runs, and the folders they are working on, are readable** (backend FR-005a / D-015).
  FR-019 and FR-020 rest entirely on this. It is the one place where this half's requirements changed
  the other half's: the server records folder paths with a run and leaves the in-flight listing
  readable by any back-end user **because** this client asked to restore marking after a reload and to
  mark another author's run. The consequence — folder paths, though not content, are visible to
  back-end users who have no rights on those sites — was accepted deliberately, and is this feature's
  to own rather than to discover later.

**Explicitly the server's business, not specified here**: how a run is executed, the transaction
boundary, what happens to a subtree the author cannot fully act on, retry and abandonment behaviour,
and the durable record's lifetime.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An author deletes a selection of N folders with one confirmation and one action, where
  today it takes N of each.
- **SC-002**: The author regains control of the interface within two seconds of confirming, regardless
  of how much content the folders hold.
- **SC-003**: Across reloads taken at arbitrary points during a run, a folder being deleted is never
  presented as an ordinary, usable folder — in the listing or in the sidebar tree.
- **SC-004**: A folder another author is deleting is marked for every author who can see it, in 100% of
  cases — whether the run was already in flight when they opened Content Drive or started afterwards.
  A folder no author may see is marked for nobody who may not see it: the filtering is the server's
  and is measured as such (backend FR-035b).
- **SC-005**: 100% of folders the server accepted are accounted for in the report — deleted, failed
  with a reason, or skipped with which kind of skip. Zero are silently dropped.
- **SC-006**: The counts the author is shown match the server's record in 100% of runs, including
  partial ones.
- **SC-007**: Every failure reason the server can emit renders as product copy; zero render as a raw
  code, and zero render the server's diagnostic text.
- **SC-008**: An author who was absent when a run ended can determine the full outcome, including which
  folders survived, without having watched it.
- **SC-009**: After any terminal state — success, failure or cancellation — zero folders remain marked
  as in-flight.
- **SC-010**: When the in-flight state cannot be read, the listing still opens and is fully usable, and
  the author is shown no error about an action they did not take.
- **SC-011**: The author is never told a delete succeeded when it did not, nor that it failed when it
  succeeded.
- **SC-012**: 100% of in-flight folders report their state to assistive technology, in both the
  listing and the sidebar tree — an author who cannot see the marking is never left believing an
  in-flight folder is an ordinary one.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive portlet, which is modern rather than legacy surface.
  Three shared pieces are touched and each has a second consumer that must not regress: the folder list
  view is shared with the AssetPicker work; the portlet's shared execution state is consumed by the
  Action Center and by bulk upload; and the sidebar tree is shared with the move and upload flows. The
  sidebar tree gains a per-node in-flight state it does not have today — that part is genuinely new,
  unlike the listing's row marking, which already exists and is only being fed a new kind of run.
- **Backward-compatibility expectations**: The shipped single-folder delete in the right-click menu is
  unchanged — it keeps its endpoint, its request and its synchronous behaviour, and remains the path
  the context menu uses. No new permission concept is introduced: the right to delete a folder in bulk
  is the right to delete it singly. The AssetPicker must not regress through the shared listing, and
  the existing in-flight reporting must keep behaving as it does for upload, move and the Action
  Center's runs.
- **Known related decisions**: Bulk file upload (#37166) established every pattern this feature reuses
  — a long operation submitted and answered immediately, its completion pushed rather than polled,
  execution state held by the portlet rather than the dialog, and individual rows marked while an
  operation acts on them. Bulk reindex established the pushed-completion precedent before it. This
  feature generalises those to folders and to the sidebar tree; it does not invent a third approach.
  Folder copy (#37062) and folder move (#37165) will consume the same outcome shape, so the reason-to-
  copy mapping this feature writes is written once for all three. `/speckit-plan` will consult
  `dotCMS/platform-adrs` formally.

---

## Out of Scope

- **The server-side implementation of #37063.** Delivered separately and specified in
  `specs/37063-bulk-folder-delete-backend/spec.md`. Nothing about how the server satisfies
  §Contract Consumed is specified here.
- **Cancelling a run from this client** (§Decisions D-011). The server provides cancellation and the
  contract fixes its wording; surfacing it is expected to arrive with the background task manager
  (#33331), which is also where bulk upload's stop lives. This feature must not depend on that epic
  existing, and does not build it.
- **A general-purpose background-jobs screen.** Owned by #33331. FR-034 is satisfied by the server's
  durable record, not by building one here.
- **Bounding a single folder's deletion.** The memory and transaction ceilings of deleting one very
  large folder are the server half's deferred work (#37565). The client's only obligation is not to
  present a proportion it cannot honestly compute (FR-016).
- **Deleting files in a mixed selection as part of the same run.** Files are deleted through the
  existing content path; reconciling two runs into one report is not attempted here (FR-003).
- **Restoring or undoing a deleted folder.** Deletion is permanent; nothing here adds recovery.
- **Marking descendants of an in-flight folder** (FR-015). The folders submitted and their tree nodes
  are marked; what is inside them is not walked.
- **The single-folder context menu delete.** Shipped on #35161 and unchanged.

---

## Planning Obligations

- **Test coverage** (Constitution V). The plan MUST name which layers this feature exercises and which
  it does not, with the reason for each omission — silence is not an acceptable answer to Principle V.
  At minimum it MUST say what is covered by component and store unit tests (Vitest/Spectator — the
  workspace migrated off Jest under #37444, so the sibling spec's wording is stale and MUST NOT be
  copied), what is covered end to end (Playwright, extending the existing
  `apps/dotcms-ui-e2e/src/tests/content-drive` suite), and how two things are exercised that no unit
  test of a single component reaches: the pushed completion signal, and the restoration of in-flight
  state on load (User Story 3). The per-story **Independent Test** steps are acceptance criteria for a
  reviewer, not a substitute for this.
- **The reason-to-copy mapping is a cross-half dependency with a deadline.** C-005 adds failure reasons
  that do not exist in the shared set today. Every added value needs client copy before **either** half
  is implemented, and copy and move will consume the same mapping. The plan MUST name where that
  mapping lives and MUST NOT leave it to be discovered during implementation.
- **How in-flight state is established on load, and what it costs.** FR-019 … FR-023 are the largest
  new piece of client work here and the only one with a performance budget attached. The plan MUST say
  when the set is read, how each discovered run is observed, how observation is torn down, how it is
  reconciled with runs this client started itself — which are already covered by the pushed completion
  and MUST NOT be observed twice — and what happens when an observation drops. It MUST NOT introduce a
  polling loop over runs it already knows about (FR-020a), and it MUST NOT let the listing's first
  render wait on any of it.
- **The cost of observing several foreign runs at once.** One observation per discovered run is fine at
  the scale this feature expects, and is not free at every scale: browsers cap concurrent connections
  per host. The plan MUST say what happens when the number of in-flight runs exceeds what can
  reasonably be observed at once, even if the answer is that the cap is far above any realistic count.
- **Where the sidebar tree's in-flight state lives.** The listing's marking already exists and is fed
  from the portlet's shared execution state; the tree has no equivalent. The plan MUST say whether the
  tree reads the same state or gains its own, and must keep one source of truth — two would drift, and
  the drift would show as a folder inert in one surface and usable in the other.
- **Which surface presents the outcome** (FR-033). Several already can. The plan MUST name one.
- **How a folder is matched between a run and a row.** The backend spec carries this as its own
  planning obligation, and the two plans MUST agree: the client has both a path and an identifier for
  every folder it lists, and which one travels in the submission, in the run's parameters and in the
  outcome need not be the same choice. The client-side consequence is what the plan must settle — what
  the in-flight set is keyed by, and how a marked folder is matched to a row in the listing and a node
  in the tree without matching the wrong one. Note that the listing's existing marking is keyed by
  inode, which is not what a run carries, so this is a real reconciliation and not a formality.

---

## Assumptions

- The server half honours §Contract Consumed. Where it does not yet, this feature's stories are blocked
  rather than worked around: no client-side polling loop, retry ladder or count estimation is
  introduced to compensate.
- Because completion is pushed, the client does not poll for it. It may read progress while the author
  is present, which is a different thing and is what FR-016 renders.
- A run this client did not submit can be observed individually until it ends, and observation costs one
  connection per run rather than a repeated request. If that turns out not to hold, FR-021's guarantee
  for foreign runs is what breaks, and the fallback is to report it rather than to reintroduce polling.
- Folder rights arrive with the folders the listing already loads. Where they are absent, FR-005 applies
  and the server's refusal reports it, rather than the client fetching rights separately to decide.
- The listing's existing row marking is reused as-is. If it turns out not to fit a run whose targets
  are folders rather than content items, that is a gap to report rather than a reason to build a second
  mechanism (FR-012).
- An outcome is reported once, by one responsible surface, rather than by each surface that knows about
  the run.
- All new author-facing copy is localisable through the existing message definitions, and no copy is
  hard-coded.
- Authors reaching this are back-office users on a maintained desktop browser; no additional
  compatibility target is introduced.
- The number of folders in one selection is bounded by the server's configured maximum, so the client
  is not designing for an unbounded report.

---

## Decisions

Recorded from the joint decision review of 2026-09-15/16 (questionnaire F1 … F15). Each is closed;
none is reopened by this document.

- **D-001 — Delete is offered as a multi-selection action alongside the others** *(F2 = A)*. Authors
  already look there for bulk actions, and the eligibility and counting behaviour it inherits is what
  FR-003 needs.
- **D-002 — A mixed selection still offers Delete, acting on the folders** *(F3 = A)*. Disabling on any
  stray file reads as broken; covering both folders and files in one run means two mechanisms and two
  result shapes behind one label, which is a larger feature.
- **D-003 — The listing's row marking already exists and is reused** *(F4, F5)*. The earlier claim that
  it did not exist was wrong: it shipped with #37166, and this feature follows it rather than adding a
  second mechanism. The sidebar tree's equivalent is the genuinely new part.
- **D-004 — Marking covers the submitted folders and their tree nodes only** *(F6 = A)*. Propagating to
  descendants means a path-prefix test on every row and node on every render, for a folder that is
  about to disappear.
- **D-005 — The job-following primitive from #37166 is reused, not rebuilt** *(F7 = A)*. It exists and
  is already shared with copy and move.
- **D-006 — Completion arrives pushed; progress is read while the author is present** *(F8, F10 = B,
  reconciled)*. F8 was answered as though the client would follow the run over a stream with polling
  behind it, which is neither what #37166 does nor what C-009 provides. The shipped pattern — a pushed
  completion signal correlated by run handle, plus a durable record — satisfies both F8's intent and
  F10's answer, and is what FR-024 and FR-034 require. Recorded explicitly because the two answers
  contradicted each other and the contradiction was resolved in favour of what already exists.
- **D-007 — In-flight state is restored from server state after a reload** *(F1 = A)*. Feasible because
  the in-flight runs are listable and carry the folders each run is working on. This is the decision
  that forced the server's answer on path visibility (backend D-015) — the two halves answered the same
  question hours apart without noticing, and the client's answer is the one that won.
- **D-008 — A folder another author is deleting is marked for everyone who can see it** *(F9 = A)*.
  Content Drive deliberately ignores other sessions' runs when *reporting outcomes*; that reasoning does
  not extend to letting an author walk into a folder being destroyed. Outcomes stay scoped to the
  submitter; marking does not.
- **D-009 — Rights carried with the folder gate the action; unknown rights do not** *(F13 = A)*. The
  client cannot know about descendants, so it gates on what it has and lets the server refuse the rest.
  **Refined by clarification (2026-09-16)**: the gate is whole-selection, not per-folder. A selection
  the author may only partly delete is submitted whole and the refusals are reported (FR-004a). The
  alternative — filtering the selection down to what the client believes is permitted — was considered
  and rejected: it shrinks a destructive action behind the author's back, and the client's reading of
  rights is the less reliable of the two anyway.
- **D-010 — Failures are named a few at a time, with the remainder counted** *(F14 = A)*. Honest at a
  glance, and the durable record holds the full set — which is why C-009 spells out that the record
  carries per-folder records and not just counts.
- **D-011 — Cancellation is not exposed in this version** *(F11 = B)*. The server provides it and fixes
  its wording; the client's surface for it is expected to be the background task manager (#33331),
  where bulk upload's stop also lives. Noted as a known asymmetry rather than an oversight: the server
  ships a capability this client does not yet surface.
- **D-012 — An author inside a deleted folder is moved to the site root** *(F12 = B)*. Always valid and
  always predictable. The nearest surviving ancestor would be less disorienting and was not chosen;
  reopening this is cheap if authors find the jump jarring.
- **D-013 — The confirmation is a plain one with explicit wording** *(F15 = A)*. Consistent with the
  shipped single-folder delete. The wording carries the weight, and FR-010 removes the inaccurate claim
  the original issue text proposed putting in it.
- **D-015 — In-flight state is established on load and kept current by announcement** *(clarification
  2026-09-16, revised after the backend took the broadcast)*. Governed by FR-020a and FR-020b. This
  started as an accepted limitation — a run another author began after this page opened would not be
  marked until the next load — and was raised to the other half as a question rather than assumed.
  That half took it (backend D-016, C-012), and the limitation is gone. **What survives the change is
  the load-time read**, and it is worth saying why it was not dropped as redundant: a run whose
  process dies never announces that it ended, so announcements alone would mark a folder forever. The
  two mechanisms cover different failures and the client keeps both.
- **D-014 — Progress renders as an indeterminate indicator** *(backend B18, C-006)*. Not a client
  preference: the server counts completed top-level folders and nothing finer, so a bar would sit at 0%
  for the whole of the folder that matters and read as a hung feature.

---

## Open Decisions

**None.** Every question this specification raised is closed, either here in §Decisions or inline at
the requirement it governs.

What remains genuinely undecided is deliberately *not* a specification question and is listed under
§Planning Obligations: where the sidebar tree's in-flight state lives, when and how in-flight state is
read on load, and which surface presents the outcome. All three are sized at the plan phase and none
changes what this document requires.
