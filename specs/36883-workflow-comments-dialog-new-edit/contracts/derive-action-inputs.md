# Internal Contract: `deriveActionInputs()`

**Data model**: [../data-model.md](../data-model.md) · **Spec**: [../spec.md](../spec.md)

This feature exposes no external interface — no REST endpoint, no CLI, no public SDK surface. The
only contract worth pinning is the internal one between the new helper and its callers, because it
is a deliberate TypeScript mirror of a Java function and the two must not drift.

## Location

`core-web/libs/data-access/src/lib/dot-workflows-actions/dot-workflows-actions.utils.ts`
Exported from `core-web/libs/data-access/src/index.ts`.

## Signature

```ts
/**
 * Derives the `actionInputs` a workflow action needs, from the raw action flags.
 *
 * TypeScript mirror of `WorkflowResource#createActionInputViews`
 * (dotCMS/src/main/java/com/dotcms/rest/api/v1/workflow/WorkflowResource.java).
 * The default/initial-action endpoints return `WorkflowDefaultActionView`, which wraps the raw
 * `WorkflowAction` and carries no `actionInputs` — unlike the per-inode endpoint, which returns
 * `WorkflowActionView`. Keep both derivations in sync if a new input type is added.
 */
export const deriveActionInputs = (action: DotCMSWorkflowAction): DotCMSWorkflowInput[];
```

## Rules

Evaluated in order; see [data-model.md](./data-model.md#derivation-rules) for the table.

1. `assignable` → `{ id: 'assignable', body: {} }`
2. `commentable` → `{ id: 'commentable', body: {} }`
3. `hasPushPublishActionlet` → `{ id: 'pushPublish', body: {} }`
4. `hasMoveActionletActionlet && !hasMoveActionletHasPathActionlet` → `{ id: 'moveable', body: {} }`

## Guarantees

| Guarantee | Why it matters |
|---|---|
| Never returns `undefined`; empty means `[]` | `fireWorkflowAction` branches on `.length`; `undefined` is what caused the bug |
| A server-provided array is preserved — **including an empty one** | Presence, not length. Re-deriving over a legitimately empty `actionInputs` would hide the exact backend regression this guard exists to surface |
| Pure — no mutation of `action`, no I/O | Safe inside an RxJS `map` over a shared response |
| Order is stable and matches the Java rule order | `mergeCommentAndAssign` filters while preserving order; `setWizardInput` builds steps in sequence |
| Idempotent | Re-deriving an already-derived action yields an equal array |

## Consumer contract

`DotWorkflowsActionsService` applies it in — and only in — the two methods whose endpoints omit
the field:

| Method | Endpoint | Derivation applied? |
|---|---|---|
| `getDefaultActions()` | `GET /api/v1/workflow/initialactions/contenttype/{id}` | ✅ yes |
| `getWorkFlowActions()` | `GET /api/v1/workflow/defaultactions/contenttype/{id}` | ✅ yes |
| `getByInode()` | `GET /api/v1/workflow/contentlet/{inode}/actions` | ❌ no — server already populates it |
| `getByWorkflows()` | `POST /api/v1/workflow/schemes/actions/NEW` | ❌ no — out of scope; not consumed by edit-content |
| `getBulkActions()` | `POST /api/v1/workflow/contentlet/actions/bulk` | ❌ no — different view type, out of scope |

Deliberately **not** applied everywhere: blanket application would mask a future backend regression
in the endpoints that are supposed to populate the field themselves.

## Sync obligation

If `createActionInputViews` gains an input type in Java, this helper and its spec must be updated
too. The TypeScript side names its Java counterpart in the `deriveActionInputs` doc comment.

**The Java side does not point back**, and this PR does not add that pointer: a comment-only
`.java` change would put a Java file in the diff and, per
[ADR-0013](https://github.com/dotCMS/platform-adrs/blob/main/decisions/0013-skip-integration-and-postman-tests-for-frontend-only-changes-in-merge-queue-to-increase-flow.md),
move the PR off the frontend-only CI path for no behavioral gain. So the drift risk is one-way:
a Java dev adding a fifth input type has nothing pointing them here. Worth a follow-up issue
rather than a sentence claiming a symmetry that does not exist.
