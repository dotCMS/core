import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { Button } from 'primeng/button';
import { Menu } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import {
    MockDotMessageService,
    mockWorkflowsActions,
    mockWorkflowsActionsWithMove
} from '@dotcms/utils-testing';

import { DotWorkflowActionsComponent } from './dot-workflow-actions.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

// mockWorkflowsActions       → 3 actions (Assign Workflow, Save, Save/Publish)
// mockWorkflowsActionsWithMove → 4 actions (above + Move)

const SEPARATOR_ACTION = {
    assignable: false,
    commentable: false,
    condition: '',
    icon: '',
    id: 'separator-id',
    name: 'SEPARATOR',
    nextAssign: '',
    nextStep: '',
    nextStepCurrentStep: false,
    order: 0,
    roleHierarchyForAssign: false,
    schemeId: '',
    showOn: [],
    actionInputs: [],
    metadata: { subtype: 'SEPARATOR' }
};

const messageServiceMock = new MockDotMessageService({
    'edit.ema.page.no.workflow.action': 'no workflow action',
    Loading: 'loading'
});

describe('DotWorkflowActionsComponent', () => {
    let spectator: Spectator<DotWorkflowActionsComponent>;

    const createComponent = createComponentFactory({
        component: DotWorkflowActionsComponent,
        imports: [DotMessagePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({ props: { actions: [] } });
    });

    describe('empty state', () => {
        beforeEach(() => {
            spectator.detectChanges();
        });

        it('should show loading spinner and loading label when loading is true', () => {
            spectator.setInput('loading', true);
            spectator.detectChanges();

            const button = spectator.query(Button);

            expect(button.loading).toBeTruthy();
            expect(button.disabled).toBeFalsy();
            expect(button.label).toBe('loading');
        });

        it('should show disabled button with no-workflow label when not loading', () => {
            const button = spectator.query(Button);

            expect(button.disabled).toBeTruthy();
            expect(button.loading).toBeFalsy();
            expect(button.label).toBe('no workflow action');
        });
    });

    describe('inline buttons', () => {
        it('should render 1 primary button for 1 action', () => {
            spectator.setInput('actions', [mockWorkflowsActions[0]]);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(1);
            expect(buttons[0].variant).toBeNull();
        });

        it('should render primary and outlined buttons for 2 actions', () => {
            spectator.setInput('actions', mockWorkflowsActions.slice(0, 2));
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(2);
            expect(buttons[0].variant).toBeNull();
            expect(buttons[1].variant).toBe('outlined');
        });

        it('should render primary and outlined buttons for 3 actions', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(3);
            expect(buttons[0].variant).toBeNull();
            expect(buttons[1].variant).toBe('outlined');
            expect(buttons[2].variant).toBe('outlined');
        });

        it('should render labels matching the action names', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            mockWorkflowsActions.forEach((action, idx) => {
                expect(buttons[idx].label).toBe(action.name);
            });
        });

        it('should filter out SEPARATOR actions', () => {
            spectator.setInput('actions', [
                mockWorkflowsActions[0],
                SEPARATOR_ACTION,
                mockWorkflowsActions[1]
            ]);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);

            expect(buttons.length).toBe(2);
        });
    });

    describe('overflow', () => {
        it('should not show overflow button when actions are 3 or fewer', () => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();

            expect(spectator.query(byTestId('overflow-button'))).toBeNull();
            expect(spectator.query(Menu)).toBeNull();
        });

        it('should show overflow button when actions exceed 3', () => {
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            expect(spectator.query(byTestId('overflow-button'))).toBeTruthy();
        });

        it('should put actions beyond the third into the overflow menu model', () => {
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            const menu = spectator.query(Menu);
            const overflowAction = mockWorkflowsActionsWithMove[3];

            expect(menu.model.length).toBe(1);
            expect(menu.model[0].label).toBe(overflowAction.name);
        });

        it('should emit actionFired when an overflow menu item command is invoked', () => {
            spectator.setInput('actions', mockWorkflowsActionsWithMove);
            spectator.detectChanges();

            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const menu = spectator.query(Menu);
            menu.model[0].command({});

            expect(spy).toHaveBeenCalledWith(mockWorkflowsActionsWithMove[3]);
        });
    });

    describe('actionFired', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();
        });

        it('should emit the action when an inline button is clicked', () => {
            const spy = jest.spyOn(spectator.component.actionFired, 'emit');
            const action = mockWorkflowsActions[0];
            const btn = spectator
                .query(byTestId(`action-button-${action.id}`))
                ?.querySelector('button');

            spectator.click(btn);

            expect(spy).toHaveBeenCalledWith(action);
        });
    });

    describe('loading', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.setInput('loading', true);
            spectator.detectChanges();
        });

        it('should show loading spinner only on the primary (first) button', () => {
            const buttons = spectator.queryAll(Button);

            expect(buttons[0].loading).toBeTruthy();
            expect(buttons[1].loading).toBeFalsy();
            expect(buttons[2].loading).toBeFalsy();
        });

        it('should disable all buttons while loading', () => {
            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeTruthy();
            });
        });
    });

    describe('disabled', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
            spectator.detectChanges();
        });

        it('should disable all buttons when disabled is true', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeTruthy();
            });
        });

        it('should have all buttons enabled by default', () => {
            spectator.queryAll(Button).forEach((button) => {
                expect(button.disabled).toBeFalsy();
            });
        });
    });

    describe('size', () => {
        beforeEach(() => {
            spectator.setInput('actions', mockWorkflowsActions);
        });

        it('should use PrimeNG default size when size is normal', () => {
            spectator.setInput('size', 'normal');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBeUndefined();
            });
        });

        it('should set size to small on all buttons', () => {
            spectator.setInput('size', 'small');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBe('small');
            });
        });

        it('should set size to large on all buttons', () => {
            spectator.setInput('size', 'large');
            spectator.detectChanges();

            spectator.queryAll(Button).forEach((button) => {
                expect(button.size).toBe('large');
            });
        });
    });
});
