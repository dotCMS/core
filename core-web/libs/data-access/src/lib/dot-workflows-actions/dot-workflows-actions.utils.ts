import { DotCMSWorkflowAction, DotCMSWorkflowInput } from '@dotcms/dotcms-models';

/**
 * Derives the `actionInputs` a workflow action needs, from the raw action flags.
 *
 * TypeScript mirror of `WorkflowResource#createActionInputViews`
 * (`dotCMS/src/main/java/com/dotcms/rest/api/v1/workflow/WorkflowResource.java`).
 *
 * The default/initial-action endpoints return `WorkflowDefaultActionView`, which wraps the raw
 * `WorkflowAction` and carries **no** `actionInputs` — unlike the per-inode endpoint, which
 * returns `WorkflowActionView` with the array already built server-side. Consumers that gate on
 * `actionInputs.length` (the new Edit Content UI decides whether to open its input wizard that
 * way) therefore saw every action as input-less for content with no inode. See issue #36883.
 *
 * Keep this in sync with the Java method if a new input type is ever added.
 *
 * @param action the workflow action to inspect
 * @returns the derived inputs, in the same order the Java rule emits them; `[]` when none apply
 */
export const deriveActionInputs = (action: DotCMSWorkflowAction): DotCMSWorkflowInput[] => {
    const actionInputs: DotCMSWorkflowInput[] = [];

    if (action.assignable) {
        actionInputs.push({ id: 'assignable', body: {} });
    }

    if (action.commentable) {
        actionInputs.push({ id: 'commentable', body: {} });
    }

    if (action.hasPushPublishActionlet) {
        actionInputs.push({ id: 'pushPublish', body: {} });
    }

    // Has a move actionlet but the path is empty — a preset path means there is nothing to ask
    // the author for.
    if (action.hasMoveActionletActionlet && !action.hasMoveActionletHasPathActionlet) {
        actionInputs.push({ id: 'moveable', body: {} });
    }

    return actionInputs;
};

/**
 * Returns the action with its `actionInputs` guaranteed to be populated.
 *
 * Applied only to responses from endpoints known to omit the field. An array the server already
 * sent is left untouched: re-deriving it would silently paper over a backend regression in the
 * endpoints that are supposed to build it themselves.
 *
 * @param action the workflow action as it came off the wire
 * @returns a new action; the argument is never mutated
 */
export const withDerivedActionInputs = (action: DotCMSWorkflowAction): DotCMSWorkflowAction => {
    // No optional chaining on `action` itself: `deriveActionInputs` dereferences it anyway, so a
    // `?.` here would only move the TypeError one line down while reading as if it were handled.
    // A missing action is a malformed payload — let it fail loudly rather than half-guard it.
    if (action.actionInputs?.length) {
        return action;
    }

    return { ...action, actionInputs: deriveActionInputs(action) };
};
