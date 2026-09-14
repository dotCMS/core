# Issue Resolution Specification: Relationship field — navigating to content just created from the field triggers the unsaved-changes prompt

**Feature Branch**: `37385-relationship-nav-unsaved`

**Created**: 2026-09-03

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37385](https://github.com/dotCMS/core/issues/37385)

**Input**: User description: "Relationship field: navigating to content just created from the field triggers the unsaved-changes prompt. Frontend (core-web, libs/edit-content). Team Falcon."

## Problem Statement *(mandatory)*

Content created directly from a Relationship field cannot be opened right after creating it. Clicking the new row raises the "unsaved changes" dialog, which offers only *Keep editing* or *Discard changes* — so the user either abandons the navigation or throws away the relation they just built.

The prompt is technically correct, which is what makes this a design dead end rather than a stray dialog: creating content from a Relationship field adds the new contentlet to the field's value, marking the editor's form dirty, and that relation exists **only** in the unsaved form. Opening the new content destroys the editor (full-screen) or reloads it in place (overlay), so the relation — plus any other in-flight edits — genuinely would be lost. The guard fires exactly as designed.

A second, independent defect surfaces in the same flow: the row added for the newly created content shows an **empty Locales column**. It only fills in after the parent is saved. Same root flow, different cause (see Root-Cause Hypothesis), and it is fixed here because the row-refresh introduced by this change would otherwise reproduce it on every save.

**Severity / Impact**: Medium — some functionality impacted. Affects every content type with a Relationship field in the new Edit Content editor. It is not a silent data loss (the dialog does warn), but a flow users are directed into ("create the related content from here, then go look at it") cannot be completed, and the only way forward discards a change the user just made deliberately.

## Reproduction *(mandatory)*

**Environment**: dotCMS admin UI (Angular), new Edit Content editor. Latest from `main` (`23.4.0-next.1`). Browser-agnostic; reproduced on Chrome / macOS.

**Steps to Reproduce**:

1. Open a content item whose content type has a Relationship field, in the new Edit Content editor.
2. In the Relationship field, choose **Create new content**.
3. Fill in the new content and save it. It now shows as a row in the parent's Relationship field.
4. Click that new row to open the content that was just created.

**Expected Behavior**: The content just created opens, and the work in progress in the editor behind it is preserved.

**Actual Behavior**: The "unsaved changes" dialog appears. The user must either discard the relation just created (losing it) or cancel and not open the content at all.

**Reproducibility**: Always, whenever the editor has unsaved changes. Creating content from the field always leaves it in that state, so this path reproduces 100% of the time.

**Second defect (empty Locales column)** — same steps 1–3, then observe the new row: its Locales cell is blank while the pre-existing rows show their locale (e.g. "English (en)"). Save or publish the parent and the cell fills in. Also always reproducible.

## Scope of Investigation *(mandatory)*

- **Affected area**: Admin UI — new Edit Content editor, Relationship field and its related-content navigation. Frontend only; no backend change is anticipated.
- **Suspected surface**: Modern frontend (`core-web/libs/edit-content`). No `com.dotmarketing.*` or `com.dotcms.*` Java code is expected to be touched; the legacy Dojo editor is not involved.
- **Related known decisions**: Related-content navigation with a breadcrumb trail was introduced by issue #36349 (URL-driven `rc` query param for the full-screen editor, per-instance in-memory trail for overlays). This fix must not regress that behavior for editors with no unsaved changes. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

The relation between the parent content and the content created from the field is never persisted at creation time — it lives only in the parent's unsaved form value. Two observations support this:

1. On a successful create, the field appends the new contentlet to its store data, which marks the form control (and therefore the editor's form) dirty.
2. The dialog data carries a `relationshipInfo` payload (parent id, relationship name, isParent) that is **declared but never consumed** — nothing sends it to the backend.

Related-content navigation is a real navigation: the full-screen host performs a router navigation and the overlay host reloads in place. Either way the current editor's form is discarded, so the unsaved-changes guard is correct to intervene. The defect is therefore in the interaction design, not in the guard: the user is asked to choose between two losses in a flow that should have neither.

**Empty Locales column** — confirmed against a running instance, not inferred. The column formats the item's full language object (`{ language: 'English', languageCode: 'en', … }`). Related items normally arrive through the parent's `depth` fetch, where each child carries that object. A contentlet returned by a **workflow action** does not: it carries only `languageId`. The row added on create therefore has nothing for the column to format, and stays blank until the parent is saved and the field re-reads its items from a `depth` fetch.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- From the **full-screen** editor, related content opens in a side panel over it instead of navigating away, so the editor stays mounted and nothing unsaved is discarded or prompted about.
- From **inside a side panel**, related content keeps navigating in place with its own crumb trail — no second panel on top.
- Closing the panel returns to the editor exactly as left: same values, same unsaved state, the new item still listed.
- Refreshing a related row (title, status) in the parent's Relationship field after that content is edited and saved, without that refresh marking the parent form dirty by itself.
- Filling in the Locales column for a just-saved contentlet, in both paths that put one in the table (creating content from the field, and refreshing a row edited in the panel).

**Explicitly out of scope / non-goals**:

- Persisting the relationship at creation time (wiring `relationshipInfo` to a backend relate call, or auto-saving the parent). That changes save semantics and needs its own decision.
- Snapshotting and restoring form state across a navigation that unmounts the editor. Evaluated and rejected for this fix: the per-field-type restore cost (binary/file, category, block editor, custom field) is large and the failure mode is silent.
- Changing the unsaved-changes prompt itself. It must keep firing when the user genuinely leaves an editor with unsaved changes (closing it, navigating away, tab close/refresh via `beforeunload`).
- Stacking a second panel on top of the first. Navigation from inside a panel stays as it is today.
- Adding a limit to how deep related content can be opened. Explicitly decided against (see Assumptions).
- Making the panel show a crumb trail for the hop that opened it. The panel header names the content but that hop appears in no trail — a known gap, deferred (see Assumptions).

## Regression Risk *(mandatory)*

- **Blast radius**:
  - Related-content navigation and its breadcrumb trail (#36349) — the pristine path must be untouched.
  - The unsaved-changes guard, in all its entry points: route `canDeactivate`, the host navigation guard for same-route moves, the editor's close guard, and `beforeunload`.
  - The "create content from a Relationship field" flow, which shares the code that opens the editor.
  - Cross-cutting state that is already reference-counted or stacked per open editor: main-navigation collapse, the form bridge, ESC/click-outside handling for the frontmost layer only.
- **Backward compatibility**: The `rc` trail query param, breadcrumb rendering and shareable/deep links must keep working for the pristine path. No API, DB or ES contract is touched; nothing here is rollback-unsafe.
- **Data considerations**: None. No migration, no repair of existing data. The fix changes only how the editor is presented during navigation.
- **Resource risk**: Keeping the previous editor alive means more than one full editor (form + sidebar + workflow) mounted at once. With no depth limit this grows with how deep the user navigates. The plan must state how this is measured and what the acceptable ceiling is in practice.

## Acceptance & Verification *(mandatory)*

- **AC-001**: The reproduction steps above no longer produce the actual behavior — clicking the newly created row opens that content with no unsaved-changes dialog.
- **AC-002**: The full-screen editor keeps its unsaved changes while the related content is open in the panel, including the relation to the content just created.
- **AC-003**: Closing the panel returns to that editor with the form exactly as left: same values, same unsaved state, the new item still listed in the Relationship field.
- **AC-004**: From the full-screen editor, the panel opens for any related content — whether or not the form has unsaved changes and whether or not that relation was already saved.
- **AC-005**: Editing a related content and saving it updates that row's title and status in the parent's Relationship field.
- **AC-006**: Refreshing that row does not, by itself, mark the parent form as having unsaved changes.
- **AC-007**: From inside a side panel, opening a related content navigates in place and shows the crumb trail — it does not open another panel.
- **AC-008**: The Locales column is populated as soon as content created from the field appears in the table, without waiting for the parent to be saved.
- **AC-009**: The Locales column stays populated after a related content is edited and saved in the panel. A language id that matches no known locale leaves the cell blank rather than showing a wrong locale.
- **AC-010** *(regression)*: Without the side panel flag, related-content navigation behaves exactly as before — router navigation, trail, breadcrumb, deep links, and the unsaved-changes prompt.
- **AC-011** *(regression)*: The unsaved-changes prompt still fires when the user genuinely leaves an editor with unsaved changes: closing it, navigating away from it, or a tab close/refresh.

- **Verification method**:
  - Jest/Spectator specs in `core-web/libs/edit-content`, covering: the dirty path does not delegate to the navigation host; the pristine path still does; the previous editor stays mounted and dirty across a navigation and back; a related row refreshes by identifier after a save without dirtying the form; the close/exit guard still prompts.
    `pnpm nx test edit-content --testPathPatterns="relationship-field"`
  - Full library regression: `pnpm nx test edit-content` and `pnpm nx lint edit-content`.
  - Manual: the reproduction steps above, plus a two-level-deep navigation with unsaved changes at each level, verified back to the root editor.

## Assumptions

- The new Edit Content side panel presentation is enabled by default and is the presentation this fix targets.
- **No depth limit.** A prototype capped how deep related content could be opened; the decision (dev, 2026-09-03) is to have no limit, on the grounds that a limit is a surprising behavior change mid-flow. The resource cost of unlimited depth is recorded under Regression Risk so the plan can address measurement rather than a hard cap.
- **The rule is the chrome, not the state.** Two narrower rules were built and rejected in review (dev, 2026-09-04): keying on "the form is dirty" hijacked every related-content click as soon as anything on the page was touched, losing the trail and breadcrumb; keying on "this relation is not saved yet" still diverted clicks unpredictably. The accepted rule is positional — full-screen always opens the panel, inside a panel always navigates — so the same click always behaves the same way in the same place.
- **The hop that opens the panel leaves no crumb.** The panel's own trail starts at the panel's content, so the editor underneath does not appear in it. Accepted for now; a panel-header trail was scoped and deferred.
- The relation created from the field is expected to be persisted when the user saves the parent, as it is today. This fix does not change when the relation is written.
- **TDD note (Constitution Principle V):** an exploratory prototype of this fix already exists on the working branch, with passing Jest specs. Those tests must be reviewed and dev-approved, and confirmed to FAIL against the unfixed code (Red), before being accepted as the fix's test suite. The plan and tasks phases must re-establish that ordering rather than inherit green tests.
