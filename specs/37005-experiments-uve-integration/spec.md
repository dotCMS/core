# Feature Specification: UVE Integration for the Experiments Portlet — Flagged Entry Point + Variant Edit Content Round-Trip

**Feature Branch**: `issue-37005-experiments-uve-integration`

**Created**: 2026-08-26

**Status**: Draft

**Type**: Task (flagged behavior change to an existing, customer-facing flow)

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37005 — Experiments Portlet — UVE integration behind FEATURE_FLAG_EXPERIMENTS_PORTLET + variant Edit Content round-trip](https://github.com/dotCMS/core/issues/37005)

**Input**: User description: "Experiments Portlet — UVE integration behind FEATURE_FLAG_EXPERIMENTS_PORTLET + variant Edit Content round-trip" — taken from issue #37005.

---

## Scope Note *(read this first)*

Every other issue in the Experiments Portlet series adds something new beside what customers use
today. This one is the first that reaches into a flow already in production: the **Experiments entry
point inside the Universal Visual Editor**. An editor working on a page opens the Experiments item
in the UVE navigation bar, sees that page's experiments, configures one, and edits a variant without
ever leaving the page context. That flow ships today and works.

The new portlet replaces it with a site-wide experience: one list across all pages, one configuration
screen, one results screen, reachable from the main navigation rather than from a page. At some point
the UVE entry point has to stop leading to the old per-page screens and start leading to the new ones.
This issue makes that switch **reversible** — it lands both destinations in the same build, selects
between them with a runtime switch, and defaults to the behavior customers already have.

It also closes the last hole in the new configuration screen. The portlet's Variants card lists an
experiment's variants and already renders an **Edit Content** button — permanently disabled, tooltip
"Editing Variant content is available in an upcoming release", with the source comment *"Editing
content is a UVE round-trip, which this screen does not do yet."* Enabling that button and wiring the
round-trip is the second half of this work, and unlike the entry-point switch it is not gated: the
new portlet is incomplete without it.

Two boundaries are worth stating up front. This does **not** retire the old screens or remove the
switch — a later migration issue (#37008) does that. And it does **not** change how UVE edits
anything; UVE's own editing behavior, save semantics, and lock handling are untouched. What changes
is only *which screens the Experiments entry point reaches* and *whether the portlet can hand off to
UVE and get the editor back*.

### What the current system already constrains

Three facts about the code as it stands shaped this spec more than the issue text did, and each one
contradicted the issue. All three have been resolved — the decisions are recorded in
[Resolved Decisions](#resolved-decisions) and folded into the requirements below.

**1. The flag named in the issue is not free.** The issue proposes reviving
`FEATURE_FLAG_EXPERIMENTS`, described as "declared, zero consumers today". That is true of the
frontend enum entry (`libs/dotcms-models/src/lib/shared-models.ts:28`) and false of the property
itself. The same property name is a live backend kill-switch for the *entire Experiments feature*: it
is read by `ConfigExperimentUtil.isExperimentEnabled()`, which gates experiment JavaScript injection
into rendered pages (`ExperimentWebAPIImpl:338`) and experiment resolution during page render
(`HTMLPageAssetRenderedAPIImpl:176,239`). Setting it to `false` — the state this issue wants as the
default — would stop running experiments from serving to visitors. → **D1: a separate, dedicated
switch.**

**2. "Off by default" is not what the default flag plumbing does.** The shared flag reader treats a
missing property as **enabled**: both `DotPropertiesService.getFeatureFlags()` and the `withFlags()`
store feature map the sentinel `NOT_FOUND` to `true`, on the documented rule that "when a feature
flag is not defined on the server, the feature is considered enabled by default". A switch that is
merely *declared* and never *set* therefore ships **on**. The backend agrees:
`Config.getBooleanProperty(FEATURE_FLAG_EXPERIMENTS_KEY, true)` also defaults to `true` — even though
its own javadoc claims "The default value is FALSE", a pre-existing inaccuracy. Delivering a switch
that is genuinely off unless an operator turns it on requires an explicit shipped default, not just a
new enum entry. → **D1, second half.**

**3. Nothing anywhere records that a variant was edited.** The issue asks that a variant's meta line
read "Edited in the Universal Visual Editor" after the round-trip. No such signal exists and none of
the current UI claims one. The variant model carries `id`, `name`/`description`, `weight`, `url` and
`promoted` on both sides of the wire (`AbstractExperimentVariant`, `Variant`) — no edited-at, no
revision count, no has-own-content. The legacy per-page Variants card displays **no edit indication
of any kind**: a name, a copy-URL button, a weight, a View/Edit button, a Delete button, and nothing
else. → **D3: do not invent one.**

### The one thing D3 does leave to fix

The new Variants card has a meta line under each variant name (`data-testid="variant-meta"`) that
renders a fixed string: `Unmodified Page content` for the control, **`No content changes yet`** for
every other variant. It is unconditional — no data feeds it.

Today that string is trivially true, because the Edit Content button beside it is disabled and no one
can change a variant's content from this screen at all. **This issue is what makes the string capable
of being false.** The moment the round-trip works, a variant the editor just rewrote still reads "No
content changes yet".

So D3 does not mean "change nothing". It means the card must stop *asserting* an edit state it has no
way to know, rather than acquiring a signal so it can assert one correctly. That is FR-007.

### What the current system does well enough to keep

The return leg already exists in a usable shape. When UVE is showing a page in a variant, its
info-display carries a variant chip, and acting on that chip navigates back to the experiment's
configuration screen (`dot-uve-toolbar.component.ts:262-277`), clearing the `mode`, `variantName` and
`experimentId` query parameters on the way out. Under the new flow the same gesture must land on the
portlet's configuration route instead of the UVE-embedded one. The mechanism is right; only the
destination changes.

The outbound leg is the part to replace rather than reuse. The legacy Variants card builds its target
by re-parsing `window.location.href` for a `url=` fragment and hand-decoding percent escapes
(`dot-experiments-configuration-variants.component.ts:187-234`), with an origin fallback when the
parse fails. The portlet already holds the experiment's page data; the deep link belongs to that
data, not to the address bar.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Edit a variant's content and come back (Priority: P1)

An editor is working on a page in the Universal Visual Editor. They open Experiments from the editor's
navigation, reach the experiment they want, and land on its configuration in the new portlet. The
Variants card lists the control plus one or more variants. They choose **Edit Content** on a variant,
return to the Universal Visual Editor — same page, now in that variant — change the content, and come
back to the configuration screen of the experiment they left, not to a list and not to a different
experiment.

**The journey starts and ends in the editor.** Nobody sets out to "use the Experiments portlet"; they
set out to test a change to the page in front of them. The portlet is somewhere the editor passes
through, and every story below is written from that direction.

**Why this priority**: Without it the new configuration screen cannot do the one thing an A/B test
exists for — make the variants differ. Every other part of the portlet is already usable; this is the
gap that keeps it from replacing the old flow at all.

Reaching this journey *from the editor* requires the switch on — that entry point is US3. The
round-trip capability itself does not, because the portlet stays reachable from the main navigation
either way (FR-026, FR-027).

**Independent Test**: From a page open in the editor, open Experiments, reach an experiment with at
least one non-control variant, use Edit Content, confirm the editor reopens on the correct page in the
correct variant in an editable mode, make a change, return, and confirm you are back on that same
experiment's configuration screen.

**Acceptance Scenarios**:

1. **Given** a page open in the Universal Visual Editor that has at least one experiment, **When** the
   editor opens Experiments from the editor's navigation and selects one, **Then** they land on that
   experiment's configuration screen in the new portlet.
2. **Given** a draft experiment with a non-control variant and an unlocked page, **When** the editor
   chooses Edit Content on that variant, **Then** the Universal Visual Editor opens on the
   experiment's page, in that variant, in an editable mode.
3. **Given** the editor is in the Universal Visual Editor for a variant reached from the portlet,
   **When** they leave the editor by the variant/return affordance, **Then** they arrive on the
   configuration screen of the experiment they came from — resolved by that experiment's identity,
   not by the page alone — and no variant, experiment or mode parameters remain in the address.
4. **Given** a page that hosts more than one experiment, **When** the editor returns from a variant of
   one of them, **Then** they arrive at that experiment's configuration screen and not at another
   experiment's, nor at a list of the page's experiments.
5. **Given** the editor has returned from editing a variant, **When** the Variants card renders,
   **Then** it makes no claim about whether that variant's content has been modified.
6. **Given** an experiment whose page has been reassigned or is otherwise unavailable, **When** the
   editor chooses Edit Content, **Then** they are told the page cannot be opened rather than being
   sent to a broken editor.

---

### User Story 2 - Nothing changes for anyone who has not opted in (Priority: P1)

An editor on a build that contains all of this work, with the switch left at its default, opens a
page in the Universal Visual Editor and uses Experiments exactly as they did before: the same
navigation item, the same per-page list, the same configuration and reports screens, the same deep
links from the toolbar's running-experiment tag.

**Why this priority**: This is the entire reason the work is switched. The UVE experiments flow is the
one piece of the new portlet's territory that customers use today; shipping a build that quietly
changes it is the failure mode this issue exists to prevent. It ties with User Story 1 at P1 because
it is a release-safety guarantee, not a feature.

**Independent Test**: With the switch at its default, walk the current per-page experiments flow end
to end — navigate in, list, configure, view reports, follow the running-experiment tag — and confirm
every destination and every address matches the pre-change build.

**Acceptance Scenarios**:

1. **Given** the switch is at its shipped default, **When** the editor opens the Experiments item in
   the UVE navigation bar, **Then** they reach the per-page experiments list for that page, at the
   same address as before.
2. **Given** the switch is at its shipped default and a running experiment is shown in the UVE
   toolbar, **When** the editor follows the running-experiment tag, **Then** they reach that
   experiment's reports screen inside UVE, as before.
3. **Given** the switch is at its shipped default, **When** the editor acts on the variant chip while
   viewing a page in a variant, **Then** they reach the UVE-embedded configuration screen, as before.
4. **Given** the switch has been turned on and is then turned back off, **When** the editor repeats
   scenarios 1–3, **Then** the original behavior is fully restored without a redeploy.
5. **Given** the switch is at its shipped default, **When** a visitor requests a page carrying a
   running experiment, **Then** the experiment is served exactly as before — the switch has no
   bearing on what visitors receive.

---

### User Story 3 - Opt in and reach the new portlet from UVE (Priority: P2)

An operator turns the switch on. From then on, an editor who opens the Experiments item in the UVE
navigation bar arrives in the new site-wide Experiments list, already narrowed to the page they were
working on, and can create, configure and start an experiment from there.

**Why this priority**: It is the point of the switch, but it delivers no value until the portlet's
screens are complete (this issue's own dependencies), and it can be exercised only by someone who
deliberately opts in. It follows the two P1 stories rather than leading them.

**Independent Test**: Turn the switch on, open a page in UVE, use the Experiments navigation item, and
confirm the destination is the site-wide list filtered to that page.

**Acceptance Scenarios**:

1. **Given** the switch is on, **When** the editor opens the Experiments item in the UVE navigation
   bar, **Then** they arrive at the new Experiments portlet's site-wide list, filtered to the page
   they came from.
2. **Given** the switch is on and the page has no experiments, **When** the editor arrives at the
   filtered list, **Then** they see an empty state scoped to that page with a way to create an
   experiment for it — not an unfiltered site-wide list.
3. **Given** the switch is on and the page has several experiments, **When** the editor arrives at the
   filtered list, **Then** all of that page's experiments are listed and no other page's are.
4. **Given** the switch is on and the editor is on the filtered list, **When** they clear the filter,
   **Then** they see the full site-wide list — the filter is a starting point, not a cage.
5. **Given** the switch is on, **When** the editor finishes in the portlet and returns to the page,
   **Then** they return to the Universal Visual Editor for that page rather than to an unrelated
   screen.

---

### User Story 4 - Look without touching (Priority: P3)

Same journey as User Story 1 — the editor starts on a page in the Universal Visual Editor and works
through to an experiment's configuration — but this time the variant they open is the control, or the
experiment has already started or ended, or the page is locked by someone else. The editor reopens in
a read-only presentation instead of an editable one, and no content can be changed by accident.

**Why this priority**: It protects data rather than enabling work. It matters most for running
experiments, where an accidental edit corrupts results already being collected, but it is a refinement
of User Story 1 rather than an independent capability.

**Independent Test**: From a page open in the editor, reach an experiment's configuration for each of
control variant, started experiment, and page locked by another user; trigger the open-in-editor
action and confirm the editor presents read-only and offers no editing affordance.

**Acceptance Scenarios**:

1. **Given** an experiment's control variant, **When** the editor opens it from the configuration
   screen they reached from the editor, **Then** the Universal Visual Editor opens in preview/read-only
   mode.
2. **Given** an experiment that is not a draft, **When** the editor opens any of its variants,
   **Then** the Universal Visual Editor opens in preview/read-only mode.
3. **Given** an experiment whose page is locked by another user, **When** the editor opens any
   variant, **Then** the Universal Visual Editor opens in preview/read-only mode and says why.
4. **Given** any of the read-only cases above, **When** the editor returns, **Then** the return lands
   on the same configuration screen as the editable case — read-only changes what UVE offers, not
   where the round-trip ends.

---

### Edge Cases

- **The switch is unreadable.** The configuration read fails, times out, or returns something that is
  neither on nor off. The system must resolve to the *safe* side — the current, pre-change behavior —
  and must not leave the navigation item inert or the editor on a blank screen.
- **The switch changes mid-session.** An operator flips it while an editor already has UVE open. The
  editor's current navigation must not break; the new value may take effect on the next full load.
- **The editor deep-links straight into a variant.** A pasted or bookmarked address opens UVE for a
  page in a variant without the editor ever passing through the portlet. Leaving by the return
  affordance must still land somewhere coherent for the switch's current value, not on a dead route.
- **Two experiments on one page.** The return destination must be resolved from the experiment the
  editor actually came from. A page-scoped return is not sufficient.
- **The experiment is deleted or archived while UVE is open.** Returning must report that the
  experiment is gone rather than rendering an empty configuration screen.
- **The page is locked while UVE is open.** The editor left an unlocked page and returns to a locked
  one; the Variants card's available actions must reflect what is actually true now.
- **The experiment's page has no viewable URL.** The deep link cannot be built from store data
  because the data is incomplete; the action must refuse clearly rather than fall back to guessing
  from the address bar.
- **The editor uses browser Back instead of the return affordance.** Behavior may differ from the
  affordance, but must not strand the editor on a screen that no longer applies.
- **The filtered list's page no longer exists.** With the switch on, an editor arrives from a page
  that has since been deleted; the list must say so rather than silently showing everything.

---

## Requirements *(mandatory)*

### Functional Requirements

#### A. Variant Edit Content round-trip *(the capability is not gated by the switch; reaching it from the editor is)*

- **FR-001**: The portlet's Variants card MUST offer a working open-in-editor action for each variant
  of an experiment the user can see, replacing the permanently-disabled placeholder that ships today.
- **FR-002**: Choosing that action for an editable variant MUST open the Universal Visual Editor on
  the experiment's page, in that variant, in an editable mode.
- **FR-003**: The destination MUST be derived from the experiment data the portlet already holds. The
  system MUST NOT derive it by parsing the browser's current address.
- **FR-004**: If the experiment data needed to build the destination is missing or incomplete, the
  system MUST refuse the action with a message naming the reason, and MUST NOT navigate to a
  partially-formed destination.
- **FR-005**: Leaving the Universal Visual Editor by the variant/return affordance MUST land on the
  configuration screen of the originating experiment in the new portlet, resolved by that
  experiment's identity. A destination scoped only to the page is NOT sufficient — a page may host
  more than one experiment.
- **FR-006**: On return, the variant, experiment and editing-mode parameters MUST be cleared from the
  address so the page is no longer being viewed in an experiment context.
- **FR-007**: The Variants card MUST NOT assert whether a variant's content has been modified. The
  fixed per-variant meta text introduced for the new card — which today tells every non-control
  variant "No content changes yet" — MUST NOT survive this change in a form that makes a claim the
  system cannot substantiate: it MUST be reworded to something state-neutral, or removed.
- **FR-007a**: The system MUST NOT introduce a persisted, derived, or session-held "edited" signal for
  a variant, and MUST NOT display an edited badge, an edited meta line, or a last-modified indicator.
  The observable outcome of the round-trip is the content the editor changed in the Universal Visual
  Editor plus the correct return destination — nothing more. *(Rationale in D3.)*
- **FR-008**: The control variant MUST open read-only.
- **FR-009**: Every variant of an experiment that is not a draft MUST open read-only.
- **FR-010**: Every variant of an experiment whose page is locked by another user MUST open
  read-only, and the reason MUST be stated to the user.
- **FR-010a**: The return destination MUST be the same whether the variant was opened read-only or
  editable.

#### B. The switch

- **FR-011**: The system MUST provide a runtime switch that selects which experiments experience the
  Universal Visual Editor's Experiments navigation item leads to.
- **FR-011a**: The switch MUST be a **new, dedicated** control introduced by this work. It MUST NOT
  reuse the existing feature-wide experiments kill-switch, whose value governs whether experiments
  are served to site visitors at all. *(Rationale in D1.)*
- **FR-012**: The switch MUST be readable and changeable by an operator without a redeploy, and its
  value MUST be reversible in both directions.
- **FR-013**: The switch MUST resolve to **off** on a build where an operator has never set it. An
  absent or unset value MUST NOT be treated as on, which means an explicit shipped default is
  required — declaring the switch is not sufficient.
- **FR-014**: The switch MUST NOT change the behavior of any capability other than the Universal
  Visual Editor's Experiments entry point. In particular, turning it off MUST NOT disable experiments
  for site visitors, MUST NOT stop experiment code from being served on rendered pages, and MUST NOT
  hide the Experiments portlet from the main navigation.
- **FR-015**: If the switch cannot be read, the system MUST behave as if it were off.
- **FR-015a**: The existing feature-wide experiments kill-switch MUST retain its current name,
  meaning, default and consumers. This work MUST NOT change what it does.

#### C. Switch **off** — the guarantee

- **FR-016**: With the switch off, the Universal Visual Editor's Experiments navigation item MUST
  lead to the same destination, at the same address, as on a build without this change.
- **FR-017**: With the switch off, the running-experiment tag in the Universal Visual Editor toolbar
  MUST lead to the same reports destination as before.
- **FR-018**: With the switch off, the variant chip's return action in the Universal Visual Editor
  MUST lead to the same UVE-embedded configuration destination as before.
- **FR-019**: With the switch off, the existing per-page experiments screens MUST remain reachable
  and fully functional, including their list, configuration and reports routes.
- **FR-020**: The guarantees in FR-016 through FR-019 MUST be covered by automated regression tests
  that fail if any of those destinations change.

#### D. Switch **on**

- **FR-021**: With the switch on, the Universal Visual Editor's Experiments navigation item MUST lead
  into the new Experiments portlet.
- **FR-021a**: The destination MUST be the portlet's site-wide list, **filtered to the page the
  editor came from**. It MUST NOT be an unfiltered site-wide list, and it MUST NOT navigate straight
  to a single experiment. *(Rationale in D2.)*
- **FR-021b**: The filtered list MUST behave correctly for zero, one, and many experiments on the
  page: an empty state scoped to the page with a way to create one; a single row; all rows and no
  other page's rows.
- **FR-021c**: The page filter MUST be visible to the editor and MUST be clearable, revealing the
  full site-wide list.
- **FR-022**: With the switch on, the destination MUST carry the page context the editor came from,
  so the editor is not asked to find their page again in a site-wide list.
- **FR-023**: With the switch on, the Experiments navigation item MUST remain subject to the same
  visibility and permission rules as today — an editor who cannot see experiments for a page MUST NOT
  gain access through the new destination.
- **FR-024**: With the switch on, an editor who reaches the portlet from the Universal Visual Editor
  MUST be able to get back to the page they came from.

#### E. Coexistence

- **FR-025**: Both the old per-page screens and the new portlet screens MUST be present and
  functional in the same build; the switch selects between entry points, it does not remove either
  set of screens.
- **FR-026**: The Experiments portlet MUST remain reachable from the main navigation regardless of
  the switch's value.
- **FR-027**: The variant round-trip in Section A MUST work identically regardless of the switch's
  value. The switch governs how an editor **reaches** the portlet's configuration screen from the
  Universal Visual Editor, not what that screen can do once reached — and the portlet stays reachable
  from the main navigation either way (FR-026).

### Key Entities

- **Experiment**: The test being configured. Relevant here for the page it targets, its status (draft
  versus anything else), and its identity — which the return destination is resolved by, since a page
  may host several.
- **Variant**: One arm of an experiment. Relevant here for its identity and whether it is the control.
  It carries **no** representation of whether its content has been edited, and this work does not add
  one (FR-007a).
- **Page**: The content the experiment targets. Relevant here for its identifier, its viewable
  address, and its lock state. Also the filter value carried into the list when the switch is on.
- **Entry-point switch**: The operator-controlled value that selects which experiments experience the
  Universal Visual Editor leads to. A new control, distinct from the existing feature-wide experiments
  kill-switch (FR-011a, FR-015a).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a build with the switch untouched, every step of the existing per-page experiments
  flow reaches the same screen at the same address as on the previous release — verified by an
  automated regression suite with zero differences.
- **SC-002**: An operator can move between the old and the new entry point, in either direction, in
  under one minute and without a deployment or restart.
- **SC-003**: Turning the switch off leaves running experiments serving to site visitors unaffected —
  measured as no change in experiment participation for live experiments across the change.
- **SC-004**: An editor can go from an experiment's configuration screen, into the editor, change a
  variant's content, and be back on the configuration screen in under 30 seconds of interaction time,
  without typing or pasting an address.
- **SC-005**: 100% of returns from the editor land on the originating experiment — including on pages
  that host more than one — and none land on a page-scoped list, a site-wide list, or an error.
- **SC-006**: Zero variant edit actions produce a destination the editor cannot open — every action
  either opens the intended page and variant or is refused up front with a stated reason.
- **SC-007**: No open-in-editor action on a control variant, a non-draft experiment, or a page locked
  by another user results in an editable session.
- **SC-008**: No screen in the delivered flow tells an editor whether a variant's content has or has
  not been changed — verified by inspection of the Variants card in both the edited and unedited case,
  which must read identically.
- **SC-009**: With the switch on, an editor arriving from a page reaches a list showing exactly that
  page's experiments — zero, one, or many — in a single navigation, with no manual filtering.

---

## Assumptions

- The old per-page experiments screens and the new portlet screens can be present in the same build
  without conflicting; the switch chooses an entry point, not a code path that must be compiled out.
- The switch is read once per full application load. An operator flipping it does not need to affect
  editors already mid-session, and a stale value until the next reload is acceptable.
- The Experiments portlet's own reachability from the main navigation is settled by its own issue and
  is not gated by this switch.
- The variant round-trip belongs to the portlet's configuration screen and therefore ships with it,
  gated only by that screen existing — not by the entry-point switch.
- The portlet's site-wide list can accept and display a page filter, or can be extended to do so
  within this work. If it cannot, FR-021a needs revisiting before implementation.
- The Universal Visual Editor's existing behavior for a page opened in a variant (what can be edited,
  how it saves, how locks apply) is correct and unchanged by this work.
- "Read-only" means the editor's existing preview presentation, not a new mode invented here.
- Permission and license checks that guard the experiments experience today continue to apply
  unchanged on both sides of the switch.
- Regression coverage for the switch-off guarantee can be expressed as assertions on the destinations
  the Universal Visual Editor produces, without needing a full browser journey for every case.
- The new switch is named `FEATURE_FLAG_EXPERIMENTS_PORTLET`, placing it in the same family as the
  existing UI feature switches and keeping it unambiguous against `FEATURE_FLAG_EXPERIMENTS`. Its
  storage follows the precedent those switches set. The name is fixed; everything else about how it is
  read remains a planning decision, constrained by FR-011a and FR-013.

---

## Dependencies

- **#36989** — Experiments Portlet, Screen 1 (portlet base + site-wide list). The switch-on
  destination is that list, and FR-021a requires it to accept a page filter.
- **#37003** — Experiments Portlet, Screen 2 (create/update, `/experiments/:id/configuration`). The
  Variants card that hosts the open-in-editor action, the disabled placeholder button this work
  enables, the meta text FR-007 constrains, and the screen the round-trip returns to are all
  delivered there. Section A cannot be built before it lands.
- **#37004** — Experiments Portlet, Screen 3 (view results). Needed for the switch-on experience to be
  complete, since the old flow's reports destination has a new-flow counterpart.
- End-to-end coverage of both the round-trip (switch on) and the old-flow regression (switch off) is
  owned by the dedicated E2E issue, not by this one.

### Relationship to #37008 (migration)

[#37008](https://github.com/dotCMS/core/issues/37008) — the migration issue that deletes the old UI —
retires **`FEATURE_FLAG_EXPERIMENTS_PORTLET`**, the switch this work introduces, together with its
shipped default and its wiring.

It does **not** retire `FEATURE_FLAG_EXPERIMENTS`. That property is the backend kill-switch for the
whole Experiments feature, with live consumers in page render and experiment JS injection; removing it
would disable experiments for site visitors, the exact outcome D1 exists to prevent. The unused
frontend enum entry `FeaturedFlags.LOAD_FRONTEND_EXPERIMENTS` may be dropped there, since it has no
consumers and never gains one under D1, but the property and its backend readers stay.

---

## Out of Scope

- Removing the entry-point switch, retiring the old per-page screens, or deleting the legacy
  address-parsing code — all owned by #37008 (see the impact note above).
- Removing, renaming, or changing the default of the existing feature-wide experiments kill-switch
  (FR-015a).
- Adding any notion of variant edit history, edited-at timestamps, or content-diff indicators
  (FR-007a) — including for a later issue's benefit.
- Any change to how the Universal Visual Editor edits, saves, locks, or renders content.
- Any change to how experiments are served to site visitors, including experiment code injection.
- Migrating variant content between pages, or any change to variant creation or deletion.
- Changing the Experiments portlet's placement or visibility in the main navigation.
- Correcting the pre-existing inaccuracy in the backend experiments kill-switch documentation
  (javadoc says the default is FALSE; the code defaults to TRUE), beyond noting it here.

---

## Resolved Decisions

Three decisions were open when this spec was first drafted. All three are settled; the rationale is
kept because each contradicts the issue text and a reader will otherwise assume the issue is right.

### D1 — A new, dedicated switch; `FEATURE_FLAG_EXPERIMENTS` untouched

**Decision**: Introduce a distinct switch for the UVE entry point, shipped explicitly **off**.
`FEATURE_FLAG_EXPERIMENTS` keeps its current job as the backend kill-switch for the Experiments
feature. → FR-011a, FR-013, FR-014, FR-015a.

**Why not reuse it**: the property already gates whether experiments reach site visitors
(`ConfigExperimentUtil.isExperimentEnabled()` → `ExperimentWebAPIImpl:338`,
`HTMLPageAssetRenderedAPIImpl:176,239`). Reusing it would make FR-013 ("off by default") and FR-014
("must not disable experiments for visitors") mutually unsatisfiable: the default state would take
running experiments off the air, and an operator reverting the entry point would silently do the same.
Scoping the new meaning to the frontend only does not help — one value, two meanings, no way for an
operator to express "new portlet, experiments still serving".

**Costs accepted**: a new switch — named `FEATURE_FLAG_EXPERIMENTS_PORTLET` — an entry in the
configuration allow-list so the frontend can read it, and an explicit `false` in the shipped
configuration (required by FR-013, because the shared flag readers treat an absent flag as enabled).
Follow-on impact on #37008 is recorded above.

### D2 — The switch-on destination is the site-wide list, filtered to the page

**Decision**: With the switch on, the UVE Experiments navigation item lands on the portlet's site-wide
list, filtered to the page in hand. → FR-021a, FR-021b, FR-021c, US3.

**Why**: it is closest to the mental model the entry point already sets — "the experiments for this
page" — and it is the only option that behaves sensibly across zero, one, and many experiments without
a special case. Going straight to a single experiment is faster in the common case but needs a defined
fallback for none and for several, and it makes the destination unpredictable from the gesture. The
issue deferred this decision explicitly ("filtered list vs direct to the page's experiment — decided
in the PR with the design at hand"); it is decided here so the plan can proceed.

**Cost accepted**: the site-wide list must accept, display, and allow clearing a page filter.

### D3 — No "edited" signal; the round-trip is the deliverable

**Decision**: Do not invent an edited signal in any form — not persisted, not derived, not
session-held — and do not display an edited badge or meta line. Replicate what the current flow does,
which is nothing. The requirement is that the round-trip lands on the **correct** experiment
configuration. → FR-007, FR-007a, US1 scenarios 2–4, SC-008.

**Why**: no such signal exists on the variant model on either side of the wire, and the legacy
per-page Variants card displays no edit indication of any kind. The issue's "the edited variant's meta
line updates (*Edited in the Universal Visual Editor*)" would be new behavior with no data behind it —
either a contract change with rollback implications, or a session-only badge that vanishes on reload
and is invisible to a second editor, which is misleading rather than merely incomplete.

**What this still requires**: the new Variants card's fixed meta text tells every non-control variant
"No content changes yet". That is true today only because the Edit Content button is disabled; this
issue is what makes it capable of being false. The card must therefore stop making the claim — reword
to something state-neutral, or drop the line — rather than acquire a signal so it can make the claim
accurately. That is FR-007, and it is the only UI change D3 obliges.
