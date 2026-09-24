# Phase 1 — Data Model: Editing a Variant While Its Experiment Is Live

**Feature**: #37308 | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-23

This change introduces **no new entity, no new field and no new request**. What follows is the
existing state it reads, the derived signals it adds, and the rules that govern them — which is what
`/speckit-tasks` needs in order to write tests before implementation.

## 1. Existing state consumed

| Source | Shape | Where it comes from | Notes |
|---|---|---|---|
| `store.pageExperiment` | `DotExperiment \| null \| undefined` | `withPageApi.ts`, from `dotExperimentsService.getById(experimentId)` | The single input for both the removed condition and the new banner. `undefined` for a page with no experiment **and** for a failed lookup — the two are indistinguishable, deliberately (FR-014) |
| `store.pageAsset()?.page.canEdit` | `boolean` | page API | Survives untouched (FR-023) |
| `store.$lockIsPageLocked()` | `boolean` | `withWorkflow.ts` via `computeIsPageLocked` | Survives untouched (FR-022) |
| `store.pageAsset()?.template.drawed` | `boolean` | page API | Survives untouched (FR-024) |
| `$lockOptions()` | `{ isLocked, isLockedByCurrentUser, lockedBy, canLock }` | `withWorkflow` | Drives the existing lock banner; the new banner must not alter it |
| `DotExperimentsConfigureStore.$isLocked()` | `boolean` | experiments portlet | `status !== DRAFT`. Read by the variant row's `editorMode` today. **Must not change** — see §2 |
| `DotExperimentsPanelStore` | signal store | `dot-experiments-panel.store.ts` | Already injected by the shell as `experimentsPanel`. `openResults(experimentId)` is the banner's link target (FR-017) |

### `DotExperiment` — fields this feature touches

`dotcms-models/src/lib/dot-experiments.model.ts`

| Field | Type | Use here |
|---|---|---|
| `id` | `string` | Passed to `openResults` by the banner link, and read by the confirmation's status lookup |
| `status` | `DotExperimentStatus` | Decides banner visibility, which copy, whether the confirmation is asked, and the variant's mode |
| `scheduling` | `RangeOfDateAndTime \| null` | **Nullable — neither surface may read it**. The toolbar tag reads `scheduling.endDate` unguarded; the banner must not inherit that exposure |

The model carries **neither `runningIds` nor `lookBackWindow`**. Both exist only on the backend
`AbstractExperiment`, which is why FR-027 pushes that verification to the integration test.

### `DotExperimentStatus` — the state machine that drives everything

`dotcms-models/src/lib/dot-experiments-constants.ts`

| Status | Page editable after this change | Banner | Copy | Variant opens in | Confirms first |
|---|---|---|---|---|---|
| `RUNNING` | yes | yes | running warning | `EDIT` (FR-001) | yes (FR-008) |
| `SCHEDULED` | yes | yes | scheduled warning | `EDIT` (FR-001) | yes (FR-008) |
| `DRAFT` | yes (unchanged) | no | — | `EDIT` (unchanged) | no (FR-011) |
| `ENDED` | yes (unchanged) | no | — | `PREVIEW` (unchanged, FR-006) | no |
| `ARCHIVED` | yes (unchanged) | no | — | `PREVIEW` (unchanged, FR-006) | no |
| none / lookup failed | yes | no | — | n/a | no |

The control variant opens in `PREVIEW` in **every** row of this table (FR-005) and never confirms
(FR-011).

## 2. Derived state this change modifies

### `editorHasAccessToEditMode` — `withEditor.ts`

```
before: canEdit && !(status in {RUNNING, SCHEDULED}) && !locked
after:  canEdit && !locked
```

Feeds `editorCanEditContent` (`&& viewMode === EDIT`) and the mode selector directly.

### `hasPermissionToEditLayout` — `withEditor.ts`

```
before: canEdit && drawed && !(status in {RUNNING, SCHEDULED}) && !locked
after:  canEdit && drawed && !locked
```

The compound expression is the hazard: exactly one term is removed, three remain (FR-022, FR-023,
FR-024).

### `computeCanEditPage()` — `utils/index.ts`

Same removal. **No production caller** — corrected for consistency, not for behavior (research.md
R1). It must not be cited as evidence for FR-001 or FR-002.

### `editorMode` — the `VariantRowViewModel` mapping in `dot-experiments-configure-variants.component.ts`

```
before: isControl || experimentIsReadOnly              ? PREVIEW : EDIT
after:  isControl || status in {ENDED, ARCHIVED}       ? PREVIEW : EDIT
```

`isControl` stays (FR-005). **`DotExperimentsConfigureStore.$isLocked` itself must not change** — it
also freezes the configuration form's name, description, traffic allocation, goal, scheduling and
Save. The component does not read experiment status today, so this adds a dependency rather than
deleting a term.

### `onEditContent(row, mode)` — same component

Gains the confirmation gate in front of its existing body. The body's order — suspend the panel, then
navigate — is unchanged and moves **whole** into the accept branch (FR-009). See
[contracts/variant-edit-confirmation.contract.md](./contracts/variant-edit-confirmation.contract.md).

## 3. Derived state this change adds

### `$showExperimentBanner` — new, `dot-ema-shell.component.ts`

```
true  ⟺  pageExperiment()?.status ∈ {RUNNING, SCHEDULED}
```

Not gated on `canEdit` (FR-015), not on view mode (A3), not on lock (FR-016). No dismissal state —
the existing `$showBanner` signal must not be consulted (FR-013). Rendered inside the same
`$canRead()` gate the lock banner uses.

### `$experimentWarningKey` — new, `dot-ema-shell.component.ts`

| `status` | key |
|---|---|
| `RUNNING` | `uve.shell.experiment.running.edit.warning` |
| `SCHEDULED` | `uve.shell.experiment.scheduled.edit.warning` |

A single shared key is forbidden (FR-018).

### `onExperimentBannerLink()` — new, `dot-ema-shell.component.ts`

Calls `experimentsPanel.openResults(pageExperiment().id)` and nothing else — no navigation, no
reload (FR-017, SC-009). Unconditional: the panel store does not read
`FEATURE_FLAG_EXPERIMENTS_PORTLET`, which is what lets FR-017 hold on a stock build (research.md
R10).

### The confirmation gate — new, `dot-experiments-configure-variants.component.ts`

```
confirm  ⟺  resolved mode is EDIT  ∧  status ∈ {RUNNING, SCHEDULED}
```

No preference, flag or per-session memory participates (FR-010).

## 4. New message keys

`dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`, following the existing
`uve.shell.page.locked.*` and `experiments.configure.variants.*` conventions.

| Key | Constraint |
|---|---|
| `uve.shell.experiment.running.edit.warning` | Conveys that results will mix data from before and after (FR-019). No "invalidate" (FR-021) |
| `uve.shell.experiment.scheduled.edit.warning` | Informational, not cautionary — a scheduled experiment has nothing to pollute (FR-020) |
| `uve.shell.experiment.view.experiment` | Banner link label |
| `experiments.configure.variants.edit-live.confirm.header` | Names the condition: this variant belongs to a live experiment |
| `experiments.configure.variants.edit-live.confirm.running` | States the cost as a consequence to accept, never a loss to avert (A2, FR-021) |
| `experiments.configure.variants.edit-live.confirm.scheduled` | Edits before the start become part of what is measured (FR-020's framing) |

The legacy `experiment.running.edit.confirmation` and
`experiment.running.edit.lock.confirmation.note` carry the phrasing FR-021 forbids. They are left in
place and not reused.

## 5. Render-order rule (FR-016)

Within the shell template, when both are eligible:

```
1. lock banner       (existing, unchanged)
2. experiment banner (new)
```

A property a test asserts, not an incidental consequence of template order.

## 6. Freshness and lifecycle

`pageLoad` clears `editorSelected` and `editorContentArea` but **not** `pageExperiment`
(`withPageApi.ts`). Consequences the tasks must respect:

- A mode change or reload does not flicker the banner — it holds its value until the fetch resolves.
- Navigating page-to-page briefly carries the previous page's experiment. Self-correcting, matches
  the toolbar tag's existing behavior, and a test asserting banner content immediately after a
  navigation must account for it.
- No polling and no new request (A4). A status that changes server-side mid-session is reflected on
  the next refresh of that value, not instantly.

## 7. Backend state asserted by the integration test (FR-027)

Not modified by this change — asserted to be unmodified.

| Entity | Assertion after an edit to a variant during `RUNNING` |
|---|---|
| Experiment `id`, variants | unchanged (FR-025) |
| `runningIds` | unchanged; no new run started, no running id rotated (FR-025) |
| `lookBackWindow` | unchanged (FR-025) |
| `scheduling` | unchanged, start and end dates (FR-025) |
| Collected measurements for the pre-edit period | unchanged, still returned (FR-026) |
| Variant that received the write | **reported** — the assertion that answers the variant-storage question the spec parks in Out of Scope (FR-004) |
