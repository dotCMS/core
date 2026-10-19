# Feature Specification: Editing a Variant While Its Experiment Is Live

**Feature Branch**: `issue-37308-edit-page-active-experiment`

**Created**: 2026-09-10

**Status**: Draft

**Type**: Task (constraint removal + a new warning surface)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37308 — Allow editing pages with an active experiment, with a non-blocking warning banner](https://github.com/dotCMS/core/issues/37308)

**Input**: User description: "Allow editing pages with an active experiment, with a non-blocking warning banner" — taken from issue #37308.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fix a variant while its experiment is running (Priority: P1)

A marketer is four days into an A/B test on the pricing page when they spot a typo in the variant's
headline. Today the variant opens read-only, so their only options are to leave the typo up for the
rest of the run or kill the experiment. They should be able to fix it, having been told what fixing
it costs.

**Why this priority**: this is the feature. Everything else supports it.

**Independent Test**: start an experiment with a variant, edit that variant's content, and save. The
change persists on that variant and the experiment keeps running.

**Acceptance Scenarios**:

1. **Given** a running experiment and a user with permission to edit its page, **When** they choose
   to edit one of its variants, **Then** the variant opens for editing rather than read-only.
2. **Given** a scheduled experiment that has not started, **When** they choose to edit one of its
   variants, **Then** the variant opens for editing.
3. **Given** they are editing that variant, **When** they change content and save, **Then** the
   change is stored against **that variant**, is visible when the variant is reopened, and the other
   variants are unaffected.
4. **Given** they saved a change, **When** they look at the experiment, **Then** it is still running,
   unchanged in schedule, variants and identity.
5. **Given** an experiment that has ended or been archived, **When** they open one of its variants,
   **Then** it opens read-only, as it does today.
6. **Given** any experiment, **When** they open its **control** variant, **Then** it opens read-only,
   as it does today.

---

### User Story 2 - Decide, knowingly, before entering a live variant (Priority: P1)

The same marketer should not fall into editing a live variant by accident. At the moment they choose
it, they are told what it does to the run and given a way to back out.

**Why this priority**: removing the block without telling anyone is the half of this change that
would do harm. US1 and US2 ship together or not at all.

**Independent Test**: choose to edit a variant of a running experiment; a confirmation appears.
Cancel it and confirm nothing opened and nothing changed.

**Acceptance Scenarios**:

1. **Given** a running or scheduled experiment, **When** the user chooses to edit one of its
   variants, **Then** they are asked to confirm before being taken there, told what they are about
   to do and what it costs.
2. **Given** that confirmation, **When** they cancel, **Then** they stay where they were with nothing
   opened and nothing changed.
3. **Given** that confirmation, **When** they accept, **Then** the variant opens for editing.
4. **Given** a **draft** experiment, **When** they choose to edit one of its variants, **Then** no
   confirmation is asked — this is the ordinary case and carries none of the cost.
5. **Given** they confirmed earlier in the session, **When** they choose to edit a variant again,
   **Then** they are asked again.

---

### User Story 3 - Keep knowing, while the work goes on (Priority: P1)

Having accepted the confirmation, the editor may work for an hour. The experiment does not stop
running because a dialog was dismissed, so the fact stays on screen — and from there they can look
at how the run is doing without losing the page.

**Why this priority**: a confirmation is answered once; the condition persists. An editor returning
from a coffee break has no other way to know the run is still live.

**Independent Test**: open a page whose experiment is running. A warning is present, cannot be
dismissed, and is still present after an edit and a save.

**Acceptance Scenarios**:

1. **Given** a page whose experiment is running, **When** the editor opens it, **Then** a warning
   says the experiment's results will mix data from before and after their changes.
2. **Given** a page whose experiment is scheduled, **When** the editor opens it, **Then** the warning
   says instead that edits made before the start become part of what the experiment measures.
3. **Given** that warning, **When** the editor looks for a way to dismiss it, **Then** there is none.
4. **Given** that warning, **When** the editor edits and saves, **Then** it is still shown, and
   nothing it does delays or blocks any editing action.
5. **Given** that warning, **When** the editor follows it, **Then** the experiment opens beside the
   page without leaving it.

---

### User Story 4 - Edit the page itself during a run (Priority: P2)

The page behind the experiment — not one of its variants — is unblocked by the same change and
carries the same warning.

**Why this priority**: separately valuable and separately testable, but the variant case is what the
feature exists for.

**Independent Test**: open a page with a running experiment and confirm it can be edited and saved.

**Acceptance Scenarios**:

1. **Given** a page with a running experiment, unlocked, and a user with edit permission, **When**
   they open it, **Then** editing is available and they are not returned to a read-only view.
2. **Given** they are editing, **When** they change content and save, **Then** the save succeeds and
   the controls needed to perform it are present.
3. **Given** that page is built on a standard template, **When** they open the layout editor,
   **Then** the layout can be edited.

---

### Edge Cases

- **An experiment starts or ends while the editor is on the page.** Neither transition may take
  editing away, and the warning must match the status as the editor's view last saw it.
- **The experiment cannot be retrieved.** If the system cannot tell whether an experiment exists, it
  must neither invent a warning nor withhold editing.
- **The editor moves from one page to another.** The warning must not linger from the previous page
  once the new page's state is known.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users with edit permission MUST be able to edit a variant of an experiment that is
  running or scheduled.
- **FR-002**: Users with edit permission MUST be able to edit a page whose experiment is running or
  scheduled, including its layout.
- **FR-003**: Saving such an edit MUST succeed, and the controls needed to save MUST be present.
- **FR-004**: An edit made to a variant MUST be stored against that variant, readable from it
  afterwards, and MUST leave the other variants untouched.
- **FR-005**: The control variant MUST continue to open read-only, in every experiment status.
- **FR-006**: Variants of an experiment that has ended or been archived MUST continue to open
  read-only.
- **FR-007**: A variant of a live experiment MUST be reachable for editing from both the experiments
  portlet and the experiments panel inside the editor, with identical behavior.
- **FR-008**: Before opening a variant of a running or scheduled experiment, the system MUST ask the
  editor to confirm, stating what they are about to do and what it costs.
- **FR-009**: The confirmation MUST offer a way to cancel, and cancelling MUST leave the editor where
  they were with nothing opened and nothing changed.
- **FR-010**: The confirmation MUST be asked on every such action, and MUST NOT be suppressible by a
  user preference.
- **FR-011**: The confirmation MUST NOT be asked for a draft experiment, nor for the control variant.
- **FR-012**: While a page's experiment is running or scheduled, the system MUST show a persistent
  warning on that page that blocks, gates and delays nothing.
- **FR-013**: The warning MUST have no way to be dismissed, and MUST remain for as long as the status
  that justifies it holds.
- **FR-014**: The warning MUST be shown only while the experiment is running or scheduled — never for
  a draft, ended or archived experiment, for a page with no experiment, or when the experiment cannot
  be retrieved.
- **FR-015**: The warning MUST NOT depend on edit permission. A user who can read the page but not
  edit it MUST still see it.
- **FR-016**: When a locking warning is also eligible, both MUST be shown, with the locking warning
  first. The order MUST be a guaranteed property, not an accident of layout.
- **FR-017**: Following the warning MUST open the experiment beside the page, in the editor's
  experiments panel, without navigating away or reloading, and MUST NOT send the editor to a
  full-screen screen.
- **FR-018**: Running and scheduled experiments MUST use different wording. A single shared message
  MUST NOT serve both.
- **FR-019**: The running message MUST convey that results will mix data from before and after the
  change.
- **FR-020**: The scheduled message MUST convey that edits made before the start become part of what
  the experiment measures, and MUST read as informational rather than cautionary — a scheduled
  experiment has nothing to pollute.
- **FR-021**: No wording added by this feature MAY claim that editing invalidates, discards or
  destroys measurements already collected, or that it destroys content.
- **FR-022**: Page locking MUST be untouched. A page locked by another user MUST remain non-editable,
  on a page with a live experiment exactly as on a page without one.
- **FR-023**: Edit permission MUST remain enforced. This change MUST NOT grant edit access to anyone
  who did not have it.
- **FR-024**: Layout editing MUST remain unavailable on hand-coded templates, and the reason surfaced
  MUST remain the template rather than the experiment.
- **FR-025**: An edit MUST NOT delete, reset, purge or archive collected experiment data, and MUST
  NOT alter the experiment's identity, variants, measurement window, status, schedule or dates, nor
  start a new run.
- **FR-026**: Data collected before an edit MUST remain accessible on the experiment's results after
  it.
- **FR-027**: FR-025 and FR-026 MUST be proven by an automated test running against a real running
  experiment, registered so that it actually executes in continuous integration rather than passing
  by not running.

### Key Entities

- **Experiment**: a test running on one page, with a status, a schedule, a set of variants and a
  measurement window. Its status is what this feature reads: running and scheduled are *live*; draft,
  ended and archived are not.
- **Variant**: one version of the page being measured. One is the **control**, which represents the
  page as it was and is never edited through this feature.
- **Collected measurements**: what the experiment has recorded so far. Read live when results are
  requested, and never rewritten by an edit.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A marketer can correct content in a variant of a running experiment and save it without
  ending the experiment, and without any step refusing them.
- **SC-002**: That journey behaves identically whether it starts in the experiments portlet or in the
  experiments panel inside the editor.
- **SC-003**: The correction is read back on the variant it was made on; the other variants and the
  page itself are unchanged.
- **SC-004**: A measurement recorded before an edit reads exactly the same after it, and the
  experiment's identity, variants, window, schedule and dates are unchanged.
- **SC-005**: The test proving SC-004 executes in continuous integration and reports a non-zero
  number of tests run.
- **SC-006**: No editor reaches a live variant without having confirmed, and none is asked to confirm
  for a draft experiment or a control variant.
- **SC-007**: An editor who could not edit before still cannot — no edit permission, or a page locked
  by someone else — on a page with a live experiment exactly as on one without.
- **SC-008**: The warning is visible for as long as the experiment is live and at no other time,
  including when the experiment cannot be retrieved.
- **SC-009**: Reaching the experiment from the warning leaves the editor on the page.
- **SC-010**: No wording this feature adds tells an editor that their change destroys measurements or
  content.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the page editor's rules about when a page may be edited, the
  experiments configuration screen, and the editor's warning area. All are part of the current
  product surface, not the older admin. The *product rule* being reversed is older than the editor
  that now enforces it.
- **Backward-compatibility expectations**: editors who learned that a live experiment freezes the
  page will find it no longer does — that is the intent, and the warnings are what make the new rule
  legible rather than surprising. No content, API or admin workflow changes, and nothing here is
  rollback-unsafe.
- **Known related decisions**: an earlier work item in this epic deliberately made variants of every
  non-draft experiment open read-only. This feature amends that for running and scheduled
  experiments and keeps it for ended and archived ones, so that decision needs revisiting alongside
  this one. The plan phase consults `dotCMS/platform-adrs` formally.

## Assumptions

- **A1 — The wording is a proposal.** Messages are specified by what they must convey (FR-019,
  FR-020) and never claim (FR-021), not by final copy. Product and design have not reviewed it;
  changes within those constraints need no change to this spec.
- **A2 — Nothing is lost by editing.** A page under an experiment behaves like any other page: the
  edit is versioned and recoverable the same way, and collected measurements are untouched. The
  confirmation therefore states a consequence to accept — from this point the run measures different
  content — not a loss to avert.
- **A3 — The warning shows in every viewing mode**, following the habit of the other warnings on that
  surface rather than inventing a mode-specific rule.
- **A4 — The warning is as fresh as the experiment data the editor already holds.** No new request
  and no polling is added, so a status that changes on the server mid-session appears on the next
  refresh rather than instantly.
- **A5 — A scheduled experiment reaches the editor only from the experiments screens**, because that
  is where it arrives with the page context. Making the scheduled warning appear on any arbitrary
  visit would require the page data to carry scheduled experiments, which it does not today.
- **A6 — Editing a variant needs no new saving machinery.** A draft experiment's variants are
  editable today and save correctly, and nothing on that path reads experiment status, so releasing
  the status rule is sufficient.
- **A7 — Out of scope**, each worth its own work item if wanted: restoring the pre-release confirm
  dialog; annotating on the results screen which measurements straddle an edit; surfacing scheduled
  experiments on arbitrary page visits (A5); how the page's own publishing controls behave on a
  variant; and an open question about whether one particular content-editing path stores variant
  edits correctly, which is reachable today on draft experiments and so is neither created nor
  exposed by this change — FR-027's test will report what it does.
