# Feature Specification: Changing the Page of a Draft Experiment

**Feature Branch**: `issue-37176-draft-experiment-page-change`

**Created**: 2026-08-24

**Status**: Draft

**Type**: Task (behavior enablement + silent-drop defect)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37176 — Allow changing the page of a draft experiment that has no variants](https://github.com/dotCMS/core/issues/37176)

**Input**: User description: "Allow changing the page of a draft experiment that has no variants" — taken from issue #37176.

---

## Scope Note *(read this first)*

Today the page an experiment tests is frozen the instant the draft is created. The page picker is
disabled for *every* experiment that already exists, so an editor who picked the wrong page has
exactly one remedy: delete the experiment and start over.

That restriction is broader than the data actually requires, and it rests on a gap rather than a
decision. Partial updates to an experiment silently discard a submitted page — the field is accepted
by the request contract, then never read. Nothing rejects the change; nothing applies it either.

The **real** constraint is variants. Creating a variant copies the page's layout into that variant,
so the copy is meaningful only for the page it came from. Repoint the experiment at a different page
and every such copy is orphaned. The control variant is deliberately exempt from that copy — it owns
no duplicated layout, it *is* the page. So a page swap is safe in exactly one situation: the
experiment is still a draft and the control is its only variant.

This spec covers that situation and the honest refusal of every other one. It does **not** cover
migrating variant content between pages, nor changing the page of a running, scheduled, ended, or
archived experiment.

**Why DRAFT and not also SCHEDULED.** A scheduled experiment has not started and has collected no
data, so it looks like it should qualify. It does not, and the reason is not about data — it is
about a clearance that has already been granted. Starting an experiment validates that its page is
free of overlapping experiments, and it validates that against *the page the experiment had at that
moment*. A scheduled experiment has passed that gate and is committed to starting on its own. Let it
move to a different page and the clearance is silently stale: the new page may already host a
scheduled or running experiment with an overlapping window, and nothing re-checks before the
experiment fires.

That is exactly why FR-007 can say schedule conflicts are not re-validated here. For a draft the
rule is safe, because a draft has never been cleared and will be checked when it starts. For a
scheduled experiment it would not be. DRAFT is the last state in which the page is still
un-cleared, which is what makes it the boundary.

Two consequences worth stating plainly. Admitting SCHEDULED would mean either accepting that hole or
re-validating conflicts on the page change — the second contradicts FR-007 and is a larger change
than this one. And in practice a scheduled experiment is very unlikely to qualify anyway: starting
one requires more than the control variant, so the eligibility rule below would refuse it on the
variants condition even if status allowed it.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Correct the page on a fresh draft (Priority: P1)

An editor creates an A/B test, then realizes they attached it to the wrong page. The experiment is
still a draft and they have not built any variants yet. They reopen the page picker, choose the
right page, and the experiment now tests that page. Reloading confirms it stuck. The control's
"copy preview URL" hands back a link to the *new* page, not the old one.

**Why this priority**: This is the entire point of the work. Without it the editor's only recovery
from a mis-picked page is to throw the experiment away and rebuild it — name, description, goals,
traffic split and all.

**Independent Test**: Create a draft experiment with only the control, change its page, reload, and
assert both the experiment's page and the control's preview URL address the new page.

**Acceptance Scenarios**:

1. **Given** a draft experiment whose only variant is the control, **When** the editor selects a
   different page, **Then** the change is accepted and persisted.
2. **Given** that change has been accepted, **When** the experiment is reloaded from scratch,
   **Then** it shows the new page.
3. **Given** that change has been accepted, **When** the editor copies the control's preview URL,
   **Then** the URL addresses the new page.
4. **Given** a draft experiment whose only variant is the control, **When** the editor submits the
   page it already has, **Then** nothing changes and no error is raised.

---

### User Story 2 - An ineligible page change is refused, never silently dropped (Priority: P1)

Any attempt to repoint an experiment that does not qualify is rejected outright, with a reason that
names *which* rule blocked it — not accepted-then-ignored. An editor (or an integration) never
walks away believing a page change took effect when it did not.

**Why this priority**: Silently discarding a submitted page is the defect at the root of this issue.
Enabling the permitted case without closing the silent-drop path would leave the more dangerous half
of the bug in place: a caller that *thinks* its experiment moved.

**Independent Test**: Submit a differing page against (a) a draft carrying a real variant and (b) a
running experiment; assert both are refused with a client error naming the failing condition, and
that neither experiment's stored page changed.

**Acceptance Scenarios**:

1. **Given** a draft experiment that has at least one non-control variant, **When** a differing page
   is submitted, **Then** the request is refused and the message names the variants rule.
2. **Given** a running experiment, **When** a differing page is submitted, **Then** the request is
   refused and the message names the status rule.
3. **Given** any refused request, **When** it completes, **Then** the experiment's stored page is
   unchanged.
4. **Given** an experiment that is running *or* carries real variants, **When** the page it already
   has is submitted, **Then** it is treated as no change and is **not** refused.
5. **Given** a page change is refused, **When** the editor is working in the UI, **Then** the
   server's reason is surfaced to them and the rejected page is not left showing as selected.

---

### User Story 3 - Variants explain why the page is locked (Priority: P2)

An editor on a draft that already has variants finds the page picker disabled, and the explanation
tells them precisely what to do about it: *"This experiment already has variants. Delete them to
change the page."* They are not left guessing whether the lock is permanent.

**Why this priority**: Turns a dead end into a recoverable one. It is a usability layer over the
rule enforced in Story 2 rather than the rule itself, so it ranks below it — but without it the
newly-relaxed behavior looks arbitrary to the person using it.

**Independent Test**: Render the page card for a draft experiment carrying one non-control variant
and assert the control is disabled and carries exactly that explanation.

**Acceptance Scenarios**:

1. **Given** a draft experiment whose only variant is the control, **When** the page card renders,
   **Then** the page selection control is enabled.
2. **Given** a draft experiment with at least one non-control variant, **When** the page card
   renders, **Then** the control is disabled and explains that variants must be deleted first.

---

### User Story 4 - A locked experiment keeps its existing reason (Priority: P3)

An editor opening a running, scheduled, ended, or archived experiment sees the page picker disabled
for the reason it has always given — the experiment is read-only. The new variants explanation does
not displace it, even when the experiment also happens to have variants.

**Why this priority**: Pure regression protection on messaging. Nothing new is delivered; the
existing behavior simply must not be degraded by Story 3, which would otherwise tell an editor to
delete variants that are not the actual obstacle.

**Independent Test**: Render the page card for a non-draft experiment that also carries variants and
assert the read-only reason is the one shown.

**Acceptance Scenarios**:

1. **Given** a non-draft experiment, **When** the page card renders, **Then** the control is
   disabled with the existing read-only reason.
2. **Given** a non-draft experiment that *also* has non-control variants, **When** the page card
   renders, **Then** the read-only reason is shown, not the variants reason.

---

### Edge Cases

- **Submitting the unchanged page.** Always a no-op, never an error, whatever the status or variant
  count. This is what keeps clients that echo the whole experiment back on every save working.
- **Both rules fail at once** (non-draft *and* has variants). The refusal still names a blocking
  condition; the read-only reason is what the UI shows.
- **The UI's view is stale.** A variant was added, or the experiment started, since the editor
  loaded the screen. The client may believe the change is allowed; the server is the authority and
  refuses, and the editor sees why.
- **The target page hosts another experiment.** Permitted. Schedule overlap is settled when an
  experiment starts, not when its page is chosen, and re-checking it here would be stricter than the
  platform is elsewhere.
- **The editor cannot edit the target page**, or it does not exist. The change is refused by the
  same page-validity and permission rules that already govern saving an experiment.
- **The control has no stored preview URL yet.** The successful page change still leaves the control
  addressing the new page.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST read a submitted page on a partial update of an experiment and apply
  it, instead of discarding it.
- **FR-002**: A submitted page that differs from the stored one MUST be applied only when the
  experiment is in **Draft** status **and** the control is its only variant.
- **FR-003**: A differing page that fails either condition in FR-002 MUST be refused with a client
  error (HTTP 400) whose message names which condition failed. It MUST NOT be silently ignored.
- **FR-004**: A submitted page equal to the stored one MUST be treated as no change and MUST NOT be
  refused, regardless of the experiment's status or variant count.
- **FR-005**: On an accepted page change, the control variant's stored preview URL MUST be
  regenerated so it addresses the new page.
- **FR-006**: Every field the partial update already supports MUST keep working unchanged.
- **FR-007**: The page-change path MUST NOT re-validate schedule conflicts on the target page; that
  rule remains owned by experiment start.
- **FR-008**: The page selection control MUST be enabled for a Draft experiment whose only variant
  is the control.
- **FR-009**: When a Draft experiment has at least one non-control variant, the page selection
  control MUST be disabled and MUST explain: *"This experiment already has variants. Delete them to
  change the page."*
- **FR-010**: For any non-Draft experiment the page selection control MUST stay disabled showing the
  existing read-only reason, and that reason MUST take precedence over the variants reason.
- **FR-011**: An accepted page change MUST persist and MUST still be in effect after a full reload.
- **FR-012**: A refused page change MUST surface the server's reason through the standard error
  presentation, and the UI MUST NOT leave the rejected page displayed as selected.
- **FR-013**: The client-side update contract MUST admit a page change only as narrowly as FR-002
  allows, so an ineligible page change cannot be constructed and sent.

### Key Entities

- **Experiment**: The A/B test. Carries a status (Draft being the only editable one for this
  purpose), the page under test, and its traffic split across variants.
- **Control variant**: The variant representing the page itself. Holds no copied layout and carries
  a preview URL derived from the page. Always present.
- **Non-control variant**: A variant holding a *copy* of the page's layout, and therefore bound to
  the page it was copied from. Its existence is what forbids a page change.
- **Page**: The content page the experiment tests, and the source of every variant's preview URL.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An editor who picked the wrong page on a fresh draft can correct it without deleting
  and rebuilding the experiment — recovering a task that currently costs a full re-entry of the
  experiment's name, description, goals and traffic split.
- **SC-002**: Zero page changes are silently discarded. Every submitted page change is either
  applied or refused with a stated reason — measured across all three cases (eligible draft, draft
  with variants, non-draft).
- **SC-003**: After an accepted page change, the control's preview link opens the new page in 100%
  of cases.
- **SC-004**: All eight fields the partial update supported before this change continue to update
  successfully.
- **SC-005**: An editor blocked by existing variants is told what to remove; an editor blocked by a
  read-only experiment is told that instead. Neither is shown the other's reason.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Experiments (A/B testing) area — experiment editing, its
  partial-update REST contract, and the variant/preview-URL relationship. The experiment domain
  itself is modern code. The layout copying that motivates the variants rule reaches into the older
  page-layout subsystem, but this change only *reads* the rule's consequence; it does not modify
  layout copying.
- **Backward-compatibility expectations**:
  - Existing experiments continue to work untouched; nothing is migrated.
  - The partial-update contract *gains* a meaningful field. Input previously ignored can now be
    applied or refused — a behavior change for any caller already sending a page.
  - FR-004 is what contains that risk: a caller echoing back the experiment's current page is
    unaffected, because an equal value stays a no-op. Only a caller sending a genuinely *different*
    page against an ineligible experiment sees a new 400 — and that caller is today being lied to.
  - No deprecations. No stored-data shape changes.
- **Known related decisions**: The client-side contract deliberately excluded a page field as a
  compile-time guard added under #36988; relaxing it is in scope here and must stay as narrow as the
  rule it guards (FR-013). Sits alongside #37003 (experiment create/update screen) and is
  deliberately outside PR #37064. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **"Only the control" means exactly one variant, and it is the control.** The control is identified
  the way the product already identifies it (the default variant, surfaced to editors as
  "Original"). The frontend reuses the existing control check rather than inventing a second one.
- **The refusal wording is left to implementation**, beyond the requirement that it names the
  failing condition. The editor-facing variants message in FR-009 is fixed copy and is added as a
  localizable message.
- **Permission and page-validity checks are inherited.** Saving an experiment already validates that
  its page exists and that the user may edit it; after a page change those checks apply to the new
  page. No separate permission rule is introduced.
- **Only the control's preview URL is derived from the page.** Nothing else stored on the experiment
  (goals, targeting, traffic split) is re-derived on a page change.
- **Out of scope**: changing the page of a running, scheduled, ended, or archived experiment —
  scheduled included, for the clearance reason given in the Scope Note, not merely because it is
  "already started"; migrating variant content between pages; relaxing the variants rule by moving
  or re-copying layouts.
