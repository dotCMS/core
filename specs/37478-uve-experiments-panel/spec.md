# Feature Specification: The Page's Experiments as a Panel Beside the Canvas

**Feature Branch**: `issue-37478-uve-experiments-panel`

**Created**: 2026-09-09

**Status**: Draft

**Type**: Task (flagged behavior change to an existing, customer-facing flow)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37478 — Experiments Portlet — the page's experiments as a UVE panel, behind FEATURE_FLAG_EXPERIMENTS_PORTLET](https://github.com/dotCMS/core/issues/37478)

**Input**: User description: "Experiments Portlet — the page's experiments as a UVE panel, behind FEATURE_FLAG_EXPERIMENTS_PORTLET" — taken from issue #37478, plus the flow requirements given directly by the author and the "dotCMS A/B Testing Prototype" design.

---

## Scope Note *(read this first)*

### The premise this inherits

[#37005](https://github.com/dotCMS/core/issues/37005) decided (its spec, D2) that with
`FEATURE_FLAG_EXPERIMENTS_PORTLET` on, the editor's Experiments entry point leads to **the
experiments for the page in hand** — not to a site-wide list, and not straight to a single
experiment. That reading of the gesture is not reopened here. It is *kept*, and the only thing
that changes is where those experiments are shown: today the editor is sent away from the page to
see them; after this work they appear beside the page, which stays on screen.

### What #37005 left half-done, and how this closes it

#37005's FR-021c requires the page filter to be "visible to the editor and clearable, revealing
the full site-wide list". Half of that shipped: the filter is applied and honoured, but there is
no affordance to clear it — and clearing it would lead somewhere the editor cannot get back from.
The portlet is **opt-in**: it is declared in `portlet.xml`, and no upgrade task adds it to any
layout, so on a stock instance the administration menu carries no Experiments entry at all. An
editor who arrived from the editor and cleared the filter would be standing in a site-wide list
with no menu entry leading back to it.

A page-scoped panel changes the shape of that requirement rather than satisfying it literally. The
panel **is** the page's scope, by construction — there is no filter left to clear. "Clearable"
becomes an explicit, deliberate way out to the whole portlet, taken only when the editor asks for
it, and taken without costing them the page (D6).

### The flow, end to end

The flag chooses between the behavior that exists today and this one. With it on:

1. The editor activates Experiments on a page. **No navigation happens** — a panel opens beside
   the canvas, and the page stays rendered.
2. **The panel opens on the page's list**, always — that page's experiments, and no other page's.
   When the page has none, the list shows its empty state with a **New Experiment** action; the
   editor creates one when they choose to, rather than being taken there (D11).
3. **The editor picks an experiment from the list** to configure.
4. **In creation and in configuration, the page is fixed.** The experiment belongs to the page on
   the canvas. There is no page picker and no way to change the page, and the field says **why**
   it cannot be changed rather than merely refusing (D12).
5. **From a variant, Preview or Edit Content takes the editor into the editor for that variant.**
   The panel closes, the page is shown as that variant, and a banner says which variant of which
   experiment this is and whether it is editable.
6. **One control returns.** It brings the editor back and **reopens the panel on the same
   experiment's configuration** — the same experiment, the same screen, the configuration sequence
   intact rather than restarted (D13).
7. **Results open in the panel too**, from configuration and from the toolbar's running-experiment
   badge (D2, D14).
8. **The toolbar's running-experiment badge stops ejecting.** It already exists and today deep-links
   to the legacy full-screen results route; with the flag on it opens the panel on that experiment's
   results instead (D14).

### What is in scope, in three phases

| Phase | Screen | State after the phase |
| --- | --- | --- |
| 1 | **The list**, including its empty state and the New Experiment action | The page's experiments render in the panel. A row's Configure still opens the full-screen screen. |
| 2 | **Configuration**, and the variant round-trip in it | Configuration renders in the panel too. Neither listing, configuring nor editing a variant costs the page. |
| 3 | **Results**, and the toolbar badge that opens them | No entry point into experiments ejects the editor any more. |

Phase 1 must ship usable on its own. A panel that only lists, with Configure still opening
full-screen, is a smaller improvement than the whole thing — but it is not a broken one, and it
is where most of the shared plumbing lands (D3, D4, D5, D7).

All three phases are specified here, in one work item rather than three, because the plumbing the
panel needs — view state that does not live in the address, the page-scoped read, the fail-closed
flag read, and the dual-mode components — is shared, and phases 2 and 3 change none of it.

### Design reference

The "dotCMS A/B Testing Prototype" design canvas (`Experiments.dc.html`) models this flow, and this
spec follows it. The panel exists only inside the editor; the list, configuration and results
screens are the **same** screens the portlet renders, selected by a mode rather than duplicated
(its drawer has `list`, `exp` and `results` views); leaving for a variant closes the panel and
returns to the configuration of the same experiment; and the editor is told, in a banner, which
variant of which experiment they are in and whether it is read-only.

Earlier drafts of this spec diverged from the canvas in two places — opening straight on creation
when a page had no experiments, and keeping Results full-screen. Both divergences were reviewed and
reversed (D11, D2), so the spec and the canvas now agree.

The prototype also lets the page be changed from configuration, behind a warning dialog about
variants being lost. Inside the panel that affordance is absent (D12) — this is not a divergence
in the design so much as a consequence of the panel's scope, since the prototype's dialog belongs
to the full-screen portlet where no page is implied.

### What the current system already constrains

These are properties of the code as it stands on this branch, verified. Several of them are what
makes this work non-trivial, and one of them contradicts the issue that seeded this spec.

1. **The list's view state lives in the browser address.** The full-screen list reads its search
   term, status narrowing, sort and page from the address on init, writes every subsequent change
   back to it, and re-reads it on browser Back/Forward. The editor writes its *own* state to that
   same address, and it does so by **replacing** the query string rather than merging into it.
   Two writers, one address: whatever the list wrote is dropped the next time the editor writes.
   A panel therefore cannot use the address for its view state at all (D3). This is the largest
   piece of work in phase 1, and it is a change to the list's state management, not to a template.

2. **The full-screen list's table cannot render at panel width.** It is laid out across seven
   sortable columns with a hard floor of 81rem (1296px) below which it stops shrinking and scrolls
   horizontally instead. No sidebar width in the editor is close to that. The screen is reused;
   that layout is what has to become mode-dependent (D7).

3. **The list fetches every experiment on every site.** It then resolves each distinct page
   identifier through a bulk page lookup and narrows the set in the browser. A page-scoped panel
   has the page in hand already, and a page-scoped read already exists — three requests and a
   full-set payload become one (D4).

4. **The editor's batched feature-flag read fails *open*.** Flags fetched in that batch treat "key
   not found" as *enabled*. This flag sits beside the visitor-facing kill switch, and #37005
   deliberately refused that batch for it, reading it through a dedicated fail-closed reader
   instead. That refusal stands (D5).

5. **The list's analytics health gate is real.** The full-screen list makes no experiments request
   until an analytics health check reports OK, and renders a misconfiguration state instead when
   it does not. A panel is subject to the same gate.

6. **Breadcrumbs are pushed by the experiments screens onto a shared trail.** Inside the editor
   that trail belongs to the page (D8).

7. **Push Publish and Add to Bundle need nothing from the router — the issue is wrong about
   this.** Issue #37478 states that the list route's push-publish-environments resolver is read
   by the row menu, and that a panel mounted outside the router would break that action silently.
   It is not: the list component never reads that resolved value. Push Publish is opened through
   a global dialog service that takes only an asset identifier, and Add to Bundle fetches its own
   bundles. The resolver on the list route is a prefetch that nothing consumes. Both actions
   therefore work from a panel row with no route plumbing at all (D9). The other half of the
   issue's concern — the analytics health gate — does hold, and is covered by FR-027.

8. **Experiments are scoped to a page, not to a language version of a page.** `DotExperiment` carries
   `pageId` and has no language field at all; the page-scoped read narrows on `pageId` alone. The
   `language_id` that #37005 carries from the editor to the portlet is **return context**, not a
   filter — that code says so itself ("`language_id` is not a filter — the list narrows on `pageId`
   alone… a page identifier says nothing about which version was open"), and it exists so the return
   link and the Configure prefill do not have to guess which version to open. Everything in this spec
   that touches language follows from that one fact: creation stores none (FR-015), a language change
   does not re-scope the panel (FR-034a, D10), and the round trip carries it purely to land the editor
   back where they came from (FR-022a).

9. **The editor already fetches an experiment on every page load, and already shows a badge for it.**
   `DotEmaRunningExperimentComponent` sits in the toolbar and renders a "running until …" tag whenever
   the page has a running experiment. It is fed by the page load itself: `withPageApi` requests
   `getById(pageParams.experimentId ?? pageAsset.runningExperimentId)` alongside the page and the
   languages. Two consequences, and both correct earlier drafts of this spec:
   - A "**zero** experiments requests when the panel is never opened" budget is not achievable and
     never was — that request predates this work. The budget is that the panel adds **no further**
     requests until it is opened (FR-036, SC-004).
   - That badge is a **second entry point into experiments**, and it deep-links to the legacy
     per-page results route, so it ejects the editor exactly the way the nav item used to. Leaving it
     alone would ship a panel that does not eject beside a badge that does, and #37008 deletes the
     route underneath it. It is brought into scope (D14).

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See this page's experiments without losing the page (Priority: P1)

An editor is working on a page in the visual editor. They want to know whether this page has
experiments, and what state they are in. They open Experiments and the answer appears beside the
page they are editing. The page is still there — still rendered, still scrolled where they left
it — and closing the panel puts them back exactly where they were, with nothing to navigate back
through.

**Why this priority**: this is the whole point of the work. Everything else in this spec either
supports it, protects it, or extends it. It is also the smallest thing worth shipping.

**Independent Test**: with the flag on, open a page with experiments in the editor, open
Experiments, confirm the page is still rendered and has not reloaded, confirm the listed
experiments are that page's, close the panel, confirm the page is untouched.

**Acceptance Scenarios**:

1. **Given** an editor on a page with three experiments and the flag on, **When** they open
   Experiments, **Then** a panel appears beside the page listing those three, and the page remains
   rendered and unreloaded behind it.
2. **Given** that panel is open, **When** the editor closes it, **Then** the page is exactly as it
   was — same content, same scroll position, no reload, nothing to navigate back through.
3. **Given** experiments exist on other pages of the site, **When** the panel is open, **Then**
   none of them appear in it.
4. **Given** an editor opens Experiments, **When** the panel renders, **Then** the browser address
   is unchanged from what it was before the gesture.
5. **Given** the page's experiments are still being retrieved, **When** the panel opens, **Then**
   it appears immediately in a loading state rather than waiting, and rather than showing an empty
   list.

---

### User Story 2 - Start testing a page that has no experiments yet (Priority: P1)

An editor opens Experiments on a page that has never been tested. The panel opens on the list, as
it always does, and the list tells them plainly that this page has none and offers to create the
first one. They create it when they decide to — and the experiment is for the page they are
standing on, which they are never asked to name and cannot change.

**Why this priority**: no experiments is the state most pages are in, so this is the panel's
most-seen opening. It is P1 with US1 because a panel whose most common answer is a dead end has
not delivered the entry point.

**Independent Test**: on a page with no experiments, open the panel, confirm it opens on the list
showing an empty state with a create action, take that action, confirm the page cannot be changed
and says why, create the experiment, and confirm it belongs to that page.

**Acceptance Scenarios**:

1. **Given** a page with no experiments, **When** the editor opens Experiments, **Then** the panel
   opens on the list, showing an empty state for *this page* with an action to create the first
   experiment.
2. **Given** that empty state, **When** the editor takes the create action, **Then** the creation
   screen opens in the panel with the page still rendered.
3. **Given** the creation screen, **When** the editor looks at the page field, **Then** the page is
   shown as settled context, there is no way to change it or pick another, and a hint explains why
   it is fixed.
4. **Given** the editor completes creation, **When** the experiment is created, **Then** it belongs
   to the page open in the editor, which they were never asked to identify.
5. **Given** the experiment is created, **When** creation finishes, **Then** the editor continues
   into its configuration in the panel, with the page still rendered.

---

### User Story 3 - Build and verify this without disturbing what ships (Priority: P1)

While this work is in progress and under QA, the flag keeps it off the path everyone else is on.
With the flag off the Experiments entry point still leads to the per-page screens it always led to,
and nothing about the panel is reachable, visible, or costly. That holds until QA signs off on the
whole of it, at which point the flag and the screens it guards are removed together — by #37008
(migration), not here.

**Why this priority**: this is what makes the work safe to merge before it is finished, and it is
what lets phases 2 and 3 land incrementally. It has to hold not only when the flag is off but when
reading it fails, because a flag that fails open would expose unfinished work by accident.

**Note on the flag's nature**: it is a **development and QA gate**, not a long-lived customer
switch. Nobody is expected to run with it off permanently once the work is done, and no promise is
made about the two behaviors coexisting in the field. See D15 — this is a deliberate correction to
how earlier drafts framed it, and it needs reconciling with #37005's own spec.

**Independent Test**: with the flag absent, with it explicitly off, and with the read failing,
exercise the Experiments entry point and confirm identical behavior to the current build in all
three cases.

**Acceptance Scenarios**:

1. **Given** the flag is off, **When** the editor activates Experiments, **Then** the legacy
   per-page screens open exactly as they do today and no panel exists.
2. **Given** the flag key is absent from the instance entirely, **When** the editor activates
   Experiments, **Then** the legacy behavior applies — the absence is *not* read as "on".
3. **Given** the request that reads the flag fails, **When** the editor activates Experiments,
   **Then** the legacy behavior applies.
4. **Given** the flag is off, **When** a page is loaded in the editor, **Then** no experiments
   request is made beyond the one the editor already makes today for the toolbar badge.

---

### User Story 4 - Configure an experiment without losing the page (Priority: P2)

An editor opens an experiment from the panel and edits it — its details, goal, variants, schedule
and traffic split — with the page still rendered beside them, so they can see what they are
experimenting on while they describe the experiment.

**Why this priority**: this is phase 2, and it is the half of the value phase 1 cannot deliver. P2
rather than P1 only because phase 1 stands without it.

**Independent Test**: with phase 2 in place, open an experiment from a panel row, change every kind
of field the full-screen screen offers, confirm each behaves identically, and confirm the page never
left the screen.

**Acceptance Scenarios**:

1. **Given** the panel lists an experiment, **When** the editor opens it, **Then** its configuration
   renders in the panel and the page stays rendered beside it.
2. **Given** the editor edits a field in the panel, **When** the edit settles, **Then** it is
   persisted by the same rules as the full-screen screen — same autosave behavior, same validation,
   same state transitions.
3. **Given** the editor is configuring in the panel, **When** they go back, **Then** they land on
   the panel's list for the same page, with the page still rendered.
4. **Given** the experiment is in a state where the full-screen screen is read-only, **When** it is
   opened in the panel, **Then** it is read-only there too, and says why.
5. **Given** the full-screen screen protects unsaved work when it is left, **When** the panel's
   configuration is closed with unsaved work, **Then** the same protection applies.

---

### User Story 5 - Edit a variant, and come back to where you were (Priority: P2)

An editor configuring an experiment wants to see or change what a variant actually looks like. They
choose Preview or Edit Content on that variant; the panel closes and the page is shown as that
variant, with a banner naming the variant and the experiment. When they are done, one control brings
them back — and the panel reopens on **that experiment's configuration**, exactly where they left
it. They do not land on the list, and they do not start the configuration over.

**Why this priority**: this is the step that makes configuring in the panel worth doing rather than
merely possible. Configuring an experiment means looking at its variants, and an editor who loses
their place every time they look has gained nothing over the full-screen screen.

**Independent Test**: from a panel configuration, open a variant for editing, make a change, return,
and confirm the panel reopens on the same experiment's configuration with the work intact. Repeat
for a preview and for the control variant, which return without saving.

**Acceptance Scenarios**:

1. **Given** the editor is configuring an experiment in the panel, **When** they choose Edit Content
   on a variant, **Then** the panel closes and the page is shown as that variant for editing.
2. **Given** they are editing a variant, **When** they look at the page, **Then** a banner names the
   variant and the experiment and says the changes apply only to that variant.
3. **Given** they are editing a variant, **When** they take the return control, **Then** the panel
   reopens on the **same experiment's** configuration — not the list, not another experiment — with
   the configuration they had.
4. **Given** they chose Preview rather than Edit Content, **When** they are on the page, **Then** it
   is read-only and says so, and returning saves nothing.
5. **Given** the variant is the control, or the experiment is in a read-only state, **When** they are
   on the page, **Then** it is read-only and says which of the two reasons applies, and the return
   control returns them without saving.
6. **Given** they returned from a variant, **When** they continue configuring, **Then** nothing about
   the sequence has been reset — the same experiment, the same screen, the same state.

---

### User Story 6 - See how the running experiment is doing, on the page it is running on (Priority: P2)

An editor is on a page that has an experiment running — the toolbar already tells them so with a
badge. They want the numbers. The badge opens the panel on that experiment's results, beside the
page it is testing, and closing the panel puts them back on the page.

**Why this priority**: the badge is the one entry point into experiments that appears without the
editor going looking for it, and today it is the one that most abruptly throws the page away. P2
alongside configuration because it is phase 3 and the panel stands without it, but it is the step
that finishes the promise — after it, no route into experiments ejects the editor.

**Independent Test**: on a page with a running experiment, click the toolbar badge, confirm results
render in the panel with the page still on screen, close the panel, confirm the page is untouched.
Then reach the same results from configuration and confirm they render the same way.

**Acceptance Scenarios**:

1. **Given** a page with a running experiment, **When** the editor clicks the toolbar badge, **Then**
   the panel opens on that experiment's results and the page stays rendered — the editor is not
   ejected and the iframe is not reloaded.
2. **Given** the editor is configuring an experiment in the panel, **When** they open its results,
   **Then** the results render in the panel, and going back returns them to that configuration.
3. **Given** results are open in the panel, **When** the editor closes it, **Then** the page is
   exactly as it was.
4. **Given** the flag is off, **When** the editor clicks the toolbar badge, **Then** it behaves
   exactly as it does today.

---

### User Story 7 - Get to the whole portlet when one page is not enough (Priority: P3)

An editor who is thinking about the site rather than the page wants the full list — every experiment,
unfiltered. The panel gives them a way there that does not cost them the page they are on.

**Why this priority**: this is #37005's FR-021c, resolved honestly. P3 because the panel answers the
common question by itself; this is the escape hatch for the uncommon one.

**Independent Test**: from an open panel, take the way out to the full portlet, and confirm the
unfiltered list is reached and the editor's page is still there.

**Acceptance Scenarios**:

1. **Given** the panel is open, **When** the editor takes the way out to the full portlet, **Then**
   the site-wide, unfiltered list is reached.
2. **Given** they have taken it, **When** they look at the editor, **Then** the page is still
   rendered and was never ejected.

---

### Edge Cases

- **The page on the canvas changes while the panel is open.** The editor navigates to another page
  without closing the panel. The panel re-scopes to that page (D10). It must never be possible for
  the panel to describe a page other than the one being edited. If the new page has no experiments,
  the list shows the empty state for *that* page. The old page's search, filters, sort and open
  experiment are discarded rather than carried across (FR-034b).
- **The language on the canvas changes while the panel is open.** Experiments belong to a page, not
  to a language version of it, so the panel's contents do not change and it MUST NOT refetch or
  reset. What updates is the language the panel hands to the round trip and to creation as return
  context (D10, FR-022a).
- **The page's experiments are still loading.** The panel appears immediately in a loading state.
  It never shows an empty list, and never delays its own opening, while a request is in flight.
- **The page's experiments fail to load.** The panel reports the failure and offers a retry. A failed
  load is never presented as the empty state — "we could not ask" and "there are none" send the
  editor in different directions.
- **Analytics is misconfigured.** The list is gated on an analytics health check. The panel says so
  plainly rather than rendering as an empty list, which would read as "this page has no experiments"
  and invite the editor to create one that cannot be measured.
- **The last experiment on the page is deleted or archived from the panel.** The list falls back to
  its empty state, with the create action, rather than to a blank panel.
- **The toolbar badge is clicked while the panel is already open on something else.** The panel
  switches to that experiment's results; the badge always means "show me the running experiment",
  and it does not open a second surface (FR-025c, one panel at a time).
- **An experiment stops running while the panel shows its results.** The badge is fed by the page
  load, so it does not change until the page does. The panel is not required to notice mid-session;
  a reopen reflects reality.
- **The editor lacks permission to edit the page.** The entry point is governed by the same rule as
  today — no one reaches experiments through the panel that they could not reach through the entry
  point before it.
- **The editor closes the panel while editing a variant is pending, or reloads the browser mid
  round-trip.** The return path is defined only for the round trip taken from the panel; a browser
  reload while on the variant returns the editor to the page, not to a phantom panel state.
- **The panel is opened, closed, and opened again.** The second open shows current data, not what was
  on screen at the first close, and leaves nothing from the first open running.
- **The editor uses browser Back while the panel is open.** Because the panel writes nothing to the
  address, Back means what it meant before the panel existed. It does not step through the panel's
  search or filter changes.
- **A very long experiment name, or many experiments on one page.** The row truncates rather than
  reflowing the panel, and the list remains navigable past the first screenful.

---

## Requirements *(mandatory)*

### Functional Requirements

#### A. The entry point

- **FR-001**: With the flag on, activating the Universal Visual Editor's Experiments entry point
  MUST open a panel alongside the page canvas. The page MUST remain rendered and MUST NOT reload.
- **FR-002**: With the flag on, the Experiments entry point MUST behave as an action rather than a
  destination: activating it MUST NOT navigate, MUST NOT activate a route, and MUST NOT change the
  browser address.
- **FR-003**: The panel MUST be dismissible by the editor, and dismissing it MUST leave the page
  exactly as it was — same content, same scroll position, no reload.
- **FR-004**: The Experiments entry point MUST remain subject to the same visibility and permission
  rules as it is today, on both sides of the flag. No editor may reach experiments through the panel
  that they could not reach through the entry point before it (inherits #37005 FR-023).
- **FR-005**: The flag MUST be read fail-closed. An absent key, any value other than an explicit
  affirmative, and a failed read MUST each leave the pre-change behavior in place. The flag MUST NOT
  be read through any mechanism that treats an absent key as enabled.

#### B. What the panel opens onto

- **FR-006**: The panel MUST open on the list of exactly that page's experiments, whether or not the
  page has any, and MUST NOT list any other page's.
- **FR-006a**: When the page has no experiments, the list MUST show an empty state scoped to *this
  page*, carrying an action to create the first one. The panel MUST NOT navigate the editor to
  creation on its own — creating is the editor's gesture, not the panel's.
- **FR-007**: Taking the create action MUST open the creation screen in the panel, with the page
  still rendered.
- **FR-008**: From the list the editor MUST be able to select an experiment and open its
  configuration.
- **FR-009**: Each row MUST carry at minimum the experiment's name and its current status. The page
  column MUST NOT be shown — the panel is the page.
- **FR-010**: The panel MUST retrieve the page's experiments in a single request scoped to that page.
  It MUST NOT retrieve the site-wide set, and MUST NOT perform a page-resolution lookup to narrow the
  result.
- **FR-011**: Row actions offered by the full-screen list MUST be available from a panel row where
  they apply to a page-scoped list, or be deliberately absent with a stated reason recorded in the
  plan. In particular, Push Publish and Add to Bundle MUST work from a panel row.
- **FR-012**: The panel MUST offer an explicit way to the full, unfiltered portlet. It MUST open in a
  **new browser tab**, so the editor's page is not ejected and remains rendered (D6).
- **FR-013**: Until phase 2 lands, opening an experiment from a panel row MUST open the full-screen
  configuration screen, and the editor MUST be able to return from it to the page they came from.

#### C. The page is fixed

- **FR-014**: In creation and in configuration rendered inside the panel, the experiment's page MUST
  be the page open in the editor, presented as settled context. There MUST be no page picker, no
  page field the editor can edit, and no way to change the page — including any confirmation flow
  that would delete variants in order to change it.
- **FR-014a**: The page field MUST carry a hint explaining **why** it cannot be changed — that the
  experiment belongs to the page being edited. Refusing the interaction without explaining it reads
  as a defect; the editor must be able to tell "fixed by design" from "broken".
- **FR-015**: Creating an experiment from the panel MUST create it for the page open in the editor,
  without asking the editor to identify it. An experiment belongs to a page, not to a language
  version of it, so no language is chosen, stored or implied by creation.
- **FR-016**: Once creation completes, the editor MUST continue into that experiment's configuration
  within the panel, without the page being lost.

#### D. Configuration in the panel *(phase 2)*

- **FR-017**: An experiment's configuration rendered in the panel MUST edit, persist and transition
  exactly as the full-screen configuration screen does — same fields, same validation, same autosave
  behavior, same state transitions. Only the layout differs, and FR-014 removes the page control.
- **FR-018**: Moving between the panel's list and the panel's configuration MUST NOT eject the editor
  or reload the page.
- **FR-019**: Any protection the full-screen configuration screen gives unsaved work when it is left
  MUST hold equally when the panel's configuration is closed.
- **FR-020**: An experiment that the full-screen screen renders read-only MUST be read-only in the
  panel, and the panel MUST say why.

#### E. The variant round-trip

- **FR-021**: From a variant in the panel's configuration, Preview and Edit Content MUST each take
  the editor to that variant of the page in the editor. The panel closes; the page is not lost.
- **FR-022**: While the editor is on a variant, they MUST be told which variant of which experiment
  they are looking at, and whether what they are seeing is editable or read-only — and, if read-only,
  which reason applies: a preview, the unmodified control, or an experiment whose state forbids
  editing.
- **FR-022a**: Leaving for a variant and returning MUST land the editor on the **language version of
  the page they came from**. The language is carried as return context only — it does not select the
  experiment, narrow the panel, or become part of what is created or saved. It is carried because a
  page identifier alone does not say which version was open, and guessing shows the wrong content.
- **FR-023**: A single explicit return control MUST bring the editor back and **reopen the panel on
  the same experiment's configuration** — the same experiment, the same screen. It MUST NOT return
  them to the list, to another experiment, or to a restarted configuration.
- **FR-024**: Returning from a read-only visit (a preview, the control variant, or a read-only
  experiment) MUST persist nothing.
- **FR-025**: The configuration the editor returns to MUST be in the state they left it in. The
  round trip MUST NOT discard in-progress configuration work.

#### F. Results, and the toolbar badge *(phase 3)*

- **FR-025a**: An experiment's results MUST render in the panel, beside the page they are measuring.
  Opening them MUST NOT eject the editor or reload the page.
- **FR-025b**: Results MUST be reachable from the panel's configuration, and going back from them
  MUST return to that configuration — the same experiment, the same state (the FR-023 rule, applied
  to this transition).
- **FR-025c**: With the flag on, the toolbar's running-experiment badge MUST open the panel on that
  experiment's results. It MUST NOT navigate, MUST NOT change the browser address, and MUST NOT
  reload the page. Its existing deep link to the legacy full-screen results route MUST NOT be used
  while the flag is on.
- **FR-025d**: With the flag off, the badge MUST behave exactly as it does today.
- **FR-025e**: Results rendered in the panel MUST present the same measurements as the full-screen
  screen. Charts MUST remain legible at panel width, or the panel MUST widen for this screen — the
  choice is O1's, and it MUST NOT be resolved by silently omitting a measurement.
- **FR-025f**: The charting dependency Results carries MUST NOT be loaded until results are actually
  opened. It MUST NOT enter what the editor loads before the panel is opened (FR-037), and it MUST
  NOT be loaded merely by opening the panel on the list.
- **FR-025g**: Until phase 3 lands, results MUST continue to open full-screen as they do today, and
  the editor MUST be able to return from them to the page they came from.

#### G. States

- **FR-026**: While the page's experiments are being retrieved, the panel MUST show a loading state.
  It MUST NOT show the empty state, and MUST NOT delay opening.
- **FR-027**: When analytics is not correctly configured, the panel MUST say so, and MUST NOT render
  as an empty list — "misconfigured" and "this page has no experiments" are different answers and
  lead the editor to different actions.
- **FR-028**: When the page's experiments fail to load, the panel MUST report the failure and offer a
  retry. A failed load MUST NOT be presented as an empty page.
- **FR-029**: The empty state MUST mean exactly "this page has no experiments" and nothing else. It
  is a resting state of the panel (FR-006a), which is precisely why loading, error and
  analytics-misconfigured MUST each be distinguishable from it (FR-026, FR-027, FR-028).
- **FR-030**: Every read-only condition — a locked or otherwise non-editable experiment, a preview, an
  unmodified control variant — MUST be **stated** to the editor, not merely enforced by disabling
  controls.

#### H. Isolation from the editor

- **FR-031**: Nothing the panel does MAY write to the editor's browser address — not its search term,
  not its status narrowing, not its sort, not its page, not the fact that it is open, not which
  experiment it is showing.
- **FR-032**: Nothing the editor writes to its own address MAY reset or discard the panel's view
  state.
- **FR-033**: The panel MUST NOT contribute a breadcrumb. Inside the editor the trail belongs to the
  page.
- **FR-034**: If the **page** on the canvas changes while the panel is open, the panel MUST re-scope
  to that page (D10), including re-evaluating FR-006 for it.
- **FR-034b**: On a page re-scope the panel MUST reset the view state that belongs to the old page —
  its search term, status narrowing, sort and paging, and any experiment it had open — and MUST NOT
  carry them onto the new page. Filters left over from another page silently misrepresent the new
  one as having fewer experiments than it does.
- **FR-034a**: If only the **language** on the canvas changes, the panel's contents MUST NOT change:
  experiments belong to a page, not to a language version of it, so the same experiments apply. The
  panel MUST NOT refetch and MUST NOT reset its view state. Only the return context of FR-022a
  updates.
- **FR-035**: Browser Back and Forward MUST mean, while the panel is open, exactly what they meant
  before the panel existed. They MUST NOT step through the panel's view-state changes.

#### I. Cost and fluidity

- **FR-036**: A page loaded in the editor without the panel ever being opened MUST make **no
  experiments request beyond the one the editor already makes today** for the toolbar badge — no
  list, no health check, no page lookup. The pre-existing badge request is untouched by this work
  and MUST NOT be duplicated by the panel: if the panel needs that experiment, it reuses what the
  editor already holds.
- **FR-037**: The experiments screens MUST NOT be part of what the editor loads before the panel is
  first opened. They MUST be retrieved on first open, and the charting dependency only when results
  are opened (FR-025f).
- **FR-038**: Loading the experiments screens MUST NOT degrade the fluidity of the editor: the panel
  MUST respond to the gesture immediately with a loading state, the page MUST NOT stutter, reflow or
  reload while it loads, and no editor interaction MUST be blocked waiting for it.
- **FR-039**: Closing the panel MUST leave nothing of it running — no live subscription, no
  outstanding effect, no retained view state that a later open would show as current.

#### J. One implementation, two presentations

- **FR-040**: The portlet's existing list and configuration screens MUST be reused for the panel and
  adapted to render in both contexts. A parallel copy of either screen MUST NOT be created.
- **FR-041**: The presentation MUST adapt to the context rather than the behavior: at panel width the
  list MUST NOT use the full-screen table layout, which cannot render there. The screen, its state and
  its rules are shared; the layout is what varies.
- **FR-042**: The full-screen portlet reached from the main navigation MUST keep its current behavior
  — its table, its columns, its address-backed view state, and its page column.

#### K. Flag off — the development gate

- **FR-043**: With the flag off, the Experiments entry point, the per-page screens behind it and the
  toolbar badge MUST behave exactly as they do in the current build, verified by both automated tests
  and an end-to-end regression.
- **FR-044**: With the flag off, none of the panel's behavior MUST be reachable or observable.
- **FR-045**: The full portlet MUST remain reachable and unfiltered from the main navigation,
  regardless of the flag's value (inherits #37005 FR-026).
- **FR-046**: The variant Edit Content round-trip delivered by #37005 — editing a variant's content in
  the editor and returning to the originating experiment's configuration — MUST keep working
  unchanged, regardless of the flag's value (inherits #37005 FR-027). With the flag on, FR-023 is how
  that return is satisfied.
- **FR-047**: Both the legacy per-page screens and the portlet screens MUST remain present and
  functional in the same build **for as long as the flag exists**. This work removes neither set of
  screens, and does not remove the flag: that is #37008's job, and it takes all three together (D15).
- **FR-048**: The flag MUST NOT acquire behavior that would make it worth keeping permanently. No
  capability may be offered on one side of it and withheld on the other beyond the transition this
  work is making — the two sides are "before" and "after", not two supported products (D15).

### Key Entities

- **Experiment**: a test defined against one **page**. Carries a name, a status, a goal, variants, a
  schedule and a traffic allocation — and no language: an experiment is not scoped to a language
  version of its page. The panel lists it and, in phase 2, creates and edits it. Nothing about the
  entity changes in this work.
- **Variant**: one arm of an experiment, including the unmodified control. It is what the editor
  previews or edits on the page, and what the round trip in Section E is about.
- **The page in hand**: the page currently open in the editor. This is the panel's scope and, inside
  the panel, the experiment's page. It is held by the editor already; the panel is given it rather
  than deriving it, and cannot change it.
- **The editor's return context**: the language version of the page the editor was standing in. It is
  *not* part of the experiment and never narrows the panel's contents. It exists only so the round
  trip in Section E and any prefill land the editor back on the version they came from, instead of
  guessing — a page identifier says nothing about which version was open.
- **Panel view state**: what the editor has done *within* the panel — the search term, status
  narrowing, sort and paging on the list; which experiment is open and how far its configuration has
  got. It exists for as long as the panel's session does, survives the round trip in Section E, is not
  part of any address, and is not shared with the full-screen list.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In 100% of panel openings — whether the page has experiments, one, many, or none — the
  page being edited is still on screen when the panel has finished answering, and the editor did not
  have to navigate back to it.
- **SC-002**: An editor goes from looking at a page to looking at that page's experiments in **one
  gesture**, and back in one more — with no intermediate screen and no reload of the page.
- **SC-003**: On a page with no experiments, the editor reaches the creation screen in **two
  gestures** from the entry point — open the panel, take the create action — and is never asked
  which page it is for.
- **SC-003a**: Every route into experiments from the editor keeps the page on screen: the nav item,
  the toolbar badge, a row's configuration, and results. After phase 3, **zero** of them eject.
- **SC-004**: A page loaded in the editor where the panel is never opened makes **no experiments
  request beyond the single pre-existing badge request**, measured over a full editing session — the
  panel adds none.
- **SC-005**: Opening the panel makes **exactly one** request for the page's experiments (plus at most
  one analytics health check). It does not retrieve experiments belonging to other pages.
- **SC-006**: What the editor loads before the panel is first opened does not grow, measured before and
  after against the same build configuration.
- **SC-007**: The panel responds to the entry-point gesture with visible feedback in the same
  interaction, and the page behind it neither reloads nor visibly reflows while the panel loads.
- **SC-008**: Every one of loading, error, analytics-misconfigured and read-only is distinguishable by
  the editor from "this page has no experiments" — 0 cases where one is shown in place of another.
- **SC-009**: With the flag off, absent, or unreadable, behavior is the behavior of the current build in
  all three cases — 0 regressions across the automated and end-to-end suites.
- **SC-010**: Across a session in which the editor changes device, orientation, preview mode and every
  other view setting the editor writes to its address, the panel's view state survives **every** one of
  them — 0 resets.
- **SC-011**: Nothing the panel does appears in the browser address, and Back from an open panel goes
  where it went before the panel existed — verified over a session with at least 10 panel view-state
  changes.
- **SC-012**: The breadcrumb trail shown while the panel is open is identical to the trail shown with
  the panel closed.
- **SC-013**: After phase 2, an editor edits every variant of an experiment in turn and returns from
  each, and lands back on that experiment's configuration **every** time — 0 landings on the list, on
  another experiment, or on a restarted configuration.
- **SC-014**: After phase 2, an editor changes an experiment's goal, variants, schedule and traffic
  split without the page leaving the screen once.
- **SC-015**: The list and configuration screens have **one** implementation each, serving both the
  portlet and the panel — 0 duplicated screens.
- **SC-016**: Closing the panel leaves nothing running: a session of 10 open/close cycles shows no growth
  in outstanding subscriptions or effects, and the 10th open shows current data rather than the 9th
  close's.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the Universal Visual Editor's Experiments entry point, and the
  Experiments portlet's list and configuration screens. Both are current-generation surfaces, not the
  older product surface — but the *per-page* experiments screens they are replacing are, and those stay
  live and untouched behind the flag until #37008 retires them. This is frontend work only: no
  server-side capability is added, removed or changed, and the page-scoped read the panel uses already
  exists and is already served.
- **Backward-compatibility expectations**: total while the work is in flight, and the flag is what
  buys that. With it off — the shipped default, and the state of every instance until QA signs off —
  nothing observable changes: the entry point, the per-page screens and the toolbar badge all behave
  as they do today. Experiments already defined keep running and keep being served to visitors
  exactly as before; nothing about how experiments reach visitors is touched, and the full-screen
  portlet reached from the main navigation is unchanged (FR-042).

  What is *not* promised is permanence. The flag is a development and QA gate, not a switch anyone is
  expected to keep flipping (D15): once this lands and is approved, the flag and the legacy per-page
  screens are removed together by #37008. So the per-page screens are on their way out — this work
  does not deprecate them, but it is the work that makes deprecating them possible, and nothing here
  should be built as though both behaviors will coexist indefinitely (FR-048).
- **Known related decisions**: #37005's spec, decision D2 — the entry point means "the experiments for
  this page" — is the premise this inherits and does not reopen. #37005's decision to read this flag
  through a dedicated fail-closed reader rather than the editor's batched flag read is also inherited,
  and is restated here as FR-005 because the natural implementation would silently invert it. #37005's
  variant round-trip is the flag-off form of Section E and must keep working (FR-046). #37007 owns the
  server-side list contract and touches the same read this panel adopts (D4); #37008 owns the migration
  that eventually deletes what the flag currently guards. The plan phase will formally consult
  `dotCMS/platform-adrs`.

## Assumptions

- **The panel's width, phase 1**: the width already used by the editor's right-hand panel
  (`clamp(360px, 20vw, 500px)`) is the starting point, chosen because it is the sidebar width the
  template already has rather than because it was measured against the panel's row. It is explicitly
  provisional — if the row does not read well at that width, the width changes, and that change is not a
  scope change. The widths for configuration and results are a separate, open decision — see O1.
- **The row's content beyond name and status**: goal, schedule and last modification are expected to
  collapse into a subline or be dropped. Exactly which survive is a design detail for the plan, bounded
  by FR-009 and by the panel width above.
- **Panel view state does not persist across closes.** Closing the panel discards it; the next open
  starts from FR-006. This follows from FR-039, and it is what makes the panel's lifecycle simple
  enough to guarantee FR-036. The variant round trip in Section E is *not* a close — the panel's state
  survives it by design (FR-025).
- **One panel at a time.** The panel is a single surface showing the list, a creation screen, a
  configuration, or results — never two of them side by side, and never a second panel.
- **The editor already holds the page in hand.** The panel is given the page's identifier rather than
  resolving it, which is what makes FR-010's single request and FR-015 possible. The editor's current
  language comes along as return context (FR-022a), not as scope.
- **Automated coverage is the primary gate; an end-to-end regression covers the flag-off guarantee**
  (FR-043) and the round trip (FR-023). No new server-side test surface is expected, because no
  server-side code changes.
- **The three phases may land in separate commits within the same pull request**, or in their own.
  Either is acceptable; FR-013 and FR-025g are what make the interim states coherent.
- **The design canvas is a reference, not a contract** — but the two places this spec previously
  departed from it were both reversed on review (D2, D11), so it currently governs nothing this spec
  contradicts. The one remaining difference is the change-page dialog, which is a consequence of the
  panel's scope rather than a disagreement (D12).

## Dependencies

- **#37005** — the flag itself, the page-scoped destination, the fail-closed flag reader, and the variant
  Edit Content round-trip all land there. This work is stacked on it and must not regress it (FR-046).
- **#37007** — the server-side list contract. It owns the same page-scoped read this panel adopts (D4).
  The two must agree on that read rather than each changing it independently.
- **#37008** — the migration. It is made simpler by this work, not harder: with the panel as the entry
  point, the migration also removes the editor's experiments route and the legacy branch of the entry
  point, and no full-page eject survives anywhere.

## Out of Scope

- Any change to how the editor edits, saves, locks or renders content, beyond telling the editor which
  variant they are on (FR-022).
- Any change to how experiments are served to visitors, or to how their results are collected — the
  measurements Results renders are unchanged, only where they render (D2).
- Removing the flag, or removing the legacy per-page screens — both belong to #37008, and D15 is
  explicit that they go together.
- Any change to the full-screen portlet's behavior when reached from the main navigation (FR-042). It
  keeps its table, its columns, its page column and its address-backed view state.
- Changing an experiment's page from anywhere. Inside the panel it is forbidden (FR-014); in the
  full-screen portlet it is unchanged from today.
- Live cross-tab or cross-session consistency of the panel's contents.
- Any change to the analytics health check itself.

## Resolved Decisions

### D1 — The experiments appear beside the page; the entry point stops being a destination

**Decision**: With the flag on, the Experiments entry point opens a panel alongside the canvas rather
than navigating anywhere. → FR-001, FR-002, FR-003.

**Why**: the gesture already means "the experiments for this page" (#37005 D2). Answering it by taking
the page away is the one part of that answer that costs the editor something, and it is the only part
this work changes.

**Cost accepted**: the entry point loses its highlighted-destination behavior, and the route exemption
that exists to let the editor navigate to the portlet becomes unnecessary for this path.

### D2 — All three screens move into the panel, in three phases

**Decision**: The list (with its empty state and creation) moves into the panel first, configuration
second, results third. One work item, three phases. → Scope Note, FR-013, FR-025a … FR-025g.

**Why**: the goal is that *no* route into experiments costs the editor the page. Two of three screens
leaves that unfinished, and the screen left behind is the one an editor is most often pushed into
without asking — the toolbar badge puts it in front of them whenever an experiment is running (D14).

An earlier draft kept Results full-screen, reasoning that charts need width and that Results carries
the portlet's heaviest dependency. Both facts are still true, but neither is a reason to eject:
width is a layout question that O1 already has to answer for configuration, and the dependency is a
loading question that `@defer` already answers everywhere else in this editor — FR-025f requires it
to stay out of the panel's cost until results are actually opened. The design canvas models all
three screens in the drawer, and reviewers asked for the same.

The three phases stay one work item because the plumbing (D3, D4, D5, D7) is shared and phases 2 and
3 change none of it.

**Cost accepted**: interim states in which listing is cheap but configuring and reading results still
cost the page — FR-013 and FR-025g make those states coherent rather than broken. Results also
inherits O1's width question, which is now a question about two screens rather than one.

### D3 — The panel's view state does not live in the browser address

**Decision**: In the panel, the list's search, filters, sort and paging — and which experiment is open —
are held in memory and seeded from the page in hand, not read from or written to the address.
→ FR-031, FR-032, FR-035.

**Why**: the address already has an owner inside the editor, and that owner replaces the query string
rather than merging into it. Two writers on one address is not a layout problem that can be worked
around in a template — the panel's state would be silently dropped by the editor's next view change, and
the editor's state would be perturbed by the panel's. There is no version of this that works while the
panel writes to the address.

**Cost accepted**: this is the largest piece of work in phase 1, and it is a change to how the list's
state is managed — the full-screen list must keep its address-backed behavior (FR-042) while the panel
gets an address-free one. The plan owns how.

### D4 — The panel asks for one page's experiments, not for all of them

**Decision**: The panel makes a single page-scoped request and does no page-resolution lookup.
→ FR-010, SC-005.

**Why**: the full-screen list retrieves every experiment on every site and narrows in the browser because
it has no page to scope to. The panel does have one — the editor is holding it. The page-scoped read
already exists and is already served, so this is an adoption, not a new contract.

**Cost accepted**: the panel and the full-screen list no longer make the same request, so they can
diverge. #37007 owns that read and is the place to keep them honest.

### D5 — The flag read stays fail-closed

**Decision**: The panel does not join the editor's batched feature-flag read. The flag continues to be
read through a reader that treats an absent key, a non-affirmative value and a failed read all as "off".
→ FR-005.

**Why**: the batched read maps "key not found" to *enabled*, which is right for flags that ship on and
wrong for this one. #37005 refused that batch for exactly this flag, which sits beside the visitor-facing
kill switch, and nothing about moving the screens into a panel changes that reasoning. The editor already
makes this read for the entry point itself, so the panel adds no request by keeping it.

**Cost accepted**: one flag stays outside the batch. If the batched read ever gains a fail-closed variant,
this flag may move to it — the requirement is the behavior, not the mechanism.

### D6 — "Clearable" becomes a way out to the whole portlet, in a new tab

**Decision**: The panel offers an explicit way to the full, **unfiltered** portlet, and it opens in a
**new tab** so the editor's page is not ejected. → FR-012, US7.

**Why**: FR-021c of #37005 asked for a clearable page filter. In a page-scoped panel there is no filter
to clear — the scope is structural. What the requirement was actually protecting is the editor's ability
to get from "this page" to "everything", and a new tab delivers that without reintroducing the eject this
work exists to remove. The unfiltered list is the honest destination: a page-filtered one would not be
"clearing" anything.

**Cost accepted**: a second browser tab, and that tab has no back-link to the page. Both are acceptable
for a deliberate, uncommon gesture; the common question is answered in the panel.

### D7 — One implementation of each screen, two presentations

**Decision**: The portlet's list and configuration screens are reused for the panel and made to render in
both contexts. No parallel panel-only copy of either. The layout adapts; the state and the rules do not.
→ FR-040, FR-041, FR-042, SC-015.

**Why**: these are the screens the portlet work already built, and two implementations of the same screen
diverge — a fix or a rule added to one silently missing from the other is the failure mode, and it is
invisible until a user hits it. The design canvas models exactly this: its list and configuration markup
is selected by a mode flag rather than duplicated.

**Cost accepted**: the screens grow a presentation mode, and the full-screen list's table layout — which
has a hard floor of 81rem and cannot render at panel width — is the largest thing that has to become
mode-dependent. The page column is dropped in panel mode because the panel *is* the page.

### D8 — The panel contributes no breadcrumb

**Decision**: While rendering inside the editor, the experiments screens push nothing onto the breadcrumb
trail. → FR-033, SC-012.

**Why**: the trail describes where the editor is, and inside the editor that is the page. A crumb for a
panel that is not a location would make the trail describe something the address does not, and Back would
then disagree with it.

### D9 — Push Publish and Add to Bundle need no route plumbing

**Decision**: Both actions work from a panel row with no resolver provided to the panel. → FR-011.

**Why**: this contradicts issue #37478, which states that the row menu reads its push-publish environments
off the route and would break silently outside the router. Verified against the code on this branch: the
list component never reads that resolved value. Push Publish is opened through a global dialog service that
takes only an asset identifier, and Add to Bundle fetches its own bundles. The resolver on the list route
is a prefetch that nothing consumes.

**Cost accepted**: none for this work. The unused resolver on the full-screen list route is pre-existing
and out of scope here; it is worth noting for #37008.

### D10 — A page change re-scopes the panel; a language change does not

**Decision**: If the **page** on the canvas changes while the panel is open, the panel re-scopes to it
rather than closing or holding its original scope. If only the **language** changes, the panel does
nothing — same contents, no refetch, no reset. → FR-034, FR-034a, Edge Cases.

**Why**: the panel's contract is "the experiments for the page in hand", stated once and held continuously.
Closing on a page change would be defensible but discards the editor's intent for no reason; holding the
original scope would let the panel describe a page that is no longer on screen, which is the one thing
FR-007 rules out.

A language change is a different question, and the answer is the opposite one. **An experiment belongs
to a page, not to a language version of a page** — `DotExperiment` carries `pageId` and no language, and
the page-scoped read narrows on `pageId` alone. The same experiments therefore apply whichever version
the editor is standing in, so refetching would re-request an identical set and resetting would discard
the editor's view state for no change in the answer. What the language does affect is where the editor
is sent *back* to (FR-022a), which is exactly the role #37005 gave it: it carried `language_id` to the
portlet as return context and said so in as many words — "not a filter; the list narrows on `pageId`
alone".

**Cost accepted**: one page-scoped request per page change while the panel is open, and a defined reset
rule for the panel's view state when the page changes — the plan owns which parts survive a re-scope.
The language must be read live rather than captured at open, so the return context does not go stale
after a mid-session switch.

### D11 — The panel always opens on the list; creating is the editor's gesture

**Decision**: The panel opens on the list whether or not the page has experiments. When it has none,
the list shows an empty state carrying a **New Experiment** action, and the editor takes it when they
choose to. → FR-006, FR-006a, FR-007, FR-029, US2.

**Why**: the gesture is "show me this page's experiments", and the honest answer to it on an untested
page is "none yet" — not a form. Opening a creation screen answers a question the editor did not ask
and puts them in a state they have to back out of if they were only checking. It also makes the panel
inconsistent with itself: the same gesture would land on different screens depending on data the
editor cannot see beforehand.

An earlier draft did open straight on creation, on the reasoning that there is only one useful next
step on an empty page. Review rejected it, and the design canvas has always shown the empty state.
The saving was one gesture; the cost was predictability, and predictability is worth more on a
surface the editor opens many times a day.

**Cost accepted**: creating on an untested page takes two gestures rather than one. The empty state
must be genuinely actionable rather than a dead end — an empty screen that only reports emptiness
would be the failure this decision risks, which is why FR-006a requires the create action to live
in it. And because the empty state is now a resting state, loading, error and
analytics-misconfigured must each be distinguishable from it (FR-026, FR-027, FR-028) — that is why
Section G is a section rather than a footnote.

### D12 — Inside the panel, the page cannot be changed

**Decision**: Creation and configuration rendered in the panel present the page as settled context, with
no picker, no editable page field and no change-page flow. → FR-014, FR-015.

**Why**: the editor is standing on the page. The experiment being configured is that page's experiment —
that is the panel's entire scope — so a control offering to move it elsewhere contradicts the surface it
is rendered on, and the full-screen portlet's version of it exists precisely because there no page is
implied. Removing it also removes the flow behind it, in which changing the page deletes the experiment's
variants.

**Cost accepted**: an editor who genuinely wants to move an experiment to another page does it from the
full-screen portlet, which FR-042 leaves unchanged and FR-012 gives them a way to reach.

### D13 — The variant round trip returns to the configuration it left

**Decision**: Preview and Edit Content on a variant close the panel and take the editor to that variant of
the page. One return control brings them back and reopens the panel on the **same experiment's**
configuration, in the state they left it. → FR-021 … FR-025, US5.

**Why**: configuring an experiment means looking at its variants, so this trip is taken repeatedly within
a single configuration session. Returning to the list — or to a configuration that has been rebuilt from
scratch — would make the editor re-find their place every time, which costs more than the full-screen
screen did and would make configuring in the panel worse than the thing it replaces. Remembering which
experiment and which screen is therefore a requirement, not an optimisation. The design canvas models this
exactly, including the read-only cases.

**Cost accepted**: the panel's state has to survive a period during which the panel is closed, which is
the one exception to "closing discards everything" (FR-039). The plan owns where that state lives and how
it is bounded — a browser reload during the trip is explicitly not covered (Edge Cases).

### D14 — The toolbar's running-experiment badge opens the panel

**Decision**: With the flag on, the existing running-experiment badge in the toolbar opens the panel
on that experiment's results instead of deep-linking to the legacy full-screen results route.
→ FR-025c, FR-025d, US6.

**Why**: the badge is already there, and it is a second entry point into experiments that this spec
originally failed to notice. Leaving it alone would ship an editor with two doors into the same
feature that behave in opposite ways — a nav item that keeps the page and a badge that throws it
away — and the badge is the door the editor did not choose to walk through, since it appears on its
own whenever an experiment is running. It is also the more urgent of the two: it points at the
*legacy* per-page route, which #37008 deletes, so it would break on that migration regardless.

Bringing it in is what makes phase 3 worth having: after it, no route into experiments ejects the
editor, which is the whole claim of this work.

**Cost accepted**: this pulls Results into scope (D2) — a badge that opens the panel on a screen the
panel cannot render would be worse than the eject it replaces. Results therefore had to move with
it, and the two decisions stand or fall together.

### D15 — The flag is a development gate, not a product switch

**Decision**: `FEATURE_FLAG_EXPERIMENTS_PORTLET` exists to let this work be built and QA'd without
disturbing what ships. It is not a long-lived operator switch, no promise is made about the two
behaviors coexisting in the field, and it is removed — together with the legacy screens it guards —
by #37008 (migration). → US3, FR-047, FR-048.

**Why**: reviewers were explicit that after QA approves the whole, the flag is out of the picture and
both behaviors are not kept. Earlier drafts of this spec framed flag-off as a customer-facing
guarantee, with a P1 user story about "an operator who has not opted in". That framing invites
exactly the outcome the reviewers ruled out: requirements written to protect a permanent second
behavior, features added on both sides of the switch, and a flag nobody can ever remove. FR-048 is
the guard against it.

The technical requirement is unchanged and is not weakened by the reframing: flag off must still
mean today's behavior exactly, and the read must still fail closed (D5). A development gate that
fails open is worse than a product switch that does, because it exposes unfinished work by accident.

**Cost accepted, and an inconsistency to resolve outside this spec**: this **contradicts #37005's own
approved spec**, whose SC-002 promises "an operator under a minute to move between entry points
without a deployment or restart" — an operator-facing switch, on the same flag. Both specs cannot be
right. This one records the reviewed intent; #37005's SC-002 needs revisiting, and that is a change
to *that* spec, not something this one can make on its own. Flagged rather than silently diverged
from.

## Open Decisions

### O1 — The panel's width for the wide screens (phases 2 and 3)

**Deliberately unresolved.** Two screens do not fit the width phase 1 uses, for related reasons:

- **Configuration** is a multi-card form — details, goal, variants, scheduling, traffic — laid out
  for a full-width column, and its variants card is a table.
- **Results** is charts, which have a legibility floor rather than a layout floor: a line chart
  narrowed far enough stops being readable before it stops rendering, and FR-025e forbids answering
  that by quietly dropping a measurement.

Two candidates, and the answer may differ per screen:

- **A wider panel for these screens.** There is precedent: the editor's content side panel already
  varies its own width between two settings. Cost: the panel's width changes with what is in it,
  which the editor sees as motion — and with three screens in rotation that motion happens more
  often.
- **A reflow.** A single-column reflow of the configuration cards; for results, fewer series per
  chart or a stacked layout. Cost: a second layout for the portlet's two largest screens, and the
  variants table has the same width problem the list's table has (D7).

This must be decided before phase 2 is built, and revisited for phase 3. It blocks nothing in phase
1 — every requirement in Sections B, C, G, H, I, J and K stands regardless — which is why phase 1
can proceed while it is open.
