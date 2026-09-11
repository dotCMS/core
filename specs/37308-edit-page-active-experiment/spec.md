# Feature Specification: Editing a Page While an Experiment Is Active

**Feature Branch**: `issue-37308-edit-page-active-experiment`

**Created**: 2026-09-10

**Status**: Draft

**Type**: Task (constraint removal + a new warning surface in the UVE shell)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37308 — Allow editing pages with an active experiment, with a non-blocking warning banner](https://github.com/dotCMS/core/issues/37308)

**Input**: User description: "Allow editing pages with an active experiment, with a non-blocking warning banner" — taken from issue #37308.

---

## Scope Note *(read this first)*

### This is a reversal, not a repair

Nothing here is broken. A page carrying a `RUNNING` or `SCHEDULED` experiment is deliberately
frozen for editing, and the freeze works exactly as written. This issue reverses the product
decision behind it: the cost of a mid-run edit is real but it is the editor's cost to weigh, and a
content team cannot promise to leave the home page untouched for three weeks. So the block goes and
a warning takes its place.

Because there is no defect, this spec is written in the epic's feature form (the shape #37176 and
#37478 use) rather than the reproduce-and-diagnose form: there is no wrong behavior to reproduce,
only a rule to remove and a surface to add.

The change has two halves, and they are independent enough to build and verify separately:

1. **Remove the experiment condition** from the three edit-capability computations, leaving lock and
   permission untouched.
2. **Add a persistent, non-dismissible warning banner** to the UVE shell, with different copy for
   `RUNNING` and `SCHEDULED`.

### Five things the code says that the issue does not

The issue's account of the code is accurate on the guard itself and on the absence of backend
enforcement. Five findings from reading the named files change what the work actually delivers, and
the requirements below are written against the code rather than against the issue's table.

**1. `computeCanEditPage()` is not wired to anything.** Its only references in the entire repository
are its own definition (`utils/index.ts:672`) and its own unit test (`utils.spec.ts`). No production
code imports it. Removing the experiment condition from it therefore changes no runtime behavior
whatsoever — it is a correctness fix to a dormant helper, not the thing that unblocks editing. The
live guard is entirely in `withEditor.ts`. This matters for verification: an acceptance criterion
that says "a user can enter edit mode on a `RUNNING` page" is satisfied by `withEditor.ts`, and a
unit test on `computeCanEditPage()` proves nothing about it. Both changes are still in scope — the
helper is exported and its current behavior is now wrong — but the spec keeps them honestly
separated. (Its sibling `computeIsPageLocked()` *is* live, via `withWorkflow.ts:118`; the two look
alike and are not alike.)

**2. A `SCHEDULED` experiment is invisible to the editor on an ordinary page load.**
`store.pageExperiment` is filled from `experimentId = pageParams?.experimentId ?? pageAsset?.runningExperimentId`
(`withPageApi.ts:249-250`). The page asset's `runningExperimentId` comes from
`getRunningExperimentPerPage()` → `getRunningExperiments()` → `cacheRunningExperiments()`, which
queries `statuses(set(Status.RUNNING))` (`ExperimentsAPIImpl.java:1392-1398`). `SCHEDULED` is not in
that set, so a scheduled experiment never reaches the editor by that route. It reaches it only when
`experimentId` arrives in the URL — the portlet and variant round-trip that #37005 shipped.

The consequence runs both ways. Today the `SCHEDULED` half of the block only bites on that arrival
path: navigate to a scheduled page normally and it is already editable. And after this change, the
`SCHEDULED` banner can only render on that same path. Making it appear on a plain page load requires
the page API to surface scheduled experiments, which is backend work and outside this epic's
frontend scope. This is recorded as **O3** for the coordinator rather than decided here.

**3. The guard is untested.** `withEditor.spec.ts` covers `editorHasAccessToEditMode` for
permission, for an unlocked page, for a lock held by the current user, for a lock held by another
user, and for both toggle-lock flag states — and not once for experiment status. Nothing asserts the
current blocking behavior, so removing it breaks no existing test. Every test the issue asks for is
new coverage of ground that was never covered.

**4. Editing a variant is already a solved, experiment-independent path — and that is what makes
FR-007 cheap.**

A variant of a **`DRAFT`** experiment is editable today. The portlet opens non-control variants in
`EDIT` (`dot-experiments-configure-variants.component.ts:193`, since `$isLocked` is
`status !== DRAFT`), the editor lets the user change content, and the save persists. That path is in
production and nothing along it reads experiment status.

The consequence is the useful one: **whatever makes a variant editable under `DRAFT` makes it
editable under `RUNNING` once the guard is gone.** There is no separate "variant save" problem for
this issue to solve. The only thing standing between `DRAFT` behavior and `RUNNING` behavior on the
variant path is `$isLocked` choosing `PREVIEW` for every non-draft status — which is **D8**, a
one-expression frontend change, not new machinery.

Two things that look like obstacles and are not:

- **`$showWorkflowsActions` is about the page, not the content.** `withView.ts:145-152` is
  `isEditMode && isDefaultVariant`, and the component it gates
  (`dot-uve-workflow-actions.component.ts:42`) fires on `pageAsset().page.inode` — the **page's** own
  workflow actions. Editing a contentlet inside the page goes through the contentlet dialog, a
  different path with its own save. So this hides page-level Save/Publish while viewing any variant,
  which is exactly what it already does under `DRAFT`. It is existing, deliberate,
  experiment-independent behavior, not something this change breaks or must fix.
- **The variant-coercion question is pre-existing and not this issue's to expose.** A save routed
  through the *new* Edit Content editor carries no variant: `EditContentDialogData` has no variant
  field, `libs/edit-content` has no variant handling, and its save uses
  `fireTo` (`workflow.feature.ts:240`) → `PUT /v1/workflow/actions/{actionId}/fire`, which declares
  no `variantName` — server-side that reaches the 7-argument `getContentlet` overload hard-coding
  `VariantAPI.DEFAULT_VARIANT.name()` (`WorkflowResource.java:5080`) and `resolveContentletByVariant`
  (`:5164-5188`). The **legacy** dialog is variant-correct by contrast: `dot-ema-dialog.store.ts`
  threads `variantName: pageVariantId()` into the JSP editor URL, a fix made deliberately in
  `fix(uve): Save variant content correctly (#30556)`, November 2024.

  Crucially, **this is reachable today under `DRAFT`** — it needs no running experiment and no change
  from this issue. If it is a live defect it is a live defect now, for any variant edit on a content
  type carrying `FEATURE_FLAG_CONTENT_EDITOR2_ENABLED`. The *fix* is therefore **out of scope here**
  (**O6**), but the *question* is not: the integration test required by **FR-040a** asserts which
  variant actually received the write, so this issue finds out rather than leaving a suspicion
  standing.

**5. The guard does not merely hide the option — it ejects you.**
`$modeGuardEffect` (`dot-editor-mode-selector.component.ts:96-108`) fires `onModeChange(PREVIEW)` —
a real `store.pageLoad({mode: PREVIEW})`, not a cosmetic toggle — whenever the editor is in `EDIT`
without edit access. Its sibling `$shouldShowDraftMode` (`:41-52`) removes the Draft option from the
menu. Both branches are gated on `FEATURE_FLAG_UVE_TOGGLE_LOCK`, and that flag ships as **`false`**
(`dotmarketing-config.properties:880`), so the ejecting branch is the default path, not the
alternative. Both behaviors are pinned by tests (`dot-editor-mode-selector.component.spec.ts:111-131`
and `:207-222`). This matters for verification: on the default configuration the observable symptom
of the current block is not a greyed-out control, it is the editor bouncing out of edit mode.

### What removing the guard actually switches back on

`editorHasAccessToEditMode` and `hasPermissionToEditLayout` are not leaf values; they feed the
editor's whole editable surface. Naming the dependents makes the acceptance criteria checkable and
the regression risk legible:

| Removing the condition from | Re-enables |
|---|---|
| `editorHasAccessToEditMode` → `editorCanEditContent` | the editable overlay (`edit-ema-editor.component.html:20`), contentlet controls (`$showContentletControls`), the drop/click handlers (`edit-ema-editor.component.ts:323,329,355`), the toolbar's `$canEditPage`, and **the workflow actions** (`dot-uve-workflow-actions.component.ts:45`) |
| `editorHasAccessToEditMode` directly | the Edit/Preview/Live mode selector (`dot-editor-mode-selector.component.ts:51,98`) |
| `hasPermissionToEditLayout` → `editorCanEditLayout` | the Layout screen (`edit-ema-layout.component.ts:91`) and the nav bar's Layout item (`dot-ema-shell.component.ts:193`) |

The workflow-actions entry is the one worth pausing on: it is what makes the issue's "saving an edit
made during an active experiment succeeds and persists" reachable at all. With the guard in place
the Save and Publish controls are not merely refused — they are absent.

### The banner is a warning, not a gate

The pre-UVE product asked for confirmation. This one does not. There is no dialog, no click-through,
and no dismissal state, precisely so that the warning cannot be dismissed-and-forgotten while it is
still true. It borrows the page-lock banner's layout (`dot-ema-shell.component.html:1-34`) and
deliberately does not borrow its close button.

The copy in this spec is the issue's proposal and is **not** final — product and design have not
seen it. What is firm is the split: `RUNNING` and `SCHEDULED` describe genuinely different risks and
must not share a string. What is an assumption is every actual word (**A1**).

---

## Clarifications

### Session 2026-09-11

- Q: When a page is both locked by another user and carries an active experiment, what shows, and in
  what order? → A: Both, lock banner first and experiment banner below it. The order is fixed by this
  spec (D9), not inherited from template ordering, which is what FR-024 demanded and the spec had not
  supplied.
- Q: Does the banner show to a user who can read the page but not edit it? → A: Yes. No permission
  gate (D10), matching the two existing precedents in the shell — neither `$showLockBanner` nor the
  toolbar's running-experiment tag consults `canEdit`.
- Q: `ExperimentDataGen.nextPersistedAndStart()` already exists, so an integration test can put an
  experiment into `RUNNING` today. Is that test written in this issue? → A: Yes (D11). One test
  anchors both the data-integrity guarantee (FR-040 / SC-011) and settles the open variant-coercion
  question (O6), replacing static analysis with evidence.

Resolved by reading the code rather than by asking:

- Banner stability across reloads: `pageLoad` clears `editorSelected` and `editorContentArea` but not
  `pageExperiment` (`withPageApi.ts:191-202`), so the banner does not flicker on a mode change or
  reload. The one real case — briefly carrying the previous page's experiment across a page-to-page
  navigation — is recorded as an edge case.
- Accessibility semantics: no `p-message` anywhere in the repository declares `role` or `aria`
  attributes, including the lock banner. The new banner follows that precedent rather than inventing
  a pattern the codebase does not use (A8).

## Verified Code Baseline

Every claim the issue makes about the code, checked against the tree at `44a4026d37`. This table is
the spec's factual floor; the requirements below depend on it.

| Issue's claim | Verdict | What the code says |
|---|---|---|
| Guard in `computeCanEditPage()` blocks on `RUNNING` + `SCHEDULED` | **Confirmed** | `utils/index.ts:680-686`, exactly as quoted |
| `computeCanEditPage()` "blocks entering edit mode" | **Wrong** | No production caller anywhere in the repo; only its own spec references it |
| Guard in `editorHasAccessToEditMode` | **Confirmed** | `withEditor.ts:96-112`; condition at 98-101 |
| Guard in `hasPermissionToEditLayout` | **Confirmed** | `withEditor.ts:114-134`; condition at 123-126, applied as `!isExperimentRunning` at 131 |
| No backend enforcement of the edit block | **Confirmed, exhaustively** | Nine backend layers audited independently, each then challenged by two adversarial reviewers briefed to find a block: the contentlet checkin path (`ESContentletAPIImpl.checkin` → factory write → versioning → unique-field validation), the REST endpoints UVE actually calls, the permission layer, the workflow engine and its actionlets, the variant domain, everything the experiments domain writes outward on start (quartz job, Rule, cache, system table), every interception point (servlet filters, `WebInterceptor`s, CDI interceptors, the `LocalSystemEventsAPI` subscriber registry, and the `ContentletAPI` pre/post hooks — the designed extension point for exactly this kind of rule), push publishing and its bundlers, and integration-test feasibility. No layer found a block; no challenger overturned a layer. The structural reason: the save path holds no reference to the experiment domain, so there is nowhere for such a check to live — the experiment is consulted when the page is *rendered*, never when it is *saved*. The only refusals in `ExperimentsAPIImpl` are overlap/start guards (`getRunningExperimentsOnPage`, `CANNOT_START_AN_ALREADY_STARTED_EXPERIMENT_MESSAGE`), and `start()` never locks the page. Still static analysis: see **FR-040** and **O6** for the runtime proof that remains unrun |
| Only `ContentletDeletedEvent` is subscribed | **Confirmed** | `ExperimentsAPIImpl.java:142-146` subscribes exactly two events: `ContentletDeletedEvent` → `checkAndDeleteExperiment`, and `SystemTableUpdatedKeyEvent`. No save or publish subscriber exists |
| Results are queried live from CubeJS | **Confirmed** | `getResults()` → `ExperimentResultsQueryFactory`, which reads `experiment.runningIds().getCurrent()` at query time |
| Lock banner is `p-message severity="warn"` at the top of the shell | **Confirmed** | `dot-ema-shell.component.html:1-34`, with a close button and a `$showBanner` signal (`dot-ema-shell.component.ts:160`) |
| `dot-ema-running-experiment` is gated on `RUNNING` only | **Confirmed** | `dot-uve-toolbar.component.ts:130-135`; links to `/edit-page/experiments/{pageId}/{id}/reports` with `queryParamsHandling="preserve"` |
| Message keys follow `uve.shell.page.locked.*` | **Confirmed** | `Language.properties:7047-7051` |
| Legacy copy at `Language.properties:6034` | **Off by 8** | `experiment.running.edit.confirmation` is at **6042**; the cited range 6033-6035 is **6041-6043**. A second dead string with the same inaccurate "may invalidate any results already collected" phrasing sits at 6043 (`experiment.running.edit.lock.confirmation.note`). Neither is referenced by any frontend code |
| `docs/backend/EXPERIMENTS_CONSTRAINTS.md` documents the lifecycle | **Does not exist** | Not in the working tree, not in `HEAD`, not on any remote branch, and no commit has ever touched that path |
| Data integrity holds `runningIds` and `lookBackWindow` steady | **Confirmed, but not frontend-checkable** | Both live only on the backend `AbstractExperiment` / `RunningIds`; the frontend `DotExperiment` model carries neither |
| Editing a created variant needs new work | **No — it already works under `DRAFT`** | The portlet opens non-control variants in `EDIT` whenever the experiment is a draft (`dot-experiments-configure-variants.component.ts:193`), and that save path reads no experiment status. FR-007 therefore needs only **D8** — the portlet's `PREVIEW`-for-every-non-draft choice — and no new save machinery. See Scope Note finding 4 |
| The guard hides the edit affordance | **Understated** | It ejects: `$modeGuardEffect` (`dot-editor-mode-selector.component.ts:96-108`) issues a real `pageLoad({mode: PREVIEW})`. The ejecting branch is the default, since `FEATURE_FLAG_UVE_TOGGLE_LOCK=false` (`dotmarketing-config.properties:880`) |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit the page the experiment is running on (Priority: P1)

A content editor needs to fix a typo in the hero copy of the home page. An A/B experiment has been
running on that page for nine days and has four more to go. Today the editor is locked out and their
only options are to wait or to kill the experiment. They should be able to make the fix, told plainly
what it costs.

**Why this priority**: this is the whole issue. Without it, experiments remain unusable on the pages
worth experimenting on.

**Independent Test**: on a page with a `RUNNING` experiment and a user holding edit permission on an
unlocked page, enter edit mode, change a contentlet, and save. The change persists and the experiment
is still running.

**Acceptance Scenarios**:

1. **Given** a page with a `RUNNING` experiment and a user with edit permission, **When** they open
   the page in the editor, **Then** edit mode is available and the mode selector offers it.
2. **Given** they are in edit mode, **When** they edit a contentlet in the **original** variant,
   **Then** the edit is accepted and the workflow actions are available to save it.
3. **Given** they are in edit mode on a **created variant** of that experiment, **When** they edit a
   contentlet, **Then** the edit is accepted and persists on that variant — behaving exactly as the
   same variant does under a `DRAFT` experiment today. *Reaching edit mode on that variant from the
   portlet depends on **D8**; the save itself needs nothing new.*
4. **Given** they save, **When** the save completes, **Then** it succeeds with no error from the page
   or contentlet APIs, and the experiment's status, schedule and identity are unchanged.
5. **Given** a page with a `RUNNING` experiment on a standard (drawed) template, **When** they open
   the Layout screen, **Then** the nav item is enabled and the layout is editable.

---

### User Story 2 - The warning is in view for as long as it is true (Priority: P1)

The same editor must not be able to make that edit without knowing what it does to the run. Not a
dialog they click past, not a toast that fades — a banner that sits above the page and stays there.

**Why this priority**: unblocking without warning is the irresponsible half of the change. US1 and
US2 ship together or not at all.

**Independent Test**: on a page with a `RUNNING` experiment, confirm the banner is present on
arrival, has no close control, and is still present after editing and saving.

**Acceptance Scenarios**:

1. **Given** a page whose experiment is `RUNNING`, **When** the editor opens it, **Then** a
   `severity="warn"` message is shown at the top of the shell explaining that the results will mix
   data from before and after the change.
2. **Given** the banner is shown, **When** the editor looks for a way to dismiss it, **Then** there
   is none — no close button, no dismissal state.
3. **Given** the banner is shown, **When** the editor edits and saves, **Then** the banner is still
   shown; nothing about it was consumed by the interaction.
4. **Given** the banner is shown, **When** the editor uses any editing control, **Then** nothing is
   blocked, gated, delayed or confirmed by the banner's presence.
5. **Given** the banner is shown, **When** the editor follows its link, **Then** they reach that
   experiment's reports.

---

### User Story 3 - A scheduled experiment does not freeze the page, and says something different (Priority: P2)

An editor schedules an experiment to start on Monday. It is Thursday. Nothing is being measured and
nothing can be polluted, yet today the page is already frozen. It should be editable, and the warning
it carries should say what is actually true: edits made now become part of the baseline.

**Why this priority**: separately valuable and separately testable, but the `RUNNING` case is what
customers are blocked on. Its reach is also narrower than the issue implies — see **O3**.

**Independent Test**: arrive at a page carrying a `SCHEDULED` experiment via a URL bearing its
`experimentId`, confirm the page is editable and the banner shows the scheduled copy, not the
running copy.

**Acceptance Scenarios**:

1. **Given** a page whose experiment is `SCHEDULED` and a user with edit permission, **When** they
   open the page in the editor, **Then** edit mode is available.
2. **Given** that page, **When** the banner renders, **Then** its text differs from the `RUNNING`
   text and states that edits made before the start become part of what the experiment measures.
3. **Given** that page, **When** the banner renders, **Then** it does not claim that editing
   invalidates or discards results already collected.
4. **Given** a `SCHEDULED` experiment on a standard template, **When** the editor opens Layout,
   **Then** it is editable.

---

### User Story 4 - Lock and permission still decide who edits (Priority: P1)

The experiment condition sat next to two rules that must survive it untouched: a page locked by
someone else is not editable, and a user without edit permission cannot edit. Removing one condition
from a compound expression is exactly where those get lost.

**Why this priority**: a permission or lock regression here is worse than the constraint this issue
removes.

**Independent Test**: on a page with a `RUNNING` experiment, confirm a page locked by another user is
still not editable and a user without `canEdit` still cannot edit.

**Acceptance Scenarios**:

1. **Given** a page with a `RUNNING` experiment that is locked by another user, **When** a user with
   edit permission opens it, **Then** edit mode is unavailable — exactly as on a page with no
   experiment.
2. **Given** a page with a `RUNNING` experiment, **When** a user without `canEdit` opens it, **Then**
   edit mode is unavailable.
3. **Given** a page with a `RUNNING` experiment on an **advanced** (non-drawed) template, **When** a
   user with full edit permission opens it, **Then** layout editing is still unavailable, for the
   template reason and not the experiment reason.
4. **Given** a page locked by another user with a `RUNNING` experiment, **When** the shell renders,
   **Then** the lock banner behaves exactly as it does today.

---

### User Story 5 - The numbers collected before the edit are still there afterwards (Priority: P1)

The justification for allowing the edit is that it pollutes future data rather than destroying past
data. That has to be true of the system, not just of the banner copy.

**Why this priority**: it is the premise the whole change rests on. It is also, per the issue, a
**regression guard rather than new work** — no code path purges collected data on a save today, and
the point is to keep it that way.

**Independent Test**: run an experiment, record what the results screen shows, edit a variant, and
re-read the results. The pre-edit measurements are unchanged.

**Acceptance Scenarios**:

1. **Given** an experiment with collected data, **When** a variant is edited, **Then** no experiment
   data is deleted, reset, purged or archived.
2. **Given** an experiment with collected data, **When** a variant is edited, **Then** the results
   screen still shows the measurements taken before the edit.
3. **Given** an experiment with collected data, **When** `GET /v1/experiments/{id}/results` is called
   after an edit, **Then** the measurements for the period preceding the edit are returned unchanged.
4. **Given** an experiment, **When** a variant is edited, **Then** the experiment keeps its `id`, its
   variants, its `runningIds` and its `lookBackWindow`, and its `scheduling` is unaltered — no new
   run is started and no running id is rotated.
5. **Given** this change, **When** the diff is reviewed, **Then** no subscriber to contentlet save or
   publish events has been added, and the `ContentletDeletedEvent` subscription is still scoped to
   page deletion.

---

### User Story 6 - The banner leaves when the experiment does (Priority: P2)

The banner's contract is that it is present exactly while it is true. That obliges it to disappear on
its own, including while the editor is sitting on the page.

**Why this priority**: correctness of the surface rather than access to it; a stale warning is
misleading but not blocking.

**Independent Test**: with the banner shown, end the experiment and reload; the banner is gone and the
page is still editable.

**Acceptance Scenarios**:

1. **Given** a page with no experiment, **When** the editor opens it, **Then** there is no banner and
   nothing about the page's behavior differs from today.
2. **Given** a page whose experiment is `DRAFT`, `ENDED` or `ARCHIVED`, **When** the editor opens it,
   **Then** there is no banner.
3. **Given** the banner is shown for a `RUNNING` experiment, **When** the experiment ends, is
   cancelled or is archived and the editor's view of the experiment refreshes, **Then** the banner is
   gone and the page remains editable.

---

### Edge Cases

- **A `SCHEDULED` experiment flips to `RUNNING` while the editor is on the page.** The page stays
  editable throughout — the two statuses now behave identically for access. The banner must follow the
  status it is given: whichever copy corresponds to the status the editor last observed. Neither
  status change may make the page momentarily non-editable.
- **An experiment ends while the editor is on the page.** The page stays editable and the banner
  goes. Nothing in the transition may take editability away, since after the end there is no reason
  at all to withhold it.
- **The store's experiment is `undefined`.** Either the page has no experiment, or the lookup failed —
  `DotExperimentsService.getById` swallows errors into `of(undefined)`. Both resolve to "no banner",
  which is the right answer for the first and an acceptable silent one for the second: a failed
  experiment lookup must not make a page non-editable, and it must not invent a warning.
- **The page has a `RUNNING` experiment and is also locked by another user.** Two banners are
  eligible. The lock banner reports a rule that is still enforced; the experiment banner reports a
  cost that no longer blocks anything. Their coexistence must be deliberate and stated (**FR-024**,
  **O5**).
- **The editor is in `PREVIEW` or `LIVE` mode on a page with a `RUNNING` experiment.** Nothing is
  being edited, so the warning is arguably noise; but the lock banner is not mode-gated and the
  running-experiment tag is not either. Assumption **A2**, question **O4**.
- **An advanced (non-drawed) template with a `RUNNING` experiment.** Layout stays unavailable. The
  reason must remain the template, and the "advanced-template" tooltip must not be replaced by an
  experiment reason that no longer exists.
- **The editor navigates from one page to another.** `pageLoad` does not clear `pageExperiment`
  (`withPageApi.ts:191-202`), so between the navigation and the new fetch resolving, the banner still
  reflects the page being left. It is self-correcting and matches what the toolbar tag already does,
  but a test asserting banner content immediately after a navigation must account for it.
- **A `RUNNING` experiment whose `scheduling` is `null`.** `DotExperiment.scheduling` is nullable and
  the toolbar tag already reads `scheduling.endDate` unguarded. The banner must not acquire the same
  exposure — nothing in it may depend on a date being present.

---

## Requirements *(mandatory)*

### Functional Requirements

#### A. Removing the block

- **FR-001**: A user with edit permission MUST be able to enter edit mode on a page whose experiment
  is `RUNNING`.
- **FR-002**: A user with edit permission MUST be able to enter edit mode on a page whose experiment
  is `SCHEDULED`.
- **FR-003**: `editorHasAccessToEditMode` MUST NOT consult experiment status. Its result MUST depend
  only on edit permission and lock ownership.
- **FR-004**: `hasPermissionToEditLayout` MUST NOT consult experiment status. Its result MUST depend
  only on edit permission, whether the template is drawed, and lock state.
- **FR-005**: `computeCanEditPage()` MUST NOT consult experiment status. Its result MUST depend only
  on edit permission and lock state. This FR is a correctness fix to an **unused exported helper** and
  MUST NOT be presented as the mechanism that satisfies FR-001 or FR-002 — see the Scope Note.
- **FR-006**: Content in the **original** variant MUST be editable while the page's experiment is
  `RUNNING` or `SCHEDULED`.
- **FR-007**: Content in a **created variant** MUST be editable while the experiment is `RUNNING` or
  `SCHEDULED`, on the same terms it is editable under `DRAFT` today. This requires the portlet to stop
  choosing `PREVIEW` for non-draft statuses (**D8**); it requires no new save machinery, because the
  variant save path is already in production and reads no experiment status (Scope Note finding 4).
- **FR-007a**: An edit made while viewing a non-DEFAULT variant MUST be written to **that** variant,
  exactly as it is under `DRAFT`. This change MUST NOT introduce any new way for a variant edit to
  land on `DEFAULT`, and MUST NOT alter the variant routing of either the legacy dialog or the new
  editor. It is a hold-the-line requirement, not a fix: whether the new editor already mishandles
  this is pre-existing and belongs to **O6**.
- **FR-008**: Saving an edit made while an experiment is active MUST succeed and persist, with no
  error from the page or contentlet APIs, on the same terms as under `DRAFT`. Page-level workflow
  actions remain governed by `$showWorkflowsActions` (`withView.ts:145-152`), which hides them on any
  non-DEFAULT variant regardless of experiment status; this change neither alters nor needs to alter
  that.
- **FR-009**: No new gate, confirmation, dialog or interstitial MAY be introduced in place of the
  removed condition. The removal MUST be a removal.

#### B. What must not change

- **FR-010**: Page lock behavior MUST be untouched. A page locked by another user MUST remain
  non-editable, on a page with an active experiment exactly as on a page without one.
- **FR-011**: Edit permission MUST remain enforced. A user without `canEdit` MUST NOT gain edit
  access from this change.
- **FR-012**: Layout editing MUST remain unavailable on advanced (non-drawed) templates, and the
  reason surfaced MUST remain the template — not an experiment reason that no longer applies.
- **FR-013**: Editing MUST NOT end, cancel, pause, restart or otherwise alter the experiment's status
  or schedule.
- **FR-014**: The experiment overlap rules MUST be untouched — two experiments still MUST NOT run on
  the same page.
- **FR-015**: The existing `dot-ema-running-experiment` toolbar tag MUST keep working and MUST keep
  reaching that experiment's reports. Whether it keeps its current `routerLink` depends on **O1**.
- **FR-016**: No behavior on a page with no experiment MAY change in any way.

#### C. The banner

- **FR-017**: While the page's experiment is `RUNNING` or `SCHEDULED`, the UVE shell MUST show a
  persistent, non-blocking warning message.
- **FR-018**: The banner MUST use `p-message severity="warn"`, matching the page-lock banner it is
  modelled on, and MUST be positioned at the top of the shell as that banner is.
- **FR-019**: The banner MUST have **no** close control and MUST hold **no** dismissal state. It MUST
  NOT be suppressible for a session, for a page, or for a user.
- **FR-020**: The banner MUST remain visible for the entire editing session for as long as the status
  that justifies it holds.
- **FR-021**: The banner MUST NOT block, gate, disable or delay any editing action, and MUST NOT
  introduce a confirmation step.
- **FR-022**: The banner MUST offer a link to the experiment's reports, reachable by keyboard and
  labelled as a link to the experiment.
- **FR-023**: The banner MUST NOT be shown when the page has no experiment, when the experiment
  lookup returned nothing, or when the experiment's status is `DRAFT`, `ENDED` or `ARCHIVED`. It MUST
  disappear once the experiment ends, is cancelled or is archived. These are the **only** conditions
  that suppress it.
- **FR-023a**: The banner MUST NOT be gated on edit permission. A user who can read the page but not
  edit it MUST still see it, matching `$showLockBanner` and the toolbar's running-experiment tag,
  neither of which consults `canEdit` (D10).
- **FR-024**: When the lock banner is also eligible, **both MUST be shown, with the lock banner
  first and the experiment banner directly below it** (D9). The order MUST be a property of the
  implementation that a test can assert, not an incidental consequence of template ordering.
- **FR-025**: The banner MUST NOT depend on any nullable field of the experiment being populated. In
  particular it MUST NOT read `scheduling.endDate` unguarded.
- **FR-026**: The banner MUST be added without altering the lock banner's own condition, copy or
  dismissal behavior.

#### D. The copy

- **FR-027**: `RUNNING` and `SCHEDULED` MUST use **different** strings. A single shared string MUST
  NOT be used for both.
- **FR-028**: The `RUNNING` string MUST convey that the experiment's results will mix data from before
  and after the change.
- **FR-029**: The `SCHEDULED` string MUST convey that edits made before the start become part of what
  the experiment measures. It MUST be informational, not cautionary — a scheduled experiment has
  nothing to pollute.
- **FR-030**: No string MAY claim that editing invalidates, discards, destroys or resets results
  already collected. The word "invalidate" MUST NOT appear.
- **FR-031**: All banner copy MUST resolve through the `| dm` message pipe. No hardcoded user-facing
  string MAY appear in the template.
- **FR-032**: New keys MUST be added under the `uve.shell.experiment.*` namespace, following the
  `uve.shell.page.locked.*` convention.
- **FR-033**: The legacy strings `experiment.running.edit.confirmation` and
  `experiment.running.edit.lock.confirmation.note` (`Language.properties:6042-6043`) MUST NOT be
  reused, and their phrasing MUST NOT be carried forward. Whether to delete them is out of scope.

#### E. Data integrity *(regression guard)*

These requirements add no code. They exist so that the guarantee the banner copy makes is verified
rather than assumed, and so that a later change cannot quietly break it.

- **FR-034**: Editing a variant — original or created — MUST NOT delete, reset, purge or archive any
  experiment data already collected.
- **FR-035**: Data collected before an edit MUST remain accessible on the experiment's results screen
  after the edit.
- **FR-036**: `GET /v1/experiments/{id}/results` MUST return the pre-edit measurements unchanged for
  the period preceding the edit.
- **FR-037**: An edit MUST NOT change the experiment's `id`, its variants, its `runningIds` or its
  `lookBackWindow`, and MUST NOT start a new run or rotate a running id.
- **FR-038**: An edit MUST NOT alter the experiment's `scheduling`.
- **FR-039**: No subscriber to contentlet **save** or **publish** events that touches experiment data
  MAY be added. The `ContentletDeletedEvent` subscription in `ExperimentsAPIImpl` MUST stay scoped to
  page deletion and MUST NOT be widened.
- **FR-040**: FR-034 through FR-038 MUST be verified at the API or database level, since `runningIds`
  and `lookBackWindow` are not present on the frontend `DotExperiment` model. A frontend unit test
  MUST NOT be offered as evidence for them.
- **FR-040a**: That verification MUST be an **integration test**, not a manual QA step (D11). The
  pieces already exist: `ExperimentDataGen.nextPersistedAndStart()` puts an experiment into `RUNNING`
  in one call. The test MUST start an experiment, save a contentlet on a non-DEFAULT variant, and
  assert both that the collected data survives (FR-034 through FR-038) **and which variant actually
  received the write** — the second assertion is what settles **O6** with evidence instead of static
  analysis.
- **FR-040b**: That test MUST be registered in a `MainSuite*` / `Junit5Suite*` `@SuiteClasses` list.
  An unregistered integration test compiles, leaves CI green, and is **never executed** — which would
  turn this issue's central guarantee into a test that only ever passes by not running.

#### F. Coexistence with in-flight work

- **FR-041**: This change MUST NOT contradict #37478 (the experiments panel behind
  `FEATURE_FLAG_EXPERIMENTS_PORTLET`). Where the banner and that work overlap, the overlap MUST be
  resolved by the coordinator before implementation — see **O1** and **O2**.
- **FR-042**: The banner MUST work identically with `FEATURE_FLAG_EXPERIMENTS_PORTLET` on and off,
  except for whatever its link does — which is exactly what **O1** decides.

### Key Entities

| Entity | Where it lives | Role here |
|---|---|---|
| `DotExperiment` | `dotcms-models`: `id`, `pageId`, `status`, `scheduling` (nullable), `trafficProportion` | The banner's whole input. Note it carries neither `runningIds` nor `lookBackWindow` |
| `DotExperimentStatus` | `RUNNING`, `SCHEDULED`, `DRAFT`, `ENDED`, `ARCHIVED` | The first two show a banner and no longer block; the last three do neither |
| `store.pageExperiment` | `UVEState`, filled in `withPageApi.ts:249-282` | The single source for both the removed condition and the new banner. `undefined` for a page with no experiment and for a failed lookup alike |
| `uve.shell.experiment.*` | `Language.properties` | Three new keys: the running warning, the scheduled warning, the link label |

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a page with a `RUNNING` experiment, a user with edit permission on an unlocked page
  can enter edit mode on the **original** (DEFAULT) variant, change content and save it, in one
  uninterrupted pass with no confirmation step — and is not ejected back to `PREVIEW` by
  `$modeGuardEffect` on the default configuration (`FEATURE_FLAG_UVE_TOGGLE_LOCK=false`).
- **SC-001a**: On a **created variant** of that experiment, the same pass succeeds and the saved
  content is read back on that variant — with the result indistinguishable from performing the same
  edit on a variant of a `DRAFT` experiment. That equivalence is the measurement: if the two differ,
  something in this change reached the save path, which it must not.
- **SC-002**: On a page with a `SCHEDULED` experiment reached with its `experimentId`, the same pass
  succeeds.
- **SC-003**: Layout editing is reachable on a drawed template with a `RUNNING` or `SCHEDULED`
  experiment, and still unreachable on an advanced template.
- **SC-004**: `editorHasAccessToEditMode`, `hasPermissionToEditLayout` and `computeCanEditPage()`
  contain no reference to `DotExperimentStatus`; a repository search for that symbol in the three
  functions returns nothing.
- **SC-005**: A page locked by another user is non-editable under a `RUNNING` experiment, and the
  test that asserts it is new — no such test exists today.
- **SC-006**: The banner is present for `RUNNING` and for `SCHEDULED`, absent for `DRAFT`, `ENDED`,
  `ARCHIVED`, no experiment and a failed lookup — six cases, each asserted by `byTestId`.
- **SC-007**: The `RUNNING` and `SCHEDULED` banners render different text, asserted against the two
  distinct message keys.
- **SC-008**: The banner's rendered markup contains no close control, and the shell's existing
  `$showBanner` dismissal signal is not consulted by it.
- **SC-009**: The string "invalidate" appears in no new message value.
- **SC-010**: Every user-facing string the banner renders comes from `Language.properties` through
  `| dm`; the template contains no literal copy.
- **SC-011**: After an edit to a variant of a running experiment, the results returned for the
  pre-edit period are byte-identical to those recorded before the edit, and the experiment's `id`,
  variants, `runningIds`, `lookBackWindow` and `scheduling` are unchanged.
- **SC-012**: The diff adds no contentlet save or publish subscriber.
- **SC-013**: A page with no experiment renders and behaves identically before and after the change,
  with no banner and no access difference.
- **SC-014**: The existing `withEditor.spec.ts` lock and permission assertions still pass unmodified.
- **SC-015**: On a page that is both locked by another user and carrying a `RUNNING` experiment, both
  banners render and the lock banner precedes the experiment banner in the DOM — asserted by a test,
  not by inspection.
- **SC-016**: A user with `canRead` but not `canEdit` sees the banner; no suppression condition other
  than the five in FR-023 exists.
- **SC-017**: The integration test required by FR-040a exists, is registered in a `MainSuite*` /
  `Junit5Suite*` `@SuiteClasses` list, and passes — and its variant assertion reports which variant
  received the write, closing **O6** with evidence.

---

## Fix Scope & Non-Goals

**In scope**:

- Removing the experiment condition from `editorHasAccessToEditMode`, `hasPermissionToEditLayout` and
  `computeCanEditPage()`.
- A persistent warning banner in `dot-ema-shell`, with `RUNNING` and `SCHEDULED` copy.
- Three new `uve.shell.experiment.*` keys in `Language.properties`.
- **Releasing the portlet's variant mode for `RUNNING` and `SCHEDULED`** (**D8**) — one expression at
  `dot-experiments-configure-variants.component.ts:193`, without touching `$isLocked`. This is what
  makes FR-007 reachable, and it extends the change into `libs/portlets/dot-experiments`.
- **Amending #37005's FR-009 and its `describe.each`** (`dot-experiments-configure-variants.component.spec.ts:1253-1273`)
  so `RUNNING` and `SCHEDULED` expect `EDIT` while `ENDED` and `ARCHIVED` keep `PREVIEW`.
- Unit and component tests for the above, plus the lock and permission regression tests the guard
  never had.
- **One integration test** (FR-040a/FR-040b, D11) proving the data-integrity guarantee against a real
  `RUNNING` experiment, registered in a `MainSuite*` / `Junit5Suite*`. Java, but a test only — no
  production Java.

**Explicitly out of scope / non-goals**:

- **Restoring the confirm dialog** in any form. The banner replaces it; the legacy strings stay
  unreferenced.
- **Deleting the legacy strings** at `Language.properties:6042-6043`. They are dead but removing them
  is unrelated cleanup.
- **Recording or annotating edits made mid-experiment** so the results screen can mark them. The
  issue defers this explicitly; file separately if results need annotating.
- **Surfacing `SCHEDULED` experiments through the page API** so the scheduled banner appears on an
  ordinary page load. Backend work, outside the epic's frontend scope — **O3**, resolved as not in
  scope.
- **Any backend change.** The block is frontend-only and there is nothing server-side to remove.
  FR-034 through FR-040 are verification obligations, not backend work.
- **The new Edit Content editor's variant handling** (**O6**). Pre-existing, reachable today under a
  `DRAFT` experiment, unverified at runtime, and independent of experiments altogether. Recorded so it
  is not lost; to be verified and filed on its own.
- **`$showWorkflowsActions`** (`withView.ts:145-152`), which hides the page's own workflow actions on
  any non-DEFAULT variant. Deliberate existing behavior, identical under `DRAFT`, and unrelated to
  editing contentlets.
- **Changing the toolbar's running-experiment tag**, beyond whatever **O1** decides about link
  targets.
- **Changing the page-lock banner**, its copy, or its dismissal behavior.
- **Deleting `computeCanEditPage()`** even though nothing calls it. Removing dead code is a defensible
  change and not this one; the spec fixes its logic and records that it is unused.

---

## Regression Risk

- **Blast radius**: the two store computations feed the editor's entire editable surface — the
  editable overlay, contentlet controls, drop and click handlers, the mode selector, the workflow
  actions, the Layout screen and the nav bar's Layout item (see the Scope Note table). Every one of
  them changes behavior on a page with an active experiment, and none of them may change behavior on
  a page without one. The compound expressions are the specific hazard: `hasPermissionToEditLayout`
  returns `canEditPage && canDrawTemplate && !isExperimentRunning && !$lockIsPageLocked()`, and
  dropping the wrong term silently grants layout editing on an advanced template or on a page locked
  by someone else.
- **Backward compatibility**: no API, contentlet, serialized state, DB or ES mapping is touched. The
  three new message keys are additive. `computeCanEditPage()` is exported but unused, so its changed
  behavior reaches nothing. Not rollback-unsafe.
- **Data considerations**: none for this change. FR-034 through FR-040 exist to prove that, which is
  why they are stated as verification rather than migration.
- **Not an exposure risk**: an earlier reading of this spec held that the change would expose
  pre-existing variant-save defects. It does not. Every variant path this issue reaches is already
  reachable under a `DRAFT` experiment, which needs nothing from #37308 — so this change widens no
  surface that was previously unreachable. The open question recorded in **O6** is independent of
  this issue and independent of experiments altogether.
- **Product risk, accepted and stated**: editing a variant mid-run pollutes the run's dataset going
  forward, and the comparison gets weaker. This is the accepted cost of the change, not a defect. It
  does not invalidate measurements already taken. Anyone wanting a clean comparison can still end the
  experiment before editing.
- **Merge risk**: #37478 edits `dot-ema-shell.component.html` and `.ts` in the same files this change
  edits. Its HTML additions sit after the nav bar (around line 49) while the banner belongs at the
  top, so they do not textually collide — but both branches touch the shell component's TypeScript,
  and whichever lands second rebases.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the Universal Visual Editor's edit-capability rules and the UVE
  shell's banner area — modern frontend (`core-web/libs/portlets/edit-ema/portlet`), not the legacy
  admin surface. The block being removed is recent UVE code, not inherited behavior; the *product*
  rule it encodes is older than UVE and predates the editor it now lives in.
- **Backward-compatibility expectations**: editors who have learned that an active experiment freezes
  the page will find it no longer does. That is the intent, and the banner is what makes the new rule
  legible rather than surprising. No API or content contract changes. The pre-UVE confirm dialog is
  not restored and its strings stay dead.
- **Known related decisions**: the issue cites `docs/backend/EXPERIMENTS_CONSTRAINTS.md` for the
  status lifecycle and the rules that remain in force. **That file does not exist** anywhere in the
  repository's history, so this spec grounds the lifecycle in `DotExperimentStatus`
  (`dot-experiments-constants.ts:35-41`) and the surviving rules in `ExperimentsAPIImpl` directly.
  The plan phase will consult `dotCMS/platform-adrs` formally; no ADR is proposed here — this change
  removes a condition and adds a message, and decides nothing architectural.

---

## Assumptions

- **A1 — The copy is a proposal.** The exact wording in the issue is treated as a starting point
  pending product and design review. What this spec holds firm is the `RUNNING`/`SCHEDULED` split
  (FR-027), the framing as forward pollution rather than retroactive invalidation (FR-030), and the
  key namespace (FR-032). Rewording within those constraints needs no spec change.
- **A2 — The banner shows in every view mode.** The lock banner is not mode-gated and neither is the
  running-experiment tag, so the banner follows the shell's existing habit rather than inventing a
  mode condition. If it should be `EDIT`-only, that is **O4** and it changes FR-017.
- **A3 — Banner visibility follows `store.pageExperiment` and nothing else.** No new request is made
  and no polling is added. The banner is as fresh as the experiment the editor already fetches on page
  load, which means a status that changes server-side mid-session is reflected on the next refresh of
  that value, not instantly. The issue's edge cases are read as "the banner must be correct for the
  status it has", not "the banner must detect status changes in real time".
- **A4 — Both banners show when both apply.** Absent direction, the lock banner and the experiment
  banner are assumed to coexist rather than one suppressing the other: they report different things
  and the lock one reports a rule still being enforced. FR-024 requires the choice to be explicit;
  this is the default it starts from, and **O5** is where it gets confirmed.
- **A5 — The scheduled banner's reach is the portlet arrival path.** Per finding 2 in the Scope Note,
  `SCHEDULED` reaches the editor only with an `experimentId` in the URL. FR-002 and the scheduled copy
  are specified for that path. Widening it is **O3**.
- **A6 — "Enter edit mode" means the store's capability signals.** Verification targets
  `editorHasAccessToEditMode`, `editorCanEditContent` and `editorCanEditLayout`, since those are what
  the UI reads. A unit test of `computeCanEditPage()` is required by FR-005 but is not evidence for
  FR-001 or FR-002.
- **A7 — The variant path needs no new machinery, because `DRAFT` already exercises it.** FR-007 and
  SC-001a assume that a variant editable under a `DRAFT` experiment behaves identically under
  `RUNNING` once D8 lets the editor reach it, since no code on that path reads experiment status.
  This is an assumption drawn from the code, not from a runtime test: it is exactly what SC-001a
  measures, and if the two ever differ, something in this change reached the save path and the change
  is wrong.
- **A8 — The banner declares no accessibility attributes of its own.** No `p-message` anywhere in the
  repository sets `role` or `aria-*`, the lock banner included; the new banner follows that precedent
  and inherits whatever PrimeNG provides. Adding bespoke a11y semantics to this one surface would
  make it the only one of its kind and is not this issue's to introduce. If the shell's banners should
  be announced differently, that is a change to all of them and belongs in its own issue.

---

## Dependencies

- **#37005 (UVE entry point + variant Edit Content round-trip)** — merged as `44a4026d37`. It is what
  puts `experimentId` in the editor's URL, and therefore the only reason a `SCHEDULED` experiment ever
  reaches `store.pageExperiment` at all. A5 rests on it.
- **#37478 (the page's experiments as a UVE panel)** — open, PR #37485, in progress. It edits the same
  two shell files and it redefines what the running-experiment badge does when the flag is on. **O1**
  and **O2** are the unresolved parts.
- **No backend dependency.** The block is frontend-only; nothing server-side needs to change for
  FR-001 through FR-009.

---

## Out of Scope

Restated compactly for the reviewer: the confirm dialog is not coming back; the dead legacy strings
stay; mid-experiment edits are not recorded or annotated on the results screen; `SCHEDULED`
experiments are not surfaced through the page API; `computeCanEditPage()` is fixed but not deleted;
the lock banner is not touched; and no backend code changes.

---

## Resolved Decisions

### D1 — Both `RUNNING` and `SCHEDULED` are unblocked, and the condition is deleted outright

Not narrowed to `RUNNING`, and not softened into a warning-plus-gate. The condition leaves the three
computations entirely. A `SCHEDULED` experiment has collected nothing, so freezing a page before a
single measurement exists was never defensible; and for `RUNNING`, the judgement belongs to the
editor. → FR-001 through FR-005, FR-009.

### D2 — A persistent banner, not the legacy confirm dialog

A dialog is a gate, and a gate is what this issue removes. It also has to be answered once and then
stops existing, which is exactly wrong for a condition that persists for days. The banner is
non-dismissible for the same reason: a warning the editor can close is a warning that is absent while
still true. → FR-017 through FR-021.

### D3 — Template and layout editing are unblocked on the same terms as content

The issue treats layout as content's equal here, and the code already did: the same status condition
sat in both computations. Nothing about a layout edit is safer or riskier for an experiment than a
content edit. → FR-004, FR-012.

### D4 — Different copy per status, framed as forward pollution

The two statuses carry genuinely different risks — one is measuring right now, the other has not
started — so one string cannot be honest about both. And neither may say "invalidate": measurements
already taken remain accurate for the content that was live when they were taken. What an edit does
is make the run measure two different things. → FR-027 through FR-030.

### D5 — Data integrity is verified, not assumed

The change's justification is a claim about system behavior ("your collected data survives this"), so
the claim gets acceptance criteria. The verification is real but cheap: results are queried live from
CubeJS at read time, and `ExperimentsAPIImpl` subscribes to exactly two events, neither of which is a
save. → FR-034 through FR-040.

### D6 — The spec is written in the feature form, not the issue-resolution form

`/speckit-specify-fix` seeds a defect-framed template (Reproduction, Root-Cause Hypothesis). There is
no defect and nothing to root-cause: the code does exactly what it was written to do. The template's
genuinely useful sections — Fix Scope & Non-Goals, Regression Risk — are kept as their own sections;
the diagnostic ones are dropped in favour of the epic's established structure (#37176, #37478), which
the epic's review tooling also expects.

### D7 — `computeCanEditPage()` is fixed and kept

It has no caller, so the honest options were to fix it, delete it, or leave it wrong. Leaving it wrong
is untenable now that the rule has changed — it is exported, and the next caller would inherit a stale
rule. Deleting it is defensible but is unrelated cleanup that would widen this diff and lose the
issue's explicit acceptance criterion. So: fixed, tested, and documented as unused. → FR-005, A6.

---

### D8 — A non-draft experiment's variants open editable, for `RUNNING` and `SCHEDULED` only

The guard removal alone does not let an editor reach a variant of a running experiment, because the
portlet chooses the mode before UVE ever sees the page: `editorMode` is
`isControl || experimentIsReadOnly ? PREVIEW : EDIT`
(`dot-experiments-configure-variants.component.ts:193`), and `experimentIsReadOnly` is
`store.$isLocked()` — defined as `status !== DRAFT`
(`dot-experiments-configure.store.ts:166`). So every non-draft experiment's variants open read-only,
`RUNNING` and `SCHEDULED` included.

That rule is #37005's **FR-009** ("every variant of an experiment that is not a draft MUST open
read-only") and it is pinned by a test: a `describe.each` over `RUNNING`, `SCHEDULED`, `ENDED` and
`ARCHIVED` asserting `PREVIEW` (`dot-experiments-configure-variants.component.spec.ts:1253-1273`).

**Decision: release `RUNNING` and `SCHEDULED`; keep `ENDED` and `ARCHIVED` read-only.** The shipped
test's own rationale — "an ended or archived experiment's results are just as corruptible by an
accidental edit" — is strongest exactly where this keeps it and weakest where it releases it: a
finished experiment's variant has nothing to gain from being editable, while a live one has an editor
with a reason to edit and a banner stating the cost. Releasing all four statuses would be simpler and
worse.

Consequences, stated rather than discovered later:

- The change is one expression at `dot-experiments-configure-variants.component.ts:193`, which must
  become status-aware. It MUST NOT be made by relaxing `$isLocked`: that signal also freezes the
  configuration form's name, description, traffic allocation, goal, scheduling and Save
  (`dot-experiments-configure.component.ts:364-424`,
  `dot-experiments-configure-footer.component.ts:72,101`,
  `dot-experiments-configure-page.component.ts:141,152`), and those MUST stay frozen. The variants
  component does not read experiment status today, so this adds a dependency rather than deleting a
  term.
- `isControl` stays: the control variant keeps opening read-only (#37005 **FR-008**), which is about
  the control being unmodified and is orthogonal to experiment status.
- #37005's FR-009 must be amended, and its `describe.each` split — `RUNNING` and `SCHEDULED` flip to
  expecting `EDIT`, `ENDED` and `ARCHIVED` keep `PREVIEW`.
- This widens the change beyond `edit-ema` into `libs/portlets/dot-experiments`. Both are frontend,
  so the epic's frontend-only rule holds.

### D9 — Both banners show, lock first

A page can be locked by another user *and* carry an active experiment. Both banners are then
eligible, and both are shown, with the **lock banner above the experiment banner**.

The lock reports a rule that is being enforced right now and offers the one action available
(Unlock); the experiment reports a cost that blocks nothing. What blocks the user goes first.
Suppressing the experiment banner while locked was rejected for a specific reason: the editor
deciding whether to take the lock is exactly the person who benefits from knowing an experiment is
running — hiding it withholds the information at the moment it is most decision-relevant.

The order is a property the implementation must guarantee and a test must assert. FR-024 required
this to be stated rather than left to template ordering, and until this decision the spec had not
stated it.

### D10 — The banner is not gated on edit permission

A user who can read the page but not edit it still sees it. Both existing precedents in the shell
behave this way — `$showLockBanner` and the toolbar's running-experiment tag consult `canEdit`
nowhere — and the copy informs as much as it warns: knowing an experiment is running explains why the
page has variants. Gating it would also add a fourth suppression condition to FR-023, with its own
test, to remove information from someone who might be about to request access.

### D11 — The data-integrity guarantee is verified by an integration test, not by QA

FR-034 through FR-038 assert that collected data survives an edit. That is the premise the whole
change rests on, and until now this spec could only offer static analysis for it — no runtime
verification had been performed.

It turns out none is needed as a special effort: `ExperimentDataGen.nextPersistedAndStart()` already
puts an experiment into `RUNNING` in a single call. So the test is cheap and is written here (FR-040a):
start an experiment, save a contentlet on a non-DEFAULT variant, and assert both that the collected
measurements survive and **which variant actually received the write**.

The second assertion is the reason this decision matters beyond its own FRs: it settles **O6** with
evidence. The static reading is genuinely inconclusive there, because `resolveContentletByVariant`
returns the contentlet untouched when the requested variant already matches its own — so whether a
real edit is coerced depends on which variant the contentlet lives in, which no amount of reading
decides. One test answers it.

This puts a Java *test* in a frontend-only epic. That is accepted: the constitution's Principle V
asks for layer-appropriate tests, the claim being tested is a backend claim, and the alternative is
shipping the issue's central guarantee unverified. It adds no production Java.

FR-040b exists because of a known trap in this repository: an integration test not registered in a
`MainSuite*` / `Junit5Suite*` `@SuiteClasses` list compiles, leaves CI green, and never runs.

## Open Decisions

Questions this spec deliberately does not answer. Each is either a product call or a cross-issue
coordination call, and each is recorded here rather than guessed at.

Only **O1** and **O2** are still open, and both concern the banner's link and its wording against
#37478 — neither blocks the guard removal, the banner itself, or FR-007.

**O3**, **O4** and **O5** are resolved. **O6** is not a blocker at all: it records a pre-existing,
experiment-independent question about the new Edit Content editor's variant handling, deliberately
left for its own issue. All four are kept below with their resolutions rather than deleted, so a
reader of the review artifact can see what was asked and what came back.

What this issue needed and no longer lacks: the decision on whether a non-draft experiment's variants
open editable is now **D8**, not an open question.

### O1 — Where does the banner's link go when the experiments panel flag is on?

The proposed banner links via `routerLink` to `/edit-page/experiments/{pageId}/{id}/reports`. That is
precisely the route #37478 is taking out of service: **#37478's FR-025c** says that with
`FEATURE_FLAG_EXPERIMENTS_PORTLET` on, the toolbar badge "MUST open the panel on that experiment's
results. It MUST NOT navigate, MUST NOT change the browser address, and MUST NOT reload the page. Its
existing deep link to the legacy full-screen results route MUST NOT be used while the flag is on."
Its stated goal for that phase is that "no entry point into experiments ejects the editor any more."

A banner that deep-links to the legacy full-screen route would be the last surface that still ejects
the editor — and it would appear on exactly the pages where the panel is most relevant. Options: make
the link flag-aware and have it open the panel when the flag is on; keep the eject and accept the
inconsistency; or drop the link and let the toolbar badge be the only way through.

**This is a question for the coordinator, not a decision this spec makes.** It affects this spec's
FR-022 and FR-042.

### O2 — Does #37478's FR-022 need amending once "the experiment forbids editing" stops existing?

#37478 **FR-022** requires that while the editor is on a variant they be told "whether what they are
seeing is editable or read-only — and, if read-only, which reason applies: a preview, the unmodified
control, or **an experiment whose state forbids editing**."

This issue removes that third reason. After #37308 there is no experiment state that forbids editing
the page. Either #37478's FR-022 needs its third reason struck, or it was always about a read-only
experiment *configuration* (#37478's FR-020) rather than a read-only page — in which case the wording is
ambiguous and worth tightening. Surfacing rather than deciding, since #37478's spec is already
reviewed and posted.

### O3 — Is surfacing `SCHEDULED` experiments through the page API in scope? *(resolved: no)*

Per finding 2 in the Scope Note, `pageAsset.runningExperimentId` is `RUNNING`-only by construction, so
a scheduled experiment never reaches the editor on an ordinary page load. As a result the scheduled
banner will only ever appear on the portlet arrival path, and the scheduled half of the block was
already inert everywhere else.

If the intent is that an editor navigating normally to a page with an experiment starting Monday sees
the scheduled warning, the page API has to change — backend work, which collides with the epic's
frontend-only scope. If the narrower reach is acceptable, FR-002 and A5 stand as written.

**Resolved: not in scope.** The epic is frontend-only, so FR-002 stays confined to the portlet
arrival path and A5 stands. A backend follow-up can be filed separately if the wider reach is wanted.

### O4 — Should the banner appear in `PREVIEW` and `LIVE` mode, or only in `EDIT`? *(resolved: all modes)*

The warning is about editing, which argues for `EDIT` only. But the lock banner is not mode-gated,
the running-experiment tag is not mode-gated, and a reader in preview who is about to switch to edit
arguably benefits from seeing it first. A2 assumes all modes, following the shell's existing habit.

**Resolved: all modes.** A2 stands as an assumption rather than hardening into a MUST, so FR-017 is
unchanged.

### O5 — When a page is both locked by another user and running an experiment, what shows? *(resolved: both)*

Two banners are eligible, stacked. The lock banner reports a rule that is still enforced and blocks
the editor; the experiment banner reports a cost that blocks nothing. Showing both is honest but
noisy, and the experiment warning is arguably irrelevant to someone who cannot edit at all. A4
assumes both show; FR-024 requires whatever is chosen to be explicit rather than incidental to
template order.

**Resolved: both show.** A4 stands, and FR-024's requirement that the ordering be deliberate rather
than incidental still applies.

### O6 — Is the new Edit Content editor's variant handling a live defect? *(not this issue's, but do not lose it)*

Recorded here so it is not lost, **not** because it gates this issue.

A save routed through the new Edit Content editor carries no variant (Scope Note finding 4): no
variant field on `EditContentDialogData`, no variant handling in `libs/edit-content`, `fireTo` →
`PUT /v1/workflow/actions/{actionId}/fire` which declares no `variantName`, and server-side the
7-argument `getContentlet` overload hard-coding `VariantAPI.DEFAULT_VARIANT.name()`
(`WorkflowResource.java:5080`) plus `resolveContentletByVariant` (`:5164-5188`). The legacy dialog is
variant-correct by contrast, deliberately so since `fix(uve): Save variant content correctly`
(#30556, November 2024).

**Why it is not this issue's problem**: the path is reachable today under a `DRAFT` experiment, which
needs nothing from #37308. If it is a defect, it is already a defect, for any variant edit on a
content type carrying `FEATURE_FLAG_CONTENT_EDITOR2_ENABLED`. #37308 neither creates it nor uniquely
exposes it, and FR-007a requires only that this change not make it worse.

**What static analysis cannot settle**: whether it actually misbehaves. The coercion is conditional —
`resolveContentletByVariant` returns the contentlet unchanged when the requested variant already
equals its own — so whether a real variant edit is coerced depends on which variant the contentlet
being edited lives in at that moment, which reading the code does not decide. **Until FR-040a's test
runs, this MUST NOT be reported as a confirmed bug.**

**Disposition — resolved by D11.** The verification is no longer deferred. The integration test
required by **FR-040a** asserts which variant actually received the write, which answers this
question as a by-product of proving the data-integrity guarantee. If it reproduces, file the defect
with #30556 as the precedent for the fix shape; if it does not, this entry is closed with evidence
rather than left as a standing suspicion. Either way the *fix*, should one be needed, stays out of
#37308 — the question was only ever whether we would find out.

One thing that fix would have to reckon with, recorded so the next person does not rediscover it: the
by-actionId fire path has nowhere to put a variant. `/actions/{actionId}/fire` declares no
`variantName` at either layer — not in `DotWorkflowActionsFireService.fireTo` nor in
`WorkflowResource`. The only variant-aware fire endpoint is
`/actions/default/fire/{systemAction}` (`WorkflowResource.java:3291`, `@QueryParam("variantName")`
with `@DefaultValue("DEFAULT")`), which fires a content type's *default* action for a system action
rather than the specific action the user chose — so it is not a drop-in substitute. A fix therefore
likely needs the by-actionId endpoint to accept `variantName` and thread it to the 8-argument
`getContentlet`, mirroring its sibling. That is a Java change, which is why it does not belong in a
frontend-only epic.
