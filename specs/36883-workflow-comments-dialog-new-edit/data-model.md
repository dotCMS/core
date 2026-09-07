# Phase 1 Data Model: Workflow action inputs (#36883)

**Plan**: [plan.md](./plan.md) · **Contract**: [contracts/derive-action-inputs.md](./contracts/derive-action-inputs.md)

No persisted entities are introduced or changed. The "model" here is the in-flight shape of a
workflow action as it crosses from REST into the edit-content store, and the derivation that
repairs it.

## Entities

### `DotCMSWorkflowAction` (existing — `libs/dotcms-models/src/lib/dot-workflow-action.model.ts`)

Unchanged. The fields this feature reads:

| Field | Type | Role in this feature |
|---|---|---|
| `assignable` | `boolean` | Source → `assignable` input |
| `commentable` | `boolean` | Source → `commentable` input (**the reported bug**) |
| `hasPushPublishActionlet` | `boolean?` | Source → `pushPublish` input |
| `hasMoveActionletActionlet` | `boolean?` | Source → `moveable` input |
| `hasMoveActionletHasPathActionlet` | `boolean?` | Suppresses `moveable` when a path is preset |
| `nextAssign` | `string` | Read later by `getAssignableData` when building the wizard step |
| `roleHierarchyForAssign` | `boolean` | Same |
| `actionInputs` | `DotCMSWorkflowInput[]` | **The gap.** Declared non-optional; absent in the default-actions payload until this fix. |

### `DotCMSWorkflowInput` (existing)

```ts
{ id: string; body: Record<string, unknown>; }
```

Derived entries always use `body: {}` — matching Java's `new ActionInputView(id, Collections.emptyMap())`.
The non-empty `body` is produced later and separately by
`DotWorkflowEventHandlerService.mergeCommentAndAssign`, which folds `assignable` + `commentable` +
`moveable` into a single `commentAndAssign` step and attaches the assignable data. **That merge is
not part of this feature** and must keep receiving the same input ids it always has.

## Derivation rules

Ordered, and the order matters — `mergeCommentAndAssign` preserves relative order when it filters,
and `setWizardInput` builds wizard steps in the resulting sequence.

| # | Condition | Emits `id` |
|---|---|---|
| 1 | `assignable === true` | `assignable` |
| 2 | `commentable === true` | `commentable` |
| 3 | `hasPushPublishActionlet === true` | `pushPublish` |
| 4 | `hasMoveActionletActionlet === true` **and** `hasMoveActionletHasPathActionlet !== true` | `moveable` |

No condition met → empty array (**not** `undefined`), so the action fires directly with no wizard.

This mirrors `WorkflowResource#createActionInputViews` one-for-one, including rule 4's exclusion.

## State transition — where the shape is repaired

```text
GET /initialactions/contenttype/{id}   ─┐
GET /defaultactions/contenttype/{id}   ─┤→ WorkflowDefaultActionView[]  (no actionInputs)
                                        │
                                        ▼
                    DotWorkflowsActionsService  ← ★ derivation applied here
                                        │
                                        ▼
                          DotCMSContentletWorkflowActions[]  (actionInputs populated)
                                        │
              ┌─────────────────────────┼─────────────────────────┐
              ▼                         ▼                         ▼
   content.feature:204          content.feature:338      locales.feature:259
     (new content)            (schemes → post-Reset)      (new translation)
              └─────────────────────────┼─────────────────────────┘
                                        ▼
                            store.currentContentActions
                                        ▼
              DotEditContentFormComponent.fireWorkflowAction
                                        ▼
                   actionInputs.length ? openWizard() : fire()
```

`GET /contentlet/{inode}/actions` (`getByInode`) already returns `WorkflowActionView` with
`actionInputs` populated server-side and is **not** touched — applying the derivation there too
would be redundant and would risk masking a future backend regression.

## Validation rules

- The derivation is **pure**: no I/O, no mutation of its argument, deterministic.
- It never returns `undefined` — an action with no inputs yields `[]`.
- It must not overwrite a non-empty `actionInputs` already present on the action. Applied only in
  the two methods whose endpoints are known not to send one; a defensive "preserve if present"
  keeps the helper safe if it is ever reused against an input-bearing payload.
