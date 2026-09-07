import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { deriveActionInputs } from './dot-workflows-actions.utils';

/**
 * Builds an action shaped like the one the default/initial-action endpoints actually return:
 * the raw `WorkflowAction` flags, and **no `actionInputs` key at all**.
 *
 * Verified against a live instance — `GET /api/v1/workflow/initialactions/contenttype/webPageContent`
 * returns `commentable: true` with no `actionInputs` property, while
 * `GET /api/v1/workflow/contentlet/{inode}/actions` returns the same action with
 * `actionInputs: [{ body: {}, id: 'commentable' }]`.
 */
const buildRawAction = (
    flags: Partial<DotCMSWorkflowAction> = {}
): Omit<DotCMSWorkflowAction, 'actionInputs'> => ({
    assignable: false,
    commentable: false,
    condition: '',
    icon: 'workflowIcon',
    id: 'action-id',
    name: 'Publish',
    nextAssign: 'role-id',
    nextStep: 'step-id',
    nextStepCurrentStep: false,
    order: 0,
    roleHierarchyForAssign: false,
    schemeId: 'scheme-id',
    showOn: ['NEW', 'EDITING'],
    ...flags
});

describe('deriveActionInputs', () => {
    describe('rule per flag', () => {
        it('should derive nothing when no flag is set', () => {
            expect(deriveActionInputs(buildRawAction() as DotCMSWorkflowAction)).toEqual([]);
        });

        it('should derive assignable', () => {
            const action = buildRawAction({ assignable: true }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action)).toEqual([{ id: 'assignable', body: {} }]);
        });

        it('should derive commentable', () => {
            const action = buildRawAction({ commentable: true }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action)).toEqual([{ id: 'commentable', body: {} }]);
        });

        it('should derive pushPublish', () => {
            const action = buildRawAction({
                hasPushPublishActionlet: true
            }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action)).toEqual([{ id: 'pushPublish', body: {} }]);
        });

        it('should derive moveable when the move actionlet has no preset path', () => {
            const action = buildRawAction({
                hasMoveActionletActionlet: true,
                hasMoveActionletHasPathActionlet: false
            }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action)).toEqual([{ id: 'moveable', body: {} }]);
        });

        it('should NOT derive moveable when the move actionlet already has a path', () => {
            // Mirrors the exclusion in WorkflowResource#createActionInputViews: a preset path
            // means there is nothing to ask the author for.
            const action = buildRawAction({
                hasMoveActionletActionlet: true,
                hasMoveActionletHasPathActionlet: true
            }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action)).toEqual([]);
        });
    });

    describe('combinations and ordering', () => {
        it('should derive every input in the Java rule order', () => {
            const action = buildRawAction({
                assignable: true,
                commentable: true,
                hasPushPublishActionlet: true,
                hasMoveActionletActionlet: true
            }) as DotCMSWorkflowAction;

            // Order matters: mergeCommentAndAssign filters while preserving order, and
            // setWizardInput builds the wizard steps in the resulting sequence.
            expect(deriveActionInputs(action).map(({ id }) => id)).toEqual([
                'assignable',
                'commentable',
                'pushPublish',
                'moveable'
            ]);
        });

        it('should derive assignable and commentable together (the "Allow Comments" + "Assign to" case)', () => {
            const action = buildRawAction({
                assignable: true,
                commentable: true
            }) as DotCMSWorkflowAction;

            expect(deriveActionInputs(action).map(({ id }) => id)).toEqual([
                'assignable',
                'commentable'
            ]);
        });
    });

    describe('guarantees', () => {
        it('should always return an array, never undefined', () => {
            expect(
                Array.isArray(deriveActionInputs(buildRawAction() as DotCMSWorkflowAction))
            ).toBe(true);
        });

        it('should not mutate the action it is given', () => {
            const action = buildRawAction({ commentable: true }) as DotCMSWorkflowAction;
            const snapshot = JSON.stringify(action);

            deriveActionInputs(action);

            expect(JSON.stringify(action)).toBe(snapshot);
        });

        it('should be idempotent when re-derived from an already-derived action', () => {
            const action = buildRawAction({ commentable: true }) as DotCMSWorkflowAction;
            const first = deriveActionInputs(action);
            const second = deriveActionInputs({ ...action, actionInputs: first });

            expect(second).toEqual(first);
        });

        it('should tolerate the optional actionlet flags being absent', () => {
            // The endpoints omit these when false, so they arrive as `undefined`, not `false`.
            const action = { ...buildRawAction(), commentable: true } as DotCMSWorkflowAction;
            delete (action as Partial<DotCMSWorkflowAction>).hasPushPublishActionlet;
            delete (action as Partial<DotCMSWorkflowAction>).hasMoveActionletActionlet;

            expect(deriveActionInputs(action)).toEqual([{ id: 'commentable', body: {} }]);
        });
    });
});
