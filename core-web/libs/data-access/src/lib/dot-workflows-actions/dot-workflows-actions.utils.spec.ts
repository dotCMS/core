import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { createFakeWorkflowAction } from '@dotcms/utils-testing';

import {
    deriveActionInputs,
    deriveInputsOnEach,
    withDerivedActionInputs
} from './dot-workflows-actions.utils';

describe('deriveActionInputs', () => {
    describe('rule per flag', () => {
        it('should derive nothing when no flag is set', () => {
            expect(deriveActionInputs(createFakeWorkflowAction())).toEqual([]);
        });

        it('should derive assignable', () => {
            const action = createFakeWorkflowAction({ assignable: true });

            expect(deriveActionInputs(action)).toEqual([{ id: 'assignable', body: {} }]);
        });

        it('should derive commentable', () => {
            const action = createFakeWorkflowAction({ commentable: true });

            expect(deriveActionInputs(action)).toEqual([{ id: 'commentable', body: {} }]);
        });

        it('should derive pushPublish', () => {
            const action = createFakeWorkflowAction({ hasPushPublishActionlet: true });

            expect(deriveActionInputs(action)).toEqual([{ id: 'pushPublish', body: {} }]);
        });

        it('should derive moveable when the move actionlet has no preset path', () => {
            const action = createFakeWorkflowAction({
                hasMoveActionletActionlet: true,
                hasMoveActionletHasPathActionlet: false
            });

            expect(deriveActionInputs(action)).toEqual([{ id: 'moveable', body: {} }]);
        });

        it('should NOT derive moveable when the move actionlet already has a path', () => {
            // Mirrors the exclusion in WorkflowResource#createActionInputViews: a preset path
            // means there is nothing to ask the author for.
            const action = createFakeWorkflowAction({
                hasMoveActionletActionlet: true,
                hasMoveActionletHasPathActionlet: true
            });

            expect(deriveActionInputs(action)).toEqual([]);
        });
    });

    describe('combinations and ordering', () => {
        it('should derive every input in the Java rule order', () => {
            const action = createFakeWorkflowAction({
                assignable: true,
                commentable: true,
                hasPushPublishActionlet: true,
                hasMoveActionletActionlet: true
            });

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
            const action = createFakeWorkflowAction({ assignable: true, commentable: true });

            expect(deriveActionInputs(action).map(({ id }) => id)).toEqual([
                'assignable',
                'commentable'
            ]);
        });
    });
});

describe('withDerivedActionInputs', () => {
    it('should derive when the key is absent, the way the default-action endpoints send it', () => {
        const action = createFakeWorkflowAction({ commentable: true });

        expect(withDerivedActionInputs(action).actionInputs).toEqual([
            { id: 'commentable', body: {} }
        ]);
    });

    it('should preserve a server-provided array rather than re-derive it', () => {
        // The fixture is deliberately contradictory — `commentable: true` but an array that does
        // not contain `commentable` — so a re-derivation would visibly change the result.
        const serverProvided = [{ id: 'moveable', body: {} }];
        const action = createFakeWorkflowAction({
            commentable: true,
            actionInputs: serverProvided
        });

        expect(withDerivedActionInputs(action).actionInputs).toBe(serverProvided);
    });

    it('should preserve a server-provided EMPTY array', () => {
        // Presence, not length. An endpoint that starts emitting WorkflowActionView with a
        // legitimately empty actionInputs is the regression this guard exists to surface;
        // re-deriving over it would hide exactly that.
        const action = createFakeWorkflowAction({ commentable: true, actionInputs: [] });

        expect(withDerivedActionInputs(action).actionInputs).toEqual([]);
    });

    it('should return an empty array, never undefined, for an action with no inputs', () => {
        expect(withDerivedActionInputs(createFakeWorkflowAction()).actionInputs).toEqual([]);
    });

    it('should not mutate the action it is given', () => {
        const action = createFakeWorkflowAction({ commentable: true });
        const snapshot = JSON.stringify(action);

        withDerivedActionInputs(action);

        expect(JSON.stringify(action)).toBe(snapshot);
    });

    it('should be idempotent', () => {
        const action = createFakeWorkflowAction({ commentable: true });
        const once = withDerivedActionInputs(action);

        expect(withDerivedActionInputs(once).actionInputs).toEqual(once.actionInputs);
    });
});

describe('deriveInputsOnEach', () => {
    it('should derive on every action in the response without mutating the input', () => {
        const response = [
            {
                scheme: { id: 'scheme-id' },
                action: createFakeWorkflowAction({ commentable: true }),
                firstStep: { id: 'first-step' }
            },
            {
                scheme: { id: 'scheme-id' },
                action: createFakeWorkflowAction({ assignable: true }),
                firstStep: { id: 'first-step' }
            }
        ] as unknown as Parameters<typeof deriveInputsOnEach>[0];

        const result = deriveInputsOnEach(response);

        expect(result.map((r) => r.action.actionInputs)).toEqual([
            [{ id: 'commentable', body: {} }],
            [{ id: 'assignable', body: {} }]
        ]);
        expect((response[0].action as DotCMSWorkflowAction).actionInputs).toBeUndefined();
    });
});
