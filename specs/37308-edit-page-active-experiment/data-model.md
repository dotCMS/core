# Phase 1 — Data Model: Editing a Page While an Experiment Is Active

**Feature**: #37308 | **Plan**: [plan.md](./plan.md) | **Date**: 2026-09-15

This change introduces **no new entity, no new field and no new request**. What follows is the
existing state it reads, the derived signals it adds, and the rules that govern them — which is what
`/speckit-tasks` needs in order to write tests before implementation.

## 1. Existing state consumed

| Source | Shape | Where it comes from | Notes |
|---|---|---|---|
| `store.pageExperiment` | `DotExperiment \| null \| undefined` | `withPageApi.ts:282`, from `dotExperimentsService.getById(experimentId)` | The single input for both the removed condition and the new banner. `undefined` for a page with no experiment **and** for a failed lookup — the two are indistinguishable, deliberately (FR-014) |
| `store.pageAsset()?.page.canEdit` | `boolean` | page API | Survives untouched (FR-023) |
| `store.$lockIsPageLocked()` | `boolean` | `withWorkflow.ts:118` via `computeIsPageLocked` | Survives untouched (FR-022) |
| `store.pageAsset()?.template.drawed` | `boolean` | page API | Survives untouched (FR-024) |
| `$lockOptions()` | `{ isLocked, isLockedByCurrentUser, lockedBy, canLock }` | `withWorkflow` | Drives the existing lock banner; the new banner must not alter it |

### `DotExperiment` — fields this feature touches

`dotcms-models/src/lib/dot-experiments.model.ts:12`

| Field | Type | Use here |
|---|---|---|
| `id` | `string` | Banner's report link |
| `pageId` | `string` | Banner's report link |
| `status` | `DotExperimentStatus` | Decides banner visibility and which copy |
| `scheduling` | `RangeOfDateAndTime \| null` | **Nullable — the banner must not read it**. The toolbar tag reads `scheduling.endDate` unguarded; the banner must not inherit that exposure |

Note the model carries **neither `runningIds` nor `lookBackWindow`**. Both exist only on the backend
`AbstractExperiment`, which is why FR-027 pushes that verification to the integration test.

### `DotExperimentStatus` — the state machine that drives everything

`dotcms-models/src/lib/dot-experiments-constants.ts:35-41`

| Status | Page editable after this change | Banner | Copy | Variant opens in |
|---|---|---|---|---|
| `RUNNING` | yes | yes | running warning | `EDIT` (FR-001) |
| `SCHEDULED` | yes | yes | scheduled warning | `EDIT` (FR-001) |
| `DRAFT` | yes (unchanged) | no | — | `EDIT` (unchanged) |
| `ENDED` | yes (unchanged) | no | — | `PREVIEW` (unchanged, FR-006) |
| `ARCHIVED` | yes (unchanged) | no | — | `PREVIEW` (unchanged, FR-006) |
| none / lookup failed | yes | no | — | n/a |

## 2. Derived signals this change modifies

### `editorHasAccessToEditMode` — `withEditor.ts:96`

```
before: canEdit && !(status in {RUNNING, SCHEDULED}) && !locked
after:  canEdit && !locked
```

Feeds `editorCanEditContent` (`&& viewMode === EDIT`) and the mode selector directly.

### `hasPermissionToEditLayout` — `withEditor.ts:114`

```
before: canEdit && drawed && !(status in {RUNNING, SCHEDULED}) && !locked
after:  canEdit && drawed && !locked
```

The compound expression is the hazard: exactly one term is removed, three remain.

### `computeCanEditPage()` — `utils/index.ts:672`

Same removal. **No production caller** — corrected for consistency, not for behavior (R1).

### `editorMode` — `dot-experiments-configure-variants.component.ts:193` (FR-001)

```
before: isControl || $isLocked()                     ? PREVIEW : EDIT
after:  isControl || status in {ENDED, ARCHIVED}     ? PREVIEW : EDIT
```

`isControl` stays (spec FR-005). **`$isLocked` itself must not change** — it also freezes the
configuration form's name, description, traffic allocation, goal, scheduling and Save
(`dot-experiments-configure.component.ts:364-424`, `…-footer.component.ts:72,101`,
`…-page.component.ts:141,152`). The component does not read experiment status today, so this adds a
dependency rather than deleting a term.

## 3. Derived signals this change adds

### `$showExperimentBanner` — new, `dot-ema-shell.component.ts`

```
true  ⟺  pageExperiment()?.status ∈ {RUNNING, SCHEDULED}
```

Not gated on `canEdit` (FR-015), not gated on view mode (A3), not gated on lock (FR-016). No
dismissal state — the existing `$showBanner` signal must not be consulted (FR-015).

### `$experimentWarningKey` — new, `dot-ema-shell.component.ts`

| `status` | key |
|---|---|
| `RUNNING` | `uve.shell.experiment.running.edit.warning` |
| `SCHEDULED` | `uve.shell.experiment.scheduled.edit.warning` |

A single shared key is forbidden (FR-018).

## 4. New message keys

`dotCMS/src/main/webapp/WEB-INF/messages/Language.properties`, `uve.shell.experiment.*` namespace,
following the `uve.shell.page.locked.*` convention at lines 7047-7051.

| Key | Constraint |
|---|---|
| `uve.shell.experiment.running.edit.warning` | Must convey results will mix data from before and after (FR-019). Must not contain "invalidate" (FR-021) |
| `uve.shell.experiment.scheduled.edit.warning` | Must be informational, not cautionary — a scheduled experiment has nothing to pollute (FR-020) |
| `uve.shell.experiment.view.experiment` | Link label |

The legacy strings at `Language.properties:6042-6043` must not be reused and their phrasing must not
be carried forward (FR-021). They are left in place.

## 5. Render-order rule (FR-016)

Within the shell template, when both are eligible:

```
1. lock banner       (existing, unchanged)
2. experiment banner (new)
```

This is a property a test asserts (FR-016), not an incidental consequence of template order.

## 6. Freshness and lifecycle

`pageLoad` clears `editorSelected` and `editorContentArea` but **not** `pageExperiment`
(`withPageApi.ts:191-202`). Consequences the tasks must respect:

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
| Experiment `id`, variants | unchanged |
| `runningIds` | unchanged; no new run started, no running id rotated |
| `lookBackWindow` | unchanged |
| `scheduling` | unchanged (start and end dates) |
| Collected measurements for the pre-edit period | unchanged, still returned |
| Variant that received the write | **reported** — the assertion that answers the open variant-storage question the spec parks under Out of Scope |
