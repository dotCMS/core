import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotMessageService, DotRolesService } from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';

import {
    DotWorkflowAssignCommentComponent,
    DotWorkflowAssignCommentValue
} from './dot-workflow-assign-comment.component';

const ROLES: DotRole[] = [
    { id: 'role-1', name: 'Reviewer', user: false, roleKey: 'reviewer' },
    { id: 'role-2', name: 'Editor', user: false, roleKey: 'editor' }
];

describe('DotWorkflowAssignCommentComponent', () => {
    let spectator: Spectator<DotWorkflowAssignCommentComponent>;

    const get = jest.fn();

    const createComponent = createComponentFactory({
        component: DotWorkflowAssignCommentComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            })
        ],
        componentProviders: [mockProvider(DotRolesService, { get })],
        detectChanges: false
    });

    beforeEach(() => {
        // `get` is shared across tests, so calls would otherwise accumulate into the next assertion.
        jest.clearAllMocks();
        get.mockReturnValue(of(ROLES));
    });

    /** Collects everything the component emitted, in order. */
    const captureValues = (): DotWorkflowAssignCommentValue[] => {
        const values: DotWorkflowAssignCommentValue[] = [];
        spectator
            .output<DotWorkflowAssignCommentValue>('valueChange')
            .subscribe((v) => values.push(v));

        return values;
    };

    const captureValidity = (): boolean[] => {
        const valid: boolean[] = [];
        spectator.output<boolean>('validChange').subscribe((v) => valid.push(v));

        return valid;
    };

    it('should render no dialog or submit control of its own', () => {
        // The host owns the step frame; a control here would be an unsynchronised second commit path.
        spectator = createComponent({ props: { assignable: true, commentable: true } });
        spectator.detectChanges();

        expect(spectator.query('p-dialog')).toBeNull();
        expect(spectator.query('button')).toBeNull();
    });

    describe('which fields render', () => {
        it('should render only the assignee when the action is assignable only', () => {
            spectator = createComponent({ props: { assignable: true, commentable: false } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-assign-select'))).toBeTruthy();
            expect(spectator.query(byTestId('workflow-comment-textarea'))).toBeNull();
        });

        it('should render only the comment when the action is commentable only', () => {
            spectator = createComponent({ props: { assignable: false, commentable: true } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-assign-select'))).toBeNull();
            expect(spectator.query(byTestId('workflow-comment-textarea'))).toBeTruthy();
        });

        it('should render both when the action declares both', () => {
            // The common case for an approval action, and why they share one component.
            spectator = createComponent({ props: { assignable: true, commentable: true } });
            spectator.detectChanges();

            expect(spectator.query(byTestId('workflow-assign-select'))).toBeTruthy();
            expect(spectator.query(byTestId('workflow-comment-textarea'))).toBeTruthy();
        });

        it('should render no move field', () => {
            // Unlike the legacy form, which conflates `moveable` in here and would give a consumer
            // with its own move step a duplicate control.
            spectator = createComponent({ props: { assignable: true, commentable: true } });
            spectator.detectChanges();

            expect(spectator.query('dot-page-selector')).toBeNull();
        });
    });

    describe('assignable roles', () => {
        it('should scope the lookup to the action next assign and hierarchy', () => {
            spectator = createComponent({
                props: { assignable: true, roleId: 'role-x', roleHierarchy: true }
            });
            spectator.detectChanges();

            expect(get).toHaveBeenCalledWith('role-x', true);
        });

        it('should not look up roles for a comment-only action', () => {
            spectator = createComponent({ props: { assignable: false, commentable: true } });
            spectator.detectChanges();

            expect(get).not.toHaveBeenCalled();
        });

        it('should default to the first role so the step is valid on arrival', () => {
            const values: DotWorkflowAssignCommentValue[] = [];
            spectator = createComponent({ props: { assignable: true } });
            spectator
                .output<DotWorkflowAssignCommentValue>('valueChange')
                .subscribe((v) => values.push(v));
            spectator.detectChanges();

            expect(values.at(-1)?.assign).toBe('role-1');
        });
    });

    describe('validity', () => {
        it('should be valid immediately for a comment-only action', () => {
            // A comment is never required — the backend accepts an empty one — so there is nothing
            // the user must supply.
            spectator = createComponent({ props: { assignable: false, commentable: true } });
            const valid = captureValidity();
            spectator.detectChanges();

            expect(valid.at(-1)).toBe(true);
        });

        it('should be invalid when an assignable action has no roles to assign to', () => {
            get.mockReturnValue(of([]));

            spectator = createComponent({ props: { assignable: true } });
            const valid = captureValidity();
            spectator.detectChanges();

            expect(valid.at(-1)).toBe(false);
        });

        it('should stay usable and invalid when the roles lookup fails', () => {
            get.mockReturnValue(throwError(() => new Error('boom')));

            spectator = createComponent({ props: { assignable: true } });
            const valid = captureValidity();
            spectator.detectChanges();

            expect(valid.at(-1)).toBe(false);
            expect(spectator.query(byTestId('workflow-assign-select'))).toBeTruthy();
        });
    });

    describe('the emitted value', () => {
        it('should carry the comment as typed', () => {
            spectator = createComponent({ props: { commentable: true } });
            const values = captureValues();
            spectator.detectChanges();

            spectator.component['onCommentChange']('Looks good');
            spectator.detectChanges();

            expect(values.at(-1)).toEqual({ assign: '', comment: 'Looks good' });
        });

        it('should carry the chosen assignee', () => {
            spectator = createComponent({ props: { assignable: true } });
            const values = captureValues();
            spectator.detectChanges();

            spectator.component['onAssignChange']('role-2');
            spectator.detectChanges();

            expect(values.at(-1)).toEqual({ assign: 'role-2', comment: '' });
        });

        it('should normalise a cleared field to an empty string', () => {
            // The backend expects strings; `null` would serialise as "null" in the fire payload.
            spectator = createComponent({ props: { assignable: true, commentable: true } });
            const values = captureValues();
            spectator.detectChanges();

            spectator.component['onAssignChange'](null);
            spectator.component['onCommentChange'](null);
            spectator.detectChanges();

            expect(values.at(-1)).toEqual({ assign: '', comment: '' });
        });
    });
});
