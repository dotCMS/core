# Issue Resolution Specification: Workflow "Allow Comments" dialog not showing up in new Edit Mode

**Feature Branch**: `36883-workflow-comments-dialog-new-edit`

**Created**: 2026-09-07

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#36883](https://github.com/dotCMS/core/issues/36883)

**Input**: User description: "https://github.com/dotCMS/core/issues/36883 — In the new (Angular) Edit Content UI, executing a workflow action that has **Allow Comments** enabled does not show the comment dialog on its first execution. The contentlet is saved/transitioned correctly, but the author is given no opportunity to enter the comment — the dialog only appears if the same action is triggered a second time."

## Problem Statement *(mandatory)*

In the new (Angular) Edit Content UI, firing a workflow action that has **Allow Comments**
(and/or **Assign to**) enabled skips the input dialog on its first execution. The action runs to
completion — the contentlet is saved and transitioned — but the author never gets the chance to
enter the comment that the workflow was explicitly configured to collect. The dialog only shows
up if the *same* action is fired a second time, which by then means running the action twice.

In the legacy (classic) Edit Content UI the dialog appears correctly on the first execution, so
the two editors disagree on behavior for the same workflow configuration.

The comment is not merely cosmetic: it is the audit/hand-off note recorded in the workflow task
history. Losing it silently degrades the workflow audit trail without any error being surfaced.

**Severity / Impact**: Medium. Affects every author using a content type that has the **new edit
mode** enabled, on any workflow action configured with **Allow Comments** (or **Assign to** /
**Move** without a preset path — everything that produces an action input). It happens on the
*first* execution of the action for content that has not yet been saved through that editor
session, which in practice is the most common case: creating new content. The action itself
succeeds, so the failure is silent — no error, no warning, just a missing dialog and a missing
comment.

## Reproduction *(mandatory)*

**Environment**: Latest `main` / local dev environment. Reproduced independently of the Angular
21→22 migration tracked in #35930 (i.e. it is a pre-existing defect, not a migration regression).
Browser/OS not specified by the reporter; not believed to be browser-dependent. Requires a
content type with the **new edit mode** enabled and a workflow action with **Allow Comments**
checked.

**Steps to Reproduce**:

1. Create a test Content Type with a single text field for the title. Leave the new edit mode
   **disabled** for now.
2. Go to **System Workflow** and, for the `Publish` action, check the **Allow Comments** box.
3. Create a Contentlet of that test type and click `Publish`. Enter any value in the comment box
   and click `Send`. The dialog appears as expected and everything works (this is the legacy
   edit mode baseline).
4. Go back to the test Content Type definition and **enable the new (Angular) edit mode**.
5. Create another Contentlet of that type and click `Publish`.
6. Observe: the contentlet is published, but no comment dialog was shown.
7. Click `Publish` again on the now-saved contentlet. The comment dialog now appears.

**Expected Behavior**: The comment dialog appears on the **first** click of `Publish`, exactly as
it does in the legacy edit mode. The workflow action does not run until the author submits or
cancels the dialog.

**Actual Behavior**: The workflow action fires immediately with no dialog and no comment. The
dialog appears only on a subsequent execution of the same action, once the contentlet has been
saved and its allowed actions have been re-fetched.

**Reproducibility**: Always, for content that has **no inode yet**. There are three such paths,
all seeded from the same input-less payload:

1. **New content, single workflow scheme** — the reported repro. The scheme is auto-selected and
   the actions are seeded during initialization.
2. **New content, multiple workflow schemes** — the author picks a scheme in the sidebar and the
   actions are seeded from that pick.
3. **New translation of an untranslated locale** — the copy-to-locale flow seeds the same way.

Once the content has been saved through the new editor, the actions are re-fetched from the
per-inode endpoint and the dialog behaves correctly.

**Does *not* reproduce after a Reset action.** An earlier draft of this spec asserted that it did.
That was reasoned, not observed, and it is wrong: after a Reset the action list has already been
re-fetched per-inode (so it carries its inputs), and re-picking a scheme on content that now has
an inode does not overwrite it. Recorded here so the claim is not resurrected. See
[Clarifications Q2](#clarifications).

## Scope of Investigation *(mandatory)*

- **Affected area**: Workflows + the new (Angular) Edit Content UI. Specifically the workflow
  action list that the new editor uses to decide whether an action needs to collect input before
  firing, and the REST endpoints that feed it.
- **Suspected surface**: Primarily **frontend** — `core-web/libs/edit-content` (the edit-content
  signal store's workflow/content features and `DotEditContentFormComponent`) plus
  `core-web/libs/data-access` (`DotWorkflowsActionsService`, `DotWorkflowEventHandlerService`).
  The root cause originates in a **modern backend** view contract
  (`com.dotcms.rest.api.v1.workflow.*` — `WorkflowResource`, `WorkflowDefaultActionView`,
  `WorkflowActionView`), so the fix may land on either side or both. No `com.dotmarketing.*`
  legacy code is expected to be touched; the legacy edit mode reaches this data through a
  different path and must be left alone.
- **Related known decisions**: The new Edit Content UI is expected to reach behavioral parity
  with the legacy editor for workflow execution. The plan phase formally consults
  `dotCMS/platform-adrs`; of particular interest is anything covering the workflow REST view
  contract, since one candidate fix changes what an existing endpoint returns.

## Root-Cause Hypothesis

The new editor decides whether to open the input wizard purely from the action's `actionInputs[]`
array, and for unsaved content that array is never populated — so it takes the "no inputs, fire
directly" branch.

Two different REST shapes describe a workflow action:

- `WorkflowActionView` (`convertToWorkflowActionView` in `WorkflowResource`) — carries
  `actionInputs[]`, derived by `createActionInputViews` from `isAssignable()`, `isCommentable()`,
  `hasPushPublishActionlet()` and the move actionlet.
- `WorkflowDefaultActionView` — a plain `{ scheme, action, firstStep }` wrapper around the raw
  `WorkflowAction`. It has **no** `actionInputs[]` at all.

The endpoints the new editor uses for content that has no inode yet —
`GET /api/v1/workflow/initialactions/contenttype/{id}` (`getDefaultActions`) and
`GET /api/v1/workflow/defaultactions/contenttype/{id}` (`getWorkFlowActions`) — both return
`WorkflowDefaultActionView`. The per-inode endpoint
`GET /api/v1/workflow/contentlet/{inode}/actions` (`getByInode`) returns `WorkflowActionView`.

Three places in `core-web/libs/edit-content` seed `currentContentActions` from that input-less
payload — one per reproduction path above:

| # | Location | When it runs |
|---|---|---|
| 1 | `store/features/content/content.feature.ts:211-218` (`initializeNewContent`) | New content, **single scheme** — auto-selected, seeded during init. **This is the reported repro's path.** |
| 2 | `store/features/workflow/workflow.feature.ts:192-197` (`setSelectedWorkflow`, `isNew` branch) | New content, **multi-scheme** — author picks a scheme in the sidebar |
| 3 | `store/features/locales/locales.feature.ts:268-273` | New translation of an untranslated locale |

Path 2 has exactly one caller — `(onSelectWorkflow)` in
`components/dot-edit-content-sidebar/dot-edit-content-sidebar.component.html:172` — so it is
reached only by a manual scheme pick, which the single-scheme repro never performs.

Then in
`core-web/libs/edit-content/src/lib/components/dot-edit-content-form/dot-edit-content-form.component.ts`,
`fireWorkflowAction` does `const { actionInputs = [] } = workflow;` and, on
`if (!actionInputs.length)`, fires the action immediately instead of calling `openWizard`.

After that first execution, `fireWorkflowAction` in the store re-fetches with
`workflowActionService.getByInode(inode, DotRenderMode.EDITING)`, which *does* include
`actionInputs`. That is why the second click shows the dialog — and it is also why existing
content opened directly in the new editor behaves correctly, since `initializeExistingContent`
already loads its actions via `getByInode`.

The legacy editor does not consult `actionInputs`; it reads the raw `commentable` / `assignable`
flags, which is why it is unaffected.

**Verified fact bearing on the fix**: every flag `createActionInputViews` reads is *already*
serialized on the raw `WorkflowAction` embedded in `WorkflowDefaultActionView` — `commentable`
(`isCommentable()`), `assignable` (`isAssignable()`), `hasPushPublishActionlet`,
`hasMoveActionletActionlet` / `hasMoveActionletHasPathActionlet` (all four `@JsonProperty`), plus
`nextAssign` and `roleHierarchyForAssign`. None is `@JsonIgnore`
(`com.dotmarketing.portlets.workflows.model.WorkflowAction`). The default/initial-action payload
therefore already carries everything needed to determine an action's inputs — what is missing is
only the derived `actionInputs[]` array itself.

The plan phase will confirm this and choose between the candidate fixes (backend: have the
default/initial-action endpoints emit `actionInputs`; frontend: derive the inputs from the raw
action flags, or fetch an input-bearing action list for unsaved content). Given the fact above, a
frontend-only fix is viable **without any REST contract change**; the trade-off against it is that
it duplicates in TypeScript a derivation rule that today lives only in Java. Weighing that belongs
in the plan, not here.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- The new (Angular) Edit Content UI opens the workflow input dialog on the **first** execution of
  any action that declares an input (comment, assign, push publish, move-without-path), including
  for content that has not been saved yet.
- The workflow action does not execute until the author submits or cancels that dialog, matching
  legacy gating.
- All **three** seeding paths in the Root-Cause Hypothesis, not just the reported one — they share
  a single input-less source, so a fix that covers one should cover all three.
- Whatever contract change this requires, **if any**, between `WorkflowResource`'s
  default/initial action endpoints and the edit-content store, kept minimal and additive. A
  frontend-only fix requiring no contract change at all is in scope and, per the Root-Cause
  Hypothesis, appears achievable.
- Regression coverage (see Acceptance & Verification).

**Explicitly out of scope / non-goals**:

- Any change to the legacy (classic) Edit Content UI or the code paths it uses.
- Redesigning the workflow wizard (`DotWizardComponent` / `DotCommentAndAssignFormComponent`) or
  its UX.
- Reworking `DotWorkflowEventHandlerService` / `DotWizardService` beyond what this fix needs; the
  wizard mechanics themselves are working (they work on the second click).
- ~~Fixing the *other* consumers of the input-less default-actions endpoints (UVE toolbar, content
  drive context menu, create-content dialog, bulk actions)…~~ **Withdrawn — the premise was
  false.** `getDefaultActions` / `getWorkFlowActions` have exactly three call sites, all inside
  `libs/edit-content`, and all three are in scope (see Root-Cause Hypothesis). The UVE toolbar,
  Content Drive context menu and bulk actions read actions through per-inode or bulk endpoints,
  which already carry `actionInputs`. There is no latent defect there and **no follow-up issue is
  needed**.
- Cleaning up the unrelated missing null-guard in `openWizard` (where `setWizardInput` may return
  `null`) unless the chosen fix makes it reachable.
- Backporting to LTS branches.

## Regression Risk *(mandatory)*

- **Blast radius**:
  - If the fix is on the **backend**, `GET /api/v1/workflow/initialactions/contenttype/{id}` and
    `GET /api/v1/workflow/defaultactions/contenttype/{id}` are shared by more than the new
    editor — the content-drive folder context menu, the create-content dialog, the legacy
    edit-content bootstrap, and any customer integration reading them. An **additive** field is
    low risk; changing or removing existing fields is not acceptable.
  - If the fix is on the **frontend**, the blast radius is `withWorkflow` /
    `setSelectedWorkflow` and `DotEditContentFormComponent.fireWorkflowAction` — used by the
    full-screen editor, the Content Drive side-panel editor, and the create-content dialog. All
    three must keep working, and actions with genuinely no inputs must keep firing directly
    with no spurious dialog.
  - Push Publish actions route through a distinct branch of `fireWorkflowAction`
    (`containsPushPublish` → `checkPublishEnvironments` → wizard), but it is gated on the same
    `actionInputs` check, so the fix reaches it too. Whether Push Publish is reachable at all as an
    initial action on unsaved content is unverified and it is not an AC — see the note under
    Acceptance. The "no publish environments configured" notification path must keep working.
- **Backward compatibility**: Nothing here *requires* a REST change — a frontend-only fix leaves
  every contract untouched, which is the lower-risk path. Should the plan pick the backend route,
  the response of a published REST endpoint changes, and then: any change
  must be **additive only** (`actionInputs[]` appears where it previously did not); no existing
  property may be renamed, retyped, or dropped. `@Schema` annotations must match the actual
  return type and `openapi.yaml` must be regenerated and committed alongside. An additive JSON
  field is rollback-safe; a changed or removed one is not.
- **Data considerations**: None. No DB schema, no Elasticsearch mapping, no stored state. No
  repair of existing data is needed — comments that were silently dropped by this bug are not
  recoverable and are out of scope.

## Acceptance & Verification *(mandatory)*

- **AC-001**: With a content type using the **new** edit mode and a workflow action that has
  **Allow Comments** enabled, creating a new contentlet and clicking that action shows the
  comment dialog on the **first** click — matching the legacy edit mode.
- **AC-002**: The workflow action does not complete until the author submits the dialog. While the
  dialog is open nothing is persisted and no saving indicator is shown. On **cancel**:
  - for **new** content (which does not exist yet): nothing is persisted, the editor stays on the
    new-content route, **the form keeps every value the author typed**, and firing the action a
    second time still works — no stuck saving state, no cleared form values;
  - for **existing** content: the contentlet is left unchanged and untransitioned.
- **AC-003**: A single execution produces exactly one workflow execution — one version and one
  workflow history/audit entry. The comment the author typed is present on that history entry.
- **AC-004**: Verified against at least one **non-`Publish`** workflow action with **Allow
  Comments** enabled (and ideally on a non-System workflow scheme), confirming the fix is not
  scoped to the System Workflow's default `Publish` action.
- **AC-005**: Actions with **no** inputs still fire directly on the first click, with no dialog and
  no extra round-trip. *(Guards the opposite failure mode: over-triggering the dialog.)*
- **AC-006**: An action with **Assign to** enabled presents its assign input on the first click on
  content with no inode.
- **AC-007**: AC-001 holds on all three seeding paths from the Root-Cause Hypothesis: new content
  with a single scheme, new content with multiple schemes (author picks one), and a new
  translation of an untranslated locale.
- **AC-008**: Legacy (classic) edit mode behavior is unchanged for the same content type and
  workflow action.
- **AC-009** *(contingent, not expected)*: The chosen approach is expected to change no REST
  response. **If** the plan nonetheless changes one, that change is additive; `@Schema` matches the
  actual return type and the regenerated `openapi.yaml` is committed with the Java change.

**Deliberately not an AC — Push Publish on unsaved content.** An earlier draft asserted that Push
Publish actions "present their inputs on the first click on unsaved content". Push Publish is
reachable as an initial action only if an administrator places it in the workflow's first step; it
is not reachable in a default configuration, and this was not verified. It shares the same
`actionInputs` code path, so the fix covers it if reachable, but it is not made an acceptance
criterion. The pre-existing behavior where `checkPublishEnvironments()` returning `false` silently
does nothing is likewise out of scope. See [Clarifications Q3](#clarifications).

**Removed — post-Reset behavior.** A previous AC-006 asserted the defect reproduced after a Reset
action. It does not; see Reproducibility. No replacement AC is added, because there is no defect
to guard against on that path.

- **Verification method**:
  - **Frontend unit (Jest + Spectator)** — `pnpm nx test edit-content`:
    - `dot-edit-content-form.component.spec.ts`: given a workflow action carrying a `commentable`
      input on **unsaved** content, `fireWorkflowAction` opens the wizard and does **not** call
      `$store.fireWorkflowAction` until the wizard emits; given an action with no inputs it fires
      directly.
    - `content.feature.spec.ts`: `initializeNewContent` seeds `currentContentActions` with actions
      that carry their inputs. **This is the reported repro's path and therefore the primary
      test** — an earlier draft assigned it to `workflow.feature.spec.ts`, which covers only the
      multi-scheme variant.
    - `workflow.feature.spec.ts`: same assertion for `setSelectedWorkflow` (multi-scheme pick).
    - `locales.feature.spec.ts`: same assertion for the new-translation path.
  - **Backend integration and Postman** — *not expected to be needed.* The approach anticipated by
    this spec changes no Java, and per
    [ADR-0013](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0013-skip-integration-and-postman-tests-for-frontend-only-changes-in-merge-queue-to-increase-flow.md)
    a `core-web/**`-only PR skips both suites in the merge queue, so they would never act as a
    gate. They become required **only if** the plan takes the backend route (see AC-009).
  - **E2E (Playwright)** — new edit mode + an action with **Allow Comments** → dialog appears on
    the first click, submitting it records the comment, cancelling aborts the action. Satisfies
    the issue's "regression test added (e2e or component)" criterion; if e2e proves impractical
    for this flow, the component-level specs above stand in and the plan must say so explicitly
    (per constitution Principle V, silence is not consent).
  - **Manual** — the reproduction steps above, plus AC-004, AC-007 and AC-008.

## Clarifications

Raised by @nicobytes on [PR #37437](https://github.com/dotCMS/core/pull/37437) and verified
against the code before answering. Q1 and Q2 found real errors in this spec; both are corrected
above.

**Q1 — Which seeding points are in scope? `setSelectedWorkflow` is not the repro's path.**
**Confirmed.** `initializeNewContent` (`content.feature.ts:211-218`) auto-selects the scheme when
`schemeIds.length === 1` and seeds the actions there; `setSelectedWorkflow` has one caller, the
sidebar's `(onSelectWorkflow)`, so the single-scheme repro never reaches it. The Root-Cause
Hypothesis now names all three seeding points with path 1 as primary, and the new-translation path
is explicitly in scope. The primary store test moves to `content.feature.spec.ts`.

**Q2 — Does the Reset case actually reproduce? — Answer (b): it was reasoned, not reproduced, and
it is wrong.** On every fire including Reset, `currentContentActions` is patched from
`getByInode(inode, EDITING)`, which carries `actionInputs`. Re-picking a scheme afterwards runs
`setSelectedWorkflow` with `isNew === false` (the contentlet has an inode), taking the `else`
branch, which does not overwrite `currentContentActions`. The Reproducibility claim and AC-006 are
removed. The separate concern Q2 raises — that after a Reset the retained list may be stale for the
newly picked scheme — is a distinct potential defect and is not addressed here.

**Q3 — Expected first-click behavior for Push Publish on unsaved content?** Push Publish is
reachable as an initial action only if an administrator puts it in the workflow's first step
(`WorkflowHelper#findInitialAvailableActionsByContentTypeSkippingSeparators` builds from an empty
contentlet); not reachable by default, and unverified. It is therefore no longer an AC. The
observation that `checkPublishEnvironments()` has no `else` branch is correct but pre-existing and
out of scope. AC-005 has been split: no-input actions (AC-005) and Assign-to (AC-006) are now
separate.

**Q4 — What does "unchanged" mean for content that does not exist yet? — Answer (a).** AC-002 now
states it: nothing persisted, editor stays on the new-content route, form values survive, and a
second fire still works. The related point is also correct — `ComponentStatus.SAVING` and the
processing toast are set inside `$store.fireWorkflowAction`, which the fix leaves *after* the
dialog, so no saving indicator appears while the dialog is open. That is now part of AC-002.

**Q5 — Is the backend route genuinely open? — Answer (a): frontend-only is the intended default.**
The backend route is a documented fallback, not an equal fork. AC-008 became AC-009 and is marked
*contingent, not expected*; the integration/Postman lines are marked not-expected for the same
reason, reinforced by ADR-0013. The Red gate is unambiguously the frontend specs. The hand-off note
about a single shared helper plus a comment pointing back at `WorkflowResource#createActionInputViews`
matches what the plan already specifies.

## Assumptions

- The reporter's "new edit mode" means the per-content-type flag that routes editing to the
  Angular edit-content UI (`core-web/libs/edit-content`), not the UVE page editor.
- The defect is not browser- or OS-specific; the reporter did not specify one and the suspected
  cause is data-shape, not rendering.
- "Allow Comments" is the workflow action's `commentable` flag, surfaced as the `commentable`
  action input.
- The `Publish` action in the repro is the System Workflow's default `Publish`; AC-004 exists
  precisely to prove the fix is not specific to it.
- The comment collected must be persisted to the workflow task history exactly as it is in legacy
  edit mode; no new comment semantics are introduced.
- The fix targets `main`. No LTS backport is assumed unless a maintainer labels the issue for one.
