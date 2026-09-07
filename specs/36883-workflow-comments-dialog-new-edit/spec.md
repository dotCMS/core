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

**Reproducibility**: Always, for content that has not yet been saved in the current editor
session (new content). It also reproduces for existing content after a **Reset** action, because
resetting clears the selected scheme and the author must re-pick a workflow — which re-populates
the action list from the same input-less source. Once the content has been saved through the new
editor, the actions are re-fetched from the per-inode endpoint and the dialog behaves correctly.

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

In `core-web/libs/edit-content/src/lib/store/features/workflow/workflow.feature.ts`,
`setSelectedWorkflow` seeds `currentContentActions` from `schemes[currentSchemeId].actions` —
i.e. from the *default actions* payload, with no `actionInputs`. Then in
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
- The same correctness applies after a **Reset** action, where the author re-selects a scheme and
  the action list is re-seeded from the same source.
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
- Fixing the *other* consumers of the input-less default-actions endpoints (UVE toolbar, content
  drive context menu, create-content dialog, bulk actions) unless they turn out to share the
  exact same code path being changed. If the investigation shows they carry the same latent
  defect, that is reported as a follow-up issue, not absorbed here.
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
  - Push Publish actions on unsaved content go through the same `actionInputs` check
    (`containsPushPublish`), so they are affected by the same bug and by the same fix — the
    "no publish environments configured" notification path must keep working.
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
- **AC-002**: The workflow action does not complete (the contentlet is not saved or transitioned)
  until the author submits or cancels the dialog. Cancelling leaves the content unchanged and
  untransitioned.
- **AC-003**: A single execution produces exactly one workflow execution — one version and one
  workflow history/audit entry. The comment the author typed is present on that history entry.
- **AC-004**: Verified against at least one **non-`Publish`** workflow action with **Allow
  Comments** enabled (and ideally on a non-System workflow scheme), confirming the fix is not
  scoped to the System Workflow's default `Publish` action.
- **AC-005**: Actions with **no** inputs still fire directly on the first click, with no dialog
  and no extra round-trip. Actions with **Assign to** enabled, and Push Publish actions, also
  present their inputs on the first click on unsaved content.
- **AC-006**: The same behavior holds after a **Reset** action, when the author re-selects a
  workflow scheme and fires an action with **Allow Comments** on the re-seeded action list.
- **AC-007**: Legacy (classic) edit mode behavior is unchanged for the same content type and
  workflow action.
- **AC-008**: If the fix changes a REST response, the change is additive; `@Schema` matches the
  actual return type and the regenerated `openapi.yaml` is committed with the Java change.

- **Verification method**:
  - **Frontend unit (Jest + Spectator)** — `pnpm nx test edit-content`:
    - `dot-edit-content-form.component.spec.ts`: given a workflow action carrying a `commentable`
      input on **unsaved** content, `fireWorkflowAction` opens the wizard and does **not** call
      `$store.fireWorkflowAction` until the wizard emits; given an action with no inputs it fires
      directly.
    - `workflow.feature.spec.ts`: `setSelectedWorkflow` seeds `currentContentActions` with actions
      that carry their inputs.
  - **Backend integration** (only if the endpoints change) —
    `./mvnw verify -pl :dotcms-integration -Dcoreit.test.skip=false -Dit.test=WorkflowResourceIntegrationTest`
    (exact class/method confirmed in the plan): the default/initial-action endpoints expose
    `actionInputs[]` for a commentable action, and existing properties are unchanged.
  - **Postman** (only if the endpoints change) —
    `./mvnw verify -pl :dotcms-postman -Dpostman.test.skip=false -Dpostman.collections=Workflow`.
  - **E2E (Playwright)** — new edit mode + an action with **Allow Comments** → dialog appears on
    the first click, submitting it records the comment, cancelling aborts the action. Satisfies
    the issue's "regression test added (e2e or component)" criterion; if e2e proves impractical
    for this flow, the component-level specs above stand in and the plan must say so explicitly
    (per constitution Principle V, silence is not consent).
  - **Manual** — the reproduction steps above, plus AC-004, AC-006 and AC-007.

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
