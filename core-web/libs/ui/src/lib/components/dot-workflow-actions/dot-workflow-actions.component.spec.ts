import { createHostFactory, SpectatorHost } from '@ngneat/spectator';

import { Button } from 'primeng/button';
import { SplitButton, SplitButtonModule } from 'primeng/splitbutton';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockWorkflowsActions } from '@dotcms/utils-testing';

import { DotWorkflowActionsComponent } from './dot-workflow-actions.component';

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

describe('DotWorkflowActionsComponent', () => {
    let spectator: SpectatorHost<DotWorkflowActionsComponent>;

    const createHost = createHostFactory({
        component: DotWorkflowActionsComponent,
        imports: [ToolbarModule, SplitButtonModule],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        // CREATE HOST COMPONENT TO LET ANGULAR UPDATE SIGNAL COMPONENTS UNDER THE HOOD
        spectator = createHost(
            `
        <dot-workflow-actions
          [actions]="actions"
          [groupAction]="groupAction"
          [loading]="loading"
          [size]="size"
        ></dot-workflow-actions>
      `,
            {
                hostProps: {
                    actions: WORKFLOW_ACTIONS_MOCK,
                    groupAction: true,
                    loading: false,
                    size: 'normal'
                }
            }
        );
        spectator.detectChanges();
    });

    describe('without actions', () => {
        beforeEach(() => {
            // THIS TRIGGER THE SIGNAL INPUT UPDATE
            spectator.setHostInput({
                actions: [],
                loading: true
            });
            spectator.detectChanges();
        });

        it('should render the empty button with loading', () => {
            const button = spectator.query(Button);
            expect(button).toBeTruthy();
            expect(button?.disabled).toBeFalsy();
            expect(button?.label).toBe('loading');
        });

        it('should render the empty button with disabled', () => {
            spectator.setHostInput({
                loading: false
            });
            spectator.detectChanges();

            const button = spectator.query(Button);

            expect(button.disabled).toBeTruthy();
            expect(button.loading).toBeFalsy();
            expect(button.label).toBe('no workflow action');
        });
    });

    describe('group action', () => {
        beforeEach(() => {
            spectator.setHostInput({
                actions: WORKFLOW_ACTIONS_MOCK,
                groupAction: true,
                loading: false,
                size: 'normal'
            });
            spectator.detectChanges();
        });

        it('should render an extra split button for each `SEPARATOR` Action', () => {
            const splitButtons = spectator.queryAll(SplitButton);

            expect(splitButtons.length).toBe(2);
        });

        it('should emit the action when click on a split button', () => {
            const spy = spyOn(spectator.component.actionFired, 'emit');
            const splitButton = spectator.query('.p-splitbutton > button');
            splitButton.dispatchEvent(new Event('click'));

            expect(spy).toHaveBeenCalledWith(WORKFLOW_ACTIONS_MOCK[0]);
        });

        it('should render a normal button is a group has only one action', () => {
            spectator.setHostInput({
                actions: [WORKFLOW_ACTIONS_MOCK[0]]
            });
            spectator.detectChanges();

            const button = spectator.query(Button);
            expect(button).not.toBeNull();
        });
    });

    describe('not group action', () => {
        beforeEach(() => {
            spectator.setHostInput({ groupAction: false });
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

            spectator.setHostInput({
                actions: [action, WORKFLOW_ACTIONS_SEPARATOR_MOCK]
            });
            spectator.detectChanges();

            const buttons = spectator.queryAll(Button);
            expect(buttons.length).toBe(1);
            expect(buttons[0].label.trim()).toBe(action.name.trim());
        });
    });

    describe('loading', () => {
        beforeEach(() => {
            spectator.setHostInput({
                actions: [
                    ...WORKFLOW_ACTIONS_MOCK,
                    WORKFLOW_ACTIONS_SEPARATOR_MOCK,
                    WORKFLOW_ACTIONS_MOCK[0]
                ],
                loading: true
            });
            spectator.detectChanges();
        });

        it('should disabled split buttons and set normal buttons loading', () => {
            const button = spectator.query(Button);
            const splitButton = spectator.query(SplitButton);

            expect(button.loading).toBeTruthy();
            expect(splitButton.disabled).toBeTruthy();
        });
    });

    describe('size', () => {
        beforeEach(async () => {
            spectator.setHostInput({
                actions: [
                    mockWorkflowsActions[0],
                    WORKFLOW_ACTIONS_SEPARATOR_MOCK,
                    ...mockWorkflowsActions
                ],
                groupAction: true,
                loading: false,
                size: 'small'
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set style class p-button-sm', () => {
            const button = spectator.query(Button);
            const splitButton = spectator.query(SplitButton);

            expect(button.styleClass.trim()).toBe('p-button-sm');
            expect(splitButton.styleClass.trim()).toBe('p-button-sm p-button-outlined');
        });

        it('should set style class p-button-lg', () => {
            spectator.setHostInput({ size: 'large' });
            spectator.detectChanges();

            const button = spectator.query(Button);
            const splitButton = spectator.query(SplitButton);

            expect(button.styleClass.trim()).toBe('p-button-lg');
            expect(splitButton.styleClass.trim()).toBe('p-button-lg p-button-outlined');
        });

        it('should have default size', () => {
            spectator.setHostInput({ size: 'normal' });
            spectator.detectChanges();

            const button = spectator.query(Button);
            const splitButton = spectator.query(SplitButton);

            expect(button.styleClass.trim()).toBe('');
            expect(splitButton.styleClass.trim()).toBe('p-button-outlined');
        });
    });
});
