# Issue Resolution Specification: Locking or unlocking content reverts unsaved Relationship field changes

**Feature Branch**: `issue-38034-lock-keeps-relationship-edits`

**Created**: 2026-10-15

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#38034](https://github.com/dotCMS/core/issues/38034)

**Input**: User description: "https://github.com/dotCMS/core/issues/38034"

## Problem Statement *(mandatory)*

In the new Edit Content, an editor can change a Relationship field (add, remove or reorder related items) without saving. If they then click **Lock for Editing** or **Unlock** in the sidebar, the Relationship field silently goes back to the saved relationships and their changes are lost. There is no warning. Other fields keep their unsaved edits; the Title was checked.

The editor can then save the content believing the relationship change is still there, when it is not.

**Severity / Impact**: Medium. It affects every editor who changes a Relationship field and then changes the lock state before saving. It is easy to hit, because the content can be edited without locking it first, and locking right after starting to edit is a normal flow. The work lost is limited to unsaved relationship changes; nothing already saved is damaged.

## Reproduction *(mandatory)*

**Environment**: dotCMS main (1.0.0-SNAPSHOT, build of 2026-10-09), local instance, demo starter content. Chrome on macOS. New Edit Content enabled.

**Steps to Reproduce**:

1. Open the Blog "Easy Snowboard Tricks You can Start Using Right Away" in the new Edit Content. Its **Author** relationship field has one related item, "Will".
2. Click **Lock for Editing** in the sidebar.
3. In the Author field, remove "Will" (or add another related item).
4. Click **Unlock**.

**Expected Behavior**: The Author field still shows the change made in step 3 (no "Will"), and that change is still saved when the editor saves.

**Actual Behavior**: "Will" is back in the Author field; the change from step 3 is gone.

The reverse order fails the same way: with the content unlocked, change the Title and remove "Will", then click **Lock for Editing**. The Title keeps its new value, but "Will" comes back.

**Reproducibility**: Always. Reproduced in both directions (Lock and Unlock) on 2026-10-14.

## Scope of Investigation *(mandatory)*

- **Affected area**: Content editing, in the new Edit Content (Angular, `core-web/libs/edit-content`). Two places are involved: the lock / unlock actions in the sidebar, and the Relationship field.
- **Suspected surface**: Two parts. (1) The lock revert is frontend only, all of it modern code: the Edit Content signal store and the Relationship field component and store. The backend lock / unlock endpoints and the legacy `com.dotmarketing.*` code are not involved: they return the right lock state, and the revert happens on the client. (2) The save that ignores other-side fields (added 2026-10-15) is in the backend populator, `com.dotcms.rest.MapToContentletPopulator`, which is modern code.
- **Related known decisions**: None known. The plan consults `dotCMS/platform-adrs` as usual.

## Root-Cause Hypothesis

1. `lockContent` and `unlockContent` in the Edit Content store (`store/features/lock/lock.feature.ts`) update the lock fields (`locked`, `lockedBy`, `lockedByName`, `lockedOn`) by putting a **new `contentlet` object** in the store.
2. The Relationship field (`dot-relationship-field.component.ts`) builds its `$inputs` from the `contentlet` input. It passes that signal to the store's `initialize` method, which re-runs every time `$inputs` changes. This is on purpose: manual translation and populate locale need the field to re-initialize without being recreated.
3. So a new `contentlet` object, even one whose only change is the lock state, makes the Relationship store re-initialize from the saved relationships. That overwrites the editor's pending changes.

The Category field also takes the `contentlet`, but it reads it once when it is created, so it is not affected. The plan should confirm that no other field re-initializes when the `contentlet` object changes.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Unsaved Relationship field changes (items added, removed or reordered) survive **Lock for Editing**, **Unlock** and **Release Lock**.
- The fix keeps the cases where the Relationship field *must* re-initialize: another contentlet or version is loaded, manual translation, and populate locale.
- Unit tests (Vitest / Spectator) that reproduce the revert and fail without the fix.

**In scope (added 2026-10-15, developer-approved)**:

- Saving a Relationship change made from the **other-side field** of a two-sided relationship whose relationship row has an empty `parent_relation_name` is persisted. This is the case for every two-sided relationship imported from the demo starter, e.g. `Blog.author` → `BlogAuthor.blog`.
  - `ImportStarterUtil` skips a starter relationship whose `relation_type_value` already exists, so the other-side name is lost.
  - `MapToContentletPopulator.getRelationshipQuery` then cannot map the field, so the save keeps the old relationships. This happens with or without Lock/Unlock, and through the REST API too.
- Fix: the populator also resolves a relationship through the content type's relationship field whose `relationType` equals the relationship's `relationTypeValue`, the same way the field is resolved when it is read.

**Explicitly out of scope / non-goals**:

- No change to the starter import and no data migration. Existing rows with an empty `parent_relation_name` keep working through the populator fix.

- No change to the lock / unlock REST endpoints or to the lock rules in the backend.
- No change to how other fields handle the lock state. They are not affected, as the Title check shows.
- No new "unsaved changes" warning, and no change to the lock button's labels or flow.
- No refactor of the Relationship field store beyond what the fix needs.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - The Relationship field's re-initialization is shared by several flows: first load, switching contentlet or version, manual translation, populate locale, and the in-place navigation between related content. A fix that stops re-initialization too broadly would leave the field showing the wrong content after a locale or version change.
  - Changing how the store applies the lock state could affect anything that reads `contentlet()`: the sidebar lock button, workflow actions, and `canLock`.
- **Backward compatibility**: Frontend state, plus one backend change: the populator also maps other-side relationship fields when the relationship row has no other-side name. This changes behavior only for saves that were silently dropped before; relationships are not versioned, so a wrong mapping would affect live content, which the integration tests guard against. No API, serialized content, DB or ES mapping changes. Rollback-safe.
- **Data considerations**: None. Nothing is saved wrongly; the bug only loses unsaved changes in the browser.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With the content locked, an unsaved change to a Relationship field is still shown after clicking **Unlock**, and it is the value saved with the content.
- **AC-002**: With the content unlocked, an unsaved change to a Relationship field is still shown after clicking **Lock for Editing**.
- **AC-003**: An unsaved Relationship field change is still shown after **Release Lock** on content locked by another user.
- **AC-004**: If the lock or unlock request fails, the Relationship field keeps the unsaved changes.
- **AC-005**: After lock and unlock, the lock state, the Lock / Unlock button label and the workflow actions update as they do today.
- **AC-007**: On a two-sided relationship with an empty `parent_relation_name`, emptying the other-side field and saving removes the relationship.
- **AC-008**: On the same relationship, relating an item from the other-side field and saving relates it.
- **AC-006** (regression): The Relationship field still re-initializes when another contentlet or version is loaded, and for manual translation and populate locale.
- **Verification method**:
  - New Vitest / Spectator specs for the Relationship field: a `contentlet` input that changes only its lock fields does **not** reset pending changes, while a change of contentlet, field or locale **does** re-initialize. No new lock-feature spec: `lock.feature.ts` is unchanged, and its existing specs (including the error paths for AC-004) must stay green. The developer approved this at T007.
  - New integration tests in `MapToContentletPopulatorTest` for AC-007 and AC-008: `…WithoutParentRelationName_emptyValueWipesIt` and `…WithoutParentRelationName_identifierRelatesIt`. Run them with `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=MapToContentletPopulatorTest -Dmaven.build.cache.enabled=false`.
  - The specs must fail before the fix (TDD Red).
  - Run them with `cd core-web && pnpm nx test edit-content`.
  - Manual check: the reproduction steps above, in both directions, on a local instance.

## Assumptions

- The behavior is the same for every Relationship field, whatever the cardinality and whatever the content type; the reproduction used a one-to-many Author relationship.
- Release Lock follows the same store path as Unlock (`unlockContent`), so it has the same bug. It was not reproduced in the browser.
- Spec and implementation ship in one branch and one PR. The fix covers the frontend lock revert (US1) and the backend save from other-side relationship fields (US2). Because it touches the backend, the merge queue runs the integration suite.
