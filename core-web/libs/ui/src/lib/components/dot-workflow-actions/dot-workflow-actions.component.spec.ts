import { createComponentFactory, Spectator } from '@ngneat/spectator';

import { Button } from 'primeng/button';
import { SplitButton, SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { DotClipboardUtil, DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';
import { MockDotMessageService, mockWorkflowsActions } from '@dotcms/utils-testing';

const WORKFLOW_ACTIONS_SEPARATOR_MOCK: DotCMSWorkflowAction = {
    assignable: true,
    commentable: true,
    condition: '',
    icon: 'workflowIcon',
    id: '44d4d4cd-c812-49db-adb1-1030be73e69a',
    name: 'SEPARATOR',
    nextAssign: 'db0d2bca-5da5-4c18-b5d7-87f02ba58eb6',
    nextStep: '43e16aac-5799-46d0-945c-83753af39426',
    nextStepCurrentStep: false,
    order: 0,
    owner: null,
    roleHierarchyForAssign: true,
    schemeId: '85c1515c-c4f3-463c-bac2-860b8fcacc34',
    showOn: ['UNLOCKED', 'LOCKED'],
    metadata: {
        subtype: 'SEPARATOR'
    },
    actionInputs: [
        {
            body: {},
            id: 'assignable'
        },
        {
            body: {},
            id: 'commentable'
        },
        {
            body: {},
            id: 'pushPublish'
        },
        { body: {}, id: 'moveable' }
    ]
};

const WORKFLOW_ACTIONS_MOCK = [
    ...mockWorkflowsActions,
    WORKFLOW_ACTIONS_SEPARATOR_MOCK,
    ...mockWorkflowsActions
];

const messageServiceMock = new MockDotMessageService({
    'edit.ema.page.no.workflow.action': 'no workflow action',
    Loading: 'loading'
});

const getComponents = (spectator: Spectator<DotWorkflowActionsComponent>) => {
    return {
        button: spectator.query(Button),
        splitButton: spectator.query(SplitButton)
    };
};

describe('DotWorkflowActionsComponent', () => {
    let spectator: Spectator<DotWorkflowActionsComponent>;

    const createComponent = createComponentFactory({
        component: DotWorkflowActionsComponent,
        imports: [ToolbarModule, SplitButtonModule, DotMessagePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            DotClipboardUtil
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                actions: WORKFLOW_ACTIONS_MOCK,
                groupActions: true,
                loading: false,
                size: 'normal'
            }
        });
        spectator.detectChanges();
    });

    describe('without actions', () => {
        beforeEach(() => {
            spectator.setInput('actions', []);
            spectator.detectChanges();
        });

        it('should render the empty button with loading', () => {
            spectator.setInput('loading', true);

            spectator.detectChanges();
            const button = spectator.query(Button);

            expect(button.loading).toBeTruthy();
            expect(button.disabled).toBeFalsy();
            expect(button.label).toBe('loading');
        });

        it('should render the empty button with disabled', () => {
            spectator.setInput('loading', false);

            const button = spectator.query(Button);

            spectator.detectChanges();

            expect(button.disabled).toBeTruthy();
            expect(button.loading).toBeFalsy();
            expect(button.label).toBe('no workflow action');
        });
    });

    describe('group action', () => {
        it('should render an extra split button for each `SEPARATOR` Action', () => {
            const splitButtons = spectator.queryAll(SplitButton);
            spectator.detectComponentChanges();
            expect(splitButtons.length).toBe(2);
        });

        it('should emit the action when click on a split button', () => {
            const spy = spyOn(spectator.component.actionFired, 'emit');
            const splitButton = spectator.query('.p-splitbutton > button');
            splitButton.dispatchEvent(new Event('click'));

            expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
        });

        it('should render a normal button is a group has only one action', () => {
            spectator.setInput('actions', [WORKFLOW_ACTIONS_MOCK[0]]);
            spectator.detectChanges();

            const button = spectator.query('.p-button');
            expect(button).not.toBeNull();
        });
    });

    describe('not group action', () => {
        beforeEach(() => {
            spectator.setInput('groupActions', false);
            spectator.detectComponentChanges();
        });

        it('should render one split button and remove the `SEPARATOR` Action', () => {
            const splitButtons = spectator.queryAll(SplitButton);
            const amountOfItems = WORKFLOW_ACTIONS_MOCK.length - 2; // Less the `SEPARATOR` Action and the First actions which is the default one
            expect(splitButtons.length).toBe(1);
            expect(splitButtons[0].model.length).toBe(amountOfItems);
        });

        it('should render a normal button and remove the `SEPARATOR` Action', () => {
            const action = WORKFLOW_ACTIONS_MOCK[0];

            spectator.setInput('actions', [action, WORKFLOW_ACTIONS_SEPARATOR_MOCK]);
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);
            expect(buttons.length).toBe(1);
            expect(buttons[0].label.trim()).toBe(action.name.trim());
        });
    });

    describe('loading', () => {
        beforeEach(() => {
            spectator.setInput('loading', true);
            spectator.setInput('actions', [
                ...WORKFLOW_ACTIONS_MOCK,
                WORKFLOW_ACTIONS_SEPARATOR_MOCK,
                WORKFLOW_ACTIONS_MOCK[0]
            ]);
            spectator.detectComponentChanges();
        });

        it('should disabled split buttons and set normal buttons loading', () => {
            const button = spectator.query(Button);
            const splitButton = spectator.query(SplitButton);

            expect(button.loading).toBeTruthy();
            expect(splitButton.disabled).toBeTruthy();
        });
    });

    describe('size', () => {
        beforeEach(() => {
            spectator.setInput('actions', [
                mockWorkflowsActions[0],
                WORKFLOW_ACTIONS_SEPARATOR_MOCK,
                ...mockWorkflowsActions
            ]);
            spectator.detectChanges();
        });

        it('should have default size', () => {
            const { button, splitButton } = getComponents(spectator);
            expect(button.styleClass.trim()).toBe('');
            expect(splitButton.styleClass.trim()).toBe('');
        });

        it('should set style class p-button-sm', () => {
            spectator.setInput('size', 'small');
            spectator.detectChanges();

            const { button, splitButton } = getComponents(spectator);

            expect(splitButton.styleClass.trim()).toBe('p-button-sm');
            expect(button.styleClass.trim()).toBe('p-button-sm');
        });

        it('should set style class p-button-lg', () => {
            spectator.setInput('size', 'large');
            spectator.detectChanges();

            const { button, splitButton } = getComponents(spectator);

            expect(button.styleClass.trim()).toBe('p-button-lg');
            expect(splitButton.styleClass.trim()).toBe('p-button-lg');
        });
    });
});
