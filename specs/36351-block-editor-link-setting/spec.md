# Block Editor 2.0: hyperlinks stop depending on Allowed Blocks

**Feature Branch**: `nicobytes/36351-block-editor-20-add-hyperlink-option-to-block-editor-field-settings-toolbar`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature (customer-reported gap)

**GitHub**: [#36351](https://github.com/dotCMS/core/issues/36351) · Freshdesk [#37852](https://dotcms.freshdesk.com/a/tickets/37852)

**Input**: User description: "https://github.com/dotCMS/core/issues/36351 — Block Editor 2.0: Add 'Hyperlink' option to Block Editor field settings toolbar"

---

> ## ⚠️ Read this first — the issue's acceptance criteria are superseded
>
> The GitHub issue offers two mutually exclusive resolutions in its own *Expected* section:
>
> > *"A 'Hyperlink' option should be available in the Settings tab so it can be explicitly
> > enabled/disabled like other toolbar options. **OR** Hyperlink's are always available despite
> > what is selected within the Settings tab."*
>
> Its four checkbox ACs then spell out only the first. **This spec implements the second**, decided
> on 2026-09-14 after weighing both (see [Why not the toggle](#why-not-the-toggle-decision-record)).
>
> Concretely, **issue ACs 1, 2 and 3 do not apply** — no option is added to the Settings tab, and
> there is no enabled/disabled state to test. Issue AC 4 (no regression for fields with an empty
> selection) stands and is FR-005 below.
>
> An approver expecting a new checkbox in the Settings tab will not find one. That is intended.

---

## Context

A Block Editor field has one restriction control in its **Settings** tab: **Allowed Blocks**. Leave
it empty and the author gets everything; pick any subset and the author gets exactly that subset.

Hyperlinking is not a block — it is formatting applied to text — but the editor nonetheless asks
the Allowed Blocks list whether hyperlinks are permitted. The list has never offered a hyperlink
entry to pick. So the answer is "yes" only while the list is empty, and "no" the instant an admin
picks anything at all.

The result is a restriction nobody chose. An admin who allows *Bold, Italic, Bullet List* has
silently also forbidden hyperlinks, with no control anywhere in the product to undo it. The only
escape is to clear the whole list and give up restriction entirely — the workaround the support
ticket documents.

This is also a **regression against the previous Block Editor**, where the hyperlink action was
never subject to Allowed Blocks and always worked.

The fix is to stop asking the question: hyperlinks become permanently available, in every field,
whatever Allowed Blocks says.

---

## Why not the toggle *(decision record)*

Making the toggle real requires more than a checkbox. Today, a restricted field with no hyperlink
entry means *"saved before the option existed"*; the moment a toggle ships, the same stored data
means *"the admin unchecked it"*. The two are indistinguishable, so a working toggle also needs
either a one-time data migration over every stored Allowed Blocks value, or a version marker
embedded in that value and a permanent "no marker ⇒ permitted" rule.

That is real cost — mutating customer rows, or carrying a special case forever — bought for a
capability nobody asked for. The reported problem is that hyperlinks are *missing*, not that
someone wanted them *gone*.

There is direct precedent. **#37340** hit the identical bug for emoji and resolved it exactly this
way: the gate was removed, emoji became permanently available, and no option was added to the
Settings tab. Emoji, YouTube embeds and hyperlinks were always in the same bucket — capabilities the
Allowed Blocks list never offered and therefore could never legitimately restrict. Two of the three
have already been ungated; this change finishes the set.

**What is given up**: an admin cannot forbid hyperlinks on a field. Accepted — no customer has asked
to, and if one ever does, the toggle can be built then, on top of a codebase where the gate's
semantics are no longer ambiguous.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The hyperlink button comes back on restricted fields (Priority: P1)

An author works on a content item whose Block Editor field was restricted by an admin — whether
months ago or five minutes ago. Today the hyperlink button is simply missing from the toolbar. After
this change it is there, with no admin action, no content-type edit and no re-save.

**Why this priority**: This is the reported customer impact and the visible half of the fix. It
repairs already-deployed sites and every field created from now on, with nobody needing to do
anything.

**Independent Test**: Take any content type whose Block Editor field stores a restricted Allowed
Blocks list. Without touching the content type, open a content item and confirm the hyperlink
control is present and creates a working link.

**Acceptance Scenarios**:

1. **Given** a Block Editor field whose stored Allowed Blocks list is a non-empty subset,
   **When** an author opens a content item using that field, **Then** the hyperlink control is
   present in the toolbar.
2. **Given** that control, **When** the author selects text and uses it, **Then** a link is created,
   and it can be edited and removed exactly as on an unrestricted field.
3. **Given** a field restricted to a single non-hyperlink entry — the narrowest restriction
   possible — **When** an author opens it, **Then** the hyperlink control is still present.
4. **Given** a field with an empty Allowed Blocks list, **When** an author opens it, **Then**
   nothing has changed: the hyperlink control is present, as it already was.

---

### User Story 2 - Pasting and typing URLs works again on restricted fields (Priority: P2)

An author on a restricted field pastes a URL over selected text, or types a bare URL and keeps
going. Today neither produces a link, because the same gate that hides the button also disables the
implicit authoring paths. After this change both behave as on an unrestricted field.

**Why this priority**: A separate slice — these paths are gated independently of the button, so
shipping US1 alone would leave a field where the button works but pasting a URL silently does
nothing, which is arguably more confusing than the original bug. Lower than P1 only because the
missing button is what customers actually reported.

**Independent Test**: On a restricted field, paste a URL over a text selection and separately type a
bare URL followed by a space; confirm both produce links.

**Acceptance Scenarios**:

1. **Given** a restricted Block Editor field, **When** the author pastes a URL over selected text,
   **Then** the selection becomes a link to that URL.
2. **Given** the same field, **When** the author types a bare URL, **Then** it is automatically
   linked, matching unrestricted behaviour.
3. **Given** the same field, **When** the author pastes rich content containing links, **Then**
   those links survive the paste.

---

### Edge Cases

- **Stored content containing links, on any field.** Rendering and saving must be lossless. A
  hyperlink is a *mark*, and dropping a mark from the editor's vocabulary aborts loading the whole
  document — a restricted field renders blank. That was **#37175**, which fixed it by registering
  the hyperlink vocabulary unconditionally. This change removes the remaining gate and must not
  disturb that.
- **An empty Allowed Blocks list.** Unchanged in every respect. This is today's only working
  configuration and it must not move.
- **Selecting every entry in the list.** Behaves identically to an empty list, as today.
- **A stored list containing an entry the product no longer offers.** Tolerated, as today.
- **A stored list that literally contains a hyperlink entry.** Not producible through the UI, but a
  value could have been written by hand or by an API client. It must be harmless — hyperlinks are
  available either way, and the entry must not be treated as an unknown block or surfaced as
  unsupported content.
- **The legacy Block Editor reading the same field.** Both editors read the same stored setting. The
  legacy editor never restricted hyperlinks and must not start.
- **Other capabilities on a restricted field.** Removing this gate must not widen any other one:
  a field restricted to *Bold, Italic, Bullet List* must still offer no tables, no images, no code
  blocks.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Hyperlink authoring MUST be available on every Block Editor field regardless of the
  field's Allowed Blocks value. No configuration, in any surface, may disable it.
- **FR-002**: The hyperlink toolbar control MUST be present and fully functional — create, edit,
  remove — on a field with a restricted Allowed Blocks list, identically to an unrestricted field.
- **FR-003**: The implicit hyperlink authoring paths MUST likewise be available on a restricted
  field: pasting a URL over a selection, and automatic linking of typed URLs.
- **FR-004**: No option representing hyperlinks may be added to the Allowed Blocks list. A control
  that cannot change the outcome must not be shown.
- **FR-005**: A field with an empty Allowed Blocks list MUST continue to behave exactly as it does
  today, hyperlinks included. No regression. *(This is issue AC 4.)*
- **FR-006**: Removing the hyperlink gate MUST NOT relax any other Allowed Blocks restriction.
  Every other capability keeps gating exactly as it does today.
- **FR-007**: Stored content MUST be unaffected. Documents containing links MUST render, round-trip
  and save losslessly on every field, with no block or mark dropped and nothing shown as unsupported
  content.
- **FR-008**: The previous (legacy) Block Editor MUST continue to offer hyperlinks on every field
  regardless of Allowed Blocks, exactly as it does today.
- **FR-009**: Neither editor's slash (`/`) menu may gain a hyperlink command. Hyperlinks are a
  toolbar action and the slash menus do not offer them today.
- **FR-010**: Once the gate is gone, no dead configuration may be left behind — nothing in the
  product may still read an Allowed Blocks value to decide anything about hyperlinks.

### Key Entities

- **Allowed Blocks setting**: a per-field stored list of permitted editor capabilities, saved on the
  content-type field and read by both the current and the legacy Block Editor. Empty means
  unrestricted. **This change does not modify it** — not its format, not its contents, not the set
  of values it may contain. It only stops one consumer from consulting it.
- **Hyperlink capability**: the ability to attach a URL to selected text. Unlike every member of the
  Allowed Blocks list it is text formatting rather than a block, which is both why it was never
  listed and why gating it by that list was wrong from the start.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On an upgraded installation, 100% of existing Block Editor fields that had lost
  hyperlinks have them working again with zero administrator actions — no content type opened, no
  field re-saved, no data migrated.
- **SC-002**: Hyperlink authoring succeeds in 100% of field configurations tested: empty list,
  single-entry list, several-entry list, all-entries list, and a hand-written list that names
  hyperlinks explicitly.
- **SC-003**: The workaround documented in the support ticket — emptying Allowed Blocks to recover
  hyperlinks — is unnecessary for every configuration, and the linked Freshdesk ticket class stops
  recurring.
- **SC-004**: Zero content loss: across a regression set of documents containing links, every
  document opened and re-saved is unchanged in its links, on restricted and unrestricted fields
  alike.
- **SC-005**: Zero collateral widening: for each capability that Allowed Blocks legitimately
  restricts, a field excluding it still excludes it, verified across the same configurations as
  SC-002.
- **SC-006**: Authors reach and use the hyperlink control in the same number of steps on every
  field, restricted or not.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Content authoring in the current Block Editor only. The Settings
  tab — which lives in the older admin surface — is **not** touched, because no option is added
  (FR-004). That is a direct consequence of choosing this resolution over the toggle and keeps the
  change out of the legacy admin code entirely.

- **Backward-compatibility expectations**:
  - **No stored data changes.** No migration, no upgrade task, no new value in any field variable,
    no schema change. Nothing is rollback-unsafe: reverting this change restores the previous
    (buggy) behaviour and nothing else, because no data was written.
  - **Stored documents are the real constraint.** See #37175 below — the failure mode for getting
    this wrong is a blank field, not a missing button.
  - **The legacy editor must not change at all** (FR-008).

- **Shared catalogue — a risk this resolution avoids**: the catalogue of selectable Allowed Blocks
  entries is shared with the legacy Block Editor, which uses it to build its slash menu. Adding an
  entry there — the toggle path — would have surfaced a new row in the legacy editor's `/` menu.
  Choosing this resolution removes that blast radius, and FR-009 keeps it removed.

- **Known related decisions**:
  - **#37175** — capabilities not selectable in Allowed Blocks (hyperlinks, emoji, YouTube embeds)
    must stay in the editor's vocabulary unconditionally and gate only their authoring paths.
    Hyperlinks got that fix; this change removes the authoring gate that #37175 deliberately left in
    place. The vocabulary half must stay exactly as it is — the tests guarding it are the ones that
    prove a restricted field still loads its content.
  - **#37340** — the same bug for emoji, resolved by removing the gate rather than adding an option.
    This change follows that precedent. One noted side effect there is relevant here too: removing a
    gate can leave a toolbar group unconditionally populated, making previously-collapsed groups
    visible on heavily restricted fields.
  - The plan phase will formally consult `dotCMS/platform-adrs`.

---

## Out of Scope / Non-Goals

- **Adding any hyperlink option to the Settings tab.** Explicitly rejected — see
  [Why not the toggle](#why-not-the-toggle-decision-record) and FR-004.
- **Any migration of stored Allowed Blocks values.** Not needed under this resolution.
- **Changing the legacy Block Editor** in any way (FR-008).
- **Adding a hyperlink entry to either slash menu** (FR-009).
- **Rethinking how text formatting is restricted.** Bold, Italic, Underline and the rest are equally
  absent from Allowed Blocks and equally unrestrictable. That is a design gap for a broader "allowed
  formatting" feature, not this issue — and after this change the editor no longer pretends
  otherwise for any of them.
### Two adjacent defects found during research — deliberately left out

Auditing every capability key against the catalogue of selectable Allowed Blocks entries turned up
two more instances of this same bug. Both are real and currently unreported. Scope was held to
hyperlinks on 2026-09-14; each should be filed as its own issue.

- **YouTube embeds** — identical to hyperlinks, and named in the very same #37175 comment
  (*"`link`, `emoji` and `youtube` are not selectable"*). On any restricted field the YouTube tab of
  the "Add asset by URL" popover is permanently disabled. It is less visible only because the
  popover's trigger is shown when images *or* videos are permitted, so authors reach the popover and
  find a dead tab rather than missing the feature outright. The fix is the same one-liner as this
  spec's, and folding it in was considered and declined.

- **AI Content / AI Image** — a different and worse shape: the option *does* exist in the Settings
  tab, but the two sides use different identifiers (the tab saves one spelling, the slash menu checks
  another). An admin who explicitly permits AI Content on a restricted field still never sees it in
  the `/` menu — a silently broken promise rather than an absent capability. This one is not a
  one-liner: correcting the spelling on the settings side would invalidate values already stored in
  customer field variables, so which side gives is a decision with stored data behind it and needs
  its own spec.

---

## Assumptions

- **No user-visible copy changes.** The hyperlink control keeps its current label, tooltip and
  position in the toolbar; only its unconditional presence changes. No new localisation strings.
- **"Hyperlinks are available" is one thing**, not separate states for the button and the implicit
  paths. They move together (FR-002/FR-003), which is how the editor already treats them — the
  current gate switches all of them at once.
- **No REST, database, or search-index contract changes.**
- **Verification is front-end.** Component-level tests for the editor toolbar and for the editor's
  capability wiring, covering the configurations named in SC-002 and SC-005.
- **An existing test pins the behaviour being removed.** Today there is a test asserting that a
  restricted field disables the implicit hyperlink paths — that assertion is exactly what US2
  inverts, and it must be rewritten rather than deleted, so the new expectation stays pinned. The
  #37175 tests asserting the hyperlink vocabulary is always registered must stay green untouched;
  they are the guard against the blank-field failure mode.
- **There is currently no test asserting the hyperlink button's visibility under any Allowed Blocks
  configuration.** That gap is why the bug shipped, and closing it is part of the work.
- Per Principle V, all of the above tests are written, developer-approved, and confirmed failing
  (Red) before any implementation.
